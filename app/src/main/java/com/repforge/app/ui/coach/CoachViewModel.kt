package com.repforge.app.ui.coach

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.HealthJournalStore
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.time.*

internal data class CoachTarget(val programId: String, val dayId: String, val title: String,
    val schedule: CalendarScheduleEntity? = null, val upcoming: Boolean = false,
    val programTitle: String = "")
internal data class CoachUiState(val loading: Boolean = true, val error: Boolean = false, val language: String = "hu",
    val readiness: NativeCoachReadiness.Result? = null, val target: CoachTarget? = null,
    val refreshing: Boolean = false, val refreshFailed: Boolean = false,
    val variationPreview: NativeCoachVariation.Preview? = null, val variationBusy: Boolean = false,
    val variationFailed: Boolean = false, val variationSaved: Boolean = false,
    val exercises: List<CoachExerciseAdvice> = emptyList(), val provider: String = "") {
    /** Only the initial load needs an expanding placeholder; re-reads keep the existing layout. */
    val initialLoading: Boolean get() = loading && provider.isBlank()
}

/** Reads canonical provider projections and a bounded history slice; never fabricates Health samples. */
internal class CoachViewModel internal constructor(application: Application,
    private val db: TrainPilotDatabase,
    private val snapshotReader: (suspend () -> CoachUiState)? = null) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, TrainPilotDatabase.getInstance(application.applicationContext))
    private val app = application.applicationContext
    private val _state = MutableStateFlow(CoachUiState())
    val state = _state.asStateFlow()
    private var loading: Job? = null
    private var reloadAgain = false
    init { reload() }
    fun previewVariation(exerciseId: String) {
        val target = _state.value.target ?: return
        if (_state.value.variationBusy || _state.value.loading) return
        _state.value = _state.value.copy(variationBusy = true, variationFailed = false, variationSaved = false)
        viewModelScope.launch {
            try {
                val preview = withContext(Dispatchers.IO) { NativeCoachVariation(app, db).preview(target, exerciseId) }
                _state.value = _state.value.copy(variationPreview = preview)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.value = _state.value.copy(variationFailed = true) }
            finally { _state.value = _state.value.copy(variationBusy = false) }
        }
    }
    fun closeVariation() { if (!_state.value.variationBusy) {
        _state.value = _state.value.copy(variationPreview = null, variationFailed = false)
        reload()
    } }
    fun saveVariation() {
        val preview = _state.value.variationPreview ?: return
        if (_state.value.variationBusy) return
        _state.value = _state.value.copy(variationBusy = true, variationFailed = false)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { NativeCoachVariation(app, db).save(preview) }
                _state.value = _state.value.copy(variationPreview = null, variationSaved = true)
                try { withContext(Dispatchers.IO) { com.repforge.app.WearSyncRepository.publishWatchHomeSnapshot(app) } }
                catch (_: Exception) { /* Durable program remains available; normal Wear sync retries. */ }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.value = _state.value.copy(variationFailed = true) }
            finally {
                _state.value = _state.value.copy(variationBusy = false)
                if (_state.value.variationSaved) reload()
            }
        }
    }
    fun refreshHealth(activity: android.app.Activity?) {
        if (activity == null || _state.value.refreshing || _state.value.loading) return
        _state.value = _state.value.copy(refreshing = true, refreshFailed = false)
        viewModelScope.launch {
            try {
                val result = com.repforge.app.NativeHealthRefresh.readToday(app, activity)
                _state.value = _state.value.copy(refreshFailed = result.isFailure)
                // Read the canonical stored projections even on permission/provider failure;
                // stale or absent measurements remain subject to the same freshness rules.
                loading?.join()
                reload()
            } finally { _state.value = _state.value.copy(refreshing = false) }
        }
    }
    fun reload() {
        if (_state.value.variationBusy || _state.value.variationPreview != null) return
        // Multiple native/health/UI notifications must not repeatedly cancel the same read.
        // Keep one follow-up so a real change during IO is never lost.
        if (loading?.isActive == true) { reloadAgain = true; return }
        loading = viewModelScope.launch {
            do {
                reloadAgain = false
                readCoach()
            } while (reloadAgain)
        }
    }

    private suspend fun readCoach() {
        val provider = HealthJournalStore.get(app).preferences().optString("provider", HealthJournalStore.HC)
        if (_state.value.provider != provider) _state.value = _state.value.copy(readiness = null, exercises = emptyList())
        _state.value = _state.value.copy(loading = true, error = false)
        try {
            val loaded = withContext(Dispatchers.IO) { snapshotReader?.invoke() ?: readCanonical() }
            _state.value = loaded.copy(loading = reloadAgain)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            // Failed re-read retains the prior snapshot and explicitly marks its freshness.
            _state.value = _state.value.copy(loading = reloadAgain, error = true)
        }
    }

    private suspend fun readCanonical(): CoachUiState = com.repforge.app.NativeDataReplacement.mutate(app) {
        val settings = TrainPilotSettingsStore(app).current()
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val selected = NativeSelectedCoachReadings.load(app, db, now, zone)
        val target = NativeCoachTargets(db).select(settings.activeProgramId, settings.language, now, zone)
        val advice = NativeCoachAdviceRepository(db).load(target, settings, selected.readiness, now)
        check(HealthJournalStore.get(app).preferences().optString("provider", HealthJournalStore.HC) == selected.provider) {
            "Az egészségadat-forrás megváltozott. Frissítsd a Coach adatait."
        }
        CoachUiState(false, false, settings.language, selected.readiness, target,
            refreshing = _state.value.refreshing, refreshFailed = _state.value.refreshFailed,
            exercises = advice, provider = selected.provider, variationSaved = _state.value.variationSaved)
    }
}

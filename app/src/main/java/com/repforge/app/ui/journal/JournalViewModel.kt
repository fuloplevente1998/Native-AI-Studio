package com.repforge.app.ui.journal

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.BackupRestoreManager
import com.repforge.app.GpsRouteStore
import com.repforge.app.NativeJournalEditor
import com.repforge.app.NativeWorkoutHealth
import com.repforge.app.R
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.WearSyncRepository
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.shared.GpsRouteLedger193
import kotlinx.coroutines.Dispatchers
import androidx.paging.cachedIn
import androidx.room.withTransaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class PrRecordUiModel(
    val exerciseId: String,
    val exerciseName: String,
    val kind: String,
    val value: String,
    val previous: String
)

data class RouteCoordinatesUiModel(
    val routeKey: String,
    val segments: List<List<Pair<Double, Double>>>,
    val pointCount: Int,
    val distanceMeters: Double,
    val accuracyMeters: Double?,
    val missingRouteParts: Int = 0
)

data class JournalUiState(
    val query: String = "", val month: java.time.YearMonth? = null,
    val selectedWorkoutId: String? = null,
    val detail: WorkoutHistoryWithDetails? = null,
    val detailLoading: Boolean = false, val detailError: Boolean = false,
    val activeRoute: RouteCoordinatesUiModel? = null,
    val routeLoading: Boolean = false, val routeError: Boolean = false,
    val workoutToDelete: WorkoutHistoryWithDetails? = null,
    val deleting: Boolean = false, val operationFailed: Boolean = false,
    val healthRefreshing: Boolean = false, val healthError: Int? = null,
    val healthProvider: String = com.repforge.app.HealthJournalStore.HC, val healthStatus: String? = null
)

internal data class JournalEditState(val opened: NativeJournalEditor.Opened,
    val draft: NativeJournalEditor.Draft, val catalog: List<ExerciseLibraryEntity>,
    val saving: Boolean = false, val failed: Boolean = false)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = TrainPilotDatabase.getInstance(context)
    private val backupManager = BackupRestoreManager(context)
    private val repository = NativeJournalRepository(database)
    private val filters = MutableStateFlow(JournalFilter())
    private data class DetailRequest(val id: String, val revision: Long)
    private data class DetailResult(val request: DetailRequest?, val workout: WorkoutHistoryWithDetails?, val error: Boolean = false)
    private val selected = MutableStateFlow<DetailRequest?>(null)
    private var requestRevision = 0L
    private var routeRevision = 0L
    private var healthRevision = 0L
    private var healthJob: kotlinx.coroutines.Job? = null
    private val _uiState = MutableStateFlow(JournalUiState())
    private var editRevision = 0L
    private val _editor = MutableStateFlow<JournalEditState?>(null)
    internal val editor: StateFlow<JournalEditState?> = _editor.asStateFlow()
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()
    val workouts = filters.debounce { if (it.query.isBlank()) 0L else 180L }.flatMapLatest { repository.pages(it) }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            selected.flatMapLatest { request ->
                if (request == null) flowOf(DetailResult(null, null))
                else database.workoutHistoryDao().observeWorkoutDetails(request.id)
                    .map { DetailResult(request, it) }
                    .catch { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        emit(DetailResult(request, null, true))
                    }
            }.collect { result ->
                if (selected.value == result.request) _uiState.update { it.copy(detail = result.workout,
                    detailLoading = false, detailError = result.error) }
            }
        }
    }
    fun setSearch(value: String) {
        _uiState.update { it.copy(query = value) }
        filters.value = filters.value.copy(query = value, zone = java.time.ZoneId.systemDefault())
    }
    fun setMonth(value: java.time.YearMonth?) {
        _uiState.update { it.copy(month = value) }
        filters.value = filters.value.copy(month = value, zone = java.time.ZoneId.systemDefault())
    }
    fun openDetails(id: String) {
        if (_editor.value?.saving == true) return
        cancelEditor()
        cancelHealthRefresh()
        closeRouteMap()
        _uiState.update { it.copy(selectedWorkoutId = id, detail = null, detailLoading = true, detailError = false) }
        selected.value = DetailRequest(id, ++requestRevision)
    }
    fun closeDetails() {
        if (_uiState.value.deleting || _editor.value?.saving == true) return
        cancelEditor()
        cancelHealthRefresh()
        selected.value = null
        closeRouteMap()
        _uiState.update { it.copy(selectedWorkoutId = null, detail = null, detailLoading = false, detailError = false) }
    }
    internal fun openEditor() {
        val detail = _uiState.value.detail ?: return
        if (_uiState.value.deleting || _editor.value != null) return
        val revision = ++editRevision
        viewModelScope.launch {
            try {
                val state = withContext(Dispatchers.IO) {
                    com.repforge.app.NativeDataReplacement.mutate(context) {
                        database.withTransaction {
                            val latest = database.workoutHistoryDao().getWorkoutWithDetailsById(detail.workout.id)
                                ?: error("Workout no longer exists")
                            JournalEditState(NativeJournalEditor.open(context, latest), NativeJournalEditor.from(latest),
                                database.exerciseLibraryDao().getAllExercises())
                        }
                    }
                }
                if (revision == editRevision && _uiState.value.selectedWorkoutId == detail.workout.id) _editor.value = state
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { if (revision == editRevision) _uiState.update { it.copy(operationFailed = true) } }
        }
    }
    internal fun editDraft(draft: NativeJournalEditor.Draft) {
        _editor.update { state -> state?.takeIf { !it.saving }?.copy(draft = draft, failed = false) ?: state }
    }
    internal fun cancelEditor() {
        if (_editor.value?.saving == true) return
        editRevision++
        _editor.value = null
    }
    internal fun saveEditor(draft: NativeJournalEditor.Draft) {
        val state = _editor.value ?: return
        if (state.saving) return
        _editor.value = state.copy(draft = draft, saving = true, failed = false)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { NativeJournalEditor.save(context, database, state.opened, draft) }
                _editor.value = null
                closeRouteMap()
                runCatching { WearSyncRepository.publishWatchHomeSnapshot(context) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _editor.update { it?.copy(saving = false, failed = true) } }
        }
    }
    private fun cancelHealthRefresh() {
        healthRevision++
        healthJob?.cancel()
        _uiState.update { it.copy(healthRefreshing = false, healthError = null) }
    }
    fun requestSamsungWorkoutPermission(activity: android.app.Activity) {
        val state = _uiState.value
        if (state.healthProvider != com.repforge.app.HealthJournalStore.SAMSUNG || state.healthRefreshing) return
        val id = state.selectedWorkoutId ?: return
        val revision = ++healthRevision
        _uiState.update { it.copy(healthRefreshing = true, healthStatus = null) }
        healthJob = viewModelScope.launch {
            val result = com.repforge.app.SamsungHealthRepository.requestReadPermissions(activity)
            if (revision != healthRevision || _uiState.value.selectedWorkoutId != id ||
                _uiState.value.healthProvider != com.repforge.app.HealthJournalStore.SAMSUNG) return@launch
            _uiState.update { it.copy(healthRefreshing = false) }
            if (result.isSuccess) refreshWorkoutHealth(activity)
            else _uiState.update { it.copy(healthStatus = "Samsung Health: ${com.repforge.app.SamsungHealthRepository.errorCode(result.exceptionOrNull())}") }
        }
    }

    fun selectWorkoutHealthProvider(provider: String) {
        com.repforge.app.NativeWorkoutHealthSource.requireProvider(provider)
        if (provider == _uiState.value.healthProvider) return
        cancelHealthRefresh()
        _uiState.update { it.copy(healthProvider = provider, healthStatus = null) }
    }

    fun refreshWorkoutHealth(activity: android.app.Activity?) {
        val detail = _uiState.value.detail ?: return
        if (_uiState.value.healthRefreshing || _uiState.value.deleting || _editor.value?.saving == true) return
        if (activity == null) { _uiState.update { it.copy(healthError = R.string.journal_health_unavailable) }; return }
        val revision = ++healthRevision
        val provider = _uiState.value.healthProvider
        _uiState.update { it.copy(healthRefreshing = true, healthError = null, healthStatus = null) }
        healthJob = viewModelScope.launch {
            val result = NativeWorkoutHealth(context, database).refresh(activity, detail.workout, provider)
            if (revision == healthRevision && _uiState.value.selectedWorkoutId == detail.workout.id) {
                val error = result.exceptionOrNull()?.let {
                    when (it) {
                        is SecurityException -> if (provider == com.repforge.app.HealthJournalStore.SAMSUNG) R.string.journal_samsung_permission else R.string.journal_health_permission
                        is UnsupportedOperationException -> if (provider == com.repforge.app.HealthJournalStore.SAMSUNG) R.string.journal_samsung_unavailable else R.string.journal_health_unavailable
                        else -> R.string.journal_health_refresh_error
                    }
                }
                _uiState.update { it.copy(healthRefreshing = false, healthError = error,
                    healthStatus = if (result.isSuccess) "${com.repforge.app.NativeWorkoutHealthSource.label(provider)}: lekérdezés befejezve"
                        else result.exceptionOrNull()?.let { failure ->
                            if (provider == com.repforge.app.HealthJournalStore.SAMSUNG)
                                "Samsung Health: ${com.repforge.app.SamsungHealthRepository.errorCode(failure)} • ${failure.message.orEmpty().take(180)}"
                            else failure.message?.take(180)
                        }) }
            }
        }
    }
    fun openRouteMap(keys: List<String>) {
        val request = ++routeRevision
        val workoutId = _uiState.value.selectedWorkoutId
        _uiState.update { it.copy(routeLoading = true, routeError = false) }
        viewModelScope.launch {
            val coordinates = withContext(Dispatchers.IO) { WorkoutRouteCoordinates.loadAll(context, keys) }
            if (request == routeRevision && workoutId == _uiState.value.selectedWorkoutId) _uiState.update {
                it.copy(activeRoute = coordinates, routeLoading = false, routeError = coordinates == null)
            }
        }
    }
    fun closeRouteMap() {
        routeRevision++
        _uiState.update { it.copy(activeRoute = null, routeLoading = false, routeError = false) }
    }
    fun requestDeleteWorkout() { _uiState.update { it.copy(workoutToDelete = it.detail, operationFailed = false) } }
    fun cancelDeleteWorkout() { if (!_uiState.value.deleting) _uiState.update { it.copy(workoutToDelete = null) } }
    fun clearOperationError() { _uiState.update { it.copy(operationFailed = false) } }
    fun confirmDeleteWorkout() {
        val target = _uiState.value.workoutToDelete ?: return
        if (_uiState.value.deleting) return
        _uiState.update { it.copy(deleting = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    com.repforge.app.NativeDataReplacement.mutate(context) {
                    database.withTransaction {
                        val current = database.workoutHistoryDao().getWorkoutWithDetails(target.workout.id) ?: return@withTransaction
                        val w = current.workout
                        val aliases = listOf(w.id, w.syncId, w.workoutKey).filter { it.isNotBlank() }.distinct()
                        database.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(primaryId = w.id,
                            workoutKey = w.workoutKey, idsJson = JSONArray(aliases).toString(), deletedAt = System.currentTimeMillis()))
                        database.workoutHistoryDao().deleteWorkoutByIdOrKey(w.id)
                    }
                    // Private route cleanup follows durable deletion; an SQL failure never removes a live route.
                    if (target.workout.ownershipProtocol193 == 1) runCatching {
                        GpsRouteLedger193.open(context.noBackupFilesDir).deleteWorkout(target.workout.id)
                    }
                    }
                }
                _uiState.update { it.copy(deleting = false, workoutToDelete = null) }
                closeDetails()
                runCatching { WearSyncRepository.publishWatchHomeSnapshot(context) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(deleting = false, operationFailed = true) } }
        }
    }
    fun createCalendarIntent(workout: WorkoutHistoryWithDetails): Intent {
        val w = workout.workout
        return backupManager.createCalendarInsertIntent(title = "TrainPilot: ${w.programName} • ${w.dayName}".trim(' ', '•'),
            description = "", startMs = w.startedEpochMs, endMs = w.finishedEpochMs)
    }
    fun parsePrRecords(rawJson: String): List<PrRecordUiModel> = runCatching {
        val arr = JSONArray(rawJson)
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { obj ->
            PrRecordUiModel(obj.optString("exerciseId"), obj.optString("exerciseName"), obj.optString("kind"),
                obj.optString("value"), obj.optString("previous"))
        } }
    }.getOrDefault(emptyList())
}

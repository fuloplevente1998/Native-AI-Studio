package com.repforge.app.ui.coach

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeDataReplacement
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

internal data class WorkoutCoachAdviceState(
    val exerciseId: String = "", val loading: Boolean = true, val error: Boolean = false,
    val row: CoachExerciseAdvice? = null
)

/** Read-only active-exercise advice. Approval tickets remain owned by ActiveWorkoutViewModel. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class WorkoutCoachAdviceViewModel internal constructor(application: Application,
    private val db: TrainPilotDatabase) : AndroidViewModel(application) {
    constructor(application: Application) : this(application,
        TrainPilotDatabase.getInstance(application.applicationContext))
    private val app = application.applicationContext
    private val settings = TrainPilotSettingsStore(app)
    private val selectedExercise = MutableStateFlow("")
    private val refresh = MutableStateFlow(0L)
    private val _state = MutableStateFlow(WorkoutCoachAdviceState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Ignore ticking timers and set edits: advice depends on persisted history and target.
            val target = ActiveWorkoutStore.snapshotFlow.map(::targetFrame).distinctUntilChanged()
            val inputs = combine(selectedExercise, target, settings.settingsFlow) { id, frame, prefs ->
                Triple(id, frame, prefs)
            }.distinctUntilChanged()
            val changes = db.invalidationTracker.createFlow("workout_history", "workout_history_exercises",
                "workout_history_sets", "history_tombstones", "exercise_library", "programs", "program_days")
            var renderedContext: Pair<String?, com.repforge.app.data.store.TrainPilotUserSettings>? = null
            refresh.flatMapLatest {
                combine(inputs, changes) { input, _ -> input }.catch {
                    // Settings/observer failures are read errors too. Refresh resubscribes.
                    _state.value = WorkoutCoachAdviceState(selectedExercise.value, loading = false, error = true)
                }
            }.collectLatest { (id, raw, prefs) ->
                val retained = _state.value.row.takeIf {
                    _state.value.exerciseId == id && renderedContext == (raw to prefs)
                }
                _state.value = WorkoutCoachAdviceState(id, row = retained)
                if (id.isBlank() || raw == null) {
                    _state.value = WorkoutCoachAdviceState(id, loading = false)
                    return@collectLatest
                }
                try {
                    val row = withContext(Dispatchers.IO) {
                        NativeDataReplacement.mutate(app) {
                            val now = Instant.now()
                            val selected = NativeSelectedCoachReadings.load(app, db, now, ZoneId.systemDefault())
                            val advice = db.withTransaction {
                                NativeCoachAdviceRepository(db).loadActiveExercise(JSONObject(raw), id, prefs,
                                    selected.readiness, now)
                            }
                            check(HealthJournalStore.get(app).preferences().optString("provider", HealthJournalStore.HC) == selected.provider)
                            advice
                        }
                    }
                    // A selection change may arrive while Room is reading. Never publish its old advice.
                    if (selectedExercise.value == id && targetFrame(ActiveWorkoutStore.snapshotFlow.value) == raw) {
                        renderedContext = raw to prefs
                        _state.value = WorkoutCoachAdviceState(id, loading = false, row = row)
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { _state.value = WorkoutCoachAdviceState(id, loading = false, error = true) }
            }
        }
    }

    fun selectExercise(id: String) { selectedExercise.value = id }
    fun reload() { refresh.value += 1 }

    companion object {
        /** Stable context only; no copied timer, completion, weight or measured set state. */
        internal fun targetFrame(frame: JSONObject?): String? {
            if (frame?.optBoolean("active") != true) return null
            val active = frame.optJSONArray("exercises")?.optJSONObject(frame.optInt("currentExercise")) ?: return null
            val target = JSONObject()
            for (key in listOf("id", "prescription", "loadType", "repUnit", "measurementType", "targetReps"))
                if (active.has(key)) target.put(key, active.get(key))
            val context = JSONObject().put("active", true).put("currentExercise", 0)
                .put("exercises", org.json.JSONArray().put(target))
            for (key in listOf("workoutId", "generation", "programId", "dayId", "workout", "quickWorkout", "type"))
                if (frame.has(key)) context.put(key, frame.get(key))
            return context.toString()
        }
    }
}

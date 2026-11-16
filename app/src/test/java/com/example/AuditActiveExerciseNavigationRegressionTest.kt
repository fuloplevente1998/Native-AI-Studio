package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.SubScreen
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.PhoneWorkoutExecutionStore
import com.repforge.app.IntervalTimerService
import com.repforge.app.NativeSetStopwatch
import com.repforge.app.NativeWorkoutTarget
import com.repforge.app.ui.workout.ActiveWorkoutViewModel
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class AuditActiveExerciseNavigationRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private val parent = SupervisorJob()
    private val scope = CoroutineScope(parent + Dispatchers.Main.immediate)

    @Before fun prepare() {
        val store = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (store.get(null) as? PhoneWorkoutExecutionStore)?.close(); store.set(null, null)
        val room = TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        (room.get(null) as? TrainPilotDatabase)?.close()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        room.set(null, db)
        native { db.exerciseLibraryDao().upsertExercises(TrainPilotDatabase.defaultExercises()) }
    }

    @After fun close() {
        parent.cancel(); await { parent.isCompleted }
        db.close()
        TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }.set(null, null)
    }

    private fun model() = TrainPilotViewModel().also {
        it.bindNativeEnvironment(context, TrainPilotSettingsStore(context), db, scope)
        await { !it.uiState.value.savedRecordsLoading }
    }

    private fun started(): TrainPilotViewModel {
        val vm = model()
        val exercises = vm.uiState.value.exercises.filter { !it.isTimed && !it.isBilateral }.take(2)
        assertEquals(2, exercises.size)
        vm.startWorkoutWithExercises(exercises)
        await { ActiveWorkoutStore.current(context)?.let { frame -> frame.optBoolean("active") &&
            vm.uiState.value.activeWorkoutId == frame.optString("workoutId") && vm.uiState.value.isWorkoutSessionActive } == true }
        return vm
    }

    @Suppress("UNCHECKED_CAST")
    private fun stateFlow(vm: TrainPilotViewModel): MutableStateFlow<TrainPilotUiState> =
        TrainPilotViewModel::class.java.getDeclaredField("_uiState").apply { isAccessible = true }
            .get(vm) as MutableStateFlow<TrainPilotUiState>

    private fun assertNavigationRejected(vm: TrainPilotViewModel) {
        val before = vm.uiState.value
        val durable = ActiveWorkoutStore.current(context)?.toString()
        listOf<() -> Unit>({ vm.nextActiveExercise() }, { vm.previousActiveExercise() }).forEach { action ->
            action(); shadowOf(Looper.getMainLooper()).idle()
            val after = vm.uiState.value
            assertEquals(before.activeExerciseIndex, after.activeExerciseIndex)
            assertEquals(before.selectedExerciseId, after.selectedExerciseId)
            assertEquals(before.activeWorkoutExercises, after.activeWorkoutExercises)
            assertEquals(before.activeSets, after.activeSets)
            assertEquals(before.activeSetsByExerciseId, after.activeSetsByExerciseId)
            assertEquals(before.currentSubScreen, after.currentSubScreen)
            assertEquals(durable, ActiveWorkoutStore.current(context)?.toString())
        }
    }

    @Test fun watchOwnerRejectsNextAndPrevious() {
        val vm = started()
        ActiveWorkoutStore.saveOwned(context, JSONObject(ActiveWorkoutStore.current(context)!!.toString()).put("timerOwner", "watch"))
        PhoneWorkoutExecution.store(context).suspendCommands(true)
        await { !vm.uiState.value.phoneControlsWorkout }
        assertNavigationRejected(vm)
    }

    @Test fun pendingHandoffAndSavingRejectNextAndPrevious() {
        val vm = started()
        val flow = stateFlow(vm)
        flow.value = flow.value.copy(watchControlPending = true)
        assertNavigationRejected(vm)
        flow.value = flow.value.copy(watchControlPending = false, isSavingWorkout = true)
        assertNavigationRejected(vm)
    }

    @Test fun suspendedNativeCommandsRejectNavigationBeforeUiOwnershipChanges() {
        val vm = started()
        assertTrue(vm.uiState.value.phoneControlsWorkout)
        PhoneWorkoutExecution.store(context).suspendCommands(true)
        assertNavigationRejected(vm)
    }

    @Test fun noSessionOrEmptyWorkoutCannotFabricateSelectionFromCatalog() {
        val vm = model()
        assertTrue(vm.uiState.value.exercises.isNotEmpty())
        assertNavigationRejected(vm)
        val flow = stateFlow(vm)
        flow.value = flow.value.copy(isWorkoutSessionActive = true, activeWorkoutExercises = emptyList())
        assertNavigationRejected(vm)
    }

    @Test fun nextAndPreviousSelectExistingRowsAndPreserveRecordedValues() {
        val vm = started()
        vm.updateSetValuesExact(1, 23f, 8, 1)
        await { ActiveWorkoutStore.current(context)!!.getJSONArray("exercises").getJSONObject(0)
            .getJSONArray("sets").getJSONObject(0).optDouble("weight") == 23.0 }
        val original = ActiveWorkoutStore.current(context)!!
        val rows = original.getJSONArray("exercises").toString()
        val first = original.getJSONArray("exercises").getJSONObject(0).getString("id")
        val second = original.getJSONArray("exercises").getJSONObject(1).getString("id")
        vm.nextActiveExercise()
        await { vm.uiState.value.selectedExerciseId == second }
        var frame = ActiveWorkoutStore.current(context)!!
        assertEquals(1, frame.getInt("currentExercise")); assertEquals(rows, frame.getJSONArray("exercises").toString())
        vm.previousActiveExercise()
        await { vm.uiState.value.selectedExerciseId == first }
        frame = ActiveWorkoutStore.current(context)!!
        assertEquals(0, frame.getInt("currentExercise")); assertEquals(rows, frame.getJSONArray("exercises").toString())
        assertEquals(original.getString("workoutId"), frame.getString("workoutId"))
        assertEquals(original.getLong("generation"), frame.getLong("generation"))
    }

    @Test fun lastRunningTimedMeasurementPausesBeforeSummary() {
        val vm = model()
        val exercise = vm.uiState.value.exercises.first { it.measurementType == "time" && it.isTimed && !it.isBilateral }
        vm.startQuickTimedWithExercise(exercise)
        await { ActiveWorkoutStore.current(context)?.let { it.optBoolean("active") &&
            vm.uiState.value.activeWorkoutId == it.optString("workoutId") && vm.uiState.value.isWorkoutSessionActive } == true }
        val frame = ActiveWorkoutStore.current(context)!!
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            service.get().onStartCommand(Intent(context, IntervalTimerService::class.java).apply {
                action = IntervalTimerService.ARM
                putExtra("workoutId", frame.getString("workoutId")); putExtra("generation", frame.getLong("generation"))
            }, 0, 1)
            val row = frame.getJSONArray("exercises").getJSONObject(0)
            val target = NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"),
                row.getString("id"), row.getJSONArray("sets").getJSONObject(0).getString("setId"))
            val measurement = ActiveWorkoutViewModel(ApplicationProvider.getApplicationContext<Application>())
            await { measurement.uiState.value.exercises.isNotEmpty() }
            measurement.startSetStopwatch(target, "single")
            await { measurement.uiState.value.stopwatchRunning }
            vm.nextActiveExercise()
            await { vm.uiState.value.currentSubScreen == SubScreen.WORKOUT_SUMMARY }
            val paused = PhoneWorkoutExecution.store(context).snapshot()!!
            assertTrue(paused.optBoolean("workoutPaused"))
            assertFalse(NativeSetStopwatch.running(paused.getJSONObject(NativeSetStopwatch.FIELD)))
        } finally { service.destroy() }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            check(System.nanoTime() < deadline) { "Studio session timed out" }
            Thread.sleep(5)
        }
    }

    private fun <T> native(block: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { block() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) { "Native operation timed out" } }
            }
        } finally { future.cancel(true); worker.shutdownNow() }
    }
}

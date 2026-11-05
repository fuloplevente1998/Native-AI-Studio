package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.SubScreen
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.*
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.workout.ActiveWorkoutViewModel
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class StudioSessionMeasurementRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private val parent = SupervisorJob()
    private val scope = CoroutineScope(parent + Dispatchers.Main.immediate)
    private val models = ViewModelStore()

    @Before fun prepare() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        native { db.exerciseLibraryDao().upsertExercises(TrainPilotDatabase.defaultExercises()) }
    }

    @After fun close() {
        models.clear(); parent.cancel(); shadowOf(Looper.getMainLooper()).idle(); db.close()
    }

    private fun model() = TrainPilotViewModel().also {
        it.bindNativeEnvironment(context, TrainPilotSettingsStore(context), db, scope)
        await { !it.uiState.value.savedRecordsLoading }
    }

    private fun nativeModel() = ActiveWorkoutViewModel(ApplicationProvider.getApplicationContext<Application>()).also {
        models.put("measurement", it)
        await { it.uiState.value.exercises.isNotEmpty() }
    }

    private fun timed(model: TrainPilotViewModel) = model.uiState.value.exercises.first { it.isBilateral }
    private fun strength(model: TrainPilotViewModel) = model.uiState.value.exercises.first { !it.isTimed && !it.isBilateral }
    private fun target(frame: JSONObject): NativeWorkoutTarget {
        val exercise = frame.getJSONArray("exercises").getJSONObject(frame.optInt("currentExercise"))
        return NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"),
            exercise.getString("id"), exercise.getJSONArray("sets").getJSONObject(0).getString("setId"))
    }

    @Test fun repeatedQuickTimedStartKeepsTheCanonicalWorkoutAndStableSetIds() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model))
        await { ActiveWorkoutStore.current(context)?.optBoolean("active") == true }
        val first = ActiveWorkoutStore.current(context)!!
        model.startQuickTimedWithExercise(timed(model))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(first.getString("workoutId"), model.uiState.value.activeWorkoutId)
        val current = ActiveWorkoutStore.current(context)!!
        assertEquals(first.getLong("generation"), current.getLong("generation"))
        assertEquals(target(first), target(current))
        assertEquals(current.getString("workoutId"), PhoneWorkoutExecution.store(context).snapshot()!!.getString("workoutId"))
    }

    @Test fun timedStartWhileStrengthIsActiveReopensThatSessionWithoutPhantomUi() {
        val model = model()
        model.startQuickStrengthWithExercise(strength(model))
        await { model.uiState.value.isWorkoutSessionActive }
        val first = ActiveWorkoutStore.current(context)!!
        model.startQuickTimedWithExercise(timed(model))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(first.getString("workoutId"), model.uiState.value.activeWorkoutId)
        assertEquals(SubScreen.ACTIVE_STRENGTH, model.uiState.value.currentSubScreen)
        assertEquals(target(first), target(ActiveWorkoutStore.current(context)!!))
    }

    @Test fun swappingBilateralExerciseKeepsTheNativeAndDisplayedMeasurementMetadataTogether() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model))
        await { ActiveWorkoutStore.current(context)?.optBoolean("active") == true }
        val before = ActiveWorkoutStore.current(context)!!
        val replacement = strength(model)
        model.switchCurrentActiveExercise(replacement)
        shadowOf(Looper.getMainLooper()).idle()
        val after = ActiveWorkoutStore.current(context)!!
        assertEquals(before.getString("workoutId"), after.getString("workoutId"))
        assertEquals(before.getLong("generation"), after.getLong("generation"))
        assertEquals(replacement.id, target(after).exerciseId)
        assertEquals(replacement.measurementType, after.getJSONArray("exercises").getJSONObject(0).getString("measurementType"))
        assertEquals(SubScreen.ACTIVE_STRENGTH, model.uiState.value.activeWorkoutSubScreen)
    }

    @Test fun repeatedNativeStartCannotImplicitlyStopOrRecordTheRunningSide() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model))
        await { ActiveWorkoutStore.current(context)?.optBoolean("active") == true }
        val frame = ActiveWorkoutStore.current(context)!!
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            arm(service.get(), frame)
            val target = target(frame)
            val started = NativeWorkoutEditing.startStopwatch(context, target, "left")
            val clockId = started.getJSONObject(NativeSetStopwatch.FIELD).getString("id")
            ShadowSystemClock.advanceBy(Duration.ofSeconds(2))
            val repeated = NativeWorkoutEditing.startStopwatch(context, target, "left")
            assertEquals(clockId, repeated.getJSONObject(NativeSetStopwatch.FIELD).getString("id"))
            assertTrue(NativeSetStopwatch.running(repeated.getJSONObject(NativeSetStopwatch.FIELD)))
            assertTrue(NativeSetStopwatch.elapsed(repeated.getJSONObject(NativeSetStopwatch.FIELD)) >= 2_000)
            assertFalse(target.resolve(repeated).set.optBoolean("done"))
        } finally { service.destroy() }
    }

    @Test fun closingNativeWorkoutClearsBothStudioAndMeasurementProjections() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model))
        await { ActiveWorkoutStore.current(context)?.optBoolean("active") == true }
        val measurement = nativeModel()
        val before = ActiveWorkoutStore.current(context)!!
        native { TrainPilotWorkoutFinalization(context, db).discardWorkout(before) }
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(model.uiState.value.isWorkoutSessionActive)
        assertEquals("", model.uiState.value.activeWorkoutId)
        assertEquals("", measurement.uiState.value.workoutId)
        assertTrue(measurement.uiState.value.exercises.isEmpty())
        assertFalse(PhoneWorkoutExecution.store(context).snapshot()!!.optBoolean("active"))
    }

    private fun arm(service: IntervalTimerService, frame: JSONObject) {
        service.onStartCommand(Intent(context, IntervalTimerService::class.java).apply {
            action = IntervalTimerService.ARM
            putExtra("workoutId", frame.getString("workoutId")); putExtra("generation", frame.getLong("generation"))
        }, 0, 1)
        assertTrue(IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation")))
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
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) { "Native session operation timed out" } }
            }
        } finally { future.cancel(true); worker.shutdownNow() }
    }
}

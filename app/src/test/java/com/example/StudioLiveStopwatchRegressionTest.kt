package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.localization.TrainPilotStrings
import com.example.ui.screens.ActiveBilateralWorkoutScreen
import com.example.ui.screens.ActiveTimedWorkoutScreen
import com.example.ui.theme.TrainPilotTheme
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
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** Exercises the real elapsedRealtime -> native model -> Studio readout path before Stop. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class StudioLiveStopwatchRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<Application>()
    private val context: Context get() = app
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private val parent = SupervisorJob()
    private val scope = CoroutineScope(parent + Dispatchers.Main.immediate)
    private val owner = object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    @Before fun prepare() {
        val execution = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (execution.get(null) as? PhoneWorkoutExecutionStore)?.close(); execution.set(null, null)
        val room = TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        (room.get(null) as? TrainPilotDatabase)?.close()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        room.set(null, db)
        settings = TrainPilotSettingsStore(context, publishTheme = {})
        native {
            settings.resetAllSettingsForEraseLocal()
            db.exerciseLibraryDao().upsertExercises(TrainPilotDatabase.defaultExercises())
        }
    }

    @After fun close() {
        owner.viewModelStore.clear()
        parent.cancel(); await { parent.isCompleted }
        db.close()
        TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }.set(null, null)
    }

    @Test fun singleReadoutChangesWhileRunningThenPauseFreezesAndRecordStoresOnlyElapsedWork() {
        val studio = model()
        val exercise = studio.uiState.value.exercises.first {
            it.isTimed && !it.isBilateral && it.measurementType == "time"
        }
        studio.startQuickTimedWithExercise(exercise)
        awaitStarted(studio)
        val frame = ActiveWorkoutStore.current(context)!!
        val target = target(frame)
        val initialSet = target.resolve(frame).set.toString()
        val measurement = measurement()
        val controller = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            arm(controller.get(), frame)
            render(studio, bilateral = false)
            readout("timed_live_readout").assertTextEquals("00:00").assertIsDisplayed()
            assertReadable("timed_live_readout", minimumFontSp = 32f)
            compose.onNodeWithTag("toggle_timed_stopper").performClick()
            await { measurement.uiState.value.stopwatchRunning }
            val clockId = clock().getString("id")

            advance(2)
            assertEquals(2L, measurement.uiState.value.stopwatchSeconds)
            assertTrue(measurement.uiState.value.stopwatchRunning)
            readout("timed_live_readout").assertTextEquals("00:02").assertIsDisplayed()
            assertEquals(initialSet, target.resolve(PhoneWorkoutExecution.store(context).snapshot()!!).set.toString())

            advance(2)
            assertEquals(4L, measurement.uiState.value.stopwatchSeconds)
            readout("timed_live_readout").assertTextEquals("00:04").assertIsDisplayed()
            assertEquals(clockId, clock().getString("id"))
            compose.onNodeWithTag("toggle_timed_stopper").performClick()
            await { !measurement.uiState.value.stopwatchRunning }
            advance(5)
            assertEquals(4L, measurement.uiState.value.stopwatchSeconds)
            readout("timed_live_readout").assertTextEquals("00:04")
            assertFalse(target.resolve(PhoneWorkoutExecution.store(context).snapshot()!!).set.optBoolean("done"))

            compose.onNodeWithTag("toggle_timed_stopper").performClick()
            await { measurement.uiState.value.stopwatchRunning }
            advance(2)
            readout("timed_live_readout").assertTextEquals("00:06")
            compose.onNodeWithTag("record_timed_stopper").performScrollTo().performClick()
            await { target.resolve(PhoneWorkoutExecution.store(context).snapshot()!!).set.optBoolean("done") }
            val recorded = PhoneWorkoutExecution.store(context).snapshot()!!
            assertEquals("6", target.resolve(recorded).set.getString("reps"))
            assertFalse(NativeSetStopwatch.running(recorded.getJSONObject(NativeSetStopwatch.FIELD)))
            assertEquals(6L, NativeMeasuredTime.summary(recorded).workMs / 1000L)
        } finally { controller.destroy() }
    }

    @Test fun bothSideReadoutsAdvanceBeforeRecordingWithoutTheHeaderConsumingTheirWidth() {
        val studio = model()
        studio.startQuickTimedWithExercise(studio.uiState.value.exercises.first { it.id == "side-plank" })
        awaitStarted(studio)
        val frame = ActiveWorkoutStore.current(context)!!
        val target = target(frame)
        val measurement = measurement()
        val controller = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            arm(controller.get(), frame)
            render(studio, bilateral = true)
            for ((side, seconds) in listOf("left" to 2, "right" to 3)) {
                compose.onNodeWithTag("bilateral_${side}_start_pause_btn").performScrollTo().performClick()
                await { measurement.uiState.value.stopwatchRunning && measurement.uiState.value.stopwatchSide == side ||
                    measurement.uiState.value.statusMessage != null }
                assertTrue("Start $side: ${measurement.uiState.value.statusMessage}", measurement.uiState.value.stopwatchRunning)
                val clockId = clock().getString("id")
                advance(1)
                assertEquals(1L, measurement.uiState.value.stopwatchSeconds)
                readout("bilateral_${side}_live_readout").assertTextEquals("00:01").assertIsDisplayed()
                assertReadable("bilateral_${side}_live_readout", minimumFontSp = 20f)
                advance(seconds - 1)
                assertEquals(seconds.toLong(), measurement.uiState.value.stopwatchSeconds)
                assertTrue(measurement.uiState.value.stopwatchRunning)
                readout("bilateral_${side}_live_readout").assertTextEquals("00:0$seconds")
                assertEquals(clockId, clock().getString("id"))
                assertEquals(0, target.resolve(PhoneWorkoutExecution.store(context).snapshot()!!).set.optInt("${side}Seconds"))
                compose.onNodeWithTag("bilateral_${side}_stop_record_btn").performClick()
                await { !measurement.uiState.value.stopwatchRunning }
                val recorded = target.resolve(PhoneWorkoutExecution.store(context).snapshot()!!).set
                assertEquals(seconds, recorded.getInt("${side}Seconds"))
                assertEquals(side == "right", recorded.optBoolean("done"))
            }
            val recorded = PhoneWorkoutExecution.store(context).snapshot()!!
            assertEquals("2", target.resolve(recorded).set.getString("reps"))
            assertEquals(5L, NativeMeasuredTime.summary(recorded).workMs / 1000L)
        } finally { controller.destroy() }
    }

    private fun render(studio: TrainPilotViewModel, bilateral: Boolean) {
        compose.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                val state by studio.uiState.collectAsState()
                val strings = TrainPilotStrings.forLanguage(state.language)
                TrainPilotTheme {
                    if (bilateral) ActiveBilateralWorkoutScreen(state, strings,
                        onBack = {}, onOpenSettings = {}, onSelectMeasurementMode = {})
                    else ActiveTimedWorkoutScreen(state, strings,
                        onBack = {}, onOpenSettings = {}, onSelectMeasurementMode = {})
                }
            }
        }
    }

    private fun readout(tag: String) = compose.onNode(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true)

    private fun assertReadable(tag: String, minimumFontSp: Float) {
        val layouts = mutableListOf<TextLayoutResult>()
        readout(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertTrue("Counter must retain the existing large typography", layout.layoutInput.style.fontSize.value >= minimumFontSp)
        assertFalse("Counter digits must fit on one line: size=${layout.size}, input=${layout.layoutInput.constraints}, width=${layout.didOverflowWidth}, height=${layout.didOverflowHeight}", layout.hasVisualOverflow)
        assertEquals(1, layout.lineCount)
    }

    private fun model() = TrainPilotViewModel().also {
        it.bindNativeEnvironment(context, settings, db, scope)
        await { !it.uiState.value.savedRecordsLoading && it.uiState.value.exercises.isNotEmpty() }
    }

    private fun measurement() = ViewModelProvider(owner, ViewModelProvider.AndroidViewModelFactory.getInstance(app))[ActiveWorkoutViewModel::class.java]
        .also { model -> await { model.uiState.value.exercises.isNotEmpty() } }

    private fun target(frame: JSONObject): NativeWorkoutTarget {
        val exercise = frame.getJSONArray("exercises").getJSONObject(frame.optInt("currentExercise"))
        return NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"), exercise.getString("id"),
            exercise.getJSONArray("sets").getJSONObject(0).getString("setId"))
    }

    private fun clock() = PhoneWorkoutExecution.store(context).snapshot()!!.getJSONObject(NativeSetStopwatch.FIELD)

    private fun arm(service: IntervalTimerService, frame: JSONObject) {
        service.onStartCommand(Intent(context, IntervalTimerService::class.java).apply {
            action = IntervalTimerService.ARM
            putExtra("workoutId", frame.getString("workoutId")); putExtra("generation", frame.getLong("generation"))
        }, 0, 1)
        assertTrue(IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation")))
    }

    private fun advance(seconds: Int) {
        // Advance Android's actual monotonic source, then let both the controller's
        // checkpoint and the model's existing projection run; no fake uiState writes.
        ShadowSystemClock.advanceBy(Duration.ofSeconds(seconds.toLong()))
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    private fun awaitStarted(model: TrainPilotViewModel) {
        await { ActiveWorkoutStore.current(context)?.let { it.optBoolean("active") &&
            model.uiState.value.activeWorkoutId == it.optString("workoutId") && model.uiState.value.isWorkoutSessionActive } == true }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            check(System.nanoTime() < deadline) { "Live stopwatch timed out" }
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
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) { "Native stopwatch setup timed out" } }
            }
        } finally { future.cancel(true); worker.shutdownNow() }
    }
}

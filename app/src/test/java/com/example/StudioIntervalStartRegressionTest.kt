package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.localization.TrainPilotStrings
import com.example.model.MainTab
import com.example.model.SubScreen
import com.example.ui.screens.ActiveTimedWorkoutScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.*
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.workout.ActiveWorkoutViewModel
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
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w800dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class StudioIntervalStartRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<Application>()
    private val context: Context get() = app
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private val parent = SupervisorJob()
    private val scope = CoroutineScope(parent + Dispatchers.Main.immediate)
    private val models = ViewModelStore()
    private val owner = object : ViewModelStoreOwner { override val viewModelStore = models }

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
        models.clear()
        parent.cancel(); await { parent.isCompleted }
        db.close()
        TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }.set(null, null)
    }

    private fun model() = TrainPilotViewModel().also {
        it.bindNativeEnvironment(context, settings, db, scope)
        await { !it.uiState.value.savedRecordsLoading && it.uiState.value.exercises.isNotEmpty() }
    }

    private fun measurement() = ViewModelProvider(owner, ViewModelProvider.AndroidViewModelFactory(app))[ActiveWorkoutViewModel::class.java]

    private fun render(vm: TrainPilotViewModel) {
        // An explicit current-Application factory avoids AndroidViewModelFactory's
        // process singleton retaining another Robolectric test's Application.
        measurement()
        compose.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                val state by vm.uiState.collectAsState()
                val strings = TrainPilotStrings.forLanguage(state.language)
                TrainPilotTheme {
                    when (state.currentSubScreen) {
                        SubScreen.INTERVAL_SETTINGS -> StudioIntervalSettingsRoute(vm, state, strings)
                        SubScreen.ACTIVE_TIMED -> ActiveTimedWorkoutScreen(state, strings,
                            onBack = { vm.navigateBack() }, onOpenSettings = {}, onSelectMeasurementMode = {},
                            consumeIntervalStart = vm::consumeIntervalStart)
                        else -> Unit
                    }
                }
            }
        }
    }

    @Test fun productionFormCommitsThenArmsAndStartsOnlyTheNativeIntervalClock() {
        val vm = model()
        val expected = vm.uiState.value.exercises.first {
            it.isTimed && !it.isBilateral && it.measurementType !in setOf("gps", "distance") && it.repUnit !in setOf("m", "km")
        }
        // Initial Studio selection is normally a strength exercise: Start must pick a real timed catalog entry.
        vm.navigateToSubScreen(SubScreen.INTERVAL_SETTINGS, MainTab.WORKOUT)
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            render(vm)
            compose.onNodeWithTag("studio_interval_work_plus").performClick()
            compose.onNodeWithTag("studio_interval_rest_minus").performClick()
            compose.onNodeWithTag("studio_interval_rounds_plus").performClick()
            compose.onNodeWithTag("studio_interval_sound").performClick()
            compose.onNodeWithTag("studio_interval_auto_advance").performClick()
            compose.onNodeWithTag("studio_interval_save_start").performScrollTo().performClick()
            await(details = { "route=${vm.uiState.value.currentSubScreen}, active=${vm.uiState.value.isWorkoutSessionActive}, pending=${vm.uiState.value.pendingIntervalStart}, banner=${vm.uiState.value.statusBannerMessage}, frame=${ActiveWorkoutStore.current(context)}" }) {
                compose.waitForIdle()
                vm.uiState.value.isWorkoutSessionActive && vm.uiState.value.pendingIntervalStart == null
            }
            val frame = requireNotNull(ActiveWorkoutStore.current(context))
            assertEquals(SubScreen.ACTIVE_TIMED, vm.uiState.value.currentSubScreen)
            assertEquals(expected.id, vm.uiState.value.selectedExerciseId)
            val saved = native { settings.current() }
            assertEquals(35, saved.workSeconds); assertEquals(85, saved.intervalRestSeconds)
            assertEquals(2, saved.rounds); assertFalse(saved.soundEnabled); assertFalse(saved.intervalAutoAdvance)
            assertEquals(saved.timerConfiguration().toString(), frame.getJSONObject("timerConfig").toString())
            assertFalse(IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation")))
            assertFalse(frame.has(NativeSetStopwatch.FIELD))
            // Deliver the actual visible editor's foreground ARM request, not a fabricated start command.
            var delivered = false
            while (true) {
                val intent = shadowOf(app).nextStartedService ?: break
                if (intent.action == IntervalTimerService.ARM) {
                    service.get().onStartCommand(intent, 0, 1); delivered = true
                }
            }
            assertTrue("The production editor must request ARM: status=${measurement().uiState.value.statusMessage}, nativeWorkout=${measurement().uiState.value.workoutId}, frame=${ActiveWorkoutStore.current(context)}", delivered)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(60))
            compose.waitUntil(10_000) { IntervalTimerService.snapshot(context).optBoolean("running") }
            val timer = IntervalTimerService.snapshot(context)
            assertEquals(frame.getString("workoutId"), timer.getString("workoutId"))
            assertFalse(timer.getBoolean("restOnly"))
            val step = timer.getJSONArray("steps").getJSONObject(0)
            assertEquals(35, step.getInt("work")); assertEquals(85, step.getInt("rest")); assertEquals(2, step.getInt("rounds"))
            assertFalse(PhoneWorkoutExecution.store(context).snapshot()!!.has(NativeSetStopwatch.FIELD))
            val set = frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0)
            assertFalse(set.getBoolean("done")); assertEquals("", set.getString("reps"))
            compose.onNodeWithTag("interval_options").assertExists()
            compose.onNodeWithTag("toggle_timed_stopper").assertDoesNotExist()
            val key = timer.getString("key")
            compose.runOnIdle { vm.toggleCompactPhoneMode(true) }
            assertEquals(key, IntervalTimerService.snapshot(context).getString("key"))
            compose.runOnIdle { vm.navigateToSubScreen(SubScreen.SETTINGS_MAIN) }
            compose.runOnIdle { vm.navigateToSubScreen(SubScreen.ACTIVE_TIMED) }
            assertNull(vm.uiState.value.pendingIntervalStart)
            assertTrue(ActiveWorkoutStore.current(context)!!.getBoolean("workoutPaused"))
            assertFalse(IntervalTimerService.snapshot(context).optBoolean("running"))
        } finally { service.destroy() }
    }

    @Test fun existingStrengthOrWatchOwnerIsNeverReplacedOrResumedByFormStart() {
        val vm = model()
        vm.startQuickStrengthWithExercise(vm.uiState.value.exercises.first { !it.isTimed && !it.isBilateral })
        await { vm.uiState.value.isWorkoutSessionActive }
        vm.pauseActiveWorkout()
        vm.navigateToSubScreen(SubScreen.INTERVAL_SETTINGS)
        val before = PhoneWorkoutExecution.store(context).snapshot()!!.toString()
        vm.saveAndStartIntervals(73, 17, 4, false, true, false)
        assertEquals(before, PhoneWorkoutExecution.store(context).snapshot()!!.toString())
        assertNotNull(vm.uiState.value.statusBannerMessage)
        ActiveWorkoutStore.saveOwned(context, JSONObject(ActiveWorkoutStore.current(context)!!.toString()).put("timerOwner", "watch"))
        PhoneWorkoutExecution.store(context).suspendCommands(true)
        val owned = PhoneWorkoutExecution.store(context).snapshot()!!.toString()
        vm.saveAndStartIntervals(73, 17, 4, false, true, false)
        assertEquals(owned, PhoneWorkoutExecution.store(context).snapshot()!!.toString())
        assertNull(vm.uiState.value.pendingIntervalStart)
    }

    @Test fun noTimedCatalogCannotCreateAFallbackStrengthSession() {
        val vm = model()
        // A regular catalog reload deliberately restores missing builtins. Exercise
        // the unavailable-catalog projection without asking the seeder to undo it.
        val strength = vm.uiState.value.exercises.first { !it.isTimed && !it.isBilateral }
        stateFlow(vm).value = vm.uiState.value.copy(exercises = listOf(strength), selectedExerciseId = strength.id)
        vm.navigateToSubScreen(SubScreen.INTERVAL_SETTINGS)
        vm.saveAndStartIntervals(73, 17, 4, false, true, false)
        assertNull(ActiveWorkoutStore.current(context))
        assertNotNull(vm.uiState.value.statusBannerMessage)
        assertNull(vm.uiState.value.pendingIntervalStart)
    }

    @Test fun failedPresetSaveCannotStartASessionOrLeaveTheForm() {
        val vm = model()
        vm.navigateToSubScreen(SubScreen.INTERVAL_SETTINGS)
        val before = native { settings.current().timerConfiguration().toString() }
        vm.saveAndStartIntervals(0, 17, 4, false, true, false)
        await { vm.uiState.value.statusBannerMessage != null }
        assertEquals(before, native { settings.current().timerConfiguration().toString() })
        assertNull(ActiveWorkoutStore.current(context)); assertNull(vm.uiState.value.pendingIntervalStart)
        assertEquals(SubScreen.INTERVAL_SETTINGS, vm.uiState.value.currentSubScreen)
    }

    @Test fun selectedValidTimedExerciseWinsAndRequireNewCannotRecoverEvenTheSameExercise() {
        val vm = model()
        val exercise = vm.uiState.value.exercises.last {
            it.isTimed && !it.isBilateral && it.measurementType !in setOf("gps", "distance") && it.repUnit !in setOf("m", "km")
        }
        stateFlow(vm).value = vm.uiState.value.copy(selectedExerciseId = exercise.id)
        vm.navigateToSubScreen(SubScreen.INTERVAL_SETTINGS)
        vm.saveAndStartIntervals(73, 17, 4, false, true, false)
        await { vm.uiState.value.pendingIntervalStart != null }
        val before = PhoneWorkoutExecution.store(context).snapshot()!!.toString()
        assertEquals(exercise.id, vm.uiState.value.pendingIntervalStart!!.exerciseId)
        val failed = native { runCatching {
            NativeWorkoutSession.startQuickExercises(context, listOf(exercise.id), "Other start", true, db, requireNew = true)
        }.isFailure }
        assertTrue(failed)
        assertEquals(before, PhoneWorkoutExecution.store(context).snapshot()!!.toString())
    }

    @Test fun pauseWhileWaitingForArmConsumesStartAndCannotReplayAfterResume() {
        val vm = model()
        vm.startQuickTimedWithExercise(vm.uiState.value.exercises.first { it.measurementType == "time" && !it.isBilateral })
        await { vm.uiState.value.isWorkoutSessionActive }
        val frame = ActiveWorkoutStore.current(context)!!
        val ex = frame.getJSONArray("exercises").getJSONObject(0)
        val target = NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"),
            ex.getString("id"), ex.getJSONArray("sets").getJSONObject(0).getString("setId"))
        val measurement = measurement()
        measurement.startIntervalsWhenReady(target)
        shadowOf(Looper.getMainLooper()).idle()
        vm.pauseActiveWorkout()
        vm.pauseActiveWorkout(false) // A transient pause still invalidates the captured explicit request.
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            while (true) {
                val intent = shadowOf(app).nextStartedService ?: break
                if (intent.action == IntervalTimerService.ARM) service.get().onStartCommand(intent, 0, 1)
            }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            assertNotNull(measurement.uiState.value.statusMessage)
            measurement.onEditorVisible()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            assertFalse(IntervalTimerService.snapshot(context).optBoolean("running"))
            assertFalse(PhoneWorkoutExecution.store(context).snapshot()!!.has(NativeSetStopwatch.FIELD))
        } finally { service.destroy() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun stateFlow(vm: TrainPilotViewModel) = TrainPilotViewModel::class.java.getDeclaredField("_uiState")
        .apply { isAccessible = true }.get(vm) as MutableStateFlow<TrainPilotUiState>

    private fun await(details: () -> String = { "" }, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            check(System.nanoTime() < deadline) { "Interval start timed out: ${details()}" }
            Thread.sleep(5)
        }
    }

    private fun <T> native(operation: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { operation() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) }
            }
        } finally { future.cancel(true); worker.shutdownNow() }
    }
}

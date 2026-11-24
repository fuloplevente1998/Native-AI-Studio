package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.PhoneWorkoutExecutionStore
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.data.store.TrainPilotUserSettings
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import com.example.ui.viewmodel.setFontScaleMultiplier
import com.example.ui.viewmodel.startQuickTimedWithExercise
import com.example.ui.viewmodel.toggleCompactPhoneMode
import com.example.ui.viewmodel.updateIntervalSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class AuditRemainingPreferencesViewModelTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private val parent = SupervisorJob()
    private val scope = CoroutineScope(parent + Dispatchers.Main.immediate)

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
        parent.cancel(); await { parent.isCompleted }
        db.close()
        TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }.set(null, null)
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

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            check(System.nanoTime() < deadline) { "ViewModel operation timed out" }
            Thread.sleep(5)
        }
    }

    private fun model() = TrainPilotViewModel().also {
        it.bindNativeEnvironment(context, settings, db, scope)
        await { !it.uiState.value.savedRecordsLoading && it.uiState.value.exercises.isNotEmpty() }
    }

    @Test fun appearanceSurvivesNewBoundViewModel() {
        val vm = model()
        vm.setFontScaleMultiplier(1.28f)
        vm.toggleCompactPhoneMode(true)
        await { native { settings.current() }.let { it.fontScaleMultiplier == 1.28f && it.compactPhoneMode } }
        val reloaded = model()
        await { reloaded.uiState.value.fontScaleMultiplier == 1.28f && reloaded.uiState.value.compactPhoneMode }
    }

    @Test fun committedIntervalPresetFeedsQuickTimedStartAndPartialChangesPreserveIt() {
        val vm = model()
        var committed: TrainPilotUserSettings? = null
        vm.updateIntervalSettings(73, 17, 4, false, true, false) {
            committed = runBlocking { settings.current() }
        }
        await { committed != null }
        val savedAtCallback = requireNotNull(committed)
        assertEquals(73, savedAtCallback.workSeconds)
        assertEquals(17, savedAtCallback.intervalRestSeconds)
        assertEquals(4, savedAtCallback.rounds)
        assertFalse(savedAtCallback.soundEnabled)
        assertTrue(savedAtCallback.vibrationEnabled)
        assertFalse(savedAtCallback.intervalAutoAdvance)
        val exercise = vm.uiState.value.exercises.first { it.measurementType == "time" && it.isTimed && !it.isBilateral }
        vm.startQuickTimedWithExercise(exercise)
        await { ActiveWorkoutStore.current(context)?.let { it.optBoolean("active") &&
            vm.uiState.value.activeWorkoutId == it.optString("workoutId") && vm.uiState.value.isWorkoutSessionActive } == true }
        val timer = ActiveWorkoutStore.current(context)!!.getJSONObject("timerConfig")
        assertEquals(73, timer.getInt("work")); assertEquals(17, timer.getInt("rest"))
        assertEquals(4, timer.getInt("rounds")); assertFalse(timer.getBoolean("sound"))
        assertTrue(timer.getBoolean("vibration")); assertFalse(timer.getBoolean("autoAdvance"))
        vm.toggleSoundAlert(true)
        vm.toggleVibrationAlert(false)
        vm.cycleDefaultRestTimer()
        await { native { settings.current() }.let { it.soundEnabled && !it.vibrationEnabled && it.restSeconds == 120 } }
        val after = native { settings.current() }
        assertEquals(73, after.workSeconds); assertEquals(17, after.intervalRestSeconds)
        assertEquals(4, after.rounds); assertFalse(after.intervalAutoAdvance)
    }

    @Test fun invalidAndUnboundSavesNeverAcknowledge() {
        val vm = model()
        val before = native { settings.current().timerConfiguration().toString() }
        var called = false
        vm.updateIntervalSettings(0, 17, 4, false, true, false) { called = true }
        await { vm.uiState.value.statusBannerMessage != null }
        assertFalse(called)
        assertEquals(before, native { settings.current().timerConfiguration().toString() })
        val unbound = TrainPilotViewModel()
        unbound.updateIntervalSettings(73, 17, 4, false, true, false) { called = true }
        assertFalse(called)
        assertNotNull(unbound.uiState.value.statusBannerMessage)
    }
}

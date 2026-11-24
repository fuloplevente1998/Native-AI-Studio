package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.SubScreen
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.PhoneWorkoutExecutionStore
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
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
import com.example.ui.viewmodel.navigateBack
import com.example.ui.viewmodel.navigateToSubScreen
import com.example.ui.viewmodel.pauseOutdoorMeasurement
import com.example.ui.viewmodel.startGpsTrackingSession
import com.example.ui.viewmodel.startOutdoorWorkout
import com.example.ui.viewmodel.startWorkoutWithExercises

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class AuditOutdoorPreviewRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)

    @Before fun prepare() {
        val store = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (store.get(null) as? PhoneWorkoutExecutionStore)?.close(); store.set(null, null)
        val instance = TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        (instance.get(null) as? TrainPilotDatabase)?.close()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        instance.set(null, db)
        native { db.exerciseLibraryDao().upsertExercises(TrainPilotDatabase.defaultExercises()) }
    }
    @After fun close() {
        job.cancel(); await { job.isCompleted }
        db.close()
        TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }.set(null, null)
    }
    private fun model() = TrainPilotViewModel().also {
        it.bindNativeEnvironment(context, TrainPilotSettingsStore(context), db, scope)
        await { !it.uiState.value.savedRecordsLoading }
    }
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            check(System.nanoTime() < deadline) { "Timed out waiting for native state" }
            Thread.sleep(10)
        }
    }
    private fun <T> native(block: suspend () -> T): T {
        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit<T> { runBlocking { block() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) }
            }
        } finally { future.cancel(true); executor.shutdownNow() }
    }
    private fun onMain(block: () -> Unit) = native { withContext(Dispatchers.Main.immediate) { block() } }

    @Test fun previewBackReopenCreatesNoNativeSessionOrClock() {
        val vm = model()
        val before = PhoneWorkoutExecution.store(context).snapshot()?.toString()
        repeat(2) {
            onMain { vm.navigateToSubScreen(SubScreen.ACTIVE_OUTDOOR_GPS) }
            val state = vm.uiState.value
            assertEquals(SubScreen.ACTIVE_OUTDOOR_GPS, state.currentSubScreen)
            assertFalse(state.isWorkoutSessionActive)
            assertEquals(0L, state.activeWorkoutGeneration)
            assertEquals(0L, state.workoutStartedEpochMs)
            assertTrue(ActiveWorkoutStore.current(context)?.optBoolean("active") != true)
            assertEquals(before, PhoneWorkoutExecution.store(context).snapshot()?.toString())
            onMain { vm.navigateBack() }
        }
    }

    @Test fun startTwiceAndReopenKeepThePausedNativeSession() {
        val vm = model()
        onMain { vm.navigateToSubScreen(SubScreen.ACTIVE_OUTDOOR_GPS); assertTrue(vm.startGpsTrackingSession()) }
        val first = ActiveWorkoutStore.current(context)!!
        val id = first.getString("workoutId")
        val generation = first.getLong("generation")
        onMain { assertTrue(vm.startGpsTrackingSession()); assertTrue(vm.pauseOutdoorMeasurement(true)); vm.navigateBack() }
        val paused = ActiveWorkoutStore.current(context)!!
        assertTrue(paused.getBoolean("workoutPaused"))
        onMain { vm.startOutdoorWorkout() }
        val reopened = ActiveWorkoutStore.current(context)!!
        assertEquals(id, reopened.getString("workoutId"))
        assertEquals(generation, reopened.getLong("generation"))
        assertEquals(paused.getLong("pauseRevision"), reopened.getLong("pauseRevision"))
        assertTrue(reopened.getBoolean("workoutPaused"))
        assertEquals(SubScreen.ACTIVE_OUTDOOR_GPS, vm.uiState.value.currentSubScreen)
    }

    @Test fun anotherActiveModeRejectsOutdoorStart() {
        val vm = model()
        val exercise = vm.uiState.value.exercises.first { !it.isTimed && !it.isBilateral }
        onMain { vm.startWorkoutWithExercises(listOf(exercise)) }
        await { ActiveWorkoutStore.current(context)?.optBoolean("active") == true && vm.uiState.value.isWorkoutSessionActive }
        val before = ActiveWorkoutStore.current(context)!!
        onMain { vm.startOutdoorWorkout() }
        val after = ActiveWorkoutStore.current(context)!!
        assertEquals(before.getString("workoutId"), after.getString("workoutId"))
        assertEquals(before.getLong("generation"), after.getLong("generation"))
        assertNotEquals(SubScreen.ACTIVE_OUTDOOR_GPS, vm.uiState.value.currentSubScreen)
        assertFalse(vm.uiState.value.statusBannerMessage.isNullOrBlank())
    }
}

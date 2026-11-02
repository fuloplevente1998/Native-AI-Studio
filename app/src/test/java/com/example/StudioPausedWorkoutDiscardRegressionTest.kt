package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.*
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.health.NativeHealthForegroundAdmission as Admission
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class StudioPausedWorkoutDiscardRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    @Before fun prepare() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
    }
    @After fun close() { db.close() }
    private fun <T> native(block: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { block() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) { "Native finalization timed out" } }
            }
        } finally { future.cancel(true); worker.shutdownNow() }
    }
    private fun start(): JSONObject = NativeWorkoutSession.installPrepared(context,
        NativeWorkoutSession.quickCandidate(JSONObject("""
            {"id":"press","name":"Press","repUnit":"ism.","measurementType":"reps","loadType":"weight",
             "sets":[{"setId":"admission-one","set":1,"reps":10,"weight":15,"done":false}]}
        """), TrainPilotUserSettings()))

    @Test fun studioDiscardUsesTheNativeFinalizerAndPersistsTheDeletion() {
        val frame = start()
        val parent = kotlinx.coroutines.SupervisorJob()
        val scope = kotlinx.coroutines.CoroutineScope(parent + kotlinx.coroutines.Dispatchers.Main.immediate)
        val model = TrainPilotViewModel()
        try {
            model.bindNativeEnvironment(context, com.repforge.app.data.store.TrainPilotSettingsStore(context), db, scope)
            val id = frame.getString("workoutId")
            await { model.uiState.value.activeWorkoutId == id && !model.uiState.value.savedRecordsLoading }
            model.stopActiveWorkoutSession(id)
            await { !model.uiState.value.isSavingWorkout && !model.uiState.value.isWorkoutSessionActive }
            assertFalse(ActiveWorkoutStore.current(context)!!.optBoolean("active"))
            assertFalse(PhoneWorkoutExecution.store(context).snapshot()!!.optBoolean("active"))
            assertTrue(native { db.workoutHistoryDao().isTombstoned(id, JSONObject.quote(id)) })
            assertTrue(Admission.safeOnMain(context))
        } finally { parent.cancel() }
    }
    @Test fun staleStudioDiscardIdCannotDeleteTheCurrentWorkout() {
        val frame = start()
        val parent = kotlinx.coroutines.SupervisorJob()
        val scope = kotlinx.coroutines.CoroutineScope(parent + kotlinx.coroutines.Dispatchers.Main.immediate)
        val model = TrainPilotViewModel()
        try {
            model.bindNativeEnvironment(context, com.repforge.app.data.store.TrainPilotSettingsStore(context), db, scope)
            val id = frame.getString("workoutId")
            await { model.uiState.value.activeWorkoutId == id && !model.uiState.value.savedRecordsLoading }
            model.stopActiveWorkoutSession("stale-other-workout")
            await { !model.uiState.value.isSavingWorkout }
            assertTrue(ActiveWorkoutStore.current(context)!!.optBoolean("active"))
            assertEquals(id, ActiveWorkoutStore.current(context)!!.getString("workoutId"))
            assertTrue(native { db.workoutHistoryDao().getTombstones().isEmpty() })
        } finally { parent.cancel() }
    }
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            check(System.nanoTime() < deadline) { "Studio discard timed out" }
            Thread.sleep(10)
        }
    }
}

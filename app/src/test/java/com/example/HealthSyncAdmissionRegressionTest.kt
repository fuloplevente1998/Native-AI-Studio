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
class HealthSyncAdmissionRegressionTest {
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

    @Test fun durableFinalizationUnblocksHealthWithoutDeletingTerminalFrames() {
        val frame = start()
        assertFalse(Admission.safeOnMain(context)); assertFalse(Admission.safeOffMain(context))
        native { TrainPilotWorkoutFinalization(context, db).finishWorkout(frame) }
        assertNotNull(native { db.workoutHistoryDao().getWorkoutWithDetails(frame.getString("workoutId")) })
        val execution = PhoneWorkoutExecution.store(context).snapshot()!!
        assertFalse(execution.getBoolean("active"))
        assertTrue(execution.optBoolean("workoutPaused"))
        val before = execution.toString()
        assertTrue(Admission.safeOnMain(context)); assertTrue(Admission.safeOffMain(context))
        assertEquals(before, PhoneWorkoutExecution.store(context).snapshot()!!.toString())
        assertEquals(frame.getString("workoutId"), ActiveWorkoutStore.current(context)!!.getString("workoutId"))
    }
    @Test fun pausedAndFailedSaveRemainBlockedWithAnActionableReason() {
        val frame = start()
        StudioWorkoutBridge.pause(context, true)
        assertEquals(Admission.Block.ACTIVE, Admission.mainBlockReason(context))
        assertFalse(Admission.safeOffMain(context))
        db.close()
        try { native { TrainPilotWorkoutFinalization(context, db).finishWorkout(frame) }; fail("Closed Room accepted a save") }
        catch (_: Exception) { }
        assertTrue(ActiveWorkoutStore.current(context)!!.optBoolean("finalizationPending"))
        assertTrue(PhoneWorkoutExecution.store(context).snapshot()!!.optBoolean("active"))
        assertEquals(Admission.Block.FINALIZING, Admission.mainBlockReason(context))
        assertFalse(Admission.safeOffMain(context))
    }
    @Test fun onlyExplicitInactiveTerminalSourcesAreProjectedAndAllOtherGatesRemainClosed() {
        val archived = JSONObject().put("active", false).put("workoutPaused", true).put("finalizationPending", true)
        val original = archived.toString()
        assertNull(Admission.admissionFrame(archived, true))
        assertSame(archived, Admission.admissionFrame(archived, false))
        assertEquals(original, archived.toString())
        val active = JSONObject(archived.toString()).put("active", true)
        assertSame(active, Admission.admissionFrame(active, true))
        val unknown = JSONObject().put("workoutPaused", true)
        assertSame(unknown, Admission.admissionFrame(unknown, true))
        assertFalse(Admission.eligible(false, true, "closed", emptyList()))
        assertFalse(Admission.eligible(true, false, "closed", emptyList()))
        for (stage in listOf("requested", "offered", "incoming", "accepted", "committed", "installed"))
            assertFalse(Admission.eligible(true, true, stage, emptyList()))
        assertFalse(Admission.eligible(true, true, "closed", listOf(unknown)))
    }
    @Test fun healthErrorsAreInlineAndNeverBecomeASuccessToast() {
        val model = TrainPilotViewModel()
        model.updateHealthSyncStatus(false, failed = true, message = Admission.Block.ACTIVE.message)
        assertTrue(model.uiState.value.healthSyncFailed)
        assertEquals(Admission.Block.ACTIVE.message, model.uiState.value.healthSyncMessage)
        assertNull(model.uiState.value.statusBannerMessage)
    }
}

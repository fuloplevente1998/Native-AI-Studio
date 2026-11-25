package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.*
import com.repforge.app.PhoneMeasurementReconnect351.ReconnectPlan
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.shared.WorkoutOwnership193
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

/**
 * 2809 #5 (phone side): a reconnect after an app update must not push a stored draft to the
 * watch or ask it to record, and the explicit phone discard must end a watch-owned workout.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class WatchPhantomWorkout2809RegressionTest {
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
             "sets":[{"setId":"phantom-one","set":1,"reps":10,"weight":15,"done":false}]}
        """), TrainPilotUserSettings()))

    private fun frame(id: String = "w") = JSONObject().put("workoutId", id).put("active", true).put("revision", 42)
        .put("ownerEpoch193", 0).put("timerOwner", "phone")

    @Test fun reconnectAfterRestartRetriesOnlyTheTerminalProjectionAndNeverAsksTheWatchToRecord() {
        val now = 100_000L
        // Process start / app update: nothing armed, a stored active draft is not pushed or recorded.
        assertEquals(ReconnectPlan.NONE, PhoneMeasurementReconnect351.reconnectPlan(frame(), null, now))
        assertEquals(ReconnectPlan.NONE, PhoneMeasurementReconnect351.reconnectPlan(frame(), "previous-workout", now))
        // The durable terminal projection is still retried (pre-#370 rule).
        assertEquals(ReconnectPlan.TERMINAL, PhoneMeasurementReconnect351.reconnectPlan(frame().put("active", false), null, now))
        assertEquals(ReconnectPlan.NONE, PhoneMeasurementReconnect351.reconnectPlan(frame("").put("active", false), null, now))
        assertEquals(ReconnectPlan.NONE, PhoneMeasurementReconnect351.reconnectPlan(null, "w", now))
        // Only the start this process requested, and only while it is still eligible (#351 reconnect).
        assertEquals(ReconnectPlan.MEASUREMENT, PhoneMeasurementReconnect351.reconnectPlan(frame(), "w", now))
        for (blocked in listOf(frame().put("timerOwner", "watch"), frame().put("workoutPaused", true),
                frame().put("discardPending", true), frame().put("finalizationPending", true)))
            assertEquals(ReconnectPlan.NONE, PhoneMeasurementReconnect351.reconnectPlan(blocked, "w", now))
        // A real restart has no armed workout: the listener's plan for a stored draft is NONE.
        start()
        assertTrue(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
        assertEquals(ReconnectPlan.NONE, PhoneMeasurementReconnect351.reconnectPlan(ActiveWorkoutStore.current(context),
            null, System.currentTimeMillis()))
    }

    private fun discardsWatchSide(mark: (JSONObject) -> JSONObject) {
        start()
        val watchSide = ActiveWorkoutStore.saveOwned(context, mark(JSONObject(ActiveWorkoutStore.current(context)!!.toString())))
        val id = watchSide.getString("workoutId")
        var published: JSONObject? = null
        val closed = native {
            TrainPilotWorkoutFinalization(context, db) { terminal -> published = JSONObject(terminal.toString()) }
                .discardWorkout(watchSide)
        }
        // Applied on the phone: durable tombstone, inactive native frame, closed draft.
        assertTrue(native { db.workoutHistoryDao().isTombstoned(id, JSONObject.quote(id)) })
        assertFalse(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
        assertFalse(PhoneWorkoutExecution.store(context).snapshot()!!.getBoolean("active"))
        // The watch gets the marked terminal for exactly this workout (also on a later reconnect retry).
        assertTrue(WorkoutOwnership193.phoneDiscardTargets(published, id))
        assertTrue(WorkoutOwnership193.phoneDiscardTargets(ActiveWorkoutStore.current(context), id))
        assertEquals(closed.toString(), published.toString())
        assertEquals(ReconnectPlan.TERMINAL, PhoneMeasurementReconnect351.plan(context))
        // The watch's durable acknowledgement is answered from the tombstone, never re-applied.
        val ack = JSONObject().put("schema", 1).put("commandId", "watch-ack-2809").put("workoutId", id)
            .put("action", "discardWorkout").put("confirmed", true).put("sequence", 9)
            .put(WorkoutOwnership193.PHONE_DISCARD_2809, true).put("ownerEpoch193", watchSide.optLong("ownerEpoch193"))
        assertEquals(NativeWatchDiscardAdmission.Decision.ALREADY_DISCARDED, native { NativeWatchDiscardAdmission.resolve(context, db, ack) })
    }

    @Test fun explicitPhoneDiscardEndsAWatchOwnedWorkout() = discardsWatchSide {
        it.put("timerOwner", "watch").put("ownershipProtocol193", 1).put("ownerEpoch193", 1)
    }

    @Test fun explicitPhoneDiscardEndsAWorkoutStuckInAPendingHandoff() = discardsWatchSide {
        it.put("ownershipProtocol193", 1).put("handoff193", JSONObject().put("state", "offered").put("target", "watch"))
    }
}

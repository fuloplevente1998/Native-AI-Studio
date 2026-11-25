package com.repforge.app.wear

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.repforge.shared.WearMeasurementRequest351
import com.repforge.shared.WorkoutOwnership193
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * 2809 #5: after the 1.5.1 update a workout appeared/started on the watch by itself and the phone
 * could not discard it. Restart/boot/update must never start or resurrect a workout, a closed ID
 * must stay closed when replayed, and the explicit phone discard must reach a watch-owned workout.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class Wear2809PhantomWorkoutTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun clear() {
        for (prefs in listOf("trainpilot_wear_snapshot", "trainpilot_wear_outbox", "trainpilot_wear_closure",
                "trainpilot_wear_health", "wear_daily_health_367"))
            context.getSharedPreferences(prefs, Context.MODE_PRIVATE).edit().clear().commit()
        context.noBackupFilesDir.listFiles()?.filter { it.name.startsWith("ownership193") }?.forEach { it.delete() }
        shadowOf(context as Application).clearStartedServices()
    }

    private fun workout(id: String, revision: Long, owner: String = "phone", epoch: Long = 0L, protocol: Int = 1) =
        WearWorkout(revision, "p", "Plan", "d", "", "2026-11-25T08:00:00Z", id, 0, 60, 0,
            listOf(WearExercise("press", "Press", "weight", "ism.", "reps",
                listOf(WearSet(1, "10", "40", true, 0, 0, 0.0, "$id-set-1"), WearSet(2, "10", "40", false, 0, 0, 0.0, "$id-set-2")))),
            localSequence = 5, timerOwner = owner, ownershipProtocol193 = protocol, ownerEpoch193 = epoch)

    private fun active(id: String, revision: Long, epoch: Long = 0L) =
        WorkoutSnapshotStore.encode(workout(id, revision, epoch = epoch)).toString()

    private fun terminal(id: String, revision: Long, epoch: Long = 0L, phoneDiscard: Boolean = false) = JSONObject()
        .put("schema", 1).put("active", false).put("workoutId", id).put("revision", revision)
        .put("ownerEpoch193", epoch).put("syncedAt", System.currentTimeMillis())
        .apply { if (phoneDiscard) put(WorkoutOwnership193.PHONE_DISCARD_2809, true) }.toString()

    private fun outbox(): JSONArray =
        JSONArray(context.getSharedPreferences("trainpilot_wear_outbox", Context.MODE_PRIVATE).getString("pending", "[]"))

    private fun noServiceStarted() = assertNull(shadowOf(context as Application).nextStartedService)

    // (a) restart / boot / package replaced with a stale snapshot or request: nothing starts.

    @Test fun bootAndPackageReplacedNeverStartAWorkoutOrMeasurementFromAStoredSnapshot() {
        val stale = WorkoutSnapshotStore.saveWorkout(context, workout("stale-after-update", 10, epoch = 2))
        for (action in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED))
            WearDailyHealthRestoreReceiver().onReceive(context, Intent(action))
        noServiceStarted()
        assertEquals(stale, WorkoutSnapshotStore.load(context))
        assertEquals("", WearHealthStore.active(context))
    }

    @Test fun aRestoredPhoneCopyRecordsOnlyAfterThePhoneStateOrAnExplicitAction() {
        val restored = workout("restored-phone-copy", 10, epoch = 2)
        assertFalse(wearMayAutoRecord2809(restored, phoneStateApplied = false, explicitPhoneStart = false))
        assertTrue(wearMayAutoRecord2809(restored, phoneStateApplied = true, explicitPhoneStart = false))
        assertTrue(wearMayAutoRecord2809(restored, phoneStateApplied = false, explicitPhoneStart = true))
        // The watch's own workout is its own durable state, not a phone copy.
        assertTrue(wearMayAutoRecord2809(workout("watch-own", 10, owner = "watch", epoch = 1), false, false))
    }

    @Test fun staleOrUnconfirmedMeasurementRequestsAreDroppedAndStartNothing() {
        val current = WorkoutSnapshotStore.saveWorkout(context, workout("request-after-restart", 12))
        val now = System.currentTimeMillis()
        // Older than the 15 s lifetime: a request that waited across a restart.
        WearBackgroundMeasurement.receive(context, "phone-node", WearMeasurementRequest351(current.workoutId, 12, 0, now - 60_000L))
        // Fresh, but no phone snapshot from that sender was observed in this process.
        WearBackgroundMeasurement.receive(context, "phone-node", WearMeasurementRequest351(current.workoutId, 12, 0, now))
        noServiceStarted()
        assertEquals("", WearHealthStore.active(context))
    }

    // (b) a replayed finished/discarded workout stays closed.

    @Test fun phoneClosureOfAnUnknownWorkoutIsTombstonedAndItsReplayIgnored() {
        assertNull(WorkoutSnapshotStore.save(context, terminal("closed-on-phone", 15)))
        assertTrue(WearClosureStore.isClosed(context, "closed-on-phone"))
        assertNull(WorkoutSnapshotStore.save(context, active("closed-on-phone", 14)))
        assertNull(WorkoutSnapshotStore.load(context))
    }

    @Test fun epochFencedPhoneMirrorTheDataLayerSkippedPastIsDroppedAndStaysClosed() {
        // The phone closed X and moved on; only its later terminal (other workout) reached the watch.
        WorkoutSnapshotStore.saveWorkout(context, workout("stuck-mirror", 10, epoch = 2))
        assertNull(WorkoutSnapshotStore.save(context, terminal("later-workout", 20)))
        assertNull(WorkoutSnapshotStore.load(context))
        assertTrue(WearClosureStore.isClosed(context, "stuck-mirror"))
        // A late replay of X (any epoch) never comes back.
        assertNull(WorkoutSnapshotStore.save(context, active("stuck-mirror", 10, epoch = 2)))
        assertNull(WorkoutSnapshotStore.save(context, active("stuck-mirror", 11)))
        assertNull(WorkoutSnapshotStore.load(context))
    }

    @Test fun aNewerPhoneWorkoutReplacesTheStuckMirrorButTheSameWorkoutStillUpdates() {
        WorkoutSnapshotStore.saveWorkout(context, workout("old-mirror", 10, epoch = 2))
        val same = WorkoutSnapshotStore.save(context, active("old-mirror", 11, epoch = 2))!!
        assertEquals("old-mirror", same.workoutId); assertEquals(11L, same.revision)
        assertEquals("new-phone-workout", WorkoutSnapshotStore.save(context, active("new-phone-workout", 21))!!.workoutId)
        assertTrue(WearClosureStore.isClosed(context, "old-mirror"))
        // An older phone state cannot drop the current mirror.
        assertEquals("new-phone-workout", WorkoutSnapshotStore.save(context, terminal("other", 20))!!.workoutId)
    }

    @Test fun watchOwnedWorkoutIsNeverDroppedByAnotherWorkoutsPhoneState() {
        val own = WorkoutSnapshotStore.saveWorkout(context, workout("watch-recording", 10, owner = "watch", epoch = 1))
        assertEquals(own, WorkoutSnapshotStore.save(context, terminal("other-workout", 30)))
        assertEquals(own, WorkoutSnapshotStore.load(context))
        assertFalse(WearClosureStore.isClosed(context, "watch-recording"))
        assertEquals(0, outbox().length())
    }

    // (c) explicit phone discard of a watch-owned stuck workout is applied and acknowledged.

    @Test fun explicitPhoneDiscardEndsAWatchOwnedStuckWorkoutAndQueuesTheAcknowledgement() {
        val stuck = WorkoutSnapshotStore.saveWorkout(context, workout("watch-owned-stuck", 10, owner = "watch", epoch = 3)
            .copy(controlIntent367 = JSONObject().put("commandId", "never-acked").toString()))
        // Without the explicit marker the existing epoch fence keeps the newer-epoch watch workout.
        assertEquals(stuck, WorkoutSnapshotStore.save(context, terminal("watch-owned-stuck", 20, epoch = 2)))
        assertNull(WorkoutSnapshotStore.save(context, terminal("watch-owned-stuck", 21, epoch = 2, phoneDiscard = true)))
        assertNull(WorkoutSnapshotStore.load(context))
        assertTrue(WearClosureStore.isClosed(context, "watch-owned-stuck"))
        val acks = (0 until outbox().length()).map { outbox().getJSONObject(it) }.filter { it.optString("action") == "discardWorkout" }
        assertEquals(1, acks.size)
        val ack = acks.single()
        assertEquals("watch-owned-stuck", ack.getString("workoutId"))
        assertTrue(ack.getBoolean(WorkoutOwnership193.PHONE_DISCARD_2809))
        assertTrue(ack.getBoolean("confirmed"))
        assertEquals("watch-owned-stuck", ack.getJSONObject("finalSnapshot").getString("workoutId"))
        // Replays of the terminal or the old active state are inert and do not queue a second ACK.
        assertNull(WorkoutSnapshotStore.save(context, terminal("watch-owned-stuck", 21, epoch = 2, phoneDiscard = true)))
        assertNull(WorkoutSnapshotStore.save(context, active("watch-owned-stuck", 9)))
        assertEquals(1, (0 until outbox().length()).count { outbox().getJSONObject(it).optString("action") == "discardWorkout" })
    }

    @Test fun phoneDiscardMarkerTargetsOnlyItsOwnWorkout() {
        val other = WorkoutSnapshotStore.saveWorkout(context, workout("watch-other", 10, owner = "watch", epoch = 1))
        assertEquals(other, WorkoutSnapshotStore.save(context, terminal("different", 40, epoch = 1, phoneDiscard = true)))
        assertFalse(WearPhoneClosure2809.phoneDiscard(other, JSONObject(terminal("", 40, phoneDiscard = true))))
        assertFalse(WearPhoneClosure2809.phoneDiscard(other, JSONObject(active("watch-other", 40)).put(WorkoutOwnership193.PHONE_DISCARD_2809, true)))
    }
}

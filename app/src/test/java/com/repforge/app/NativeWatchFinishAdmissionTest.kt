package com.repforge.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NativeWatchFinishAdmissionTest {
    private fun frame(owner: String = "phone") = JSONObject().put("workoutId", "session")
        .put("started", "2026-11-06T10:00:00Z").put("active", true).put("generation", 7)
        .put("ownershipProtocol193", 1).put("ownerEpoch193", 2).put("timerOwner", owner)
    private fun command(snapshot: JSONObject = frame()) = JSONObject().put("action", "finishWorkout")
        .put("schema", 1).put("confirmed", true).put("sequence", 3).put("workoutId", "session")
        .put("ownerEpoch193", 2).put("finalSnapshot", snapshot)

    @Test fun mirroredFinishRequestsPhoneClosureWithoutGrantingWatchOwnership() {
        val current = frame()
        assertTrue(NativeWatchFinishAdmission.isPhoneOwnedRequest(command(), current))
        for ((key, value) in listOf("workoutId" to "other", "generation" to 8,
            "ownerEpoch193" to 1, "started" to "2026-11-05T10:00:00Z", "timerOwner" to "watch")) {
            assertFalse(key, NativeWatchFinishAdmission.isPhoneOwnedRequest(command(frame().put(key, value)), current))
        }
        assertFalse(NativeWatchFinishAdmission.isPhoneOwnedRequest(command().put("confirmed", false), current))
        assertFalse(NativeWatchFinishAdmission.isPhoneOwnedRequest(command(), frame().put("discardPending", true)))
        assertFalse(NativeWatchFinishAdmission.isPhoneOwnedRequest(command(), frame().put("handoff193", JSONObject().put("state", "prepared"))))
    }

    @Test fun suspendedPhoneEditsPermitOnlyExactAcknowledgedWatchSessionToFinish() {
        val current = frame("watch")
        val execution = frame()
        assertTrue(NativeWatchFinishAdmission.canFinalizeSuspendedPhoneFrame(frame("watch"), current, execution))
        assertFalse(NativeWatchFinishAdmission.canFinalizeSuspendedPhoneFrame(frame(), current, execution))
        assertFalse(NativeWatchFinishAdmission.canFinalizeSuspendedPhoneFrame(frame("watch").put("ownerEpoch193", 1), current, execution))
        assertFalse(NativeWatchFinishAdmission.canFinalizeSuspendedPhoneFrame(frame("watch"), current, execution.put("generation", 8)))
        assertFalse(NativeWatchFinishAdmission.canFinalizeSuspendedPhoneFrame(frame("watch"), current.put("discardPending", true), frame()))
    }

    @Test fun lostAckReplaysOnlyDurablySavedAndFullyClosedSession() {
        val stored = frame().put("active", false).put("finished", "2026-11-06T11:00:00Z")
        assertTrue(NativeWatchFinishAdmission.isSavedRetry(command(), stored, null, null))
        assertTrue(NativeWatchFinishAdmission.isSavedRetry(command(), stored, frame().put("workoutId", "new-session"), null))
        assertFalse(NativeWatchFinishAdmission.isSavedRetry(command(), stored, frame(), null))
        assertFalse(NativeWatchFinishAdmission.isSavedRetry(command(), stored, null, frame()))
        assertFalse(NativeWatchFinishAdmission.isSavedRetry(command(), stored, frame().put("active", false).put("finalizationPending", true), null))
        assertFalse(NativeWatchFinishAdmission.isSavedRetry(command().put("sequence", 0), stored, null, null))
        assertFalse(NativeWatchFinishAdmission.isSavedRetry(command(), JSONObject(stored.toString()).put("started", "2026-11-05T10:00:00Z"), null, null))
    }
}

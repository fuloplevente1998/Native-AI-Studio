package com.repforge.app.wear

import com.repforge.shared.WorkoutContinuation
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WearWorkoutContinuationTest {
    private fun paused() = JSONObject("""{"active":true,"workoutId":"session","timerOwner":"watch","ownershipProtocol193":1,"ownerEpoch193":2,"pauseRevision":20,"workoutPaused":true}""")
    private fun request() = WorkoutContinuation.request(paused(), "explicit-tap", 100_000)

    @Test fun explicitPhoneTapTargetsOnlyThePausedOwnersExactRevision() {
        val current = paused()
        val request = request()
        assertTrue(WorkoutContinuation.accepts(current, request, 100_001))
        assertTrue(current.getBoolean("workoutPaused")) // Transport cannot optimistically resume its projection.
        for ((key, value) in listOf("workoutId" to "new", "ownerEpoch193" to 3, "pauseRevision" to 21,
            "timerOwner" to "phone", "ownershipProtocol193" to 0, "workoutPaused" to false,
            "active" to false, "finalizationPending" to true)) {
            assertFalse(key, WorkoutContinuation.accepts(paused().put(key, value), request, 100_001))
        }
        assertFalse(WorkoutContinuation.accepts(paused().put("handoff193", JSONObject().put("state", "accepted")), request, 100_001))
    }

    @Test fun duplicateDelayedAndMalformedRequestsCannotResumeAnotherPause() {
        assertFalse(WorkoutContinuation.accepts(paused(), request(), 115_001))
        assertFalse(WorkoutContinuation.accepts(paused(), request(), 99_999))
        assertFalse(WorkoutContinuation.accepts(paused(), request().put("schema", 2), 100_001))
        assertFalse(WorkoutContinuation.accepts(paused(), request().put("commandId", ""), 100_001))
        assertFalse(WorkoutContinuation.accepts(paused(), request().put("action", "resume"), 100_001))
        assertFalse(WorkoutContinuation.accepts(paused().put("pauseRevision", 22), request(), 100_001))
    }
}

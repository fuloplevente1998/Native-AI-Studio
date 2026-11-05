package com.repforge.app.wear

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PhoneWorkoutClosureTest {
    private fun workout() = WorkoutSnapshotStore.parse("""{"workoutId":"phone-closed","started":"2026-11-05T10:00:00Z","revision":0,"localSequence":8,"ownerEpoch193":0,"timerOwner":"phone","exercises":[{"id":"press","sets":[{"setId":"s1","reps":"8","done":false}]}]}""")!!
    private fun terminal() = JSONObject().put("active", false).put("workoutId", "phone-closed")
        .put("ownerEpoch193", 0).put("revision", 12).put("syncedAt", java.time.Instant.parse("2026-11-05T10:01:00Z").toEpochMilli())

    @Test fun matchingDurableClosureWinsOverUnacknowledgedLocalEdits() {
        val current = workout()
        assertTrue(current.localSequence > 0)
        assertTrue(acceptsPhoneWorkoutClosure(current, terminal()))
    }

    @Test fun anotherWorkoutOrOlderEpochRevisionAndTimestampCannotCloseCurrentWorkout() {
        val current = workout().copy(revision = 10, ownerEpoch193 = 2)
        val valid = terminal().put("ownerEpoch193", 2)
        assertTrue(acceptsPhoneWorkoutClosure(current, valid))
        for (invalid in listOf(JSONObject(valid.toString()).put("workoutId", "another"),
            JSONObject(valid.toString()).put("ownerEpoch193", 1),
            JSONObject(valid.toString()).put("ownerEpoch193", 3),
            JSONObject(valid.toString()).put("revision", 9),
            JSONObject(valid.toString()).put("syncedAt", 1),
            JSONObject(valid.toString()).put("active", true))) {
            assertFalse(acceptsPhoneWorkoutClosure(current, invalid))
        }
    }
}

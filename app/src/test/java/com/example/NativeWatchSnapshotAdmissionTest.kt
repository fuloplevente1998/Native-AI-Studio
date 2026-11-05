package com.example

import com.repforge.app.NativeWatchSnapshotAdmission
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NativeWatchSnapshotAdmissionTest {
    private fun current() = JSONObject("""{"active":true,"workoutId":"session","timerOwner":"watch","ownershipProtocol193":1,"ownerEpoch193":2,"pauseRevision":20,"workoutPaused":false,"localSequence":5,"exercises":[{"id":"press","sets":[{"setId":"set-1","reps":"12"}]}]}""")
    private fun command(frame: JSONObject = current(), sequence: Long = 6) = JSONObject()
        .put("workoutId", "session").put("action", "watchAutomationState").put("ownerEpoch193", 2)
        .put("sequence", sequence).put("finalSnapshot", frame)

    @Test fun reorderedSnapshotCannotUndoNewerPauseOrEdits() {
        val newest = NativeWatchSnapshotAdmission.snapshot(current(), command())!!
        assertEquals(6L, newest.getLong("localSequence"))
        assertNull(NativeWatchSnapshotAdmission.snapshot(newest, command(sequence = 6)))
        assertNull(NativeWatchSnapshotAdmission.snapshot(newest, command(sequence = 5)))
        assertNull(NativeWatchSnapshotAdmission.snapshot(newest, command(current().put("pauseRevision", 19), 7)))
        assertNull(NativeWatchSnapshotAdmission.snapshot(newest, command(current().put("workoutPaused", true), 7)))
        assertEquals("12", newest.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getString("reps"))
    }

    @Test fun matchingNewPauseAndResumeRemainDurableUnderTheSameOwner() {
        val paused = NativeWatchSnapshotAdmission.snapshot(current(), command(current().put("pauseRevision", 21).put("workoutPaused", true)))!!
        val resumed = NativeWatchSnapshotAdmission.snapshot(paused, command(current().put("pauseRevision", 22), 7))!!
        assertFalse(resumed.getBoolean("workoutPaused"))
        assertEquals(2L, resumed.getLong("ownerEpoch193"))
        assertEquals("watch", resumed.getString("timerOwner"))
        assertNull(NativeWatchSnapshotAdmission.snapshot(resumed, command(paused, 8)))
    }

    @Test fun latePreviousOwnerWrongPayloadAndClosedDraftCannotReplaceCurrentSession() {
        assertNull(NativeWatchSnapshotAdmission.snapshot(current().put("timerOwner", "phone").put("ownerEpoch193", 3), command()))
        assertNull(NativeWatchSnapshotAdmission.snapshot(current(), command(current().put("ownerEpoch193", 1))))
        assertNull(NativeWatchSnapshotAdmission.snapshot(current(), command(current().put("ownerEpoch193", 3))))
        assertNull(NativeWatchSnapshotAdmission.snapshot(current(), command(current().put("workoutId", "foreign"))))
        assertNull(NativeWatchSnapshotAdmission.snapshot(current().put("active", false), command()))
        assertNull(NativeWatchSnapshotAdmission.snapshot(current().put("workoutId", "new-session"), command()))
        assertNull(NativeWatchSnapshotAdmission.snapshot(current().put("handoff193", JSONObject().put("state", "offered")), command()))
    }
}

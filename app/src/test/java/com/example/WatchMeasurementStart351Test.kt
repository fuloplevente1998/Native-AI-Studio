package com.example

import com.example.ui.viewmodel.watchMeasurementEligible
import com.example.ui.viewmodel.watchMeasurementReply
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WatchMeasurementStart351Test {
    private fun frame() = JSONObject().put("active", true).put("workoutId", "w").put("timerOwner", "phone")
    @Test fun onlyCurrentUnpausedPhoneWorkoutMayRequestRecording() {
        assertTrue(watchMeasurementEligible(frame(), "w"))
        assertFalse(watchMeasurementEligible(frame(), "old"))
        assertFalse(watchMeasurementEligible(frame().put("active", false), "w"))
        assertFalse(watchMeasurementEligible(frame().put("workoutPaused", true), "w"))
        assertFalse(watchMeasurementEligible(frame().put("timerOwner", "watch"), "w"))
        assertFalse(watchMeasurementEligible(null, "w"))
    }
    @Test fun remoteLaunchOrStartingDoesNotConfirmActiveAndOldWorkoutReplyIsIgnored() {
        assertNull(watchMeasurementReply(null, "w"))
        val reply = JSONObject().put("workoutId", "w").put("state", "starting")
        assertEquals("starting", watchMeasurementReply(reply, "w"))
        reply.put("state", "active")
        assertEquals("active", watchMeasurementReply(reply, "w"))
        assertNull(watchMeasurementReply(reply, "old"))
        assertEquals("error", watchMeasurementReply(reply.put("state", "error"), "w"))
        assertNull(watchMeasurementReply(reply.put("state", "unknown"), "w"))
    }
}

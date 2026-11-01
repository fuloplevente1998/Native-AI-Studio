package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearRemoteOwnershipRequestTest {
    private val start = WearMeasurementStart("current", 42)
    private fun current() = WearWorkout(42L, "p", "Plan", "day", "", "2026-11-01T10:00:00Z", "current", 0, 90, 0,
        emptyList(), ownershipProtocol193 = 1, timerOwner = "phone")

    @Test fun visibleExplicitMatchingPhoneRequestCanRequestTakeoverOnce() {
        assertTrue(wearRemoteOwnershipMayRequest(true, false, true, start, current()))
        assertFalse(wearRemoteOwnershipMayRequest(true, true, true, start, current()))
        assertFalse(wearRemoteOwnershipMayRequest(false, false, true, start, current()))
        assertFalse(wearRemoteOwnershipMayRequest(true, false, false, start, current()))
    }

    @Test fun staleWrongOrPendingWorkoutsNeverTriggerOwnershipRequest() {
        assertFalse(wearRemoteOwnershipMayRequest(true, false, true, null, current()))
        assertFalse(wearRemoteOwnershipMayRequest(true, false, true, start, null))
        assertFalse(wearRemoteOwnershipMayRequest(true, false, true, start, current().copy(workoutId = "old")))
        assertFalse(wearRemoteOwnershipMayRequest(true, false, true, start, current().copy(revision = 41)))
        assertFalse(wearRemoteOwnershipMayRequest(true, false, true, start, current().copy(timerOwner = "watch")))
        assertFalse(wearRemoteOwnershipMayRequest(true, false, true, start, current().copy(ownershipProtocol193 = 0)))
        assertFalse(wearRemoteOwnershipMayRequest(true, false, true, start,
            current().copy(handoff193 = "{\"state\":\"offered\"}")))
    }
}

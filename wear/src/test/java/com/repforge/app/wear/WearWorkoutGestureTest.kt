package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearWorkoutGestureTest {
    @Test fun rightOpensControlsLeftReturnsAndNoOtherPagesAreReachable() {
        assertEquals(1, wearWorkoutGesturePage(0, 40f, 36f))
        assertEquals(0, wearWorkoutGesturePage(1, -40f, 36f))
        assertNull(wearWorkoutGesturePage(0, -40f, 36f))
        assertNull(wearWorkoutGesturePage(1, 40f, 36f))
        assertNull(wearWorkoutGesturePage(0, 20f, 36f))
        assertNull(wearWorkoutGesturePage(2, -40f, 36f))
    }
}

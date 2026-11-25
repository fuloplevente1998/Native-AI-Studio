package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearMainNavigationTest {
    @Test fun pagesHaveStableOrderAndDoNotWrap() {
        assertEquals("journal", wearAdjacentMainRoute("home", 1))
        assertEquals("metrics", wearAdjacentMainRoute("journal", 1))
        assertEquals("calendar", wearAdjacentMainRoute("metrics", 1))
        assertEquals("metrics", wearAdjacentMainRoute("calendar", -1))
        assertNull(wearAdjacentMainRoute("home", -1))
        assertNull(wearAdjacentMainRoute("calendar", 1))
    }

    @Test fun workoutAndSubmenusCannotPage() {
        for (route in listOf(null, "workout", "editor", "menu", "journal-detail", "finish")) {
            assertNull(wearAdjacentMainRoute(route, 1))
            assertNull(wearAdjacentMainRoute(route, -1))
        }
    }

    @Test fun systemRimRemainsUnclaimed() {
        val back = WEAR_SWIPE_BACK_EDGE_DP
        val rim = WEAR_SWIPE_RIM_DP
        // rightward (system back direction): the left band is protected
        assertFalse(wearSwipeMayStart(12f, 192f, 30f, back, rim))
        assertTrue(wearSwipeMayStart(96f, 192f, 30f, back, rim))
        // the physical rim never starts a swipe in either direction
        assertFalse(wearSwipeMayStart(3f, 192f, -30f, back, rim))
        assertFalse(wearSwipeMayStart(190f, 192f, -30f, back, rim))
        assertFalse(wearSwipeMayStart(190f, 192f, 30f, back, rim))
        // a screen too narrow for the protected band claims nothing
        assertFalse(wearSwipeMayStart(20f, 40f, 30f, back, rim))
    }
}

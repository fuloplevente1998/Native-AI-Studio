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
        assertFalse(wearMainSwipeCanStart(12f, 192f, 24f))
        assertTrue(wearMainSwipeCanStart(96f, 192f, 24f))
        assertFalse(wearMainSwipeCanStart(180f, 192f, 24f))
        assertFalse(wearMainSwipeCanStart(20f, 40f, 24f))
    }
}

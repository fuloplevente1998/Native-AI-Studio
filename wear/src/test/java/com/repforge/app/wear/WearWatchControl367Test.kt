package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearWatchControl367Test {
    @Test fun restDeadlineUsesOneAbsoluteClockAndClamps() {
        val now = 100000L
        assertEquals(145000L, wearRestDeadline367(now + 30000, now, 15))
        assertEquals(0L, wearRestDeadline367(now + 5000, now, -15))
        assertEquals(now + 600000, wearRestDeadline367(now + 595000, now, 15))
        assertNull(wearRestDeadline367(now, now, 15))
        assertNull(wearRestDeadline367(now + 30000, now, 1))
    }
}

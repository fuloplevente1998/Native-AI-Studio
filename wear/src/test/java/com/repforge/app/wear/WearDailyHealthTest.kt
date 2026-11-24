package com.repforge.app.wear

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class WearDailyHealthTest {
    @Test fun percentageRequiresBothRealInputs() {
        assertNull(wearDailyStepPercent(null, 10000))
        assertNull(wearDailyStepPercent(5000, null))
        assertNull(wearDailyStepPercent(5000, 0))
        assertEquals(0, wearDailyStepPercent(0, 10000))
        assertEquals(78, wearDailyStepPercent(7842, 10000))
        assertEquals(120, wearDailyStepPercent(12000, 10000))
    }
    @Test fun staleFutureAndPreviousDaySamplesAreRejected() {
        val zone = ZoneId.of("Europe/Budapest")
        val now = Instant.parse("2026-11-24T12:00:00Z").toEpochMilli()
        assertTrue(wearDailyAccept(0, now - 1000, now, zone))
        assertFalse(wearDailyAccept(now - 1000, now - 2000, now, zone))
        assertFalse(wearDailyAccept(0, now - 86400000, now, zone))
        assertFalse(wearDailyAccept(0, now + 1, now, zone))
    }
}

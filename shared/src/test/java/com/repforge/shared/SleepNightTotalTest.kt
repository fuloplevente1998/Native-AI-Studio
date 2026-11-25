package com.repforge.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SleepNightTotalTest {
    private val m = 60_000L
    private fun at(hours: Int, minutes: Int = 0) = (hours * 60L + minutes) * m

    @Test fun awakeGapIsExcludedAcrossMidnight() {
        // 23:00–02:00 asleep, 02:00–02:20 awake, 02:20–06:00 asleep → 6 h 40 min, not 3 h 40 or 7 h.
        val spans = listOf(at(23) to at(26), at(26, 20) to at(30))
        assertEquals(400L, SleepNightTotal.unionMinutes(spans))
    }

    @Test fun duplicateAndOverlappingRecordsCountOnce() {
        val night = at(23) to at(30)
        val stageInside = at(1 + 24) to at(3 + 24)
        assertEquals(420L, SleepNightTotal.unionMinutes(listOf(night, night, stageInside)))
        assertEquals(420L, SleepNightTotal.unionMinutes(listOf(at(23) to at(27), at(26) to at(30))))
    }

    @Test fun missingOrInvalidSpansStayUnknown() {
        assertNull(SleepNightTotal.unionMinutes(emptyList()))
        assertNull(SleepNightTotal.unionMinutes(listOf(at(5) to at(5), at(6) to at(4))))
        assertNull(SleepNightTotal.unionMillis(emptyList()))
    }
}

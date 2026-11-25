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
    @Test fun sleepRequiresCompletedOrderedBoundedInterval() {
        val start = 100000000L
        assertEquals(420L, wearObservedSleepMinutes(start, start + 420 * 60000L))
        assertNull(wearObservedSleepMinutes(0, start))
        assertNull(wearObservedSleepMinutes(start, start - 1))
        assertNull(wearObservedSleepMinutes(start, start + 25 * 3600000L))
    }

    /** #367/9: the night is the sum of every observed interval, not the last one. */
    @Test fun watchObservedIntervalsOfTheNightAreSummedWithoutAwakeGap() {
        val zone = ZoneId.of("Europe/Budapest")
        fun t(day: String, time: String) = java.time.LocalDateTime.parse(day + "T" + time).atZone(zone).toInstant().toEpochMilli()
        val (day1, first) = wearSleepSpansAfter(null, emptyList(), t("2026-11-24", "23:00"), t("2026-11-25", "02:00"), zone)
        val (day2, night) = wearSleepSpansAfter(day1, first, t("2026-11-25", "02:20"), t("2026-11-25", "06:00"), zone)
        assertEquals("2026-11-25", day2)
        assertEquals(400L, com.repforge.shared.SleepNightTotal.unionMinutes(night))
        // A repeated delivery of the same transition is not added twice.
        val (_, repeated) = wearSleepSpansAfter(day2, night, t("2026-11-25", "02:20"), t("2026-11-25", "06:00"), zone)
        assertEquals(400L, com.repforge.shared.SleepNightTotal.unionMinutes(repeated))
        // The next night starts from zero; an older night is never added to it.
        val (_, next) = wearSleepSpansAfter(day2, night, t("2026-11-25", "23:30"), t("2026-11-26", "06:30"), zone)
        assertEquals(420L, com.repforge.shared.SleepNightTotal.unionMinutes(next))
        assertEquals(night, wearSleepSpansDecode(wearSleepSpansEncode(night)))
        assertTrue(wearSleepSpansDecode("x-1,5-3,").isEmpty())
    }

    @Test fun phoneTotalOfTodayWinsAndOtherDaysAreNeverShown() {
        val zone = ZoneId.of("Europe/Budapest")
        val now = Instant.parse("2026-11-25T09:00:00Z").toEpochMilli()
        val phone = WatchPhoneSleepSummary.parse(org.json.JSONObject().put("date", "2026-11-25").put("minutes", 400).put("provider", "samsung_health"))
        assertEquals(WearSleepDisplay(400, WearSleepOrigin.PHONE_SAMSUNG), wearSleepDisplay(phone, 220, now, zone))
        val yesterday = phone!!.copy(date = "2026-11-24")
        assertEquals(WearSleepDisplay(220, WearSleepOrigin.WATCH_OBSERVED), wearSleepDisplay(yesterday, 220, now, zone))
        assertNull(wearSleepDisplay(yesterday, null, now, zone))
        assertNull(wearSleepDisplay(null, 0, now, zone))
        assertNull(WatchPhoneSleepSummary.parse(org.json.JSONObject().put("date", "2026-11-25").put("minutes", 0).put("provider", "samsung_health")))
        assertNull(WatchPhoneSleepSummary.parse(org.json.JSONObject().put("date", "bad").put("minutes", 400).put("provider", "health_connect")))
        assertNull(WatchPhoneSleepSummary.parse(org.json.JSONObject().put("date", "2026-11-25").put("minutes", 400).put("provider", "x")))
    }
}

package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** #367/9: the watch receives exactly the phone Today-tile sleep total, never a second calculation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WatchSleepSummary367Test {
    private val zone = ZoneId.of("Europe/Budapest")
    private val date = LocalDate.parse("2026-11-25")
    private val now = Instant.parse("2026-11-25T10:00:00Z")
    private fun local(day: String, time: String) = LocalDate.parse(day).atTime(java.time.LocalTime.parse(time)).atZone(zone).toInstant().toString()
    private fun span(start: String, end: String) = JSONObject().put("start", start).put("end", end)

    @Test fun samsungSessionsSplitByAwakeTimeMatchPhoneTotal() {
        // Samsung splits one night into sessions around the awake period (23:00–02:00, 02:20–06:00).
        val day = JSONObject().put("sleepSessions", JSONArray()
            .put(span(local("2026-11-24", "23:00"), local("2026-11-25", "02:00")))
            .put(span(local("2026-11-25", "02:20"), local("2026-11-25", "06:00")))
            // The same night read twice (re-sync/mirror) must not be added again.
            .put(span(local("2026-11-25", "02:20"), local("2026-11-25", "06:00"))))
        val payload = requireNotNull(WatchSleepSummary367.payload(day, HealthJournalStore.SAMSUNG, date, zone, now))
        assertEquals(400L, payload.getLong("minutes"))
        assertEquals(NativeDailyHealthDetails.sleepMinutes(day, date, zone, now)!!.toLong(), payload.getLong("minutes"))
        assertEquals("2026-11-25", payload.getString("date"))
        assertEquals(HealthJournalStore.SAMSUNG, payload.getString("provider"))
    }

    @Test fun healthConnectStagesExcludeAwakeStage() {
        fun stage(start: String, end: String, asleep: Boolean) = span(start, end).put("isAsleep", asleep)
        val stages = JSONArray()
            .put(stage(local("2026-11-24", "23:00"), local("2026-11-25", "02:00"), true))
            .put(stage(local("2026-11-25", "02:00"), local("2026-11-25", "02:20"), false))
            .put(stage(local("2026-11-25", "02:20"), local("2026-11-25", "06:00"), true))
        val day = JSONObject().put("sleepSessions", JSONArray().put(
            span(local("2026-11-24", "23:00"), local("2026-11-25", "06:00")).put("stages", stages)))
        assertEquals(400L, WatchSleepSummary367.payload(day, HealthJournalStore.HC, date, zone, now)!!.getLong("minutes"))
    }

    @Test fun missingSleepOrUnknownProviderSendsNothing() {
        assertNull(WatchSleepSummary367.payload(null, HealthJournalStore.HC, date, zone, now))
        assertNull(WatchSleepSummary367.payload(JSONObject(), HealthJournalStore.SAMSUNG, date, zone, now))
        val day = JSONObject().put("sleepSessions", JSONArray()
            .put(span(local("2026-11-24", "23:00"), local("2026-11-25", "06:00"))))
        assertNull(WatchSleepSummary367.payload(day, "other", date, zone, now))
    }
}

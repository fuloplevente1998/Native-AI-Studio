package com.example

import com.repforge.app.NativeDailyHealthDetails
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyHealthDetailsRegressionTest {
    private val day = LocalDate.parse("2026-11-02")
    private val now = Instant.parse("2026-11-02T19:00:00Z")
    private fun session(start: String, end: String) = JSONObject().put("start", start).put("end", end)

    @Test fun afternoonNapAndNighttimeSleepUseRealWakeDay() {
        val raw = JSONObject().put("sleepSessions", JSONArray()
            .put(session("2026-11-01T22:00:00Z", "2026-11-02T06:00:00Z"))
            .put(session("2026-11-02T14:00:00Z", "2026-11-02T17:00:00Z")))
        assertEquals(660.0, NativeDailyHealthDetails.sleepMinutes(raw, day, ZoneOffset.UTC, now)!!, 0.001)
        assertNull(NativeDailyHealthDetails.sleepMinutes(raw, day.minusDays(1), ZoneOffset.UTC, now))
    }

    @Test fun overlappingSleepStagesAreNotAddedTwice() {
        fun stage(start: String, end: String, asleep: Boolean) = session(start, end).put("isAsleep", asleep)
        val stages = JSONArray().put(stage("2026-11-02T14:00:00Z", "2026-11-02T16:00:00Z", true))
            .put(stage("2026-11-02T15:00:00Z", "2026-11-02T17:00:00Z", true))
            .put(stage("2026-11-02T16:00:00Z", "2026-11-02T16:30:00Z", false))
        val raw = JSONObject().put("sleepSessions", JSONArray().put(
            session("2026-11-02T14:00:00Z", "2026-11-02T17:00:00Z").put("stages", stages)))
        assertEquals(180.0, NativeDailyHealthDetails.sleepMinutes(raw, day, ZoneOffset.UTC, now)!!, 0.001)
    }

    @Test fun missingOrUnfinishedSleepRemainsUnknown() {
        assertNull(NativeDailyHealthDetails.sleepMinutes(JSONObject(), day, ZoneOffset.UTC, now))
        val future = JSONObject().put("sleepSessions", JSONArray().put(
            session("2026-11-02T18:00:00Z", "2026-11-02T21:00:00Z")))
        assertNull(NativeDailyHealthDetails.sleepMinutes(future, day, ZoneOffset.UTC, now))
    }

    @Test fun pulseHistoryIsOrderedDeduplicatedAndNotFabricated() {
        fun point(at: String, bpm: Int) = JSONObject().put("time", at).put("bpm", bpm).put("source", "samsung")
        val raw = JSONObject().put("heartRateSeries", JSONArray()
            .put(point("2026-11-02T15:00:00Z", 62))
            .put(point("2026-11-02T12:00:00Z", 75))
            .put(point("2026-11-02T15:00:00Z", 62))
            .put(point("2026-11-02T18:00:00Z", 0))
            .put(point("invalid", 88)))
        assertEquals(listOf(75, 62), NativeDailyHealthDetails.pulseSeries(raw).map { it.bpm })
        assertTrue(NativeDailyHealthDetails.pulseSeries(JSONObject()).isEmpty())
    }
}

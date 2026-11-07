package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NativeCalendarCleanupV1Test {
    @Test fun removesEverythingThroughNovemberFirstAndKeepsOnePerLaterDay() {
        val marker = NativeCalendarCleanupV1.marker(1234L)
        val input = JSONArray()
            .put(row("old-a", "2026-11-01", "planned", "", 1))
            .put(row("old-b", "2026-10-30", "completed", "", 2))
            .put(row("nov2-planned", "2026-11-02", "planned", "Otthoni A/B – Alap", 3))
            .put(row("nov2-completed", "2026-11-02", "completed", "", 4))
            .put(row("nov4-cancelled", "2026-11-04", "cancelled", "", 20))
            .put(row("nov4-current", "2026-11-04", "planned", "Otthoni A/B – Alap", 10))
            .put(row("nov4-legacy", "2026-11-04", "planned", "", 30))

        val out = NativeCalendarCleanupV1.filterScheduled(input, marker)
        assertEquals(2, out.length())
        val ids = (0 until out.length()).map { out.getJSONObject(it).getString("id") }.toSet()
        assertEquals(setOf("nov2-completed", "nov4-current"), ids)
        assertTrue((0 until out.length()).all { out.getJSONObject(it).getString("date") > "2026-11-01" })
    }

    @Test fun newestPortableMarkerWinsAndCanFilterStaleRemoteRows() {
        val local = JSONObject().put(NativeCalendarCleanupV1.JSON_KEY,
            NativeCalendarCleanupV1.marker(100L))
        val remote = JSONObject().put(NativeCalendarCleanupV1.JSON_KEY,
            NativeCalendarCleanupV1.marker(200L))
        val marker = NativeCalendarCleanupV1.newestMarker(local, remote)
        assertEquals(200L, marker?.getLong("updatedAt"))

        val stale = JSONArray()
            .put(row("remote-old", "2026-09-10", "planned", "Old", 999))
            .put(row("remote-future-a", "2026-11-10", "planned", "", 1))
            .put(row("remote-future-b", "2026-11-10", "planned", "Current", 2))
        val filtered = NativeCalendarCleanupV1.filterScheduled(stale, marker)
        assertEquals(1, filtered.length())
        assertEquals("remote-future-b", filtered.getJSONObject(0).getString("id"))
    }

    @Test fun withoutMarkerLeavesPortableScheduleUntouched() {
        val input = JSONArray().put(row("a", "2026-09-10", "planned", "", 1))
            .put(row("b", "2026-09-10", "planned", "", 2))
        val out = NativeCalendarCleanupV1.filterScheduled(input, null)
        assertEquals(2, out.length())
    }

    private fun row(id: String, date: String, status: String, program: String, updatedAt: Long) =
        JSONObject()
            .put("id", id)
            .put("date", date)
            .put("status", status)
            .put("programName", program)
            .put("dayName", if (program.isBlank()) "" else "A")
            .put("googleEventId", "rf" + "a".repeat(64))
            .put("updatedAt", updatedAt)
}

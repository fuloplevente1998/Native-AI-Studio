package com.example

import com.repforge.app.HealthJournalStore
import com.repforge.app.ui.health.samsungHourlyEnergy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SamsungHourlyEnergy2814Test {
    private val day = LocalDate.parse("2026-11-25")
    private val now = Instant.parse("2026-11-26T09:00:00Z")
    private val provider = HealthJournalStore.SAMSUNG
    private fun row(start: String, end: String, value: Double) = JSONObject().put("start", start).put("end", end).put("kcal", value)
    private fun base() = JSONObject().put("day", day.toString()).put("channel", provider).put("source", "@aggregate")
        .put("end", "2026-11-26T00:00:00Z").put("activeCalories", 301).put("totalCalories", 1846)
        .put("permissions", JSONObject().put("READ_ACTIVE_CALORIES_BURNED", true).put("READ_TOTAL_CALORIES_BURNED", true))
    private fun read(raw: JSONObject, type: String = "", source: String = "", channel: String = provider) =
        samsungHourlyEnergy(raw, day, channel, source, type, ZoneOffset.UTC, now)
    @Test fun dailyTotalsAreNeverInventedAsTimedMeasurements() {
        assertTrue(read(base()).isEmpty())
        val raw = base().put("activeCaloriesHourly", JSONArray().put(row("2026-11-25T18:00:00Z", "2026-11-25T19:00:00Z", 25.0)))
            .put("totalCaloriesHourly", JSONArray().put(row("2026-11-25T18:00:00Z", "2026-11-25T19:00:00Z", 80.0)))
        assertEquals(25.0, read(raw).single().active!!, 0.0)
        assertEquals(80.0, read(raw).single().total!!, 0.0)
        assertNull(read(raw, "ActiveCaloriesBurnedRecord").single().total)
        assertNull(read(raw, "TotalCaloriesBurnedRecord").single().active)
        assertTrue(read(raw, source = "watch").isEmpty())
        assertTrue(read(raw, channel = HealthJournalStore.HC).isEmpty())
    }
    @Test fun duplicateOverlapFailureAndDeniedGrantDoNotDisplayCachedValues() {
        val a = row("2026-11-25T18:00:00Z", "2026-11-25T19:00:00Z", 0.0)
        val raw = base().put("activeCaloriesHourly", JSONArray().put(a))
        assertEquals(0.0, read(raw).single().active!!, 0.0)
        raw.put("activeCaloriesHourly", JSONArray().put(a).put(a))
        assertTrue(read(raw).isEmpty())
        raw.put("activeCaloriesHourly", JSONArray().put(a)).put("failedFields", JSONArray().put("activeCaloriesHourly"))
        assertTrue(read(raw).isEmpty())
        raw.remove("failedFields")
        raw.getJSONObject("permissions").put("READ_ACTIVE_CALORIES_BURNED", false)
        assertTrue(read(raw).isEmpty())
    }
}

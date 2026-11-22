package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthConnectVisibleDayTest {
    private val day = JSONObject().put("channel", HealthJournalStore.HC)
        .put("day", "2026-11-22")
        .put("steps", 8500)
        .put("activeCalories", 430)
        .put("totalCalories", 1800)
        .put("oxygenSaturationPercent", 98)
        .put("sleepSessions", JSONArray().put(JSONObject().put("start", "2026-11-21T23:00:00Z")))
        .put("bodyFatPercent", 17.5)
        .put("metricProviders", JSONObject().put("steps", HealthJournalStore.HC)
            .put("activeCalories", HealthJournalStore.HC))

    @Test fun deniedGrantsHideOnlyTheirMetricsWithoutChangingPersistedDay() {
        val visible = HealthConnectVisibleDay.project(day, setOf("READ_STEPS", "READ_TOTAL_CALORIES_BURNED"))!!
        assertEquals(8500, visible.getInt("steps"))
        assertEquals(1800, visible.getInt("totalCalories"))
        assertFalse(visible.has("activeCalories"))
        assertFalse(visible.has("oxygenSaturationPercent"))
        assertFalse(visible.has("sleepSessions"))
        assertFalse(visible.getJSONObject("metricProviders").has("activeCalories"))
        assertTrue(day.has("activeCalories"))
        assertTrue(day.has("sleepSessions"))
        assertTrue(visible.getJSONObject("permissions").optBoolean("READ_STEPS"))
        assertFalse(visible.getJSONObject("permissions").optBoolean("READ_SLEEP"))
    }

    @Test fun failedReadNeverLabelsRetainedOldValueAsFreshButOtherMetricsSurvive() {
        val raw = JSONObject(day.toString())
            .put("warnings", JSONArray().put("READ_STEPS: Health Connect timed out"))
            .put("failedFields", JSONArray().put("bodyFatPercent"))
        val granted = setOf("READ_STEPS", "READ_BODY_FAT", "READ_ACTIVE_CALORIES_BURNED")
        val visible = HealthConnectVisibleDay.project(raw, granted)!!
        assertFalse(visible.has("steps"))
        assertFalse(visible.has("bodyFatPercent"))
        assertEquals(430, visible.getInt("activeCalories"))
        assertEquals(8500, raw.getInt("steps"))
    }

    @Test fun noGrantedPermissionsMeansNoHealthMetricButKeepsDayIdentity() {
        val visible = HealthConnectVisibleDay.project(day, emptySet())!!
        assertEquals("2026-11-22", visible.getString("day"))
        assertEquals(HealthJournalStore.HC, visible.getString("channel"))
        assertFalse(visible.has("steps"))
        assertFalse(visible.has("totalCalories"))
    }
}

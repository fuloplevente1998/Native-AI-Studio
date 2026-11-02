package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.HealthJournalStore
import com.repforge.app.HealthProviderProjection
import com.repforge.app.NativeHealthProviderData
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthDailyProviderIsolationRegressionTest {
    @Test fun selectedSamsungNeverUsesMissingHealthConnectMetricsOrSleep() {
        val hc = JSONObject().put("steps", 8500).put("restingHeartRate", 58)
            .put("weightKg", 70).put("weightTime", "2026-11-01T10:00:00Z")
            .put("sleepSessions", JSONArray().put(JSONObject().put("source", "hc")))
        val samsung = JSONObject().put("steps", 3200).put("sleepSessions", JSONArray())

        val selected = HealthProviderProjection.day(hc, samsung, null, HealthJournalStore.SAMSUNG)
        assertEquals(HealthJournalStore.SAMSUNG, selected.getString("channel"))
        assertEquals(3200, selected.getInt("steps"))
        assertTrue(selected.isNull("restingHeartRate"))
        assertTrue(selected.isNull("weightKg"))
        assertFalse(selected.has("weightTime"))
        assertEquals(0, selected.getJSONArray("sleepSessions").length())
        assertEquals(HealthJournalStore.SAMSUNG,
            selected.getJSONObject("metricProviders").getString("steps"))
        assertFalse(selected.getJSONObject("metricProviders").has("restingHeartRate"))
        assertEquals(8500, hc.getInt("steps"))
    }

    @Test fun absentSelectedSamsungDoesNotBorrowAnAvailableHealthConnectDay() {
        val hc = JSONObject().put("steps", 8500).put("activeCalories", 500)
        val selected = HealthProviderProjection.day(hc, null, null, HealthJournalStore.SAMSUNG)
        assertEquals(HealthJournalStore.SAMSUNG, selected.getString("channel"))
        assertTrue(selected.isNull("steps"))
        assertTrue(selected.isNull("activeCalories"))
        assertFalse(selected.getJSONObject("metricProviders").has("steps"))
        val selectedHc = HealthProviderProjection.day(hc, null, null, HealthJournalStore.HC)
        assertEquals(8500, selectedHc.getInt("steps"))
    }

    @Test fun journalAndProjectionKeepSelectedProviderDaysSeparateFromExplicitHcHistory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        HealthJournalStore(context).use { store ->
            store.erase()
            try {
                val day = LocalDate.parse("2026-11-01")
                store.healthDay(day.toString(),
                    JSONObject().put("source", "@aggregate").put("steps", 8500)
                        .put("restingHeartRate", 58),
                    store.generation())
                store.setPreferences(JSONObject().put("provider", HealthJournalStore.SAMSUNG))

                val before = store.page(day.toString(), day.toString(), "journal", "", 0, 0, 1)
                assertEquals(0, before.getJSONArray("days").length())
                assertFalse(store.projection().getJSONObject("days").has(day.toString()))
                assertEquals(8500,
                    NativeHealthProviderData.day(store, HealthJournalStore.HC, day)!!.getInt("steps"))

                store.samsungDay(day.toString(), JSONObject().put("steps", 3200),
                    JSONArray(), store.generation())
                val after = store.page(day.toString(), day.toString(), "journal", "", 0, 0, 1)
                val journal = after.getJSONArray("days").getJSONObject(0)
                assertEquals(HealthJournalStore.SAMSUNG, journal.getString("channel"))
                assertEquals(3200, journal.getInt("steps"))
                assertTrue(journal.isNull("restingHeartRate"))
                assertEquals(3200, store.projection().getJSONObject("days")
                    .getJSONObject(day.toString()).getInt("steps"))
                assertEquals(8500, NativeHealthProviderData.day(
                    store, HealthJournalStore.HC, day)!!.getInt("steps"))
            } finally {
                store.erase()
            }
        }
    }
}

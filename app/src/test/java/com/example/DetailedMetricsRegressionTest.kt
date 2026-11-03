package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.HealthJournalStore
import com.repforge.app.ui.health.NativeDetailedHealthMetrics as Metrics
import com.repforge.app.ui.health.NativeHealthMetrics
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
class DetailedMetricsRegressionTest {
    private val date = LocalDate.parse("2026-11-02")
    private val now = Instant.parse("2026-11-03T18:00:00Z")
    private val provider = HealthJournalStore.SAMSUNG
    private fun day() = JSONObject().put("day", date.toString()).put("channel", provider)
    private fun status(raw: JSONObject, key: String) = Metrics.status(raw, key, provider, date, now, ZoneOffset.UTC)

    @Test fun allDefinitionsHaveRecordDetailsAndMissingValuesAreNotZero() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val raw = day().put("steps", 0).put("weightKg", 0).put("averageHeartRate", 0)
        val list = NativeHealthMetrics.parse(context, Metrics.admitted(raw, provider, date, now, ZoneOffset.UTC), "hu")
        assertEquals(26, list.size)
        assertEquals(26, list.map { it.key }.distinct().size)
        assertTrue(list.all { Metrics.recordType(it.key).isNotBlank() })
        assertTrue(list.first { it.key == "steps" }.isAvailable)
        assertFalse(list.first { it.key == "weightKg" }.isAvailable)
        assertFalse(list.first { it.key == "averageHeartRate" }.isAvailable)
        assertFalse(list.first { it.key == "bloodGlucoseMmolL" }.isAvailable)
    }
    @Test fun wrongDateOrProviderNeverReusesHistoricalOrOtherProviderNumbers() {
        val raw = day().put("steps", 28000)
        assertEquals(Metrics.Status.NO_DATA, Metrics.status(raw, "steps", HealthJournalStore.HC, date, now, ZoneOffset.UTC))
        assertEquals(Metrics.Status.NO_DATA, Metrics.status(raw, "steps", provider, date.plusDays(1), now, ZoneOffset.UTC))
        assertEquals(Metrics.Status.NO_DATA, Metrics.status(null, "steps", provider, date, now, ZoneOffset.UTC))
    }
    @Test fun cachedValuesAfterDeniedOrFailedReadRemainUnavailable() {
        val raw = day().put("averageHeartRate", 76)
            .put("permissions", JSONObject().put("READ_HEART_RATE", false))
        assertEquals(Metrics.Status.DENIED, status(raw, "averageHeartRate"))
        raw.getJSONObject("permissions").put("READ_HEART_RATE", true)
        raw.put("failedFields", JSONArray().put("averageHeartRate"))
        assertEquals(Metrics.Status.ERROR, status(raw, "averageHeartRate"))
        raw.remove("failedFields")
        raw.put("warnings", JSONArray().put("READ_HEART_RATE: failed"))
        assertEquals(Metrics.Status.ERROR, status(raw, "averageHeartRate"))
    }
    @Test fun historicalEnergyNeedsItsOwnDayTimestampAndActualSamsungOriginIncludingValidZero() {
        val raw = day().put("energyScore", 0).put("energyScoreSource", "com.sec.android.app.shealth")
            .put("energyScoreTime", "2026-11-02T07:00:00Z")
        assertEquals(Metrics.Status.AVAILABLE, status(raw, "energyScore"))
        raw.put("energyScoreTime", "2026-11-03T07:00:00Z")
        assertEquals(Metrics.Status.NO_DATA, status(raw, "energyScore"))
        raw.put("energyScoreTime", "2026-11-02T07:00:00Z").put("energyScoreSource", "trainpilot_estimate")
        assertEquals(Metrics.Status.NO_DATA, status(raw, "energyScore"))
    }
}

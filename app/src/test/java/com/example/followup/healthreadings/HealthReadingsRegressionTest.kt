package com.example.followup.healthreadings

import com.repforge.app.HealthJournalStore
import com.repforge.app.healthreadings.HealthReadingsProjection
import com.repforge.app.healthreadings.ReadingState
import com.repforge.app.healthreadings.RecoverySource
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
class HealthReadingsRegressionTest {
    private val hc = HealthJournalStore.HC
    private val samsung = HealthJournalStore.SAMSUNG
    private val day = LocalDate.parse("2026-11-02")
    private val now = Instant.parse("2026-11-02T20:00:00Z")
    private fun row(provider: String = hc, date: LocalDate = day): JSONObject =
        JSONObject().put("provider", provider).put("day", date.toString())
    private fun pulse(at: String, bpm: Int) =
        JSONObject().put("time", at).put("bpm", bpm).put("source", "origin.one")
    private fun read(provider: String, data: JSONObject?) =
        HealthReadingsProjection.pulseDay(provider, day, data, now, ZoneOffset.UTC)

    @Test fun actualHcAggregateIsSeparateFromTimestampedLastAndResting() {
        val raw = row().put("averageHeartRate", 71.5).put("restingHeartRate", 57)
            .put("minHeartRate", 52).put("maxHeartRate", 126).put("heartRateSamples", 18)
            .put("heartRateSeries", JSONArray()
                .put(pulse("2026-11-02T12:00:00Z", 78))
                .put(pulse("2026-11-02T17:00:00Z", 82)))
        val value = read(hc, raw)
        assertEquals(ReadingState.AVAILABLE, value.state)
        assertEquals(71.5, value.averageBpm!!, 0.001)
        assertEquals("HC napi aggregált átlag", value.averageDescriptionHu)
        assertEquals(82, value.lastBpm)
        assertEquals(Instant.parse("2026-11-02T17:00:00Z"), value.lastAt)
        assertEquals(57.0, value.restingBpm!!, 0.001)
        assertEquals(52.0, value.observedMinBpm!!, 0.001)
        assertEquals(126.0, value.observedMaxBpm!!, 0.001)
        assertEquals(18L, value.totalSamples)
        assertEquals(2, value.displayedSamples.size)
    }

    @Test fun boundedSeriesOnlyHasLastValueNeverInventedDailyAverage() {
        val samples = JSONArray()
        for (i in 0 until 140) {
            samples.put(pulse("2026-11-02T${(i / 60).toString().padStart(2, '0')}:${(i % 60).toString().padStart(2, '0')}:00Z", 60 + i % 20))
        }
        val value = read(samsung, row(samsung).put("heartRateSeries", samples).put("heartRateSamples", 140))
        assertNull(value.averageBpm)
        assertNull(value.averageDescriptionHu)
        assertEquals(96, value.displayedSamples.size)
        assertEquals(140L, value.totalSamples)
        assertEquals(79, value.lastBpm)
        assertTrue(value.mainLabelHu.contains("Utolsó"))
    }

    @Test fun restingHeartRateAloneIsNotPretendedToBeDailyAverageOrLivePulse() {
        val value = read(hc, row().put("restingHeartRate", 55))
        assertEquals(ReadingState.AVAILABLE, value.state)
        assertNull(value.averageBpm)
        assertNull(value.lastBpm)
        assertEquals(55.0, value.restingBpm!!, 0.001)
        assertTrue(value.mainLabelHu.contains("Nyugalmi"))
    }

    @Test fun zeroNullAndMissingDoNotTurnIntoZeroBpmReadings() {
        for (raw in listOf(row(), row().put("averageHeartRate", 0).put("restingHeartRate", 0),
            row().put("averageHeartRate", JSONObject.NULL).put("restingHeartRate", JSONObject.NULL))) {
            val value = read(hc, raw)
            assertEquals(ReadingState.NO_DATA, value.state)
            assertNull(value.averageBpm)
            assertNull(value.restingBpm)
            assertNull(value.lastBpm)
        }
        assertEquals(ReadingState.NOT_SYNCED, read(hc, null).state)
    }

    @Test fun wrongProviderOrDayCannotPopulateSelectedChannel() {
        val raw = row(samsung).put("averageHeartRate", 72)
        assertEquals(ReadingState.WRONG_DAY_OR_PROVIDER, read(hc, raw).state)
        assertNull(read(hc, raw).averageBpm)
        val yesterday = row(hc, day.minusDays(1)).put("averageHeartRate", 78)
        assertEquals(ReadingState.WRONG_DAY_OR_PROVIDER, read(hc, yesterday).state)
        assertNull(read(hc, yesterday).averageBpm)
    }

    @Test fun sevenWeekRowsAreDateScopedAndNeverReuseOtherProvidersOrDates() {
        val earlier = row(hc, day.minusDays(1)).put("averageHeartRate", 62)
        val tomorrow = row(hc, day.plusDays(1)).put("averageHeartRate", 88)
        val foreign = row(samsung, day).put("averageHeartRate", 105)
        val values = HealthReadingsProjection.pulseWeek(hc, day.minusDays(2),
            listOf(earlier, tomorrow, foreign), now, ZoneOffset.UTC, 7)
        assertEquals(7, values.size)
        assertEquals(62.0, values[1].averageBpm!!, 0.001)
        assertNull(values[2].averageBpm)
        assertEquals(ReadingState.NOT_SYNCED, values[2].state)
        // Tomorrow can be present in storage but must not produce a future measurement.
        assertEquals(ReadingState.NOT_SYNCED, values[3].state)
        assertNull(values[3].averageBpm)
    }

    @Test fun revokedHeartGrantDoesNotLeakCachedPulseIntoUi() {
        val raw = row().put("averageHeartRate", 80)
            .put("permissions", JSONObject().put("READ_HEART_RATE", false).put("READ_RESTING_HEART_RATE", false))
        val result = read(hc, raw)
        assertEquals(ReadingState.PERMISSION_DENIED, result.state)
        assertNull(result.averageBpm)
        assertNull(result.lastBpm)
        val rhr = row().put("averageHeartRate", 80).put("restingHeartRate", 58)
            .put("permissions", JSONObject().put("READ_HEART_RATE", false).put("READ_RESTING_HEART_RATE", true))
        val partial = read(hc, rhr)
        assertEquals(ReadingState.AVAILABLE, partial.state)
        assertNull(partial.averageBpm)
        assertEquals(58.0, partial.restingBpm!!, 0.001)
    }

    @Test fun sdkFailureIsNotMisdiagnosedAsPermissionOrNoData() {
        val raw = row(samsung).put("averageHeartRate", 73)
            .put("permissions", JSONObject().put("READ_HEART_RATE", true))
            .put("errors", JSONObject().put("averageHeartRate", "SAMSUNG_HEALTH_2001"))
            .put("failedFields", JSONArray().put("averageHeartRate"))
        val result = read(samsung, raw)
        assertEquals(ReadingState.READ_ERROR, result.state)
        assertNull(result.averageBpm)
        assertTrue(result.explanationHu.contains("SDK"))
    }

    @Test fun samplesMustBelongToExactLocalDayAndNotFuture() {
        val raw = row().put("heartRateSeries", JSONArray()
            .put(pulse("2026-11-01T23:59:59Z", 61))
            .put(pulse("2026-11-02T09:00:00Z", 71))
            .put(pulse("2026-11-02T09:00:00Z", 71))
            .put(pulse("2026-11-02T20:01:00Z", 90))
            .put(pulse("2026-11-03T00:00:00Z", 91))
            .put(pulse("not-a-time", 92)))
        val result = read(hc, raw)
        assertEquals(listOf(71), result.displayedSamples.map { it.bpm })
        assertNull(result.averageBpm)
        assertEquals(71, result.lastBpm)
    }

    @Test fun validTodaySamsungEnergyUsesCanonicalReadiness() {
        val energy = row(samsung).put("energyScore", 83)
            .put("energyScoreTime", "2026-11-02T18:00:00Z")
            .put("energyScoreSource", "samsung_health")
            .put("permissions", JSONObject().put("READ_ENERGY_SCORE", true))
        val result = HealthReadingsProjection.recoveryToday(samsung, listOf(energy), emptyList(), now, ZoneOffset.UTC)
        assertEquals(RecoverySource.SAMSUNG_ENERGY, result.source)
        assertEquals(83.0, result.samsungEnergy!!, 0.001)
        assertEquals(83.0, result.score!!, 0.001)
        assertNull(result.trainPilotEstimate)
    }

    @Test fun missingOrStaleEnergyDoesNotCreateFakeReadinessScore() {
        val stale = row(samsung).put("energyScore", 87)
            .put("energyScoreTime", "2026-10-31T10:00:00Z")
            .put("energyScoreSource", "samsung_health")
        val result = HealthReadingsProjection.recoveryToday(samsung, listOf(stale), emptyList(), now, ZoneOffset.UTC)
        assertEquals(RecoverySource.NONE, result.source)
        assertNull(result.score)
        assertNull(result.trainPilotEstimate)
        assertNull(result.samsungEnergy)
        val zero = row(samsung).put("energyScore", 0)
            .put("energyScoreTime", "2026-11-02T18:00:00Z").put("energyScoreSource", "samsung_health")
        val zeroReading = HealthReadingsProjection.recoveryToday(samsung, listOf(zero), emptyList(), now, ZoneOffset.UTC)
        assertEquals(RecoverySource.SAMSUNG_ENERGY, zeroReading.source)
        assertEquals(0.0, zeroReading.score!!, 0.001)
        assertEquals(0.0, zeroReading.samsungEnergy!!, 0.001)
    }

    @Test fun healthConnectCannotInheritSamsungScoreEmbeddedInWrongRow() {
        val hcRow = row().put("energyScore", 91)
            .put("energyScoreSource", "samsung_health")
            .put("energyScoreTime", "2026-11-02T19:00:00Z")
        val result = HealthReadingsProjection.recoveryToday(hc, listOf(hcRow), emptyList(), now, ZoneOffset.UTC)
        assertEquals(RecoverySource.NONE, result.source)
        assertNull(result.samsungEnergy)
        assertNull(result.score)
    }

    @Test fun realRecentSleepSupportsLabeledTrainPilotEstimate() {
        val sleep = JSONObject().put("source", "hc.origin")
            .put("start", "2026-11-02T00:00:00Z").put("end", "2026-11-02T07:20:00Z")
        val hcDay = row().put("sleepSessions", JSONArray().put(sleep))
        val result = HealthReadingsProjection.recoveryToday(hc, listOf(hcDay), emptyList(), now, ZoneOffset.UTC)
        assertEquals(RecoverySource.TRAINPILOT_ESTIMATE, result.source)
        assertNotNull(result.trainPilotEstimate)
        assertNull(result.samsungEnergy)
        assertEquals(result.trainPilotEstimate!!.toDouble(), result.score!!, 0.001)
    }


    @Test fun hcReadWarningsIdentifySdkFailureWithoutBlamingUnrelatedMetrics() {
        val failed = row().put("warnings", JSONArray().put("READ_HEART_RATE: provider transport failure"))
        assertEquals(ReadingState.READ_ERROR, read(hc, failed).state)
        val unrelated = row().put("warnings", JSONArray().put("READ_STEPS: provider transport failure"))
        assertEquals(ReadingState.NO_DATA, read(hc, unrelated).state)
        val diagnosed = row().put("hcReadDiagnosis", JSONObject().put("readErrorFields",
            JSONArray().put("READ_RESTING_HEART_RATE: aggregation failure")))
        assertEquals(ReadingState.READ_ERROR, read(hc, diagnosed).state)
    }

    @Test fun channelMismatchIsNotReusedAsSelectedProvider() {
        val wrongChannel = row().put("channel", samsung).put("averageHeartRate", 100)
        assertEquals(ReadingState.WRONG_DAY_OR_PROVIDER, read(hc, wrongChannel).state)
        assertNull(read(hc, wrongChannel).averageBpm)
    }

    @Test fun yesterdaySleepCannotFillAnEmptyTodayRecovery() {
        val yesterday = day.minusDays(1)
        val olderSleep = JSONObject().put("source", "hc.origin")
            .put("start", "2026-11-01T12:00:00Z").put("end", "2026-11-01T19:00:00Z")
        val oldRow = row(hc, yesterday).put("sleepSessions", JSONArray().put(olderSleep))
        val todayEmpty = row()
        val result = HealthReadingsProjection.recoveryToday(hc,
            listOf(oldRow, todayEmpty), emptyList(), now, ZoneOffset.UTC)
        assertEquals(RecoverySource.NONE, result.source)
        assertNull(result.score)
        assertNull(result.trainPilotEstimate)
        val missingToday = HealthReadingsProjection.recoveryToday(hc,
            listOf(oldRow), emptyList(), now, ZoneOffset.UTC)
        assertEquals(ReadingState.NOT_SYNCED, missingToday.state)
    }

    @Test fun healthConnectRecoveryDeniedAndReadErrorRemainDifferent() {
        val denied = row().put("permissions", JSONObject()
            .put("READ_SLEEP", false).put("READ_HEART_RATE_VARIABILITY", false))
        assertEquals(ReadingState.PERMISSION_DENIED, HealthReadingsProjection.recoveryToday(
            hc, listOf(denied), emptyList(), now, ZoneOffset.UTC).state)
        val failed = row().put("permissions", JSONObject()
            .put("READ_SLEEP", true).put("READ_HEART_RATE_VARIABILITY", true))
            .put("warnings", JSONArray().put("READ_SLEEP: unavailable"))
        assertEquals(ReadingState.READ_ERROR, HealthReadingsProjection.recoveryToday(
            hc, listOf(failed), emptyList(), now, ZoneOffset.UTC).state)
    }

    @Test fun deniedEnergyGrantIsSeparateFromSdkError() {
        val denied = row(samsung).put("energyScore", 87)
            .put("energyScoreTime", "2026-11-02T18:00:00Z")
            .put("energyScoreSource", "samsung_health")
            .put("permissions", JSONObject().put("READ_ENERGY_SCORE", false))
        val failure = row(samsung).put("permissions", JSONObject().put("READ_ENERGY_SCORE", true))
            .put("errors", JSONObject().put("energyScore", "SAMSUNG_HEALTH_1001"))
        val deniedResult = HealthReadingsProjection.recoveryToday(samsung, listOf(denied), emptyList(), now, ZoneOffset.UTC)
        assertEquals(ReadingState.PERMISSION_DENIED, deniedResult.state)
        assertNull(deniedResult.samsungEnergy)
        assertNull(deniedResult.score)
        assertEquals(ReadingState.READ_ERROR,
            HealthReadingsProjection.recoveryToday(samsung, listOf(failure), emptyList(), now, ZoneOffset.UTC).state)
    }
}

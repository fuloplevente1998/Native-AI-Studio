package com.repforge.app.ui.health

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.HealthJournalStore
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NativeHealthJournalRegressionTest {
    private val day = LocalDate.parse("2026-11-02")
    private val now = Instant.parse("2026-11-03T18:00:00Z")
    private val hc = HealthJournalStore.HC
    private val samsung = HealthJournalStore.SAMSUNG
    private fun record(id: String, provider: String = hc, source: String = "watch",
        date: LocalDate = day, type: String = "StepsRecord", data: JSONObject = JSONObject().put("value", 500)) =
        JSONObject().put("recordId", id).put("channel", provider).put("source", source).put("type", type)
            .put("day", date.toString()).put("startMs", date.atTime(10, 0).toInstant(ZoneOffset.UTC).toEpochMilli())
            .put("endMs", date.atTime(11, 0).toInstant(ZoneOffset.UTC).toEpochMilli())
            .put("lastModifiedMs", now.toEpochMilli()).put("data", data)

    private fun withStore(block: (HealthJournalStore) -> Unit) {
        HealthJournalStore(ApplicationProvider.getApplicationContext<Context>()).use { store ->
            store.erase()
            try { block(store) } finally { store.erase() }
        }
    }

    @Test fun providerSummaryAndDetailsReuseSavedPriorityTotalWithoutAddingMirrors() = withStore { store ->
        val mirrored = JSONArray().put(record("mirror-hc")).put(record("mirror-samsung", samsung))
            .put(record("overlap", source = "phone"))
        store.importRecords(mirrored, store.generation())
        store.healthDay(day.toString(), JSONObject().put("steps", 500).put("activeCalories", 0), store.generation())
        store.samsungDay(day.toString(), JSONObject().put("steps", 500), JSONArray(), store.generation())
        val before = store.exportSnapshot().toString()
        val preferences = store.preferences().toString()
        for (provider in listOf(hc, samsung)) {
            val snapshot = NativeHealthJournal.load(store, provider, "", HealthJournalRange(day, day))
            val raw = NativeHealthJournal.day(snapshot, provider, "", day)!!
            val parsed = NativeHealthMetrics.parse(ApplicationProvider.getApplicationContext(),
                NativeDetailedHealthMetrics.admitted(raw, provider, day, now, ZoneOffset.UTC), "en")
            assertEquals("500", parsed.first { it.key == "steps" }.valueText)
            assertEquals(500, raw.getInt("steps"))
            assertTrue(NativeHealthRecords(store).load(HealthRecordQuery(day, provider)).rows.all { it.channel == provider })
        }
        assertEquals(before, store.exportSnapshot().toString())
        assertEquals(preferences, store.preferences().toString())
    }

    @Test fun sourceAndDaySelectionNeverBorrowAnotherOriginProviderOrDate() = withStore { store ->
        store.healthDay(day.toString(), JSONObject().put("steps", 900), store.generation())
        store.healthDay(day.toString(), JSONObject().put("source", "watch").put("steps", 320), store.generation())
        store.healthDay(day.minusDays(1).toString(), JSONObject().put("steps", 8000), store.generation())
        store.importRecords(JSONArray().put(record("watch")).put(record("phone", source = "phone"))
            .put(record("old", source = "old", date = day.minusDays(8)))
            .put(record("other", samsung, "other-provider")), store.generation())
        val page = NativeHealthJournal.load(store, hc, "watch", HealthJournalRange(day.minusDays(6), day))
        assertEquals(320, NativeHealthJournal.day(page, hc, "watch", day)!!.getInt("steps"))
        assertNull(NativeHealthJournal.day(page, hc, "phone", day))
        assertNull(NativeHealthJournal.day(page, hc, "watch", day.minusDays(1)))
        assertNull(NativeHealthJournal.day(page, samsung, "watch", day))
        val origins = page.getJSONArray("sources")
        assertEquals(setOf("watch", "phone"), (0 until origins.length()).map { origins.getJSONObject(it).getString("source") }.toSet())
        assertEquals(listOf("watch"), NativeHealthRecords(store).load(HealthRecordQuery(day, hc, "watch")).rows.map { it.id })
        val missing = NativeHealthJournal.load(store, hc, "phone", HealthJournalRange(day, day))
        assertNull(NativeHealthJournal.day(missing, hc, "phone", day))
    }

    @Test fun equalTimeRecordsAreStableDeduplicatedAndPagedWithinOneSourceAndDay() = withStore { store ->
        val records = JSONArray()
        repeat(57) { records.put(record("r$it")) }
        records.put(record("unselected", source = "phone")).put(record("other-day", date = day.minusDays(1)))
        store.importRecords(records, store.generation())
        // Idempotent reimport must not move or duplicate same-timestamp rows.
        store.importRecords(records, store.generation())
        val reader = NativeHealthRecords(store)
        val first = reader.load(HealthRecordQuery(day, hc, "watch"))
        assertEquals((56 downTo 32).map { "r$it" }, first.rows.map { it.id })
        val secondQuery = HealthRecordQuery(day, hc, "watch", cursor = first.next, generation = first.generation)
        val second = reader.load(secondQuery)
        val third = reader.load(secondQuery.copy(cursor = second.next))
        assertEquals(listOf(25, 25, 7), listOf(first, second, third).map { it.rows.size })
        assertEquals(57, (first.rows + second.rows + third.rows).map { it.id }.distinct().size)
        assertNull(third.next)
        assertEquals(first.rows, reader.load(HealthRecordQuery(day, hc, "watch")).rows)
        assertNull(secondQuery.withFilters(source = "phone").cursor)
        assertNull(secondQuery.withFilters(source = "phone").generation)
        assertNull(secondQuery.withRange(day.minusDays(1), day).cursor)
    }

    @Test fun replacementRejectsAnOldRecordCursorAndPendingJournalLoad() = withStore { store ->
        store.importRecords(JSONArray().put(record("r")), store.generation())
        val query = HealthRecordQuery(day, hc, generation = store.generation())
        val token = store.prepareRestore(store.exportSnapshot())
        try {
            assertThrows(NativeHealthRecords.ReplacedJournal::class.java) {
                NativeHealthJournal.load(store, hc, "", HealthJournalRange(day, day))
            }
        } finally { store.finishRestore(token, false) }
        assertThrows(NativeHealthRecords.ReplacedJournal::class.java) { NativeHealthRecords(store).load(query) }
    }

    @Test fun missingZeroDeniedAndFailedMeasurementsRemainDistinct() {
        val raw = JSONObject().put("day", day.toString()).put("channel", hc).put("steps", 0)
            .put("averageHeartRate", 72).put("permissions", JSONObject().put("READ_HEART_RATE", false))
            .put("activeCalories", 12).put("errors", JSONObject().put("activeCalories", "READ_FAILED"))
        fun status(key: String) = NativeDetailedHealthMetrics.status(raw, key, hc, day, now, ZoneOffset.UTC)
        assertEquals(NativeDetailedHealthMetrics.Status.AVAILABLE, status("steps"))
        assertEquals(NativeDetailedHealthMetrics.Status.NO_DATA, status("weightKg"))
        assertEquals(NativeDetailedHealthMetrics.Status.DENIED, status("averageHeartRate"))
        assertEquals(NativeDetailedHealthMetrics.Status.ERROR, status("activeCalories"))
        val admitted = NativeDetailedHealthMetrics.admitted(raw, hc, day, now, ZoneOffset.UTC)
        assertEquals(0, admitted.getInt("steps"))
        assertFalse(admitted.has("averageHeartRate"))
        assertFalse(admitted.has("activeCalories"))
    }

    @Test fun metadataPreservesBodyCompositionOriginAndRejectsWrongDayTime() {
        val raw = JSONObject().put("day", day.toString()).put("channel", samsung).put("bodyFatMassKg", 14)
            .put("bodyFatMassSource", "com.sec.android.app.shealth").put("bodyFatMassTime", "2026-11-02T08:30:00Z")
        assertEquals("com.sec.android.app.shealth", NativeDetailedHealthMetrics.origin(raw, "bodyFatMassKg"))
        assertEquals("2026-11-02T08:30:00Z", NativeDetailedHealthMetrics.timestamp(raw, "bodyFatMassKg"))
        assertEquals(NativeDetailedHealthMetrics.Status.AVAILABLE,
            NativeDetailedHealthMetrics.status(raw, "bodyFatMassKg", samsung, day, now, ZoneOffset.UTC))
        raw.put("bodyFatMassTime", "2026-11-01T08:30:00Z")
        assertEquals(NativeDetailedHealthMetrics.Status.NO_DATA,
            NativeDetailedHealthMetrics.status(raw, "bodyFatMassKg", samsung, day, now, ZoneOffset.UTC))
    }

    @Test fun sleepUsesLocalWakeDayAndUnionsOverlapsWithoutProviderFallback() {
        fun session(start: String, end: String) = JSONObject().put("start", start).put("end", end)
        val raw = JSONObject().put("channel", hc).put("day", day.toString()).put("sleepSessions", JSONArray()
            .put(session("2026-11-01T22:00:00Z", "2026-11-02T06:00:00Z"))
            .put(session("2026-11-01T23:00:00Z", "2026-11-02T06:00:00Z"))
            .put(session("2026-11-02T14:00:00Z", "2026-11-02T15:00:00Z")))
        assertEquals(540.0, NativeHealthJournal.sleepMinutes(raw, hc, day, now, ZoneOffset.UTC)!!, 0.001)
        assertNull(NativeHealthJournal.sleepMinutes(raw, samsung, day, now, ZoneOffset.UTC))
        assertNull(NativeHealthJournal.sleepMinutes(raw, hc, day.minusDays(1), now, ZoneOffset.UTC))
        raw.put("permissions", JSONObject().put("READ_SLEEP", false))
        assertNull(NativeHealthJournal.sleepMinutes(raw, hc, day, now, ZoneOffset.UTC))
    }

    @Test fun sleepStagesAndPulseSamplesMaterializeOnlyTheirCurrentTwentyItems() {
        val stages = JSONArray()
        val samples = JSONArray()
        repeat(47) { i ->
            stages.put(JSONObject().put("startMs", 1000L + i * 1000L).put("endMs", 2000L + i * 1000L).put("type", 5))
            samples.put(JSONObject().put("timeMs", 1000L + i * 1000L).put("value", 60 + i))
        }
        for ((key, values) in listOf("stages" to stages, "samples" to samples)) {
            val data = JSONObject().put(key, values)
            assertEquals(listOf(20, 20, 7), listOf(0, 20, 40).map { NativeHealthRecordSeries.page(data, it).items.size })
            assertEquals(47, NativeHealthRecordSeries.page(data, 0).total)
            assertEquals(21000L, NativeHealthRecordSeries.page(data, 20).items.first().startMs)
            assertTrue(NativeHealthRecordSeries.page(data, 60).items.isEmpty())
        }
        assertNull(NativeHealthRecordSeries.page(JSONObject().put("samples", JSONArray().put(JSONObject())), 0).items.first().value)
    }

    @Test fun localDatePaginationIsBoundedContiguousAndReversibleAcrossDst() {
        val from = LocalDate.parse("2026-10-20")
        val to = LocalDate.parse("2026-11-02")
        val range = HealthJournalRange(from, to)
        assertEquals(7, range.days.size)
        assertEquals(LocalDate.parse("2026-10-27"), range.pageStart)
        val older = range.older()
        assertEquals(from, older.pageStart)
        assertEquals(14, (range.days + older.days).distinct().size)
        assertEquals(range, older.newer())
        val zone = ZoneId.of("Europe/Budapest")
        assertEquals(25, java.time.Duration.between(LocalDate.parse("2026-10-25").atStartOfDay(zone),
            LocalDate.parse("2026-10-26").atStartOfDay(zone)).toHours().toInt())
        assertEquals(listOf(from), HealthJournalRange(from, from).days)
        assertThrows(IllegalArgumentException::class.java) { HealthJournalRange(to, from) }
    }
}

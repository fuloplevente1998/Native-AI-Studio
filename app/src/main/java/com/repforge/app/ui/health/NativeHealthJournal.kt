package com.repforge.app.ui.health

import com.repforge.app.NativeDailyHealthDetails
import com.repforge.app.HealthJournalStore
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Inclusive local dates; advancing a page never assumes that a day has 24 hours. */
internal data class HealthJournalRange(val from: LocalDate, val to: LocalDate, val pageEnd: LocalDate = to) {
    init {
        require(HealthJournalStore.validDay(from.toString()) && HealthJournalStore.validDay(to.toString()))
        require(from <= pageEnd && pageEnd <= to)
    }
    val pageStart: LocalDate get() = maxOf(from, pageEnd.minusDays(PAGE_DAYS - 1L))
    val days: List<LocalDate> get() = (0..ChronoUnit.DAYS.between(pageStart, pageEnd).toInt()).map { pageEnd.minusDays(it.toLong()) }
    fun older(): HealthJournalRange = copy(pageEnd = pageStart.minusDays(1))
    fun newer(): HealthJournalRange = copy(pageEnd = minOf(to, pageEnd.plusDays(PAGE_DAYS.toLong())))
    companion object { const val PAGE_DAYS = 7 }
}

/** One persisted provider/source projection supplies both the compact day and its 26 details.
 * Provider-priority SDK totals are kept intact: individual records are never re-summed here.
 */
internal object NativeHealthJournal {
    fun load(store: HealthJournalStore, provider: String, source: String, range: HealthJournalRange): JSONObject {
        require(provider in setOf(HealthJournalStore.HC, HealthJournalStore.SAMSUNG))
        require(source.length <= 255)
        val generation = store.generation()
        if (store.pendingRestore().isNotEmpty()) throw NativeHealthRecords.ReplacedJournal()
        val from = range.pageStart.toString()
        val to = range.pageEnd.toString()
        val projection = store.page(from, to, provider, source, 0, 0, 1)
        // The record-only query scopes origin discovery to these same seven days/provider.
        // Unlike summaries, this query's origin list is range-scoped by the existing store.
        val origins = store.page(from, to, provider, "", 0, 0, 1, includeSummaries = false).getJSONArray("sources")
        if (generation != store.generation() || store.pendingRestore().isNotEmpty()) throw NativeHealthRecords.ReplacedJournal()
        return JSONObject().put("days", projection.getJSONArray("days")).put("sources", origins).put("generation", generation)
    }

    fun sleepMinutes(raw: JSONObject?, provider: String, date: LocalDate,
        now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Double? {
        if (raw == null || raw.optString("channel") != provider || raw.optString("day") != date.toString() ||
            date > now.atZone(zone).toLocalDate()) return null
        val permissions = raw.optJSONObject("permissions")
        if (permissions?.has("READ_SLEEP") == true && !permissions.optBoolean("READ_SLEEP")) return null
        val errors = raw.optJSONObject("errors")
        if (listOf("sleepMinutes", "sleepSessions").any { errors?.optString(it).orEmpty().isNotBlank() }) return null
        val failed = raw.optJSONArray("failedFields")
        if ((0 until (failed?.length() ?: 0)).any { failed?.optString(it) in setOf("sleepMinutes", "sleepSessions") }) return null
        val warnings = raw.optJSONArray("warnings")
        if ((0 until (warnings?.length() ?: 0)).any { warnings?.optString(it)?.startsWith("READ_SLEEP:") == true }) return null
        return NativeDailyHealthDetails.sleepMinutes(raw, date, zone, now)
    }

    fun day(snapshot: JSONObject, provider: String, source: String, date: LocalDate): JSONObject? {
        val days = snapshot.getJSONArray("days")
        return (0 until days.length()).map { days.getJSONObject(it) }.firstOrNull {
            it.optString("day") == date.toString() && it.optString("channel") == provider &&
                it.optString("source") == source.ifEmpty { "@aggregate" }
        }
    }
}

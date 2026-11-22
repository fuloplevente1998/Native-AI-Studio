package com.repforge.app.ui.coach

import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Accepted rf245Recovery/rf250SleepSpans/rf229ApplyLatestRecovery, from real journal days. */
internal object NativeCoachSignals {
    data class Snapshot(val recovery: JSONObject, val daily: JSONObject)
    fun fromDays(days: List<JSONObject>, today: LocalDate, zone: ZoneId): Snapshot {
        val valid = days.mapNotNull { day -> runCatching { LocalDate.parse(day.getString("day")) to day }.getOrNull() }
            .filter { it.first <= today }.sortedByDescending { it.first }
        val sessions = valid.flatMap { (_, day) ->
            val rows = day.optJSONArray("sleepSessions")
            (0 until (rows?.length() ?: 0)).mapNotNull { rows?.optJSONObject(it) }
        }.distinctBy { listOf(it.optString("source"), it.optString("start"), it.optString("end")) }
            .groupBy { parse(it.optString("end"))?.atZone(zone)?.toLocalDate() }
        val recoveryRows = valid.map { (date, input) ->
            val row = JSONObject(input.toString())
            val sleep = sessions[date].orEmpty()
            // Use one device's sleep intervals for the day. Prefer the best measured coverage,
            // regardless of manufacturer, and never add matching records from other origins.
            val (source, merged) = sleep.groupBy { it.optString("source") }
                .mapValues { (_, sessions) -> mergeSleep(sessions) }
                .maxByOrNull { (_, spans) -> sleepMillis(spans) }
                ?.let { it.key to it.value } ?: (null to emptyList())
            // A measured daily total can stand on its own. Awake-only stages cannot.
            if (sleep.isNotEmpty()) {
                row.put("sleepMinutes", if (merged.isEmpty()) JSONObject.NULL else sleepMillis(merged) / 60_000.0).put("sleepStart", merged.firstOrNull()?.first?.toString() ?: JSONObject.NULL)
                    .put("sleepEnd", merged.lastOrNull()?.second?.toString() ?: JSONObject.NULL)
                    .put("sleepSource", source ?: JSONObject.NULL)
            }
            date to row
        }
        val latest = recoveryRows.firstOrNull { (_, row) -> listOf("sleepMinutes", "hrvRmssdMs", "energyScore").any { number(row, it) != null } }
        val recovery = latest?.let { (date, row) ->
            val hrvSource = row.optString("hrvSource").takeUnless { row.isNull("hrvSource") || it.isBlank() }
            val baseline = recoveryRows.filter { (d, previous) -> hrvSource != null && d < date &&
                d >= date.minusDays(7) && previous.optString("hrvSource") == hrvSource }
                .mapNotNull { number(it.second, "hrvRmssdMs")?.takeIf { n -> n > 0 } }.sorted()
            val median = if (baseline.size < 3) null else if (baseline.size % 2 == 1) baseline[baseline.size / 2]
                else (baseline[baseline.size / 2 - 1] + baseline[baseline.size / 2]) / 2
            row.put("hrvBaselineMs", median ?: JSONObject.NULL).put("baselineSamples", baseline.size)
        } ?: JSONObject()
        return Snapshot(recovery, valid.firstOrNull { it.first == today }?.second?.let { JSONObject(it.toString()) } ?: JSONObject())
    }
    private fun mergeSleep(sessions: List<JSONObject>): List<Pair<Instant, Instant>> {
        val spans = sessions.flatMap { session ->
            val (start, end) = span(session) ?: return@flatMap emptyList()
            val stages = session.optJSONArray("stages")
            if (stages == null || stages.length() == 0) listOf(start to end)
            else (0 until stages.length()).mapNotNull { index ->
                val stage = stages.optJSONObject(index)?.takeIf { it.optBoolean("isAsleep") }
                    ?.let(::span) ?: return@mapNotNull null
                val from = maxOf(start, stage.first)
                val to = minOf(end, stage.second)
                (from to to).takeIf { from < to }
            }
        }.sortedBy { it.first }
        val merged = ArrayList<Pair<Instant, Instant>>()
        for (span in spans) {
            val last = merged.lastOrNull()
            if (last != null && span.first <= last.second) {
                merged[merged.lastIndex] = last.first to maxOf(last.second, span.second)
            } else {
                merged.add(span)
            }
        }
        return merged
    }

    private fun sleepMillis(spans: List<Pair<Instant, Instant>>): Long =
        spans.sumOf { (from, to) -> Duration.between(from, to).toMillis() }

    private fun parse(value: String) = runCatching { Instant.parse(value) }.getOrNull()
    private fun span(row: JSONObject): Pair<Instant, Instant>? {
        val start = parse(row.optString("start")) ?: return null
        val end = parse(row.optString("end")) ?: return null
        return (start to end).takeIf { end > start }
    }
    private fun number(row: JSONObject, key: String): Double? = if (row.isNull(key)) null else row.optDouble(key, Double.NaN).takeIf { it.isFinite() }
}

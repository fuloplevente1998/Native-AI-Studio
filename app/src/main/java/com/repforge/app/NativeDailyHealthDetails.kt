package com.repforge.app

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

/**
 * Presentation-only projections of the ONE selected provider's persisted daily result.
 * Missing readings remain unknown; no cross-provider merge and no changes to Room/backup data.
 */
internal object NativeDailyHealthDetails {
    data class PulsePoint(val timeIso: String, val bpm: Int, val source: String)

    fun pulseSeries(day: JSONObject?, limit: Int = 48): List<PulsePoint> {
        val values = day?.optJSONArray("heartRateSeries") ?: return emptyList()
        val unique = linkedMapOf<String, PulsePoint>()
        for (index in 0 until values.length()) {
            val sample = values.optJSONObject(index) ?: continue
            val at = sample.optString("time")
            val bpm = sample.optInt("bpm", 0)
            val parsed = runCatching { Instant.parse(at) }.getOrNull() ?: continue
            if (bpm !in 20..250) continue
            val source = sample.optString("source")
            unique["$source|$parsed"] = PulsePoint(parsed.toString(), bpm, source)
        }
        return unique.values.sortedBy { it.timeIso }.takeLast(limit.coerceIn(1, 96))
    }

    /**
     * A sleep session belongs to the local day on which it ENDS (including afternoon naps).
     * Overlapping stage/session intervals are unioned, not added together. Stage data, when
     * present, limits the sum to explicitly marked sleeping stages.
     */
    fun sleepMinutes(day: JSONObject?, date: LocalDate, zone: ZoneId, now: Instant): Double? {
        val sessions = day?.optJSONArray("sleepSessions")
        if (sessions == null || sessions.length() == 0) {
            return day?.optDouble("sleepMinutes", Double.NaN)?.takeIf { it.isFinite() && it > 0 }
        }
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = min(date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), now.toEpochMilli())
        if (dayEnd <= dayStart) return null
        val spans = mutableListOf<Pair<Long, Long>>()
        for (index in 0 until sessions.length()) {
            val session = sessions.optJSONObject(index) ?: continue
            val start = runCatching { Instant.parse(session.optString("start")).toEpochMilli() }.getOrNull() ?: continue
            val end = runCatching { Instant.parse(session.optString("end")).toEpochMilli() }.getOrNull() ?: continue
            if (end <= start || end <= dayStart || end > dayEnd) continue
            val stages = session.optJSONArray("stages")
            if (stages == null || stages.length() == 0) {
                spans += start to end
            } else {
                for (s in 0 until stages.length()) {
                    val stage = stages.optJSONObject(s) ?: continue
                    if (!stage.optBoolean("isAsleep", false)) continue
                    val a = runCatching { Instant.parse(stage.optString("start")).toEpochMilli() }.getOrNull() ?: continue
                    val b = runCatching { Instant.parse(stage.optString("end")).toEpochMilli() }.getOrNull() ?: continue
                    val from = max(start, a)
                    val to = min(end, b)
                    if (to > from) spans += from to to
                }
            }
        }
        if (spans.isEmpty()) return null
        val ordered = spans.sortedWith(compareBy<Pair<Long, Long>> { it.first }.thenBy { it.second })
        var start = ordered.first().first
        var end = ordered.first().second
        var totalMs = 0L
        for (span in ordered.drop(1)) {
            if (span.first <= end) end = max(end, span.second)
            else { totalMs += end - start; start = span.first; end = span.second }
        }
        totalMs += end - start
        return (totalMs / 60_000.0).takeIf { it > 0.0 }
    }
}

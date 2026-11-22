package com.repforge.app.ui.health

import org.json.JSONObject
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

/**
 * Presentation-only sleep semantics. A session's start/end describes the observation
 * window, not necessarily time asleep. Journal rows are never deleted or rewritten.
 */
internal object NativeSleepRecordPresentation {
    private fun timestamp(item: JSONObject, millis: String, iso: String): Long? {
        val raw = item.opt(millis)
        if (raw is Number && raw.toLong() >= 0L) return raw.toLong()
        return runCatching { Instant.parse(item.optString(iso)).toEpochMilli() }.getOrNull()
    }

    /** Null means the provider supplied no usable stage evidence; zero is a measured zero. */
    fun asleepMinutes(data: JSONObject, windowStartMs: Long, windowEndMs: Long): Double? {
        val stages = data.optJSONArray("stages") ?: return null
        if (stages.length() == 0 || windowEndMs <= windowStartMs) return null
        val sleeping = mutableListOf<Pair<Long, Long>>()
        for (i in 0 until stages.length()) {
            val stage = stages.optJSONObject(i) ?: continue
            if (!stage.optBoolean("isAsleep", false)) continue
            val a = timestamp(stage, "startMs", "start") ?: continue
            val b = timestamp(stage, "endMs", "end") ?: continue
            val start = max(windowStartMs, a)
            val end = min(windowEndMs, b)
            if (end > start) sleeping += start to end
        }
        if (sleeping.isEmpty()) return 0.0
        val sorted = sleeping.sortedWith(compareBy<Pair<Long, Long>> { it.first }.thenBy { it.second })
        var a = sorted.first().first
        var b = sorted.first().second
        var total = 0L
        for ((nextStart, nextEnd) in sorted.drop(1)) {
            if (nextStart <= b) b = max(b, nextEnd)
            else { total += b - a; a = nextStart; b = nextEnd }
        }
        return (total + b - a) / 60_000.0
    }

    /**
     * A semantic duplicate must have the same provider, origin, window and stages.
     * Differing real stages are preserved even when their session windows coincide.
     * Do not use the unstable SDK-parent record ID for duplicate identity.
     */
    fun duplicateKey(row: HealthRecordRow): String? {
        if (row.type != "SleepSessionRecord") return null
        val data = runCatching { JSONObject(row.data) }.getOrNull() ?: return null
        val stages = data.optJSONArray("stages")
        val details = if (stages == null || stages.length() == 0) {
            "no-stages|"+data.optString("durationMinutes")
        } else {
            (0 until stages.length()).mapNotNull { i ->
                val s = stages.optJSONObject(i) ?: return@mapNotNull null
                val a = timestamp(s, "startMs", "start") ?: return@mapNotNull null
                val b = timestamp(s, "endMs", "end") ?: return@mapNotNull null
                val stage = s.optString("stage").ifEmpty { s.optString("type") }
                "$a:$b:$stage:${s.optBoolean("isAsleep", false)}"
            }.sorted().joinToString(";")
        }
        return "${row.channel}|${row.source}|${row.startMs}|${row.endMs}|$details"
    }

    fun distinct(rows: List<HealthRecordRow>): List<HealthRecordRow> {
        val seen = HashSet<String>()
        return rows.filter { row -> duplicateKey(row)?.let { seen.add(it) } ?: true }
    }
}

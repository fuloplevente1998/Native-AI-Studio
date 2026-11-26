package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

/** Raw time availability, not proof of which records contributed to the SDK aggregate.
 * Never infer calories from duration, or add values from different origins. */
internal object WorkoutEnergyCoverage {
    data class Span(val start: Instant, val end: Instant, val origin: String)
    enum class Status { FULL_RAW, PARTIAL_RAW, UNKNOWN, MISSING }

    private fun millis(start: Instant, end: Instant) = Duration.between(start, end).toMillis()
    private fun union(spans: List<Pair<Instant, Instant>>): Long {
        var total = 0L
        var first: Instant? = null
        var last: Instant? = null
        for ((a,b) in spans.sortedBy { it.first }) {
            val previous = last
            if (previous == null || a > previous) {
                if (previous != null) total += millis(requireNotNull(first), previous)
                first = a; last = b
            } else if (b > previous) last = b
        }
        if (last != null) total += millis(requireNotNull(first), last)
        return total
    }

    fun record(out: JSONObject, key: String, start: Instant, end: Instant, spans: List<Span>,
        permitted: Boolean, permission: String, warnings: JSONArray) {
        val duration = millis(start,end)
        val failed = (0 until warnings.length()).any { warnings.optString(it).startsWith("$permission:") }
        val coverage = JSONObject().put("requestedMs",duration)
        val measured = NativeWorkoutHealth.metrics(out,false).containsKey(key)
        val status = when {
            !permitted || failed || duration <= 0 -> Status.UNKNOWN
            !measured -> Status.MISSING
            else -> {
                val clipped = spans.mapNotNull { span ->
                    val a = maxOf(start,span.start); val b = minOf(end,span.end)
                    if (span.origin.isBlank() || b <= a) null else Triple(a,b,span.origin)
                }
                val all = union(clipped.map { it.first to it.second })
                val origins = JSONObject()
                for ((origin,rows) in clipped.groupBy { it.third }) {
                    origins.put(origin, JSONObject().put("coveredMs",union(rows.map { it.first to it.second }))
                        .put("lastEnd",rows.maxOf { it.second }.toString()))
                }
                coverage.put("rawCoveredMs",all).put("origins",origins)
                clipped.maxOfOrNull { it.second }?.let { coverage.put("lastEnd",it.toString()) }
                if (all == duration) Status.FULL_RAW else Status.PARTIAL_RAW
            }
        }
        coverage.put("status",status.name)
        val fields = out.optJSONObject("energyCoverage") ?: JSONObject().also { out.put("energyCoverage",it) }
        fields.put(key,coverage)
    }

    /** Samsung ActivitySummary is a direct interval aggregate, not raw samples.
     * Its numeric value is authoritative for that query, but raw coverage is unknown. */
    fun recordDirectAggregate(out: JSONObject, key: String, start: Instant, end: Instant, permitted: Boolean) {
        val fields = out.optJSONObject("energyCoverage") ?: JSONObject().also { out.put("energyCoverage",it) }
        val errors = out.optJSONObject("errors")
        val failed = out.optJSONArray("failedFields")
        val failure = errors?.optString(key).orEmpty().isNotBlank() ||
            (0 until (failed?.length() ?: 0)).any { failed?.optString(it) == key }
        val available = NativeWorkoutHealth.metrics(out,false).containsKey(key)
        val reason = when {
            !permitted -> "PERMISSION_DENIED"
            failure -> "READ_FAILED"
            !available -> "EMPTY_RESPONSE"
            else -> "DIRECT_AGGREGATE_NO_RAW_COVERAGE"
        }
        fields.put(key,JSONObject().put("requestedMs",millis(start,end))
            .put("status",if (permitted && !failure && !available) Status.MISSING.name else Status.UNKNOWN.name)
            .put("reason",reason))
    }

    fun status(data: JSONObject, key: String): Status {
        val rows = data.optJSONArray("measurementWindows")
        if (rows != null && rows.length() > 0) {
            val statuses = (0 until rows.length()).map { index ->
                rows.optJSONObject(index)?.let { status(it,key) } ?: Status.UNKNOWN
            }
            return when {
                statuses.all { it == Status.FULL_RAW } -> Status.FULL_RAW
                statuses.all { it == Status.MISSING } -> Status.MISSING
                statuses.any { it == Status.PARTIAL_RAW || it == Status.MISSING } -> Status.PARTIAL_RAW
                else -> Status.UNKNOWN
            }
        }
        val field = data.optJSONObject("energyCoverage")?.optJSONObject(key) ?: return Status.UNKNOWN
        return runCatching { Status.valueOf(field.getString("status")) }.getOrDefault(Status.UNKNOWN)
    }

    // A union of accessible raw intervals does not establish the SDK aggregate's contributors.
    fun aggregateStatus(data: JSONObject, key: String): Status =
        status(data, key).let { if (it == Status.FULL_RAW) Status.UNKNOWN else it }

    fun rank(data: JSONObject, key: String): Int =
        if (aggregateStatus(data, key) == Status.UNKNOWN) 1 else 0

    fun knownMillis(data: JSONObject, key: String): Long? =
        data.optJSONObject("energyCoverage")?.optJSONObject(key)?.let {
            if (it.has("rawCoveredMs")) it.optLong("rawCoveredMs").takeIf { n -> n >= 0 } else null
        }
}

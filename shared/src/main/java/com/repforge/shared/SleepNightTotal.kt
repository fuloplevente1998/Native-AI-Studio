package com.repforge.shared

/**
 * #367/9: the ONE sleep total used by the phone summary and the watch display.
 *
 * Input spans are real asleep intervals (epoch ms) of one provider/window: whole sessions when the
 * source has no stages, otherwise only the stages it marks as asleep. Awake gaps between spans are
 * therefore never counted; overlapping or duplicated records (the same sleep read twice, a mirrored
 * copy, a stage inside its session) are unioned, never added. No spans → null (unknown, not 0).
 */
object SleepNightTotal {
    /** Union length in milliseconds, or null when no valid span exists. */
    fun unionMillis(spans: Iterable<Pair<Long, Long>>): Long? {
        val ordered = spans.filter { it.second > it.first }
            .sortedWith(compareBy<Pair<Long, Long>> { it.first }.thenBy { it.second })
        if (ordered.isEmpty()) return null
        var start = ordered.first().first
        var end = ordered.first().second
        var total = 0L
        for (span in ordered.drop(1)) {
            if (span.first <= end) end = maxOf(end, span.second)
            else { total += end - start; start = span.first; end = span.second }
        }
        total += end - start
        return total.takeIf { it > 0L }
    }

    /** Whole minutes for display; same rounding on phone and watch. */
    fun unionMinutes(spans: Iterable<Pair<Long, Long>>): Long? = unionMillis(spans)?.div(60_000L)?.takeIf { it > 0L }

    /** Phone → watch home snapshot key (additive, optional; older watches ignore it). */
    const val HOME_KEY = "sleepSummary367"
}

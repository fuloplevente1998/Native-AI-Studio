package com.example.ui.components

/** Only recorded points can produce markers. Null finish means one physical start/finish marker. */
internal data class RecordedMapEndpoints(
    val start: Pair<Double, Double>,
    val finish: Pair<Double, Double>?
)

internal fun recordedMapEndpoints(
    segments: List<List<Pair<Double, Double>>>
): RecordedMapEndpoints? {
    val start = segments.firstNotNullOfOrNull { it.firstOrNull() } ?: return null
    val finish = segments.lastOrNull { it.isNotEmpty() }?.last() ?: return null
    return RecordedMapEndpoints(start, finish.takeUnless { it == start })
}

/** Keep the original segment boundaries: a pause or ownership handover must be a visible gap. */
internal fun recordedPolylineSegments(
    segments: List<List<Pair<Double, Double>>>
): List<List<Pair<Double, Double>>> = segments.filter { it.size >= 2 }

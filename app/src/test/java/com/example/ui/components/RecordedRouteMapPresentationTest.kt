package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordedRouteMapPresentationTest {
    private val a = 47.0 to 18.0
    private val b = 47.1 to 18.1
    private val c = 47.2 to 18.2
    private val d = 47.3 to 18.3

    @Test fun emptyRouteHasNoMarkersOrLines() {
        assertNull(recordedMapEndpoints(emptyList()))
        assertNull(recordedMapEndpoints(listOf(emptyList(), emptyList())))
        assertEquals(emptyList<List<Pair<Double, Double>>>(), recordedPolylineSegments(emptyList()))
    }

    @Test fun singlePointHasExactlyOnePhysicalMarker() {
        assertEquals(RecordedMapEndpoints(a, null), recordedMapEndpoints(listOf(listOf(a))))
        assertEquals(emptyList<List<Pair<Double, Double>>>(), recordedPolylineSegments(listOf(listOf(a))))
    }

    @Test fun sameRecordedStartAndFinishDoesNotInventSecondPlace() {
        assertEquals(RecordedMapEndpoints(a, null), recordedMapEndpoints(listOf(
            emptyList(), listOf(a, b), emptyList(), listOf(c, a))))
    }

    @Test fun differentEndpointsComeFromTheActualFirstAndLastPoint() {
        assertEquals(RecordedMapEndpoints(a, d), recordedMapEndpoints(listOf(
            emptyList(), listOf(a, b), emptyList(), listOf(c, d), emptyList())))
    }

    @Test fun gapsRemainSeparateInsteadOfBecomingStraightLines() {
        val segments = listOf(listOf(a, b), emptyList(), listOf(c, d), listOf(d))
        assertEquals(listOf(listOf(a, b), listOf(c, d)), recordedPolylineSegments(segments))
        assertEquals(RecordedMapEndpoints(a, d), recordedMapEndpoints(segments))
    }
}

package com.example

import com.repforge.app.NativeWorkoutHeartRate
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutHeartRate403Test {
    @Test fun usesOneOriginAndUniqueValidSamplesWithinTheHalfOpenWindow() {
        val start = Instant.parse("2026-11-26T22:57:00Z")
        val end = start.plusSeconds(600)
        fun sample(offset: Long, bpm: Long, origin: String = "watch.feed") =
            NativeWorkoutHeartRate.Sample(start.plusSeconds(offset), bpm, origin)
        val out = JSONObject()
        assertTrue(NativeWorkoutHeartRate.fromSamples(out, start, end, sequenceOf(
            sample(-1, 80), sample(0, 110), sample(100, 140), sample(100, 140),
            sample(150, 145), sample(150, 180), sample(600, 215), sample(220, 0),
            sample(100, 140, "mirror.feed")
        )))
        assertEquals("watch.feed", out.getString("heartRateOrigin"))
        assertEquals(2, out.getInt("heartRateSamples"))
        assertEquals(125.0, out.getDouble("averageHeartRate"), 0.0)
        assertEquals(110, out.getInt("minHeartRate"))
        assertEquals(140, out.getInt("maxHeartRate"))
    }

    @Test fun intervalSummariesKeepOneKnownOrigin() {
        val out = JSONObject()
        fun source(name: String, count: Int, average: Int) = JSONObject()
            .put("heartRateOrigin", name).put("heartRateSamples", count).put("averageHeartRate", average)
        NativeWorkoutHeartRate.summarize(out, listOf(source("watch", 10, 120), source("other", 2, 160)))
        assertEquals("watch", out.getString("heartRateOrigin"))
        assertEquals(10, out.getInt("heartRateSamples"))
        assertEquals(120.0, out.getDouble("averageHeartRate"), 0.0)
    }
}

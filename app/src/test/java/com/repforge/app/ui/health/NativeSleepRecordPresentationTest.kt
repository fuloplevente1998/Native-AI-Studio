package com.repforge.app.ui.health

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeSleepRecordPresentationTest {
    private val start = 1_800_000_000_000L
    private fun stage(a: Int, b: Int, type: String, asleep: Boolean): JSONObject =
        JSONObject().put("startMs", start + a * 60_000L).put("endMs", start + b * 60_000L)
            .put("stage", type).put("isAsleep", asleep)

    private fun row(id: String, source: String = "samsung.health", stages: JSONArray = JSONArray()): HealthRecordRow =
        HealthRecordRow(id, "samsung_health", source, "SleepSessionRecord",
            start, start + 282 * 60_000L, start, JSONObject().put("durationMinutes", 282.0)
                .put("stages", stages).toString())

    @Test fun realSleepExcludesAwakeTimeAndUnionsOverlappingStages() {
        val data = JSONObject().put("stages", JSONArray()
            .put(stage(0, 180, "LIGHT", true))
            .put(stage(150, 258, "REM", true))
            .put(stage(258, 282, "AWAKE", false)))
        assertEquals(258.0, NativeSleepRecordPresentation.asleepMinutes(data, start, start + 282 * 60_000L)!!, 0.01)
        assertNull(NativeSleepRecordPresentation.asleepMinutes(JSONObject(), start, start + 282 * 60_000L))
    }

    @Test fun sameWindowDifferentIdsAreCollapsedButDistinctStagesSurvive() {
        val stages = JSONArray().put(stage(0, 258, "LIGHT", true))
        val different = JSONArray().put(stage(0, 220, "LIGHT", true))
        val original = row("parent-1", stages = stages)
        val duplicate = row("parent-2", stages = stages)
        val otherPhases = row("parent-3", stages = different)
        val otherSource = row("parent-4", source = "another.source", stages = stages)
        assertEquals(listOf(original, otherPhases, otherSource),
            NativeSleepRecordPresentation.distinct(listOf(original, duplicate, otherPhases, otherSource)))
    }
}

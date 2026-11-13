package com.example

import com.repforge.app.StudioWorkoutSessionPresentation
import com.repforge.app.StudioWorkoutSummaryPresentation
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class StudioSummaryAuditRegressionTest {
    private val end = Instant.parse("2026-11-13T10:30:00Z")
    private fun frame() = JSONObject().put("workoutId","session").put("generation",7)
        .put("started","2026-11-13T10:00:00Z").put("finished",end.toString())
        .put("activeIntervals",JSONArray().put(JSONObject().put("start","2026-11-13T10:00:00Z").put("end","2026-11-13T10:10:00Z"))
            .put(JSONObject().put("start","2026-11-13T10:20:00Z").put("end",end.toString())))
    @Test fun summaryUsesActiveWindowsAndMinuteSecondTargets() {
        assertEquals(1200L,StudioWorkoutSessionPresentation.elapsedSeconds(frame(),end))
        assertEquals("01:30",StudioWorkoutSessionPresentation.durationLabel(90))
        assertNull(StudioWorkoutSummaryPresentation.current(frame(),"another",7))
        assertNull(StudioWorkoutSummaryPresentation.current(frame(),"session",8))
    }
    @Test fun caloriesUseOnlyMatchingWorkoutEvidenceAndPreserveMissingVersusZero() {
        val live=JSONObject().put("schema",1).put("revision",1).put("watchId","watch")
            .put("updatedAt",end.toEpochMilli()).put("workoutId","session").put("totalCalories",123.0)
        assertEquals(123.0,StudioWorkoutSummaryPresentation.calories(frame(),live,end)!!,0.0)
        assertNull(StudioWorkoutSummaryPresentation.calories(frame(),JSONObject(live.toString()).put("workoutId","other"),end))
        assertNull(StudioWorkoutSummaryPresentation.calories(frame(),null,end))
        assertEquals(0.0,StudioWorkoutSummaryPresentation.calories(frame(),live.put("totalCalories",0),end)!!,0.0)
    }
}

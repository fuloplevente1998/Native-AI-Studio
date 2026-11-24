package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExternalSessionEvidenceTest {
    class Present {
        fun hasSetIndex() = true
        fun getSetIndex() = 0
        fun hasRateOfPerceivedExertion() = true
        fun getRateOfPerceivedExertion() = 0f
        fun getWeight() = Weight()
    }
    class Weight { fun getInGrams() = 2500.0 }
    class Absent {
        fun hasSetIndex() = false
        fun getSetIndex(): Int = error("absent")
        fun hasRateOfPerceivedExertion() = false
        fun getRateOfPerceivedExertion(): Float = error("absent")
    }
    @Test fun optionalFieldsKeepRealZeroAndNeverInventMissing() {
        val out = JSONObject()
        ExternalSessionEvidence.platformStrength(Present(), out)
        assertEquals(0, out.getInt("setIndex"))
        assertEquals(0.0, out.getDouble("rpe"), 0.0)
        assertEquals(2.5, out.getDouble("weightKg"), 0.0)
        val missing = JSONObject()
        ExternalSessionEvidence.platformStrength(Absent(), missing)
        assertEquals(0, missing.length())
    }
    @Test fun explicitPauseUnionAndProviderSessionValuesStaySeparate() {
        val pause = JSONArray().put(JSONObject().put("pause", true).put("startMs", 2000).put("endMs", 5000))
            .put(JSONObject().put("pause", true).put("startMs", 4000).put("endMs", 6000))
        val hc = ExternalWorkout(ExternalWorkoutIdentity(HealthJournalStore.HC, "third.party", "a"),
            10000, 1000, 10000, 1, null, null, null, null, pause, null)
        assertEquals(mapOf("recordedPauseSeconds" to 4.0), ExternalSessionEvidence.directValues(hc))
        val samsung = hc.copy(identity = hc.identity.copy(provider = HealthJournalStore.SAMSUNG),
            sessionEvidence = JSONObject().put("distanceMeters", 42).put("sessionDurationSeconds", 6).put("vo2Max", 40))
        assertEquals(mapOf("distanceMeters" to 42.0, "sessionDurationSeconds" to 6.0, "vo2Max" to 40.0),
            ExternalSessionEvidence.directValues(samsung))
    }
}

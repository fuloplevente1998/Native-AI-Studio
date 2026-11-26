package com.example

import com.repforge.app.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WearWorkoutEnergy2815Test {
    private val now = Instant.parse("2026-01-02T00:10:00Z")
    private val start = now.minusSeconds(1200)
    private fun frame() = JSONObject().put("workoutId","w").put("id","w").put("ownerEpoch193",3)
        .put("started",start.toString()).put("finished",now.toString())
    private fun data(revision: Long = 4, kcal: Double = 200.0) = JSONObject().put("workoutId","w")
        .put("watchId","watch").put("revision",revision).put("updatedAt",now.toEpochMilli())
        .put("totalCalories",kcal).put("activeDurationSeconds",1200)
    private fun packet() = JSONObject().put("schema",1).put("revision",1).put("workoutId","w")
        .put("watchId","transport").put("ownerEpoch351",3).put("sourceNode351","node")
        .put("state","paused").put("updatedAt",now.toEpochMilli()).put("summary",data())

    @Test fun modernLiveNeedsFreshNodeEpochAndNestedMeasurement() {
        val value = WearWorkoutEnergyEvidence.live(frame(),packet(),"node",now)!!
        assertEquals(200.0,value.getDouble("totalCalories"),0.0)
        assertFalse(value.has("activeCalories"))
        assertTrue(value.optBoolean("partial"))
        assertNull(WearWorkoutEnergyEvidence.live(frame(),packet(),"wrong",now))
        assertNull(WearWorkoutEnergyEvidence.live(frame().put("ownerEpoch193",4),packet(),"node",now))
        assertNull(WearWorkoutEnergyEvidence.live(frame(),packet(),"node",now.plusSeconds(46)))
        val stale = packet().put("summary",data().put("updatedAt",now.minusSeconds(60).toEpochMilli()))
        assertNull(WearWorkoutEnergyEvidence.live(frame(),stale,"node",now))
        val foreign = packet().put("summary",data().put("workoutId","other"))
        assertNull(WearWorkoutEnergyEvidence.live(frame(),foreign,"node",now))
        val projection = StudioWorkoutSummaryPresentation.projection(frame(),packet(),now,expectedNode = "node")!!
        assertEquals(mapOf("totalCalories" to 200.0),projection.totalEnergy.values)
        assertTrue(projection.activeEnergy.values.isEmpty())
    }

    @Test fun lateWatchReceiptCannotReplaceNewerOrEraseMeasuredEnergy() {
        val old = data()
        assertFalse(WearWorkoutEnergyEvidence.accept(old,data(3,40.0),setOf("w")))
        assertFalse(WearWorkoutEnergyEvidence.accept(old,data(5).put("workoutId","other"),setOf("w")))
        assertFalse(WearWorkoutEnergyEvidence.accept(old,data(5).apply { remove("totalCalories") },setOf("w")))
        assertFalse(WearWorkoutEnergyEvidence.accept(old,data(5).put("watchId","other"),setOf("w")))
        assertTrue(WearWorkoutEnergyEvidence.accept(old,data(5,250.0),setOf("w")))
        assertTrue(WearWorkoutEnergyEvidence.accept(data().put("livePreview2815",true),data(),setOf("w")))
        assertFalse(WearWorkoutEnergyEvidence.accept(old,data(),setOf("w")))
        val previous = frame().put("generation",1).put("wear",old)
        val projected = frame().put("generation",1)
        WearWorkoutEnergyEvidence.preserveDraft(previous,projected)
        assertEquals(200.0,projected.getJSONObject("wear").getDouble("totalCalories"),0.0)
        val other = frame().put("generation",2)
        WearWorkoutEnergyEvidence.preserveDraft(previous,other)
        assertFalse(other.has("wear"))
        val newer = frame().put("generation",1).put("wear",data(5,250.0))
        WearWorkoutEnergyEvidence.preserveDraft(previous,newer)
        assertEquals(250.0,newer.getJSONObject("wear").getDouble("totalCalories"),0.0)
    }

    @Test fun shorterWatchDurationDoesNotHideBetterEnergyAndNeverAddsDifferentDefinitions() {
        val measured = data(kcal=48.0).put("activeDurationSeconds",180)
        val source = JSONObject().put("windowStart",start.toString()).put("windowEnd",now.toString())
            .put("activeCalories",150).put("totalCalories",300)
        val document = NativeWorkoutHealthSource.attach(null,HealthJournalStore.SAMSUNG,source)
        val result = NativeWorkoutHealthSummary.project(frame(),measured,document)
        assertEquals(mapOf("activeCalories" to 150.0),result.activeEnergy.values)
        assertEquals(mapOf("totalCalories" to 300.0),result.totalEnergy.values)
        assertTrue(result.totalEnergy.overlapExcluded)
        assertTrue(WearWorkoutEnergyEvidence.incompleteDuration(measured,1200000))
    }
}

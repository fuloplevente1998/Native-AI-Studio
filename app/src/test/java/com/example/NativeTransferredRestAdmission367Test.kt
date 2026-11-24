package com.example

import com.repforge.app.NativeTransferredRestAdmission367
import com.repforge.shared.IntervalTimeline193
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NativeTransferredRestAdmission367Test {
    private fun fixture(delta: Int): Pair<JSONObject, JSONObject> {
        val position=12000L
        val steps=listOf(IntervalTimeline193.Step("e","s","Timed",10,5,2,""))
        val old=IntervalTimeline193(steps,true)
        val overrides=old.adjustedRestOverrides(position,delta)!!
        val after=IntervalTimeline193(steps,true,0,false,overrides)
        val p=after.at(position)
        val timer=JSONObject().put("key","native|4|9").put("workoutId","w")
            .put("autoAdvance",true).put("restOnly",false).put("initialRest",0)
            .put("steps",JSONArray().put(JSONObject().put("exerciseId","e").put("setId","s")
                .put("name","Timed").put("work",10).put("rest",5).put("rounds",2)))
            .put("elapsedMs",p.elapsedMs).put("phase",p.phase).put("stepIndex",p.step)
            .put("round",p.round).put("remainingMs",p.remainingMs).put("totalMs",p.totalMs)
            .put("completed",p.completed).put("stepElapsedMs",p.stepElapsedMs)
            .put("restOverridesMs367",after.restOverridesJson()).put("restChangeRevision367",1)
            .put("ledger",JSONObject().put("positionMs",position).put("workMs",10000)
                .put("restMs",2000).put("pausedMs",0).put("steps",JSONArray().put(JSONObject()
                    .put("exerciseId","e").put("setId","s").put("workMs",10000).put("restMs",2000).put("pausedMs",0))))
        val snapshot=JSONObject().put("workoutId","w").put("pauseRevision",10).put("restEndAt",0)
            .put("timerCheckpoint193",JSONObject().put("schema",1).put("timer",timer))
            .put("automation",JSONObject().put("owner","watch").put("key","watch-k")
                .put("phase",p.phase).put("remainingMs",p.remainingMs)
                .put("phaseNumber",if(delta<0)8 else 7).put("restChangeRevision367",1))
        val command=JSONObject().put("delta",delta).put("phaseNumber",7).put("remainingBefore",3000)
            .put("exerciseId","e").put("setId","s").put("configuredRest",5).put("timerKey","watch-k")
            .put("transferRestProof367",JSONObject().put("restKey","0:1").put("position",position)
                .put("priorOverride",JSONObject.NULL).put("beforeRemaining",3000)
                .put("beforePhaseDuration",5000).put("restRevision",1).put("sourceTimerKey","native|4|9"))
        return snapshot to command
    }
    @Test fun validExtensionAndZeroBoundaryUseSameAbsolutePosition() {
        val (longer,c1)=fixture(15)
        assertTrue(NativeTransferredRestAdmission367.valid(longer,c1,10))
        val (shorter,c2)=fixture(-15)
        assertTrue(NativeTransferredRestAdmission367.valid(shorter,c2,10))
        assertEquals("work",shorter.getJSONObject("automation").getString("phase"))
    }
    @Test fun invalidLedgerPhaseAndOverrideProofRejected() {
        val (frame,c)=fixture(15)
        assertFalse(NativeTransferredRestAdmission367.valid(frame,c,11))
        assertFalse(NativeTransferredRestAdmission367.valid(frame,JSONObject(c.toString()).put("remainingBefore",4000),10))
        val timer=frame.getJSONObject("timerCheckpoint193").getJSONObject("timer")
        timer.getJSONObject("ledger").getJSONArray("steps").getJSONObject(0).put("restMs",999)
        assertFalse(NativeTransferredRestAdmission367.valid(frame,c,10))
        timer.getJSONObject("ledger").getJSONArray("steps").getJSONObject(0).put("restMs",2000)
        timer.getJSONObject("restOverridesMs367").put("0:1",21000)
        assertFalse(NativeTransferredRestAdmission367.valid(frame,c,10))
    }
}

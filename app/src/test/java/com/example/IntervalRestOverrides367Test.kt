package com.example

import com.repforge.shared.IntervalTimeline193
import com.repforge.shared.IntervalTimeLedger193
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IntervalRestOverrides367Test {
    private fun steps() = listOf(IntervalTimeline193.Step("e","s1","First",10,20,3,""),
        IntervalTimeline193.Step("e","s2","Second",5,7,2,""))
    private fun timeline(overrides: Map<String, Long> = emptyMap()) =
        IntervalTimeline193(steps(),true,5,false,overrides)

    @Test fun initialAndFirstRoundAreSeparateAndFutureRestsUnchanged() {
        val original=timeline()
        val changed=timeline(original.adjustedRestOverrides(2000,15)!!)
        assertEquals(18000L,changed.at(2000).remainingMs)
        assertEquals(20000L,changed.phaseDurationMs(changed.at(30000)))
        assertEquals("0:1",changed.restKey(changed.at(30000)))
        val again=timeline(changed.adjustedRestOverrides(35000,-15)!!)
        assertEquals("work",again.at(35000).phase)
        val secondRest=again.at(45000)
        assertEquals("0:2",again.restKey(secondRest))
        assertEquals(20000L,again.phaseDurationMs(secondRest))
        assertEquals(20,steps()[0].rest)
        val raw=JSONObject().put("restOverridesMs367",again.restOverridesJson())
        assertEquals(again.at(45000).remainingMs,timeline(IntervalTimeline193.readRestOverrides(raw)).at(45000).remainingMs)
    }
    @Test fun adjustingDoesNotInventMeasuredRestOrMoveVirtualPosition() {
        val original=timeline()
        val ledger=IntervalTimeLedger193(2,0)
        val rows=JSONArray().put(JSONObject().put("exerciseId","e").put("setId","s1"))
            .put(JSONObject().put("exerciseId","e").put("setId","s2"))
        ledger.sample(original,2000,2000,true,false,false)
        val next=timeline(original.adjustedRestOverrides(2000,-15)!!)
        ledger.sample(next,2000,2000,true,false,false)
        assertEquals("work",next.at(2000).phase)
        assertEquals(2000L,ledger.json(rows).getLong("restMs"))
        ledger.sample(next,3000,3000,true,false,false)
        assertEquals(1000L,ledger.json(rows).getLong("workMs"))
        assertEquals(2000L,ledger.json(rows).getLong("restMs"))
    }
    @Test fun upperBoundAndInvalidTargets() {
        val long=timeline(mapOf("initial" to 599000L))
        assertEquals(600000L,timeline(long.adjustedRestOverrides(0,15)!!).at(0).remainingMs)
        assertNull(timeline(mapOf("initial" to 600000L)).adjustedRestOverrides(0,15))
        assertNull(timeline().adjustedRestOverrides(5000,15))
        assertNull(timeline().adjustedRestOverrides(0,1))
        assertThrows(IllegalArgumentException::class.java){timeline(mapOf("9:1" to 1L))}
        assertThrows(IllegalArgumentException::class.java){timeline(mapOf("0:1" to -1L))}
        assertThrows(IllegalArgumentException::class.java){timeline(mapOf("0:1" to IntervalTimeline193.MAX_MS))}
        assertThrows(Exception::class.java){IntervalTimeline193.readRestOverrides(JSONObject().put("restOverridesMs367",JSONObject().put("initial",1.5)))}
    }
}

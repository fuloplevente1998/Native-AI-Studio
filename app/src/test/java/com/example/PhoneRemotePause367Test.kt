package com.example

import com.repforge.app.PhoneRemotePause367
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhoneRemotePause367Test {
    private fun frame() = JSONObject("""{"active":true,"workoutId":"w","generation":4,"ownerEpoch193":2,"nativeEditorProjection":1,"timerOwner":"phone","pauseRevision":10,"workoutPaused":false,"started":"2026-11-24T10:00:00Z"}""")
    private fun command() = JSONObject().put("action","phonePause367").put("commandId","p").put("sequence",9)
        .put("workoutId","w").put("generation",4).put("ownerEpoch193",2).put("pauseRevision",10)
        .put("requestedRevision",11).put("paused",true)
    @Test fun pauseIsExplicitAndReplayHasExactProof() {
        val paused = PhoneRemotePause367.prepare(frame(),command(),1795515000000)!!
        assertTrue(paused.getBoolean("workoutPaused"))
        assertFalse(paused.getJSONArray("activeIntervals").getJSONObject(0).isNull("end"))
        assertTrue(PhoneRemotePause367.sameProof(paused,command()))
        assertFalse(PhoneRemotePause367.sameProof(paused,command().put("commandId","other")))
        assertNull(PhoneRemotePause367.prepare(paused,command(),1795515000000))
        val resumed = PhoneRemotePause367.prepare(paused,command().put("commandId","r").put("paused",false)
            .put("pauseRevision",11).put("requestedRevision",12),1795515060000)!!
        assertFalse(resumed.getBoolean("workoutPaused"))
        assertEquals(2,resumed.getJSONArray("activeIntervals").length())
        assertFalse(PhoneRemotePause367.sameProof(resumed,command()))
    }
    @Test fun wrongOwnerGenerationEpochOrStalePauseRejected() {
        assertNull(PhoneRemotePause367.prepare(frame().put("timerOwner","watch"),command(),1795515000000))
        assertNull(PhoneRemotePause367.prepare(frame(),command().put("generation",5),1795515000000))
        assertNull(PhoneRemotePause367.prepare(frame(),command().put("ownerEpoch193",3),1795515000000))
        assertNull(PhoneRemotePause367.prepare(frame(),command().put("pauseRevision",9),1795515000000))
    }
}

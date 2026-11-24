package com.example

import com.repforge.app.PhoneRemoteRest367
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhoneRemoteRest367Test {
    private fun frame() = JSONObject("""{"active":true,"workoutId":"w","timerOwner":"phone","nativeEditorProjection":1,"generation":5,"ownerEpoch193":2,"pauseRevision":10}""")
    private fun timer() = JSONObject("""{"key":"k","workoutId":"w","generation":5,"phaseNumber":3,"restOnly":true,"running":true,"phase":"rest","steps":[{"rest":90}]}""")
    private fun command() = JSONObject("""{"action":"phoneRest367","commandId":"c","sequence":4,"workoutId":"w","generation":5,"ownerEpoch193":2,"pauseRevision":10,"key":"k","phaseNumber":3,"duration":90,"delta":15}""")
    @Test fun matchingRestOnlyTargetRequired() {
        assertTrue(PhoneRemoteRest367.matches(frame(),timer(),command()))
        assertFalse(PhoneRemoteRest367.matches(frame(),timer().put("phaseNumber",4),command()))
        assertFalse(PhoneRemoteRest367.matches(frame(),timer().put("restOnly",false),command()))
        assertFalse(PhoneRemoteRest367.matches(frame().put("workoutPaused",true),timer(),command()))
        assertFalse(PhoneRemoteRest367.matches(frame(),timer(),command().put("delta",30)))
    }
    @Test fun durableProofRequiresExactRequest() {
        val c=command()
        val saved=frame().put("remoteRest367",PhoneRemoteRest367.proof(c))
        assertTrue(PhoneRemoteRest367.sameProof(saved,c))
        assertFalse(PhoneRemoteRest367.sameProof(saved,command().put("delta",-15)))
    }
}

package com.example

import com.repforge.app.NativeWatchControlAdmission367
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NativeWatchControl367Test {
    private fun current() = JSONObject("""{"active":true,"workoutId":"w","timerOwner":"watch","ownershipProtocol193":1,"ownerEpoch193":2,"generation":5,"pauseRevision":10,"localSequence":3,"currentExercise":0,"restEndAt":130000,"exercises":[{"id":"ex","sets":[{"setId":"s"}]}]}""")
    private fun command(kind: String, frame: JSONObject) = JSONObject()
        .put("action","watchControl367").put("commandId","c").put("sequence",4)
        .put("workoutId","w").put("ownerEpoch193",2).put("generation",5).put("pauseRevision",10)
        .put("kind",kind).put("createdAt",100000L).put("expectedDeadline",130000L).put("delta",15)
        .put("exerciseId","ex").put("setId","s").put("finalSnapshot",frame)

    @Test fun absoluteRestReplayIsIdempotentAndWrongOwnerRejected() {
        val c = command("rest",current().put("restEndAt",145000))
        val accepted = NativeWatchControlAdmission367.snapshot(current(),c)!!
        assertEquals(145000L, accepted.getLong("restEndAt"))
        assertSame(accepted, NativeWatchControlAdmission367.snapshot(accepted,c))
        assertNull(NativeWatchControlAdmission367.snapshot(accepted,command("rest",current().put("restEndAt",160000))))
        assertNull(NativeWatchControlAdmission367.snapshot(current().put("timerOwner","phone"),c))
        assertNull(NativeWatchControlAdmission367.snapshot(current().put("ownerEpoch193",3),c))
        assertNull(NativeWatchControlAdmission367.snapshot(current(),command("rest",current().put("restEndAt",160000))))
    }
    @Test fun pauseMustAdvanceRevisionAndLateRestCannotResumeIt() {
        val paused = NativeWatchControlAdmission367.snapshot(current(),command("pause",current()
            .put("workoutPaused",true).put("pauseRevision",11).put("restEndAt",0)))!!
        assertTrue(paused.getBoolean("workoutPaused"))
        assertNull(NativeWatchControlAdmission367.snapshot(paused,command("rest",current().put("restEndAt",145000))
            .put("commandId","late").put("sequence",5)))
        assertNull(NativeWatchControlAdmission367.snapshot(current(),command("pause",current().put("workoutPaused",true).put("restEndAt",0))))
    }
}

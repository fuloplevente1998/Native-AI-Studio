package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.NativeWorkoutSession
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.PhoneWorkoutExecutionStore
import com.repforge.app.data.store.TrainPilotUserSettings
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NativeSessionWearAdmissionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun isolateExecutionConnection() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
    }
    private fun candidate() = NativeWorkoutSession.quickCandidate(JSONObject("""
        {"id":"pushup","name":"Push-up","repUnit":"ism.","measurementType":"reps","loadType":"bodyweight",
         "sets":[{"setId":"set-1","set":1,"reps":0,"weight":0,"done":false}]}
    """), TrainPilotUserSettings())

    @Test fun newNativePhoneSessionPersistsOwnershipAdmissionInTheCanonicalFrame() {
        val input = candidate()
        val saved = NativeWorkoutSession.installPrepared(context, input)
        val execution = PhoneWorkoutExecution.store(context).snapshot()!!
        assertEquals(input.getString("workoutId"), saved.getString("workoutId"))
        assertEquals(1, saved.getInt("ownershipProtocol193"))
        assertEquals(0L, saved.getLong("ownerEpoch193"))
        assertEquals("phone", saved.getString("timerOwner"))
        assertEquals(1, execution.getInt("ownershipProtocol193"))
        assertEquals(saved.getString("workoutId"), execution.getString("workoutId"))
    }

    @Test fun existingLegacySessionIsRecoveredWithoutRewritingItsOwnershipOrSets() {
        val legacy = candidate().put("generation", 42L).put("executionRevision", 0).put("nativeEditorProjection", 1)
        val prepared = PhoneWorkoutExecution.store(context).prepare(legacy, 0)
        val original = ActiveWorkoutStore.save(context, prepared)
        val recovered = NativeWorkoutSession.installPrepared(context, candidate())
        assertEquals(original.getString("workoutId"), recovered.getString("workoutId"))
        assertEquals(42L, recovered.getLong("generation"))
        assertEquals(0, recovered.optInt("ownershipProtocol193"))
        assertEquals(original.getJSONArray("exercises").toString(), recovered.getJSONArray("exercises").toString())
    }

    @Test fun transferredCandidateCannotBeInstalledAsANewPhoneSession() {
        val foreign = candidate().put("timerOwner", "watch").put("ownerEpoch193", 2)
        assertThrows(IllegalStateException::class.java) { NativeWorkoutSession.installPrepared(context, foreign) }
        assertNull(PhoneWorkoutExecution.store(context).snapshot())
    }
}

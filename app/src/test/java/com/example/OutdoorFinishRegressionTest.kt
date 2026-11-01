package com.example

import com.repforge.app.NativeGpsMeasurement
import com.repforge.app.StudioOutdoorFinish
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OutdoorFinishRegressionTest {
    private fun stopped(paused: Boolean = false): JSONObject = JSONObject()
        .put("active", true).put("workoutId", "workout").put("generation", 7)
        .put("timerOwner", "phone").put("workoutPaused", paused)
        .put(NativeGpsMeasurement.FIELD, JSONObject().put("id", "measurement")
            .put("workoutId", "workout").put("generation", 7).put("applied", true)
            .put("state", if (paused) "interrupted" else "explicitStopped")
            .put("capture", JSONObject().put("measurementId", "measurement")
                .put("nativeWorkoutId", "workout").put("nativeGeneration", 7)
                .put("active", false).put("elapsedSeconds", 25).put("distanceMeters", 0)))

    @Test fun noFixCanSaveActualTimeWithoutInventingCompletedGpsSet() {
        val frame = stopped()
        assertFalse(StudioOutdoorFinish.canSaveTimeOnly(frame))
        frame.put(StudioOutdoorFinish.FIELD, StudioOutdoorFinish.receipt(frame))
        assertTrue(StudioOutdoorFinish.canSaveTimeOnly(frame))
        assertFalse(frame.has("exercises")) // Receipt never manufactures a done set.
        assertEquals(0, frame.getJSONObject(NativeGpsMeasurement.FIELD).getJSONObject("capture").getInt("distanceMeters"))
    }

    @Test fun pausedInterruptedCaptureCanBeExplicitlyFinishedWithoutRestartingGps() {
        val frame = stopped(paused = true)
        frame.put(StudioOutdoorFinish.FIELD, StudioOutdoorFinish.receipt(frame))
        assertTrue(StudioOutdoorFinish.canSaveTimeOnly(frame))
        assertTrue(frame.getBoolean("workoutPaused"))
    }

    @Test fun liveZeroTimeAndForeignCapturesCannotAuthorizeSaving() {
        for ((key, value) in listOf("active" to true, "elapsedSeconds" to 0,
            "measurementId" to "other", "nativeWorkoutId" to "other", "nativeGeneration" to 8)) {
            val frame = stopped()
            frame.getJSONObject(NativeGpsMeasurement.FIELD).getJSONObject("capture").put(key, value)
            assertNull("Must reject $key", StudioOutdoorFinish.receipt(frame))
        }
    }

    @Test fun receiptCannotBeReusedAfterCaptureOrOwnershipChanges() {
        for (change in listOf("capture", "owner", "finalization")) {
            val frame = stopped().also { it.put(StudioOutdoorFinish.FIELD, StudioOutdoorFinish.receipt(it)) }
            when (change) {
                "capture" -> frame.getJSONObject(NativeGpsMeasurement.FIELD).getJSONObject("capture").put("elapsedSeconds", 26)
                "owner" -> frame.put("timerOwner", "watch")
                else -> frame.put("finalizationPending", true)
            }
            assertFalse(StudioOutdoorFinish.canSaveTimeOnly(frame))
        }
    }
}

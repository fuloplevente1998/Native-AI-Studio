package com.repforge.app.wear

import android.content.Context
import com.repforge.shared.WearMeasurementRequest351

/** Best effort only: Android may still deny a foreground service start from background. */
internal object WearBackgroundMeasurement {
    fun receive(context: Context, request: WearMeasurementRequest351) {
        val workout=WorkoutSnapshotStore.load(context) ?: return
        val id=workout.workoutId
        if(!request.matches(id,workout.revision,workout.ownerEpoch193,System.currentTimeMillis()) ||
            workout.timerOwner!="phone" || workout.workoutPaused || WearOwnership193.pending(workout) ||
            WearClosureStore.isClosed(context,id))return

        val active=WearHealthStore.active(context)
        if(active.isNotBlank())return // The service owns deduplication and closure, never replace an active recording.
        if(WearHealthStore.load(context,id)?.optString("state") in setOf("ended","error","ending"))return
        fun foreground()=WearLiveHealth.publish(context,id,"foregroundRequired","Az órás méréshez nyisd meg a TrainPilotot.")
        if(!WearHealthStore.enabled(context)) {
            WearLiveHealth.publish(context,id,"disabled","Órás mérés kikapcsolva.")
            return
        }
        val config=wearRecordingRequestForWorkout(workout,WearHealthStore.gps(context))
        // Do not request background location or silently downgrade requested GPS/movement metrics.
        if(!WearBackgroundSensorAccess.allowed(context) || config.exerciseType!="strength" ||
            config.gps || WearHealthStore.providerRequiresLocation(context)) {
            foreground();return
        }
        try {
            WearHealthService.start(context,workout,background=true)
        } catch (_: SecurityException) {
            foreground()
        } catch (_: IllegalStateException) {
            // Includes ForegroundServiceStartNotAllowedException on API 31+.
            foreground()
        }
    }
}

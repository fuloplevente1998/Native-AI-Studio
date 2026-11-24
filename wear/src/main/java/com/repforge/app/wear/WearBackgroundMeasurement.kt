package com.repforge.app.wear

import android.content.Context
import com.repforge.shared.WearMeasurementRequest351
import org.json.JSONObject

/** One bounded in-memory pending request, confined to the main thread. Never a durable start queue. */
internal object WearBackgroundMeasurement {
    private data class Pending(val source: String,val request: WearMeasurementRequest351)
    private data class Origin(val source: String,val id: String,val epoch: Long,val revision: Long)
    private var pending: Pending?=null
    private var origin: Origin?=null
    private var lastAttempt: Pending?=null

    fun receive(context: Context,source: String,request: WearMeasurementRequest351) {
        if(source.isBlank() || !request.fresh(System.currentTimeMillis()) ||
            WearClosureStore.isClosed(context,request.workoutId))return
        pending=Pending(source,request)
        drain(context)
    }

    /** Observe the actual sender only after the existing snapshot/epoch/tombstone admission. */
    fun snapshot(context: Context,source: String?,raw: String?) {
        if(source.isNullOrBlank() || raw.isNullOrBlank())return
        val data=try { JSONObject(raw) }catch(_:Exception){return}
        val current=WorkoutSnapshotStore.load(context)
        if(!data.optBoolean("active",true)) {
            if(current==null && pending?.request?.workoutId==data.optString("workoutId"))pending=null
            if(current==null)origin=null
            return
        }
        if(current!=null && data.optString("timerOwner","phone")=="phone" &&
            current.workoutId==data.optString("workoutId") &&
            current.ownerEpoch193==data.optLong("ownerEpoch193") &&
            current.revision==data.optLong("revision")) {
            origin=Origin(source,current.workoutId,current.ownerEpoch193,current.revision)
        }
        drain(context)
    }

    private fun drain(context: Context) {
        val entry=pending ?: return
        val request=entry.request
        val now=System.currentTimeMillis()
        if(!request.fresh(now) || WearClosureStore.isClosed(context,request.workoutId)) { pending=null;return }
        val workout=WorkoutSnapshotStore.load(context) ?: return
        val id=workout.workoutId
        val sender=origin ?: return
        if(sender.source!=entry.source || sender.id!=id || sender.epoch!=workout.ownerEpoch193 ||
            sender.revision<request.revision ||
            !request.matches(id,workout.revision,workout.ownerEpoch193,now))return
        pending=null
        if(workout.timerOwner!="phone" || workout.workoutPaused || WearOwnership193.pending(workout))return
        val active=WearHealthStore.active(context)
        if(active.isNotBlank()) {
            if(active==id)WearHealthService.refreshConfirmation(context)
            return
        }
        if(WearHealthStore.load(context,id)?.optString("state") in setOf("ended","error","ending"))return
        val previous=lastAttempt
        if(previous?.source==entry.source && previous.request.workoutId==id &&
            previous.request.epoch==request.epoch && previous.request.fresh(now))return
        lastAttempt=entry
        fun foreground()=WearLiveHealth.publish(context,id,"foregroundRequired","Az órás méréshez nyisd meg a TrainPilotot.")
        if(!WearHealthStore.enabled(context)) {
            WearLiveHealth.publish(context,id,"disabled","Órás mérés kikapcsolva.")
            return
        }
        val config=wearRecordingRequestForWorkout(workout,WearHealthStore.gps(context))
        if(!WearBackgroundSensorAccess.allowed(context) || config.exerciseType!="strength" ||
            config.gps || WearHealthStore.providerRequiresLocation(context)) {
            foreground();return
        }
        try {
            WearHealthService.start(context,workout,background=true)
        } catch (_: SecurityException) {
            foreground()
        } catch (_: IllegalStateException) {
            foreground()
        }
    }
}

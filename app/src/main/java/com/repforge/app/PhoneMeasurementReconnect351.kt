package com.repforge.app

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.repforge.shared.WearMeasurementRequest351
import org.json.JSONObject

/** Reconnect is best effort and cannot open Activities or change ownership. */
internal object PhoneMeasurementReconnect351 {
    /** What a peer (re)connect may do. It never installs or starts a workout on its own. */
    enum class ReconnectPlan { NONE, TERMINAL, MEASUREMENT }

    /**
     * 2809: process scoped on purpose. Only a workout whose watch recording this phone process
     * explicitly requested (a new phone start) may be re-requested after a Bluetooth drop. An app
     * update, reboot or process restart begins with no armed workout, so a stored draft is never
     * pushed to the watch or recorded there just because the watch reconnected.
     */
    @Volatile private var armedWorkoutId: String? = null

    fun arm(workoutId: String) { if (workoutId.isNotBlank()) armedWorkoutId = workoutId }

    /** Pre-#370 rule restored for durable state: reconnect retries only the terminal projection. */
    fun reconnectPlan(frame: JSONObject?, armed: String?, now: Long): ReconnectPlan = when {
        frame == null || frame.optString("workoutId").isBlank() -> ReconnectPlan.NONE
        !frame.optBoolean("active", true) -> ReconnectPlan.TERMINAL
        armed == frame.optString("workoutId") && WearMeasurementRequest351.fromFrame(frame, now) != null -> ReconnectPlan.MEASUREMENT
        else -> ReconnectPlan.NONE
    }

    fun plan(context: Context, now: Long = System.currentTimeMillis()): ReconnectPlan =
        reconnectPlan(ActiveWorkoutStore.current(context),
            armedWorkoutId.takeIf { NativePrivacyControls.enabled(context, NativePrivacyControls.TRAINPILOT_WEAR) }, now)

    fun send(context: Context,node: String) {
        if(plan(context)!=ReconnectPlan.MEASUREMENT)return
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            if(nodes.size!=1 || nodes.single().id!=node)return@addOnSuccessListener
            Wearable.getCapabilityClient(context).getCapability(NativeWearCompanion.CAPABILITY,CapabilityClient.FILTER_REACHABLE)
                .addOnSuccessListener capability@{ capability ->
                    if(capability.nodes.none { it.id==node })return@capability
                    val frame=ActiveWorkoutStore.current(context) ?: return@capability
                    val now = System.currentTimeMillis()
                    val armed = armedWorkoutId.takeIf { NativePrivacyControls.enabled(context, NativePrivacyControls.TRAINPILOT_WEAR) }
                    if (reconnectPlan(frame, armed, now) != ReconnectPlan.MEASUREMENT) return@capability
                    val request = WearMeasurementRequest351.fromFrame(frame, now) ?: return@capability
                    WearLiveHealthStore.expectNode351(node)
                    WearSyncRepository.publishActiveWorkoutSnapshot(context,frame)
                    Wearable.getMessageClient(context).sendMessage(node,WearMeasurementRequest351.PATH,request.encode())
                }
        }
    }

    /** A cached receipt, wrong node/epoch or expired timestamp is not live evidence. */
    fun freshReceipt(health: JSONObject?,id: String,epoch: Long,node: String?,now: Long): JSONObject? =
        health?.takeIf {
            id.isNotBlank() && !node.isNullOrBlank() && it.optString("workoutId")==id &&
                it.optString("sourceNode351")==node && it.optLong("ownerEpoch351",-1)==epoch &&
                epoch>=0 && now-it.optLong("updatedAt") in -5000L..45000L
        }
}

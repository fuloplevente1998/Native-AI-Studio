package com.repforge.app

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.repforge.shared.WearMeasurementRequest351
import org.json.JSONObject

/** Reconnect is best effort and cannot open Activities or change ownership. */
internal object PhoneMeasurementReconnect351 {
    fun send(context: Context,node: String) {
        if(WearMeasurementRequest351.fromFrame(ActiveWorkoutStore.current(context) ?: return,System.currentTimeMillis())==null)return
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            if(nodes.size!=1 || nodes.single().id!=node)return@addOnSuccessListener
            Wearable.getCapabilityClient(context).getCapability(NativeWearCompanion.CAPABILITY,CapabilityClient.FILTER_REACHABLE)
                .addOnSuccessListener capability@{ capability ->
                    if(capability.nodes.none { it.id==node })return@capability
                    val frame=ActiveWorkoutStore.current(context) ?: return@capability
                    val request=WearMeasurementRequest351.fromFrame(frame,System.currentTimeMillis()) ?: return@capability
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

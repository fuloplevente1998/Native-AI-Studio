package com.repforge.app

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/** System pairing is not proof that the matching, same-signer TrainPilot companion exists. */
internal object NativeWearCompanion {
    const val CAPABILITY = "trainpilot_workout_companion193"
    suspend fun reachable(context: Context, nodeId: String): Boolean =
        Wearable.getCapabilityClient(context).getCapability(CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .await().nodes.any { it.id == nodeId }
}

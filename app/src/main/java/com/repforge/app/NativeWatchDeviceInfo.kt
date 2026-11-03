package com.repforge.app

import android.content.Context
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.repforge.shared.WatchDeviceInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

internal object NativeWatchDeviceInfo {
    data class Result(val nodeName: String, val data: JSONObject)
    suspend fun request(context: Context): Result = withTimeout(12_000) {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        val companions = nodes.filter { NativeWearCompanion.reachable(context, it.id) }
        check(companions.size == 1) { if (companions.isEmpty())
            "Nem érhető el a TrainPilot óraapp. Ellenőrizd az órakapcsolatot és az óraapp verzióját."
            else "Több TrainPilot óra érhető el. Egyetlen órával csatlakozz a lekéréshez." }
        val node = companions.single()
        val requestId = UUID.randomUUID().toString()
        val reply = CompletableDeferred<JSONObject>()
        val client = Wearable.getMessageClient(context)
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.sourceNodeId == node.id && event.path == WatchDeviceInfo.REPLY &&
                WatchDeviceInfo.accepts(event.data, requestId)) {
                runCatching { JSONObject(event.data.toString(Charsets.UTF_8)) }.getOrNull()?.let { reply.complete(it) }
            }
        }
        try {
            client.addListener(listener).await()
            client.sendMessage(node.id, WatchDeviceInfo.REQUEST,
                JSONObject().put("requestId", requestId).toString().toByteArray(Charsets.UTF_8)).await()
            Result(node.displayName, reply.await())
        } finally { withContext(NonCancellable) { runCatching { withTimeout(2_000) { client.removeListener(listener).await() } } } }
    }
}

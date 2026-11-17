package com.repforge.app

import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.repforge.shared.OwnershipTransport193
import com.repforge.shared.WearDataLayerPaths
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class WearCommandListenerService : WearableListenerService() {
    companion object {
        const val COMMAND_PATH_PREFIX: String = WearDataLayerPaths.COMMAND_PATH_PREFIX

        /** Testable Data Layer dispatch: terminal bilateral errors get a durable error receipt. */
        internal fun dispatchNativeCommand(context: android.content.Context, command: JSONObject, uri: Uri?): JSONObject {
            WearPendingCommandRecovery.capture(context, command)
            val result = PhoneWorkoutExecution.execute(context, command)
            val status = result.optString("status")
            val blocked = PhoneWorkoutExecution.store(context).restoreBlocked() ||
                PhoneWorkoutExecution.store(context).commandsSuspended() || NativeDataReplacement.blocked(context)
            val measuredError = command.optString("action") == "completeMeasuredSet" && command.has("wearBilateralValues") &&
                status == "rejected" && !blocked &&
                !WearPendingCommandRecovery.transient(result)
            val terminalError = status == "rejected" && !blocked && (command.optString("action") == "undoSet" || measuredError)
            if (terminalError) {
                if (measuredError) runCatching { PhoneWorkoutExecution.publish(context) } else PhoneWorkoutExecution.publish(context)
            }
            if (terminalError || status in setOf("applied", "duplicate")) {
                val receiptStatus = if (terminalError) "error" else "applied"
                val committed = try { WearCommandReceipts.complete(context, command, receiptStatus, uri) } catch (transport: Exception) {
                    // A synchronous transport failure cannot undo an already committed receipt.
                    if (WearCommandQueueStore.receipt(context, command.optString("commandId")) == null) throw transport
                    true
                }
                if (!committed) WearCommandQueueStore.enqueue(context, command, uri?.toString())
            } else WearCommandQueueStore.enqueue(context, command, uri?.toString())
            WearSyncRepository.drainNativeUndo188(context)
            return result
        }

        /** Legacy interval controls retain their phase/key fence during local recovery. */
        internal fun dispatchAutomaticCommand(context: android.content.Context, command: JSONObject, uri: Uri?) {
            if (WearCommandQueueStore.seen(context, command.optString("commandId"))) {
                WearCommandReceipts.replay(context, command, uri)
                return
            }
            WearPendingCommandRecovery.capture(context, command)
            when (WearPendingCommandRecovery.admission(context, command)) {
                WearPendingCommandRecovery.Admission.WAIT -> {
                    WearCommandQueueStore.enqueue(context, command, uri?.toString())
                    return
                }
                WearPendingCommandRecovery.Admission.STALE -> {
                    WearCommandReceipts.complete(context, command, "superseded", uri)
                    return
                }
                WearPendingCommandRecovery.Admission.READY -> Unit
            }
            val timer = IntervalTimerService.snapshot(context)
            val active = ActiveWorkoutStore.current(context)
            val action = command.optString("value")
            val automation = command.optJSONObject("automation")
            if (active != null && automation != null && active.optBoolean("active") &&
                !active.optBoolean("workoutPaused") && !active.optBoolean("finalizationPending") &&
                PhoneOwnership193.commandAllowed(context, command) &&
                (!command.has("pauseRevision") || command.optLong("pauseRevision") == active.optLong("pauseRevision")) &&
                active.optString("workoutId") == command.optString("workoutId") &&
                timer.optString("key") == automation.optString("key") && action in setOf("pause", "resume", "skipRest")) {
                val matches = automation.optLong("phaseNumber") == timer.optLong("phaseNumber")
                IntervalTimerService.command(context, timer.optString("key"), if (matches) action else "status")
                if (WearCommandReceipts.complete(context, command, if (matches) "applied" else "superseded", uri)) return
            }
            WearCommandQueueStore.enqueue(context, command, uri?.toString())
        }
    }

    override fun onCreate() {
        super.onCreate()
        WearPendingCommandRecovery.schedule(this)
    }

    override fun onPeerConnected(peer: com.google.android.gms.wearable.Node) {
        WearPendingCommandRecovery.schedule(this)
        // Settings are independent of an active workout: reconnect also republishes the latest palette.
        WearSyncRepository.refreshWatchHomeAsync(this)
        // Retry the durable terminal projection when a previous Data Layer write failed.
        val current = ActiveWorkoutStore.current(this) ?: return
        if (!current.optBoolean("active", true) && current.optString("workoutId").isNotBlank())
            runCatching { WearSyncRepository.publishActiveWorkoutSnapshot(this, current) }
    }

    private fun queue(command: JSONObject, uri: Uri?) {
        try {
            WearCommandQueueStore.enqueue(this, command, uri?.toString())
        } catch (_: Exception) {
            // Retain the DataItem for scanning after storage recovers.
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (WearWorkoutHandoff.REQUEST != event.path || event.data.size > 4096) return
        try {
            val request = JSONObject(String(event.data, StandardCharsets.UTF_8))
            val snapshot = WearWorkoutHandoff.response(
                ActiveWorkoutStore.current(this),
                request,
                System.currentTimeMillis()
            ) ?: return
            Wearable.getMessageClient(this).sendMessage(
                event.sourceNodeId,
                WearWorkoutHandoff.REPLY,
                snapshot.toString().toByteArray(StandardCharsets.UTF_8)
            )
            val map = PutDataMapRequest.create(WearSyncRepository.ACTIVE_WORKOUT_PATH)
            map.dataMap.putString("snapshot", snapshot.toString())
            map.dataMap.putBoolean("active", true)
            map.dataMap.putLong("revision", snapshot.optLong("revision"))
            map.dataMap.putLong("refreshedAt", System.currentTimeMillis())
            Wearable.getDataClient(this).putDataItem(map.asPutDataRequest().setUrgent())
        } catch (_: Exception) {
        }
    }

    private fun drainNativeUndo188() = WearSyncRepository.drainNativeUndo188(this)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val item = event.dataItem ?: continue
            val uri = item.uri ?: continue
            val path = uri.path
            if (OwnershipTransport193.receive(this, item) { message, origin, local ->
                    PhoneOwnership193.receive(this, message, origin, local)
                }
            ) {
                continue
            }
            if (WearLiveHealthStore.PATH == path) {
                try {
                    WearLiveHealthStore.save(
                        this,
                        DataMapItem.fromDataItem(item).dataMap.getString("health")
                    )
                } catch (_: Exception) {
                }
                continue
            }
            if (path == null || (!path.startsWith(COMMAND_PATH_PREFIX) && path != WearDataLayerPaths.WATCH_AUTOMATIC_PATH)) {
                continue
            }
            try {
                val raw = DataMapItem.fromDataItem(item).dataMap.getString("command")
                if (raw.isNullOrEmpty()) continue
                val command = JSONObject(raw)
                command.put("_uri", uri.toString())
                WearPendingCommandRecovery.capture(this, command, incoming = true)

                // 1. Private GPS route chunks (#193)
                if (PhoneGpsRoute193.receive(this, command, uri)) continue

                // 2. Ownership Epoch Fencing (#193)
                if (!PhoneOwnership193.commandAllowed(this, command)) {
                    if (command.optString("action") in setOf("discardWorkout", "finishWorkout")) {
                        // A closed workout may already have a durable deletion marker.
                        // Reconcile terminal Room proof on IO; never relax the fence for edits.
                        WearSyncRepository.processIncomingWatchCommandAsync(this, command, uri)
                    } else WearCommandReceipts.complete(this, command, "superseded", uri)
                    continue
                }

                // 3. Deduplication Replay
                if (WearCommandQueueStore.seen(this, command.optString("commandId"))) {
                    WearCommandReceipts.replay(this, command, uri)
                    Handler(Looper.getMainLooper()).post(::drainNativeUndo188)
                    continue
                }

                // 4. Native Execution Protocol v2 (phone-owned sets, undoSet, timeline start)
                if (PhoneWorkoutExecution.compatible(command)) {
                    Handler(Looper.getMainLooper()).post {
                        try {
                            dispatchNativeCommand(this, command, uri)
                        } catch (_: Exception) {
                            queue(command, uri)
                        }
                    }
                    continue
                }

                // 5. Phone Interval Timer control (pause / resume / skipRest)
                if ("automaticControl" == command.optString("action")) {
                    Handler(Looper.getMainLooper()).post {
                        if (WearCommandQueueStore.seen(this, command.optString("commandId"))) {
                            WearCommandReceipts.replay(this, command, uri)
                            return@post
                        }
                        try { dispatchAutomaticCommand(this, command, uri) }
                        catch (_: Exception) { queue(command, uri) }
                    }
                    continue
                }

                // 6. All other watch commands (startWorkout, finishWorkout, discardWorkout,
                //    saveWorkoutFeedback, healthSummary, updateSet, completeSet, requestHome, etc.)
                //    are now processed 100% natively in the background by WearSyncRepository!
                WearSyncRepository.processIncomingWatchCommandAsync(this, command, uri)
            } catch (_: Exception) {
                // Invalid or malformed watch commands must never affect the phone workout.
            }
        }
    }
}

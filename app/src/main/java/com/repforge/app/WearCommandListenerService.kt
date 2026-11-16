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
            val result = PhoneWorkoutExecution.execute(context, command)
            val status = result.optString("status")
            val frame = PhoneWorkoutExecution.store(context).snapshot()
            val unavailable = frame != null && (!frame.optBoolean("active") || frame.optBoolean("workoutPaused") ||
                PhoneWorkoutExecution.store(context).commandsSuspended())
            val measuredError = command.optString("action") == "completeMeasuredSet" && command.has("wearBilateralValues") &&
                status == "rejected" && !PhoneWorkoutExecution.store(context).restoreBlocked() &&
                (result.optString("reason") !in setOf("executionNotPrepared", "restoreInProgress",
                    "controllerNotArmed", "timerStillActive", "timedResultsAwaitingReconciliation") ||
                    result.optString("reason") == "executionNotPrepared" && unavailable)
            val terminalError = status == "rejected" && (command.optString("action") == "undoSet" || measuredError)
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
    }

    override fun onPeerConnected(peer: com.google.android.gms.wearable.Node) {
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
                        val timer = IntervalTimerService.snapshot(this)
                        val active = ActiveWorkoutStore.current(this)
                        val action = command.optString("value")
                        val autoObj = command.optJSONObject("automation")
                        if (active != null &&
                            autoObj != null &&
                            active.optString("workoutId") == command.optString("workoutId") &&
                            timer.optString("key") == autoObj.optString("key") &&
                            action in listOf("pause", "resume", "skipRest")
                        ) {
                            try {
                                val expectedPhase = autoObj.optLong("phaseNumber")
                                val phaseMatches = expectedPhase == timer.optLong("phaseNumber")
                                IntervalTimerService.command(
                                    this,
                                    timer.optString("key"),
                                    if (phaseMatches) action else "status"
                                )
                                WearCommandReceipts.complete(
                                    this,
                                    command,
                                    if (phaseMatches) "applied" else "superseded",
                                    uri
                                )
                                return@post
                            } catch (_: Exception) {
                            }
                        }
                        queue(command, uri)
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

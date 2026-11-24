package com.example.ui.viewmodel

import android.app.Activity
import com.google.android.gms.wearable.Wearable
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.NativeWearCompanion
import com.repforge.app.WearLiveHealthStore
import com.repforge.app.WearSyncRepository
import com.repforge.shared.WearMeasurementRequest351
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

/** Only a newly committed, unpaused phone-owned workout can request recording. */
internal fun watchMeasurementEligible(frame: JSONObject?, workoutId: String): Boolean =
    frame != null && workoutId.isNotBlank() && frame.optBoolean("active") &&
        frame.optString("workoutId") == workoutId && !frame.optBoolean("workoutPaused") &&
        !frame.optBoolean("finalizationPending") && !frame.optBoolean("discardPending") &&
        frame.optString("timerOwner", "phone") == "phone" &&
        !com.repforge.shared.WorkoutOwnership193.pending(frame)

internal fun watchMeasurementReply(health: JSONObject?, workoutId: String): String? =
    health?.takeIf { workoutId.isNotBlank() && it.optString("workoutId") == workoutId }
        ?.optString("state")?.takeIf { it in setOf("active", "paused", "starting", "disabled", "permissionRequired", "error", "ended", "foregroundRequired") }

internal fun TrainPilotViewModel.queueWatchMeasurementStart(frame: JSONObject) {
    val id = frame.optString("workoutId")
    if (!watchMeasurementEligible(frame, id)) return
    _uiState.update { it.copy(watchMeasurementStatusId = id, watchMeasurementStatus =
        tr("Órás mérés ellenőrzése…", "Checking watch recording…", "Uhrmessung wird geprüft…", "Se verifică măsurarea ceasului…")) }
    pendingWatchMeasurementStart.value = id
}

/** Remote open is not a recording receipt. The existing live-health channel is authoritative. */
internal suspend fun TrainPilotViewModel.launchPendingWatchMeasurement(activity: Activity?, id: String) {
    val ctx = nativeContext ?: return
    fun eligible() = watchMeasurementEligible(ActiveWorkoutStore.current(ctx), id)
    fun status(message: String?) {
        if (eligible() && pendingWatchMeasurementStart.value == id)
            _uiState.update { it.copy(watchMeasurementStatusId = id, watchMeasurementStatus = message) }
    }
    fun reply(): String? {
        val frame=ActiveWorkoutStore.current(ctx) ?: return null
        return watchMeasurementReply(com.repforge.app.PhoneMeasurementReconnect351.freshReceipt(
            WearLiveHealthStore.current(ctx),id,frame.optLong("ownerEpoch193"),
            WearLiveHealthStore.expectedNode351.value,System.currentTimeMillis()),id)
    }
    fun manual() = status(tr("Az órás mérés nincs visszaigazolva. Nyisd meg a TrainPilotot az órán.",
        "Watch recording not confirmed. Open TrainPilot on the watch.",
        "Uhrmessung nicht bestätigt. TrainPilot auf der Uhr öffnen.",
        "Măsurarea nu este confirmată. Deschide TrainPilot pe ceas."))
    try {
        if (!eligible()) return
        val nodes = withTimeout(5000) { Wearable.getNodeClient(ctx).connectedNodes.await() }
        if (nodes.size != 1) {
            status(tr("Órás mérés: nincs egyértelmű órakapcsolat.", "Watch recording: no unique watch connection.",
                "Uhrmessung: keine eindeutige Verbindung.", "Măsurare ceas: conexiune neclară."))
            return
        }
        if (!withTimeout(5000) { NativeWearCompanion.reachable(ctx, nodes.single().id) }) {
            status(tr("A TrainPilot óraalkalmazás nem érhető el.", "TrainPilot watch app unavailable.",
                "TrainPilot-Uhr-App nicht verfügbar.", "Aplicația TrainPilot de pe ceas nu este disponibilă."))
            return
        }
        WearLiveHealthStore.expectNode351(nodes.single().id)
        val frame=ActiveWorkoutStore.current(ctx)
        if (!watchMeasurementEligible(frame,id)) return
        val request=WearMeasurementRequest351.fromFrame(requireNotNull(frame),System.currentTimeMillis())
        // Target the verified companion; message delivery is not a measurement acknowledgement.
        if(request!=null)try {
            withTimeout(3000) { Wearable.getMessageClient(ctx).sendMessage(nodes.single().id,
                WearMeasurementRequest351.PATH,request.encode()).await() }
        } catch (_: TimeoutCancellationException) {
            // Older companions or failed delivery still use the existing visible fallback.
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { }
        // Allow background promotion and an actual Health Services acknowledgement first.
        for (attempt in 0 until 20) {
            if (!eligible()) return
            if (reply() in setOf("active", "paused", "disabled", "permissionRequired", "error", "ended")) {
                status(null); return
            }
            if (reply()=="foregroundRequired") break
            delay(250)
        }
        if (!eligible()) return
        // When the existing service is starting, don't open another Activity.
        if (reply() != "starting") {
            if (activity == null) { manual(); return }
            withTimeout(15000) {
                WearSyncRepository.startWatchMeasurementRemotely(activity, id, requestOwnership = false).getOrThrow()
            }
        }
        repeat(60) {
            if (!eligible()) return
            if (reply() in setOf("active", "paused", "disabled", "permissionRequired", "error", "ended")) {
                status(null); return
            }
            delay(250)
        }
        manual()
    } catch (_: TimeoutCancellationException) {
        manual()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        manual()
    } finally {
        if (pendingWatchMeasurementStart.value == id) pendingWatchMeasurementStart.value = null
    }
}

package com.example.ui.viewmodel

import android.app.Activity
import com.google.android.gms.wearable.Wearable
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.NativeWearCompanion
import com.repforge.app.WearLiveHealthStore
import com.repforge.app.WearSyncRepository
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
        frame.optString("timerOwner", "phone") == "phone" &&
        !com.repforge.shared.WorkoutOwnership193.pending(frame)

internal fun watchMeasurementReply(health: JSONObject?, workoutId: String): String? =
    health?.takeIf { workoutId.isNotBlank() && it.optString("workoutId") == workoutId }
        ?.optString("state")?.takeIf { it in setOf("active", "paused", "starting", "disabled", "permissionRequired", "error", "ended") }

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
    fun reply() = watchMeasurementReply(WearLiveHealthStore.current(ctx), id)
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
        // An already visible watch can start from the canonical snapshot without changing screens.
        // Give its existing Health Services flow a chance to acknowledge first.
        repeat(8) {
            if (!eligible()) return
            if (reply() in setOf("active", "paused", "disabled", "permissionRequired", "error", "ended")) {
                status(null); return
            }
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

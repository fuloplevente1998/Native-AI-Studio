package com.example.ui.viewmodel

import java.time.LocalTime
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.WearSyncRepository
import com.example.model.watchinfo.WatchInfoCardData
import com.example.model.watchinfo.WatchInfoStatus
import java.time.format.DateTimeFormatter
import com.repforge.app.PhoneOwnership193

// Watch, Settings, Backup & Permissions Actions (P0-04: real Wear OS node & ACK check)
fun TrainPilotViewModel.toggleWatchControlMode(activity: android.app.Activity? = null) {
    val ctx = nativeContext ?: return
    val scope = nativeScope ?: return
    if (_uiState.value.watchControlPending || _uiState.value.isSavingWorkout) return
    scope.launch(Dispatchers.Main.immediate) {
        val active = ActiveWorkoutStore.current(ctx)?.takeIf { it.optBoolean("active") }
        if (active == null) { showBannerMessage("Nincs átadható aktív edzés."); return@launch }
        val id = active.optString("workoutId")
        if (active.optInt("ownershipProtocol193") != 1) {
            showBannerMessage("Ez a korábbi edzés nem támogatja az órás átadást; a telefonon biztonságosan folytatható.")
            return@launch
        }
        watchHandoffBusy = true
        suspend fun awaitOwner(owner: String) {
            kotlinx.coroutines.withTimeout(32_000) {
                while (!com.repforge.app.NativeWatchHandoffReceipt.confirmed(
                        ActiveWorkoutStore.current(ctx), PhoneOwnership193.journal(ctx).read(), id, owner)) {
                    kotlinx.coroutines.delay(200)
                }
            }
        }
        _uiState.update { it.copy(watchControlPending = true, statusBannerMessage = "Óra kapcsolat és tulajdonosi ACK ellenőrzése…") }
        try {
            if (com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(active)) {
                com.repforge.app.NativePhoneReturnInstallation.complete(ctx)
                showBannerMessage("A telefon visszavette az edzést. Az edzés szünetel; folytatható vagy menthető.")
                return@launch
            }
            val nodes = kotlinx.coroutines.withTimeout(5_000) {
                com.google.android.gms.wearable.Wearable.getNodeClient(ctx).connectedNodes.await()
            }
            check(nodes.size == 1) { if (nodes.isEmpty()) "Nincs csatlakoztatott óra; a tulajdonos változatlan." else "Több óra csatlakozik; nem egyértelmű az átadás célja." }
            check(kotlinx.coroutines.withTimeout(5_000) {
                com.repforge.app.NativeWearCompanion.reachable(ctx, nodes.single().id)
            }) { "A TrainPilot Wear OS alkalmazás nem érhető el. Telepítsd a telefonos APK-val párosított óraalkalmazást." }
            if (active.optString("timerOwner", "phone") == "phone") {
                com.repforge.app.NativePrivacyControls.requireEnabled(ctx, com.repforge.app.NativePrivacyControls.TRAINPILOT_WEAR)
                check(!com.repforge.app.DistanceTrackingService.snapshot(ctx).optBoolean("active")) {
                    "Aktív GPS-mérés közben nem adható át a vezérlés. Előbb fejezd be a GPS-mérést."
                }
                checkNotNull(activity) { "Az óra indításához látható telefonos felület szükséges." }
                com.repforge.app.StudioWorkoutBridge.pauseForHandover(ctx, id)
                kotlinx.coroutines.withTimeout(15_000) {
                    WearSyncRepository.startWatchMeasurementRemotely(activity, id, requestOwnership = true).getOrThrow()
                }
                // Remote activity launch is a request, not an ownership receipt.
                awaitOwner("watch")
            } else {
                val local = com.google.android.gms.wearable.Wearable.getNodeClient(ctx).localNode.await()
                val journal = PhoneOwnership193.journal(ctx)
                journal.recover()
                journal.request(local.id, nodes.single().id, System.currentTimeMillis())
                com.repforge.shared.OwnershipTransport193.send(ctx, journal)
                awaitOwner("phone")
                com.repforge.app.NativePhoneReturnInstallation.complete(ctx)
            }
            val current = checkNotNull(ActiveWorkoutStore.current(ctx))
            check(current.optString("workoutId") == id && !com.repforge.shared.WorkoutOwnership193.pending(current))
            _uiState.update { it.copy(watchConnected = true, watchName = nodes.single().displayName,
                phoneControlsWorkout = current.optString("timerOwner", "phone") == "phone",
                lastWatchSyncLabel = "Tulajdonosi ACK igazolva", statusBannerMessage = "Az edzésvezérlés átadása visszaigazolva; az edzés szünetel, explicit Folytatás szükséges.") }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException && error !is kotlinx.coroutines.TimeoutCancellationException) throw error
            runCatching { PhoneOwnership193.journal(ctx).recover() }
            showBannerMessage(error.message ?: "Nem érkezett igazolt tulajdonosi ACK.")
        } finally {
            watchHandoffBusy = false
            val current = ActiveWorkoutStore.current(ctx)
            val pending = com.repforge.app.NativeWatchHandoffReceipt.pending(current, watchJournal())
            _uiState.update { it.copy(watchControlPending = pending,
                phoneControlsWorkout = (current == null || current.optString("timerOwner", "phone") == "phone") && !pending && !com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(current),
                lastWatchSyncLabel = if (pending) "Átadás függőben; vezérlés zárolva" else it.lastWatchSyncLabel) }
        }
    }
}

fun TrainPilotViewModel.requestWatchDeviceInfo() {
    val context = nativeContext ?: return
    val scope = nativeScope ?: return
    // Keep the single in-flight native request and its node/nonce/timeout protections.
    if (_uiState.value.watchInfoLoading) return
    _uiState.update { it.copy(
        watchInfoLoading = true, watchInfoMessage = "Óraadatok lekérése…",
        watchInfoCardData = it.watchInfoCardData.copy(
            loading = true, status = "Óraadatok lekérése…", statusKind = WatchInfoStatus.IDLE)
    ) }
    scope.launch {
        try {
            val result = com.repforge.app.NativeWatchDeviceInfo.request(context)
            val data = result.data
            val battery = if (data.isNull("battery")) -1 else
                com.repforge.shared.WatchDeviceInfo.battery(data.optInt("battery", -1)) ?: -1
            val time = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            val verifiedCard = WatchInfoCardData.fromVerifiedReply(
                nodeName = result.nodeName, reply = data,
                connected = true, appAvailable = true,
                status = "Óraadatok frissítve: $time",
                statusKind = WatchInfoStatus.SUCCESS, lastSync = time)
            _uiState.update { it.copy(
                watchInfoCardData = verifiedCard,
                watchName = result.nodeName, watchConnected = true, watchAppAvailable = true,
                watchBatteryPercent = battery, watchInfoMessage = "Óraadatok frissítve: $time",
                watchDeviceDetails = listOf(data.optString("model"),
                    data.optString("os").takeIf { it.isNotBlank() }?.let { "Android $it" },
                    data.optString("appVersion").takeIf { it.isNotBlank() && it != "null" }?.let { "TrainPilot $it" })
                    .filterNotNull().filter(String::isNotBlank).joinToString(" • "),
                lastWatchSyncLabel = "Óraadatok: $time") }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            val msg = "Az óra nem válaszolt. Ellenőrizd az órakapcsolatot és frissítsd az óraappot is."
            _uiState.update { it.copy(watchInfoMessage = msg,
                watchInfoCardData = it.watchInfoCardData.copy(
                    status = msg, statusKind = WatchInfoStatus.TIMEOUT, loading = false)) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            val msg = error.message ?: "Az óraadatok nem kérhetők le."
            _uiState.update { it.copy(watchInfoMessage = msg,
                watchInfoCardData = it.watchInfoCardData.copy(
                    status = msg, statusKind = WatchInfoStatus.ERROR, loading = false)) }
        } finally {
            _uiState.update { it.copy(watchInfoLoading = false,
                watchInfoCardData = it.watchInfoCardData.copy(loading = false)) }
        }
    }
}

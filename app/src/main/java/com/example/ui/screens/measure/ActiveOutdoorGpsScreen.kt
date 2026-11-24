package com.example.ui.screens

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.localization.LocalizedStrings
import com.example.model.SubScreen
import com.example.ui.components.NativeWorkoutRouteMapBox
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import androidx.compose.runtime.rememberCoroutineScope
import com.repforge.app.ActiveWorkoutStore
import org.json.JSONObject

/**
 * 4. KÜLTÉRI MÉRÉS (Mockup 2/4 #4 - Futás GPS - Valós DistanceTrackingService & LocationManager,
 * szimulált +2,75 m/mp és képletes pulzus NÉLKÜL)
 */
@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("MissingPermission")
@Composable
fun ActiveOutdoorGpsScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit,
    onUpdateOutdoorMetrics: (String, Float, String) -> Unit = { _, _, _ -> },
    onSelectOutdoorActivity: (String) -> Unit = {},
    onStartGpsTracking: (String, String) -> Boolean = { _, _ -> false },
    onPauseMeasurement: (Boolean) -> Boolean = { false },
    onAdjustRestTimer: (Int) -> Unit = {},
    onToggleRestRunning: () -> Unit = {},
    onSkipRest: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val context = LocalContext.current

    // Observe canonical DistanceTrackingService state flow
    val serviceState by com.repforge.app.DistanceTrackingService.gpsStateFlow.collectAsState()
    val initialSnap = remember { com.repforge.app.DistanceTrackingService.snapshot(context) }
    val candidateSnap = if (serviceState.length() > 0) serviceState else initialSnap
    val activeSnap = if (uiState.outdoorRouteKey.isNotBlank() && candidateSnap.optString("key") == uiState.outdoorRouteKey)
        candidateSnap else org.json.JSONObject()

    val screenScope = androidx.compose.runtime.rememberCoroutineScope()
    val nativeMeasurement: com.repforge.app.ui.workout.ActiveWorkoutViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var startRequested by remember { mutableStateOf(false) }
    var gpsActive by remember { mutableStateOf(uiState.outdoorGpsEnabled) }
    var isStarted by remember { mutableStateOf(activeSnap.optBoolean("active", false)) }
    var isPaused by remember { mutableStateOf(activeSnap.optBoolean("paused", false)) }
    var elapsedSec by remember { mutableIntStateOf(activeSnap.optLong("elapsedSeconds", 0L).toInt()) }
    val initialFrame = remember(uiState.activeWorkoutId) { com.repforge.app.ActiveWorkoutStore.current(context) }
    val initialManualMeters = remember(uiState.activeWorkoutId) {
        com.repforge.app.StudioWorkoutBridge.readOutdoorManualDistanceMeters(initialFrame)?.toFloat()
    }
    var distanceMeters by remember { mutableFloatStateOf(activeSnap.optDouble("distanceMeters", 0.0).toFloat()) }
    var manualDistanceMetersState by remember(uiState.activeWorkoutId) { mutableStateOf<Float?>(initialManualMeters) }
    var manualDistanceInput by androidx.compose.runtime.saveable.rememberSaveable(uiState.activeWorkoutId) {
        mutableStateOf(
            initialManualMeters?.let { String.format(java.util.Locale.US, "%.2f", it / 1000f).trimEnd('0').trimEnd('.') } ?: ""
        )
    }
    var manualDistanceError by androidx.compose.runtime.saveable.rememberSaveable(uiState.activeWorkoutId) { mutableStateOf("") }
    var realGpsFixesCount by remember { mutableIntStateOf(activeSnap.optInt("routePointCount", 0)) }
    var hasServiceFix by remember { mutableStateOf(activeSnap.optBoolean("hasFix", false)) }
    var gpsAccuracyMeters by remember { mutableFloatStateOf(activeSnap.optDouble("accuracyMeters", 0.0).toFloat()) }
    var finishErrorMsg by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var serviceErrorMsg by remember { mutableStateOf(activeSnap.optString("error", "")) }
    var lastGpsLocation by remember { mutableStateOf<Location?>(null) }
    var lapCount by remember { mutableIntStateOf(1) }
    var lapSplits by remember { mutableStateOf(listOf<String>()) }
    var isFullscreenMapOpen by remember { mutableStateOf(false) }
    var routeOwnerKey by remember { mutableStateOf(
        uiState.outdoorRouteKey.takeIf(String::isNotBlank)
            ?: initialSnap.optString("key").takeIf { initialSnap.optBoolean("active") }
            ?: "outdoor_run_${java.util.UUID.randomUUID()}"
    ) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(uiState.activeWorkoutId) {
        nativeMeasurement.onEditorVisible()
        val currentFrame = com.repforge.app.ActiveWorkoutStore.current(context)
        val persistedManual = com.repforge.app.StudioWorkoutBridge.readOutdoorManualDistanceMeters(currentFrame)?.toFloat()
        if (persistedManual != null && persistedManual > 0f) {
            manualDistanceMetersState = persistedManual
            if (manualDistanceInput.isBlank()) {
                manualDistanceInput = String.format(java.util.Locale.US, "%.2f", persistedManual / 1000f).trimEnd('0').trimEnd('.')
            }
        }
    }

    // Sync from DistanceTrackingService whenever its state updates
    LaunchedEffect(serviceState) {
        if (gpsActive && serviceState.optString("key") == routeOwnerKey) {
            val binding = com.repforge.app.ActiveWorkoutStore.current(context)?.optJSONObject(com.repforge.app.NativeGpsMeasurement.FIELD)
            val baseMeters = binding?.optDouble("baseMeters", 0.0) ?: 0.0
            val baseSeconds = binding?.optLong("baseSeconds", 0) ?: 0
            val srvActive = serviceState.optBoolean("active", false)
            if (srvActive) {
                isStarted = true
                isPaused = serviceState.optBoolean("paused", false)
            }
            val srvDist = serviceState.optDouble("distanceMeters", Double.NaN)
            if (srvDist.isFinite() && srvDist >= 0.0) {
                distanceMeters = (baseMeters + srvDist).toFloat()
            }
            val srvElapsed = serviceState.optLong("elapsedSeconds", -1L)
            if (srvElapsed >= 0L && srvElapsed.toInt() > elapsedSec) {
                elapsedSec = (baseSeconds + srvElapsed).toInt()
            }
            realGpsFixesCount = serviceState.optInt("routePointCount", realGpsFixesCount)
            hasServiceFix = serviceState.optBoolean("hasFix", false)
            gpsAccuracyMeters = serviceState.optDouble("accuracyMeters", 0.0).toFloat()
            serviceErrorMsg = serviceState.optString("error", "")
        }
    }

    LaunchedEffect(uiState.activeWorkoutId, gpsActive) {
        if (!gpsActive) {
            val frame = com.repforge.app.PhoneWorkoutExecution.store(context).snapshot()
            if (frame?.optString("workoutId") == uiState.activeWorkoutId) {
                val clock = frame.optJSONObject(com.repforge.app.NativeSetStopwatch.FIELD)
                isStarted = clock != null
                isPaused = frame.optBoolean("workoutPaused") || clock?.let { !com.repforge.app.NativeSetStopwatch.running(it) } == true
                elapsedSec = clock?.let { (com.repforge.app.NativeSetStopwatch.elapsed(it) / 1000).toInt() } ?: 0
            }
        }
    }

    val lang = uiState.language
    val startNativeGpsTracking = {
        if (!startRequested && (!isStarted || isPaused)) {
            startRequested = true
            serviceErrorMsg = tr(
                lang,
                "GPS-szolgáltatás indul; visszaigazolásra vár…",
                "Starting GPS service; waiting for confirmation…",
                "GPS-Dienst startet; warte auf Bestätigung…",
                "Serviciul GPS pornește; se așteaptă confirmarea…"
            )
            screenScope.launch {
                var confirmed = false
                try {
                    val activityName = uiState.activeWorkoutTitleHu.ifBlank { "Kültéri futás" }
                    check(onStartGpsTracking(activityName, routeOwnerKey)) {
                        tr(lang, "A natív munkamenet nem készíthető elő.", "Native session could not be prepared.", "Native Sitzung konnte nicht vorbereitet werden.", "Sesiunea nativă nu a putut fi pregătită.")
                    }
                    nativeMeasurement.onEditorVisible()
                    val frame = checkNotNull(com.repforge.app.PhoneWorkoutExecution.store(context).snapshot())
                    check(kotlinx.coroutines.withTimeoutOrNull(5_000) {
                        while (!com.repforge.app.IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation"))) delay(100)
                        true
                    } == true) {
                        tr(lang, "A natív háttérvezérlő nem igazolta vissza az indítást.", "Native background controller did not confirm startup.", "Nativer Hintergrunddienst hat den Start nicht bestätigt.", "Controlerul nativ de fundal nu a confirmat pornirea.")
                    }
                    val exercise = frame.getJSONArray("exercises").getJSONObject(frame.optInt("currentExercise"))
                    val sets = exercise.getJSONArray("sets")
                    val set = (0 until sets.length()).map { sets.getJSONObject(it) }.first { !it.optBoolean("done") }
                    val target = com.repforge.app.NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"), exercise.getString("id"), set.getString("setId"))
                    val prepared = com.repforge.app.NativeWorkoutEditing.startGps(context, target)
                    val binding = prepared.getJSONObject(com.repforge.app.NativeGpsMeasurement.FIELD)
                    routeOwnerKey = binding.getString("key")
                    val receipt = kotlinx.coroutines.withTimeoutOrNull(10_000) {
                        while (true) {
                            val snap = com.repforge.app.DistanceTrackingService.snapshot(context)
                            if (snap.optString("key") == routeOwnerKey && snap.optBoolean("active")) break
                            delay(100)
                        }
                        true
                    } == true
                    check(receipt) {
                        tr(lang, "A GPS-szolgáltatás nem igazolta vissza az indítást.", "GPS service did not confirm startup.", "GPS-Dienst hat den Start nicht bestätigt.", "Serviciul GPS nu a confirmat pornirea.")
                    }
                    check(onStartGpsTracking(activityName, routeOwnerKey)) {
                        tr(lang, "A GPS-mérés natív edzéshez kötése nem sikerült.", "Binding GPS tracking to native workout failed.", "Verknüpfung der GPS-Messung mit dem nativen Training fehlgeschlagen.", "Asocierea măsurătorii GPS cu antrenamentul nativ a eșuat.")
                    }
                    confirmed = true
                    isStarted = true
                    isPaused = false
                    serviceErrorMsg = ""
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) {
                    serviceErrorMsg = error.message ?: tr(lang, "A GPS-mérés nem indult el.", "GPS tracking did not start.", "GPS-Messung wurde nicht gestartet.", "Măsurătoarea GPS nu a pornit.")
                }
                finally {
                    startRequested = false
                    if (!confirmed) {
                        isStarted = false
                        runCatching { com.repforge.app.NativeWorkoutEditing.interruptGps(context) }
                    }
                }
            }
        }
    }

    val startIndoorMeasurement = {
        try {
            check(onStartGpsTracking(uiState.activeWorkoutTitleHu.ifBlank { "Kültéri futás" }, "")) {
                tr(lang, "A beltéri munkamenet nem menthető.", "Indoor session could not be saved.", "Indoor-Sitzung konnte nicht gespeichert werden.", "Sesiunea indoor nu a putut fi salvată.")
            }
            nativeMeasurement.onEditorVisible()
            val frame = com.repforge.app.StudioWorkoutBridge.startIndoor(context)
            manualDistanceMetersState?.let { manualM ->
                if (manualM > 0f) {
                    com.repforge.app.StudioWorkoutBridge.setOutdoorManualDistance(context, manualM.toDouble(), markDoneIfPositive = false)
                }
            }
            isStarted = frame.optJSONObject(com.repforge.app.NativeSetStopwatch.FIELD)?.let {
                com.repforge.app.NativeSetStopwatch.running(it)
            } == true
            isPaused = !isStarted
            serviceErrorMsg = ""
        } catch (error: Exception) {
            serviceErrorMsg = error.message ?: tr(lang, "A beltéri időmérés nem indult el.", "Indoor timer did not start.", "Indoor-Zeitmessung wurde nicht gestartet.", "Cronometrul indoor nu a pornit.")
        }
    }

    val pauseMeasurement = {
        val paused = !isPaused
        if (onPauseMeasurement(paused)) {
            if (gpsActive && !paused) startNativeGpsTracking()
            else {
                if (!gpsActive && !paused) com.repforge.app.StudioWorkoutBridge.startIndoor(context)
                isPaused = paused
            }
        }
    }

    val applyManualDistanceInput = { markDone: Boolean ->
        val validMeters = com.repforge.app.StudioWorkoutBridge.validateManualDistanceKm(manualDistanceInput)
        if (validMeters == null) {
            manualDistanceError = tr(
                lang,
                "Adj meg érvényes, pozitív távolságot (0,01 – 2000 km).",
                "Enter a valid positive distance (0.01 – 2000 km).",
                "Gib eine gültige positive Distanz ein (0,01 – 2000 km).",
                "Introdu o distanță pozitivă validă (0,01 – 2000 km)."
            )
            false
        } else {
            val activityName = uiState.activeWorkoutTitleHu.ifBlank { "Kültéri futás" }
            if (com.repforge.app.ActiveWorkoutStore.current(context) == null) {
                onStartGpsTracking(activityName, if (gpsActive) routeOwnerKey else "")
            }
            val updated = com.repforge.app.StudioWorkoutBridge.setOutdoorManualDistance(
                context,
                validMeters,
                markDoneIfPositive = markDone
            )
            if (updated == null) {
                manualDistanceError = tr(
                    lang,
                    "A távolság nem menthető ebben az edzésállapotban.",
                    "Distance could not be saved in this workout state.",
                    "Distanz konnte in diesem Trainingszustand nicht gespeichert werden.",
                    "Distanța nu a putut fi salvată în această stare."
                )
                false
            } else {
                manualDistanceMetersState = validMeters.toFloat()
                manualDistanceError = ""
                val min = elapsedSec / 60
                val sec = elapsedSec % 60
                val formattedTime = String.format("%02d:%02d", min, sec)
                val distKm = (validMeters / 1000.0).toFloat()
                val formattedPace = if (distKm >= 0.01f && elapsedSec > 0) {
                    val paceSecPerKm = (elapsedSec / distKm).toInt()
                    String.format("%d:%02d", (paceSecPerKm / 60).coerceIn(1, 59), (paceSecPerKm % 60).coerceIn(0, 59))
                } else {
                    "—"
                }
                onUpdateOutdoorMetrics(formattedTime, distKm, formattedPace)
                true
            }
        }
    }

    val clearManualDistance = {
        com.repforge.app.StudioWorkoutBridge.setOutdoorManualDistance(context, null, markDoneIfPositive = false)
        manualDistanceMetersState = null
        manualDistanceInput = ""
        manualDistanceError = ""
        val min = elapsedSec / 60
        val sec = elapsedSec % 60
        val formattedTime = String.format("%02d:%02d", min, sec)
        val distKm = distanceMeters / 1000f
        val formattedPace = if (distKm >= 0.01f && elapsedSec > 0) {
            val paceSecPerKm = (elapsedSec / distKm).toInt()
            String.format("%d:%02d", (paceSecPerKm / 60).coerceIn(1, 59), (paceSecPerKm % 60).coerceIn(0, 59))
        } else {
            "—"
        }
        onUpdateOutdoorMetrics(formattedTime, distKm, formattedPace)
    }

    val finishMeasurement = finish@{
        finishErrorMsg = ""
        if (manualDistanceInput.isNotBlank() && manualDistanceMetersState == null) {
            if (!applyManualDistanceInput(false)) return@finish false
        }
        var recorded = false
        try {
            nativeMeasurement.onEditorVisible()
            if (com.repforge.app.ActiveWorkoutStore.current(context) == null) {
                check(onStartGpsTracking(uiState.activeWorkoutTitleHu.ifBlank { "Kültéri futás" }, if (gpsActive) routeOwnerKey else ""))
            }
            var frame = com.repforge.app.NativeWorkoutEditing.project(context)
            if (!gpsActive && frame.optBoolean("workoutPaused")) {
                check(onPauseMeasurement(false))
                frame = com.repforge.app.NativeWorkoutEditing.project(context)
            }
            val saved = if (gpsActive && frame.optJSONObject(com.repforge.app.NativeGpsMeasurement.FIELD) != null) {
                val binding = frame.getJSONObject(com.repforge.app.NativeGpsMeasurement.FIELD)
                if (binding.optBoolean("applied")) frame
                else com.repforge.app.NativeWorkoutEditing.finishGps(context,
                    com.repforge.app.NativeGpsMeasurement.target(frame), binding.getString("id"))
            } else if (!gpsActive && (isStarted || frame.optJSONObject(com.repforge.app.NativeSetStopwatch.FIELD) != null)) {
                val clock = frame.optJSONObject(com.repforge.app.NativeSetStopwatch.FIELD)
                if (clock == null || !com.repforge.app.NativeSetStopwatch.running(clock)) {
                    frame = com.repforge.app.StudioWorkoutBridge.startIndoor(context)
                }
                val current = frame.getJSONObject(com.repforge.app.NativeSetStopwatch.FIELD)
                com.repforge.app.NativeWorkoutEditing.stopStopwatch(context, com.repforge.app.NativeWorkoutTarget(
                    frame.getString("workoutId"), frame.getLong("generation"), current.getString("exerciseId"), current.getString("setId")))
            } else {
                frame
            }
            val manualToCommit = manualDistanceMetersState?.takeIf { it > 0f }?.toDouble()
                ?: com.repforge.app.StudioWorkoutBridge.readOutdoorManualDistanceMeters(saved)
            val afterManual = if (manualToCommit != null && manualToCommit > 0.0) {
                com.repforge.app.StudioWorkoutBridge.setOutdoorManualDistance(context, manualToCommit, markDoneIfPositive = true) ?: saved
            } else {
                saved
            }
            val exercises = afterManual.getJSONArray("exercises")
            recorded = (0 until exercises.length()).any { i ->
                val sets = exercises.getJSONObject(i).getJSONArray("sets")
                (0 until sets.length()).any { sets.getJSONObject(it).optBoolean("done") }
            }
            if (gpsActive && !recorded) {
                com.repforge.app.StudioOutdoorFinish.receipt(afterManual)?.let { receipt ->
                    afterManual.put(com.repforge.app.StudioOutdoorFinish.FIELD, receipt)
                    com.repforge.app.NativeWorkoutEditing.commitEdit(context, afterManual, gpsEdit = true)
                }
            }
            val finalDistMeters = (manualToCommit ?: distanceMeters.toDouble()).toFloat()
            val finalDistKm = finalDistMeters / 1000f
            val finalTimeLabel = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60)
            val finalPaceLabel = if (finalDistKm >= 0.01f && elapsedSec > 0) {
                val paceSecPerKm = (elapsedSec / finalDistKm).toInt()
                String.format("%d:%02d", (paceSecPerKm / 60).coerceIn(1, 59), (paceSecPerKm % 60).coerceIn(0, 59))
            } else "—"
            onUpdateOutdoorMetrics(finalTimeLabel, finalDistKm, finalPaceLabel)
            // Stopping and opening Summary never requires inventing a completed set.
            // Save still requires a completed native set or a verified positive-time receipt.
            recorded = true
            isPaused = true
        } catch (error: Exception) {
            finishErrorMsg = error.message ?: tr(lang, "A mérés nem rögzíthető.", "Measurement could not be recorded.", "Messung konnte nicht erfasst werden.", "Măsurătoarea nu a putut fi înregistrată.")
        }
        recorded
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val grantedFine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        hasLocationPermission = grantedFine
        if (grantedFine && gpsActive) {
            startNativeGpsTracking()
        } else if (!grantedFine && gpsActive) {
            serviceErrorMsg = tr(
                lang,
                "A GPS-méréshez pontos helymeghatározási engedély szükséges. Engedély nélkül 0 m kerül rögzítésre.",
                "Precise location permission is required for GPS tracking. Without permission, 0 m will be recorded.",
                "Für die GPS-Messung ist die genaue Standortberechtigung erforderlich. Ohne Berechtigung werden 0 m erfasst.",
                "Pentru măsurătoarea GPS este necesară permisiunea de locație precisă. Fără permisiune se vor înregistra 0 m."
            )
        }
    }

    // Live 1-second stopwatch loop — STRICTLY NO FAKE DISTANCE OR FAKE HEART RATE
    LaunchedEffect(isStarted, isPaused) {
        while (isStarted && !isPaused) {
            delay(1000L)
            val frame = com.repforge.app.PhoneWorkoutExecution.store(context).snapshot()
            if (!gpsActive && frame != null) {
                com.repforge.app.PhoneWorkoutExecution.store(context).checkpointStopwatch(
                    frame.optString("workoutId"), frame.optLong("generation"), android.os.SystemClock.elapsedRealtime())
                val current = com.repforge.app.PhoneWorkoutExecution.store(context).snapshot()
                val clock = current?.optJSONObject(com.repforge.app.NativeSetStopwatch.FIELD)
                elapsedSec = clock?.let { (com.repforge.app.NativeSetStopwatch.elapsed(it) / 1000).toInt() } ?: 0
                isPaused = current?.optBoolean("workoutPaused") == true
            }
            val snap = com.repforge.app.DistanceTrackingService.snapshot(context)
            if (gpsActive && snap.optString("key") == routeOwnerKey && snap.optBoolean("active", false)) {
                val binding = com.repforge.app.ActiveWorkoutStore.current(context)?.optJSONObject(com.repforge.app.NativeGpsMeasurement.FIELD)
                elapsedSec = ((binding?.optLong("baseSeconds", 0) ?: 0) + snap.optLong("elapsedSeconds", 0)).toInt()
                val srvMeters = ((binding?.optDouble("baseMeters", 0.0) ?: 0.0) + snap.optDouble("distanceMeters", 0.0)).toFloat()
                if (srvMeters > distanceMeters) {
                    distanceMeters = srvMeters
                }
                realGpsFixesCount = maxOf(realGpsFixesCount, snap.optInt("routePointCount", 0))
                hasServiceFix = snap.optBoolean("hasFix", hasServiceFix)
            }
            val min = elapsedSec / 60
            val sec = elapsedSec % 60
            val formattedTime = String.format("%02d:%02d", min, sec)
            val effectiveMeters = manualDistanceMetersState ?: distanceMeters
            val distKm = effectiveMeters / 1000f
            val formattedPace = if (distKm >= 0.01f && elapsedSec > 0) {
                val paceSecPerKm = (elapsedSec / distKm).toInt()
                String.format("%d:%02d", (paceSecPerKm / 60).coerceIn(1, 59), (paceSecPerKm % 60).coerceIn(0, 59))
            } else {
                "—"
            }
            onUpdateOutdoorMetrics(formattedTime, distKm, formattedPace)
        }
    }

    val formattedElapsed = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60)
    val effectiveDistanceMeters = manualDistanceMetersState ?: distanceMeters
    val distanceKm = effectiveDistanceMeters / 1000f
    val formattedDistance = String.format(com.example.localization.StudioLocale.forLanguage(lang), "%.2f km", distanceKm)
    val formattedPace = if (distanceKm >= 0.01f && elapsedSec > 0) {
        val paceSecPerKm = (elapsedSec / distanceKm).toInt()
        String.format("%d:%02d /km", (paceSecPerKm / 60).coerceIn(1, 59), (paceSecPerKm % 60).coerceIn(0, 59))
    } else {
        "— /km"
    }

    // Real sensor HR only (from WearLiveHealthStore or Health sync) — never fabricated
    val wearLiveHr = remember(elapsedSec) {
        com.repforge.app.WearLiveHealthStore.current(context)?.optInt("heartRate", 0) ?: 0
    }
    val realHrBpm = if (wearLiveHr > 0) wearLiveHr else uiState.currentHrBpm
    val hrLabel = if (realHrBpm > 0) "♥ $realHrBpm bpm" else "♥ — bpm (nincs szenzor)"
    val caloriesLabel = "— kcal (edzésadat nem érhető el)"

    val display = OutdoorGpsDisplay(
        title = uiState.activeWorkoutTitleHu.ifBlank { "Kültéri futás" },
        status = when {
            !gpsActive -> tr(lang, "Beltéri időmérés • GPS kikapcsolva", "Indoor timer • GPS disabled", "Indoor-Zeitmessung • GPS deaktiviert", "Cronometru indoor • GPS dezactivat")
            !hasLocationPermission -> tr(lang, "Helymeghatározási engedély szükséges", "Location permission required", "Standortberechtigung erforderlich", "Permisiune de locație necesară")
            isStarted && isPaused -> tr(lang, "GPS szüneteltetve", "GPS paused", "GPS pausiert", "GPS în pauză")
            isStarted && hasServiceFix -> tr(
                lang,
                "Élő GPS-jel ($realGpsFixesCount pont • ±${gpsAccuracyMeters.toInt()} m)",
                "Live GPS signal ($realGpsFixesCount pts • ±${gpsAccuracyMeters.toInt()} m)",
                "Live-GPS-Signal ($realGpsFixesCount Pkt. • ±${gpsAccuracyMeters.toInt()} m)",
                "Semnal GPS live ($realGpsFixesCount pct. • ±${gpsAccuracyMeters.toInt()} m)"
            )
            isStarted -> tr(lang, "GPS-jelre vár…", "Waiting for GPS signal…", "Warte auf GPS-Signal…", "Se așteaptă semnalul GPS…")
            else -> tr(lang, "Indításra kész", "Ready to start", "Bereit zum Starten", "Pregătit de start")
        },
        gpsEnabled = gpsActive, started = isStarted, paused = isPaused,
        elapsed = formattedElapsed, distance = formattedDistance, pace = formattedPace,
        calories = "— kcal", heartRate = if (realHrBpm > 0) "♥ $realHrBpm bpm" else "♥ — bpm", pointCount = realGpsFixesCount,
        lapCount = lapCount, latestLap = lapSplits.lastOrNull().orEmpty(),
        error = listOf(finishErrorMsg, serviceErrorMsg).filter(String::isNotBlank).distinct().joinToString("\n"),
        language = uiState.language,
        manualDistanceInput = manualDistanceInput,
        manualDistanceError = manualDistanceError,
        hasManualDistance = manualDistanceMetersState != null && manualDistanceMetersState!! > 0f
    )
    val actions = OutdoorGpsActions(
        back = onBack, settings = onOpenSettings, selectActivity = onSelectOutdoorActivity,
        setGpsEnabled = { enabled ->
            gpsActive = enabled
            if (!enabled && isStarted) com.repforge.app.DistanceTrackingService.stop(context)
        },
        start = {
            serviceErrorMsg = ""
            if (gpsActive) {
                hasLocationPermission = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (!hasLocationPermission) permissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                else startNativeGpsTracking()
            } else startIndoorMeasurement()
        },
        pause = { pauseMeasurement() },
        lap = {
            val lapWord = tr(lang, "kör", "lap", "Runde", "tură")
            lapSplits = lapSplits + "${lapCount}. $lapWord: $formattedElapsed ($formattedDistance)"
            lapCount += 1
        },
        finish = {
            if (finishMeasurement()) {
                isFullscreenMapOpen = false
                onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY)
            }
        },
        expandMap = { isFullscreenMapOpen = true },
        onManualDistanceInputChange = { next ->
            manualDistanceInput = next
            if (manualDistanceError.isNotBlank()) manualDistanceError = ""
        },
        onSaveManualDistance = { applyManualDistanceInput(false) },
        onClearManualDistance = { clearManualDistance() }
    )
    val routeMap: @Composable (Modifier) -> Unit = { modifier ->
        val mapPadding = LocalOutdoorMapContentPadding.current
        NativeWorkoutRouteMapBox(modifier = modifier, interactiveControls = true,
            routeKeys = listOf(routeOwnerKey), refreshToken = realGpsFixesCount,
            showZoomControls = mapPadding.calculateTopPadding() > 0.dp,
            mapContentPadding = mapPadding)
    }
    // 2778: rest is optional on continuous distance workouts: a "Pihenő" chip shows the shared rest dock.
    var restShown by androidx.compose.runtime.saveable.rememberSaveable(uiState.activeWorkoutId) {
        mutableStateOf(com.repforge.app.StudioRestTimer.visible(uiState) && uiState.restTimerRunning)
    }
    var restLarge by remember { mutableStateOf(false) }
    if (restShown) StudioRestCountdown(uiState, onAdjustRestTimer)
    val restSlot: @Composable () -> Unit = {
        if (restShown) com.example.ui.components.StudioRestTimerDock(uiState, restLarge, onLarge = { restLarge = it },
            onAdjust = onAdjustRestTimer, onToggle = onToggleRestRunning, onSkip = onSkipRest, skipLabel = strings.skipAction)
        com.repforge.app.ui.theme.TrainPilotOutlinedButton(onClick = {
                // Showing loads the default rest (paused) so the dock has something to start.
                if (!restShown && uiState.restTimerRemainingSec == 0) onAdjustRestTimer(uiState.defaultRestSec)
                restShown = !restShown
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("outdoor_rest_toggle"),
            shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.Timer, null, tint = palette.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (restShown) tr(uiState.language, "Pihenő elrejtése", "Hide rest", "Pause ausblenden", "Ascunde pauza")
                else tr(uiState.language, "Pihenő", "Rest", "Pause", "Pauză"), color = palette.textPrimary)
        }
    }
    OutdoorGpsDashboard(display, strings, actions, restSlot = restSlot, mapContent = routeMap)
    if (isFullscreenMapOpen) {
        OutdoorGpsFullscreenDialog(
            state = display,
            strings = strings,
            actions = actions,
            onDismissRequest = { isFullscreenMapOpen = false },
            mapContent = routeMap
        )
    }
}

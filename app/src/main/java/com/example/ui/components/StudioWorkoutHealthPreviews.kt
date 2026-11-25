package com.example.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.repforge.app.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal val LocalWorkoutHealthPreviews = staticCompositionLocalOf<Map<String, JSONObject>> { emptyMap() }

/** Ephemeral UI preview only. No new sensor, timer, service, background job or saved daily value. */
internal object WorkoutLiveHealthPolicy {
    const val INTERVAL_MS = 120_000L
    fun sessionKey(frame: JSONObject?): String? = frame?.takeIf {
        it.optBoolean("active") && !it.optBoolean("finalizationPending") && !it.optBoolean("discardPending") &&
            !it.optBoolean("workoutPaused") && !com.repforge.shared.WorkoutOwnership193.pending(it) &&
            it.optString("workoutId").isNotBlank()
    }?.let {
        listOf(it.optString("workoutId"), it.optString("started"), it.optLong("generation").toString(),
            it.optLong("ownerEpoch193").toString(), it.optLong("pauseRevision").toString(),
            it.optJSONArray("activeIntervals")?.toString().orEmpty()).joinToString("|")
    }
    fun watchMetrics(frame: JSONObject?, packet: JSONObject?, node: String?, now: Long): Map<String, Double> {
        if (sessionKey(frame) == null || frame == null || packet == null || !WearLiveHealthStore.valid(packet)) return emptyMap()
        val receipt = PhoneMeasurementReconnect351.freshReceipt(packet, frame.optString("workoutId"),
            frame.optLong("ownerEpoch193", -1L), node, now) ?: return emptyMap()
        if (receipt.optString("state") != "active") return emptyMap()
        val data = receipt.optJSONObject("summary") ?: return emptyMap()
        if (data.optString("workoutId", frame.optString("workoutId")) != frame.optString("workoutId")) return emptyMap()
        val started = runCatching { java.time.Instant.parse(frame.getString("started")).toEpochMilli() }.getOrNull() ?: return emptyMap()
        if (receipt.optLong("updatedAt") < started) return emptyMap()
        val values = NativeWorkoutHealth.metrics(data, true).toMutableMap()
        // Health Services TOTAL_CALORIES is not active energy. Only expose an explicitly
        // named measured active value if a future sender actually supplies one.
        for (key in listOf("heartRate", "activeCalories")) {
            val value = (data.opt(key) as? Number)?.toDouble()
            if (value != null && value.isFinite() && (if (key == "heartRate") value > 0 else value >= 0))
                values[key] = value
        }
        return values
    }
}

@Composable
internal fun rememberWorkoutHealthPreviews(enabled: Boolean, workoutId: String, generation: Long,
    hcEnabled: Boolean, samsungEnabled: Boolean): Map<String, JSONObject> {
    val context = LocalContext.current
    val activity = remember(context) { context.healthActivity() }
    val owner = LocalLifecycleOwner.current
    val frame by ActiveWorkoutStore.snapshotFlow.collectAsState()
    val eligible = frame?.takeIf { it.optString("workoutId") == workoutId &&
        (generation <= 0 || it.optLong("generation") == generation) }
    val key = WorkoutLiveHealthPolicy.sessionKey(eligible)
    var previews by remember(key, enabled, hcEnabled, samsungEnabled) { mutableStateOf(emptyMap<String, JSONObject>()) }
    LaunchedEffect(owner, activity, key, enabled, hcEnabled, samsungEnabled) {
        if (!enabled || activity == null || key == null) return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                while (true) {
                    for ((provider, allowed) in listOf(HealthJournalStore.SAMSUNG to samsungEnabled, HealthJournalStore.HC to hcEnabled)) {
                        if (!allowed) continue
                        val expected = ActiveWorkoutStore.current(context) ?: break
                        if (WorkoutLiveHealthPolicy.sessionKey(expected) != key) break
                        try {
                            // Availability/permission IPC stays off main. Existing guarded
                            // read rechecks the actual session after acquiring its shared mutex.
                            val permitted = withContext(Dispatchers.IO) {
                                val feature = if (provider == HealthJournalStore.HC)
                                    NativePrivacyControls.HEALTH_CONNECT else NativePrivacyControls.SAMSUNG_HEALTH
                                NativePrivacyControls.enabled(context, feature) && NativeHealthBackupGate.available() &&
                                    when (provider) {
                                        HealthJournalStore.HC -> HealthConnectRepository.isSupported(context) &&
                                            HealthConnectRepository.currentlyGrantedReadKeys(context).isNotEmpty()
                                        else -> SamsungHealthRepository.isInstalled(context) && SamsungHealthRepository.isSdkAvailable()
                                    }
                            }
                            if (!permitted) { previews = previews - provider; continue }
                            val data = NativeLiveWorkoutHealth.read(context, activity, provider, expected)
                            if (WorkoutLiveHealthPolicy.sessionKey(ActiveWorkoutStore.current(context)) == key)
                                previews = previews + (provider to data)
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { previews = previews - provider }
                    }
                    delay(WorkoutLiveHealthPolicy.INTERVAL_MS)
                }
            } finally { previews = emptyMap() }
        }
    }
    return previews
}

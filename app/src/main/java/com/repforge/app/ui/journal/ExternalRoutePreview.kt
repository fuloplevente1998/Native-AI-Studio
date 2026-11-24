package com.repforge.app.ui.journal

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.health.connect.client.contracts.ExerciseRouteRequestContract
import com.repforge.app.*
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

/** One-time consent per view. Coordinates never enter Room, journal JSON, backups or logs. */
@Composable
internal fun ExternalRoutePreview(detail: WorkoutHistoryWithDetails, language: String) {
    val row = detail.workout
    val source = remember(row.rawJson) { runCatching { JSONObject(row.rawJson).optJSONObject("externalWorkout359") }.getOrNull() }
    if (source == null || source.optString("provider") != HealthJournalStore.HC ||
        source.optBoolean("sourceDeleted") || source.optBoolean("windowChanged")) return
    val recordId = source.optString("id")
    if (recordId.isBlank()) return
    key(row.id, row.rawJson) {
        val context = LocalContext.current.applicationContext
        val scope = rememberCoroutineScope()
        var ticket by remember { mutableStateOf<NativeDataReplacement.Continuation?>(null) }
        var route by remember { mutableStateOf<RouteCoordinatesUiModel?>(null) }
        var busy by remember { mutableStateOf(false) }
        var unavailable by remember { mutableStateOf(false) }
        fun tr(hu: String, en: String, de: String, ro: String) = when (language.lowercase(Locale.ROOT).take(2)) {
            "hu" -> hu; "de" -> de; "ro" -> ro; else -> en
        }
        suspend fun verify(current: NativeDataReplacement.Continuation) = withContext(Dispatchers.IO) {
            current.requireCurrent(context)
            check(NativePrivacyControls.enabled(context, NativePrivacyControls.GPS) &&
                NativePrivacyControls.enabled(context, NativePrivacyControls.HEALTH_CONNECT) &&
                "READ_EXERCISE" in HealthConnectRepository.currentlyGrantedReadKeys(context))
            val latest = TrainPilotDatabase.getInstance(context).workoutHistoryDao().getWorkoutWithDetailsById(row.id)
            check(latest?.workout?.rawJson == row.rawJson)
            val journal = HealthJournalStore.get(context)
            val identity = ExternalWorkoutIdentity(HealthJournalStore.HC, source.getString("source"), recordId)
            val currentRecord = journal.externalWorkoutRecord(identity, journal.generation())
            check(currentRecord != null && currentRecord.modifiedMs == source.getLong("lastModifiedMs") &&
                currentRecord.startMs == row.startedEpochMs && currentRecord.endMs == row.finishedEpochMs)
            current.requireCurrent(context)
        }
        val launcher = rememberLauncherForActivityResult(ExerciseRouteRequestContract()) { received ->
            val request = ticket
            ticket = null
            scope.launch {
                try {
                    if (received == null || request == null) { unavailable = true; return@launch }
                    verify(request)
                    val points = withContext(Dispatchers.Default) {
                        received.route.asSequence().filter {
                            it.time.toEpochMilli() in row.startedEpochMs..row.finishedEpochMs &&
                                it.latitude.isFinite() && it.longitude.isFinite() &&
                                it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0
                        }.sortedBy { it.time }.map { it.latitude to it.longitude }.take(50_001).toList()
                    }
                    if (points.isEmpty() || points.size > 50_000) unavailable = true
                    else route = RouteCoordinatesUiModel(row.id, listOf(points), points.size, 0.0, null)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { unavailable = true }
                finally { busy = false }
            }
        }
        Column(Modifier.fillMaxWidth()) {
            TrainPilotOutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("external_route_request"), onClick = {
                busy = true; unavailable = false
                scope.launch {
                    try {
                        val current = withContext(Dispatchers.IO) { NativeDataReplacement.continuationForCurrentData(context) }
                        verify(current)
                        ticket = current
                        launcher.launch(recordId)
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { ticket = null; unavailable = true; busy = false }
                }
            }) { Text(tr("GPS-útvonal megtekintése", "View GPS route", "GPS-Route ansehen", "Vezi traseul GPS")) }
            if (unavailable) Text(tr("Nincs megosztott útvonal, vagy a hozzáférés nem engedélyezett.",
                "No shared route or access not granted.", "Keine freigegebene Route oder Zugriff nicht erlaubt.",
                "Nu există traseu partajat sau accesul nu este acordat."))
        }
        route?.let { WorkoutRouteMapDialog(it, { route = null }, language) }
    }
}

package com.example.ui.components

import com.example.ui.theme.liquidDropMaterial
import com.example.ui.theme.GlassMaterialRole
import androidx.compose.foundation.shape.RoundedCornerShape

import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeLiveWorkoutHealth
import com.repforge.app.NativeWorkoutHealth
import com.repforge.app.NativeWorkoutHealthSource
import com.repforge.app.SamsungHealthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

internal val LocalRequestWorkoutHealthPermission = staticCompositionLocalOf<(String) -> Unit> { {} }

internal val LocalOpenWorkoutHealth = staticCompositionLocalOf<() -> Unit> { {} }

@Composable internal fun StudioWorkoutHealthAction() {
    val open = LocalOpenWorkoutHealth.current
    val palette = LocalTrainPilotPalette.current
    val lang = com.example.localization.LocalTrainPilotStrings.current.language
    fun word(hu: String, en: String, de: String, ro: String) = when (lang) {
        com.example.model.AppLanguage.HU -> hu
        com.example.model.AppLanguage.EN -> en
        com.example.model.AppLanguage.DE -> de
        com.example.model.AppLanguage.RO -> ro
    }
    TrainPilotIconButton(onClick = open, modifier = Modifier.size(48.dp)
        .liquidDropMaterial(palette, RoundedCornerShape(14.dp), GlassMaterialRole.Button, live = false)
        .testTag("workout_read_health")) {
        Icon(Icons.Default.FavoriteBorder, contentDescription = word(
            "Edzés egészségadatainak lekérése",
            "Read workout health data",
            "Trainings-Gesundheitsdaten abrufen",
            "Preia datele de sănătate ale antrenamentului"
        ), tint = palette.primary)
    }
}

@Composable internal fun StudioLiveWorkoutHealthDialog(showWatch: Boolean, samsungEnabled: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.healthActivity() }
    val scope = rememberCoroutineScope()
    val requestPermission = LocalRequestWorkoutHealthPermission.current
    var provider by remember(samsungEnabled) { mutableStateOf(HealthJournalStore.HC) }
    val previews = LocalWorkoutHealthPreviews.current
    val currentFrame by com.repforge.app.ActiveWorkoutStore.snapshotFlow.collectAsState()
    val frameKey = WorkoutLiveHealthPolicy.sessionKey(currentFrame)
    var result by remember(frameKey) { mutableStateOf<JSONObject?>(null) }
    val liveWear by com.repforge.app.WearLiveHealthStore.liveHealthFlow.collectAsState()
    val expectedNode by com.repforge.app.WearLiveHealthStore.expectedNode351.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(frameKey) {
        while (true) { now = System.currentTimeMillis(); kotlinx.coroutines.delay(1000) }
    }
    val watch = if (showWatch) WorkoutLiveHealthPolicy.watchMetrics(currentFrame, liveWear, expectedNode, now)
        else emptyMap()
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val palette = LocalTrainPilotPalette.current
    val lang = com.example.localization.LocalTrainPilotStrings.current.language
    val langCode = lang.name.lowercase()
    fun word(hu: String, en: String, de: String, ro: String) = when (lang) {
        com.example.model.AppLanguage.HU -> hu
        com.example.model.AppLanguage.EN -> en
        com.example.model.AppLanguage.DE -> de
        com.example.model.AppLanguage.RO -> ro
    }
    com.example.ui.components.TrainPilotFormSheet(scrollText = false, onDismissRequest = { if (!busy) onDismiss() }, title = {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(word("Edzés egészségadatai", "Workout health data", "Trainings-Gesundheitsdaten", "Date de sănătate antrenament"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TrainPilotRedCloseButton(onDismiss)
        }
    }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (source in com.repforge.app.NativeHealthProviderBatch.providers.filter {
                    it == HealthJournalStore.HC || samsungEnabled
                }) {
                    com.example.ui.components.TrainPilotFilterChip(selected = provider == source, enabled = !busy,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        onClick = { provider = source; result = null; message = "" },
                        label = { Text(NativeWorkoutHealthSource.label(source)) })
                }
            }
            TrainPilotOutlinedButton(enabled = !busy, onClick = { requestPermission(provider) }) {
                Text("${NativeWorkoutHealthSource.label(provider)} • ${word("engedélyek", "permissions", "Berechtigungen", "permisiuni")}")
            }
            if (watch.isNotEmpty()) {
                Text("TrainPilot Wear OS", style = MaterialTheme.typography.titleSmall, color = palette.primary)
                watch["heartRate"]?.let { value ->
                    Text("${word("Aktuális pulzus", "Current heart rate", "Aktueller Puls", "Puls curent")}: ${value.toInt()} bpm")
                }
                com.repforge.app.ui.journal.JournalHealthMetrics(watch - "heartRate", "live_wear",
                    com.repforge.app.ui.text.nativeResources(context, langCode),
                    java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag(langCode)))
                HorizontalDivider()
            }
            listOfNotNull(result, previews[provider]).maxByOrNull { it.optString("readAt") }?.let { data ->
                Text(NativeWorkoutHealthSource.label(provider), color = palette.primary, style = MaterialTheme.typography.titleSmall)
                Text("${word("Időablak", "Time window", "Zeitfenster", "Interval orar")}: ${data.optString("start")} – ${data.optString("end")}")
                val metrics = NativeWorkoutHealth.metrics(data, false) - setOf("activeCalories", "workoutCalories")
                com.repforge.app.ui.journal.JournalHealthMetrics(metrics, "live", com.repforge.app.ui.text.nativeResources(context, langCode),
                    java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag(langCode)))
                if (metrics.isEmpty()) Text(word(
                    "Ehhez az aktív időablakhoz nincs elérhető mérési adat.",
                    "No measurement data available for this active time window.",
                    "Für dieses aktive Zeitfenster sind keine Messdaten verfügbar.",
                    "Nu există date măsurate disponibile pentru acest interval activ."
                ))
                if (data.optBoolean("partial")) Text(word(
                    "Az adatolvasás részleges.",
                    "Data read is partial.",
                    "Datenabruf ist unvollständig.",
                    "Citirea datelor este parțială."
                ), color = palette.warning)
            }
            if (message.isNotBlank()) Text(message, modifier = Modifier.testTag("live_health_result"))
        }
    }, confirmButton = {
        Button(enabled = !busy && activity != null, onClick = {
            busy = true; result = null; message = word("Lekérdezés…", "Querying…", "Abfrage läuft…", "Interogare…")
            scope.launch {
                try {
                    result = NativeLiveWorkoutHealth.read(context, activity!!, provider, currentFrame)
                    message = "${NativeWorkoutHealthSource.label(provider)}: ${word("lekérdezés befejezve", "query completed", "Abfrage abgeschlossen", "interogare finalizată")}"
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    message = "${NativeWorkoutHealthSource.label(provider)}: ${if (provider == HealthJournalStore.SAMSUNG) SamsungHealthRepository.errorCode(error) else ""} • ${error.message.orEmpty().take(180)}"
                } finally { busy = false }
            }
        }) { Text(if (busy) word("Lekérdezés…", "Querying…", "Abfrage läuft…", "Interogare…") else word("Adatok lekérése", "Fetch data", "Daten abrufen", "Preia datele")) }
    })
}

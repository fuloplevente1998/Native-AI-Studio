package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
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
    IconButton(onClick = open, modifier = Modifier.size(48.dp).testTag("workout_read_health")) {
        Icon(Icons.Default.FavoriteBorder, contentDescription = "Edzés egészségadatainak lekérése", tint = palette.primary)
    }
}

@Composable internal fun StudioLiveWorkoutHealthDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.healthActivity() }
    val scope = rememberCoroutineScope()
    val requestPermission = LocalRequestWorkoutHealthPermission.current
    var provider by remember { mutableStateOf(HealthJournalStore.HC) }
    var result by remember { mutableStateOf<JSONObject?>(null) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val palette = LocalTrainPilotPalette.current
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Edzés egészségadatai", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TrainPilotRedCloseButton(onDismiss)
        }
    }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Csak lekérés: az edzésvezérlés és a mérések állapota nem változik.")
            for (source in listOf(HealthJournalStore.SAMSUNG, HealthJournalStore.HC)) {
                FilterChip(selected = provider == source, enabled = !busy,
                    onClick = { provider = source; result = null; message = "" }, label = { Text(NativeWorkoutHealthSource.label(source)) })
            }
            OutlinedButton(enabled = !busy, onClick = { requestPermission(provider) }) {
                Text("${NativeWorkoutHealthSource.label(provider)} • engedélyek")
            }
            result?.let { data ->
                Text("Időablak: ${data.optString("start")} – ${data.optString("end")}")
                val metrics = NativeWorkoutHealth.metrics(data, false)
                com.repforge.app.ui.journal.JournalHealthMetrics(metrics, "live", com.repforge.app.ui.text.nativeResources(context, "hu"),
                    java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("hu")))
                if (metrics.isEmpty()) Text("Ehhez az aktív időablakhoz nincs elérhető mérési adat.")
                if (data.optBoolean("partial")) Text("Az adatolvasás részleges.", color = palette.warning)
            }
            if (message.isNotBlank()) Text(message, modifier = Modifier.testTag("live_health_result"))
        }
    }, confirmButton = {
        Button(enabled = !busy && activity != null, onClick = {
            busy = true; result = null; message = "Lekérdezés…"
            scope.launch {
                try {
                    result = NativeLiveWorkoutHealth.read(context, activity!!, provider)
                    message = "${NativeWorkoutHealthSource.label(provider)}: lekérdezés befejezve"
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    message = "${NativeWorkoutHealthSource.label(provider)}: ${if (provider == HealthJournalStore.SAMSUNG) SamsungHealthRepository.errorCode(error) else ""} • ${error.message.orEmpty().take(180)}"
                } finally { busy = false }
            }
        }) { Text(if (busy) "Lekérdezés…" else "Adatok lekérése") }
    })
}

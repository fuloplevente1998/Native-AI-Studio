package com.repforge.app.wear

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal data class WearDailyUi(val data: WearDailyHealth, val enabled: Boolean, val registered: Boolean?, val toggle: () -> Unit)

@Composable
internal fun rememberWearDailyHealth(): WearDailyUi {
    val context = LocalContext.current.applicationContext
    val owner = LocalView.current.findViewTreeLifecycleOwner()
    var enabled by remember { mutableStateOf(WearDailyHealthStore.enabled(context)) }
    var resumed by remember(owner) { mutableStateOf(owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != false) }
    var registered by remember { mutableStateOf<Boolean?>(null) }
    var data by remember { mutableStateOf(WearDailyHealthStore.read(context)) }
    var retry by remember { mutableIntStateOf(0) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { WearDailyHealthStore.enable(context, true); enabled = true; retry++ }
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { resumed = true; retry++ }
            if (event == Lifecycle.Event.ON_PAUSE) resumed = false
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    LaunchedEffect(enabled, retry, resumed) {
        if (resumed) {
            registered = null
            registered = withContext(Dispatchers.IO) { runCatching { WearDailyHealthStore.register(context) }.getOrDefault(false) }
        }
    }
    LaunchedEffect(enabled, resumed) {
        if (!enabled) data = WearDailyHealth()
        while (resumed && enabled) { data = WearDailyHealthStore.read(context); delay(2000L) }
    }
    return WearDailyUi(data, enabled, registered) {
        if (enabled) { WearDailyHealthStore.enable(context, false); enabled = false; data = WearDailyHealth() }
        else if (WearDailyHealthStore.permitted(context)) { WearDailyHealthStore.enable(context, true); enabled = true }
        else request.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }
}

private fun dailyText(hu: String, en: String, de: String, ro: String) = when (java.util.Locale.getDefault().language) {
    "hu" -> hu; "de" -> de; "ro" -> ro; else -> en
}

@Composable
internal fun WearDailyHealthScreen(daily: WearDailyUi, goal: Int?, health: JSONObject?, active: WearWorkout?, onDetails: () -> Unit, onBack: () -> Unit) {
    val pulse = wearHomeLivePulse(health, active?.workoutId, true, System.currentTimeMillis())
    RoundList(dailyText("Egészség", "Health", "Gesundheit", "Sănătate")) {
        centerItem { Title(daily.data.steps?.toString() ?: "–", 32) }
        centerItem { Label(dailyText("Lépés · cél", "Steps · goal", "Schritte · Ziel", "Pași · obiectiv") + " " + (goal?.toString() ?: "–")) }
        centerItem { Label("♥ " + (pulse?.let { "$it bpm" } ?: "–")) }
        centerItem { Label((daily.data.calories?.let(::trimWearNumber) ?: "–") + " kcal · " + dailyText("összes", "total", "gesamt", "total")) }
        centerItem { Label(dailyText("Alvás", "Sleep", "Schlaf", "Somn") + " –") }
        centerItem { Label(dailyText("Órás napi adatok", "Watch daily data", "Tagesdaten der Uhr", "Date zilnice de pe ceas"), size = 10) }
        if (daily.enabled && daily.registered == false) centerItem {
            Label(dailyText("Nem elérhető · ellenőrizd az engedélyt", "Unavailable · check permission", "Nicht verfügbar · Berechtigung prüfen", "Indisponibil · verifică permisiunea"), lines = 2)
        }
        centerItem { Pill(if (daily.enabled) dailyText("Napi mérés kikapcsolása", "Disable daily data", "Tagesdaten deaktivieren", "Dezactivează datele zilnice") else dailyText("Napi mérés engedélyezése", "Enable daily data", "Tagesdaten aktivieren", "Activează datele zilnice"), onClick = daily.toggle) }
        centerItem { Pill(WearMenu216.text("metrics"), tone = Secondary, onClick = onDetails) }
        centerItem { Pill(WearMenu216.text("back"), tone = Secondary, onClick = onBack) }
    }
}

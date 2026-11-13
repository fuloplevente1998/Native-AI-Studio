package com.repforge.app.ui.onboarding

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.repforge.app.NativeDataReplacement
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.data.store.TrainPilotUserSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The production Studio root shares the existing first-run policy and persisted profile. */
@Composable
internal fun NativeFirstLaunchGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { TrainPilotSettingsStore(context) }
    val loaded by store.settingsFlow.collectAsState(initial = null)
    val settings = loaded
    if (settings == null || !settings.migrationV1371Complete) {
        androidx.compose.material3.CircularProgressIndicator()
        return
    }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    fun write(change: suspend (NativeDataReplacement.Continuation) -> Boolean) {
        if (saving) return
        val ticket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }
            .getOrElse { failed = true; return }
        saving = true
        failed = false
        scope.launch {
            try { failed = !withContext(Dispatchers.IO) { change(ticket) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failed = true }
            finally { saving = false }
        }
    }
    val destination = NativeOnboardingPolicy.destination(settings.onboarding128)
    if (destination == NativeOnboardingPolicy.Destination.HOME) content()
    else NativeOnboardingScreen(settings = settings, destination = destination,
        saving = saving, failed = failed,
        onBegin = { write { store.transitionOnboarding(settings.onboarding128, NativeOnboardingPolicy.IN_PROGRESS, it) } },
        onSkip = { write { store.transitionOnboarding(settings.onboarding128, NativeOnboardingPolicy.SKIPPED, it) } },
        onProfileSave = { fields -> write { store.completeOnboarding(settings.profileJson, fields, it) } },
        onBack = {})
}

package com.repforge.app.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.StudioHealthPermissionRouting
import com.example.ui.components.StudioPlatformAccess
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotViewModel
import com.example.ui.viewmodel.updatePrivacyChoice
import com.repforge.app.HealthConnectRepository
import com.repforge.app.NativePrivacyControls
import com.repforge.app.SamsungHealthRepository
import com.repforge.app.ui.theme.TrainPilotButton
import com.repforge.app.ui.theme.TrainPilotTextButton
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.workout.NativeGpsPermissionRecovery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PERMISSIONS_SETUP = "trainpilot-first-run-permissions"

internal fun firstRunPermissionsPending(context: Context): Boolean =
    !context.getSharedPreferences(PERMISSIONS_SETUP, Context.MODE_PRIVATE).getBoolean("completed", false)

private fun localized(language: String, hu: String, en: String, de: String, ro: String): String =
    when (language.lowercase().take(2)) {
        "en" -> en
        "de" -> de
        "ro" -> ro
        else -> hu
    }

internal data class FirstRunPermissionOption(val key: String, val title: String)

@Composable
internal fun NativeOnboardingPermissions(
    language: String,
    viewModel: TrainPilotViewModel,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val activity = NativeGpsPermissionRecovery.findActivity(context)
    val scope = rememberCoroutineScope()
    val healthConnect = NativePrivacyControls.HEALTH_CONNECT
    val samsung = NativePrivacyControls.SAMSUNG_HEALTH
    val notifications = NativePrivacyControls.NOTIFICATIONS
    val readPermissions = remember(context) {
        StudioPlatformAccess.healthPermissions(context)
            .filterNot { it == HealthConnectRepository.WRITE_EXERCISE_PERMISSION }
    }
    val options = remember(context, language, readPermissions) {
        buildList {
            if (readPermissions.isNotEmpty()) add(FirstRunPermissionOption(healthConnect, "Health Connect"))
            if (SamsungHealthRepository.isInstalled(context) && SamsungHealthRepository.isSdkAvailable())
                add(FirstRunPermissionOption(samsung, "Samsung Health"))
            add(FirstRunPermissionOption(notifications, localized(language, "Értesítések",
                "Notifications", "Benachrichtigungen", "Notificări")))
        }
    }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var permissionResult by remember { mutableStateOf<CompletableDeferred<Unit>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionResult?.complete(Unit)
    }
    val legacyHealthLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { _ -> permissionResult?.complete(Unit) }

    suspend fun request(permissions: List<String>, healthConnect: Boolean = false) {
        if (permissions.isEmpty()) return
        val result = CompletableDeferred<Unit>()
        permissionResult = result
        try {
            if (healthConnect && Build.VERSION.SDK_INT < 34)
                legacyHealthLauncher.launch(permissions.toSet())
            else launcher.launch(permissions.toTypedArray())
            result.await()
        } finally {
            permissionResult = null
        }
    }

    fun finish(selected: Set<String>) {
        if (busy) return
        busy = true
        error = false
        scope.launch {
            try {
                val features = listOf(healthConnect, samsung, notifications)
                withContext(Dispatchers.IO) {
                    features.forEach { feature -> NativePrivacyControls.set(context, feature, feature in selected) }
                }
                features.forEach { feature -> viewModel.updatePrivacyChoice(feature, feature in selected) }
                for (feature in features.filter { it in selected }) {
                    lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
                    when (feature) {
                        healthConnect -> {
                            val granted = HealthConnectRepository.currentlyGrantedReadKeys(context)
                            request(readPermissions.filter { it.substringAfterLast('.') !in granted },
                                healthConnect = true)
                        }
                        samsung -> activity?.let { SamsungHealthRepository.requestReadPermissions(it).getOrThrow() }
                        notifications -> if (Build.VERSION.SDK_INT >= 33 &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                            request(listOf(Manifest.permission.POST_NOTIFICATIONS))
                    }
                }
                withContext(Dispatchers.IO) {
                    check(context.getSharedPreferences(PERMISSIONS_SETUP, Context.MODE_PRIVATE)
                        .edit().putBoolean("completed", true).commit())
                }
                onFinished()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = true
            } finally {
                busy = false
            }
        }
    }

    FirstRunPermissionsScreen(language, options, busy, error, onContinue = ::finish, onSkip = { finish(emptySet()) })
}

@Composable
internal fun FirstRunPermissionsScreen(
    language: String,
    options: List<FirstRunPermissionOption>,
    busy: Boolean,
    error: Boolean,
    onContinue: (Set<String>) -> Unit,
    onSkip: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var selected by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    BackHandler { if (!busy) onSkip() }
    Column(
        Modifier.fillMaxSize().background(palette.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(20.dp)
            .testTag("first_run_permissions"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = palette.primary)
            Text("TrainPilot", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = palette.primary)
        }
        Text(localized(language, "Engedélyek beállítása", "Set up permissions",
            "Berechtigungen einrichten", "Configurează permisiunile"),
            style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary,
            fontWeight = FontWeight.Bold)
        Text(localized(language, "Válaszd ki, mit szeretnél használni.", "Choose what you want to use.",
            "Wähle aus, was du verwenden möchtest.", "Alege ce vrei să folosești."),
            color = palette.textSecondary)
        val shape = RoundedCornerShape(14.dp)
        options.forEach { option ->
            val checked = option.key in selected
            Surface(shape = shape, color = palette.surfaceCard,
                modifier = Modifier.fillMaxWidth().trainPilotBorder(1.dp, palette.cardBorder, shape)) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .toggleable(checked, enabled = !busy, role = Role.Checkbox) { enabled ->
                            selected = ArrayList(if (enabled) selected + option.key else selected - option.key)
                        }.testTag("first_run_permission_${option.key}").padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(option.title, modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                    Checkbox(checked, onCheckedChange = null, enabled = !busy)
                }
            }
        }
        if (error) Text(localized(language, "A beállítás megszakadt. Újrapróbálhatod vagy kihagyhatod.",
            "Setup was interrupted. You can retry or skip.", "Die Einrichtung wurde unterbrochen. Erneut versuchen oder überspringen.",
            "Configurarea a fost întreruptă. Poți reîncerca sau omite."), color = palette.danger)
        TrainPilotButton(onClick = { onContinue(selected.toSet()) }, enabled = !busy && selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("first_run_permissions_continue"),
            colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)) {
            Text(localized(language, "Tovább", "Continue", "Weiter", "Continuă"))
        }
        TrainPilotTextButton(onClick = onSkip, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("first_run_permissions_skip")) {
            Text(localized(language, "Kihagyás", "Skip", "Überspringen", "Omite"), color = palette.primary)
        }
    }
}

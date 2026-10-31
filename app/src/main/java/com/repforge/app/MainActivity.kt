package com.repforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.navigation.TrainPilotAppScaffold
import com.repforge.app.ui.theme.TrainPilotTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.activity.viewModels
import com.example.TrainPilotAppRoot
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.viewmodel.TrainPilotViewModel

class MainActivity : ComponentActivity() {
    private lateinit var startupScreen: StartupScreen
    private lateinit var settingsStore: TrainPilotSettingsStore
    private lateinit var database: TrainPilotDatabase
    private lateinit var migrator: LegacyWebViewMigrator
    private val viewModel: TrainPilotViewModel by viewModels()
    private var recoveryNeeded by mutableStateOf(false)
    private var recoveryBusy by mutableStateOf(false)
    private var recoveryError by mutableStateOf(false)
    private var migrationReady by mutableStateOf(false)
    private var migrationFailure by mutableStateOf(false)
    private var migrationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        startupScreen = StartupScreen.install(this)
        super.onCreate(savedInstanceState)
        // All native palettes use dark surfaces, independently of Android's day/night mode.
        // Auto system-bar styling otherwise puts dark clock/icons on the dark app background.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        settingsStore = TrainPilotSettingsStore(this)
        database = TrainPilotDatabase.getInstance(this)
        migrator = LegacyWebViewMigrator(this, database, settingsStore)

        viewModel.bindNativeEnvironment(
            context = applicationContext,
            settingsStore = settingsStore,
            database = database,
            scope = lifecycleScope
        )

        recoveryNeeded = NativeDataReplacement.blocked(this) || NativeReplacementJournal(this).exists() || PhoneWorkoutExecution.store(this).restoreBlocked()
        startMigration()

        setContent {
            val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.repforge.app.data.store.TrainPilotUserSettings()
            )

            LaunchedEffect(settings) {
                viewModel.syncNativeSettings(
                    languageCode = settings.language,
                    themeKey = settings.themeKey,
                    restSec = settings.restSeconds,
                    sound = settings.soundEnabled,
                    vibration = settings.vibrationEnabled
                )
            }

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val currentPalette = TrainPilotPalettes.getById(uiState.selectedPaletteId)

            com.example.ui.theme.TrainPilotTheme(
                palette = currentPalette,
                fontScaleMultiplier = uiState.fontScaleMultiplier
            ) {
                if (recoveryNeeded) {
                    LaunchedEffect(Unit) { startupScreen.markNativeReady() }
                    com.repforge.app.ui.settings.ReplacementRecoveryPanel(
                        settings.language,
                        recoveryBusy,
                        recoveryError,
                        onResume = { recoverReplacement() },
                        onJson = { recoverReplacement(it, false) },
                        onZip = { recoverReplacement(it, true) }
                    )
                } else if (migrationReady) {
                    LaunchedEffect(Unit) {
                        startupScreen.markNativeReady()
                    }
                    TrainPilotAppRoot(viewModel = viewModel)
                } else if (migrationFailure) {
                    LaunchedEffect(Unit) {
                        startupScreen.markNativeReady()
                    }
                    MigrationFailurePanel(
                        language = settings.language,
                        onRetry = { startMigration() }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = when (settings.language) {
                                    "en" -> "Reading legacy data…"
                                    "de" -> "Altdaten werden geladen…"
                                    "ro" -> "Se citesc datele anterioare…"
                                    else -> "Korábbi adatok betöltése…"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
        startupScreen.attach()
    }

    private fun startMigration() {
        if (recoveryNeeded || migrationJob?.isActive == true || migrationReady) return
        migrationFailure = false
        migrationJob = lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { migrator.migrateIfNeeded() }
                viewModel.bindNativeEnvironment(
                    context = applicationContext,
                    settingsStore = settingsStore,
                    database = database,
                    scope = lifecycleScope
                )
                migrationReady = true
                withContext(Dispatchers.IO) {
                    try { HealthBackgroundService.resume(this@MainActivity) } catch (_: Throwable) {}
                    try { HealthBackgroundJob.schedule(this@MainActivity) } catch (_: Throwable) {}
                    try { WearSyncRepository.publishWatchHomeSnapshot(this@MainActivity) } catch (_: Throwable) {}
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (e: Exception) {
                android.util.Log.e("MainActivity", "Startup migration failed", e)
                migrationFailure = true
            }
        }
    }

    private fun recoverReplacement(uri: android.net.Uri? = null, zip: Boolean = false) {
        if (recoveryBusy) return
        recoveryBusy = true
        lifecycleScope.launch {
            try {
                val manager = BackupRestoreManager(this@MainActivity)
                val result = when { uri == null -> manager.resumeInterruptedReplacement()
                    zip -> manager.importZipArchiveFromUri(uri, recoverInterrupted = true)
                    else -> manager.importJsonBackupFromUri(uri, recoverInterrupted = true) }
                recoveryError = result.isFailure
                if (result.isSuccess) { recoveryNeeded = false; startMigration() }
            } finally { recoveryBusy = false }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!migrationReady) return
        HealthBackgroundService.stop(this)
        lifecycleScope.launch(Dispatchers.IO) {
            try { HealthJournalStore.get(this@MainActivity).retireWatchUi() } catch (_: Exception) {}
            try { HealthBackgroundJob.schedule(this@MainActivity) } catch (_: Exception) {}
            WearSyncRepository.publishWatchHomeSnapshot(this@MainActivity)
        }
    }

    override fun onDestroy() {
        startupScreen.destroy()
        super.onDestroy()
    }
}

@Composable
fun MigrationFailurePanel(
    language: String,
    onRetry: () -> Unit
) {
    val title = when (language) {
        "en" -> "Migration Incomplete"
        "de" -> "Datenmigration fehlgeschlagen"
        "ro" -> "Migrare incompletă"
        else -> "Adatmigrációs hiba"
    }
    val message = when (language) {
        "en" -> "Unable to safely read your legacy workout data. Your records are intact, but migration could not finish. Please retry."
        "de" -> "Ihre bisherigen Trainingsdaten konnten nicht sicher geladen werden. Die Daten sind intakt. Bitte versuchen Sie es erneut."
        "ro" -> "Nu s-au putut citi în siguranță datele anterioare de antrenament. Înregistrările sunt intacte. Vă rugăm să reîncercați."
        else -> "Nem sikerült a korábbi edzésadatok biztonságos beolvasása. Az adatok nem vesztek el, de a folyamat megszakadt. Kérjük, próbáld újra."
    }
    val retryLabel = when (language) {
        "en" -> "Retry Migration"
        "de" -> "Erneut versuchen"
        "ro" -> "Reîncearcă"
        else -> "Újrapróbálás"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("migration_failure_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("migration_retry_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = retryLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

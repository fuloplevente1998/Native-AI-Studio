package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import java.io.File
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.repforge.app.ui.settings.SettingsUiState
import com.repforge.app.ui.settings.SettingsViewModel
import com.repforge.app.GoogleSyncRepository
import com.repforge.app.GoogleSyncUiState
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
import com.example.localization.LocalTrainPilotStrings
import com.example.model.AppLanguage
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.components.TrainPilotScreenSheet
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.LocalBackupStatus
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ui.settings.GoogleActionFeedback
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class BackupCompactViewportRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun resultsAreVisibleInTheOpenSheetAndDriveShowsWhyItIsUnavailable() {
        var state by mutableStateOf(TrainPilotUiState(language = AppLanguage.EN, selectedBackupFormat = "ZIP"))
        compose.setContent {
            val strings = TrainPilotStrings.forLanguage(AppLanguage.EN)
            CompositionLocalProvider(LocalTrainPilotStrings provides strings) { TrainPilotTheme {
                TrainPilotScreenSheet(onDismiss = {}, testTag = "backup_test_sheet", fillHeight = true) {
                    BackupRestoreScreen(state, strings, onBack = {}, onOpenSettings = {},
                        onSelectFormat = { state = state.copy(selectedBackupFormat = it) },
                        onToggleIncludeHealth = {}, onShowBanner = {})
                }
            } }
        }
        compose.runOnIdle {
            state = state.copy(localBackupStatus = LocalBackupStatus("ZIP backup saved"))
        }
        compose.onNodeWithTag("backup_operation_status").assertIsDisplayed()
        compose.onNodeWithText("ZIP backup saved").assertIsDisplayed()
        compose.runOnIdle {
            state = state.copy(localBackupStatus = LocalBackupStatus("File could not be restored", failed = true))
        }
        compose.onNodeWithText("File could not be restored").assertIsDisplayed()
        compose.onNodeWithText("ZIP backup saved").assertDoesNotExist()

        val settings = ViewModelProvider(compose.activity)[SettingsViewModel::class.java]
        val settingsFlow = SettingsViewModel::class.java.getDeclaredField("_uiState").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val settingsState = settingsFlow.get(settings) as MutableStateFlow<SettingsUiState>
        compose.runOnIdle {
            state = state.copy(selectedBackupFormat = "DRIVE", localBackupStatus = null, isWorkoutSessionActive = true)
            settingsState.value = settingsState.value.copy(googleSyncState = GoogleSyncUiState(connectedEmail = "fixture@example.test"))
        }
        compose.onNodeWithText("Finish the active workout before syncing Drive.").assertIsDisplayed()
        compose.onNodeWithTag("backup_export_btn").assertIsNotEnabled()
        compose.runOnIdle {
            state = state.copy(isWorkoutSessionActive = false)
            settingsState.value = settingsState.value.copy(googleActionFeedback = GoogleActionFeedback("drive", "Drive sync completed.", false))
        }
        compose.onNodeWithTag("backup_export_btn").assertIsEnabled()
        compose.onNodeWithText("Drive sync completed.").assertIsDisplayed()
    }

    @Test fun compactBackupCardKeepsItsExpandableStatusWithoutAQuickSyncIcon() {
        val settings = ViewModelProvider(compose.activity)[SettingsViewModel::class.java]
        val field = SettingsViewModel::class.java.getDeclaredField("_uiState").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(settings) as MutableStateFlow<SettingsUiState>
        compose.setContent {
            CompositionLocalProvider(LocalTrainPilotStrings provides TrainPilotStrings.forLanguage(AppLanguage.HU)) {
                TrainPilotTheme {
                    com.example.ui.screens.StudioGoogleCloudSyncCard(onShowBanner = {},
                        settingsViewModel = settings, compact = true)
                }
            }
        }
        compose.runOnIdle {
            flow.value = flow.value.copy(googleSyncState = GoogleSyncUiState(connectedEmail = "fixture@example.test"))
        }
        compose.onNodeWithTag("backup_drive_quick_sync").assertDoesNotExist()
        compose.onNodeWithTag("settings_google_quick_sync").assertDoesNotExist()
        compose.onNodeWithTag("settings_google_expand").performClick()
        compose.onNodeWithTag("settings_google_expanded").assertExists()
    }

    @Test fun settingsCloudShortcutSharesTheFullSyncActionWithoutExpanding() {
        val settings = ViewModelProvider(compose.activity)[SettingsViewModel::class.java]
        val field = SettingsViewModel::class.java.getDeclaredField("_uiState").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(settings) as MutableStateFlow<SettingsUiState>
        var cloudCalls = 0
        compose.setContent {
            CompositionLocalProvider(LocalTrainPilotStrings provides TrainPilotStrings.forLanguage(AppLanguage.HU)) {
                TrainPilotTheme {
                    com.example.ui.screens.StudioGoogleCloudSyncCard(onShowBanner = {}, settingsViewModel = settings,
                        onSyncAllCloud = { cloudCalls++ })
                }
            }
        }
        val quick = compose.onNodeWithTag("settings_google_quick_sync")
        quick.assertDoesNotExist()
        compose.runOnIdle {
            flow.value = flow.value.copy(googleSyncState = GoogleSyncUiState(connectedEmail = "fixture@example.test"))
        }
        quick.assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithTag("settings_google_expanded").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, cloudCalls)
            flow.value = flow.value.copy(googleSyncState = flow.value.googleSyncState.copy(isBusy = true))
        }
        quick.assertIsNotEnabled()
        compose.runOnIdle {
            flow.value = flow.value.copy(googleSyncState = flow.value.googleSyncState.copy(isBusy = false))
        }
        compose.onNodeWithTag("settings_google_expand").performClick()
        quick.assertDoesNotExist()
        compose.onNodeWithTag("settings_google_sync_now_btn").performClick()
        compose.runOnIdle { assertEquals(2, cloudCalls) }
    }

    @Test fun controlsFitAndHealthConsentRemainsAccessibleInAllLanguages() {
        var language by mutableStateOf(AppLanguage.HU)
        var format by mutableStateOf("JSON")
        var health by mutableStateOf(false)
        var toggleCalls = 0
        compose.setContent { CompositionLocalProvider(
            LocalTrainPilotStrings provides TrainPilotStrings.forLanguage(language)
        ) { TrainPilotTheme {
            Box(Modifier.size(400.dp, 640.dp)) {
                BackupRestoreScreen(
                    TrainPilotUiState(language = language, selectedBackupFormat = format, includeHealthInBackup = health),
                    TrainPilotStrings.forLanguage(language), onBack = {}, onOpenSettings = {},
                    onSelectFormat = { format = it }, onToggleIncludeHealth = { health = it; toggleCalls++ }, onShowBanner = {}
                )
            }
        } } }
        // Exercise the same connected/calendar-success state seen on the device, without
        // real credentials or network calls. Settings collects this real repository flow.
        compose.runOnIdle {
            val model = ViewModelProvider(compose.activity)[SettingsViewModel::class.java]
            val field = GoogleSyncRepository::class.java.getDeclaredField("_uiState").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val flow = field.get(model.googleRepo) as MutableStateFlow<GoogleSyncUiState>
            flow.value = GoogleSyncUiState(connectedEmail = "fixture@example.test",
                lastCalendarSyncIso = "2026-11-01T10:00:00Z", lastCalendarSyncCount = 23,
                lastDriveSyncIso = "2026-11-01T09:00:00Z")
        }
        listOf(AppLanguage.HU, AppLanguage.EN, AppLanguage.DE, AppLanguage.RO).forEach { lang ->
            compose.runOnIdle {
                language = lang; format = "JSON"; health = false
                val model = ViewModelProvider(compose.activity)[SettingsViewModel::class.java]
                val field = SettingsViewModel::class.java.getDeclaredField("_uiState").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                val flow = field.get(model) as MutableStateFlow<SettingsUiState>
                flow.value = flow.value.copy(settings = flow.value.settings.copy(language = lang.code))
            }
            val viewport = compose.onNodeWithTag("backup_restore_screen").getUnclippedBoundsInRoot()
            listOf("backup_format_JSON", "backup_format_ZIP", "backup_format_DRIVE", "backup_health_switch", "backup_export_btn", "import_repforge_legacy_btn").forEach { tag ->
                val node = compose.onNodeWithTag(tag).assertIsDisplayed()
                val bounds = node.getUnclippedBoundsInRoot()
                assertTrue("$lang $tag outside viewport", bounds.top >= viewport.top && bounds.bottom <= viewport.bottom)
                assertTrue("$lang $tag has a short touch target", (bounds.bottom - bounds.top).value >= 48f)
            }
            compose.onNodeWithTag("studio_google_cloud_card").assertDoesNotExist()
            compose.onNodeWithTag("backup_format_JSON").assertIsSelected()
            val visual = compose.onNodeWithTag("backup_format_JSON_visual", useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertEquals(36.dp, (visual.bottom - visual.top))
            capture("backup-${lang.code}-json")
            compose.onNodeWithTag("backup_health_switch").performClick()
            compose.runOnIdle { assertTrue(health) }
            compose.onNodeWithTag("backup_health_info").performClick()
            val inHealthInfo = hasAnyAncestor(hasTestTag("backup_health_info_sheet"))
            compose.onNode(inHealthInfo and hasText("JSON/ZIP", substring = true)).assertIsDisplayed()
            if (lang == AppLanguage.EN) {
                compose.onNode(inHealthInfo and hasText("workout-attached health summaries only", substring = true)).assertExists()
                compose.onNode(inHealthInfo and hasText("does not delete local data or existing backups", substring = true)).assertExists()
            }
            compose.onNode(inHealthInfo and hasText("OK")).performClick()
            compose.onNodeWithTag("backup_health_info_sheet").assertDoesNotExist()
            compose.onNodeWithTag("backup_health_switch").performClick()
            compose.runOnIdle { assertFalse(health) }
            compose.onNodeWithTag("backup_format_DRIVE").performClick()
            compose.runOnIdle { assertEquals("DRIVE", format) }
            compose.onNodeWithTag("studio_google_cloud_card").assertIsDisplayed()
            compose.onNodeWithTag("settings_google_calendar_sync_success").assertDoesNotExist()
            compose.onNodeWithTag("backup_drive_last_sync").assertIsDisplayed()
            val lastBackupLabel = when (lang) {
                AppLanguage.HU -> "Utolsó mentés"
                AppLanguage.EN -> "Last backup"
                AppLanguage.DE -> "Letzte Sicherung"
                AppLanguage.RO -> "Ultimul backup"
            }
            compose.onNodeWithTag("backup_drive_last_sync")
                .assertTextContains(lastBackupLabel, substring = true)
            listOf("backup_export_btn", "import_repforge_legacy_btn").forEach { tag ->
                val bounds = compose.onNodeWithTag(tag).assertIsDisplayed().getUnclippedBoundsInRoot()
                assertTrue("$lang Drive $tag outside viewport", bounds.top >= viewport.top && bounds.bottom <= viewport.bottom)
            }
            capture("backup-${lang.code}-drive")
            compose.onNodeWithTag("backup_format_ZIP").performClick()
            compose.runOnIdle { assertEquals("ZIP", format) }
            compose.onNodeWithTag("studio_google_cloud_card").assertDoesNotExist()
        }
        compose.runOnIdle { assertEquals(8, toggleCalls) }
    }
    private fun capture(name: String) = compose.runOnIdle {
        val view = compose.activity.findViewById<View>(android.R.id.content)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val crop = Bitmap.createBitmap(bitmap, 0, 0, minOf(400, bitmap.width), minOf(640, bitmap.height))
        val appDir = if (File("app/build.gradle.kts").exists()) File("app") else File(".")
        val target = File(appDir, "build/reports/tests/device-ui/$name.png")
        target.parentFile!!.mkdirs()
        target.outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
        crop.recycle()
        if (crop !== bitmap) bitmap.recycle()
    }

}

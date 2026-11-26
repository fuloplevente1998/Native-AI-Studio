package com.example.ui.screens

import com.example.ui.theme.liquidDropMaterial

import androidx.compose.ui.semantics.semantics

import androidx.compose.foundation.BorderStroke

import com.example.ui.theme.trainPilotGlassRim

import com.example.ui.theme.GlassMaterialRole

import com.example.ui.theme.glassControlFill

import com.example.ui.theme.glassStateTile
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassCardFill
import com.example.ui.theme.glassTile
import com.example.ui.theme.trainPilotPageBackdrop
import com.example.ui.theme.LocalGlassTopInset
import com.example.ui.components.customThemeText
import com.example.ui.components.trainPilotAnimatedExpansion

import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import com.repforge.app.ui.theme.TrainPilotCardSpacing
import com.repforge.app.ui.theme.TrainPilotFrames

import com.example.ui.components.trainPilotAnimatedExpansion
import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.AppLanguage
import com.example.model.SubScreen
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * 4/4 #1 BEÁLLÍTÁSOK (Mockup 4/4 #1 + main Beállítások + 4 lenyíló téma-kategória oszlopokban)
 */
@Composable
fun SettingsMainScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenPalettePicker: () -> Unit,
    onSelectPalette: (Int) -> Unit = {},
    onSelectLanguage: (AppLanguage) -> Unit,
    onCycleRestTimer: () -> Unit,
    onToggleSoundAlert: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onToggleProgressionModal: (Boolean) -> Unit,
    onNavigateToSubScreen: (SubScreen) -> Unit,
    onShowBanner: (String) -> Unit,
    onCloudSyncCompleted: () -> Unit = {},
    onPreviewProgression: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onApproveProgression: () -> Unit = {},
    onEditProgressionPreview: () -> Unit = {},
    onUndoProgression: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val currentPaletteObj = palette

    var expandedMainThemesSection by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var expandedThemeFamily by rememberSaveable {
        mutableStateOf(currentPaletteObj.subGroup.name)
    }
    var showPrivacyPolicyDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val secUnit = tr(uiState.language, "mp", "sec", "Sek.", "sec")

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            containerColor = palette.surfaceCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = tr(uiState.language, "Adatvédelmi tájékoztató", "Privacy Policy", "Datenschutzhinweise", "Informare privind confidențialitatea"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag("settings_privacy_dialog"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = com.repforge.app.PrivacyPolicy.text(uiState.language.code.lowercase()),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyPolicyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)
                ) {
                    Text(strings.closeAction, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    val settingsListState = androidx.compose.foundation.lazy.rememberLazyListState()

    com.example.ui.components.TrainPilotScrollableListBox(
        lazyListState = settingsListState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = settingsListState,
            modifier = Modifier
                .fillMaxSize()
                .trainPilotPageBackdrop(palette)
                .testTag("settings_main_screen"),
            contentPadding = PaddingValues(start = TrainPilotCardSpacing.ScreenGutter, end = TrainPilotCardSpacing.ScreenGutter,
                top = 6.dp + LocalGlassTopInset.current, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
        item {
            SubScreenTopBar(
                title = strings.settingsTitle,
                onBack = onBack,
                onOpenSettings = {}
            )
        }

        // Google Cloud Szinkron (Drive appDataFolder + Managed Naptár)
        item {
            StudioGoogleCloudSyncCard(
                onShowBanner = onShowBanner,
                onCloudSyncCompleted = onCloudSyncCompleted
            )
        }

        // One shared frame owns the compact header and the expanded category panels.
        item {
            val toggleThemesHint = if (expandedMainThemesSection) {
                tr(uiState.language, "Koppints a 5 téma stílus becsukásához", "Tap to collapse 5 theme categories", "Tippen zum Einklappen der 5 Themenkategorien", "Atinge pentru a restrânge cele 5 categorii de teme")
            } else {
                tr(uiState.language, "Koppints a 5 téma stílus kinyitásához", "Tap to expand 5 theme categories", "Tippen zum Ausklappen der 5 Themenkategorien", "Atinge pentru a extinde cele 5 categorii de teme")
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.cardPaint,
                modifier = Modifier.fillMaxWidth().trainPilotGlassMaterial(palette, RoundedCornerShape(18.dp))
                    .trainPilotBorder(TrainPilotFrames.page(palette.sectionBorder, palette.primary), RoundedCornerShape(18.dp))
                    .trainPilotAnimatedExpansion()
                    .testTag("settings_themes_group")
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth().trainPilotClickable(RoundedCornerShape(14.dp)) {
                            expandedMainThemesSection = !expandedMainThemesSection
                        }.testTag("settings_open_palettes_row")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(customThemeText(uiState.language, "Témák", "Themes", "Designs", "Teme"), style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text(toggleThemesHint, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            }
                            Icon(imageVector = if (expandedMainThemesSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null, tint = palette.textSecondary)
                        }
                    }

                    if (expandedMainThemesSection) {
                        com.example.ui.components.StudioPaletteCategories(
                            language = uiState.language,
                            selectedId = if (palette.id == -1) -1 else uiState.selectedPaletteId,
                            expanded = com.example.ui.theme.PaletteCategory.entries.firstOrNull { it.name == expandedThemeFamily },
                            onExpand = { expandedThemeFamily = it?.name.orEmpty() },
                            onSelect = onSelectPalette
                        )
                        com.example.ui.components.CustomThemeSection(uiState.language)
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.cardPaint,
                modifier = Modifier
                    .fillMaxWidth().trainPilotGlassMaterial(palette, RoundedCornerShape(18.dp))
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(strings.languageTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val selected = uiState.language == lang
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else if (selected) palette.primary else palette.surfaceElevated,
                                border = if (palette.isLiquidGlass && selected) BorderStroke(1.dp, palette.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .liquidDropMaterial(palette, RoundedCornerShape(10.dp), if (selected) GlassMaterialRole.Selection else GlassMaterialRole.Button,
                                        accent = if (selected) palette.primary else null, live = selected)
                                    .semantics { this.selected = selected }
                                    .trainPilotClickable(RoundedCornerShape(10.dp)) { onSelectLanguage(lang) }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(
                                        text = "${lang.flag} ${lang.code}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected && !palette.isLiquidGlass) palette.onPrimary else palette.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(22.dp), color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.cardPaint,
                modifier = Modifier.fillMaxWidth().trainPilotGlassMaterial(palette, RoundedCornerShape(22.dp)).testTag("settings_workout_group")
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(22.dp))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().trainPilotClickable(RoundedCornerShape(12.dp), enabled = !uiState.progressionModeBusy) {
                                onToggleProgressionModal(!uiState.showProgressionModal)
                            }.testTag("settings_workout_toggle").padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(tr(uiState.language, "Edzés beállításai", "Workout settings", "Trainingseinstellungen", "Setări antrenament"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("${localizeTrainingStyle(uiState.trainingStyleHu, uiState.language)} • ${localizeProgressionRule(uiState.progressionRuleHu, uiState.language)} • ${strings.restLabel}: ${uiState.defaultRestSec} $secUnit",
                                style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null, tint = palette.primary)
                    }
                    // Same full-height editor window as Workout tab → Workout settings (global scope here).
                    if (uiState.showProgressionModal) {
                        StudioWorkoutModesDialog(
                            state = uiState, scope = "global", embedded = false,
                            onDismiss = { onToggleProgressionModal(false) },
                            onCycleRest = onCycleRestTimer,
                            onToggleSound = onToggleSoundAlert,
                            onToggleVibration = onToggleVibration,
                            onPreview = onPreviewProgression,
                            onApprove = onApproveProgression,
                            onEditPreview = onEditProgressionPreview,
                            onUndo = onUndoProgression
                        )
                    }
                }
            }
        }

        // Eszközök és adatok (Óra, Mentés, Engedélyek, Privát fotók, Adatvédelem)
        item {
            Text(tr(uiState.language, "Eszközök és adatok", "Devices & Data", "Geräte & Daten", "Dispozitive și date"), style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
        }

        item {
            SettingsActionRowCard(
                title = strings.watchAndSyncTitle,
                subtitle = when {
                    !uiState.trainPilotWearEnabled -> "TrainPilot Wear OS"
                    uiState.watchAppAvailable -> "${uiState.watchName} • ${tr(uiState.language, "Wear OS adatkapcsolat", "Wear OS data link", "Wear OS-Datenverbindung", "Conexiune date Wear OS")}"
                    uiState.watchConnected -> tr(uiState.language, "Wear OS rendszerkapcsolat • TrainPilot óraapp nincs telepítve", "Wear OS system connection • TrainPilot watch app not installed", "Wear OS-Systemverbindung • TrainPilot-Uhren-App nicht installiert", "Conexiune sistem Wear OS • Aplicația de ceas TrainPilot nu este instalată")
                    else -> tr(uiState.language, "Nincs csatlakoztatott óra", "No watch connected", "Keine Uhr verbunden", "Niciun ceas conectat")
                },
                badgeText = if (!uiState.trainPilotWearEnabled) "" else if (uiState.watchAppAvailable) tr(uiState.language, "Adatkapcsolat", "Data link", "Datenlink", "Conexiune") else tr(uiState.language, "Nincs kapcsolat", "Disconnected", "Getrennt", "Deconectat"),
                onClick = { onNavigateToSubScreen(SubScreen.WATCH_AND_SYNC) },
                testTag = "settings_open_watch_row"
            )
        }

        item {
            SettingsActionRowCard(
                title = strings.backupRestoreTitle,
                subtitle = tr(uiState.language, "Helyi JSON / ZIP és Google Drive biztonsági mentés", "Local JSON / ZIP and Google Drive backup", "Lokales JSON / ZIP und Google Drive-Backup", "Backup local JSON / ZIP și Google Drive"),
                badgeText = uiState.selectedBackupFormat,
                onClick = { onNavigateToSubScreen(SubScreen.BACKUP_RESTORE) },
                testTag = "settings_open_backup_row"
            )
        }

        item {
            SettingsActionRowCard(
                title = strings.permissionsTitle,
                subtitle = tr(uiState.language, "GPS, Értesítések, Health Connect, Samsung Health", "GPS, Notifications, Health Connect, Samsung Health", "GPS, Benachrichtigungen, Health Connect, Samsung Health", "GPS, Notificări, Health Connect, Samsung Health"),
                badgeText = tr(uiState.language, "Kezelés", "Manage", "Verwalten", "Gestionează"),
                onClick = { onNavigateToSubScreen(SubScreen.PERMISSIONS_SCREEN) },
                testTag = "settings_open_permissions_row"
            )
        }

        item {
            SettingsActionRowCard(
                title = strings.privatePhotosTitle,
                subtitle = tr(uiState.language, "Privát galéria • Drive-szinkronnal felhőbe kerül", "Private gallery • included in Drive sync", "Private Galerie • wird mit Drive synchronisiert", "Galerie privată • inclusă în sincronizarea Drive"),
                badgeText = "${uiState.privatePhotoCount} ${tr(uiState.language, "fotó", "photos", "Fotos", "foto")}",
                onClick = { onNavigateToSubScreen(SubScreen.PRIVATE_PHOTOS) },
                testTag = "settings_open_photos_row"
            )
        }

        item {
            SettingsActionRowCard(
                title = tr(uiState.language, "Adatvédelmi tájékoztató", "Privacy Policy", "Datenschutzhinweise", "Informare privind confidențialitatea"),
                subtitle = tr(
                    uiState.language,
                    "Helyi adattárolás, Health Connect, Samsung Health, Wear OS és felhőmentés",
                    "Local storage, Health Connect, Samsung Health, Wear OS & cloud backup",
                    "Lokale Speicherung, Health Connect, Samsung Health, Wear OS & Cloud-Backup",
                    "Stocare locală, Health Connect, Samsung Health, Wear OS și backup în cloud"
                ),
                badgeText = tr(uiState.language, "Megnyitás", "View", "Ansehen", "Vezi"),
                onClick = { showPrivacyPolicyDialog = true },
                testTag = "settings_open_privacy_row"
            )
        }

        // 2781: About / version – the installed build, developer and contact; opens the website.
        item {
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            SettingsActionRowCard(
                title = tr(uiState.language, "Névjegy", "About", "Über", "Despre"),
                subtitle = settingsAboutLine(uiState.language),
                badgeText = tr(uiState.language, "Weboldal", "Website", "Webseite", "Site"),
                onClick = { runCatching { uriHandler.openUri(settingsWebsiteUrl()) } },
                testTag = "settings_about_row"
            )
        }
    }
}
}

/** 2781: "TrainPilot 1.4.0 (278000) · FulTechStudios · e-mail" from the build itself, never typed by hand. */
internal fun settingsAboutLine(language: com.example.model.AppLanguage): String {
    val version = when (language) {
        com.example.model.AppLanguage.HU -> "Verzió"
        com.example.model.AppLanguage.EN -> "Version"
        com.example.model.AppLanguage.DE -> "Version"
        com.example.model.AppLanguage.RO -> "Versiune"
    }
    return "$version: TrainPilot ${com.repforge.app.BuildConfig.VERSION_NAME} (${com.repforge.app.BuildConfig.VERSION_CODE})" +
        " · ${com.repforge.app.BuildConfig.DEVELOPER_NAME} · ${com.repforge.app.BuildConfig.SUPPORT_EMAIL}"
}

internal fun settingsWebsiteUrl(): String =
    com.repforge.app.BuildConfig.PRIVACY_POLICY_URL.substringBeforeLast('/') + "/"

@Composable
internal fun SettingsActionRowCard(
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit,
    testTag: String = "",
    isExpanded: Boolean? = null,
    subtleBorder: Boolean = false
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        // 2810/2: Liquid Glass rows sample the page backdrop like every card; legacy fill unchanged.
        color = palette.glassCardFill(palette.surfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotGlassMaterial(palette, RoundedCornerShape(18.dp))
            .trainPilotBorder(if (subtleBorder) 0.75.dp else 1.25.dp,
                if (subtleBorder) palette.cardBorder.copy(alpha = 0.45f) else palette.sectionBorder,
                RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .trainPilotClickable(RoundedCornerShape(18.dp), onClick = onClick)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (badgeText.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.glassStateTile(palette.primary, palette.primary.copy(alpha = 0.18f))
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            val trailingVector = when (isExpanded) {
                true -> Icons.Default.ExpandLess
                false -> Icons.Default.ExpandMore
                null -> Icons.AutoMirrored.Filled.KeyboardArrowRight
            }
            Icon(imageVector = trailingVector, contentDescription = null, tint = palette.textSecondary)
        }
    }
}

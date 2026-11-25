package com.example.ui.screens

import com.example.ui.theme.glassTile
import com.example.ui.theme.trainPilotScreenBackdrop
import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * Explicit application switches; OS/provider permission management stays a separate action.
 */
@Composable
fun PermissionsScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onTogglePermission: (String, Boolean) -> Unit,
    onManagePermission: (String) -> Unit,
    hcBackgroundEnabled: Boolean = false,
    hcBackgroundPermissionGranted: Boolean = false,
    hcWorkoutWriteEnabled: Boolean = false,
    hcWorkoutWritePermissionGranted: Boolean = false,
    onToggleHcBackground: (Boolean) -> Unit = {},
    onToggleHcWorkoutWrite: (Boolean) -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val required = tr(uiState.language,
        "A használathoz rendszerengedély szükséges.",
        "System permission is required to use this feature.",
        "Für die Nutzung ist eine Systemberechtigung erforderlich.",
        "Este necesară permisiunea sistemului pentru utilizare.")
    LazyColumn(
        modifier = Modifier
            .then(com.example.ui.components.sheetAwareFillMaxSize())
            .trainPilotScreenBackdrop(palette)
            .testTag("permissions_screen"),
        contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        item {
            SubScreenTopBar(title = strings.permissionsTitle, onBack = onBack,
                onOpenSettings = onOpenSettings)
        }
        item {
            PermissionSettingsGroup("Health Connect") {
                PermissionToggleRow(
                    tr(uiState.language, "Adatok olvasása", "Read data", "Daten lesen", "Citire date"),
                    tr(uiState.language, "Pulzus, alvás, lépések és testsúly olvasása",
                        "Read heart rate, sleep, steps and weight",
                        "Puls, Schlaf, Schritte und Gewicht lesen",
                        "Citire puls, somn, pași și greutate"),
                    uiState.permissionHealthConnectEnabled,
                    if (uiState.permissionHealthConnectEnabled && !uiState.permissionHealthConnectGranted) required
                        else if (uiState.permissionHealthConnectEnabled &&
                            uiState.healthGrantedCount < uiState.healthPermissionCount)
                            tr(uiState.language, "Részleges hozzáférés", "Partial access",
                                "Teilweiser Zugriff", "Acces parțial") +
                            " ${uiState.healthGrantedCount}/${uiState.healthPermissionCount}"
                        else if (uiState.permissionHealthConnectEnabled)
                            "${uiState.healthGrantedCount}/${uiState.healthPermissionCount}" else null,
                    "privacy_health_connect_switch"
                ) { onTogglePermission("hc", it) }
                if (uiState.permissionHealthConnectEnabled) {
                    PermissionSettingsDivider()
                    PermissionToggleRow(
                        tr(uiState.language, "Háttérfrissítés", "Background refresh", "Hintergrundaktualisierung", "Actualizare în fundal"),
                        tr(uiState.language, "Külön engedély és Android-ütemezés szükséges.",
                            "Requires separate access and Android scheduling.",
                            "Benötigt eine separate Berechtigung und Android-Planung.",
                            "Necesită permisiune separată și programare Android."),
                        hcBackgroundEnabled,
                        if (hcBackgroundEnabled && !hcBackgroundPermissionGranted) required else null,
                        "privacy_hc_background_switch"
                    ) { onToggleHcBackground(it) }
                    PermissionSettingsDivider()
                    PermissionToggleRow(
                        tr(uiState.language, "Edzésírás", "Workout writing", "Trainings schreiben", "Scriere antrenamente"),
                        tr(uiState.language, "Opcionális; a forrás olvasásától független.",
                            "Optional; separate from reading health data.",
                            "Optional; getrennt vom Lesen der Gesundheitsdaten.",
                            "Opțional; separat de citirea datelor de sănătate."),
                        hcWorkoutWriteEnabled,
                        if (hcWorkoutWriteEnabled && !hcWorkoutWritePermissionGranted) required else null,
                        "privacy_hc_workout_write_switch"
                    ) { onToggleHcWorkoutWrite(it) }
                }
                PermissionSettingsDivider()
                PermissionSettingsRow(tr(uiState.language, "Health Connect engedélyei",
                    "Health Connect permissions", "Health-Connect-Berechtigungen", "Permisiuni Health Connect"),
                    "privacy_manage_health_connect") { onManagePermission("hc_manage") }
            }
        }
        item {
            PermissionSettingsGroup("Samsung Health") {
                PermissionToggleRow(
                    tr(uiState.language, "Közvetlen szinkron", "Direct sync", "Direkt-Sync", "Sincronizare directă"),
                    tr(uiState.language, "Csak a közvetlen Samsung-forrás; a már tárolt adatok megmaradnak.",
                        "Only direct Samsung reads; saved records remain.",
                        "Nur direkte Samsung-Daten; gespeicherte Einträge bleiben.",
                        "Doar date Samsung directe; înregistrările salvate rămân."),
                    uiState.permissionSamsungHealthEnabled,
                    if (uiState.permissionSamsungHealthEnabled && !uiState.permissionSamsungHealthGranted) required else null,
                    "privacy_samsung_health_switch"
                ) { onTogglePermission("samsung", it) }
                PermissionSettingsDivider()
                PermissionSettingsRow(tr(uiState.language, "Samsung Health engedélyei",
                    "Samsung Health permissions", "Samsung-Health-Berechtigungen", "Permisiuni Samsung Health"),
                    "privacy_manage_samsung") { onManagePermission("samsung_manage") }
            }
        }
        item {
            PermissionSettingsGroup(tr(uiState.language, "GPS és értesítések",
                "GPS & notifications", "GPS und Benachrichtigungen", "GPS și notificări")) {
                PermissionToggleRow(
                    tr(uiState.language, "GPS és helyadatok", "GPS & Location",
                        "GPS & Standortdaten", "GPS și locație"),
                    tr(uiState.language, "Kültéri útvonalrögzítés", "Outdoor route recording",
                        "Outdoor-Routenaufzeichnung", "Înregistrare traseu exterior"),
                    uiState.permissionGpsEnabled,
                    if (uiState.permissionGpsEnabled && (!uiState.permissionGpsGranted ||
                            !uiState.permissionCentralLocEnabled)) required else null,
                    "privacy_gps_switch"
                ) { onTogglePermission("gps", it) }
                PermissionSettingsDivider()
                PermissionToggleRow(
                    tr(uiState.language, "Opcionális értesítések", "Optional notifications",
                        "Optionale Benachrichtigungen", "Notificări opționale"),
                    tr(uiState.language,
                        "A kötelező, aktív edzéshez tartozó rendszerértesítések megmaradhatnak.",
                        "Mandatory foreground workout notifications may still appear.",
                        "Erforderliche Vordergrund-Trainingsmeldungen können weiterhin erscheinen.",
                        "Notificările obligatorii pentru antrenamente active pot apărea în continuare."),
                    uiState.permissionNotificationsEnabled,
                    if (uiState.permissionNotificationsEnabled && !uiState.permissionNotificationsGranted) required else null,
                    "privacy_notifications_switch"
                ) { onTogglePermission("notif", it) }
                PermissionSettingsDivider()
                PermissionSettingsRow(tr(uiState.language, "TrainPilot rendszerengedélyei",
                    "TrainPilot system permissions", "TrainPilot-Systemberechtigungen", "Permisiuni de sistem TrainPilot"),
                    "privacy_manage_permissions") { onManagePermission("permissions_manage") }
                PermissionSettingsDivider()
                PermissionSettingsRow(tr(uiState.language, "Rendszer helybeállításai",
                    "System location settings", "System-Standorteinstellungen", "Setări de locație ale sistemului"),
                    "privacy_system_location_settings",
                    if (uiState.permissionCentralLocEnabled) tr(uiState.language, "Bekapcsolva", "On", "Ein", "Pornit")
                    else tr(uiState.language, "Kikapcsolva", "Off", "Aus", "Oprit")) { onManagePermission("central") }
            }
        }
    }
}

@Composable
internal fun PermissionSettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val shape = RoundedCornerShape(18.dp)
    Surface(shape = shape, color = palette.glassTile(palette.surfaceCard),
        modifier = Modifier.fillMaxWidth().trainPilotBorder(1.25.dp, palette.sectionBorder, shape)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = palette.textPrimary,
                modifier = Modifier.padding(bottom = 2.dp))
            content()
        }
    }
}

@Composable
internal fun PermissionSettingsDivider() {
    HorizontalDivider(color = LocalTrainPilotPalette.current.cardBorder.copy(alpha = 0.45f))
}

@Composable
internal fun PermissionSettingsRow(title: String, tag: String, status: String? = null, onClick: () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .trainPilotClickable(RoundedCornerShape(10.dp), onClick = onClick)
        .testTag(tag).padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = palette.primary, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f))
        if (status != null) Text(status, color = palette.textSecondary,
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.primary)
    }
}

@Composable
internal fun PermissionToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    status: String?,
    testTag: String,
    onToggle: (Boolean) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            if (status != null) Text(status, style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary, modifier = Modifier.testTag("${testTag}_status"))
        }
        Switch(checked = checked, onCheckedChange = onToggle, modifier = Modifier.testTag(testTag))
    }
}

/**
 * 4/4 #5 PRIVÁT EDZÉSFOTÓK (Mockup 4/4 #5)
 */
@Composable
fun PrivatePhotosScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowBanner: (String) -> Unit,
    onPhotosUpdated: () -> Unit = {}
) {
    com.example.ui.components.StudioPrivatePhotos(
        uiState, strings, onBack, onOpenSettings, onPhotosUpdated = onPhotosUpdated
    )
}

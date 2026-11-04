package com.example.ui.components.watchinfo

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.watchinfo.WatchInfoCardData
import com.example.model.watchinfo.WatchInfoStatus
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.TrainPilotFrames

/**
 * Replaces both the former watch card and the separate unframed info/button row.
 * The caller supplies real, verified data and the existing requestWatchDeviceInfo callback.
 */
@Composable
fun StudioWatchInformationCard(
    data: WatchInfoCardData,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    val lang = com.example.localization.LocalTrainPilotStrings.current.language
    fun word(hu: String, en: String, de: String, ro: String) = when (lang) {
        com.example.model.AppLanguage.HU -> hu
        com.example.model.AppLanguage.EN -> en
        com.example.model.AppLanguage.DE -> de
        com.example.model.AppLanguage.RO -> ro
    }
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier.fillMaxWidth()
            .border(TrainPilotFrames.page(palette.sectionBorder, palette.primary), shape)
            .testTag("watch_device_info_card")
            .clickable(enabled = !data.loading, role = Role.Button, onClick = onRefresh),
        shape = shape,
        color = palette.surfaceCard
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Watch, contentDescription = null,
                    tint = palette.primary, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(data.name?.takeIf(String::isNotBlank) ?: word("Óraadatok", "Watch info", "Uhrdaten", "Date ceas"), style = MaterialTheme.typography.titleSmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Text(
                        when {
                            data.appAvailable && data.connected -> word("TrainPilot Wear elérhető", "TrainPilot Wear available", "TrainPilot Wear verfügbar", "TrainPilot Wear disponibil")
                            data.connected -> word("Wear OS elérhető • TrainPilot Wear még nem igazolt", "Wear OS available • TrainPilot Wear not verified yet", "Wear OS verfügbar • TrainPilot Wear noch nicht bestätigt", "Wear OS disponibil • TrainPilot Wear încă neverificat")
                            else -> word("Nincs órakapcsolat", "No watch connection", "Keine Uhrverbindung", "Fără conexiune cu ceasul")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            data.appAvailable && data.connected -> palette.success
                            data.connected -> palette.warning
                            else -> palette.textSecondary
                        }
                    )
                }
                IconButton(
                    onClick = onRefresh, enabled = !data.loading,
                    modifier = Modifier.size(48.dp).testTag("watch_refresh_device_info")
                ) {
                    if (data.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp, color = palette.primary
                        )
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = word("Óraadatok frissítése", "Refresh watch info", "Uhrdaten aktualisieren", "Actualizează datele ceasului"),
                            tint = palette.primary)
                    }
                }
            }

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val sideBySide = maxWidth >= 300.dp && LocalDensity.current.fontScale <= 1.3f
                val device: @Composable (Modifier) -> Unit = { panel ->
                    WatchInfoPanel(word("Eszköz", "Device", "Gerät", "Dispozitiv"), "watch_info_device_panel", panel) {
                        WatchInfoField(word("Modell", "Model", "Modell", "Model"), data.model)
                        WatchInfoField(word("Akku", "Battery", "Akku", "Baterie"), data.batteryPercent?.let { "${it}%" })
                    }
                }
                val system: @Composable (Modifier) -> Unit = { panel ->
                    WatchInfoPanel(word("Rendszer", "System", "System", "Sistem"), "watch_info_system_panel", panel) {
                        WatchInfoField("Android", data.androidVersion)
                        WatchInfoField("TrainPilot", data.appVersion)
                        WatchInfoField("Build", data.appBuild?.toString())
                    }
                }
                if (sideBySide) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        device(Modifier.weight(1f))
                        system(Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        device(Modifier.fillMaxWidth())
                        system(Modifier.fillMaxWidth())
                    }
                }
            }
            Text(
                text = when {
                    data.loading -> word("Óraadatok lekérése…", "Fetching watch info…", "Uhrdaten werden abgerufen…", "Se preiau datele ceasului…")
                    data.statusKind == WatchInfoStatus.ERROR || data.statusKind == WatchInfoStatus.TIMEOUT ->
                        data.status?.takeIf(String::isNotBlank) ?: word("Az óraadatok lekérése nem sikerült.", "Failed to fetch watch info.", "Abruf der Uhrdaten fehlgeschlagen.", "Preluarea datelor ceasului a eșuat.")
                    data.lastSync != null -> "${word("Frissítve", "Updated", "Aktualisiert", "Actualizat")} · ${data.lastSync}"
                    else -> data.status?.takeIf(String::isNotBlank) ?: word("Még nincs frissítési eredmény.", "No refresh result yet.", "Noch kein Aktualisierungsergebnis.", "Încă nu există rezultat de actualizare.")
                },
                modifier = Modifier.testTag("watch_device_info_status"),
                color = when {
                    data.statusKind == WatchInfoStatus.ERROR || data.statusKind == WatchInfoStatus.TIMEOUT -> palette.danger
                    else -> palette.textSecondary
                },
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun WatchInfoPanel(title: String, tag: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.fillMaxWidth()
            .border(TrainPilotFrames.stroke(TrainPilotFrames.panelWidth, palette.sectionBorder), shape)
            .testTag(tag)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
        content()
    }
}

@Composable
private fun WatchInfoField(label: String, value: String?) {
    val palette = LocalTrainPilotPalette.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        Text(value?.takeIf(String::isNotBlank) ?: "—",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textPrimary)
    }
}

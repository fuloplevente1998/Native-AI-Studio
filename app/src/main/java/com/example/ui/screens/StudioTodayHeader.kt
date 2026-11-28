package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.AppLanguage
import com.example.ui.components.OperationFeedbackKind
import com.example.ui.components.OperationFeedbackMark
import com.example.ui.components.operationFeedbackColor
import com.example.ui.theme.*
import com.example.ui.viewmodel.TrainPilotUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.delay

private fun statusText(lang: AppLanguage, hu: String, en: String, de: String, ro: String) =
    when (lang) { AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro }

@Composable
internal fun StudioTodayCompactHeader(state: TrainPilotUiState, strings: LocalizedStrings,
    editing: Boolean, onRefresh: () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    Row(Modifier.fillMaxWidth().testTag("home_today_header"), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Default.Favorite, null, tint = palette.primary, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(strings.todayStateTitle, color = palette.textPrimary, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge)
            Text(LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.forLanguageTag(state.language.code.lowercase()))),
                color = palette.primary, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.testTag("home_today_date"))
        }
        if (!editing) IconButton(onClick = onRefresh, enabled = !state.healthSyncBusy,
            modifier = Modifier.testTag("home_today_header_sync")) {
            Icon(Icons.Default.Sync, statusText(state.language, "Mai egészségadatok frissítése",
                "Refresh today's health data", "Heutige Gesundheitsdaten aktualisieren",
                "Actualizează datele de sănătate de azi"), tint = palette.primary)
        }
    }
}

/** Real terminal outcomes only; a nonempty message is not evidence of success. */
@Composable
internal fun StudioTodaySyncFeedback(state: TrainPilotUiState, tag: String, onRefresh: () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val kind = when {
        state.healthSyncBusy -> OperationFeedbackKind.RUNNING
        state.healthSyncOutcome == "CANCELLED" -> OperationFeedbackKind.CANCELLED
        state.healthSyncFailed -> OperationFeedbackKind.FAILED
        state.healthSyncPartial -> OperationFeedbackKind.PARTIAL
        state.healthSyncOutcome == "SUCCESS" -> OperationFeedbackKind.SUCCESS
        else -> return
    }
    var show by remember(kind, state.healthSyncMessage) { mutableStateOf(true) }
    LaunchedEffect(kind, state.healthSyncMessage) {
        if (kind == OperationFeedbackKind.SUCCESS) {
            delay(3500)
            show = false
        }
    }
    if (!show) return
    val error = kind == OperationFeedbackKind.FAILED || kind == OperationFeedbackKind.PARTIAL ||
        kind == OperationFeedbackKind.CANCELLED
    val color = palette.operationFeedbackColor(kind)
    val message = when (kind) {
        OperationFeedbackKind.RUNNING -> statusText(state.language, "Frissítés…", "Refreshing…", "Aktualisierung…", "Se actualizează…")
        OperationFeedbackKind.SUCCESS -> statusText(state.language, "Mai állapot frissítve", "Today's status refreshed", "Heutiger Status aktualisiert", "Starea de azi actualizată")
        OperationFeedbackKind.PARTIAL -> statusText(state.language, "Részleges szinkron", "Partial sync", "Teilweise synchronisiert", "Sincronizare parțială")
        OperationFeedbackKind.CANCELLED -> statusText(state.language, "Frissítés megszakítva", "Refresh cancelled", "Aktualisierung abgebrochen", "Actualizare anulată")
        OperationFeedbackKind.FAILED -> statusText(state.language, "Szinkron sikertelen", "Sync failed", "Synchronisierung fehlgeschlagen", "Sincronizare eșuată")
        OperationFeedbackKind.INFORMATION -> return
    }
    Row(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
        .testTag("${tag}_today_sync_result"), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OperationFeedbackMark(kind)
        Column(Modifier.weight(1f)) {
            Text(message, color = color, style = MaterialTheme.typography.labelSmall)
            if (error && state.healthSyncMessage.isNotBlank())
                Text(state.healthSyncMessage, color = palette.textSecondary, style = MaterialTheme.typography.labelSmall,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        if (error) com.repforge.app.ui.theme.TrainPilotTextButton(onClick = onRefresh,
            modifier = Modifier.testTag("${tag}_today_sync_retry")) {
            Text(statusText(state.language, "Újra", "Retry", "Erneut", "Reîncearcă"))
        }
    }
}

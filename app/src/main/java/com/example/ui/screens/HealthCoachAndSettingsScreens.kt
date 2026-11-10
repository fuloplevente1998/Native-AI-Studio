package com.example.ui.screens

import com.repforge.app.ui.theme.trainPilotSoftGlow

import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import com.repforge.app.ui.theme.TrainPilotCardSpacing
import com.repforge.app.ui.theme.TrainPilotFrames

import com.example.model.workoutsOn
import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.material3.LinearProgressIndicator

import com.example.model.localWorkoutDate

import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.ui.components.watchinfo.StudioWatchInformationCard
import com.example.ui.components.healthreadings.HealthPulseWeekRows
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalizedStrings
import com.example.model.AppLanguage
import com.example.model.SubScreen
import com.example.ui.components.MockupHealthConnectBadge
import com.example.ui.components.MockupWatchGraphicBadge
import com.example.ui.components.SubScreenTopBar
import android.app.Activity
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ui.settings.NativeGoogleConsentLauncher
import com.repforge.app.ui.settings.NativeGoogleMaintenanceConfirmation
import com.repforge.app.ui.settings.NativeGoogleMaintenanceControls
import com.repforge.app.ui.settings.NativeGoogleMaintenanceFeedback
import com.repforge.app.ui.settings.SettingsViewModel

/**
 * 5. EGÉSZSÉG (Mockup 3/4 #5 + teljes régi main Egészség elrendezés)
 */
data class WeightMeasurementEntry(
    val id: String,
    val weightLabel: String,
    val weightValue: Float,
    val dateLabel: String,
    val parsedDate: java.time.LocalDate = java.time.LocalDate.now()
)

private fun tr(lang: AppLanguage, hu: String, en: String, de: String, ro: String): String = when (lang) {
    AppLanguage.HU -> hu
    AppLanguage.EN -> en
    AppLanguage.DE -> de
    AppLanguage.RO -> ro
}

private fun localizeTrainingStyle(rawHu: String, lang: AppLanguage): String = when (rawHu.trim()) {
    "Normál hipertrófia" -> tr(lang, "Normál hipertrófia", "Standard Hypertrophy", "Normale Hypertrophie", "Hipertrofie standard")
    "HIT-inspirált" -> tr(lang, "HIT-inspirált", "HIT-inspired", "HIT-inspiriert", "Inspirat de HIT")
    else -> rawHu
}

private fun localizeProgressionRule(rawHu: String, lang: AppLanguage): String = when (rawHu.trim()) {
    "Dupla progresszió" -> tr(lang, "Dupla progresszió", "Double Progression", "Doppelte Progression", "Progresie dublă")
    "Lineáris progresszió" -> tr(lang, "Lineáris progresszió", "Linear Progression", "Lineare Progression", "Progresie liniară")
    "RIR/Coach" -> "RIR / Coach"
    else -> rawHu
}

private fun parseWeightEntryLocalDate(rawDate: String, fallback: java.time.LocalDate = java.time.LocalDate.now()): java.time.LocalDate {
    val cleaned = rawDate.trim()
    if (cleaned.isBlank()) return fallback
    val nums = Regex("\\d+").findAll(cleaned).mapNotNull { it.value.toIntOrNull() }.toList()
    if (nums.size >= 3) {
        val y = if (nums[0] in 1970..2100) nums[0] else fallback.year
        val m = nums[1].coerceIn(1, 12)
        val d = nums[2].coerceIn(1, 28)
        return runCatching { java.time.LocalDate.of(y, m, nums[2].coerceIn(1, 31)) }
            .getOrElse { java.time.LocalDate.of(y, m, d) }
    }
    return fallback
}

/**
 * 5. EGÉSZSÉG (A csatolt 3 main Egészség képernyőkép pontos, lenyitható felépítése:
 * Mai állapot + Pulzustrend + Testsúlynapló + Egészségnapló + Health Connect)
 */
@Composable
fun HealthTabScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onToggleHealthSection: (String) -> Unit,
    onSelectWeightRange: (Int) -> Unit,
    onToggleAddWeightDialog: (Boolean) -> Unit,
    onSaveNewWeight: (Float, String) -> Unit,
    onTogglePrimaryHealthSource: () -> Unit,
    onTriggerHealthSync: () -> Unit,
    onNavigateToSubScreen: (SubScreen) -> Unit,
    onOpenPalettePicker: () -> Unit,
    onOpenGoalsDialog: () -> Unit = {},
    onDeleteWeightLog: (String) -> Unit = {},
    onSelectHealthSource: (Boolean) -> Unit = { onTogglePrimaryHealthSource() },
    onRequestHealthPermissions: () -> Unit = { onNavigateToSubScreen(SubScreen.PERMISSIONS_SCREEN) },
    onOpenLoggedWorkout: (String) -> Unit = { onNavigateToSubScreen(SubScreen.LOG_WORKOUT_DETAIL) },
    onReadHealthSource: ((String) -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    val today = remember { java.time.LocalDate.now() }
    val todayHuLabel = remember(today) {
        today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy. MMM d.", java.util.Locale("hu", "HU")))
    }
    val todayDotLabel = remember(today) {
        today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy. MM. dd."))
    }
    var syncDetailsExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val dotFormatter = remember {
        java.time.format.DateTimeFormatter.ofPattern("yyyy. MM. dd.")
    }
    val shortChartDateFormatter = remember {
        java.time.format.DateTimeFormatter.ofPattern("MM.dd.")
    }
    val coroutineScope = rememberCoroutineScope()

    var recentMeasurements by remember(uiState.bodyWeightLogs) {
        if (uiState.bodyWeightLogs.isNotEmpty()) {
            mutableStateOf(
                uiState.bodyWeightLogs.map { item ->
                    val parsed = runCatching { java.time.LocalDate.parse(item.dayKey.take(10)) }.getOrElse { today }
                    WeightMeasurementEntry(
                        id = item.id,
                        weightLabel = "${"%.1f".format(item.kg).replace('.', ',')} kg",
                        weightValue = item.kg,
                        dateLabel = item.dateLabelHu,
                        parsedDate = parsed
                    )
                }.sortedByDescending { it.parsedDate }
            )
        } else {
            mutableStateOf(emptyList<WeightMeasurementEntry>())
        }
    }
    // Always sorted chronologically (oldest -> newest) by the user-entered date for the chart!
    val chronologicalMeasurements = remember(recentMeasurements, uiState.selectedWeightRangeIndex) {
        val cutoff = when (uiState.selectedWeightRangeIndex) {
            0 -> today.minusDays(7)
            1 -> today.minusDays(30)
            2 -> today.minusMonths(3)
            3 -> today.minusYears(1)
            else -> java.time.LocalDate.of(1970, 1, 1)
        }
        val filtered = recentMeasurements.filter { !it.parsedDate.isBefore(cutoff) }
        (if (filtered.size >= 2) filtered else recentMeasurements).sortedBy { it.parsedDate }
    }
    val latestChronologicalEntry = remember(recentMeasurements) {
        recentMeasurements.maxByOrNull { it.parsedDate }
    }
    var editingMeasurement by remember { mutableStateOf<WeightMeasurementEntry?>(null) }

    if (uiState.showAddWeightDialog || editingMeasurement != null) {
        val initialVal = editingMeasurement?.weightValue?.toString()
            ?: if (uiState.currentBodyWeightKg > 0f) uiState.currentBodyWeightKg.toString() else ""
        var weightText by remember(editingMeasurement, uiState.showAddWeightDialog) { mutableStateOf(initialVal) }
        var dateText by remember(editingMeasurement, uiState.showAddWeightDialog) {
            mutableStateOf(editingMeasurement?.dateLabel ?: todayDotLabel)
        }
        AlertDialog(
            onDismissRequest = {
                editingMeasurement = null
                onToggleAddWeightDialog(false)
            },
            containerColor = palette.surfaceCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = if (editingMeasurement != null) {
                        tr(uiState.language, "Mérés szerkesztése", "Edit Measurement", "Messung bearbeiten", "Editează măsurătoarea")
                    } else {
                        tr(uiState.language, "Új testsúlymérés", "New Weight Entry", "Neue Gewichtsmessung", "Măsurătoare nouă greutate")
                    },
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        tr(
                            uiState.language,
                            "Testsúly (kg) és a mérés dátuma (a grafikon ezen dátum alapján rendezi):",
                            "Body weight (kg) and measurement date (sorted chronologically on chart):",
                            "Körpergewicht (kg) und Messdatum (im Diagramm chronologisch sortiert):",
                            "Greutate (kg) și data măsurătorii (ordonate cronologic pe grafic):"
                        )
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        singleLine = true,
                        label = { Text(tr(uiState.language, "Testsúly (kg)", "Weight (kg)", "Gewicht (kg)", "Greutate (kg)")) }
                    )
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        singleLine = true,
                        label = {
                            Text(
                                tr(
                                    uiState.language,
                                    "Mérés dátuma (pl. 2026. 10. 15.)",
                                    "Measurement date (e.g. 2026. 10. 15.)",
                                    "Messdatum (z. B. 2026. 10. 15.)",
                                    "Data măsurătorii (ex. 2026. 10. 15.)"
                                )
                            )
                        }
                    )
                    // Quick date presets for fast testing of historical dates
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            tr(uiState.language, "Ma", "Today", "Heute", "Azi") to today,
                            tr(uiState.language, "-3 nap", "-3d", "-3T", "-3 zile") to today.minusDays(3),
                            tr(uiState.language, "-7 nap", "-7d", "-7T", "-7 zile") to today.minusDays(7),
                            tr(uiState.language, "-14 nap", "-14d", "-14T", "-14 zile") to today.minusDays(14)
                        ).forEach { (presetTitle, presetDate) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { dateText = presetDate.format(dotFormatter) }
                            ) {
                                Text(
                                    text = presetTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                com.repforge.app.ui.theme.TrainPilotSaveButton(
                    onClick = {
                        val parsed = weightText.replace(',', '.').toFloatOrNull() ?: uiState.currentBodyWeightKg
                        if (parsed > 0f) {
                            val formattedKg = if (parsed % 1f == 0f) "${parsed.toInt()} kg" else "${"%.1f".format(parsed).replace('.', ',')} kg"
                            val entryLocalDate = parseWeightEntryLocalDate(dateText, today)
                            val normalizedDateLabel = entryLocalDate.format(dotFormatter)
                            if (editingMeasurement != null) {
                                val targetId = editingMeasurement!!.id
                                recentMeasurements = recentMeasurements.map {
                                    if (it.id == targetId) {
                                        it.copy(
                                            weightLabel = formattedKg,
                                            weightValue = parsed,
                                            dateLabel = normalizedDateLabel,
                                            parsedDate = entryLocalDate
                                        )
                                    } else it
                                }.sortedByDescending { it.parsedDate }
                                editingMeasurement = null
                            } else {
                                recentMeasurements = (recentMeasurements + WeightMeasurementEntry(
                                    id = "w_${System.currentTimeMillis()}",
                                    weightLabel = formattedKg,
                                    weightValue = parsed,
                                    dateLabel = normalizedDateLabel,
                                    parsedDate = entryLocalDate
                                )).sortedByDescending { it.parsedDate }
                            }
                            onSaveNewWeight(parsed, normalizedDateLabel)
                        } else {
                            editingMeasurement = null
                            onToggleAddWeightDialog(false)
                        }
                    }) {
                    Text(strings.saveAction, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TrainPilotOutlinedButton(
                    onClick = {
                        editingMeasurement = null
                        onToggleAddWeightDialog(false)
                    }
                ) {
                    Text(strings.cancelAction, color = palette.textPrimary)
                }
            }
        )
    }

    val healthListState = androidx.compose.foundation.lazy.rememberLazyListState()

    com.example.ui.components.TrainPilotScrollableListBox(
        lazyListState = healthListState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = healthListState,
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .testTag("health_tab_screen"),
            contentPadding = PaddingValues(start = TrainPilotCardSpacing.ScreenGutter, end = TrainPilotCardSpacing.ScreenGutter, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
        // 1. OUTER GOLD-BORDERED CONTAINER: "Mai állapot" (6 daily tiles + dynamic today date)
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.2.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.primary.copy(alpha = 0.14f),
                            modifier = Modifier
                                .size(48.dp)
                                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = strings.todayStateTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = todayHuLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary
                                )
                            }
                        }
                        IconButton(
                            onClick = onTriggerHealthSync,
                            enabled = !uiState.healthSyncBusy,
                            modifier = Modifier.testTag("health_today_header_sync")
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = tr(
                                    uiState.language,
                                    "Mai egészségadatok frissítése",
                                    "Refresh today's health data",
                                    "Heutige Gesundheitsdaten aktualisieren",
                                    "Actualizează datele de sănătate de azi"
                                ),
                                tint = palette.primary
                            )
                        }
                    }

                    var expandedTodayTileKey by remember { mutableStateOf<String?>(null) }

                    StudioTodayStatusGrid(uiState) { key ->
                        expandedTodayTileKey = if (expandedTodayTileKey == key) null else key
                        when (key) {
                            "sleep", "steps" -> onToggleHealthSection("journal")
                            "pulse" -> onToggleHealthSection("pulse")
                        }
                    }

                    // Expandable Detail Dropdown Card when any of the 6 "Mai állapot" tiles is tapped
                    androidx.compose.animation.AnimatedVisibility(visible = expandedTodayTileKey != null) {
                        val activeKey = expandedTodayTileKey ?: "sleep"
                        val currentDaily = uiState.dailyHealthDate == today.toString()
                        val activeEnergy = if (currentDaily && "activeCalories" in uiState.dailyHealthAvailableFields)
                            "${uiState.activeCaloriesKcal} kcal" else "—"
                        val totalEnergy = if (currentDaily && "totalCalories" in uiState.dailyHealthAvailableFields)
                            "${uiState.totalCaloriesKcal} kcal" else "—"
                        val heartRange = if (currentDaily && uiState.minHrBpm > 0 && uiState.maxHrBpm > 0)
                            "${uiState.minHrBpm}–${uiState.maxHrBpm} bpm" else tr(uiState.language, "nincs lekért napi tartomány", "no daily range fetched", "kein Tagesbereich geladen", "fără interval zilnic preluat")
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.background.copy(alpha = 0.75f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                                .testTag("health_today_tile_expanded_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val openHealthJournalAndScroll: () -> Unit = {
                                    if (!uiState.expandedHealthJournal) {
                                        onToggleHealthSection("journal")
                                    }
                                    coroutineScope.launch {
                                        healthListState.animateScrollToItem(2)
                                    }
                                }
                                val openPulseTrendAndScroll: () -> Unit = {
                                    if (!uiState.expandedHealthPulseTrend) {
                                        onToggleHealthSection("pulse")
                                    }
                                    coroutineScope.launch {
                                        healthListState.animateScrollToItem(1)
                                    }
                                }
                                val (titleStr, onActionClick) = when (activeKey) {
                                    "calories" -> listOf(
                                        tr(uiState.language, "Aktív kalória részletek • $activeEnergy", "Active Calories Details • $activeEnergy", "Aktive Kalorien Details • $activeEnergy", "Detalii calorii active • $activeEnergy"),
                                        if (currentDaily && "activeCalories" in uiState.dailyHealthAvailableFields) {
                                            tr(uiState.language, "Aktív energia (edzés & mozgás): $activeEnergy • Teljes napi energia (nyugalmival): $totalEnergy • Forrás: ${uiState.dailyHealthSourceLabel}",
                                                "Active energy (workout & activity): $activeEnergy • Total daily energy (with resting): $totalEnergy • Source: ${uiState.dailyHealthSourceLabel}",
                                                "Aktive Energie: $activeEnergy • Tagesgesamtenergie: $totalEnergy • Quelle: ${uiState.dailyHealthSourceLabel}",
                                                "Energie activă: $activeEnergy • Energie zilnică totală: $totalEnergy • Sursă: ${uiState.dailyHealthSourceLabel}")
                                        } else {
                                            tr(uiState.language, "A kiválasztott szolgáltatótól (${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}) ma még nem érkezett aktív kalória adat. A nyugalmi energia külön kerül elszámolásra.",
                                                "No active calories fetched yet today from ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}. Resting energy is accounted separately.",
                                                "Noch keine aktiven Kalorien von ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} geladen. Ruheenergie wird separat berechnet.",
                                                "Fără calorii active de la ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} azi. Energia de repaus este calculată separat.")
                                        },
                                        tr(uiState.language, "Egészségnapló megnyitása lent", "Open Health Journal below", "Gesundheitstagebuch unten öffnen", "Deschide jurnalul de sănătate mai jos")
                                    ) to openHealthJournalAndScroll
                                    "pulse" -> {
                                        val pulseReading = uiState.todayPulseReading
                                        val pulseMain = pulseReading?.mainLabelHu ?: if (uiState.currentHrBpm > 0) "${uiState.currentHrBpm} bpm" else "—"
                                        listOf(
                                            tr(uiState.language, "Mai pulzus • $pulseMain", "Today's Heart Rate • $pulseMain", "Heutiger Puls • $pulseMain", "Pulsul de azi • $pulseMain"),
                                            if (pulseReading != null && pulseReading.state == com.repforge.app.healthreadings.ReadingState.AVAILABLE) {
                                                tr(uiState.language, "Pontos típus: ${pulseReading.averageDescriptionHu ?: "Időbélyeges mérés"} • Nyugalmi pulzus: ${if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—"} • Tartomány: $heartRange",
                                                    "Reading type: ${pulseReading.averageDescriptionHu ?: "Timestamped reading"} • Resting HR: ${if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—"} • Range: $heartRange",
                                                    "Messart: ${pulseReading.averageDescriptionHu ?: "Zeitstempel-Messung"} • Ruhepuls: ${if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—"} • Bereich: $heartRange",
                                                    "Tip măsurătoare: ${pulseReading.averageDescriptionHu ?: "Măsurătoare"} • Puls repaus: ${if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—"} • Interval: $heartRange")
                                            } else {
                                                pulseReading?.explanationHu ?: tr(uiState.language, "Nincs lekért pulzusmérés a kiválasztott szolgáltatótól.", "No heart rate reading fetched from the selected provider.", "Keine Pulsmessung vom ausgewählten Dienst geladen.", "Nicio măsurătoare de puls preluată de la furnizorul selectat.")
                                            },
                                            tr(uiState.language, "Korábbi mai pulzusmérések megtekintése lent", "View earlier pulse readings below", "Frühere Pulsmessungen unten ansehen", "Vezi măsurătorile anterioare de puls mai jos")
                                        ) to openPulseTrendAndScroll
                                    }
                                    "steps" -> listOf(
                                        tr(uiState.language, "Lépések részletek • ${if ("steps" in uiState.dailyHealthAvailableFields) "${uiState.dailySteps}" else "—"} / ${uiState.stepsGoal}", "Steps Details • ${if ("steps" in uiState.dailyHealthAvailableFields) "${uiState.dailySteps}" else "—"} / ${uiState.stepsGoal}", "Schritte Details • ${if ("steps" in uiState.dailyHealthAvailableFields) "${uiState.dailySteps}" else "—"} / ${uiState.stepsGoal}", "Detalii pași • ${if ("steps" in uiState.dailyHealthAvailableFields) "${uiState.dailySteps}" else "—"} / ${uiState.stepsGoal}"),
                                        if ("steps" in uiState.dailyHealthAvailableFields) {
                                            tr(uiState.language, "Egyetlen deduplikált forrásból (${uiState.dailyHealthSourceLabel}). Teljesítés: ${((uiState.dailySteps.toFloat() / uiState.stepsGoal.coerceAtLeast(1)) * 100).toInt()}% • Becsült táv: ${"%.1f".format(uiState.dailySteps * 0.00075f)} km",
                                                "Single deduplicated source (${uiState.dailyHealthSourceLabel}). Completion: ${((uiState.dailySteps.toFloat() / uiState.stepsGoal.coerceAtLeast(1)) * 100).toInt()}% • Est. distance: ${"%.1f".format(uiState.dailySteps * 0.00075f)} km",
                                                "Aus einer deduplizierten Quelle (${uiState.dailyHealthSourceLabel}). Erfüllung: ${((uiState.dailySteps.toFloat() / uiState.stepsGoal.coerceAtLeast(1)) * 100).toInt()}% • Gesch. Distanz: ${"%.1f".format(uiState.dailySteps * 0.00075f)} km",
                                                "Sursă deduplicată unică (${uiState.dailyHealthSourceLabel}). Progres: ${((uiState.dailySteps.toFloat() / uiState.stepsGoal.coerceAtLeast(1)) * 100).toInt()}% • Dist. est.: ${"%.1f".format(uiState.dailySteps * 0.00075f)} km")
                                        } else {
                                            tr(uiState.language, "Ma még nem érkezett lépésadat a kiválasztott szolgáltatótól (${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}). Napi cél: ${uiState.stepsGoal} lépés.",
                                                "No steps data fetched yet today from ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}. Daily target: ${uiState.stepsGoal} steps.",
                                                "Noch keine Schrittdaten von ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} geladen. Tagesziel: ${uiState.stepsGoal} Schritte.",
                                                "Fără date despre pași de la ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} azi. Obiectiv zilnic: ${uiState.stepsGoal} pași.")
                                        },
                                        tr(uiState.language, "Célok módosítása >", "Edit Goals >", "Ziele ändern >", "Modifică obiectivele >")
                                    ) to { onOpenGoalsDialog() }
                                    "workout" -> {
                                        val todaysWorkouts = uiState.loggedWorkouts.workoutsOn(today)
                                        val latestTodayTitle = todaysWorkouts.firstOrNull()?.localizedTitle(uiState.language, uiState.programs, uiState.exercises)
                                        listOf(
                                            tr(uiState.language, "Mai edzések • ${todaysWorkouts.size} rögzített edzés", "Today's Workouts • ${todaysWorkouts.size} logged", "Heutige Trainings • ${todaysWorkouts.size} erfasst", "Antrenamente azi • ${todaysWorkouts.size} înregistrate"),
                                            if (todaysWorkouts.isNotEmpty()) {
                                                tr(uiState.language, "Helyi Room edzésnapló • Utolsó mai edzés: ${latestTodayTitle ?: "Edzés"}",
                                                    "Local Room workout log • Latest workout today: ${latestTodayTitle ?: "Workout"}",
                                                    "Lokales Room-Protokoll • Letztes Training heute: ${latestTodayTitle ?: "Training"}",
                                                    "Jurnal local Room • Ultimul antrenament azi: ${latestTodayTitle ?: "Antrenament"}")
                                            } else {
                                                tr(uiState.language, "Ma még nincs befejezett és elmentett edzés a helyi Room naplóban a mai naptári napra.",
                                                    "No completed workout logged yet today in local Room database for today's calendar date.",
                                                    "Heute noch kein abgeschlossenes Training im lokalen Room-Protokoll für das heutige Kalenderdatum.",
                                                    "Niciun antrenament finalizat înregistrat azi în baza de date locală Room.")
                                            },
                                            if (todaysWorkouts.isNotEmpty()) {
                                                tr(uiState.language, "Edzésnapló részletek megnyitása >", "Open Workout Log details >", "Trainingsprotokoll-Details öffnen >", "Deschide detaliile jurnalului >")
                                            } else {
                                                tr(uiState.language, "Edzésprogramok megnyitása >", "Open Workout Programs >", "Trainingsprogramme öffnen >", "Deschide programele de antrenament >")
                                            }
                                        ) to {
                                            val firstId = todaysWorkouts.firstOrNull()?.id
                                            if (firstId != null) onOpenLoggedWorkout(firstId) else onNavigateToSubScreen(SubScreen.PROGRAMS_LIST)
                                        }
                                    }
                                    "recovery" -> {
                                        val rec = uiState.todayRecoveryReading
                                        val scoreProvHu = when (rec?.source) {
                                            com.repforge.app.healthreadings.RecoverySource.SAMSUNG_ENERGY -> "Közvetlen Samsung Health Energy Score (${rec.displayScoreHu})"
                                            com.repforge.app.healthreadings.RecoverySource.TRAINPILOT_ESTIMATE -> "TrainPilot-becslés (${rec.displayScoreHu})"
                                            else -> "Nincs igazolt regenerációs adat"
                                        }
                                        listOf(
                                            tr(uiState.language, "Regeneráció & Készenlét • $scoreProvHu", "Recovery & Readiness • $scoreProvHu", "Erholung & Bereitschaft • $scoreProvHu", "Recuperare și pregătire • $scoreProvHu"),
                                            rec?.explanationHu ?: tr(uiState.language, "Nincs ellenőrizhető friss regenerációs jel a kiválasztott szolgáltatótól.", "No verified recovery signal from the selected provider.", "Kein bestätigtes Erholungssignal vom ausgewählten Dienst.", "Niciun semnal de recuperare verificat de la furnizorul selectat."),
                                            tr(uiState.language, "TrainPilot Coach megnyitása >", "Open TrainPilot Coach >", "TrainPilot Coach öffnen >", "Deschide TrainPilot Coach >")
                                        ) to { onNavigateToSubScreen(SubScreen.COACH_DETAIL) }
                                    }
                                    else -> {
                                        val hasSleep = currentDaily && "sleepMinutes" in uiState.dailyHealthAvailableFields && uiState.sleepDurationLabel != "—"
                                        val stageInfo = if (!hasSleep) {
                                            tr(uiState.language, "Ma még nem érkezett alvásadat a kiválasztott szolgáltatótól (${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}).",
                                                "No sleep data fetched yet today from ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}.",
                                                "Noch keine Schlafdaten von ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} geladen.",
                                                "Fără date despre somn de la ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} azi.")
                                        } else if (uiState.sleepHasStages) {
                                            uiState.sleepStagesSummaryHu
                                        } else {
                                            tr(uiState.language, "Alvásfázisok: a szolgáltató nem adott át fázisbontást (csak teljes időtartam: ${uiState.sleepDurationLabel}). Nem generálunk fiktív fázisokat.",
                                                "Sleep stages: the provider did not supply stage breakdown (total duration only: ${uiState.sleepDurationLabel}). Fabricated stages are not generated.",
                                                "Schlafphasen: der Dienst hat keine Phasenaufteilung geliefert (nur Gesamtdauer: ${uiState.sleepDurationLabel}). Keine fiktiven Phasen.",
                                                "Faze somn: furnizorul nu a transmis detalii de fază (doar durata totală: ${uiState.sleepDurationLabel}).")
                                        }
                                        listOf(
                                            tr(uiState.language, "Alvás részletek • ${uiState.sleepDurationLabel} (${uiState.sleepHoursDecimal})", "Sleep Details • ${uiState.sleepDurationLabel} (${uiState.sleepHoursDecimal})", "Schlaf Details • ${uiState.sleepDurationLabel} (${uiState.sleepHoursDecimal})", "Detalii somn • ${uiState.sleepDurationLabel} (${uiState.sleepHoursDecimal})"),
                                            stageInfo,
                                            tr(uiState.language, "Egészségnapló megnyitása lent", "Open Health Journal below", "Gesundheitstagebuch unten öffnen", "Deschide jurnalul de sănătate mai jos")
                                        ) to openHealthJournalAndScroll
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = titleStr[0],
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.clickable { expandedTodayTileKey = null }
                                    ) {
                                        Text(
                                            text = strings.closeAction,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textSecondary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ExpandLess,
                                            contentDescription = null,
                                            tint = palette.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (activeKey != "pulse") Text(
                                    text = titleStr[1],
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary
                                )
                                if (activeKey == "pulse") {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = palette.background.copy(alpha = 0.55f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .trainPilotBorder(
                                                width = 1.dp,
                                                color = if (uiState.expandedHealthPulseTrend) palette.primary.copy(alpha = 0.55f) else palette.cardBorder,
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onToggleHealthSection("pulse") }
                                                    .testTag("health_pulse_trend_header"),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFEF4444).copy(alpha = 0.16f),
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Favorite,
                                                            contentDescription = null,
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = tr(uiState.language, "24 órás pulzustrend", "24h Heart Rate Trend", "24-Stunden-Pulstrend", "Tendință puls 24h"),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = palette.textPrimary
                                                    )
                                                    val restLbl = if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—"
                                                    val peakLbl = if (uiState.maxHrBpm > 0) "${uiState.maxHrBpm} bpm" else "—"
                                                    Text(
                                                        text = tr(uiState.language, "Nyugalmi: $restLbl • Csúcs: $peakLbl", "Resting: $restLbl • Peak: $peakLbl", "Ruhe: $restLbl • Spitze: $peakLbl", "Repaus: $restLbl • Vârf: $peakLbl"),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = palette.textSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = palette.surfaceCard,
                                                    modifier = Modifier.trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = strings.detailsAction,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = palette.primary
                                                        )
                                                        Icon(
                                                            imageVector = if (uiState.expandedHealthPulseTrend) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                            contentDescription = null,
                                                            tint = palette.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Full-width dark sparkline box with red wave curve (Screenshot 9)
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = palette.surfaceCard.copy(alpha = 0.7f),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(54.dp)
                                                    .trainPilotBorder(1.dp, palette.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                                    .clickable { onToggleHealthSection("pulse") }
                                            ) {
                                                if (uiState.dailyPulseHistory.size < 2) {
                                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        Text(
                                                            if (uiState.dailyPulseHistory.isEmpty()) {
                                                                tr(uiState.language, "Nincs lekért pulzus-idősor", "No heart rate time series fetched", "Keine Puls-Zeitreihe geladen", "Fără serie temporală de puls")
                                                            } else {
                                                                tr(uiState.language, "Egy időbélyeges pulzusmérés érhető el", "One timestamped heart rate reading available", "Eine Pulsmessung mit Zeitstempel verfügbar", "O măsurătoare de puls disponibilă")
                                                            },
                                                            color = palette.textSecondary, style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                } else {
                                                    val readings = uiState.dailyPulseHistory
                                                    Canvas(Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 7.dp)
                                                        .testTag("health_pulse_real_trend")) {
                                                        val first = java.time.Instant.parse(readings.first().timeIso).toEpochMilli()
                                                        val last = java.time.Instant.parse(readings.last().timeIso).toEpochMilli()
                                                        val delta = (last - first).coerceAtLeast(1L).toFloat()
                                                        val lowest = readings.minOf { it.bpm }.toFloat()
                                                        val spread = (readings.maxOf { it.bpm } - readings.minOf { it.bpm })
                                                            .toFloat().coerceAtLeast(1f)
                                                        val line = Path()
                                                        readings.forEachIndexed { index, sample ->
                                                            val time = java.time.Instant.parse(sample.timeIso).toEpochMilli()
                                                            val x = ((time - first) / delta).coerceIn(0f, 1f) * size.width
                                                            val y = size.height - (sample.bpm - lowest) / spread * size.height
                                                            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
                                                        }
                                                        drawPath(line, color = Color(0xFFEF4444),
                                                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                                                    }
                                                }
                                            }

                                            if (uiState.expandedHealthPulseTrend) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                HorizontalDivider(color = palette.cardBorder)
                                                Spacer(modifier = Modifier.height(12.dp))

                                                Surface(
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = palette.surfaceCard,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(14.dp),
                                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Text(
                                                            text = tr(uiState.language, "Mai pulzus", "Today's Heart Rate", "Heutiger Puls", "Pulsul de azi"),
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = palette.textPrimary
                                                        )
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            PulseInnerBox(
                                                                label = tr(uiState.language, "Átlagpulzus", "Avg HR", "Ø Puls", "Puls mediu"),
                                                                value = if (uiState.currentHrBpm > 0) "${uiState.currentHrBpm} bpm" else "—",
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            PulseInnerBox(
                                                                label = strings.restingHrTitle,
                                                                value = if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—",
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            PulseInnerBox(
                                                                label = tr(uiState.language, "Max. pulzus", "Max HR", "Max. Puls", "Puls max."),
                                                                value = if (uiState.maxHrBpm > 0) "${uiState.maxHrBpm} bpm" else "—",
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            PulseInnerBox(
                                                                label = tr(uiState.language, "Min. pulzus", "Min HR", "Min. Puls", "Puls min."),
                                                                value = if (uiState.minHrBpm > 0) "${uiState.minHrBpm} bpm" else "—",
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                        Text(
                                                            text = if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = palette.textSecondary
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { onNavigateToSubScreen(SubScreen.COACH_DETAIL) }
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = tr(uiState.language, "Regenerációs előzmények", "Recovery History", "Erholungsverlauf", "Istoric recuperare"),
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = palette.textPrimary
                                                    )
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                        contentDescription = null,
                                                        tint = palette.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (activeKey != "pulse") Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.primary.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onActionClick() }
                                        .testTag("health_expanded_action_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = titleStr[2],
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. SCREENSHOT 9: "Heti egészség & edzés célok" CONTAINER (with Heti lépésszám, Aktív edzésidő, Expandable Weekly Source & 7-Day Breakdown, 24 órás pulzustrend & Testsúlynapló)
        item {
            val today = java.time.LocalDate.now()
            val cachedDays = uiState.weeklyHealthDays.associateBy { it.dateIso }
            val goalDays = (6 downTo 0).map { offset ->
                val date = today.minusDays(offset.toLong())
                val workouts = uiState.loggedWorkouts.filter { it.localWorkoutDate() == date }
                val cached = cachedDays[date.toString()]
                    ?: com.example.ui.viewmodel.WeeklyHealthDaySummary(dateIso = date.toString(), dayLabelHu = date.toString(), steps = 0, activeCaloriesKcal = 0, sleepLabel = "—", workoutMinutes = 0, workoutCount = 0, sourceLabelHu = "Nincs rögzített adat")
                cached.copy(workoutMinutes = workouts.sumOf { it.durationMinutes.coerceAtLeast(0) }, workoutCount = workouts.size)
            }
            val weeklyStepsTarget = (uiState.stepsGoal.coerceAtLeast(1) * 7)
            val weeklyStepsCurrent = goalDays.filter { it.stepsAvailable }.sumOf { it.steps }
            val weeklyStepsPct = ((weeklyStepsCurrent.toFloat() / weeklyStepsTarget.toFloat()) * 100f).toInt().coerceIn(0, 100)
            val weeklyMinutesTarget = (uiState.userScheduleDaysPerWeek * uiState.userWorkoutDurationMin).coerceAtLeast(150)
            val weeklyMinutesCurrent = goalDays.sumOf { it.workoutMinutes }
            val weeklyMinutesPct = ((weeklyMinutesCurrent.toFloat() / weeklyMinutesTarget.toFloat()) * 100f).toInt().coerceIn(0, 100)
            val primaryProviderName = if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.2.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                    .testTag("health_weekly_status_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
                ) {
                    // Header row: "Heti egészség & edzés célok" + "🎯 Célok" pill button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onToggleHealthSection("weekly") }
                        ) {
                            Text(
                                text = if (uiState.workoutGoalPeriod == com.example.model.WorkoutGoalPeriod.MONTHLY) {
                                    tr(uiState.language, "Egészség & edzéscélok", "Health & Workout Goals", "Gesundheits- & Trainingsziele", "Obiective sănătate și antrenament")
                                } else {
                                    tr(uiState.language, "Heti egészség & edzés célok", "Weekly Health & Workout Goals", "Wöchentliche Gesundheits- & Trainingsziele", "Obiective săptămânale sănătate și antrenament")
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tr(
                                    uiState.language,
                                    "$primaryProviderName & Room edzésnapló (Koppints a forrásokhoz)",
                                    "$primaryProviderName & Room workout log (Tap for sources)",
                                    "$primaryProviderName & Room-Trainingsprotokoll (Tippen für Quellen)",
                                    "$primaryProviderName și jurnal Room (Atinge pentru surse)"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = palette.background.copy(alpha = 0.65f),
                            modifier = Modifier
                                .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenGoalsDialog() }
                                .testTag("health_weekly_goals_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🎯", fontSize = 13.sp)
                                Text(
                                    text = tr(uiState.language, "Célok", "Goals", "Ziele", "Obiective"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary
                                )
                            }
                        }
                    }

                    val minUnit = tr(uiState.language, "perc", "min", "Min.", "min")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (steps in listOf(true, false)) {
                            val expanded = if (steps) uiState.expandedWeeklySteps else uiState.expandedWeeklyWorkouts
                            val tag = if (steps) "steps" else "minutes"
                            Surface(shape = RoundedCornerShape(18.dp), color = palette.background.copy(alpha = 0.55f),
                                modifier = Modifier.weight(1f).trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onToggleHealthSection(if (steps) "weekly_steps" else "weekly_workouts") }
                                    .testTag("health_weekly_${tag}_card")) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            if (steps) tr(uiState.language, "Heti lépésszám", "Weekly Steps", "Wöchentliche Schritte", "Pași săptămânali")
                                            else tr(uiState.language, "Aktív edzésidő", "Active Workout Time", "Aktive Trainingszeit", "Timp activ antrenament"),
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelLarge, color = palette.textPrimary
                                        )
                                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = palette.primary)
                                    }
                                    Text(if (steps) { if (goalDays.none { it.stepsAvailable }) "—" else "%,d".format(java.util.Locale.US, weeklyStepsCurrent).replace(',', ' ') }
                                        else "$weeklyMinutesCurrent $minUnit", style = MaterialTheme.typography.headlineMedium,
                                        color = palette.textPrimary, modifier = Modifier.testTag("health_weekly_${tag}_total"))
                                    Text(
                                        if (steps) strings.stepsGoalWithCount(weeklyStepsTarget)
                                        else "${strings.stepsGoalWithCount(weeklyMinutesTarget)} $minUnit",
                                        style = MaterialTheme.typography.labelSmall, color = palette.textSecondary
                                    )
                                    LinearProgressIndicator(progress = { (if (steps) weeklyStepsPct else weeklyMinutesPct) / 100f },
                                        modifier = Modifier.fillMaxWidth(), color = palette.primary, trackColor = palette.surfaceElevated)
                                    if (steps) Text(
                                        tr(
                                            uiState.language,
                                            "${goalDays.count { it.stepsAvailable }}/7 napi adat • $primaryProviderName",
                                            "${goalDays.count { it.stepsAvailable }}/7 daily records • $primaryProviderName",
                                            "${goalDays.count { it.stepsAvailable }}/7 Tagesdaten • $primaryProviderName",
                                            "${goalDays.count { it.stepsAvailable }}/7 zile cu date • $primaryProviderName"
                                        ),
                                        style = MaterialTheme.typography.labelSmall, color = palette.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Details reserve a full-width row beneath both summary cards.
                    for (steps in listOf(true, false)) {
                        val expanded = if (steps) uiState.expandedWeeklySteps else uiState.expandedWeeklyWorkouts
                        val tag = if (steps) "steps" else "minutes"
                        AnimatedVisibility(visible = expanded) {
                            Surface(shape = RoundedCornerShape(18.dp), color = palette.surfaceElevated,
                                modifier = Modifier.fillMaxWidth().testTag("health_weekly_${tag}_days")
                                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        if (steps) tr(uiState.language, "Heti lépésszám • napi bontás", "Weekly Steps • Daily Breakdown", "Wöchentliche Schritte • Tagesübersicht", "Pași săptămânali • Detaliere zilnică")
                                        else tr(uiState.language, "Aktív edzésidő • napi bontás", "Active Workout Time • Daily Breakdown", "Aktive Trainingszeit • Tagesübersicht", "Timp activ • Detaliere zilnică"),
                                        style = MaterialTheme.typography.titleMedium, color = palette.textPrimary
                                    )
                                    for (day in goalDays) {
                                        val dayWorkouts = if (steps) emptyList() else
                                            uiState.loggedWorkouts.filter { it.localWorkoutDate()?.toString() == day.dateIso }
                                        HorizontalDivider(color = palette.cardBorder)
                                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                Text(day.dayLabelHu, style = MaterialTheme.typography.labelLarge,
                                                    color = palette.textPrimary)
                                                Text(if (steps) day.stepsSourceLabelHu else
                                                    if (dayWorkouts.isEmpty()) tr(uiState.language, "Room edzésnapló • nincs mai edzés", "Room workout log • no workout", "Room-Protokoll • kein Training", "Jurnal Room • fără antrenament")
                                                    else "Room • " + dayWorkouts.joinToString(" · ") {
                                                        "${it.titleHu}: ${it.durationMinutes} $minUnit"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textSecondary, maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis)
                                            }
                                            Text(
                                                if (steps) {
                                                    if (day.stepsAvailable) "${day.steps} ${tr(uiState.language, "lépés", "steps", "Schritte", "pași")}" else "—"
                                                } else {
                                                    "${day.workoutMinutes} $minUnit • ${day.workoutCount} ${tr(uiState.language, "edzés", "workouts", "Trainings", "antr.")}"
                                                },
                                                modifier = Modifier.testTag("health_weekly_${tag}_${day.dateIso}"),
                                                style = MaterialTheme.typography.bodySmall, color = palette.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Expandable Inner Card 2 inside Weekly Section: "Testsúlynapló & Trend" (Screenshot 9 style + full expanded details)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = palette.background.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .trainPilotBorder(
                                width = 1.dp,
                                color = if (uiState.expandedHealthWeightLog) palette.primary.copy(alpha = 0.55f) else palette.cardBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleHealthSection("weight") }
                                    .testTag("health_weight_log_header"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = palette.primary.copy(alpha = 0.16f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MonitorWeight,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr(uiState.language, "Testsúlynapló & Trend", "Weight Log & Trend", "Gewichtsprotokoll & Trend", "Jurnal greutate și tendință"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val latestWeightLabel = if (latestChronologicalEntry != null) {
                                        "${tr(uiState.language, "Legújabb", "Latest", "Neueste", "Ultima")} (${latestChronologicalEntry.dateLabel}): ${latestChronologicalEntry.weightLabel}"
                                    } else if (uiState.currentBodyWeightKg > 0f) {
                                        "${tr(uiState.language, "Legújabb", "Latest", "Neueste", "Ultima")}: ${"%.1f".format(uiState.currentBodyWeightKg).replace('.', ',')} kg"
                                    } else {
                                        tr(uiState.language, "Legújabb: nincs rögzített mérés", "Latest: no recorded measurement", "Neueste: keine Messung erfasst", "Ultima: nicio măsurătoare înregistrată")
                                    }
                                    Text(
                                        text = latestWeightLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = palette.surfaceCard,
                                    modifier = Modifier.trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = strings.detailsAction,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.primary
                                        )
                                        Icon(
                                            imageVector = if (uiState.expandedHealthWeightLog) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Full-width trend box with curve AND user-entered X-axis dates
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.surfaceCard.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .trainPilotBorder(1.dp, palette.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                    .clickable { onToggleHealthSection("weight") }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    val lineColor = palette.primary
                                    if (chronologicalMeasurements.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = tr(uiState.language, "Még nincs rögzített testsúlymérés.", "No weight entries recorded yet.", "Noch keine Gewichtsmessung erfasst.", "Nicio măsurătoare de greutate înregistrată."),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.textSecondary
                                            )
                                        }
                                    } else {
                                        val pts = chronologicalMeasurements.map { it.weightValue }
                                        Canvas(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                        ) {
                                            val minW = (pts.minOrNull() ?: 0f) - 0.5f
                                            val maxW = (pts.maxOrNull() ?: 1f) + 0.5f
                                            val offsets = pts.mapIndexed { idx, w ->
                                                val x = if (pts.size > 1) {
                                                    (idx.toFloat() / (pts.size - 1).coerceAtLeast(1)) * size.width
                                                } else {
                                                    size.width / 2f
                                                }
                                                val y = size.height - ((w - minW) / (maxW - minW).coerceAtLeast(0.1f)) * size.height
                                                Offset(x, y)
                                            }
                                            val path = Path()
                                            offsets.forEachIndexed { i, pt ->
                                                if (i == 0) path.moveTo(pt.x, pt.y)
                                                else {
                                                    val prev = offsets[i - 1]
                                                    val cx = (prev.x + pt.x) / 2f
                                                    path.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                                                }
                                            }
                                            drawPath(path, lineColor, style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round))
                                            offsets.forEach { pt ->
                                                drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = pt)
                                            }
                                        }
                                    }
                                    if (chronologicalMeasurements.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            chronologicalMeasurements.takeLast(5).forEach { entry ->
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = entry.parsedDate.format(shortChartDateFormatter),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.textSecondary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = "${"%.1f".format(entry.weightValue).replace('.', ',')}kg",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.expandedHealthWeightLog) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = palette.cardBorder)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Sub-card: Test és fittség (Testzsír, SpO2, VO2max)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = palette.surfaceCard,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = strings.bodyAndFitnessTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.primary
                                            )
                                            Text(
                                                text = tr(uiState.language, "Testzsír, SpO₂, VO₂max", "Body fat, SpO₂, VO₂max", "Körperfett, SpO₂, VO₂max", "Grăsime corporală, SpO₂, VO₂max"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.textSecondary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = palette.background.copy(alpha = 0.45f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = tr(uiState.language, "Testsúly (legutóbbi dátum szerint)", "Body weight (by latest date)", "Körpergewicht (nach letztem Datum)", "Greutate (după ultima dată)"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textSecondary
                                                )
                                                Text(
                                                    text = latestChronologicalEntry?.weightLabel
                                                        ?: if (uiState.currentBodyWeightKg > 0f) "${uiState.currentBodyWeightKg} kg" else "—",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = palette.textPrimary
                                                )
                                                Text(
                                                    text = "${tr(uiState.language, "Rögzített dátum", "Recorded date", "Erfasstes Datum", "Data înregistrată")}: ${latestChronologicalEntry?.dateLabel ?: todayHuLabel}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = palette.textSecondary
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            PulseInnerBox(
                                                label = tr(uiState.language, "Testzsír", "Body fat", "Körperfett", "Grăsime"),
                                                value = if (uiState.bodyFatPercent > 0f) "${uiState.bodyFatPercent} %" else "—",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PulseInnerBox(
                                                label = "SpO₂",
                                                value = if (uiState.spo2Percent > 0f) "${uiState.spo2Percent} %" else "—",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PulseInnerBox(
                                                label = "VO₂max",
                                                value = if (uiState.vo2Max > 0f) "${uiState.vo2Max}" else "—",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Full-width "+ Új mérés >" card
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = palette.surfaceCard,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                                        .clickable { onToggleAddWeightDialog(true) }
                                        .testTag("health_add_weight_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.newMeasurementAction,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.primary
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = palette.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val wRanges = listOf("7d", "30d", "3m", "1y", "All")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    wRanges.forEachIndexed { idx, r ->
                                        val sel = uiState.selectedWeightRangeIndex == idx
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (sel) palette.primary.copy(alpha = 0.25f) else palette.surfaceCard,
                                            modifier = Modifier
                                                .trainPilotBorder(
                                                    width = 1.dp,
                                                    color = if (sel) palette.primary else palette.cardBorder,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onSelectWeightRange(idx) }
                                        ) {
                                            Text(
                                                text = r,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (sel) palette.primary else palette.textPrimary,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (recentMeasurements.isEmpty()) {
                                    Text(
                                        text = strings.notEnoughRecentData,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    Text(
                                        text = strings.recentMeasurementsTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        recentMeasurements.forEach { entry ->
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = palette.surfaceCard,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = entry.weightLabel,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = palette.textPrimary
                                                        )
                                                        Text(
                                                            text = entry.dateLabel,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = palette.textSecondary
                                                        )
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Surface(
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = palette.primary.copy(alpha = 0.12f),
                                                            modifier = Modifier
                                                                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .clickable { editingMeasurement = entry }
                                                        ) {
                                                            Text(
                                                                text = strings.editAction,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = palette.textPrimary,
                                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                                            )
                                                        }
                                                        Surface(
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = Color(0xFFEF4444).copy(alpha = 0.14f),
                                                            modifier = Modifier
                                                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .clickable {
                                                                    recentMeasurements = recentMeasurements.filterNot { it.id == entry.id }
                                                                    onDeleteWeightLog(entry.id)
                                                                }
                                                        ) {
                                                            Text(
                                                                text = strings.deleteAction,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = palette.textPrimary,
                                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // One journal: the selected provider/day projection supplies summaries and inline details.
        item {
            ExpandableHealthSectionCard(
                icon = Icons.Default.Favorite,
                title = strings.healthJournalTitle,
                subtitle = null,
                expanded = uiState.expandedHealthJournal,
                onToggle = { onToggleHealthSection("journal") }
            ) {
                com.example.ui.components.StudioHealthMetricsBrowser(
                    initialProvider = if (uiState.primaryHealthSourceSamsung) com.repforge.app.HealthJournalStore.SAMSUNG
                        else com.repforge.app.HealthJournalStore.HC,
                    language = uiState.language.name.lowercase(),
                    refreshKey = "${uiState.healthRefreshRevision}:${uiState.healthSyncBusy}",
                    onRefreshProvider = onReadHealthSource,
                    refreshBusy = uiState.healthSyncBusy,
                    refreshError = uiState.healthSyncMessage.takeIf { uiState.healthSyncFailed })
            }
        }

        // 4. Expandable Section: Health Connect (Screenshot 1 + custom Health Connect badge)
        item {
            ExpandableHealthSectionCard(
                icon = Icons.Default.Link,
                title = tr(uiState.language, "Egészségadat-források", "Health Data Sources", "Gesundheitsdatenquellen", "Surse date de sănătate"),
                subtitle = tr(uiState.language, "Adatforrások, engedélyek és szinkron", "Data sources, permissions & sync", "Datenquellen, Berechtigungen & Sync", "Surse de date, permisiuni și sincronizare"),
                expanded = uiState.expandedHealthConnect,
                onToggle = { onToggleHealthSection("connect") },
                customLeadingBadge = {
                    MockupHealthConnectBadge(size = 44.dp)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surfaceElevated,
                        modifier = Modifier
                            .weight(1f)
                            .trainPilotBorder(
                                width = if (uiState.primaryHealthSourceSamsung) 1.5.dp else 1.dp,
                                color = if (uiState.primaryHealthSourceSamsung) palette.primary else palette.cardBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectHealthSource(true) }
                            .testTag("health_provider_samsung")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Samsung Health", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text(
                                if (uiState.samsungGrantedCount < 0) tr(uiState.language, "Engedély még nem igazolt", "Permission not verified yet", "Berechtigung noch nicht bestätigt", "Permisiune încă neverificată")
                                else "${uiState.samsungGrantedCount}/${uiState.samsungPermissionCount} ${tr(uiState.language, "adatkör", "scopes", "Bereiche", "domenii")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.samsungGrantedCount > 0) palette.success else palette.textSecondary
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surfaceElevated,
                        modifier = Modifier
                            .weight(1f)
                            .trainPilotBorder(
                                width = if (!uiState.primaryHealthSourceSamsung) 1.5.dp else 1.dp,
                                color = if (!uiState.primaryHealthSourceSamsung) palette.primary else palette.cardBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectHealthSource(false) }
                            .testTag("health_provider_connect")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Health Connect", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text(
                                "● ${uiState.healthGrantedCount}/${uiState.healthPermissionCount} ${tr(uiState.language, "engedély", "permissions", "Berechtigungen", "permisiuni")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.healthGrantedCount > 0) palette.success else palette.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // 4b. Direct Óra és szinkron (Wear OS) card on Egészség tab so it's 1-tap reachable
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onNavigateToSubScreen(SubScreen.WATCH_AND_SYNC) }
                    .testTag("health_open_watch_sync_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MockupWatchGraphicBadge(size = 46.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        val wearTitle = tr(uiState.language, "TrainPilot Wear OS – edzésmérések", "TrainPilot Wear OS – workout measurements",
                            "TrainPilot Wear OS – Trainingsmessungen", "TrainPilot Wear OS – măsurători antrenament")
                        val watchTitle = if (uiState.watchAppAvailable) "$wearTitle (${uiState.watchName})" else wearTitle
                        Text(
                            text = watchTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = when {
                                uiState.watchAppAvailable -> tr(uiState.language, "● Óra csatlakoztatva", "● Watch connected", "● Uhr verbunden", "● Ceas conectat")
                                uiState.watchConnected -> tr(uiState.language, "Wear OS rendszerkapcsolat • TrainPilot óraapp nincs telepítve", "Wear OS system connection • TrainPilot watch app not installed", "Wear OS-Systemverbindung • TrainPilot-Uhren-App nicht installiert", "Conexiune sistem Wear OS • Aplicația de ceas TrainPilot nu este instalată")
                                else -> tr(uiState.language, "Nincs csatlakoztatott óra", "No watch connected", "Keine Uhr verbunden", "Niciun ceas conectat")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.watchAppAvailable) palette.success else palette.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.primary
                    )
                }
            }
        }

        // 5. Bottom Action Row: Szinkronizálás & Engedélyek + Sync Status Footer (Screenshot 1)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.primary.copy(alpha = 0.42f),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = !uiState.healthSyncBusy) { onTriggerHealthSync() }
                            .testTag("health_sync_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (uiState.healthSyncBusy) tr(uiState.language, "Szinkronizálás…", "Syncing…", "Synchronisiere…", "Se sincronizează…") else strings.syncAction,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surfaceCard,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onRequestHealthPermissions() }
                            .testTag("health_open_permissions_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = strings.permissionsAction,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        }
                    }
                }

                listOf(com.repforge.app.HealthJournalStore.SAMSUNG, com.repforge.app.HealthJournalStore.HC).forEach { provider ->
                    val lastSuccess = uiState.healthLastSyncByProvider[provider]
                    val time = runCatching {
                        java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd. HH:mm")
                            .withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.parse(lastSuccess))
                    }.getOrDefault(tr(uiState.language, "Még nincs sikeres szinkron", "No successful sync yet", "Noch kein erfolgreicher Sync", "Nicio sincronizare reușită încă"))
                    Text("${com.repforge.app.NativeWorkoutHealthSource.label(provider)} • ${tr(uiState.language, "Utolsó sikeres szinkron", "Last successful sync", "Letzter erfolgreicher Sync", "Ultima sincronizare reușită")}: $time",
                        style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                        modifier = Modifier.testTag("health_last_sync_$provider"))
                }
                if (uiState.healthSyncMessage.isNotBlank()) Text(uiState.healthSyncMessage,
                    color = if (uiState.healthSyncFailed) palette.danger else if (uiState.healthSyncPartial) palette.warning else palette.textPrimary,
                    modifier = Modifier.testTag("health_sync_result"))
                TrainPilotOutlinedButton(onClick = { syncDetailsExpanded = !syncDetailsExpanded },
                    modifier = Modifier.fillMaxWidth().testTag("health_sync_details_toggle")) {
                    Text(tr(uiState.language, "Szinkron részletei", "Sync details", "Sync-Details", "Detalii sincronizare"),
                        modifier = Modifier.weight(1f))
                    Icon(if (syncDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
                if (syncDetailsExpanded) {
                    Text(
                        if (uiState.healthAutoOnOpen) tr(uiState.language,
                            "Automatikus frissítés appnyitáskor és előtérben 5 percenként.",
                            "Automatic refresh on app opening and every 5 minutes in the foreground.",
                            "Automatische Aktualisierung beim App-Start und im Vordergrund alle 5 Minuten.",
                            "Actualizare automată la deschiderea aplicației și la fiecare 5 minute în prim-plan.")
                        else tr(uiState.language, "Automatikus frissítés kikapcsolva.", "Automatic refresh is off.", "Automatische Aktualisierung deaktiviert.", "Actualizarea automată este oprită."),
                        style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                        modifier = Modifier.testTag("health_auto_refresh_status"))
                    Text(
                        when {
                            uiState.healthBackgroundOptedIn && uiState.healthBackgroundScheduled -> tr(uiState.language,
                                "Health Connect háttérfrissítés: kb. 15 percenként; az Android késleltetheti.",
                                "Health Connect background refresh: about every 15 minutes; Android may delay it.",
                                "Health Connect im Hintergrund: etwa alle 15 Minuten; Android kann dies verzögern.",
                                "Health Connect în fundal: aproximativ la fiecare 15 minute; Android poate întârzia actualizarea.")
                            uiState.healthBackgroundOptedIn -> tr(uiState.language, "Health Connect háttérfrissítés nincs ütemezve; ellenőrizd az engedélyeket.", "Health Connect background refresh is not scheduled; check permissions.", "Health Connect-Hintergrundaktualisierung nicht geplant; Berechtigungen prüfen.", "Actualizarea Health Connect în fundal nu este programată; verifică permisiunile.")
                            else -> tr(uiState.language, "Health Connect háttérfrissítés kikapcsolva.", "Health Connect background refresh is off.", "Health Connect-Hintergrundaktualisierung deaktiviert.", "Actualizarea Health Connect în fundal este oprită.")
                        }, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                        modifier = Modifier.testTag("health_background_refresh_status"))
                    Text("${tr(uiState.language, "Utolsó adatolvasás", "Last data read", "Letzter Datenabruf", "Ultima citire date")}: ${uiState.lastHealthReadLabel}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    Text(
                        text = uiState.healthProviderStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}
}

@Composable
internal fun MainTodayStatusTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.background.copy(alpha = 0.55f),
        modifier = modifier
            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 5.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PulseInnerBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.background.copy(alpha = 0.45f),
        modifier = modifier
            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ExpandableHealthSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    customLeadingBadge: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotSoftGlow(palette.primary, RoundedCornerShape(20.dp), 3.dp)
            .trainPilotBorder(1.dp, if (expanded) palette.primary.copy(alpha = 0.6f) else palette.cardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (customLeadingBadge != null) {
                    customLeadingBadge()
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.primary.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(44.dp)
                            .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = palette.primary
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = palette.cardBorder)
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun SummarySmallPill(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surfaceElevated,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }
    }
}

/**
 * 6. COACH (Mockup 3/4 #6 + main TrainPilot Coach)
 */
@Composable
internal fun CoachDetailScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartSuggestedWorkout: () -> Unit,
    onOpenExerciseDetail: (String) -> Unit,
    onStartCoachTarget: (String, String, String?) -> Unit = { _, _, _ -> onStartSuggestedWorkout() },
    onOpenWorkout: () -> Unit = {},
    onCoachProgramChanged: () -> Unit = {},
    model: com.repforge.app.ui.coach.CoachViewModel = androidx.lifecycle.viewmodel.compose.viewModel(key = "studio-canonical-coach")
) {
    val palette = LocalTrainPilotPalette.current
    val nativeState by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(nativeState.variationSaved) { if (nativeState.variationSaved) onCoachProgramChanged() }
    val readiness = nativeState.readiness
    val hasScore = readiness?.score != null
    val displayScore = readiness?.score?.toInt()?.coerceIn(0, 100) ?: 0
    val samsungScore = readiness?.samsungScore?.toInt()?.coerceIn(0, 100) ?: 0
    val ownReadinessScore = readiness?.trainpilotScore ?: 0
    var coachInputsObserved by remember(model) { mutableStateOf(false) }
    LaunchedEffect(uiState.loggedWorkouts, uiState.programs, uiState.dailyHealthLastSyncIso, uiState.primaryHealthSourceSamsung, uiState.language) {
        val firstAttachment = !coachInputsObserved
        coachInputsObserved = true
        // The model already starts its first read. Do not restart it on initial composition.
        if (firstAttachment && nativeState.initialLoading) return@LaunchedEffect
        model.reload()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("coach_detail_screen"),
        contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        if (uiState.currentSubScreen == SubScreen.COACH_DETAIL) {
            item {
                SubScreenTopBar(
                    title = "TrainPilot Coach",
                    onBack = onBack,
                    onOpenSettings = onOpenSettings
                )
            }
        }

        // Screenshot 4: Mai felkészültség (Arc gauge + 3 distinct D10 readiness indicators)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(uiState.language, "Mai felkészültség", "Today's Readiness", "Heutige Bereitschaft", "Pregătirea de azi"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        val statusBadgeText = when {
                            displayScore >= 75 -> tr(uiState.language, "OPTIMÁLIS KÉSZENLÉT", "OPTIMAL READINESS", "OPTIMALE BEREITSCHAFT", "PREGĂTIRE OPTIMĂ")
                            displayScore >= 50 -> tr(uiState.language, "MÉRSÉKELT KÉSZENLÉT", "MODERATE READINESS", "MÄSSIGE BEREITSCHAFT", "PREGĂTIRE MODERATĂ")
                            hasScore -> tr(uiState.language, "PIHENÉS JAVASOLT", "REST ADVISED", "PAUSE EMPFOHLEN", "ODIHNĂ RECOMANDATĂ")
                            else -> tr(uiState.language, "NINCS MÉRT ADAT", "NO MEASURED DATA", "KEINE MESSDATEN", "FĂRĂ DATE MĂSURATE")
                        }
                        val statusBadgeColor = when {
                            displayScore >= 75 -> palette.success
                            displayScore >= 50 -> palette.primary
                            hasScore -> palette.warning
                            else -> palette.textSecondary
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusBadgeColor.copy(alpha = 0.16f),
                            modifier = Modifier.border(1.dp, statusBadgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = statusBadgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusBadgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val trackCol = palette.surfaceElevated
                            val ringCol = if (hasScore) palette.success else palette.cardBorder
                            Canvas(modifier = Modifier.size(98.dp)) {
                                drawArc(
                                    color = trackCol,
                                    startAngle = 135f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                )
                                if (hasScore) {
                                    drawArc(
                                        color = ringCol,
                                        startAngle = 135f,
                                        sweepAngle = 270f * (displayScore / 100f),
                                        useCenter = false,
                                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (hasScore) "$displayScore" else "—",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "/ 100",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = tr(uiState.language, "3. Terheléssel korlátozott végső Coach készenlét", "3. Load-Adjusted Final Coach Readiness", "3. Belastungsbereinigte Coach-Bereitschaft", "3. Pregătire finală Coach ajustată la sarcină"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                            Text(
                                text = com.example.ui.components.studioCoachReadinessText(readiness, uiState.language),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(tr(uiState.language, "1. Mai Samsung...", "1. Samsung Energy", "1. Samsung Energie", "1. Energie Samsung"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1)
                                Text(
                                    text = if (readiness?.samsungScore != null) "$samsungScore / 100" else "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = if (readiness?.samsungScore != null) "Samsung Health" else tr(uiState.language, "Nincs adat", "No data", "Keine Daten", "Fără date"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (readiness?.samsungScore != null) palette.success else palette.textSecondary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(tr(uiState.language, "2. TrainPilot saját...", "2. TrainPilot Own", "2. TrainPilot Eigen", "2. Scor TrainPilot"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1)
                                Text(
                                    text = if (readiness?.trainpilotScore != null) "$ownReadinessScore / 100" else "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(tr(uiState.language, "HRV & alvás", "HRV & sleep", "HRV & Schlaf", "HRV și somn"), style = MaterialTheme.typography.labelSmall, color = palette.primary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(tr(uiState.language, "3. Terheléssel...", "3. Load-Adjusted", "3. Mit Belastung", "3. Cu sarcină"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1)
                                Text(
                                    text = if (hasScore) "$displayScore / 100" else "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary
                                )
                                Text(tr(uiState.language, "Végső Coach", "Final Coach", "Finaler Coach", "Coach final"), style = MaterialTheme.typography.labelSmall, color = palette.primary)
                            }
                        }
                    }
                }
            }
        }

        // Put the next actionable day immediately after readiness, before supplementary signals.
        item {
            com.example.ui.components.StudioCoachNextTrainingCard(
                nativeState, onStartCoachTarget, onOpenWorkout
            )
        }

        // Kulcsjelek Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr(uiState.language, "Kulcsjelek", "Key Signals", "Schlüsselsignale", "Semnale cheie"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text(when (nativeState.provider) {
                            com.repforge.app.HealthJournalStore.SAMSUNG -> "Samsung Health"
                            com.repforge.app.HealthJournalStore.HC -> "Health Connect"
                            else -> tr(uiState.language, "Adatok betöltése…", "Loading data…", "Daten werden geladen…", "Se încarcă datele…")
                        }, style = MaterialTheme.typography.labelMedium, color = palette.primary)
                    }
                    CoachSignalRow(
                        strings.sleepTitle,
                        readiness?.sleepMinutes?.let {
                            val hUnit = tr(uiState.language, "ó", "h", "Std.", "h")
                            val mUnit = tr(uiState.language, "p", "m", "Min.", "m")
                            "${it.toInt() / 60} $hUnit ${it.toInt() % 60} $mUnit"
                        } ?: "—",
                        palette.success
                    )
                    CoachSignalRow(strings.restingHrTitle, uiState.todayPulseReading?.takeIf { it.provider == nativeState.provider }?.restingBpm?.let { "${it.toInt()} bpm" } ?: "—", palette.success)
                    CoachSignalRow("HRV (RMSSD)", readiness?.hrvMs?.let { "${it.toInt()} ms" } ?: "—", palette.primary)
                    CoachSignalRow(tr(uiState.language, "Rögzített edzések", "Logged Workouts", "Erfasste Trainings", "Antrenamente înregistrate"), "${uiState.loggedWorkouts.size} ${tr(uiState.language, "db", "", "Stk.", "")}".trim(), palette.primary)
                }
            }
        }

        item {
            com.example.ui.components.StudioCoachRecommendations(nativeState,
                onStartCoachTarget, onOpenWorkout, onOpenExerciseDetail,
                model::previewVariation, model::closeVariation, model::saveVariation,
                showTarget = false)
        }
    }
}

@Composable
private fun CoachSignalRow(label: String, value: String, dotColor: Color) {
    val palette = LocalTrainPilotPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = palette.textSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
private fun CoachExerciseAdviceRow(
    name: String,
    advice: String,
    dotColor: Color,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                Spacer(modifier = Modifier.width(6.dp))
                Text(advice, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.textSecondary)
            }
        }
    }
}

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
    onCloudSyncCompleted: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val currentPaletteObj = TrainPilotPalettes.getById(uiState.selectedPaletteId)

    var expandedMainThemesSection by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var expandedMatteThemes by remember { mutableStateOf(false) }
    var expandedContrastThemes by remember { mutableStateOf(false) }
    var expandedGlowThemes by remember { mutableStateOf(false) }
    var expandedLightThemes by remember { mutableStateOf(false) }
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
                .background(palette.background)
                .testTag("settings_main_screen"),
            contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 6.dp),
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
                tr(uiState.language, "Koppints a 4 téma stílus becsukásához", "Tap to collapse 4 theme categories", "Tippen zum Einklappen der 4 Themenkategorien", "Atinge pentru a restrânge cele 4 categorii de teme")
            } else {
                tr(uiState.language, "Koppints a 4 téma stílus kinyitásához", "Tap to expand 4 theme categories", "Tippen zum Ausklappen der 4 Themenkategorien", "Atinge pentru a extinde cele 4 categorii de teme")
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier.fillMaxWidth()
                    .trainPilotBorder(TrainPilotFrames.page(palette.sectionBorder, palette.primary), RoundedCornerShape(18.dp))
                    .testTag("settings_themes_group")
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                            expandedMainThemesSection = !expandedMainThemesSection
                        }.testTag("settings_open_palettes_row")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.themes32Title, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text("${currentPaletteObj.localizedName(uiState.language)} • $toggleThemesHint",
                                    style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = palette.primary.copy(alpha = 0.18f)) {
                                Text(text = "#${uiState.selectedPaletteId}", style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold, color = palette.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = if (expandedMainThemesSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null, tint = palette.textSecondary)
                        }
                    }

                    // 4 expandable theme categories inside the shared Settings/Themes frame.
                    if (expandedMainThemesSection) {
                val allPalettes = TrainPilotPalettes.allPalettes
                val categories = listOf(
                    Triple(tr(uiState.language, "Matt témák (Sötét / Prémium matt)", "Matte Themes (Dark / Premium Matte)", "Matte Themen (Dunkel / Premium Matt)", "Teme mate (Întunecat / Mat premium)"), allPalettes.filter { it.id in 1..8 }, expandedMatteThemes) to { expandedMatteThemes = !expandedMatteThemes },
                    Triple(tr(uiState.language, "Kontrasztos témák (Élénk kontraszt)", "High-Contrast Themes (Vivid Contrast)", "Kontrast-Themen (Lebendiger Kontrast)", "Teme cu contrast ridicat"), allPalettes.filter { it.id in 9..16 }, expandedContrastThemes) to { expandedContrastThemes = !expandedContrastThemes },
                    Triple(tr(uiState.language, "Fényes / Glow témák (Neon & Glow)", "Glow Themes (Neon & Glow)", "Glow-Themen (Neon & Glow)", "Teme Glow (Neon & Glow)"), allPalettes.filter { it.id in 17..24 }, expandedGlowThemes) to { expandedGlowThemes = !expandedGlowThemes },
                    Triple(tr(uiState.language, "Világos témák (Nappali / Light)", "Light Themes (Daylight / Light)", "Helle Themen (Tag / Hell)", "Teme luminoase (Zi / Light)"), allPalettes.filter { it.id in 25..32 }, expandedLightThemes) to { expandedLightThemes = !expandedLightThemes }
                )

                categories.forEachIndexed { categoryIndex, entry ->
                    val (catInfo, onToggleCat) = entry
                    val (catTitle, catPalettes, isExpanded) = catInfo
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                            .trainPilotBorder(TrainPilotFrames.panelWidth, palette.cardBorder, RoundedCornerShape(18.dp))
                            .testTag("settings_theme_category_$categoryIndex")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable { onToggleCat() }
                                    .testTag("settings_theme_category_toggle_$categoryIndex"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = catTitle,
                                        modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${catPalettes.size} ${tr(uiState.language, "téma", "themes", "Themen", "teme")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.primary
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = palette.primary
                                    )
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                // 2-column grid of themes in this category
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    catPalettes.chunked(2).forEach { pair ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            pair.forEach { itemPal ->
                                                val isSel = uiState.selectedPaletteId == itemPal.id
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = itemPal.surfaceCard,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .border(
                                                            width = if (isSel) 2.dp else 1.dp,
                                                            color = if (isSel) itemPal.primary else itemPal.cardBorder,
                                                            shape = RoundedCornerShape(14.dp)
                                                        )
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .clickable {
                                                            onSelectPalette(itemPal.id)
                                                            onShowBanner("${tr(uiState.language, "Téma aktiválva", "Theme activated", "Thema aktiviert", "Temă activată")}: ${itemPal.localizedName(uiState.language)}")
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(22.dp)
                                                                .clip(CircleShape)
                                                                .background(itemPal.primary)
                                                                .border(1.dp, itemPal.secondary, CircleShape)
                                                        )
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = itemPal.localizedName(uiState.language),
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = itemPal.textPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "#${"%02d".format(itemPal.id)}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = itemPal.primary
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            if (pair.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
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
                                color = if (selected) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectLanguage(lang) }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(
                                        text = "${lang.flag} ${lang.code}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) palette.onPrimary else palette.textPrimary
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
                shape = RoundedCornerShape(22.dp), color = palette.surfaceCard,
                modifier = Modifier.fillMaxWidth().testTag("settings_workout_group")
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(22.dp))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr(uiState.language, "Edzés és értesítések", "Workout & Notifications", "Training & Benachrichtigungen", "Antrenament și notificări"), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surfaceCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .trainPilotBorder(0.75.dp, palette.cardBorder.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(strings.soundAlertTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                    Text(tr(uiState.language, "Pihenőidő lejártakor", "When rest timer expires", "Nach Ablauf der Pausenzeit", "La expirarea pauzei"), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                                }
                                Switch(checked = uiState.soundAlertEnabled, onCheckedChange = onToggleSoundAlert)
                            }
                            HorizontalDivider(color = palette.cardBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(tr(uiState.language, "Rezgés", "Vibration", "Vibration", "Vibrație"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                    Text(tr(uiState.language, "Haptikus visszajelzés pihenőidő végén", "Haptic feedback at the end of rest", "Haptisches Feedback am Pausenende", "Feedback haptic la finalul pauzei"), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                                }
                                Switch(
                                    checked = uiState.vibrationEnabled,
                                    onCheckedChange = onToggleVibration
                                )
                            }
                        }
                    }
                    SettingsActionRowCard(
                        title = tr(uiState.language, "Edzés beállításai és progresszió", "Workout Settings & Progression", "Trainingseinstellungen & Progression", "Setări antrenament și progresie"),
                        subtitle = "${localizeTrainingStyle(uiState.trainingStyleHu, uiState.language)} • ${localizeProgressionRule(uiState.progressionRuleHu, uiState.language)} • ${strings.restLabel}: ${uiState.defaultRestSec} $secUnit",
                        badgeText = tr(uiState.language, "Szerk.", "Edit", "Bearb.", "Edit."),
                        subtleBorder = true,
                        onClick = { onToggleProgressionModal(true) }
                    )
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
                    uiState.watchAppAvailable -> "${uiState.watchName} • ${tr(uiState.language, "Wear OS adatkapcsolat", "Wear OS data link", "Wear OS-Datenverbindung", "Conexiune date Wear OS")}"
                    uiState.watchConnected -> tr(uiState.language, "Wear OS rendszerkapcsolat • TrainPilot óraapp nincs telepítve", "Wear OS system connection • TrainPilot watch app not installed", "Wear OS-Systemverbindung • TrainPilot-Uhren-App nicht installiert", "Conexiune sistem Wear OS • Aplicația de ceas TrainPilot nu este instalată")
                    else -> tr(uiState.language, "Nincs csatlakoztatott óra", "No watch connected", "Keine Uhr verbunden", "Niciun ceas conectat")
                },
                badgeText = if (uiState.watchAppAvailable) tr(uiState.language, "Adatkapcsolat", "Data link", "Datenlink", "Conexiune") else tr(uiState.language, "Nincs kapcsolat", "Disconnected", "Getrennt", "Deconectat"),
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
                subtitle = tr(uiState.language, "Csak ezen az eszközön tárolva • Nem kerül felhőbe", "Stored on this device only • Never uploaded by default", "Nur auf diesem Gerät gespeichert • Nicht in der Cloud", "Stocate doar pe acest dispozitiv • Nu ajung în cloud implicit"),
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
    }
}
}

@Composable
private fun SettingsActionRowCard(
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
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotBorder(if (subtleBorder) 0.75.dp else 1.25.dp,
                if (subtleBorder) palette.cardBorder.copy(alpha = 0.45f) else palette.sectionBorder,
                RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
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
                    color = palette.primary.copy(alpha = 0.18f)
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

/**
 * 4/4 #2 ÓRA ÉS SZINKRON (Mockup 4/4 #2 teljes, részletes felépítése)
 */
@Composable
fun WatchAndSyncScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleControlMode: () -> Unit,
    onShowBanner: (String) -> Unit,
    onRequestPermission: (String) -> Unit = {},
    onRefreshWatchInfo: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("watch_and_sync_screen"),
        contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        item {
            SubScreenTopBar(
                title = strings.watchAndSyncTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }

        // Section 1: Órakapcsolat (Mockup 4/4 #2)
        item {
            Text(
                text = strings.watchConnectedSub,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }

        item {
            StudioWatchInformationCard(
                data = uiState.watchInfoCardData.copy(loading = uiState.watchInfoLoading || uiState.watchInfoCardData.loading),
                onRefresh = onRefreshWatchInfo
            )
        }

        // Section 2: Aktív edzés vezérlése (Mockup 4/4 #2 side-by-side mode cards + gold button + bullets)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
                ) {
                    Text(
                        text = tr(uiState.language, "Aktív edzés vezérlése", "Active Workout Control", "Steuerung des aktiven Trainings", "Controlul antrenamentului activ"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )

                    // Side-by-side cards: Telefon vezérli vs Óra vezérli
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val phoneSelected = uiState.phoneControlsWorkout && !uiState.watchControlPending
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (phoneSelected) palette.primary.copy(alpha = 0.16f) else palette.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .trainPilotBorder(
                                    width = if (phoneSelected) 1.5.dp else 1.dp,
                                    color = if (phoneSelected) palette.primary else palette.cardBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (!phoneSelected && !uiState.watchControlPending) onToggleControlMode()
                                }
                                .testTag("watch_mode_phone_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr(uiState.language, "📱 Telefon vezérli", "📱 Phone Controls", "📱 Telefon steuert", "📱 Controlat de telefon"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (phoneSelected) palette.primary else palette.textPrimary
                                    )
                                    if (phoneSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = tr(uiState.language, "Óra megjeleníti", "Watch mirrors state", "Uhr zeigt an", "Ceasul afișează"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        val watchSelected = !uiState.phoneControlsWorkout && !uiState.watchControlPending
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (watchSelected) palette.primary.copy(alpha = 0.16f) else palette.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .trainPilotBorder(
                                    width = if (watchSelected) 1.5.dp else 1.dp,
                                    color = if (watchSelected) palette.primary else palette.cardBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (!watchSelected && !uiState.watchControlPending) onToggleControlMode()
                                }
                                .testTag("watch_mode_watch_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr(uiState.language, "⌚ Óra vezérli", "⌚ Watch Controls", "⌚ Uhr steuert", "⌚ Controlat de ceas"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (watchSelected) palette.primary else palette.textPrimary
                                    )
                                    if (watchSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = tr(uiState.language, "Önálló edzés", "Autonomous session", "Eigenständiges Training", "Sesiune autonomă"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onToggleControlMode,
                        enabled = uiState.watchAppAvailable && !uiState.watchControlPending && !uiState.isSavingWorkout,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("watch_handover_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.watchControlPending) tr(uiState.language, "Óra ACK-ra vár…", "Waiting for Watch ACK…", "Warte auf Uhr-ACK…", "Se așteaptă ACK de la ceas…")
                                else if (uiState.phoneControlsWorkout) strings.handoverToWatchAction else strings.reclaimToPhoneAction,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!uiState.watchAppAvailable) Text(
                            tr(
                                uiState.language,
                                "Az órára a párosított TrainPilot Wear OS alkalmazást telepítsd; a Galaxy Wearable önmagában nem elég.",
                                "Install the paired TrainPilot Wear OS app on your watch; Galaxy Wearable alone is not enough.",
                                "Installiere die gekoppelte TrainPilot Wear OS-App auf der Uhr; Galaxy Wearable allein reicht nicht aus.",
                                "Instalează aplicația asociată TrainPilot Wear OS pe ceas; Galaxy Wearable singur nu este suficient."
                            ),
                            style = MaterialTheme.typography.bodySmall, color = palette.warning)
                        Text(
                            text = tr(
                                uiState.language,
                                "• A pulzus és edzésadatok szinkronja órakapcsolatot és külön visszaigazolást igényel",
                                "• Heart rate and workout sync requires watch connection and explicit ACK",
                                "• Puls- und Trainingssynchronisierung erfordert Uhrenverbindung und ACK",
                                "• Sincronizarea pulsului și a antrenamentului necesită conexiune cu ceasul și ACK"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Text(
                            text = tr(
                                uiState.language,
                                "• Offline folytatás: kapcsolat megszakadásakor az óra önállóan folytatja az edzést",
                                "• Offline continuation: if disconnected, the watch continues the workout autonomously",
                                "• Offline-Fortsetzung: Bei Verbindungsabbruch führt die Uhr das Training selbstständig fort",
                                "• Continuare offline: la întreruperea conexiunii, ceasul continuă autonom antrenamentul"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }

        // Section 3: Egészségügyi szinkron (Mockup 4/4 #2: Health Connect & Samsung Health cards)
        item {
            Text(
                text = tr(uiState.language, "Egészségügyi szinkron", "Health Sync", "Gesundheitssynchronisierung", "Sincronizare sănătate"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onRequestPermission("hc") }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MockupHealthConnectBadge(size = 46.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Health Connect",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (uiState.permissionHealthConnectEnabled) {
                                "${tr(uiState.language, "Engedélyezett", "Granted", "Erteilt", "Acordate")}: ${uiState.healthGrantedCount}/${uiState.healthPermissionCount}"
                            } else {
                                tr(uiState.language, "Nincs olvasási engedély • engedélykérés", "No read permission • request access", "Keine Leseberechtigung • Zugriff anfordern", "Fără permisiune de citire • solicită acces")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.success
                        )
                        Text(
                            text = if (uiState.permissionHealthConnectEnabled) {
                                tr(uiState.language, "Koppints a Health Connect szinkronizálásához", "Tap to sync Health Connect", "Tippen zum Synchronisieren von Health Connect", "Atinge pentru a sincroniza Health Connect")
                            } else {
                                tr(uiState.language, "Pulzus, alvás, lépések, elégetett kalória, edzések", "Heart rate, sleep, steps, calories burned, workouts", "Puls, Schlaf, Schritte, Kalorien, Trainings", "Puls, somn, pași, calorii arse, antrenamente")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.textSecondary
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onRequestPermission("samsung") }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF14B8A6).copy(alpha = 0.16f),
                        modifier = Modifier
                            .size(46.dp)
                            .border(1.dp, Color(0xFF14B8A6).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFF14B8A6),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Samsung Health",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (uiState.permissionSamsungHealthEnabled) {
                                tr(uiState.language, "Olvasási engedély igazolva", "Read permission verified", "Leseberechtigung bestätigt", "Permisiune de citire verificată")
                            } else {
                                tr(uiState.language, "Nincs igazolt olvasási engedély", "No verified read permission", "Keine bestätigte Leseberechtigung", "Nicio permisiune de citire verificată")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.success
                        )
                        Text(
                            text = if (uiState.permissionSamsungHealthEnabled) {
                                tr(uiState.language, "Koppints a Samsung Health szinkronizálásához", "Tap to sync Samsung Health", "Tippen zum Synchronisieren von Samsung Health", "Atinge pentru a sincroniza Samsung Health")
                            } else {
                                tr(uiState.language, "Koppints az engedély megadásához és szinkronhoz", "Tap to grant permission and sync", "Tippen für Berechtigung und Synchronisierung", "Atinge pentru permisiune și sincronizare")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun StudioGoogleCloudSyncCard(
    onShowBanner: (String) -> Unit,
    onCloudSyncCompleted: () -> Unit = {},
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val palette = LocalTrainPilotPalette.current
    val strings = com.example.localization.LocalTrainPilotStrings.current
    val lang = strings.language
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val gs = settingsState.googleSyncState
    val context = LocalContext.current
    val activity = context as? Activity
    var cloudExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showMaintenanceOptions by remember { mutableStateOf(false) }

    val maintenancePreview by settingsViewModel.maintenancePreview.collectAsStateWithLifecycle()
    NativeGoogleMaintenanceConfirmation(
        request = maintenancePreview,
        language = settingsState.settings.language,
        busy = settingsState.maintenanceBusy,
        cancel = settingsViewModel::cancelGoogleMaintenance,
        confirm = { activity?.let(settingsViewModel::confirmGoogleMaintenance) }
    )

    val googleConsent by settingsViewModel.googleConsent.collectAsStateWithLifecycle()
    NativeGoogleConsentLauncher(
        prompt = googleConsent,
        take = settingsViewModel::takeGoogleConsent,
        complete = settingsViewModel::completeGoogleConsent,
        fail = settingsViewModel::failGoogleConsent
    )

    LaunchedEffect(settingsState.statusMessage) {
        val msg = settingsState.statusMessage
        if (!msg.isNullOrBlank()) {
            onShowBanner(msg)
        }
    }

    LaunchedEffect(gs.lastDriveSyncIso, gs.lastCalendarSyncIso, settingsState.maintenanceCompleted) {
        if (gs.lastDriveSyncIso.isNotBlank() || gs.lastCalendarSyncIso.isNotBlank() || settingsState.maintenanceCompleted > 0) {
            onCloudSyncCompleted()
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(20.dp))
            .testTag("studio_google_cloud_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("settings_google_expand").clickable { cloudExpanded = !cloudExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (gs.connectedEmail.isNotEmpty()) Icons.Default.CloudDone else Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(lang, "Google Cloud Szinkron", "Google Cloud Sync", "Google Cloud-Synchronisierung", "Sincronizare Google Cloud"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (gs.connectedEmail.isNotEmpty()) {
                                gs.connectedEmail
                            } else {
                                tr(lang, "Google Drive felhőmentés és Google Naptár szinkron", "Google Drive cloud backup & Google Calendar sync", "Google Drive Cloud-Backup & Google Kalender-Sync", "Backup Google Drive și sincronizare Google Calendar")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (gs.connectedEmail.isNotEmpty()) palette.primary else palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (gs.connectedEmail.isNotEmpty()) palette.success.copy(alpha = 0.16f) else palette.surfaceElevated,
                    modifier = Modifier.trainPilotBorder(
                        1.dp,
                        if (gs.connectedEmail.isNotEmpty()) palette.success.copy(alpha = 0.55f) else palette.cardBorder,
                        RoundedCornerShape(10.dp)
                    )
                ) {
                    Text(
                        text = (if (gs.connectedEmail.isNotEmpty()) tr(lang, "Csatlakoztatva", "Connected", "Verbunden", "Conectat") else tr(lang, "Nincs belépve", "Signed out", "Nicht angemeldet", "Neconectat")) + if (cloudExpanded) " ▴" else " ▾",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (gs.connectedEmail.isNotEmpty()) palette.success else palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (cloudExpanded) {
                Column(Modifier.testTag("settings_google_expanded"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (gs.connectedEmail.isNotEmpty()) {
                if (gs.connectedName.isNotBlank()) {
                    Text(
                        text = "${tr(lang, "Fiók", "Account", "Konto", "Cont")}: ${gs.connectedName} (${gs.connectedEmail})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                }
                if (gs.lastDriveSyncIso.isNotEmpty()) {
                    Text(
                        text = "${tr(lang, "Utolsó Drive szinkron", "Last Drive sync", "Letzter Drive-Sync", "Ultima sincronizare Drive")}: ${gs.lastDriveSyncIso.take(19).replace('T', ' ')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
                if (gs.lastCalendarSyncIso.isNotEmpty()) {
                    val countSuffix = gs.lastCalendarSyncCount?.let { cnt ->
                        " • " + tr(lang, "Szinkronizált események: $cnt", "Synced events: $cnt", "Synchronisierte Termine: $cnt", "Evenimente sincronizate: $cnt")
                    }.orEmpty()
                    Text(
                        text = "${tr(lang, "Utolsó Naptár szinkron", "Last Calendar sync", "Letzter Kalender-Sync", "Ultima sincronizare Calendar")}: ${gs.lastCalendarSyncIso.take(19).replace('T', ' ')}$countSuffix",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        modifier = Modifier.testTag("settings_google_last_calendar_sync")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { settingsViewModel.setRecoverEarlierBackups(!settingsState.recoverEarlierBackupsChecked) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = settingsState.recoverEarlierBackupsChecked,
                        onCheckedChange = { settingsViewModel.setRecoverEarlierBackups(it) },
                        colors = CheckboxDefaults.colors(checkedColor = palette.primary)
                    )
                    Text(
                        text = tr(lang, "Korábbi felhőmentések keresése és összefésülése", "Search and merge earlier cloud backups", "Frühere Cloud-Backups suchen und zusammenführen", "Caută și îmbină backup-urile cloud anterioare"),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { activity?.let { settingsViewModel.syncDriveCloud(it) } },
                        enabled = !gs.isBusy && !settingsState.replacementBusy && !settingsState.maintenanceBusy && activity != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("settings_google_drive_sync_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(lang, "Drive szinkron", "Drive Sync", "Drive-Sync", "Sincronizare Drive"),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    TrainPilotOutlinedButton(
                        onClick = { activity?.let { settingsViewModel.syncCalendarCloud(it) } },
                        enabled = !gs.isBusy && !settingsState.replacementBusy && !settingsState.maintenanceBusy && activity != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("settings_google_calendar_sync_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(lang, "Naptár szinkron", "Calendar Sync", "Kalender-Sync", "Sincronizare Calendar"),
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { settingsViewModel.disconnectGoogle() },
                        enabled = !gs.isBusy && !settingsState.replacementBusy && !settingsState.maintenanceBusy,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("settings_google_disconnect_btn")
                    ) {
                        Text(
                            text = tr(lang, "Google-fiók lekapcsolása", "Disconnect Google Account", "Google-Konto trennen", "Deconectează contul Google"),
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    TextButton(
                        onClick = { showMaintenanceOptions = !showMaintenanceOptions },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = if (showMaintenanceOptions) tr(lang, "Karbantartás elrejtése", "Hide Maintenance", "Wartung ausblenden", "Ascunde mentenanța") else tr(lang, "Felhő karbantartás", "Cloud Maintenance", "Cloud-Wartung", "Mentenanță cloud"),
                            color = palette.textSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                if (showMaintenanceOptions) {
                    HorizontalDivider(color = palette.cardBorder)
                    NativeGoogleMaintenanceControls(
                        language = settingsState.settings.language,
                        busy = settingsState.maintenanceBusy || gs.isBusy || settingsState.replacementBusy,
                        open = settingsViewModel::openGoogleMaintenance
                    )
                }
            } else {
                Text(
                    text = tr(
                        lang,
                        "Jelentkezz be Google-fiókkal az automatikus többeszközös Drive-szinkronhoz (edzésnapló, tervek, privát fotók) és a dedikált edzésnaptárhoz.",
                        "Sign in with Google for automatic multi-device Drive sync (workout log, plans, private photos) and dedicated workout calendar.",
                        "Melde dich mit Google an für automatische Drive-Synchronisierung (Protokoll, Pläne, private Fotos) und Trainingskalender.",
                        "Conectează-te cu Google pentru sincronizare automată Drive (jurnal, planuri, fotografii private) și calendar dedicat."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                Button(
                    onClick = { activity?.let { settingsViewModel.connectGoogle(it) } },
                    enabled = !gs.isBusy && !settingsState.replacementBusy && !settingsState.maintenanceBusy && activity != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("settings_google_login_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr(lang, "Bejelentkezés Google-fiókkal", "Sign in with Google", "Mit Google anmelden", "Conectare cu Google"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

                }
            }

            NativeGoogleMaintenanceFeedback(
                language = settingsState.settings.language,
                outcome = settingsState.maintenanceOutcome,
                count = settingsState.maintenanceCompleted
            )

            if (gs.isBusy && gs.stageMessage.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.primary.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth().testTag("settings_google_sync_loading")
                ) {
                    Text(
                        text = gs.stageMessage,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (!gs.isBusy && gs.errorMessage.isNullOrBlank() && gs.lastCalendarSyncIso.isNotBlank()) {
                val syncedCountLabel = gs.lastCalendarSyncCount?.let { cnt ->
                    " • " + tr(lang, "$cnt esemény szinkronizálva", "$cnt events synced", "$cnt Termine synchronisiert", "$cnt evenimente sincronizate")
                }.orEmpty()
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.success.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth().testTag("settings_google_calendar_sync_success")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = tr(lang, "Naptár szinkron sikeres", "Calendar sync successful", "Kalender-Sync erfolgreich", "Sincronizare calendar reușită") + syncedCountLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.success
                        )
                        Text(
                            text = "${tr(lang, "Utolsó sikeres szinkron", "Last successful sync", "Letzter erfolgreicher Sync", "Ultima sincronizare reușită")}: ${gs.lastCalendarSyncIso.take(19).replace('T', ' ')}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }

            if (!gs.errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth().testTag("settings_google_sync_error")
                ) {
                    Text(
                        text = gs.errorMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * 4/4 #3 MENTÉS ÉS VISSZAÁLLÍTÁS (Mockup 4/4 #3)
 */
@Composable
fun BackupRestoreScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectFormat: (String) -> Unit,
    onToggleIncludeHealth: (Boolean) -> Unit,
    onShowBanner: (String) -> Unit,
    onExportBackup: ((android.net.Uri) -> Unit)? = null,
    onImportBackup: ((android.net.Uri) -> Unit)? = null,
    onCloudSyncCompleted: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val settingsViewModel: SettingsViewModel = viewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val gs = settingsState.googleSyncState
    val context = LocalContext.current
    val activity = context as? Activity
    val mimeType = if (uiState.selectedBackupFormat == "ZIP") "application/zip" else "application/json"
    val defaultFileName = if (uiState.selectedBackupFormat == "ZIP") "trainpilot-backup.zip" else "trainpilot-backup.json"

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType)
    ) { uri ->
        if (uri != null) {
            if (onExportBackup != null) {
                onExportBackup(uri)
            } else {
                onShowBanner(tr(uiState.language, "Mentés készítése...", "Creating backup...", "Sicherung wird erstellt...", "Se creează backup-ul..."))
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            if (onImportBackup != null) {
                onImportBackup(uri)
            } else {
                onShowBanner(tr(uiState.language, "Visszaállítás fájlból...", "Restoring from file...", "Wiederherstellung aus Datei...", "Se restaurează din fișier..."))
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("backup_restore_screen"),
        contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        item {
            SubScreenTopBar(
                title = strings.backupRestoreTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("JSON", "ZIP", "DRIVE").forEach { fmt ->
                    val sel = uiState.selectedBackupFormat == fmt
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (sel) palette.primary else palette.surfaceCard,
                        modifier = Modifier
                            .weight(1f)
                            .trainPilotBorder(1.dp, if (sel) palette.primary else palette.cardBorder, RoundedCornerShape(14.dp))
                            .clickable { onSelectFormat(fmt) }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 14.dp)) {
                            Text(
                                text = fmt,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) palette.onPrimary else palette.textPrimary
                            )
                        }
                    }
                }
            }
        }
        item {
            StudioGoogleCloudSyncCard(
                onShowBanner = onShowBanner,
                onCloudSyncCompleted = onCloudSyncCompleted,
                settingsViewModel = settingsViewModel
            )
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr(uiState.language, "Mentés tartalma", "Backup Contents", "Inhalt der Sicherung", "Conținutul backup-ului"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Text(
                        tr(
                            uiState.language,
                            "✓ Edzésnapló és PR rekordok (${uiState.loggedWorkouts.size} edzés)",
                            "✓ Workout log & PR records (${uiState.loggedWorkouts.size} workouts)",
                            "✓ Trainingsprotokoll & PR-Rekorde (${uiState.loggedWorkouts.size} Trainings)",
                            "✓ Jurnal antrenamente și recorduri PR (${uiState.loggedWorkouts.size} antrenamente)"
                        ),
                        color = palette.success
                    )
                    Text(
                        tr(
                            uiState.language,
                            "✓ Saját programok és gyakorlatok (${uiState.programs.size} program)",
                            "✓ Custom programs & exercises (${uiState.programs.size} programs)",
                            "✓ Eigene Programme & Übungen (${uiState.programs.size} Programme)",
                            "✓ Programe și exerciții proprii (${uiState.programs.size} programe)"
                        ),
                        color = palette.success
                    )
                    Text(
                        tr(
                            uiState.language,
                            "✓ Beállítások és 32 színpaletta profil",
                            "✓ Settings & 32 color palette profile",
                            "✓ Einstellungen & 32-Farbpaletten-Profil",
                            "✓ Setări și profilul celor 32 de palete"
                        ),
                        color = palette.success
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            tr(
                                uiState.language,
                                "Health Connect, Samsung Health és órás egészségadatok",
                                "Health Connect, Samsung Health & watch health data",
                                "Health Connect-, Samsung Health- und Uhren-Gesundheitsdaten",
                                "Date de sănătate Health Connect, Samsung Health și ceas"
                            ),
                            color = palette.textPrimary, modifier = Modifier.weight(1f)
                        )
                        Switch(checked = uiState.includeHealthInBackup, enabled = !uiState.backupHealthConsentSaving, onCheckedChange = onToggleIncludeHealth)
                    }
                    Text(
                        tr(
                            uiState.language,
                            "Alapból kikapcsolva. JSON/ZIP: tárolt egészségnapló és edzésösszesítések. Drive: az edzéshez mentett egészségadatok; a teljes napi egészségnapló nem kerül Drive-ra. Kikapcsolás nem törli a helyi adatokat vagy a korábbi mentéseket.",
                            "Off by default. JSON/ZIP: stored health journal & workout summaries. Drive: workout-attached health summaries only; full daily health journal is not uploaded to Drive. Turning off does not delete local data or existing backups.",
                            "Standardmäßig aus. JSON/ZIP: gespeichertes Gesundheitstagebuch & Trainingszusammenfassungen. Drive: nur trainingsbezogene Gesundheitsdaten; das volle Tagesprotokoll wird nicht auf Drive geladen.",
                            "Dezactivat implicit. JSON/ZIP: jurnalul de sănătate stocat și rezumatele antrenamentelor. Drive: doar datele de sănătate atașate antrenamentelor; jurnalul zilnic complet nu este încărcat pe Drive."
                        ),
                        color = palette.textSecondary, style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.2.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tr(uiState.language, "Korábbi RepForge app mentés behozása", "Import Legacy RepForge Backup", "Früheres RepForge-Backup importieren", "Importă backup RepForge anterior"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary
                    )
                    Text(
                        text = tr(
                            uiState.language,
                            "Teljes kompatibilitás a korábbi RepForge (.zip és .json) biztonsági mentésekkel: edzésnapló, egyéni programok, gyakorlatok, testsúlynapló és privát fotók automatikus konvertálása.",
                            "Full compatibility with earlier RepForge (.zip and .json) backups: automatic conversion of workout logs, custom programs, exercises, weight log, and private photos.",
                            "Volle Kompatibilität mit früheren RepForge-Backups (.zip und .json): automatische Konvertierung von Trainingsprotokoll, Programmen, Übungen, Gewicht und privaten Fotos.",
                            "Compatibilitate completă cu backup-urile RepForge (.zip și .json): conversie automată pentru jurnal, programe, exerciții, greutate și fotografii private."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    TrainPilotOutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "*/*"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("import_repforge_legacy_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = palette.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(uiState.language, "RepForge ZIP / JSON mentés importálása", "Import RepForge ZIP / JSON Backup", "RepForge ZIP / JSON importieren", "Importă backup ZIP / JSON RepForge"),
                            color = palette.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        item {
            com.repforge.app.ui.theme.TrainPilotSaveButton(
                onClick = {
                    if (uiState.selectedBackupFormat == "DRIVE") {
                        if (gs.connectedEmail.isEmpty()) {
                            activity?.let { settingsViewModel.connectGoogle(it) }
                        } else {
                            activity?.let { settingsViewModel.syncDriveCloud(it) }
                        }
                    } else {
                        exportLauncher.launch(defaultFileName)
                    }
                },
                enabled = if (uiState.selectedBackupFormat == "DRIVE") {
                    !gs.isBusy && !settingsState.replacementBusy && !settingsState.maintenanceBusy && activity != null
                } else true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.Backup, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.selectedBackupFormat == "DRIVE" && gs.connectedEmail.isEmpty()) {
                        tr(uiState.language, "Bejelentkezés Google-fiókkal (Drive)", "Sign in with Google (Drive)", "Mit Google anmelden (Drive)", "Conectare cu Google (Drive)")
                    } else {
                        "${tr(uiState.language, "Mentés indítása", "Start Backup", "Sicherung starten", "Pornește backup")} (${uiState.selectedBackupFormat})"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            TrainPilotOutlinedButton(
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "*/*"))
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = palette.textPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    tr(uiState.language, "Visszaállítás fájlból (TrainPilot / RepForge)", "Restore from file (TrainPilot / RepForge)", "Aus Datei wiederherstellen (TrainPilot / RepForge)", "Restaurare din fișier (TrainPilot / RepForge)"),
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * 4/4 #4 ENGEDÉLYEK (Mockup 4/4 #4)
 */
@Composable
fun PermissionsScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onTogglePermission: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("permissions_screen"),
        contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        item {
            SubScreenTopBar(
                title = strings.permissionsTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }
        item {
            PermissionToggleCard(
                tr(uiState.language, "GPS és helyadatok", "GPS & Location", "GPS & Standortdaten", "GPS și locație"),
                tr(uiState.language, "Kültéri futás és útvonal rögzítése", "Outdoor run and route recording", "Outdoor-Lauf und Routenaufzeichnung", "Alergare exterioară și înregistrare traseu"),
                uiState.permissionGpsEnabled
            ) {
                onTogglePermission("gps")
            }
        }
        item {
            PermissionToggleCard(
                tr(uiState.language, "Központi helymeghatározás", "System Location Services", "System-Standortdienste", "Servicii de localizare sistem"),
                tr(uiState.language, "Android rendszer-helyszolgáltatás beállításai", "Android system location settings", "Android-Standorteinstellungen", "Setări locație sistem Android"),
                uiState.permissionCentralLocEnabled
            ) {
                onTogglePermission("central")
            }
        }
        item {
            PermissionToggleCard(
                tr(uiState.language, "Értesítések", "Notifications", "Benachrichtigungen", "Notificări"),
                tr(uiState.language, "Pihenőidő és aktív edzés állapotsor", "Rest timer & active workout status notification", "Pausen-Timer & Trainingsstatus-Benachrichtigung", "Cronometru pauză și notificare antrenament activ"),
                uiState.permissionNotificationsEnabled
            ) {
                onTogglePermission("notif")
            }
        }
        item {
            PermissionToggleCard(
                "Health Connect (${uiState.healthGrantedCount}/${uiState.healthPermissionCount})",
                tr(uiState.language, "Pulzus, alvás, lépések és testsúly szinkron", "Heart rate, sleep, steps & weight sync", "Puls-, Schlaf-, Schritte- & Gewichtssynchronisierung", "Sincronizare puls, somn, pași și greutate"),
                uiState.permissionHealthConnectEnabled
            ) {
                onTogglePermission("hc_manage")
            }
        }
        item {
            PermissionToggleCard(
                tr(uiState.language, "Samsung Health közvetlen híd", "Samsung Health Direct Bridge", "Direkte Samsung Health-Brücke", "Punte directă Samsung Health"),
                tr(uiState.language, "Samsung Health olvasási engedélyének kezelése", "Manage Samsung Health read permissions", "Samsung Health-Leseberechtigungen verwalten", "Gestionează permisiunile de citire Samsung Health"),
                uiState.permissionSamsungHealthEnabled
            ) {
                onTogglePermission("samsung_manage")
            }
        }
    }
}

@Composable
private fun PermissionToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }
            Switch(checked = checked, onCheckedChange = { onToggle() })
        }
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
    onShowBanner: (String) -> Unit
) {
    com.example.ui.components.StudioPrivatePhotos(uiState, strings, onBack, onOpenSettings)
}

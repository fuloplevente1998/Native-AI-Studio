package com.example.ui.screens

import com.repforge.app.ui.theme.TrainPilotCardSpacing

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
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
 * Mai állapot + Pulzustrend + Testsúlynapló + További Health-adatok + Egészségnapló + Health Connect)
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
    onOpenLoggedWorkout: (String) -> Unit = { onNavigateToSubScreen(SubScreen.LOG_WORKOUT_DETAIL) }
) {
    val palette = LocalTrainPilotPalette.current
    val requestDailyHealthSync = com.example.ui.components.LocalSyncDailyHealth.current
    val today = remember { java.time.LocalDate.now() }
    val todayHuLabel = remember(today) {
        today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy. MMM d.", java.util.Locale("hu", "HU")))
    }
    val todayDotLabel = remember(today) {
        today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy. MM. dd."))
    }
    val dotFormatter = remember {
        java.time.format.DateTimeFormatter.ofPattern("yyyy. MM. dd.")
    }
    val shortChartDateFormatter = remember {
        java.time.format.DateTimeFormatter.ofPattern("MM.dd.")
    }

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
                    text = if (editingMeasurement != null) "Mérés szerkesztése" else "Új testsúlymérés",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Testsúly (kg) és a mérés dátuma (a grafikon ezen dátum alapján rendezi):")
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        singleLine = true,
                        label = { Text("Testsúly (kg)") }
                    )
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        singleLine = true,
                        label = { Text("Mérés dátuma (pl. 2026. 10. 15.)") }
                    )
                    // Quick date presets for fast testing of historical dates
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "Ma" to today,
                            "-3 nap" to today.minusDays(3),
                            "-7 nap" to today.minusDays(7),
                            "-14 nap" to today.minusDays(14)
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
                Button(
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
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)
                ) {
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

    LazyColumn(
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
                                    text = "Mai állapot",
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
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Az egészséged rövid áttekintése",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        IconButton(
                            onClick = { requestDailyHealthSync(if (uiState.primaryHealthSourceSamsung)
                                com.repforge.app.HealthJournalStore.SAMSUNG else com.repforge.app.HealthJournalStore.HC) },
                            enabled = !uiState.healthSyncBusy,
                            modifier = Modifier.testTag("health_today_header_sync")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Mai egészségadatok frissítése",
                                tint = palette.primary)
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
                            "${uiState.minHrBpm}–${uiState.maxHrBpm} bpm" else "nincs lekért napi tartomány"
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
                                val (titleStr, onActionClick) = when (activeKey) {
                                    "calories" -> listOf(
                                        "Aktív kalória részletek • $activeEnergy",
                                        "Aktív energia: $activeEnergy • Teljes napi energia (nyugalmival): $totalEnergy",
                                        "További Health-adatok megnyitása lent"
                                    ) to { onToggleHealthSection("extra") }
                                    "pulse" -> listOf(
                                        "Pulzus részletek • Napi átlag: ${if (uiState.currentHrBpm > 0) "${uiState.currentHrBpm} bpm" else "—"}",
                                        "Nyugalmi: ${if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—"} • Tartomány: $heartRange",
                                        "Korábbi mai pulzusmérések megtekintése lent"
                                    ) to { onToggleHealthSection("pulse") }
                                    "steps" -> listOf(
                                        "Lépések részletek • ${uiState.dailySteps} / ${uiState.stepsGoal}",
                                        "Teljesítés: ${((uiState.dailySteps.toFloat() / uiState.stepsGoal.coerceAtLeast(1)) * 100).toInt()}% • Becsült táv: ${"%.1f".format(uiState.dailySteps * 0.00075f)} km",
                                        "Célok módosítása >"
                                    ) to { onOpenGoalsDialog() }
                                    "workout" -> listOf(
                                        "Mai edzések • ${uiState.loggedWorkouts.workoutsOn(today).size} rögzített edzés",
                                        "Utolsó mai edzés: ${uiState.loggedWorkouts.workoutsOn(today).firstOrNull()?.titleHu ?: "Ma még nincs rögzített edzés"}",
                                        "Edzésnapló részletek megnyitása >"
                                    ) to { uiState.loggedWorkouts.workoutsOn(today).firstOrNull()?.id?.let(onOpenLoggedWorkout) }
                                    "recovery" -> listOf(
                                        "Regeneráció & Készenlét • ${uiState.todayRecoveryReading?.displayScoreHu ?: "—"}",
                                        uiState.todayRecoveryReading?.explanationHu ?: "Nincs igazolt regenerációs mérés.",
                                        "TrainPilot Coach megnyitása >"
                                    ) to { onNavigateToSubScreen(SubScreen.COACH_DETAIL) }
                                    else -> listOf(
                                        "Alvás részletek • ${uiState.sleepDurationLabel} (${uiState.sleepHoursDecimal})",
                                        "Alvásfázisok: nincs lekért adat",
                                        "Egészségnapló megnyitása lent"
                                    ) to { onToggleHealthSection("journal") }
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
                                            text = "Bezárás",
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
                                Text(
                                    text = titleStr[1],
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary
                                )
                                if (activeKey == "pulse") {
                                    Text("Korábbi mai mérések", style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary, modifier = Modifier.testTag("health_today_pulse_history_label"))
                                    if (uiState.dailyPulseHistory.isEmpty()) Text("Nincs lekért időbélyeges pulzusmérés",
                                        color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                                    uiState.dailyPulseHistory.takeLast(8).asReversed().forEach { reading ->
                                        val stamp = runCatching { java.time.Instant.parse(reading.timeIso)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) }.getOrDefault("—")
                                        Text("$stamp • ${reading.bpm} bpm", style = MaterialTheme.typography.bodySmall,
                                            color = palette.textPrimary, modifier = Modifier.testTag("health_today_pulse_sample"))
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.primary.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onActionClick() }
                                ) {
                                    Text(
                                        text = titleStr[2],
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
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
                                text = "Heti egészség & edzés célok",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$primaryProviderName & Room edzésnapló (Koppints a forrásokhoz)",
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
                                    text = "Célok",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary
                                )
                            }
                        }
                    }

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
                                        Text(if (steps) "Heti lépésszám" else "Aktív edzésidő", modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelLarge, color = palette.textPrimary)
                                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = palette.primary)
                                    }
                                    Text(if (steps) { if (goalDays.none { it.stepsAvailable }) "—" else "%,d".format(java.util.Locale.US, weeklyStepsCurrent).replace(',', ' ') }
                                        else "$weeklyMinutesCurrent perc", style = MaterialTheme.typography.headlineMedium,
                                        color = palette.textPrimary, modifier = Modifier.testTag("health_weekly_${tag}_total"))
                                    Text(if (steps) "Cél: $weeklyStepsTarget" else "Cél: $weeklyMinutesTarget perc",
                                        style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                    LinearProgressIndicator(progress = { (if (steps) weeklyStepsPct else weeklyMinutesPct) / 100f },
                                        modifier = Modifier.fillMaxWidth(), color = palette.primary, trackColor = palette.surfaceElevated)
                                    if (steps) Text("${goalDays.count { it.stepsAvailable }}/7 napi adat • $primaryProviderName",
                                        style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
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
                                    Text(if (steps) "Heti lépésszám • napi bontás" else "Aktív edzésidő • napi bontás",
                                        style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
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
                                                    if (dayWorkouts.isEmpty()) "Room edzésnapló • nincs mai edzés"
                                                    else "Room • " + dayWorkouts.joinToString(" · ") {
                                                        "${it.titleHu}: ${it.durationMinutes} perc"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textSecondary, maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis)
                                            }
                                            Text(if (steps) { if (day.stepsAvailable) "${day.steps} lépés" else "—" }
                                                else "${day.workoutMinutes} perc • ${day.workoutCount} edzés",
                                                modifier = Modifier.testTag("health_weekly_${tag}_${day.dateIso}"),
                                                style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Expandable Inner Card 1 inside Weekly Section: "24 órás pulzustrend" (Screenshot 9 style + full expanded details)
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
                                        text = "24 órás pulzustrend",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    val restLbl = if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—"
                                    val peakLbl = if (uiState.maxHrBpm > 0) "${uiState.maxHrBpm} bpm" else "—"
                                    Text(
                                        text = "Nyugalmi: $restLbl • Csúcs: $peakLbl",
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
                                            text = "Részletek",
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
                                        Text(if (uiState.dailyPulseHistory.isEmpty()) "Nincs lekért pulzus-idősor"
                                            else "Egy időbélyeges pulzusmérés érhető el",
                                            color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
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
                                            text = "Mai pulzus",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            PulseInnerBox(
                                                label = "Átlagpulzus",
                                                value = if (uiState.currentHrBpm > 0) "${uiState.currentHrBpm} bpm" else "—",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PulseInnerBox(
                                                label = "Nyugalmi pulzus",
                                                value = if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            PulseInnerBox(
                                                label = "Max. pulzus",
                                                value = if (uiState.maxHrBpm > 0) "${uiState.maxHrBpm} bpm" else "—",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PulseInnerBox(
                                                label = "Min. pulzus",
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
                                        text = "Regenerációs előzmények",
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
                                        text = "Testsúlynapló & Trend",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val latestWeightLabel = if (latestChronologicalEntry != null) {
                                        "Legújabb (${latestChronologicalEntry.dateLabel}): ${latestChronologicalEntry.weightLabel}"
                                    } else if (uiState.currentBodyWeightKg > 0f) {
                                        "Legújabb: ${"%.1f".format(uiState.currentBodyWeightKg).replace('.', ',')} kg"
                                    } else {
                                        "Legújabb: nincs rögzített mérés"
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
                                            text = "Részletek",
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
                                                text = "Még nincs rögzített testsúlymérés.",
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
                                                text = "Test és fittség",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.primary
                                            )
                                            Text(
                                                text = "Testzsír, SpO₂, VO₂max",
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
                                                    text = "Testsúly (legutóbbi dátum szerint)",
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
                                                    text = "Rögzített dátum: ${latestChronologicalEntry?.dateLabel ?: todayHuLabel}",
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
                                                label = "Testzsír",
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

        // 2. Expandable Section: További Health-adatok (Screenshot 1)
        item {
            ExpandableHealthSectionCard(
                icon = Icons.Default.Restore,
                title = strings.moreHealthDataTitle,
                subtitle = "Vérnyomás, vércukor, légzésszám, távolság",
                expanded = uiState.expandedHealthExtraData,
                onToggle = { onToggleHealthSection("extra") }
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummarySmallPill("Vérnyomás", uiState.bloodPressureLabel, Modifier.weight(1f))
                    SummarySmallPill("Vércukor", uiState.bloodGlucoseLabel, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummarySmallPill("Légzésszám", uiState.respiratoryRateLabel, Modifier.weight(1f))
                    SummarySmallPill(
                        "Távolság (ma)",
                        if (uiState.dailyDistanceKm > 0f) String.format(java.util.Locale.US, "%.2f km", uiState.dailyDistanceKm).replace('.', ',') else "—",
                        Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummarySmallPill("Véroxigén (SpO₂)", if (uiState.spo2Percent > 0f) "${uiState.spo2Percent} %" else "—", Modifier.weight(1f))
                    SummarySmallPill("HRV (RMSSD)", if (uiState.hrvRmssdMs > 0f) "${uiState.hrvRmssdMs.roundToInt()} ms" else "—", Modifier.weight(1f))
                }
            }
        }

        // 3. Expandable Section: Egészségnapló (Screenshot 1)
        item {
            ExpandableHealthSectionCard(
                icon = Icons.Default.Favorite,
                title = strings.healthJournalTitle,
                subtitle = null,
                expanded = uiState.expandedHealthJournal,
                onToggle = { onToggleHealthSection("journal") }
            ) {
                val journalEntries = if (uiState.weeklyHealthDays.isNotEmpty()) {
                    uiState.weeklyHealthDays.reversed().map { day ->
                        day.dayLabelHu to ("Forrás: ${day.sourceLabelHu} • " +
                            "Alvás: ${day.sleepLabel} • " +
                            "Lépés: ${if (day.steps > 0) day.steps.toString() else "—"} • " +
                            "Kalória: ${if (day.activeCaloriesKcal > 0) "${day.activeCaloriesKcal} kcal" else "—"}")
                    }
                } else {
                    listOf(
                        today.toString() to ("${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} • " +
                            "Alvás: ${uiState.sleepDurationLabel} • " +
                            "Pulzus: ${if (uiState.currentHrBpm > 0) "${uiState.currentHrBpm} bpm" else "—"} • " +
                            "Lépések: ${if (uiState.dailySteps > 0) uiState.dailySteps.toString() else "—"} • " +
                            "Kalória: ${if (uiState.activeCaloriesKcal > 0) "${uiState.activeCaloriesKcal} kcal" else "—"}")
                    )
                }
                journalEntries.forEach { (d, summary) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(d, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text(summary, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                    }
                }
                val weeklyPulse = uiState.weeklyHealthDays.mapNotNull { it.pulse }
                if (weeklyPulse.isNotEmpty()) {
                    Text("Pulzus – elmúlt 7 nap", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    HealthPulseWeekRows(weeklyPulse)
                }
            }
        }

        // 4. Expandable Section: Health Connect (Screenshot 1 + custom Health Connect badge)
        item {
            ExpandableHealthSectionCard(
                icon = Icons.Default.Link,
                title = "Egészségadat-források",
                subtitle = "Adatforrások, engedélyek és szinkron",
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
                            Text(if (uiState.samsungGrantedCount < 0) "Engedély még nem igazolt"
                                else "${uiState.samsungGrantedCount}/${uiState.samsungPermissionCount} adatkör",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.samsungGrantedCount > 0) palette.success else palette.textSecondary)
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
                            Text("● ${uiState.healthGrantedCount}/${uiState.healthPermissionCount} engedély", style = MaterialTheme.typography.labelSmall, color = if (uiState.healthGrantedCount > 0) palette.success else palette.textSecondary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Az elérhető adatok a választott szolgáltatótól és a tényleges engedélyektől függenek.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                // Show source-scoped verification here, not above the six daily measurement tiles.
                val selectedProviderSyncTime = remember(uiState.dailyHealthLastSyncIso) {
                    runCatching {
                        java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd. HH:mm")
                            .withZone(java.time.ZoneId.systemDefault())
                            .format(java.time.Instant.parse(uiState.dailyHealthLastSyncIso))
                    }.getOrDefault("Még nincs igazolt szinkron")
                }
                Text(
                    text = "Kiválasztott forrás: ${uiState.dailyHealthSourceLabel} • Utolsó sikeres szinkron: $selectedProviderSyncTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    modifier = Modifier.testTag("health_selected_provider_last_sync")
                )
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
                        val watchTitle = if (uiState.watchAppAvailable) "Óra és szinkron (${uiState.watchName})" else "Óra és szinkron"
                        Text(
                            text = watchTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = when {
                                uiState.watchAppAvailable -> "● Wear OS adatkapcsolat • átadás külön ACK-val"
                                uiState.watchConnected -> "Wear OS rendszerkapcsolat • TrainPilot óraapp nincs telepítve"
                                else -> "Nincs csatlakoztatott óra"
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
                                text = if (uiState.healthSyncBusy) "Szinkronizálás…" else strings.syncAction,
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

                Text(
                    text = "${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} • utolsó teljes szinkron: ${uiState.lastHealthSyncLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                if (uiState.healthSyncMessage.isNotBlank()) Text(uiState.healthSyncMessage,
                    color = if (uiState.healthSyncFailed) palette.danger else if (uiState.healthSyncPartial) palette.warning else palette.textPrimary,
                    modifier = Modifier.testTag("health_sync_result"))
                Text("Utolsó adatolvasás: ${uiState.lastHealthReadLabel}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                Text(
                    text = uiState.healthProviderStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
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
            modifier = Modifier.padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 6.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(16.dp)
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
                style = MaterialTheme.typography.titleMedium,
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
fun CoachDetailScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartSuggestedWorkout: () -> Unit,
    onOpenExerciseDetail: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val displayScore = uiState.coachReadinessScore.coerceIn(0, 100)
    val samsungScore = uiState.samsungEnergyScore.coerceIn(0, 100)
    val ownReadinessScore = uiState.trainPilotReadinessScore.coerceIn(0, 100)

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
                            text = "Mai felkészültség",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        val statusBadgeText = when {
                            displayScore >= 75 -> "OPTIMÁLIS KÉSZENLÉT"
                            displayScore >= 50 -> "MÉRSÉKELT KÉSZENLÉT"
                            displayScore > 0 -> "PIHENÉS JAVASOLT"
                            else -> "NINCS MÉRT ADAT"
                        }
                        val statusBadgeColor = when {
                            displayScore >= 75 -> palette.success
                            displayScore >= 50 -> palette.primary
                            displayScore > 0 -> palette.warning
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
                            val ringCol = if (displayScore > 0) palette.success else palette.cardBorder
                            Canvas(modifier = Modifier.size(98.dp)) {
                                drawArc(
                                    color = trackCol,
                                    startAngle = 135f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                )
                                if (displayScore > 0) {
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
                                    text = if (displayScore > 0) "$displayScore" else "—",
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
                                text = "3. Terheléssel korlátozott végső Coach készenlét",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                            Text(
                                text = if (displayScore > 0) {
                                    "A napi regeneráció és az akut izomterhelés alapján ma teljes értékű edzés végezhető 1–2 RIR tartalékkal."
                                } else {
                                    "Szinkronizáld a Health Connect vagy Samsung Health adatokat a mai személyre szabott készenléti érték kiszámításához."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Három különálló készenléti mutató (D10 — nem mosható össze):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.textSecondary
                    )
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
                                Text("1. Mai Samsung...", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1)
                                Text(
                                    text = if (samsungScore > 0) "$samsungScore / 100" else "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = if (samsungScore > 0) "Samsung Health" else "Nincs adat",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (samsungScore > 0) palette.success else palette.textSecondary
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
                                Text("2. TrainPilot saját...", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1)
                                Text(
                                    text = if (ownReadinessScore > 0) "$ownReadinessScore / 100" else "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text("HRV & alvás", style = MaterialTheme.typography.labelSmall, color = palette.primary)
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
                                Text("3. Terheléssel...", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1)
                                Text(
                                    text = if (displayScore > 0) "$displayScore / 100" else "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary
                                )
                                Text("Végső Coach", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                            }
                        }
                    }
                }
            }
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
                        Text("Kulcsjelek", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text("Élő szinkron", style = MaterialTheme.typography.labelMedium, color = palette.primary)
                    }
                    CoachSignalRow("Alvás", uiState.sleepDurationLabel, palette.success)
                    CoachSignalRow("Nyugalmi pulzus", if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—", palette.success)
                    CoachSignalRow("HRV (RMSSD)", if (uiState.hrvRmssdMs > 0f) "${uiState.hrvRmssdMs.roundToInt()} ms" else "—", palette.primary)
                    CoachSignalRow("Rögzített edzések", "${uiState.loggedWorkouts.size} db", palette.primary)
                }
            }
        }

        // Javasolt edzés Card
        item {
            val activeProgram = uiState.programs.firstOrNull { it.isActive } ?: uiState.programs.firstOrNull()
            val nextDay = activeProgram?.days?.firstOrNull()
            val suggestedExercises = nextDay?.exercises?.take(3).orEmpty().ifEmpty { uiState.activeWorkoutExercises.take(3) }
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Javasolt edzés (MAI JAVASLAT)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null, tint = palette.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (activeProgram != null) "${activeProgram.nameHu} • ${nextDay?.dayCode ?: "A nap"}" else "Személyre szabott edzés",
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text("Felszereléshez igazított terv • ~ ${uiState.userWorkoutDurationMin} perc", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                    }

                    suggestedExercises.forEach { ex ->
                        CoachExerciseAdviceRow(ex.nameHu, "${ex.defaultSets}×${ex.defaultReps} • ${ex.equipmentHu}", palette.success) {
                            onOpenExerciseDetail(ex.id)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onStartSuggestedWorkout,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("coach_start_workout_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edzés indítása", fontWeight = FontWeight.Bold)
                    }
                }
            }
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
    onToggleProgressionModal: (Boolean) -> Unit,
    onSaveProgression: (String, String, Boolean) -> Unit,
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
    var vibrationEnabled by remember { mutableStateOf(uiState.vibrationEnabled) }

    if (uiState.showProgressionModal) {
        var selectedStyle by remember { mutableStateOf(uiState.trainingStyleHu) }
        var selectedRule by remember { mutableStateOf(uiState.progressionRuleHu) }
        var beginnerGentle by remember { mutableStateOf(uiState.beginnerGentleProfileActive) }
        AlertDialog(
            onDismissRequest = { onToggleProgressionModal(false) },
            containerColor = palette.surfaceCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text("Edzés beállításai és progresszió", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Alapértelmezett pihenőidő:", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.primary.copy(alpha = 0.2f),
                            modifier = Modifier
                                .trainPilotBorder(1.dp, palette.primary, RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onCycleRestTimer() }
                        ) {
                            Text(
                                text = "${uiState.defaultRestSec} mp ↻",
                                fontWeight = FontWeight.Bold,
                                color = palette.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hangjelzés pihenő végén", color = palette.textPrimary)
                        Switch(checked = uiState.soundAlertEnabled, onCheckedChange = onToggleSoundAlert)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rezgés pihenő végén", color = palette.textPrimary)
                        Switch(checked = vibrationEnabled, onCheckedChange = { vibrationEnabled = it })
                    }
                    Text("Edzésstílus:", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    listOf("Normál hipertrófia", "Erő (5×5)", "Állóképesség / Köredzés").forEach { st ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedStyle == st) palette.primary else palette.surfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStyle = st }
                        ) {
                            Text(
                                text = st,
                                color = if (selectedStyle == st) palette.onPrimary else palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Text("Progressziós elv:", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    listOf("Dupla progresszió", "Lineáris súlyemelés", "RIR alapú autoreguláció").forEach { rl ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedRule == rl) palette.primary else palette.surfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRule = rl }
                        ) {
                            Text(
                                text = rl,
                                color = if (selectedRule == rl) palette.onPrimary else palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kezdő kímélő profil", color = palette.textPrimary)
                        Switch(checked = beginnerGentle, onCheckedChange = { beginnerGentle = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onSaveProgression(selectedStyle, selectedRule, beginnerGentle) },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)
                ) {
                    Text(strings.saveAction, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TrainPilotOutlinedButton(onClick = { onToggleProgressionModal(false) }) {
                    Text(strings.cancelAction, color = palette.textPrimary)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("settings_main_screen"),
        contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
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

        // Megjelenés és nyelv (32 Palettes in 4 Expandable Columns + 4 Languages)
        item {
            Text(strings.appearanceAndLang, style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
        }

        item {
            SettingsActionRowCard(
                title = strings.themes32Title,
                subtitle = "${currentPaletteObj.localizedName(uiState.language)} • Koppints a 4 téma stílus ${if (expandedMainThemesSection) "becsukásához" else "kinyitásához"}",
                badgeText = "#${uiState.selectedPaletteId}",
                onClick = { expandedMainThemesSection = !expandedMainThemesSection },
                testTag = "settings_open_palettes_row",
                isExpanded = expandedMainThemesSection
            )
        }

        // 4 Expandable Theme Categories inside Settings (Matt témák | Kontrasztos témák | Fényes / Glow témák | Világos témák)
        if (expandedMainThemesSection) {
            item {
                val allPalettes = TrainPilotPalettes.allPalettes
                val categories = listOf(
                    Triple("Matt témák (Sötét / Prémium matt)", allPalettes.filter { it.id in 1..8 }, expandedMatteThemes) to { expandedMatteThemes = !expandedMatteThemes },
                    Triple("Kontrasztos témák (Élénk kontraszt)", allPalettes.filter { it.id in 9..16 }, expandedContrastThemes) to { expandedContrastThemes = !expandedContrastThemes },
                    Triple("Fényes / Glow témák (Neon & Glow)", allPalettes.filter { it.id in 17..24 }, expandedGlowThemes) to { expandedGlowThemes = !expandedGlowThemes },
                    Triple("Világos témák (Nappali / Light)", allPalettes.filter { it.id in 25..32 }, expandedLightThemes) to { expandedLightThemes = !expandedLightThemes }
                )

            Surface(shape = RoundedCornerShape(18.dp), color = palette.surfaceCard,
                modifier = Modifier.fillMaxWidth().trainPilotBorder(1.5.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                    .testTag("settings_themes_group")) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                categories.forEachIndexed { categoryIndex, entry ->
                    val (catInfo, onToggleCat) = entry
                    val (catTitle, catPalettes, isExpanded) = catInfo
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surfaceElevated,
                        modifier = Modifier.fillMaxWidth().testTag("settings_theme_category_$categoryIndex")
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
                                        text = "${catPalettes.size} téma",
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
                                                            onShowBanner("Téma aktiválva: ${itemPal.localizedName(uiState.language)}")
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

        // Edzés és értesítések
        item {
            Text("Edzés és értesítések", style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
        }

        item {
            SettingsActionRowCard(
                title = strings.restTimerTitle,
                subtitle = "Alapértelmezett pihenőidő (koppints a váltáshoz)",
                badgeText = "${uiState.defaultRestSec} mp",
                onClick = onCycleRestTimer
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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(strings.soundAlertTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("Pihenőidő lejártakor", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
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
                            Text("Rezgés", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("Haptikus visszajelzés pihenőidő végén", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = {
                                vibrationEnabled = it
                                onShowBanner(if (it) "Rezgés bekapcsolva" else "Rezgés kikapcsolva")
                            }
                        )
                    }
                }
            }
        }

        item {
            SettingsActionRowCard(
                title = "Edzés beállításai és progresszió",
                subtitle = "${uiState.trainingStyleHu} • ${uiState.progressionRuleHu} • Pihenő: ${uiState.defaultRestSec} mp",
                badgeText = "Szerk.",
                onClick = { onToggleProgressionModal(true) }
            )
        }

        // Eszközök és adatok (Óra, Mentés, Engedélyek, Privát fotók)
        item {
            Text("Eszközök és adatok", style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
        }

        item {
            SettingsActionRowCard(
                title = strings.watchAndSyncTitle,
                subtitle = when {
                    uiState.watchAppAvailable -> "${uiState.watchName} • Wear OS adatkapcsolat"
                    uiState.watchConnected -> "Wear OS rendszerkapcsolat • TrainPilot óraapp nincs telepítve"
                    else -> "Nincs csatlakoztatott óra"
                },
                badgeText = if (uiState.watchAppAvailable) "Adatkapcsolat" else "Nincs kapcsolat",
                onClick = { onNavigateToSubScreen(SubScreen.WATCH_AND_SYNC) },
                testTag = "settings_open_watch_row"
            )
        }

        item {
            SettingsActionRowCard(
                title = strings.backupRestoreTitle,
                subtitle = "Helyi JSON / ZIP és Google Drive biztonsági mentés",
                badgeText = uiState.selectedBackupFormat,
                onClick = { onNavigateToSubScreen(SubScreen.BACKUP_RESTORE) },
                testTag = "settings_open_backup_row"
            )
        }

        item {
            SettingsActionRowCard(
                title = strings.permissionsTitle,
                subtitle = "GPS, Értesítések, Health Connect, Samsung Health",
                badgeText = "Kezelés",
                onClick = { onNavigateToSubScreen(SubScreen.PERMISSIONS_SCREEN) },
                testTag = "settings_open_permissions_row"
            )
        }

        item {
            SettingsActionRowCard(
                title = strings.privatePhotosTitle,
                subtitle = "Csak ezen az eszközön tárolva • Nem kerül felhőbe",
                badgeText = "${uiState.privatePhotoCount} fotó",
                onClick = { onNavigateToSubScreen(SubScreen.PRIVATE_PHOTOS) },
                testTag = "settings_open_photos_row"
            )
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
    isExpanded: Boolean? = null
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
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
                text = "Órakapcsolat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }

        item {
            StudioWatchInformationCard(
                data = uiState.watchInfoCardData,
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
                        text = "Aktív edzés vezérlése",
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
                                        text = "📱 Telefon vezérli",
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
                                    text = "Óra megjeleníti",
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
                                        text = "⌚ Óra vezérli",
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
                                    text = "Önálló edzés",
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
                            text = if (uiState.watchControlPending) "Óra ACK-ra vár…"
                                else if (uiState.phoneControlsWorkout) strings.handoverToWatchAction else strings.reclaimToPhoneAction,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!uiState.watchAppAvailable) Text(
                            "Az órára a párosított TrainPilot Wear OS alkalmazást telepítsd; a Galaxy Wearable önmagában nem elég.",
                            style = MaterialTheme.typography.bodySmall, color = palette.warning)
                        Text(
                            text = "• A pulzus és edzésadatok szinkronja órakapcsolatot és külön visszaigazolást igényel",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Text(
                            text = "• Offline folytatás: kapcsolat megszakadásakor az óra önállóan folytatja az edzést",
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
                text = "Egészségügyi szinkron",
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
                            text = if (uiState.permissionHealthConnectEnabled) "Engedélyezett: ${uiState.healthGrantedCount}/${uiState.healthPermissionCount}" else "Nincs olvasási engedély • engedélykérés",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.success
                        )
                        Text(
                            text = if (uiState.permissionHealthConnectEnabled) "Koppints a Health Connect szinkronizálásához" else "Pulzus, alvás, lépések, elégetett kalória, edzések",
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
                            text = if (uiState.permissionSamsungHealthEnabled) "Olvasási engedély igazolva" else "Nincs igazolt olvasási engedély",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.success
                        )
                        Text(
                            text = if (uiState.permissionSamsungHealthEnabled) "Koppints a Samsung Health szinkronizálásához" else "Koppints az engedély megadásához és szinkronhoz",
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

    LaunchedEffect(gs.lastDriveSyncIso, settingsState.maintenanceCompleted) {
        if (gs.lastDriveSyncIso.isNotBlank() || settingsState.maintenanceCompleted > 0) {
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
                            text = "Google Cloud Szinkron",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (gs.connectedEmail.isNotEmpty()) {
                                gs.connectedEmail
                            } else {
                                "Google Drive felhőmentés és Google Naptár szinkron"
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
                        text = (if (gs.connectedEmail.isNotEmpty()) "Csatlakoztatva" else "Nincs belépve") + if (cloudExpanded) " ▴" else " ▾",
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
                        text = "Fiók: ${gs.connectedName} (${gs.connectedEmail})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                }
                if (gs.lastDriveSyncIso.isNotEmpty()) {
                    Text(
                        text = "Utolsó Drive szinkron: ${gs.lastDriveSyncIso.take(19).replace('T', ' ')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
                if (gs.lastCalendarSyncIso.isNotEmpty()) {
                    Text(
                        text = "Utolsó Naptár szinkron: ${gs.lastCalendarSyncIso.take(19).replace('T', ' ')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
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
                        text = "Korábbi felhőmentések keresése és összefésülése",
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
                            text = "Drive szinkron",
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
                            text = "Naptár szinkron",
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
                            text = "Google-fiók lekapcsolása",
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
                            text = if (showMaintenanceOptions) "Karbantartás elrejtése" else "Felhő karbantartás",
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
                    text = "Jelentkezz be Google-fiókkal az automatikus többeszközös Drive-szinkronhoz (edzésnapló, tervek, privát fotók) és a dedikált edzésnaptárhoz.",
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
                        text = "Bejelentkezés Google-fiókkal",
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
                    modifier = Modifier.fillMaxWidth()
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

            if (!gs.errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
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
                onShowBanner("Mentés készítése...")
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
                onShowBanner("Visszaállítás fájlból...")
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
                    Text("Mentés tartalma", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Text("✓ Edzésnapló és PR rekordok (${uiState.loggedWorkouts.size} edzés)", color = palette.success)
                    Text("✓ Saját programok és gyakorlatok (${uiState.programs.size} program)", color = palette.success)
                    Text("✓ Beállítások és 32 színpaletta profil", color = palette.success)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Health Connect, Samsung Health és órás egészségadatok", color = palette.textPrimary, modifier = Modifier.weight(1f))
                        Switch(checked = uiState.includeHealthInBackup, enabled = !uiState.backupHealthConsentSaving, onCheckedChange = onToggleIncludeHealth)
                    }
                    Text("Alapból kikapcsolva. JSON/ZIP: tárolt egészségnapló és edzésösszesítések. Drive: az edzéshez mentett egészségadatok; a teljes napi egészségnapló nem kerül Drive-ra. Kikapcsolás nem törli a helyi adatokat vagy a korábbi mentéseket.",
                        color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
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
                        text = "Korábbi RepForge app mentés behozása",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary
                    )
                    Text(
                        text = "Teljes kompatibilitás a korábbi RepForge (.zip és .json) biztonsági mentésekkel: edzésnapló, egyéni programok, gyakorlatok, testsúlynapló és privát fotók automatikus konvertálása.",
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
                            text = "RepForge ZIP / JSON mentés importálása",
                            color = palette.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        item {
            Button(
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
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.Backup, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.selectedBackupFormat == "DRIVE" && gs.connectedEmail.isEmpty()) {
                        "Bejelentkezés Google-fiókkal (Drive)"
                    } else {
                        "Mentés indítása (${uiState.selectedBackupFormat})"
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
                Text("Visszaállítás fájlból (TrainPilot / RepForge)", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
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
            PermissionToggleCard("GPS és helyadatok", "Kültéri futás és útvonal rögzítése", uiState.permissionGpsEnabled) {
                onTogglePermission("gps")
            }
        }
        item {
            PermissionToggleCard("Központi helymeghatározás", "Android rendszer-helyszolgáltatás beállításai", uiState.permissionCentralLocEnabled) {
                onTogglePermission("central")
            }
        }
        item {
            PermissionToggleCard("Értesítések", "Pihenőidő és aktív edzés állapotsor", uiState.permissionNotificationsEnabled) {
                onTogglePermission("notif")
            }
        }
        item {
            PermissionToggleCard("Health Connect (${uiState.healthGrantedCount}/${uiState.healthPermissionCount})", "Pulzus, alvás, lépések és testsúly szinkron", uiState.permissionHealthConnectEnabled) {
                onTogglePermission("hc_manage")
            }
        }
        item {
            PermissionToggleCard("Samsung Health közvetlen híd", "Samsung Health olvasási engedélyének kezelése", uiState.permissionSamsungHealthEnabled) {
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

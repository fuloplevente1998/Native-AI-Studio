package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.viewmodel.TrainPilotUiState

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
    onOpenGoalsDialog: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
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

    var recentMeasurements by remember {
        val baseWeight = if (uiState.currentBodyWeightKg > 20f) uiState.currentBodyWeightKg else 74.5f
        val d1 = today.minusDays(18)
        val d2 = today.minusDays(12)
        val d3 = today.minusDays(6)
        val d4 = today
        mutableStateOf(
            listOf(
                WeightMeasurementEntry("w_1", "${"%.1f".format(baseWeight + 1.2f).replace('.', ',')} kg", baseWeight + 1.2f, d1.format(dotFormatter), d1),
                WeightMeasurementEntry("w_2", "${"%.1f".format(baseWeight + 0.7f).replace('.', ',')} kg", baseWeight + 0.7f, d2.format(dotFormatter), d2),
                WeightMeasurementEntry("w_3", "${"%.1f".format(baseWeight + 0.3f).replace('.', ',')} kg", baseWeight + 0.3f, d3.format(dotFormatter), d3),
                WeightMeasurementEntry("w_4", "${"%.1f".format(baseWeight).replace('.', ',')} kg", baseWeight, d4.format(dotFormatter), d4)
            ).sortedByDescending { it.parsedDate }
        )
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
                    fontWeight = FontWeight.Bold
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
                OutlinedButton(
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
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. OUTER GOLD-BORDERED CONTAINER: "Mai állapot" (6 daily tiles + dynamic today date)
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                .border(1.dp, palette.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
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
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                    }

                    var expandedTodayTileKey by remember { mutableStateOf<String?>(null) }

                    // Row 1 of 3 tiles: Alvás | Aktív kalória | Pulzus
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MainTodayStatusTile(
                            icon = Icons.Default.Bedtime,
                            title = "Alvás",
                            value = uiState.sleepHoursDecimal,
                            onClick = {
                                expandedTodayTileKey = if (expandedTodayTileKey == "sleep") null else "sleep"
                                onToggleHealthSection("journal")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        MainTodayStatusTile(
                            icon = Icons.Default.LocalFireDepartment,
                            title = "Aktív kalória",
                            value = "${uiState.activeCaloriesKcal} kcal",
                            onClick = {
                                expandedTodayTileKey = if (expandedTodayTileKey == "calories") null else "calories"
                                onToggleHealthSection("extra")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        MainTodayStatusTile(
                            icon = Icons.Default.Favorite,
                            title = "Pulzus",
                            value = if (uiState.currentHrBpm > 0) "${uiState.currentHrBpm} bpm" else "—",
                            onClick = {
                                expandedTodayTileKey = if (expandedTodayTileKey == "pulse") null else "pulse"
                                onToggleHealthSection("pulse")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2 of 3 tiles: Lépések | Mai edzés | Regeneráció
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MainTodayStatusTile(
                            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                            title = "Lépések",
                            value = "${uiState.dailySteps}",
                            onClick = {
                                expandedTodayTileKey = if (expandedTodayTileKey == "steps") null else "steps"
                                onToggleHealthSection("journal")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        MainTodayStatusTile(
                            icon = Icons.Default.FitnessCenter,
                            title = "Mai edzés",
                            value = "${uiState.loggedWorkouts.count { it.dateLabelHu.contains(todayDotLabel) || it.dateLabelHu.contains(todayHuLabel) }}",
                            onClick = {
                                expandedTodayTileKey = if (expandedTodayTileKey == "workout") null else "workout"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        MainTodayStatusTile(
                            icon = Icons.Default.Eco,
                            title = "Regeneráció",
                            value = if (uiState.coachReadinessScore > 0) "${uiState.coachReadinessScore}/100" else "—",
                            onClick = {
                                expandedTodayTileKey = if (expandedTodayTileKey == "recovery") null else "recovery"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Expandable Detail Dropdown Card when any of the 6 "Mai állapot" tiles is tapped
                    androidx.compose.animation.AnimatedVisibility(visible = expandedTodayTileKey != null) {
                        val activeKey = expandedTodayTileKey ?: "sleep"
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.background.copy(alpha = 0.75f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                                .testTag("health_today_tile_expanded_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val (titleStr, onActionClick) = when (activeKey) {
                                    "calories" -> listOf(
                                        "Aktív kalória részletek • ${uiState.activeCaloriesKcal} kcal",
                                        "Edzés + napi mozgás: ${uiState.activeCaloriesKcal} kcal | Alapanyagcsere: 1 780 kcal",
                                        "További Health-adatok megnyitása lent ▼"
                                    ) to { onToggleHealthSection("extra") }
                                    "pulse" -> listOf(
                                        "Pulzus részletek • Aktuális: ${if (uiState.currentHrBpm > 0) "${uiState.currentHrBpm} bpm" else "—"}",
                                        "Nyugalmi pulzus: ${uiState.restingHrBpm} bpm | Napi tartomány: 58–146 bpm",
                                        "24 órás Pulzustrend megtekintése lent ▼"
                                    ) to { onToggleHealthSection("pulse") }
                                    "steps" -> listOf(
                                        "Lépések részletek • ${uiState.dailySteps} / ${uiState.stepsGoal}",
                                        "Teljesítés: ${((uiState.dailySteps.toFloat() / uiState.stepsGoal.coerceAtLeast(1)) * 100).toInt()}% • Becsült táv: ${"%.1f".format(uiState.dailySteps * 0.00075f)} km",
                                        "Célok módosítása >"
                                    ) to { onOpenGoalsDialog() }
                                    "workout" -> listOf(
                                        "Mai edzések • ${uiState.loggedWorkouts.size} rögzített edzés",
                                        "Utolsó edzés: ${uiState.loggedWorkouts.firstOrNull()?.titleHu ?: "Még nincs rögzített edzés"}",
                                        "Edzésnapló részletek megnyitása >"
                                    ) to { onNavigateToSubScreen(SubScreen.LOG_WORKOUT_DETAIL) }
                                    "recovery" -> listOf(
                                        "Regeneráció & Készenlét • ${uiState.coachReadinessScore}/100",
                                        "Samsung Energia: ${uiState.samsungEnergyScore}/100 • Javaslat: 1–2 RIR kontrollált terhelés",
                                        "TrainPilot Coach megnyitása >"
                                    ) to { onNavigateToSubScreen(SubScreen.COACH_DETAIL) }
                                    else -> listOf(
                                        "Alvás részletek • ${uiState.sleepDurationLabel} (${uiState.sleepHoursDecimal})",
                                        "Mélyalvás: 1 ó 45 p • REM: 1 ó 50 p • Minőség: Jó",
                                        "Egészségnapló megnyitása lent ▼"
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
                                    Text(
                                        text = "Bezárás ▲",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textSecondary,
                                        modifier = Modifier.clickable { expandedTodayTileKey = null }
                                    )
                                }
                                Text(
                                    text = titleStr[1],
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.primary.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.primary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
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

        // 2. SCREENSHOT 9: "Heti egészség & edzés célok" CONTAINER (with Heti lépésszám, Aktív edzésidő, 24 órás pulzustrend & Testsúlynapló & Trend inside!)
        item {
            val weeklyStepsTarget = (uiState.stepsGoal * 7).coerceAtLeast(70000)
            val weeklyStepsCurrent = uiState.dailySteps
            val weeklyStepsPct = ((weeklyStepsCurrent.toFloat() / weeklyStepsTarget.toFloat()) * 100f).toInt().coerceIn(0, 100)

            val weeklyMinutesTarget = 180
            val weeklyMinutesCurrent = uiState.loggedWorkouts.sumOf { it.durationMinutes }
            val weeklyMinutesPct = ((weeklyMinutesCurrent.toFloat() / weeklyMinutesTarget.toFloat()) * 100f).toInt().coerceIn(0, 100)

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                    .testTag("health_weekly_status_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header row: "Heti egészség & edzés célok" + "🎯 Célok" pill button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Heti egészség & edzés célok",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Health Connect & Room gyorsítótár (Offline elérhető)",
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
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
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

                    // 2-Column Weekly Progress Cards: Heti lépésszám | Aktív edzésidő
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left: Heti lépésszám
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = palette.background.copy(alpha = 0.55f),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                        contentDescription = null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Heti lépésszám",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.textSecondary
                                    )
                                }
                                Text(
                                    text = "%,d".format(java.util.Locale.US, weeklyStepsCurrent),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cél: %,d".format(java.util.Locale.US, weeklyStepsTarget),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = "$weeklyStepsPct%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3B82F6)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(palette.surfaceElevated)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((weeklyStepsPct / 100f).coerceIn(0.02f, 1f))
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF3B82F6))
                                    )
                                }
                            }
                        }

                        // Right: Aktív edzésidő
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = palette.background.copy(alpha = 0.55f),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Aktív edzésidő",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.textSecondary
                                    )
                                }
                                Text(
                                    text = "$weeklyMinutesCurrent perc",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cél: $weeklyMinutesTarget perc",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = "$weeklyMinutesPct%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(palette.surfaceElevated)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((weeklyMinutesPct / 100f).coerceIn(0.02f, 1f))
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(palette.primary)
                                    )
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
                            .border(
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
                                    val peakLbl = if (uiState.currentHrBpm > 0) "${uiState.currentHrBpm} bpm" else "—"
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
                                    modifier = Modifier.border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                                ) {
                                    Text(
                                        text = if (uiState.expandedHealthPulseTrend) "▲ Részletek" else "▼ Részletek",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
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
                                    .border(1.dp, palette.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                    .clickable { onToggleHealthSection("pulse") }
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    val values = if (uiState.currentHrBpm > 0) {
                                        listOf(55f, 56f, 62f, 88f, 70f, 79f, 58f, 54f)
                                    } else {
                                        listOf(55f, 56f, 60f, 78f, 65f, 72f, 57f, 55f)
                                    }
                                    val minV = 50f
                                    val maxV = 95f
                                    val pts = values.mapIndexed { idx, v ->
                                        val x = (idx.toFloat() / (values.size - 1)) * size.width
                                        val norm = (v - minV) / (maxV - minV)
                                        val y = size.height - norm * size.height
                                        Offset(x, y)
                                    }
                                    val path = Path()
                                    pts.forEachIndexed { i, pt ->
                                        if (i == 0) path.moveTo(pt.x, pt.y)
                                        else {
                                            val prev = pts[i - 1]
                                            val cx = (prev.x + pt.x) / 2f
                                            path.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color(0xFFEF4444),
                                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                                    )
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
                                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
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
                                                value = if (uiState.currentHrBpm > 0) "${uiState.currentHrBpm + 25} bpm" else "—",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PulseInnerBox(
                                                label = "Min. pulzus",
                                                value = if (uiState.restingHrBpm > 0) "${(uiState.restingHrBpm - 6).coerceAtLeast(45)} bpm" else "—",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Text(
                                            text = "Health Connect / Samsung Health",
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
                            .border(
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
                                    modifier = Modifier.border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                                ) {
                                    Text(
                                        text = if (uiState.expandedHealthWeightLog) "▲ Részletek" else "▼ Részletek",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Full-width trend box with curve AND user-entered X-axis dates
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.surfaceCard.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, palette.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                    .clickable { onToggleHealthSection("weight") }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    val lineColor = palette.primary
                                    val pts = if (chronologicalMeasurements.isNotEmpty()) {
                                        chronologicalMeasurements.map { it.weightValue }
                                    } else {
                                        listOf(75.5f, 75.0f, 74.6f, 74.2f)
                                    }
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
                                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
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
                                                .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
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
                                        .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
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
                                                .border(
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
                                                    .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
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
                                                                .border(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
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
                    SummarySmallPill("Vérnyomás", "118/76 Hgmm", Modifier.weight(1f))
                    SummarySmallPill("Vércukor", "5,1 mmol/L", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummarySmallPill("Légzésszám", "14,2 /perc", Modifier.weight(1f))
                    SummarySmallPill("Távolság (ma)", "5,24 km", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummarySmallPill("Véroxigén (SpO₂)", "97,0 %", Modifier.weight(1f))
                    SummarySmallPill("Testhőmérséklet", "36,5 °C", Modifier.weight(1f))
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
                val journalDays = listOf(
                    "2026. okt. 27." to "Alvás: 6,2 h • Pulzus: 65 bpm • Lépések: 6 734 • 292 kcal",
                    "2026. okt. 26." to "Alvás: 9,4 h • Pulzus: 61 bpm • Lépések: 6 036 • 235 kcal",
                    "2026. okt. 25." to "Alvás: 4,2 h • Pulzus: 63 bpm • Lépések: 9 919 • 392 kcal"
                )
                journalDays.forEach { (d, summary) ->
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
            }
        }

        // 4. Expandable Section: Health Connect (Screenshot 1 + custom Health Connect badge)
        item {
            ExpandableHealthSectionCard(
                icon = Icons.Default.Link,
                title = "Health Connect",
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
                            .border(
                                width = if (uiState.primaryHealthSourceSamsung) 1.5.dp else 1.dp,
                                color = if (uiState.primaryHealthSourceSamsung) palette.primary else palette.cardBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onTogglePrimaryHealthSource() }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Samsung Health", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("● Közvetlen (1/2)", style = MaterialTheme.typography.labelSmall, color = palette.success)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surfaceElevated,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (!uiState.primaryHealthSourceSamsung) 1.5.dp else 1.dp,
                                color = if (!uiState.primaryHealthSourceSamsung) palette.primary else palette.cardBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onTogglePrimaryHealthSource() }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Health Connect", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("● 10/17 engedély", style = MaterialTheme.typography.labelSmall, color = palette.success)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Olvasott adatok: Lépések, Pulzus, Alvás, Aktív kalória, Testsúly, Testzsír, SpO₂, VO₂max, Edzések.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }

        // 4b. Direct Óra és szinkron (Galaxy Watch7) card on Egészség tab so it's 1-tap reachable
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
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
                        Text(
                            text = "Óra és szinkron (${uiState.watchName})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "● Csatlakoztatva • ${uiState.watchBatteryPercent}% • Edzés átadása órának",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.success
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
                            .border(1.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onTriggerHealthSync() }
                            .testTag("health_sync_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = strings.syncAction,
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
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToSubScreen(SubScreen.PERMISSIONS_SCREEN) }
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
                    text = "Utolsó sikeres szinkron: 2026. 10. 27. 14:59",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                Text(
                    text = "Samsung Health · közvetlen: 1/2",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
    }
}

@Composable
private fun MainTodayStatusTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.background.copy(alpha = 0.55f),
        modifier = modifier
            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
            .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
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
            .border(1.dp, if (expanded) palette.primary.copy(alpha = 0.6f) else palette.cardBorder, RoundedCornerShape(20.dp))
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
                            .border(1.dp, palette.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
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
                    imageVector = if (expanded) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
    val displayScore = if (uiState.coachReadinessScore > 0) uiState.coachReadinessScore else 78
    val samsungScore = if (uiState.samsungEnergyScore > 0) uiState.samsungEnergyScore else 84

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("coach_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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

        // Screenshot 4: Mai felkészültség (Arc gauge + OPTIMÁLIS KÉSZENLÉT + 3 distinct D10 readiness indicators)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(22.dp))
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
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.success.copy(alpha = 0.16f),
                            modifier = Modifier.border(1.dp, palette.success.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "OPTIMÁLIS KÉSZENLÉT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.success,
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
                            val ringCol = palette.success
                            Canvas(modifier = Modifier.size(98.dp)) {
                                drawArc(
                                    color = trackCol,
                                    startAngle = 135f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = ringCol,
                                    startAngle = 135f,
                                    sweepAngle = 270f * (displayScore / 100f),
                                    useCenter = false,
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$displayScore",
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
                                text = "A napi regeneráció és az akut izomterhelés alapján ma teljes értékű edzés végezhető 1–2 RIR tartalékkal.",
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
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("1. Mai Samsung...", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1)
                                Text("$samsungScore / 100", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text("Jó állapotban", style = MaterialTheme.typography.labelSmall, color = palette.success)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("2. TrainPilot saját...", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1)
                                Text("81 / 100", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text("HRV & alvás", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("3. Terheléssel...", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1)
                                Text("$displayScore / 100", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primary)
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
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kulcsjelek", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text("Élő szinkron", style = MaterialTheme.typography.labelMedium, color = palette.primary)
                    }
                    CoachSignalRow("Alvás", uiState.sleepDurationLabel, palette.success)
                    CoachSignalRow("Nyugalmi pulzus", if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—", palette.success)
                    CoachSignalRow("Rögzített edzések", "${uiState.loggedWorkouts.size} db", palette.primary)
                }
            }
        }

        // Javasolt edzés Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(22.dp))
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
                            Text("Ma • Teljes testes edzés A/B", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("Közepes intenzitás • ~ ${uiState.userWorkoutDurationMin} perc", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                    }

                    CoachExerciseAdviceRow("Guggolás", "Normál terhelés", palette.success) {
                        onOpenExerciseDetail("goblet_squat")
                    }
                    CoachExerciseAdviceRow("Fekvenyomás", "Kissé csökkentett terhelés", palette.primary) {
                        onOpenExerciseDetail("bench_press")
                    }
                    CoachExerciseAdviceRow("Evezés gumiszalaggal", "Könnyebb kezdés", palette.primary) {
                        onOpenExerciseDetail("band_row")
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
    onShowBanner: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val currentPaletteObj = TrainPilotPalettes.getById(uiState.selectedPaletteId)

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
            title = { Text("Edzés beállításai és progresszió", fontWeight = FontWeight.Bold) },
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
                                .border(1.dp, palette.primary, RoundedCornerShape(10.dp))
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
                OutlinedButton(onClick = { onToggleProgressionModal(false) }) {
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar(
                title = strings.settingsTitle,
                onBack = onBack,
                onOpenSettings = {}
            )
        }

        // Profile & Google Sync Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("TP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TrainPilot Profil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("Helyi Room adatbázis és felhő szinkron", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onShowBanner("Google Drive és Naptár szinkronizálva") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Szinkron most", color = palette.textPrimary)
                        }
                    }
                }
            }
        }

        // Megjelenés és nyelv (32 Palettes in 4 Expandable Columns + 4 Languages)
        item {
            Text(strings.appearanceAndLang, style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
        }

        item {
            SettingsActionRowCard(
                title = strings.themes32Title,
                subtitle = "${currentPaletteObj.localizedName(uiState.language)} • Koppints a teljes választóhoz",
                badgeText = "#${uiState.selectedPaletteId}",
                onClick = onOpenPalettePicker,
                testTag = "settings_open_palettes_row"
            )
        }

        // 4 Expandable Theme Categories inside Settings (Matt témák | Kontrasztos témák | Fényes / Glow témák | Világos témák)
        item {
            val allPalettes = TrainPilotPalettes.allPalettes
            val categories = listOf(
                Triple("Matt témák (Sötét / Prémium matt)", allPalettes.filter { it.id in 1..8 }, expandedMatteThemes) to { expandedMatteThemes = !expandedMatteThemes },
                Triple("Kontrasztos témák (Élénk kontraszt)", allPalettes.filter { it.id in 9..16 }, expandedContrastThemes) to { expandedContrastThemes = !expandedContrastThemes },
                Triple("Fényes / Glow témák (Neon & Glow)", allPalettes.filter { it.id in 17..24 }, expandedGlowThemes) to { expandedGlowThemes = !expandedGlowThemes },
                Triple("Világos témák (Nappali / Light)", allPalettes.filter { it.id in 25..32 }, expandedLightThemes) to { expandedLightThemes = !expandedLightThemes }
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { (catInfo, onToggleCat) ->
                    val (catTitle, catPalettes, isExpanded) = catInfo
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surfaceCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isExpanded) palette.primary.copy(alpha = 0.6f) else palette.cardBorder,
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleCat() },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
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

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
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
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
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
                subtitle = "${uiState.watchName} • Csatlakoztatva (${uiState.watchBatteryPercent}%)",
                badgeText = "Aktív",
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
    testTag: String = ""
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
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
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.textSecondary)
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
    onShowBanner: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("watch_and_sync_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, palette.primary.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        MockupWatchGraphicBadge(size = 64.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.watchName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "● Csatlakoztatva",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.success
                                )
                                Text(
                                    text = "Akkumulátor: ${uiState.watchBatteryPercent}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TrainPilot v2.4  •  Utolsó szinkron: ${uiState.lastWatchSyncLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Aktív edzés vezérlése (Mockup 4/4 #2 side-by-side mode cards + gold button + bullets)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        val phoneSelected = uiState.phoneControlsWorkout
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (phoneSelected) palette.primary.copy(alpha = 0.16f) else palette.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (phoneSelected) 1.5.dp else 1.dp,
                                    color = if (phoneSelected) palette.primary else palette.cardBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (!phoneSelected) onToggleControlMode()
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

                        val watchSelected = !uiState.phoneControlsWorkout
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (watchSelected) palette.primary.copy(alpha = 0.16f) else palette.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (watchSelected) 1.5.dp else 1.dp,
                                    color = if (watchSelected) palette.primary else palette.cardBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (!watchSelected) onToggleControlMode()
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
                            text = if (uiState.phoneControlsWorkout) strings.handoverToWatchAction else strings.reclaimToPhoneAction,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "• Pulzus, sorozatok és pihenőidő valós időben szinkronizálva",
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
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onShowBanner("Health Connect szinkronizáció aktív") }
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
                            text = "● Aktív • Olvasás és írás engedélyezve (10/17)",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.success
                        )
                        Text(
                            text = "Pulzus, alvás, lépések, elégetett kalória, edzések",
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
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onShowBanner("Samsung Health közvetlen híd szinkronizálva") }
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
                            text = "● Szinkronizálva • Közvetlen kapcsolat (1/2)",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.success
                        )
                        Text(
                            text = "Utolsó frissítés: ma 14:59 • Energia pontszám & testsúly",
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
    onImportBackup: ((android.net.Uri) -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            .border(1.dp, if (sel) palette.primary else palette.cardBorder, RoundedCornerShape(14.dp))
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
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
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
                        Text("Egészségügyi előzmények (Health)", color = palette.textPrimary)
                        Switch(checked = uiState.includeHealthInBackup, onCheckedChange = onToggleIncludeHealth)
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
                    .border(1.2.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
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
                    OutlinedButton(
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
                        onShowBanner("Google Drive mentés előkészítve")
                    } else {
                        exportLauncher.launch(defaultFileName)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.Backup, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mentés indítása (${uiState.selectedBackupFormat})", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            PermissionToggleCard("Központi helymeghatározás", "Pontos távolságmérés a háttérben", uiState.permissionCentralLocEnabled) {
                onTogglePermission("central")
            }
        }
        item {
            PermissionToggleCard("Értesítések", "Pihenőidő és aktív edzés állapotsor", uiState.permissionNotificationsEnabled) {
                onTogglePermission("notif")
            }
        }
        item {
            PermissionToggleCard("Health Connect (10/17)", "Pulzus, alvás, lépések és testsúly szinkron", uiState.permissionHealthConnectEnabled) {
                onTogglePermission("hc")
            }
        }
        item {
            PermissionToggleCard("Samsung Health közvetlen híd", "Energia pontszám és Galaxy Watch7 adatok", uiState.permissionSamsungHealthEnabled) {
                onTogglePermission("samsung")
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
            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
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
    val palette = LocalTrainPilotPalette.current
    var capturedPhotoStatus by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedPhotoStatus = "Készült foto: ${bitmap.width}x${bitmap.height}"
            onShowBanner("Privát edzésfotó rögzítve az eszközön")
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            capturedPhotoStatus = "Importálva: ${uri.lastPathSegment}"
            onShowBanner("Fotó hozzáadva a privát képtárhoz")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("private_photos_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SubScreenTopBar(
                title = strings.privatePhotosTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = palette.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = capturedPhotoStatus ?: "Csak ezen az eszközön tárolva • A privát edzésfotók nem kerülnek felhőbe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrivatePhotoPlaceholderCard("2026. okt. 11. • 17:48", "Teljes testes edzés A #1", Modifier.weight(1f))
                PrivatePhotoPlaceholderCard("2026. okt. 11. • 17:50", "Teljes testes edzés A #2", Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { cameraLauncher.launch(null) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kamera", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galéria", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PrivatePhotoPlaceholderCard(
    dateLabel: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surfaceCard,
        modifier = modifier.border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(42.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(caption, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        }
    }
}

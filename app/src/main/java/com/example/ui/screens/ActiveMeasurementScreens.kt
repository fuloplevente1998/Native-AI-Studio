package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalTrainPilotStrings
import com.example.localization.LocalizedStrings
import com.example.model.ExerciseSetItem
import com.example.model.SubScreen
import com.example.ui.components.ExercisePostureMiniIllustration
import com.example.ui.components.MargitszigetDetailedMapBox
import com.example.ui.components.SmallExerciseVideoButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeasurementModeSwitcherRow(
    currentSubScreen: SubScreen,
    onSelectMode: (SubScreen) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val modes = listOf(
        SubScreen.ACTIVE_STRENGTH to "1. ${strings.modeStrengthLabel}",
        SubScreen.ACTIVE_TIMED to "2. ${strings.modeTimedLabel}",
        SubScreen.ACTIVE_BILATERAL to "3. ${strings.modeBilateralLabel}",
        SubScreen.ACTIVE_OUTDOOR_GPS to "4. ${strings.modeOutdoorGpsLabel}",
        SubScreen.INTERVAL_SETTINGS to "5. ${strings.modeIntervalsLabel}",
        SubScreen.WORKOUT_SUMMARY to "6. ${strings.modeSummaryLabel}"
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        modes.forEach { (mode, label) ->
            val selected = currentSubScreen == mode
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) palette.primary else palette.surfaceElevated,
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectMode(mode) }
                    .testTag("measurement_mode_${mode.name.lowercase()}")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) palette.onPrimary else palette.textSecondary
                    )
                }
            }
        }
    }
}

/**
 * 1. SÚLYZÓS EDZÉS (Mockup 2/4 #1)
 */
@Composable
fun ActiveStrengthWorkoutScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit,
    onToggleSetCompleted: (Int) -> Unit,
    onAdjustSetValues: (Int, Float, Int, Int) -> Unit,
    onUndoLastSet: () -> Unit,
    onAdjustRestTimer: (Int) -> Unit,
    onToggleRestRunning: () -> Unit,
    onSkipRest: () -> Unit,
    onOpenExerciseDetail: (String) -> Unit,
    onUpdateSetValuesExact: (Int, Float, Int, Int) -> Unit = { _, _, _, _ -> },
    onDeleteSet: (Int) -> Unit = {},
    onAddSet: () -> Unit = {},
    onToggleAutoMode: (Boolean) -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val restMin = uiState.restTimerRemainingSec / 60
    val restSec = uiState.restTimerRemainingSec % 60
    val formattedRest = String.format("%02d:%02d", restMin, restSec)

    // Interactive edit dialog state for filling in reps and weight
    var editingSet by remember { mutableStateOf<ExerciseSetItem?>(null) }
    var editWeightText by remember { mutableStateOf("") }
    var editRepsText by remember { mutableStateOf("") }
    var editRirText by remember { mutableStateOf("") }

    val targetSet = editingSet
    if (targetSet != null) {
        AlertDialog(
            onDismissRequest = { editingSet = null },
            containerColor = palette.surfaceCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = "✏️ ${targetSet.setNumber}. sorozat kitöltése",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Súly (kg):",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = editWeightText,
                        onValueChange = { editWeightText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_set_weight_field")
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("5.5", "8", "10", "14", "16", "20").forEach { presetKg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { editWeightText = presetKg }
                            ) {
                                Text(
                                    text = "$presetKg kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ismétlések száma:",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = editRepsText,
                        onValueChange = { editRepsText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_set_reps_field")
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("8", "10", "12", "15", "20").forEach { presetReps ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { editRepsText = presetReps }
                            ) {
                                Text(
                                    text = "$presetReps ism.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "RIR (Ráspórolt ismétlések):",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    OutlinedTextField(
                        value = editRirText,
                        onValueChange = { editRirText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_set_rir_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val kg = editWeightText.toFloatOrNull() ?: targetSet.weightKg
                        val reps = editRepsText.toIntOrNull() ?: targetSet.reps
                        val rir = editRirText.toIntOrNull() ?: targetSet.rir
                        onUpdateSetValuesExact(targetSet.setNumber, kg, reps, rir)
                        editingSet = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Mentés", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { editingSet = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Mégse", color = palette.textPrimary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("active_strength_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Exercise Header Card matching screenshot 3: Goblet guggolás + Tárcsa & Bemelegítés
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Goblet guggolás",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Láb • PER_HAND_DUMBBELL • 3×8–20 • Pihenő: 90 mp",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenExerciseDetail("goblet_squat") }
                ) {
                    Text(
                        text = "Tárcsa & Bemelegítés",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Section header
        item {
            Text(
                text = "Sorozatok",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }

        // Table column header matching image 3: # | Előző | kg/kar | /láb | RIR | ✓ | —
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", style = MaterialTheme.typography.labelSmall, color = palette.textMuted, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                Text("Előző", style = MaterialTheme.typography.labelSmall, color = palette.textMuted, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                Text("kg/kar", style = MaterialTheme.typography.labelSmall, color = palette.textMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("/láb", style = MaterialTheme.typography.labelSmall, color = palette.textMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("RIR", style = MaterialTheme.typography.labelSmall, color = palette.textMuted, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                Text("✓", style = MaterialTheme.typography.labelSmall, color = palette.textMuted, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
                Text("—", style = MaterialTheme.typography.labelSmall, color = palette.textMuted, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
            }
        }

        // Set rows (matching image 3)
        items(uiState.activeSets, key = { it.setNumber }) { setItem ->
            val isActiveSet = !setItem.completed && uiState.activeSets.firstOrNull { !it.completed }?.setNumber == setItem.setNumber
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isActiveSet) 1.5.dp else 1.dp,
                        color = if (isActiveSet) palette.primary else palette.cardBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Top row with values
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Yellow circle number
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(palette.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${setItem.setNumber}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.onPrimary
                            )
                        }

                        // 2. Previous column text
                        Text(
                            text = "${setItem.weightKg}×${setItem.reps}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            modifier = Modifier.width(56.dp),
                            textAlign = TextAlign.Center
                        )

                        // 3. kg/kar input pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.background,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .padding(horizontal = 2.dp)
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    editingSet = setItem
                                    editWeightText = setItem.weightKg.toString()
                                    editRepsText = setItem.reps.toString()
                                    editRirText = setItem.rir.toString()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (setItem.weightKg % 1f == 0f) "${setItem.weightKg.toInt()}" else "${setItem.weightKg}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                            }
                        }

                        // 4. /láb (reps) input pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.background,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .padding(horizontal = 2.dp)
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    editingSet = setItem
                                    editWeightText = setItem.weightKg.toString()
                                    editRepsText = setItem.reps.toString()
                                    editRirText = setItem.rir.toString()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${setItem.reps}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                            }
                        }

                        // 5. RIR input pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.background,
                            modifier = Modifier
                                .weight(0.9f)
                                .height(38.dp)
                                .padding(horizontal = 2.dp)
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val nextRir = if (setItem.rir >= 3) 0 else setItem.rir + 1
                                    onAdjustSetValues(setItem.setNumber, 0f, 0, nextRir - setItem.rir)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (setItem.rir > 0) "${setItem.rir}" else "0",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (setItem.rir > 0) palette.textPrimary else palette.textSecondary
                                )
                            }
                        }

                        // 6. OK button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (setItem.completed) palette.primary else palette.surfaceElevated,
                            modifier = Modifier
                                .size(width = 40.dp, height = 38.dp)
                                .border(1.dp, if (setItem.completed) palette.primary else palette.cardBorder, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleSetCompleted(setItem.setNumber) }
                                .testTag("toggle_set_${setItem.setNumber}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "OK",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (setItem.completed) palette.onPrimary else palette.textPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // 7. Red circle minus button (removes set)
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6B1A1A),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { onDeleteSet(setItem.setNumber) }
                                .testTag("delete_set_${setItem.setNumber}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "—",
                                    color = Color(0xFFFF8A80),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row of quick adjustment chips matching screenshot 3
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SmallAdjustChip("-2.5 kg") { onAdjustSetValues(setItem.setNumber, -2.5f, 0, 0) }
                        SmallAdjustChip("+2.5 kg") { onAdjustSetValues(setItem.setNumber, 2.5f, 0, 0) }
                        SmallAdjustChip("-1 ism.") { onAdjustSetValues(setItem.setNumber, 0f, -1, 0) }
                        SmallAdjustChip("+1 ism.") { onAdjustSetValues(setItem.setNumber, 0f, 1, 0) }
                        SmallAdjustChip("RIR 0") { onUpdateSetValuesExact(setItem.setNumber, setItem.weightKg, setItem.reps, 0) }
                        SmallAdjustChip("RIR 1") { onUpdateSetValuesExact(setItem.setNumber, setItem.weightKg, setItem.reps, 1) }
                        SmallAdjustChip("RIR 2") { onUpdateSetValuesExact(setItem.setNumber, setItem.weightKg, setItem.reps, 2) }
                    }
                }
            }
        }

        // Add new set button
        item {
            OutlinedButton(
                onClick = onAddSet,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Új sorozat hozzáadása", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        // TrainPilot Coach & Valós RIR / Progressziós Motor javaslat kártya
        item {
            var selectedEngineMode by remember(uiState.progressionRuleHu) {
                mutableStateOf(
                    when {
                        uiState.progressionRuleHu.contains("Lineáris", ignoreCase = true) -> "Lineáris progresszió"
                        uiState.progressionRuleHu.contains("Dupla", ignoreCase = true) -> "Dupla progresszió (8–12 ism.)"
                        else -> "RIR-alapú autoreguláció"
                    }
                )
            }
            var modeDropdownOpen by remember { mutableStateOf(false) }

            val baseWeight = uiState.activeSets.firstOrNull()?.weightKg ?: 16f
            val minReps = uiState.activeSets.minOfOrNull { it.reps } ?: 10
            val maxReps = uiState.activeSets.maxOfOrNull { it.reps } ?: 10
            val observedMinRir = uiState.activeSets.minOfOrNull { it.rir } ?: 2
            val observedAvgRir = if (uiState.activeSets.isNotEmpty()) {
                uiState.activeSets.map { it.rir }.average()
            } else 2.0
            val completedCount = uiState.activeSets.count { it.completed }
            val historyCount = uiState.loggedWorkouts.size
            val readinessScore = when {
                uiState.coachReadinessScore > 0 -> uiState.coachReadinessScore
                uiState.samsungEnergyScore > 0 -> uiState.samsungEnergyScore
                else -> 84
            }

            data class LiveCoachCalc(
                val actionTitle: String,
                val suggestedWeightKg: Float,
                val suggestedReps: Int,
                val suggestedRir: Int,
                val reasonText: String,
                val accentColor: Color
            )

            val engineCalc = when {
                readinessScore < 55 -> LiveCoachCalc(
                    actionTitle = "Könnyített terhelés (Regeneráció)",
                    suggestedWeightKg = (baseWeight - 2.5f).coerceAtLeast(0f),
                    suggestedReps = minReps.coerceAtMost(10),
                    suggestedRir = 2,
                    reasonText = "Alacsony napi készenlét ($readinessScore/100) miatt a Coach motor visszafogott terhelést és 2–3 RIR tartalékot javasol.",
                    accentColor = palette.secondary
                )
                selectedEngineMode.startsWith("RIR") -> when {
                    observedMinRir >= 3 -> LiveCoachCalc(
                        actionTitle = "Súlyemelés javasolt (+2.5 kg)",
                        suggestedWeightKg = baseWeight + 2.5f,
                        suggestedReps = minReps,
                        suggestedRir = 2,
                        reasonText = "Magas RIR tartalék (min. RIR $observedMinRir > cél 1–2 RIR). A motor +2.5 kg emelést számolt (${baseWeight} kg → ${baseWeight + 2.5f} kg) 2 RIR megtartásával.",
                        accentColor = palette.success
                    )
                    observedMinRir == 0 -> LiveCoachCalc(
                        actionTitle = "Terhelés korrekció (-2.5 kg • RIR 0)",
                        suggestedWeightKg = (baseWeight - 2.5f).coerceAtLeast(0f),
                        suggestedReps = minReps,
                        suggestedRir = 2,
                        reasonText = "Elérted a bukást (RIR 0 < cél 1–2 RIR). A túledzés elkerüléséhez a motor ${(baseWeight - 2.5f).coerceAtLeast(0f)} kg-ot és 1–2 RIR tartalékot javasol.",
                        accentColor = palette.danger
                    )
                    else -> LiveCoachCalc(
                        actionTitle = "Céltartományban (1–2 RIR) • +1 ism.",
                        suggestedWeightKg = baseWeight,
                        suggestedReps = (minReps + 1).coerceAtMost(20),
                        suggestedRir = observedMinRir,
                        reasonText = "A sorozatok RIR értéke pontosan a célzott 1–2 RIR zónában van (átlag RIR: ${String.format("%.1f", observedAvgRir)}). Tartsd a ${baseWeight} kg súlyt és lépj ${(minReps + 1).coerceAtMost(20)} ismétlésre.",
                        accentColor = palette.primary
                    )
                }
                selectedEngineMode.startsWith("Dupla") -> {
                    if (minReps >= 12 && observedMinRir >= 1) {
                        LiveCoachCalc(
                            actionTitle = "Felső határ elérve (12 ism.) → +2.5 kg",
                            suggestedWeightKg = baseWeight + 2.5f,
                            suggestedReps = 8,
                            suggestedRir = 2,
                            reasonText = "Minden sorozat elérte a 12 ismétlést biztonságos tartalékkal (RIR $observedMinRir). Dupla progresszió szerint lépj ${baseWeight + 2.5f} kg-ra 8 ismétléssel!",
                            accentColor = palette.success
                        )
                    } else {
                        LiveCoachCalc(
                            actionTitle = "Ismétlésszám építése (+1 ism.)",
                            suggestedWeightKg = baseWeight,
                            suggestedReps = (minReps + 1).coerceAtMost(12),
                            suggestedRir = 2,
                            reasonText = "Jelenlegi sorozatok: ${minReps}–${maxReps} ism. (${baseWeight} kg). A dupla progressziós motor a súly tartását és +1 ismétlést (${(minReps + 1).coerceAtMost(12)} ism.) javasol 12 ismétlésig.",
                            accentColor = palette.primary
                        )
                    }
                }
                else -> {
                    if (minReps >= 8 && observedMinRir >= 1) {
                        LiveCoachCalc(
                            actionTitle = "Lineáris súlyemelés (+2.5 kg)",
                            suggestedWeightKg = baseWeight + 2.5f,
                            suggestedReps = minReps,
                            suggestedRir = 2,
                            reasonText = "Stabil teljesítmény (${minReps} ism., RIR $observedMinRir, készenlét $readinessScore/100). A lineáris motor +2.5 kg súlyemelést javasol (${baseWeight} kg → ${baseWeight + 2.5f} kg).",
                            accentColor = palette.success
                        )
                    } else {
                        LiveCoachCalc(
                            actionTitle = "Alapsúly stabilizálása (${baseWeight} kg)",
                            suggestedWeightKg = baseWeight,
                            suggestedReps = minReps.coerceAtLeast(8),
                            suggestedRir = 2,
                            reasonText = "Érd el a stabil min. 8 ismétlést 1–2 RIR tartalékkal a következő lineáris súlyemelés előtt.",
                            accentColor = palette.primary
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, engineCalc.accentColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .testTag("active_workout_coach_engine_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TrainPilot Coach • RIR & Progressziós Motor",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.primary
                            )
                            Text(
                                text = "Előzmény: $historyCount edzés • Kész: $completedCount/${uiState.activeSets.size} sorozat • Készenlét: $readinessScore/100",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }

                        // Floating dropdown selector for progression mode (opens on top of content)
                        Box {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier
                                    .border(1.dp, if (modeDropdownOpen) palette.primary else palette.cardBorder, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { modeDropdownOpen = !modeDropdownOpen }
                                    .testTag("coach_engine_mode_dropdown_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = when {
                                            selectedEngineMode.startsWith("RIR") -> "RIR-mód"
                                            selectedEngineMode.startsWith("Dupla") -> "Dupla prog."
                                            else -> "Lineáris"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = if (modeDropdownOpen) "▲" else "▼",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.primary
                                    )
                                }
                            }

                            androidx.compose.material3.DropdownMenu(
                                expanded = modeDropdownOpen,
                                onDismissRequest = { modeDropdownOpen = false },
                                containerColor = palette.background,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, palette.primary.copy(alpha = 0.7f))
                            ) {
                                listOf(
                                    "RIR-alapú autoreguláció",
                                    "Dupla progresszió (8–12 ism.)",
                                    "Lineáris progresszió"
                                ).forEach { modeOption ->
                                    val isSel = selectedEngineMode == modeOption
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = modeOption,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) palette.primary else palette.textPrimary
                                                )
                                                if (isSel) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("✓", color = palette.primary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedEngineMode = modeOption
                                            modeDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Live telemetry badges: Alapsúly -> Javasolt | Átlag RIR | Cél ismétlés
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Súly javaslat", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                Text(
                                    text = "${baseWeight} → ${engineCalc.suggestedWeightKg} kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = engineCalc.accentColor
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("RIR analízis", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                Text(
                                    text = "Átl. ${String.format("%.1f", observedAvgRir)} (Cél: ${engineCalc.suggestedRir})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Cél ism.", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                Text(
                                    text = "${engineCalc.suggestedReps} ism.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.primary
                                )
                            }
                        }
                    }

                    // Recommendation summary & explanation
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.background,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = engineCalc.actionTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = engineCalc.accentColor
                            )
                            Text(
                                text = engineCalc.reasonText,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    // Apply Coach recommendation button
                    Button(
                        onClick = {
                            val targets = uiState.activeSets.filter { !it.completed }.ifEmpty { uiState.activeSets }
                            targets.forEach { s ->
                                onUpdateSetValuesExact(
                                    s.setNumber,
                                    engineCalc.suggestedWeightKg,
                                    engineCalc.suggestedReps,
                                    engineCalc.suggestedRir
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("apply_coach_engine_advice_btn")
                    ) {
                        Text(
                            text = "Coach javaslat alkalmazása (${engineCalc.suggestedWeightKg} kg × ${engineCalc.suggestedReps} • RIR ${engineCalc.suggestedRir})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Rest Timer Card (Pihenőidő 00:52)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pihenőidő",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formattedRest,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Automatikus indítás • Rezgés a végén",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }

                        // Circular Rest Ring
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable { onToggleRestRunning() },
                            contentAlignment = Alignment.Center
                        ) {
                            val progress = (uiState.restTimerRemainingSec.toFloat() / uiState.defaultRestSec.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val ringColor = palette.primary
                            val trackColor = palette.surfaceElevated
                            Canvas(modifier = Modifier.size(60.dp)) {
                                drawArc(
                                    color = trackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = ringColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAdjustRestTimer(-15) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-15 mp", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onAdjustRestTimer(15) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+15 mp", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onSkipRest,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                        ) {
                            Text(strings.skipAction, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        }

        // Fixed Sticky Bottom Bar: < Előző | Következő > (remains visible while scrolling, no Auto Mode toggle)
        Surface(
            color = palette.surfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.cardBorder, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("active_prev_exercise_btn")
                ) {
                    Text("< Előző", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("active_next_exercise_btn")
                ) {
                    Text("Következő >", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SetValuePill(
    valueText: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.background.copy(alpha = 0.65f),
        modifier = Modifier
            .width(64.dp)
            .border(
                width = 1.dp,
                color = if (highlighted) palette.primary else palette.cardBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
    ) {
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun SmallAdjustChip(
    label: String,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = palette.background,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * 2. IDŐALAPÚ EDZÉS (Mockup 2/4 #2 - Plank)
 */
@Composable
fun ActiveTimedWorkoutScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var currentSec by remember { mutableIntStateOf(uiState.timedCurrentSec) }
    var isRunning by remember { mutableStateOf(false) }
    var targetSec by remember { mutableIntStateOf(45) }
    var currentSetIndex by remember { mutableIntStateOf(2) }
    var completedSets by remember { mutableStateOf(listOf(45)) }

    // Live ticking stopwatch coroutine loop
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000L)
            currentSec++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("active_timed_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
            SubScreenTopBar(
                title = "Teljes test A • 3 / 8 gyakorlat",
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }
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
                        ExercisePostureMiniIllustration(
                            exerciseName = "Plank",
                            size = 54.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Plank",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Cél: $currentSetIndex. sorozat • Célidő: $targetSec mp",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isRunning) palette.primary.copy(alpha = 0.2f) else palette.secondary.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = if (isRunning) palette.primary else palette.secondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isRunning) "Stopper fut ●" else "Stopper készenlétben",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isRunning) palette.primary else palette.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        SmallExerciseVideoButton(exerciseName = "Plank")
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = if (targetSec > 0) (currentSec.toFloat() / targetSec).coerceIn(0f, 1f) else 0f
                        val primaryColor = if (currentSec >= targetSec) palette.success else palette.primary
                        val trackColor = palette.surfaceElevated
                        Canvas(modifier = Modifier.size(190.dp)) {
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = primaryColor,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val min = currentSec / 60
                            val sec = currentSec % 60
                            Text(
                                text = String.format("%02d:%02d", min, sec),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            val remaining = (targetSec - currentSec).coerceAtLeast(0)
                            Text(
                                text = if (currentSec >= targetSec) "Cél teljesítve! 🎉" else "Hátra: $remaining mp",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (currentSec >= targetSec) palette.success else palette.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Cél: 00:$targetSec",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Quick adjustment chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SmallAdjustChip("-5 mp") { currentSec = (currentSec - 5).coerceAtLeast(0) }
                        SmallAdjustChip("+5 mp") { currentSec += 5 }
                        SmallAdjustChip("+15 mp") { currentSec += 15 }
                        SmallAdjustChip("+30 mp") { currentSec += 30 }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stopwatch Controls: Start/Pause + Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { isRunning = !isRunning },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) palette.primary else palette.success,
                                contentColor = palette.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .testTag("toggle_timed_stopper")
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRunning) "Szünet" else "Stopper indítása",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                isRunning = false
                                currentSec = 0
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = palette.textPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.resetAction, color = palette.textPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Set complete button
                    Button(
                        onClick = {
                            isRunning = false
                            completedSets = completedSets + currentSec
                            currentSetIndex++
                            currentSec = 0
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.surfaceElevated,
                            contentColor = palette.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sorozat rögzítése (${String.format("%02d:%02d", currentSec / 60, currentSec % 60)}) ✓",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Target Preset selector
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Célidő beállítása:",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(30, 45, 60, 90).forEach { sec ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (targetSec == sec) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { targetSec = sec }
                            ) {
                                Text(
                                    text = "$sec mp",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetSec == sec) palette.onPrimary else palette.textPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Completed sets list
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Eddigi sorozatok:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary
                    )
                    completedSets.forEachIndexed { index, duration ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${index + 1}. sorozat", color = palette.textSecondary)
                            Text(
                                text = String.format("%02d:%02d ✓", duration / 60, duration % 60),
                                color = palette.success,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "$currentSetIndex. sorozat (Aktuális)",
                            color = palette.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format("%02d:%02d", currentSec / 60, currentSec % 60),
                            color = palette.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        }

        // Fixed Sticky Bottom navigation buttons
        Surface(
            color = palette.surfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.cardBorder, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("< Előző", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier.weight(1.3f).height(48.dp)
                ) {
                    Text("Következő >", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 3. KÉTOLDALI MÉRÉS (2 STOPPER OLDALANKÉNT: Bal és Jobb oldal külön-külön stopperrel)
 */
@Composable
fun ActiveBilateralWorkoutScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit
) {
    val palette = LocalTrainPilotPalette.current

    // Left side stopwatch state
    var leftSec by remember { mutableIntStateOf(uiState.bilateralLeftSec) }
    var leftRunning by remember { mutableStateOf(false) }
    var leftCompleted by remember { mutableStateOf(false) }

    // Right side stopwatch state
    var rightSec by remember { mutableIntStateOf(uiState.bilateralRightSec) }
    var rightRunning by remember { mutableStateOf(false) }
    var rightCompleted by remember { mutableStateOf(false) }

    // Ticking coroutine for Left Side Stopwatch
    LaunchedEffect(leftRunning) {
        while (leftRunning) {
            delay(1000L)
            leftSec++
        }
    }

    // Ticking coroutine for Right Side Stopwatch
    LaunchedEffect(rightRunning) {
        while (rightRunning) {
            delay(1000L)
            rightSec++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("active_bilateral_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
            SubScreenTopBar(
                title = "Oldalsó plank • Kétoldali stopper",
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }

        // Header Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExercisePostureMiniIllustration(
                        exerciseName = "Oldalsó plank",
                        size = 50.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Oldalsó plank",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "2 külön stopper oldalanként • Bal és Jobb oldal",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                    SmallExerciseVideoButton(exerciseName = "Oldalsó plank")
                }
            }
        }

        // 1. BAL OLDAL STOPPER (Left Side Stopwatch)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (leftRunning) 2.dp else 1.dp,
                        color = if (leftRunning) palette.primary else palette.cardBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (leftRunning) palette.primary else palette.surfaceElevated,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "B",
                                        fontWeight = FontWeight.Bold,
                                        color = if (leftRunning) palette.onPrimary else palette.textPrimary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Bal oldal stopper",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = if (leftRunning) "Időmérés folyamatban ●" else if (leftCompleted) "Befejezve ✓" else "Készenlétben",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (leftRunning) palette.primary else if (leftCompleted) palette.success else palette.textSecondary
                                )
                            }
                        }

                        // Digital display
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier.border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                        ) {
                            Text(
                                text = String.format("%02d:%02d", leftSec / 60, leftSec % 60),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (leftRunning) palette.primary else palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Left stopwatch controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { leftRunning = !leftRunning },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (leftRunning) palette.primary else palette.success,
                                contentColor = palette.onPrimary
                            ),
                            modifier = Modifier.weight(1.3f).height(44.dp)
                        ) {
                            Icon(
                                imageVector = if (leftRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (leftRunning) "Szünet" else "Indítás", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                leftRunning = false
                                leftSec = 0
                                leftCompleted = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.9f).height(44.dp)
                        ) {
                            Text("Nulláz", color = palette.textPrimary)
                        }

                        SmallAdjustChip("+5 mp") { leftSec += 5 }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (leftCompleted) palette.success.copy(alpha = 0.2f) else palette.surfaceElevated,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    leftRunning = false
                                    leftCompleted = true
                                }
                        ) {
                            Box(
                                modifier = Modifier.height(44.dp).padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (leftCompleted) "Kész ✓" else "Rögzít",
                                    color = if (leftCompleted) palette.success else palette.primary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. JOBB OLDAL STOPPER (Right Side Stopwatch)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (rightRunning) 2.dp else 1.dp,
                        color = if (rightRunning) palette.primary else palette.cardBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (rightRunning) palette.primary else palette.surfaceElevated,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "J",
                                        fontWeight = FontWeight.Bold,
                                        color = if (rightRunning) palette.onPrimary else palette.textPrimary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Jobb oldal stopper",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = if (rightRunning) "Időmérés folyamatban ●" else if (rightCompleted) "Befejezve ✓" else "Készenlétben",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (rightRunning) palette.primary else if (rightCompleted) palette.success else palette.textSecondary
                                )
                            }
                        }

                        // Digital display
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier.border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                        ) {
                            Text(
                                text = String.format("%02d:%02d", rightSec / 60, rightSec % 60),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (rightRunning) palette.primary else palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Right stopwatch controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { rightRunning = !rightRunning },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (rightRunning) palette.primary else palette.success,
                                contentColor = palette.onPrimary
                            ),
                            modifier = Modifier.weight(1.3f).height(44.dp)
                        ) {
                            Icon(
                                imageVector = if (rightRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (rightRunning) "Szünet" else "Indítás", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                rightRunning = false
                                rightSec = 0
                                rightCompleted = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.9f).height(44.dp)
                        ) {
                            Text("Nulláz", color = palette.textPrimary)
                        }

                        SmallAdjustChip("+5 mp") { rightSec += 5 }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (rightCompleted) palette.success.copy(alpha = 0.2f) else palette.surfaceElevated,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    rightRunning = false
                                    rightCompleted = true
                                }
                        ) {
                            Box(
                                modifier = Modifier.height(44.dp).padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (rightCompleted) "Kész ✓" else "Rögzít",
                                    color = if (rightCompleted) palette.success else palette.primary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. EGYENSÚLY ÉS SZIMMETRIA ELEMZÉS (Side balance comparison)
        item {
            val diff = kotlin.math.abs(leftSec - rightSec)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Oldalak egyensúlya",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (diff <= 3) palette.success.copy(alpha = 0.2f) else palette.primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (diff == 0) "Különbség: 0 mp" else "Eltérés: $diff mp",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (diff <= 3) palette.success else palette.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = when {
                            leftSec == 0 && rightSec == 0 -> "Indítsd el a stoppereket a bal és jobb oldal méréséhez."
                            diff == 0 -> "Tökéletes szimmetria a bal és jobb oldal között! 🎯"
                            diff <= 5 -> "Kiváló stabilitási egyensúly mindkét oldalon! 👍"
                            else -> "Figyelem: $diff másodperc különbség. Cél a gyengébb oldal fejlesztése!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        }

        }

        // Fixed Sticky Bottom Navigation Row
        Surface(
            color = palette.surfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.cardBorder, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("< Előző", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier.weight(1.3f).height(48.dp)
                ) {
                    Text("Következő >", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 4. KÜLTÉRI MÉRÉS (Mockup 2/4 #4 - Futás GPS)
 */
@Composable
fun ActiveOutdoorGpsScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var gpsActive by remember { mutableStateOf(true) }
    var isPaused by remember { mutableStateOf(false) }
    var lapCount by remember { mutableIntStateOf(2) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("active_outdoor_gps_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mockup 2/4 #4 Header: Pause icon + "Futás" + green "● GPS aktív • Jó jel" + Settings icon
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onBack() }
                        .padding(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surfaceCard,
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Szünet / Vissza",
                                tint = palette.textPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Futás • Kültéri mérés",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (gpsActive) palette.success else palette.primary)
                            )
                            Text(
                                text = if (gpsActive) "GPS aktív • Jó jel" else "GPS rögzítés szünetel",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (gpsActive) palette.success else palette.textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = strings.settingsTitle,
                        tint = palette.textSecondary
                    )
                }
            }
        }

        // 1. GPS Route Mini Map Card (Mockup 2/4 #4 "GPS útvonal" + "GPS rögzítés" switch + Margitsziget mini map)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = palette.success.copy(alpha = 0.16f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = palette.success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "GPS útvonal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Margitsziget futókör • Élő mini térkép",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "GPS rögzítés",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Switch(
                                checked = gpsActive,
                                onCheckedChange = { gpsActive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = palette.onPrimary,
                                    checkedTrackColor = palette.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    MargitszigetDetailedMapBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(205.dp),
                        interactiveControls = true
                    )
                }
            }
        }

        // 2. Hero Elapsed Timer + 3-column Live Metrics Card (Mockup 2/4 #4: 09:42 Eltelt idő | 1,62 km | 6:00 /km | 118 kcal)
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isPaused) "SZÜNETELTETVE" else "ELTELT IDŐ",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaused) palette.primary else palette.textSecondary
                    )
                    Text(
                        text = uiState.outdoorElapsedLabel,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutdoorMetricTile(
                            label = strings.distanceLabel,
                            value = "1,62 km",
                            subValue = "Cél: 5,0 km",
                            highlighted = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutdoorMetricTile(
                            label = strings.avgPaceLabel,
                            value = "${uiState.outdoorPaceLabel} /km",
                            subValue = "Aktuális: 5:52",
                            highlighted = false,
                            modifier = Modifier.weight(1f)
                        )
                        OutdoorMetricTile(
                            label = "Kalória & Pulzus",
                            value = "118 kcal",
                            subValue = "♥ 142 bpm",
                            highlighted = false,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = palette.cardBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Km-részidők (Splits) preview row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. km részidő: 5:58 /km  •  2. km (rész): 6:03 /km",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.primary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "$lapCount kör",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Control buttons: Szünet | Részidő (Kör) | Befejezés
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isPaused = !isPaused },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                tint = palette.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPaused) strings.continueBtn else strings.pauseAction,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { lapCount += 1 },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "+ Kör ($lapCount)",
                                color = palette.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.finishWorkoutAction, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutdoorMetricTile(
    label: String,
    value: String,
    subValue: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.background.copy(alpha = 0.55f),
        modifier = modifier.border(
            width = 1.dp,
            color = if (highlighted) palette.primary.copy(alpha = 0.6f) else palette.cardBorder,
            shape = RoundedCornerShape(14.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlighted) palette.primary else palette.textPrimary,
                maxLines = 1
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = palette.textSecondary,
                maxLines = 1
            )
        }
    }
}

/**
 * 5. INTERVALLUMOK BEÁLLÍTÁSA (Mockup 2/4 #5)
 */
@Composable
fun IntervalSettingsScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit,
    onSaveAndStartIntervals: (Int, Int, Int, Boolean, Boolean, Boolean) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var workSec by remember { mutableIntStateOf(uiState.intervalWorkSec) }
    var restSec by remember { mutableIntStateOf(uiState.intervalRestSec) }
    var rounds by remember { mutableIntStateOf(uiState.intervalRounds) }
    var sound by remember { mutableStateOf(uiState.soundAlertEnabled) }
    var vibration by remember { mutableStateOf(uiState.vibrationEnabled) }
    var autoStep by remember { mutableStateOf(uiState.autoStepIntervals) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("interval_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar(
                title = "Intervallumok beállítása",
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }
        item {
            IntervalStepperCard(
                label = "Munkaidő",
                valueText = "$workSec mp",
                onMinus = { workSec = (workSec - 5).coerceAtLeast(10) },
                onPlus = { workSec = (workSec + 5).coerceAtMost(300) }
            )
        }
        item {
            IntervalStepperCard(
                label = "Pihenőidő",
                valueText = "$restSec mp",
                onMinus = { restSec = (restSec - 5).coerceAtLeast(10) },
                onPlus = { restSec = (restSec + 5).coerceAtMost(300) }
            )
        }
        item {
            IntervalStepperCard(
                label = "Ismétlések (körök)",
                valueText = "$rounds kör",
                onMinus = { rounds = (rounds - 1).coerceAtLeast(1) },
                onPlus = { rounds = (rounds + 1).coerceAtMost(20) }
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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hangjelzés", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Switch(checked = sound, onCheckedChange = { sound = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rezgés", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Switch(checked = vibration, onCheckedChange = { vibration = it })
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    onSaveAndStartIntervals(workSec, restSec, rounds, sound, vibration, autoStep)
                    onSelectMeasurementMode(SubScreen.ACTIVE_TIMED)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.startIntervalsAction, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun IntervalStepperCard(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = com.example.localization.LocalTrainPilotStrings.current
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
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Text(valueText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onMinus,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surfaceElevated)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = strings.decreaseValueDesc, tint = palette.textPrimary)
                }
                IconButton(
                    onClick = onPlus,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = strings.increaseValueDesc, tint = palette.onPrimary)
                }
            }
        }
    }
}

/**
 * 6. EDZÉS BEFEJEZÉSE / ÖSSZEGZÉSE (Mockup 2/4 #6)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutSummaryScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onSelectMood: (Int) -> Unit,
    onUpdateNote: (String) -> Unit,
    onSaveWorkout: () -> Unit,
    onPauseAndExit: () -> Unit,
    onDiscardWorkout: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val moods = listOf(
        "😄 Túl könnyű",
        "🙂 Könnyű",
        "💪 Pont jó",
        "😤 Nehéz",
        "🥵 Túl nehéz",
        "⚠️ Fájdalom"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("workout_summary_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = strings.workoutSummaryTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = "Teljes test A • 8 / 8 gyakorlat",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
        }

        // 3 stat boxes: 38:24, 420 ism., 312 kcal
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatTile("Időtartam", "38:24", Modifier.weight(1f))
                SummaryStatTile("Volumen", "420 ism.", Modifier.weight(1f))
                SummaryStatTile("Kalória", "312 kcal", Modifier.weight(1f))
            }
        }

        // Mood selector ("Milyen volt az edzés?")
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Milyen volt az edzés?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        moods.forEachIndexed { idx, moodLabel ->
                            val selected = uiState.workoutSummaryMoodIndex == idx
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectMood(idx) }
                                    .testTag("mood_chip_$idx")
                            ) {
                                Text(
                                    text = moodLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) palette.onPrimary else palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = uiState.workoutSummaryNote,
                        onValueChange = onUpdateNote,
                        placeholder = { Text("Megjegyzés hozzáadása az edzéshez...", color = palette.textMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }

        // Action buttons: Mentés, Szüneteltetés és kilépés, Edzés elvetése
        item {
            Button(
                onClick = onSaveWorkout,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_workout_summary_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.saveAction, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onPauseAndExit,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Pause, contentDescription = null, tint = palette.textPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.pauseAndExitAction, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDiscardWorkout,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = palette.danger)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.discardWorkoutAction, color = palette.danger, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SummaryStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surfaceCard,
        modifier = modifier.border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }
    }
}

package com.example.ui.screens

import com.example.ui.components.trainPilotAnimatedExpansion

import com.repforge.app.ui.theme.trainPilotSoftGlow

import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import com.repforge.app.ui.theme.TrainPilotCardSpacing
import com.repforge.app.ui.theme.TrainPilotFrames

import com.example.model.workoutsOn
import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.example.ui.components.trainPilotAnimatedExpansion
import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.material3.LinearProgressIndicator

import com.example.model.localWorkoutDate

import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.foundation.BorderStroke
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.repforge.app.ui.theme.TrainPilotOutlinedButton as OutlinedButton
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
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.components.healthDetailsToggle
import com.example.ui.components.trainPilotChartMarkColor
import com.example.ui.components.trainPilotChartSelection
import com.example.ui.components.trainPilotChartTap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import com.repforge.app.ui.theme.TrainPilotTextButton as TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.viewmodel.TrainPilotUiState
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
    val parsedDate: java.time.LocalDate = java.time.LocalDate.now(),
    val photoCount: Int = 0
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
    onReadHealthSource: ((String) -> Unit)? = null,
    /** 2788: edit an existing weight entry in place (id, kg, date label). */
    onUpdateWeight: (String, Float, String) -> Unit = { _, kg, date -> onSaveNewWeight(kg, date) },
    onWeightPhotosUpdated: () -> Unit = {}
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
                        parsedDate = parsed,
                        photoCount = item.photoCount
                    )
                }.sortedByDescending { it.parsedDate }
            )
        } else {
            mutableStateOf(emptyList<WeightMeasurementEntry>())
        }
    }
    // Always sorted chronologically (oldest -> newest) by the user-entered date for the chart!
    val chronologicalMeasurements = remember(recentMeasurements, uiState.selectedWeightRangeIndex) {
        // 2785: only the selected period's measurements (no silent fallback to the full list).
        weightMeasurementsInRange(recentMeasurements, uiState.selectedWeightRangeIndex, today)
    }
    val compactMeasurements = remember(recentMeasurements) { recentWeightTrend(recentMeasurements) }
    val latestChronologicalEntry = compactMeasurements.lastOrNull()
    var editingMeasurement by remember { mutableStateOf<WeightMeasurementEntry?>(null) }
    var photoMeasurement by remember { mutableStateOf<WeightMeasurementEntry?>(null) }
    var pendingWeightPhoto by remember { mutableStateOf<com.repforge.app.NativeWeightPhotos.Request?>(null) }
    var weightPhotoError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val photoRepo = remember(context) { com.repforge.app.WorkoutPhotoRepository(context) }
    val weightDb = remember(context) { com.repforge.app.data.db.TrainPilotDatabase.getInstance(context) }

    fun receiveWeightPhoto(importPhoto: suspend () -> Result<com.repforge.app.StoredPhotoMetadata?>) {
        val request = pendingWeightPhoto ?: return
        pendingWeightPhoto = null
        coroutineScope.launch {
            try {
                if (!com.repforge.app.NativeWeightPhotos.importAndAttach(context, weightDb, request, importPhoto)) return@launch
                recentMeasurements = recentMeasurements.map {
                    if (it.id == request.weightId) it.copy(photoCount = it.photoCount + 1) else it
                }
                onWeightPhotosUpdated()
                weightPhotoError = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                weightPhotoError = true
            }
        }
    }

    val weightCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result -> receiveWeightPhoto { photoRepo.handleCameraResult(result.resultCode, result.data) } }

    val weightGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        receiveWeightPhoto {
            if (uri != null) photoRepo.importPickedUri(uri)
            else Result.success(null)
        }
    }

    if (uiState.showAddWeightDialog || editingMeasurement != null) {
        val initialVal = editingMeasurement?.weightValue?.toString()
            ?: if (uiState.currentBodyWeightKg > 0f) uiState.currentBodyWeightKg.toString() else ""
        var weightText by remember(editingMeasurement, uiState.showAddWeightDialog) { mutableStateOf(initialVal) }
        var dateText by remember(editingMeasurement, uiState.showAddWeightDialog) {
            mutableStateOf(editingMeasurement?.dateLabel ?: todayDotLabel)
        }
        com.example.ui.components.TrainPilotFormSheet(
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
                    com.example.ui.components.TrainPilotTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        singleLine = true,
                        label = { Text(tr(uiState.language, "Testsúly (kg)", "Weight (kg)", "Gewicht (kg)", "Greutate (kg)")) }
                    )
                    // 2785: the date opens a calendar; quick chips for the usual days.
                    var datePickerOpen by remember { mutableStateOf(false) }
                    val pickedDate = parseWeightEntryLocalDate(dateText, today)
                    Box(Modifier.fillMaxWidth()) {
                        com.example.ui.components.TrainPilotTextField(
                            value = pickedDate.format(dotFormatter),
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(tr(uiState.language, "Mérés dátuma", "Measurement date", "Messdatum", "Data măsurătorii")) },
                            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                        )
                        Box(Modifier.matchParentSize().padding(top = 8.dp)
                            .trainPilotClickable(RoundedCornerShape(14.dp)) { datePickerOpen = true }
                            .testTag("health_weight_date_field"))
                    }
                    if (datePickerOpen) com.example.ui.components.CompactDatePickerDialog(pickedDate, uiState.language.name.lowercase(),
                        onDismiss = { datePickerOpen = false },
                        onConfirm = { dateText = it.format(dotFormatter); datePickerOpen = false })
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            tr(uiState.language, "Ma", "Today", "Heute", "Azi") to today,
                            tr(uiState.language, "Tegnap", "Yesterday", "Gestern", "Ieri") to today.minusDays(1),
                            tr(uiState.language, "−3 nap", "−3 days", "−3 Tage", "−3 zile") to today.minusDays(3),
                            tr(uiState.language, "−7 nap", "−7 days", "−7 Tage", "−7 zile") to today.minusDays(7)
                        ).forEachIndexed { idx, (presetTitle, presetDate) ->
                            com.example.ui.components.TrainPilotFilterChip(selected = pickedDate == presetDate,
                                onClick = { dateText = presetDate.format(dotFormatter) },
                                label = { Text(presetTitle, maxLines = 1) },
                                modifier = Modifier.heightIn(min = 48.dp).testTag("health_weight_date_preset_$idx"))
                        }
                    }
                }
            },
            confirmButton = {
              // 2785: Mégse dark, Mentés in the theme accent (gold by default), both full-width halves.
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.example.ui.components.TrainPilotSecondaryButton(strings.cancelAction, onClick = {
                    editingMeasurement = null
                    onToggleAddWeightDialog(false)
                }, modifier = Modifier.weight(1f).testTag("health_weight_cancel"))
                com.example.ui.components.TrainPilotPrimaryButton(strings.saveAction, modifier = Modifier.weight(1f).testTag("health_weight_save"),
                    onClick = {
                        // 2788: captured before the sheet state is cleared below.
                        val editId = editingMeasurement?.id
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
                            // 2788: an edit keeps its ID (no duplicate row); a new entry gets a new one.
                            if (editId != null) onUpdateWeight(editId, parsed, normalizedDateLabel)
                            else onSaveNewWeight(parsed, normalizedDateLabel)
                        } else {
                            editingMeasurement = null
                            onToggleAddWeightDialog(false)
                        }
                    })
              }
            }
        )
    }

    photoMeasurement?.let { measurement ->
        com.example.ui.components.TrainPilotFormSheet(
            onDismissRequest = { photoMeasurement = null; pendingWeightPhoto = null },
            containerColor = palette.surfaceCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    tr(uiState.language, "Fotó a súlyméréshez", "Photo for weight entry", "Foto für Gewichtsmessung", "Fotografie pentru greutate"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${measurement.dateLabel} · ${measurement.weightLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    if (weightPhotoError) {
                        Text(
                            tr(uiState.language, "A fotó hozzáadása nem sikerült. Próbáld újra.",
                                "Could not add photo. Please try again.",
                                "Foto konnte nicht hinzugefügt werden. Bitte erneut versuchen.",
                                "Fotografia nu a putut fi adăugată. Încearcă din nou."),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.danger,
                            modifier = Modifier.testTag("health_weight_photo_error")
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                try {
                                    pendingWeightPhoto = com.repforge.app.NativeWeightPhotos.prepare(context, measurement.id)
                                    weightPhotoError = false
                                    weightCameraLauncher.launch(photoRepo.prepareCameraCaptureIntent())
                                } catch (_: Exception) {
                                    pendingWeightPhoto = null
                                    weightPhotoError = true
                                }
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("health_weight_photo_camera"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceElevated, contentColor = palette.textPrimary)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = palette.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(tr(uiState.language, "Kamera", "Camera", "Kamera", "Cameră"))
                        }
                        Button(
                            onClick = {
                                try {
                                    pendingWeightPhoto = com.repforge.app.NativeWeightPhotos.prepare(context, measurement.id)
                                    weightPhotoError = false
                                    weightGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } catch (_: Exception) {
                                    pendingWeightPhoto = null
                                    weightPhotoError = true
                                }
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("health_weight_photo_gallery"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceElevated, contentColor = palette.textPrimary)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = palette.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(tr(uiState.language, "Galéria", "Gallery", "Galerie", "Galerie"))
                        }
                    }
                }
            },
            confirmButton = {
                com.example.ui.components.TrainPilotPrimaryButton(
                    strings.closeAction,
                    onClick = { photoMeasurement = null; pendingWeightPhoto = null; weightPhotoError = false },
                    modifier = Modifier.fillMaxWidth().testTag("health_weight_photo_close")
                )
            }
        )
    }

    val healthListState = androidx.compose.foundation.lazy.rememberLazyListState()
    // 2788: positions of the Today card and its weekly block, so a tile sheet can jump to the weekly view.
    val weeklyAnchor = remember { arrayOfNulls<androidx.compose.ui.layout.LayoutCoordinates>(2) }
    // 2783: a Today tile with data opens the journal at today's records of that type.
    var journalFocus by remember { mutableStateOf<Pair<String, String>?>(null) }
    var journalFocusKey by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var expandedTodayTileKey by remember { mutableStateOf<String?>(null) }
    val healthSectionState = rememberSaveableStateHolder()
    val healthSectionSheet = when {
        uiState.expandedHealthJournal -> "journal"
        uiState.expandedHealthConnect -> "connect"
        else -> null
    }
    val sourcesTitle = tr(uiState.language, "Források, óra és szinkron", "Sources, watch & sync",
        "Quellen, Uhr & Sync", "Surse, ceas și sincronizare")
    LaunchedEffect(healthSectionSheet) {
        if (healthSectionSheet != null) expandedTodayTileKey = null
    }

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
                    .onGloballyPositioned { weeklyAnchor[0] = it }
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
                                    fontWeight = FontWeight.Bold,
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

                    // 2784: every tile opens its own detail sheet; the page below does not move.
                    StudioTodayStatusGrid(uiState) { key ->
                        expandedTodayTileKey = if (expandedTodayTileKey == key) null else key
                        if (key == "pulse") onToggleHealthSection("pulse")
                    }

                    val goalShape = RoundedCornerShape(12.dp)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { weeklyAnchor[1] = it }
                            .trainPilotAnimatedExpansion()
                            .trainPilotBorder(1.dp, palette.sectionBorder, goalShape)
                            .clip(goalShape)
                    ) {
                        com.example.ui.components.WeeklyGoalsCompact(
                            uiState,
                            framed = false,
                            onOpen = { onToggleHealthSection("weekly") },
                            onGoals = onOpenGoalsDialog,
                            expanded = uiState.expandedHealthWeeklyGoals,
                            modifier = Modifier.testTag("health_weekly_goals_compact")
                        )
                        com.example.ui.components.TrainPilotAnimatedDisclosure(visible = uiState.expandedHealthWeeklyGoals) {
                            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                HealthWeeklyGoalsPanel(uiState)
                            }
                        }
                    }

                    // Expandable Detail Dropdown Card when any of the 6 "Mai állapot" tiles is tapped
                    // 2771: tile details open from the bottom instead of pushing the page down.
                    if (expandedTodayTileKey != null && healthSectionSheet == null) com.example.ui.components.TrainPilotScreenSheet(
                        onDismiss = { expandedTodayTileKey = null },
                        testTag = "health_today_tile_sheet"
                    ) {
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
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                // 2784: opens to its content, at most ~2/3 of the screen.
                                .heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.66f).dp)
                                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                                .testTag("health_today_tile_expanded_card")
                        ) {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val openHealthJournal: () -> Unit = {
                                    val recordType = when (activeKey) {
                                        "sleep" -> "SleepSessionRecord"
                                        "steps" -> com.repforge.app.ui.health.NativeDetailedHealthMetrics.recordType("steps")
                                        "calories" -> com.repforge.app.ui.health.NativeDetailedHealthMetrics.recordType("activeCalories")
                                        else -> null
                                    }
                                    journalFocus = recordType?.let { today.toString() to it }
                                    journalFocusKey++
                                    if (!uiState.expandedHealthJournal) onToggleHealthSection("journal")
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
                                            tr(uiState.language, "Aktív energia (edzés & mozgás): $activeEnergy • Teljes napi energia (nyugalmival): $totalEnergy",
                                                "Active energy (workout & activity): $activeEnergy • Total daily energy (with resting): $totalEnergy",
                                                "Aktive Energie: $activeEnergy • Tagesgesamtenergie: $totalEnergy",
                                                "Energie activă: $activeEnergy • Energie zilnică totală: $totalEnergy")
                                        } else {
                                            tr(uiState.language, "A kiválasztott szolgáltatótól (${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}) ma még nem érkezett aktív kalória adat. A nyugalmi energia külön kerül elszámolásra.",
                                                "No active calories fetched yet today from ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}. Resting energy is accounted separately.",
                                                "Noch keine aktiven Kalorien von ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} geladen. Ruheenergie wird separat berechnet.",
                                                "Fără calorii active de la ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} azi. Energia de repaus este calculată separat.")
                                        },
                                        tr(uiState.language, "Teljes egészségnapló", "Full health journal", "Gesamtes Gesundheitstagebuch", "Jurnalul de sănătate complet")
                                    ) to openHealthJournal
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
                                            tr(uiState.language, "Teljesítés: ${((uiState.dailySteps.toFloat() / uiState.stepsGoal.coerceAtLeast(1)) * 100).toInt()}% • Becsült táv: ${"%.1f".format(uiState.dailySteps * 0.00075f)} km",
                                                "Completion: ${((uiState.dailySteps.toFloat() / uiState.stepsGoal.coerceAtLeast(1)) * 100).toInt()}% • Est. distance: ${"%.1f".format(uiState.dailySteps * 0.00075f)} km",
                                                "Erfüllung: ${((uiState.dailySteps.toFloat() / uiState.stepsGoal.coerceAtLeast(1)) * 100).toInt()}% • Gesch. Distanz: ${"%.1f".format(uiState.dailySteps * 0.00075f)} km",
                                                "Progres: ${((uiState.dailySteps.toFloat() / uiState.stepsGoal.coerceAtLeast(1)) * 100).toInt()}% • Dist. est.: ${"%.1f".format(uiState.dailySteps * 0.00075f)} km")
                                        } else {
                                            tr(uiState.language, "Ma még nem érkezett lépésadat a kiválasztott szolgáltatótól (${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}). Napi cél: ${uiState.stepsGoal} lépés.",
                                                "No steps data fetched yet today from ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}. Daily target: ${uiState.stepsGoal} steps.",
                                                "Noch keine Schrittdaten von ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} geladen. Tagesziel: ${uiState.stepsGoal} Schritte.",
                                                "Fără date despre pași de la ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} azi. Obiectiv zilnic: ${uiState.stepsGoal} pași.")
                                        },
                                        tr(uiState.language, "Teljes egészségnapló", "Full health journal", "Gesamtes Gesundheitstagebuch", "Jurnalul de sănătate complet")
                                    ) to openHealthJournal
                                    "workout" -> {
                                        val todaysWorkouts = uiState.loggedWorkouts.workoutsOn(today)
                                        val latestTodayTitle = todaysWorkouts.firstOrNull()?.localizedTitle(uiState.language, uiState.programs, uiState.exercises)
                                        listOf(
                                            tr(uiState.language, "Mai edzések • ${todaysWorkouts.size} rögzített edzés", "Today's Workouts • ${todaysWorkouts.size} logged", "Heutige Trainings • ${todaysWorkouts.size} erfasst", "Antrenamente azi • ${todaysWorkouts.size} înregistrate"),
                                            if (todaysWorkouts.isNotEmpty()) {
                                                tr(uiState.language, "Utolsó mai edzés: ${latestTodayTitle ?: "Edzés"}",
                                                    "Latest workout today: ${latestTodayTitle ?: "Workout"}",
                                                    "Letztes Training heute: ${latestTodayTitle ?: "Training"}",
                                                    "Ultimul antrenament azi: ${latestTodayTitle ?: "Antrenament"}")
                                            } else {
                                                tr(uiState.language, "Ma még nincs elmentett edzés.",
                                                    "No saved workout yet today.",
                                                    "Heute noch kein gespeichertes Training.",
                                                    "Niciun antrenament salvat azi.")
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
                                            tr(uiState.language, "Teljes egészségnapló", "Full health journal", "Gesamtes Gesundheitstagebuch", "Jurnalul de sănătate complet")
                                        ) to openHealthJournal
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = titleStr[0],
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary,
                                        modifier = Modifier.weight(1f).padding(top = 12.dp)
                                    )
                                    IconButton(
                                        onClick = { expandedTodayTileKey = null },
                                        modifier = Modifier.size(48.dp).testTag("health_today_tile_close")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = strings.closeAction, tint = palette.danger)
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
                                            .trainPilotAnimatedExpansion()
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
                                                    .heightIn(min = 48.dp)
                                                    .healthDetailsToggle(uiState.expandedHealthPulseTrend, uiState.language, RoundedCornerShape(14.dp)) {
                                                        onToggleHealthSection("pulse")
                                                    }
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
                                                Icon(
                                                    imageVector = if (uiState.expandedHealthPulseTrend) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    tint = palette.primary
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            var selectedPulse by remember(uiState.dailyPulseHistory) {
                                                androidx.compose.runtime.mutableIntStateOf(uiState.dailyPulseHistory.lastIndex)
                                            }
                                            // Full-width dark sparkline box with red wave curve (Screenshot 9)
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = palette.surfaceCard.copy(alpha = 0.7f),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(54.dp)
                                                    .trainPilotBorder(1.dp, palette.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                                    .trainPilotClickable(RoundedCornerShape(14.dp)) { onToggleHealthSection("pulse") }
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
                                                    // 2785: tap → nearest reading highlighted; time + bpm in the info line below.
                                                    val fractions = remember(readings) { pulseTimeFractions(readings) }
                                                    val pulseLine = Color(0xFFEF4444)
                                                    val pulseDotCenter = palette.surfaceCard
                                                    val selectedLabel = readings.getOrNull(selectedPulse)?.let {
                                                        "${pulseClockLabel(it.timeIso)} · ${it.bpm} bpm"
                                                    }
                                                    Canvas(Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 7.dp)
                                                        .testTag("health_pulse_real_trend")
                                                        .trainPilotChartTap(fractions, { x, w -> com.example.ui.components.chartNearestByFraction(x, w, fractions) },
                                                            selectedLabel = selectedLabel) { selectedPulse = it }) {
                                                        val first = java.time.Instant.parse(readings.first().timeIso).toEpochMilli()
                                                        val last = java.time.Instant.parse(readings.last().timeIso).toEpochMilli()
                                                        val delta = (last - first).coerceAtLeast(1L).toFloat()
                                                        val lowest = readings.minOf { it.bpm }.toFloat()
                                                        val spread = (readings.maxOf { it.bpm } - readings.minOf { it.bpm })
                                                            .toFloat().coerceAtLeast(1f)
                                                        val line = Path()
                                                        var prev: Offset? = null
                                                        readings.forEach { sample ->
                                                            val time = java.time.Instant.parse(sample.timeIso).toEpochMilli()
                                                            val x = ((time - first) / delta).coerceIn(0f, 1f) * size.width
                                                            val y = size.height - (sample.bpm - lowest) / spread * size.height
                                                            // 2784: curved 24 h line.
                                                            val p = prev
                                                            if (p == null) line.moveTo(x, y) else { val mx = (p.x + x) / 2f; line.cubicTo(mx, p.y, mx, y, x, y) }
                                                            prev = Offset(x, y)
                                                        }
                                                        drawPath(line, color = pulseLine,
                                                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                                                        readings.getOrNull(selectedPulse)?.let { sample ->
                                                            val x = (fractions.getOrElse(selectedPulse) { 1f }) * size.width
                                                            val y = size.height - (sample.bpm - lowest) / spread * size.height
                                                            drawLine(palette.primary.copy(alpha = 0.55f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
                                                            drawCircle(palette.primary, radius = 5.dp.toPx(), center = Offset(x, y))
                                                            drawCircle(pulseDotCenter, radius = 2.dp.toPx(), center = Offset(x, y))
                                                        }
                                                    }
                                                }
                                            }
                                            uiState.dailyPulseHistory.getOrNull(selectedPulse)?.takeIf { uiState.dailyPulseHistory.size >= 2 }?.let { sample ->
                                                Spacer(modifier = Modifier.height(4.dp))
                                                com.example.ui.components.TrainPilotChartInfoLine("${pulseClockLabel(sample.timeIso)} · ${sample.bpm} bpm",
                                                    Modifier.testTag("health_pulse_chart_info"))
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
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .trainPilotClickable(RoundedCornerShape(12.dp)) { onNavigateToSubScreen(SubScreen.COACH_DETAIL) }
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
                                // 2788: the sheet is about TODAY; the 7-day steps/workout history lives only in Weekly goals.
                                TodayTileSheetExtras(activeKey, uiState, today, onOpenWeekly = {
                                    expandedTodayTileKey = null
                                    if (!uiState.expandedHealthWeeklyGoals) onToggleHealthSection("weekly")
                                    coroutineScope.launch {
                                        val card = weeklyAnchor[0]
                                        val block = weeklyAnchor[1]
                                        val offset = if (card != null && block != null && card.isAttached && block.isAttached)
                                            card.localPositionOf(block, Offset.Zero).y.toInt().coerceAtLeast(0) else 0
                                        healthListState.animateScrollToItem(0, offset)
                                    }
                                })
                                if (activeKey != "pulse") Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.primary.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                        .trainPilotClickable(RoundedCornerShape(10.dp)) { expandedTodayTileKey = null; onActionClick() }
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

        // 2786: the weight log is its own card right under "Today" (weekly goals moved into "Today").
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("health_weight_card")
                    .trainPilotAnimatedExpansion()
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
                            .heightIn(min = 48.dp)
                            .healthDetailsToggle(uiState.expandedHealthWeightLog, uiState.language, RoundedCornerShape(14.dp)) {
                                onToggleHealthSection("weight")
                            }
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
                                text = tr(uiState.language, "Testsúlynapló", "Weight log", "Gewichtsprotokoll", "Jurnal de greutate"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Clip
                            )
                            val latestWeightLabel = if (latestChronologicalEntry != null) {
                                "${tr(uiState.language, "Legújabb", "Latest", "Neueste", "Ultima")} (${latestChronologicalEntry.dateLabel}): ${latestChronologicalEntry.weightLabel}"
                            } else {
                                tr(uiState.language, "Legújabb: nincs rögzített mérés", "Latest: no recorded measurement", "Neueste: keine Messung erfasst", "Ultima: nicio măsurătoare înregistrată")
                            }
                            Text(
                                text = latestWeightLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primary
                            )
                        }
                        Icon(
                            imageVector = if (uiState.expandedHealthWeightLog) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = palette.primary
                        )
                    }

                    // 2784: compact curved chart when closed; large chart with periods, fitness tiles,
                    // compact rows (edit/delete in a ⋮ menu) when open.
                    WeightLogBody(
                        uiState = uiState,
                        strings = strings,
                        chronological = if (uiState.expandedHealthWeightLog) chronologicalMeasurements else compactMeasurements,
                        newestFirst = recentMeasurements,
                        dateFormatter = shortChartDateFormatter,
                        onToggle = { onToggleHealthSection("weight") },
                        onSelectRange = onSelectWeightRange,
                        onAdd = { onToggleAddWeightDialog(true) },
                        onEdit = { editingMeasurement = it },
                        onPhoto = { photoMeasurement = it },
                        onDelete = { entry ->
                            recentMeasurements = recentMeasurements.filterNot { it.id == entry.id }
                            onDeleteWeightLog(entry.id)
                        }
                    )
                }
            }
        }

        item {
            HealthSectionLauncher(
                icon = Icons.Default.DateRange,
                title = strings.healthJournalTitle,
                subtitle = null,
                onOpen = { onToggleHealthSection("journal") }
            )
        }
        item {
            HealthSectionLauncher(
                icon = Icons.Default.Link,
                title = sourcesTitle,
                subtitle = listOf(
                    "Samsung Health " + if (uiState.samsungGrantedCount > 0) "✓" else "—",
                    "Health Connect " + if (uiState.healthGrantedCount > 0) "✓" else "—",
                    tr(uiState.language, "Óra", "Watch", "Uhr", "Ceas") + " " + if (uiState.watchAppAvailable) "✓" else "—"
                ).joinToString(" · "),
                onOpen = { onToggleHealthSection("connect") },
                customLeadingBadge = { MockupHealthConnectBadge(size = 44.dp) }
            )
        }
        }
    }

    if (healthSectionSheet != null) {
        val journal = healthSectionSheet == "journal"
        val closeSheet: () -> Unit = {
            if (journal) journalFocus = null
            onToggleHealthSection(healthSectionSheet)
        }
        com.example.ui.components.TrainPilotRouteSheet(
            routeKey = healthSectionSheet,
            onDismiss = closeSheet,
            testTag = "health_${healthSectionSheet}_sheet",
            fillHeight = journal
        ) {
            healthSectionState.SaveableStateProvider(healthSectionSheet) {
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (journal) strings.healthJournalTitle else sourcesTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(closeSheet, Modifier.size(48.dp).testTag("health_${healthSectionSheet}_close")) {
                            Icon(Icons.Default.Close, strings.closeAction, tint = palette.danger)
                        }
                    }
                    if (uiState.healthSyncMessage.isNotBlank()) {
                        val color = when {
                            uiState.healthSyncFailed -> palette.danger
                            uiState.healthSyncPartial -> palette.warning
                            else -> palette.textPrimary
                        }
                        Text(
                            text = uiState.healthSyncMessage,
                            color = color,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite }
                                .testTag(if (journal && uiState.healthSyncFailed) "journal_refresh_error" else "health_sync_result")
                        )
                    }
                    val sectionScroll = rememberScrollState()
                    LaunchedEffect(journalFocus, journalFocusKey) {
                        if (journal && journalFocus != null) sectionScroll.scrollTo(0)
                    }
                    Column(
                        Modifier.fillMaxWidth().weight(1f, fill = false)
                            .verticalScroll(sectionScroll)
                            .testTag("health_${healthSectionSheet}_sheet_scroll"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (journal) {
                            com.example.ui.components.StudioHealthMetricsBrowser(
                                initialProvider = if (uiState.primaryHealthSourceSamsung) com.repforge.app.HealthJournalStore.SAMSUNG
                                    else com.repforge.app.HealthJournalStore.HC,
                                language = uiState.language.code.lowercase(),
                                refreshKey = "${uiState.healthRefreshRevision}:${uiState.healthSyncBusy}",
                                onRefreshProvider = onReadHealthSource,
                                refreshBusy = uiState.healthSyncBusy,
                                focus = journalFocus,
                                focusKey = journalFocusKey
                            )
                        } else {
                            HealthSourcesContent(
                                uiState = uiState,
                                strings = strings,
                                onSelectHealthSource = onSelectHealthSource,
                                onTriggerHealthSync = onTriggerHealthSync,
                                onRequestHealthPermissions = onRequestHealthPermissions,
                                onNavigateToSubScreen = onNavigateToSubScreen,
                                closeSheet = closeSheet,
                                syncDetailsExpanded = syncDetailsExpanded,
                                onToggleSyncDetails = { syncDetailsExpanded = !syncDetailsExpanded }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthSourcesContent(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onSelectHealthSource: (Boolean) -> Unit,
    onTriggerHealthSync: () -> Unit,
    onRequestHealthPermissions: () -> Unit,
    onNavigateToSubScreen: (SubScreen) -> Unit,
    closeSheet: () -> Unit,
    syncDetailsExpanded: Boolean,
    onToggleSyncDetails: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
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
                .trainPilotClickable(RoundedCornerShape(14.dp)) { onSelectHealthSource(true) }
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
                .trainPilotClickable(RoundedCornerShape(14.dp)) { onSelectHealthSource(false) }
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
                    .heightIn(min = 52.dp)
                    .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    .trainPilotClickable(RoundedCornerShape(16.dp), enabled = !uiState.healthSyncBusy) { onTriggerHealthSync() }
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
                    .heightIn(min = 52.dp)
                    .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                    .trainPilotClickable(RoundedCornerShape(16.dp)) { onRequestHealthPermissions() }
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

        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surfaceCard,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .trainPilotClickable(RoundedCornerShape(20.dp)) {
                    closeSheet()
                    onNavigateToSubScreen(SubScreen.WATCH_AND_SYNC)
                }
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
        Spacer(Modifier.height(10.dp))

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
        TrainPilotOutlinedButton(onClick = onToggleSyncDetails,
            modifier = Modifier.fillMaxWidth().testTag("health_sync_details_toggle")) {
            Text(tr(uiState.language, "Szinkron részletei", "Sync details", "Sync-Details", "Detalii sincronizare"),
                modifier = Modifier.weight(1f))
            Icon(if (syncDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
        }
        com.example.ui.components.TrainPilotAnimatedDisclosure(visible = syncDetailsExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

@Composable
internal fun MainTodayStatusTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    titleTag: String = "today_tile_title"
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.background.copy(alpha = 0.55f),
        modifier = modifier
            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            .trainPilotClickable(RoundedCornerShape(16.dp)) { onClick() }
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
                // 2788: whole word, never "Aktív ka…": the font steps down, wraps only as a last resort.
                com.example.ui.components.TrainPilotFitText(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    minSize = 8.sp,
                    modifier = Modifier.weight(1f).testTag(titleTag)
                )
            }
            Text(
                text = value,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
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
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HealthSectionLauncher(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onOpen: () -> Unit,
    customLeadingBadge: (@Composable () -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotSoftGlow(palette.primary, RoundedCornerShape(20.dp), 3.dp)
            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            .trainPilotClickable(RoundedCornerShape(20.dp), onClick = onOpen)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
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
                    imageVector = Icons.Default.ExpandLess,
                    contentDescription = null,
                    tint = palette.primary
                )
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
                    leadingIcon = com.example.ui.components.TrainPilotCoachIcon,
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
                                    fontWeight = FontWeight.Bold,
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
                                text = tr(uiState.language, "3. Végső Coach készenlét", "3. Final Coach Readiness", "3. Finale Coach-Bereitschaft", "3. Pregătire finală Coach"),
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
                                Text(tr(uiState.language, "1. Samsung Health", "1. Samsung Energy", "1. Samsung Energie", "1. Energie Samsung"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                Text(tr(uiState.language, "2. TrainPilot", "2. TrainPilot Own", "2. TrainPilot Eigen", "2. Scor TrainPilot"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                Text(tr(uiState.language, "3. Samsung / saját", "3. Samsung / own", "3. Samsung / eigen", "3. Samsung / propriu"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            .trainPilotClickable(RoundedCornerShape(12.dp)) { onClick() }
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
    onCloudSyncCompleted: () -> Unit = {},
    onPreviewProgression: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onApproveProgression: () -> Unit = {},
    onEditProgressionPreview: () -> Unit = {},
    onUndoProgression: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val currentPaletteObj = TrainPilotPalettes.getById(uiState.selectedPaletteId)

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
                                Text(strings.themes32Title, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text(toggleThemesHint, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            }
                            Icon(imageVector = if (expandedMainThemesSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null, tint = palette.textSecondary)
                        }
                    }

                    if (expandedMainThemesSection) com.example.ui.components.StudioPaletteCategories(
                        language = uiState.language,
                        selectedId = uiState.selectedPaletteId,
                        expanded = com.example.ui.theme.PaletteCategory.entries.firstOrNull { it.name == expandedThemeFamily },
                        onExpand = { expandedThemeFamily = it?.name.orEmpty() },
                        onSelect = onSelectPalette
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
                                    .trainPilotClickable(RoundedCornerShape(10.dp)) { onSelectLanguage(lang) }
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
            .then(com.example.ui.components.sheetAwareFillMaxSize())
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
                                .trainPilotClickable(RoundedCornerShape(16.dp)) {
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
                                .trainPilotClickable(RoundedCornerShape(16.dp)) {
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
                    .trainPilotClickable(RoundedCornerShape(18.dp)) { onRequestPermission("hc_permission") }
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
                    .trainPilotClickable(RoundedCornerShape(18.dp)) { onRequestPermission("samsung") }
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
    settingsViewModel: SettingsViewModel = viewModel(),
    compact: Boolean = false
) {
    val palette = LocalTrainPilotPalette.current
    val strings = com.example.localization.LocalTrainPilotStrings.current
    val lang = strings.language
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val gs = settingsState.googleSyncState
    val activity = LocalActivity.current
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

    LaunchedEffect(settingsState.statusMessage, compact) {
        val msg = settingsState.statusMessage
        if (!compact && !msg.isNullOrBlank()) {
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
            .trainPilotAnimatedExpansion()
            .testTag("studio_google_cloud_card")
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 4.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("settings_google_expand").trainPilotClickable(RoundedCornerShape(10.dp)) { cloudExpanded = !cloudExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (!compact) Box(
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
                    Spacer(modifier = Modifier.width(if (compact) 8.dp else 12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (compact) "Google Drive" else tr(lang, "Google Cloud Szinkron", "Google Cloud Sync", "Google Cloud-Synchronisierung", "Sincronizare Google Cloud"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        if (!compact) Text(
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
                        .trainPilotClickable(RoundedCornerShape(10.dp)) { settingsViewModel.setRecoverEarlierBackups(!settingsState.recoverEarlierBackupsChecked) }
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
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = palette.surfaceElevated),
                        border = BorderStroke(1.dp, palette.cardBorder),
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
                    TrainPilotOutlinedButton(
                        onClick = { settingsViewModel.disconnectGoogle() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = palette.surfaceElevated),
                        border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.45f)),
                        enabled = !gs.isBusy && !settingsState.replacementBusy && !settingsState.maintenanceBusy,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("settings_google_disconnect_btn")
                    ) {
                        Text(
                            text = tr(lang, "Google-fiók lekapcsolása", "Disconnect Google Account", "Google-Konto trennen", "Deconectează contul Google"),
                            color = palette.danger,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    TrainPilotOutlinedButton(
                        onClick = { showMaintenanceOptions = !showMaintenanceOptions },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = palette.surfaceElevated),
                        border = BorderStroke(1.dp, palette.cardBorder),
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
                        "Drive-mentés és Naptár szinkron", "Drive backup and Calendar sync",
                        "Drive-Sicherung und Kalendersynchronisierung", "Backup Drive și sincronizare Calendar"
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

            if (!compact) Text(
                driveHealthBackupSummary(lang),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )

            if (!compact && gs.isBusy && gs.stageMessage.isNotBlank()) {
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

            if (compact && !gs.isBusy && gs.errorMessage.isNullOrBlank() && gs.lastDriveSyncIso.isNotBlank()) {
                Text(
                    text = "${tr(lang, "Utolsó mentés", "Last backup", "Letzte Sicherung", "Ultimul backup")}: ${gs.lastDriveSyncIso.take(19).replace('T', ' ')}",
                    style = MaterialTheme.typography.labelSmall, color = palette.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp).testTag("backup_drive_last_sync")
                )
            }
            if (!compact && !gs.isBusy && gs.errorMessage.isNullOrBlank() && gs.lastCalendarSyncIso.isNotBlank()) {
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

            if (!compact && !gs.errorMessage.isNullOrBlank()) {
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

private fun driveHealthBackupSummary(language: AppLanguage) = tr(language,
    "Egészségadatok engedélyezésekor a Drive az edzések egészségösszesítőit menti. A napi lépések a helyi JSON/ZIP mentésbe kerülhetnek.",
    "When enabled, Drive includes workout health summaries. Daily steps can be included in local JSON/ZIP backups.",
    "Bei Zustimmung sichert Drive Trainings-Gesundheitsdaten. Tagesschritte können lokal als JSON/ZIP gesichert werden.",
    "La activare, Drive include rezumatele de sănătate ale antrenamentelor. Pașii zilnici pot fi salvați local în JSON/ZIP.")

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
    val activity = LocalActivity.current
    val lang = uiState.language
    val drive = uiState.selectedBackupFormat == "DRIVE"
    val inSheet = com.example.ui.components.LocalTrainPilotInSheet.current
    val localStatus = uiState.localBackupStatus
    var pickerMessage by remember { mutableStateOf<String?>(null) }
    val driveFeedback = settingsState.googleActionFeedback?.takeIf { it.action == "drive" || it.action == "connect" }
    val driveBlock = when {
        gs.isBusy -> gs.stageMessage.ifBlank { tr(lang, "Google-szinkron folyamatban…", "Google sync in progress…", "Google-Sync läuft…", "Sincronizarea Google este în curs…") }
        settingsState.replacementBusy || localStatus?.busy == true ->
            tr(lang, "Mentés vagy visszaállítás folyamatban.", "Backup or restore in progress.", "Sicherung oder Wiederherstellung läuft.", "Backup-ul sau restaurarea este în curs.")
        settingsState.maintenanceBusy -> tr(lang, "Karbantartás folyamatban.", "Maintenance in progress.", "Wartung läuft.", "Mentenanță în curs.")
        activity == null -> tr(lang, "Nyisd meg újra ezt a képernyőt.", "Reopen this screen.", "Öffne diese Seite erneut.", "Redeschide acest ecran.")
        uiState.isWorkoutSessionActive && gs.connectedEmail.isNotBlank() ->
            tr(lang, "A Drive-szinkron előtt fejezd be az aktív edzést.", "Finish the active workout before syncing Drive.",
                "Beende das laufende Training vor dem Drive-Sync.", "Încheie antrenamentul activ înainte de sincronizarea Drive.")
        else -> null
    }
    val status = when {
        drive && driveBlock != null -> com.example.ui.viewmodel.LocalBackupStatus(driveBlock, busy = gs.isBusy || settingsState.maintenanceBusy)
        pickerMessage != null -> com.example.ui.viewmodel.LocalBackupStatus(pickerMessage.orEmpty())
        drive && driveFeedback != null -> com.example.ui.viewmodel.LocalBackupStatus(driveFeedback.message, failed = driveFeedback.failed)
        else -> localStatus
    }
    val mimeType = if (uiState.selectedBackupFormat == "ZIP") "application/zip" else "application/json"
    var showHealthInfo by remember { mutableStateOf(false) }
    val defaultFileName = if (uiState.selectedBackupFormat == "ZIP") "trainpilot-backup.zip" else "trainpilot-backup.json"

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType)
    ) { uri ->
        if (uri != null) {
            settingsViewModel.clearGoogleActionFeedback()
            if (onExportBackup != null) {
                onExportBackup(uri)
            } else {
                onShowBanner(tr(uiState.language, "Mentés készítése...", "Creating backup...", "Sicherung wird erstellt...", "Se creează backup-ul..."))
            }
        } else {
            pickerMessage = tr(lang, "A mentés megszakítva.", "Backup canceled.", "Sicherung abgebrochen.", "Backup anulat.")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            settingsViewModel.clearGoogleActionFeedback()
            if (onImportBackup != null) {
                onImportBackup(uri)
            } else {
                onShowBanner(tr(uiState.language, "Visszaállítás fájlból...", "Restoring from file...", "Wiederherstellung aus Datei...", "Se restaurează din fișier..."))
            }
        } else {
            pickerMessage = tr(lang, "A fájlimport megszakítva.", "File import canceled.", "Dateiimport abgebrochen.", "Import de fișier anulat.")
        }
    }

    if (showHealthInfo) {
        com.example.ui.components.TrainPilotFormSheet(scrollText = false,
            testTag = "backup_health_info_sheet",
            onDismissRequest = { showHealthInfo = false },
            title = { Text(tr(uiState.language, "Egészségadatok", "Health data", "Gesundheitsdaten", "Date de sănătate")) },
            text = { Text(modifier = Modifier.verticalScroll(rememberScrollState()), text = tr(uiState.language,
                "Alapból kikapcsolva. JSON/ZIP: tárolt egészségnapló és edzésösszesítések. Drive: csak az edzéshez mentett egészségadatok; a teljes napi egészségnapló nem kerül Drive-ra. Kikapcsolás nem törli a helyi adatokat vagy a korábbi mentéseket.",
                "Off by default. JSON/ZIP: stored health journal and workout summaries. Drive: workout-attached health summaries only; the full daily health journal is not uploaded to Drive. Turning off does not delete local data or existing backups.",
                "Standardmäßig aus. JSON/ZIP: gespeichertes Gesundheitstagebuch und Trainingszusammenfassungen. Drive: nur trainingsbezogene Gesundheitsdaten; das vollständige Tagestagebuch wird nicht hochgeladen. Das Ausschalten löscht weder lokale Daten noch vorhandene Sicherungen.",
                "Dezactivat implicit. JSON/ZIP: jurnalul de sănătate stocat și rezumatele antrenamentelor. Drive: doar datele de sănătate atașate antrenamentelor; jurnalul zilnic complet nu este încărcat. Dezactivarea nu șterge datele locale sau backup-urile existente.")) },
            confirmButton = { com.repforge.app.ui.theme.TrainPilotTextButton(onClick = { showHealthInfo = false }) { Text("OK") } }
        )
    }

    Column(Modifier.then(com.example.ui.components.sheetAwareFillMaxSize()).background(palette.background)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth().weight(1f, fill = !inSheet)
                // In the sheet the JSON / ZIP / DRIVE tabs share one stable height (the tallest, Drive),
                // so switching tabs never makes the sheet jump.
                .then(if (inSheet)
                    Modifier.heightIn(min = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.78f)
                    else Modifier)
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
                        val interaction = remember(fmt) { MutableInteractionSource() }
                        val pressed by interaction.collectIsPressedAsState()
                        val shape = RoundedCornerShape(10.dp)
                        val background = if (sel) palette.primary else palette.surfaceCard
                        Box(
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                .testTag("backup_format_$fmt")
                                .selectable(selected = sel, role = androidx.compose.ui.semantics.Role.Tab,
                                    interactionSource = interaction, indication = null, onClick = { onSelectFormat(fmt) }),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = shape,
                                color = lerp(background, palette.primary, if (pressed) 0.12f else 0f),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp).clip(shape)
                                    .indication(interaction, ripple())
                                    .trainPilotBorder(1.dp, if (sel) palette.primary else palette.cardBorder, shape)
                                    .testTag("backup_format_${fmt}_visual")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(fmt, fontWeight = FontWeight.Bold, color = if (sel) palette.onPrimary else palette.textPrimary)
                                }
                            }
                        }
                    }
                }
            }
            if (drive) item {
                StudioGoogleCloudSyncCard(
                    onShowBanner = onShowBanner,
                    onCloudSyncCompleted = onCloudSyncCompleted,
                    settingsViewModel = settingsViewModel,
                    compact = true
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
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(tr(uiState.language, "Mentés tartalma", "Backup contents", "Sicherungsinhalt", "Conținut backup"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text(tr(uiState.language, "✓ Edzések és rekordok", "✓ Workouts and records", "✓ Trainings und Rekorde", "✓ Antrenamente și recorduri"), color = palette.success, style = MaterialTheme.typography.bodyMedium)
                        Text(tr(uiState.language, "✓ Programok és gyakorlatok", "✓ Programs and exercises", "✓ Programme und Übungen", "✓ Programe și exerciții"), color = palette.success, style = MaterialTheme.typography.bodyMedium)
                        Text(tr(lang, "✓ Célok, naptárterv és beállítások", "✓ Goals, calendar plan and settings", "✓ Ziele, Kalenderplan und Einstellungen", "✓ Obiective, calendar și setări"), color = palette.success, style = MaterialTheme.typography.bodyMedium)
                        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (drive) tr(lang, "Edzések egészségadatai", "Workout health data", "Training-Gesundheitsdaten", "Date de sănătate la antrenament")
                                else tr(lang, "Egészségadatok", "Health data", "Gesundheitsdaten", "Date de sănătate"), color = palette.textPrimary, modifier = Modifier.weight(1f))
                            IconButton(onClick = { showHealthInfo = true }, modifier = Modifier.testTag("backup_health_info")) {
                                Icon(Icons.Default.Info, contentDescription = tr(uiState.language, "Egészségadatok információ", "Health data information", "Informationen zu Gesundheitsdaten", "Informații despre datele de sănătate"), tint = palette.textSecondary)
                            }
                            Switch(checked = uiState.includeHealthInBackup, enabled = !uiState.backupHealthConsentSaving, onCheckedChange = onToggleIncludeHealth, modifier = Modifier.heightIn(min = 48.dp).testTag("backup_health_switch"))
                        }
                        if (drive) Text(tr(lang, "Napi lépések: helyi JSON/ZIP.", "Daily steps: local JSON/ZIP.",
                            "Tagesschritte: lokal als JSON/ZIP.", "Pași zilnici: JSON/ZIP local."), color = palette.textSecondary,
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("backup_drive_health_scope"))
                    }
                }
            }
            item {
                com.repforge.app.ui.theme.TrainPilotSaveButton(
                    onClick = {
                        pickerMessage = null
                        if (drive) {
                            if (gs.connectedEmail.isEmpty()) {
                                activity?.let { settingsViewModel.connectGoogle(it) }
                            } else {
                                activity?.let { settingsViewModel.syncDriveCloud(it) }
                            }
                        } else {
                            exportLauncher.launch(defaultFileName)
                        }
                    },
                    enabled = if (drive) driveBlock == null else localStatus?.busy != true && !gs.isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("backup_export_btn")
                ) {
                    Icon(imageVector = Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            drive && gs.connectedEmail.isEmpty() -> tr(lang, "Bejelentkezés Google-fiókkal", "Sign in with Google", "Mit Google anmelden", "Conectare cu Google")
                            drive -> tr(lang, "Drive-mentés és szinkron", "Drive backup and sync", "Drive-Sicherung und Sync", "Backup și sincronizare Drive")
                            else -> "${tr(lang, "Mentés", "Backup", "Sichern", "Salvează")} (${uiState.selectedBackupFormat})"
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                if (drive && gs.connectedEmail.isNotBlank()) {
                    Text(tr(lang, "A szinkron a felhőből is visszaállít és összefésül.",
                        "Sync also restores and merges data from Drive.",
                        "Der Sync stellt Daten aus Drive wieder her und führt sie zusammen.",
                        "Sincronizarea restaurează și îmbină datele din Drive."),
                        color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TrainPilotOutlinedButton(
                    onClick = {
                        pickerMessage = null
                        importLauncher.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "*/*"))
                    },
                    shape = RoundedCornerShape(14.dp),
                    enabled = localStatus?.busy != true && !gs.isBusy && !settingsState.replacementBusy &&
                        !settingsState.maintenanceBusy && !uiState.isWorkoutSessionActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("import_repforge_legacy_btn")
                ) {
                    Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = palette.textPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tr(lang, "Helyi JSON/ZIP importálása", "Import local JSON/ZIP", "Lokale JSON-/ZIP-Datei importieren", "Importă JSON/ZIP local"),
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Text(
                    if (uiState.isWorkoutSessionActive) tr(lang, "Az importálás előtt fejezd be az aktív edzést.",
                        "Finish the active workout before importing.", "Beende das laufende Training vor dem Import.",
                        "Încheie antrenamentul activ înainte de import.")
                    else tr(lang, "Korábbi mentések is visszaállíthatók.", "Earlier backups can also be restored.",
                        "Auch ältere Sicherungen lassen sich wiederherstellen.", "Poți restaura și backup-uri mai vechi."),
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (status != null) {
            val color = when {
                status.failed -> palette.danger
                status.busy -> palette.primary
                pickerMessage == null && (!drive || driveBlock == null) &&
                    (status === localStatus || driveFeedback != null) -> palette.success
                else -> palette.textSecondary
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.surfaceElevated,
                modifier = Modifier.fillMaxWidth().padding(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 6.dp)
                    .testTag("backup_operation_status").semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                Text(status.message, color = color, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState()).padding(10.dp))
            }
        }
    }
}

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
            .background(palette.background)
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
private fun PermissionSettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val shape = RoundedCornerShape(18.dp)
    Surface(shape = shape, color = palette.surfaceCard,
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
private fun PermissionSettingsDivider() {
    HorizontalDivider(color = LocalTrainPilotPalette.current.cardBorder.copy(alpha = 0.45f))
}

@Composable
private fun PermissionSettingsRow(title: String, tag: String, status: String? = null, onClick: () -> Unit) {
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
private fun PermissionToggleRow(
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

/** 2783: Today tile → journal record type to open directly, or null when the tile has no data (or no journal type). */
internal fun todayTileJournalType(key: String, state: com.example.ui.viewmodel.TrainPilotUiState): String? = when (key) {
    "sleep" -> "SleepSessionRecord".takeIf { state.sleepHoursDecimal != "—" }
    "calories" -> com.repforge.app.ui.health.NativeDetailedHealthMetrics.recordType("activeCalories")
        .takeIf { "activeCalories" in state.dailyHealthAvailableFields }
    "steps" -> com.repforge.app.ui.health.NativeDetailedHealthMetrics.recordType("steps")
        .takeIf { "steps" in state.dailyHealthAvailableFields }
    // Pulse keeps its own daily pulse-trend view.
    else -> null
}

/** 2785: measurements of the selected weight period only (0 = 7 d, 1 = 30 d, 2 = 3 mo, 3 = 1 yr, else all), oldest first. */
internal fun weightMeasurementsInRange(entries: List<WeightMeasurementEntry>, rangeIndex: Int, today: java.time.LocalDate): List<WeightMeasurementEntry> {
    val cutoff = when (rangeIndex) {
        0 -> today.minusDays(7)
        1 -> today.minusDays(30)
        2 -> today.minusMonths(3)
        3 -> today.minusYears(1)
        else -> null
    }
    return entries.filter { cutoff == null || !it.parsedDate.isBefore(cutoff) }.sortedBy { it.parsedDate }
}

internal fun recentWeightTrend(entries: List<WeightMeasurementEntry>): List<WeightMeasurementEntry> =
    entries.sortedByDescending { it.parsedDate }.take(7).asReversed()

/** 2786: the big weekly goals view (opened from the compact block in "Today" or from Home). Real data only. */
@Composable
private fun HealthWeeklyGoalsPanel(uiState: TrainPilotUiState) {
    val palette = LocalTrainPilotPalette.current
  Column(verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)) {
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
    val weeklyStepsPct = kotlin.math.round((weeklyStepsCurrent.toDouble() / weeklyStepsTarget.toDouble()) * 100.0).toInt()
    val weeklyMinutesCurrent = goalDays.sumOf { it.workoutMinutes }
    val primaryProviderName = if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"

    // 2788: one section per goal (no nested cards, no second "daily breakdown" to open): summary, bar,
    // tappable day columns with day names, then the days themselves. The only place of the 7-day history.
    val goal = com.example.ui.components.weeklyGoalSummary(uiState, today)
    val lang = uiState.language
    val langCode = lang.name.lowercase()
    val minUnit = tr(lang, "perc", "min", "Min.", "min")
    val nf = java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag(langCode))
    val dates = goalDays.map { java.time.LocalDate.parse(it.dateIso) }
    val weekWorkouts = goalDays.sumOf { it.workoutCount }

    @Composable
    fun Section(title: String, summary: String, tag: String, content: @Composable () -> Unit) {
        Surface(shape = RoundedCornerShape(18.dp), color = palette.background.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth().trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp)).testTag(tag)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary, modifier = Modifier.weight(1f))
                    Text(summary, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, modifier = Modifier.testTag("${tag}_summary"))
                }
                content()
            }
        }
    }

    // The week remains interactive. Only the selected day's chart value is visible by default;
    // complete measured/journal history is available in the existing bottom sheet.
    val workouts = uiState.loggedWorkouts.filter { w -> w.localWorkoutDate()?.let { it in dates } == true }
        .sortedByDescending { it.localWorkoutDate() }
    var historySheet by rememberSaveable { mutableStateOf<String?>(null) }
    Section(tr(lang, "Edzések", "Workouts", "Trainings", "Antrenamente"),
        "${goal.workoutsDone} / ${goal.workoutsTarget} " + (if (goal.monthly) tr(lang, "ebben a hónapban", "this month", "diesen Monat", "luna aceasta")
            else tr(lang, "ezen a héten", "this week", "diese Woche", "săptămâna aceasta")), "health_weekly_workouts_section") {
        LinearProgressIndicator(progress = { goal.workoutFraction }, modifier = Modifier.fillMaxWidth(), color = palette.primary, trackColor = palette.surfaceElevated)
        Text(tr(lang, "Utolsó 7 nap: $weekWorkouts edzés · $weeklyMinutesCurrent perc", "Last 7 days: $weekWorkouts workouts · $weeklyMinutesCurrent min",
                "Letzte 7 Tage: $weekWorkouts Trainings · $weeklyMinutesCurrent Min.", "Ultimele 7 zile: $weekWorkouts antrenamente · $weeklyMinutesCurrent min"),
            style = MaterialTheme.typography.labelMedium, color = palette.textPrimary, modifier = Modifier.testTag("health_weekly_minutes_total"))
        com.example.ui.components.WeeklyDayBars(dates, goalDays.map { d -> d.workoutMinutes.toDouble().takeIf { d.workoutCount > 0 } },
            label = { date, v -> todayTileWeekLabel("workout", date, v, langCode) }, tag = "health_weekly_minutes_bars",
            lang = langCode, showScale = true, detailIcon = Icons.Default.FitnessCenter)
        if (workouts.isEmpty()) {
            Text(tr(lang, "Az elmúlt 7 napban nincs edzés.", "No workouts in the last 7 days.", "Keine Trainings in den letzten 7 Tagen.", "Niciun antrenament în ultimele 7 zile."),
                style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        } else {
            TextButton(onClick = { historySheet = "workouts" },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("health_weekly_workouts_history")) {
                Text(tr(lang, "Edzéstartalom és előzmények ›", "Workout details and history ›", "Trainingsdetails und Verlauf ›", "Detalii și istoric antrenamente ›"))
            }
        }
    }

    val measuredDays = goalDays.count { it.stepsAvailable }
    Section(tr(lang, "Lépések", "Steps", "Schritte", "Pași"),
        if (measuredDays == 0) "—" else "${nf.format(weeklyStepsCurrent)} / ${nf.format(weeklyStepsTarget)} · $weeklyStepsPct%", "health_weekly_steps_section") {
        LinearProgressIndicator(progress = { if (measuredDays == 0) 0f else (weeklyStepsPct / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(),
            color = palette.primary, trackColor = palette.surfaceElevated)
        Text(tr(lang, "Utolsó 7 nap · $measuredDays/7 nap adat · $primaryProviderName", "Last 7 days · $measuredDays/7 days of data · $primaryProviderName",
                "Letzte 7 Tage · $measuredDays/7 Tage mit Daten · $primaryProviderName", "Ultimele 7 zile · $measuredDays/7 zile cu date · $primaryProviderName"),
            style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, modifier = Modifier.testTag("health_weekly_steps_total"))
        if (measuredDays == 0) {
            Text(todayTileWeekLabel("steps", today, null, langCode),
                style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                modifier = Modifier.testTag("health_weekly_steps_empty"))
        } else {
            com.example.ui.components.WeeklyDayBars(dates, goalDays.map { d -> d.steps.toDouble().takeIf { d.stepsAvailable } },
                label = { date, v -> todayTileWeekLabel("steps", date, v, langCode) + if (date == today) " · " + tr(lang, "napi cél", "daily goal", "Tagesziel", "obiectiv zilnic") + " ${nf.format(uiState.stepsGoal)}" else "" },
                tag = "health_weekly_steps_bars", lang = langCode, showScale = true,
                detailIcon = Icons.AutoMirrored.Filled.DirectionsWalk)
            TextButton(onClick = { historySheet = "steps" },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("health_weekly_steps_history")) {
                Text(tr(lang, "Lépéselőzmények megtekintése ›", "View step history ›", "Schrittverlauf anzeigen ›", "Vezi istoricul pașilor ›"))
            }
        }
    }

    // The caller's scrolling page never grows when a history list opens.
    // The shared modal already supports bottom-up entrance, swipe dismissal and system back.
    if (historySheet != null) com.example.ui.components.TrainPilotScreenSheet(
        onDismiss = { historySheet = null }, testTag = "health_weekly_history_sheet"
    ) {
        Column(Modifier.fillMaxWidth()
            .heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.68f).dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (historySheet == "workouts") tr(lang, "Edzéselőzmények", "Workout history", "Trainingsverlauf", "Istoric antrenamente")
                    else tr(lang, "Napi lépések", "Daily steps", "Tagesschritte", "Pași zilnici"),
                    style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, modifier = Modifier.weight(1f))
                IconButton(onClick = { historySheet = null }, modifier = Modifier.testTag("health_weekly_history_close")) {
                    Icon(Icons.Default.Close, contentDescription = tr(lang, "Bezárás", "Close", "Schließen", "Închide"), tint = palette.textPrimary)
                }
            }
            if (historySheet == "workouts") {
                workouts.forEach { w ->
                    HorizontalDivider(color = palette.cardBorder)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .testTag("health_weekly_workout_row_${w.id}"), verticalAlignment = Alignment.CenterVertically) {
                        Text(w.localWorkoutDate()?.let { com.example.ui.components.chartDateLabel(it, langCode) }.orEmpty(),
                            style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, modifier = Modifier.weight(1f))
                        Text("${w.localizedTitle(lang, uiState.programs, uiState.exercises)} · ${w.durationMinutes.coerceAtLeast(0)} $minUnit",
                            style = MaterialTheme.typography.bodySmall, color = palette.textPrimary, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            } else {
                goalDays.asReversed().forEach { day ->
                    HorizontalDivider(color = palette.cardBorder)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("health_weekly_steps_${day.dateIso}"),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(com.example.ui.components.chartDateLabel(java.time.LocalDate.parse(day.dateIso), langCode),
                            style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, modifier = Modifier.weight(1f))
                        Text(if (day.stepsAvailable) "${nf.format(day.steps)} ${tr(lang, "lépés", "steps", "Schritte", "pași")}"
                            else tr(lang, "nincs adat", "no data", "keine Daten", "fără date"),
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    }
                }
            }
        }
    }
  }
}

@Composable
private fun WeightLogBody(
    uiState: TrainPilotUiState,
    strings: com.example.localization.LocalizedStrings,
    chronological: List<WeightMeasurementEntry>,
    newestFirst: List<WeightMeasurementEntry>,
    dateFormatter: java.time.format.DateTimeFormatter,
    onToggle: () -> Unit,
    onSelectRange: (Int) -> Unit,
    onAdd: () -> Unit,
    onEdit: (WeightMeasurementEntry) -> Unit,
    onPhoto: (WeightMeasurementEntry) -> Unit,
    onDelete: (WeightMeasurementEntry) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val lang = uiState.language
    val expanded = uiState.expandedHealthWeightLog
    val kg: (Float) -> String = { "${"%.1f".format(it).replace('.', ',')}" }
    Spacer(Modifier.height(8.dp))
    if (expanded) {
        val labels = listOf(tr(lang, "7 nap", "7 days", "7 Tage", "7 zile"), tr(lang, "30 nap", "30 days", "30 Tage", "30 zile"),
            tr(lang, "3 hó", "3 mo", "3 Mon.", "3 luni"), tr(lang, "1 év", "1 yr", "1 Jahr", "1 an"), tr(lang, "Mind", "All", "Alle", "Toate"))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEachIndexed { idx, label ->
                com.example.ui.components.TrainPilotFilterChip(selected = uiState.selectedWeightRangeIndex == idx, onClick = { onSelectRange(idx) }, label = { Text(label, maxLines = 1) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("health_weight_range_$idx"))
            }
        }
    }
    Surface(shape = RoundedCornerShape(14.dp), color = palette.surfaceCard.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth().trainPilotAnimatedExpansion().trainPilotBorder(1.dp, palette.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .trainPilotClickable(RoundedCornerShape(14.dp)) { if (!expanded) onToggle() }
            .testTag(if (expanded) "health_weight_chart_large" else "health_weight_chart_compact")) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            val latest = newestFirst.firstOrNull()
            if (latest == null) {
                Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                    Text(tr(lang, "Még nincs rögzített testsúlymérés.", "No weight entries recorded yet.", "Noch keine Gewichtsmessung.", "Nicio greutate înregistrată."),
                        style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
            } else if (expanded && chronological.size < 2) {
                val measurement = chronological.singleOrNull() ?: latest
                val last = "${measurement.dateLabel} · ${measurement.weightLabel}"
                Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("health_weight_range_empty"), contentAlignment = Alignment.Center) {
                    Text(if (chronological.isEmpty()) tr(lang, "Nincs mérés ebben az időszakban · utolsó: $last", "No entries in this period · latest: $last",
                            "Keine Messung in diesem Zeitraum · letzte: $last", "Nicio măsurătoare în această perioadă · ultima: $last")
                        else tr(lang, "1 mérés ebben az időszakban: $last", "1 entry in this period: $last", "1 Messung in diesem Zeitraum: $last", "1 măsurătoare în această perioadă: $last"),
                        style = MaterialTheme.typography.bodySmall, color = palette.textSecondary, textAlign = TextAlign.Center)
                }
            } else {
                val pts = chronological.map { it.weightValue }
                var selectedPoint by remember(chronological) { androidx.compose.runtime.mutableIntStateOf(chronological.lastIndex) }
                val selectedDotCenter = palette.surfaceCard
                val minW = (pts.minOrNull() ?: 0f) - 0.5f
                val maxW = (pts.maxOrNull() ?: 1f) + 0.5f
                Row(Modifier.fillMaxWidth()) {
                    if (expanded) Column(Modifier.height(150.dp).padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        listOf(maxW, (maxW + minW) / 2f, minW).forEach {
                            Text(kg(it), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                    }
                    val lineColor = trainPilotChartMarkColor(!expanded, palette.primary, palette.textSecondary)
                    val grid = palette.cardBorder
                    val selectedLabel = chronological.getOrNull(selectedPoint)?.let { "${it.dateLabel} · ${it.weightLabel}" }
                    val tapModifier = if (expanded) Modifier.testTag("health_weight_chart_canvas")
                        .trainPilotChartTap(pts.size, { x, w -> com.example.ui.components.chartNearestIndex(x, w, pts.size) },
                            selectedLabel = selectedLabel) { selectedPoint = it }
                    else Modifier
                    Canvas(
                        Modifier.weight(1f).height(if (expanded) 150.dp else 56.dp)
                            .then(if (expanded) tapModifier else Modifier.testTag("health_weight_chart_compact_canvas"))
                    ) {
                        val inset = 6.dp.toPx()
                        val h = size.height - inset * 2
                        val offsets = pts.mapIndexed { idx, w ->
                            val x = if (pts.size > 1) idx.toFloat() / (pts.size - 1) * size.width else size.width / 2f
                            Offset(x, inset + h - ((w - minW) / (maxW - minW).coerceAtLeast(0.1f)) * h)
                        }
                        if (expanded) for (i in 0..2) { val y = inset + h * i / 2f; drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx()) }
                        val path = Path()
                        offsets.forEachIndexed { i, pt ->
                            if (i == 0) path.moveTo(pt.x, pt.y) else { val prev = offsets[i - 1]; val cx = (prev.x + pt.x) / 2f; path.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y) }
                        }
                        if (offsets.size > 1) {
                            val fill = Path().apply { addPath(path); lineTo(offsets.last().x, size.height); lineTo(offsets.first().x, size.height); close() }
                            drawPath(fill, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.25f), lineColor.copy(alpha = 0f))))
                        }
                        drawPath(path, lineColor, style = Stroke(width = (if (expanded) 3f else 2.5f).dp.toPx(), cap = StrokeCap.Round))
                        if (expanded) {
                            offsets.forEach { drawCircle(lineColor, radius = 3.5.dp.toPx(), center = it) }
                            offsets.getOrNull(selectedPoint)?.let { o ->
                                drawLine(palette.primary.copy(alpha = 0.55f), Offset(o.x, 0f), Offset(o.x, size.height), strokeWidth = 1.5.dp.toPx())
                                drawCircle(palette.primary, radius = 6.5.dp.toPx(), center = o)
                                drawCircle(selectedDotCenter, radius = 3.dp.toPx(), center = o)
                            }
                        } else drawCircle(lineColor, radius = 3.5.dp.toPx(), center = offsets.last())
                    }
                }
                val dates = if (expanded) chronological.takeLast(5) else listOfNotNull(chronological.firstOrNull(),
                    chronological.getOrNull(chronological.size / 2), chronological.lastOrNull()).distinct()
                Row(Modifier.fillMaxWidth().padding(start = if (expanded) 30.dp else 0.dp, top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    dates.forEach { entry ->
                        Text(entry.parsedDate.format(dateFormatter), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                }
                if (expanded) chronological.getOrNull(selectedPoint)?.let { entry ->
                    com.example.ui.components.TrainPilotChartInfoLine("${entry.dateLabel} · ${entry.weightLabel}",
                        Modifier.padding(top = 4.dp).testTag("health_weight_chart_info"))
                }
            }
        }
    }
    if (!expanded) return
    Spacer(Modifier.height(10.dp))
    Text(strings.bodyAndFitnessTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.textSecondary)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PulseInnerBox(tr(lang, "Testzsír", "Body fat", "Körperfett", "Grăsime"), if (uiState.bodyFatPercent > 0f) "${uiState.bodyFatPercent} %" else "—", Modifier.weight(1f))
        PulseInnerBox("SpO₂", if (uiState.spo2Percent > 0f) "${uiState.spo2Percent} %" else "—", Modifier.weight(1f))
        PulseInnerBox("VO₂max", if (uiState.vo2Max > 0f) "${uiState.vo2Max}" else "—", Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    if (newestFirst.isEmpty()) {
        Text(strings.notEnoughRecentData, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
    } else {
        var all by rememberSaveable { mutableStateOf(false) }
        Text(strings.recentMeasurementsTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.textSecondary)
        (if (all) newestFirst else newestFirst.take(3)).forEach { entry ->
            var menu by remember(entry.id) { mutableStateOf(false) }
            HorizontalDivider(color = palette.cardBorder)
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("health_weight_row_${entry.id}"), verticalAlignment = Alignment.CenterVertically) {
                Text(entry.weightLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary, modifier = Modifier.weight(1f))
                Text(entry.dateLabel, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                if (entry.photoCount > 0) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = tr(lang, "Csatolt fotó", "Attached photo", "Angehängtes Foto", "Fotografie atașată"),
                        tint = palette.primary,
                        modifier = Modifier.size(18.dp).testTag("health_weight_photo_count_${entry.id}")
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${entry.photoCount}", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                    Spacer(Modifier.width(4.dp))
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.testTag("health_weight_menu_${entry.id}")) {
                        Icon(Icons.Default.MoreVert, contentDescription = tr(lang, "Műveletek", "Actions", "Aktionen", "Acțiuni"), tint = palette.textSecondary)
                    }
                    androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        androidx.compose.material3.DropdownMenuItem(text = { Text(strings.editAction) }, onClick = { menu = false; onEdit(entry) },
                            modifier = Modifier.testTag("health_weight_edit_${entry.id}"))
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(tr(lang, "Fotó hozzáadása", "Add photo", "Foto hinzufügen", "Adaugă fotografie")) },
                            onClick = { menu = false; onPhoto(entry) },
                            modifier = Modifier.testTag("health_weight_photo_${entry.id}")
                        )
                        androidx.compose.material3.DropdownMenuItem(text = { Text(strings.deleteAction, color = palette.danger) }, onClick = { menu = false; onDelete(entry) },
                            modifier = Modifier.testTag("health_weight_delete_${entry.id}"))
                    }
                }
            }
        }
        if (newestFirst.size > 3) TextButton(onClick = { all = !all }, modifier = Modifier.heightIn(min = 48.dp).testTag("health_weight_all_toggle")) {
            Text(if (all) tr(lang, "▴ Kevesebb", "▴ Less", "▴ Weniger", "▴ Mai puțin")
                else tr(lang, "▾ Összes mérés (${newestFirst.size})", "▾ All entries (${newestFirst.size})", "▾ Alle Messungen (${newestFirst.size})", "▾ Toate (${newestFirst.size})"))
        }
    }
    Spacer(Modifier.height(6.dp))
    Surface(shape = RoundedCornerShape(14.dp), color = palette.surfaceCard,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .trainPilotClickable(RoundedCornerShape(14.dp)) { onAdd() }.testTag("health_add_weight_button")) {
        Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
            Text(strings.newMeasurementAction, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.primary)
        }
    }
}

/** 2785: horizontal positions (0..1) of pulse readings on their own time axis. */
internal fun pulseTimeFractions(readings: List<com.repforge.app.NativeDailyHealthDetails.PulsePoint>): List<Float> {
    val times = readings.map { runCatching { java.time.Instant.parse(it.timeIso).toEpochMilli() }.getOrDefault(0L) }
    val first = times.firstOrNull() ?: return emptyList()
    val delta = ((times.lastOrNull() ?: first) - first).coerceAtLeast(1L).toFloat()
    return times.map { ((it - first) / delta).coerceIn(0f, 1f) }
}

/** Local clock time of a reading ("14:32"). */
internal fun pulseClockLabel(timeIso: String, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String =
    runCatching { java.time.Instant.parse(timeIso).atZone(zone).toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) }
        .getOrDefault("—")

/** 2785: "nov. 18. · 8 432 lépés"; a missing day says "nincs adat" (never 0). */
internal fun todayTileWeekLabel(key: String, date: java.time.LocalDate, value: Double?, lang: String): String {
    val l = lang.take(2)
    val locale = java.util.Locale.forLanguageTag(l)
    val day = com.example.ui.components.chartDateLabel(date, l)
    // 2788: the workout journal is complete, so a day without a workout is "no workout", not "no data".
    if (value == null && key == "workout") return "$day · " + when (l) { "en" -> "no workout"; "de" -> "kein Training"; "ro" -> "fără antrenament"; else -> "nincs edzés" }
    if (value == null) return "$day · " + when (l) { "en" -> "no data"; "de" -> "keine Daten"; "ro" -> "fără date"; else -> "nincs adat" }
    val number = java.text.NumberFormat.getIntegerInstance(locale).format(Math.round(value))
    val unit = when (key) {
        "steps" -> when (l) { "en" -> "steps"; "de" -> "Schritte"; "ro" -> "pași"; else -> "lépés" }
        "calories" -> "kcal"
        "pulse" -> "bpm"
        else -> when (l) { "en" -> "min"; "de" -> "Min."; "ro" -> "min"; else -> "perc" }
    }
    return "$day · $number $unit"
}

/**
 * 2788: one calorie day as text. Total = resting + active (measured by the provider); the two are never
 * added together. "nincs adat" for what was not measured; no estimate.
 */
internal fun calorieDayLabel(date: java.time.LocalDate, total: Int?, active: Int?, lang: String): String {
    val l = lang.take(2)
    val day = com.example.ui.components.chartDateLabel(date, l)
    val nf = java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag(l))
    val noData = when (l) { "en" -> "no data"; "de" -> "keine Daten"; "ro" -> "fără date"; else -> "nincs adat" }
    val totalWord = when (l) { "en" -> "total"; "de" -> "gesamt"; "ro" -> "total"; else -> "teljes" }
    val activeWord = when (l) { "en" -> "active"; "de" -> "aktiv"; "ro" -> "activ"; else -> "aktív" }
    val ofWhich = when (l) { "en" -> "of which active"; "de" -> "davon aktiv"; "ro" -> "din care activ"; else -> "ebből aktív" }
    return when {
        total != null && active != null -> "$day · $totalWord ${nf.format(total)} kcal · $ofWhich ${nf.format(active)}"
        total != null -> "$day · $totalWord ${nf.format(total)} kcal · $activeWord: $noData"
        active != null -> "$day · $activeWord ${nf.format(active)} kcal · $totalWord: $noData"
        else -> "$day · $noData"
    }
}

/**
 * 2784/2788: extras of a Today tile sheet. The sheet is about TODAY: steps and workouts link to the 7-day
 * history in Weekly goals (no second copy of that chart here); calories show total vs active per day;
 * pulse keeps its 7-day averages. Today's own records appear only when there are any.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayTileSheetExtras(key: String, uiState: TrainPilotUiState, today: java.time.LocalDate, onOpenWeekly: () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val lang = uiState.language
    val days = (6 downTo 0).map { today.minusDays(it.toLong()).toString() }
    val weekly = (uiState.weeklyHealthDays + uiState.dailyActivityDays).associateBy { it.dateIso }
    if (key == "steps") com.example.ui.components.StudioHourlyStepsChart(
        day = today,
        provider = if (uiState.primaryHealthSourceSamsung) com.repforge.app.HealthJournalStore.SAMSUNG
            else com.repforge.app.HealthJournalStore.HC,
        origin = weekly[today.toString()]?.stepOriginPackage.orEmpty(),
        revision = uiState.healthRefreshRevision,
        language = lang
    )
    if (key == "steps" || key == "workout") {
        Surface(shape = RoundedCornerShape(10.dp), color = palette.surfaceCard,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .trainPilotClickable(RoundedCornerShape(10.dp)) { onOpenWeekly() }.testTag("health_today_tile_open_weekly_$key")) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (key == "steps") tr(lang, "Heti lépések és előzmények", "Weekly steps and history", "Wochenschritte und Verlauf", "Pașii săptămânii și istoric")
                    else tr(lang, "Heti edzések és előzmények", "Weekly workouts and history", "Wochentrainings und Verlauf", "Antrenamentele săptămânii și istoric"),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.primary, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.primary, modifier = Modifier.size(16.dp))
            }
        }
    } else if (key == "calories") {
        val totals = days.map { d -> weekly[d]?.totalCaloriesKcal }
        val actives = days.map { d -> weekly[d]?.activeCaloriesMeasured }
        if (totals.any { it != null } || actives.any { it != null }) {
            Text(tr(lang, "Napi kalóriaégetés · 7 nap", "Daily calories burned · 7 days", "Täglicher Kalorienverbrauch · 7 Tage", "Calorii arse zilnic · 7 zile"),
                style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
            val max = days.indices.maxOf { (totals[it] ?: actives[it] ?: 0) }.coerceAtLeast(1).toFloat()
            var sel by remember(key, today) { androidx.compose.runtime.mutableIntStateOf(days.lastIndex) }
            val neutral = trainPilotChartMarkColor(false, palette.primary, palette.textSecondary)
            val resting = neutral.copy(alpha = 0.36f)
            val unsplit = neutral.copy(alpha = 0.55f)
            val langCode = lang.code.lowercase()
            val shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
            Row(
                Modifier.fillMaxWidth().height(72.dp).testTag("health_today_tile_week_calories"),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                days.indices.forEach { i ->
                    val selected = i == sel
                    val total = totals[i]
                    val active = actives[i]
                    val color = trainPilotChartMarkColor(selected, palette.primary, palette.textSecondary)
                    val label = calorieDayLabel(java.time.LocalDate.parse(days[i]), total, active, langCode)
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .trainPilotClickable(RoundedCornerShape(6.dp)) { sel = i }
                            .trainPilotChartSelection(selected, label)
                            .testTag("health_today_tile_week_calories_$i"),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (total != null && active != null && total > 0 && active in 0..total) {
                            Column(Modifier.fillMaxWidth().fillMaxHeight((total / max).coerceIn(0.05f, 1f)).clip(shape)) {
                                val activeShare = active.toFloat() / total
                                if (activeShare < 1f) Box(
                                    Modifier.fillMaxWidth().weight(1f - activeShare)
                                        .background(if (selected) color.copy(alpha = 0.42f) else resting)
                                )
                                if (activeShare > 0f) Box(Modifier.fillMaxWidth().weight(activeShare).background(color))
                            }
                        } else if (total != null) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight((total / max).coerceIn(0.05f, 1f))
                                .background(if (selected) color.copy(alpha = 0.65f) else unsplit, shape))
                        } else if (active != null) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight((active / max).coerceIn(0.05f, 1f)).background(color, shape))
                        } else {
                            Box(Modifier.fillMaxWidth().height(2.dp).background(color))
                        }
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CalorieChartLegend(neutral, tr(lang, "aktív (mozgás)", "active (movement)", "aktiv (Bewegung)", "activ (mișcare)"))
                CalorieChartLegend(resting, tr(lang, "nyugalmi", "resting", "Ruhe", "repaus"))
                if (days.indices.any { i ->
                        val total = totals[i]
                        total != null && (total <= 0 || actives[i]?.let { it in 0..total } != true)
                    }) {
                    CalorieChartLegend(unsplit, tr(lang, "teljes (bontás nélkül)", "total (no breakdown)",
                        "gesamt (ohne Aufteilung)", "total (fără defalcare)"))
                }
            }
            val summaryShape = RoundedCornerShape(10.dp)
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = summaryShape,
                color = palette.primary.copy(alpha = 0.12f),
                border = TrainPilotFrames.control(palette.primary, selected = true)
            ) {
                Text(
                    text = calorieDayLabel(java.time.LocalDate.parse(days[sel]), totals[sel], actives[sel], langCode),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).testTag("health_today_tile_week_info_calories"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary
                )
            }
        }
    } else if (key == "pulse") {
        val values = days.map { d -> weekly[d]?.pulse?.averageBpm }
        if (values.any { it != null }) {
            Text(tr(lang, "Utolsó 7 nap", "Last 7 days", "Letzte 7 Tage", "Ultimele 7 zile"), style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
            com.example.ui.components.WeeklyDayBars(days.map { java.time.LocalDate.parse(it) }, values,
                label = { date, v -> todayTileWeekLabel("pulse", date, v, lang.name.lowercase()) }, tag = "health_today_tile_week_pulse",
                lang = lang.name.lowercase())
        }
    }
    val type = when (key) {
        "sleep" -> "SleepSessionRecord"
        "steps" -> com.repforge.app.ui.health.NativeDetailedHealthMetrics.recordType("steps")
        "calories" -> com.repforge.app.ui.health.NativeDetailedHealthMetrics.recordType("activeCalories")
        else -> null
    } ?: return
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val provider = if (uiState.primaryHealthSourceSamsung) com.repforge.app.HealthJournalStore.SAMSUNG else com.repforge.app.HealthJournalStore.HC
    val generation by androidx.compose.runtime.produceState<Long?>(null, uiState.healthRefreshRevision) {
        value = runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.repforge.app.HealthJournalStore.get(context).generation() } }.getOrNull()
    }
    val g = generation ?: return
    // 2788: no empty "Today's records" block with a pager when there is nothing stored.
    com.repforge.app.ui.health.HealthRecordDayDetails(today, provider, "", type, lang.name.lowercase(), g,
        "${uiState.healthRefreshRevision}", visible = true,
        title = tr(lang, "Mai rekordok", "Today's records", "Heutige Einträge", "Înregistrările de azi"), hideWhenEmpty = true)
}


@Composable
private fun CalorieChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(9.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalTrainPilotPalette.current.textSecondary)
    }
}

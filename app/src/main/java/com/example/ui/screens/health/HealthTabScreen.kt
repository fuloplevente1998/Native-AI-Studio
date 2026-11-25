package com.example.ui.screens
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.trainPilotPageBackdrop
import com.example.ui.theme.LocalGlassTopInset
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import com.example.ui.components.PageLayoutCard

import com.example.ui.components.PageLayoutBlock
import com.example.ui.components.PageLayoutFrame
import com.example.ui.components.PageLayoutFooter
import com.example.ui.components.PageLayoutTile
import com.example.ui.components.rememberPageLayout

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
import com.example.ui.components.healthDetailsToggle
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.SubScreen
import com.example.ui.components.MockupHealthConnectBadge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.selectionColors
import com.example.ui.viewmodel.TrainPilotUiState

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
    onWeightPhotosUpdated: () -> Unit = {},
    requestedJournalFocus: Pair<String, String>? = null,
    onJournalFocusConsumed: () -> Unit = {}
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
    // 2783: a Today tile with data opens the journal at today's records of that type.
    var journalFocus by remember { mutableStateOf<Pair<String, String>?>(null) }
    var journalFocusKey by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(requestedJournalFocus) {
        if (requestedJournalFocus != null) {
            journalFocus = requestedJournalFocus
            journalFocusKey++
            onJournalFocusConsumed()
        }
    }
    var expandedTodayTileKey by remember { mutableStateOf<String?>(null) }
    var selectedFitnessReading by remember { mutableStateOf<BodyFitnessReading?>(null) }
    val healthSectionState = rememberSaveableStateHolder()
    val healthSectionSheet = when {
        uiState.expandedHealthJournal -> "journal"
        uiState.expandedHealthConnect -> "connect"
        else -> null
    }
    val sourcesTitle = tr(uiState.language, "Források, óra és szinkron", "Sources, watch & sync",
        "Quellen, Uhr & Sync", "Surse, ceas și sincronizare")
    val blocks = listOf(
        PageLayoutBlock("today", tr(uiState.language, "Mai állapot", "Today’s status", "Heutiger Status", "Starea de azi")),
        PageLayoutBlock("weekly", if (uiState.workoutGoalPeriod == com.example.model.WorkoutGoalPeriod.MONTHLY)
            tr(uiState.language, "Havi célok", "Monthly goals", "Monatsziele", "Obiective lunare")
            else tr(uiState.language, "Heti célok", "Weekly goals", "Wochenziele", "Obiective săptămânale")),
        PageLayoutBlock("body_fitness", tr(uiState.language, "Test és fittség", "Body & fitness", "Körper & Fitness", "Corp și fitness")),
        PageLayoutBlock("weight", tr(uiState.language, "Testsúlynapló", "Weight log", "Gewichtsverlauf", "Jurnal de greutate")),
        PageLayoutBlock("journal", strings.healthJournalTitle),
        PageLayoutBlock("sources", sourcesTitle)
    )
    val layout = rememberPageLayout("health", blocks.map { it.id },
        newBlockPlacements = listOf("body_fitness" to "today", "weekly" to "today"))
    LaunchedEffect(Unit) {
        if (uiState.expandedHealthWeeklyGoals && healthListState.firstVisibleItemIndex == 0 &&
            healthListState.firstVisibleItemScrollOffset == 0) {
            healthListState.scrollToItem(layout.saved.indexOf("weekly"))
        }
    }
    LaunchedEffect(healthSectionSheet) {
        if (healthSectionSheet != null) expandedTodayTileKey = null
    }

    val healthBlock: @Composable (String) -> Unit = { id ->
    val hasGrid = id == "today" || id == "body_fitness"
    PageLayoutTile(layout, id, hasHorizontalTiles = hasGrid, overlayHandle = hasGrid) {
        when (id) {
            "today" -> {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.cardPaint,
                    modifier = Modifier
                        .fillMaxWidth().trainPilotGlassMaterial(palette, RoundedCornerShape(24.dp))
                        .trainPilotBorder(1.2.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        PageLayoutCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.selectionColors().surface,
                                modifier = Modifier
                                    .size(38.dp)
                                    .trainPilotBorder(1.dp, palette.selectionColors().border, RoundedCornerShape(14.dp))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = palette.selectionColors().foreground,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = strings.todayStateTitle,
                                        style = MaterialTheme.typography.titleMedium,
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
                            if (layout.draft != null) Spacer(Modifier.width(24.dp))
                            else IconButton(
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
                        }

                        // 2784: every tile opens its own detail sheet; the page below does not move.
                        StudioTodayStatusGrid(uiState, layout = layout) { key ->
                            selectedFitnessReading = null
                            expandedTodayTileKey = if (expandedTodayTileKey == key) null else key
                            if (key == "pulse") onToggleHealthSection("pulse")
                        }

                    }
                }
            }
            "weekly" -> {
                val goalShape = RoundedCornerShape(14.dp)
                Column(
                    Modifier.fillMaxWidth()
                        .trainPilotBorder(1.dp, palette.sectionBorder, goalShape)
                        .clip(goalShape)
                        .background(palette.surfaceCard)
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
                        Column(Modifier.padding(8.dp)) {
                            HealthWeeklyGoalsPanel(uiState)
                        }
                    }
                }
            }
            "body_fitness" -> {
                val shape = RoundedCornerShape(20.dp)
                Surface(
                    shape = shape,
                    color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.cardPaint,
                    modifier = Modifier.fillMaxWidth().trainPilotGlassMaterial(palette, shape).trainPilotBorder(1.dp, palette.cardBorder, shape)
                ) {
                    Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        StudioBodyFitnessTiles(uiState, layout) { key, reading ->
                            selectedFitnessReading = reading
                            expandedTodayTileKey = key
                        }
                    }
                }
            }
            "weight" -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.cardPaint,
                    modifier = Modifier
                        .fillMaxWidth().trainPilotGlassMaterial(palette, RoundedCornerShape(20.dp))
                        .testTag("health_weight_card")
                        .trainPilotBorder(
                            width = 1.dp,
                            color = if (uiState.expandedHealthWeightLog) palette.selectionColors().border else palette.cardBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
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
                                color = palette.selectionColors().surface,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MonitorWeight,
                                        contentDescription = null,
                                        tint = palette.selectionColors().foreground,
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
                        AnimatedContent(
                            targetState = uiState.expandedHealthWeightLog,
                            transitionSpec = {
                                (fadeIn(tween(150)) togetherWith fadeOut(tween(100)))
                                    .using(SizeTransform { _, _ -> tween(220) })
                            },
                            contentAlignment = Alignment.TopStart,
                            label = "weight log"
                        ) { expanded ->
                            Column(Modifier.fillMaxWidth()) {
                            WeightLogBody(
                                uiState = uiState,
                                expanded = expanded,
                                strings = strings,
                                chronological = if (expanded) chronologicalMeasurements else compactMeasurements,
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
                }
            }
            "journal" -> {
                HealthSectionLauncher(
                    icon = Icons.Default.DateRange,
                    title = strings.healthJournalTitle,
                    subtitle = null,
                    onOpen = { onToggleHealthSection("journal") }
                )
            }
            "sources" -> {
                HealthSectionLauncher(
                    icon = Icons.Default.Link,
                    title = sourcesTitle,
                    subtitle = listOf(
                        "Samsung Health " + if (uiState.samsungGrantedCount > 0) "✓" else "—",
                        "Health Connect " + if (uiState.healthGrantedCount > 0) "✓" else "—",
                        tr(uiState.language, "Óra", "Watch", "Uhr", "Ceas") + " " + if (uiState.watchAppAvailable) "✓" else "—"
                    ).joinToString(" · "),
                    onOpen = { onToggleHealthSection("connect") },
                    customLeadingBadge = { MockupHealthConnectBadge(size = 34.dp) }
                )
            }
        }
    }
    }
    PageLayoutFrame(layout, uiState.language, blocks, healthListState, TrainPilotCardSpacing.CardGap) {
    com.example.ui.components.TrainPilotScrollableListBox(
        lazyListState = healthListState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = healthListState,
            modifier = Modifier
                .fillMaxSize()
                .trainPilotPageBackdrop(palette)
                .testTag("health_tab_screen"),
            contentPadding = PaddingValues(start = TrainPilotCardSpacing.ScreenGutter, end = TrainPilotCardSpacing.ScreenGutter, top = 8.dp + LocalGlassTopInset.current, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
        if (layout.draft == null) {
            layout.order.forEach { id ->
                item(key = id) { healthBlock(id) }
            }
        } else {
            // Keep all draggable Health blocks composed so edge scrolling cannot dispose the dragged row.
            item(key = "health_edit_blocks") {
                Column(verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)) {
                    layout.order.forEach { id -> key(id) { healthBlock(id) } }
                }
            }
        }
        item(key = "customize") { PageLayoutFooter(layout, uiState.language) }
        }
    }

    }

    val tileKey = expandedTodayTileKey
    if (tileKey != null && healthSectionSheet == null) {
        StudioTodayStatusDetailSheet(
            uiState, strings, tileKey, today,
            onDismiss = { expandedTodayTileKey = null },
            fitnessReading = selectedFitnessReading,
            onOpenJournal = { focus ->
                journalFocus = focus
                journalFocusKey++
                if (!uiState.expandedHealthJournal) onToggleHealthSection("journal")
            },
            onTogglePulseTrend = { onToggleHealthSection("pulse") },
            onOpenWeekly = {
                if (!uiState.expandedHealthWeeklyGoals) onToggleHealthSection("weekly")
                coroutineScope.launch {
                    healthListState.animateScrollToItem(layout.saved.indexOf("weekly"))
                }
            },
            onOpenLoggedWorkout = onOpenLoggedWorkout,
            onNavigateToSubScreen = onNavigateToSubScreen
        )
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

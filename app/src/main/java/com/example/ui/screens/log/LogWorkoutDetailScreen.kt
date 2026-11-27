package com.example.ui.screens

import com.example.ui.components.TrainPilotListDivider
import com.example.ui.components.trainPilotListPanel

import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.overlayRoleFill
import com.example.ui.theme.GlassMaterialRole
import com.example.model.localWorkoutDateTimeLabel

import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassTile
import com.example.ui.theme.glassCardFill
import com.example.ui.theme.trainPilotScreenBackdrop

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.example.ui.components.trainPilotAnimatedExpansion
import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.ui.components.AnatomicalMuscleHeatmapCard
import com.example.ui.components.MockupHealthConnectBadge
import com.example.ui.components.MockupWatchGraphicBadge
import com.example.ui.components.SmallExerciseVideoButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * 2. EDZÉS RÉSZLETEI (Mockup 3/4 #2)
 */
@Composable
fun LogWorkoutDetailScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectDetailTab: (Int) -> Unit,
    onToggleExpandedExercise: (Int) -> Unit,
    onOpenPhotos: () -> Unit,
    onOpenRouteMap: () -> Unit,
    onOpenWatchSync: () -> Unit,
    onOpenHealthTab: () -> Unit,
    workoutHealth: @Composable () -> Unit = {},
    deleteActions: (@Composable () -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    val workout = uiState.loggedWorkouts.find { it.id == uiState.selectedLogWorkoutId }

    if (workout == null) {
        Column(
            modifier = Modifier
                .then(com.example.ui.components.sheetAwareFillMaxSize())
                .trainPilotScreenBackdrop(palette)
                .padding(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            SubScreenTopBar(
                title = strings.workoutDetailTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.glassTile(palette.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                    .padding(24.dp).trainPilotGlassRim(palette, RoundedCornerShape(18.dp))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = trL(uiState.language, "Nincs kiválasztott edzés", "No workout selected", "Kein Training ausgewählt", "Niciun antrenament selectat"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )
                    Text(
                        text = trL(uiState.language, "Válassz egy befejezett edzést a Naplóból a részletek megtekintéséhez.", "Select a completed workout from the Log to view details.", "Wähle ein abgeschlossenes Training aus dem Protokoll für Details.", "Selectează un antrenament finalizat din Jurnal pentru detalii."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .then(com.example.ui.components.sheetAwareFillMaxSize())
            .trainPilotScreenBackdrop(palette)
            .testTag("log_workout_detail_screen"),
        contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (uiState.logDetailTabIndex == 0) 4.dp else TrainPilotCardSpacing.CardGap)
    ) {
        // Summary Header Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.glassTile(palette.surfaceCard),
                modifier = Modifier.trainPilotGlassRim(palette, RoundedCornerShape(22.dp))
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(22.dp))
                    .testTag("journal_detail_header")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${strings.navProgress} · ${strings.navLog}", style = MaterialTheme.typography.labelSmall,
                                color = palette.primary, modifier = Modifier.testTag("journal_detail_location"))
                            Text(workout.localizedTitle(uiState.language, uiState.programs, uiState.exercises), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = palette.textPrimary,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(workout.localWorkoutDateTimeLabel(), style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        com.example.ui.components.TrainPilotRedCloseButton(onClick = onBack, testTag = "subscreen_back_button")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = palette.cardBorder)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${workout.durationMinutes} ${trL(uiState.language, "perc", "min", "Min.", "min")}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text(trL(uiState.language, "edzésidő", "duration", "Trainingszeit", "durată"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                        Column {
                            Text(workout.workTimeLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text(trL(uiState.language, "munkaidő", "work time", "Arbeitszeit", "timp activ"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                        Column {
                            Text(workout.restTimeLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text(trL(uiState.language, "pihenőidő", "rest time", "Pausenzeit", "timp odihnă"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                    }
                }
            }
        }

        if (deleteActions != null) item { deleteActions() }

        // Sub-tabs: Gyakorlatok (5) | Statisztikák | Forrás
        item {
            val tabTitles = listOf(
                if (workout.isOutdoorRun) trL(uiState.language, "Aktivitás", "Activity", "Aktivität", "Activitate") else trL(uiState.language, "Gyakorlatok (${workout.exerciseDetails.size})", "Exercises (${workout.exerciseDetails.size})", "Übungen (${workout.exerciseDetails.size})", "Exerciții (${workout.exerciseDetails.size})"),
                trL(uiState.language, "Statisztikák", "Statistics", "Statistiken", "Statistici"),
                trL(uiState.language, "Forrás", "Source", "Quelle", "Sursă")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabTitles.forEachIndexed { idx, label ->
                    val selected = uiState.logDetailTabIndex == idx
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.overlayRoleFill(if (selected) palette.primary else palette.surfaceCard,
                            if (selected) GlassMaterialRole.Selection else GlassMaterialRole.Button),
                        modifier = Modifier
                            .weight(1f)
                            .trainPilotClickable(RoundedCornerShape(12.dp)) { onSelectDetailTab(idx) }
                            .testTag("log_detail_tab_$idx")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selected && !(palette.isLiquidGlass && com.example.ui.theme.LocalGlassOverlay.current)) palette.onPrimary else if (selected) palette.textPrimary else palette.textSecondary,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }

        // Tab-specific content for Gyakorlatok (0) | Statisztikák (1) | Forrás (2)
        if (uiState.logDetailTabIndex == 0) {
            val details = workout.exerciseDetails
            if (workout.originDevice == "external" && details.isEmpty()) item {
                Text(trL(uiState.language, "Nincs igazolt gyakorlat- vagy sorozatadat.", "No verified exercise or set data.", "Keine bestätigten Übungs- oder Satzdaten.", "Nu există date confirmate despre exerciții sau serii."), color = palette.textSecondary)
            }
            if (details.isNotEmpty()) item(key = "log_exercises") {
                Column(Modifier.fillMaxWidth().trainPilotListPanel().padding(horizontal = 8.dp)) {
                    details.forEachIndexed { index, exDetail ->
                        if (index > 0) TrainPilotListDivider()
                        val expanded = uiState.expandedLogExerciseIndex == index
                        val shape = RoundedCornerShape(18.dp)
                        val exerciseName = exDetail.localizedName(uiState.language, uiState.exercises)
                        val expansionState = if (expanded) {
                            trL(uiState.language, "Kinyitva", "Expanded", "Ausgeklappt", "Extins")
                        } else {
                            trL(uiState.language, "Összecsukva", "Collapsed", "Eingeklappt", "Restrâns")
                        }
                        Column(Modifier.fillMaxWidth().trainPilotAnimatedExpansion()) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .trainPilotClickable(shape, stretchOnDrag = true) { onToggleExpandedExercise(index) }
                                    .semantics { stateDescription = expansionState }
                                    .testTag("log_exercise_card_$index")
                                    .padding(horizontal = 4.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exerciseName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = exDetail.localizedSummaryPrescription(uiState.language),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                }
                                SmallExerciseVideoButton(
                                    exerciseName = exerciseName,
                                    exerciseId = uiState.exercises.firstOrNull { it.nameHu.equals(exDetail.nameHu, ignoreCase = true) }?.id
                                )
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = palette.textSecondary
                                )
                            }

                            if (expanded) Column(Modifier.padding(horizontal = 4.dp, vertical = 3.dp)) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = palette.cardBorder)
                                Spacer(modifier = Modifier.height(8.dp))
                                exDetail.sets.forEach { setRow ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (workout.isOutdoorRun || exDetail.measurementType == "distance") {
                                                trL(uiState.language, "${setRow.setNumber}. mérés", "Measurement #${setRow.setNumber}", "${setRow.setNumber}. Messung", "Măsurătoarea #${setRow.setNumber}")
                                            } else {
                                                trL(uiState.language, "${setRow.setNumber}. sorozat", "Set #${setRow.setNumber}", "${setRow.setNumber}. Satz", "Seria #${setRow.setNumber}")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textSecondary
                                        )
                                        Text(
                                            text = com.example.model.StudioWorkoutJournalDisplay.setLabel(setRow, exDetail.measurementType, exDetail.repUnit, workout.isOutdoorRun, uiState.language),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary
                                        )
                                        if (setRow.completed) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = trL(uiState.language, "Teljesített mérés", "Completed measurement", "Abgeschlossene Messung", "Măsurătoare finalizată"),
                                                tint = palette.success,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(trL(uiState.language, "Nem teljesített", "Not completed", "Nicht abgeschlossen", "Nefinalizat"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (uiState.logDetailTabIndex == 1) {
            item {
                if (workout.isOutdoorRun) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.glassTile(palette.surfaceCard),
                        modifier = Modifier.trainPilotGlassRim(palette, RoundedCornerShape(18.dp)).fillMaxWidth().trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(trL(uiState.language, "Kültéri edzés mért adatai", "Outdoor workout measured data", "Gemessene Daten des Outdoor-Trainings", "Date măsurate ale antrenamentului exterior"), style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text("${trL(uiState.language, "Aktív idő", "Active time", "Aktive Zeit", "Timp activ")}: ${workout.workTimeLabel}", color = palette.textPrimary)
                            val distVal = if (workout.distanceKm > 0f) "%.2f km".format(java.util.Locale.forLanguageTag("hu-HU"), workout.distanceKm) else trL(uiState.language, "Nincs GPS-adat", "No GPS data", "Keine GPS-Daten", "Fără date GPS")
                            Text("${strings.distanceLabel}: $distVal", color = palette.textPrimary)
                            val paceVal = workout.paceLabel.takeUnless { it.isBlank() || it == "—" } ?: trL(uiState.language, "Nincs adat", "No data", "Keine Daten", "Fără date")
                            Text("${strings.avgPaceLabel}: $paceVal", color = palette.textPrimary)
                        }
                    }
                } else {
                    val localizedTitle = workout.localizedTitle(uiState.language, uiState.programs, uiState.exercises)
                    AnatomicalMuscleHeatmapCard(
                        title = "${strings.muscleLoadTitle} ($localizedTitle)",
                        subtitle = "",
                        realMuscleLoads = uiState.logWorkoutMuscleLoads,
                        periodDays = null
                    )
                }
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.glassTile(palette.surfaceCard),
                    modifier = Modifier.trainPilotGlassRim(palette, RoundedCornerShape(18.dp))
                        .fillMaxWidth()
                        .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(trL(uiState.language, "Adatforrás és szinkron napló", "Data source & sync log", "Datenquelle & Synchronisationsprotokoll", "Sursă de date și jurnal de sincronizare"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        val originLabel = when (workout.originDevice) {
                            "external" -> trL(uiState.language, "Külső forrás, eszköz nem igazolt", "External source, device unverified", "Externe Quelle, Gerät nicht bestätigt", "Sursă externă, dispozitiv neverificat")
                            "watch" -> trL(uiState.language, "Óra", "Watch", "Uhr", "Ceas")
                            else -> trL(uiState.language, "Telefon", "Phone", "Telefon", "Telefon")
                        }
                        Text("• ${trL(uiState.language, "Rögzítő eszköz", "Recording device", "Aufzeichnungsgerät", "Dispozitiv de înregistrare")}: $originLabel", color = palette.textPrimary)
                        // #359: an imported workout's source lives here, not in its name or description.
                        if (workout.originDevice == "external") Text("• ${trL(uiState.language, "Forrás", "Source", "Quelle", "Sursă")}: " +
                            (if (workout.externalProvider == com.repforge.app.HealthJournalStore.HC &&
                                workout.externalSource == "com.sec.android.app.shealth")
                                com.repforge.app.NativeWorkoutHealthSummary.originLabel(com.repforge.app.HealthJournalStore.HC,
                                    setOf("com.sec.android.app.shealth"), uiState.language.name.lowercase())
                            else com.example.model.externalSourceLabel(workout.externalProvider, workout.externalSource)).ifBlank {
                                trL(uiState.language, "Forrásadat nem elérhető", "Source unavailable", "Quelle nicht verfügbar", "Sursă indisponibilă")
                            }, color = palette.textPrimary, modifier = Modifier.testTag("log_detail_external_source"))
                    }
                }
            }
        }

        // The native observer remains outside this list; changing tabs only changes presentation.
        if (uiState.logDetailTabIndex in 1..2) item(key = "workout_health") {
            androidx.compose.runtime.CompositionLocalProvider(
                com.example.ui.components.LocalWorkoutHealthView provides
                    if (uiState.logDetailTabIndex == 1) com.example.ui.components.WorkoutHealthView.Summary
                    else com.example.ui.components.WorkoutHealthView.Sources
            ) { workoutHealth() }
        }

                        // Fotók (2) & Útvonal megtekintése buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrainPilotOutlinedButton(
                    onClick = onOpenPhotos,
                    enabled = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("log_detail_photos_button")
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(trL(uiState.language, "Fotók (${workout.photoCount})", "Photos (${workout.photoCount})", "Fotos (${workout.photoCount})", "Fotografii (${workout.photoCount})"), color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                }
                TrainPilotOutlinedButton(
                    onClick = onOpenRouteMap,
                    enabled = workout.routeKeys.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("log_detail_route_button")
                ) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (workout.routeKeys.isEmpty()) trL(uiState.language, "Nincs útvonal", "No route", "Keine Route", "Fără traseu") else trL(uiState.language, "Útvonal megtekintése", "View route", "Route ansehen", "Vezi traseul"), color = palette.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }

    }
}

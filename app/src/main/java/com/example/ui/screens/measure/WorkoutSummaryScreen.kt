package com.example.ui.screens

import com.example.ui.theme.trainPilotGlassRim

import com.example.ui.theme.trainPilotScreenBackdrop
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassTile
import com.example.ui.theme.glassCardFill
import com.repforge.app.ui.theme.trainPilotClickable
import androidx.compose.ui.semantics.contentDescription

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.localization.LocalizedStrings
import com.example.model.SubScreen
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.repforge.app.WorkoutPhotoRepository
import com.repforge.app.ActiveWorkoutStore
import org.json.JSONObject
import org.json.JSONArray
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SummaryAttachedPhoto(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val bytes: Long
)

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
    onDiscardWorkout: () -> Unit,
    onExerciseEffort: (String, Int) -> Unit = { _, _ -> },
    onExerciseNote: (String, String) -> Unit = { _, _ -> }
) {
    val palette = LocalTrainPilotPalette.current
    val lang = uiState.language
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoRepo = remember(context) { WorkoutPhotoRepository(context) }
    var selectedPhotoCategory by remember { mutableStateOf("after") }
    var exerciseFeedbackExpanded by androidx.compose.runtime.saveable.rememberSaveable(uiState.activeWorkoutId) { mutableStateOf(false) }

    val attachedPhotos = remember(uiState.activeWorkoutId) {
        val list = mutableStateListOf<SummaryAttachedPhoto>()
        val current = ActiveWorkoutStore.current(context)
        val arr = current?.optJSONArray("photos")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                if (id.isNotBlank()) {
                    list.add(
                        SummaryAttachedPhoto(
                            id = id,
                            label = obj.optString("label", "after"),
                            width = obj.optInt("width", 0),
                            height = obj.optInt("height", 0),
                            bytes = obj.optLong("bytes", 0L)
                        )
                    )
                }
            }
        }
        list
    }

    var photoError by remember { mutableStateOf(false) }
    var photoBusy by remember { mutableStateOf(false) }
    var pendingPhoto by remember { mutableStateOf<com.repforge.app.NativeWorkoutPhotos.Request?>(null) }

    fun showSavedPhotos(photos: JSONArray) {
        attachedPhotos.clear()
        for (i in 0 until photos.length()) {
            val p = photos.getJSONObject(i)
            attachedPhotos.add(SummaryAttachedPhoto(p.getString("id"), p.optString("label", "after"),
                p.optInt("width"), p.optInt("height"), p.optLong("bytes")))
        }
    }

    fun receivePhoto(importPhoto: suspend () -> Result<com.repforge.app.StoredPhotoMetadata?>) {
        val request = pendingPhoto ?: return
        pendingPhoto = null
        scope.launch {
            try {
                val photo = importPhoto().getOrThrow() ?: return@launch
                showSavedPhotos(com.repforge.app.NativeWorkoutPhotos.attach(context, request, photo))
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                photoError = true
            } finally {
                photoBusy = false
            }
        }
    }

    val summaryCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res -> receivePhoto { photoRepo.handleCameraResult(res.resultCode, res.data) } }

    val summaryGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res -> receivePhoto {
        if (res.resultCode == android.app.Activity.RESULT_OK) photoRepo.importPickedUri(res.data?.data)
        else Result.success(null)
    } }


    val summaryListState = androidx.compose.foundation.lazy.rememberLazyListState()
    remember(context) { ActiveWorkoutStore.current(context); com.repforge.app.WearLiveHealthStore.current(context) }
    val summaryFrame by ActiveWorkoutStore.snapshotFlow.collectAsState()
    val summaryLiveHealth by com.repforge.app.WearLiveHealthStore.liveHealthFlow.collectAsState()
    val activeSnapshotForSummary = com.repforge.app.StudioWorkoutSummaryPresentation.current(
        summaryFrame, uiState.activeWorkoutId, uiState.activeWorkoutGeneration)
    val summaryNow = java.time.Instant.now()
    val summaryProvider = com.repforge.app.StudioWorkoutSummaryPresentation.selectedProvider(context)
    val summaryWindowKey = activeSnapshotForSummary?.optJSONArray("activeIntervals")?.toString().orEmpty()
    val summaryPauseRevision = activeSnapshotForSummary?.optLong("pauseRevision") ?: 0L
    val summaryStarted = activeSnapshotForSummary?.optString("started").orEmpty()
    val summaryEnd = remember(uiState.activeWorkoutId, uiState.activeWorkoutGeneration, summaryPauseRevision, summaryWindowKey, summaryStarted) {
        java.time.Instant.now()
    }
    var summaryProviderHealth by remember(uiState.activeWorkoutId, uiState.activeWorkoutGeneration,
        summaryProvider, summaryPauseRevision, summaryWindowKey, summaryStarted) {
        mutableStateOf<com.repforge.app.StudioWorkoutSummaryPresentation.HealthSnapshot?>(null)
    }
    LaunchedEffect(uiState.activeWorkoutId, uiState.activeWorkoutGeneration, summaryProvider,
        summaryPauseRevision, summaryWindowKey, summaryStarted) {
        summaryProviderHealth = null
        var host: android.content.Context = context
        while (host is android.content.ContextWrapper && host !is android.app.Activity) host = host.baseContext
        val activity = host as? android.app.Activity
        val expected = activeSnapshotForSummary?.let { JSONObject(it.toString()) }
        if (activity != null && expected != null) {
            try {
                summaryProviderHealth = com.repforge.app.StudioWorkoutSummaryPresentation.readHealth(
                    context, activity, summaryProvider, expected, summaryEnd)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { /* Keep only existing workout-matched evidence when provider data is unavailable. */ }
        }
    }
    val persistedOutdoorDistKm = remember(uiState.activeWorkoutId, uiState.outdoorDistanceKm) {
        (com.repforge.app.StudioWorkoutBridge.readOutdoorEffectiveDistanceMeters(
            activeSnapshotForSummary,
            (uiState.outdoorDistanceKm * 1000f).toDouble()
        ) / 1000.0).toFloat()
    }
    val isOutdoor = uiState.activeWorkoutSubScreen == SubScreen.ACTIVE_OUTDOOR_GPS || persistedOutdoorDistKm > 0f || uiState.outdoorDistanceKm > 0f
    val effectiveOutdoorDistanceKm = maxOf(uiState.outdoorDistanceKm, persistedOutdoorDistKm)

    com.example.ui.components.TrainPilotScrollableListBox(
        lazyListState = summaryListState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = summaryListState,
            modifier = Modifier
                .fillMaxSize()
                .trainPilotScreenBackdrop(palette)
                .testTag("workout_summary_screen"),
            contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {

        val summarySubtitle = if (isOutdoor) {
            val distLabel = if (effectiveOutdoorDistanceKm > 0f) {
                String.format(com.example.localization.StudioLocale.forLanguage(lang), "%.2f km", effectiveOutdoorDistanceKm)
            } else {
                uiState.outdoorElapsedLabel.ifBlank { "00:00" }
            }
            val titleLabel = com.example.model.LoggedWorkoutEntry(
                id = "",
                titleHu = uiState.activeWorkoutTitleHu.ifBlank { "Kültéri futás" }
            ).localizedTitle(lang, uiState.programs, uiState.exercises)
            "$titleLabel • $distLabel"
        } else {
            val completedExsCount = uiState.activeWorkoutExercises.count { ex ->
                (uiState.activeSetsByExerciseId[ex.id] ?: emptyList()).any { it.completed }
            }
            val totalExsCount = uiState.activeWorkoutExercises.size.coerceAtLeast(1)
            val displayTitle = uiState.activeWorkoutTitleHu.takeIf { it.isNotBlank() }?.let { raw ->
                com.example.model.LoggedWorkoutEntry(
                    id = "",
                    titleHu = raw
                ).localizedTitle(lang, uiState.programs, uiState.exercises)
            } ?: tr(lang, "Tervezett edzés", "Planned workout", "Geplantes Training", "Antrenament planificat")
            tr(
                lang,
                "$displayTitle • $completedExsCount / $totalExsCount gyakorlat",
                "$displayTitle • $completedExsCount / $totalExsCount exercises",
                "$displayTitle • $completedExsCount / $totalExsCount Übungen",
                "$displayTitle • $completedExsCount / $totalExsCount exerciții"
            )
        }

        val elapsedSec = activeSnapshotForSummary?.let {
            com.repforge.app.StudioWorkoutSessionPresentation.elapsedSeconds(it, summaryNow)
        }
        val durationLabel = elapsedSec?.let(com.repforge.app.StudioWorkoutSessionPresentation::durationLabel)
            ?: "—"

        val volumeOrDistLabel: String
        val volumeOrDistHeader: String
        if (isOutdoor) {
            volumeOrDistHeader = strings.distanceLabel
            volumeOrDistLabel = String.format(com.example.localization.StudioLocale.forLanguage(lang), "%.2f km", effectiveOutdoorDistanceKm)
        } else {
            val totalVolKg = uiState.activeSetsByExerciseId.values.flatten().filter { it.completed }.sumOf { (it.weightKg * it.reps).toDouble() }
            val totalReps = uiState.activeSetsByExerciseId.values.flatten().filter { it.completed }.sumOf { it.reps }
            if (totalVolKg > 0.0) {
                volumeOrDistHeader = tr(lang, "Volumen", "Volume", "Volumen", "Volum")
                volumeOrDistLabel = "${totalVolKg.toInt()} kg"
            } else {
                volumeOrDistHeader = tr(lang, "Ismétlések", "Repetitions", "Wiederholungen", "Repetări")
                volumeOrDistLabel = "$totalReps ${tr(lang, "ism.", "reps", "Wdh.", "rep.")}"
            }
        }

        val energyProjection = com.repforge.app.StudioWorkoutSummaryPresentation.projection(
            activeSnapshotForSummary, summaryLiveHealth, summaryNow, summaryProvider, summaryProviderHealth)
        val activeEnergy = energyProjection?.activeEnergy
        val totalEnergy = energyProjection?.totalEnergy
        val sessionEnergy = energyProjection?.energy?.takeIf { it.values.containsKey("workoutCalories") }
        val energyRows = listOfNotNull(
            activeEnergy?.takeIf { it.values.isNotEmpty() }?.let { tr(lang,"Aktív kcal","Active kcal","Aktiv kcal","Kcal active") to it },
            totalEnergy?.takeIf { it.values.isNotEmpty() }?.let { tr(lang,"Teljes kcal","Total kcal","Gesamt kcal","Kcal totale") to it }
        ).ifEmpty { listOfNotNull(sessionEnergy?.let { tr(lang,"Edzésenergia","Session energy","Trainingsenergie","Energie antrenament") to it }) }
        val primaryEnergy = energyRows.firstOrNull()
        val caloriesLabel = primaryEnergy?.second?.values?.values?.singleOrNull()?.let { "${it.toInt()} kcal" } ?: "— kcal"

        item {
            Row(
                modifier = Modifier.fillMaxWidth().testTag("workout_summary_header_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.workoutSummaryTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = summarySubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                com.example.ui.components.StudioWorkoutHealthAction()
            }
        }


        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatTile(tr(lang, "Időtartam", "Duration", "Dauer", "Durată"), durationLabel, Modifier.weight(1f))
                SummaryStatTile(volumeOrDistHeader, volumeOrDistLabel, Modifier.weight(1f))
                SummaryStatTile(primaryEnergy?.first ?: tr(lang, "Kalória", "Calories", "Kalorien", "Calorii"), caloriesLabel, Modifier.weight(1f))
            }
        }
        if (energyRows.isNotEmpty()) item(key = "summary_energy_sources") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                energyRows.forEachIndexed { index, (label, metric) ->
                    val providers = metric.evidence.map { com.repforge.app.NativeWorkoutHealthSummary.label(it.provider) }.distinct().joinToString(", ")
                    val uncertain = metric.incomplete || metric.evidence.any { row ->
                        row.provider != com.repforge.app.NativeWorkoutHealthSummary.WEAR && metric.values.keys.any { key ->
                            com.repforge.app.WorkoutEnergyCoverage.status(row.data,key) != com.repforge.app.WorkoutEnergyCoverage.Status.FULL_RAW
                        }
                    }
                    val value = metric.values.values.single()
                    Text("$label${if (index > 0) ": ${value.toInt()} kcal" else ""} · $providers" +
                        if (uncertain) tr(lang," · lefedettség hiányos vagy nem igazolt"," · incomplete or unverified coverage",
                            " · Abdeckung lückenhaft oder ungeprüft"," · acoperire incompletă sau neverificată") else "",
                        color = palette.textSecondary, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.testTag("workout_summary_energy_source_$index"))
                }
            }
        }

        if (uiState.activeWorkoutExercises.isNotEmpty()) item(key = "exercise_feedback_heading") {
            TrainPilotOutlinedButton(onClick = { exerciseFeedbackExpanded = !exerciseFeedbackExpanded },
                modifier = Modifier.fillMaxWidth().testTag("summary_exercise_feedback_toggle")) {
                Text(tr(lang, "Gyakorlatok visszajelzései", "Exercise feedback", "Übungsfeedback", "Feedback exerciții") +
                    " (${uiState.activeWorkoutExercises.size})", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Icon(if (exerciseFeedbackExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
        }
        if (exerciseFeedbackExpanded) uiState.activeWorkoutExercises.forEach { exercise ->
            item(key = "exercise_feedback_${exercise.id}") {
                val same = uiState.exerciseFeedbackWorkoutId == uiState.activeWorkoutId
                com.example.ui.components.StudioExerciseFeedbackReview(
                    effort = if (same) com.repforge.app.StudioWorkoutFeedback.efforts.getOrNull(
                        uiState.exerciseFeedbackChoices[exercise.id] ?: -1) else null,
                    note = if (same) uiState.exerciseFeedbackNotes[exercise.id].orEmpty() else "",
                    language = uiState.language.name.lowercase(), title = exercise.localizedName(lang),
                    enabled = !uiState.isSavingWorkout,
                    onEffort = { onExerciseEffort(exercise.id, com.repforge.app.StudioWorkoutFeedback.efforts.indexOf(it)) },
                    onNote = { onExerciseNote(exercise.id, it) }, tag = "summary_feedback_${exercise.id}")
            }
        }

        // Optional overall feeling is separate from the exercise answers.
        item(key = "overall_feedback") {
            val same = uiState.summaryFeedbackWorkoutId == uiState.activeWorkoutId
            com.example.ui.components.StudioWorkoutOverallFeedback(
                effort = if (same) com.repforge.app.StudioWorkoutFeedback.efforts.getOrNull(uiState.workoutSummaryMoodIndex) else null,
                note = if (same) uiState.workoutSummaryNote else "",
                language = lang.name.lowercase(), enabled = !uiState.isSavingWorkout,
                onEffort = { onSelectMood(com.repforge.app.StudioWorkoutFeedback.efforts.indexOf(it)) },
                onNote = onUpdateNote, tag = "workout_overall_${uiState.activeWorkoutId}")
        }

        // Dedicated Workout Photos Card ("Edzésfotók")
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.glassTile(palette.surfaceCard),
                modifier = Modifier.trainPilotGlassRim(palette, RoundedCornerShape(20.dp))
                    .fillMaxWidth()
                    .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                    .testTag("workout_summary_photos_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = tr(lang, "Edzésfotók", "Workout Photos", "Trainingsfotos", "Fotografii de antrenament"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        }
                        if (attachedPhotos.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.glassTile(palette.surfaceElevated, elevated = true)
                            ) {
                                Text(
                                    text = "${attachedPhotos.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Category chips: Edzés előtt / Edzés után / Egyéb
                    Text(
                        text = tr(lang, "Kategória kiválasztása", "Select category", "Kategorie wählen", "Selectează categoria"),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val categories = listOf(
                            "before" to tr(lang, "Edzés előtt", "Before", "Vorher", "Înainte"),
                            "after" to tr(lang, "Edzés után", "After", "Nachher", "După"),
                            "other" to tr(lang, "Egyéb", "Other", "Sonstiges", "Altele")
                        )
                        categories.forEach { (catKey, catLabel) ->
                            val isSelected = selectedPhotoCategory == catKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .trainPilotClickable(RoundedCornerShape(10.dp)) { selectedPhotoCategory = catKey }
                                    .testTag("summary_photo_cat_$catKey")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = catLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) palette.onPrimary else palette.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Camera and Gallery buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    pendingPhoto = com.repforge.app.NativeWorkoutPhotos.prepare(context,
                                        uiState.activeWorkoutId, selectedPhotoCategory)
                                    photoError = false
                                    photoBusy = true
                                    summaryCameraLauncher.launch(photoRepo.prepareCameraCaptureIntent())
                                } catch (_: Exception) {
                                    pendingPhoto = null
                                    photoBusy = false
                                    photoError = true
                                }
                            },
                            enabled = !uiState.isSavingWorkout && !photoBusy,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.surfaceElevated,
                                contentColor = palette.textPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("workout_summary_camera_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(lang, "Kamera", "Camera", "Kamera", "Cameră"), fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                try {
                                    pendingPhoto = com.repforge.app.NativeWorkoutPhotos.prepare(context,
                                        uiState.activeWorkoutId, selectedPhotoCategory)
                                    photoError = false
                                    photoBusy = true
                                    summaryGalleryLauncher.launch(photoRepo.createPickImageIntent())
                                } catch (_: Exception) {
                                    pendingPhoto = null
                                    photoBusy = false
                                    photoError = true
                                }
                            },
                            enabled = !uiState.isSavingWorkout && !photoBusy,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.surfaceElevated,
                                contentColor = palette.textPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("workout_summary_gallery_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(lang, "Galéria", "Gallery", "Galerie", "Galerie"), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (photoError) {
                        Text(tr(lang, "A fotóművelet nem sikerült. Próbáld újra.",
                            "Photo operation failed. Please try again.",
                            "Fotoaktion fehlgeschlagen. Bitte erneut versuchen.",
                            "Operațiunea foto a eșuat. Încearcă din nou."),
                            color = palette.danger, modifier = Modifier.testTag("summary_photo_error"))
                    }

                    // Photos preview row or empty prompt
                    if (attachedPhotos.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(attachedPhotos, key = { it.id }) { photo ->
                                val bitmap by produceState<Bitmap?>(initialValue = null, key1 = photo.id) {
                                    value = withContext(Dispatchers.IO) {
                                        try {
                                            val file = photoRepo.photoFile(photo.id)
                                            if (file.isFile && file.length() > 0) {
                                                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                                BitmapFactory.decodeFile(file.absolutePath, bounds)
                                                val sample = maxOf(1, maxOf(bounds.outWidth / 240, bounds.outHeight / 240))
                                                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                                                BitmapFactory.decodeFile(file.absolutePath, opts)
                                            } else null
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.glassTile(palette.surfaceElevated, elevated = true),
                                    modifier = Modifier
                                        .size(width = 96.dp, height = 120.dp)
                                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap!!.asImageBitmap(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoLibrary,
                                                    contentDescription = null,
                                                    tint = palette.textMuted
                                                )
                                            }
                                        }

                                        // Category badge on bottom
                                        val catText = when (photo.label) {
                                            "before" -> tr(lang, "Előtte", "Before", "Vorher", "Înainte")
                                            "after" -> tr(lang, "Utána", "After", "Nachher", "După")
                                            else -> tr(lang, "Egyéb", "Other", "Sonstiges", "Altele")
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 8.dp),
                                            color = palette.background.copy(alpha = 0.85f),
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        ) {
                                            Text(
                                                text = catText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Delete button top right
                                        Surface(
                                            shape = CircleShape,
                                            color = com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeBackground,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(48.dp)
                                                .border(1.dp, com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeOutline, CircleShape)
                                                .trainPilotClickable(CircleShape, enabled = !uiState.isSavingWorkout && !photoBusy) {
                                                    scope.launch {
                                                        photoBusy = true
                                                        try {
                                                            val request = com.repforge.app.NativeWorkoutPhotos.prepare(
                                                                context, uiState.activeWorkoutId, selectedPhotoCategory)
                                                            showSavedPhotos(com.repforge.app.NativeWorkoutPhotos.remove(
                                                                context, request, photo.id))
                                                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                                            throw cancelled
                                                        } catch (_: Exception) {
                                                            photoError = true
                                                        } finally {
                                                            photoBusy = false
                                                        }
                                                    }
                                                }
                                                .testTag("summary_delete_photo_${photo.id}")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = tr(lang, "Törlés", "Delete", "Löschen", "Șterge"),
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = tr(
                                lang,
                                "Fotó (opcionális)", "Photo (optional)", "Foto (optional)", "Fotografie (opțional)"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textMuted
                        )
                    }
                }
            }
        }

        // Action buttons: Mentés, Szüneteltetés és kilépés, Edzés elvetése
        item {
            com.repforge.app.ui.theme.TrainPilotSaveButton(
                onClick = onSaveWorkout,
                enabled = !uiState.isSavingWorkout && !photoBusy,
                shape = RoundedCornerShape(14.dp),
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
            TrainPilotOutlinedButton(
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
            com.repforge.app.ui.theme.TrainPilotNegativeButton(
                onClick = onDiscardWorkout,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.discardWorkoutAction, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
}

@Composable
internal fun SummaryStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.glassTile(palette.surfaceCard),
        modifier = modifier.trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp)).trainPilotGlassRim(palette, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }
    }
}

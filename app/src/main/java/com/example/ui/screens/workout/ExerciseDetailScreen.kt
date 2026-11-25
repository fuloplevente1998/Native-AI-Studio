package com.example.ui.screens

import com.example.ui.theme.glassTile
import com.example.ui.theme.glassStateTile
import com.repforge.app.ui.theme.trainPilotClickable

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ExerciseCategory
import com.example.model.ExerciseItem
import com.example.ui.components.AnatomicalMuscleHeatmapCard
import com.example.ui.components.LocalOpenExerciseVideo
import com.example.ui.components.SmallExerciseVideoButton
import com.example.ui.components.SubScreenTopBar
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

@Composable
fun ExerciseDetailScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddExerciseToWorkout: (ExerciseItem) -> Unit,
    onStartDemoWithExercise: () -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    customExerciseActions: @Composable () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val openVideoModal = LocalOpenExerciseVideo.current
    val exercise = state.exercises.firstOrNull { it.id == state.selectedExerciseId }
        ?: state.exercises.firstOrNull()
        ?: ExerciseItem(
            id = "default_ex",
            nameHu = "Gyakorlat",
            nameEn = "Exercise",
            nameDe = "Übung",
            nameRo = "Exercițiu",
            category = ExerciseCategory.CHEST,
            equipmentHu = "Súlyzó",
            primaryMusclesHu = "Mellkas",
            secondaryMusclesHu = "Tricepsz",
            defaultSets = 3,
            defaultReps = 10,
            defaultWeightKg = 20f,
            defaultRestSec = 90
        )

    Column(
        modifier = Modifier
            .then(com.example.ui.components.sheetAwareFillMaxSize())
    ) {
        // Matches Mockup 1/4 #6 "Gyakorlat részletei"
        Box(Modifier.padding(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 8.dp)) {
            SubScreenTopBar(
                title = strings.detailsAction,
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }

        Column(
            modifier = Modifier.weight(1f, fill = !com.example.ui.components.LocalTrainPilotInSheet.current).testTag("exercise_detail_body").verticalScroll(rememberScrollState())
                .padding(horizontal = TrainPilotCardSpacing.ScreenGutter)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.localizedName(state.language),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallExerciseVideoButton(exerciseName = exercise.localizedName(state.language), exerciseId = exercise.id)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.glassStateTile(palette.primary, palette.primary.copy(alpha = 0.18f)),
                        modifier = Modifier.trainPilotBorder(1.dp, palette.primary, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = exercise.category.localized(state.language),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    // Compact favourite toggle in the title row (was a full-width button row).
                    val isFavorite = exercise.id in state.favoriteExerciseIds
                    val favoriteLabel = if (isFavorite)
                        trW(state.language, "Eltávolítás a kedvencekből", "Remove from favorites", "Aus Favoriten entfernen", "Elimină din favorite")
                    else
                        trW(state.language, "Hozzáadás a kedvencekhez", "Add to favorites", "Zu Favoriten hinzufügen", "Adaugă la favorite")
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .trainPilotClickable(RoundedCornerShape(12.dp), enabled = !state.libraryMutationBusy) { onToggleFavorite(exercise.id) }
                            .semantics { contentDescription = favoriteLabel }
                            .testTag("studio_exercise_favorite")
                    ) {
                        Text(
                            text = if (isFavorite) "★" else "☆",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.primary
                        )
                    }
                }
            }

            customExerciseActions()

            // Metadata table: Eszköz, Fő izmok, Másodlagos izmok
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                    .testTag("exercise_detail_metadata_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExerciseMetaRow(label = trW(state.language, "Eszköz", "Equipment", "Gerät", "Echipament"), value = exercise.localizedEquipment(state.language))
                    ExerciseMetaRow(label = trW(state.language, "Fő izmok", "Primary muscles", "Hauptmuskeln", "Mușchi principali"), value = exercise.localizedPrimaryMuscles(state.language))
                    ExerciseMetaRow(label = trW(state.language, "Másodlagos izmok", "Secondary muscles", "Sekundärmuskeln", "Mușchi secundari"), value = exercise.localizedSecondaryMuscles(state.language))
                }
            }

            // Dual Front & Back Anatomical Muscle Map highlighting the target & secondary muscles of this exercise
            val mappedMuscleId = when (exercise.category) {
                ExerciseCategory.LEGS -> "legs"
                ExerciseCategory.CHEST -> "chest"
                ExerciseCategory.BACK -> "back"
                ExerciseCategory.SHOULDERS -> "shoulders"
                ExerciseCategory.ARMS -> "arms"
                ExerciseCategory.CORE -> "core"
                ExerciseCategory.ALL -> "legs"
            }
            val secondaryLower = exercise.secondaryMusclesHu.lowercase()
            val secondaryMuscleIds = buildSet {
                if (secondaryLower.contains("mell")) add("chest")
                if (secondaryLower.contains("hát") || secondaryLower.contains("trapéz") || secondaryLower.contains("rombusz")) add("back")
                if (secondaryLower.contains("váll") || secondaryLower.contains("delta")) add("shoulders")
                if (secondaryLower.contains("láb") || secondaryLower.contains("comb") || secondaryLower.contains("far") || secondaryLower.contains("vádli") || secondaryLower.contains("hajlító")) add("legs")
                if (secondaryLower.contains("tricepsz") || secondaryLower.contains("bicepsz") || secondaryLower.contains("alkar") || secondaryLower.contains("kar")) add("arms")
                if (secondaryLower.contains("törzs") || secondaryLower.contains("has") || secondaryLower.contains("core") || secondaryLower.contains("gerinc")) add("core")
                remove(mappedMuscleId)
            }
            val defaultSets = exercise.defaultSets.coerceAtLeast(1)
            val secondarySets = (defaultSets / 2).coerceAtLeast(1)
            val exerciseMuscleLoads = remember(exercise.id, mappedMuscleId, secondaryMuscleIds, defaultSets) {
                listOf(
                    "chest" to "Mell",
                    "back" to "Hát",
                    "shoulders" to "Váll",
                    "legs" to "Láb",
                    "arms" to "Kar",
                    "core" to "Törzs"
                ).map { (id, labelHu) ->
                    when {
                        id == mappedMuscleId -> com.example.ui.components.MuscleLoadItem(
                            id = id,
                            nameHu = labelHu,
                            setsText = "$defaultSets sorozat",
                            percentage = 100,
                            ratio = 1f,
                            levelLabelHu = "Fő célizom",
                            topExercisesHu = listOf(exercise.nameHu to "$defaultSets sorozat"),
                            exerciseIdToOpen = exercise.id,
                            setsCount = defaultSets
                        )
                        id in secondaryMuscleIds -> com.example.ui.components.MuscleLoadItem(
                            id = id,
                            nameHu = labelHu,
                            setsText = "$secondarySets sorozat",
                            percentage = 50,
                            ratio = 0.5f,
                            levelLabelHu = "Másodlagos izom",
                            topExercisesHu = listOf(exercise.nameHu to "$secondarySets sorozat"),
                            exerciseIdToOpen = exercise.id,
                            setsCount = secondarySets
                        )
                        else -> com.example.ui.components.MuscleLoadItem(
                            id = id,
                            nameHu = labelHu,
                            setsText = "0 sorozat",
                            percentage = 0,
                            ratio = 0f,
                            levelLabelHu = "Nincs terhelés",
                            topExercisesHu = emptyList(),
                            exerciseIdToOpen = "",
                            setsCount = 0
                        )
                    }
                }
            }
            AnatomicalMuscleHeatmapCard(
                title = "${strings.muscleLoadTitle} (${exercise.localizedName(state.language)})",
                subtitle = "${exercise.localizedPrimaryMuscles(state.language)} • ${exercise.localizedSecondaryMuscles(state.language)}",
                highlightedMuscleId = mappedMuscleId,
                realMuscleLoads = exerciseMuscleLoads,
                periodDays = null
            )

            Text(
                text = strings.executionTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.textPrimary
            )

            val steps = exercise.localizedSteps(state.language)
            steps.forEachIndexed { idx, stepText ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = palette.surfaceElevated,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.primary
                            )
                        }
                    }
                    Text(
                        text = stepText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrainPilotOutlinedButton(
                    onClick = { onAddExerciseToWorkout(exercise) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = palette.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.addToWorkoutAction, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onStartDemoWithExercise() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("exercise_detail_start_now_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(trW(state.language, "Edzés indítása ezzel", "Start workout with this", "Training damit starten", "Începe antrenamentul cu acesta"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun ExerciseMetaRow(label: String, value: String) {
    val palette = LocalTrainPilotPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = palette.textPrimary,
            modifier = Modifier.weight(0.58f)
        )
    }
}

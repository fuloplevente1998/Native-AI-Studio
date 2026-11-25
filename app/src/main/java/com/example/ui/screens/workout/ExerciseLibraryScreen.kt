package com.example.ui.screens

import com.example.ui.theme.trainPilotGlassRim

import com.example.ui.theme.glassStateTile
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassTile
import com.example.ui.theme.glassCardFill
import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotCombinedClickable

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.IconToggleButton
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import com.example.ui.components.TrainPilotDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExerciseCategory
import com.example.model.ExerciseItem
import com.example.ui.components.LocalOpenExerciseVideo
import com.example.ui.components.SubScreenTopBar
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseLibraryScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (ExerciseCategory) -> Unit,
    onSelectExercise: (String) -> Unit,
    onOpenAddCustomExercise: () -> Unit = {},
    onStartExerciseNow: (ExerciseItem) -> Unit = {},
    onAddExerciseToQueue: (ExerciseItem) -> Unit = {},
    onStartQueuedWorkout: () -> Unit = {},
    onToggleFavoritesOnly: () -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onRemoveExerciseFromQueue: (String) -> Unit = {},
    /** Inside the Quick workout sheet (2772): no own title bar, the sheet has the header and tabs. */
    embedded: Boolean = false,
    /** Timed tab of the Quick workout sheet: only time-based exercises. */
    timedOnly: Boolean = false,
    onDeleteCustomExercise: ((String) -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val startLabel = if (state.exerciseSwapMode) trW(state.language, "Csere", "Swap", "Tauschen", "Schimbă") else strings.startAction
    val pool = quickWorkoutExercisePool(state.exercises, timedOnly)
    var ownOnly by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ExerciseItem?>(null) }
    pendingDelete?.let { exercise ->
        com.repforge.app.ui.programs.CustomExerciseDeleteConfirmation(
            language = state.language.code, name = exercise.localizedName(state.language),
            saving = state.libraryMutationBusy, error = null,
            onConfirm = { pendingDelete = null; onDeleteCustomExercise?.invoke(exercise.id) },
            onCancel = { pendingDelete = null }
        )
    }
    val filtered = pool.filter { ex ->
        val matchesCategory = state.selectedExerciseCategory == ExerciseCategory.ALL || ex.category == state.selectedExerciseCategory
        val matchesSearch = state.exerciseSearchQuery.isBlank() ||
            ex.nameHu.contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.nameEn.contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.nameDe.contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.nameRo.contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.localizedPrimaryMuscles(state.language).contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.localizedEquipment(state.language).contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.primaryMusclesHu.contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.equipmentHu.contains(state.exerciseSearchQuery, ignoreCase = true)
        matchesCategory && matchesSearch && (!ownOnly || ex.isCustom) &&
            (!state.favoriteExercisesOnly || ex.id in state.favoriteExerciseIds)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = TrainPilotCardSpacing.ScreenGutter, end = TrainPilotCardSpacing.ScreenGutter, top = 4.dp, bottom = 24.dp)
    ) {
        // Matches Mockup 1/4 #5 "Gyakorlatok" (106-os gyakorlat-adatbázis kategóriák szerint)
        if (!embedded) SubScreenTopBar(
            title = "${if (state.exerciseSwapMode) strings.exercisePickerTitle else strings.exerciseLibraryTitle} (${state.exercises.size})",
            onBack = onBack,
            onOpenSettings = onOpenSettings
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CompactJournalSearchField(state.exerciseSearchQuery, onSearchQueryChange,
                trW(state.language, "Gyakorlatok keresése…", "Search exercises…", "Übungen suchen…", "Caută exerciții…"))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                com.example.ui.components.TrainPilotFilterChip(selected = state.favoriteExercisesOnly, onClick = onToggleFavoritesOnly,
                    label = { Text(trW(state.language, "Kedvencek (${state.favoriteExerciseIds.size})", "Favorites (${state.favoriteExerciseIds.size})", "Favoriten (${state.favoriteExerciseIds.size})", "Favorite (${state.favoriteExerciseIds.size})")) },
                    modifier = Modifier.testTag("studio_library_favorites_only"))
                com.example.ui.components.TrainPilotFilterChip(selected = false, onClick = onOpenAddCustomExercise,
                    label = { Text(trW(state.language, "Új gyakorlat", "New exercise", "Neue Übung", "Exercițiu nou")) },
                    modifier = Modifier.testTag("exercise_library_add_custom_button"))
                com.example.ui.components.TrainPilotFilterChip(selected = ownOnly, onClick = { ownOnly = !ownOnly },
                    label = { Text(trW(state.language, "Saját", "Custom", "Eigene", "Proprii")) },
                    modifier = Modifier.testTag("exercise_library_custom_only"))
            }

            // Compact Horizontal Category Chips with exact counts: Összes (106) | Láb | Mell | Hát | Váll | Kar | Core
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExerciseCategory.entries.filter { !timedOnly || it == ExerciseCategory.ALL || pool.any { e -> e.category == it } }.forEach { cat ->
                    val selected = state.selectedExerciseCategory == cat
                    val catCount = if (cat == ExerciseCategory.ALL) {
                        pool.size
                    } else {
                        pool.count { it.category == cat }
                    }
                    val shape = RoundedCornerShape(14.dp)
                    Box(Modifier.heightIn(min = 48.dp).trainPilotClickable(shape) { onSelectCategory(cat) },
                        contentAlignment = Alignment.Center) {
                        Surface(
                            shape = shape,
                            color = if (selected) palette.primary else palette.surface,
                            modifier = Modifier.heightIn(min = 40.dp).trainPilotBorder(
                                1.dp, if (selected) palette.primary else palette.cardBorder, shape)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${cat.localized(state.language)} ($catCount)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) palette.onPrimary else palette.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trW(state.language, "${filtered.size} / ${pool.size} gyakorlat", "${filtered.size} / ${pool.size} exercises", "${filtered.size} / ${pool.size} Übungen", "${filtered.size} / ${pool.size} exerciții"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textSecondary
                )

            }

            if (state.quickWorkoutQueue.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.glassStateTile(palette.primary, palette.primary.copy(alpha = 0.14f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.5.dp, palette.primary, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = trW(state.language, "Edzésre összeállítva (${state.quickWorkoutQueue.size} gyakorlat)", "Queued for workout (${state.quickWorkoutQueue.size} exercises)", "Für Training zusammengestellt (${state.quickWorkoutQueue.size} Übungen)", "Pregătit pentru antrenament (${state.quickWorkoutQueue.size} exerciții)"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                            Text(
                                text = state.quickWorkoutQueue.joinToString(", ") { it.localizedName(state.language) },
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onStartQueuedWorkout,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("exercise_library_start_queued_workout_button")
                        ) {
                            Text("▶ ${strings.startAction}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            filtered.forEach { ex ->
                val inQueue = state.quickWorkoutQueue.any { it.id == ex.id }
                val favorite = ex.id in state.favoriteExerciseIds
                val name = ex.localizedName(state.language)
                val metadata = listOf(ex.category.localized(state.language), ex.localizedPrimaryMuscles(state.language),
                    ex.localizedEquipment(state.language)).map { it.trim() }.filter { it.isNotBlank() }
                    .distinctBy { it.lowercase(java.util.Locale.ROOT) }.joinToString(" • ")
                val openVideo = LocalOpenExerciseVideo.current
                var rowMenuOpen by remember(ex.id) { mutableStateOf(false) }
                val nameStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
                val measurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val longestWord = remember(name, nameStyle, density) {
                    name.split(Regex("\\s+")).maxOfOrNull { measurer.measure(it, style = nameStyle).size.width } ?: 0
                }
                Surface(
                    shape = RoundedCornerShape(14.dp), color = palette.glassTile(palette.surface),
                    modifier = Modifier.trainPilotGlassRim(palette, RoundedCornerShape(14.dp)).fillMaxWidth()
                        .trainPilotBorder(if (inQueue) 1.5.dp else 1.dp,
                            if (inQueue) palette.primary else palette.cardBorder, RoundedCornerShape(14.dp))
                        .trainPilotCombinedClickable(
                            shape = RoundedCornerShape(14.dp),
                            onLongClickLabel = trW(state.language, "Műveletek: $name", "Actions: $name", "Aktionen: $name", "Acțiuni: $name"),
                            onLongClick = { rowMenuOpen = true },
                            onClick = { onSelectExercise(ex.id) }
                        )
                        .testTag("exercise_library_row_${ex.id}")
                ) {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val inline = with(density) { (maxWidth - 48.dp - 144.dp - 8.dp).toPx() >= longestWord }
                        val favoriteControl: @Composable () -> Unit = {
                            IconToggleButton(checked = favorite, onCheckedChange = { onToggleFavorite(ex.id) },
                                enabled = !state.libraryMutationBusy,
                                modifier = Modifier.size(48.dp).testTag("exercise_library_favorite_${ex.id}")) {
                                Icon(if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = if (favorite) trW(state.language, "Eltávolítás a kedvencekből", "Remove from favorites", "Aus Favoriten entfernen", "Elimină din favorite")
                                    else trW(state.language, "Hozzáadás a kedvencekhez", "Add to favorites", "Zu Favoriten hinzufügen", "Adaugă la favorite"),
                                    tint = if (favorite) palette.primary else palette.textSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                        val textContent: @Composable () -> Unit = {
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Text(name, style = nameStyle, color = palette.textPrimary,
                                    modifier = Modifier.fillMaxWidth().testTag("exercise_library_name_${ex.id}"))
                                Text(metadata,
                                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                                    color = palette.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth().testTag("exercise_library_meta_${ex.id}"))
                            }
                        }
                        // Approved option B: one primary "Start" action + a more button; video and
                        // add/remove live in the menu (also on long press). Row tap = details.
                        val actions: @Composable () -> Unit = {
                            Row(Modifier.padding(end = 2.dp).testTag("exercise_library_actions_${ex.id}"),
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)
                                        .trainPilotClickable(RoundedCornerShape(10.dp)) { onStartExerciseNow(ex) }
                                        .semantics { contentDescription = "$startLabel: $name" }
                                        .testTag("exercise_library_start_now_${ex.id}")
                                ) {
                                    Row(
                                        Modifier.clip(RoundedCornerShape(10.dp)).background(palette.primary)
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(if (state.exerciseSwapMode) Icons.Default.SwapHoriz else Icons.Default.PlayArrow, contentDescription = null, tint = palette.onPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text(startLabel, style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold, color = palette.onPrimary, maxLines = 1)
                                    }
                                }
                                Box {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(48.dp)
                                            .trainPilotClickable(RoundedCornerShape(10.dp)) { rowMenuOpen = true }
                                            .semantics { contentDescription = trW(state.language, "További műveletek: $name", "More actions: $name", "Weitere Aktionen: $name", "Mai multe acțiuni: $name") }
                                            .testTag("exercise_library_more_${ex.id}")
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = palette.textSecondary)
                                    }
                                    DropdownMenu(
                                        expanded = rowMenuOpen,
                                        onDismissRequest = { rowMenuOpen = false },
                                        containerColor = palette.surfaceCard,
                                        shape = RoundedCornerShape(14.dp),
                                        border = com.repforge.app.ui.theme.trainPilotFrameStroke(androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorder))
                                    ) {
                                        val videoLabel = "${strings.videoLabel}: $name"
                                        DropdownMenuItem(
                                            text = { Text(strings.videoLabel, color = palette.textPrimary) },
                                            leadingIcon = { Icon(Icons.Default.OndemandVideo, contentDescription = null, tint = Color(0xFFE53935)) },
                                            onClick = { rowMenuOpen = false; openVideo(name, ex.id) },
                                            modifier = Modifier.semantics { contentDescription = videoLabel }
                                                .testTag("exercise_video_btn_${name.hashCode()}")
                                        )
                                        val queueLabel = if (inQueue) trW(state.language, "Visszavonás az összeállításból", "Remove from workout selection", "Aus Trainingsauswahl entfernen", "Elimină din selecția antrenamentului")
                                            else trW(state.language, "Hozzáadás az edzéshez", "Add to workout", "Zum Training hinzufügen", "Adaugă la antrenament")
                                        DropdownMenuItem(
                                            text = { Text(queueLabel, color = palette.textPrimary) },
                                            leadingIcon = { Icon(if (inQueue) Icons.Default.Check else Icons.Default.Add, contentDescription = null,
                                                tint = if (inQueue) Color(0xFFE53935) else palette.primary) },
                                            enabled = !state.libraryMutationBusy,
                                            onClick = {
                                                rowMenuOpen = false
                                                if (inQueue) onRemoveExerciseFromQueue(ex.id) else onAddExerciseToQueue(ex)
                                            },
                                            modifier = Modifier.semantics { contentDescription = queueLabel }
                                                .testTag("exercise_library_add_to_queue_${ex.id}")
                                        )
                                        if (ex.isCustom && onDeleteCustomExercise != null) DropdownMenuItem(
                                            text = { Text(strings.deleteAction, color = palette.danger) },
                                            leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = palette.danger) },
                                            enabled = !state.libraryMutationBusy,
                                            onClick = { rowMenuOpen = false; pendingDelete = ex },
                                            modifier = Modifier.testTag("exercise_library_delete_${ex.id}")
                                        )
                                    }
                                }
                            }
                        }
                        if (inline) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                favoriteControl()
                                Box(Modifier.weight(1f).padding(end = 4.dp)) { textContent() }
                                actions()
                            }
                        } else {
                            Column(Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    favoriteControl()
                                    Box(Modifier.weight(1f).padding(end = 8.dp)) { textContent() }
                                }
                                Box(Modifier.fillMaxWidth().padding(bottom = 4.dp), contentAlignment = Alignment.CenterEnd) { actions() }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExerciseLibraryAction(
    tag: String,
    description: String,
    color: Color,
    iconColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(48.dp).background(color)
            .trainPilotClickable(androidx.compose.ui.graphics.RectangleShape, onClick = onClick)
            .semantics { contentDescription = description }.testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
    }
}

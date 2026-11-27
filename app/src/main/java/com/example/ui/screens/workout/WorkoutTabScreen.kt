package com.example.ui.screens
import com.example.ui.theme.glassCardFill
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.liquidDropMaterial
import com.example.ui.components.WorkoutModeArt
import com.example.ui.components.QuickWorkoutMode
import com.example.ui.components.PageLayoutHorizontalRow
import com.example.ui.components.PageLayoutCard
import androidx.compose.runtime.key
import com.example.ui.components.rememberPageLayout
import com.example.ui.components.PageLayoutTile
import com.example.ui.components.PageLayoutFooter
import com.example.ui.components.pageFooterArrangement
import com.example.ui.components.PageLayoutFrame
import com.example.ui.components.PageLayoutBlock

import com.repforge.app.ui.theme.trainPilotClickable

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotSoftGlow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.repforge.app.ui.theme.TrainPilotButton as Button
import com.repforge.app.ui.theme.TrainPilotGlassPrimaryButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExerciseItem
import com.example.model.AppLanguage
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

@Composable
internal fun WorkoutModeTile(
    title: String,
    subtitle: String,
    badge: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeColor: Color? = null,
    graphic: @Composable () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val shape = RoundedCornerShape(18.dp)
    val tint = badgeColor ?: palette.primary
    Surface(
        shape = shape,
        color = palette.glassCardFill(palette.surface),
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = 124.dp)
            .trainPilotSoftGlow(palette.primary, shape, 4.dp)
            .trainPilotClickable(shape, stretchOnDrag = true) { onClick() }
            .liquidDropMaterial(palette, shape, GlassMaterialRole.Button, live = false)
            .trainPilotBorder(1.25.dp, palette.sectionBorder, shape)
            .testTag(testTag)
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // 2776: the arrow sits next to the title so subtitle and badge can use the full width (no "14…").
            // 2784: fixed title row height, so a smaller fitted font never moves the graphics below.
            Row(Modifier.fillMaxWidth().heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                com.example.ui.components.TrainPilotFitText(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = palette.textPrimary, modifier = Modifier.weight(1f).testTag("${testTag}_title"))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.textSecondary, modifier = Modifier.size(18.dp))
            }
            // 303: reuse the app-wide adaptive text component instead of forcing multiple lines.
            // It shrinks the font to fit the tile and only wraps as a last resort.
            com.example.ui.components.TrainPilotFitText(subtitle, style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary, minSize = 9.sp,
                modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp).testTag("${testTag}_subtitle"))
            Box(Modifier.fillMaxWidth().weight(1f).testTag("${testTag}_graphic"), contentAlignment = Alignment.TopCenter) { graphic() }
            Surface(shape = RoundedCornerShape(8.dp), color = tint.copy(alpha = 0.16f)) {
                com.example.ui.components.TrainPilotFitText(badge, style = MaterialTheme.typography.labelSmall,
                    color = tint, fontWeight = FontWeight.Bold, minSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp).testTag("${testTag}_badge"))
            }
        }
    }
}

internal fun quickWorkoutExercisePool(exercises: List<ExerciseItem>, timedOnly: Boolean): List<ExerciseItem> =
    if (timedOnly) exercises.filter { it.isTimed || it.measurementType == "time" } else exercises

internal fun quickWorkoutExerciseCount(language: AppLanguage, count: Int): String {
    val number = java.text.NumberFormat.getIntegerInstance(com.example.localization.StudioLocale.forLanguage(language)).format(count)
    return when (language) {
        AppLanguage.HU -> "$number gyakorlat"
        AppLanguage.EN -> if (count == 1) "$number exercise" else "$number exercises"
        AppLanguage.DE -> if (count == 1) "$number Übung" else "$number Übungen"
        AppLanguage.RO -> when {
            count == 1 -> "$number exercițiu"
            count % 100 in 20..99 || count > 0 && count % 100 == 0 -> "$number de exerciții"
            else -> "$number exerciții"
        }
    }
}

@Composable
fun WorkoutTabScreen(
    state: TrainPilotUiState,
    onOpenSettings: () -> Unit,
    onOpenQuickWorkout: () -> Unit,
    onOpenExerciseLibrary: () -> Unit,
    onOpenOutdoorWorkout: () -> Unit,
    onOpenProgramsList: () -> Unit,
    onOpenTimed: () -> Unit = onOpenExerciseLibrary,
    /** 2773: browse / manage the exercise library (sheet) without starting anything. */
    onOpenExerciseCatalog: () -> Unit = {},
    onOpenProgramEditor: (String, Int) -> Unit,
    onStartWorkoutDay: (Int) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenCoach: () -> Unit,
    onOpenProgressionModal: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val activeProgram = state.programs.firstOrNull { it.isActive } ?: state.programs.firstOrNull()

    val scrollState = rememberScrollState()
    val blocks = listOf(
        PageLayoutBlock("quick", strings.quickWorkoutTitle),
        PageLayoutBlock("modes", trW(state.language, "Ismétléses, időzített és kültéri", "Reps, timed and outdoor", "Wiederholungen, Zeit und Outdoor", "Repetări, timp și exterior")),
        PageLayoutBlock("library", trW(state.language, "Gyakorlatkönyvtár", "Exercise library", "Übungsbibliothek", "Bibliotecă de exerciții")),
        PageLayoutBlock("program", strings.currentProgramTitle),
        PageLayoutBlock("settings", trW(state.language, "Edzés beállításai", "Workout settings", "Trainingseinstellungen", "Setări antrenament"))
    ).filter { it.id != "settings" || activeProgram != null }
    val layout = rememberPageLayout("workout", blocks.map { it.id })
    PageLayoutFrame(layout, state.language, blocks, scrollState, TrainPilotCardSpacing.CardGap) {
    // 2779: no "more below" arrow; the tab fits one screen. Scrolling stays only as a fallback for
    // very small screens / large font scale, so nothing becomes unreachable.
    Box(modifier = Modifier.fillMaxSize().testTag("workout_tab_root")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = TrainPilotCardSpacing.ScreenGutter)
                .padding(top = 8.dp + com.example.ui.theme.LocalGlassTopInset.current, bottom = 12.dp),
            verticalArrangement = remember(layout.draft == null) { pageFooterArrangement(TrainPilotCardSpacing.CardGap, layout.draft == null) }
        ) {
        layout.order.forEach { id ->
            key(id) {
                PageLayoutTile(layout, id, hasHorizontalTiles = id == "modes" || id == "program") {
                    when (id) {
                        "quick" -> {
                            // 1. Gyors edzés Card (Mockup 1/4 #2)
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = palette.glassCardFill(palette.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .trainPilotClickable(RoundedCornerShape(18.dp), stretchOnDrag = true) { onOpenQuickWorkout() }
                                    .liquidDropMaterial(palette, RoundedCornerShape(18.dp), GlassMaterialRole.Button, live = false)
                                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                                    .testTag("workout_tab_quick_card")
                            ) {
                                // 2779: flatter row (48 dp touch target kept) so the whole tab fits without scrolling.
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.quickWorkoutTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = palette.textPrimary
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
                        "modes" -> {
                            // 2. Three tall tiles (2772): Strength / Timed / Outdoor, title on top, big icon below.
                            //    Each opens the Quick workout sheet on its own tab (no separate page).
                            val timedCount = quickWorkoutExercisePool(state.exercises, timedOnly = true).size
                            val modes = listOf(
                                PageLayoutBlock("reps", trW(state.language, "Ismétléses", "Reps", "Wiederholungen", "Repetări")),
                                PageLayoutBlock("timed", trW(state.language, "Időzített", "Timed", "Zeit", "Cronometrat")),
                                PageLayoutBlock("outdoor", trW(state.language, "Kültéri", "Outdoor", "Outdoor", "Exterior"))
                            )
                            PageLayoutHorizontalRow(layout, "workout_modes", modes, 8.dp,
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) { tile, modifier ->
                                when (tile) {
                                    "reps" -> WorkoutModeTile(
                                    title = trW(state.language, "Ismétléses", "Reps", "Wiederholungen", "Repetări"),
                                    subtitle = trW(state.language, "súly × ismétlés", "weight × reps", "Gewicht × Wdh.", "greutate × rep."),
                                    badge = quickWorkoutExerciseCount(state.language, quickWorkoutExercisePool(state.exercises, timedOnly = false).size),
                                    testTag = "workout_tab_exercise_picker",
                                    onClick = onOpenExerciseLibrary,
                                    modifier = modifier
                                ) { WorkoutModeArt(QuickWorkoutMode.REPS, height = 52.dp) }
                                    "timed" -> WorkoutModeTile(
                                    title = trW(state.language, "Időzített", "Timed", "Zeit", "Cronometrat"),
                                    subtitle = trW(state.language, "idő / tartás", "time / hold", "Zeit / Halten", "timp / menținere"),
                                    badge = quickWorkoutExerciseCount(state.language, timedCount),
                                    testTag = "workout_tab_timed_card",
                                    onClick = onOpenTimed,
                                    modifier = modifier
                                ) { WorkoutModeArt(QuickWorkoutMode.TIMED, height = 52.dp) }
                                    "outdoor" -> WorkoutModeTile(
                                    title = trW(state.language, "Kültéri", "Outdoor", "Outdoor", "Exterior"),
                                    subtitle = trW(state.language, "futás, séta", "run, walk", "Laufen, Gehen", "alergare, mers"),
                                    badge = trW(state.language, "GPS élő", "GPS live", "GPS live", "GPS live"),
                                    badgeColor = palette.success,
                                    testTag = "workout_tab_outdoor_card",
                                    onClick = onOpenOutdoorWorkout,
                                    modifier = modifier
                                ) { WorkoutModeArt(QuickWorkoutMode.OUTDOOR, height = 52.dp) }
                                }
                            }
                        }
                        "library" -> {
                            // 2773 (A): exercise library entry right under the three tiles.
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = palette.glassCardFill(palette.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .trainPilotClickable(RoundedCornerShape(18.dp), stretchOnDrag = true) { onOpenExerciseCatalog() }
                                    .liquidDropMaterial(palette, RoundedCornerShape(18.dp), GlassMaterialRole.Button, live = false)
                                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                                    .testTag("workout_tab_exercise_catalog")
                            ) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.FormatListBulleted, null, tint = palette.primary, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(trW(state.language, "Gyakorlatkönyvtár", "Exercise library", "Übungsbibliothek", "Bibliotecă de exerciții"),
                                            style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                                        Text(trW(state.language, "${state.exercises.size} gyakorlat",
                                            "${state.exercises.size} exercises",
                                            "${state.exercises.size} Übungen",
                                            "${state.exercises.size} exerciții"),
                                            style = MaterialTheme.typography.bodySmall, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.textSecondary)
                                }
                            }
                        }
                        "program" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)) {
                            // 3. Aktuális program Header + Összes megtekintése >
                            PageLayoutCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.currentProgramTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .trainPilotClickable(RoundedCornerShape(8.dp)) { onOpenProgramsList() }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.viewAllAction,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            }
                            if (activeProgram != null) {
                                // 2779: every day of the program, equal height, each with its next planned date.
                                WorkoutTabDayCards(
                                    program = activeProgram,
                                    events = state.calendarEvents,
                                    target = state.homeWorkoutTarget,
                                    language = state.language,
                                    onOpenProgramEditor = onOpenProgramEditor,
                                    onStartWorkoutDay = onStartWorkoutDay,
                                    layout = layout
                                )

                            } else {
                                PageLayoutCard {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = palette.glassCardFill(palette.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .trainPilotGlassMaterial(palette, RoundedCornerShape(18.dp))
                                        .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                                        .padding(20.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = trW(state.language, "Nincs aktív program", "No active program", "Kein aktives Programm", "Niciun program activ"),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = palette.textPrimary
                                        )
                                        Text(
                                            text = trW(
                                                state.language,
                                                "Válassz egy edzésprogramot a programlistából vagy hozz létre egy újat.",
                                                "Select a workout program from the list or create a new one.",
                                                "Wähle ein Trainingsprogramm aus der Liste oder erstelle ein neues.",
                                                "Selectează un program din listă sau creează unul nou."
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.textSecondary,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        TrainPilotGlassPrimaryButton(
                                            onClick = onOpenProgramsList,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = palette.primary,
                                                contentColor = palette.onPrimary
                                            )
                                        ) {
                                            Text(strings.programsShortcutTitle)
                                        }
                                    }
                                }
                                }
                            }
                            }
                        }
                        "settings" -> {
                            if (activeProgram != null) {
                                // Edzés beállításai (from main Edzés screen)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = palette.glassCardFill(palette.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .trainPilotClickable(RoundedCornerShape(16.dp), stretchOnDrag = true) { onOpenProgressionModal() }
                                        .liquidDropMaterial(palette, RoundedCornerShape(16.dp), GlassMaterialRole.Button, live = false)
                                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "⚙ ${trW(state.language, "Edzés beállításai", "Workout settings", "Trainingseinstellungen", "Setări antrenament")} • ${activeProgram.localizedName(state.language)}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = palette.textPrimary
                                            )
                                            val localizedStyle = when {
                                                state.trainingStyleHu.contains("Erő-alap", ignoreCase = true) -> trW(state.language, state.trainingStyleHu, "Strength-focused (lower reps, higher weight)", "Kraftfokus (weniger Wdh., höheres Gewicht)", "Focus pe forță (repetări mai puține, greutate mai mare)")
                                                state.trainingStyleHu.contains("Állóképesség", ignoreCase = true) -> trW(state.language, state.trainingStyleHu, "Endurance / Toning (12–15+ reps)", "Ausdauer / Definition (12–15+ Wdh.)", "Anduranță / Tonifiere (12–15+ repetări)")
                                                else -> trW(state.language, state.trainingStyleHu, "Balanced (strength + muscle, 8–12 reps)", "Ausgewogen (Kraft + Muskel, 8–12 Wdh.)", "Echilibrat (forță + masă, 8–12 repetări)")
                                            }
                                            val localizedRule = when {
                                                state.progressionRuleHu.contains("Óvatos", ignoreCase = true) -> trW(state.language, state.progressionRuleHu, "Conservative progression (first +reps, then weight)", "Vorsichtige Progression (zuerst +Wdh., dann Gewicht)", "Progresie prudentă (întâi +repetări, apoi greutate)")
                                                state.progressionRuleHu.contains("Kézi", ignoreCase = true) -> trW(state.language, state.progressionRuleHu, "Manual control (recommendation only)", "Manuelle Steuerung (nur Empfehlung)", "Control manual (doar recomandare)")
                                                else -> trW(state.language, state.progressionRuleHu, "Standard progression (+1–2 reps or +2 kg)", "Standard-Progression (+1–2 Wdh. oder +2 kg)", "Progresie standard (+1–2 repetări sau +2 kg)")
                                            }
                                            Text(
                                                text = "$localizedStyle • $localizedRule",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.textSecondary
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
                        }
                    }
                }
            }
        }
        PageLayoutFooter(layout, state.language)
    }
}
    }
}

@Composable
internal fun ShortcutSquareCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.glassCardFill(palette.surface),
        modifier = modifier
            .heightIn(min = 48.dp)
            .trainPilotClickable(RoundedCornerShape(16.dp), stretchOnDrag = true) { onClick() }
            .liquidDropMaterial(palette, RoundedCornerShape(16.dp), GlassMaterialRole.Button, live = false)
            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.textPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }
    }
}

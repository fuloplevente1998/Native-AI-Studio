package com.example.ui.components

import com.example.ui.theme.glassCardFill
import com.example.ui.theme.glassTile
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.GlassMaterialRole

import com.example.ui.components.trainPilotAnimatedExpansion

import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.repforge.app.ui.theme.trainPilotPressIndication
import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotSoftGlow

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette

import com.example.model.AppLanguage

data class MuscleLoadItem(
    val id: String,
    val nameHu: String,
    val setsText: String,
    val percentage: Int,
    val ratio: Float,
    val levelLabelHu: String,
    val topExercisesHu: List<Pair<String, String>>, // ExerciseName to sets/load
    val exerciseIdToOpen: String,
    val setsCount: Int = 0,
    val workoutRows: List<com.repforge.app.data.db.ProgressMuscleWorkoutRow> = emptyList()
) {
    fun localizedName(lang: AppLanguage): String = when (id.lowercase()) {
        "chest" -> when (lang) {
            AppLanguage.HU -> nameHu.ifBlank { "Mell" }
            AppLanguage.EN -> "Chest"
            AppLanguage.DE -> "Brust"
            AppLanguage.RO -> "Piept"
        }
        "back" -> when (lang) {
            AppLanguage.HU -> nameHu.ifBlank { "Hát" }
            AppLanguage.EN -> "Back"
            AppLanguage.DE -> "Rücken"
            AppLanguage.RO -> "Spate"
        }
        "shoulders" -> when (lang) {
            AppLanguage.HU -> nameHu.ifBlank { "Váll" }
            AppLanguage.EN -> "Shoulders"
            AppLanguage.DE -> "Schultern"
            AppLanguage.RO -> "Umeri"
        }
        "legs" -> when (lang) {
            AppLanguage.HU -> nameHu.ifBlank { "Láb" }
            AppLanguage.EN -> "Legs"
            AppLanguage.DE -> "Beine"
            AppLanguage.RO -> "Picioare"
        }
        "arms" -> when (lang) {
            AppLanguage.HU -> nameHu.ifBlank { "Kar" }
            AppLanguage.EN -> "Arms"
            AppLanguage.DE -> "Arme"
            AppLanguage.RO -> "Brațe"
        }
        "core" -> when (lang) {
            AppLanguage.HU -> nameHu.ifBlank { "Törzs" }
            AppLanguage.EN -> "Core"
            AppLanguage.DE -> "Rumpf"
            AppLanguage.RO -> "Trunchi"
        }
        "quads", "hamstrings", "glutes", "calves", "biceps", "triceps", "forearms" ->
            if (lang == AppLanguage.HU) nameHu else com.example.model.translateSingleMuscleLabel(id, lang)
        else -> com.example.model.translateSingleMuscleLabel(nameHu, lang)
    }

    fun localizedSetsText(lang: AppLanguage): String = when (lang) {
        AppLanguage.HU -> setsText.ifBlank { "$setsCount sorozat" }
        AppLanguage.EN -> "$setsCount sets"
        AppLanguage.DE -> "$setsCount Sätze"
        AppLanguage.RO -> "$setsCount serii"
    }

    fun localizedLevelLabel(lang: AppLanguage): String {
        if (lang == AppLanguage.HU) return levelLabelHu
        return when {
            setsCount <= 0 -> when (lang) {
                AppLanguage.EN -> "No load"
                AppLanguage.DE -> "Keine Belastung"
                AppLanguage.RO -> "Fără încărcare"
                AppLanguage.HU -> levelLabelHu
            }
            percentage >= 70 -> when (lang) {
                AppLanguage.EN -> "Higher load"
                AppLanguage.DE -> "Höhere Belastung"
                AppLanguage.RO -> "Încărcare mare"
                AppLanguage.HU -> levelLabelHu
            }
            percentage >= 40 -> when (lang) {
                AppLanguage.EN -> "Moderate load"
                AppLanguage.DE -> "Mittlere Belastung"
                AppLanguage.RO -> "Încărcare medie"
                AppLanguage.HU -> levelLabelHu
            }
            else -> when (lang) {
                AppLanguage.EN -> "Lower load"
                AppLanguage.DE -> "Geringere Belastung"
                AppLanguage.RO -> "Încărcare mică"
                AppLanguage.HU -> levelLabelHu
            }
        }
    }
}

/**
 * Compact Side-by-Side Front & Back Anatomical Muscle Heatmap ("Izomcsoport-terhelés")
 * Matches the `main` app screenshot (`Screenshot_2026-10-27-15-00-06-481_com.repforge.app.jpg`):
 * - Left: Dual Front & Back Anatomical Muscular Figures
 * - Right: 6 horizontal progress bars (Mell 45%, Hát 66%, Váll 59%, Láb 75%, Kar 100%, Core 61%)
 * - Bottom: Kisebb / Közepes / Nagyobb legend + explanatory caption
 * - Tapping any muscle bar or period pill expands/updates the details inline!
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnatomicalMuscleHeatmapCard(
    title: String = "Izomcsoport-terhelés",
    subtitle: String = "A színek és arányok az időszak legterheltebb izomcsoportjához viszonyítanak, nem fáradtságot mérnek.",
    highlightedMuscleId: String? = null,
    realMuscleLoads: List<MuscleLoadItem> = emptyList(),
    onPeriodSelected: ((Int) -> Unit)? = null,
    periodDays: Int? = 90,
    onOpenWorkout: ((String) -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val lang = strings.language
    val resolvedTitle = remember(title, lang) {
        if (lang == AppLanguage.HU || title != "Izomcsoport-terhelés") title
        else when (lang) {
            AppLanguage.EN -> "Muscle Group Load"
            AppLanguage.DE -> "Muskelgruppen-Belastung"
            AppLanguage.RO -> "Încărcare grupe musculare"
            AppLanguage.HU -> title
        }
    }
    val resolvedSubtitle = remember(subtitle, lang) {
        if (lang == AppLanguage.HU) subtitle
        else when {
            subtitle == "A színek és arányok az időszak legterheltebb izomcsoportjához viszonyítanak, nem fáradtságot mérnek." -> when (lang) {
                AppLanguage.EN -> "Colors and ratios compare against the most loaded muscle group in the period; they do not measure fatigue."
                AppLanguage.DE -> "Farben und Verhältnisse beziehen sich auf die am stärksten belastete Muskelgruppe im Zeitraum, nicht auf Ermüdung."
                AppLanguage.RO -> "Culorile și proporțiile se raportează la grupa musculară cea mai încărcată din perioadă, nu măsoară oboseala."
                AppLanguage.HU -> subtitle
            }
            subtitle.startsWith("A kiválasztott edzés izomterhelése") -> when (lang) {
                AppLanguage.EN -> "Muscle load of the selected workout based on recorded sets."
                AppLanguage.DE -> "Muskelbelastung des ausgewählten Trainings basierend auf erfassten Sätzen."
                AppLanguage.RO -> "Încărcarea musculară a antrenamentului selectat pe baza seriilor înregistrate."
                AppLanguage.HU -> subtitle
            }
            else -> subtitle
        }
    }
    val periodOptions = remember(lang) {
        when (lang) {
            AppLanguage.HU -> listOf("3 hó", "30 nap", "7 nap", "1 nap")
            AppLanguage.EN -> listOf("3 mo", "30 days", "7 days", "1 day")
            AppLanguage.DE -> listOf("3 Mon.", "30 Tage", "7 Tage", "1 Tag")
            AppLanguage.RO -> listOf("3 luni", "30 zile", "7 zile", "1 zi")
        }
    }
    val periodDayOptions = remember { listOf(90, 30, 7, 1) }
    val selectedPeriodIdx = periodDayOptions.indexOf(periodDays ?: 90).coerceAtLeast(0)

    val muscleLoads = remember(realMuscleLoads) {
        if (realMuscleLoads.isNotEmpty()) {
            realMuscleLoads
        } else {
            listOf(
                MuscleLoadItem("chest", "Mell", "0 sorozat", 0, 0f, "Nincs terhelés", emptyList(), ""),
                MuscleLoadItem("back", "Hát", "0 sorozat", 0, 0f, "Nincs terhelés", emptyList(), ""),
                MuscleLoadItem("shoulders", "Váll", "0 sorozat", 0, 0f, "Nincs terhelés", emptyList(), ""),
                MuscleLoadItem("legs", "Láb", "0 sorozat", 0, 0f, "Nincs terhelés", emptyList(), ""),
                MuscleLoadItem("arms", "Kar", "0 sorozat", 0, 0f, "Nincs terhelés", emptyList(), ""),
                MuscleLoadItem("core", "Törzs", "0 sorozat", 0, 0f, "Nincs terhelés", emptyList(), "")
            )
        }
    }

    var selectedMuscleId by androidx.compose.runtime.saveable.rememberSaveable(highlightedMuscleId) {
        mutableStateOf(highlightedMuscleId ?: "all")
    }

    val selectedMuscleObj = muscleLoads.find { it.id == selectedMuscleId }
    var showWorkoutRows by remember(selectedMuscleId, periodDays) { mutableStateOf(false) }
    var showExplanation by remember { mutableStateOf(false) }

    val smallHeatColor = studioMuscleLoadColor(0f)   // Yellow-Gold (Kisebb)
    val mediumHeatColor = studioMuscleLoadColor(0.4f)  // Warm Orange (Közepes)
    val highHeatColor = studioMuscleLoadColor(0.7f)    // Coral Red (Nagyobb)
    val legendSmall = when (lang) {
        AppLanguage.HU -> "Kisebb"
        AppLanguage.EN -> "Lower"
        AppLanguage.DE -> "Geringer"
        AppLanguage.RO -> "Mică"
    }
    val legendMedium = when (lang) {
        AppLanguage.HU -> "Közepes"
        AppLanguage.EN -> "Moderate"
        AppLanguage.DE -> "Mittel"
        AppLanguage.RO -> "Medie"
    }
    val legendHigh = when (lang) {
        AppLanguage.HU -> "Nagyobb"
        AppLanguage.EN -> "Higher"
        AppLanguage.DE -> "Höher"
        AppLanguage.RO -> "Mare"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.glassCardFill(palette.surfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotGlassMaterial(palette, RoundedCornerShape(20.dp))
                .trainPilotSoftGlow(palette.primary, RoundedCornerShape(20.dp), 3.dp)
            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            .trainPilotAnimatedExpansion()
            .testTag("anatomical_muscle_heatmap_card")
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            // Compact header: 41dp visual period pill, but a 48dp tap target.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = resolvedTitle,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                if (onPeriodSelected != null && periodDays != null) {
                    val periodInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier.size(48.dp)
                            .testTag("muscle_heatmap_period_pill")
                            .clickable(interactionSource = periodInteraction, indication = null) {
                                onPeriodSelected(periodDayOptions[(selectedPeriodIdx + 1) % periodOptions.size])
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.glassTile(palette.surfaceElevated, elevated = true),
                            modifier = Modifier.size(41.dp)
                                .trainPilotGlassRim(palette, RoundedCornerShape(10.dp), GlassMaterialRole.Button)
                                .trainPilotPressIndication(RoundedCornerShape(10.dp), periodInteraction)
                                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                                .testTag("muscle_heatmap_period_visual")
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = periodOptions[selectedPeriodIdx],
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // No extra header-to-chart gap: preserve all 48dp period-control hit area.

            // Side-by-Side Row: Left = Front & Back Anatomical Figures, Right = 6 Muscle Progress Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Left half: Dual Front & Back Anatomical Muscular Figures
                Box(
                    modifier = Modifier
                        .weight(0.46f)
                        .height(114.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val nativeLoads = remember(muscleLoads, lang, selectedMuscleId) {
                        muscleLoads.groupBy { com.repforge.app.ui.progress.NativeProgressQuery.muscleGroup(it.id) }
                            .map { (region, items) ->
                                val shown = items.firstOrNull { it.id == selectedMuscleId } ?: items.maxBy { it.ratio }
                                com.repforge.app.ui.progress.MuscleLoadUiModel(region, shown.localizedName(lang),
                                    shown.setsCount, shown.ratio)
                            }
                    }
                    com.repforge.app.ui.progress.NativeMuscleLoadVisual(
                        loads = nativeLoads,
                        language = lang.code.lowercase(),
                        modifier = Modifier.fillMaxSize(),
                        highlight = com.repforge.app.ui.progress.NativeProgressQuery.muscleGroup(selectedMuscleId),
                        loadColor = ::studioMuscleLoadColor
                    )
                }

                // Right half: 6 Compact Muscle Progress Bars (Mell, Hát, Váll, Láb, Kar, Core)
                Column(
                    modifier = Modifier.weight(0.54f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    muscleLoads.forEach { m ->
                        val isSel = selectedMuscleId == m.id
                        val (levelColor, level) = when {
                            m.percentage >= 70 -> highHeatColor to legendHigh
                            m.percentage >= 40 -> mediumHeatColor to legendMedium
                            else -> smallHeatColor to legendSmall
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSel) palette.primary.copy(alpha = 0.13f) else Color.Transparent, RoundedCornerShape(6.dp))
                                .trainPilotClickable(RoundedCornerShape(6.dp)) {
                                    selectedMuscleId = if (selectedMuscleId == m.id) "all" else m.id
                                }
                                .trainPilotChartSelection(isSel, "${m.localizedName(lang)} · ${m.percentage}% · $level")
                                .padding(vertical = 1.dp)
                                .testTag("muscle_chip_${m.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = m.localizedName(lang),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) palette.primary else palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(64.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.surfaceElevated)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(m.ratio)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(levelColor)
                                )
                            }
                            Box(Modifier.size(5.dp).background(levelColor, CircleShape))
                            Text(
                                text = "${m.percentage}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) palette.primary else palette.textSecondary,
                                modifier = Modifier.width(34.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Legend Row: 🟡 Kisebb   🟠 Közepes   🔴 Nagyobb
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = smallHeatColor, label = legendSmall)
                Spacer(modifier = Modifier.width(14.dp))
                LegendDot(color = mediumHeatColor, label = legendMedium)
                Spacer(modifier = Modifier.width(14.dp))
                LegendDot(color = highHeatColor, label = legendHigh)
            }

            Spacer(modifier = Modifier.height(2.dp))

            val explanationToggleLabel = when (lang) {
                AppLanguage.HU -> "Relatív sorozatterhelés, nem fáradtság · Részletek"
                AppLanguage.EN -> "Relative set volume, not fatigue · Details"
                AppLanguage.DE -> "Relatives Satzvolumen, keine Ermüdung · Details"
                AppLanguage.RO -> "Volum relativ de serii, nu oboseală · Detalii"
            }

            // Keep the safety caveat visible; put the longer explanation behind a tap.
            Text(
                text = explanationToggleLabel,
                modifier = Modifier.trainPilotClickable(RoundedCornerShape(8.dp)) { showExplanation = !showExplanation }
                    .testTag("muscle_explanation_toggle"),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showExplanation && resolvedSubtitle.isNotBlank()) Text(
                text = resolvedSubtitle,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                modifier = Modifier.testTag("muscle_explanation_text")
            )

            // Expandable Muscle Exercise Breakdown when user taps any muscle bar or body part
            if (selectedMuscleObj != null) {
                val openExLabel = when (lang) {
                    AppLanguage.HU -> "Gyakorlatok"
                    AppLanguage.EN -> "Exercises"
                    AppLanguage.DE -> "Übungen"
                    AppLanguage.RO -> "Exerciții"
                }
                val closeLabel = when (lang) {
                    AppLanguage.HU -> "Bezárás ✕"
                    AppLanguage.EN -> "Close ✕"
                    AppLanguage.DE -> "Schließen ✕"
                    AppLanguage.RO -> "Închide ✕"
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.glassTile(palette.surfaceElevated, elevated = true),
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotGlassRim(palette, RoundedCornerShape(14.dp), GlassMaterialRole.Tile)
                        .testTag("muscle_selected_summary")
                        .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .trainPilotClickable(RoundedCornerShape(14.dp)) { selectedMuscleId = "all" }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedMuscleObj.localizedName(lang)} • ${selectedMuscleObj.localizedSetsText(lang)} (${selectedMuscleObj.percentage}%)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary,
                                modifier = Modifier.weight(1f)
                            )
                            if (onOpenWorkout != null && selectedMuscleObj.workoutRows.any { it.workoutId.isNotBlank() }) {
                                Text(
                                    text = openExLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary,
                                    modifier = Modifier.heightIn(min = 48.dp).wrapContentHeight()
                                        .testTag("muscle_open_workouts")
                                        .trainPilotClickable(RoundedCornerShape(8.dp)) { showWorkoutRows = true }
                                )
                            } else {
                                Text(
                                    text = closeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        if (selectedMuscleObj.topExercisesHu.isNotEmpty()) HorizontalDivider(color = palette.cardBorder)
                        selectedMuscleObj.topExercisesHu.forEach { (exName, exLoad) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• $exName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = exLoad,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (showWorkoutRows && selectedMuscleObj != null && onOpenWorkout != null) {
        TrainPilotScreenSheet(onDismiss = { showWorkoutRows = false },
            testTag = "muscle_workouts_sheet") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${selectedMuscleObj.localizedName(lang)} · ${periodOptions[selectedPeriodIdx]}",
                        Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TrainPilotRedCloseButton(onClick = { showWorkoutRows = false })
                }
                androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    val rows = selectedMuscleObj.workoutRows.filter { it.workoutId.isNotBlank() }
                    items(rows.size) { index ->
                        val entry = rows[index]
                        val date = java.time.Instant.ofEpochMilli(entry.startedEpochMs)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        val group = when (entry.muscleKey.lowercase()) {
                            "biceps" -> if (lang == AppLanguage.HU) "Bicepsz" else "Biceps"
                            "triceps" -> if (lang == AppLanguage.HU) "Tricepsz" else "Triceps"
                            "glutes" -> when (lang) {
                                AppLanguage.HU -> "Far"
                                AppLanguage.EN -> "Glutes"
                                AppLanguage.DE -> "Gesäß"
                                AppLanguage.RO -> "Fesieri"
                            }
                            else -> null
                        }
                        Column(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("muscle_workout_${entry.workoutId}")
                            .trainPilotClickable(RoundedCornerShape(8.dp)) {
                                showWorkoutRows = false
                                onOpenWorkout(entry.workoutId)
                            }.padding(8.dp)) {
                            Text(entry.exerciseName, style = MaterialTheme.typography.bodyLarge)
                            Text(listOfNotNull(chartDateLabel(date, lang.code),
                                selectedMuscleObj.copy(setsCount = entry.setsCount, setsText = "").localizedSetsText(lang), group)
                                .joinToString(" · "), color = palette.textSecondary,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

}

internal fun studioMuscleLoadColor(ratio: Float): Color = when {
    ratio >= 0.7f -> Color(0xFFE25555)
    ratio >= 0.4f -> Color(0xFFE68A2E)
    else -> Color(0xFFE5B842)
}

@Composable
internal fun LegendDot(color: Color, label: String) {
    val palette = LocalTrainPilotPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
    }
}

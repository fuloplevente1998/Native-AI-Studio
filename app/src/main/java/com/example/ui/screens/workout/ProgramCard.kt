package com.example.ui.screens

import com.example.ui.theme.glassTile
import com.example.ui.theme.trainPilotGlassRim
import androidx.compose.runtime.key

import com.repforge.app.ui.theme.trainPilotClickable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.ui.components.ProgramTypeEmblemIcon
import com.example.ui.components.programEquipmentSummary
import com.example.ui.components.programEquipmentCategoryLabel
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette

@Composable
internal fun ProgramCardContent(
    program: com.example.model.TrainingProgramModel,
    language: com.example.model.AppLanguage,
    isExpanded: Boolean,
    tagPrefix: String,
    emblemSize: androidx.compose.ui.unit.Dp,
    onActivateProgram: () -> Unit,
    onOpenMoreActions: () -> Unit,
    headerModifier: Modifier = Modifier,
    reorderSlot: (@Composable () -> Unit)? = null,
    expandedContent: (@Composable () -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val progName = program.localizedName(language)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header row: tap expands/collapses the inline editor (no separate toggle control);
        // long press and the trailing more button open the quick-action sheet.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(headerModifier)
                .semantics {
                    stateDescription = trW(
                        language,
                        if (isExpanded) "Kinyitva" else "Összecsukva",
                        if (isExpanded) "Expanded" else "Collapsed",
                        if (isExpanded) "Ausgeklappt" else "Eingeklappt",
                        if (isExpanded) "Extins" else "Restrâns"
                    )
                }
                .testTag("${tagPrefix}program_header_${program.id}")
                .padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            reorderSlot?.invoke()

            ProgramTypeEmblemIcon(
                programTitle = program.nameHu,
                isHighlighted = program.isActive,
                size = emblemSize
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = progName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    ),
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("${tagPrefix}program_title_${program.id}")
                )
                Text(
                    text = programCardShortSubtitle(program, language),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("${tagPrefix}program_subtitle_${program.id}")
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                if (!program.isActive) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .trainPilotClickable(RoundedCornerShape(12.dp)) { onActivateProgram() }
                            .testTag("${tagPrefix}program_activate_button_${program.id}")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.glassTile(palette.primary, elevated = true),
                            modifier = Modifier.trainPilotGlassRim(palette, RoundedCornerShape(12.dp))
                                .testTag("${tagPrefix}program_activate_visual_${program.id}")
                        ) {
                            Text(
                                text = strings.activateAction,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (palette.isLiquidGlass) palette.textPrimary else palette.onPrimary,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.primary,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .testTag("${tagPrefix}program_active_badge_${program.id}")
                    ) {
                        Text(
                            text = strings.activeBadgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.onPrimary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .size(48.dp)
                    .trainPilotClickable(RoundedCornerShape(10.dp)) { onOpenMoreActions() }
                    .semantics {
                        contentDescription = trW(
                            language,
                            "További műveletek: $progName",
                            "More actions: $progName",
                            "Weitere Aktionen: $progName",
                            "Mai multe acțiuni: $progName"
                        )
                    }
                    .testTag("${tagPrefix}program_more_button_${program.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.padding(end = 2.dp)
                )
            }
        }

        if (isExpanded && expandedContent != null) {
            val equipmentValue = remember(program, language) {
                programDetailsLocationAndEquipment(program, language).second
            }
            val equipmentLabel = trW(language, "Felszerelés", "Equipment", "Ausrüstung", "Echipament")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 10.dp)
                    .testTag("${tagPrefix}program_inline_editor_${program.id}"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HorizontalDivider(color = palette.cardBorder.copy(alpha = 0.65f))
                Text(
                    text = "$equipmentLabel: $equipmentValue",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                    color = palette.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${tagPrefix}program_details_equipment_${program.id}")
                )
                expandedContent()
            }
        }
    }
}

/** True when an exercise draft differs from the program's stored prescription. */
internal fun hasUnsavedPrescriptionDrafts(
    program: com.example.model.TrainingProgramModel,
    drafts: Map<Pair<String, String>, com.example.model.StudioProgramPrescriptionDraft>
): Boolean = drafts.any { (key, draft) ->
    val exercise = program.days.firstOrNull { it.id == key.first }?.exercises?.firstOrNull { it.id == key.second }
    exercise == null || exercise.defaultSets != draft.sets || exercise.defaultReps != draft.reps ||
        exercise.defaultWeightKg != draft.weightKg || exercise.defaultRestSec != draft.restSec
}

internal fun programCardShortSubtitle(
    program: com.example.model.TrainingProgramModel,
    lang: com.example.model.AppLanguage
): String {
    val sub = program.localizedSubtitle(lang).trim()
    val hasDayCount = Regex("\\b\\d+\\s*(?:edzésnap|nap|workout days|days|Trainingstage|Tage|zile)", RegexOption.IGNORE_CASE)
        .containsMatchIn(sub)
    return if (hasDayCount || program.days.isEmpty()) {
        sub
    } else {
        val daySuffix = trW(
            lang,
            "${program.days.size} edzésnap",
            "${program.days.size} workout days",
            "${program.days.size} Trainingstage",
            "${program.days.size} zile de antrenament"
        )
        if (sub.isNotBlank()) "$sub • $daySuffix" else daySuffix
    }
}

internal fun programDetailsLocationAndEquipment(
    program: com.example.model.TrainingProgramModel,
    lang: com.example.model.AppLanguage
): Pair<String, String> {
    val candidateTokens = buildList {
        addAll(program.tagsHu)
        addAll(program.localizedTags(lang))
        addAll(program.subtitleHu.split('•', '·').map { it.trim() })
        addAll(program.localizedSubtitle(lang).split('•', '·').map { it.trim() })
        add(program.nameHu)
        add(program.id)
    }.filter { it.isNotBlank() }

    val locationValue = candidateTokens.firstNotNullOfOrNull { token ->
        val low = token.lowercase()
        when {
            low in setOf("otthon", "otthoni", "home", "zuhause", "acasă", "acasa") ||
                low.startsWith("home-") || low.contains("otthon") ->
                trW(lang, "Otthoni", "Home", "Zuhause", "Acasă")
            low in setOf("edzőterem", "konditerem", "konditermi", "terem", "gym", "fitnessstudio", "sală", "sala") ||
                low.startsWith("gym-") || low.contains("konditerm") || low.contains("edzőterm") ->
                trW(lang, "Konditermi", "Gym", "Fitnessstudio", "Sală")
            low in setOf("szabadtér", "szabadtéri", "kültéri", "outdoor", "exterior") ||
                low.contains("szabadtér") || low.contains("street workout") || low == "calisthenics-ab" ->
                trW(lang, "Szabadtéri", "Outdoor", "Outdoor", "Exterior")
            low in setOf("vegyes", "hibrid", "mixed", "hybrid", "gemischt", "mixt") ||
                low.contains("vegyes") ->
                trW(lang, "Vegyes", "Mixed", "Gemischt", "Mixt")
            else -> null
        }
    } ?: if (program.isCustom) {
        trW(lang, "Saját program", "Custom program", "Eigenes Programm", "Program propriu")
    } else {
        trW(lang, "Otthoni", "Home", "Zuhause", "Acasă")
    }

    val allExercises = program.days.flatMap { it.exercises }
    val rawEquipment = if (allExercises.isNotEmpty()) {
        programEquipmentSummary(allExercises, "", lang)
    } else {
        programEquipmentCategoryLabel(program.id.ifBlank { program.nameHu }, lang)
    }
    val knownPrefixes = setOf(
        "otthoni", "home", "zuhause", "acasă", "acasa",
        "konditermi", "gym", "fitnessstudio", "sală", "sala",
        "szabadtéri", "outdoor", "exterior",
        "saját program", "custom", "eigenes", "personalizat"
    )
    val equipmentValue = if (rawEquipment.contains("•")) {
        val prefix = rawEquipment.substringBefore("•").trim()
        if (prefix.lowercase() in knownPrefixes) {
            rawEquipment.substringAfter("•").trim().replaceFirstChar { it.uppercase() }
        } else {
            rawEquipment
        }
    } else {
        rawEquipment
    }
    return locationValue to equipmentValue
}

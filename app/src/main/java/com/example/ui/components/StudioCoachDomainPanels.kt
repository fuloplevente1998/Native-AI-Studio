package com.example.ui.components

import com.example.ui.theme.liquidDropMaterial
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.trainPilotGlassRim

import com.example.ui.theme.glassStateTile
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassTile
import com.example.ui.theme.glassCardFill
import com.example.ui.theme.coachAdviceDividerColor
import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotSoftGlow

import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.theme.GlassScrollChrome
import com.example.ui.theme.glassHeaderMaterial
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import com.repforge.app.ui.theme.TrainPilotTextButton as TextButton
import com.repforge.app.ui.theme.TrainPilotOutlinedButton as OutlinedButton
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.coach.*
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.trainPilotBorder
import androidx.compose.foundation.shape.RoundedCornerShape

private fun coachTr(lang: String, hu: String, en: String, de: String, ro: String): String =
    when (lang.lowercase().take(2)) {
        "en" -> en
        "de" -> de
        "ro" -> ro
        else -> hu
    }

/** Studio presentation of the canonical Coach; no local advice formula or silent prescription edits. */
@Composable
internal fun StudioCoachRecommendations(
    state: CoachUiState,
    onStart: (String, String, String?) -> Unit,
    onWorkout: () -> Unit,
    onExercise: (String) -> Unit,
    onPreviewProgramApplication: (String) -> Unit,
    onCloseVariation: () -> Unit,
    onSaveVariation: () -> Unit,
    showTarget: Boolean = true,
    programChangeAllowed: Boolean = true,
    modifier: Modifier = Modifier,
    pinnedList: Boolean = false
) {
    val palette = LocalTrainPilotPalette.current
    val lang = state.language.ifBlank { com.example.localization.LocalTrainPilotStrings.current.language.code }
    val text = nativeResources(LocalContext.current, lang)
    state.variationPreview?.let { preview ->
        TrainPilotFormSheet(onDismissRequest = onCloseVariation,
            title = { Text(coachTr(lang, "Programmódosítás előnézete", "Preview program change", "Programmanpassung prüfen", "Previzualizare modificare program")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${preview.target.programTitle} · ${preview.target.title}", fontWeight = FontWeight.Bold)
                    preview.weight?.let { change ->
                        val number = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag(lang)).apply {
                            maximumFractionDigits = 2
                        }
                        val oldWeight = if (change.before > 0) "${number.format(change.before)} kg"
                            else coachTr(lang, "nincs beállítva", "unset", "nicht festgelegt", "nesetat")
                        val unit = when (change.loadType) {
                            "per_hand" -> coachTr(lang, " kézenként", " per hand", " pro Hand", " pe mână")
                            "total" -> coachTr(lang, " összesen", " total", " gesamt", " total")
                            "single_dumbbell" -> coachTr(lang, " egy súlyzóval", " single dumbbell", " eine Hantel", " o ganteră")
                            else -> ""
                        }
                        Text(change.exerciseName)
                        Text("$oldWeight → ${number.format(change.after)} kg$unit")
                    }
                    preview.next?.let { Text(NativeCoachAdviceText.variation(text, it)) }
                    Text(coachTr(lang, "A módosítás az aktuális program ezen edzésnapjára kerül.",
                        "The change is saved to this day of the active program.",
                        "Die Änderung wird für diesen Tag im aktiven Programm gespeichert.",
                        "Modificarea se salvează pentru această zi din programul activ."))
                    if (state.variationFailed) Text(programApplicationError(lang), color = palette.danger)
                }
            },
            confirmButton = { com.repforge.app.ui.theme.TrainPilotSaveButton(onClick = onSaveVariation, enabled = !state.variationBusy,
                modifier = Modifier.testTag("studio_coach_variation_confirm")) {
                Text(coachTr(lang, "Mentés a programba", "Save to program", "Im Programm speichern", "Salvează în program"))
            } },
            dismissButton = { TextButton(onClick = onCloseVariation, enabled = !state.variationBusy) { Text(coachTr(lang, "Mégse", "Cancel", "Abbrechen", "Anulează")) } })
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.initialLoading) CircularProgressIndicator(Modifier.size(24.dp).testTag("studio_coach_initial_loading"))
        if (state.error) Text(coachTr(lang, "A Coach adatai nem frissíthetők. A korábbi kijelzés nem friss mérés.", "Coach data could not be refreshed. Previous values are not a fresh measurement.", "Coach-Daten konnten nicht aktualisiert werden.", "Datele Coach nu pot fi actualizate."), color = palette.danger)
        if (showTarget) StudioCoachNextTrainingCard(state, onStart, onWorkout)
        // 2783: one compact card, one row per exercise (name · action chip · target · sets); the reason
        // shared by every exercise is shown once on top, the rest opens per row.
        if (state.exercises.isNotEmpty()) {
            val reasons = state.exercises.map { it.advice.reason }.distinct()
            val sharedReason = reasons.singleOrNull()?.let { NativeCoachAdviceText.reason(text, it) }?.takeIf { it.isNotBlank() }
            var openRow by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
            val listShape = RoundedCornerShape(16.dp)
            val heading: @Composable () -> Unit = {
                Text(coachTr(lang, "Mai javaslat gyakorlatonként", "Today's advice per exercise", "Heutige Empfehlung je Übung", "Recomandarea de azi pe exerciții"),
                    style = MaterialTheme.typography.titleSmall, color = palette.textPrimary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("studio_coach_fixed_advice_header"))
            }
            val adviceRows: @Composable () -> Unit = {
                    if (sharedReason != null) Surface(color = palette.glassStateTile(palette.warning, palette.warning.copy(alpha = 0.14f)), shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp).testTag("studio_coach_shared_reason")) {
                        Text(sharedReason, color = palette.textPrimary, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                    }
                    state.exercises.forEach { row ->
                        val open = openRow == row.id
                        val chipColor = when (row.advice.action) {
                            "increase", "start", "reps" -> palette.success
                            "reduce", "easy", "pain" -> palette.danger
                            else -> palette.primary
                        }
                        androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = palette.coachAdviceDividerColor())
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .trainPilotClickable(RoundedCornerShape(10.dp)) { openRow = if (open) null else row.id }
                            .testTag("studio_coach_advice_${row.id}"),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(row.name, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary,
                                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            val chipShape = if (palette.isLiquidGlass) RoundedCornerShape(50) else RoundedCornerShape(8.dp)
                            Surface(color = palette.glassStateTile(chipColor, chipColor.copy(alpha = 0.16f)),
                                shape = chipShape, modifier = if (palette.isLiquidGlass)
                                    Modifier.trainPilotGlassRim(palette, chipShape, GlassMaterialRole.Tile) else Modifier) {
                                Text(NativeCoachAdviceText.label(text, row.advice.action), color = chipColor,
                                    style = MaterialTheme.typography.labelSmall, maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp).testTag("studio_coach_action_${row.id}"))
                            }
                            val target = if (row.advice.target.text.isNotBlank()) coachTargetText(row.advice.target, row.repUnit, lang) else ""
                            val sets = row.modePreview?.workingSets?.let { "$it×" }.orEmpty()
                            Text(listOf(target, sets).filter { it.isNotBlank() }.joinToString(" · "), color = palette.textSecondary,
                                style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.testTag("studio_coach_target_${row.id}"))
                            Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                        }
                        if (open) Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("studio_coach_details_${row.id}"),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (sharedReason == null) Text(NativeCoachAdviceText.reason(text, row.advice.reason), color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(coachTr(lang, "Megfelelő előzmény: ${row.advice.historyCount}", "Matching history: ${row.advice.historyCount}", "Passender Verlauf: ${row.advice.historyCount}", "Istoric relevant: ${row.advice.historyCount}"),
                                color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                            if (row.advice.action in setOf("increase", "reduce", "start") && row.advice.weight > 0)
                                Text("${coachTr(lang, "Javasolt súly", "Suggested weight", "Empfohlenes Gewicht", "Greutate recomandată")}: ${row.advice.weight} kg" + when (row.loadType) {
                                    "per_hand" -> coachTr(lang, " kézenként", " per hand", " pro Hand", " pe mână")
                                    "total" -> coachTr(lang, " összesen", " total", " gesamt", " total")
                                    "single_dumbbell" -> coachTr(lang, " egy súlyzóval", " single dumbbell", " mit einer Kurzhantel", " cu o ganteră")
                                    else -> ""
                                }, color = palette.textPrimary, style = MaterialTheme.typography.bodySmall)
                            if (row.advice.action == "reps" && row.advice.nextReps.isNotEmpty())
                                Text("${coachTr(lang, "Javasolt ismétlések", "Suggested reps", "Empfohlene Wdh.", "Repetări recomandate")}: ${row.advice.nextReps.joinToString(" / ")}",
                                    color = palette.textPrimary, style = MaterialTheme.typography.bodySmall)
                            row.modePreview?.let { preview ->
                                val style = if (preview.trainingStyle == "hit") coachTr(lang, "HIT-inspirált", "HIT-inspired", "HIT-inspiriert", "Inspirat HIT") else coachTr(lang, "Normál", "Normal", "Normal", "Normal")
                                val mode = when (preview.mode) {
                                    "linear" -> coachTr(lang, "Lineáris", "Linear", "Linear", "Liniar")
                                    "rir" -> "RIR/Coach"
                                    else -> coachTr(lang, "Dupla progresszió", "Double progression", "Doppelte Progression", "Progresie dublă")
                                }
                                Text("$style / $mode • ${preview.workingSets} ${coachTr(lang, "munkasorozat", "working sets", "Arbeitssätze", "serii de lucru")}",
                                    color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onExercise(row.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(coachTr(lang, "Gyakorlat részletei", "Exercise details", "Übungsdetails", "Detalii exercițiu"))
                            }
                            if (state.target != null && NativeCoachVariation.isApplicable(row, state.readiness)) {
                                if (state.programEditable) OutlinedButton(onClick = { onPreviewProgramApplication(row.id) },
                                    enabled = programChangeAllowed && !state.variationBusy && !state.loading && !state.error,
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("studio_coach_apply_${row.id}")) {
                                    Text(coachTr(lang, "Alkalmazás a programra", "Apply to program", "Auf das Programm anwenden", "Aplică în program"))
                                } else Text(coachTr(lang, "A javaslat az aktív saját programban menthető.", "The recommendation can be saved to the active custom program.",
                                    "Die Empfehlung kann im aktiven eigenen Programm gespeichert werden.", "Recomandarea poate fi salvată în programul personalizat activ."),
                                    style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            }
                        }
                    }
            }
            Surface(color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.glassTile(palette.surfaceCard), shape = listShape,
                modifier = (if (pinnedList) Modifier.weight(1f) else Modifier)
                    .then(if (palette.isLiquidGlass)
                        Modifier.liquidDropMaterial(palette, listShape, GlassMaterialRole.Panel, live = false)
                        else Modifier.trainPilotGlassRim(palette, listShape))
                    .fillMaxWidth().testTag("studio_coach_advice_list")
                    .trainPilotBorder(0.75.dp, palette.cardBorder, listShape)) {
                if (pinnedList) {
                    GlassScrollChrome(modifier = Modifier.fillMaxSize(), rootInset = 0.dp, localWindow = true,
                        top = {
                            Column(Modifier.fillMaxWidth().glassHeaderMaterial(palette, listShape)
                                .then(if (palette.isLiquidGlass) Modifier else Modifier.background(palette.surfaceCard, listShape))) {
                                heading()
                                HorizontalDivider(color = palette.coachAdviceDividerColor(),
                                    modifier = Modifier.padding(horizontal = 12.dp))
                            }
                        }
                    ) { scrollModifier, topInset, bottomInset ->
                        Column(scrollModifier.verticalScroll(rememberScrollState())
                            .testTag("studio_coach_advice_scroll")
                            .padding(horizontal = 12.dp)) {
                            Spacer(Modifier.height(topInset))
                            adviceRows()
                            Spacer(Modifier.height(bottomInset + 6.dp))
                        }
                    }
                } else Column(Modifier.padding(vertical = 6.dp)) {
                    heading()
                    Column(Modifier.padding(horizontal = 12.dp)) { adviceRows() }
                }
            }
        }
        if (state.variationFailed && state.variationPreview == null)
            Text(programApplicationError(lang), color = palette.danger)
        if (state.variationSaved) Text(coachTr(lang, "A programmódosítás elmentve.", "Program change saved.",
            "Programmänderung gespeichert.", "Modificarea programului a fost salvată."), color = palette.success)
    }
}

private fun programApplicationError(lang: String) = coachTr(lang,
    "A programmódosítás nem menthető. Frissítsd a javaslatot, és ellenőrizd, hogy lezártad-e az aktív edzést.",
    "The change could not be saved. Refresh the recommendation and check that the active workout is finished.",
    "Die Änderung konnte nicht gespeichert werden. Empfehlung aktualisieren und laufendes Training beenden.",
    "Modificarea nu a putut fi salvată. Actualizează recomandarea și verifică dacă ai încheiat antrenamentul activ.")

/** The next real program day is the primary Coach action, not a secondary status tile. */
@Composable
internal fun StudioCoachNextTrainingCard(
    state: CoachUiState,
    onStart: (String, String, String?) -> Unit,
    onWorkout: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val lang = state.language.ifBlank { com.example.localization.LocalTrainPilotStrings.current.language.code }
    val target = state.target
    if (target == null) {
        if (!state.loading && !state.error) {
            Text(coachTr(lang, "Nincs elérhető programnap. Válassz aktív programot.", "No program day available. Select an active program.", "Kein Programmtag verfügbar. Wähle ein aktives Programm.", "Nicio zi de program disponibilă. Alege un program activ."), color = palette.textSecondary)
        }
        return
    }
    // Do not invent an A/B badge from a UUID; prefer the actual day title.
    val dayTitle = target.title.trim()
    val titleLetter = listOf("A", "B", "C", "D").firstOrNull { letter ->
        listOf(letter, "$letter nap", "Day $letter", "Tag $letter", "Ziua $letter")
            .any { it.equals(dayTitle, ignoreCase = true) }
    }
    val letterDay = titleLetter ?: dayTitle.takeIf { it.length in 1..2 }
        ?: target.dayId.uppercase().takeIf { it in setOf("A", "B", "C", "D") }
    val shape = RoundedCornerShape(18.dp)
    Surface(color = palette.glassCardFill(palette.tintedSurface(0.12f)), shape = shape,
        modifier = Modifier.fillMaxWidth().testTag("studio_coach_target")
            .trainPilotGlassMaterial(palette, shape)
            .trainPilotSoftGlow(palette.primary, shape, 4.dp)
            .trainPilotBorder(2.dp, palette.primary.copy(alpha = 0.85f), shape)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                val badgeShape = RoundedCornerShape(50.dp)
                Surface(color = palette.glassStateTile(palette.primary, palette.tintedSurface(0.20f)), shape = badgeShape,
                    modifier = if (palette.isLiquidGlass) Modifier.trainPilotGlassRim(palette, badgeShape, GlassMaterialRole.Tile) else Modifier) {
                    Text(coachTr(lang, "KÖVETKEZŐ EDZÉSNAP", "NEXT WORKOUT DAY", "NÄCHSTER TRAININGSTAG", "URMĂTOAREA ZI DE ANTRENAMENT"), color = palette.primary,
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
                if (target.upcoming) Text(coachTr(lang, "TERVEZETT", "PLANNED", "GEPLANT", "PLANIFICAT"), color = palette.textSecondary,
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(target.programTitle.ifBlank { coachTr(lang, "Következő edzés", "Next workout", "Nächstes Training", "Următorul antrenament") },
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color = palette.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("studio_coach_program_name"))
                    // Hide only the duplicate label; a descriptive day name must remain visible.
                    if (letterDay == null || (titleLetter == null && dayTitle != letterDay))
                        Text(target.title, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary,
                            modifier = Modifier.testTag("studio_coach_day_name"))
                    // 2773: same date line as the Home card (today / still doable today / next date / cycle).
                    val line = homePlannedDateLine(target.schedule?.startIso, java.time.ZonedDateTime.now(),
                        com.example.model.AppLanguage.entries.firstOrNull { it.code == lang } ?: com.example.model.AppLanguage.HU, compact = true)
                    Text(line.text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                        color = when (line.timing) {
                            HomePlanTiming.TODAY_UPCOMING -> palette.success
                            HomePlanTiming.TODAY_PASSED -> palette.warning
                            else -> palette.textSecondary
                        }, modifier = Modifier.testTag("studio_coach_next_date"))
                }
                if (letterDay != null) {
                    Surface(color = palette.glassStateTile(palette.primary, palette.tintedSurface(0.16f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.then(if (palette.isLiquidGlass)
                            Modifier.trainPilotGlassRim(palette, RoundedCornerShape(16.dp), GlassMaterialRole.Tile) else Modifier)
                            .trainPilotBorder(1.25.dp, palette.primary,
                            RoundedCornerShape(16.dp)).testTag("studio_coach_day_badge")) {
                        Box(Modifier.sizeIn(minWidth = 46.dp, minHeight = 46.dp).padding(5.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text(letterDay, style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = palette.primary)
                        }
                    }
                }
            }
            com.repforge.app.ui.theme.TrainPilotGlassPrimaryButton(enabled = !state.loading && !state.error,
                onClick = { onStart(target.programId, target.dayId, target.schedule?.id) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("studio_coach_start_target")) {
                Text(coachTr(lang, "Edzés indítása", "Start workout", "Training starten", "Începe antrenamentul"),
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Canonical read-only advice is available independently of explicit mode approval. */
@Composable
internal fun StudioWorkoutCoachAdvice(state: WorkoutCoachAdviceState, onReload: () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val lang = com.example.localization.LocalTrainPilotStrings.current.language.code
    val text = nativeResources(LocalContext.current, lang)
    when {
        state.loading && state.row == null -> {
            CircularProgressIndicator(Modifier.size(24.dp).testTag("studio_active_coach_loading"))
            Text(coachTr(lang, "Előzmények betöltése…", "Loading history…", "Verlauf wird geladen…", "Se încarcă istoricul…"), color = palette.textSecondary)
        }
        state.error -> Text(coachTr(lang, "A Coach adatai nem olvashatók. Próbáld újra a frissítést.", "Coach data could not be read. Try refreshing again.", "Coach-Daten konnten nicht gelesen werden. Bitte erneut aktualisieren.", "Datele Coach nu pot fi citite. Încearcă din nou actualizarea."),
            color = palette.danger, modifier = Modifier.testTag("studio_active_coach_error"))
        state.row != null -> {
            val row = state.row
            Text(NativeCoachAdviceText.label(text, row.advice.action), color = palette.primary,
                modifier = Modifier.testTag("studio_active_coach_action"))
            // 2774: say where the pain hold comes from; never claim past history when there is none.
            Text(if (row.advice.reason == "pain" && row.advice.historyCount == 0) coachTr(lang,
                    "Ma fájdalmat jeleztél, ezért nincs terhelésemelés.",
                    "You reported pain today, so the load is not increased.",
                    "Du hast heute Schmerzen gemeldet, daher keine Laststeigerung.",
                    "Ai semnalat durere azi, deci nu creștem încărcarea.")
                else NativeCoachAdviceText.reason(text, row.advice.reason), color = palette.textSecondary,
                modifier = Modifier.testTag("studio_active_coach_reason"))
            Text(coachTr(lang, "Megfelelő előzmény: ${row.advice.historyCount}", "Matching history: ${row.advice.historyCount}", "Passender Verlauf: ${row.advice.historyCount}", "Istoric relevant: ${row.advice.historyCount}"), color = palette.textSecondary)
            if (row.advice.historyCount == 0 && row.advice.reason != "distance" &&
                row.modePreview?.reason !in setOf("invalid-target", "non-strength-activity")) Text(coachTr(lang,
                "Az elmúlt 90 napból nincs megfelelő, összehasonlítható előzmény ehhez a gyakorlathoz.",
                "No matching, comparable history for this exercise in the last 90 days.",
                "Kein passender, vergleichbarer Verlauf für diese Übung in den letzten 90 Tagen.",
                "Nu există istoric comparabil pentru acest exercițiu în ultimele 90 de zile."),
                color = palette.textSecondary, modifier = Modifier.testTag("studio_active_coach_no_history"))
            if (row.advice.target.text.isNotBlank()) Text("${coachTr(lang, "Cél", "Target", "Ziel", "Țintă")}: ${coachTargetText(row.advice.target, row.repUnit, lang)}", color = palette.textSecondary)
            if (row.advice.action in setOf("increase", "reduce", "start") && row.advice.weight > 0)
                Text("${coachTr(lang, "Javasolt súly", "Suggested weight", "Empfohlenes Gewicht", "Greutate recomandată")}: ${row.advice.weight} kg" + when (row.loadType) {
                    "per_hand" -> coachTr(lang, " kézenként", " per hand", " pro Hand", " pe mână")
                    "total" -> coachTr(lang, " összesen", " total", " gesamt", " total")
                    "single_dumbbell" -> coachTr(lang, " egy súlyzóval", " single dumbbell", " mit einer Kurzhantel", " cu o ganteră")
                    else -> ""
                }, color = palette.textPrimary, modifier = Modifier.testTag("studio_active_coach_weight"))
            if (row.advice.nextReps.isNotEmpty()) Text("${coachTr(lang, "Javasolt ismétlések", "Suggested reps", "Empfohlene Wdh.", "Repetări recomandate")}: ${row.advice.nextReps.joinToString(" / ")}",
                color = palette.textPrimary, modifier = Modifier.testTag("studio_active_coach_reps"))
        }
        else -> Text(coachTr(lang, "Nincs kiválasztott aktív gyakorlat.", "No active exercise selected.", "Keine aktive Übung ausgewählt.", "Niciun exercițiu activ selectat."), color = palette.textSecondary)
    }
    TextButton(onClick = onReload, enabled = !state.loading,
        modifier = Modifier.testTag("studio_active_coach_refresh")) {
        Text(coachTr(lang, "Coach frissítése", "Refresh Coach", "Coach aktualisieren", "Actualizează Coach"))
    }
}

/** Apply only through the existing snapshot/ownership/settings/history-checked native proposal. */
@Composable
internal fun rememberStudioWorkoutCoachOwner(): androidx.lifecycle.ViewModelStoreOwner {
    // Retain across lazy-item disposal; clear when the exercise screen leaves composition.
    val store = remember { androidx.lifecycle.ViewModelStore() }
    val owner = remember { object : androidx.lifecycle.ViewModelStoreOwner {
        override val viewModelStore = store
    } }
    DisposableEffect(store) { onDispose { store.clear() } }
    return owner
}

@Composable
internal fun StudioWorkoutCoachPanel(exerciseId: String,
    owner: androidx.lifecycle.ViewModelStoreOwner = rememberStudioWorkoutCoachOwner()) {
    val app = LocalContext.current.applicationContext as android.app.Application
    val model: com.repforge.app.ui.workout.ActiveWorkoutViewModel = viewModel(
        viewModelStoreOwner = owner,
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app))
    val state by model.uiState.collectAsStateWithLifecycle()
    val adviceModel: WorkoutCoachAdviceViewModel = viewModel(viewModelStoreOwner = owner,
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app))
    LaunchedEffect(exerciseId) { adviceModel.selectExercise(exerciseId) }
    val adviceState by adviceModel.state.collectAsStateWithLifecycle()
    val palette = LocalTrainPilotPalette.current
    val lang = com.example.localization.LocalTrainPilotStrings.current.language.code
    if (state.modeReviewOpen) AlertDialog(
        onDismissRequest = model::cancelTrainingModeReview,
        title = { Text(coachTr(lang, "Coach-javaslat jóváhagyása", "Approve Coach Proposal", "Coach-Vorschlag bestätigen", "Aprobă propunerea Coach")) },
        text = { Text(coachTr(lang, "Csak az érintetlen sorozatok súlya változik. RIR, teljesítettség, idő és mérés nem keletkezik.", "Only untouched sets will update weight. RIR, completion, time, and measurements are not fabricated.", "Nur unberührte Sätze ändern das Gewicht. RIR, Abschluss und Zeit werden nicht erzeugt.", "Doar seriile neatinse își modifică greutatea. RIR, finalizarea și timpul nu sunt generate.")) },
        confirmButton = { TextButton(onClick = model::approveTrainingModeWeight, enabled = !state.modeProposalLoading,
            modifier = Modifier.testTag("studio_active_coach_approve")) { Text(coachTr(lang, "Alkalmazás", "Apply", "Anwenden", "Aplică")) } },
        dismissButton = { TextButton(onClick = model::cancelTrainingModeReview) { Text(coachTr(lang, "Mégse", "Cancel", "Abbrechen", "Anulează")) } })
    Surface(color = palette.glassTile(palette.surfaceCard), shape = RoundedCornerShape(18.dp),
        modifier = Modifier.trainPilotGlassRim(palette, RoundedCornerShape(18.dp)).fillMaxWidth().testTag("studio_active_coach_panel")
            .trainPilotBorder(1.dp, palette.sectionBorder, RoundedCornerShape(18.dp))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TrainPilot Coach", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            StudioWorkoutCoachAdvice(if (adviceState.exerciseId == exerciseId) adviceState else WorkoutCoachAdviceState(exerciseId), adviceModel::reload)
            if (state.explicitTrainingMode) {
                OutlinedButton(onClick = { model.previewTrainingModeWeight(exerciseId) },
                    enabled = !state.modeProposalLoading && !state.isFinishing && !state.isPaused && state.timerOwner == "phone",
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("studio_active_coach_preview")) {
                    Text(if (state.modeProposalLoading) coachTr(lang, "Előkészítés…", "Preparing…", "Vorbereiten…", "Se pregătește…") else coachTr(lang, "Valódi előzmények alapján javaslat", "Proposal from real history", "Vorschlag aus echtem Verlauf", "Propunere din istoricul real"))
                }
                if (state.modeProposalForExercise == exerciseId) {
                    Text(state.modeReason, color = palette.textPrimary)
                    Text(coachTr(lang, "Megfelelő előzmény: ${state.modeHistoryCount}", "Matching history: ${state.modeHistoryCount}", "Passender Verlauf: ${state.modeHistoryCount}", "Istoric relevant: ${state.modeHistoryCount}"), color = palette.textSecondary)
                    if (state.modeSuggestedWeight != null && state.modeBaselineWeight != null) {
                        Text("${state.modeBaselineWeight} → ${state.modeSuggestedWeight} kg", color = palette.textPrimary)
                        Button(onClick = model::reviewTrainingModeWeight,
                            enabled = !state.modeProposalLoading && !state.isPaused && state.timerOwner == "phone",
                            modifier = Modifier.testTag("studio_active_coach_review")) { Text(coachTr(lang, "Javaslat áttekintése", "Review Proposal", "Vorschlag prüfen", "Revizuiește propunerea")) }
                    } else Text(coachTr(lang, "Most nincs jóváhagyható súlymódosítás.", "No weight adjustment to approve right now.", "Derzeit keine Gewichtsänderung verfügbar.", "Nicio modificare de greutate de aprobat acum."), color = palette.textSecondary)
                }
            }
            state.statusMessage?.let { Text(it, color = palette.textSecondary) }
        }
    }
}


/** 2774: collapsed Coach during a workout: icon + "Coach" + state dot; the full panel opens as a sheet. */
@Composable
internal fun StudioWorkoutCoachChip(exerciseId: String,
    owner: androidx.lifecycle.ViewModelStoreOwner = rememberStudioWorkoutCoachOwner(),
    modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as android.app.Application
    val adviceModel: WorkoutCoachAdviceViewModel = viewModel(viewModelStoreOwner = owner,
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app))
    LaunchedEffect(exerciseId) { adviceModel.selectExercise(exerciseId) }
    val adviceState by adviceModel.state.collectAsStateWithLifecycle()
    val palette = LocalTrainPilotPalette.current
    val row = adviceState.row?.takeIf { adviceState.exerciseId == exerciseId }
    val tone = coachChipTone(row?.advice)
    val warning = tone == CoachChipTone.WARNING
    val proposal = tone == CoachChipTone.PROPOSAL
    val tint = when { warning -> palette.warning; proposal -> palette.primary; else -> palette.textSecondary }
    var open by androidx.compose.runtime.saveable.rememberSaveable(exerciseId) { mutableStateOf(false) }
    // 2775: small framed tool button like the red Video button (~32 dp visible, 48 dp touch target).
    val language = com.example.localization.LocalTrainPilotStrings.current.language
    val adviceDescription = when (language) {
        com.example.model.AppLanguage.HU -> "Coach-javaslat érhető el"
        com.example.model.AppLanguage.EN -> "Coach advice available"
        com.example.model.AppLanguage.DE -> "Coach-Empfehlung verfügbar"
        com.example.model.AppLanguage.RO -> "Recomandare Coach disponibilă"
    }
    val shape = RoundedCornerShape(8.dp)
    androidx.compose.foundation.layout.Box(modifier.heightIn(min = 48.dp).clip(shape)
        .then(if (warning || proposal) Modifier.semantics { stateDescription = adviceDescription } else Modifier)
        .trainPilotClickable(shape) { open = true }.testTag("studio_active_coach_chip"),
        contentAlignment = androidx.compose.ui.Alignment.Center) {
        Surface(shape = shape, color = when {
            palette.isLiquidGlass -> androidx.compose.ui.graphics.Color.Transparent
            warning || proposal -> tint.copy(alpha = .14f)
            else -> palette.surfaceElevated
        },
            modifier = Modifier.liquidDropMaterial(palette, shape, GlassMaterialRole.Button,
                accent = tint.takeIf { warning || proposal }, live = false)
                .trainPilotBorder(1.dp, if (warning || proposal) tint.copy(alpha = .8f) else palette.cardBorder, shape)
                .testTag("studio_active_coach_chip_pill")) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(TrainPilotCoachIcon, null, tint = if (warning || proposal) tint else palette.primary, modifier = Modifier.size(15.dp))
                Text("Coach", color = if (warning) tint else palette.textPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                if (warning || proposal) Text("!", color = tint, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.testTag("studio_active_coach_chip_dot"))
            }
        }
    }
    if (open) TrainPilotScreenSheet(onDismiss = { open = false }, testTag = "studio_active_coach_sheet") {
        androidx.compose.foundation.layout.Box(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
            StudioWorkoutCoachPanel(exerciseId, owner)
        }
    }
}


internal enum class CoachChipTone { NEUTRAL, PROPOSAL, WARNING }

/** Warning for pain / low readiness / reduce, proposal for a real weight change, otherwise neutral. */
internal fun coachChipTone(advice: NativeCoachExerciseAdvice.Advice?): CoachChipTone = when {
    advice == null -> CoachChipTone.NEUTRAL
    advice.reason in setOf("pain", "low-readiness", "recovery") || advice.action == "reduce" -> CoachChipTone.WARNING
    advice.action in setOf("increase", "start") && advice.weight > 0 -> CoachChipTone.PROPOSAL
    else -> CoachChipTone.NEUTRAL
}


/** 2779: the target with its unit ("25 mp", "8–12 ism."); a text that already names a unit is kept. */
internal fun coachTargetText(target: com.repforge.app.ui.coach.NativeCoachExerciseAdvice.Target, repUnit: String, lang: String): String {
    val text = target.text.trim()
    // 2783: "10 /kar" (per-arm target without the rep word) → "10 ism./kar".
    Regex("^(\\d+(?:[.,]\\d+)?)\\s*/\\s*(\\p{L}+)$").find(text)?.let { m ->
        val word = if (target.kind == "time") coachTr(lang, "mp", "s", "Sek.", "sec") else coachTr(lang, "ism.", "reps", "Wdh.", "rep.")
        return "${m.groupValues[1]} $word/${m.groupValues[2]}"
    }
    if (text.isEmpty() || Regex("[a-zA-ZáéíóöőúüűÁÉÍÓÖŐÚÜŰ]").containsMatchIn(text)) return text
    val unit = repUnit.trim().ifBlank {
        when (target.kind) {
            "time" -> coachTr(lang, "mp", "s", "Sek.", "sec")
            "reps" -> coachTr(lang, "ism.", "reps", "Wdh.", "rep.")
            else -> ""
        }
    }
    return if (unit.isBlank()) text else "$text $unit"
}

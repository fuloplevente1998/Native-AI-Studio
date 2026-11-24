package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/** The caller owns durable save, active selection and dismissal after successful save. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CustomThemeEditorSheet(
    initial: CustomThemeDraft,
    themeKey: String,
    language: AppLanguage,
    onSave: (CustomThemeDefinition) -> Unit,
    onDismiss: () -> Unit,
    saving: Boolean = false,
    saveFailed: Boolean = false
) {
    val palette = LocalTrainPilotPalette.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var draft by remember(themeKey, initial) { mutableStateOf(initial) }
    var role by remember(themeKey) { mutableStateOf(CustomThemeRole.PRIMARY) }
    var workoutPreview by remember(themeKey) { mutableStateOf(false) }
    val resolved = remember(draft, themeKey) { resolveCustomTheme(draft, themeKey) }
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    fun word(hu: String, en: String, de: String, ro: String) = customThemeText(language, hu, en, de, ro)
    val opacityLabel = word("Felületi fedettség", "Surface opacity", "Flächendeckkraft", "Opacitatea suprafeței")
    val corrected = resolved?.correctedRoles.orEmpty()
    fun cancel() {
        if (!saving) scope.launch { sheet.hide(); onDismiss() }
    }

    TrainPilotModalBottomSheet(onDismissRequest = { if (!saving) onDismiss() }, sheetState = sheet,
        containerColor = palette.surface, contentColor = palette.textPrimary) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).navigationBarsPadding().imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp).testTag("custom_theme_editor"),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(word("Saját téma", "Custom theme", "Eigenes Design", "Temă personalizată"),
                style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TrainPilotFilterChip(!workoutPreview, { workoutPreview = false },
                        { Text(word("Kezdőlap", "Home", "Start", "Acasă")) })
                    TrainPilotFilterChip(workoutPreview, { workoutPreview = true },
                        { Text(word("Aktív edzés", "Workout", "Training", "Antrenament")) })
                }
                if (resolved != null) CustomThemePreview(resolved, workoutPreview, language)
                else Text(word("Az előnézethez érvényes színek szükségesek.", "Enter valid colors to preview.",
                    "Gültige Farben für die Vorschau eingeben.", "Introdu culori valide pentru previzualizare."),
                    color = palette.textSecondary)
                TrainPilotTextField(draft.name, { draft = draft.copy(name = it) }, Modifier.fillMaxWidth().testTag("custom_theme_name"),
                    enabled = !saving, singleLine = true, label = { Text(word("Téma neve", "Theme name", "Designname", "Numele temei")) })
                Text(word("Alapstílus (a színek visszaállnak)", "Base style (resets colors)",
                    "Grundstil (setzt Farben zurück)", "Stil de bază (resetează culorile)"), color = palette.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CustomThemeStyle.entries.forEach { style ->
                        TrainPilotFilterChip(draft.style == style, {
                            if (style != draft.style) draft = CustomThemeDraft.fromStyle(style, draft.name)
                                .copy(opacityPercent = draft.opacityPercent)
                        }, { Text(style.title(language)) }, enabled = !saving)
                    }
                }
                Text(word("Színszerep", "Color role", "Farbrolle", "Rolul culorii"), color = palette.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CustomThemeRole.entries.forEach { option ->
                        TrainPilotFilterChip(role == option, { role = option }, { Text(option.title(language)) },
                            enabled = !saving, modifier = Modifier.testTag("custom_theme_role_" + option.name.lowercase()))
                    }
                }
                if (!saving) CustomThemeColorControls(role, draft.colors[role].orEmpty(), language) {
                    draft = draft.withColor(role, it)
                }
                TrainPilotFilterChip(draft.opacityPercent != 100, {
                    draft = draft.copy(opacityPercent = if (draft.opacityPercent == 100) 80 else 100)
                }, { Text(word("Áttetsző felületek", "Translucent surfaces", "Transparente Flächen", "Suprafețe translucide")) },
                    enabled = !saving)
                if (draft.opacityPercent != 100) {
                    Text("$opacityLabel: ${draft.opacityPercent}%", color = palette.textPrimary)
                    Slider(draft.opacityPercent.toFloat(), { draft = draft.copy(opacityPercent = it.toInt().coerceIn(70, 90)) },
                        valueRange = 70f..90f, enabled = !saving,
                        modifier = Modifier.semantics { contentDescription = opacityLabel }.testTag("custom_theme_opacity"))
                }
                if (corrected.isNotEmpty() && resolved != null) {
                    Text(word("Kontrasztjavítás szükséges. Az előnézet a javasolt színeket mutatja.",
                        "Contrast correction needed. The preview shows suggested colors.",
                        "Kontrastkorrektur nötig. Die Vorschau zeigt vorgeschlagene Farben.",
                        "Este necesară corectarea contrastului. Previzualizarea arată culorile sugerate."),
                        color = palette.warning)
                    corrected.forEach { changed ->
                        Text(changed.title(language) + ": " + draft.colors[changed] + " → " + resolved.suggestedDraft.colors[changed],
                            color = palette.textPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                    TrainPilotSecondaryButton(word("Javaslat alkalmazása", "Accept suggestion", "Vorschlag übernehmen", "Acceptă sugestia"),
                        { draft = resolved.suggestedDraft }, Modifier.fillMaxWidth().testTag("custom_theme_accept_correction"), enabled = !saving)
                }
            }
            if (saveFailed) Text(word("A mentés nem sikerült.", "Could not save theme.",
                "Design konnte nicht gespeichert werden.", "Tema nu a putut fi salvată."), color = palette.danger)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainPilotSecondaryButton(word("Mégse", "Cancel", "Abbrechen", "Anulează"), ::cancel,
                    Modifier.weight(1f), enabled = !saving)
                TrainPilotPrimaryButton(word("Mentés", "Save", "Speichern", "Salvează"), {
                    draft.toDefinition(themeKey)?.let(onSave)
                }, Modifier.weight(1f).testTag("custom_theme_save"),
                    enabled = !saving && draft.isInputValid && resolved != null && corrected.isEmpty())
            }
        }
    }
}

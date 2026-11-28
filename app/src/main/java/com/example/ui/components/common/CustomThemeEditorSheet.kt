package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
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
    saveFailed: Boolean = false,
    saveCompleted: Boolean = false
) {
    val palette = LocalTrainPilotPalette.current
    val currentSaving by rememberUpdatedState(saving)
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !currentSaving })
    val scope = rememberCoroutineScope()
    var draft by remember(themeKey, initial) { mutableStateOf(initial.copy(opacityPercent = 100)) }
    var role by remember(themeKey) { mutableStateOf(CustomThemeRole.PRIMARY) }
    var previewOpen by remember(themeKey) { mutableStateOf(false) }
    var nameOpen by remember(themeKey) { mutableStateOf(false) }
    var pendingName by remember(themeKey) { mutableStateOf(initial.name) }
    val resolved = remember(draft, themeKey) { resolveCustomTheme(draft, themeKey) }
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    fun word(hu: String, en: String, de: String, ro: String) = customThemeText(language, hu, en, de, ro)
    val corrected = resolved?.correctedRoles.orEmpty()
    LaunchedEffect(saveCompleted) {
        if (saveCompleted) { nameOpen = false; sheet.hide(); onDismiss() }
    }
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
                Text(word("Alapstílus (a színek visszaállnak)", "Base style (resets colors)",
                    "Grundstil (setzt Farben zurück)", "Stil de bază (resetează culorile)"), color = palette.textPrimary)
                if (palette.isLiquidGlass) {
                    val styles = CustomThemeStyle.entries
                    com.example.ui.components.LiquidSegmentedTabs(styles.map { it.title(language) }, styles.indexOf(draft.style),
                        { index -> val style = styles[index]; if (style != draft.style) draft = CustomThemeDraft.fromStyle(style, draft.name) },
                        "custom_theme_style_track", scrollable = true, enabled = !saving) { _, label, _ -> Text(label) }
                } else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CustomThemeStyle.entries.forEach { style ->
                        TrainPilotFilterChip(draft.style == style, {
                            if (style != draft.style) draft = CustomThemeDraft.fromStyle(style, draft.name)
                        }, { Text(style.title(language)) }, enabled = !saving)
                    }
                }
                Text(word("Színszerep", "Color role", "Farbrolle", "Rolul culorii"), color = palette.textPrimary)
                if (palette.isLiquidGlass) {
                    val roles = CustomThemeRole.entries
                    com.example.ui.components.LiquidSegmentedTabs(roles.map { it.title(language) }, roles.indexOf(role),
                        { role = roles[it] }, "custom_theme_role_track", scrollable = true, enabled = !saving,
                        itemTag = { "custom_theme_role_" + roles[it].name.lowercase() }) { _, label, _ -> Text(label) }
                } else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CustomThemeRole.entries.forEach { option ->
                        TrainPilotFilterChip(role == option, { role = option }, { Text(option.title(language)) },
                            enabled = !saving, modifier = Modifier.testTag("custom_theme_role_" + option.name.lowercase()))
                    }
                }
                if (!saving) CustomThemeColorControls(role, draft.colors[role].orEmpty(), language) {
                    draft = draft.withColor(role, it)
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
            TrainPilotSecondaryButton(word("Előnézet", "Preview", "Vorschau", "Previzualizare"),
                { previewOpen = true }, Modifier.fillMaxWidth().testTag("custom_theme_open_preview"),
                enabled = !saving && resolved != null)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainPilotSecondaryButton(word("Mégse", "Cancel", "Abbrechen", "Anulează"), ::cancel,
                    Modifier.weight(1f), enabled = !saving)
                TrainPilotPrimaryButton(word("Mentés", "Save", "Speichern", "Salvează"), {
                    pendingName = draft.name
                    nameOpen = true
                }, Modifier.weight(1f).testTag("custom_theme_save"),
                    enabled = !saving && !saveCompleted && draft.invalidRoles.isEmpty() && draft.hasValidOpacity && resolved != null && corrected.isEmpty())
            }
        }
    }
    // Full-size read-only preview does not mutate the draft or the active theme.
    if (previewOpen && resolved != null) {
        CustomThemePreviewDialog(resolved, language, onDismiss = { previewOpen = false })
    }
    // The name is only requested on Save, including a prefilled name for edits.
    if (nameOpen) {
        TrainPilotAlertDialog(
            onDismissRequest = { if (!saving) nameOpen = false },
            modifier = Modifier.testTag("custom_theme_name_dialog"),
            title = { Text(word("Téma mentése", "Save theme", "Design speichern", "Salvează tema")) },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrainPilotTextField(pendingName, { pendingName = it },
                        Modifier.fillMaxWidth().testTag("custom_theme_name"),
                        enabled = !saving, singleLine = true,
                        label = { Text(word("Téma neve", "Theme name", "Designname", "Numele temei")) })
                    if (saveFailed) Text(word("A mentés nem sikerült.", "Could not save theme.",
                        "Design konnte nicht gespeichert werden.", "Tema nu a putut fi salvată."), color = palette.danger)
                }
            },
            dismissButton = {
                TrainPilotSecondaryButton(word("Mégse", "Cancel", "Abbrechen", "Anulează"),
                    { nameOpen = false }, enabled = !saving)
            },
            confirmButton = {
                TrainPilotPrimaryButton(word("Mentés", "Save", "Speichern", "Salvează"), {
                    draft.copy(name = pendingName).toDefinition(themeKey)?.let(onSave)
                }, Modifier.testTag("custom_theme_confirm_save"),
                    enabled = !saving && !saveCompleted && pendingName.isNotBlank() &&
                        resolved != null && corrected.isEmpty())
            }
        )
    }

}

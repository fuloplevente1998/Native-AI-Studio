package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal class CustomThemeAccess(
    val themes: List<CustomThemeDefinition>,
    val activeKey: String,
    val save: suspend (CustomThemeDefinition) -> Unit,
    val apply: suspend (String) -> Unit,
    val delete: suspend (String) -> Unit
)

internal val LocalCustomThemeAccess = staticCompositionLocalOf<CustomThemeAccess?> { null }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CustomThemeSection(language: AppLanguage) {
    val access = LocalCustomThemeAccess.current ?: return
    val palette = LocalTrainPilotPalette.current
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<CustomThemeDefinition?>(null) }
    var deleting by remember { mutableStateOf<CustomThemeDefinition?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var saveCompleted by remember { mutableStateOf(false) }
    val newThemeFocus = remember { FocusRequester() }
    var restoreFocus by remember { mutableStateOf(false) }
    LaunchedEffect(editing, restoreFocus) {
        if (editing == null && restoreFocus) {
            newThemeFocus.requestFocus()
            restoreFocus = false
        }
    }
    fun word(hu: String, en: String, de: String, ro: String) = customThemeText(language, hu, en, de, ro)
    fun runOperation(action: suspend () -> Unit, success: () -> Unit = {}) {
        if (busy) return
        busy = true
        failed = false
        scope.launch {
            try {
                action()
                success()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failed = true
            } finally { busy = false }
        }
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("custom_theme_section"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(word("Saját témák", "Custom themes", "Eigene Designs", "Teme personalizate"),
            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
        TrainPilotPrimaryButton(word("+ Új téma", "+ New theme", "+ Neues Design", "+ Temă nouă"), {
            failed = false
            editing = CustomThemeDefinition(newCustomThemeKey(), CustomThemeDraft.fromStyle(CustomThemeStyle.MATTE))
        }, Modifier.fillMaxWidth().focusRequester(newThemeFocus).focusable().testTag("custom_theme_new"), enabled = !busy)
        access.themes.forEach { theme ->
            key(theme.key) {
                val active = theme.key == access.activeKey
                val shape = RoundedCornerShape(12.dp)
                Column(Modifier.fillMaxWidth().border(1.dp,
                    if (active) palette.selectionColors().border else palette.cardBorder, shape).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(theme.draft.name, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    Text(theme.draft.style.title(language) + if (active) " · " +
                        word("Aktív", "Active", "Aktiv", "Activă") else "", color = palette.textSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TrainPilotSecondaryButton(word("Alkalmaz", "Apply", "Anwenden", "Aplică"), {
                            runOperation({ access.apply(theme.key) })
                        }, enabled = !busy && !active)
                        TrainPilotSecondaryButton(word("Szerkesztés", "Edit", "Bearbeiten", "Editează"), {
                            failed = false
                            editing = theme
                        }, enabled = !busy)
                        TrainPilotSecondaryButton(word("Törlés", "Delete", "Löschen", "Șterge"), {
                            deleting = theme
                        }, enabled = !busy)
                    }
                }
            }
        }
        if (failed) Text(word("A témaművelet nem sikerült.", "Theme operation failed.",
            "Designaktion fehlgeschlagen.", "Operația asupra temei a eșuat."), color = palette.danger)
    }
    editing?.let { theme ->
        CustomThemeEditorSheet(theme.draft, theme.key, language,
            onSave = { saved -> runOperation({ access.save(saved) }) { saveCompleted = true } },
            onDismiss = { if (!busy) { editing = null; saveCompleted = false; restoreFocus = true } }, saving = busy,
            saveFailed = failed, saveCompleted = saveCompleted)
    }
    deleting?.let { theme ->
        AlertDialog(onDismissRequest = { if (!busy) deleting = null },
            title = { Text(word("Téma törlése?", "Delete theme?", "Design löschen?", "Ștergi tema?")) },
            text = { Text(theme.draft.name) },
            confirmButton = { androidx.compose.material3.Button(onClick = {
                runOperation({ access.delete(theme.key) }) { deleting = null }
            }, enabled = !busy, colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = palette.danger,
                contentColor = com.repforge.app.ui.theme.NativeThemeTokens.onFill(palette.danger))) {
                Text(word("Törlés", "Delete", "Löschen", "Șterge"))
            } },
            dismissButton = { TrainPilotSecondaryButton(word("Mégse", "Cancel", "Abbrechen", "Anulează"),
                { deleting = null }, enabled = !busy) })
    }
}

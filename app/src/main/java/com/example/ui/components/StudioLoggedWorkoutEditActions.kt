package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.ui.journal.JournalEditorDialog
import com.repforge.app.ui.journal.JournalViewModel
import com.repforge.app.ui.theme.TrainPilotOutlinedButton

@Composable
internal fun StudioLoggedWorkoutEditActions(workoutId: String, language: String, onSaved: () -> Unit,
    model: JournalViewModel = viewModel(key = "studio-workout-health")) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val editor by model.editor.collectAsStateWithLifecycle()
    var saving by remember(workoutId) { mutableStateOf(false) }
    LaunchedEffect(editor?.saving, editor?.failed, editor == null) {
        if (editor?.saving == true) saving = true
        else if (saving && editor == null) { saving = false; onSaved() }
        else if (editor?.failed == true) saving = false
    }
    TrainPilotOutlinedButton(onClick = model::openEditor,
        enabled = !state.deleting && state.detail?.workout?.id == workoutId && editor == null,
        modifier = Modifier.fillMaxWidth().testTag("studio_journal_edit")) { Text("Edzés és visszajelzés szerkesztése") }
    editor?.takeIf { it.opened.detail.workout.id == workoutId }?.let {
        StudioNativeEditorTheme { JournalEditorDialog(it, language, model::editDraft, { draft -> saving = true; model.saveEditor(draft) }, model::cancelEditor) }
    }
}

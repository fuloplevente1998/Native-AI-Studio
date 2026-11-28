package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    val label = when (language.lowercase()) {
        "en" -> "Edit workout and feedback"
        "de" -> "Training und Feedback bearbeiten"
        "ro" -> "Editează antrenamentul și feedbackul"
        else -> "Edzés és visszajelzés szerkesztése"
    }
    TrainPilotOutlinedButton(onClick = model::openEditor,
        enabled = !state.deleting && state.detail?.workout?.id == workoutId && editor == null,
        modifier = Modifier.fillMaxWidth().testTag("studio_journal_edit")) { Text(label) }
    editor?.takeIf { it.opened.detail.workout.id == workoutId }?.let {
        StudioNativeEditorTheme { JournalEditorDialog(it, language, model::editDraft, { draft -> model.saveEditor(draft, onSaved) }, model::cancelEditor) }
    }
}

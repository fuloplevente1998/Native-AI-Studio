package com.repforge.app.ui.journal

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repforge.app.NativeJournalEditor
import com.repforge.app.R
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.shared.SetEditorRules212
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Functional editor over a ViewModel-owned isolated draft. Only Save touches Room. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun JournalEditorDialog(state: JournalEditState, language: String,
    onChange: (NativeJournalEditor.Draft) -> Unit, onSave: (NativeJournalEditor.Draft) -> Unit, onCancel: () -> Unit) {
    val text = journalResources(LocalContext.current, language)
    val colors = LocalTrainPilotColors.current
    val draft = state.draft
    val feedbackLocked = remember(state.opened) {
        LegacyWebViewMigrator.workoutJson(state.opened.detail).optJSONObject("feedback")?.optBoolean("applied") == true
    }
    val original = remember(state.opened) { LegacyWebViewMigrator.workoutJson(state.opened.detail).getJSONArray("exercises") }
    val format = remember { DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss XXX")
        .withResolverStyle(java.time.format.ResolverStyle.STRICT) }
    fun local(value: String) = format.format(Instant.parse(value).atZone(ZoneId.systemDefault()))
    var started by rememberSaveable(state.opened.detail.workout.id) { mutableStateOf(local(draft.started)) }
    var finished by rememberSaveable(state.opened.detail.workout.id) { mutableStateOf(local(draft.finished)) }
    var invalid by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    fun replaceExercise(index: Int, value: NativeJournalEditor.Exercise) {
        onChange(draft.copy(exercises = draft.exercises.mapIndexed { i, ex -> if (i == index) value else ex }))
    }
    fun editSet(e: Int, s: Int, value: NativeJournalEditor.Set) {
        replaceExercise(e, draft.exercises[e].copy(sets = draft.exercises[e].sets.mapIndexed { i, set -> if (s == i) value else set }))
    }
    Dialog(onDismissRequest = { if (!state.saving) onCancel() }, properties = DialogProperties(
        usePlatformDefaultWidth = false, dismissOnBackPress = !state.saving, dismissOnClickOutside = false, decorFitsSystemWindows = true)) {
        JournalEditorFrame(text.getString(R.string.journal_edit_title), state.saving, onCancel) {
                if (choosing) {
                    OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth().testTag("journal_editor_search"),
                        singleLine = true, label = { Text(text.getString(R.string.journal_search)) })
                    val available = state.catalog.filter { source -> draft.exercises.none { it.id == source.id } &&
                        (search.isBlank() || listOf(source.nameHu, source.nameEn, source.nameDe, source.nameRo, source.id)
                            .any { it.contains(search, ignoreCase = true) }) }.take(80)
                    LazyColumn(Modifier.weight(1f).testTag("journal_editor_picker")) {
                        items(available, key = { it.id }) { source ->
                            val name = when (language) { "en" -> source.nameEn; "de" -> source.nameDe; "ro" -> source.nameRo; else -> source.nameHu }
                            TextButton(onClick = { onChange(NativeJournalEditor.addExercise(draft, source)); choosing = false; search = "" },
                                enabled = !state.saving, modifier = Modifier.fillMaxWidth().testTag("journal_editor_add_${source.id}")) {
                                Text(name.ifBlank { source.nameHu })
                            }
                        }
                    }
                    TextButton(onClick = { choosing = false }, enabled = !state.saving) { Text(text.getString(R.string.journal_cancel)) }
                } else {
                    LazyColumn(Modifier.weight(1f).testTag("journal_editor_list"), verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)) {
                        item {
                            JournalEditorPanel {
                                Text(text.getString(R.string.journal_edit_time_hint), color = colors.muted,
                                    style = MaterialTheme.typography.labelSmall)
                                JournalEditField(started, { started = it; invalid = false }, text.getString(R.string.journal_edit_started),
                                    "journal_editor_started", !state.saving, KeyboardType.Text)
                                JournalEditField(finished, { finished = it; invalid = false }, text.getString(R.string.journal_edit_finished),
                                    "journal_editor_finished", !state.saving, KeyboardType.Text)
                            }
                        }
                        item {
                            JournalEditorPanel {
                                Text(text.getString(R.string.journal_feedback), style = MaterialTheme.typography.titleSmall)
                                if (feedbackLocked) Text(text.getString(R.string.journal_feedback_locked), color = colors.muted)
                                val choices = listOf("easy" to R.string.journal_effort_easy, "light" to R.string.journal_effort_light,
                                    "good" to R.string.journal_effort_good, "challenging" to R.string.journal_effort_challenging,
                                    "hard" to R.string.journal_effort_hard, "pain" to R.string.journal_effort_pain)
                                JournalEffortDropdown(draft.feedbackEffort,
                                    choices.map { (key, label) -> key to text.getString(label) },
                                    text.getString(R.string.journal_feedback), !state.saving && !feedbackLocked,
                                    onSelect = { onChange(draft.copy(feedbackEffort = it)) })
                                OutlinedTextField(draft.note, { onChange(draft.copy(note = it)) },
                                    label = { Text(when (language) {
                                        "en" -> "Workout note"; "de" -> "Trainingsnotiz"; "ro" -> "Notiță antrenament"; else -> "Edzésjegyzet"
                                    }) }, enabled = !state.saving, minLines = 1, maxLines = 3,
                                    shape = RoundedCornerShape(12.dp), textStyle = MaterialTheme.typography.bodyMedium,
                                    colors = journalEditorFieldColors(),
                                    modifier = Modifier.fillMaxWidth().testTag("journal_editor_note"))
                            }
                        }
                        draft.exercises.forEachIndexed { e, exercise ->
                            val source = exercise.originalIndex?.let { original.getJSONObject(it) }
                            val library = state.catalog.firstOrNull { it.id == exercise.id }
                            val name = source?.optString(language)?.takeIf { it.isNotBlank() }
                                ?: library?.let { when (language) { "en" -> it.nameEn; "de" -> it.nameDe; "ro" -> it.nameRo; else -> it.nameHu } }?.takeIf { it.isNotBlank() }
                                ?: source?.optString("hu")?.takeIf { it.isNotBlank() } ?: exercise.id
                            val unit = source?.optString("repUnit") ?: library?.repUnit.orEmpty()
                            val type = source?.optString("measurementType") ?: library?.measurementType.orEmpty()
                            val bilateral = SetEditorRules212.normalizeUnit(unit) == "mp/oldal" || exercise.id == "side-plank"
                            val distance = type == "distance" || unit in setOf("km", "m")
                            val bodyweight = (source?.optString("loadType") ?: library?.loadType) == "bodyweight"
                            item {
                                JournalEditorPanel {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                        com.repforge.app.ui.theme.TrainPilotNegativeButton(onClick = { onChange(draft.copy(exercises = draft.exercises.filterIndexed { i, _ -> i != e })) },
                                            enabled = !state.saving && draft.exercises.size > 1,
                                            modifier = Modifier.heightIn(min = 48.dp).testTag("journal_editor_remove_exercise_$e")) { Text(text.getString(R.string.journal_edit_remove)) }
                                }
                                com.example.ui.components.ExerciseFeedbackControl(
                                    effort = exercise.effort, note = exercise.note, language = language,
                                    onEffort = { replaceExercise(e, exercise.copy(effort = it)) },
                                    onNote = { replaceExercise(e, exercise.copy(note = it)) },
                                    enabled = !state.saving,
                                    effortLocked = feedbackLocked || source?.optJSONObject("feedback")?.optBoolean("applied") == true,
                                    tag = "journal_exercise_feedback_$e")
                                }
                            }
                            exercise.sets.forEachIndexed { s, set ->
                                item {
                                    JournalEditorPanel {
                                        FlowRow(Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("${s + 1}.", style = MaterialTheme.typography.labelLarge,
                                                color = colors.accentText, modifier = Modifier
                                                    .background(colors.accent.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp))
                                            Row(Modifier.widthIn(min = 120.dp).heightIn(min = 48.dp)
                                                .toggleable(set.done, enabled = !state.saving, role = Role.Checkbox,
                                                    onValueChange = { editSet(e, s, set.copy(done = it)) })
                                                .testTag("journal_editor_done_${e}_$s"),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(set.done, onCheckedChange = null, enabled = !state.saving)
                                                Text(text.getString(R.string.journal_edit_done),
                                                    style = MaterialTheme.typography.labelMedium)
                                            }
                                            com.repforge.app.ui.theme.TrainPilotNegativeButton(onClick = { replaceExercise(e, exercise.copy(sets = exercise.sets.filterIndexed { i, _ -> i != s })) },
                                                enabled = !state.saving && exercise.sets.size > 1,
                                                modifier = Modifier.heightIn(min = 48.dp).testTag("journal_editor_remove_set_${e}_$s")) {
                                                Text(text.getString(R.string.journal_edit_remove), style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        if (bilateral) {
                                            if (!bodyweight) JournalEditField(set.weight, { editSet(e, s, set.copy(weight = it)) },
                                                text.getString(R.string.journal_edit_weight), "journal_editor_weight_${e}_$s", !state.saving)
                                            JournalEditorFieldPair(
                                                first = { JournalEditField(set.left, { editSet(e, s, set.copy(left = it)) },
                                                    text.getString(R.string.journal_edit_left), "journal_editor_left_${e}_$s", !state.saving) },
                                                second = { JournalEditField(set.right, { editSet(e, s, set.copy(right = it)) },
                                                    text.getString(R.string.journal_edit_right), "journal_editor_right_${e}_$s", !state.saving) })
                                        } else {
                                            JournalEditorFieldPair(
                                                first = {
                                                    if (!bodyweight) JournalEditField(set.weight, { editSet(e, s, set.copy(weight = it)) },
                                                        text.getString(R.string.journal_edit_weight), "journal_editor_weight_${e}_$s", !state.saving)
                                                    else JournalEditField(set.rir, { editSet(e, s, set.copy(rir = it)) },
                                                        text.getString(R.string.journal_edit_rir), "journal_editor_rir_${e}_$s", !state.saving)
                                                },
                                                second = { JournalEditField(set.reps, { editSet(e, s, set.copy(reps = it)) },
                                                    text.getString(if (distance || type == "time" || SetEditorRules212.normalizeUnit(unit).startsWith("mp"))
                                                        R.string.journal_edit_seconds else R.string.journal_edit_reps), "journal_editor_reps_${e}_$s", !state.saving) })
                                        }
                                        if (distance) JournalEditField(set.distance, { editSet(e, s, set.copy(distance = it)) },
                                            text.getString(R.string.journal_edit_distance), "journal_editor_distance_${e}_$s", !state.saving)
                                        if (!bodyweight || bilateral) JournalEditField(set.rir, { editSet(e, s, set.copy(rir = it)) },
                                            text.getString(R.string.journal_edit_rir), "journal_editor_rir_${e}_$s", !state.saving)
                                    }
                                }
                            }
                            item {
                                TrainPilotOutlinedButton(onClick = { replaceExercise(e, exercise.copy(sets = exercise.sets +
                                    NativeJournalEditor.Set(null, exercise.sets.lastOrNull()?.weight ?: "0", "", false))) },
                                    enabled = !state.saving && exercise.sets.size < 500,
                                    modifier = Modifier.fillMaxWidth().testTag("journal_editor_add_set_$e")) { Text(text.getString(R.string.journal_edit_add_set)) }
                            }
                        }
                        item {
                            TrainPilotOutlinedButton(onClick = { choosing = true }, enabled = !state.saving && draft.exercises.size < 200,
                                modifier = Modifier.fillMaxWidth().testTag("journal_editor_add_exercise")) { Text(text.getString(R.string.journal_edit_add_exercise)) }
                        }
                    }
                    if (invalid || state.failed) Text(text.getString(if (invalid) R.string.journal_edit_invalid else R.string.journal_edit_failed),
                        color = colors.dangerText, modifier = Modifier.testTag("journal_editor_error"))
                    com.repforge.app.ui.theme.TrainPilotSaveButton(onClick = {
                        val dates = runCatching {
                            val oldStart = state.opened.detail.workout.started
                            val oldFinish = state.opened.detail.workout.finished
                            draft.copy(started = if (started == local(oldStart)) oldStart else OffsetDateTime.parse(started, format).toInstant().toString(),
                                finished = if (finished == local(oldFinish)) oldFinish else OffsetDateTime.parse(finished, format).toInstant().toString())
                        }.getOrNull()
                        if (dates == null) invalid = true else onSave(dates)
                    }, enabled = !state.saving, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("journal_editor_save")) {
                        Text(text.getString(if (state.saving) R.string.journal_edit_saving else R.string.journal_edit_save))
                    }
                }
        }
    }
}

@Composable
private fun JournalEditField(value: String, change: (String) -> Unit, label: String, tag: String,
    enabled: Boolean, keyboard: KeyboardType = KeyboardType.Decimal) {
    OutlinedTextField(value, change, label = { Text(label) }, singleLine = true, enabled = enabled,
        shape = RoundedCornerShape(12.dp), textStyle = MaterialTheme.typography.bodyMedium,
        colors = journalEditorFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard), modifier = Modifier.fillMaxWidth().testTag(tag))
}

/** Keep numeric values side by side only when labels and scaled text have room. */
@Composable
private fun JournalEditorFieldPair(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 300.dp && fontScale <= 1.3f) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { first() }
                Box(Modifier.weight(1f)) { second() }
            }
        } else Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            first()
            second()
        }
    }
}

@Composable
internal fun journalEditorFieldColors(): TextFieldColors {
    val colors = LocalTrainPilotColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.text, unfocusedTextColor = colors.text,
        focusedContainerColor = colors.card, unfocusedContainerColor = colors.card,
        focusedBorderColor = colors.selectedBorder, unfocusedBorderColor = colors.line.copy(alpha = 0.4f),
        focusedLabelColor = colors.accentText, unfocusedLabelColor = colors.muted,
        cursorColor = colors.accent)
}

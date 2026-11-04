package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.repforge.app.NativeCustomExerciseEditor
import com.repforge.app.NativeDataReplacement
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.ui.programs.CustomExerciseDialog
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import kotlinx.coroutines.*

/** Reuses the validated editor and its history/program/active-workout deletion guards. */
@Composable
internal fun StudioCustomExerciseActions(id: String, language: String, beforeWrite: suspend () -> Unit, onChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val db = remember(context) { TrainPilotDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    var opened by remember(id) { mutableStateOf<ExerciseLibraryEntity?>(null) }
    var ticket by remember(id) { mutableStateOf<NativeDataReplacement.Continuation?>(null) }
    var busy by remember(id) { mutableStateOf(false) }
    var error by remember(id) { mutableStateOf<String?>(null) }
    fun word(hu: String, en: String, de: String, ro: String) = when (language.lowercase()) {
        "en" -> en
        "de" -> de
        "ro" -> ro
        else -> hu
    }
    fun change(deleted: Boolean = false, operation: suspend (ExerciseLibraryEntity) -> Unit) {
        val existing = opened ?: return
        val lease = ticket ?: return
        if (busy) return
        busy = true; error = null
        scope.launch {
            try {
                beforeWrite()
                withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        lease.requireCurrent(context)
                        operation(existing)
                    }
                }
                opened = null
                onChanged(deleted)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                error = when {
                    failure.message.orEmpty().contains("used in") -> word(
                        "A programban vagy naplóban használt gyakorlat nem törölhető.",
                        "Exercises used in a program or workout log cannot be deleted.",
                        "Eine im Programm oder Protokoll verwendete Übung kann nicht gelöscht werden.",
                        "Exercițiul folosit într-un program sau în jurnal nu poate fi șters."
                    )
                    failure.message.orEmpty().contains("active workout") -> word(
                        "Előbb fejezd be az aktív edzést.",
                        "Finish the active workout first.",
                        "Beende zuerst das aktive Training.",
                        "Finalizează mai întâi antrenamentul activ."
                    )
                    failure.message.orEmpty().contains("changed") -> word(
                        "A gyakorlat megváltozott. Nyisd meg újra.",
                        "The exercise has changed. Reopen it.",
                        "Die Übung wurde geändert. Öffne sie erneut.",
                        "Exercițiul s-a modificat. Redeschide-l."
                    )
                    else -> word(
                        "Nem sikerült a módosítás. Ellenőrizd a mezőket, vagy nyisd meg újra.",
                        "Could not save changes. Check the fields or reopen.",
                        "Änderung fehlgeschlagen. Prüfe die Felder oder öffne sie erneut.",
                        "Modificarea nu a reușit. Verifică câmpurile sau redeschide."
                    )
                }
            } finally { busy = false }
        }
    }
    TrainPilotOutlinedButton(onClick = {
        if (!busy) {
            busy = true; error = null
            scope.launch {
                try {
                    beforeWrite()
                    val lease = NativeDataReplacement.continuationForCurrentData(context)
                    val exercise = withContext(Dispatchers.IO) { requireNotNull(db.exerciseLibraryDao().getExerciseById(id)) }
                    lease.requireCurrent(context)
                    require(exercise.custom)
                    ticket = lease; opened = exercise
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    error = word(
                        "A gyakorlat nem nyitható meg. Frissítsd az oldalt.",
                        "Could not open exercise. Refresh the page.",
                        "Übung konnte nicht geöffnet werden. Aktualisiere die Seite.",
                        "Exercițiul nu poate fi deschis. Reîmprospătează pagina."
                    )
                }
                finally { busy = false }
            }
        }
    }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("studio_custom_exercise_edit")) {
        Text(word(
            "Saját gyakorlat szerkesztése",
            "Edit custom exercise",
            "Eigene Übung bearbeiten",
            "Editează exercițiul personalizat"
        ))
    }
    if (opened == null && error != null) Text(requireNotNull(error))
    opened?.let { existing -> StudioNativeEditorTheme {
        CustomExerciseDialog(existing, language, busy, error,
            onSave = { draft -> change { NativeCustomExerciseEditor.save(context, db, id, draft, language, existing.rawJson) } },
            onDelete = { change(deleted = true) { NativeCustomExerciseEditor.delete(context, db, id, existing.rawJson) } },
            onCancel = { if (!busy) { opened = null; ticket = null } })
    } }
}

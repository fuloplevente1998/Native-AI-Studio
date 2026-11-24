package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.repforge.app.*
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Reads only candidates already published by the existing health sync. No separate provider worker. */
@Composable
internal fun ExternalWorkoutImportPanel(language: AppLanguage, enabled: Boolean) {
    val context = LocalContext.current.applicationContext
    var open by remember { mutableStateOf(false) }
    TrainPilotOutlinedButton(onClick = { open = true }, enabled = enabled,
        modifier = Modifier.fillMaxWidth().testTag("external_workout_import")) {
        Text(tr(language, "Külső edzések importálása", "Import external workouts", "Externe Trainings importieren", "Importă antrenamente externe"))
    }
    if (open) ExternalWorkoutImportDialog(language, context) { open = false }
}

@Composable
private fun ExternalWorkoutImportDialog(language: AppLanguage, context: android.content.Context, close: () -> Unit) {
    val scope = rememberCoroutineScope()
    var provider by remember { mutableStateOf(HealthJournalStore.HC) }
    var days by remember { mutableStateOf(7L) }
    var before by remember { mutableStateOf(0L) }
    var beforeTime by remember { mutableStateOf(0L) }
    var page by remember { mutableStateOf<ExternalWorkoutPage?>(null) }
    var busy by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyMap<ExternalWorkoutIdentity, ExternalWorkoutImport.Outcome>()) }
    val error = tr(language, "Nem olvasható. Ellenőrizd az engedélyeket, majd szinkronizálj.", "Cannot read. Check permissions, then sync.", "Nicht lesbar. Berechtigungen prüfen und synchronisieren.", "Nu se poate citi. Verifică permisiunile, apoi sincronizează.")
    LaunchedEffect(provider, days, before, beforeTime) {
        busy = true
        page = null
        message = ""
        try {
            val selectedProvider = provider
            val requestedDays = days
            page = withContext(Dispatchers.IO) {
                val ticket = NativeDataReplacement.continuationForCurrentData(context)
                check(ExternalWorkoutImport.hasReadGrant(context, selectedProvider) { ticket.requireCurrent(context) })
                val journal = HealthJournalStore.get(context)
                val generation = journal.generation()
                val today = LocalDate.now()
                val result = journal.externalWorkoutPage(today.minusDays(requestedDays - 1).toString(),
                    today.toString(), context.packageName, before, beforeTime, provider = selectedProvider)
                ticket.requireCurrent(context)
                check(journal.generation() == generation && journal.pendingRestore().isEmpty())
                result
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { message = error }
        finally { busy = false }
    }
    AlertDialog(onDismissRequest = { if (!busy) close() },
        title = { Text(tr(language, "Külső edzések", "External workouts", "Externe Trainings", "Antrenamente externe")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr(language, "A korábban szinkronizált edzések. Frissítéshez használd az Egészség szinkronizálását.",
                    "Previously synced workouts. Use Health sync to refresh.", "Bereits synchronisierte Trainings. Zum Aktualisieren Gesundheit synchronisieren.",
                    "Antrenamente sincronizate anterior. Actualizează prin sincronizarea Sănătate."))
                Row {
                    TextButton(enabled = !busy && provider != HealthJournalStore.HC, onClick = {
                        provider = HealthJournalStore.HC; before = 0; beforeTime = 0
                    }) { Text("Health Connect") }
                    TextButton(enabled = !busy && provider != HealthJournalStore.SAMSUNG, onClick = {
                        provider = HealthJournalStore.SAMSUNG; before = 0; beforeTime = 0
                    }) { Text("Samsung Health") }
                }
                Row {
                    for (n in listOf(7L, 30L)) TextButton(enabled = !busy && days != n, onClick = {
                        days = n; before = 0; beforeTime = 0
                    }) { Text(tr(language, "$n nap", "$n days", "$n Tage", "$n zile")) }
                }
                if (busy) Text(tr(language, "Feldolgozás…", "Processing…", "Verarbeitung…", "Se procesează…"))
                if (message.isNotBlank()) Text(message)
                if (!busy && page?.workouts?.isEmpty() == true) Text(tr(language, "Nincs edzés ezen az oldalon.", "No workouts on this page.", "Keine Trainings auf dieser Seite.", "Nu există antrenamente pe această pagină."))
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(page?.workouts.orEmpty(), key = { ExternalWorkoutImport.localId(it.identity) }) { workout ->
                        Column {
                            val date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
                                .format(Instant.ofEpochMilli(workout.startMs))
                            Text(workout.title ?: tr(language, "Külső edzés", "External workout", "Externes Training", "Antrenament extern"))
                            Text("$date · ${workout.elapsedMs / 60_000} min · ${workout.identity.source}")
                            val outcome = results[workout.identity]
                            if (outcome != null) Text(importOutcome(language, outcome))
                            TrainPilotOutlinedButton(enabled = !busy && outcome !in setOf(
                                ExternalWorkoutImport.Outcome.IMPORTED, ExternalWorkoutImport.Outcome.ALREADY_IMPORTED,
                                ExternalWorkoutImport.Outcome.LOCALLY_DELETED, ExternalWorkoutImport.Outcome.REVIEW_OVERLAP),
                                onClick = {
                                    busy = true
                                    scope.launch {
                                        try {
                                            val result = ExternalWorkoutImport.importSelected(context, workout.identity, workout.modifiedMs)
                                            results = results + (workout.identity to result)
                                        } catch (cancelled: CancellationException) { throw cancelled }
                                        catch (_: Exception) { message = error }
                                        finally { busy = false }
                                    }
                                }) { Text(tr(language, "Importálás", "Import", "Importieren", "Importă")) }
                        }
                    }
                }
                val next = page
                if (next != null && next.next != 0L) TextButton(enabled = !busy, onClick = {
                    before = next.next; beforeTime = next.nextTime
                }) { Text(tr(language, "Következő oldal", "Next page", "Nächste Seite", "Pagina următoare")) }
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = close) { Text(tr(language, "Bezárás", "Close", "Schließen", "Închide")) } })
}

private fun importOutcome(language: AppLanguage, outcome: ExternalWorkoutImport.Outcome): String = when (outcome) {
    ExternalWorkoutImport.Outcome.IMPORTED -> tr(language, "Naplóba mentve", "Saved to journal", "Im Protokoll gespeichert", "Salvat în jurnal")
    ExternalWorkoutImport.Outcome.ALREADY_IMPORTED -> tr(language, "Már importálva", "Already imported", "Bereits importiert", "Deja importat")
    ExternalWorkoutImport.Outcome.LOCALLY_DELETED -> tr(language, "Korábban törölve", "Previously deleted", "Zuvor gelöscht", "Șters anterior")
    ExternalWorkoutImport.Outcome.REVIEW_OVERLAP -> tr(language, "Átfedő edzés – nincs összevonás", "Overlapping workout – not merged", "Überlappendes Training – nicht zusammengeführt", "Antrenament suprapus – necombinat")
    ExternalWorkoutImport.Outcome.UNAVAILABLE -> tr(language, "Nem elérhető", "Unavailable", "Nicht verfügbar", "Indisponibil")
    ExternalWorkoutImport.Outcome.CHANGED -> tr(language, "Megváltozott – frissíts", "Changed – refresh", "Geändert – aktualisieren", "Modificat – actualizează")
}

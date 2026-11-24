package com.example.ui.viewmodel

import com.example.model.MainTab
import com.example.model.SubScreen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import com.repforge.app.NativeDataReplacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

// Health / Egészség Actions
fun TrainPilotViewModel.openHealthAndExpand(
    pulse: Boolean = false,
    weight: Boolean = false,
    extra: Boolean = false,
    journal: Boolean = false,
    connect: Boolean = false,
    weeklyGoals: Boolean = false
) {
    _uiState.update { state ->
        val back = (state.backStack + state.navigationRoute()).takeLast(16)
        state.copy(
            currentTab = MainTab.HEALTH,
            currentSubScreen = SubScreen.NONE,
            backStack = back,
            expandedHealthPulseTrend = pulse || state.expandedHealthPulseTrend,
            expandedHealthWeightLog = weight || state.expandedHealthWeightLog,
            expandedHealthWeeklyGoals = weeklyGoals ||
                (state.currentTab == MainTab.HEALTH && state.expandedHealthWeeklyGoals),
            expandedHealthExtraData = extra || state.expandedHealthExtraData,
            expandedHealthJournal = journal,
            expandedHealthConnect = connect && !journal
        )
    }
}

fun TrainPilotViewModel.toggleHealthSection(section: String) {
    _uiState.update { state ->
        when (section) {
            "weekly" -> state.copy(expandedHealthWeeklyGoals = !state.expandedHealthWeeklyGoals)
            "weekly_steps" -> state.copy(expandedWeeklySteps = !state.expandedWeeklySteps)
            "weekly_workouts" -> state.copy(expandedWeeklyWorkouts = !state.expandedWeeklyWorkouts)
            "pulse" -> state.copy(expandedHealthPulseTrend = !state.expandedHealthPulseTrend)
            "weight" -> state.copy(expandedHealthWeightLog = !state.expandedHealthWeightLog)
            "extra" -> state.copy(expandedHealthExtraData = !state.expandedHealthExtraData)
            "journal" -> state.copy(expandedHealthJournal = !state.expandedHealthJournal, expandedHealthConnect = false)
            "connect" -> state.copy(expandedHealthJournal = false, expandedHealthConnect = !state.expandedHealthConnect)
            else -> state
        }
    }
}

fun TrainPilotViewModel.selectWeightRange(index: Int) {
    _uiState.update { it.copy(selectedWeightRangeIndex = index) }
}

fun TrainPilotViewModel.toggleAddWeightDialog(show: Boolean) {
    _uiState.update { it.copy(showAddWeightDialog = show) }
}

internal fun TrainPilotViewModel.parseUserEnteredDateToLocalDate(rawDate: String): LocalDate {
    val cleaned = rawDate.trim()
    if (cleaned.isBlank()) return LocalDate.now()
    val digits = Regex("\\d+").findAll(cleaned).mapNotNull { it.value.toIntOrNull() }.toList()
    if (digits.size >= 3) {
        val y = if (digits[0] in 1970..2100) digits[0] else LocalDate.now().year
        val m = digits[1].coerceIn(1, 12)
        val d = digits[2].coerceIn(1, 28)
        return runCatching { LocalDate.of(y, m, digits[2].coerceIn(1, 31)) }.getOrElse { LocalDate.of(y, m, d) }
    }
    return LocalDate.now()
}

/**
 * Saves a body weight entry. [editingId] = the existing entry's ID when editing (2788): the row is
 * replaced in place (same primary key), so an edit never leaves the old measurement behind.
 */
fun TrainPilotViewModel.saveNewBodyWeight(weightKg: Float, dateStr: String = "", editingId: String? = null) {
    if (weightKg <= 20f || weightKg >= 350f) {
        _uiState.update {
            it.copy(
                statusBannerMessage = tr(
                    "Érvénytelen testsúly: 20 és 350 kg között adható meg!",
                    "Invalid body weight: enter a value between 20 and 350 kg!",
                    "Ungültiges Körpergewicht: bitte zwischen 20 und 350 kg eingeben!",
                    "Greutate corporală invalidă: introdu o valoare între 20 și 350 kg!",
                    it.language
                )
            )
        }
        return
    }
    val parsedLocalDate = parseUserEnteredDateToLocalDate(dateStr)
    val dayKey = parsedLocalDate.toString()
    val db = nativeDatabase
    val scope = nativeScope
    val context = nativeContext
    if (db == null || scope == null || context == null) {
        _uiState.update {
            it.copy(
                statusBannerMessage = tr(
                    "A testsúly-adatbázis nem érhető el.",
                    "Body weight database is unavailable.",
                    "Körpergewichts-Datenbank nicht verfügbar.",
                    "Baza de date pentru greutate nu este disponibilă.",
                    it.language
                )
            )
        }
        return
    }
    scope.launch(Dispatchers.IO) {
        try {
            val repository = com.repforge.app.ui.progress.NativeWeightJournalRepository(context, db)
            val editId = editingId?.takeIf { it.isNotBlank() }
            if (editId == null) {
                repository.add(weightKg.toDouble(), dayKey)
            } else {
                val expected = db.bodyWeightDao().getWeightById(editId)
                    ?: throw com.repforge.app.ui.progress.NativeWeightJournalRepository.ChangedEntry()
                repository.edit(expected, weightKg.toDouble(), dayKey)
            }
            val allWeights = db.bodyWeightDao().getAllWeights()
            val mappedLogs = mapBodyWeightEntitiesToLogItems(allWeights)
            val loosePhotoCount = com.repforge.app.NativeLoosePhotos.count(db)
            val verified = db.bodyWeightDao().getWeightJournalSummary()
            val latestByDayKey = mappedLogs.lastOrNull()?.kg ?: verified.currentKg?.toFloat() ?: weightKg
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        currentBodyWeightKg = latestByDayKey,
                        bodyWeightLogs = mappedLogs,
                        privatePhotoCount = it.loggedWorkouts.sumOf { workout -> workout.photoCount } +
                            mappedLogs.sumOf { weight -> weight.photoCount } + loosePhotoCount,
                        showAddWeightDialog = false,
                        statusBannerMessage = tr(
                            "Testsúlymérés sikeresen elmentve ($dayKey): $weightKg kg",
                            "Body weight saved ($dayKey): $weightKg kg",
                            "Körpergewicht gespeichert ($dayKey): $weightKg kg",
                            "Greutate corporală salvată ($dayKey): $weightKg kg",
                            it.language
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("TrainPilotViewModel", "Failed to save body weight to Room", e)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(statusBannerMessage = tr(
                        "Nem sikerült elmenteni a testsúlymérést: ${e.localizedMessage ?: "adatbázishiba"}",
                        "Failed to save body weight: ${e.localizedMessage ?: "database error"}",
                        "Speichern des Körpergewichts fehlgeschlagen: ${e.localizedMessage ?: "Datenbankfehler"}",
                        "Salvarea greutății a eșuat: ${e.localizedMessage ?: "eroare bază de date"}",
                        it.language
                    ))
                }
            }
        }
    }
}

fun TrainPilotViewModel.deleteBodyWeightLog(id: String) {
    val db = nativeDatabase
    val scope = nativeScope
    val context = nativeContext
    if (db == null || scope == null || context == null) {
        _uiState.update {
            it.copy(
                statusBannerMessage = tr(
                    "A testsúly-adatbázis nem érhető el.",
                    "Body weight database is unavailable.",
                    "Körpergewichts-Datenbank nicht verfügbar.",
                    "Baza de date pentru greutate nu este disponibilă.",
                    it.language
                )
            )
        }
        return
    }
    scope.launch(Dispatchers.IO) {
        try {
            val expected = db.bodyWeightDao().getWeightById(id)
                ?: throw com.repforge.app.ui.progress.NativeWeightJournalRepository.ChangedEntry()
            com.repforge.app.ui.progress.NativeWeightJournalRepository(context, db).delete(expected)
            val allWeights = db.bodyWeightDao().getAllWeights()
            val mappedLogs = mapBodyWeightEntitiesToLogItems(allWeights)
            val loosePhotoCount = com.repforge.app.NativeLoosePhotos.count(db)
            val summary = db.bodyWeightDao().getWeightJournalSummary()
            val latestKg = mappedLogs.lastOrNull()?.kg ?: summary.currentKg?.toFloat() ?: 0f
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        bodyWeightLogs = mappedLogs,
                        currentBodyWeightKg = latestKg,
                        privatePhotoCount = it.loggedWorkouts.sumOf { workout -> workout.photoCount } +
                            mappedLogs.sumOf { weight -> weight.photoCount } + loosePhotoCount,
                        statusBannerMessage = tr(
                            "Testsúlymérés törölve.",
                            "Body weight entry deleted.",
                            "Körpergewichtseintrag gelöscht.",
                            "Înregistrare greutate ștearsă.",
                            it.language
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _uiState.update {
                it.copy(
                    statusBannerMessage = tr(
                        "A testsúly törlése nem sikerült; a mérés megmaradt.",
                        "Failed to delete body weight entry.",
                        "Löschen des Körpergewichts fehlgeschlagen.",
                        "Ștergerea greutății a eșuat.",
                        it.language
                    )
                )
            }
            android.util.Log.e("TrainPilotViewModel", "Failed to delete body weight log from Room", e)
        }
    }
}

fun TrainPilotViewModel.refreshPrivatePhotoCount() {
    val db = nativeDatabase ?: return
    val scope = nativeScope ?: return
    val context = nativeContext ?: return
    val ticket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }.getOrNull() ?: return
    val previous = _uiState.value
    val exercises = previous.exercises.associateBy { it.id }
    scope.launch(Dispatchers.IO) {
        try {
            val mappedWorkouts = readNativeWorkoutHistory(db, exercises)
            val mappedLogs = mapBodyWeightEntitiesToLogItems(db.bodyWeightDao().getAllWeights())
            val loosePhotoCount = com.repforge.app.NativeLoosePhotos.count(db)
            withContext(Dispatchers.Main.immediate) {
                if (nativeDatabase !== db) return@withContext
                if (runCatching { ticket.requireCurrent(context) }.isFailure) return@withContext
                _uiState.update { state ->
                    if (state.loggedWorkouts !== previous.loggedWorkouts || state.bodyWeightLogs !== previous.bodyWeightLogs)
                        return@update state
                    state.copy(
                        loggedWorkouts = mappedWorkouts,
                        bodyWeightLogs = mappedLogs,
                        privatePhotoCount = mappedWorkouts.sumOf { workout -> workout.photoCount } +
                            mappedLogs.sumOf { weight -> weight.photoCount } + loosePhotoCount
                    )
                }
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("TrainPilotViewModel", "Failed to refresh body weight photo count", e)
            _uiState.update {
                it.copy(statusBannerMessage = tr(
                    "A testsúlyfotók listáját nem sikerült frissíteni.",
                    "Could not refresh body weight photos.",
                    "Körpergewichtsfotos konnten nicht aktualisiert werden.",
                    "Fotografiile greutății nu au putut fi actualizate.",
                    it.language
                ))
            }
        }
    }
}

fun TrainPilotViewModel.togglePrimaryHealthSource() {
    _uiState.update {
        val next = !it.primaryHealthSourceSamsung
        it.copy(
            primaryHealthSourceSamsung = next,
            statusBannerMessage = if (next) {
                tr("Elsődleges forrás: Samsung Health", "Primary source: Samsung Health", "Primärquelle: Samsung Health", "Sursă principală: Samsung Health", it.language)
            } else {
                tr("Elsődleges forrás: Health Connect", "Primary source: Health Connect", "Primärquelle: Health Connect", "Sursă principală: Health Connect", it.language)
            }
        )
    }
}

fun TrainPilotViewModel.updateHealthSyncStatus(busy: Boolean, partial: Boolean = false, failed: Boolean = false, message: String) {
    _uiState.update { it.copy(healthSyncBusy = busy, healthSyncPartial = partial,
        healthSyncFailed = failed, healthSyncMessage = message) }
}

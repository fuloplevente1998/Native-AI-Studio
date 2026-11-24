package com.example.ui.viewmodel

import androidx.compose.ui.graphics.toArgb
import com.example.model.AppLanguage
import com.example.model.HomeDemoMode
import com.example.model.workoutsInCalendarWeekToDate
import com.example.model.workoutsInPeriodToDate
import com.example.ui.theme.PaletteCategory
import com.example.ui.theme.TrainPilotPalettes
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.repforge.app.NativeDataReplacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.store.ThemePaletteOption
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate

fun TrainPilotViewModel.selectPalette(paletteId: Int) {
    _uiState.update {
        it.copy(
            selectedPaletteId = paletteId,
            statusBannerMessage = tr(
                "Színpaletta frissítve (#$paletteId)",
                "Color palette updated (#$paletteId)",
                "Farbpalette aktualisiert (#$paletteId)",
                "Paletă de culori actualizată (#$paletteId)",
                it.language
            )
        )
    }
    val store = nativeSettingsStore
    val scope = nativeScope
    if (store != null && scope != null) {
        val pal = TrainPilotPalettes.getById(paletteId)
        val hexAccent = "#" + (pal.primary.toArgb().toLong() and 0xFFFFFFL).toString(16).padStart(6, '0')
        val hexAccent2 = "#" + (pal.secondary.toArgb().toLong() and 0xFFFFFFL).toString(16).padStart(6, '0')
        val option = ThemePaletteOption(
            key = pal.key,
            nameHu = pal.nameHu,
            nameEn = pal.nameEn,
            accent = hexAccent,
            accent2 = hexAccent2,
            family = when (pal.category) {
                PaletteCategory.MATTE, PaletteCategory.BASIC -> "matte"
                PaletteCategory.CONTRAST -> "contrast"
                PaletteCategory.GLOW, PaletteCategory.VIVID -> "glow"
                PaletteCategory.LIGHT -> "light"
                PaletteCategory.LIQUID_GLASS -> "glass"
                else -> "matte"
            },
            nameDe = pal.nameDe,
            nameRo = pal.nameRo
        )
        val ticket = runCatching { NativeDataReplacement.continuationForCurrentData(requireNotNull(nativeContext)) }
            .getOrElse { showBannerMessage(it.message ?: "A téma nem menthető."); return }
        val previous = paletteWriteJob
        previous?.cancel()
        paletteWriteJob = scope.launch(Dispatchers.IO) {
            try {
                previous?.join()
                store.setThemeForDataset(option, ticket)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { showBannerMessage(tr("A téma mentése nem sikerült.", "Could not save theme.",
                "Das Design konnte nicht gespeichert werden.", "Tema nu a putut fi salvată.", _uiState.value.language)) }
        }
    }
}

fun TrainPilotViewModel.setPaletteFilterCategory(category: PaletteCategory?) {
    _uiState.update { it.copy(paletteFilterCategory = category) }
}

fun TrainPilotViewModel.togglePalettePickerSheet(show: Boolean) {
    _uiState.update { it.copy(showPalettePickerSheet = show) }
}

fun TrainPilotViewModel.selectLanguage(lang: AppLanguage) {
    _uiState.update {
        it.copy(
            language = lang,
            statusBannerMessage = tr(
                "Nyelv: ${lang.label} (${lang.code})",
                "Language: ${lang.label} (${lang.code})",
                "Sprache: ${lang.label} (${lang.code})",
                "Limbă: ${lang.label} (${lang.code})",
                lang
            )
        )
    }
    val store = nativeSettingsStore
    val scope = nativeScope
    if (store != null && scope != null) {
        scope.launch(Dispatchers.IO) {
            try { store.setLanguage(lang.code.lowercase()) } catch (_: Throwable) {}
        }
    }
}

internal fun TrainPilotViewModel.persistAppearance(write: suspend (TrainPilotSettingsStore) -> Unit) {
    val store = nativeSettingsStore ?: return
    val context = nativeContext ?: return
    val scope = nativeScope ?: return
    val ticket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }.getOrNull() ?: return
    val previous = appearanceWriteJob
    appearanceWriteJob = scope.launch(Dispatchers.IO) {
        try {
            previous?.join()
            NativeDataReplacement.mutate(context) { ticket.requireCurrent(context); write(store) }
        } catch (error: Exception) {
            if (error is CancellationException) currentCoroutineContext().ensureActive()
            _uiState.update { it.copy(statusBannerMessage = tr("A megjelenés nem menthető.",
                "Could not save appearance.", "Darstellung konnte nicht gespeichert werden.", "Aspectul nu a putut fi salvat.")) }
        }
    }
}

fun TrainPilotViewModel.setFontScaleMultiplier(scale: Float) {
    val normalized = TrainPilotSettingsStore.normalizeFontScale(scale)
    _uiState.update { it.copy(fontScaleMultiplier = normalized) }
    persistAppearance { it.setFontScaleMultiplier(normalized) }
}

fun TrainPilotViewModel.toggleCompactPhoneMode(compact: Boolean) {
    _uiState.update { it.copy(compactPhoneMode = compact) }
    persistAppearance { it.setCompactPhoneMode(compact) }
}

fun TrainPilotViewModel.cycleHomeDemoMode(mode: HomeDemoMode) {
    // Demo layout selection must never fabricate health/Coach measurements.
    _uiState.update { it.copy(homeDemoMode = mode) }
}

fun TrainPilotViewModel.toggleGoalsDialog(show: Boolean) {
    _uiState.update { it.copy(showGoalsDialog = show) }
}

fun TrainPilotViewModel.updateGoals(
    workoutsTarget: Int,
    period: com.example.model.WorkoutGoalPeriod,
    stepsTarget: Int
) {
    val maxWorkouts = if (period == com.example.model.WorkoutGoalPeriod.WEEKLY) 14 else 60
    val cleanWorkouts = workoutsTarget.coerceIn(1, maxWorkouts)
    val cleanSteps = stepsTarget.coerceIn(2000, 30000)
    fun failed() { _uiState.update { it.copy(statusBannerMessage = tr(
        "A célok mentése nem sikerült. Próbáld újra.", "Could not save goals. Try again.",
        "Ziele konnten nicht gespeichert werden. Erneut versuchen.", "Obiectivele nu au fost salvate. Încearcă din nou.", it.language)) } }
    val db = nativeDatabase ?: run { failed(); return }
    val scope = nativeScope ?: run { failed(); return }
    val context = nativeContext ?: run { failed(); return }
    val ticket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }.getOrElse { failed(); return }
    val periodLabelHu = if (period == com.example.model.WorkoutGoalPeriod.MONTHLY) "havi" else "heti"
    scope.launch(Dispatchers.IO) {
        try {
            NativeDataReplacement.mutate(context) {
                ticket.requireCurrent(context)
                val json = JSONObject().put("weeklyGoalTarget", cleanWorkouts)
                    .put("workoutGoalTarget", cleanWorkouts).put("workoutGoalPeriod", period.name.lowercase())
                    .put("stepsGoal", cleanSteps).toString()
                db.keyValueDocumentDao().putDocument(
                    com.repforge.app.data.db.KeyValueDocumentEntity("native:userGoals", json, System.currentTimeMillis()))
                withContext(Dispatchers.Main.immediate) {
                    ticket.requireCurrent(context)
                    _uiState.update {
                        it.copy(
                            workoutGoalPeriod = period,
                            workoutGoalTarget = cleanWorkouts,
                            workoutGoalCompleted = it.loggedWorkouts.workoutsInPeriodToDate(period, LocalDate.now()).size,
                            weeklyGoalTarget = cleanWorkouts,
                            weeklyGoalCompleted = if (period == com.example.model.WorkoutGoalPeriod.WEEKLY) it.loggedWorkouts.workoutsInCalendarWeekToDate(LocalDate.now()).size
                                else it.loggedWorkouts.workoutsInCalendarWeekToDate(LocalDate.now()).size,
                            stepsGoal = cleanSteps,
                            showGoalsDialog = false,
                            statusBannerMessage = tr(
                                "Célok elmentve: $cleanWorkouts $periodLabelHu edzés, $cleanSteps lépés",
                                "Goals saved: $cleanWorkouts ${if (period == com.example.model.WorkoutGoalPeriod.MONTHLY) "monthly" else "weekly"} workouts, $cleanSteps steps",
                                "Ziele gespeichert: $cleanWorkouts ${if (period == com.example.model.WorkoutGoalPeriod.MONTHLY) "monatliche" else "wöchentliche"} Trainings, $cleanSteps Schritte",
                                "Obiective salvate: $cleanWorkouts antrenamente ${if (period == com.example.model.WorkoutGoalPeriod.MONTHLY) "lunare" else "săptămânale"}, $cleanSteps pași",
                                it.language
                            )
                        )
                    }
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { withContext(Dispatchers.Main.immediate) { failed() } }
    }
}

fun TrainPilotViewModel.updateGoals(weeklyTarget: Int, stepsTarget: Int) {
    updateGoals(weeklyTarget, _uiState.value.workoutGoalPeriod, stepsTarget)
}

fun TrainPilotViewModel.togglePersonalPlannerModal(show: Boolean) {
    _uiState.update { it.copy(showPersonalPlannerModal = show) }
}

fun TrainPilotViewModel.toggleQuickWorkoutSheet(show: Boolean) {
    _uiState.update { it.copy(showQuickWorkoutSheet = show) }
}

/** Opens the Quick workout sheet directly on a tab (Workout tab tiles). */
fun TrainPilotViewModel.openQuickWorkoutSheet(tab: Int) {
    _uiState.update { it.copy(showQuickWorkoutSheet = true, quickWorkoutTab = tab.coerceIn(0, 2)) }
}

fun TrainPilotViewModel.selectQuickWorkoutTab(tab: Int) {
    _uiState.update { it.copy(quickWorkoutTab = tab.coerceIn(0, 2)) }
}

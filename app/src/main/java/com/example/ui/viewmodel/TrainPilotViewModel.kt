package com.example.ui.viewmodel

import androidx.compose.ui.graphics.toArgb
import androidx.room.withTransaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.YearMonth
import java.time.ZoneId
import java.time.LocalTime
import androidx.lifecycle.ViewModel
import com.example.data.DemoDataProvider
import com.example.model.AppLanguage
import com.example.model.BodyWeightLogItem
import com.example.model.CalendarDayEvent
import com.example.model.CalendarEventStatus
import com.example.model.ExerciseCategory
import com.example.model.ExerciseItem
import com.example.model.ExerciseSetItem
import com.example.model.HomeDemoMode
import com.example.model.LoggedWorkoutEntry
import com.example.model.workoutsInCalendarWeekToDate
import com.example.model.workoutsInCalendarMonthToDate
import com.example.model.workoutsInPeriodToDate
import com.example.model.localWorkoutDate
import com.example.model.StudioProgressWindow
import com.example.model.progressPeriodDays
import com.example.model.MainTab
import com.example.model.PrRecordEntry
import com.example.model.TrainingProgramModel
import com.example.model.SubScreen
import com.example.ui.theme.PaletteCategory
import com.example.ui.theme.TrainPilotPalettes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.repforge.app.NativeDataReplacement
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.ui.graphics.toArgb
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.HealthBackgroundJob
import com.repforge.app.HealthJournalStore
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.WearSyncRepository
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.ProgramDayEntity
import com.repforge.app.data.db.ProgramEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.ThemePaletteOption
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.example.model.watchinfo.WatchInfoCardData
import com.example.model.watchinfo.WatchInfoStatus
import com.repforge.app.healthreadings.HealthReadingsProjection
import com.repforge.app.healthreadings.PulseDayReading
import com.repforge.app.healthreadings.RecoveryDayReading
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import com.repforge.app.TrainPilotWorkoutFinalization
import com.repforge.app.NativeWorkoutSession
import com.repforge.app.PhoneOwnership193
import com.repforge.app.BackupRestoreManager
import com.repforge.app.ui.progress.NativeProgressQuery
import com.repforge.app.ui.progress.MuscleLoadUiModel
import android.net.Uri
import kotlin.math.roundToInt

class TrainPilotViewModel : ViewModel() {
    internal val _uiState = MutableStateFlow(TrainPilotUiState())
    val uiState: StateFlow<TrainPilotUiState> = _uiState.asStateFlow()

    internal var nativeContext: Context? = null
    internal val calendarWriteMutex = Mutex()
    internal val programWriteMutex = Mutex()
    @Volatile private var programWriteRevision = 0L
    internal var programWriteJob: Job? = null
    @Volatile internal var programWriteError: String? = null
    @Volatile internal var calendarWriteRevision = 0L
    internal var calendarWriteJob: Job? = null
    @Volatile internal var calendarWriteError: String? = null
    @Volatile private var calendarProjectionTicket: NativeDataReplacement.Continuation? = null
    private val plannerSettingsWriteMutex = Mutex()
    @Volatile private var plannerSettingsRevision = 0L
    @Volatile private var plannerSettingsWriteJob: Job? = null
    @Volatile private var plannerSettingsWriteError: String? = null
    internal var nativeSettingsStore: TrainPilotSettingsStore? = null
    internal var nativeDatabase: TrainPilotDatabase? = null
    internal var nativeScope: CoroutineScope? = null
    internal var paletteWriteJob: Job? = null
    internal var nativeReloadJob: Job? = null
    internal var programEditorContinuation: com.repforge.app.NativeDataReplacement.Continuation? = null
    private var nativeObserversJob: Job? = null
    private var personalRecordsJob: Job? = null
    private var personalRecordsRevision = 0L
    @Volatile internal var watchHandoffBusy = false
    internal fun watchJournal(): JSONObject = nativeContext?.let { context ->
        runCatching { PhoneOwnership193.journal(context).read() }
            .getOrElse { JSONObject().put("stage", "unreadable") }
    } ?: JSONObject()


    internal fun tr(
        hu: String,
        en: String,
        de: String = en,
        ro: String = en,
        lang: AppLanguage = _uiState.value.language
    ): String = when (lang) {
        AppLanguage.HU -> hu
        AppLanguage.EN -> en
        AppLanguage.DE -> de
        AppLanguage.RO -> ro
    }

    private fun translateMuscleTokenHu(token: String): String {
        return when (token.trim().lowercase()) {
            "chest", "mell", "mellkas", "push" -> "Mell"
            "back", "hát", "lats", "pull" -> "Hát"
            "shoulders", "váll", "delts" -> "Váll"
            "biceps", "bicepsz" -> "Bicepsz"
            "triceps", "tricepsz" -> "Tricepsz"
            "arms", "kar", "forearms" -> "Kar"
            "legs", "quads", "láb", "comb" -> "Láb"
            "hamstrings", "combhajlító" -> "Combhajlító"
            "glutes", "far", "farizom" -> "Farizom"
            "calves", "vádli" -> "Vádli"
            "core", "abs", "has", "törzs" -> "Törzs / Has"
            "cardio", "kardió" -> "Kardió"
            "full", "full_body", "teljes test" -> "Teljes test"
            else -> token.trim().replaceFirstChar { it.uppercase() }
        }
    }

    private fun translateLoadTypeOrEquipmentHu(rawEquip: String, loadType: String): String {
        val cleanEquip = rawEquip.trim()
        val technicalTokens = setOf(
            "per_hand", "single_dumbbell", "bodyweight", "total", "weight",
            "dumbbell", "barbell", "machine", "cable", "band", "kettlebell"
        )
        if (cleanEquip.isNotBlank() && cleanEquip.lowercase() !in technicalTokens) {
            return cleanEquip.replaceFirstChar { it.uppercase() }
        }
        return when (cleanEquip.ifBlank { loadType }.trim().lowercase()) {
            "per_hand", "dumbbell" -> "2 kézisúlyzó"
            "single_dumbbell" -> "1 kézisúlyzó"
            "bodyweight" -> "Saját testsúly"
            "barbell" -> "Kétkezes rúd"
            "machine" -> "Edzőtermi gép"
            "cable" -> "Csigás gép"
            "band" -> "Gumiszalag"
            "kettlebell" -> "Kettlebell"
            "total", "weight" -> "Szabadsúly / Gép"
            else -> "Súlyzó / Eszköz"
        }
    }

    internal fun mapExerciseEntity(entity: com.repforge.app.data.db.ExerciseLibraryEntity): ExerciseItem {
        val rawObj = runCatching { JSONObject(entity.rawJson) }.getOrNull()
        val rawEquip = rawObj?.optString("equipment").orEmpty()
        val rawTarget = rawObj?.optString("target").orEmpty()
        val parsedMuscleList = runCatching {
            val arr = JSONArray(entity.muscleGroupsJson)
            (0 until arr.length()).mapNotNull { idx ->
                arr.optString(idx)?.takeIf { it.isNotBlank() }?.let { translateMuscleTokenHu(it) }
            }.distinct()
        }.getOrDefault(emptyList())

        val primaryKey = entity.primaryMuscle.lowercase()
        val cat = when (primaryKey) {
            "chest", "mell", "mellkas", "push" -> ExerciseCategory.CHEST
            "back", "hát", "pull" -> ExerciseCategory.BACK
            "shoulders", "váll" -> ExerciseCategory.SHOULDERS
            "arms", "biceps", "triceps", "bicepsz", "tricepsz", "kar" -> ExerciseCategory.ARMS
            "legs", "quads", "hamstrings", "calves", "glutes", "comb", "láb", "far", "cardio" -> ExerciseCategory.LEGS
            "core", "abs", "has", "törzs" -> ExerciseCategory.CORE
            else -> ExerciseCategory.CHEST
        }

        val cleanPrimaryHu = if (rawTarget.isNotBlank() && !rawTarget.startsWith("[")) {
            rawTarget.replaceFirstChar { it.uppercase() }
        } else {
            parsedMuscleList.firstOrNull() ?: translateMuscleTokenHu(entity.primaryMuscle.ifBlank { "Mell" })
        }

        val secondaryList = parsedMuscleList.filterNot {
            it.equals(cleanPrimaryHu, ignoreCase = true)
        }
        val cleanSecondaryHu = if (secondaryList.isNotEmpty()) {
            secondaryList.joinToString(", ")
        } else if (parsedMuscleList.size > 1) {
            parsedMuscleList.drop(1).joinToString(", ")
        } else {
            "Stabilizáló izmok"
        }

        val cleanEquipmentHu = translateLoadTypeOrEquipmentHu(rawEquip, entity.loadType)
        val parsedReps = Regex("\\d+").findAll(entity.defaultReps).lastOrNull()?.value?.toIntOrNull()
            ?: entity.defaultReps.toIntOrNull()
            ?: 10

        fun parseNotesToSteps(raw: String, fallback: String): List<String> = raw
            .split(Regex("(?<=\\.)\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(fallback) }

        val parsedStepsHu = parseNotesToSteps(
            entity.notesHu,
            "Helyes kivitelezés: ügyelj az egyenes hátra és a kontrollált mozgástartományra."
        )
        val parsedStepsEn = parseNotesToSteps(
            entity.notesEn.ifBlank { entity.notesHu },
            "Proper execution: keep a neutral spine and controlled range of motion."
        )
        val parsedStepsDe = parseNotesToSteps(
            entity.notesDe.ifBlank { entity.notesEn.ifBlank { entity.notesHu } },
            "Saubere Ausführung: gerader Rücken und kontrollierter Bewegungsumfang."
        )
        val parsedStepsRo = parseNotesToSteps(
            entity.notesRo.ifBlank { entity.notesEn.ifBlank { entity.notesHu } },
            "Execuție corectă: menține spatele drept și mișcarea controlată."
        )
        val isTimedFlag = entity.measurementType.contains("time", ignoreCase = true) ||
            entity.repUnit.contains("mp", ignoreCase = true) ||
            entity.repUnit.contains("sec", ignoreCase = true)
        val isBilateralFlag = entity.id.contains("side-plank", ignoreCase = true) ||
            entity.nameHu.contains("oldalsó plank", ignoreCase = true) ||
            entity.measurementType.contains("bilateral", ignoreCase = true) ||
            entity.measurementType.contains("side", ignoreCase = true) ||
            entity.repUnit.contains("oldal", ignoreCase = true)
        val defaultTargetSec = if (isTimedFlag || isBilateralFlag) parsedReps.coerceIn(10, 300) else 45
        return ExerciseItem(
            id = entity.id,
            nameHu = entity.nameHu.ifBlank { entity.nameEn }.ifBlank { "Gyakorlat" },
            nameEn = entity.nameEn.ifBlank { entity.nameHu }.ifBlank { "Exercise" },
            nameDe = entity.nameDe.ifBlank { entity.nameEn.ifBlank { entity.nameHu } },
            nameRo = entity.nameRo.ifBlank { entity.nameEn.ifBlank { entity.nameHu } },
            category = cat,
            equipmentHu = cleanEquipmentHu,
            primaryMusclesHu = cleanPrimaryHu,
            secondaryMusclesHu = cleanSecondaryHu,
            defaultSets = entity.defaultSets.coerceAtLeast(1),
            defaultReps = parsedReps.coerceAtLeast(1),
            defaultWeightKg = entity.defaultWeight.toFloat(),
            defaultRestSec = entity.defaultRestSeconds.coerceAtLeast(30),
            isTimed = isTimedFlag || isBilateralFlag,
            isBilateral = isBilateralFlag,
            targetSeconds = defaultTargetSec,
            stepsHu = parsedStepsHu,
            stepsEn = parsedStepsEn,
            stepsDe = parsedStepsDe,
            stepsRo = parsedStepsRo,
            coachStatusHu = "Normál terhelés",
            coachHintHu = parsedStepsHu.firstOrNull() ?: "Kezdj könnyebben; ma ne erőltesd a progressziót.",
            videoUrl = entity.videoUrl.orEmpty(),
            isCustom = entity.custom,
            loadType = entity.loadType, repUnit = entity.repUnit, measurementType = entity.measurementType,
            targetRepsText = entity.defaultReps
        )
    }

    private fun computePrRecordsFromWorkouts(workouts: List<LoggedWorkoutEntry>): List<PrRecordEntry> {
        data class BestEntry(val weightKg: Float, val reps: Int, val e1Rm: Float, val dateLabelHu: String)
        val bestByExercise = linkedMapOf<String, BestEntry>()
        for (w in workouts) {
            for (exDetail in w.exerciseDetails) {
                for (s in exDetail.sets) {
                    if (!s.completed) continue
                    val score = if (s.weightKg > 0f) s.weightKg * (1f + s.reps / 30f) else s.reps.toFloat()
                    val prev = bestByExercise[exDetail.nameHu]
                    if (prev == null || score > prev.e1Rm || (score == prev.e1Rm && s.weightKg > prev.weightKg)) {
                        bestByExercise[exDetail.nameHu] = BestEntry(
                            weightKg = s.weightKg,
                            reps = s.reps,
                            e1Rm = score,
                            dateLabelHu = w.dateLabelHu
                        )
                    }
                }
            }
        }
        return bestByExercise.entries
            .sortedByDescending { it.value.e1Rm }
            .mapIndexed { idx, (exName, best) ->
                val valueStr = if (best.weightKg > 0f) {
                    "${best.weightKg.toString().removeSuffix(".0")} kg × ${best.reps}"
                } else {
                    "${best.reps} ism. / mp"
                }
                val e1RmLabel = if (best.weightKg > 0f) "1RM ≈ ${"%.1f".format(best.e1Rm).replace('.', ',')} kg" else "Idő / ismétlés rekord"
                PrRecordEntry(
                    id = "pr_${idx}_${exName.hashCode()}",
                    exerciseNameHu = exName,
                    typeLabelHu = e1RmLabel,
                    dateLabel = best.dateLabelHu,
                    previousLabel = "—",
                    newLabel = valueStr
                )
            }
    }

    private fun mapBodyWeightEntitiesToLogItems(entities: List<BodyWeightEntryEntity>): List<BodyWeightLogItem> {
        val dotFormatter = DateTimeFormatter.ofPattern("yyyy. MM. dd.")
        return entities
            .sortedWith(compareBy<BodyWeightEntryEntity> { it.dayKey.ifBlank { it.date.take(10) } }.thenBy { it.updatedAt })
            .map { e ->
                val cleanDayKey = e.dayKey.ifBlank { e.date.take(10) }
                val parsedDate = runCatching { LocalDate.parse(cleanDayKey.take(10)) }.getOrElse { LocalDate.now() }
                val photoCount = runCatching {
                    val raw = JSONObject(e.rawJson)
                    val photos = raw.optJSONArray("photos")
                    if (photos == null) 0 else (0 until photos.length()).count { index ->
                        val photo = photos.optJSONObject(index)
                        val id = photo?.optString("id").orEmpty()
                        id.isNotBlank() && !photo.optBoolean("deleted", false)
                    }
                }.getOrDefault(0)
                BodyWeightLogItem(
                    id = e.id,
                    kg = e.kg.toFloat(),
                    dayKey = parsedDate.toString(),
                    dateLabelHu = parsedDate.format(dotFormatter),
                    photoCount = photoCount
                )
            }
    }

    internal fun persistProgramsToRoomAsync(
        profile: JSONObject? = null,
        completeOnboarding: Boolean = false,
        proposedPrograms: List<TrainingProgramModel>? = null,
        profileProgramToWrite: String? = null,
        profileSourceTemplateId: String? = null,
        activatedProgramId: String? = null,
        trainingStyle: String? = null,
        progressionMode: String? = null,
        onSaveResult: ((Boolean) -> Unit)? = null
    ) {
        val db = nativeDatabase
        val scope = nativeScope
        val currentPrograms = proposedPrograms ?: _uiState.value.programs
        if (db == null || scope == null || currentPrograms.isEmpty()) {
            onSaveResult?.invoke(false)
            return
        }
        val nowMs = System.currentTimeMillis()
        val revision = ++programWriteRevision
        programWriteError = null
        val context = nativeContext ?: run { onSaveResult?.invoke(false); return }
        val ticket = try { com.repforge.app.NativeDataReplacement.continuationForCurrentData(context) }
            catch (error: Exception) {
                programWriteError = error.message ?: "A terv nem menthető."
                _uiState.update { it.copy(statusBannerMessage = programWriteError) }
                onSaveResult?.invoke(false)
                return
            }
        programWriteJob = scope.launch(Dispatchers.IO) {
            programWriteMutex.withLock {
            if (revision != programWriteRevision) {
                onSaveResult?.invoke(false)
                return@withLock
            }
            runCatching {
                com.repforge.app.NativeDataReplacement.mutate(context) {
                ticket.requireCurrent(context)
                if (activatedProgramId != null && db.programDao().getProgramWithDaysById(activatedProgramId) == null) {
                    val rows = db.programDao().getProgramsWithDays()
                    val activeId = nativeSettingsStore?.current()?.activeProgramId.orEmpty()
                    val exercisesMap = _uiState.value.exercises.associateBy { it.id }
                    val reloaded = mapProgramRows(rows.map { it.program }, rows.flatMap { it.days }, exercisesMap, activeId)
                    _uiState.update { st ->
                        st.copy(
                            programs = reloaded,
                            editingProgramId = reloaded.firstOrNull { it.isActive }?.id ?: reloaded.firstOrNull()?.id.orEmpty(),
                            statusBannerMessage = tr(
                                "A kiválasztott program már nem található.",
                                "Selected program was not found.",
                                "Das ausgewählte Programm wurde nicht gefunden.",
                                "Programul selectat nu a mai fost găsit.",
                                st.language
                            )
                        )
                    }
                    onSaveResult?.invoke(false)
                    return@mutate
                }
                val modeUpdate = if (trainingStyle != null || progressionMode != null) {
                    checkNotNull(nativeSettingsStore).prepareStarterProgramModes(
                        currentPrograms.first { it.isActive }.id, trainingStyle, progressionMode
                    )
                } else null
                val builtinCatalogMeta = runCatching { TrainPilotDatabase.defaultPrograms().first.associateBy { it.id } }
                    .getOrDefault(emptyMap())
                currentPrograms.forEachIndexed { pIdx, prog ->
                    if (proposedPrograms != null && prog.id != profileProgramToWrite) return@forEachIndexed
                    val existing = db.programDao().getProgramWithDaysById(prog.id)?.program
                    val persistedDesc = if (!prog.isCustom) {
                        existing?.description.orEmpty()
                    } else prog.subtitleHu
                    val rawProgJson = JSONObject(existing?.rawJson ?: "{}").apply {
                        put("id", prog.id)
                        put("nameHu", prog.nameHu)
                        put("nameEn", prog.nameEn)
                        put("nameDe", prog.nameDe)
                        put("nameRo", prog.nameRo)
                        put("description", persistedDesc)
                        put("isActive", prog.isActive)
                        put("isCustom", prog.isCustom)
                    }
                    val created = ProgramEntity(
                        id = prog.id,
                        nameHu = prog.nameHu,
                        nameEn = prog.nameEn,
                        nameDe = prog.nameDe.ifBlank { prog.nameEn.ifBlank { prog.nameHu } },
                        nameRo = prog.nameRo.ifBlank { prog.nameEn.ifBlank { prog.nameHu } },
                        location = profile?.takeIf { prog.id == profileProgramToWrite || prog.id.startsWith("custom_plan_") }
                            ?.optString("location")?.takeIf(String::isNotBlank)
                            ?: builtinCatalogMeta[prog.id]?.location?.takeIf(String::isNotBlank)
                            ?: prog.tagsHu.firstOrNull() ?: "Otthon / Edzőterem",
                        level = profile?.takeIf { prog.id == profileProgramToWrite || prog.id.startsWith("custom_plan_") }
                            ?.optString("experience")?.takeIf(String::isNotBlank)
                            ?: builtinCatalogMeta[prog.id]?.level?.takeIf(String::isNotBlank)
                            ?: prog.tagsHu.getOrNull(1) ?: "Kezdő",
                        description = persistedDesc,
                        builtin = !prog.isCustom,
                        generated = prog.id.startsWith("custom_plan_"),
                        sourceType = if (prog.isCustom) "user" else "builtin",
                        sourceTemplateId = profileSourceTemplateId,
                        templateVersion = 1,
                        accentHex = "#D8B36A",
                        sortOrder = pIdx,
                        updatedAt = nowMs,
                        rawJson = rawProgJson.toString()
                    )
                    val progEntity = existing?.copy(
                        nameHu = prog.nameHu,
                        nameEn = prog.nameEn,
                        nameDe = prog.nameDe.ifBlank { existing.nameDe.ifBlank { prog.nameEn } },
                        nameRo = prog.nameRo.ifBlank { existing.nameRo.ifBlank { prog.nameEn } },
                        description = persistedDesc,
                        sortOrder = pIdx,
                        updatedAt = nowMs,
                        rawJson = rawProgJson.toString()
                    ) ?: created
                    val progDays = prog.days.mapIndexed { dIdx, day ->
                        val dayKey = day.id.substringAfter("${prog.id}:").ifBlank { "day_${dIdx + 1}" }
                        val compositeId = if (day.id.contains(":")) day.id else "${prog.id}:$dayKey"
                        val exArr = JSONArray()
                        day.exercises.forEach { ex ->
                            exArr.put(
                                JSONObject().apply {
                                    put("id", ex.id)
                                    put("sets", ex.defaultSets)
                                    put("reps", ex.targetRepsText.ifBlank { ex.defaultReps.toString() })
                                    put("weight", ex.defaultWeightKg.toDouble())
                                    if (proposedPrograms != null && ex.defaultWeightKg == 0f &&
                                        com.repforge.app.NativeProgramPrescription.allowsTrial(ex.loadType)) put("trial", true)
                                    put("rest", ex.defaultRestSec)
                                }
                            )
                        }
                        ProgramDayEntity(
                            compositeId = compositeId,
                            programId = prog.id,
                            dayId = dayKey,
                            nameHu = day.titleHu,
                            nameEn = day.titleEn,
                            nameDe = day.titleDe.ifBlank { day.titleEn.ifBlank { day.titleHu } },
                            nameRo = day.titleRo.ifBlank { day.titleEn.ifBlank { day.titleHu } },
                            sortOrder = dIdx,
                            exercisesJson = exArr.toString()
                        )
                    }
                    db.programDao().saveProgramWithDays(progEntity, progDays)
                }
                if (profile == null) currentPrograms.firstOrNull { it.isActive }?.let { nativeSettingsStore?.setActiveProgramId(it.id) }
                if (profile != null) {
                    val store = checkNotNull(nativeSettingsStore)
                    val merged = JSONObject(store.current().profileJson)
                    profile.keys().forEach { key -> merged.put(key, profile.get(key)) }
                    val active = currentPrograms.first { it.isActive }
                    check(db.programDao().getProgramWithDaysById(active.id)?.days?.isNotEmpty() == true)
                    store.saveStarterProfileAndActivate(merged, active.id, completeOnboarding, ticket, modeUpdate)
                    _uiState.update { it.copy(profileComplete = true, showPersonalPlannerModal = false,
                        statusBannerMessage = tr(
                            "Profil mentve, a terv aktív.",
                            "Profile saved and plan activated.",
                            "Profil gespeichert und Plan aktiviert.",
                            "Profil salvat și plan activat.",
                            it.language
                        )) }
                }
                }
            }.onSuccess { onSaveResult?.invoke(true) }
            .onFailure { failure ->
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                onSaveResult?.invoke(false)
                val errMsg = tr(
                    "A terv/profil mentése nem sikerült. Próbáld újra.",
                    "Failed to save plan/profile. Please try again.",
                    "Speichern von Plan/Profil fehlgeschlagen. Bitte erneut versuchen.",
                    "Salvarea planului/profilului a eșuat. Încearcă din nou."
                )
                if (revision == programWriteRevision) programWriteError = errMsg
                _uiState.update { it.copy(statusBannerMessage = errMsg) }
            }
            }
        }
    }

    internal fun persistCalendarStateToRoomAsync() {
        val db = nativeDatabase ?: return
        val scope = nativeScope ?: return
        val context = nativeContext ?: return
        val ticket = calendarProjectionTicket ?: return
        val state = _uiState.value
        val events = state.calendarEvents
        val undoStack = state.calendarUndoStack
        val activeProg = state.programs.firstOrNull { it.isActive } ?: state.programs.firstOrNull()
        val nowMs = System.currentTimeMillis()
        val revision = ++calendarWriteRevision
        calendarWriteError = null

        fun calendarUiJson(event: CalendarDayEvent) = JSONObject()
            .put("id", event.id).put("dayOfMonth", event.dayOfMonth).put("dateIso", event.dateIso)
            .put("timeLabel", event.timeLabel).put("titleHu", event.titleHu).put("exerciseCount", event.exerciseCount)
            .put("durationMinutes", event.durationMinutes).put("status", event.status.name)
            .put("splitBadge", event.splitBadge).put("programId", event.programId).put("dayId", event.dayId)

        calendarWriteJob = scope.launch(Dispatchers.IO) {
            try {
                calendarWriteMutex.withLock {
                    if (revision != calendarWriteRevision) return@withLock
                    programWriteJob?.join()
                    check(programWriteError == null) { requireNotNull(programWriteError) }
                    NativeDataReplacement.mutate(context) {
                        ticket.requireCurrent(context)
                        db.withTransaction {
                            val stateKey = com.repforge.app.NativeCalendarBackup.STATE_KEY
                            val oldDocument = db.keyValueDocumentDao().getDocument(stateKey)
                            val previous = oldDocument?.json?.let { JSONObject(it).optJSONArray("events") } ?: JSONArray()
                            val previousById = (0 until previous.length()).mapNotNull { previous.optJSONObject(it) }
                                .associateBy { it.optString("id") }
                            val fallbackMonth = YearMonth.parse(state.calendarYearMonth)
                            val currentRows = events.associateBy({ it.id }, ::calendarUiJson)
                            for (id in previousById.keys - currentRows.keys) {
                                val existing = db.calendarScheduleDao().getEntry(id) ?: continue
                                if (existing.status.equals("completed", ignoreCase = true) ||
                                    db.workoutHistoryDao().hasScheduleHistory(id)) continue
                                val stamp = maxOf(nowMs, existing.updatedAt + 1)
                                val cancelled = runCatching { JSONObject(existing.rawJson) }.getOrElse { JSONObject() }
                                    .put("status", "skipped").put("cancelled", true).put("updatedAt", stamp)
                                db.calendarScheduleDao().upsertEntry(existing.copy(status = "skipped",
                                    updatedAt = stamp, rawJson = cancelled.toString()))
                            }
                            val changedRows = events.mapNotNull { event ->
                                val existing = db.calendarScheduleDao().getEntry(event.id)
                                val rawUi = currentRows.getValue(event.id)
                                val previousUi = previousById[event.id]
                                if (existing != null && previousUi != null &&
                                    rawUi.keys().asSequence().all { rawUi.opt(it) == previousUi.opt(it) }
                                ) return@mapNotNull null

                                val fallbackDate = fallbackMonth.atDay(event.dayOfMonth.coerceIn(1, fallbackMonth.lengthOfMonth()))
                                val resolvedDate = event.dateIso.ifBlank { existing?.date.orEmpty() }
                                val date = runCatching { LocalDate.parse(resolvedDate) }.getOrDefault(fallbackDate).toString()
                                val (start, end) = com.example.model.StudioCalendarDates.window(
                                    event.copy(dateIso = date), ZoneId.systemDefault())
                                val status = when (event.status) {
                                    CalendarEventStatus.COMPLETED -> "completed"
                                    CalendarEventStatus.MISSED -> "skipped"
                                    else -> "planned"
                                }
                                val programId = event.programId.ifBlank { existing?.programId.orEmpty() }.ifBlank { activeProg?.id.orEmpty() }
                                val program = state.programs.firstOrNull { it.id == programId }
                                val requestedDayId = event.dayId.ifBlank { existing?.dayId.orEmpty() }.ifBlank { event.splitBadge }
                                val day = program?.days?.firstOrNull { it.id.removePrefix("$programId:").equals(requestedDayId, true) }
                                val dayId = day?.id?.removePrefix("$programId:") ?: requestedDayId
                                val programName = program?.nameHu ?: existing?.programName ?: event.titleHu
                                val dayName = day?.titleHu ?: existing?.dayName ?: event.titleHu
                                val stamp = maxOf(nowMs, (existing?.updatedAt ?: 0L) + 1)
                                val raw = JSONObject(rawUi.toString()).put("dateIso", date).put("programId", programId)
                                    .put("programName", programName).put("dayId", dayId).put("dayName", dayName)
                                    .put("updatedAt", stamp)
                                CalendarScheduleEntity(
                                    id = event.id, googleEventId = existing?.googleEventId.orEmpty(), date = date,
                                    startIso = start.toString(), endIso = end.toString(), programId = programId,
                                    programName = programName, dayId = dayId, dayName = dayName, status = status,
                                    updatedAt = stamp, rawJson = raw.toString()
                                )
                            }
                            if (changedRows.isNotEmpty()) db.calendarScheduleDao().upsertEntries(changedRows)
                            val document = JSONObject().put("events", JSONArray(currentRows.values.toList()))
                                .put("undoStack", JSONArray(undoStack.map { snapshot -> JSONArray(snapshot.map(::calendarUiJson)) }))
                                .put("updatedAt", nowMs)
                            db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity(stateKey, document.toString(), nowMs))
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (revision == calendarWriteRevision) {
                    calendarWriteError = error.message ?: "A naptármódosítás nem menthető."
                    _uiState.update { it.copy(statusBannerMessage = "A naptármódosítás nem menthető. Próbáld újra.") }
                }
            }
        }
    }

    internal fun calendarProjectionCurrent(): Boolean {
        val context = nativeContext ?: return true
        val ticket = calendarProjectionTicket
        if (ticket != null && runCatching { ticket.requireCurrent(context) }.isSuccess) return true
        showBannerMessage(studioModesText(
            "Várd meg, amíg a naptár frissül.", "Wait for the calendar to finish refreshing.",
            "Warte, bis der Kalender aktualisiert ist.", "Așteaptă actualizarea calendarului."))
        return false
    }

    internal fun persistStudioPlannerSettings() {
        val context = nativeContext ?: return
        val db = nativeDatabase ?: return
        val scope = nativeScope ?: return
        val state = _uiState.value
        val revision = ++plannerSettingsRevision
        val ticket = calendarProjectionTicket ?: return
        plannerSettingsWriteError = null
        plannerSettingsWriteJob = scope.launch(Dispatchers.IO) {
            try {
                plannerSettingsWriteMutex.withLock {
                    if (revision != plannerSettingsRevision) return@withLock
                    NativeDataReplacement.mutate(context) {
                        ticket.requireCurrent(context)
                        val dao = db.keyValueDocumentDao()
                        val key = com.repforge.app.NativeCalendarBackup.STUDIO_SETTINGS_KEY
                        val stamp = maxOf(System.currentTimeMillis(), (dao.getDocument(key)?.updatedAt ?: 0L) + 1)
                        val payload = JSONObject().put("cadenceHu", state.planningCadenceHu)
                            .put("firstDayHu", state.planningFirstDayHu).put("startDate", state.planningStartDate)
                            .put("time", state.planningTime).put("durationMin", state.planningDurationMin)
                            .put("weeksCount", state.planningWeeksCount).put("updatedAt", stamp)
                        dao.putDocument(KeyValueDocumentEntity(key, payload.toString(), stamp))
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (revision == plannerSettingsRevision) {
                    val message = studioModesText("A tervezési beállítások nem menthetők.", "Could not save planner settings.",
                        "Planungseinstellungen konnten nicht gespeichert werden.", "Setările planificatorului nu au putut fi salvate.")
                    plannerSettingsWriteError = error.message ?: message
                    showBannerMessage(message)
                }
            }
        }
    }

    private fun mapCatalogPrograms(exercisesMap: Map<String, ExerciseItem>): List<TrainingProgramModel> {
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        return mapProgramRows(programs, days, exercisesMap)
    }

    internal fun mapProgramRows(programs: List<ProgramEntity>, days: List<ProgramDayEntity>,
        exercisesMap: Map<String, ExerciseItem>, activeId: String? = null): List<TrainingProgramModel> {
        val explicitActive = programs.any { runCatching { JSONObject(it.rawJson).optBoolean("isActive") }.getOrDefault(false) }
        val (builtinProgs, builtinDays) = runCatching { TrainPilotDatabase.defaultPrograms() }.getOrElse { emptyList<ProgramEntity>() to emptyList<ProgramDayEntity>() }
        val builtinProgById = builtinProgs.associateBy { it.id }
        val builtinDayByComposite = builtinDays.associateBy { it.compositeId }
        return programs.mapIndexed { pIdx, prog ->
                val catalogProg = if (prog.builtin) builtinProgById[prog.id] else null
                val strippedNameHu = prog.nameHu.trim()
                    .removeSuffix(" (személyre szabott)")
                    .removeSuffix(" – másolat")
                    .removeSuffix(" - másolat")
                    .removeSuffix(" (másolat)")
                    .trim()
                val homeBasicAliases = setOf(
                    "otthoni a/b – alap", "otthoni a/b - alap", "otthoni alap a/b",
                    "home a/b – basic", "home a/b - basic", "home a/b – core",
                    "zuhause a/b – basis", "zuhause a/b - basis",
                    "acasă a/b – bază", "acasă a/b - bază"
                )
                val copiedTemplate = if (!prog.builtin) {
                    prog.sourceTemplateId?.let { builtinProgById[it] }
                        ?: builtinProgs.firstOrNull {
                            it.nameHu.equals(strippedNameHu, ignoreCase = true) ||
                                it.nameEn.equals(strippedNameHu, ignoreCase = true) ||
                                it.nameDe.equals(strippedNameHu, ignoreCase = true) ||
                                it.nameRo.equals(strippedNameHu, ignoreCase = true) ||
                                (it.id == "home-basic" && strippedNameHu.lowercase() in homeBasicAliases)
                        }
                } else null
                val localizedTemplate = catalogProg ?: copiedTemplate
                val progDays = days.filter { it.programId == prog.id }.sortedWith(compareBy<ProgramDayEntity> { it.sortOrder }.thenBy { it.dayId }).mapIndexed { dIdx, dayEntity ->
                    val catalogDay = if (prog.builtin) builtinDayByComposite[dayEntity.compositeId]
                        else localizedTemplate?.let { tpl ->
                            builtinDayByComposite["${tpl.id}:${dayEntity.dayId}"]
                                ?: builtinDays.firstOrNull { it.programId == tpl.id && it.nameHu.equals(dayEntity.nameHu, ignoreCase = true) }
                        } ?: builtinDays.firstOrNull { it.nameHu.equals(dayEntity.nameHu, ignoreCase = true) }
                    val dayExercises = runCatching {
                        val recipes = JSONObject(prog.rawJson).optJSONObject("prescriptions") ?: JSONObject()
                        val arr = JSONArray(dayEntity.exercisesJson)
                        (0 until arr.length()).mapNotNull { i ->
                            val item = arr.get(i)
                            val id = if (item is JSONObject) item.optString("id") else item.toString()
                            val source = exercisesMap[id] ?: return@mapNotNull null
                            val rx = JSONObject(recipes.optJSONObject(id)?.toString() ?: "{}")
                            if (item is JSONObject) item.keys().forEach { key -> if (key != "id") rx.put(key, item.get(key)) }
                            com.repforge.app.NativeProgramPrescription.applyBuiltinTrial(prog.builtin, rx, source.loadType)
                            val target = rx.optString("reps", source.targetRepsText.ifBlank { source.defaultReps.toString() })
                            source.copy(defaultSets = rx.optInt("sets", source.defaultSets),
                                defaultReps = Regex("[0-9]+").find(target)?.value?.toIntOrNull() ?: source.defaultReps,
                                targetRepsText = target,
                                defaultWeightKg = rx.optDouble("weight", rx.optDouble("weightKg", source.defaultWeightKg.toDouble())).toFloat(),
                                defaultRestSec = rx.optInt("rest", rx.optInt("restSec", source.defaultRestSec)))
                        }
                    }.getOrElse { emptyList() }
                    val resolvedDayHu = dayEntity.nameHu.ifBlank { catalogDay?.nameHu.orEmpty() }.ifBlank { "Edzésnap ${dIdx + 1}" }
                    val rawDayEn = (catalogDay?.nameEn?.takeIf { dayEntity.nameHu == catalogDay.nameHu && (dayEntity.nameEn.isBlank() || dayEntity.nameEn == dayEntity.nameHu) }
                        ?: dayEntity.nameEn.takeIf { it.isNotBlank() && it != dayEntity.nameHu })
                        .orEmpty()
                    val resolvedDayEn = rawDayEn.ifBlank { com.example.model.translateProgramDayTitle(resolvedDayHu, AppLanguage.EN) }
                    val rawDayDe = (catalogDay?.nameDe?.takeIf { dayEntity.nameHu == catalogDay.nameHu && (dayEntity.nameDe.isBlank() || dayEntity.nameDe == dayEntity.nameHu || dayEntity.nameDe == dayEntity.nameEn) }
                        ?: dayEntity.nameDe.takeIf { it.isNotBlank() && it != dayEntity.nameHu && it != dayEntity.nameEn })
                        .orEmpty()
                    val resolvedDayDe = rawDayDe.ifBlank { com.example.model.translateProgramDayTitle(resolvedDayHu, AppLanguage.DE) }
                    val rawDayRo = (catalogDay?.nameRo?.takeIf { dayEntity.nameHu == catalogDay.nameHu && (dayEntity.nameRo.isBlank() || dayEntity.nameRo == dayEntity.nameHu || dayEntity.nameRo == dayEntity.nameEn) }
                        ?: dayEntity.nameRo.takeIf { it.isNotBlank() && it != dayEntity.nameHu && it != dayEntity.nameEn })
                        .orEmpty()
                    val resolvedDayRo = rawDayRo.ifBlank { com.example.model.translateProgramDayTitle(resolvedDayHu, AppLanguage.RO) }
                    com.example.model.ProgramDayModel(
                        id = dayEntity.compositeId,
                        dayCode = if (dIdx < 26) "${'A' + dIdx} nap" else "${dIdx + 1}. nap",
                        titleHu = resolvedDayHu,
                        titleEn = resolvedDayEn,
                        durationMinutes = 60,
                        exercises = dayExercises,
                        titleDe = resolvedDayDe,
                        titleRo = resolvedDayRo
                    )
                }
                val (locHu, lvlHu) = com.example.model.resolveProgramLocationLevel(
                    builtin = prog.builtin,
                    storedLocation = prog.location,
                    storedLevel = prog.level,
                    templateLocation = localizedTemplate?.location,
                    templateLevel = localizedTemplate?.level
                )
                val metaSource = localizedTemplate ?: prog
                val locEn = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "location", "en")
                    else com.example.model.translateProgramTag(locHu, AppLanguage.EN)
                val locDe = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "location", "de")
                    else com.example.model.translateProgramTag(locHu, AppLanguage.DE)
                val locRo = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "location", "ro")
                    else com.example.model.translateProgramTag(locHu, AppLanguage.RO)
                val lvlEn = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "level", "en")
                    else com.example.model.translateProgramTag(lvlHu, AppLanguage.EN)
                val lvlDe = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "level", "de")
                    else com.example.model.translateProgramTag(lvlHu, AppLanguage.DE)
                val lvlRo = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "level", "ro")
                    else com.example.model.translateProgramTag(lvlHu, AppLanguage.RO)

                val isAutoGeneratedDesc = prog.description.isBlank() ||
                    prog.description == localizedTemplate?.description ||
                    Regex("^.+?\\s*•\\s*.+?\\s*•\\s*\\d+\\s*(?:edzésnap|workout days|Trainingstage|zile de antrenament)$", RegexOption.IGNORE_CASE).matches(prog.description.trim())
                val customUserDesc = prog.description.takeIf {
                    !prog.builtin && !isAutoGeneratedDesc
                }
                val builtSubtitleHu = customUserDesc ?: "$locHu • $lvlHu • ${progDays.size} edzésnap"
                val builtSubtitleEn = customUserDesc?.let { com.example.model.translateProgramSubtitle(it, AppLanguage.EN) }
                    ?: "$locEn • $lvlEn • ${progDays.size} workout days"
                val builtSubtitleDe = customUserDesc?.let { com.example.model.translateProgramSubtitle(it, AppLanguage.DE) }
                    ?: "$locDe • $lvlDe • ${progDays.size} Trainingstage"
                val builtSubtitleRo = customUserDesc?.let { com.example.model.translateProgramSubtitle(it, AppLanguage.RO) }
                    ?: "$locRo • $lvlRo • ${progDays.size} zile de antrenament"

                val effectiveProgNameHu = if (prog.builtin && prog.id == "home-basic" && prog.nameHu.trim().lowercase() in homeBasicAliases) {
                    catalogProg?.nameHu ?: "Otthoni A/B"
                } else {
                    prog.nameHu
                }
                val resolvedNameHu = effectiveProgNameHu.ifBlank { catalogProg?.nameHu.orEmpty() }.ifBlank { prog.nameEn }.ifBlank { "Edzésterv ${pIdx + 1}" }
                val rawNameEn = (localizedTemplate?.nameEn?.takeIf { effectiveProgNameHu == localizedTemplate.nameHu && (prog.nameEn.isBlank() || prog.nameEn == prog.nameHu || prog.nameEn.trim().lowercase() in homeBasicAliases) }
                    ?: prog.nameEn.takeIf { it.isNotBlank() && it != prog.nameHu && !(prog.builtin && prog.id == "home-basic" && it.trim().lowercase() in homeBasicAliases) })
                    .orEmpty()
                val resolvedNameEn = rawNameEn.ifBlank { com.example.model.translateProgramTitle(resolvedNameHu, AppLanguage.EN) }
                val rawNameDe = (localizedTemplate?.nameDe?.takeIf { effectiveProgNameHu == localizedTemplate.nameHu && (prog.nameDe.isBlank() || prog.nameDe == prog.nameHu || prog.nameDe == prog.nameEn || prog.nameDe.trim().lowercase() in homeBasicAliases) }
                    ?: prog.nameDe.takeIf { it.isNotBlank() && it != prog.nameHu && it != prog.nameEn && !(prog.builtin && prog.id == "home-basic" && it.trim().lowercase() in homeBasicAliases) })
                    .orEmpty()
                val resolvedNameDe = rawNameDe.ifBlank { com.example.model.translateProgramTitle(resolvedNameHu, AppLanguage.DE) }
                val rawNameRo = (localizedTemplate?.nameRo?.takeIf { effectiveProgNameHu == localizedTemplate.nameHu && (prog.nameRo.isBlank() || prog.nameRo == prog.nameHu || prog.nameRo == prog.nameEn || prog.nameRo.trim().lowercase() in homeBasicAliases) }
                    ?: prog.nameRo.takeIf { it.isNotBlank() && it != prog.nameHu && it != prog.nameEn && !(prog.builtin && prog.id == "home-basic" && it.trim().lowercase() in homeBasicAliases) })
                    .orEmpty()
                val resolvedNameRo = rawNameRo.ifBlank { com.example.model.translateProgramTitle(resolvedNameHu, AppLanguage.RO) }

                TrainingProgramModel(
                    id = prog.id,
                    nameHu = resolvedNameHu,
                    nameEn = resolvedNameEn,
                    subtitleHu = builtSubtitleHu,
                    subtitleEn = builtSubtitleEn,
                    tagsHu = listOf(locHu, lvlHu),
                    isCustom = !prog.builtin,
                    isActive = if (activeId != null && programs.any { it.id == activeId }) prog.id == activeId
                        else if (explicitActive) JSONObject(prog.rawJson).optBoolean("isActive") else pIdx == 0,
                    days = progDays,
                    nameDe = resolvedNameDe,
                    nameRo = resolvedNameRo,
                    subtitleDe = builtSubtitleDe,
                    subtitleRo = builtSubtitleRo,
                    tagsEn = listOf(locEn, lvlEn),
                    tagsDe = listOf(locDe, lvlDe),
                    tagsRo = listOf(locRo, lvlRo)
                )
            }
    }

    internal fun targetSubScreenForExercise(ex: ExerciseItem?): SubScreen {
        if (ex == null) return SubScreen.ACTIVE_STRENGTH
        val isBi = ex.isBilateral ||
            ex.id.contains("side-plank", ignoreCase = true) ||
            ex.nameHu.contains("oldalsó plank", ignoreCase = true)
        return when {
            isBi -> SubScreen.ACTIVE_BILATERAL
            ex.isTimed -> SubScreen.ACTIVE_TIMED
            else -> SubScreen.ACTIVE_STRENGTH
        }
    }

    internal fun buildDefaultSetsForExercise(ex: ExerciseItem): List<ExerciseSetItem> {
        val count = ex.defaultSets.coerceIn(1, 6)
        val defaultSideSec = if (ex.isBilateral || ex.isTimed) ex.targetSeconds.coerceAtLeast(15) else 30
        return (1..count).map { setNum ->
            ExerciseSetItem(
                setNumber = setNum,
                weightKg = ex.defaultWeightKg,
                reps = ex.defaultReps.coerceAtLeast(1),
                rir = 2,
                completed = false,
                leftSec = defaultSideSec,
                rightSec = defaultSideSec,
                leftRecorded = false,
                rightRecorded = false,
                id = UUID.randomUUID().toString()
            )
        }
    }

    internal fun buildCanonicalSessionJsonFromState(state: TrainPilotUiState): JSONObject {
        val wId = state.activeWorkoutId.ifBlank { UUID.randomUUID().toString() }
        val startedMs = if (state.workoutStartedEpochMs > 0L) state.workoutStartedEpochMs else System.currentTimeMillis()
        val startedIso = Instant.ofEpochMilli(startedMs).toString()
        val workoutExs = state.activeWorkoutExercises
        val currentEx = workoutExs.getOrNull(state.activeExerciseIndex)
        val allSetsMap = state.activeSetsByExerciseId.toMutableMap()
        if (currentEx != null) {
            allSetsMap[currentEx.id] = state.activeSets
        }

        val exercisesArray = JSONArray()
        workoutExs.forEachIndexed { exIdx, exItem ->
            val setsForEx = allSetsMap[exItem.id] ?: buildDefaultSetsForExercise(exItem)
            val setsArray = JSONArray()
            setsForEx.forEachIndexed { idx, s ->
                setsArray.put(JSONObject().apply {
                    put("setId", s.id.ifBlank { "${exItem.id}_set_${s.setNumber}" })
                    put("set", idx + 1)
                    put("setNumber", s.setNumber)
                    put("weight", s.weightKg.toDouble())
                    put("reps", s.reps.toString())
                    put("done", s.completed)
                    if (s.actualRirProvided) put("actualRir", s.rir)
                    put("leftSec", s.leftSec)
                    put("rightSec", s.rightSec)
                    put("leftRecorded", s.leftRecorded)
                    put("rightRecorded", s.rightRecorded)
                })
            }
            exercisesArray.put(JSONObject().apply {
                put("id", exItem.id)
                put("exerciseId", exItem.id)
                put("name", exItem.nameHu)
                put("nameHu", exItem.nameHu)
                put("nameEn", exItem.nameEn)
                put("loadType", exItem.loadType.ifBlank { if (exItem.defaultWeightKg <= 0f) "bodyweight" else "weight" })
                put("repUnit", exItem.repUnit.ifBlank { when {
                    exItem.id == "outdoor_run_ex" && state.outdoorGpsEnabled -> "km"
                    exItem.isBilateral -> "mp/oldal"
                    exItem.isTimed -> "sec"
                    else -> "reps"
                } })
                put("measurementType", exItem.measurementType.ifBlank { when {
                    exItem.id == "outdoor_run_ex" && state.outdoorGpsEnabled -> "distance"
                    exItem.isBilateral -> "bilateral_time"
                    exItem.isTimed -> "time"
                    else -> "reps"
                } })
                put("orderIndex", exIdx)
                put("sets", setsArray)
            })
        }

        val title = state.activeWorkoutTitleHu.ifBlank { "Edzés" }
        return JSONObject().apply {
            put("schema", 1)
            put("active", state.isWorkoutSessionActive)
            put("id", wId)
            put("workoutId", wId)
            put("workoutKey", wId)
            put("syncId", wId)
            put("started", startedIso)
            put("startedEpochMs", startedMs)
            put("activeIntervals", JSONArray().put(JSONObject().put("start", startedIso).put("end", JSONObject.NULL)))
            put("programId", if (state.activeWorkoutSubScreen == SubScreen.ACTIVE_OUTDOOR_GPS) "outdoor" else state.editingProgramId.ifBlank { "program_custom" })
            put("programName", title)
            put("dayId", "session")
            put("dayName", title)
            put("workout", title)
            put("currentExercise", state.activeExerciseIndex.coerceAtLeast(0))
            put("timerOwner", if (state.phoneControlsWorkout) "phone" else "watch")
            put("originDevice", "phone")
            put("workoutPaused", false)
            if (state.activeWorkoutSubScreen == SubScreen.ACTIVE_OUTDOOR_GPS) {
                put("studioGpsKey", state.outdoorRouteKey)
                put("studioIndoor", !state.outdoorGpsEnabled)
            }
            put("restSeconds", state.defaultRestSec)
            put("restEndAt", if (state.restTimerRunning && state.restTimerEndEpochMs > 0L) state.restTimerEndEpochMs else 0L)
            put("exercises", exercisesArray)
        }
    }

    internal fun persistCanonicalActiveSessionAsync(installNew: Boolean = false) {
        val ctx = nativeContext ?: return
        val scope = nativeScope ?: return
        val state = _uiState.value
        if (!state.isWorkoutSessionActive) return
        val candidate = buildCanonicalSessionJsonFromState(state)
        scope.launch(Dispatchers.Main.immediate) {
            try {
                com.repforge.app.StudioWorkoutBridge.persist(ctx, candidate)
            } catch (failure: Exception) {
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                _uiState.update { it.copy(statusBannerMessage = failure.message ?: "Az edzés nem menthető.") }
            }
        }
    }

    fun pauseActiveWorkout(paused: Boolean = true): Boolean {
        val ctx = nativeContext ?: return !_uiState.value.isWorkoutSessionActive
        val current = ActiveWorkoutStore.current(ctx)?.takeIf { it.optBoolean("active") } ?: return true
        if (current.optString("timerOwner", "phone") == "watch" && !paused) {
            return try {
                WearSyncRepository.requestWatchWorkoutContinuation(ctx) { message -> showBannerMessage(message) }
                true
            } catch (failure: Exception) {
                showBannerMessage(failure.message ?: "Az órás folytatás nem kérhető.")
                false
            }
        }
        // A running GPS session and acknowledged watch owner intentionally continue in background.
        if (current.optString("timerOwner", "phone") != "phone" ||
            (_uiState.value.activeWorkoutSubScreen == SubScreen.ACTIVE_OUTDOOR_GPS &&
                com.repforge.app.DistanceTrackingService.snapshot(ctx).optBoolean("active"))) return true
        return try {
            com.repforge.app.StudioWorkoutBridge.pause(ctx, paused)
            _uiState.update { it.copy(homeDemoMode = if (paused) HomeDemoMode.PAUSED_WORKOUT else it.homeDemoMode) }
            true
        } catch (failure: Exception) {
            _uiState.update { it.copy(statusBannerMessage = failure.message ?: "A szünet nem menthető.") }
            false
        }
    }

    fun onAppBackground() {
        // Screen-off is not an explicit workout pause. The already armed foreground
        // recorder continues; never start/resume a clock from this lifecycle callback.
        val ctx = nativeContext ?: return
        runCatching {
            val frame = ActiveWorkoutStore.current(ctx) ?: return@runCatching
            if (frame.optBoolean("active") && frame.optString("timerOwner", "phone") == "phone" &&
                !frame.optBoolean("finalizationPending") && !com.repforge.shared.WorkoutOwnership193.pending(frame) &&
                PhoneOwnership193.phoneAllowed(ctx, frame.optString("workoutId"))) {
                PhoneWorkoutExecution.store(ctx).checkpointStopwatch(frame.optString("workoutId"),
                    frame.optLong("generation"), android.os.SystemClock.elapsedRealtime())
            }
        }
    }

    internal fun pauseWhenLeavingMeasurement(): Boolean =
        if (_uiState.value.currentSubScreen in setOf(SubScreen.ACTIVE_STRENGTH, SubScreen.ACTIVE_TIMED, SubScreen.ACTIVE_BILATERAL, SubScreen.ACTIVE_OUTDOOR_GPS))
            pauseActiveWorkout() else true

    init {
        val catalogTraceStarted = runCatching { android.os.Trace.beginSection("TrainPilot.catalogProjection"); true }.getOrDefault(false)
        try {
            val allExs = TrainPilotDatabase.defaultExercises().map { mapExerciseEntity(it) }
            val exMap = allExs.associateBy { it.id }
            val allProgs = mapCatalogPrograms(exMap)
            if (allExs.isNotEmpty() && allProgs.isNotEmpty()) {
                val firstProg = allProgs.firstOrNull { it.isActive } ?: allProgs.first()
                val firstDay = firstProg.days.firstOrNull()
                val initialWorkoutExs = firstDay?.exercises?.ifEmpty { allExs.take(6) } ?: allExs.take(6)
                val firstEx = initialWorkoutExs.firstOrNull() ?: allExs.first()
                _uiState.update {
                    it.copy(
                        exercises = allExs,
                        programs = allProgs,
                        editingProgramId = firstProg.id,
                        activeWorkoutTitleHu = "${firstProg.nameHu} • ${firstDay?.dayCode ?: "A nap"}",
                        activeWorkoutExercises = initialWorkoutExs,
                        activeExerciseIndex = 0,
                        selectedExerciseId = firstEx.id,
                        activeSets = buildDefaultSetsForExercise(firstEx)
                    )
                }
            }
        } catch (_: Throwable) {}
        finally { if (catalogTraceStarted) runCatching { android.os.Trace.endSection() } }
    }

    fun bindNativeEnvironment(
        context: Context,
        settingsStore: TrainPilotSettingsStore,
        database: TrainPilotDatabase,
        scope: CoroutineScope
    ) {
        nativeContext = context
        nativeSettingsStore = settingsStore
        nativeDatabase = database
        nativeScope = scope

        // Replace observers on an explicit rebind; cloud refresh only reloads data.
        nativeObserversJob?.cancel()
        val observersJob = SupervisorJob(scope.coroutineContext[Job])
        nativeObserversJob = observersJob
        val observersScope = CoroutineScope(scope.coroutineContext + observersJob)
        observersScope.launch {
            val repository = com.repforge.app.ui.home.NativeHomeRepository(database)
            kotlinx.coroutines.flow.combine(repository.invalidations(),
                settingsStore.settingsFlow.map { it.activeProgramId to it.language }.distinctUntilChanged(),
                // 2773: re-select at local midnight (today's plan stays all day, then the next one).
                com.repforge.app.ui.home.NativeHomeRepository.dayChanges()
            ) { _, settings, _ -> settings }.collect { (activeId, language) ->
                try {
                    val ticket = NativeDataReplacement.continuationForCurrentData(context)
                    val snapshot = withContext(Dispatchers.IO) {
                        repository.read(activeId, language, Instant.now(), ZoneId.systemDefault())
                    }
                    ticket.requireCurrent(context)
                    if (nativeDatabase === database) _uiState.update { it.copy(homeWorkoutTarget = snapshot.target) }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Keep last valid target while a dataset is being replaced. */ }
            }
        }
        observersScope.launch {
            settingsStore.settingsFlow.distinctUntilChangedBy { listOf(it.fontScaleMultiplier,
                it.compactPhoneMode, it.workSeconds, it.intervalRestSeconds, it.rounds,
                it.intervalAutoAdvance, it.soundEnabled, it.vibrationEnabled, it.restSeconds) }
                .collect { saved ->
                    _uiState.update { it.copy(fontScaleMultiplier = saved.fontScaleMultiplier,
                        compactPhoneMode = saved.compactPhoneMode, intervalWorkSec = saved.workSeconds,
                        intervalRestSec = saved.intervalRestSeconds, intervalRounds = saved.rounds,
                        autoStepIntervals = saved.intervalAutoAdvance, defaultRestSec = saved.restSeconds,
                        soundAlertEnabled = saved.soundEnabled, vibrationEnabled = saved.vibrationEnabled) }
                }
        }

        observersScope.launch {
            settingsStore.settingsFlow.map { it.healthBackupConsent }.distinctUntilChanged().collect { consent ->
                _uiState.update { it.copy(includeHealthInBackup = consent) }
            }
        }

        observersScope.launch {
            settingsStore.settingsFlow.map { it.trainingModes197Json to it.activeProgramId }
                .distinctUntilChanged().collect { (raw, programId) ->
                    val global = com.repforge.app.data.store.NativeTrainingModes.globalChoice(raw)
                    val program = com.repforge.app.data.store.NativeTrainingModes.resolveSavedJson(raw, programId)
                    val changes = com.repforge.app.data.store.NativeTrainingModeChange
                    _uiState.update { it.copy(
                        trainingStyleHu = if (global.trainingStyle == "hit") "HIT-inspirált" else "Normál hipertrófia",
                        progressionRuleHu = when (global.progressionMode) {
                            "linear" -> "Lineáris progresszió"
                            "rir" -> "RIR/Coach"
                            else -> "Dupla progresszió"
                        },
                        trainingStyleCode = global.trainingStyle,
                        progressionModeCode = global.progressionMode,
                        programTrainingStyleCode = program.trainingStyle,
                        programProgressionModeCode = program.progressionMode,
                        programTrainingModeSource = program.source,
                        activeTrainingModeProgramId = programId,
                        globalTrainingModeUndo = changes.undoValue(raw, "global", null) != null,
                        programTrainingModeUndo = changes.undoValue(raw, "program", programId) != null
                    ) }
                }
        }

        observersScope.launch(Dispatchers.Main.immediate) {
            val warnedUnknownSelections = mutableSetOf<Pair<String, String?>>()
            var lastAppliedRemoteRestEndAt = 0L
            var lastAppliedWorkoutId = ""
            ActiveWorkoutStore.snapshotFlow.collect { frame ->
                if (frame?.optBoolean("active") != true) {
                    lastAppliedRemoteRestEndAt = 0L
                    lastAppliedWorkoutId = ""
                    val before = _uiState.value
                    val remoteClose = frame != null && before.isWorkoutSessionActive && !before.isSavingWorkout &&
                        before.activeWorkoutId == frame.optString("workoutId")
                    if (frame != null) _uiState.update { state ->
                        if (state.activeWorkoutId != frame.optString("workoutId")) state
                        else state.copy(isWorkoutSessionActive = false, pendingIntervalStart = null, activeWorkoutId = "", activeWorkoutGeneration = 0L, workoutStartedEpochMs = 0L,
                            activeWorkoutExercises = emptyList(), activeSets = emptyList(), activeSetsByExerciseId = emptyMap(),
                            timedCurrentSec = 0, restTimerRunning = false, restTimerEndRealtimeMs = 0L, restTimerEndEpochMs = 0L, restTimerLastTickRealtimeMs = 0L,
                            homeDemoMode = HomeDemoMode.PLANNED_NEXT,
                            currentSubScreen = if (state.currentSubScreen in activeMeasurementScreens) SubScreen.NONE else state.currentSubScreen,
                            backStack = state.backStack.filterNot { it.subScreen in activeMeasurementScreens })
                    }
                    // A remote native finalizer has committed history before publishing closure.
                    // Refresh the same Room projection once; local save already updates its UI.
                    if (remoteClose && !NativeDataReplacement.blocked(context))
                        refreshClosedWorkoutHistory(context, database, observersScope)
                } else {
                    val canonicalId = frame.optJSONArray("exercises")
                        ?.optJSONObject(frame.optInt("currentExercise", -1))?.optString("id")?.takeIf { it.isNotBlank() }
                    _uiState.update { state ->
                        val incomingId = frame.optString("workoutId")
                        if (incomingId.isBlank()) return@update state
                        val adopting = state.activeWorkoutId != incomingId
                        if (adopting && (incomingId.isBlank() || state.isWorkoutSessionActive ||
                            frame.optString("timerOwner") != "watch" || frame.optBoolean("finalizationPending") ||
                            frame.optBoolean("discardPending") || com.repforge.shared.WorkoutOwnership193.pending(frame))) return@update state
                        val rawExercises = frame.optJSONArray("exercises") ?: JSONArray()
                        val watchProjection = frame.optString("timerOwner") == "watch"
                        if (watchProjection && (rawExercises.length() == 0 ||
                            (0 until rawExercises.length()).any { rawExercises.optJSONObject(it)?.optString("id").isNullOrBlank() } ||
                            frame.optInt("currentExercise", -1) !in 0 until rawExercises.length())) return@update state
                        val projectedExercises = if (adopting || watchProjection) (0 until rawExercises.length()).map { index ->
                            val raw = rawExercises.getJSONObject(index)
                            val id = raw.getString("id")
                            val catalog = state.exercises.firstOrNull { it.id == id }
                                ?: ExerciseItem(id = id, nameHu = raw.optString("name", id))
                            com.example.ui.components.withNativeStrengthUnits(catalog,
                                raw.opt("loadType") as? String, raw.opt("repUnit") as? String).copy(
                                measurementType = raw.optString("measurementType"),
                                isTimed = com.repforge.app.NativeSetStopwatch.timed(raw),
                                isBilateral = com.repforge.app.NativeSetStopwatch.bilateral(raw))
                        } else state.activeWorkoutExercises
                        if ((adopting || watchProjection) && (projectedExercises.isEmpty() || projectedExercises.map { it.id }.distinct().size != projectedExercises.size)) return@update state
                        val nativeSets = mutableMapOf<String, List<ExerciseSetItem>>()
                        val exercises = frame.optJSONArray("exercises") ?: JSONArray()
                        for (index in 0 until exercises.length()) {
                            val exercise = exercises.getJSONObject(index)
                            val rows = exercise.optJSONArray("sets") ?: JSONArray()
                            nativeSets[exercise.optString("id")] = (0 until rows.length()).map { rowIndex ->
                                val row = rows.getJSONObject(rowIndex)
                                ExerciseSetItem(setNumber = row.optInt("setNumber", rowIndex + 1),
                                    weightKg = row.optDouble("weight", 0.0).toFloat(), reps = row.optString("reps").toDoubleOrNull()?.toInt() ?: 0,
                                    rir = row.optInt("actualRir", 2), completed = row.optBoolean("done"),
                                    id = row.optString("setId"), actualRirProvided = row.has("actualRir") && !row.isNull("actualRir"),
                                    leftSec = row.optInt("leftSeconds", row.optInt("leftSec", 0)),
                                    rightSec = row.optInt("rightSeconds", row.optInt("rightSec", 0)),
                                    leftRecorded = row.optInt("leftSeconds") > 0 || row.optBoolean("leftRecorded"),
                                    rightRecorded = row.optInt("rightSeconds") > 0 || row.optBoolean("rightRecorded"))
                            }
                        }
                        val canonicalIndex = canonicalId?.let { id ->
                            projectedExercises.indices.singleOrNull { projectedExercises[it].id == id }
                        }
                        val selectedIndex = canonicalIndex ?: if (adopting) 0 else state.activeExerciseIndex
                        val selected = projectedExercises.getOrNull(selectedIndex)
                        val exerciseChanged = canonicalIndex != null &&
                            selected?.id != state.activeWorkoutExercises.getOrNull(state.activeExerciseIndex)?.id
                        // Follow a committed selection without replaying navigation or resetting a mode
                        // chosen for the same exercise. Outdoor keeps its dedicated GPS/indoor screen.
                        val targetScreen = if ((adopting || exerciseChanged) && (adopting || state.activeWorkoutSubScreen != SubScreen.ACTIVE_OUTDOOR_GPS))
                            targetSubScreenForExercise(selected) else state.activeWorkoutSubScreen
                        var nextState = state.copy(isWorkoutSessionActive = true,
                            activeWorkoutId = incomingId,
                            activeWorkoutGeneration = frame.optLong("generation"),
                            activeWorkoutExercises = projectedExercises,
                            workoutStartedEpochMs = runCatching { Instant.parse(frame.getString("started")).toEpochMilli() }.getOrDefault(state.workoutStartedEpochMs),
                            activeWorkoutTitleHu = if (adopting) frame.optString("programName").ifBlank { frame.optString("dayName") } else state.activeWorkoutTitleHu,
                            homeDemoMode = if (adopting) HomeDemoMode.PAUSED_WORKOUT else state.homeDemoMode,
                            phoneControlsWorkout = frame.optString("timerOwner", "phone") == "phone" &&
                                !watchHandoffBusy && !com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(frame) &&
                                !com.repforge.app.NativeWatchHandoffReceipt.pending(frame, watchJournal()),
                            watchControlPending = watchHandoffBusy || com.repforge.app.NativeWatchHandoffReceipt.pending(frame, watchJournal()),
                            activeExerciseIndex = selectedIndex,
                            selectedExerciseId = if (state.currentSubScreen in setOf(SubScreen.EXERCISE_LIBRARY, SubScreen.EXERCISE_DETAIL))
                                state.selectedExerciseId else selected?.id ?: state.selectedExerciseId,
                            activeWorkoutSubScreen = targetScreen,
                            currentSubScreen = if (!adopting && exerciseChanged && state.currentSubScreen in activeMeasurementScreens &&
                                state.currentSubScreen != SubScreen.ACTIVE_OUTDOOR_GPS) targetScreen else state.currentSubScreen,
                            activeSetsByExerciseId = (if (adopting) emptyMap() else state.activeSetsByExerciseId) + nativeSets,
                            activeSets = nativeSets[selected?.id] ?: state.activeSets)
                        if (lastAppliedWorkoutId != incomingId) {
                            lastAppliedWorkoutId = incomingId
                            lastAppliedRemoteRestEndAt = 0L
                        }
                        val nowEpochMs = System.currentTimeMillis()
                        val nowRealtimeMs = runCatching { android.os.SystemClock.elapsedRealtime() }.getOrDefault(0L)
                        val autoObj = frame.optJSONObject("automation")
                        val autoRestEndAt = if (autoObj != null && autoObj.optString("phase") == "rest" && !autoObj.optBoolean("paused")) {
                            val deadline = autoObj.optLong("deadlineAt", 0L)
                            val remMs = autoObj.optLong("remainingMs", 0L)
                            when {
                                deadline > 0L -> deadline
                                remMs > 0L -> nowEpochMs + remMs
                                else -> 0L
                            }
                        } else 0L
                        val frameRestEndAt = if (frame.optBoolean("workoutPaused")) 0L else maxOf(frame.optLong("restEndAt", 0L), autoRestEndAt)
                        if (frameRestEndAt > nowEpochMs) {
                            if (kotlin.math.abs(frameRestEndAt - lastAppliedRemoteRestEndAt) > 1000L || !nextState.restTimerRunning) {
                                lastAppliedRemoteRestEndAt = frameRestEndAt
                                if (!nextState.restTimerRunning || kotlin.math.abs(frameRestEndAt - nextState.restTimerEndEpochMs) > 1500L) {
                                    nextState = com.repforge.app.StudioRestTimer.fromRemoteRestEndAt(nextState, frameRestEndAt, nowRealtimeMs, nowEpochMs)
                                }
                            }
                        } else if (lastAppliedRemoteRestEndAt > 0L && (frame.optLong("restEndAt", -1L) == 0L || watchProjection || frame.optBoolean("workoutPaused"))) {
                            lastAppliedRemoteRestEndAt = 0L
                            if (nextState.restTimerRunning) {
                                nextState = com.repforge.app.StudioRestTimer.skipped(nextState)
                            }
                        }
                        nextState
                    }
                    val displayed = _uiState.value
                    if (displayed.activeWorkoutId == frame.optString("workoutId") &&
                        displayed.activeWorkoutExercises.count { it.id == canonicalId } != 1 &&
                        warnedUnknownSelections.add(displayed.activeWorkoutId to canonicalId)) {
                        showBannerMessage("A szinkronizált gyakorlat nem található az aktív edzés nézetében. Nyisd meg újra az edzést.")
                    }
                }
            }
        }

        reloadNativeEnvironment(context, settingsStore, database, scope)
        com.repforge.app.WearPendingCommandRecovery.schedule(context.applicationContext)
    }

    /** Closure only needs a Room read; catalog seeding here would contend with restore admission. */
    private fun refreshClosedWorkoutHistory(context: Context, database: TrainPilotDatabase, scope: CoroutineScope) {
        val ticket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }.getOrNull() ?: return
        val previous = _uiState.value.loggedWorkouts
        val exercises = _uiState.value.exercises.associateBy { it.id }
        scope.launch(Dispatchers.IO) {
            try {
                val workouts = readNativeWorkoutHistory(database, exercises)
                val loosePhotoCount = com.repforge.app.NativeLoosePhotos.count(database)
                withContext(Dispatchers.Main.immediate) {
                    ticket.requireCurrent(context)
                    // A newer local save/edit or explicit rebind owns its own projection.
                    if (nativeDatabase !== database || _uiState.value.loggedWorkouts != previous) return@withContext
                    _uiState.update { it.copy(loggedWorkouts = workouts,
                        privatePhotoCount = workouts.sumOf { entry -> entry.photoCount } +
                            it.bodyWeightLogs.sumOf { weight -> weight.photoCount } + loosePhotoCount,
                        workoutGoalCompleted = workouts.workoutsInPeriodToDate(it.workoutGoalPeriod, LocalDate.now()).size,
                        weeklyGoalCompleted = workouts.workoutsInCalendarWeekToDate(LocalDate.now()).size) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Keep the prior projection on read failure or replacement. */ }
        }
    }

    private suspend fun readNativeWorkoutHistory(database: TrainPilotDatabase,
        exercisesMap: Map<String, ExerciseItem>): List<LoggedWorkoutEntry> {
        val dbWorkouts = database.workoutHistoryDao().getAllWorkoutsWithDetails()
        return dbWorkouts.map { detail ->
            val w = detail.workout
            val exerciseNames = detail.exercises.joinToString(", ") { exWithSets ->
                val ex = exWithSets.exercise
                ex.nameHu.ifBlank { exercisesMap[ex.exerciseId]?.nameHu.orEmpty() }.ifBlank { "Gyakorlat" }
            }
            val allSets = detail.exercises.flatMap { it.sets }
            val completedDbSets = allSets.filter { it.done }
            val isOutdoor = detail.exercises.any {
                val ex = it.exercise
                ex.measurementType == "distance" || ex.repUnit == "km"
            } || runCatching { JSONObject(w.rawJson).has(com.repforge.app.NativeGpsMeasurement.FIELD) }.getOrDefault(false)
            // Long pauses are already excluded from the canonical Room duration.
            val elapsedSec = w.durationSeconds.coerceAtLeast(0)
            val realDurationMin = if (elapsedSec > 0) (elapsedSec + 59) / 60 else 0
            val realWorkLabel = if (elapsedSec > 0) String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60) else "—"
            val mappedDetails = detail.exercises.map { exWithSets ->
                val ex = exWithSets.exercise
                val exName = ex.nameHu.ifBlank { exercisesMap[ex.exerciseId]?.nameHu.orEmpty() }.ifBlank { "Gyakorlat" }
                val mappedSets = exWithSets.sets.mapIndexed { sIdx, sEntity ->
                    ExerciseSetItem(
                        setNumber = if (sEntity.setNumber > 0) sEntity.setNumber else (sIdx + 1),
                        weightKg = (sEntity.weight ?: 0.0).toFloat(),
                        reps = sEntity.reps.toIntOrNull() ?: 0,
                        rir = sEntity.actualRir ?: 2, actualRirProvided = sEntity.actualRir != null,
                        completed = sEntity.done,
                        distanceMeters = sEntity.distanceMeters
                    )
                }
                com.example.model.LoggedExerciseDetail(
                    nameHu = exName,
                    summaryPrescription = if (isOutdoor || ex.measurementType == "distance") "Kültéri aktivitás" else "${mappedSets.count { it.completed }}/${mappedSets.size} sorozat",
                    sets = mappedSets,
                    measurementType = ex.measurementType,
                    repUnit = ex.repUnit
                )
            }
            LoggedWorkoutEntry(
                id = w.id,
                dateLabelHu = w.started.take(16).replace('T', ' '),
                titleHu = w.programName.ifBlank { w.dayName }.ifBlank { "Edzés" },
                programId = w.programId, dayId = if (w.quickWorkout) "" else w.dayId,
                dayName = if (w.quickWorkout) "" else w.dayName,
                scheduleId = w.scheduleId.orEmpty(),
                durationMinutes = realDurationMin,
                workTimeLabel = realWorkLabel,
                restTimeLabel = "—",
                totalSets = if (w.completedSetsCount > 0) w.completedSetsCount else completedDbSets.size,
                totalVolumeKg = if (w.totalVolumeKg > 0.0) w.totalVolumeKg.roundToInt() else completedDbSets.sumOf { (it.weight ?: 0.0) * (it.reps.toDoubleOrNull() ?: 0.0) }.roundToInt(),
                summaryExercisesHu = if (isOutdoor) w.programName.ifBlank { "Kültéri aktivitás" } else exerciseNames.ifBlank { "Rögzített gyakorlatok" },
                hasPr = w.hasPr226,
                prRecordsJson = w.prRecordsJson226,
                isOutdoorRun = isOutdoor,
                routeKeys = detail.exercises.flatMap { it.sets }.flatMap {
                    listOfNotNull(it.gpsRouteKey163, it.gpsRouteKey193)
                }.filter(String::isNotBlank).distinct(),
                sourceWatch = com.repforge.app.NativeSavedWearHealth.read(w) != null, sourceHealthConnect = !w.health240Json.isNullOrBlank(),
                distanceKm = (completedDbSets.sumOf { it.distanceMeters ?: 0.0 } / 1000.0).toFloat(),
                paceLabel = completedDbSets.sumOf { it.distanceMeters ?: 0.0 }.takeIf { it > 0 && elapsedSec > 0 }?.let { meters ->
                    val secondsPerKm = (elapsedSec / (meters / 1000.0)).toInt()
                    "%d:%02d /km".format(secondsPerKm / 60, secondsPerKm % 60)
                } ?: "—",
                originDevice = w.originDevice,
                savedStartIso = w.started,
                savedFinishIso = w.finished,
                photoCount = detail.photos.size,
                exerciseDetails = mappedDetails
            )
        }
    }

    private fun reloadNativeEnvironment(
        context: Context,
        settingsStore: TrainPilotSettingsStore,
        database: TrainPilotDatabase,
        scope: CoroutineScope
    ) {
        nativeReloadJob?.cancel()
        nativeReloadJob = scope.launch(Dispatchers.IO) {
            val projectionTicket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }.getOrNull() ?: return@launch
            val calendarRevisionAtRead = calendarWriteRevision
            calendarWriteJob?.join()
            val plannerRevisionAtRead = plannerSettingsRevision
            plannerSettingsWriteJob?.join()
            // Seed catalog into Room DB
            try {
                TrainPilotDatabase.ensureBuiltinCatalogSeeded(database, context)
            } catch (_: Throwable) {}

            val savedNativeSettings = settingsStore.current()
            val savedProfile = JSONObject(savedNativeSettings.profileJson)
            // 1. Detect if a paused or active workout is running natively
            try {
                val execution = withContext(Dispatchers.Main.immediate) { PhoneWorkoutExecution.store(context).snapshot() }
                val activeStore = ActiveWorkoutStore.current(context)
                val isPaused = (execution?.optBoolean("active") == true && execution.optBoolean("workoutPaused")) ||
                        (activeStore?.optBoolean("active") == true && activeStore.optBoolean("workoutPaused"))
                val isRunning = (execution?.optBoolean("active") == true && !execution.optBoolean("workoutPaused")) ||
                        (activeStore?.optBoolean("active") == true && !activeStore.optBoolean("workoutPaused"))

                if (isPaused || isRunning) {
                    _uiState.update { it.copy(homeDemoMode = HomeDemoMode.PAUSED_WORKOUT) }
                } else {
                    _uiState.update { it.copy(homeDemoMode = HomeDemoMode.PLANNED_NEXT) }
                }
            } catch (_: Throwable) {}

            // 2. Read all real exercises from Room ExerciseLibrary (106 exercises)
            val mappedExercises = try {
                val dbExercises = database.exerciseLibraryDao().getAllExercises()
                if (dbExercises.isNotEmpty()) {
                    dbExercises.map { mapExerciseEntity(it) }
                } else {
                    TrainPilotDatabase.defaultExercises().map { mapExerciseEntity(it) }
                }
            } catch (_: Throwable) {
                try {
                    TrainPilotDatabase.defaultExercises().map { mapExerciseEntity(it) }
                } catch (_: Throwable) { DemoDataProvider.defaultExercises }
            }

            val loadedFavoriteIds = runCatching { com.repforge.app.NativeExerciseFavorites.load(database).toSet() }
                .getOrDefault(emptySet())
            val exercisesMap = mappedExercises.associateBy { it.id }

            // 3. Read real programs and days from Room ProgramDao (with custom exercise prescriptions & active flag)
            val mappedPrograms = try {
                val dbPrograms = database.programDao().getProgramsWithDays()
                if (dbPrograms.isNotEmpty()) mapProgramRows(dbPrograms.map { it.program },
                    dbPrograms.flatMap { it.days }, exercisesMap, savedNativeSettings.activeProgramId)
                else mapCatalogPrograms(exercisesMap)
            } catch (_: Throwable) { mapCatalogPrograms(exercisesMap) }

            // 4. Read all body weight entries from Room sorted by user-entered dayKey
            var loadedWeightLogs = emptyList<BodyWeightLogItem>()
            var latestWeightKg = 0f
            var loadedLoosePhotoCount = 0
            try {
                val allWeights = database.bodyWeightDao().getAllWeights()
                loadedWeightLogs = mapBodyWeightEntitiesToLogItems(allWeights)
                val latestByDayKey = loadedWeightLogs.lastOrNull()?.kg
                val weightSummary = database.bodyWeightDao().getWeightJournalSummary()
                latestWeightKg = latestByDayKey ?: weightSummary.currentKg?.toFloat() ?: 0f
                loadedLoosePhotoCount = com.repforge.app.NativeLoosePhotos.count(database)
            } catch (_: Throwable) {}

            var loadedWeeklyGoal = 0
            var loadedStepsGoal = 0
            var loadedGoalPeriod = com.example.model.WorkoutGoalPeriod.WEEKLY
            try {
                val goalsDoc = database.keyValueDocumentDao().getDocument("native:userGoals")
                if (goalsDoc != null && goalsDoc.json.isNotBlank()) {
                    val gObj = JSONObject(goalsDoc.json)
                    loadedWeeklyGoal = if (gObj.has("workoutGoalTarget")) gObj.optInt("workoutGoalTarget", 0)
                        else gObj.optInt("weeklyGoalTarget", 0)
                    loadedStepsGoal = gObj.optInt("stepsGoal", 0)
                    val periodStr = gObj.optString("workoutGoalPeriod", "")
                    if (periodStr.isNotBlank()) {
                        loadedGoalPeriod = runCatching {
                            com.example.model.WorkoutGoalPeriod.valueOf(periodStr.uppercase())
                        }.getOrDefault(com.example.model.WorkoutGoalPeriod.WEEKLY)
                    }
                }
            } catch (_: Throwable) {}

            // 4b. Read persisted calendar schedule & undo stack from Room
            val plannerSettings = runCatching {
                database.keyValueDocumentDao().getDocument(com.repforge.app.NativeCalendarBackup.STUDIO_SETTINGS_KEY)?.json?.let(::JSONObject)
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                null
            }
            var loadedCalendarEvents = emptyList<CalendarDayEvent>()
            var loadedCalendarUndo = emptyList<List<CalendarDayEvent>>()
            try {
                val calDoc = database.keyValueDocumentDao().getDocument("native:calendarState")
                if (calDoc != null && calDoc.json.isNotBlank()) {
                    val root = JSONObject(calDoc.json)
                    val calendarRows = database.calendarScheduleDao().getAllEntries().associateBy { it.id }
                    val legacyMonth = YearMonth.from(Instant.ofEpochMilli(calDoc.updatedAt).atZone(ZoneId.systemDefault()))
                    val evArr = root.optJSONArray("events")
                    if (evArr != null) {
                        loadedCalendarEvents = (0 until evArr.length()).mapNotNull { idx ->
                            val o = evArr.optJSONObject(idx) ?: return@mapNotNull null
                            CalendarDayEvent(
                                id = o.optString("id", "ev_$idx"),
                                dayOfMonth = o.optInt("dayOfMonth", 1).coerceIn(1, 31),
                                timeLabel = o.optString("timeLabel", "10:00"),
                                titleHu = o.optString("titleHu", "Edzés"),
                                exerciseCount = o.optInt("exerciseCount", 6),
                                durationMinutes = o.optInt("durationMinutes", 60),
                                status = runCatching { CalendarEventStatus.valueOf(o.optString("status", "PLANNED")) }.getOrDefault(CalendarEventStatus.PLANNED),
                                splitBadge = o.optString("splitBadge", "A"),
                                programId = o.optString("programId").ifBlank { calendarRows[o.optString("id")]?.programId.orEmpty() },
                                dayId = o.optString("dayId").ifBlank { calendarRows[o.optString("id")]?.dayId.orEmpty() },
                                dateIso = o.optString("dateIso").ifBlank {
                                    calendarRows[o.optString("id")]?.date ?: legacyMonth.atDay(
                                        o.optInt("dayOfMonth", 1).coerceIn(1, legacyMonth.lengthOfMonth())).toString()
                                }
                            )
                        }
                    }
                    val undoArr = root.optJSONArray("undoStack")
                    if (undoArr != null) {
                        loadedCalendarUndo = (0 until undoArr.length()).mapNotNull { uIdx ->
                            val snapArr = undoArr.optJSONArray(uIdx) ?: return@mapNotNull null
                            (0 until snapArr.length()).mapNotNull { idx ->
                                val o = snapArr.optJSONObject(idx) ?: return@mapNotNull null
                                CalendarDayEvent(
                                    id = o.optString("id", "ev_$idx"),
                                    dayOfMonth = o.optInt("dayOfMonth", 1).coerceIn(1, 31),
                                    timeLabel = o.optString("timeLabel", "10:00"),
                                    titleHu = o.optString("titleHu", "Edzés"),
                                    exerciseCount = o.optInt("exerciseCount", 6),
                                    durationMinutes = o.optInt("durationMinutes", 60),
                                    status = runCatching { CalendarEventStatus.valueOf(o.optString("status", "PLANNED")) }.getOrDefault(CalendarEventStatus.PLANNED),
                                    splitBadge = o.optString("splitBadge", "A"),
                                programId = o.optString("programId").ifBlank { calendarRows[o.optString("id")]?.programId.orEmpty() },
                                dayId = o.optString("dayId").ifBlank { calendarRows[o.optString("id")]?.dayId.orEmpty() },
                                dateIso = o.optString("dateIso").ifBlank {
                                    calendarRows[o.optString("id")]?.date ?: legacyMonth.atDay(
                                        o.optInt("dayOfMonth", 1).coerceIn(1, legacyMonth.lengthOfMonth())).toString()
                                }
                                )
                            }
                        }
                    }
                }
            } catch (_: Throwable) {}

            // 5. Read persisted workouts from Room (with real durations and exerciseDetails)
            val mappedWorkouts = try {
                readNativeWorkoutHistory(database, exercisesMap)
            } catch (_: Throwable) { emptyList() }

            // 6. Read real daily health projections from HealthJournalStore using sleepMinutes
            val todayStr = LocalDate.now().toString()
            val healthProviderAtBind = HealthJournalStore.get(context).preferences().optString("provider", HealthJournalStore.HC)
            val healthGenerationAtBind = HealthJournalStore.get(context).generation()
            var stepsVal = 0
            var restingHrVal = 0
            var currentHrVal = 0
            var activeCalVal = 0
            var sleepStr = "—"
            var sleepDecimal = "—"

            try {
                val store = HealthJournalStore.get(context)
                val provider = store.preferences().optString("provider", HealthJournalStore.HC)
                val todayData = com.repforge.app.NativeHealthProviderData.day(store, provider, LocalDate.now())
                if (todayData != null) {
                    val stepsDbl = todayData.optDouble("steps", Double.NaN)
                    val restingDbl = todayData.optDouble("restingHeartRate", Double.NaN)
                    val currentDbl = todayData.optDouble("averageHeartRate", Double.NaN)
                    val calDbl = todayData.optDouble("activeCalories", Double.NaN)
                    val sleepMinDbl = todayData.optDouble("sleepMinutes", Double.NaN)

                    if (stepsDbl.isFinite() && stepsDbl >= 0) stepsVal = stepsDbl.toInt()
                    if (restingDbl.isFinite() && restingDbl > 0) restingHrVal = restingDbl.toInt()
                    if (currentDbl.isFinite() && currentDbl > 0) currentHrVal = currentDbl.toInt()
                    if (calDbl.isFinite() && calDbl > 0) activeCalVal = calDbl.toInt()
                    if (sleepMinDbl.isFinite() && sleepMinDbl > 0) {
                        sleepStr = com.example.localization.StudioLocale.durationMinutes(sleepMinDbl.toInt(), _uiState.value.language)
                        sleepDecimal = "${((sleepMinDbl / 60.0) * 10.0).roundToInt() / 10.0} h"
                    }
                }
            } catch (_: Throwable) {}

            withContext(Dispatchers.Main) {
                if (nativeDatabase !== database || runCatching { projectionTicket.requireCurrent(context) }.isFailure) return@withContext
                val restoredActiveSnap = runCatching {
                    ActiveWorkoutStore.current(context)?.takeIf { it.optBoolean("active", false) }
                        ?: PhoneWorkoutExecution.store(context).snapshot()?.takeIf { it.optBoolean("active", false) }
                }.getOrNull()
                _uiState.update { st ->
                    val activeProg = mappedPrograms.firstOrNull { it.isActive } ?: mappedPrograms.firstOrNull()
                    val firstDay = activeProg?.days?.firstOrNull()
                    var restoredExercises = emptyList<ExerciseItem>()
                    val restoredSetsMap = mutableMapOf<String, List<ExerciseSetItem>>()
                    if (restoredActiveSnap != null) {
                        val exArr = restoredActiveSnap.optJSONArray("exercises")
                        if (exArr != null && exArr.length() > 0) {
                            val list = mutableListOf<ExerciseItem>()
                            for (i in 0 until exArr.length()) {
                                val exObj = exArr.optJSONObject(i) ?: continue
                                val exId = exObj.optString("id").ifBlank { exObj.optString("exerciseId") }
                                val catalogEx = exercisesMap[exId] ?: ExerciseItem(
                                    id = exId.ifBlank { "restored_$i" },
                                    nameHu = exObj.optString("nameHu").ifBlank { exObj.optString("name").ifBlank { "Gyakorlat" } },
                                    isTimed = exObj.optString("measurementType").contains("time"),
                                    isBilateral = exObj.optString("measurementType").contains("bilateral") || exObj.optString("repUnit").contains("oldal")
                                )
                                list.add(com.example.ui.components.withNativeStrengthUnits(catalogEx,
                                    exObj.opt("loadType") as? String, exObj.opt("repUnit") as? String).copy(
                                        measurementType = exObj.optString("measurementType"),
                                        isTimed = com.repforge.app.NativeSetStopwatch.timed(exObj),
                                        isBilateral = com.repforge.app.NativeSetStopwatch.bilateral(exObj)))
                                val setsArr = exObj.optJSONArray("sets")
                                if (setsArr != null && setsArr.length() > 0) {
                                    val sList = (0 until setsArr.length()).mapNotNull { sIdx ->
                                        val sObj = setsArr.optJSONObject(sIdx) ?: return@mapNotNull null
                                        val doneFlag = sObj.optBoolean("done", false)
                                        val lSec = sObj.optInt("leftSeconds", sObj.optInt("leftSec", 0))
                                        val rSec = sObj.optInt("rightSeconds", sObj.optInt("rightSec", 0))
                                        ExerciseSetItem(
                                            setNumber = sObj.optInt("setNumber", sObj.optInt("set", sIdx + 1)),
                                            weightKg = sObj.optDouble("weight", catalogEx.defaultWeightKg.toDouble()).toFloat(),
                                            reps = sObj.optString("reps").toIntOrNull() ?: catalogEx.defaultReps,
                                            rir = sObj.optInt("actualRir", 2), id = sObj.optString("setId"),
                                            actualRirProvided = sObj.has("actualRir") && !sObj.isNull("actualRir"),
                                            completed = doneFlag,
                                            leftSec = lSec,
                                            rightSec = rSec,
                                            leftRecorded = sObj.optBoolean("leftRecorded", doneFlag),
                                            rightRecorded = sObj.optBoolean("rightRecorded", doneFlag)
                                        )
                                    }
                                    if (sList.isNotEmpty()) {
                                        restoredSetsMap[catalogEx.id] = sList
                                    }
                                }
                            }
                            restoredExercises = list
                        }
                    }

                    val initialWorkoutExs = when {
                        restoredExercises.isNotEmpty() -> restoredExercises
                        st.activeWorkoutExercises.isNotEmpty() -> st.activeWorkoutExercises
                        else -> firstDay?.exercises?.ifEmpty { mappedExercises.take(6) } ?: mappedExercises.take(6)
                    }
                    val restoredIdx = if (restoredActiveSnap != null && initialWorkoutExs.isNotEmpty()) {
                        restoredActiveSnap.optInt("currentExercise", 0).coerceIn(0, initialWorkoutExs.lastIndex)
                    } else st.activeExerciseIndex.coerceIn(0, (initialWorkoutExs.size - 1).coerceAtLeast(0))
                    val currentActiveEx = initialWorkoutExs.getOrNull(restoredIdx) ?: mappedExercises.firstOrNull()
                    val activeSetsForCurrent = if (currentActiveEx != null) {
                        restoredSetsMap[currentActiveEx.id]
                            ?: st.activeSetsByExerciseId[currentActiveEx.id]
                            ?: buildDefaultSetsForExercise(currentActiveEx)
                    } else st.activeSets

                    val computedPrs = computePrRecordsFromWorkouts(mappedWorkouts)
                    val currentPlannerSettings = plannerSettings.takeIf { plannerSettingsRevision == plannerRevisionAtRead }
                    val sameHealthSource = HealthJournalStore.get(context).generation() == healthGenerationAtBind &&
                        HealthJournalStore.get(context).preferences().optString("provider", HealthJournalStore.HC) == healthProviderAtBind
                    st.copy(
                        exercises = mappedExercises,
                        favoriteExerciseIds = loadedFavoriteIds,
                        profileComplete = com.example.model.StudioProfileCompletion.complete(savedProfile),
                        userHeightCm = savedProfile.optInt("height", st.userHeightCm),
                        userWeightKg = savedProfile.optDouble("weight", st.userWeightKg.toDouble()).toFloat(),
                        userFitnessGoalHu = savedProfile.optString("goal", st.userFitnessGoalHu),
                        userExperienceLevelHu = savedProfile.optString("experience", st.userExperienceLevelHu),
                        userWorkoutLocationHu = savedProfile.optString("location", st.userWorkoutLocationHu),
                        userSelectedEquipmentHu = savedProfile.optJSONArray("gear")?.let { arr ->
                            (0 until arr.length()).joinToString(", ") { arr.optString(it) }
                        } ?: st.userSelectedEquipmentHu,
                        userScheduleDaysPerWeek = savedProfile.optInt("daysPerWeek", st.userScheduleDaysPerWeek),
                        userWorkoutDurationMin = savedProfile.optInt("minutes", st.userWorkoutDurationMin),
                        userExcludedMusclesHu = savedProfile.optJSONArray("avoidAreas")?.let { arr ->
                            (0 until arr.length()).map { arr.optString(it) }.toSet()
                        } ?: st.userExcludedMusclesHu,
                        userExcludedExercisesHu = savedProfile.optJSONArray("excluded")?.let { arr ->
                            (0 until arr.length()).map { arr.optString(it) }.toSet()
                        } ?: st.userExcludedExercisesHu,
                        selectedExerciseId = if (restoredActiveSnap != null) currentActiveEx?.id ?: st.selectedExerciseId
                            else st.selectedExerciseId.takeIf { id -> mappedExercises.any { it.id == id } }
                                ?: mappedExercises.firstOrNull()?.id.orEmpty(),
                        programs = mappedPrograms,
                        editingProgramId = if (st.currentSubScreen == SubScreen.PROGRAM_EDITOR &&
                            mappedPrograms.any { it.id == st.editingProgramId }) st.editingProgramId
                            else activeProg?.id ?: st.editingProgramId,
                        calendarEvents = if (calendarWriteRevision == calendarRevisionAtRead) loadedCalendarEvents else st.calendarEvents,
                        calendarUndoStack = if (calendarWriteRevision == calendarRevisionAtRead) loadedCalendarUndo else st.calendarUndoStack,
                        planningCadenceHu = currentPlannerSettings?.optString("cadenceHu", st.planningCadenceHu) ?: st.planningCadenceHu,
                        planningFirstDayHu = currentPlannerSettings?.optString("firstDayHu", st.planningFirstDayHu) ?: st.planningFirstDayHu,
                        planningStartDate = currentPlannerSettings?.optString("startDate", st.planningStartDate) ?: st.planningStartDate,
                        planningTime = currentPlannerSettings?.optString("time", st.planningTime) ?: st.planningTime,
                        planningDurationMin = currentPlannerSettings?.optInt("durationMin", st.planningDurationMin) ?: st.planningDurationMin,
                        planningWeeksCount = currentPlannerSettings?.optInt("weeksCount", st.planningWeeksCount) ?: st.planningWeeksCount,
                        isWorkoutSessionActive = restoredActiveSnap != null,
                        outdoorRouteKey = restoredActiveSnap?.optString("studioGpsKey") ?: st.outdoorRouteKey,
                        outdoorGpsEnabled = restoredActiveSnap?.takeIf { it.has("studioIndoor") }?.let { !it.optBoolean("studioIndoor") } ?: st.outdoorGpsEnabled,
                        activeWorkoutId = restoredActiveSnap?.optString("workoutId").orEmpty(),
                        activeWorkoutGeneration = restoredActiveSnap?.optLong("generation") ?: 0L,
                        workoutStartedEpochMs = restoredActiveSnap?.optLong("startedEpochMs", 0L)?.takeIf { it > 0L } ?: st.workoutStartedEpochMs,
                        activeWorkoutSubScreen = if (restoredActiveSnap?.has("studioIndoor") == true) SubScreen.ACTIVE_OUTDOOR_GPS
                            else if (restoredActiveSnap != null) targetSubScreenForExercise(currentActiveEx) else st.activeWorkoutSubScreen,
                        activeWorkoutTitleHu = restoredActiveSnap?.optString("programName")?.takeIf { it.isNotBlank() }
                            ?: if (activeProg != null) "${activeProg.nameHu} • ${firstDay?.dayCode ?: "A nap"}" else st.activeWorkoutTitleHu,
                        activeWorkoutExercises = initialWorkoutExs,
                        activeExerciseIndex = restoredIdx,
                        activeSets = activeSetsForCurrent,
                        activeSetsByExerciseId = if (restoredSetsMap.isNotEmpty()) restoredSetsMap else st.activeSetsByExerciseId,
                        loggedWorkouts = mappedWorkouts,
                        privatePhotoCount = mappedWorkouts.sumOf { it.photoCount } +
                            loadedWeightLogs.sumOf { it.photoCount } + loadedLoosePhotoCount,
                        workoutGoalPeriod = loadedGoalPeriod,
                        workoutGoalTarget = if (loadedWeeklyGoal > 0) loadedWeeklyGoal else st.workoutGoalTarget,
                        workoutGoalCompleted = mappedWorkouts.workoutsInPeriodToDate(loadedGoalPeriod, LocalDate.now()).size,
                        weeklyGoalCompleted = mappedWorkouts.workoutsInCalendarWeekToDate(LocalDate.now()).size,
                        weeklyGoalTarget = if (loadedWeeklyGoal > 0) loadedWeeklyGoal else st.weeklyGoalTarget,
                        stepsGoal = if (loadedStepsGoal > 0) loadedStepsGoal else st.stepsGoal,
                        currentBodyWeightKg = if (latestWeightKg > 0f) latestWeightKg else st.currentBodyWeightKg,
                        bodyWeightLogs = loadedWeightLogs,
                        dailySteps = if (sameHealthSource) stepsVal else st.dailySteps,
                        restingHrBpm = if (sameHealthSource) restingHrVal else st.restingHrBpm,
                        currentHrBpm = if (sameHealthSource) currentHrVal else st.currentHrBpm,
                        activeCaloriesKcal = if (sameHealthSource) activeCalVal else st.activeCaloriesKcal,
                        sleepDurationLabel = if (sameHealthSource) sleepStr else st.sleepDurationLabel,
                        sleepHoursDecimal = if (sameHealthSource) sleepDecimal else st.sleepHoursDecimal
                    )
                }
                if (calendarProjectionTicket?.let { runCatching { it.requireCurrent(context) }.isSuccess } != true) {
                    calendarWriteError = null
                    plannerSettingsWriteError = null
                }
                calendarProjectionTicket = projectionTicket
            }

            withContext(Dispatchers.Main) { reloadHealthJournal() }

            // 7. Load real muscle load summary from Room
            loadMuscleLoadSummary(progressPeriodDays(_uiState.value.selectedProgressRangeIndex))
            _uiState.value.selectedLogWorkoutId.takeIf { it.isNotBlank() }?.let(::loadWorkoutMuscleSummary)
            reloadPersonalRecords()
        }
    }

    internal var appearanceWriteJob: Job? = null

    /**
     * 2779: true only while the library was opened from the program editor ("+ Gyakorlat").
     * The quick-workout queue "+" used to append into whatever program was last opened in the
     * editor (Otthoni A/B day A grew from 7 to 10) and wrote the built-in row in place.
     */
    internal var libraryAddsToProgramDay = false

    internal data class CustomExerciseProgramTarget(
        val programId: String, val dayId: String, val label: String,
        val dataset: com.repforge.app.NativeDataReplacement.Continuation
    )

    internal var customExerciseProgramTarget: CustomExerciseProgramTarget? = null

    internal var calendarClearPreview: com.repforge.app.NativeCalendarClear.Preview? = null
    internal var calendarClearTicket: NativeDataReplacement.Continuation? = null
    internal var calendarClearRevision = 0L

    internal var studioIntervalWriteJob: Job? = null
    internal var studioIntervalWriteError: String? = null
    internal var summaryFeedbackTicket: NativeDataReplacement.Continuation? = null
    internal var summaryFeedbackOwnerId = ""
    internal var exerciseFeedbackTicket: NativeDataReplacement.Continuation? = null
    internal var exerciseFeedbackOwnerId = ""
    internal var nativeStartJob: kotlinx.coroutines.Job? = null

    internal val activeMeasurementScreens = setOf(SubScreen.ACTIVE_STRENGTH, SubScreen.ACTIVE_TIMED,
        SubScreen.ACTIVE_BILATERAL, SubScreen.ACTIVE_OUTDOOR_GPS)
    internal data class ExerciseSelection(val target: com.repforge.app.NativeWorkoutTarget,
        val data: com.repforge.app.NativeDataReplacement.Continuation)
    internal var exerciseSelection: ExerciseSelection? = null
        set(value) {
            field = value
            // 2773: the library shows "Swap" instead of "Start" while it replaces the active exercise.
            if (_uiState.value.exerciseSwapMode != (value != null)) _uiState.update { it.copy(exerciseSwapMode = value != null) }
        }
    internal var exerciseSelectionJob: Job? = null

    // Log / Diary Actions
    fun updateLogSearch(query: String) {
        _uiState.update { it.copy(logSearchQuery = query) }
    }

    fun openLogWorkoutDetail(workoutId: String) {
        if (workoutId.isBlank()) return
        if (_uiState.value.currentSubScreen == SubScreen.LOG_WORKOUT_DETAIL &&
            _uiState.value.selectedLogWorkoutId == workoutId) return
        _uiState.update {
            val back = (it.backStack + it.navigationRoute()).takeLast(16)
            it.copy(
                selectedLogWorkoutId = workoutId,
                logWorkoutMuscleLoads = emptyList(),
                expandedLogExerciseIndex = -1,
                currentTab = MainTab.LOG,
                currentSubScreen = SubScreen.LOG_WORKOUT_DETAIL,
                backStack = back
            )
        }
        loadWorkoutMuscleSummary(workoutId)
    }

    fun openLoggedWorkoutRouteMap(workoutId: String) {
        if (workoutId.isBlank()) return
        _uiState.update { state ->
            state.copy(currentTab = MainTab.LOG, currentSubScreen = SubScreen.LOG_ROUTE_MAP,
                selectedLogWorkoutId = workoutId, backStack = (state.backStack + state.navigationRoute()).takeLast(16))
        }
    }

    fun openCalendarEventWorkoutDetail(eventId: String) {
        val state = _uiState.value
        val event = state.calendarEvents.firstOrNull { it.id == eventId }
        val targetEventDate = event?.dateIso?.trim().orEmpty()
        fun normalizeDayToken(raw: String): String =
            raw.substringAfterLast(':').trim().removePrefix("day_").removeSuffix(" nap").trim().lowercase(Locale.ROOT)
        val matched = state.loggedWorkouts.firstOrNull { it.id == eventId }
            ?: state.loggedWorkouts.firstOrNull { it.scheduleId.isNotBlank() && it.scheduleId == eventId }
            ?: if (event != null && targetEventDate.isNotBlank()) {
                val sameDateUnboundWorkouts = state.loggedWorkouts.filter { entry ->
                    if (entry.scheduleId.isNotBlank() && entry.scheduleId != eventId) return@filter false
                    val entryDate = entry.savedStartIso.take(10).ifBlank {
                        entry.dateLabelHu.take(10).replace(". ", "-").replace('.', '-').trim('-')
                    }
                    entryDate == targetEventDate
                }
                val eventTitlePrefix = event.titleHu.substringBefore("•").trim()
                sameDateUnboundWorkouts.firstOrNull { entry ->
                    val progMatches = if (event.programId.isNotBlank() && entry.programId.isNotBlank()) {
                        entry.programId == event.programId
                    } else if (event.programId.isNotBlank()) {
                        eventTitlePrefix.isNotBlank() && entry.titleHu.contains(eventTitlePrefix, ignoreCase = true)
                    } else true
                    val dayMatches = if (event.dayId.isNotBlank() && entry.dayId.isNotBlank()) {
                        normalizeDayToken(entry.dayId) == normalizeDayToken(event.dayId) ||
                            normalizeDayToken(entry.dayId) == normalizeDayToken(event.splitBadge)
                    } else true
                    progMatches && dayMatches
                } ?: sameDateUnboundWorkouts.singleOrNull { entry ->
                    (event.programId.isBlank() && entry.programId.isBlank()) ||
                        (event.programId.isNotBlank() && entry.programId == event.programId) ||
                        (eventTitlePrefix.isNotBlank() && entry.titleHu.contains(eventTitlePrefix, ignoreCase = true))
                }
            } else null

        if (matched != null) {
            openLogWorkoutDetail(matched.id)
        } else {
            _uiState.update {
                it.copy(
                    statusBannerMessage = tr(
                        "Ehhez a naptári eseményhez nem található naplózott edzés.",
                        "No logged workout found for this calendar event.",
                        "Für diesen Kalendereintrag wurde kein protokolliertes Training gefunden.",
                        "Nu s-a găsit niciun antrenament salvat pentru acest eveniment din calendar.",
                        it.language
                    )
                )
            }
        }
    }

    fun onLoggedWorkoutDeleted(id: String) {
        _uiState.update { state -> state.copy(
            loggedWorkouts = state.loggedWorkouts.filterNot { it.id == id },
            selectedLogWorkoutId = if (state.selectedLogWorkoutId == id) "" else state.selectedLogWorkoutId)
        }
        if (_uiState.value.currentSubScreen == SubScreen.LOG_WORKOUT_DETAIL && _uiState.value.selectedLogWorkoutId.isBlank())
            navigateBack()
        nativeContext?.let(::refreshAfterCloudSync)
    }

    fun updateLoggedWorkoutHealthSources(workoutId: String, wear: Boolean, healthConnect: Boolean) {
        _uiState.update { state -> state.copy(loggedWorkouts = state.loggedWorkouts.map { entry ->
            if (entry.id == workoutId) entry.copy(sourceWatch = wear, sourceHealthConnect = healthConnect) else entry
        }) }
    }

    fun selectLogDetailTab(index: Int) {
        _uiState.update { it.copy(logDetailTabIndex = index) }
    }

    fun toggleExpandedLogExercise(index: Int) {
        _uiState.update {
            it.copy(expandedLogExerciseIndex = if (it.expandedLogExerciseIndex == index) -1 else index)
        }
    }

    // Progress / Fejlődés Actions
    fun reloadPersonalRecords() {
        val db = nativeDatabase ?: return
        val scope = nativeScope ?: return
        val revision = ++personalRecordsRevision
        personalRecordsJob?.cancel()
        _uiState.update { it.copy(savedRecordsLoading = true, savedRecordsError = false) }
        personalRecordsJob = scope.launch(Dispatchers.IO) {
            try {
                val records = com.repforge.app.PersonalRecordCalculator226.savedExerciseBests(db)
                withContext(Dispatchers.Main) {
                    if (revision == personalRecordsRevision) _uiState.update {
                        it.copy(savedExerciseBests = records, savedRecordsLoading = false, savedRecordsError = false)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (revision == personalRecordsRevision) _uiState.update { it.copy(savedRecordsLoading = false, savedRecordsError = true) }
            }
        }
    }

    fun selectProgressMainSection(section: Int) {
        if (section != 0 && section != 2) return
        _uiState.update { it.copy(progressMainSection = section) }
        if (section == 2) reloadPersonalRecords()
    }

    fun selectProgressExercise(name: String) {
        _uiState.update { it.copy(selectedProgressExercise = name) }
    }

    fun selectProgressRange(index: Int) {
        val range = index.coerceIn(0, 3)
        val days = progressPeriodDays(range)
        _uiState.update { it.copy(selectedProgressRangeIndex = range, selectedVolumeBarIndex = -1,
            muscleLoadPeriodDays = days, realMuscleLoads = emptyList()) }
        loadMuscleLoadSummary(days)
    }

    fun selectVolumeBar(index: Int) {
        _uiState.update { it.copy(selectedVolumeBarIndex = index) }
    }

    fun selectExpandedStatsCard(index: Int) {
        _uiState.update {
            it.copy(expandedStatsCardIndex = if (it.expandedStatsCardIndex == index) -1 else index)
        }
    }

    fun selectStatsSubTab(index: Int) {
        _uiState.update { it.copy(statsSubTab = index) }
    }

    fun toggleCardioFilterStepsMode(stepsMode: Boolean) {
        _uiState.update { it.copy(cardioFilterStepsMode = stepsMode) }
    }

    fun openExerciseVideoModal(exerciseTitle: String, exerciseId: String? = null) {
        _uiState.update {
            it.copy(videoModalExerciseTitle = exerciseTitle, videoModalExerciseId = exerciseId)
        }
    }

    fun closeExerciseVideoModal() {
        _uiState.update {
            it.copy(videoModalExerciseTitle = null, videoModalExerciseId = null)
        }
    }

    // Health / Egészség Actions
    fun openHealthAndExpand(
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

    fun toggleHealthSection(section: String) {
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

    fun selectWeightRange(index: Int) {
        _uiState.update { it.copy(selectedWeightRangeIndex = index) }
    }

    fun toggleAddWeightDialog(show: Boolean) {
        _uiState.update { it.copy(showAddWeightDialog = show) }
    }

    private fun parseUserEnteredDateToLocalDate(rawDate: String): LocalDate {
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
    fun saveNewBodyWeight(weightKg: Float, dateStr: String = "", editingId: String? = null) {
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

    fun deleteBodyWeightLog(id: String) {
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

    fun refreshPrivatePhotoCount() {
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

    fun togglePrimaryHealthSource() {
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

    fun updateHealthSyncStatus(busy: Boolean, partial: Boolean = false, failed: Boolean = false, message: String) {
        _uiState.update { it.copy(healthSyncBusy = busy, healthSyncPartial = partial,
            healthSyncFailed = failed, healthSyncMessage = message) }
    }

    /** Provider whose values are on screen now; null = nothing loaded (or cleared). */
    @Volatile private var loadedHealthProvider: String? = null
    private var healthJournalReloadJob: Job? = null

    fun reloadHealthJournal() {
        val ctx = nativeContext
        val scope = nativeScope
        if (ctx == null || scope == null) {
            _uiState.update {
                it.copy(statusBannerMessage = "Az egészségügyi szolgáltatások nem elérhetők.")
            }
            return
        }
        // 2779: values are no longer blanked before every reload: the reload overwrites every field
        // below, so blanking first only made the Coach card and the Today tiles flash "—" and jump.
        // They are still cleared when the provider changed (old provider's data must not show) or
        // when the reload fails.
        fun clearedHealth(st: TrainPilotUiState) = st.copy(
                dailySteps = 0,
                currentHrBpm = 0,
                restingHrBpm = 0,
                maxHrBpm = 0,
                minHrBpm = 0,
                dailyPulseHistory = emptyList(), dailyPulseSampleCount = 0,
                activeCaloriesKcal = 0, totalCaloriesKcal = 0, dailyHealthDate = "", dailyHealthAvailableFields = emptySet(),
                sleepDurationLabel = "—",
                sleepHoursDecimal = "—",
                samsungEnergyScore = 0,
                trainPilotReadinessScore = 0,
                coachReadinessScore = 0,
                todayPulseReading = null,
                todayRecoveryReading = null,
                weeklyHealthDays = emptyList(),
                spo2Percent = 0f,
                vo2Max = 0f,
                bodyFatPercent = 0f,
                hrvRmssdMs = 0f,
                bloodPressureLabel = "—",
                bloodGlucoseLabel = "—",
                respiratoryRateLabel = "—",
                dailyDistanceKm = 0f
        )
        val providerAtStart = runCatching { HealthJournalStore.get(ctx).preferences().optString("provider", HealthJournalStore.HC) }.getOrNull()
        if (providerAtStart == null || providerAtStart != loadedHealthProvider) {
            loadedHealthProvider = null
            _uiState.update(::clearedHealth)
        }
        healthJournalReloadJob?.cancel()
        healthJournalReloadJob = scope.launch(Dispatchers.IO) {
            try {
                val store = HealthJournalStore.get(ctx)
                val prefs = store.preferences()
                val provider = prefs.optString("provider", HealthJournalStore.HC)
                val hcDailySourceRaw = prefs.optString("hcDailySource", "auto")
                val dailySourceModeHu = when (hcDailySourceRaw) {
                    "auto", "" -> studioModesText("Automatikus (Health Connect prioritás, duplikáció nélkül)", "Automatic (Health Connect priority, no duplicates)", "Automatisch (Health Connect-Priorität, ohne Duplikate)", "Automat (prioritatea Health Connect, fără duplicate)")
                    "priority" -> studioModesText("Prioritásos forrásválasztás", "Source priority", "Quellenpriorität", "Prioritatea surselor")
                    com.repforge.app.HealthDailySource.SAMSUNG -> "Rögzített forrás: Samsung Health"
                    else -> "Rögzített csomag: $hcDailySourceRaw"
                }
                val generation = store.generation()
                val zone = ZoneId.systemDefault()
                val now = Instant.now()
                val today = now.atZone(zone).toLocalDate()
                val projectedProviderDays = mutableListOf<JSONObject>()
                // The HC journal preserves older observations, including across temporary read
                // failures. Only the currently granted, successfully read fields may be shown.
                val hcGranted = com.repforge.app.HealthConnectRepository.READ_PERMS
                    .filter { com.repforge.app.HealthConnectRepository.isAllowed(ctx, it) }.toSet()
                fun admitted(dayRow: JSONObject?, fromProvider: String): JSONObject? =
                    if (fromProvider == HealthJournalStore.HC)
                        com.repforge.app.HealthConnectVisibleDay.project(dayRow, hcGranted)
                    else dayRow
                val todayData = admitted(com.repforge.app.NativeHealthProviderData.day(store, provider, today), provider)
                val alternativeProvider = if (provider == HealthJournalStore.SAMSUNG) HealthJournalStore.HC
                    else HealthJournalStore.SAMSUNG
                val privacy = com.repforge.app.NativePrivacyControls
                val primaryEnabled = privacy.enabled(ctx, if (provider == HealthJournalStore.SAMSUNG)
                    privacy.SAMSUNG_HEALTH else privacy.HEALTH_CONNECT)
                val alternativeEnabled = privacy.enabled(ctx, if (alternativeProvider == HealthJournalStore.SAMSUNG)
                    privacy.SAMSUNG_HEALTH else privacy.HEALTH_CONNECT)
                val alternateToday = if (alternativeEnabled)
                    admitted(com.repforge.app.NativeHealthProviderData.day(store, alternativeProvider, today),
                        alternativeProvider) else null
                fun measuredSteps(row: JSONObject?, fromProvider: String): Double? {
                    if (row == null) return null
                    // A previously pinned origin cannot answer today's automatic-priority query, or vice versa.
                    if (fromProvider == HealthJournalStore.HC &&
                        row.optString("activityOrigin") != com.repforge.app.HealthDailySource.choose("", hcDailySourceRaw, emptySet())) return null
                    return row.optDouble("steps", Double.NaN).takeIf { it.isFinite() && it in 0.0..Int.MAX_VALUE.toDouble() }
                }
                val todaysSteps = measuredSteps(todayData, provider) ?: measuredSteps(alternateToday, alternativeProvider)

                val todaySleepMin = com.repforge.app.NativeDailyHealthDetails.sleepMinutes(
                    todayData, today, java.time.ZoneId.systemDefault(), Instant.now()) ?: Double.NaN
                val todayPulseHistory = com.repforge.app.NativeDailyHealthDetails.pulseSeries(todayData)
                val hasSleepStages = runCatching {
                    val sessions = todayData?.optJSONArray("sleepSessions")
                    if (sessions != null && sessions.length() > 0) {
                        (0 until sessions.length()).any { idx ->
                            val s = sessions.optJSONObject(idx)
                            val stg = s?.optJSONArray("stages")
                            stg != null && stg.length() > 0
                        }
                    } else false
                }.getOrDefault(false)
                val sleepStagesSummary = if (hasSleepStages) {
                    studioModesText("Részletes alvásfázisok rögzítve (mély, REM, könnyű alvás)", "Sleep stages recorded (deep, REM, light)", "Schlafphasen aufgezeichnet (Tief-, REM-, Leichtschlaf)", "Stadii de somn înregistrate (profund, REM, ușor)")
                } else {
                    studioModesText("A szolgáltató nem adott át alvásfázisbontást (csak teljes időtartam)", "Provider supplied total sleep duration only", "Anbieter lieferte nur die Gesamtschlafdauer", "Furnizorul a transmis doar durata totală a somnului")
                }
                val availableFields = (setOf("activeCalories", "totalCalories", "averageHeartRate",
                    "restingHeartRate", "minHeartRate", "maxHeartRate", "heartRateSamples")
                    .filter { key -> todayData?.optDouble(key, Double.NaN)?.let { it.isFinite() && it >= 0 } == true } +
                    (if (todaysSteps != null) listOf("steps") else emptyList()) +
                    (if (todaySleepMin.isFinite() && todaySleepMin > 0) listOf("sleepMinutes") else emptyList()) +
                    (if (todayPulseHistory.isNotEmpty()) listOf("heartRateSeries") else emptyList())).toSet()
                val lastProviderSync = store.nativeSyncLastSuccess(provider)
                val stepsDbl = todaysSteps ?: Double.NaN
                val pulseDbl = todayData?.optDouble("averageHeartRate", Double.NaN) ?: Double.NaN
                val restingDbl = todayData?.optDouble("restingHeartRate", Double.NaN) ?: Double.NaN
                val maxHrDbl = todayData?.optDouble("maxHeartRate", Double.NaN) ?: Double.NaN
                val minHrDbl = todayData?.optDouble("minHeartRate", Double.NaN) ?: Double.NaN
                val caloriesDbl = todayData?.optDouble("activeCalories", Double.NaN) ?: Double.NaN
                val sleepMinDbl = todaySleepMin
                val hrvDbl = todayData?.optDouble("hrvRmssdMs", Double.NaN) ?: Double.NaN
                val sysDbl = todayData?.optDouble("bloodPressureSystolic", Double.NaN) ?: Double.NaN
                val diaDbl = todayData?.optDouble("bloodPressureDiastolic", Double.NaN) ?: Double.NaN
                val gluDbl = todayData?.optDouble("bloodGlucoseMmolL", Double.NaN) ?: Double.NaN
                val respDbl = todayData?.optDouble("respiratoryRate", Double.NaN) ?: Double.NaN
                val distMetersDbl = todayData?.optDouble("distanceMeters", Double.NaN) ?: Double.NaN
                val bodyFatDbl = todayData?.optDouble("bodyFatPercent", Double.NaN) ?: Double.NaN
                val energyDbl = todayData?.optDouble("energyScore", Double.NaN) ?: Double.NaN

                val bpLabel = if (sysDbl.isFinite() && sysDbl > 0 && diaDbl.isFinite() && diaDbl > 0) {
                    "${sysDbl.roundToInt()}/${diaDbl.roundToInt()} Hgmm"
                } else "—"
                val gluLabel = if (gluDbl.isFinite() && gluDbl > 0) {
                    String.format(java.util.Locale.US, "%.1f mmol/l", gluDbl).replace('.', ',')
                } else "—"
                val respLabel = if (respDbl.isFinite() && respDbl > 0) {
                    "${respDbl.roundToInt()} /perc"
                } else "—"
                val distKmToday = if (distMetersDbl.isFinite() && distMetersDbl > 0) (distMetersDbl / 1000.0).toFloat() else 0f

                // Build 7-day breakdown & source attribution
                val currentWorkouts = _uiState.value.loggedWorkouts
                val dayFmt = DateTimeFormatter.ofPattern("MM. dd. (EEE)", com.example.localization.StudioLocale.forLanguage(_uiState.value.language))
                val weeklyDaysList = mutableListOf<WeeklyHealthDaySummary>()
                var sumSteps = 0
                var sumKcal = 0
                var sumDistMeters = 0.0
                var sumSleepMin = 0.0
                var sleepDaysCount = 0
                val discoveredSources = linkedSetOf<String>()

                for (offset in 13 downTo 0) {
                    val d = today.minusDays(offset.toLong())
                    val dayObj = runCatching {
                        admitted(com.repforge.app.NativeHealthProviderData.day(store, provider, d), provider)
                    }.getOrNull()
                    // Never project another provider's cached row as the selected source.
                    val ownDay = dayObj?.takeIf { raw ->
                        raw.optString("provider").let { it.isBlank() || it == provider } &&
                            raw.optString("channel").let { it.isBlank() || it == provider }
                    }?.let { JSONObject(it.toString()).put("day", d.toString()) }
                    if (ownDay != null) projectedProviderDays.add(ownDay)
                    val ownSteps = measuredSteps(dayObj, provider)
                    val alternateStepDay = if (ownSteps != null || !alternativeEnabled) null
                        else if (d == today) alternateToday
                        else admitted(com.repforge.app.NativeHealthProviderData.day(store, alternativeProvider, d),
                            alternativeProvider)
                    val stepDay = if (ownSteps != null) dayObj else alternateStepDay
                    val measuredStepCount = ownSteps ?: measuredSteps(alternateStepDay, alternativeProvider)
                    val stepsAvailable = measuredStepCount != null
                    val dSteps = measuredStepCount?.toInt() ?: 0
                    val dKcal = dayObj?.optDouble("activeCalories", Double.NaN)?.takeIf { it.isFinite() && it >= 0 }?.toInt() ?: 0
                    // 2788: measured total energy of the day (resting + active); null when not measured (never 0).
                    val dTotalKcal = dayObj?.optDouble("totalCalories", Double.NaN)?.takeIf { it.isFinite() && it > 0 }?.toInt()
                    val dActiveKcal = dayObj?.optDouble("activeCalories", Double.NaN)?.takeIf { it.isFinite() && it >= 0 }?.toInt()
                    val dDistM = dayObj?.optDouble("distanceMeters", Double.NaN)?.takeIf { it.isFinite() && it >= 0 } ?: 0.0
                    val dSleepMin = com.repforge.app.NativeDailyHealthDetails.sleepMinutes(
                        dayObj, d, java.time.ZoneId.systemDefault(), Instant.now()) ?: Double.NaN
                    val dSleepLabel = if (dSleepMin.isFinite()) {
                        com.example.localization.StudioLocale.durationMinutes(dSleepMin.toInt(), _uiState.value.language)
                    } else "—"

                    val workoutsForDay = currentWorkouts.filter { w ->
                        w.localWorkoutDate() == d
                    }
                    val workoutMinForDay = workoutsForDay.sumOf { it.durationMinutes.coerceAtLeast(0) }
                    val workoutCountForDay = workoutsForDay.size

                    val hasHealthRow = stepsAvailable || dayObj != null && (dKcal > 0 || dSleepMin.isFinite() ||
                        dayObj.optDouble("averageHeartRate", 0.0) > 0.0 || dayObj.optDouble("restingHeartRate", 0.0) > 0.0)
                    val stepOriginPkg = stepDay?.optString("stepsOrigin", "")?.ifBlank { stepDay.optString("activityOrigin", "") }.orEmpty()
                    val dayChannel = dayObj?.optString("channel", provider).orEmpty()
                    val stepProvider = if (ownSteps != null) provider else alternativeProvider
                    val healthSourceLabel = if (stepsAvailable) formatProviderLabelHu(stepProvider, stepOriginPkg)
                        else formatProviderLabelHu(dayChannel, dayObj?.optString("activityOrigin").orEmpty())
                    val sourceLabelHu = when {
                        hasHealthRow && workoutCountForDay > 0 -> "${healthSourceLabel} + ${studioModesText("Edzésnapló", "Workout journal", "Trainingstagebuch", "Jurnal de antrenament")}"
                        hasHealthRow -> healthSourceLabel
                        workoutCountForDay > 0 -> studioModesText("Edzésnapló (aktív időszakok)", "Workout journal (active intervals)", "Trainingstagebuch (aktive Zeiträume)", "Jurnal de antrenament (intervale active)")
                        else -> studioModesText("Nincs rögzített adat", "No recorded data", "Keine aufgezeichneten Daten", "Nu există date înregistrate")
                    }
                    if (offset <= 6) {
                    if (hasHealthRow) discoveredSources.add(healthSourceLabel)
                    if (workoutCountForDay > 0) discoveredSources.add(studioModesText("Edzésnapló (aktív idő)", "Workout journal (active time)", "Trainingstagebuch (aktive Zeit)", "Jurnal de antrenament (timp activ)"))

                    sumSteps += dSteps
                    sumKcal += dKcal
                    sumDistMeters += dDistM
                    if (dSleepMin.isFinite() && dSleepMin > 0) {
                        sumSleepMin += dSleepMin
                        sleepDaysCount++
                    }

                    }

                    weeklyDaysList.add(
                        WeeklyHealthDaySummary(
                            dateIso = d.toString(),
                            dayLabelHu = if (d == today) "Ma (${d.format(dayFmt)})" else d.format(dayFmt),
                            steps = dSteps,
                            activeCaloriesKcal = dKcal,
                            activeCaloriesMeasured = dActiveKcal,
                            totalCaloriesKcal = dTotalKcal,
                            sleepLabel = dSleepLabel,
                            workoutMinutes = workoutMinForDay,
                            workoutCount = workoutCountForDay,
                            stepsAvailable = stepsAvailable,
                            stepsSourceLabelHu = if (stepsAvailable) formatProviderLabelHu(stepProvider, stepOriginPkg) else "Nincs napi lépésadat",
                            stepProvider = stepProvider.takeIf { stepsAvailable },
                            sourceLabelHu = sourceLabelHu,
                            stepOriginPackage = stepOriginPkg,
                            pulse = if (provider == HealthJournalStore.HC || provider == HealthJournalStore.SAMSUNG)
                                HealthReadingsProjection.pulseDay(provider, d, ownDay, now, zone) else null
                        )
                    )
                }

                val avgSleepHu = if (sleepDaysCount > 0) {
                    val avgMin = (sumSleepMin / sleepDaysCount).roundToInt()
                    com.example.localization.StudioLocale.durationMinutes(avgMin, _uiState.value.language)
                } else "—"

                val primaryProviderHu = if (provider == HealthJournalStore.SAMSUNG) "Samsung Health" else "Health Connect"
                val sourcesSummaryHu = if (discoveredSources.isNotEmpty()) {
                    discoveredSources.joinToString(" • ")
                } else {
                    "$primaryProviderHu (nincs szinkronizált mérés az elmúlt 7 napban)"
                }

                // Use the same selected-provider, day-validated projections for Home and Health.
                val todayPulse = if (provider == HealthJournalStore.HC || provider == HealthJournalStore.SAMSUNG)
                    HealthReadingsProjection.pulseDay(
                        provider, today, projectedProviderDays.firstOrNull { it.optString("day") == today.toString() },
                        now, zone) else null
                val roomCoachHistory = runCatching {
                    nativeDatabase?.workoutHistoryDao()?.getCoachRecentHistory(
                        now.minus(java.time.Duration.ofDays(14)).toEpochMilli(), now.toEpochMilli())
                        ?.mapNotNull { raw -> runCatching { JSONObject(raw) }.getOrNull() }
                        ?: emptyList()
                }.getOrDefault(emptyList())
                val todayRecovery = if (provider == HealthJournalStore.HC || provider == HealthJournalStore.SAMSUNG) {
                    fun recoveryDays(channel: String, enabled: Boolean): List<JSONObject> =
                        if (enabled) com.repforge.app.NativeHealthProviderData.recentRecoveryDays(store, channel, today)
                            .mapNotNull { admitted(it, channel) } else emptyList()
                    HealthReadingsProjection.recoveryToday(provider, recoveryDays(provider, primaryEnabled),
                        roomCoachHistory, now, zone, alternateDays = recoveryDays(alternativeProvider, alternativeEnabled))
                } else null

                withContext(Dispatchers.Main) {
                    if (store.generation() != generation || store.preferences().optString("provider", HealthJournalStore.HC) != provider ||
                        primaryEnabled != privacy.enabled(ctx, if (provider == HealthJournalStore.SAMSUNG)
                            privacy.SAMSUNG_HEALTH else privacy.HEALTH_CONNECT) ||
                        alternativeEnabled != privacy.enabled(ctx, if (alternativeProvider == HealthJournalStore.SAMSUNG)
                            privacy.SAMSUNG_HEALTH else privacy.HEALTH_CONNECT)) return@withContext
                    loadedHealthProvider = provider
                    _uiState.update { st ->
                        st.copy(
                            primaryHealthSourceSamsung = provider == HealthJournalStore.SAMSUNG,
                            dailyHealthDate = if (todayData != null || todaysSteps != null) today.toString() else "",
                            dailyHealthAvailableFields = availableFields,
                            dailyHealthSourceLabel = formatProviderLabelHu(provider, todayData?.optString("activityOrigin").orEmpty()),
                            dailyHealthLastSyncIso = lastProviderSync,
                            totalCaloriesKcal = todayData?.optDouble("totalCalories", Double.NaN)?.takeIf { it.isFinite() && it >= 0 }?.toInt() ?: 0,
                            dailySteps = if (stepsDbl.isFinite() && stepsDbl >= 0) stepsDbl.toInt() else 0,
                            currentHrBpm = todayPulse?.averageBpm?.roundToInt() ?: 0,
                            restingHrBpm = todayPulse?.restingBpm?.roundToInt() ?: 0,
                            maxHrBpm = if (maxHrDbl.isFinite() && maxHrDbl > 0) maxHrDbl.toInt() else 0,
                            minHrBpm = if (minHrDbl.isFinite() && minHrDbl > 0) minHrDbl.toInt() else 0,
                            dailyPulseHistory = todayPulseHistory,
                            dailyPulseSampleCount = todayData?.optInt("heartRateSamples", 0)?.coerceAtLeast(0) ?: 0,
                            activeCaloriesKcal = if (caloriesDbl.isFinite() && caloriesDbl > 0) caloriesDbl.toInt() else 0,
                            sleepDurationLabel = if (sleepMinDbl.isFinite() && sleepMinDbl > 0) com.example.localization.StudioLocale.durationMinutes(sleepMinDbl.toInt(), _uiState.value.language) else "—",
                            sleepHoursDecimal = if (sleepMinDbl.isFinite() && sleepMinDbl > 0) "${((sleepMinDbl / 60.0) * 10.0).roundToInt() / 10.0} h" else "—",
                            sleepHasStages = hasSleepStages,
                            sleepStagesSummaryHu = sleepStagesSummary,
                            samsungEnergyScore = todayRecovery?.samsungEnergy?.roundToInt() ?: 0,
                            spo2Percent = todayData?.optDouble("oxygenSaturationPercent", Double.NaN)?.takeIf { it.isFinite() && it > 0 }?.toFloat() ?: 0f,
                            vo2Max = todayData?.optDouble("vo2Max", Double.NaN)?.takeIf { it.isFinite() && it > 0 }?.toFloat() ?: 0f,
                            bodyFatPercent = if (bodyFatDbl.isFinite() && bodyFatDbl > 0) bodyFatDbl.toFloat() else 0f, // Never retain another provider's previous value.
                            hrvRmssdMs = if (hrvDbl.isFinite() && hrvDbl > 0) hrvDbl.toFloat() else 0f,
                            bloodPressureLabel = bpLabel,
                            bloodGlucoseLabel = gluLabel,
                            respiratoryRateLabel = respLabel,
                            dailyDistanceKm = distKmToday,
                            coachReadinessScore = todayRecovery?.coachScore?.roundToInt() ?: 0,
                            trainPilotReadinessScore = todayRecovery?.trainPilotEstimate ?: 0,
                            todayPulseReading = todayPulse,
                            todayRecoveryReading = todayRecovery,
                            weeklyHealthDays = weeklyDaysList.takeLast(7),
                            dailyActivityDays = weeklyDaysList,
                            weeklyTotalSteps = sumSteps,
                            weeklyTotalCaloriesKcal = sumKcal,
                            weeklyTotalDistanceKm = (sumDistMeters / 1000.0).toFloat(),
                            weeklyAvgSleepLabel = avgSleepHu,
                            weeklyDataSourcesSummary = sourcesSummaryHu,
                            dailySourceModeLabel = dailySourceModeHu,
                            statusBannerMessage = st.statusBannerMessage
                        )
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        loadedHealthProvider = null
                        clearedHealth(it).copy(statusBannerMessage = "Hiba az egészségszinkron során: ${e.localizedMessage ?: "szolgáltatói hiba"}")
                    }
                }
            }
        }
    }

    // Watch, Settings, Backup & Permissions Actions (P0-04: real Wear OS node & ACK check)
    fun toggleWatchControlMode(activity: android.app.Activity? = null) {
        val ctx = nativeContext ?: return
        val scope = nativeScope ?: return
        if (_uiState.value.watchControlPending || _uiState.value.isSavingWorkout) return
        scope.launch(Dispatchers.Main.immediate) {
            val active = ActiveWorkoutStore.current(ctx)?.takeIf { it.optBoolean("active") }
            if (active == null) { showBannerMessage("Nincs átadható aktív edzés."); return@launch }
            val id = active.optString("workoutId")
            if (active.optInt("ownershipProtocol193") != 1) {
                showBannerMessage("Ez a korábbi edzés nem támogatja az órás átadást; a telefonon biztonságosan folytatható.")
                return@launch
            }
            watchHandoffBusy = true
            suspend fun awaitOwner(owner: String) {
                kotlinx.coroutines.withTimeout(32_000) {
                    while (!com.repforge.app.NativeWatchHandoffReceipt.confirmed(
                            ActiveWorkoutStore.current(ctx), PhoneOwnership193.journal(ctx).read(), id, owner)) {
                        kotlinx.coroutines.delay(200)
                    }
                }
            }
            _uiState.update { it.copy(watchControlPending = true, statusBannerMessage = "Óra kapcsolat és tulajdonosi ACK ellenőrzése…") }
            try {
                if (com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(active)) {
                    com.repforge.app.NativePhoneReturnInstallation.complete(ctx)
                    showBannerMessage("A telefon visszavette az edzést. Az edzés szünetel; folytatható vagy menthető.")
                    return@launch
                }
                val nodes = kotlinx.coroutines.withTimeout(5_000) {
                    com.google.android.gms.wearable.Wearable.getNodeClient(ctx).connectedNodes.await()
                }
                check(nodes.size == 1) { if (nodes.isEmpty()) "Nincs csatlakoztatott óra; a tulajdonos változatlan." else "Több óra csatlakozik; nem egyértelmű az átadás célja." }
                check(kotlinx.coroutines.withTimeout(5_000) {
                    com.repforge.app.NativeWearCompanion.reachable(ctx, nodes.single().id)
                }) { "A TrainPilot Wear OS alkalmazás nem érhető el. Telepítsd a telefonos APK-val párosított óraalkalmazást." }
                if (active.optString("timerOwner", "phone") == "phone") {
                    check(!com.repforge.app.DistanceTrackingService.snapshot(ctx).optBoolean("active")) {
                        "Aktív GPS-mérés közben nem adható át a vezérlés. Előbb fejezd be a GPS-mérést."
                    }
                    checkNotNull(activity) { "Az óra indításához látható telefonos felület szükséges." }
                    com.repforge.app.StudioWorkoutBridge.pauseForHandover(ctx, id)
                    kotlinx.coroutines.withTimeout(15_000) {
                        WearSyncRepository.startWatchMeasurementRemotely(activity, id, requestOwnership = true).getOrThrow()
                    }
                    // Remote activity launch is a request, not an ownership receipt.
                    awaitOwner("watch")
                } else {
                    val local = com.google.android.gms.wearable.Wearable.getNodeClient(ctx).localNode.await()
                    val journal = PhoneOwnership193.journal(ctx)
                    journal.recover()
                    journal.request(local.id, nodes.single().id, System.currentTimeMillis())
                    com.repforge.shared.OwnershipTransport193.send(ctx, journal)
                    awaitOwner("phone")
                    com.repforge.app.NativePhoneReturnInstallation.complete(ctx)
                }
                val current = checkNotNull(ActiveWorkoutStore.current(ctx))
                check(current.optString("workoutId") == id && !com.repforge.shared.WorkoutOwnership193.pending(current))
                _uiState.update { it.copy(watchConnected = true, watchName = nodes.single().displayName,
                    phoneControlsWorkout = current.optString("timerOwner", "phone") == "phone",
                    lastWatchSyncLabel = "Tulajdonosi ACK igazolva", statusBannerMessage = "Az edzésvezérlés átadása visszaigazolva; az edzés szünetel, explicit Folytatás szükséges.") }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException && error !is kotlinx.coroutines.TimeoutCancellationException) throw error
                runCatching { PhoneOwnership193.journal(ctx).recover() }
                showBannerMessage(error.message ?: "Nem érkezett igazolt tulajdonosi ACK.")
            } finally {
                watchHandoffBusy = false
                val current = ActiveWorkoutStore.current(ctx)
                val pending = com.repforge.app.NativeWatchHandoffReceipt.pending(current, watchJournal())
                _uiState.update { it.copy(watchControlPending = pending,
                    phoneControlsWorkout = (current == null || current.optString("timerOwner", "phone") == "phone") && !pending && !com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(current),
                    lastWatchSyncLabel = if (pending) "Átadás függőben; vezérlés zárolva" else it.lastWatchSyncLabel) }
            }
        }
    }

    private fun muscleItems(rows: List<com.repforge.app.data.db.ProgressMuscleSummary>) =
        NativeProgressQuery.muscle(rows, _uiState.value.language.name.lowercase()).map { m ->
            com.example.ui.components.MuscleLoadItem(
                id = m.muscleKey, nameHu = m.label,
                setsText = "${m.setsCount} sorozat", percentage = (m.loadPercentage * 100).toInt(),
                ratio = m.loadPercentage,
                levelLabelHu = when {
                    m.setsCount == 0 -> "Nincs terhelés"
                    m.loadPercentage >= 0.75f -> "Nagyobb terhelés"
                    m.loadPercentage >= 0.4f -> "Közepes terhelés"
                    else -> "Kisebb terhelés"
                }, topExercisesHu = emptyList(), exerciseIdToOpen = "", setsCount = m.setsCount)
        }

    fun loadMuscleLoadSummary(days: Int = progressPeriodDays(_uiState.value.selectedProgressRangeIndex)) {
        val db = nativeDatabase ?: return
        val scope = nativeScope ?: return
        val window = StudioProgressWindow(days)
        scope.launch(Dispatchers.IO) {
            try {
                val entries = db.workoutHistoryDao().getProgressMuscleWorkoutRows(window.fromEpoch(), window.toEpoch(), _uiState.value.language.name.lowercase())
                val targets = _uiState.value.exercises.associate { it.id to it.primaryMusclesHu }
                val byMuscle = entries.groupBy {
                    NativeProgressQuery.detailedMuscleGroup(it.muscleKey, targets[it.exerciseId].orEmpty())
                }
                val ordered = listOf("chest", "back", "shoulders", "arms", "biceps", "triceps", "forearms",
                    "core", "legs", "quads", "hamstrings", "glutes", "calves")
                val names = listOf("Mell", "Hát", "Váll", "Kar", "Bicepsz", "Tricepsz", "Alkar",
                    "Törzs", "Láb", "Combfeszítő", "Combhajlító", "Far", "Vádli")
                val counts = byMuscle.mapValues { (_, rows) -> rows.sumOf { it.setsCount } }
                val largest = ordered.maxOf { counts[it] ?: 0 }.coerceAtLeast(1)
                val items = ordered.zip(names).mapNotNull { (key, name) ->
                    val count = counts[key] ?: 0
                    if (count == 0 && key !in setOf("chest", "back", "shoulders", "core", "legs")) return@mapNotNull null
                    val ratio = count.toFloat() / largest
                    com.example.ui.components.MuscleLoadItem(key, name, "", (ratio * 100).toInt(),
                        ratio, "", emptyList(), "", count, byMuscle[key].orEmpty())
                }
                withContext(Dispatchers.Main) {
                    _uiState.update { state ->
                        if (progressPeriodDays(state.selectedProgressRangeIndex) == days)
                            state.copy(realMuscleLoads = items, muscleLoadPeriodDays = days) else state
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { android.util.Log.e("TrainPilotViewModel", "Failed to load real muscle summary", e) }
        }
    }

    internal fun loadWorkoutMuscleSummary(workoutId: String) {
        val db = nativeDatabase ?: return
        val scope = nativeScope ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val items = muscleItems(db.workoutHistoryDao().getWorkoutMuscleSummary(workoutId))
                withContext(Dispatchers.Main) {
                    _uiState.update { state ->
                        if (state.selectedLogWorkoutId == workoutId) state.copy(logWorkoutMuscleLoads = items) else state
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { android.util.Log.e("TrainPilotViewModel", "Failed to load workout muscle summary", e) }
        }
    }

    fun exportBackup(context: Context, uri: Uri, format: String) {
        val busy = studioModesText("Mentés készül…", "Creating backup…", "Sicherung wird erstellt…", "Se creează backup-ul…")
        val failed = studioModesText("A mentés nem sikerült.", "Backup failed.", "Sicherung fehlgeschlagen.", "Backup-ul a eșuat.")
        runLocalBackup(busy, failed) {
            check(calendarWriteError == null) { requireNotNull(calendarWriteError) }
            check(plannerSettingsWriteError == null) { requireNotNull(plannerSettingsWriteError) }
            val manager = BackupRestoreManager(context)
            if (format.equals("ZIP", ignoreCase = true)) manager.exportZipArchiveToUri(uri).getOrThrow()
            else manager.exportJsonBackupToUri(uri).getOrThrow()
            studioModesText("A mentés elkészült.", "Backup saved.", "Sicherung gespeichert.", "Backup-ul a fost salvat.")
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        val busy = studioModesText("Visszaállítás folyamatban…", "Restoring backup…", "Sicherung wird eingespielt…", "Se restaurează backup-ul…")
        val failed = studioModesText("A visszaállítás nem sikerült.", "Restore failed.", "Wiederherstellung fehlgeschlagen.", "Restaurarea a eșuat.")
        runLocalBackup(busy, failed) {
            BackupRestoreManager(context).importBackupFromUri(uri, recoverInterrupted = false).getOrThrow()
            withContext(Dispatchers.Main.immediate) { refreshAfterCloudSync(context) }
            studioModesText("A visszaállítás sikerült.", "Backup restored.", "Sicherung wiederhergestellt.", "Backup-ul a fost restaurat.")
        }
    }

    private fun runLocalBackup(busy: String, failed: String, operation: suspend () -> String) {
        val scope = nativeScope ?: return
        if (_uiState.value.localBackupStatus?.busy == true) return
        _uiState.update { it.copy(localBackupStatus = LocalBackupStatus(busy, busy = true)) }
        scope.launch(Dispatchers.IO) {
            try {
                calendarWriteJob?.join()
                plannerSettingsWriteJob?.join()
                val message = operation()
                _uiState.update { it.copy(localBackupStatus = LocalBackupStatus(message)) }
            } catch (cancelled: CancellationException) {
                _uiState.update { it.copy(localBackupStatus = null) }
                throw cancelled
            } catch (error: Exception) {
                val detail = error.localizedMessage?.takeIf { it.isNotBlank() }
                _uiState.update { it.copy(localBackupStatus = LocalBackupStatus(
                    listOfNotNull(failed, detail).joinToString("\n"), failed = true)) }
            }
        }
    }

    fun refreshAfterCloudSync(context: Context) {
        val scope = nativeScope ?: return
        val db = nativeDatabase ?: return
        val store = nativeSettingsStore ?: return
        reloadNativeEnvironment(context, store, db, scope)
    }

    private fun studioModesText(hu: String, en: String, de: String, ro: String): String =
        when (_uiState.value.language) {
            AppLanguage.HU -> hu
            AppLanguage.EN -> en
            AppLanguage.DE -> de
            AppLanguage.RO -> ro
        }

    // Rest/sound/haptic settings are immediate, unlike the separately approved modes.
    // Serialize writes and preserve the user's interval work/round defaults.
    internal val studioTimerWriteMutex = Mutex()

    private fun persistStudioTimerSettings(rest: Int? = null, sound: Boolean? = null, vibration: Boolean? = null) {
        val store = nativeSettingsStore ?: return
        val context = nativeContext ?: return
        val scope = nativeScope ?: return
        val ticket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }.getOrNull() ?: return
        scope.launch(Dispatchers.IO) {
            try {
                studioTimerWriteMutex.withLock { NativeDataReplacement.mutate(context) {
                    ticket.requireCurrent(context)
                    store.updateTimerPreferences(restSeconds = rest, sound = sound, vibration = vibration)
                } }
            } catch (error: Exception) {
                if (error is CancellationException) currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(statusBannerMessage = studioModesText(
                    "Az időzítő beállítása nem menthető.", "Could not save timer settings.",
                    "Timereinstellungen konnten nicht gespeichert werden.", "Setările temporizatorului nu au putut fi salvate.")) }
            }
        }
    }

    fun toggleSoundAlert(enabled: Boolean) {
        _uiState.update { it.copy(soundAlertEnabled = enabled) }
        persistStudioTimerSettings(sound = enabled)
    }

    fun cycleDefaultRestTimer() {
        _uiState.update {
            val next = when (it.defaultRestSec) {
                60 -> 90
                90 -> 120
                120 -> 180
                else -> 60
            }
            it.copy(defaultRestSec = next, statusBannerMessage = studioModesText(
                "Alapértelmezett pihenőidő: $next mp", "Default rest time: $next sec",
                "Standard-Pausenzeit: $next Sek.", "Timp implicit de pauză: $next sec"))
        }
        persistStudioTimerSettings(rest = _uiState.value.defaultRestSec)
    }

    private var pendingStudioMode: com.repforge.app.data.store.NativeTrainingModeChange.Preview? = null
    private var pendingStudioComparison: com.repforge.app.ui.settings.NativeTrainingModeComparison.Snapshot? = null
    private var studioModeGeneration = 0L

    fun toggleProgressionModal(show: Boolean) {
        studioModeGeneration++
        pendingStudioMode = null
        pendingStudioComparison = null
        _uiState.update {
            it.copy(showProgressionModal = show, progressionModeReview = null,
                progressionModeBusy = false)
        }
    }

    fun discardProgressionPreview() {
        studioModeGeneration++
        pendingStudioMode = null
        pendingStudioComparison = null
        _uiState.update { it.copy(progressionModeReview = null, progressionModeBusy = false) }
    }

    /** No writes in preview: approval uses exactly this prepared DataStore snapshot. */
    fun previewProgressionSettings(style: String, rule: String, scopeName: String, inherit: Boolean) {
        val store = nativeSettingsStore ?: return
        val scope = nativeScope ?: return
        if (_uiState.value.progressionModeBusy || !_uiState.value.showProgressionModal) return
        if (scopeName !in setOf("global", "program") || style !in setOf("normal", "hit") ||
            rule !in setOf("double", "linear", "rir") || (inherit && scopeName != "program")) return
        val token = ++studioModeGeneration
        pendingStudioMode = null
        pendingStudioComparison = null
        _uiState.update { it.copy(progressionModeBusy = true, progressionModeReview = null) }
        scope.launch {
            try {
                val saved = store.current()
                val mode = com.repforge.app.data.store.NativeTrainingModeChange
                val proposal = if (inherit) {
                    mode.prepareInherit(saved.trainingModes197Json, saved.activeProgramId, saved.activeProgramId)
                } else {
                    mode.prepare(saved.trainingModes197Json, saved.activeProgramId, scopeName,
                        if (scopeName == "program") saved.activeProgramId else null, style, rule)
                }
                val snapshot = withContext(Dispatchers.IO) {
                    com.repforge.app.ui.settings.NativeTrainingModeComparison.read(
                        requireNotNull(nativeContext), proposal)
                }
                if (studioModeGeneration != token || !_uiState.value.showProgressionModal) return@launch
                pendingStudioMode = proposal
                pendingStudioComparison = snapshot
                _uiState.update { it.copy(
                    progressionModeBusy = false,
                    progressionModeReview = StudioTrainingModeReview(
                        beforeStyle = proposal.before.trainingStyle,
                        beforeProgression = proposal.before.progressionMode,
                        afterStyle = proposal.after.trainingStyle,
                        afterProgression = proposal.after.progressionMode,
                        inheritGlobal = proposal.inheritGlobal,
                        programName = snapshot.programName,
                        rows = snapshot.rows.map { row ->
                            StudioTrainingModeRow(
                                day = row.day, exercise = row.exercise,
                                beforeSets = row.beforeSets, afterSets = row.afterSets,
                                beforeReps = row.beforeReps, afterReps = row.afterReps,
                                restSeconds = row.restSeconds,
                                beforeWeight = row.beforeWeight, afterWeight = row.afterWeight,
                                rir = row.rir, reason = row.reason)
                        })
                ) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (studioModeGeneration == token) _uiState.update { it.copy(
                    progressionModeBusy = false,
                    statusBannerMessage = studioModesText(
                        "Nem sikerült elkészíteni a program-összehasonlítást.",
                        "Could not prepare the program comparison.",
                        "Programmvergleich konnte nicht erstellt werden.",
                        "Nu s-a putut pregăti comparația programului.")) }
            }
        }
    }

    fun approveProgressionSettings() {
        val proposal = pendingStudioMode ?: return
        val comparison = pendingStudioComparison ?: return
        val context = nativeContext ?: return
        val store = nativeSettingsStore ?: return
        val scope = nativeScope ?: return
        if (_uiState.value.progressionModeBusy || _uiState.value.progressionModeReview == null) return
        val token = ++studioModeGeneration
        pendingStudioMode = null
        pendingStudioComparison = null
        _uiState.update { it.copy(progressionModeBusy = true) }
        scope.launch {
            try {
                check(withContext(Dispatchers.IO) {
                    com.repforge.app.ui.settings.NativeTrainingModeComparison.isCurrent(
                        context, proposal, comparison)
                }) { "Az előnézetben használt program időközben megváltozott." }
                check(com.repforge.app.data.store.NativeTrainingModeChange.approve(store, proposal)) {
                    "A beállítás időközben módosult. Nyisd meg újra az előnézetet."
                }
                if (studioModeGeneration == token) _uiState.update { it.copy(
                    showProgressionModal = false, progressionModeReview = null,
                    progressionModeBusy = false, statusBannerMessage = studioModesText(
                        "Edzésmód elmentve.", "Training mode saved.",
                        "Trainingsmodus gespeichert.", "Mod de antrenament salvat.")) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (studioModeGeneration == token) _uiState.update { it.copy(
                    progressionModeReview = null, progressionModeBusy = false,
                    statusBannerMessage = if (error.message?.contains("Finish the active workout") == true)
                        studioModesText(
                            "Az edzésmód csak az aktív edzés befejezése után módosítható.",
                            "Finish the active workout before changing training mode.",
                            "Trainingsmodus erst nach Abschluss des aktiven Trainings ändern.",
                            "Finalizează antrenamentul activ înainte de schimbarea modului.")
                    else studioModesText(
                        "Az edzésmód nem menthető. Nyisd meg újra az előnézetet.",
                        "Could not save the training mode. Reopen the preview.",
                        "Trainingsmodus konnte nicht gespeichert werden. Vorschau erneut öffnen.",
                        "Modul de antrenament nu a putut fi salvat. Redeschide previzualizarea.")) }
            }
        }
    }

    fun undoProgressionSettings(scopeName: String) {
        val store = nativeSettingsStore ?: return
        val scope = nativeScope ?: return
        if (scopeName !in setOf("global", "program") || _uiState.value.progressionModeBusy) return
        val token = ++studioModeGeneration
        pendingStudioMode = null
        pendingStudioComparison = null
        _uiState.update { it.copy(progressionModeBusy = true, progressionModeReview = null) }
        scope.launch {
            try {
                val saved = store.current()
                check(com.repforge.app.data.store.NativeTrainingModeChange.undo(store, scopeName,
                    if (scopeName == "program") saved.activeProgramId else null)) {
                    "Nincs visszavonható módváltás."
                }
                if (studioModeGeneration == token) _uiState.update { it.copy(
                    progressionModeBusy = false, showProgressionModal = false,
                    statusBannerMessage = studioModesText(
                        "Az előző edzésmód visszaállítva.", "Previous training mode restored.",
                        "Vorheriger Trainingsmodus wiederhergestellt.",
                        "Modul de antrenament anterior a fost restaurat.")) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (studioModeGeneration == token) _uiState.update { it.copy(
                    progressionModeBusy = false,
                    statusBannerMessage = studioModesText(
                        "A módváltás nem vonható vissza.", "Could not undo training mode change.",
                        "Trainingsmoduswechsel konnte nicht rückgängig gemacht werden.",
                        "Schimbarea modului nu a putut fi anulată.")) }
            }
        }
    }

    fun toggleVibrationAlert(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
        persistStudioTimerSettings(vibration = enabled)
    }

    fun selectBackupFormat(format: String) {
        _uiState.update { it.copy(selectedBackupFormat = format) }
    }

    fun toggleIncludeHealthInBackup(include: Boolean) {
        val ctx = nativeContext ?: return
        val store = nativeSettingsStore ?: return
        val scope = nativeScope ?: return
        if (_uiState.value.backupHealthConsentSaving) return
        _uiState.update { it.copy(backupHealthConsentSaving = true) }
        scope.launch(Dispatchers.IO) {
            try {
                NativeDataReplacement.mutate(ctx) { store.setHealthBackupConsent(include) }
                _uiState.update { it.copy(includeHealthInBackup = include) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(statusBannerMessage = "A mentési hozzájárulás nem menthető. Próbáld újra.") } }
            finally { _uiState.update { it.copy(backupHealthConsentSaving = false) } }
        }
    }

    fun requestWatchDeviceInfo() {
        val context = nativeContext ?: return
        val scope = nativeScope ?: return
        // Keep the single in-flight native request and its node/nonce/timeout protections.
        if (_uiState.value.watchInfoLoading) return
        _uiState.update { it.copy(
            watchInfoLoading = true, watchInfoMessage = "Óraadatok lekérése…",
            watchInfoCardData = it.watchInfoCardData.copy(
                loading = true, status = "Óraadatok lekérése…", statusKind = WatchInfoStatus.IDLE)
        ) }
        scope.launch {
            try {
                val result = com.repforge.app.NativeWatchDeviceInfo.request(context)
                val data = result.data
                val battery = if (data.isNull("battery")) -1 else
                    com.repforge.shared.WatchDeviceInfo.battery(data.optInt("battery", -1)) ?: -1
                val time = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                val verifiedCard = WatchInfoCardData.fromVerifiedReply(
                    nodeName = result.nodeName, reply = data,
                    connected = true, appAvailable = true,
                    status = "Óraadatok frissítve: $time",
                    statusKind = WatchInfoStatus.SUCCESS, lastSync = time)
                _uiState.update { it.copy(
                    watchInfoCardData = verifiedCard,
                    watchName = result.nodeName, watchConnected = true, watchAppAvailable = true,
                    watchBatteryPercent = battery, watchInfoMessage = "Óraadatok frissítve: $time",
                    watchDeviceDetails = listOf(data.optString("model"),
                        data.optString("os").takeIf { it.isNotBlank() }?.let { "Android $it" },
                        data.optString("appVersion").takeIf { it.isNotBlank() && it != "null" }?.let { "TrainPilot $it" })
                        .filterNotNull().filter(String::isNotBlank).joinToString(" • "),
                    lastWatchSyncLabel = "Óraadatok: $time") }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                val msg = "Az óra nem válaszolt. Ellenőrizd az órakapcsolatot és frissítsd az óraappot is."
                _uiState.update { it.copy(watchInfoMessage = msg,
                    watchInfoCardData = it.watchInfoCardData.copy(
                        status = msg, statusKind = WatchInfoStatus.TIMEOUT, loading = false)) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                val msg = error.message ?: "Az óraadatok nem kérhetők le."
                _uiState.update { it.copy(watchInfoMessage = msg,
                    watchInfoCardData = it.watchInfoCardData.copy(
                        status = msg, statusKind = WatchInfoStatus.ERROR, loading = false)) }
            } finally {
                _uiState.update { it.copy(watchInfoLoading = false,
                    watchInfoCardData = it.watchInfoCardData.copy(loading = false)) }
            }
        }
    }

    fun updatePlatformAccess(access: JSONObject, watchNames: List<String>, companionAvailable: Boolean = false) {
        val frame = nativeContext?.let { ActiveWorkoutStore.current(it) }
        val journal = watchJournal()
        val pending = watchHandoffBusy || com.repforge.app.NativeWatchHandoffReceipt.pending(frame, journal)
        val confirmed = frame?.let { com.repforge.app.NativeWatchHandoffReceipt.confirmed(
            it, journal, it.optString("workoutId"), it.optString("timerOwner")) } == true
        val companionReady = companionAvailable && watchNames.size == 1
        _uiState.update {
            val oldCard = it.watchInfoCardData
            val sameNode = companionReady && oldCard.name == watchNames.singleOrNull() && oldCard.appAvailable
            val nextCard = if (sameNode) oldCard.copy(connected = true, appAvailable = true)
                else WatchInfoCardData(
                    connected = watchNames.isNotEmpty(),
                    appAvailable = companionReady,
                    name = watchNames.singleOrNull()
                )
            it.copy(permissionAccess = it.permissionAccess.copy(
                gpsEnabled = access.optBoolean("gps"),
                gpsGranted = access.optBoolean("gpsGranted"),
                centralLocationEnabled = access.optBoolean("location"),
                notificationsEnabled = access.optBoolean("notifications"),
                notificationsGranted = access.optBoolean("notificationsGranted"),
                healthConnectEnabled = access.optBoolean("healthConnect"),
                healthConnectGranted = access.optBoolean("healthReadGranted"),
                samsungHealthEnabled = access.optBoolean("samsungHealth")),
                healthGrantedCount = access.optInt("healthGranted"),
                healthPermissionCount = access.optInt("healthTotal"),
                watchControlPending = pending,
                phoneControlsWorkout = (frame == null || frame.optString("timerOwner", "phone") == "phone") &&
                    !pending && !com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(frame),
                watchAppAvailable = companionReady,
                watchConnected = watchNames.isNotEmpty(),
                watchName = if (companionReady) watchNames.single() else "Nincs csatlakoztatott Wear OS óra",
                watchInfoCardData = nextCard,
                watchBatteryPercent = if (sameNode) it.watchBatteryPercent else -1,
                lastWatchSyncLabel = if (!companionReady) "Nincs órakapcsolat"
                    else if (confirmed) "Tulajdonosi ACK igazolva"
                    else if (it.lastWatchSyncLabel in setOf("—", "Nincs órakapcsolat", "Nincs óra kapcsolat"))
                        "Még nincs igazolt adatszinkron" else it.lastWatchSyncLabel)
        }
    }

    fun updatePrivacyChoice(feature: String, enabled: Boolean) {
        // Only called after the persistent preference commit succeeds. Unlike OS grants,
        // this is the state the switch represents and should update without a Wear query.
        _uiState.update { state ->
            when (feature) {
                com.repforge.app.NativePrivacyControls.GPS -> state.copy(permissionAccess = state.permissionAccess.copy(gpsEnabled = enabled))
                com.repforge.app.NativePrivacyControls.NOTIFICATIONS -> state.copy(permissionAccess = state.permissionAccess.copy(notificationsEnabled = enabled))
                com.repforge.app.NativePrivacyControls.HEALTH_CONNECT -> state.copy(permissionAccess = state.permissionAccess.copy(healthConnectEnabled = enabled))
                com.repforge.app.NativePrivacyControls.SAMSUNG_HEALTH -> state.copy(permissionAccess = state.permissionAccess.copy(samsungHealthEnabled = enabled))
                else -> state
            }
        }
    }

    fun updateSamsungAccess(granted: Boolean) { _uiState.update { it.copy(permissionAccess = it.permissionAccess.copy(samsungHealthGranted = granted)) } }

    fun showBannerMessage(message: String) {
        _uiState.update { it.copy(statusBannerMessage = message) }
    }

    fun clearBannerMessage() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun syncNativeSettings(
        languageCode: String,
        themeKey: String,
        restSec: Int = 90,
        sound: Boolean = true,
        vibration: Boolean = true
    ) {
        val lang = when (languageCode.lowercase()) {
            "en" -> AppLanguage.EN
            "de" -> AppLanguage.DE
            "ro" -> AppLanguage.RO
            else -> AppLanguage.HU
        }
        val palette = com.example.ui.theme.TrainPilotPalettes.getByKey(themeKey)
        _uiState.update { current ->
            current.copy(
                language = lang,
                selectedPaletteId = palette?.id ?: TrainPilotPalettes.getById(1).id,
                defaultRestSec = if (restSec > 0) restSec else current.defaultRestSec,
                soundAlertEnabled = sound,
                vibrationEnabled = vibration
            )
        }
    }
}

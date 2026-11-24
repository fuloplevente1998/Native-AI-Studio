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
    @Volatile internal var plannerSettingsWriteJob: Job? = null
    @Volatile internal var plannerSettingsWriteError: String? = null
    internal var nativeSettingsStore: TrainPilotSettingsStore? = null
    internal var nativeDatabase: TrainPilotDatabase? = null
    internal var nativeScope: CoroutineScope? = null
    internal var paletteWriteJob: Job? = null
    internal var nativeReloadJob: Job? = null
    internal var programEditorContinuation: com.repforge.app.NativeDataReplacement.Continuation? = null
    private var nativeObserversJob: Job? = null
    internal var personalRecordsJob: Job? = null
    internal var personalRecordsRevision = 0L
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

    internal suspend fun readNativeWorkoutHistory(database: TrainPilotDatabase,
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

    internal fun reloadNativeEnvironment(
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
    internal val pendingWatchMeasurementStart = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

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

    /** Provider whose values are on screen now; null = nothing loaded (or cleared). */
    @Volatile internal var loadedHealthProvider: String? = null
    internal var healthJournalReloadJob: Job? = null

    // Rest/sound/haptic settings are immediate, unlike the separately approved modes.
    // Serialize writes and preserve the user's interval work/round defaults.
    internal val studioTimerWriteMutex = Mutex()

    internal var pendingStudioMode: com.repforge.app.data.store.NativeTrainingModeChange.Preview? = null
    internal var pendingStudioComparison: com.repforge.app.ui.settings.NativeTrainingModeComparison.Snapshot? = null
    internal var studioModeGeneration = 0L
}

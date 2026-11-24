package com.example.ui.viewmodel

import kotlinx.coroutines.sync.withLock
import java.time.YearMonth
import com.example.model.CalendarDayEvent
import com.example.model.CalendarEventStatus
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.repforge.app.NativeDataReplacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun TrainPilotViewModel.requestCalendarClear() {
    val context = nativeContext ?: return
    val db = nativeDatabase ?: return
    val scope = nativeScope ?: return
    if (_uiState.value.calendarClearBusy || !calendarProjectionCurrent()) return
    val ticket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }.getOrElse {
        showBannerMessage(com.example.localization.CalendarFeedback.clearFailed(_uiState.value.language)); return
    }
    val origin = _uiState.value.navigationRoute()
    _uiState.update { it.copy(calendarClearBusy = true) }
    scope.launch {
        try {
            calendarWriteJob?.join()
            check(calendarWriteError == null)
            com.repforge.app.NativeProgramEditor.assertNoActiveWorkout(context)
            val revision = calendarWriteRevision
            val preview = withContext(Dispatchers.IO) { com.repforge.app.NativeCalendarClear.preview(db) }
            ticket.requireCurrent(context)
            check(revision == calendarWriteRevision)
            if (origin != _uiState.value.navigationRoute()) return@launch
            calendarClearPreview = preview; calendarClearTicket = ticket; calendarClearRevision = revision
            _uiState.update { it.copy(showCalendarClearDialog = true) }
        } catch (error: Exception) {
            if (error is CancellationException) currentCoroutineContext().ensureActive()
            showBannerMessage(com.example.localization.CalendarFeedback.clearFailed(_uiState.value.language))
        } finally { _uiState.update { it.copy(calendarClearBusy = false) } }
    }
}

fun TrainPilotViewModel.cancelCalendarClear() {
    calendarClearPreview = null; calendarClearTicket = null
    _uiState.update { it.copy(showCalendarClearDialog = false) }
}

fun TrainPilotViewModel.clearCalendarPlans() {
    val context = nativeContext ?: return
    val db = nativeDatabase ?: return
    val scope = nativeScope ?: return
    if (_uiState.value.calendarClearBusy) return
    val preview = calendarClearPreview ?: return
    val ticket = calendarClearTicket ?: return
    val revision = calendarClearRevision
    cancelCalendarClear()
    _uiState.update { it.copy(calendarClearBusy = true) }
    scope.launch {
        try {
            calendarWriteJob?.join()
            val removed = withContext(Dispatchers.IO) { calendarWriteMutex.withLock {
                check(revision == calendarWriteRevision && calendarWriteError == null)
                com.repforge.app.NativeCalendarClear.commit(context, db, preview, ticket)
            } }
            ticket.requireCurrent(context)
            ++calendarWriteRevision
            _uiState.update { it.copy(calendarEvents = it.calendarEvents.filterNot { row -> row.id in removed.ids },
                calendarUndoStack = emptyList(), statusBannerMessage =
                    com.example.localization.CalendarFeedback.cleared(it.language, removed.count)) }
        } catch (error: Exception) {
            if (error is CancellationException) currentCoroutineContext().ensureActive()
            showBannerMessage(com.example.localization.CalendarFeedback.clearFailed(_uiState.value.language))
        } finally { _uiState.update { it.copy(calendarClearBusy = false) } }
    }
}

// Calendar & Replan Actions
fun TrainPilotViewModel.selectCalendarDay(day: Int) {
    _uiState.update { it.copy(selectedCalendarDay = day) }
}

fun TrainPilotViewModel.toggleReplanModal(show: Boolean) {
    _uiState.update { it.copy(showReplanModal = show) }
}

fun TrainPilotViewModel.updatePlanningParameters(
    cadenceHu: String = _uiState.value.planningCadenceHu,
    firstDayHu: String = _uiState.value.planningFirstDayHu,
    startDate: String = _uiState.value.planningStartDate,
    time: String = _uiState.value.planningTime,
    durationMin: Int = _uiState.value.planningDurationMin,
    weeksCount: Int = _uiState.value.planningWeeksCount
) {
    if (!calendarProjectionCurrent()) return
    _uiState.update {
        it.copy(
            planningCadenceHu = cadenceHu,
            planningFirstDayHu = firstDayHu,
            planningStartDate = startDate,
            planningTime = time,
            planningDurationMin = durationMin,
            planningWeeksCount = weeksCount
        )
    }
    persistStudioPlannerSettings()
}

fun TrainPilotViewModel.selectCalendarMonth(month: String) {
    val parsed = YearMonth.parse(month)
    _uiState.update { it.copy(calendarYearMonth = parsed.toString(),
        selectedCalendarDay = it.selectedCalendarDay.coerceAtMost(parsed.lengthOfMonth()),
        planningStartDate = parsed.atDay(it.selectedCalendarDay.coerceAtMost(parsed.lengthOfMonth()))
            .format(DateTimeFormatter.ofPattern("yyyy. MM. dd."))) }
}

fun TrainPilotViewModel.planMonthlyWorkouts() {
    if (_uiState.value.calendarClearBusy || !calendarProjectionCurrent()) return
    _uiState.update { com.example.model.StudioCalendarPlanning.plan(it) }
    persistStudioPlannerSettings()
    persistCalendarStateToRoomAsync()
}

fun TrainPilotViewModel.applyCalendarReplan(newStartDate: String, firstDayHu: String, programId: String? = null) {
    if (_uiState.value.calendarClearBusy || !calendarProjectionCurrent()) return
    val current = _uiState.value
    if (current.libraryMutationBusy || (programId != null && current.programs.none { it.id == programId })) return
    if (programId != null) activateProgram(programId)
    _uiState.update { state ->
        com.example.model.StudioCalendarPlanning.plan(state.copy(
            planningStartDate = newStartDate,
            planningFirstDayHu = firstDayHu
        )).copy(showReplanModal = false)
    }
    persistStudioPlannerSettings()
    persistCalendarStateToRoomAsync()
}

fun TrainPilotViewModel.modifyCalendarEvent(
    eventId: String,
    newTitleHu: String,
    newSplitBadge: String,
    newTimeLabel: String,
    newDurationMin: Int
) {
    if (_uiState.value.calendarClearBusy || !calendarProjectionCurrent()) return
    _uiState.update { state ->
        val updated = state.calendarEvents.map { ev ->
            if (ev.id == eventId) {
                ev.copy(
                    titleHu = newTitleHu,
                    splitBadge = newSplitBadge,
                    timeLabel = newTimeLabel,
                    durationMinutes = newDurationMin,
                    dayId = state.programs.firstOrNull { it.id == ev.programId }?.days
                        ?.firstOrNull { it.dayCode.substringBefore(' ').equals(newSplitBadge, true) }
                        ?.id?.removePrefix("${ev.programId}:") ?: newSplitBadge.lowercase()
                )
            } else ev
        }
        state.copy(
            calendarUndoStack = (state.calendarUndoStack + listOf(state.calendarEvents)).takeLast(12),
            calendarEvents = updated,
            statusBannerMessage = com.example.localization.CalendarFeedback.modified(state.language, newSplitBadge, newTimeLabel)
        )
    }
    persistCalendarStateToRoomAsync()
}

fun TrainPilotViewModel.moveCalendarEvent(eventId: String, targetDay: Int, shiftFollowingPlanned: Boolean = true) {
    if (_uiState.value.calendarClearBusy || !calendarProjectionCurrent()) return
    _uiState.update { state ->
        val target = state.calendarEvents.firstOrNull { it.id == eventId } ?: return@update state
        val month = YearMonth.from(LocalDate.parse(target.dateIso))
        val clampedDay = targetDay.coerceIn(1, month.lengthOfMonth())
        val delta = clampedDay - target.dayOfMonth
        val updated = state.calendarEvents.mapNotNull { ev ->
            when {
                ev.id == eventId -> ev.copy(dayOfMonth = clampedDay, dateIso = month.atDay(clampedDay).toString())
                shiftFollowingPlanned && ev.status == CalendarEventStatus.PLANNED && ev.dateIso > target.dateIso && ev.dateIso.startsWith(month.toString()) -> {
                    val nextD = ev.dayOfMonth + delta
                    if (nextD in 1..month.lengthOfMonth()) ev.copy(dayOfMonth = nextD, dateIso = month.atDay(nextD).toString()) else ev
                }
                else -> ev
            }
        }.sortedBy { it.dayOfMonth }

        state.copy(
            calendarUndoStack = (state.calendarUndoStack + listOf(state.calendarEvents)).takeLast(12),
            calendarEvents = updated,
            selectedCalendarDay = clampedDay,
            statusBannerMessage = com.example.localization.CalendarFeedback.moved(state.language, month.atDay(clampedDay).toString())
        )
    }
    persistCalendarStateToRoomAsync()
}

fun TrainPilotViewModel.deleteCalendarEvent(eventId: String) {
    if (_uiState.value.calendarClearBusy || !calendarProjectionCurrent()) return
    _uiState.update { state ->
        val updated = state.calendarEvents.filterNot { it.id == eventId }
        state.copy(
            calendarUndoStack = (state.calendarUndoStack + listOf(state.calendarEvents)).takeLast(12),
            calendarEvents = updated,
            statusBannerMessage = com.example.localization.CalendarFeedback.deleted(state.language)
        )
    }
    persistCalendarStateToRoomAsync()
}

fun TrainPilotViewModel.addPlannedWorkoutOnDay(day: Int, splitBadge: String, timeLabel: String, durationMin: Int) {
    if (_uiState.value.calendarClearBusy || !calendarProjectionCurrent()) return
    _uiState.update { state ->
        val program = state.programs.firstOrNull { it.isActive }
        val selectedDay = program?.days?.firstOrNull { it.dayCode.substringBefore(' ').equals(splitBadge, true) }
        val activeProgName = program?.localizedName(state.language) ?: "TrainPilot"
        val newEv = CalendarDayEvent(
            id = "ev_manual_${day}_${System.currentTimeMillis()}",
            dayOfMonth = day.coerceIn(1, YearMonth.parse(state.calendarYearMonth).lengthOfMonth()),
            timeLabel = timeLabel,
            titleHu = "$activeProgName • ${selectedDay?.localizedDayCode(state.language) ?: "$splitBadge nap"}",
            exerciseCount = selectedDay?.exercises?.size ?: 0,
            durationMinutes = durationMin,
            status = CalendarEventStatus.PLANNED,
            dateIso = YearMonth.parse(state.calendarYearMonth).atDay(day.coerceIn(1, YearMonth.parse(state.calendarYearMonth).lengthOfMonth())).toString(),
            splitBadge = splitBadge,
            programId = program?.id.orEmpty(), dayId = selectedDay?.id?.removePrefix("${program?.id}:").orEmpty()
        )
        state.copy(
            calendarUndoStack = (state.calendarUndoStack + listOf(state.calendarEvents)).takeLast(12),
            calendarEvents = (state.calendarEvents + newEv).sortedBy { it.dayOfMonth },
            statusBannerMessage = com.example.localization.CalendarFeedback.added(state.language, newEv.dateIso, splitBadge)
        )
    }
    persistCalendarStateToRoomAsync()
}

fun TrainPilotViewModel.undoCalendarAction() {
    if (_uiState.value.calendarClearBusy || !calendarProjectionCurrent()) return
    _uiState.update { state ->
        val previous = state.calendarUndoStack.lastOrNull() ?: return@update state.copy(
            statusBannerMessage = com.example.localization.CalendarFeedback.undoEmpty(state.language)
        )
        state.copy(
            calendarEvents = previous,
            calendarUndoStack = state.calendarUndoStack.dropLast(1),
            statusBannerMessage = com.example.localization.CalendarFeedback.undone(state.language)
        )
    }
    persistCalendarStateToRoomAsync()
}

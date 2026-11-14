package com.example.model

import com.example.ui.viewmodel.TrainPilotUiState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/** One planning rule shared by initial planning and replanning. End date is exclusive. */
internal object StudioCalendarPlanning {
    fun startDate(raw: String, fallback: LocalDate): LocalDate = runCatching {
        LocalDate.parse(raw.trim())
    }.recoverCatching {
        val parts = raw.split('.').mapNotNull { it.trim().toIntOrNull() }
        require(parts.size == 3)
        LocalDate.of(parts[0], parts[1], parts[2])
    }.getOrDefault(fallback)

    fun dates(start: LocalDate, weeks: Int, cadence: String): List<LocalDate> {
        val end = start.plusWeeks(weeks.coerceIn(1, 12).toLong())
        val weekdays = when {
            cadence.contains("3") -> setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            cadence.contains("4") -> setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            else -> null
        }
        return generateSequence(start) { it.plusDays(1) }.takeWhile { it < end }.filter { day ->
            when {
                weekdays != null -> day.dayOfWeek in weekdays
                cadence.contains("Minden nap", ignoreCase = true) -> true
                else -> java.time.temporal.ChronoUnit.DAYS.between(start, day) % 2L == 0L
            }
        }.toList()
    }

    fun plan(state: TrainPilotUiState): TrainPilotUiState {
        val month = YearMonth.parse(state.calendarYearMonth)
        val start = startDate(state.planningStartDate,
            month.atDay(state.selectedCalendarDay.coerceIn(1, month.lengthOfMonth())))
        val end = start.plusWeeks(state.planningWeeksCount.coerceIn(1, 12).toLong())
        fun date(event: CalendarDayEvent): LocalDate = runCatching { LocalDate.parse(event.dateIso) }
            .getOrDefault(month.atDay(event.dayOfMonth.coerceIn(1, month.lengthOfMonth())))
        val retained = state.calendarEvents.filter { event ->
            event.status != CalendarEventStatus.PLANNED || date(event) < start || date(event) >= end
        }.map { event ->
            // Freeze legacy day-only rows in their original month before moving the viewport.
            if (event.dateIso.isBlank()) event.copy(dateIso = date(event).toString()) else event
        }
        val completedDates = retained.filter { it.status == CalendarEventStatus.COMPLETED }.map(::date).toSet()
        val program = state.programs.firstOrNull { it.isActive }
        val days = program?.days.orEmpty()
        var index = if (state.planningFirstDayHu.contains("B", true) && days.size > 1) 1 else 0
        val planned = dates(start, state.planningWeeksCount, state.planningCadenceHu)
            .filterNot { it in completedDates }.map { date ->
                val selected = days.getOrNull(index % days.size.coerceAtLeast(1))
                val dayId = selected?.id?.removePrefix("${program?.id}:").orEmpty()
                val split = selected?.dayCode?.substringBefore(' ') ?: if (index % 2 == 0) "A" else "B"
                CalendarDayEvent(
                    id = "ev_plan_${UUID.randomUUID()}", dayOfMonth = date.dayOfMonth,
                    dateIso = date.toString(), timeLabel = state.planningTime,
                    titleHu = "${program?.localizedName(state.language) ?: "TrainPilot"} • ${selected?.localizedDayCode(state.language) ?: "$split nap"}",
                    exerciseCount = selected?.exercises?.size ?: 0, durationMinutes = state.planningDurationMin,
                    status = CalendarEventStatus.PLANNED, splitBadge = split,
                    programId = program?.id.orEmpty(), dayId = dayId
                ).also { index++ }
            }
        return state.copy(
            calendarUndoStack = (state.calendarUndoStack + listOf(state.calendarEvents)).takeLast(12),
            calendarEvents = (retained + planned).sortedWith(compareBy({ date(it) }, { it.timeLabel })),
            calendarYearMonth = YearMonth.from(start).toString(), selectedCalendarDay = start.dayOfMonth,
            statusBannerMessage = when (state.language) {
                AppLanguage.HU -> "Edzések megtervezve (${planned.size})"
                AppLanguage.EN -> "Workouts planned (${planned.size})"
                AppLanguage.DE -> "Trainings geplant (${planned.size})"
                AppLanguage.RO -> "Antrenamente planificate (${planned.size})"
            }
        )
    }
}

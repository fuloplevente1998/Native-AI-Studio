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
        }
        val completedDates = retained.filter { it.status == CalendarEventStatus.COMPLETED }.map(::date).toSet()
        val program = state.programs.firstOrNull { it.isActive }
        var split = if (state.planningFirstDayHu.contains("B", true)) "B" else "A"
        val planned = dates(start, state.planningWeeksCount, state.planningCadenceHu)
            .filterNot { it in completedDates }.map { day ->
                CalendarDayEvent(
                    id = "ev_plan_${UUID.randomUUID()}", dayOfMonth = day.dayOfMonth,
                    dateIso = day.toString(), timeLabel = state.planningTime,
                    titleHu = "${program?.nameHu ?: "TrainPilot"} • Alap $split",
                    exerciseCount = 6, durationMinutes = state.planningDurationMin,
                    status = CalendarEventStatus.PLANNED, splitBadge = split
                ).also { split = if (split == "A") "B" else "A" }
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

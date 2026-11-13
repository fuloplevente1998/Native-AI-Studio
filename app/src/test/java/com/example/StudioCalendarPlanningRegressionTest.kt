package com.example

import com.example.model.*
import com.example.ui.viewmodel.TrainPilotUiState
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class StudioCalendarPlanningRegressionTest {
    @Test fun weeklyCadencesHonorWeekdaysAcrossYearAndMonthBoundaries() {
        val start = LocalDate.of(2026, 12, 29)
        val three = StudioCalendarPlanning.dates(start, 2, "Heti 3 nap (H-Sze-P)")
        assertEquals(6, three.size)
        assertTrue(three.all { it.dayOfWeek in setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY) })
        assertEquals(LocalDate.of(2026,12,30), three.first())
        val four = StudioCalendarPlanning.dates(start, 2, "Heti 4 nap")
        assertEquals(8, four.size)
        assertTrue(four.all { it.dayOfWeek in setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY) })
    }
    @Test fun weekCountAndDailyOrAlternatingRhythmCoverTheEntireSelectedSpan() {
        val start = LocalDate.of(2026,11,29)
        assertEquals(84, StudioCalendarPlanning.dates(start,12,"Minden nap").size)
        assertEquals(42, StudioCalendarPlanning.dates(start,12,"Minden második nap").size)
        assertEquals(start.plusDays(82), StudioCalendarPlanning.dates(start,12,"Minden második nap").last())
        assertEquals(start, StudioCalendarPlanning.startDate("2026. 11. 29.", start.minusDays(1)))
        assertEquals(start, StudioCalendarPlanning.startDate(start.toString(), start.minusDays(1)))
    }
    @Test fun replanPreservesCompletedDatesAndPlansPastTheCurrentMonth() {
        val initial = TrainPilotUiState(calendarYearMonth="2026-11", planningStartDate="2026. 11. 29.",
            planningWeeksCount=2,planningCadenceHu="Minden nap",calendarEvents=emptyList())
        val planned = StudioCalendarPlanning.plan(initial)
        assertEquals(14,planned.calendarEvents.size)
        val complete = planned.calendarEvents.first().copy(status=CalendarEventStatus.COMPLETED)
        val replan = StudioCalendarPlanning.plan(planned.copy(calendarEvents=listOf(complete),planningCadenceHu="Minden második nap"))
        assertEquals(1,replan.calendarEvents.count { it.dateIso==complete.dateIso })
        assertEquals(complete,replan.calendarEvents.first())
        assertTrue(replan.calendarEvents.any { it.dateIso.startsWith("2026-12") })
    }
}

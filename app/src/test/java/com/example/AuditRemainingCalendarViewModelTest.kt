package com.example

import com.example.model.AppLanguage
import com.example.model.CalendarDayEvent
import com.example.model.CalendarEventStatus
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.TrainPilotViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test
import com.example.ui.viewmodel.addPlannedWorkoutOnDay
import com.example.ui.viewmodel.deleteCalendarEvent
import com.example.ui.viewmodel.modifyCalendarEvent
import com.example.ui.viewmodel.moveCalendarEvent
import com.example.ui.viewmodel.undoCalendarAction

class AuditRemainingCalendarViewModelTest {
    private data class Expected(val modified: String, val moved: String, val deleted: String,
        val added: String, val empty: String, val undone: String)

    @Test fun calendarActionsPublishLocalizedFeedbackAndKeepEventData() {
        val expectations = mapOf(
            AppLanguage.HU to Expected("Edzésnap módosítva (A/B nap • 09:30)", "Edzés áthelyezve: 2026-11-12.", "Tervezett edzés törölve (Visszavonható)", "Edzés hozzáadva: 2026-11-15. (Alap A/B)", "Nincs visszavonható naptárművelet", "Utolsó naptárművelet visszavonva"),
            AppLanguage.EN to Expected("Workout day updated (day A/B • 09:30)", "Workout moved to: 2026-11-12.", "Planned workout deleted (can be undone)", "Workout added: 2026-11-15. (Default A/B)", "No calendar action to undo", "Last calendar action undone"),
            AppLanguage.DE to Expected("Trainingstag geändert (Tag A/B • 09:30)", "Training verschoben auf: 2026-11-12.", "Geplantes Training gelöscht (kann rückgängig gemacht werden)", "Training hinzugefügt: 2026-11-15. (Standard A/B)", "Keine Kalenderaktion zum Rückgängigmachen", "Letzte Kalenderaktion rückgängig gemacht"),
            AppLanguage.RO to Expected("Ziua de antrenament modificată (ziua A/B • 09:30)", "Antrenament mutat la: 2026-11-12.", "Antrenament planificat șters (poate fi anulat)", "Antrenament adăugat: 2026-11-15. (Implicit A/B)", "Nu există nicio acțiune din calendar de anulat", "Ultima acțiune din calendar a fost anulată")
        )
        expectations.forEach { (language, expected) ->
            val vm = TrainPilotViewModel()
            val field = TrainPilotViewModel::class.java.getDeclaredField("_uiState").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST") val state = field.get(vm) as MutableStateFlow<TrainPilotUiState>
            val event = CalendarDayEvent("original", 10, "08:00", "Custom name", 2, 60,
                CalendarEventStatus.PLANNED, dateIso = "2026-11-10")
            state.value = state.value.copy(language = language, calendarYearMonth = "2026-11", calendarEvents = listOf(event), calendarUndoStack = emptyList())
            vm.undoCalendarAction()
            assertEquals(expected.empty, vm.uiState.value.statusBannerMessage)
            vm.modifyCalendarEvent("original", "Renamed custom", "A/B", "09:30", 45)
            assertEquals(expected.modified, vm.uiState.value.statusBannerMessage)
            assertEquals("original", vm.uiState.value.calendarEvents.single().id)
            assertEquals("Renamed custom", vm.uiState.value.calendarEvents.single().titleHu)
            vm.moveCalendarEvent("original", 12)
            assertEquals(expected.moved, vm.uiState.value.statusBannerMessage)
            assertEquals("Renamed custom", vm.uiState.value.calendarEvents.single().titleHu)
            vm.deleteCalendarEvent("original")
            assertEquals(expected.deleted, vm.uiState.value.statusBannerMessage)
            assertTrue(vm.uiState.value.calendarEvents.isEmpty())
            vm.undoCalendarAction()
            assertEquals(expected.undone, vm.uiState.value.statusBannerMessage)
            assertEquals("original", vm.uiState.value.calendarEvents.single().id)
            assertEquals("Renamed custom", vm.uiState.value.calendarEvents.single().titleHu)
            vm.addPlannedWorkoutOnDay(15, "A/B", "09:30", 45)
            assertEquals(expected.added, vm.uiState.value.statusBannerMessage)
            assertEquals("Renamed custom", vm.uiState.value.calendarEvents.first { it.id == "original" }.titleHu)
            assertEquals(2, vm.uiState.value.calendarEvents.size)
            vm.undoCalendarAction()
            assertEquals(expected.undone, vm.uiState.value.statusBannerMessage)
            assertEquals("original", vm.uiState.value.calendarEvents.single().id)
        }
    }
}

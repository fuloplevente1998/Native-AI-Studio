package com.example.localization

import com.example.model.AppLanguage

internal object CalendarFeedback {
    fun cleared(language: AppLanguage, count: Int) = when (language) {
        AppLanguage.HU -> "$count tervezett edzés törölve. A napló megmaradt."
        AppLanguage.EN -> "$count planned workouts cleared. Workout history was kept."
        AppLanguage.DE -> "$count geplante Trainings entfernt. Das Protokoll bleibt erhalten."
        AppLanguage.RO -> "$count antrenamente planificate șterse. Jurnalul a fost păstrat."
    }
    fun clearFailed(language: AppLanguage) = when (language) {
        AppLanguage.HU -> "A naptár megváltozott vagy foglalt. Nyisd meg újra az ürítést."
        AppLanguage.EN -> "The calendar changed or is busy. Reopen the clear action."
        AppLanguage.DE -> "Der Kalender wurde geändert oder ist belegt. Öffne die Leerung erneut."
        AppLanguage.RO -> "Calendarul s-a modificat sau este ocupat. Redeschide ștergerea."
    }
    fun plannedProgram(language: AppLanguage) = when (language) {
        AppLanguage.HU -> "A programnak még van tervezett edzése. Előbb töröld a terveket a naptárból."
        AppLanguage.EN -> "This program still has planned workouts. Clear those calendar plans first."
        AppLanguage.DE -> "Für dieses Programm sind noch Trainings geplant. Entferne zuerst die Kalenderpläne."
        AppLanguage.RO -> "Programul are încă antrenamente planificate. Șterge mai întâi planurile din calendar."
    }

    fun modified(language: AppLanguage, badge: String, time: String) = when (language) {
        AppLanguage.HU -> "Edzésnap módosítva ($badge nap • $time)"
        AppLanguage.EN -> "Workout day updated (day $badge • $time)"
        AppLanguage.DE -> "Trainingstag geändert (Tag $badge • $time)"
        AppLanguage.RO -> "Ziua de antrenament modificată (ziua $badge • $time)"
    }
    fun moved(language: AppLanguage, date: String) = when (language) {
        AppLanguage.HU -> "Edzés áthelyezve: $date."
        AppLanguage.EN -> "Workout moved to: $date."
        AppLanguage.DE -> "Training verschoben auf: $date."
        AppLanguage.RO -> "Antrenament mutat la: $date."
    }
    fun deleted(language: AppLanguage) = when (language) {
        AppLanguage.HU -> "Tervezett edzés törölve (Visszavonható)"
        AppLanguage.EN -> "Planned workout deleted (can be undone)"
        AppLanguage.DE -> "Geplantes Training gelöscht (kann rückgängig gemacht werden)"
        AppLanguage.RO -> "Antrenament planificat șters (poate fi anulat)"
    }
    fun added(language: AppLanguage, date: String, badge: String) = when (language) {
        AppLanguage.HU -> "Edzés hozzáadva: $date. (Alap $badge)"
        AppLanguage.EN -> "Workout added: $date. (Default $badge)"
        AppLanguage.DE -> "Training hinzugefügt: $date. (Standard $badge)"
        AppLanguage.RO -> "Antrenament adăugat: $date. (Implicit $badge)"
    }
    fun undoEmpty(language: AppLanguage) = when (language) {
        AppLanguage.HU -> "Nincs visszavonható naptárművelet"
        AppLanguage.EN -> "No calendar action to undo"
        AppLanguage.DE -> "Keine Kalenderaktion zum Rückgängigmachen"
        AppLanguage.RO -> "Nu există nicio acțiune din calendar de anulat"
    }
    fun undone(language: AppLanguage) = when (language) {
        AppLanguage.HU -> "Utolsó naptárművelet visszavonva"
        AppLanguage.EN -> "Last calendar action undone"
        AppLanguage.DE -> "Letzte Kalenderaktion rückgängig gemacht"
        AppLanguage.RO -> "Ultima acțiune din calendar a fost anulată"
    }
}

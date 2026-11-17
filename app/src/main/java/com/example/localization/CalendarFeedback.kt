package com.example.localization

import com.example.model.AppLanguage

internal object CalendarFeedback {
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

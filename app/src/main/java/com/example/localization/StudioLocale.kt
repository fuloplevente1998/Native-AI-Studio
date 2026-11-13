package com.example.localization

import com.example.model.AppLanguage
import java.util.Locale

internal object StudioLocale {
    fun forLanguage(language: AppLanguage): Locale = when (language) {
        AppLanguage.HU -> Locale.forLanguageTag("hu-HU")
        AppLanguage.EN -> Locale.forLanguageTag("en-GB")
        AppLanguage.DE -> Locale.GERMANY
        AppLanguage.RO -> Locale.forLanguageTag("ro-RO")
    }
    fun durationMinutes(minutes: Int, language: AppLanguage): String = when (language) {
        AppLanguage.HU -> "${minutes / 60} ó ${minutes % 60} p"
        AppLanguage.EN -> "${minutes / 60} h ${minutes % 60} min"
        AppLanguage.DE -> "${minutes / 60} Std. ${minutes % 60} Min."
        AppLanguage.RO -> "${minutes / 60} h ${minutes % 60} min"
    }
}

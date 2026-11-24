package com.example.ui.screens

import com.example.model.AppLanguage

internal fun tr(lang: AppLanguage, hu: String, en: String, de: String, ro: String): String = when (lang) {
    AppLanguage.HU -> hu
    AppLanguage.EN -> en
    AppLanguage.DE -> de
    AppLanguage.RO -> ro
}

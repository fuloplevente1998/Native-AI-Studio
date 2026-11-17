package com.repforge.app.localization

import com.repforge.app.data.db.ExerciseLibraryEntity
import java.util.Locale

/** Phone content language controls names sent to Wear, independently of Wear's UI locale. */
internal object WearNameLocalization {
    /** Blank translations fall back to English, then Hungarian, then any remaining catalog name. */
    fun name(language: String, hu: String, en: String, de: String, ro: String): String {
        val preferred = when (language.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')) {
            "en" -> en
            "de" -> de
            "ro" -> ro
            else -> hu
        }
        return listOf(preferred, en, hu, de, ro).firstOrNull { it.isNotBlank() }.orEmpty()
    }

    /** Translate a historical day only if its saved name still identifies a built-in catalog name. */
    fun historicalName(language: String, saved: String, hu: String, en: String, de: String, ro: String, isBuiltin: Boolean): String =
        if (isBuiltin && saved.isNotBlank() && listOf(hu, en, de, ro).any { it.isNotBlank() && it == saved })
            name(language, hu, en, de, ro)
        else saved

    /** Only translate an unchanged saved built-in name; preserve renamed and deleted history. */
    fun historicalExercise(language: String, savedHu: String, savedEn: String, catalog: ExerciseLibraryEntity?): String {
        val matches = catalog != null && !catalog.custom &&
            (savedHu.isNotBlank() || savedEn.isNotBlank()) &&
            (savedHu.isBlank() || savedHu == catalog.nameHu) &&
            (savedEn.isBlank() || savedEn == catalog.nameEn)
        return if (matches) {
            name(language, catalog!!.nameHu, catalog.nameEn, catalog.nameDe, catalog.nameRo)
        } else {
            savedHu.ifBlank { savedEn }
        }
    }
}

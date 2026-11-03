package com.repforge.app.data.store

/** A missing legacy key is the old default, not evidence of a manual choice. */
enum class MapAppearance {
    FOLLOW_APP, LIGHT, DARK;
    fun isDark(appIsDark: Boolean): Boolean = when (this) {
        FOLLOW_APP -> appIsDark
        LIGHT -> false
        DARK -> true
    }
    companion object {
        fun fromStored(value: String?, legacyManualDark: Boolean?): MapAppearance =
            entries.firstOrNull { it.name == value } ?: when (legacyManualDark) {
                true -> DARK
                false -> LIGHT
                null -> FOLLOW_APP
            }
    }
}

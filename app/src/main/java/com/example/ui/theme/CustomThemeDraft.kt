package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.compositeOver
import java.util.Locale
import java.util.UUID

/** #350: independent, validated editor data. No persistence or global theme mutation here. */
internal enum class CustomThemeStyle(val basePaletteId: Int) {
    MATTE(1), CONTRAST(9), GLOW(17), LIGHT(25)
}

internal enum class CustomThemeRole {
    BACKGROUND, CARD, PRIMARY, SECONDARY, BORDER;

    fun colorOf(palette: TrainPilotPalette): Color = when (this) {
        BACKGROUND -> palette.background
        CARD -> palette.surfaceCard
        PRIMARY -> palette.primary
        SECONDARY -> palette.secondary
        BORDER -> palette.cardBorder.compositeOver(palette.surfaceCard)
    }

    fun quickColors(): List<String> =
        TrainPilotPalettes.allPalettes.map { colorOf(it).customThemeHex() }.distinct()
}

internal fun Color.customThemeHex(): String =
    "#" + (toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase(Locale.ROOT)

/** Strict RGB input; do not accidentally accept Android ARGB or a partial HEX draft. */
internal fun normalizeCustomThemeHex(input: String): String? =
    input.trim().takeIf { Regex("#[0-9a-fA-F]{6}").matches(it) }?.uppercase(Locale.ROOT)

internal data class CustomThemeDraft(
    val name: String,
    val style: CustomThemeStyle,
    val colors: Map<CustomThemeRole, String>,
    // 100 = transparency disabled; 70..90 = opacity of the surface paint.
    val opacityPercent: Int = 100
) {
    val invalidRoles: Set<CustomThemeRole>
        get() = CustomThemeRole.entries.filterTo(mutableSetOf()) {
            normalizeCustomThemeHex(colors[it].orEmpty()) == null
        }

    val hasValidOpacity: Boolean
        get() = opacityPercent == 100 || opacityPercent in 70..90

    val isInputValid: Boolean
        get() = name.isNotBlank() && invalidRoles.isEmpty() && hasValidOpacity

    fun withColor(role: CustomThemeRole, value: String): CustomThemeDraft =
        copy(colors = colors + (role to value))

    /** Returns null for invalid drafts or keys. An edit keeps the existing stable key. */
    fun toDefinition(key: String): CustomThemeDefinition? {
        if (!isInputValid || !isCustomThemeKey(key)) return null
        val normalized = CustomThemeRole.entries.associateWith {
            requireNotNull(normalizeCustomThemeHex(colors.getValue(it)))
        }
        return CustomThemeDefinition(key, copy(name = name.trim(), colors = normalized))
    }

    companion object {
        fun fromStyle(style: CustomThemeStyle, name: String = ""): CustomThemeDraft {
            val palette = TrainPilotPalettes.getById(style.basePaletteId)
            return CustomThemeDraft(name, style, CustomThemeRole.entries.associateWith {
                it.colorOf(palette).customThemeHex()
            })
        }
    }
}

internal data class CustomThemeDefinition(val key: String, val draft: CustomThemeDraft)

internal fun newCustomThemeKey(): String = "custom:" + UUID.randomUUID().toString()

internal fun isCustomThemeKey(key: String): Boolean {
    if (!key.startsWith("custom:")) return false
    val suffix = key.removePrefix("custom:")
    return runCatching { UUID.fromString(suffix).toString() == suffix }.getOrDefault(false)
}

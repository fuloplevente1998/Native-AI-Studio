package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.repforge.app.ui.theme.NativeThemeTokens

/** #350: one pure resolver for editor preview and eventual applied theme.
 * The palette carries opaque effective colors plus optional raw card paint and a
 * monotonic page gradient; nested/unknown backdrops keep the effective colors.
 */
internal data class ResolvedCustomTheme(
    val palette: TrainPilotPalette,
    val suggestedDraft: CustomThemeDraft,
    val correctedRoles: Set<CustomThemeRole>,
    val rawCard: Color,
    val cardOpacity: Float
)

internal fun resolveCustomTheme(draft: CustomThemeDraft, key: String): ResolvedCustomTheme? {
    if (draft.invalidRoles.isNotEmpty() || !draft.hasValidOpacity || !isCustomThemeKey(key)) return null
    val base = TrainPilotPalettes.getById(draft.style.basePaletteId)
    val foreground = if (base.isDark) Color.White else Color.Black
    val backdrop = if (base.isDark) Color.Black else Color.White
    val original = CustomThemeRole.entries.associateWith {
        NativeThemeTokens.parseHex(draft.colors.getValue(it).trim(), Color.Unspecified)
    }
    val corrected = original.toMutableMap()
    val opacity = draft.opacityPercent / 100f

    fun contrast(a: Color, b: Color) = NativeThemeTokens.contrast(a, b)
    fun move(seed: Color, destination: Color, accepts: (Color) -> Boolean): Color {
        for (step in 0..100) {
            // Validate the exact 8-bit color that the HEX editor will persist.
            val color = Color(lerp(seed, destination, step / 100f).toArgb())
            if (accepts(color)) return color
        }
        return destination
    }
    fun safeSurface(color: Color): Boolean =
        contrast(foreground, color) >= 6f &&
            contrast(foreground, foreground.copy(alpha = 0.22f).compositeOver(color)) >= 5.2f

    val page = move(original.getValue(CustomThemeRole.BACKGROUND), backdrop, ::safeSurface)
    corrected[CustomThemeRole.BACKGROUND] = page
    val rawCard = move(original.getValue(CustomThemeRole.CARD), backdrop) {
        safeSurface(it.copy(alpha = opacity).compositeOver(page))
    }
    corrected[CustomThemeRole.CARD] = rawCard
    val card = rawCard.copy(alpha = opacity).compositeOver(page)
    // Each channel moves away from the chosen text luminance, so the second stop
    // cannot reduce contrast relative to the first stop used for correction.
    val pageEnd = Color(red = page.red * .78f + backdrop.red * .22f,
        green = page.green * .78f + backdrop.green * .22f,
        blue = page.blue * .78f + backdrop.blue * .22f)
    val inner = move(lerp(card, foreground, if (base.isHighContrast) 0.09f else 0.05f),
        backdrop, ::safeSurface)
    val surfaces = listOf(page, card, inner)
    fun readable(seed: Color, minimum: Float, backgrounds: List<Color>): Color =
        move(seed, foreground) { candidate -> backgrounds.all { contrast(candidate, it) >= minimum } }

    val primary = move(original.getValue(CustomThemeRole.PRIMARY), foreground) { candidate ->
        (surfaces + listOf(0.12f, 0.16f, 0.22f).map { candidate.copy(alpha = it).compositeOver(card) })
            .all { contrast(candidate, it) >= 4.55f }
    }
    val allSurfaces = surfaces + listOf(0.12f, 0.16f, 0.22f).map { primary.copy(alpha = it).compositeOver(card) }
    val secondary = readable(original.getValue(CustomThemeRole.SECONDARY), 4.55f, allSurfaces)
    val border = readable(original.getValue(CustomThemeRole.BORDER), 3.05f, surfaces)
    corrected[CustomThemeRole.PRIMARY] = primary
    corrected[CustomThemeRole.SECONDARY] = secondary
    corrected[CustomThemeRole.BORDER] = border

    fun semantic(seed: Color) = readable(seed, 4.55f, allSurfaces)
    val label = draft.name.trim()
    // -1 is a preview/render sentinel, not a persisted selection ID. The UUID key is the identity.
    val palette = base.copy(
        id = -1, key = key, category = base.subGroup,
        nameHu = label, nameEn = label, nameDe = label, nameRo = label,
        background = page, surface = card, surfaceElevated = inner,
        primary = primary, onPrimary = NativeThemeTokens.onFill(primary),
        secondary = secondary, primaryBright = primary, tertiary = secondary,
        textPrimary = foreground, textSecondary = readable(base.textSecondary, 5f, allSurfaces),
        cardBorder = border,
        success = semantic(base.success), warning = semantic(base.warning), danger = semantic(base.danger),
        sleepAccent = semantic(base.sleepAccent), heartAccent = semantic(base.heartAccent),
        stepsAccent = semantic(base.stepsAccent), energyAccent = semantic(base.energyAccent),
        customCardPaint = rawCard.copy(alpha = opacity).takeIf { opacity < 1f },
        customPageEnd = pageEnd.takeIf { opacity < 1f }
    )
    val suggested = corrected.mapValues { it.value.customThemeHex() }
    val changed = CustomThemeRole.entries.filterTo(mutableSetOf()) {
        suggested.getValue(it) != normalizeCustomThemeHex(draft.colors.getValue(it))
    }
    return ResolvedCustomTheme(palette, draft.copy(colors = suggested), changed, rawCard, opacity)
}

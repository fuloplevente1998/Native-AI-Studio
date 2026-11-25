package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import com.repforge.app.ui.theme.NativeThemeTokens

internal const val DEFAULT_GLASS_CLARITY = 65

/** Eight separate built-in themes, with one material setting for the entire family. */
internal object LiquidGlassPalettes {
    private data class Tone(val key: String, val hu: String, val en: String, val de: String, val ro: String,
        val accent: Long, val backdrop: Long)
    private val tones = listOf(
        Tone("graphite", "Grafit", "Graphite", "Graphit", "Grafit", 0xFFD0DBE8, 0xFF172333),
        Tone("ice", "Jég", "Ice", "Eis", "Gheață", 0xFFBCEBFF, 0xFF142C3D),
        Tone("sapphire", "Zafír", "Sapphire", "Saphir", "Safir", 0xFF8DB9FF, 0xFF172952),
        Tone("turquoise", "Türkiz", "Turquoise", "Türkis", "Turcoaz", 0xFF70E5EF, 0xFF123B43),
        Tone("emerald", "Smaragd", "Emerald", "Smaragd", "Smarald", 0xFF78F2BC, 0xFF12392E),
        Tone("amethyst", "Ametiszt", "Amethyst", "Amethyst", "Ametist", 0xFFD0B2FF, 0xFF35234F),
        Tone("rose", "Rózsaszín", "Rose", "Rosé", "Roz", 0xFFFFB9D8, 0xFF41253B),
        Tone("amber", "Borostyán", "Amber", "Bernstein", "Chihlimbar", 0xFFFFD18C, 0xFF402E1A)
    )
    val all: List<TrainPilotPalette> = tones.mapIndexed { i, t ->
        val bg = Color(t.backdrop)
        val accent = Color(t.accent)
        val surface = lerp(bg, Color(0xFF101722), .42f)
        val base = TrainPilotPalette(
            id = 33 + i, key = "glass_" + t.key,
            nameHu = "Liquid Glass – " + t.hu, nameEn = "Liquid Glass – " + t.en,
            nameDe = "Liquid Glass – " + t.de, nameRo = "Liquid Glass – " + t.ro,
            category = PaletteCategory.LIQUID_GLASS, background = bg, surface = surface,
            surfaceElevated = lerp(surface, Color.White, .06f), primary = accent,
            onPrimary = NativeThemeTokens.onFill(accent), secondary = accent,
            textPrimary = Color.White, textSecondary = Color(0xFFE0E8F1),
            cardBorder = Color(0xFFB4C6D9), isDark = true
        )
        base.withGlassClarity(DEFAULT_GLASS_CLARITY)
    }
}

/**
 * Clarity changes paint strength, not opacity of content. The opaque palette is the Wear fallback.
 * Text/accent colors are lifted until they stay readable on every rendered glass worst case.
 */
internal fun TrainPilotPalette.withGlassClarity(value: Int): TrainPilotPalette {
    if (!isLiquidGlass) return this
    val clarity = value.coerceIn(0, 100)
    val end = lerp(background, Color(0xFF080D17), .65f)
    val optics = GlassOptics.from(clarity)
    // 2810/6: the card paint is the thin white-ish tint (.40 Frosted -> .06 Clear). The representative
    // opaque card (dialogs, legacy call sites) is that tint over the backdrop as the lens limits it.
    val paint = GlassOptics.TINT.copy(alpha = optics.coat)
    val card = paint.compositeOver(GlassScene.guard(background, optics.backdropCap))
    val inner = lerp(card, Color.White, .05f)
    val fallback = glassFallbackPaint(clarity)
    // Worst cases of the rendered material: the glass ceiling (every lens/tint composite stays at or
    // below SURFACE_LUMA_MAX), the guarded page, the V4 scene bound and the API 31–32 blur-only lens.
    val rendered = listOf(GlassOptics.lensWorstCase, GlassContrast.grey(GlassScene.lumaCap),
        GlassOptics.sceneWorstCase(background), GlassOptics.blurOnlyWorstCase(clarity))
    // Inner tiles: the neutral tile tint (luminance below the ceiling) over any of these surfaces.
    val tiles = (rendered + listOf(card, background)).flatMap { under ->
        listOf(glassTilePaint(clarity, false), glassTilePaint(clarity, true)).map { it.compositeOver(under) } }
    val surfaces = listOf(background, end, card, paint.compositeOver(end), inner,
        fallback.compositeOver(background), fallback.compositeOver(end)) + rendered + tiles +
        listOf(.12f, .16f, .22f).map { primary.copy(alpha = it).compositeOver(card) }
    fun readable(seed: Color, minimum: Float): Color {
        for (step in 0..100) {
            val candidate = lerp(seed, Color.White, step / 100f)
            if (surfaces.all { NativeThemeTokens.contrast(candidate, it) >= minimum }) return candidate
        }
        return Color.White
    }
    return copy(surface = card, surfaceElevated = inner,
        primary = readable(primary, 4.55f), secondary = readable(secondary, 4.55f),
        onPrimary = NativeThemeTokens.onFill(readable(primary, 4.55f)),
        primaryBright = readable(primaryBright, 4.55f), tertiary = readable(tertiary, 4.55f),
        textSecondary = readable(textSecondary, 4.55f), cardBorder = readable(cardBorder, 3.05f),
        success = readable(Color(0xFF68E8AF), 4.55f), danger = readable(Color(0xFFFF969C), 4.55f),
        warning = readable(Color(0xFFFFD47A), 4.55f), heartAccent = readable(Color(0xFFFF9EB5), 4.55f),
        sleepAccent = readable(Color(0xFFC0BCFF), 4.55f), stepsAccent = readable(Color(0xFF68E8AF), 4.55f),
        energyAccent = readable(Color(0xFFFFD47A), 4.55f),
        customCardPaint = paint, customPageEnd = end, glassClarity = clarity)
}

/** The previous dark glass paint: now only the API < 31 / software opaque fallback (2810/8). */
internal val GLASS_PAINT = Color(0xFF334359)

/** Previous opaque path (API < 31, software rendering): [GLASS_PAINT] at .82 Frosted -> .18 Clear. */
internal fun glassFallbackPaint(clarity: Int): Color = GLASS_PAINT.copy(alpha = .82f - .64f * clarity.coerceIn(0, 100) / 100f)

/**
 * 2810/6 inner tile tint: a neutral blue-grey whose luminance stays below [GlassOptics.SURFACE_LUMA_MAX],
 * so a tile over any glass composite (an sRGB mix) can never lift it above the ceiling.
 */
internal val GLASS_TILE = Color(0xFF58616F)
internal val GLASS_TILE_ELEVATED = Color(0xFF5B6472)

/** Inner tile alpha, continuous and strictly falling with the slider (.16 Frosted -> .10 Clear). */
internal fun glassTileAlpha(clarity: Int): Float = .16f - .06f * clarity.coerceIn(0, 100) / 100f

internal fun glassTilePaint(clarity: Int, elevated: Boolean): Color =
    (if (elevated) GLASS_TILE_ELEVATED else GLASS_TILE).copy(alpha = glassTileAlpha(clarity))

/**
 * 2809/3: colour of an inner tile (Sleep/Steps, program card, journal row, coach panel). Liquid Glass:
 * the low-opacity tile tint, so the glass card behind shows through; all other palettes and custom
 * themes: exactly [legacy].
 */
internal fun TrainPilotPalette.glassTile(legacy: Color, elevated: Boolean = false): Color =
    if (isLiquidGlass) glassTilePaint(glassClarity, elevated) else legacy

/**
 * 2810/5: fill of a content surface that is routed through the glass card path
 * ([trainPilotGlassCard]): transparent in Liquid Glass (the material paints it), exactly [legacy] otherwise.
 */
internal fun TrainPilotPalette.glassCardFill(legacy: Color): Color = if (isLiquidGlass) Color.Transparent else legacy

internal fun TrainPilotPalette.glassPageBrush(): Brush {
    val end = customPageEnd ?: background
    // A visibly colored, monotonic gradient. No claim of sampling other app content.
    return Brush.verticalGradient(listOf(background, lerp(background, end, .35f), end))
}

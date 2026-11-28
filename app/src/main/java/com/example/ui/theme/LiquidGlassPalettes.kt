package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
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
        Tone("amber", "Arany", "Gold", "Gold", "Auriu", 0xFFFFD18C, 0xFF382E08)
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
    // Keep the optical coat white-ish; opaque compatibility surfaces are a separate
    // palette-derived material. Otherwise native icon backgrounds inherit a grey frost coat.
    val paint = GlassOptics.TINT.copy(alpha = optics.coat)
    val opticalCard = paint.compositeOver(GlassScene.guard(background, optics.backdropCap))
    val opticalInner = lerp(opticalCard, Color.White, .05f)
    val card = glassBackgroundTint(opticalCard.luminance().coerceAtMost(.115f), saturation = .55f)
    val inner = glassBackgroundTint(opticalInner.luminance().coerceAtMost(.124f), saturation = .55f)
    val fallback = glassFallbackPaint(this, clarity)
    // Worst cases of the rendered material: the glass ceiling (every lens/tint composite stays at or
    // below SURFACE_LUMA_MAX), the guarded page, the V4 scene bound and the API 31–32 blur-only lens.
    val rendered = listOf(GlassOptics.lensWorstCase, GlassContrast.grey(GlassScene.lumaCap),
        GlassOptics.sceneWorstCase(background), GlassOptics.blurOnlyWorstCase(clarity))
    // Inner tiles: palette-derived tints bounded below the ceiling over any of these surfaces.
    val tilePaints = listOf(glassTilePaint(this, clarity, false), glassTilePaint(this, clarity, true))
    val tiles = (rendered + listOf(card, background, opticalCard)).flatMap { under ->
        tilePaints.map { it.compositeOver(under) } }
    val surfaces = listOf(background, end, card, paint.compositeOver(end), inner, opticalCard, opticalInner,
        fallback.compositeOver(background), fallback.compositeOver(end)) + rendered + tiles +
        listOf(.12f, .16f, .22f).flatMap { alpha ->
            listOf(card, opticalCard).map { primary.copy(alpha = alpha).compositeOver(it) }
        }
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

/** API < 31 / software paint: preserve the old .82 -> .18 opacity curve,
 * but derive the opaque RGB seed from the active Liquid scene material hue. */
internal fun glassFallbackPaint(palette: TrainPilotPalette, clarity: Int): Color =
    palette.glassBackgroundTint(.054f, saturation = .55f)
        .copy(alpha = .82f - .64f * clarity.coerceIn(0, 100) / 100f)

/** Inner tile alpha, continuous and strictly falling with the slider (.16 Frosted -> .10 Clear). */
internal fun glassTileAlpha(clarity: Int): Float = KyantMaterialSpec.forRole(GlassMaterialRole.Tile, clarity).tintAlpha

/** Keep separate tile depths under the existing contrast ceiling, without sampling a backdrop. */
internal fun glassTilePaint(palette: TrainPilotPalette, clarity: Int, elevated: Boolean): Color =
    palette.glassBackgroundTint(if (elevated) .121f else .115f, saturation = .48f)
        .copy(alpha = glassTileAlpha(clarity))

/** Shared lightweight paint; hue follows the selected palette even when the foreground
 * accent has been lifted towards white for contrast. Preserve the established opacity curves. */
internal fun glassControlPaint(palette: TrainPilotPalette, clarity: Int, role: GlassMaterialRole): Color {
    val t = clarity.coerceIn(0, 100) / 100f
    return when (role) {
        GlassMaterialRole.Selection -> palette.glassBackgroundTint(.121f, saturation = .60f)
            .copy(alpha = .36f - .12f * t)
        GlassMaterialRole.Navigation -> glassTilePaint(palette, clarity, false).copy(alpha = .08f - .04f * t)
        else -> glassTilePaint(palette, clarity, true)
    }
}

internal fun TrainPilotPalette.glassControlFill(legacy: Color, role: GlassMaterialRole): Color =
    if (isLiquidGlass) glassControlPaint(this, glassClarity, role) else legacy

/**
 * 2809/3: colour of an inner tile (Sleep/Steps, program card, journal row, coach panel). Liquid Glass:
 * the low-opacity tile tint, so the glass card behind shows through; all other palettes and custom
 * themes: exactly [legacy].
 */
internal fun TrainPilotPalette.glassTile(legacy: Color, elevated: Boolean = false): Color =
    if (isLiquidGlass) glassTilePaint(this, glassClarity, elevated) else legacy

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

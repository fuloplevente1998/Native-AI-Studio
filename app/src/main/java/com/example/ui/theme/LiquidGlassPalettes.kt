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
    val t = clarity / 100f
    val end = lerp(background, Color(0xFF080D17), .65f)
    // 2808/12: thinner card tint towards Clear (.82 -> .18), contrast lifted below as before.
    val paint = GLASS_PAINT.copy(alpha = .82f - .64f * t)
    val card = paint.compositeOver(background)
    val inner = lerp(card, Color.White, .05f)
    val optics = GlassOptics.from(clarity)
    // Worst cases of the rendered material: brightest lens output (shader peak guard), brightest
    // procedural page scene, and blur-only content (API 31-32) under the veil over pure white.
    val rendered = listOf(GlassOptics.lensWorstCase, GlassOptics.sceneWorstCase(background),
        card.copy(alpha = optics.veil).compositeOver(Color.White))
    // 2809/3: inner tiles use the same paint at [glassTileAlpha] over any of these surfaces.
    val tiles = (rendered + listOf(card, background)).flatMap { under ->
        listOf(glassTilePaint(clarity, false), glassTilePaint(clarity, true)).map { it.compositeOver(under) } }
    val surfaces = listOf(background, end, card, paint.compositeOver(end), inner) + rendered + tiles +
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

/** The one Liquid Glass paint: cards use it at [withGlassClarity]'s card alpha, inner tiles thinner. */
internal val GLASS_PAINT = Color(0xFF334359)

/** 2809/3: inner tile tint, continuous and strictly falling with the slider (.66 Frosted -> .12 Clear). */
internal fun glassTileAlpha(clarity: Int): Float = .66f - .54f * clarity.coerceIn(0, 100) / 100f

internal fun glassTilePaint(clarity: Int, elevated: Boolean): Color =
    (if (elevated) lerp(GLASS_PAINT, Color.White, .06f) else GLASS_PAINT).copy(alpha = glassTileAlpha(clarity))

/**
 * 2809/3: colour of an inner tile (Sleep/Steps, program card, journal row, coach panel). Liquid Glass:
 * the low-opacity glass tint, so the backdrop / glass card behind shows through; all other palettes
 * and custom themes: exactly [legacy].
 */
internal fun TrainPilotPalette.glassTile(legacy: Color, elevated: Boolean = false): Color =
    if (isLiquidGlass) glassTilePaint(glassClarity, elevated) else legacy

internal fun TrainPilotPalette.glassPageBrush(): Brush {
    val end = customPageEnd ?: background
    // A visibly colored, monotonic gradient. No claim of sampling other app content.
    return Brush.verticalGradient(listOf(background, lerp(background, end, .35f), end))
}

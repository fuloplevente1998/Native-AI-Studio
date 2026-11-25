package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

/**
 * 2811 G1 (Liquid Glass 2.0 central material) + 2811/1 (header). Behaviour of the role tokens the one
 * engine consumes: Panel is the unchanged pre-role material, every role follows the slider monotonically,
 * the outer Panel outranks the inner Tile, every role keeps text >= 4.5:1 by limiting its own backdrop,
 * the header uses Navigation only in glass palettes, and the scene always has colour under the header.
 */
class LiquidGlassRoles2811Test {
    private val glass = LiquidGlassPalettes.all
    private val legacy = TrainPilotPalettes.allPalettes
    private val roles = GlassRole.values().toList()

    // Independent mirror of the sRGB/luminance maths (not the production helpers).
    private fun lin(c: Float) = if (c <= .04045f) c / 12.92f else ((c + .055f) / 1.055f).pow(2.4f)
    private fun srgb(l: Float) = if (l <= .0031308f) l * 12.92f else 1.055f * l.pow(1f / 2.4f) - .055f
    private fun luma(r: Float, g: Float, b: Float) = .2126f * lin(r) + .7152f * lin(g) + .0722f * lin(b)

    @Test fun panelRoleIsThePreRoleMaterial() {
        for (step in 0..100) assertEquals("$step", GlassOptics.from(step), GlassOptics.from(step, GlassRole.Panel))
        assertEquals(GlassOptics.from(36.5f), GlassOptics.from(36.5f, GlassRole.Panel))
        val frosted = GlassOptics.from(0, GlassRole.Panel)
        assertEquals(.40f, frosted.coat, 1e-6f)
        assertEquals(GlassOptics.FROSTED_BLUR_DP, frosted.blurDp, 1e-4f)
        assertEquals(1.40f, frosted.saturation, 1e-5f)
        assertEquals(1f, frosted.elevation, 0f)
        assertEquals(GlassRole.Panel, GlassOptics.from(65).role)
    }

    @Test fun everyRoleFollowsTheSliderMonotonicallyAndContinuously() {
        for (role in roles) {
            val all = (0..100).map { GlassOptics.from(it, role) }
            assertTrue("$role tint never reaches the ceiling alone", role.tint in .1f..1f || (role.matchesPage && role.tint == 0f))
            data class Field(val name: String, val get: (GlassOptics) -> Float, val sign: Int)
            val fields = listOf(
                Field("coat", { it.coat }, if (role.tint > 0f) -1 else 0), Field("blurDp", { it.blurDp }, if (role.blurScale > 0f) -1 else 0),
                Field("specular", { it.specular }, if (role.rim > 0f) 1 else 0), Field("shadow", { it.shadow }, if (role.depth > 0f) 1 else 0),
                Field("thickness", { it.thickness }, if (role.depth > 0f) 1 else 0), Field("bend", { it.bend }, if (role.refraction > 0f) 1 else 0),
                Field("dispersion", { it.dispersion }, if (role.chroma > 0f) 1 else 0),
                Field("saturation", { it.saturation }, if (role.vibrancy > 0f) 1 else 3),
                Field("backdropCap", { it.backdropCap }, if (role.tint > 0f) 2 else 3))
            for (f in fields) {
                val range = abs(f.get(all.last()) - f.get(all.first()))
                if (f.sign == 0) { assertTrue("$role ${f.name} off", all.all { f.get(it) == 0f }); continue }
                // 3: constant across the slider (a page-matched role has no own tint/vibrancy to move).
                if (f.sign == 3) { assertTrue("$role ${f.name} constant", all.all { f.get(it) == f.get(all.first()) }); continue }
                assertTrue("$role ${f.name} differs Frosted/Clear", range > 0f)
                for (step in 1..100) {
                    val before = f.get(all[step - 1])
                    val after = f.get(all[step])
                    when (f.sign) {
                        -1 -> assertTrue("$role ${f.name} $step", after < before)
                        1 -> assertTrue("$role ${f.name} $step", after > before)
                        else -> assertTrue("$role ${f.name} $step", after >= before)
                    }
                    assertTrue("$role ${f.name} jumps at $step", abs(after - before) <= range * .05f)
                }
            }
            assertTrue(all.all { it.elevation == role.elevation && it.role == role })
            assertEquals(all.first(), GlassOptics.from(Float.NaN, role))
            assertEquals(all.last(), GlassOptics.from(400f, role))
        }
    }

    @Test fun outerPanelIsVisiblyStrongerThanInnerTile() {
        assertTrue(GlassRole.Panel.prominence > GlassRole.Tile.prominence)
        // Among roles with an edge of their own (the page-matched header has none), the inner tile is the quietest.
        assertEquals("the inner tile is the quietest role", GlassRole.Tile, roles.filter { it.rim > 0f }.minByOrNull { it.prominence })
        assertTrue(GlassRole.Overlay.prominence >= GlassRole.Panel.prominence)
        for (step in 0..100) {
            val panel = GlassOptics.from(step, GlassRole.Panel)
            val tile = GlassOptics.from(step, GlassRole.Tile)
            assertTrue("$step rim", panel.specular > tile.specular)
            assertTrue("$step depth", panel.shadow > tile.shadow && panel.thickness > tile.thickness)
            assertTrue("$step lift", panel.elevation > tile.elevation)
            assertTrue("$step refraction", panel.bend > tile.bend && panel.dispersion > tile.dispersion)
            assertTrue("$step frost", panel.blurDp > tile.blurDp)
            assertTrue("$step tile is clearer", tile.coat < panel.coat && tile.backdropCap > panel.backdropCap)
            // 2811/1: the header shows more of its backdrop than a card (thinner tint, higher limit).
            val nav = GlassOptics.from(step, GlassRole.Navigation)
            assertTrue("$step nav tint", nav.coat < panel.coat)
            assertTrue("$step nav backdrop", nav.backdropCap > panel.backdropCap)
            val light = GlassOptics.from(step, GlassRole.Performance)
            assertTrue(light.blurDp == 0f && light.bend == 0f && light.dispersion == 0f && light.elevation == 0f)
        }
    }

    @Test fun everyRoleKeepsTextReadableByLimitingItsBackdrop() {
        val levels = (0..12).map { it / 12f }
        val tint = GlassOptics.TINT
        val ceiling = GlassOptics.SURFACE_LUMA_MAX
        for (role in roles) for (step in 0..100 step 10) {
            val optics = GlassOptics.from(step, role)
            val cap = optics.backdropCap
            val a = optics.coat
            assertTrue(cap in 0f..ceiling)
            assertTrue(GlassContrast.worstComposite(a, cap) <= ceiling)
            var worst = 0f
            for (r in levels) for (g in levels) for (b in levels) {
                val l = luma(r, g, b)
                val k = if (l <= cap || l == 0f) 1f else cap / l
                val br = if (k < 1f) srgb(lin(r) * k) else r
                val bg = if (k < 1f) srgb(lin(g) * k) else g
                val bb = if (k < 1f) srgb(lin(b) * k) else b
                worst = maxOf(worst, luma(a * tint.red + (1 - a) * br, a * tint.green + (1 - a) * bg, a * tint.blue + (1 - a) * bb))
            }
            assertTrue("$role $step: $worst", worst <= ceiling)
            val gains = listOf(.213f, .715f, .072f).map { it * (1 - optics.saturation) + optics.saturation }
            val s = optics.blurOnlyScale
            val blurOnly = luma(a * tint.red + (1 - a) * s * gains[0], a * tint.green + (1 - a) * s * gains[1],
                a * tint.blue + (1 - a) * s * gains[2])
            assertTrue("$role $step blur-only $blurOnly", blurOnly <= ceiling)
            val v = srgb(maxOf(worst, blurOnly))
            val brightest = Color(v, v, v)
            for (seed in glass) {
                val p = seed.withGlassClarity(step)
                val selection = p.selectionColors()
                for (text in listOf(p.textPrimary, p.textSecondary, p.primary, p.success, p.warning, p.danger,
                    selection.foreground))
                    assertTrue("$role ${p.key} $step $text", NativeThemeTokens.contrast(text, brightest) >= 4.5f)
                assertTrue(NativeThemeTokens.contrast(p.cardBorder, brightest) >= 3f)
                assertTrue(NativeThemeTokens.contrast(selection.border, brightest) >= 3f)
                // Quick buttons / nav track: the tile tint over the role composite stays under the ceiling.
                for (elevated in listOf(false, true))
                    assertTrue(p.glassTile(p.surface, elevated).compositeOver(brightest).luminance() <= ceiling + .003f)
            }
        }
    }

    @Test fun headerUsesTheNavigationRoleOnlyUnderGlassPalettes() {
        for (role in roles) {
            assertEquals(role, GlassRole.resolve(role, opticsEnabled = true))
            // Measurement screen: Performance, except the page-matched header (no step there either).
            assertEquals("measurement screen", if (role.matchesPage) role else GlassRole.Performance,
                GlassRole.resolve(role, opticsEnabled = false))
        }
        val custom = CustomThemeDraft.fromStyle(CustomThemeStyle.CONTRAST, "x").let {
            requireNotNull(resolveCustomTheme(it, newCustomThemeKey())).palette }
        for (p in legacy + custom) {
            assertFalse(p.glassEffectsActive(true))
            assertFalse(p.glassRimActive())
            // Quick buttons keep exactly their previous fills outside the glass family.
            for (active in listOf(false, true)) {
                val previous = if (active) p.selectionColors().surface else p.surfaceElevated
                assertEquals(previous, p.glassTile(previous, elevated = active))
            }
            assertSame(p, p.withGlassClarity(65))
        }
        for (p in glass) for (step in listOf(0, 50, 65, 100)) {
            val tinted = p.withGlassClarity(step)
            for (active in listOf(false, true))
                assertTrue("translucent quick button", tinted.glassTile(tinted.surfaceElevated, active).alpha < .2f)
        }
        fun src(path: String) = java.io.File("src/main/java/com/example/$path").readText()
        val header = src("ui/components/common/TopHeaderAndNav.kt")
        assertTrue(header.contains(".glassHeaderMaterial(palette, RoundedCornerShape(0.dp), GlassRole.Navigation)"))
        for (flag in listOf("isCalendarActive", "isSettingsActive"))
            assertTrue(flag, header.contains("color = palette.glassTile(if ($flag) selection.surface else palette.surfaceElevated, elevated = $flag)"))
        assertFalse("no dark veil on the nav track", header.contains("palette.surface.copy(alpha = .22f)"))
        val lens = src("ui/theme/GlassContentBackdrop.kt")
        assertTrue(lens.contains("role: GlassRole = GlassRole.Navigation"))
        assertTrue(src("ui/components/common/GlassSelectionPill.kt").contains("glassHeaderMaterial(palette, shape, GlassRole.Selection)"))
    }

    @Test fun sceneAlwaysPutsColourUnderTheHeader() {
        for ((w, h) in listOf(1080f to 2340f, 1080f to 1920f, 720f to 1600f, 1440f to 3120f)) {
            for (t in 0..600 step 3) {
                val blobs = GlassScene.blobs(w, h, t.toFloat())
                for (xi in 0..20) for (y in listOf(0f, h * GlassScene.HEADER_BAND / 2f, h * GlassScene.HEADER_BAND)) {
                    val weight = GlassScene.blobWeight(w * xi / 20f, y, blobs)
                    assertTrue("$w x $h t=$t x=$xi y=$y: $weight", weight >= .25f)
                }
            }
        }
        for (p in glass) assertEquals(GlassScene.blobCount, GlassScene.tones(p).size)
    }

    @Test fun rimLightIsDirectionalAndRestrained() {
        val s = .45f
        val top = GlassRimLight.edgeAlpha(0f, -1f, s)
        val left = GlassRimLight.edgeAlpha(-1f, 0f, s)
        val right = GlassRimLight.edgeAlpha(1f, 0f, s)
        val bottom = GlassRimLight.edgeAlpha(0f, 1f, s)
        assertTrue(top > left && left > right && right > bottom && bottom > 0f)
        val d = .7071f
        assertTrue("light from the upper left", GlassRimLight.edgeAlpha(-d, -d, s) > GlassRimLight.edgeAlpha(d, -d, s))
        assertEquals(1f, GlassRimLight.top, 1e-6f)
        val stops = GlassRimLight.gradient(s)
        assertEquals(s, stops[0].second.alpha, 1f / 255f)
        assertEquals(s * GlassRimLight.side, stops[1].second.alpha, 1f / 255f)
        assertEquals(s * GlassRimLight.BOTTOM, stops[2].second.alpha, 1f / 255f)
        for (role in roles) for (step in listOf(0, 65, 100)) {
            val spec = GlassOptics.from(step, role).specular
            assertTrue("$role $step no neon", GlassRimLight.edgeAlpha(-d, -d, spec) <= GlassRimLight.MAX_SHINE)
        }
        // The AGSL rim uses the same coefficients; colour split stays at the edge; thickness is under the ceiling.
        val body = GlassShaderSource.SCENE_LENS
        assertTrue(body.contains("specular * (0.27 + 0.73 * topness + 0.13 * side)"))
        assertTrue(body.contains("0.08 * specular * max(dot(n, lightDir), 0.0)"))
        assertTrue(body.contains("clamp(shine, 0.0, 0.60)"))
        assertTrue(body.contains("c.r = mix(c.r, redShift, slope * slope);"))
        val thick = body.indexOf("thickness * band * facing")
        assertTrue(thick > 0 && thick < body.indexOf("c = limitLuma(c, ceiling);"))
    }
}

package com.example.ui.theme

import kotlin.math.pow

/** One Kyant material family: roles change optical parameters, not rendering engines. */
internal enum class GlassMaterialRole { Panel, Tile, Navigation, Selection, Button, Input, Overlay }

/** Parameters are pure and cached by the stored 0–100 clarity step. */
internal data class KyantMaterialSpec(
    val blurDp: Float,
    val lensHeightDp: Float,
    val lensAmountDp: Float,
    val tintAlpha: Float,
    val saturation: Float,
    val colorScale: Float,
    val highlightAlpha: Float,
    val shadow: Boolean,
) {
    val usesLens: Boolean get() = lensAmountDp > 0f
    val usesBackdrop: Boolean get() = blurDp > 0f

    companion object {
        private val cache = Array(GlassMaterialRole.entries.size) { arrayOfNulls<KyantMaterialSpec>(101) }

        fun forRole(role: GlassMaterialRole, clarity: Int): KyantMaterialSpec {
            val step = clarity.coerceIn(0, 100)
            return cache[role.ordinal][step] ?: compute(role, step / 100f).also { cache[role.ordinal][step] = it }
        }

        private fun compute(role: GlassMaterialRole, t: Float): KyantMaterialSpec {
            val baseBlur = 24f * (.5f / 24f).pow(t)
            val baseHeight = 6f + 18f * t
            val baseAmount = 10f + 38f * t
            val saturation = 1.08f + .22f * t
            // Tile paint is composited over its parent, not separately captured or blurred.
            val (blur, lens, coat) = when (role) {
                GlassMaterialRole.Panel -> Triple(1f, 1f, .20f - .15f * t)
                GlassMaterialRole.Tile -> Triple(0f, 0f, .16f - .06f * t)
                GlassMaterialRole.Navigation -> Triple(.8f, .65f, .12f - .09f * t)
                GlassMaterialRole.Selection -> Triple(.7f, 1.15f, .16f - .12f * t)
                GlassMaterialRole.Button -> Triple(.65f, .65f, .14f - .10f * t)
                GlassMaterialRole.Input -> Triple(1.1f, .25f, .23f - .13f * t)
                GlassMaterialRole.Overlay -> Triple(1.4f, .3f, .28f - .16f * t)
            }
            // A backdrop may contain bright text or a bright photo. Keep the existing main
            // contrast ceiling even for unknown source colours, without a second shader.
            val scale = if (blur == 0f) 1f else GlassContrast.blurOnlyScale(coat, saturation)
            return KyantMaterialSpec(baseBlur * blur, baseHeight * lens, baseAmount * lens,
                coat, saturation, scale,
                if (role == GlassMaterialRole.Selection) .45f + .45f * t else .35f + .35f * t,
                role == GlassMaterialRole.Panel)
        }
    }
}

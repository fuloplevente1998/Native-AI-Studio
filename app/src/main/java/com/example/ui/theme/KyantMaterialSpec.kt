package com.example.ui.theme

import kotlin.math.pow

/** One Kyant material family: roles change optical parameters, not rendering engines. */
internal enum class GlassMaterialRole { Panel, Tile, Navigation, Selection, Button, Input, Overlay, Performance }

/** Parameters are pure and cached by the stored 0–100 clarity step. */
internal data class KyantMaterialSpec(
    val blurDp: Float,
    val lensHeightDp: Float,
    val lensAmountDp: Float,
    val tintAlpha: Float,
    val saturation: Float,
    val colorScale: Float,
    val sceneScale: Float,
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

        // Positive diagonal gain is the maximum after Android's saturation matrix;
        // off-diagonal terms are non-positive. The guarded scene bounds each channel.
        internal fun sceneWorst(coat: Float, saturation: Float, scale: Float): Float {
            val weights = floatArrayOf(.2126f, .7152f, .0722f)
            val channels = FloatArray(3) { i ->
                val maxChannel = GlassScene.srgb((GlassScene.lumaCap / weights[i]).coerceAtMost(1f))
                (maxChannel * GlassContrast.saturationGain(saturation, i) * scale).coerceIn(0f, 1f)
            }
            return GlassContrast.composite(coat, channels[0], channels[1], channels[2])
        }

        private fun sceneScale(coat: Float, saturation: Float): Float {
            var low = 0f
            var high = 1f
            repeat(16) {
                val mid = (low + high) / 2f
                if (sceneWorst(coat, saturation, mid) <= GlassContrast.target) low = mid else high = mid
            }
            return low
        }

        private fun compute(role: GlassMaterialRole, t: Float): KyantMaterialSpec {
            val baseBlur = 24f * (.5f / 24f).pow(t)
            val baseHeight = 6f + 18f * t
            val baseAmount = 10f + 38f * t
            val saturation = 1.08f + .22f * t
            // Tile paint is composited over its parent, not separately captured or blurred.
            val (blur, lens, coat) = when (role) {
                GlassMaterialRole.Panel -> Triple(1f, 1.12f, .20f - .15f * t)
                GlassMaterialRole.Tile -> Triple(0f, 0f, .16f - .06f * t)
                GlassMaterialRole.Navigation -> Triple(.8f, .65f, .12f - .09f * t)
                GlassMaterialRole.Selection -> Triple(.65f, 1.50f, .16f - .12f * t)
                GlassMaterialRole.Button -> Triple(.55f, 1.30f, .14f - .10f * t)
                GlassMaterialRole.Input -> Triple(1.1f, .35f, .23f - .13f * t)
                GlassMaterialRole.Overlay -> Triple(1.4f, .3f, .28f - .16f * t)
                GlassMaterialRole.Performance -> Triple(0f, 0f, .16f - .06f * t)
            }
            // A backdrop may contain bright text or a bright photo. Keep the existing main
            // contrast ceiling even for unknown source colours, without a second shader.
            val scale = if (blur == 0f) 1f else GlassContrast.blurOnlyScale(coat, saturation)
            val sceneScale = if (blur == 0f) 1f else sceneScale(coat, saturation)
            return KyantMaterialSpec(baseBlur * blur, baseHeight * lens, baseAmount * lens,
                coat, saturation, scale, sceneScale,
                if (role == GlassMaterialRole.Selection || role == GlassMaterialRole.Button) .45f + .45f * t else .35f + .35f * t,
                role == GlassMaterialRole.Panel || role == GlassMaterialRole.Overlay ||
                    role == GlassMaterialRole.Button || role == GlassMaterialRole.Selection)
        }
    }
}

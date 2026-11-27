package com.example.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.lerp
import com.example.model.MainTab
import com.example.model.SubScreen

internal const val NAVIGATION_FADE_MILLIS = 240

/** A bounded neutral coat keeps the different route backdrops in the same tonal family.
 * Only the new backdrop is revealed during a route change; no old-window bitmap or
 * duplicate header is retained. The labels, icons, press feedback and touch targets
 * remain fully opaque and keep their current semantics.
 */
internal fun navigationHeaderCoatAlpha(reveal: Float): Float =
    .30f + .60f * reveal.coerceIn(0f, 1f)

@Composable
internal fun Modifier.glassNavigationFade(
    palette: TrainPilotPalette, tab: MainTab, subScreen: SubScreen
): Modifier {
    if (!palette.isLiquidGlass) return this
    val route = tab to subScreen
    val motion = rememberGlassMotionEnabled() && !LocalGlassPowerSave.current &&
        LocalGlassOpticsEnabled.current
    val reveal = remember { Animatable(0f) }
    var previousRoute by remember { mutableStateOf(route) }
    LaunchedEffect(route, motion) {
        val changed = previousRoute != route
        previousRoute = route
        if (!motion) {
            reveal.snapTo(0f)
        } else if (changed) {
            // Cover the new optical source briefly with the common tone, then reveal it.
            // Rapid navigation cancels the previous tween instead of stacking animations.
            reveal.snapTo(1f)
            reveal.animateTo(0f, tween(NAVIGATION_FADE_MILLIS, easing = FastOutSlowInEasing))
        }
    }
    val neutral = GlassContrast.grey(.035f)
    val hue = GlassContrast.limit(palette.background, .065f)
    val coat = lerp(neutral, hue, .35f)
    return drawBehind { drawRect(coat.copy(alpha = navigationHeaderCoatAlpha(if (motion) reveal.value else 0f))) }
}

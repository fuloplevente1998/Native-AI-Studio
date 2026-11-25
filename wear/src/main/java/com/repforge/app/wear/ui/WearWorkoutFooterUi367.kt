package com.repforge.app.wear

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import org.json.JSONObject

/** The single workout action (#367 3.1): same size and slot for every exercise and screen type. */
@Composable
internal fun WearPrimaryFooter(
    state: WearFooterState,
    recordLabel: String,
    onRecord: () -> Unit,
    onSkipRest: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onResumeAuto: () -> Unit = {},
) {
    CompositionLocalProvider(LocalActionSize provides WEAR_FOOTER_BUTTON_DP) {
        val enabled = state.enabled
        when (state.kind) {
            WearFooterKind.WAIT_SYNC -> RoundControl("…", enabled = false, caption = wearText("Egyeztetés"),
                description = wearText("Visszavonás egyeztetése"), onClick = {})
            WearFooterKind.WAIT_PHONE -> RoundControl("…", enabled = false, caption = wearText("Telefon"),
                description = wearText("Várakozás a telefonra"), onClick = {})
            WearFooterKind.RESUME_AUTO -> RoundControl("▶", caption = wearText("Folytatás"),
                description = wearText("Automata folytatása"), onClick = onResumeAuto)
            WearFooterKind.SKIP_REST -> RoundControl("»", caption = wearText("Kihagyás"),
                description = wearText("Pihenő kihagyása"), onClick = onSkipRest)
            WearFooterKind.NEXT -> RoundControl("›", Gold, enabled = enabled, caption = wearText("Következő"),
                description = wearText("Következő gyakorlat"), onClick = onNext)
            // Finishing has its own glyph and tone, never a second check mark.
            WearFooterKind.FINISH -> RoundControl("■", Gold, enabled = enabled, caption = wearText("Befejezés"),
                description = wearText("Edzés befejezése"), onClick = onFinish)
            WearFooterKind.RECORD -> RoundControl("✓", SaveTone, enabled = enabled, caption = recordLabel,
                description = if (recordLabel == wearText("Mentés")) wearText("Idő rögzítése") else wearText("Sorozat rögzítése"),
                onClick = onRecord)
        }
    }
}

/** Fixed heart-rate row above the action: never covered, "–" when nothing real is measured. */
@Composable
internal fun WearPinnedPulse(workout: WearWorkout, health: JSONObject?) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(workout.workoutId) {
        while (true) { now = System.currentTimeMillis(); delay(1000L) }
    }
    val pulse = wearWorkoutPulse(health, workout.workoutId, now)
    val fontScale = LocalDensity.current.fontScale
    val scale = fontScale.coerceAtMost(1.1f) / fontScale
    Text(
        "♥ " + (pulse.bpm?.let { "$it bpm" } ?: "–"),
        color = if (pulse.live) LocalWearHomeTheme.current.secondaryColor else Muted,
        fontSize = (11.5f * scale).sp, lineHeight = (13f * scale).sp, fontWeight = FontWeight.SemiBold,
        // Visible: a descender or the larger heart glyph must never be clipped by the fixed 14 dp row.
        maxLines = 1, overflow = TextOverflow.Visible, modifier = Modifier.testTag("wear-live-pulse"),
    )
}

/** Fixed-size glyph + time: only the drawn glyph differs between states, never size or place. */
@Composable
internal fun WearTimerFace(glyph: WearTimerGlyph, text: String, color: Color, modifier: Modifier = Modifier) {
    val fontScale = LocalDensity.current.fontScale
    val lineScale = minOf(fontScale, WEAR_TIMER_MAX_FONT_SCALE) / fontScale
    Row(modifier.height(WEAR_TIMER_BOX_DP.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center) {
        Canvas(Modifier.size(WEAR_TIMER_GLYPH_DP.dp)) {
            val w = size.width
            val h = size.height
            when (glyph) {
                WearTimerGlyph.PLAY -> drawPath(Path().apply {
                    moveTo(w * .22f, h * .1f); lineTo(w * .92f, h * .5f); lineTo(w * .22f, h * .9f); close()
                }, color)
                WearTimerGlyph.PAUSE -> {
                    drawRoundRect(color, Offset(w * .14f, h * .1f), Size(w * .26f, h * .8f), CornerRadius(w * .05f))
                    drawRoundRect(color, Offset(w * .6f, h * .1f), Size(w * .26f, h * .8f), CornerRadius(w * .05f))
                }
                WearTimerGlyph.DONE -> drawPath(Path().apply {
                    moveTo(w * .08f, h * .54f); lineTo(w * .38f, h * .84f); lineTo(w * .94f, h * .18f)
                }, color, style = Stroke(w * .14f, cap = StrokeCap.Round))
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text, color = color, fontSize = wearTimerFontSp(fontScale).sp,
            lineHeight = (WEAR_TIMER_LINE_SP * lineScale).sp, maxLines = 1, softWrap = false,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp, fontFeatureSettings = "tnum"),
            modifier = Modifier.testTag("wear-timer-text"),
        )
    }
}

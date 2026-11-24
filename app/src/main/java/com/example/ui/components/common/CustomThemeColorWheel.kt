package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette
import kotlin.math.*

/** One visual target, with a hue ring and simultaneous saturation/value control. */
internal data class ColorWheelGeometry(val side: Float) {
    val center = Offset(side / 2, side / 2)
    val radius = side * .426f
    val ringWidth = side * .085f
    val fieldSide = side * .48f
    val fieldOrigin = Offset((side - fieldSide) / 2, (side - fieldSide) / 2)
    fun ringHit(point: Offset): Boolean =
        abs((point - center).getDistance() - radius) <= ringWidth
    fun fieldHit(point: Offset): Boolean =
        point.x in fieldOrigin.x..(fieldOrigin.x + fieldSide) &&
            point.y in fieldOrigin.y..(fieldOrigin.y + fieldSide)
    fun hueAt(point: Offset): Float =
        ((atan2(point.y - center.y, point.x - center.x) * 180f / PI.toFloat()) + 360f) % 360f
    fun saturationAt(point: Offset) = ((point.x - fieldOrigin.x) / fieldSide).coerceIn(0f, 1f)
    fun valueAt(point: Offset) = (1f - (point.y - fieldOrigin.y) / fieldSide).coerceIn(0f, 1f)
    fun hueMarker(hue: Float): Offset {
        val radians = hue * PI.toFloat() / 180f
        return center + Offset(cos(radians), sin(radians)) * radius
    }
    fun fieldMarker(saturation: Float, value: Float) =
        fieldOrigin + Offset(fieldSide * saturation, fieldSide * (1f - value))
}

@Composable
internal fun CustomThemeColorWheel(hsv: ThemeHsv, language: AppLanguage, onChange: (ThemeHsv) -> Unit) {
    val current by rememberUpdatedState(hsv)
    val callback by rememberUpdatedState(onChange)
    val p = LocalTrainPilotPalette.current
    var accessible by remember { mutableStateOf(false) }
    fun word(hu: String, en: String, de: String, ro: String) = customThemeText(language, hu, en, de, ro)
    val hue = word("Árnyalat", "Hue", "Farbton", "Nuanță")
    val saturation = word("Telítettség", "Saturation", "Sättigung", "Saturație")
    val brightness = word("Fényerő", "Brightness", "Helligkeit", "Luminozitate")
    val wheelName = word("Színkerék", "Color wheel", "Farbkreis", "Cerc cromatic")
    val spectrum = remember { (0..12).map { Color.hsv((it % 12) * 30f, 1f, 1f) } }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1f)
            .testTag("custom_theme_color_wheel")
            .semantics {
                contentDescription = wheelName
                stateDescription = hsv.hex()
            }.pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val g = ColorWheelGeometry(size.width.toFloat())
                    val ring = g.ringHit(down.position)
                    if (!ring && !g.fieldHit(down.position)) return@awaitEachGesture
                    fun apply(point: Offset) {
                        callback(if (ring) current.copy(hue = g.hueAt(point))
                            else current.copy(saturation = g.saturationAt(point), value = g.valueAt(point)))
                    }
                    down.consume()
                    apply(down.position)
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.pressed) {
                            apply(change.position)
                            change.consume()
                        }
                    } while (change.pressed)
                }
            }) {
            val g = ColorWheelGeometry(size.width)
            drawCircle(Brush.sweepGradient(spectrum, g.center), g.radius, g.center, style = Stroke(g.ringWidth))
            val rect = Size(g.fieldSide, g.fieldSide)
            drawRect(Color.hsv(hsv.hue.coerceIn(0f, 359.999f), 1f, 1f), g.fieldOrigin, rect)
            drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent),
                startX = g.fieldOrigin.x, endX = g.fieldOrigin.x + g.fieldSide), g.fieldOrigin, rect)
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black),
                startY = g.fieldOrigin.y, endY = g.fieldOrigin.y + g.fieldSide), g.fieldOrigin, rect)
            for (point in listOf(g.hueMarker(hsv.hue), g.fieldMarker(hsv.saturation, hsv.value))) {
                drawCircle(Color.Black, 9.dp.toPx(), point, style = Stroke(5.dp.toPx()))
                drawCircle(Color.White, 9.dp.toPx(), point, style = Stroke(2.dp.toPx()))
            }
        }
        Text(word("Külső kör: szín · Belső mező: világosság és telítettség",
            "Ring: hue · Field: brightness and saturation", "Ring: Farbton · Feld: Helligkeit und Sättigung",
            "Cerc: nuanță · Câmp: luminozitate și saturație"), color = p.textSecondary)
        TrainPilotFilterChip(accessible, { accessible = !accessible }, {
            Text(word("Pontos beállítás", "Fine adjustment", "Feineinstellung", "Reglaj fin"))
        })
        // Native adjustable semantics for keyboard/TalkBack; hidden by default, not three permanent rows.
        if (accessible) {
            Slider(hsv.hue, { onChange(hsv.copy(hue = it)) }, valueRange = 0f..359.999f,
                modifier = Modifier.semantics { contentDescription = hue }.testTag("custom_theme_hue"))
            Slider(hsv.saturation, { onChange(hsv.copy(saturation = it)) },
                modifier = Modifier.semantics { contentDescription = saturation }.testTag("custom_theme_saturation"))
            Slider(hsv.value, { onChange(hsv.copy(value = it)) },
                modifier = Modifier.semantics { contentDescription = brightness }.testTag("custom_theme_brightness"))
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import com.repforge.app.ui.theme.trainPilotClickable

internal fun customThemeText(language: AppLanguage, hu: String, en: String, de: String, ro: String): String =
    when (language) {
        AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
    }

internal fun CustomThemeRole.title(language: AppLanguage): String = when (this) {
    CustomThemeRole.BACKGROUND -> customThemeText(language, "Háttér", "Background", "Hintergrund", "Fundal")
    CustomThemeRole.CARD -> customThemeText(language, "Kártya", "Card", "Karte", "Card")
    CustomThemeRole.PRIMARY -> customThemeText(language, "Kiemelés", "Accent", "Akzent", "Accent")
    CustomThemeRole.SECONDARY -> customThemeText(language, "Másodlagos", "Secondary", "Sekundär", "Secundar")
    CustomThemeRole.BORDER -> customThemeText(language, "Keret", "Border", "Rahmen", "Contur")
}

internal fun CustomThemeStyle.title(language: AppLanguage): String = when (this) {
    CustomThemeStyle.MATTE -> customThemeText(language, "Matt", "Matte", "Matt", "Mat")
    CustomThemeStyle.CONTRAST -> customThemeText(language, "Kontrasztos", "Contrast", "Kontrast", "Contrast")
    CustomThemeStyle.GLOW -> "Glow"
    CustomThemeStyle.LIGHT -> customThemeText(language, "Világos", "Light", "Hell", "Deschis")
}

internal data class ThemeHsv(val hue: Float, val saturation: Float, val value: Float) {
    fun hex(): String = Color.hsv(hue.coerceIn(0f, 359.999f), saturation, value).customThemeHex()
}

private fun String.toThemeHsv(fallbackHue: Float = 0f): ThemeHsv {
    val c = NativeThemeTokens.parseHex(this, Color.Black)
    val high = maxOf(c.red, c.green, c.blue)
    val low = minOf(c.red, c.green, c.blue)
    val delta = high - low
    val hue = if (delta == 0f) fallbackHue else when (high) {
        c.red -> 60f * ((c.green - c.blue) / delta)
        c.green -> 60f * ((c.blue - c.red) / delta + 2f)
        else -> 60f * ((c.red - c.green) / delta + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return ThemeHsv(hue, if (high == 0f) 0f else delta / high, high)
}

@Composable
internal fun CustomThemeColorControls(
    role: CustomThemeRole,
    value: String,
    language: AppLanguage,
    onValueChange: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val normalized = normalizeCustomThemeHex(value)
    var hsv by remember(role) { mutableStateOf((normalized ?: "#000000").toThemeHsv()) }
    var lastSliderHex by remember(role) { mutableStateOf<String?>(null) }
    LaunchedEffect(role, normalized) {
        if (normalized != null && normalized != lastSliderHex) {
            hsv = normalized.toThemeHsv(hsv.hue)
            lastSliderHex = null
        }
    }
    fun update(next: ThemeHsv) {
        hsv = next
        lastSliderHex = next.hex()
        onValueChange(next.hex())
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrainPilotTextField(value = value, onValueChange = { lastSliderHex = null; onValueChange(it) },
                modifier = Modifier.weight(1f).testTag("custom_theme_hex"), singleLine = true,
                isError = normalized == null, label = { Text("HEX") },
                supportingText = if (normalized == null) {
                    { Text(customThemeText(language, "Formátum: #RRGGBB", "Format: #RRGGBB",
                        "Format: #RRGGBB", "Format: #RRGGBB")) }
                } else null)
            Box(Modifier.size(48.dp).background(
                normalized?.let { NativeThemeTokens.parseHex(it, palette.surface) } ?: palette.surface,
                RoundedCornerShape(10.dp)).border(1.dp, palette.fieldOutline(), RoundedCornerShape(10.dp)))
        }
        CustomThemeColorWheel(hsv, language, ::update)
        Text(customThemeText(language, "Gyors színek", "Quick colors", "Schnellfarben", "Culori rapide"),
            color = palette.textPrimary)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            role.quickColors().forEach { hex ->
                val shape = RoundedCornerShape(10.dp)
                Box(Modifier.size(48.dp).trainPilotClickable(shape) { lastSliderHex = null; onValueChange(hex) }
                    .semantics { contentDescription = hex }
                    .border(if (normalized == hex) 3.dp else 1.dp,
                        if (normalized == hex) palette.selectionColors().border else palette.fieldOutline(), shape)
                    .padding(5.dp).background(NativeThemeTokens.parseHex(hex, palette.surface), shape))
            }
        }
    }
}

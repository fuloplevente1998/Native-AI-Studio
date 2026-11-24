package com.repforge.app.ui.journal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import com.repforge.app.ExternalWorkoutMetrics
import com.repforge.app.ui.theme.LocalTrainPilotColors
import org.json.JSONArray
import java.util.Locale

/** User-defined max HR; counts only measured samples, never time interpolation or estimated maximum. */
@Composable
internal fun ExternalHeartRateZones(series: JSONArray, language: String) {
    var max by remember { mutableStateOf("") }
    val colors = LocalTrainPilotColors.current
    val samples = remember(series) {
        (0 until series.length()).mapNotNull { (series.optJSONObject(it)?.opt("value") as? Number)?.toDouble() }
    }
    fun tr(hu: String, en: String, de: String, ro: String) = when (language.lowercase(Locale.ROOT).take(2)) {
        "hu" -> hu; "de" -> de; "ro" -> ro; else -> en
    }
    Column(Modifier.fillMaxWidth().testTag("external_hr_zones")) {
        OutlinedTextField(value = max, onValueChange = { max = it.filter(Char::isDigit).take(3) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
            label = { Text(tr("Saját ismert maxpulzus (bpm)", "Known personal max HR (bpm)",
                "Bekannter persönlicher Maximalpuls (bpm)", "Puls maxim personal cunoscut (bpm)")) })
        val counts = max.toIntOrNull()?.let { ExternalWorkoutMetrics.zones(samples, it) }
        if (counts != null) {
            Text(tr("Mért minták zónánként, nem időtartam", "Measured samples per zone, not time",
                "Messwerte je Zone, keine Dauer", "Eșantioane pe zonă, nu durată"), color = colors.muted)
            counts.forEachIndexed { i, count ->
                val band = if (i == 0) "<50%" else if (i == 5) "≥90%" else "${40 + i * 10}–${50 + i * 10}%"
                Text("$band: $count", color = colors.text)
            }
        }
    }
}

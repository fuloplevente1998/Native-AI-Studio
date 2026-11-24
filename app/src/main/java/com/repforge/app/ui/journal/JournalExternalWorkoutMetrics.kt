package com.repforge.app.ui.journal

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.ui.theme.LocalTrainPilotColors
import org.json.JSONObject
import java.text.NumberFormat
import java.time.Instant
import java.util.Locale

@Composable
internal fun JournalExternalWorkoutMetrics(detail: WorkoutHistoryWithDetails, text: Resources, language: String) {
    val colors = LocalTrainPilotColors.current
    val workout = detail.workout
    val source = remember(workout.rawJson) {
        runCatching { JSONObject(workout.rawJson).optJSONObject("externalWorkout359") }.getOrNull()
    }
    val number = remember(language) { NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = 1 } }
    fun local(hu: String, en: String, de: String, ro: String) = when (language.lowercase(Locale.ROOT).substringBefore('-')) {
        "hu" -> hu; "de" -> de; "ro" -> ro; else -> en
    }
    val matches = source != null && runCatching {
        Instant.parse(source.getString("start")) == Instant.parse(workout.started) &&
            Instant.parse(source.getString("end")) == Instant.parse(workout.finished)
    }.getOrDefault(false)
    val values = if (matches && source?.optBoolean("windowChanged") != true)
        source?.optJSONObject("metrics")?.optJSONObject("values") else null
    val metrics = values?.keys()?.asSequence()?.mapNotNull { key ->
        (values.opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() && it >= 0 }?.let { key to it }
    }?.toMap().orEmpty()
    Column(Modifier.fillMaxWidth().padding(12.dp).testTag("journal_external_metrics"),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(local("Külső edzés mérései", "External workout measurements", "Externe Trainingsmessungen", "Măsurători antrenament extern"),
            style = MaterialTheme.typography.titleMedium, color = colors.text)
        source?.optString("source")?.takeIf { it.isNotBlank() }?.let { Text(it, color = colors.muted) }
        source?.optString("title")?.takeIf { it.isNotBlank() }?.let { Text(it, color = colors.text) }
        if (source != null && !matches) Text(local("A forrás és a helyi edzés időablaka eltér. Ellenőrzés szükséges.",
            "Source and local workout windows differ. Review required.", "Quell- und lokales Zeitfenster unterscheiden sich. Prüfung erforderlich.",
            "Intervalele sursei și antrenamentului local diferă. Necesită verificare."), color = colors.muted)
        if (metrics.isEmpty()) Text(text.getString(com.repforge.app.R.string.journal_no_data), color = colors.muted)
        else {
            JournalHealthMetrics(metrics, "external", text, number)
            Text(local("Részleges, azonos forrású tárolt mérés; az átlagok mintákból származnak.",
                "Partial same-source cached evidence; averages are sample-based.",
                "Teilweise gespeicherte Messwerte derselben Quelle; Mittelwerte aus Stichproben.",
                "Date parțiale stocate din aceeași sursă; mediile provin din eșantioane."), color = colors.muted)
        }
    }
}

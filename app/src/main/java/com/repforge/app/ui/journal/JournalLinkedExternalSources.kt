package com.repforge.app.ui.journal

import android.content.res.Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.ExternalWorkoutAssociation
import com.repforge.app.ui.theme.LocalTrainPilotColors
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun JournalLinkedExternalSources(raw: String, text: Resources, language: String) {
    val sources = remember(raw) { runCatching { ExternalWorkoutAssociation.snapshots(JSONObject(raw)) }.getOrDefault(emptyList()) }
    if (sources.isEmpty()) return
    val colors = LocalTrainPilotColors.current
    val number = remember(language) { NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = 1 } }
    val heading = when (language.lowercase(Locale.ROOT).substringBefore('-')) {
        "hu" -> "Társított külső forrás (külön mérés)"
        "de" -> "Verknüpfte externe Quelle (separate Messung)"
        "ro" -> "Sursă externă asociată (măsurare separată)"
        else -> "Linked external source (separate evidence)"
    }
    Column(Modifier.fillMaxWidth().testTag("journal_external_linked_sources"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(heading, color = colors.text)
        sources.forEachIndexed { i, source ->
            Text(source.optString("source"), color = colors.muted)
            Text("${source.optString("start")} – ${source.optString("end")}", color = colors.muted)
            val values = source.optJSONObject("metrics")?.optJSONObject("values")
            val map = values?.let { v -> v.keys().asSequence().mapNotNull { k -> (v.opt(k) as? Number)?.toDouble()?.let { k to it } }.toMap() }.orEmpty()
            JournalHealthMetrics(map, "external_linked_$i", text, number)
        }
    }
}

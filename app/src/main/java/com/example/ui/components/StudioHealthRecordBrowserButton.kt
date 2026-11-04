package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.repforge.app.HealthJournalStore
import com.repforge.app.ui.health.HealthRecordBrowser
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import java.time.LocalDate

/** Read-only native browser with its existing date range, origin/type and paging controls. */
@Composable
internal fun StudioHealthRecordBrowserButton(provider: String, language: String) {
    require(provider in setOf(HealthJournalStore.HC, HealthJournalStore.SAMSUNG))
    var opened by rememberSaveable(provider) { mutableStateOf(false) }
    val day = remember(opened) { LocalDate.now().toString() }
    val label = when (language.lowercase()) {
        "en" -> "Detailed data log • date and data source"
        "de" -> "Detailliertes Datenprotokoll • Datum und Datenquelle"
        "ro" -> "Jurnal detaliat de date • dată și sursă de date"
        else -> "Részletes adatnapló • dátum és adatforrás"
    }
    TrainPilotOutlinedButton(onClick = { opened = true }, modifier = Modifier.fillMaxWidth()
        .testTag("studio_health_record_browser")) { Text(label) }
    if (opened) StudioNativeEditorTheme {
        HealthRecordBrowser(day, provider, "", language, onClose = { opened = false })
    }
}

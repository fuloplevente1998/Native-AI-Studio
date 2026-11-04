package com.example.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.HealthJournalStore
import com.repforge.app.ui.health.HealthRecordBrowser
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import java.time.LocalDate

/** Read-only native browser with its existing date range, origin/type and paging controls. */
@Composable
internal fun StudioHealthRecordBrowserButton(provider: String, language: String) {
    require(provider in setOf(HealthJournalStore.HC, HealthJournalStore.SAMSUNG))
    val palette = LocalTrainPilotPalette.current
    var opened by rememberSaveable(provider) { mutableStateOf(false) }
    val day = remember(opened) { LocalDate.now().toString() }
    val label = when (language.lowercase()) {
        "en" -> "Detailed Health Measurements • Date & Source"
        "de" -> "Detaillierte Gesundheitsmessungen • Datum & Quelle"
        "ro" -> "Măsurători detaliate de sănătate • Dată și sursă"
        else -> "Részletes egészségmérések • dátum és forrás"
    }
    TrainPilotOutlinedButton(
        onClick = { opened = true },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("studio_health_record_browser")
    ) {
        Icon(
            imageVector = Icons.Default.Timeline,
            contentDescription = null,
            tint = palette.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold, color = palette.textPrimary)
    }
    if (opened) StudioNativeEditorTheme {
        HealthRecordBrowser(day, provider, "", language, onClose = { opened = false })
    }
}

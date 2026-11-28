package com.example.ui.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.*
import com.repforge.app.ui.theme.trainPilotClickable

/** Shared settings/first-run language choices; ordinary themes retain individual buttons. */
@Composable
internal fun StudioLanguageSelector(language: AppLanguage, onSelectLanguage: (AppLanguage) -> Unit,
    tag: String = "settings_language", enabled: Boolean = true) {
    val palette = LocalTrainPilotPalette.current
    if (palette.isLiquidGlass) {
        val languages = AppLanguage.entries
        com.example.ui.components.LiquidSegmentedTabs(languages.map { "${it.flag} ${it.code}" },
            languages.indexOf(language), { onSelectLanguage(languages[it]) }, "${tag}_track", enabled = enabled, itemTag = { "${tag}_${languages[it].code}" }) { _, label, _ ->
            com.example.ui.components.TrainPilotFitText(label, style = MaterialTheme.typography.labelMedium)
        }
    } else Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppLanguage.entries.forEach { lang ->
            val selected = language == lang
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else if (selected) palette.primary else palette.surfaceElevated,
                border = if (palette.isLiquidGlass && selected) BorderStroke(1.dp, palette.primary) else null,
                modifier = Modifier
                    .weight(1f)
                    .liquidDropMaterial(palette, RoundedCornerShape(10.dp), if (selected) GlassMaterialRole.Selection else GlassMaterialRole.Button,
                        accent = if (selected) palette.primary else null, live = selected)
                    .semantics { this.selected = selected }
                    .testTag("${tag}_${lang.code}")
                    .trainPilotClickable(RoundedCornerShape(10.dp)) { if (enabled) onSelectLanguage(lang) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "${lang.flag} ${lang.code}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selected && !palette.isLiquidGlass) palette.onPrimary else palette.textPrimary
                    )
                }
            }
        }
    }
}

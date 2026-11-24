package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.*
import kotlin.math.roundToInt

/** One transient preview and one durable commit per completed slider interaction. */
internal data class GlassThemeAccess(
    val clarity: Int,
    val preview: (Int) -> Unit,
    val commit: (Int) -> Unit
)
internal val LocalGlassThemeAccess = staticCompositionLocalOf<GlassThemeAccess?> { null }

@Composable
internal fun LiquidGlassControl(language: AppLanguage, selectedId: Int) {
    val access = LocalGlassThemeAccess.current
    var local by remember { mutableIntStateOf(DEFAULT_GLASS_CLARITY) }
    val clarity = access?.clarity ?: local
    val palette = LocalTrainPilotPalette.current
    val candidate = remember(selectedId, clarity) {
        TrainPilotPalettes.getById(selectedId.takeIf { it in 33..40 } ?: 35).withGlassClarity(clarity)
    }
    var latest by remember { mutableIntStateOf(clarity) }
    LaunchedEffect(clarity) { latest = clarity }
    val label = customThemeText(language, "Üveghatás", "Glass clarity", "Glas-Klarheit", "Claritatea sticlei")
    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label: $clarity%", color = palette.textPrimary, style = MaterialTheme.typography.labelLarge)
        Slider(clarity.toFloat(), { value ->
            val next = value.roundToInt().coerceIn(0, 100)
            latest = next
            if (access == null) local = next else access.preview(next)
        }, valueRange = 0f..100f, onValueChangeFinished = { access?.commit(latest) },
            modifier = Modifier.testTag("liquid_glass_clarity").semantics { contentDescription = label })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Frosted", color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
            Text("Clear Liquid", color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
        }
        LiquidGlassMaterialPreview(candidate, language)
    }
}

@Composable
private fun LiquidGlassMaterialPreview(p: TrainPilotPalette, language: AppLanguage) {
    val shape = RoundedCornerShape(18.dp)
    Box(Modifier.fillMaxWidth().clip(shape).trainPilotGlassScene(p)
        .padding(8.dp).testTag("liquid_glass_preview")) {
        Column(Modifier.fillMaxWidth().clip(shape).trainPilotGlassMaterial(p, shape).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(customThemeText(language, "Élő üvegelőnézet", "Live glass preview",
                "Live-Glasvorschau", "Previzualizare sticlă"), color = p.textSecondary,
                style = MaterialTheme.typography.labelMedium)
            Text(customThemeText(language, "Aktív program", "Active program", "Aktives Programm", "Program activ"),
                color = Color.White, style = MaterialTheme.typography.titleMedium)
            Box(Modifier.fillMaxWidth().clip(shape).trainPilotGlassMaterial(p, shape).padding(12.dp),
                contentAlignment = Alignment.Center) {
                Text(customThemeText(language, "Edzés indítása", "Start workout", "Training starten",
                    "Începe antrenamentul"), color = p.primary)
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
    // The thumb follows the finger continuously; every crossed 1% step previews live (the whole
    // app re-tints), and only the release writes the durable value once.
    var dragging by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(clarity) { if (dragging == null) latest = clarity }
    val label = customThemeText(language, "Üveghatás", "Glass clarity", "Glas-Klarheit", "Claritatea sticlei")
    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label: $clarity%", color = palette.textPrimary, style = MaterialTheme.typography.labelLarge)
        Slider(dragging ?: clarity.toFloat(), { value ->
            dragging = value
            val next = glassClarityStep(value)
            if (next != latest) {
                latest = next
                if (access == null) local = next else access.preview(next)
            }
        }, valueRange = 0f..100f, onValueChangeFinished = {
            dragging = null
            if (access == null) local = latest else access.commit(latest)
        }, modifier = Modifier.testTag("liquid_glass_clarity").semantics { contentDescription = label })
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
    val backdrop = rememberGlassBackdrop()
    val scroll = androidx.compose.foundation.rememberScrollState()
    Box(Modifier.fillMaxWidth().height(164.dp).clip(shape).testTag("liquid_glass_preview")) {
        Row(Modifier.fillMaxSize().captureGlassBackdrop(backdrop)
            .background(p.background)
            .horizontalScroll(scroll).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            repeat(12) { i ->
                Box(Modifier.width(34.dp).height((44 + (i * 37 % 104)).dp)
                    .background(if (i % 2 == 0) p.primary else p.secondary.copy(alpha = .65f), RoundedCornerShape(12.dp)))
            }
        }
        Column(Modifier.align(Alignment.Center).fillMaxWidth(.82f).clip(shape)
            .liveGlassLens(backdrop, p, shape).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(customThemeText(language, "Élő üvegelőnézet", "Live glass preview",
                "Live-Glasvorschau", "Previzualizare sticlă"), color = Color.White,
                style = MaterialTheme.typography.labelMedium)
            Text(customThemeText(language, "Aktív program", "Active program", "Aktives Programm", "Program activ"),
                color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(customThemeText(language, "Húzd oldalra a hátteret", "Drag the background sideways",
                "Hintergrund seitlich ziehen", "Trage fundalul lateral"), color = Color.White,
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

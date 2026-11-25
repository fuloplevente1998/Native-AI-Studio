package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import com.repforge.app.ui.theme.trainPilotClickable

/** One transient preview and one durable commit per completed slider interaction. */
internal data class GlassThemeAccess(
    val clarity: Int,
    val preview: (Int) -> Unit,
    val commit: (Int) -> Unit,
    /** 2809/1 B: relative private path of the background image ("" = living backdrop). */
    val backdropImage: String = "",
    val backdropBusy: Boolean = false,
    val backdropFailed: Boolean = false,
    val pickBackdrop: (android.net.Uri) -> Unit = {},
    val clearBackdrop: () -> Unit = {}
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
        if (access != null && androidx.activity.compose.LocalActivityResultRegistryOwner.current != null)
            LiquidGlassBackdropPicker(access, language)
        KyantEngineNotice(language)
    }
}

/** [Kyant test build] Apache-2.0 attribution of the glass engine; tap shows the bundled licence text. */
@Composable
private fun KyantEngineNotice(language: AppLanguage) {
    val palette = LocalTrainPilotPalette.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var open by remember { mutableStateOf(false) }
    val text = remember(open) {
        if (!open) "" else runCatching {
            context.assets.open("third-party-licenses/kyant-backdrop-NOTICE.txt").bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }
    Text(customThemeText(language, "Üvegmotor (teszt): Kyant Backdrop 1.0.6 – Apache License 2.0",
        "Glass engine (test): Kyant Backdrop 1.0.6 – Apache License 2.0",
        "Glas-Engine (Test): Kyant Backdrop 1.0.6 – Apache License 2.0",
        "Motor sticlă (test): Kyant Backdrop 1.0.6 – Apache License 2.0"),
        color = palette.textSecondary, style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("kyant_engine_notice")
            .trainPilotClickable(RoundedCornerShape(8.dp)) { open = !open }.padding(vertical = 14.dp))
    if (open && text.isNotEmpty()) Box(Modifier.fillMaxWidth().heightIn(max = 240.dp)
        .verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Text(text, color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * 2809/1 B: optional background image for the glass family. The system photo picker (falls back to
 * the document picker on older devices) needs no storage permission; the copy lives in private storage.
 */
@Composable
private fun LiquidGlassBackdropPicker(access: GlassThemeAccess, language: AppLanguage) {
    val palette = LocalTrainPilotPalette.current
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) access.pickBackdrop(uri)
    }
    val hasImage = access.backdropImage.isNotEmpty()
    Column(Modifier.fillMaxWidth().testTag("liquid_glass_backdrop"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(customThemeText(language, "Háttér", "Background", "Hintergrund", "Fundal"),
            color = palette.textPrimary, style = MaterialTheme.typography.labelLarge)
        Text(if (hasImage) customThemeText(language, "Saját háttérkép (a szöveg olvashatósága miatt sötétítve)",
                "Own background image (dimmed to keep text readable)", "Eigenes Hintergrundbild (für Lesbarkeit abgedunkelt)",
                "Imagine de fundal proprie (întunecată pentru lizibilitate)")
            else customThemeText(language, "Élő színfoltok a téma színeiből", "Living colour blobs from the theme",
                "Lebendige Farbflächen aus dem Thema", "Pete de culoare animate din temă"),
            color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
        if (access.backdropFailed) Text(customThemeText(language, "A kép nem menthető. Próbáld újra.",
            "The image could not be saved. Try again.", "Das Bild konnte nicht gespeichert werden.", "Imaginea nu a putut fi salvată."),
            color = palette.danger, style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.repforge.app.ui.theme.TrainPilotOutlinedButton(enabled = !access.backdropBusy, modifier = Modifier.weight(1f)
                .heightIn(min = 48.dp).testTag("liquid_glass_backdrop_pick"), onClick = {
                picker.launch(androidx.activity.result.PickVisualMediaRequest(
                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text(customThemeText(language, "Háttérkép választása", "Choose image", "Bild wählen", "Alege imaginea"), maxLines = 1) }
            if (hasImage) com.repforge.app.ui.theme.TrainPilotOutlinedButton(enabled = !access.backdropBusy,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("liquid_glass_backdrop_remove"),
                onClick = access.clearBackdrop) {
                Text(customThemeText(language, "Eltávolítás", "Remove", "Entfernen", "Elimină"), maxLines = 1)
            }
        }
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

package com.example.ui.components
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.LocalGlassOverlay
import com.example.ui.theme.sheetUsesGlass
import com.example.ui.theme.glassOverlayFill
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.LocalGlassOpticsEnabled
import com.example.ui.theme.KyantGlass
import com.example.ui.theme.KyantWindowOverlay
import com.example.ui.theme.LocalGlassChromeUnderlap
import com.example.ui.theme.LocalGlassPowerSave
import com.repforge.app.ui.theme.trainPilotFrame
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.TrainPilotFrames
import com.repforge.app.ui.theme.trainPilotSoftGlow

/** Shared presentation only: caller retains every confirm/cancel/busy guard and owns dismissal. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TrainPilotAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    containerColor: Color = Color.Unspecified,
    iconContentColor: Color = Color.Unspecified,
    titleContentColor: Color = Color.Unspecified,
    textContentColor: Color = Color.Unspecified,
    tonalElevation: Dp = 0.dp,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    workoutEditor: Boolean = false,
    maxHeightFraction: Float = .86f
) {
    val p = LocalTrainPilotPalette.current
    val background = containerColor.takeIf { it != Color.Unspecified } ?: p.surfaceCard
    val titleColor = titleContentColor.takeIf { it != Color.Unspecified } ?: p.textPrimary
    val bodyColor = textContentColor.takeIf { it != Color.Unspecified } ?: p.textSecondary
    val compactType = MaterialTheme.typography.copy(
        headlineSmall = MaterialTheme.typography.titleMedium,
        titleLarge = MaterialTheme.typography.titleMedium
    )
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        // Keep third-party backdrop types out of the common dialog's JVM bytecode.
        val glass = sheetUsesGlass(p, background)
        val panel: @Composable (Modifier, Color) -> Unit = { overlay, fill ->
            MaterialTheme(typography = compactType) {
                Surface(color = fill, shape = shape, tonalElevation = tonalElevation,
                    border = if (workoutEditor) workoutEditorFrame(p) else null,
                    modifier = modifier.widthIn(max = 560.dp).fillMaxWidth(0.96f)
                        .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * maxHeightFraction)
                        .then(overlay)
                        .then(if (workoutEditor) Modifier.trainPilotSoftGlow(p.primary, shape, 4.dp)
                            else if (glass) Modifier.trainPilotGlassRim(p, shape, GlassMaterialRole.Overlay)
                            else Modifier.trainPilotSoftGlow(p.primary, shape, 4.dp)
                                .trainPilotFrame(TrainPilotFrames.page(p.sectionBorder, p.primary), shape))) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (title != null || icon != null) Row(Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (icon != null) CompositionLocalProvider(LocalContentColor provides
                                (iconContentColor.takeIf { it != Color.Unspecified } ?: p.primary)) { icon() }
                            if (title != null) Box(Modifier.weight(1f)) {
                                CompositionLocalProvider(LocalContentColor provides titleColor) {
                                    ProvideTextStyle(MaterialTheme.typography.titleMedium) { title() }
                                }
                            }
                        }
                        // A finite body region preserves existing scrollable forms/lists. Do not
                        // nest another verticalScroll around callers' own scrolling content.
                        if (text != null) Box(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                            CompositionLocalProvider(LocalContentColor provides bodyColor) {
                                ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() }
                            }
                        }
                        FlowRow(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    }
                }
            }
        }
        // Only the explicitly opted-in workout dialog gets the static local lens. The
        // measurement cards and the general optics gate remain untouched.
        val workoutOptics = workoutEditor && LocalGlassChromeUnderlap.current && !LocalGlassPowerSave.current
        if (glass && (LocalGlassOpticsEnabled.current || workoutOptics) && KyantGlass.active(p)) {
            KyantWindowOverlay(p, shape, staticSheet = workoutEditor) { overlay -> panel(overlay, Color.Transparent) }
        } else {
            CompositionLocalProvider(LocalGlassOverlay provides glass) {
                panel(Modifier, if (workoutEditor) workoutEditorFill(p, background)
                    else if (glass) p.glassOverlayFill(background) else background)
            }
        }
    }
}

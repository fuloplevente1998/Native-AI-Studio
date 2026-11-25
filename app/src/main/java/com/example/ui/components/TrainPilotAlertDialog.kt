package com.example.ui.components
import com.example.ui.theme.KyantMaterialSpec
import com.example.ui.theme.GlassContrast
import com.example.ui.theme.GlassOptics
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.rememberKyantScene
import com.example.ui.theme.rememberKyantSceneBackdrop
import com.example.ui.theme.kyantSceneLayer
import com.example.ui.theme.LocalKyantHeader
import com.example.ui.theme.LocalKyantScene
import com.example.ui.theme.LocalGlassOpticsEnabled
import com.example.ui.theme.KyantGlass
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import com.repforge.app.ui.theme.trainPilotFrame
import androidx.compose.foundation.border
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
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false)
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
        // Dialogs own a separate window. Never sample the parent's recording.
        val glass = p.isLiquidGlass && (background == p.surfaceCard || background == p.surface || background == p.background)
        val layer = if (glass && KyantGlass.active(p) && LocalGlassOpticsEnabled.current) rememberKyantSceneBackdrop(p) else null
        val scene = layer?.let { rememberKyantScene(it) }
        // A raw white coat would exceed the scene luminance ceiling, especially over photos.
        val overlayPaint = if (layer != null) GlassContrast.limit(GlassOptics.TINT, GlassOptics.SURFACE_LUMA_MAX)
            .copy(alpha = KyantMaterialSpec.forRole(GlassMaterialRole.Overlay, p.glassClarity).tintAlpha) else Color.Transparent
        MaterialTheme(typography = compactType) {
            Surface(color = if (layer != null) Color.Transparent else background, shape = shape, tonalElevation = tonalElevation,
                modifier = modifier.widthIn(max = 560.dp).fillMaxWidth(0.96f)
                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.86f)
                    .then(if (layer != null) Modifier.clip(shape).kyantSceneLayer(p, layer)
                        .background(overlayPaint, shape) else Modifier)
                    .then(if (glass) Modifier.trainPilotGlassRim(p, shape, GlassMaterialRole.Overlay)
                        else Modifier.trainPilotSoftGlow(p.primary, shape, 4.dp)
                            .trainPilotFrame(TrainPilotFrames.page(p.sectionBorder, p.primary), shape))) {
                CompositionLocalProvider(LocalKyantScene provides scene, LocalKyantHeader provides null) {
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
    }
}

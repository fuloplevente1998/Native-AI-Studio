package com.example.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.repforge.app.ui.theme.trainPilotPressIndication

/** One track and one selection drop, like the five-item main navigation.
 * A pinned scrolling track may use the existing bounded protective chrome;
 * individual items never paint their own backgrounds or rims. */
@Composable
internal fun LiquidSegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    itemTag: (Int) -> String = { "${tag}_$it" },
    weights: List<Float> = List(labels.size) { 1f },
    protectScrollingContent: Boolean = false,
    scrollable: Boolean = false,
    enabled: Boolean = true,
    sharedTrack: GlassPillTrack? = null,
    label: @Composable RowScope.(Int, String, Boolean) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val selection = remember(palette) { palette.selectionColors() }
    val shape = RoundedCornerShape(50)
    val localTrack = remember { GlassPillTrack() }
    val track = sharedTrack ?: localTrack
    val scroll = if (scrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier
    Box(modifier.fillMaxWidth().then(scroll)
        .then(if (protectScrollingContent) Modifier.glassHeaderMaterial(palette, shape, GlassMaterialRole.Navigation)
            else Modifier.glassHeaderControl(palette, shape, GlassMaterialRole.Navigation))
        .padding(2.dp).testTag(tag)) {
        GlassSelectionPill(track, selectedIndex, shape, palette, selection.border)
        Row((if (scrollable) Modifier else Modifier.fillMaxWidth()).height(IntrinsicSize.Min).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            labels.forEachIndexed { index, text ->
                val selected = index == selectedIndex
                val source = remember { MutableInteractionSource() }
                val drag = if (enabled && selected && rememberGlassMotionEnabled()) track.drag.observe() else Modifier
                val color = if (!enabled) palette.textSecondary.copy(alpha = .5f) else if (selected) selection.foreground else palette.textPrimary
                val itemWidth = if (scrollable) Modifier.widthIn(min = 76.dp) else Modifier.weight(weights[index])
                Box(itemWidth.fillMaxHeight().heightIn(min = 48.dp)
                    .glassPillSlot(track, index).clip(shape).then(drag)
                    .trainPilotPressIndication(shape, source)
                    .selectable(selected, enabled = enabled, interactionSource = source, indication = null,
                        role = Role.Tab, onClick = { onSelect(index) }).testTag(itemTag(index)),
                    contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalContentColor provides color,
                        LocalTextStyle provides MaterialTheme.typography.labelLarge.copy(
                            color = color, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically) { label(index, text, selected) }
                    }
                }
            }
        }
    }
}

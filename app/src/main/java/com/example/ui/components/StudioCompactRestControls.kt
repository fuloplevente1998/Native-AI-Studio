package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette

/** Fit actual measured labels; narrow/large-font controls wrap without squeezing the timer. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StudioCompactRestControls(
    language: AppLanguage,
    remainingSec: Int,
    running: Boolean,
    enabled: Boolean,
    onAdjust: (Int) -> Unit,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    showSkip: Boolean = true
) {
    val palette = LocalTrainPilotPalette.current
    fun word(hu: String, en: String, de: String, ro: String) = when (language) {
        AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro; else -> hu
    }
    val rest = word("Pihenő", "Rest", "Pause", "Pauză")
    val toggle = if (running) word("Szünet", "Pause", "Pause", "Pauză") else word("Indít", "Start", "Start", "Start")
    val skip = word("Kihagy", "Skip", "Überspr.", "Omite")
    val expand = word("Nagy pihenő mód", "Expanded rest timer", "Großer Pausenmodus", "Mod pauză extins")
    Layout(modifier = modifier.fillMaxWidth().testTag("compact_rest_controls"), content = {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.testTag("compact_rest_time")) {
            Icon(Icons.Default.Timer, null, tint = palette.primary, modifier = Modifier.size(15.dp))
            Text("$rest: %02d:%02d".format(remainingSec.coerceAtLeast(0) / 60, remainingSec.coerceAtLeast(0) % 60),
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.ExtraBold, color = palette.textPrimary,
                maxLines = 1, softWrap = false, modifier = Modifier.width(IntrinsicSize.Max))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.testTag("compact_rest_actions")) {
            @Composable fun action(label: String, tag: String, onClick: () -> Unit, highlighted: Boolean = false) {
                Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(8.dp),
                    color = if (highlighted) { if (running) palette.primary else palette.success } else palette.surfaceElevated,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag(tag)) {
                    Box(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.labelSmall,
                            color = (if (highlighted) palette.onPrimary else palette.textPrimary).copy(alpha = if (enabled) 1f else .38f),
                            fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                            modifier = Modifier.width(IntrinsicSize.Max))
                    }
                }
            }
            action("−15", "compact_rest_minus", { onAdjust(-15) })
            action(toggle, "compact_rest_toggle", onToggle, highlighted = true)
            action("+15", "compact_rest_plus", { onAdjust(15) })
            if (showSkip) action(skip, "compact_rest_skip", onSkip)
            IconButton(onClick = onExpand, modifier = Modifier.size(48.dp).testTag("compact_rest_expand")) {
                Icon(Icons.Default.ExpandLess, expand, tint = palette.primary)
            }
        }
    }) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
        val title = measurables[0].measure(loose)
        val actions = measurables[1].measure(loose)
        val gap = 6.dp.roundToPx()
        val inline = title.width + gap + actions.width <= constraints.maxWidth
        val height = if (inline) maxOf(title.height, actions.height) else title.height + gap + actions.height
        layout(constraints.maxWidth, constraints.constrainHeight(height)) {
            if (inline) {
                title.placeRelative(0, (height - title.height) / 2)
                actions.placeRelative(constraints.maxWidth - actions.width, (height - actions.height) / 2)
            } else {
                title.placeRelative(0, 0)
                actions.placeRelative(constraints.maxWidth - actions.width, title.height + gap)
            }
        }
    }
}

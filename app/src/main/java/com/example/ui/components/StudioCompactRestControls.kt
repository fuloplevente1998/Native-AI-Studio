package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette

/** A single 48dp row. Optional adjustments live in the expanded timer when space is tight. */
@Composable
internal fun StudioCompactRestControls(
    language: AppLanguage, remainingSec: Int, running: Boolean, enabled: Boolean,
    onAdjust: (Int) -> Unit, onToggle: () -> Unit, onSkip: () -> Unit, onExpand: () -> Unit,
    modifier: Modifier = Modifier, showSkip: Boolean = true
) {
    val p = LocalTrainPilotPalette.current
    fun word(hu: String, en: String, de: String, ro: String) = when (language) {
        AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro; else -> hu
    }
    val clock = "%02d:%02d".format(remainingSec.coerceAtLeast(0) / 60, remainingSec.coerceAtLeast(0) % 60)
    val label = word("Pihenő", "Rest", "Pause", "Pauză") + ": " + clock
    val toggle = if (running) word("Szünet", "Pause", "Pause", "Pauză") else word("Indít", "Start", "Start", "Start")
    val skip = word("Kihagy", "Skip", "Überspringen", "Omite")
    val expand = word("Nagy pihenő mód", "Expanded rest timer", "Großer Pausenmodus", "Mod pauză extins")
    val style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum")
    val measure = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth().testTag("compact_rest_controls")) {
        fun titleWidth(text: String) = with(density) {
            measure.measure(text, style, maxLines = 1, softWrap = false).size.width.toDp()
        } + 19.dp
        // Reserve exact 48dp targets and 3dp gaps before deciding which optional controls fit.
        fun controlsWidth(count: Int) = (48 * count + 3 * (count - 1)).dp + 6.dp
        val adjustments = maxWidth >= titleWidth(clock) + controlsWidth(if (showSkip) 5 else 4)
        val canSkip = showSkip && maxWidth >= titleWidth(clock) + controlsWidth(if (adjustments) 5 else 3)
        val count = 2 + (if (adjustments) 2 else 0) + (if (canSkip) 1 else 0)
        val text = if (maxWidth >= titleWidth(label) + controlsWidth(count)) label else clock
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.weight(1f).testTag("compact_rest_time").semantics(mergeDescendants = true) {
                    contentDescription = label
                }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Timer, null, tint = p.primary, modifier = Modifier.size(15.dp))
                Text(text, style = style, color = p.textPrimary, maxLines = 1, softWrap = false,
                    modifier = Modifier.width(IntrinsicSize.Max))
            }
            Row(Modifier.testTag("compact_rest_actions"), horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically) {
                @Composable fun action(tag: String, description: String, highlighted: Boolean = false,
                    click: () -> Unit, content: @Composable () -> Unit) {
                    Surface(onClick = click, enabled = enabled, shape = RoundedCornerShape(8.dp),
                        color = if (highlighted) { if (running) p.primary else p.success } else p.surfaceElevated,
                        modifier = Modifier.size(48.dp).testTag("compact_rest_" + tag)
                            .semantics { contentDescription = description }) {
                        Box(contentAlignment = Alignment.Center) { content() }
                    }
                }
                if (adjustments) action("minus", "−15 s", click = { onAdjust(-15) }) {
                    Text("−15", color = p.textPrimary, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(IntrinsicSize.Max), maxLines = 1, softWrap = false)
                }
                action("toggle", toggle, highlighted = true, click = onToggle) {
                    Icon(if (running) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = p.onPrimary)
                }
                if (adjustments) action("plus", "+15 s", click = { onAdjust(15) }) {
                    Text("+15", color = p.textPrimary, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(IntrinsicSize.Max), maxLines = 1, softWrap = false)
                }
                if (canSkip) action("skip", skip, click = onSkip) {
                    Icon(Icons.Default.SkipNext, null, tint = p.textPrimary)
                }
                IconButton(onClick = onExpand, modifier = Modifier.size(48.dp).testTag("compact_rest_expand")) {
                    Icon(Icons.Default.ExpandLess, expand, tint = p.primary)
                }
            }
        }
    }
}

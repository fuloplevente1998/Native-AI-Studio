package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StudioProgressWindow
import com.example.model.volumeAxisMaximum
import com.example.ui.theme.LocalTrainPilotPalette

/** Same measured buckets in both sizes. The old app opens a scrollable chart at its right edge. */
@Composable
internal fun StudioVolumeChart(bars: List<Pair<String, Int>>, period: StudioProgressWindow,
    selected: Int, expanded: Boolean, onSelect: (Int) -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val scroll = rememberScrollState()
    val scrollable = period.days == 90
    val mode = if (expanded) "expanded" else "compact"
    val height = if (expanded) 165.dp else 96.dp
    val labelSize = if (expanded) 10.sp else 9.sp
    val labelHeight = with(LocalDensity.current) { labelSize.toDp() } * 1.4f + 6.dp
    val plotHeight = (height - labelHeight).coerceAtLeast(20.dp)
    val axisMax = volumeAxisMaximum(bars.maxOfOrNull { it.second } ?: 0)
    LaunchedEffect(period, scroll.maxValue) {
        if (scrollable) scroll.scrollTo(scroll.maxValue)
    }
    Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(Modifier.width(if (expanded) 42.dp else 30.dp).height(plotHeight),
            verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
            (4 downTo 0).forEach { tick ->
                Text((axisMax * tick / 4).toString(), color = palette.textSecondary,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = labelSize), maxLines = 1)
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val contentWidth = if (scrollable) maxOf(maxWidth, (bars.size * if (expanded) 44 else 32).dp) else maxWidth
            Box(Modifier.fillMaxSize().horizontalScroll(scroll, enabled = scrollable)
                .testTag("progress_volume_scroll_$mode")) {
                Box(Modifier.width(contentWidth).height(height)) {
                    Column(Modifier.fillMaxWidth().height(plotHeight), verticalArrangement = Arrangement.SpaceBetween) {
                        repeat(5) { HorizontalDivider(color = palette.cardBorder.copy(alpha = 0.45f), thickness = 0.5.dp) }
                    }
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom) {
                        bars.forEachIndexed { index, (date, kg) ->
                            val ratio = (kg.toFloat() / axisMax.toFloat()).coerceIn(0f, 1f)
                            Column(Modifier.weight(1f).fillMaxHeight().clickable { onSelect(index) }
                                .semantics { contentDescription = "$date • $kg kg" },
                                verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.widthIn(max = if (expanded) 48.dp else 32.dp).fillMaxWidth()
                                    .height(plotHeight * ratio)
                                    .testTag("progress_${mode}_bar_$index")
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(Brush.verticalGradient(listOf(
                                        palette.primaryBright.copy(alpha = if (selected == index) 1f else 0.88f),
                                        palette.primary.copy(alpha = if (selected == index) 1f else 0.82f)))))
                                Box(Modifier.height(labelHeight), contentAlignment = Alignment.Center) {
                                    Text(date.substring(3, 5).trimStart('0'), color = palette.textSecondary,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = labelSize), maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

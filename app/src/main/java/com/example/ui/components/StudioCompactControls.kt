package com.example.ui.components

import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassTile
import com.example.ui.theme.glassCardFill
import com.repforge.app.ui.theme.trainPilotClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.ExerciseSetItem
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.TrainPilotTextButton
import kotlin.math.floor

internal data class StudioCompactAction(val label: String, val enabled: Boolean = true, val onClick: () -> Unit)

/** Text actions get horizontal space first, then another row; never a narrow square. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StudioCompactActionRow(actions: List<StudioCompactAction>, modifier: Modifier = Modifier) {
    val palette = LocalTrainPilotPalette.current
    val style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val minimum = with(density) {
        ((actions.maxOfOrNull { measurer.measure(AnnotatedString(it.label), style, softWrap = false).size.width } ?: 0).toDp() + 24.dp)
            .coerceAtLeast(72.dp)
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val count = floor((maxWidth.value + 8f) / (minimum.value + 8f)).toInt().coerceIn(1, actions.size.coerceAtLeast(1))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp), maxItemsInEachRow = count) {
            actions.forEach { action ->
                TrainPilotTextButton(onClick = action.onClick, enabled = action.enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp), shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.textButtonColors(containerColor = palette.surfaceElevated,
                        contentColor = palette.textPrimary), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(action.label, style = style, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/** Existing button callbacks stay intact; large fonts get full-width actions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StudioAdaptiveControlRow(minimumWidth: Dp = 132.dp, modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit) {
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= minimumWidth * (2f * scale) + 8.dp) 2 else 1
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = columns, content = content)
    }
}

/** Short visible side labels, full accessible labels, identical value-field heights. */
@Composable
internal fun StudioBilateralSetControls(item: ExerciseSetItem, selected: Boolean, language: AppLanguage,
    modifier: Modifier = Modifier, onEdit: () -> Unit, onRir: () -> Unit, onToggle: () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    fun word(hu: String, en: String, de: String, ro: String) = when (language) {
        AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
    }
    val unit = word("mp", "s", "Sek.", "sec")
    val number: @Composable () -> Unit = {
        Text("${item.setNumber}.", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            color = if (selected) palette.primary else palette.textPrimary)
    }
    val rir: @Composable () -> Unit = {
        Surface(shape = RoundedCornerShape(8.dp), color = palette.glassCardFill(palette.surfaceCard),
            modifier = Modifier.trainPilotGlassMaterial(palette, RoundedCornerShape(8.dp)).heightIn(min = 48.dp).trainPilotClickable(RoundedCornerShape(8.dp), onClick = onRir).testTag("bilateral_set_${item.setNumber}_rir")) {
            Box(Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                Text(if (item.actualRirProvided) "RIR ${item.rir}" else "RIR —",
                    style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1, softWrap = false)
            }
        }
    }
    val toggle: @Composable () -> Unit = {
        Surface(shape = RoundedCornerShape(10.dp), color = if (item.completed) palette.success else palette.surfaceCard,
            border = com.repforge.app.ui.theme.trainPilotFrameStroke(BorderStroke(1.dp, if (item.completed) palette.success else palette.cardBorder)),
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                .toggleable(value = item.completed, role = Role.Checkbox, onValueChange = { onToggle() }).testTag("bilateral_set_${item.setNumber}_complete")) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, word("Sorozat kész", "Set complete", "Satz fertig", "Serie completă"),
                    tint = if (item.completed) palette.onPrimary else palette.textSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
    val fields: @Composable RowScope.() -> Unit = {
        for (left in listOf(true, false)) {
            val recorded = item.completed || if (left) item.leftRecorded else item.rightRecorded
            val seconds = if (left) item.leftSec else item.rightSec
            val title = if (left) word("Bal", "Left", "Links", "Stânga") else word("Jobb", "Right", "Rechts", "Dreapta")
            val full = if (left) word("Bal oldal", "Left side", "Linke Seite", "Partea stângă")
                else word("Jobb oldal", "Right side", "Rechte Seite", "Partea dreaptă")
            val shape = RoundedCornerShape(10.dp)
            Surface(shape = shape, color = if (recorded) palette.success.copy(alpha = .16f) else palette.surfaceCard,
                border = com.repforge.app.ui.theme.trainPilotFrameStroke(BorderStroke(1.dp, if (recorded) palette.success else palette.cardBorder)),
                modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 56.dp).trainPilotClickable(shape, onClick = onEdit)
                    .semantics { contentDescription = "$full: $seconds $unit" }
                    .testTag("bilateral_set_${item.setNumber}_${if (left) "left" else "right"}_value")) {
                Column(Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                        color = if (recorded) palette.success else palette.textSecondary, maxLines = 1, softWrap = false)
                    Text("$seconds $unit", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        color = palette.textPrimary, maxLines = 1, softWrap = false)
                }
            }
        }
    }
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        if (maxWidth < 280.dp || LocalDensity.current.fontScale > 1.15f) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { number() }; rir(); toggle()
                }
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp), content = fields)
            }
        } else {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) { number() }
                Row(Modifier.weight(1f).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(6.dp), content = fields)
                rir(); toggle()
            }
        }
    }
}

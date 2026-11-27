package com.example.ui.screens

import com.example.ui.components.TrainPilotMenuItem

import com.example.ui.theme.KyantGlass
import com.example.ui.theme.KyantWindowOverlay
import com.example.ui.theme.LocalGlassOpticsEnabled
import com.example.ui.theme.LocalGlassOverlay
import com.example.ui.theme.LocalGlassPowerSave
import com.example.ui.theme.glassOverlayFill
import androidx.compose.runtime.CompositionLocalProvider
import com.example.ui.theme.liquidDropMaterial

import com.example.ui.theme.glassStateTile
import com.example.ui.theme.glassTile
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.TrainPilotPalette
import com.example.ui.theme.glassCardFill
import com.example.ui.theme.programDaySelectionAccent
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.trainPilotGlassRim
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.RowScope
import com.repforge.app.ui.theme.TrainPilotGlassPrimaryButton
import com.repforge.app.ui.theme.trainPilotFrame

import com.repforge.app.ui.theme.trainPilotClickable

import com.repforge.app.ui.theme.TrainPilotFrames

import com.repforge.app.ui.theme.trainPilotBorder

import com.example.ui.components.trainPilotMenuSelection
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette

internal val calendarDayShape = RoundedCornerShape(10.dp)
internal val calendarDayColumnGap = 4.dp
internal val calendarDayRowGap = 2.dp

/** The full calendar can use a lens; range sheets use the same shape and lightweight edge. */
internal data class CalendarDayAppearance(
    val fill: Color,
    val selectedLens: Boolean,
    val lightRim: Boolean,
    val underlineToday: Boolean,
    val strongRim: Boolean = false
)

internal fun calendarDayAppearance(
    palette: TrainPilotPalette,
    selected: Boolean,
    today: Boolean,
    planned: Boolean,
    rangePicker: Boolean = false,
    rangeInterior: Boolean = false
): CalendarDayAppearance {
    val legacy = when {
        rangePicker && selected -> palette.primary
        rangeInterior -> palette.primary.copy(alpha = .28f)
        planned -> palette.primary.copy(alpha = .12f)
        else -> palette.surface
    }
    if (!palette.isLiquidGlass) return CalendarDayAppearance(legacy, false, false, rangePicker && today)
    val lens = selected && !rangePicker
    return CalendarDayAppearance(
        fill = when {
            lens -> Color.Transparent
            selected -> palette.glassStateTile(programDaySelectionAccent, legacy, strong = true)
            rangeInterior -> palette.glassStateTile(programDaySelectionAccent, legacy)
            planned -> palette.glassStateTile(palette.primary, legacy)
            else -> palette.glassTile(legacy, elevated = today)
        },
        selectedLens = lens,
        lightRim = !lens,
        underlineToday = today,
        strongRim = selected
    )
}

@Composable
internal fun Modifier.calendarDayFrame(
    palette: TrainPilotPalette,
    appearance: CalendarDayAppearance,
    selected: Boolean,
    today: Boolean
): Modifier {
    val material = when {
        appearance.selectedLens -> Modifier.trainPilotGlassMaterial(palette, calendarDayShape, GlassMaterialRole.Selection)
        appearance.lightRim -> Modifier.trainPilotGlassRim(palette, calendarDayShape,
            if (appearance.strongRim) GlassMaterialRole.Selection else GlassMaterialRole.Tile)
        else -> Modifier
    }
    val highlightToday = palette.isLiquidGlass && today
    return then(material).trainPilotBorder(
        width = if (selected) 1.8.dp else if (highlightToday) 1.4.dp else 1.dp,
        color = if (selected || highlightToday) palette.primary else palette.cardBorder,
        shape = calendarDayShape
    )
}

/** Only enabled glass actions replace the filled paint. Material semantics and disabled colors stay native. */
@Composable
internal fun CalendarPrimaryGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape,
    content: @Composable RowScope.() -> Unit,
) {
    TrainPilotGlassPrimaryButton(onClick = onClick, modifier = modifier, enabled = enabled,
        shape = shape, content = content)
}

@Composable
internal fun CalendarActionChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    negative: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (negative) com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeBackground else color.copy(alpha = 0.14f),
        modifier = modifier
            .heightIn(min = 48.dp)
            .trainPilotFrame(1.dp, if (negative) com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeOutline.copy(alpha = 0.7f) else color.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .trainPilotClickable(RoundedCornerShape(10.dp)) { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (negative) com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeForeground else color,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun PlannerFieldTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    compact: Boolean = false,
    showChevron: Boolean = false,
    showCalendarIcon: Boolean = false,
    dropdownOptions: List<String>? = null,
    onSelectOption: ((String) -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (!compact) Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = palette.textSecondary
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.surfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidDropMaterial(palette, RoundedCornerShape(14.dp), GlassMaterialRole.Input)
                    .trainPilotBorder(
                        1.dp,
                        if (expanded) palette.primary else palette.cardBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .trainPilotClickable(RoundedCornerShape(14.dp)) {
                        if (!dropdownOptions.isNullOrEmpty() && onSelectOption != null) {
                            expanded = !expanded
                        } else {
                            onClick()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(horizontal = if (compact) 8.dp else 14.dp, vertical = if (compact) 2.dp else 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        if (compact) Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textSecondary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = value,
                            style = (if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge)
                                .copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (showCalendarIcon) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "📅", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (showChevron || !dropdownOptions.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!dropdownOptions.isNullOrEmpty() && onSelectOption != null) {
                com.example.ui.components.TrainPilotDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = palette.surfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    border = TrainPilotFrames.stroke(1.dp, palette.primary.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .heightIn(max = 260.dp)
                ) {
                    dropdownOptions.forEach { opt ->
                        val isSelected = opt == value
                        TrainPilotMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) palette.primary else palette.textPrimary
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "✓",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.primary
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onSelectOption(opt)
                                expanded = false
                            },
                            modifier = Modifier.trainPilotMenuSelection(isSelected, palette.primary.copy(alpha = 0.16f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CalendarReplanSurface(content: @Composable () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val shape = RoundedCornerShape(24.dp)
    val panel: @Composable (Color) -> Unit = { fill ->
        Surface(
            shape = shape,
            color = fill,
            modifier = Modifier.fillMaxWidth().testTag("calendar_replan_dialog")
                .trainPilotBorder(1.dp, palette.cardBorder, shape),
            content = content
        )
    }
    Box(Modifier.padding(horizontal = 12.dp, vertical = 20.dp)) {
        if (KyantGlass.active(palette) && LocalGlassOpticsEnabled.current && !LocalGlassPowerSave.current) {
            KyantWindowOverlay(palette, shape, staticSheet = true) { panel(Color.Transparent) }
        } else {
            CompositionLocalProvider(LocalGlassOverlay provides palette.isLiquidGlass) {
                panel(palette.glassOverlayFill(palette.surfaceCard))
            }
        }
    }
}

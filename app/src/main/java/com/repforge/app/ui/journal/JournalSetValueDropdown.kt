package com.repforge.app.ui.journal

import com.example.ui.components.TrainPilotIconButton

import com.example.ui.components.TrainPilotMenuItem

import com.example.ui.theme.*
import androidx.compose.ui.graphics.Color

import com.example.ui.components.TrainPilotDropdownMenu as DropdownMenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotFrames

/** Presets are a convenience: custom input remains a string until the editor validates a save. */
@Composable
internal fun JournalSetValueDropdown(value: String, label: String, tag: String, enabled: Boolean,
    choices: List<String>, customLabel: String, onChange: (String) -> Unit) {
    val colors = LocalTrainPilotColors.current
    val palette = LocalTrainPilotPalette.current
    val shape = if (palette.isLiquidGlass) RoundedCornerShape(50) else RoundedCornerShape(8.dp)
    val fill = if (palette.isLiquidGlass) Color.Transparent else colors.card
    val material = if (palette.isLiquidGlass) Modifier.liquidDropMaterial(palette, shape, GlassMaterialRole.Input) else Modifier
    var expanded by remember(tag, enabled) { mutableStateOf(false) }
    var custom by remember(tag, enabled) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Box {
            if (custom) {
                Surface(shape = shape, color = fill,
                    border = com.repforge.app.ui.theme.trainPilotFrameStroke(TrainPilotFrames.control(colors.line)), modifier = Modifier.fillMaxWidth().then(material)) {
                    Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.muted, maxLines = 1)
                            BasicTextField(value, onChange, enabled = enabled, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.text),
                                modifier = Modifier.fillMaxWidth().testTag("${tag}_custom_input"))
                        }
                        TrainPilotIconButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.ExpandMore, label, tint = colors.accentText)
                        }
                    }
                }
            } else {
                Surface(onClick = { expanded = true }, enabled = enabled, shape = shape,
                    color = fill, border = com.repforge.app.ui.theme.trainPilotFrameStroke(TrainPilotFrames.control(colors.line)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).then(material).testTag(tag)) {
                    Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.muted, maxLines = 1)
                            Text(value.ifEmpty { "–" }, style = MaterialTheme.typography.bodyMedium,
                                color = colors.text, maxLines = 1)
                        }
                        Icon(Icons.Default.ExpandMore, null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                    }
                }
            }
            DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 280.dp).then(if (LocalTrainPilotPalette.current.isLiquidGlass) Modifier else Modifier.background(colors.card))) {
                TrainPilotMenuItem(text = { Text(customLabel, color = colors.text) },
                    onClick = { custom = true; expanded = false }, modifier = Modifier.testTag("${tag}_custom"))
                choices.forEach { choice ->
                    TrainPilotMenuItem(text = { Text(choice.ifEmpty { "–" }, color = colors.text) },
                        onClick = { custom = false; onChange(choice); expanded = false },
                        modifier = Modifier.testTag("${tag}_option_${choice.ifEmpty { "empty" }}"))
                }

            }
        }
    }
}

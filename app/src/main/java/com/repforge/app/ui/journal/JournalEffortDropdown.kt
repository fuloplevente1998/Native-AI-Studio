package com.repforge.app.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotFrames

@Composable
internal fun JournalEffortDropdown(selected: String?, choices: List<Pair<String, String>>, prompt: String,
    enabled: Boolean, onSelect: (String) -> Unit) {
    val colors = LocalTrainPilotColors.current
    var expanded by remember { mutableStateOf(false) }
    val answer = choices.firstOrNull { it.first == selected || (selected == "right" && it.first == "good") }
    Box(Modifier.fillMaxWidth()) {
        Surface(onClick = { expanded = true }, enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            color = if (answer != null) colors.accent.copy(alpha = 0.16f) else colors.card2,
            border = TrainPilotFrames.stroke(0.75.dp, if (answer != null) colors.accent else colors.line),
            modifier = Modifier.fillMaxWidth().testTag("journal_editor_feedback_selector")) {
            Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (answer != null) Icon(Icons.Default.Check, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                Text(answer?.second ?: prompt, Modifier.weight(1f), fontWeight = FontWeight.SemiBold,
                    color = if (answer != null) colors.accent else colors.text)
                Icon(Icons.Default.ExpandMore, prompt, tint = colors.accent)
            }
        }
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false },
            modifier = Modifier.background(colors.card)) {
            choices.forEach { (key, label) ->
                val chosen = answer?.first == key
                DropdownMenuItem(text = { Text(label, fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
                    color = if (chosen) colors.accent else colors.text) },
                    trailingIcon = { if (chosen) Icon(Icons.Default.Check, null, tint = colors.accent) },
                    onClick = { onSelect(key); expanded = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                        .background(if (chosen) colors.accent.copy(alpha = 0.16f) else colors.card)
                        .testTag("journal_editor_feedback_$key"))
            }
        }
    }
}

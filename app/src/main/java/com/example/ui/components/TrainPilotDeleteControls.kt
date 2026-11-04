package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette

@Composable
internal fun TrainPilotDeleteButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier, label: String? = null) {
    val lang = com.example.localization.LocalTrainPilotStrings.current.language
    val resolvedLabel = label ?: when (lang) {
        com.example.model.AppLanguage.HU -> "Edzés törlése"
        com.example.model.AppLanguage.EN -> "Delete workout"
        com.example.model.AppLanguage.DE -> "Training löschen"
        com.example.model.AppLanguage.RO -> "Șterge antrenamentul"
    }
    val foreground = if (LocalTrainPilotPalette.current.isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled = enabled,
        shape = RoundedCornerShape(11.dp), border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.65f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.16f), contentColor = foreground)) {
        Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
        Text(" $resolvedLabel")
    }
}

@Composable
internal fun TrainPilotDeleteIcon(
    onClick: () -> Unit,
    enabled: Boolean,
    tag: String,
    compactVisual: Boolean = false
) {
    val lang = com.example.localization.LocalTrainPilotStrings.current.language
    val desc = when (lang) {
        com.example.model.AppLanguage.HU -> "Edzés törlése"
        com.example.model.AppLanguage.EN -> "Delete workout"
        com.example.model.AppLanguage.DE -> "Training löschen"
        com.example.model.AppLanguage.RO -> "Șterge antrenamentul"
    }
    val foreground = if (LocalTrainPilotPalette.current.isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
    val visualSize = if (compactVisual) 41.dp else 48.dp // ~15% smaller; touch target stays 48dp.
    val shape = RoundedCornerShape(if (compactVisual) 9.dp else 11.dp)
    Box(Modifier.size(48.dp).testTag(tag)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Box(Modifier.size(visualSize).clip(shape)
            .background(Color(0xFFEF4444).copy(alpha = 0.16f))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.65f), shape),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Delete, contentDescription = desc, tint = foreground,
                modifier = Modifier.size(if (compactVisual) 20.dp else 24.dp))
        }
    }
}

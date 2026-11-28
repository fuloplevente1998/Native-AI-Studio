package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.TrainPilotPalette

enum class OperationFeedbackKind {
    RUNNING, SUCCESS, PARTIAL, FAILED, CANCELLED, INFORMATION
}

internal fun TrainPilotPalette.operationFeedbackColor(kind: OperationFeedbackKind) = when (kind) {
    OperationFeedbackKind.RUNNING -> primary
    OperationFeedbackKind.SUCCESS -> success
    OperationFeedbackKind.PARTIAL, OperationFeedbackKind.CANCELLED -> warning
    OperationFeedbackKind.FAILED -> danger
    OperationFeedbackKind.INFORMATION -> textSecondary
}

@Composable
internal fun OperationFeedbackMark(kind: OperationFeedbackKind, modifier: Modifier = Modifier) {
    val tint = LocalTrainPilotPalette.current.operationFeedbackColor(kind)
    val size = modifier.size(17.dp).testTag("operation_feedback_${kind.name.lowercase()}")
    when (kind) {
        OperationFeedbackKind.RUNNING -> CircularProgressIndicator(size, color = tint, strokeWidth = 2.dp)
        else -> Icon(when (kind) {
            OperationFeedbackKind.SUCCESS -> Icons.Default.CheckCircle
            OperationFeedbackKind.FAILED -> Icons.Default.ErrorOutline
            OperationFeedbackKind.PARTIAL, OperationFeedbackKind.CANCELLED -> Icons.Default.Warning
            else -> Icons.Default.Info
        }, contentDescription = null, modifier = size, tint = tint)
    }
}

@Composable
internal fun OperationFeedbackLine(
    kind: OperationFeedbackKind,
    message: String,
    modifier: Modifier = Modifier,
    color: Color = LocalTrainPilotPalette.current.operationFeedbackColor(kind)
) {
    Row(modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OperationFeedbackMark(kind)
        Text(message, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

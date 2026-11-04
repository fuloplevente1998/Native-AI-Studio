package com.repforge.app.ui.progress

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.trainPilotBorder

/** Shared daily-activity and cardio metric presentation. */
@Composable
internal fun StatisticsMetricTile(label: String, value: String, modifier: Modifier = Modifier,
    valueTag: String = "") {
    val colors = LocalTrainPilotColors.current
    Surface(shape = MaterialTheme.shapes.large, color = colors.card2,
        modifier = modifier.trainPilotBorder(1.dp, colors.line, MaterialTheme.shapes.large)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = colors.muted, style = MaterialTheme.typography.bodySmall)
            Text(value, color = colors.text, style = MaterialTheme.typography.headlineSmall,
                modifier = if (valueTag.isEmpty()) Modifier else Modifier.testTag(valueTag))
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repforge.app.ui.theme.trainPilotSoftGlow
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.TrainPilotFrames

/** Floating calendar details keep the month fixed; long content scrolls inside the overlay. */
@Composable
internal fun CompactCalendarDialog(
    title: String,
    tag: String,
    onClose: () -> Unit,
    asSheet: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    if (asSheet) {
        // #269: day actions open from the bottom; the month stays visible and fixed underneath.
        CompactCalendarSheet(title, tag, onClose, content)
        return
    }
    val shape = RoundedCornerShape(20.dp)
    val maximumHeight = LocalConfiguration.current.screenHeightDp.dp * 0.82f
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = palette.surfaceCard, shape = shape,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(0.96f).heightIn(max = maximumHeight)
                .trainPilotSoftGlow(palette.primary, shape)
                .border(TrainPilotFrames.page(palette.sectionBorder, palette.primary), shape).testTag(tag)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = palette.textPrimary, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    TrainPilotRedCloseButton(onClick = onClose, modifier = Modifier.testTag("${tag}_close"))
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CompactCalendarSheet(title: String, tag: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        modifier = Modifier.trainPilotSheetTopSafeArea().testTag(tag),
        containerColor = palette.surfaceCard,
        contentColor = palette.textPrimary
    ) {
        Column(
            Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
                .then(Modifier.navigationBarsPadding()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = palette.textPrimary, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                TrainPilotRedCloseButton(onClick = onClose, modifier = Modifier.testTag("${tag}_close"))
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
        }
    }
}

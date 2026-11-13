package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.TrainPilotOutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainPilotFullHeightFormSheet(
    title: String,
    onDismiss: () -> Unit,
    closeTestTag: String = "",
    content: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight().testTag("full_height_form_sheet"),
        sheetState = sheetState,
        containerColor = palette.surface,
        contentColor = palette.textPrimary,
        dragHandle = null
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight().imePadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = palette.textPrimary)
                TrainPilotRedCloseButton(onClick = onDismiss, testTag = closeTestTag)
            }
            Box(Modifier.weight(1f).fillMaxWidth().testTag("full_height_form_body").padding(horizontal = 16.dp)) {
                content()
            }
            Column(
                modifier = Modifier.fillMaxWidth().testTag("full_height_form_footer").padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                confirmButton()
                TrainPilotOutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.cancelBtn, color = palette.textPrimary)
                }
            }
        }
    }
}

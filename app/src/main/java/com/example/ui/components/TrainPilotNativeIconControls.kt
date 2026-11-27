package com.example.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.trainPilotPressIndication

/** Native toggle semantics remain intact; no extra capture or per-row lens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrainPilotIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (!LocalTrainPilotPalette.current.isLiquidGlass) {
        IconToggleButton(checked, onCheckedChange, modifier, enabled, content = content)
        return
    }
    val source = remember { MutableInteractionSource() }
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        IconToggleButton(checked, onCheckedChange,
            modifier.trainPilotPressIndication(CircleShape, source, enabled), enabled,
            interactionSource = source, content = content)
    }
}

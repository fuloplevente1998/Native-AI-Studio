package com.example.ui.components

import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotFrames

/** Palette-derived paint for existing outlined workout controls; no action or state ownership. */
internal object WorkoutControlAppearance {
    @Composable
    fun secondaryColors() = ButtonDefaults.outlinedButtonColors(
        containerColor = LocalTrainPilotColors.current.card2,
        contentColor = LocalTrainPilotColors.current.text)

    @Composable
    fun secondaryBorder(enabled: Boolean = true) = LocalTrainPilotColors.current.let {
        TrainPilotFrames.control(if (enabled) it.text else it.muted.copy(alpha = 0.38f))
    }

    @Composable
    fun discardColors() = com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeColors()

    @Composable
    fun discardBorder(enabled: Boolean = true) = com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeBorder(enabled)
}

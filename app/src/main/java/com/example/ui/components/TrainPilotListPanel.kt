package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.glassCardFill
import com.example.ui.theme.trainPilotGlassMaterial
import com.repforge.app.ui.theme.trainPilotBorder

/** The Programs list material. Place it on the list viewport or on a bounded group of rows. */
@Composable
internal fun Modifier.trainPilotListPanel(): Modifier {
    val palette = LocalTrainPilotPalette.current
    val shape = RoundedCornerShape(24.dp)
    return trainPilotGlassMaterial(palette, shape)
        .background(palette.glassCardFill(palette.surface), shape)
        .trainPilotBorder(1.dp, palette.cardBorder, shape)
}

@Composable
internal fun TrainPilotListDivider() {
    val palette = LocalTrainPilotPalette.current
    HorizontalDivider(color = palette.cardBorder.copy(alpha = .40f))
}

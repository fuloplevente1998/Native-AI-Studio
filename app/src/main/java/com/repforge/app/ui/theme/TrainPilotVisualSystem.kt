package com.repforge.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Shared presentation constants, independent of data and selected palette. */
object TrainPilotDimensions {
    val Tight = 4.dp
    val Small = 8.dp
    val Gap = 12.dp
    val Page = 16.dp
    val Section = 24.dp
    val Control = 48.dp
    val PrimaryAction = 52.dp
    val Icon = 24.dp
    val CardRadius = 16.dp
    val HeroRadius = 20.dp
}

/**
 * Page-to-card and card-to-card distances only; never use these values for
 * button hit targets, typography, or padding inside an interactive card.
 * Shared by the Studio phone tabs and their detail sheets.
 */
object TrainPilotCardSpacing {
    val ScreenGutter = 12.dp
    val CardGap = 8.dp
    val CardRowGap = 8.dp
}

val TrainPilotShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(TrainPilotDimensions.CardRadius),
    large = RoundedCornerShape(TrainPilotDimensions.HeroRadius),
    extraLarge = RoundedCornerShape(24.dp)
)

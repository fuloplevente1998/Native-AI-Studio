package com.example.ui.components.mapcontrols

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.data.store.MapAppearance
import com.repforge.app.ui.theme.trainPilotBorder

/**
 * One opaque, compact, high-contrast map style control instead of two floating
 * labels that cover the route and Google Maps gestures/zoom buttons.
 * The caller persists the selected choice; this component does not touch GPS.
 */
@Composable
internal fun MapAppearanceSelector(
    appearance: MapAppearance,
    onSelect: (MapAppearance) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val currentLabel = when (appearance) {
        MapAppearance.FOLLOW_APP -> "App témája"
        MapAppearance.LIGHT -> "Világos térkép"
        MapAppearance.DARK -> "Sötét térkép"
    }
    Box(modifier) {
        Surface(
            shape = shape,
            color = palette.surfaceCard.copy(alpha = 0.97f),
            modifier = Modifier.trainPilotBorder(0.75.dp, palette.cardBorder, shape)
                .testTag("route_map_style_surface")
        ) {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.heightIn(min = 48.dp).testTag("route_map_style_toggle")
            ) {
                Text("Térkép: $currentLabel", style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(" ▾", color = palette.textPrimary)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                MapAppearance.FOLLOW_APP to "App témája",
                MapAppearance.LIGHT to "Világos térkép",
                MapAppearance.DARK to "Sötét térkép"
            ).forEach { (choice, label) ->
                DropdownMenuItem(
                    text = {
                        Text(label + if (appearance == choice) " ✓" else "",
                            color = palette.textPrimary)
                    },
                    onClick = {
                        expanded = false
                        onSelect(choice)
                    },
                    modifier = Modifier.testTag(when (choice) {
                        MapAppearance.FOLLOW_APP -> "route_map_follow_app"
                        MapAppearance.LIGHT -> "route_map_choose_light"
                        MapAppearance.DARK -> "route_map_choose_dark"
                    })
                )
            }
        }
    }
}

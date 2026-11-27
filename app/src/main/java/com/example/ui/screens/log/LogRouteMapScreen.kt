package com.example.ui.screens

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.ui.components.NativeWorkoutRouteMapBox
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.trainPilotScreenBackdrop
import com.example.ui.theme.glassTile
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * 3. EDZÉSÚTVONAL (Mockup 3/4 #3 - Margitsziget 1,62 km)
 */
@Composable
fun LogRouteMapScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val workout = uiState.loggedWorkouts.firstOrNull { it.id == uiState.selectedLogWorkoutId }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .trainPilotScreenBackdrop(palette)
            .padding(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp)
            .testTag("log_route_map_screen"),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.routeMapTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            com.example.ui.components.TrainPilotRedCloseButton(onClick = onBack, testTag = "close_route_map_button")
        }

        NativeWorkoutRouteMapBox(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            interactiveControls = true, routeKeys = workout?.routeKeys.orEmpty(), fitRecordedRoute = true
        )

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = palette.glassTile(palette.surfaceCard),
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotGlassRim(palette, RoundedCornerShape(22.dp))
                .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = null,
                            tint = palette.success,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = workout?.distanceKm?.takeIf { it > 0 }?.let { "%.2f km".format(it) } ?: "— km",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.success.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = workout?.paceLabel?.takeIf(String::isNotBlank) ?: "— /km",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.success,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Text(
                    text = if (workout?.routeKeys.isNullOrEmpty()) trL(uiState.language, "Ehhez az edzéshez nincs rögzített útvonal.", "No recorded route for this workout.", "Für dieses Training gibt es keine aufgezeichnete Route.", "Nu există traseu înregistrat pentru acest antrenament.") else trL(uiState.language, "Privát, rögzített GPS-útvonal", "Private recorded GPS route", "Private aufgezeichnete GPS-Route", "Traseu GPS privat înregistrat"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary
                )
                Text(
                    text = workout?.dateLabelHu ?: trL(uiState.language, "Nincs dátum", "No date", "Kein Datum", "Fără dată"),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                HorizontalDivider(color = palette.cardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(trL(uiState.language, "Hitelesített részidőadat nem érhető el", "Verified split data unavailable", "Bestätigte Zwischenzeiten nicht verfügbar", "Datele intermediare verificate nu sunt disponibile"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    Text("—", style = MaterialTheme.typography.labelSmall, color = palette.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

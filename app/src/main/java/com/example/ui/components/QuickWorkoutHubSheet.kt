package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotClickable

private fun qw(lang: AppLanguage, hu: String, en: String, de: String, ro: String) = when (lang) {
    AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
}

/**
 * Quick workout (2772): one bottom sheet with three tabs instead of separate pages.
 * Strength and Timed host the exercise picker 1:1 ([strengthContent], [timedContent]); Outdoor
 * picks the activity here and only then switches to the full live GPS screen ([onStartOutdoor]),
 * which owns location permission and the native tracking engine.
 */
@Composable
fun QuickWorkoutHubSheet(
    language: AppLanguage,
    tab: Int,
    onSelectTab: (Int) -> Unit,
    onDismiss: () -> Unit,
    onStartOutdoor: (activityName: String) -> Unit,
    strengthContent: @Composable () -> Unit,
    timedContent: @Composable () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val height = LocalConfiguration.current.screenHeightDp.dp * 0.86f
    TrainPilotScreenSheet(onDismiss = onDismiss, testTag = "quick_workout_sheet", fillHeight = true) {
        Column(Modifier.fillMaxWidth().height(height)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(qw(language, "Gyors edzés", "Quick workout", "Schnelltraining", "Antrenament rapid"),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = palette.textPrimary, modifier = Modifier.weight(1f))
                TrainPilotRedCloseButton(onClick = onDismiss, modifier = Modifier.testTag("quick_workout_close"))
            }
            // Segmented tabs: Strength · Timed · Outdoor.
            val tabs = listOf(
                qw(language, "💪 Erő", "💪 Strength", "💪 Kraft", "💪 Forță"),
                qw(language, "⏱ Időzített", "⏱ Timed", "⏱ Zeit", "⏱ Cronometrat"),
                qw(language, "🗺 Kültéri", "🗺 Outdoor", "🗺 Outdoor", "🗺 Exterior")
            )
            val segShape = RoundedCornerShape(14.dp)
            Surface(shape = segShape, color = palette.surfaceCard,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .trainPilotBorder(1.dp, palette.cardBorder, segShape)) {
                Row(Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    tabs.forEachIndexed { i, label ->
                        val selected = i == tab
                        val shape = RoundedCornerShape(11.dp)
                        Surface(shape = shape, color = if (selected) palette.primary else palette.surfaceCard,
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                                .trainPilotClickable(shape) { onSelectTab(i) }.testTag("quick_tab_$i")) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) palette.onPrimary else palette.textPrimary)
                            }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when (tab) {
                    0 -> strengthContent()
                    1 -> timedContent()
                    else -> QuickOutdoorTab(language, onStartOutdoor)
                }
            }
        }
    }
}

@Composable
private fun QuickOutdoorTab(language: AppLanguage, onStartOutdoor: (String) -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val activities = listOf(
        qw(language, "🏃 Futás", "🏃 Running", "🏃 Laufen", "🏃 Alergare") to "Kültéri futás",
        qw(language, "🚶 Séta", "🚶 Walking", "🚶 Gehen", "🚶 Mers") to "Kültéri séta",
        qw(language, "🚴 Kerékpár", "🚴 Cycling", "🚴 Radfahren", "🚴 Ciclism") to "Kerékpározás",
        qw(language, "🛼 Görkorcsolya", "🛼 Skating", "🛼 Inlineskaten", "🛼 Role") to "Görkorcsolyázás",
        qw(language, "🥾 Túrázás", "🥾 Hiking", "🥾 Wandern", "🥾 Drumeție") to "Kültéri túrázás",
        qw(language, "⚡ Egyéb", "⚡ Other", "⚡ Sonstiges", "⚡ Altele") to "Egyéb kültéri edzés"
    )
    var selected by rememberSaveable { mutableStateOf(activities.first().second) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        activities.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (label, name) ->
                    val on = name == selected
                    val shape = RoundedCornerShape(14.dp)
                    Surface(shape = shape, color = if (on) palette.primary else palette.surfaceCard,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                            .trainPilotBorder(1.dp, if (on) palette.primary else palette.cardBorder, shape)
                            .trainPilotClickable(shape) { selected = name }
                            .testTag("quick_outdoor_${name.lowercase()}")) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(label, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                                color = if (on) palette.onPrimary else palette.textPrimary, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
        com.repforge.app.ui.theme.TrainPilotButton(
            onClick = { onStartOutdoor(selected) },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("quick_outdoor_start")
        ) { Text(qw(language, "▶ Tovább a GPS-méréshez", "▶ Continue to GPS tracking", "▶ Weiter zur GPS-Messung", "▶ Continuă la GPS"), fontWeight = FontWeight.Bold) }
    }
}

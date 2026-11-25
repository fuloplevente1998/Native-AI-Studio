package com.example.ui.screens

import com.example.ui.theme.trainPilotGlassRim

import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassTile
import com.example.ui.theme.glassCardFill
import com.example.ui.theme.trainPilotScreenBackdrop
import androidx.compose.ui.semantics.contentDescription

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.components.TrainPilotSwitch as Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.LocalTrainPilotStrings
import com.example.localization.LocalizedStrings
import com.example.model.SubScreen
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * 5. INTERVALLUMOK BEÁLLÍTÁSA (Mockup 2/4 #5)
 */
@Composable
fun IntervalSettingsScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit,
    onSaveAndStartIntervals: (Int, Int, Int, Boolean, Boolean, Boolean) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var workSec by remember { mutableIntStateOf(uiState.intervalWorkSec) }
    var restSec by remember { mutableIntStateOf(uiState.intervalRestSec) }
    var rounds by remember { mutableIntStateOf(uiState.intervalRounds) }
    var sound by remember { mutableStateOf(uiState.soundAlertEnabled) }
    var vibration by remember { mutableStateOf(uiState.vibrationEnabled) }
    var autoStep by remember { mutableStateOf(uiState.autoStepIntervals) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            // 2811/G4: glass = the shared (static on this screen) page backdrop instead of an opaque panel.
            .trainPilotScreenBackdrop(palette)
            .testTag("interval_settings_screen"),
        contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        val lang = uiState.language
        val secWord = tr(lang, "mp", "s", "Sek.", "sec")
        item {
            SubScreenTopBar(
                title = tr(lang, "Intervallumok beállítása", "Interval Settings", "Intervalleinstellungen", "Setări intervale"),
                onBack = onBack,
                onOpenSettings = onOpenSettings,
                    rightExtraContent = { com.example.ui.components.StudioWorkoutHealthAction() }
            )
        }
        item {
            IntervalStepperCard(
                testTagPrefix = "studio_interval_work",
                label = tr(lang, "Munkaidő", "Work time", "Arbeitszeit", "Timp de lucru"),
                valueText = "$workSec $secWord",
                onMinus = { workSec = (workSec - 5).coerceAtLeast(10) },
                onPlus = { workSec = (workSec + 5).coerceAtMost(300) }
            )
        }
        item {
            IntervalStepperCard(
                testTagPrefix = "studio_interval_rest",
                label = tr(lang, "Pihenőidő", "Rest time", "Pausenzeit", "Timp de pauză"),
                valueText = "$restSec $secWord",
                onMinus = { restSec = (restSec - 5).coerceAtLeast(10) },
                onPlus = { restSec = (restSec + 5).coerceAtMost(300) }
            )
        }
        item {
            IntervalStepperCard(
                testTagPrefix = "studio_interval_rounds",
                label = tr(lang, "Ismétlések (körök)", "Repetitions (rounds)", "Wiederholungen (Runden)", "Repetări (runde)"),
                valueText = tr(lang, "$rounds kör", "$rounds rounds", "$rounds Runden", "$rounds runde"),
                onMinus = { rounds = (rounds - 1).coerceAtLeast(1) },
                onPlus = { rounds = (rounds + 1).coerceAtMost(20) }
            )
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.glassTile(palette.surfaceCard),
                modifier = Modifier.trainPilotGlassRim(palette, RoundedCornerShape(18.dp))
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tr(lang, "Hangjelzés", "Sound alert", "Tonhinweis", "Alertă sonoră"), color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Switch(checked = sound, onCheckedChange = { sound = it }, modifier = Modifier.testTag("studio_interval_sound"))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tr(lang, "Rezgés", "Vibration", "Vibration", "Vibrație"), color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Switch(checked = vibration, onCheckedChange = { vibration = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(tr(lang, "Automatikus továbblépés", "Automatic advance", "Automatisch fortfahren", "Avansare automată"),
                            color = palette.textPrimary, modifier = Modifier.weight(1f))
                        Switch(checked = autoStep, onCheckedChange = { autoStep = it }, modifier = Modifier.testTag("studio_interval_auto_advance"))
                    }
                }
            }
        }
        item {
            com.repforge.app.ui.theme.TrainPilotSaveButton(
                onClick = {
                    onSaveAndStartIntervals(workSec, restSec, rounds, sound, vibration, autoStep)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_interval_save_start")
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.startIntervalsAction, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun IntervalStepperCard(
    label: String,
    testTagPrefix: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = com.example.localization.LocalTrainPilotStrings.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.glassTile(palette.surfaceCard),
        modifier = Modifier.trainPilotGlassRim(palette, RoundedCornerShape(18.dp))
            .fillMaxWidth()
            .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Text(valueText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onMinus,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("${testTagPrefix}_minus")
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.glassTile(palette.surfaceElevated))
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = strings.decreaseValueDesc, tint = palette.textPrimary)
                }
                IconButton(
                    onClick = onPlus,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("${testTagPrefix}_plus")
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = strings.increaseValueDesc, tint = palette.onPrimary)
                }
            }
        }
    }
}

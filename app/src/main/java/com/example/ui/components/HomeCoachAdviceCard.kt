package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotSoftGlow
import kotlin.math.roundToInt

/** A larger presentation of the existing readiness score; no separate Coach calculation. */
@Composable
internal fun HomeCoachAdviceCard(state: TrainPilotUiState, strings: LocalizedStrings, onOpen: () -> Unit) {
    val p = LocalTrainPilotPalette.current
    val score = state.todayRecoveryReading?.coachScore?.takeIf { it.isFinite() && it in 0.0..100.0 }?.roundToInt()
        ?: state.coachReadinessScore.takeIf { it in 1..100 }
    val heading = when {
        score == null -> "Mai edzésjavaslat"
        score < 55 -> "Könnyített terhelés"
        score < 75 -> "Kissé visszafogott terhelés"
        else -> "Tartható a megszokott terhelés"
    }
    val summary = when {
        score == null -> "Még nincs elég friss adat. A Coach oldalon megnézheted az előzményeket és a részleteket."
        score < 55 -> "Ma könnyebb terhelés javasolt. Ne erőltesd a súlyemelést; nézd meg a gyakorlatonkénti korlátozásokat a Coach oldalon."
        score < 75 -> "Kezdj óvatosabban, alapos bemelegítéssel. Kövesd a Coach gyakorlatonkénti javaslatait; most ne erőltesd az emelést."
        else -> "Kezdj bemelegítéssel, és kövesd a Coach gyakorlatonkénti javaslatait. A súlyváltoztatást a részletes előnézetben ellenőrizheted."
    }
    val shape = RoundedCornerShape(20.dp)
    Surface(shape = shape, color = p.surface,
        modifier = Modifier.fillMaxWidth().trainPilotSoftGlow(p.primary, shape)
            .trainPilotBorder(1.dp, Brush.linearGradient(listOf(p.primary.copy(alpha = 0.7f), p.cardBorder)), shape)
            .clickable(onClick = onOpen).testTag("home_coach_advice_card")) {
        Column(Modifier.background(Brush.verticalGradient(listOf(p.surfaceElevated.copy(alpha = 0.65f), p.surface)))
            .padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(strings.coachSuggestionHeader, color = p.primary, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.primary)
            }
            Text(heading, color = p.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(summary, color = p.textSecondary, style = MaterialTheme.typography.bodySmall,
                maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("home_coach_summary"))
            Surface(color = p.surfaceElevated, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Mai készenlét", color = p.textSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(score?.let { "$it/100" } ?: "—", color = p.textPrimary,
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

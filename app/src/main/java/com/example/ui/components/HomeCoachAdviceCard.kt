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
    val lang = strings.language
    fun tr(hu: String, en: String, de: String, ro: String) = when (lang) {
        com.example.model.AppLanguage.HU -> hu
        com.example.model.AppLanguage.EN -> en
        com.example.model.AppLanguage.DE -> de
        com.example.model.AppLanguage.RO -> ro
    }
    val score = state.todayRecoveryReading?.coachScore?.takeIf { it.isFinite() && it in 0.0..100.0 }?.roundToInt()
        ?: state.coachReadinessScore.takeIf { it in 1..100 }
    val heading = when {
        score == null -> tr("Mai edzésjavaslat", "Today's Workout Advice", "Heutige Trainingsempfehlung", "Recomandarea de antrenament de azi")
        score < 55 -> tr("Könnyített terhelés", "Lighter Load", "Leichtere Belastung", "Sarcină ușoară")
        score < 75 -> tr("Kissé visszafogott terhelés", "Moderated Load", "Etwas reduzierte Belastung", "Sarcină moderată")
        else -> tr("Tartható a megszokott terhelés", "Normal Load Ready", "Normale Belastung möglich", "Sarcină normală recomandată")
    }
    val summary = when {
        score == null -> tr(
            "Még nincs elég friss adat. A Coach oldalon megnézheted az előzményeket és a részleteket.",
            "Not enough recent data yet. Open Coach to view history and detailed recommendations.",
            "Noch nicht genügend aktuelle Daten. Auf der Coach-Seite findest du Verlauf und Details.",
            "Încă nu există suficiente date recente. Pe pagina Coach poți vedea istoricul și detaliile."
        )
        score < 55 -> tr(
            "Ma könnyebb terhelés javasolt. Ne erőltesd a súlyemelést; nézd meg a gyakorlatonkénti korlátozásokat a Coach oldalon.",
            "A lighter session is advised today. Do not force weight increases; check per-exercise limits on the Coach page.",
            "Heute wird eine leichtere Belastung empfohlen. Erzwinge keine Steigerung; prüfe die Hinweise auf der Coach-Seite.",
            "Astăzi se recomandă o sarcină mai ușoară. Nu forța creșterea greutății; verifică limitele pe pagina Coach."
        )
        score < 75 -> tr(
            "Kezdj óvatosabban, alapos bemelegítéssel. Kövesd a Coach gyakorlatonkénti javaslatait; most ne erőltesd az emelést.",
            "Start cautiously with a thorough warm-up. Follow Coach per-exercise advice and avoid forcing progression today.",
            "Beginne vorsichtig mit gründlichem Aufwärmen. Folge den Übungsempfehlungen des Coachs.",
            "Începe prudent, cu o încălzire temeinică. Urmează sfaturile Coach pe exerciții; nu forța progresia acum."
        )
        else -> tr(
            "Kezdj bemelegítéssel, és kövesd a Coach gyakorlatonkénti javaslatait. A súlyváltoztatást a részletes előnézetben ellenőrizheted.",
            "Start with a warm-up and follow Coach per-exercise advice. You can review weight adjustments in the detailed preview.",
            "Beginne mit dem Aufwärmen und folge den Übungsempfehlungen des Coachs. Gewichtsänderungen sind in der Vorschau sichtbar.",
            "Începe cu încălzirea și urmează recomandările Coach pe exerciții. Poți verifica ajustările de greutate în previzualizare."
        )
    }
    val shape = RoundedCornerShape(18.dp)
    Surface(shape = shape, color = p.surface,
        modifier = Modifier.fillMaxWidth().trainPilotSoftGlow(p.primary, shape)
            .trainPilotBorder(1.dp, Brush.linearGradient(listOf(p.primary.copy(alpha = 0.7f), p.cardBorder)), shape)
            .clickable(onClick = onOpen).testTag("home_coach_advice_card")) {
        Column(Modifier.background(Brush.verticalGradient(listOf(p.surfaceElevated.copy(alpha = 0.65f), p.surface)))
            .padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(strings.coachSuggestionHeader, color = p.primary, style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.primary, modifier = Modifier.size(18.dp))
            }
            Text(heading, color = p.textPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(summary, color = p.textSecondary, style = MaterialTheme.typography.labelSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("home_coach_summary"))
            Surface(color = p.surfaceElevated, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.readinessTodayLabel, color = p.textSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    Text(score?.let { "$it/100" } ?: "—", color = p.textPrimary,
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

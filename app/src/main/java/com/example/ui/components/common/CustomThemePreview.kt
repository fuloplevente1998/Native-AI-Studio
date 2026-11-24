package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.*

/** Illustrative mini-screen, not real exercise data or an actionable workout. */
@Composable
internal fun CustomThemePreview(resolved: ResolvedCustomTheme, workout: Boolean, language: AppLanguage) {
    val p = resolved.palette
    val selection = remember(p) { p.selectionColors() }
    fun word(hu: String, en: String, de: String, ro: String) = customThemeText(language, hu, en, de, ro)
    val shape = RoundedCornerShape(14.dp)
    TrainPilotTheme(palette = p) {
        Column(Modifier.fillMaxWidth().background(p.background, shape)
            .border(1.dp, p.cardBorder, shape).padding(10.dp).testTag("custom_theme_preview"),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PreviewLabel(word("Kezdőlap", "Home", "Start", "Acasă"),
                    if (!workout) selection.surface else p.surface, if (!workout) selection.foreground else p.textPrimary,
                    if (!workout) selection.border else p.cardBorder, Modifier.weight(1f))
                PreviewLabel(word("Edzés", "Workout", "Training", "Antrenament"),
                    if (workout) selection.surface else p.surface, if (workout) selection.foreground else p.textPrimary,
                    if (workout) selection.border else p.cardBorder, Modifier.weight(1f))
            }
            // Preview the real paint alpha over the same solid background used by the resolver.
            Column(Modifier.fillMaxWidth().background(resolved.rawCard.copy(alpha = resolved.cardOpacity), shape)
                .border(1.dp, p.cardBorder, shape).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (workout) word("Aktív edzés", "Active workout", "Aktives Training", "Antrenament activ")
                    else word("Aktív program", "Active program", "Aktives Programm", "Program activ"),
                    color = p.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                if (workout) {
                    PreviewLabel("1 / 4", selection.surface, selection.foreground, selection.border)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(word("Súly", "Weight", "Gewicht", "Greutate"), word("Ism.", "Reps", "Wdh.", "Rep."), "RIR")
                            .forEach { PreviewLabel(it, p.surfaceElevated, p.textSecondary, p.fieldOutline(), Modifier.weight(1f)) }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().height(32.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                        listOf(.35f, .65f, .45f, .85f, .55f, 1f, .75f).forEachIndexed { index, fraction ->
                            Box(Modifier.weight(1f).fillMaxHeight(fraction)
                                .background(if (index == 5) p.primary else p.secondary, RoundedCornerShape(3.dp)))
                        }
                    }
                }
                PreviewLabel(if (workout) word("Következő", "Next", "Weiter", "Următorul")
                    else word("Indítás", "Start", "Start", "Pornire"),
                    p.primary, p.readablePrimaryLabel(), p.primary, Modifier.fillMaxWidth())
                Text(word("Siker", "Success", "Erfolg", "Succes"), color = p.success, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PreviewLabel(text: String, background: Color, foreground: Color, border: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(modifier.background(background, shape).border(1.dp, border, shape).padding(6.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = foreground, style = MaterialTheme.typography.labelSmall)
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AppLanguage
import com.example.ui.theme.*
import com.repforge.app.ui.theme.TrainPilotFrames

/** Full-window sample, using the exact resolved palette, never an actual workout or stored data. */
@Composable
internal fun CustomThemePreviewDialog(resolved: ResolvedCustomTheme, language: AppLanguage, onDismiss: () -> Unit) {
    val p = resolved.palette
    var workout by remember { mutableStateOf(false) }
    var set by remember { mutableIntStateOf(1) }
    fun word(hu: String, en: String, de: String, ro: String) = customThemeText(language, hu, en, de, ro)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(
        usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        TrainPilotTheme(palette = p) {
            Surface(Modifier.fillMaxSize().testTag("custom_theme_preview_dialog"), color = p.background) {
                Column(Modifier.fillMaxSize().background(p.pageBrush).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("TrainPilot", color = p.primary, style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold)
                            Text(word("Előnézet · mintaadatok", "Preview · sample data", "Vorschau · Beispieldaten",
                                "Previzualizare · date demonstrative"), color = p.textSecondary)
                        }
                        TrainPilotRedCloseButton(onClick = onDismiss, testTag = "custom_theme_close_preview")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrainPilotFilterChip(!workout, { workout = false },
                            { Text(word("Kezdőlap", "Home", "Start", "Acasă")) },
                            modifier = Modifier.weight(1f).testTag("custom_theme_preview_home"))
                        TrainPilotFilterChip(workout, { workout = true },
                            { Text(word("Aktív edzés", "Workout", "Training", "Antrenament")) },
                            modifier = Modifier.weight(1f).testTag("custom_theme_preview_workout"))
                    }
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!workout) {
                            PreviewCard(p, word("Aktív program", "Active program", "Aktives Programm", "Program activ")) {
                                Text(word("Teljes test A", "Full body A", "Ganzkörper A", "Corp întreg A"),
                                    style = MaterialTheme.typography.titleLarge, color = p.textPrimary, fontWeight = FontWeight.Bold)
                                Text(word("8 gyakorlat · 60 perc", "8 exercises · 60 min", "8 Übungen · 60 Min.", "8 exerciții · 60 min"),
                                    color = p.textSecondary)
                                TrainPilotPrimaryButton(word("Edzés indítása", "Start workout", "Training starten", "Începe antrenamentul"),
                                    { workout = true }, Modifier.fillMaxWidth())
                            }
                            PreviewCard(p, word("Heti célok", "Weekly goals", "Wochenziele", "Obiective săptămânale")) {
                                Text("3 / 5", style = MaterialTheme.typography.headlineMedium, color = p.primary, fontWeight = FontWeight.Bold)
                                Box(Modifier.fillMaxWidth().height(10.dp).background(p.surfaceElevated, RoundedCornerShape(5.dp))) {
                                    Box(Modifier.fillMaxWidth(.6f).fillMaxHeight().background(p.primary, RoundedCornerShape(5.dp)))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    PreviewLabel("7 240", p.surfaceElevated, p.textPrimary, p.fieldOutline(), Modifier.weight(1f))
                                    PreviewLabel("78 bpm", p.surfaceElevated, p.heartAccent, p.fieldOutline(), Modifier.weight(1f))
                                }
                            }
                            PreviewCard(p, word("Fejlődés", "Progress", "Fortschritt", "Progres")) {
                                Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Bottom) {
                                    listOf(.35f, .65f, .45f, .85f, .55f, 1f, .75f).forEachIndexed { index, fraction ->
                                        Box(Modifier.weight(1f).fillMaxHeight(fraction)
                                            .background(if (index == 5) p.primary else p.secondary, RoundedCornerShape(5.dp)))
                                    }
                                }
                                Text(word("Minta – heti edzésvolumen", "Sample – weekly volume", "Beispiel – Wochenvolumen",
                                    "Exemplu – volum săptămânal"), color = p.textSecondary)
                            }
                        } else {
                            PreviewCard(p, word("Aktív edzés", "Active workout", "Aktives Training", "Antrenament activ")) {
                                Text(word("Fekvenyomás", "Bench press", "Bankdrücken", "Împins la piept"),
                                    color = p.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("00:18:32", color = p.primary, style = MaterialTheme.typography.headlineMedium)
                                val selection = remember(p) { p.selectionColors() }
                                PreviewLabel("$set / 4", selection.surface, selection.foreground, selection.border, Modifier.fillMaxWidth())
                            }
                            PreviewCard(p, word("Sorozatok", "Sets", "Sätze", "Serii")) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(word("Súly", "Weight", "Gewicht", "Greutate") + "\n60 kg",
                                        word("Ism.", "Reps", "Wdh.", "Rep.") + "\n10", "RIR\n2").forEach {
                                        PreviewLabel(it, p.surfaceElevated, p.textPrimary, p.fieldOutline(), Modifier.weight(1f))
                                    }
                                }
                                Text(word("Pihenő", "Rest", "Pause", "Pauză") + " · 01:30", color = p.warning)
                                TrainPilotPrimaryButton(word("Következő sorozat", "Next set", "Nächster Satz", "Seria următoare"),
                                    { set = if (set == 4) 1 else set + 1 }, Modifier.fillMaxWidth())
                                Text(word("Sorozat teljesítve", "Set completed", "Satz abgeschlossen", "Serie finalizată"), color = p.success)
                            }
                            PreviewCard(p, word("Állapotjelzések", "Status colors", "Statusfarben", "Culori de stare")) {
                                Text(word("Siker", "Success", "Erfolg", "Succes"), color = p.success)
                                Text(word("Figyelmeztetés", "Warning", "Warnung", "Avertisment"), color = p.warning)
                                Text(word("Törlés", "Delete", "Löschen", "Ștergere"), color = p.danger)
                            }
                        }
                    }
                    TrainPilotSecondaryButton(word("Vissza a szerkesztéshez", "Back to editor", "Zurück zum Editor",
                        "Înapoi la editor"), onDismiss, Modifier.fillMaxWidth().testTag("custom_theme_preview_back"))
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(p: TrainPilotPalette, title: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Surface(shape = shape, color = p.surfaceCard, modifier = Modifier.fillMaxWidth()
        .border(TrainPilotFrames.page(p.sectionBorder, p.primary), shape)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = p.textSecondary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun PreviewLabel(text: String, background: Color, foreground: Color, border: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Box(modifier.background(background, shape).border(1.dp, border, shape).padding(12.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = foreground, style = MaterialTheme.typography.labelLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

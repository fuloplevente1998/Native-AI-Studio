package com.example.ui.screens

import com.example.ui.components.TrainPilotMenuItem

import com.repforge.app.ui.theme.trainPilotClickable
import com.example.ui.components.TrainPilotFullHeightFormSheet

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import com.example.ui.components.TrainPilotCheckbox as Checkbox
import com.example.ui.components.TrainPilotDropdownMenu as DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ExerciseCategory
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette

@Composable
fun AddCustomExerciseModalDialog(
    onDismiss: () -> Unit,
    programDayLabel: String? = null,
    onSaveCustomExercise: (
        nameHu: String,
        category: ExerciseCategory,
        equipmentHu: String,
        descriptionHu: String,
        videoUrl: String,
        isTimed: Boolean,
        addToCurrentProgramDay: Boolean
    ) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val lang = strings.language
    var nameHu by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExerciseCategory.LEGS) }
    var categoryDropdownOpen by remember { mutableStateOf(false) }

    val equipmentOptions = listOf(
        "Saját testsúly / Kültéri (pl. Kör futás)" to trW(lang, "Saját testsúly / Kültéri (pl. Kör futás)", "Bodyweight / Outdoor (e.g. Track run)", "Eigengewicht / Outdoor (z. B. Bahnlauf)", "Greutate corporală / Exterior (ex. Alergare pe pistă)"),
        "Kézi súlyzó (Dumbbell)" to trW(lang, "Kézi súlyzó (Dumbbell)", "Dumbbell", "Kurzhantel (Dumbbell)", "Ganteră (Dumbbell)"),
        "Rúd és tárcsa (Barbell)" to trW(lang, "Rúd és tárcsa (Barbell)", "Barbell & plates", "Langhantel & Scheiben (Barbell)", "Haltere și discuri (Barbell)"),
        "Kondigép / Kábel" to trW(lang, "Kondigép / Kábel", "Gym machine / Cable", "Fitnessgerät / Kabelzug", "Aparat sală / Cablu"),
        "Gumiszalag / Kettlebell" to trW(lang, "Gumiszalag / Kettlebell", "Resistance band / Kettlebell", "Widerstandsband / Kettlebell", "Bandă elastică / Kettlebell")
    )
    var selectedEquipment by remember { mutableStateOf(equipmentOptions.first().first) }
    var equipmentDropdownOpen by remember { mutableStateOf(false) }

    var descriptionHu by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var isTimed by remember { mutableStateOf(false) }
    var addToCurrentProgramDay by remember(programDayLabel) { mutableStateOf(false) }

    TrainPilotFullHeightFormSheet(
        onDismiss = onDismiss,
        title = trW(lang, "Új gyakorlat", "New exercise", "Neue Übung", "Exercițiu nou"),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Surface(shape = RoundedCornerShape(14.dp), color = palette.surfaceElevated,
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                com.example.ui.components.TrainPilotTextField(
                    value = nameHu,
                    onValueChange = { nameHu = it },
                    label = { Text(trW(lang, "Gyakorlat neve (pl. Kör futás)", "Exercise name (e.g. Track run)", "Übungsname (z. B. Bahnlauf)", "Numele exercițiului (ex. Alergare pe pistă)")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = palette.surface,
                        unfocusedContainerColor = palette.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_exercise_name_input")
                )

                // Floating Dropdown: Izomcsoport / Kategória
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                            .trainPilotClickable(RoundedCornerShape(12.dp)) { categoryDropdownOpen = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(trW(lang, "Izomcsoport / Kategória", "Muscle group / Category", "Muskelgruppe / Kategorie", "Grupă musculară / Categorie"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                Text(selectedCategory.localized(lang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            }
                            Icon(
                                imageVector = if (categoryDropdownOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = palette.primary
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = categoryDropdownOpen,
                        onDismissRequest = { categoryDropdownOpen = false },
                        modifier = if (palette.isLiquidGlass) Modifier else Modifier.background(palette.surfaceElevated)
                    ) {
                        ExerciseCategory.entries.filter { it != ExerciseCategory.ALL }.forEach { cat ->
                            TrainPilotMenuItem(
                                text = {
                                    Text(
                                        text = cat.localized(lang),
                                        color = if (cat == selectedCategory) palette.primary else palette.textPrimary,
                                        fontWeight = if (cat == selectedCategory) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Floating Dropdown: Eszköz / Típus
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                            .trainPilotClickable(RoundedCornerShape(12.dp)) { equipmentDropdownOpen = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(trW(lang, "Eszköz / Mérés jellege", "Equipment / Measurement type", "Gerät / Messart", "Echipament / Tip măsurare"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                val displayEq = equipmentOptions.firstOrNull { it.first == selectedEquipment }?.second ?: selectedEquipment
                                Text(displayEq, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            }
                            Icon(
                                imageVector = if (equipmentDropdownOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = palette.primary
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = equipmentDropdownOpen,
                        onDismissRequest = { equipmentDropdownOpen = false },
                        modifier = if (palette.isLiquidGlass) Modifier else Modifier.background(palette.surfaceElevated)
                    ) {
                        equipmentOptions.forEach { (canonicalEq, displayEq) ->
                            TrainPilotMenuItem(
                                text = {
                                    Text(
                                        text = displayEq,
                                        color = if (canonicalEq == selectedEquipment) palette.primary else palette.textPrimary,
                                        fontWeight = if (canonicalEq == selectedEquipment) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedEquipment = canonicalEq
                                    equipmentDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                com.example.ui.components.TrainPilotTextField(
                    value = descriptionHu,
                    onValueChange = { descriptionHu = it },
                    label = { Text(trW(lang, "Leírás / Végrehajtási lépések", "Description / Execution steps", "Beschreibung / Ausführungsschritte", "Descriere / Pași de execuție")) },
                    placeholder = { Text(trW(lang, "Pl. 400m egyenletes tempójú kör futás, majd 60 mp séta pihenő...", "E.g. 400m steady pace run, then 60 s walking rest...", "Z. B. 400 m gleichmäßiger Lauf, dann 60 Sek. Gehpause...", "Ex. alergare 400 m ritm constant, apoi 60 sec pauză de mers...")) },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = palette.surface,
                        unfocusedContainerColor = palette.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_exercise_description_input")
                )

                com.example.ui.components.TrainPilotTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text(trW(lang, "Videó link vagy YouTube azonosító (opcionális)", "Video URL or YouTube ID (optional)", "Video-Link oder YouTube-ID (optional)", "Link video sau ID YouTube (opțional)")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = palette.surface,
                        unfocusedContainerColor = palette.surface),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotClickable(RoundedCornerShape(10.dp)) { isTimed = !isTimed }
                ) {
                    Checkbox(checked = isTimed, onCheckedChange = { isTimed = it })
                    Text(trW(lang, "Időalapú gyakorlat (másodperc / perc mérés)", "Timed exercise (seconds / minutes measurement)", "Zeitbasierte Übung (Sekunden-/Minutenmessung)", "Exercițiu cronometrat (măsurare în secunde / minute)"), style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                }

                if (programDayLabel != null) Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .trainPilotClickable(RoundedCornerShape(10.dp)) { addToCurrentProgramDay = !addToCurrentProgramDay }
                ) {
                    Checkbox(checked = addToCurrentProgramDay, onCheckedChange = { addToCurrentProgramDay = it },
                        modifier = Modifier.testTag("custom_exercise_add_to_program"))
                    Text(trW(lang, "Hozzáadás ehhez: $programDayLabel", "Add to: $programDayLabel",
                        "Hinzufügen zu: $programDayLabel", "Adaugă la: $programDayLabel"),
                        style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                }
                    }
                }
            }
        },
        confirmButton = {
            com.repforge.app.ui.theme.TrainPilotSaveButton(
                onClick = {
                    onSaveCustomExercise(
                        nameHu,
                        selectedCategory,
                        selectedEquipment,
                        descriptionHu,
                        videoUrl,
                        isTimed,
                        programDayLabel != null && addToCurrentProgramDay
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("custom_exercise_save_button")
            ) {
                Text(strings.saveBtn, fontWeight = FontWeight.Bold)
            }
        }
    )
}

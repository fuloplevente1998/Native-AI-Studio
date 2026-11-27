package com.example.ui.components
import com.repforge.app.ui.theme.trainPilotFrame

import com.repforge.app.ui.theme.trainPilotClickable
import androidx.compose.material.icons.filled.Flag

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.TrainPilotFrames

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.glassTile

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.TrainPilotDropdownMenu as DropdownMenu
import androidx.compose.ui.semantics.contentDescription
import com.example.localization.LocalTrainPilotStrings

@Composable
fun PersonalPlannerModalDialog(
    initialHeightCm: Int = 179,
    initialWeightKg: Float = 65f,
    initialGoalHu: String = "Izomépítés",
    initialExperienceHu: String = "Kezdő",
    initialLocationHu: String = "Otthon",
    initialEquipmentHu: String = "Kézisúlyzó és pad",
    initialDaysPerWeek: Int = 3,
    initialDurationMin: Int = 45,
    initialExcludedMuscles: Set<String> = emptySet(),
    initialExcludedExercises: Set<String> = emptySet(),
    onSaveAndOpenPlan: PersonalPlannerSave,
    onDismiss: () -> Unit,
    saving: Boolean = false,
    failed: Boolean = false,
    gentlerStart: Boolean = false,
    recommendProgram: ((String, String, String, String, Int, Int, Set<String>, Set<String>) -> String?)? = null,
    onUseRecommendedPlan: PersonalPlannerSave? = null,
    trainingStyle: String? = null,
    progressionMode: String? = null,
    onTrainingModesChange: ((String?, String?) -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val lang = strings.language
    fun option(value: String) = com.example.localization.StudioProfileOptionLabels.label(value, lang)
    fun detail(value: String) = com.example.localization.StudioProfileOptionLabels.description(value, lang)
    var heightCm by remember { mutableIntStateOf(initialHeightCm) }
    var weightKg by remember { mutableFloatStateOf(initialWeightKg) }
    var selectedGoal by remember { mutableStateOf(initialGoalHu) }
    var selectedExperience by remember { mutableStateOf(initialExperienceHu) }
    var selectedLocation by remember { mutableStateOf(initialLocationHu) }
    val equipmentCatalog = listOf(
        "Saját testsúly" to "Alap saját testsúlyos gyakorlatok (fekvőtámasz, plank, guggolás)",
        "Egykezes kézisúlyzó (1 db)" to "1 db kézisúlyzós gyakorlatok (pl. goblet guggolás, egykezes evezés)",
        "Kézisúlyzó pár (2 db)" to "Páros egykezes súlyzós gyakorlatok (nyomások, kitörések, kar)",
        "Kétkezes rúd (Súlyzórúd)" to "Kétkezes rudas alapgyakorlatok (guggolás, felhúzás, evezés, nyomás)",
        "Pad / Szék / Kanapé" to "Támasz például bolgár guggoláshoz vagy csípőemeléshez",
        "Fekvenyomó pad" to "Stabil, súlyzós edzésre tervezett fekvenyomó pad",
        "Állítható fekvenyomó pad" to "Súlyzós edzésre tervezett, dönthető pad",
        "Biztonsági állvány" to "Rúdtartó állvány állítható biztonsági támaszokkal",
        "Stabil támaszték / Fal / Szék" to "Kéztámasz pl. egykezes evezéshez vagy vádliemeléshez",
        "Fellépő / Doboz / Lépcső" to "Magasítás fellépéshez (step-up) és felugráshoz",
        "Húzódzkodó rúd" to "Húzódzkodás, függeszkedés és lábemelések",
        "Tolódzkodó korlát" to "Tolódzkodás mellre és tricepszre",
        "Gumiszalag / Ellenállás-szalag" to "Gumiszalagos evezések, lehúzások és izolációk",
        "Kettlebell" to "Kettlebell swing, goblet guggolás és nyomás",
        "Edzőtermi gépek / Csigás gépek" to "Lábtoló, hátlehúzó, csigás és vezetett gépek",
        "Alacsony, stabil rúd" to "Fordított evezéshez használható fix rúd",
        "Biztonságos szalagrögzítő" to "Stabil pont a gumiszalagos húzó és nyomó gyakorlatokhoz"
    )
    val equipments = equipmentCatalog.map { it.first }

    fun parseInitialEquipments(raw: String): Set<String> {
        val tokens = raw.split(',').map { it.trim() }.filter { it.isNotBlank() }
        val result = linkedSetOf<String>()
        for (token in tokens) {
            val lower = token.lowercase()
            when {
                token in equipments -> result.add(token)
                lower == "csak saját testsúly" || lower.contains("saját testsúly") || lower == "bodyweight" -> {
                    result.add("Saját testsúly")
                }
                lower == "kézisúlyzó és pad" -> {
                    result.add("Saját testsúly")
                    result.add("Egykezes kézisúlyzó (1 db)")
                    result.add("Kézisúlyzó pár (2 db)")
                    result.add("Pad / Szék / Kanapé")
                    result.add("Stabil támaszték / Fal / Szék")
                }
                lower == "kézisúlyzó (pad nélkül)" -> {
                    result.add("Saját testsúly")
                    result.add("Egykezes kézisúlyzó (1 db)")
                    result.add("Kézisúlyzó pár (2 db)")
                }
                lower == "teljes edzőtermi géppark" || lower == "machines" -> {
                    result.addAll(equipments)
                }
                lower == "gumiszalag és rúd" -> {
                    result.add("Saját testsúly")
                    result.add("Gumiszalag / Ellenállás-szalag")
                    result.add("Húzódzkodó rúd")
                    result.add("Kétkezes rúd (Súlyzórúd)")
                }
                lower == "kettlebell és saját testsúly" || lower == "kettlebell" -> {
                    result.add("Saját testsúly")
                    result.add("Kettlebell")
                }
                lower == "dumbbell" || lower.contains("1 kézisúlyzó") || lower.contains("egykezes") -> {
                    result.add("Egykezes kézisúlyzó (1 db)")
                }
                lower == "dumbbells" || lower.contains("kézisúlyzó") -> {
                    result.add("Egykezes kézisúlyzó (1 db)")
                    result.add("Kézisúlyzó pár (2 db)")
                }
                lower == "barbell" || lower.contains("kétkezes") || lower.contains("súlyzórúd") -> {
                    result.add("Kétkezes rúd (Súlyzórúd)")
                }
                lower == "lifting-bench" -> result.add("Fekvenyomó pad")
                lower == "incline-bench" -> result.add("Állítható fekvenyomó pad")
                lower == "rack" -> result.add("Biztonsági állvány")
                lower == "low-bar" -> result.add("Alacsony, stabil rúd")
                lower == "band-anchor" -> result.add("Biztonságos szalagrögzítő")
                lower == "bench" || lower.contains("pad") || lower.contains("kanapé") -> {
                    result.add("Pad / Szék / Kanapé")
                }
                lower == "support" || lower.contains("támasz") || lower.contains("szék") -> {
                    result.add("Stabil támaszték / Fal / Szék")
                }
                lower == "step" || lower.contains("fellépő") || lower.contains("lépcső") -> {
                    result.add("Fellépő / Doboz / Lépcső")
                }
                lower == "pullupbar" || lower.contains("húzódzkodó") -> {
                    result.add("Húzódzkodó rúd")
                }
                lower == "dipbars" || lower.contains("tolódzkodó") -> {
                    result.add("Tolódzkodó korlát")
                }
                lower == "bands" || lower.contains("gumiszalag") -> {
                    result.add("Gumiszalag / Ellenállás-szalag")
                }
                else -> result.add(token)
            }
        }
        if (result.isEmpty()) {
            result.add("Saját testsúly")
            result.add("Egykezes kézisúlyzó (1 db)")
            result.add("Kézisúlyzó pár (2 db)")
            result.add("Pad / Szék / Kanapé")
        }
        return result
    }

    var selectedEquipments by remember(initialEquipmentHu) {
        mutableStateOf(parseInitialEquipments(initialEquipmentHu))
    }
    val selectedEquipment = remember(selectedEquipments) {
        selectedEquipments.joinToString(", ").ifBlank { "Saját testsúly" }
    }
    var selectedDays by remember { mutableIntStateOf(initialDaysPerWeek) }
    var selectedDuration by remember { mutableIntStateOf(initialDurationMin) }
    var excludedMuscles by remember { mutableStateOf(initialExcludedMuscles) }
    var excludedExercises by remember { mutableStateOf(initialExcludedExercises) }

    // Individual floating dropdown state (opens on top of other fields without pushing them down)
    var openDropdownKey by remember { mutableStateOf<String?>(null) }

    val goals = listOf(
        "Izomépítés",
        "Fogyás / Szálkásítás",
        "Erőnövelés",
        "Általános fittség",
        "Állóképesség és tónus"
    )
    val experiences = listOf("Kezdő", "Újrakezdő", "Középhaladó", "Haladó")
    val locations = listOf(
        "Otthon",
        "Edzőterem",
        "Vegyes (Otthon + Terem)",
        "Szabadtér / Street workout"
    )
    val daysOptions = listOf(2, 3, 4, 5, 6)
    val durationOptions = listOf(30, 45, 60, 75, 90)

    val heightPresets = (150..205).toList()
    val weightPresets = (45..130).map { it.toFloat() }

    val muscleOptions = listOf("Láb", "Mell", "Hát", "Váll", "Kar", "Törzs", "Alsó hát", "Térdkímélő")
    val exerciseOptions = listOf(
        "Bolgár guggolás" to "Láb • Kézisúlyzó",
        "Egykezes evezés" to "Hát • Kézisúlyzó",
        "Fekvenyomás" to "Mell • Rúd",
        "Román felhúzás" to "Láb • Rúd",
        "Húzódzkodás" to "Hát • Saját testsúly",
        "Tolódzkodás" to "Mell • Saját testsúly",
        "Kitörés sétálva" to "Láb • Kézisúlyzó",
        "Vállból nyomás" to "Váll • Kézisúlyzó"
    )

    @Composable
    fun PlannerDropdownBox(
        dropdownKey: String,
        label: String,
        selectedValue: String,
        options: List<String>,
        onSelect: (Int, String) -> Unit,
        modifier: Modifier = Modifier,
        testTag: String = ""
    ) {
        val isOpen = openDropdownKey == dropdownKey
        Box(modifier = modifier) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.glassTile(palette.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(
                        1.dp,
                        if (isOpen) palette.primary else palette.cardBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .trainPilotClickable(RoundedCornerShape(12.dp)) {
                        openDropdownKey = if (isOpen) null else dropdownKey
                    }
                    .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedValue,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = isOpen,
                onDismissRequest = { openDropdownKey = null },
                containerColor = palette.background,
                shape = RoundedCornerShape(12.dp),
                border = TrainPilotFrames.stroke(1.dp, palette.primary.copy(alpha = 0.7f)),
                modifier = Modifier
                    .widthIn(min = 210.dp, max = 290.dp)
                    .heightIn(max = 260.dp)
            ) {
                options.forEachIndexed { index, optionText ->
                    val isSelected = optionText == selectedValue
                    TrainPilotMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) palette.primary else palette.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelect(index, optionText)
                            openDropdownKey = null
                        },
                        modifier = Modifier.trainPilotMenuSelection(isSelected, palette.primary.copy(alpha = 0.16f))
                    )
                }
            }
        }
    }

    TrainPilotFullHeightFormSheet(
        onDismiss = onDismiss,
        dismissEnabled = !saving,
        title = tr(lang, "Profil és új terv", "Profile and new plan", "Profil und neuer Plan", "Profil și plan nou"),
        closeTestTag = "planner_skip_btn",
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top row: Magasság (cm) & Testsúly (kg) — each opens its own floating dropdown list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    PlannerDropdownBox(
                        dropdownKey = "height",
                        label = tr(lang, "Magasság (cm)", "Height (cm)", "Größe (cm)", "Înălțime (cm)"),
                        selectedValue = "$heightCm cm",
                        options = heightPresets.map { "$it cm" },
                        onSelect = { idx, _ -> heightCm = heightPresets[idx] },
                        modifier = Modifier.weight(1f),
                        testTag = "profile_height_box"
                    )

                    PlannerDropdownBox(
                        dropdownKey = "weight",
                        label = tr(lang, "Testsúly (kg)", "Body weight (kg)", "Körpergewicht (kg)", "Greutate (kg)"),
                        selectedValue = "${weightKg.toInt()} kg",
                        options = weightPresets.map { "${it.toInt()} kg" },
                        onSelect = { idx, _ -> weightKg = weightPresets[idx] },
                        modifier = Modifier.weight(1f),
                        testTag = "profile_weight_box"
                    )
                }

                // 1. Cél és edzettség
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.glassTile(palette.surfaceElevated, elevated = true),
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                            Column {
                                Text(tr(lang, "Cél és edzettség", "Goal & fitness level", "Ziel & Fitnesslevel", "Obiectiv și nivel de fitness"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text("${option(selectedGoal)} • ${option(selectedExperience)} • ${option(selectedLocation)}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            }
                        }

                        PlannerDropdownBox(
                            dropdownKey = "goal",
                            label = tr(lang, "Elsődleges cél", "Primary goal", "Primäres Ziel", "Obiectiv principal"),
                            selectedValue = option(selectedGoal),
                            options = goals.map(::option),
                            onSelect = { index, _ -> selectedGoal = goals[index] },
                            testTag = "profile_goal_box"
                        )

                        PlannerDropdownBox(
                            dropdownKey = "experience",
                            label = tr(lang, "Tapasztalati szint", "Experience level", "Erfahrungsstufe", "Nivel de experiență"),
                            selectedValue = option(selectedExperience),
                            options = experiences.map(::option),
                            onSelect = { index, _ -> selectedExperience = experiences[index] },
                            testTag = "profile_experience_box"
                        )

                        PlannerDropdownBox(
                            dropdownKey = "location",
                            label = tr(lang, "Edzés helyszíne", "Workout location", "Trainingsort", "Locație antrenament"),
                            selectedValue = option(selectedLocation),
                            options = locations.map(::option),
                            onSelect = { index, _ -> selectedLocation = locations[index] },
                            testTag = "profile_location_box"
                        )
                    }
                }

                if (onTrainingModesChange != null) {
                    val defaultChoice = tr(lang,
                        "A program szerint", "Use plan setting", "Laut Plan", "Conform planului")
                    val styles = listOf(null, "normal", "hit")
                    val progressions = listOf(null, "double", "linear", "rir")
                    val labels = com.example.localization.StudioProfileOptionLabels
                    val styleLabels = styles.map { it?.let { code -> labels.trainingStyleName(lang, code) } ?: defaultChoice }
                    val progressionLabels = progressions.map { it?.let { code -> labels.progressionName(lang, code) } ?: defaultChoice }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.glassTile(palette.surfaceElevated, elevated = true),
                        modifier = Modifier.fillMaxWidth()
                            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                tr(lang, "Edzésbeállítások", "Workout settings", "Trainingseinstellungen", "Setări de antrenament"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            PlannerDropdownBox(
                                dropdownKey = "training_style",
                                label = tr(lang, "Edzésstílus", "Training style", "Trainingsstil", "Stil de antrenament"),
                                selectedValue = styleLabels.getOrElse(styles.indexOf(trainingStyle)) { defaultChoice },
                                options = styleLabels,
                                onSelect = { index, _ -> onTrainingModesChange(styles[index], progressionMode) },
                                testTag = "profile_training_style_box"
                            )
                            PlannerDropdownBox(
                                dropdownKey = "progression_mode",
                                label = tr(lang, "Progressziós szabály", "Progression rule", "Progressionsregel", "Regulă de progresie"),
                                selectedValue = progressionLabels.getOrElse(progressions.indexOf(progressionMode)) { defaultChoice },
                                options = progressionLabels,
                                onSelect = { index, _ -> onTrainingModesChange(trainingStyle, progressions[index]) },
                                testTag = "profile_progression_mode_box"
                            )
                        }
                    }
                }

                // 2. Felszerelés (Több opció egyidejű kijelölése)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.glassTile(palette.surfaceElevated, elevated = true),
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🏋️", fontSize = 16.sp)
                            Column {
                                Text(tr(lang, "Felszerelés (több is választható)", "Equipment (multi-select)", "Ausrüstung (Mehrfachauswahl)", "Echipament (selecție multiplă)"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text(
                                    text = tr(
                                        lang,
                                        "${selectedEquipments.size} eszköz kiválasztva • A terv ezek alapján épül fel",
                                        "${selectedEquipments.size} items selected • Plan is built from these",
                                        "${selectedEquipments.size} Geräte ausgewählt • Plan basiert darauf",
                                        "${selectedEquipments.size} echipamente selectate • Planul se bazează pe acestea"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        val isEquipmentOpen = openDropdownKey == "equipment"
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.glassTile(palette.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .trainPilotBorder(
                                        1.dp,
                                        if (isEquipmentOpen) palette.primary else palette.cardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .trainPilotClickable(RoundedCornerShape(12.dp)) {
                                        openDropdownKey = if (isEquipmentOpen) null else "equipment"
                                    }
                                    .testTag("profile_equipment_box")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tr(lang, "Elérhető eszközök (koppints a listához)", "Available equipment (tap to open list)", "Verfügbare Geräte (tippen zum Öffnen)", "Echipamente disponibile (atinge pentru listă)"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                        Icon(
                                            imageVector = if (isEquipmentOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = selectedEquipments.joinToString(", ") { option(it) },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isEquipmentOpen,
                                onDismissRequest = { openDropdownKey = null },
                                containerColor = palette.background,
                                shape = RoundedCornerShape(12.dp),
                                border = TrainPilotFrames.stroke(1.dp, palette.primary.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .widthIn(min = 250.dp, max = 320.dp)
                                    .heightIn(max = 320.dp)
                            ) {
                                equipmentCatalog.forEachIndexed { idx, (eqName, eqDesc) ->
                                    val isChecked = eqName in selectedEquipments
                                    TrainPilotMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .trainPilotBorder(
                                                            1.5.dp,
                                                            if (isChecked) palette.primary else palette.textSecondary,
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .background(if (isChecked) palette.primary else Color.Transparent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isChecked) {
                                                        Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.onPrimary)
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = option(eqName),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (isChecked) palette.primary else palette.textPrimary
                                                    )
                                                    Text(
                                                        text = option(eqDesc),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.textSecondary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            val updated = if (isChecked) {
                                                if (selectedEquipments.size > 1) selectedEquipments - eqName else selectedEquipments
                                            } else {
                                                selectedEquipments + eqName
                                            }
                                            selectedEquipments = updated
                                        },
                                        modifier = Modifier
                                            .trainPilotMenuSelection(isChecked, palette.primary.copy(alpha = 0.14f))
                                            .testTag("profile_equipment_option_$idx")
                                    )
                                }
                            }
                        }


                    }
                }

                // 3. Edzésbeosztás
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.glassTile(palette.surfaceElevated, elevated = true),
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Column {
                                Text(tr(lang, "Edzésbeosztás", "Workout schedule", "Trainingsplan", "Program de antrenament"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text(
                                    text = tr(
                                        lang,
                                        "Heti $selectedDays nap • $selectedDuration perc/edzés",
                                        "$selectedDays days/week • $selectedDuration min/workout",
                                        "$selectedDays Tage/Woche • $selectedDuration Min./Training",
                                        "$selectedDays zile/săptămână • $selectedDuration min/antrenament"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlannerDropdownBox(
                                dropdownKey = "days_per_week",
                                label = tr(lang, "Heti edzésnapok", "Weekly workout days", "Trainingstage pro Woche", "Zile de antrenament pe săptămână"),
                                selectedValue = tr(lang, "Heti $selectedDays nap", "$selectedDays days/week", "$selectedDays Tage/Woche", "$selectedDays zile/săpt."),
                                options = daysOptions.map { tr(lang, "Heti $it nap", "$it days/week", "$it Tage/Woche", "$it zile/săpt.") },
                                onSelect = { idx, _ -> selectedDays = daysOptions[idx] },
                                modifier = Modifier.weight(1f),
                                testTag = "profile_days_box"
                            )

                            PlannerDropdownBox(
                                dropdownKey = "workout_duration",
                                label = tr(lang, "Időtartam", "Duration", "Dauer", "Durată"),
                                selectedValue = tr(lang, "$selectedDuration perc", "$selectedDuration min", "$selectedDuration Min.", "$selectedDuration min"),
                                options = durationOptions.map { tr(lang, "$it perc", "$it min", "$it Min.", "$it min") },
                                onSelect = { idx, _ -> selectedDuration = durationOptions[idx] },
                                modifier = Modifier.weight(1f),
                                testTag = "profile_duration_box"
                            )
                        }
                    }
                }

                // 4. Korlátozások (Piros kiemeléssel, fölé nyíló legördülő menükkel)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.glassTile(palette.surfaceElevated, elevated = true),
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotFrame(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚙️", fontSize = 16.sp)
                            Column {
                                Text(tr(lang, "Korlátozások", "Restrictions", "Einschränkungen", "Restricții"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.primary)
                                val totalExcluded = excludedMuscles.size + excludedExercises.size
                                Text(
                                    text = if (totalExcluded == 0) {
                                        tr(lang, "Nincs kizárás megadva", "No exclusions set", "Keine Ausschlüsse festgelegt", "Nicio excludere setată")
                                    } else {
                                        tr(lang, "$totalExcluded kizárás kiválasztva", "$totalExcluded exclusions selected", "$totalExcluded Ausschlüsse ausgewählt", "$totalExcluded excluderi selectate")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        // 4.1 Kerülendő területek floating dropdown box
                        val isMusclesOpen = openDropdownKey == "excluded_muscles"
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.glassTile(palette.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isMusclesOpen) Color(0xFFFF6E6E) else Color(0xFF7A1B28),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .trainPilotClickable(RoundedCornerShape(12.dp)) {
                                        openDropdownKey = if (isMusclesOpen) null else "excluded_muscles"
                                    }
                                    .testTag("profile_excluded_muscles_box")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tr(lang, "Kerülendő területek", "Areas to avoid", "Zu vermeidende Bereiche", "Zone de evitat"),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF6E6E)
                                        )
                                        Icon(
                                            imageVector = if (isMusclesOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = Color(0xFFFF6E6E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = if (excludedMuscles.isEmpty()) {
                                            tr(lang, "Nincs kizárt terület (koppints a választáshoz)", "No excluded areas (tap to select)", "Keine ausgeschlossenen Bereiche (tippen zur Auswahl)", "Nicio zonă exclusă (atinge pentru a selecta)")
                                        } else {
                                            tr(lang, "Kizárva: ${excludedMuscles.joinToString(", ") { option(it) }}", "Excluded: ${excludedMuscles.joinToString(", ") { option(it) }}", "Ausgeschlossen: ${excludedMuscles.joinToString(", ") { option(it) }}", "Excluse: ${excludedMuscles.joinToString(", ") { option(it) }}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (excludedMuscles.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                                        color = if (excludedMuscles.isEmpty()) palette.textSecondary else palette.primary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isMusclesOpen,
                                onDismissRequest = { openDropdownKey = null },
                                containerColor = palette.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6E6E)),
                                modifier = Modifier
                                    .widthIn(min = 240.dp, max = 300.dp)
                                    .heightIn(max = 280.dp)
                            ) {
                                muscleOptions.forEach { muscle ->
                                    val isChecked = muscle in excludedMuscles
                                    TrainPilotMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .trainPilotBorder(
                                                            1.5.dp,
                                                            if (isChecked) palette.primary else palette.textSecondary,
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .background(if (isChecked) palette.primary else Color.Transparent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isChecked) {
                                                        Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.onPrimary)
                                                    }
                                                }
                                                Text(
                                                    text = option(muscle),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = palette.textPrimary
                                                )
                                            }
                                        },
                                        onClick = {
                                            excludedMuscles = if (isChecked) excludedMuscles - muscle else excludedMuscles + muscle
                                        }
                                    )
                                }
                            }
                        }

                        // 4.2 Egyedi gyakorlatkizárás floating dropdown box
                        val isExercisesOpen = openDropdownKey == "excluded_exercises"
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.glassTile(palette.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isExercisesOpen) Color(0xFFFF6E6E) else Color(0xFF7A1B28),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .trainPilotClickable(RoundedCornerShape(12.dp)) {
                                        openDropdownKey = if (isExercisesOpen) null else "excluded_exercises"
                                    }
                                    .testTag("profile_excluded_exercises_box")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tr(lang, "Egyedi gyakorlatkizárás", "Custom exercise exclusions", "Eigene Übungsausschlüsse", "Excluderi individuale de exerciții"),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF6E6E)
                                        )
                                        Icon(
                                            imageVector = if (isExercisesOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = Color(0xFFFF6E6E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = if (excludedExercises.isEmpty()) {
                                            tr(lang, "Nincs kizárt gyakorlat (koppints a választáshoz)", "No excluded exercises (tap to select)", "Keine ausgeschlossenen Übungen (tippen zur Auswahl)", "Niciun exercițiu exclus (atinge pentru a selecta)")
                                        } else {
                                            tr(lang, "Kizárva: ${excludedExercises.joinToString(", ") { option(it) }}", "Excluded: ${excludedExercises.joinToString(", ") { option(it) }}", "Ausgeschlossen: ${excludedExercises.joinToString(", ") { option(it) }}", "Excluse: ${excludedExercises.joinToString(", ") { option(it) }}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (excludedExercises.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                                        color = if (excludedExercises.isEmpty()) palette.textSecondary else palette.primary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isExercisesOpen,
                                onDismissRequest = { openDropdownKey = null },
                                containerColor = palette.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6E6E)),
                                modifier = Modifier
                                    .widthIn(min = 250.dp, max = 310.dp)
                                    .heightIn(max = 280.dp)
                            ) {
                                exerciseOptions.forEach { (exName, exSub) ->
                                    val isChecked = exName in excludedExercises
                                    TrainPilotMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .trainPilotBorder(
                                                            1.5.dp,
                                                            if (isChecked) palette.primary else palette.textSecondary,
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .background(if (isChecked) palette.primary else Color.Transparent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isChecked) {
                                                        Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.onPrimary)
                                                    }
                                                }
                                                Column {
                                                    Text(
                                                        text = option(exName),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = palette.textPrimary
                                                    )
                                                    Text(
                                                        text = detail(exSub),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = palette.textSecondary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            excludedExercises = if (isChecked) excludedExercises - exName else excludedExercises + exName
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val recommendation = remember(recommendProgram, selectedEquipment, selectedLocation, selectedExperience,
                selectedGoal, selectedDays, selectedDuration, excludedMuscles, excludedExercises) {
                recommendProgram?.invoke(selectedEquipment, selectedLocation, selectedExperience,
                    selectedGoal, selectedDays, selectedDuration, excludedMuscles, excludedExercises)
            }
            if (onUseRecommendedPlan != null && recommendation != null) {
                com.repforge.app.ui.theme.TrainPilotOutlinedButton(
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("planner_recommended_button"),
                    onClick = { onUseRecommendedPlan(heightCm, weightKg, selectedGoal, selectedExperience,
                        selectedLocation, selectedEquipment, selectedDays, selectedDuration, excludedMuscles, excludedExercises) }
                ) {
                    Text(tr(lang, "Ajánlott program: $recommendation", "Recommended program: $recommendation",
                        "Empfohlenes Programm: $recommendation", "Program recomandat: $recommendation"))
                }
            } else if (recommendProgram != null) {
                Text(tr(lang, "A beállításokhoz nincs megfelelő kész program.", "No existing program matches these settings.",
                    "Kein vorhandenes Programm passt zu diesen Einstellungen.", "Niciun program existent nu corespunde acestor setări."),
                    style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }
            if (gentlerStart) Text(
                tr(lang, "A mai készenléti adat alapján az új terv két sorozattal indít.",
                    "Today's readiness suggests starting the new plan with two sets.",
                    "Auf Basis der heutigen Trainingsbereitschaft beginnt der neue Plan mit zwei Sätzen.",
                    "Pe baza recuperării de astăzi, planul nou începe cu două serii."),
                style = MaterialTheme.typography.bodySmall, color = palette.textSecondary
            )
            if (failed) Text(
                tr(lang, "A terv/profil mentése nem sikerült. Próbáld újra.",
                    "Failed to save plan/profile. Please try again.",
                    "Speichern von Plan/Profil fehlgeschlagen. Bitte erneut versuchen.",
                    "Salvarea planului/profilului a eșuat. Încearcă din nou."),
                color = com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeForeground,
                modifier = Modifier.testTag("planner_save_error")
            )
            com.repforge.app.ui.theme.TrainPilotSaveButton(
                enabled = !saving,
                onClick = {
                    onSaveAndOpenPlan(
                        heightCm,
                        weightKg,
                        selectedGoal,
                        selectedExperience,
                        selectedLocation,
                        selectedEquipment,
                        selectedDays,
                        selectedDuration,
                        excludedMuscles,
                        excludedExercises
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp).testTag("planner_save_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    tr(
                        lang,
                        "Új terv készítése",
                        "Create a new plan",
                        "Neuen Plan erstellen",
                        "Creează un plan nou"
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

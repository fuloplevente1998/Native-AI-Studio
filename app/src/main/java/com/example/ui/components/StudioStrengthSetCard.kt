package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.ExerciseSetItem
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.TrainPilotNegativeButton
import com.repforge.app.ui.theme.TrainPilotFrames
import java.text.NumberFormat
import java.util.Locale

/** Presentation only: values and completion still use the existing canonical callbacks. */
@Composable
internal fun StudioStrengthSetCard(
    item: ExerciseSetItem,
    identity: String,
    language: AppLanguage,
    weightLabel: String,
    repsLabel: String,
    enabled: Boolean,
    active: Boolean,
    onValues: (Float, Int, Int?) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val p = LocalTrainPilotPalette.current
    fun tr(hu: String, en: String, de: String, ro: String) = when (language) {
        AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
    }
    val number = remember(language) { NumberFormat.getNumberInstance(Locale.forLanguageTag(language.name.lowercase())).apply {
        maximumFractionDigits = 6; isGroupingUsed = false
    } }
    val selectedRir = item.rir.takeIf { item.actualRirProvided }
    val weights = remember(item.weightKg) {
        // Suggestions do not round or replace unusual imported/previous weights.
        ((0..80).map { it / 2f } + listOf(50f, 60f, 70f, 80f, 100f, 120f, 150f, 200f, item.weightKg))
            .distinct().sorted().map { it.toString() }
    }
    val shape = RoundedCornerShape(14.dp)
    Surface(
        color = p.surfaceElevated, shape = shape,
        border = if (active) TrainPilotFrames.page(p.cardBorder, p.primary)
            else BorderStroke(0.75.dp, p.cardBorder.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth().testTag("active_strength_set_${item.setNumber}")
    ) {
        BoxWithConstraints(Modifier.padding(6.dp)) {
            val availableWidth = maxWidth
            val singleRow = availableWidth >= 324.dp && LocalDensity.current.fontScale <= 1.3f
                val fields: @Composable (Modifier) -> Unit = { field ->
                    WorkoutSetValuePicker(identity + "/weight", weightLabel, item.weightKg.toString(), weights,
                        enabled, language, "active_set_weight_${item.setNumber}", field,
                        display = { it.toDoubleOrNull()?.let { n -> number.format(n) } ?: it },
                        customValid = { it.toFloatOrNull()?.let { n -> n.isFinite() && n >= 0f } == true },
                        onSelect = { onValues(it.toFloat(), item.reps, selectedRir) })
                    WorkoutSetValuePicker(identity + "/reps", repsLabel, item.reps.toString(),
                        ((1..100).map(Int::toString) + item.reps.toString()).distinct(), enabled, language,
                        "active_set_reps_${item.setNumber}", field,
                        onSelect = { onValues(item.weightKg, it.toInt(), selectedRir) })
                    WorkoutSetValuePicker(identity + "/rir", "RIR", selectedRir?.toString() ?: "—",
                        listOf("—") + (0..10).map(Int::toString), enabled, language,
                        "active_set_rir_${item.setNumber}", field,
                        onSelect = { onValues(item.weightKg, item.reps, it.toIntOrNull()) })
                }

            val actions: @Composable () -> Unit = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                val toggleShape = RoundedCornerShape(12.dp)
                Surface(
                    shape = toggleShape,
                    color = if (item.completed) p.success.copy(alpha = 0.18f) else p.surface,
                    border = BorderStroke(1.dp, if (item.completed) p.success else p.cardBorder),
                    modifier = Modifier.size(48.dp).clip(toggleShape)
                        .toggleable(value = item.completed, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
                        .semantics { contentDescription = tr("Sorozat teljesítve", "Set completed", "Satz abgeschlossen", "Serie finalizată") }
                        .testTag("toggle_set_${item.setNumber}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null,
                            tint = (if (item.completed) p.success else p.textSecondary)
                                .copy(alpha = if (enabled) 1f else 0.5f),
                            modifier = Modifier.size(22.dp))
                    }
                }
                TrainPilotNegativeButton(onClick = onDelete, enabled = enabled,
                    modifier = Modifier.size(48.dp).testTag("delete_set_${item.setNumber}"),
                    contentPadding = PaddingValues(8.dp)) { Text("−") }
                }
            }
            if (singleRow) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${item.setNumber}.", color = p.textPrimary, style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp)
                            .semantics { contentDescription = "${item.setNumber}. " + tr("sorozat", "set", "Satz", "serie") })
                    fields(Modifier.weight(1f))
                    actions()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${item.setNumber}. " + tr("sorozat", "set", "Satz", "serie"),
                            modifier = Modifier.weight(1f), color = p.textPrimary, fontWeight = FontWeight.Bold)
                        actions()
                    }
                    if (availableWidth >= 270.dp && LocalDensity.current.fontScale <= 1.5f)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { fields(Modifier.weight(1f)) }
                    else Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { fields(Modifier.fillMaxWidth()) }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSetValuePicker(
    identity: String, label: String, value: String, choices: List<String>, enabled: Boolean,
    language: AppLanguage, tag: String, modifier: Modifier = Modifier,
    display: (String) -> String = { it }, customValid: ((String) -> Boolean)? = null,
    onSelect: (String) -> Unit
) {
    val p = LocalTrainPilotPalette.current
    var expanded by remember(identity, enabled) { mutableStateOf(false) }
    var custom by remember(identity, enabled) { mutableStateOf(false) }
    var draft by remember(identity, value) { mutableStateOf(value) }
    fun tr(hu: String, en: String, de: String, ro: String) = when (language) {
        AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
    }
    val canonical = draft.trim().replace(',', '.')
    val valid = customValid?.invoke(canonical) == true
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Surface(color = p.surface, shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, p.cardBorder),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled, role = Role.Button) { custom = false; expanded = true }
                    .testTag(tag)) {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(label, color = p.textSecondary, style = MaterialTheme.typography.labelSmall,
                            maxLines = 1)
                        Text(display(value), color = p.textPrimary.copy(alpha = if (enabled) 1f else 0.5f),
                            fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Icon(Icons.Default.ExpandMore, null, tint = p.primary, modifier = Modifier.size(14.dp))
                }
            }
            val density = LocalDensity.current
            val scroll = rememberScrollState()
            LaunchedEffect(expanded, value) {
                if (expanded && customValid == null) scroll.scrollTo(with(density) { (choices.indexOf(value).coerceAtLeast(0) * 48).dp.roundToPx() })
            }
            DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false },
                scrollState = scroll, modifier = Modifier.heightIn(max = 300.dp).background(p.surface).testTag("${tag}_menu")) {
                if (customValid != null) DropdownMenuItem(
                    text = { Text(tr("Egyedi érték…", "Custom value…", "Eigener Wert…", "Valoare personalizată…"), color = p.primary) },
                    modifier = Modifier.testTag("${tag}_custom"),
                    onClick = { expanded = false; draft = value; custom = true })
                choices.forEach { option ->
                    DropdownMenuItem(text = { Text(display(option), color = if (option == value) p.primary else p.textPrimary,
                        fontWeight = if (option == value) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("${tag}_option_$option"),
                        onClick = { expanded = false; onSelect(option) })
                }

            }
        }
        if (custom && enabled) {
            OutlinedTextField(value = draft, onValueChange = { draft = it }, singleLine = true,
                isError = !valid, modifier = Modifier.fillMaxWidth().testTag("${tag}_custom_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid) { onSelect(canonical); custom = false } }))
            TextButton(onClick = { if (valid) { onSelect(canonical); custom = false } }, enabled = valid,
                modifier = Modifier.testTag("${tag}_custom_apply")) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Text(tr("Beállít", "Apply", "Übernehmen", "Aplică"))
            }
        }
    }
}

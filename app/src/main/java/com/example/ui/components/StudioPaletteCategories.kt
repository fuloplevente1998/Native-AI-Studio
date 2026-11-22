package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.PaletteCategory
import com.example.ui.theme.TrainPilotPalettes
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotClickable

private val families = listOf(PaletteCategory.MATTE, PaletteCategory.CONTRAST, PaletteCategory.GLOW, PaletteCategory.LIGHT)

@Composable
internal fun StudioPaletteCategories(
    language: AppLanguage,
    selectedId: Int,
    expanded: PaletteCategory?,
    onExpand: (PaletteCategory?) -> Unit,
    onSelect: (Int) -> Unit,
    choiceTag: String = "settings_palette_choice_"
) {
    val palette = LocalTrainPilotPalette.current
    val titles = when (language) {
        AppLanguage.HU -> listOf("Matt", "Kontrasztos", "Fényes", "Világos")
        AppLanguage.EN -> listOf("Matte", "Contrast", "Glow", "Light")
        AppLanguage.DE -> listOf("Matt", "Kontrast", "Leuchtend", "Hell")
        AppLanguage.RO -> listOf("Mat", "Contrast", "Strălucitor", "Luminos")
    }
    val count = when (language) {
        AppLanguage.HU -> "8 téma"
        AppLanguage.EN -> "8 themes"
        AppLanguage.DE -> "8 Themen"
        AppLanguage.RO -> "8 teme"
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        families.forEachIndexed { index, family ->
            val open = family == expanded
            val shape = RoundedCornerShape(14.dp)
            Surface(shape = shape, color = palette.surfaceElevated,
                modifier = Modifier.fillMaxWidth().trainPilotBorder(1.dp, palette.cardBorder, shape)
                    .testTag("settings_theme_category_$index")) {
                Column {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .trainPilotClickable(shape) { onExpand(if (open) null else family) }
                        .semantics {
                            stateDescription = when (language) {
                                AppLanguage.HU -> if (open) "Nyitva" else "Csukva"
                                AppLanguage.EN -> if (open) "Expanded" else "Collapsed"
                                AppLanguage.DE -> if (open) "Ausgeklappt" else "Eingeklappt"
                                AppLanguage.RO -> if (open) "Extins" else "Restrâns"
                            }
                        }
                        .testTag("settings_theme_category_toggle_$index")
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(titles[index], Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(count, color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
                        Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = palette.primary)
                    }
                    if (open) TrainPilotPalettes.allPalettes.filter { it.subGroup == family }.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                            row.forEach { option ->
                                val active = selectedId == option.id
                                Box(Modifier.weight(1f).height(48.dp)
                                    .trainPilotClickable(CircleShape) { onSelect(option.id) }
                                    .semantics {
                                        role = Role.RadioButton
                                        selected = active
                                        contentDescription = option.localizedName(language)
                                    }.testTag("$choiceTag${option.id}"), contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(34.dp).then(if (active)
                                        Modifier.border(1.dp, palette.textPrimary, CircleShape) else Modifier),
                                        contentAlignment = Alignment.Center) {
                                        val glow = if (option.hasGlow) Modifier.border(1.dp,
                                            option.primary.copy(alpha = 0.25f), CircleShape).padding(2.dp) else Modifier
                                        Box(Modifier.size(29.dp).then(glow)
                                            .background(option.primary, CircleShape)
                                            .border(1.dp, option.secondary, CircleShape), contentAlignment = Alignment.Center) {
                                            if (active) Icon(Icons.Default.Check, null, Modifier.size(15.dp), tint = option.onPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

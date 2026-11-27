package com.example.ui.components

import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.glassTile
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.TrainPilotPalette
import com.example.ui.theme.glassReadable
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

internal fun paletteCategoryTitleColor(palette: TrainPilotPalette): Color =
    if (palette.isLiquidGlass) glassReadable(palette.textPrimary) else Color.Unspecified

private val families = listOf(PaletteCategory.MATTE, PaletteCategory.CONTRAST, PaletteCategory.GLOW, PaletteCategory.LIGHT, PaletteCategory.LIQUID_GLASS)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun StudioPaletteCategories(
    language: AppLanguage,
    selectedId: Int,
    expanded: PaletteCategory?,
    onExpand: (PaletteCategory?) -> Unit,
    onSelect: (Int) -> Unit,
    choiceTag: String = "settings_palette_choice_",
    revealSelectedId: Int? = null,
    onSelectionRevealed: () -> Unit = {}
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
            // 2811/5: glass = translucent tile + Tile rim inside the glass Themes panel (legacy unchanged).
            Surface(shape = shape, color = palette.glassTile(palette.surfaceElevated, elevated = open),
                modifier = Modifier.fillMaxWidth().trainPilotBorder(1.dp, palette.cardBorder, shape)
                    .trainPilotGlassRim(palette, shape, com.example.ui.theme.GlassMaterialRole.Tile)
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
                        Text(if (family == PaletteCategory.LIQUID_GLASS) "Liquid Glass" else titles[index],
                            Modifier.weight(1f), fontWeight = FontWeight.Bold, color = paletteCategoryTitleColor(palette))
                        Text(count, color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
                        Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = palette.primary)
                    }
                    if (open) TrainPilotPalettes.selectablePalettes.filter { it.subGroup == family }.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                            row.forEach { option ->
                                val active = selectedId == option.id
                                val requester = remember { BringIntoViewRequester() }
                                if (active && revealSelectedId == option.id && palette.id == option.id) {
                                    LaunchedEffect(option.id, palette.isLiquidGlass) {
                                        withFrameNanos { }
                                        requester.bringIntoView()
                                        onSelectionRevealed()
                                    }
                                }
                                Box(Modifier.weight(1f).height(48.dp)
                                    .bringIntoViewRequester(requester)
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
                    if (open && family == PaletteCategory.LIQUID_GLASS) LiquidGlassControl(language, selectedId)
                }
            }
        }
    }
}

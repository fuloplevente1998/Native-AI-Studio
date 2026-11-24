package com.example.ui.components

import com.repforge.app.ui.theme.trainPilotClickable

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.selectionColors
import com.example.ui.theme.readablePrimaryLabel
import androidx.compose.runtime.remember
import com.example.ui.theme.PaletteCategory
import com.example.ui.theme.TrainPilotPalettes

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Language
import androidx.compose.ui.semantics.contentDescription
import com.example.localization.LocalTrainPilotStrings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PalettePickerBottomSheet(
    selectedPaletteId: Int,
    selectedCategory: PaletteCategory?,
    currentLanguage: AppLanguage,
    fontScaleMultiplier: Float,
    compactPhoneMode: Boolean,
    onSelectPalette: (Int) -> Unit,
    onSelectCategory: (PaletteCategory?) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectFontScale: (Float) -> Unit,
    onToggleCompactMode: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val selection = remember(palette) { palette.selectionColors() }
    val strings = LocalTrainPilotStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expandedFamily by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf((selectedCategory?.takeIf { it in setOf(PaletteCategory.MATTE, PaletteCategory.CONTRAST, PaletteCategory.GLOW, PaletteCategory.LIGHT, PaletteCategory.LIQUID_GLASS) }
            ?: palette.subGroup).name)
    }

    TrainPilotModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = palette.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = customThemeText(currentLanguage, "Témák", "Themes", "Designs", "Teme"),
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        Text(
                            text = strings.fontSizePreviewLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
                TrainPilotRedCloseButton(
                    onClick = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Language & Accessibility Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppLanguage.entries.forEach { lang ->
                    val active = currentLanguage == lang
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) palette.primary else palette.surfaceElevated,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .trainPilotClickable(RoundedCornerShape(10.dp)) { onSelectLanguage(lang) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${lang.flag} ${lang.code}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) palette.readablePrimaryLabel() else palette.textPrimary
                            )
                        }
                    }
                }

                listOf(
                    1.0f to "100%",
                    1.15f to "115%",
                    1.28f to "130%",
                    2.0f to "200% (2.0x)"
                ).forEach { (scale, label) ->
                    val active = kotlin.math.abs(fontScaleMultiplier - scale) < 0.03f
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) selection.surface else palette.surfaceElevated,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .trainPilotBorder(
                                width = 1.dp,
                                color = if (active) selection.border else palette.cardBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .trainPilotClickable(RoundedCornerShape(10.dp)) { onSelectFontScale(scale) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) selection.foreground else palette.textSecondary
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (compactPhoneMode) selection.surface else palette.surfaceElevated,
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .trainPilotBorder(
                            width = 1.dp,
                            color = if (compactPhoneMode) selection.border else palette.cardBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .trainPilotClickable(RoundedCornerShape(10.dp)) { onToggleCompactMode(!compactPhoneMode) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (compactPhoneMode) "📱 320dp: ON" else "📱 320dp: OFF",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (compactPhoneMode) selection.foreground else palette.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            StudioPaletteCategories(currentLanguage, if (palette.id == -1) -1 else selectedPaletteId,
                PaletteCategory.entries.firstOrNull { it.name == expandedFamily },
                onExpand = { expandedFamily = it?.name.orEmpty(); onSelectCategory(it) },
                onSelect = onSelectPalette, choiceTag = "palette_option_")
            CustomThemeSection(currentLanguage)
        }
    }
}

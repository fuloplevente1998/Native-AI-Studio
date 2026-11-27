package com.example.ui.screens

import com.example.ui.theme.liquidDropMaterial

import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassControlFill
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.trainPilotPageBackdrop
import com.example.ui.theme.LocalGlassTopInset
import com.example.ui.theme.GlassScrollChrome
import com.example.ui.theme.glassHeaderMaterial
import com.example.ui.components.PageLayoutBlock
import com.example.ui.components.PageLayoutFrame
import com.example.ui.components.PageLayoutFooter
import com.example.ui.components.pageFooterArrangement
import com.example.ui.components.rememberPageLayout
import androidx.compose.runtime.key
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.components.trainPilotChartSelection

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.BarChart
import com.example.localization.LocalizedStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.selectionColors
import androidx.compose.runtime.remember
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * 4. FEJLŐDÉS – natív fejlődés, napló és statisztikák
 * Minden fő kártya ráfér a képernyőre, és koppintásra lenyílik / becsukódik!
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressTabScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onSelectMainSection: (Int) -> Unit,
    onSelectExercise: (String) -> Unit,
    onSelectRange: (Int) -> Unit,
    onSelectVolumeBar: (Int) -> Unit,
    onSelectExpandedStatsCard: (Int) -> Unit,
    onSelectStatsSubTab: (Int) -> Unit,
    onToggleCardioFilterStepsMode: (Boolean) -> Unit,
    onOpenWorkoutDetail: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPalettePicker: () -> Unit,
    onRefreshRecords: () -> Unit = {},
    onOpenPhotos: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val statisticsLabel = com.repforge.app.ui.text.nativeResources(androidx.compose.ui.platform.LocalContext.current,
        uiState.language.name.lowercase()).getString(com.repforge.app.R.string.journal_statistics)
    val sections = remember(strings.navProgress, statisticsLabel) { listOf(0 to strings.navProgress, 2 to statisticsLabel) }
    val activeSection = if (uiState.progressMainSection == 2) 2 else 0
    val selection = remember(palette) { palette.selectionColors() }
    val exerciseStatistics = activeSection == 2 && uiState.statsSubTab == 0

    val layoutBlocks = remember(uiState.language, strings, statisticsLabel) { listOf(
        PageLayoutBlock("photos", trL(uiState.language, "Fotógaléria", "Photo gallery", "Fotogalerie", "Galerie foto")),
        PageLayoutBlock("volume", strings.totalVolumeTitle),
        PageLayoutBlock("metrics", statisticsLabel),
        PageLayoutBlock("muscles", strings.muscleLoadTitle),
        PageLayoutBlock("advice", strings.progressAdviceTitle)
    ) }
    val blockIds = remember(layoutBlocks) { layoutBlocks.map { it.id } }
    val layout = rememberPageLayout("progress", blockIds)
    val overviewListState = rememberLazyListState()
    PageLayoutFrame(layout, uiState.language, layoutBlocks, overviewListState, 3.dp) {
    GlassScrollChrome(
        modifier = Modifier.fillMaxSize().trainPilotPageBackdrop(palette).testTag("progress_tab_screen"),
        enabled = layout.draft == null && !exerciseStatistics,
        rootInset = LocalGlassTopInset.current,
        top = {
        if (layout.draft == null) Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.cardPaint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .then(if (exerciseStatistics) Modifier.trainPilotGlassMaterial(palette, RoundedCornerShape(14.dp))
                    else Modifier.glassHeaderMaterial(palette, RoundedCornerShape(14.dp)))
                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sections.forEach { (idx, label) ->
                    val selected = activeSection == idx
                    // Shorter painted tab (42 dp), without making the tap target shorter than 48 dp.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .trainPilotClickable(RoundedCornerShape(10.dp)) { onSelectMainSection(idx) }
                            .then(Modifier.trainPilotChartSelection(selected, label))
                            .testTag("progress_section_tab_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected && !palette.isLiquidGlass) selection.surface else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .then(if (selected) Modifier.liquidDropMaterial(palette, RoundedCornerShape(10.dp), GlassMaterialRole.Selection,
                                    accent = palette.primary, live = false) else Modifier)
                                .trainPilotBorder(
                                    width = if (selected) 1.dp else 0.dp,
                                    color = if (selected) selection.border else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .testTag("progress_section_visual_$idx")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = if (idx == 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = if (selected) selection.foreground else palette.textSecondary,
                                    modifier = Modifier.size(18.dp).testTag("progress_section_icon_$idx")
                                )
                                com.example.ui.components.TrainPilotFitText(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) selection.foreground else palette.textSecondary,
                                    modifier = Modifier.weight(1f).testTag("progress_section_label_$idx")
                                )
                            }
                        }
                    }
                }
            }
        }

        }
    ) { scrollModifier, topInset, bottomInset ->
        when (activeSection) {
            // 1. AL-OLDAL (0): 📈 FEJLŐDÉS (Összes volumen, 4 lenyíló statisztika ablak, Izomcsoport-terhelés, Rekordok & Kardió)
            0 -> {
                LazyColumn(
                    state = overviewListState,
                    modifier = scrollModifier.testTag("progress_overview_list"),
                    contentPadding = PaddingValues(start = TrainPilotCardSpacing.ScreenGutter, end = TrainPilotCardSpacing.ScreenGutter, top = topInset, bottom = 10.dp + bottomInset),
                    verticalArrangement = remember(layout.draft == null) { pageFooterArrangement(2.dp, layout.draft == null) }
                ) {
                    item(key = "progress_overview", contentType = "overview") {
                        MainProgressVolumeAndMuscleSection(
                            uiState = uiState,
                            onSelectRange = onSelectRange,
                            onSelectVolumeBar = onSelectVolumeBar,
                            onSelectExpandedStatsCard = onSelectExpandedStatsCard,
                            onOpenWorkoutDetail = onOpenWorkoutDetail,
                            layout = layout,
                            photoBlock = {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.cardPaint,
                                    modifier = Modifier
                                        .fillMaxWidth().trainPilotGlassMaterial(palette, RoundedCornerShape(16.dp))
                                        .heightIn(min = 48.dp)
                                        .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                                        .trainPilotClickable(RoundedCornerShape(16.dp)) { onOpenPhotos() }
                                        .testTag("progress_open_private_photos")
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = palette.primary, modifier = Modifier.size(20.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                trL(uiState.language, "Fotógaléria", "Photo gallery", "Fotogalerie", "Galerie foto"),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.textPrimary
                                            )
                                        }
                                        Text(
                                            "${uiState.privatePhotoCount}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.primary,
                                            modifier = Modifier.testTag("progress_private_photos_count")
                                        )
                                    }
                                }
                            }
                        )
                    }
                    if (layout.draft == null) item(key = "customize", contentType = "footer") {
                        PageLayoutFooter(layout, uiState.language)
                    }
                }
            }

            // Statistics replaces the old standalone Strength/1RM tab; estimates remain in record details.
            else -> if (layout.draft == null) StudioStatisticsScreen(uiState, onSelectStatsSubTab,
                onToggleCardioFilterStepsMode, onRefreshRecords, scrollModifier = scrollModifier, topInset = topInset)
        }
    }
    }
}

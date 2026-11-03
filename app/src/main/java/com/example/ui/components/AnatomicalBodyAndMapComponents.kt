package com.example.ui.components

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.components.mapcontrols.MapAppearanceSelector

data class MuscleLoadItem(
    val id: String,
    val nameHu: String,
    val setsText: String,
    val percentage: Int,
    val ratio: Float,
    val levelLabelHu: String,
    val topExercisesHu: List<Pair<String, String>>, // ExerciseName to sets/load
    val exerciseIdToOpen: String,
    val setsCount: Int = 0
)

/**
 * Compact Side-by-Side Front & Back Anatomical Muscle Heatmap ("Izomcsoport-terhelés")
 * Matches the `main` app screenshot (`Screenshot_2026-10-27-15-00-06-481_com.repforge.app.jpg`):
 * - Left: Dual Front & Back Anatomical Muscular Figures
 * - Right: 6 horizontal progress bars (Mell 45%, Hát 66%, Váll 59%, Láb 75%, Kar 100%, Core 61%)
 * - Bottom: Kisebb / Közepes / Nagyobb legend + explanatory caption
 * - Tapping any muscle bar or period pill expands/updates the details inline!
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnatomicalMuscleHeatmapCard(
    title: String = "Izomcsoport-terhelés",
    subtitle: String = "A színek és arányok az időszak legterheltebb izomcsoportjához viszonyítanak, nem fáradtságot mérnek.",
    highlightedMuscleId: String? = null,
    realMuscleLoads: List<MuscleLoadItem> = emptyList(),
    onPeriodSelected: ((Int) -> Unit)? = null,
    periodDays: Int? = 90,
    onOpenExerciseDetail: ((String) -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    val periodOptions = remember { listOf("3 hó", "30 nap", "7 nap", "1 nap") }
    val periodDayOptions = remember { listOf(90, 30, 7, 1) }
    val selectedPeriodIdx = periodDayOptions.indexOf(periodDays ?: 90).coerceAtLeast(0)

    val muscleLoads = remember(realMuscleLoads) {
        if (realMuscleLoads.isNotEmpty()) {
            realMuscleLoads
        } else {
            listOf(
                MuscleLoadItem("chest", "Mell", "0 sorozat", 0, 0f, "Nincs terhelés", emptyList(), ""),
                MuscleLoadItem("back", "Hát", "0 sorozat", 0, 0f, "Nincs terhelés", emptyList(), ""),
                MuscleLoadItem("shoulders", "Váll", "0 sorozat", 0, 0f, "Nincs terhelés", emptyList(), ""),
                MuscleLoadItem("legs", "Láb", "0 sorozat", 0, 0f, "Nincs terhelés", emptyList(), ""),
                MuscleLoadItem("arms", "Kar", "0 sorozat", 0, 0f, "Nincs terhelés", emptyList(), ""),
                MuscleLoadItem("core", "Törzs", "0 sorozat", 0, 0f, "Nincs terhelés", emptyList(), "")
            )
        }
    }

    var selectedMuscleId by remember(highlightedMuscleId) {
        mutableStateOf(highlightedMuscleId ?: "all")
    }

    val selectedMuscleObj = muscleLoads.find { it.id == selectedMuscleId }
    var showExplanation by remember { mutableStateOf(false) }

    val smallHeatColor = Color(0xFFE5B842)   // Yellow-Gold (Kisebb)
    val mediumHeatColor = Color(0xFFE68A2E)  // Warm Orange (Közepes)
    val highHeatColor = Color(0xFFE25555)    // Coral Red (Nagyobb)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            .testTag("anatomical_muscle_heatmap_card")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header row: "Izomcsoport-terhelés" + "3 hó" pill button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                if (onPeriodSelected != null && periodDays != null) Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onPeriodSelected(periodDayOptions[(selectedPeriodIdx + 1) % periodOptions.size])
                        }
                        .testTag("muscle_heatmap_period_pill")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = periodOptions[selectedPeriodIdx],
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Side-by-Side Row: Left = Front & Back Anatomical Figures, Right = 6 Muscle Progress Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Left half: Dual Front & Back Anatomical Muscular Figures
                val baseSilhouette = Color(0xFF434A56)
                val outlineCol = Color(0xFF9EA7B8).copy(alpha = 0.55f)

                fun colorForMuscle(id: String, defaultHeat: Color): Color {
                    return if (selectedMuscleId == "all") {
                        defaultHeat
                    } else if (selectedMuscleId == id) {
                        highHeatColor
                    } else {
                        baseSilhouette.copy(alpha = 0.55f)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val nativeLoads = remember(muscleLoads) {
                        muscleLoads.map {
                            com.repforge.app.ui.progress.MuscleLoadUiModel(
                                muscleKey = it.id,
                                label = it.nameHu,
                                setsCount = it.setsCount,
                                loadPercentage = it.ratio
                            )
                        }
                    }
                    com.repforge.app.ui.progress.NativeMuscleLoadVisual(
                        loads = nativeLoads,
                        language = "hu",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Right half: 6 Compact Muscle Progress Bars (Mell, Hát, Váll, Láb, Kar, Core)
                Column(
                    modifier = Modifier.weight(0.58f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    muscleLoads.forEach { m ->
                        val isSel = selectedMuscleId == m.id
                        val barColor = when {
                            m.percentage >= 70 -> highHeatColor
                            m.percentage >= 40 -> mediumHeatColor
                            else -> smallHeatColor
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedMuscleId = if (selectedMuscleId == m.id) "all" else m.id
                                }
                                .padding(vertical = 1.dp)
                                .testTag("muscle_chip_${m.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = m.nameHu,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) palette.primary else palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(56.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(palette.surfaceElevated)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(m.ratio)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(barColor)
                                )
                            }
                            Text(
                                text = "${m.percentage}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) palette.primary else palette.textSecondary,
                                modifier = Modifier.width(34.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Legend Row: 🟡 Kisebb   🟠 Közepes   🔴 Nagyobb
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = smallHeatColor, label = "Kisebb")
                Spacer(modifier = Modifier.width(14.dp))
                LegendDot(color = mediumHeatColor, label = "Közepes")
                Spacer(modifier = Modifier.width(14.dp))
                LegendDot(color = highHeatColor, label = "Nagyobb")
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Keep the safety caveat visible; put the longer explanation behind a tap.
            Text(
                text = "Relatív sorozatterhelés, nem fáradtság · Részletek",
                modifier = Modifier.clickable { showExplanation = !showExplanation }
                    .testTag("muscle_explanation_toggle"),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showExplanation) Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                modifier = Modifier.testTag("muscle_explanation_text")
            )

            // Expandable Muscle Exercise Breakdown when user taps any muscle bar or body part
            if (selectedMuscleObj != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable { selectedMuscleId = "all" }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedMuscleObj.nameHu} • ${selectedMuscleObj.setsText} (${selectedMuscleObj.percentage}%)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                            if (onOpenExerciseDetail != null) {
                                Text(
                                    text = "Gyakorlat >",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary,
                                    modifier = Modifier.clickable {
                                        onOpenExerciseDetail(selectedMuscleObj.exerciseIdToOpen)
                                    }
                                )
                            } else {
                                Text(
                                    text = "Bezárás ✕",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        HorizontalDivider(color = palette.cardBorder)
                        selectedMuscleObj.topExercisesHu.forEach { (exName, exLoad) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• $exName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = exLoad,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val palette = LocalTrainPilotPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
    }
}

private fun DrawScope.drawAnatomicalFigure(
    cx: Float,
    h: Float,
    isFront: Boolean,
    baseColor: Color,
    outlineColor: Color,
    chestColor: Color,
    shoulderColor: Color,
    armColor: Color,
    coreColor: Color,
    legColor: Color,
    backColor: Color
) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

    // 1. Head & Neck
    drawCircle(
        color = baseColor,
        radius = h * 0.065f,
        center = Offset(cx, h * 0.10f)
    )
    drawCircle(
        color = outlineColor,
        radius = h * 0.065f,
        center = Offset(cx, h * 0.10f),
        style = stroke
    )
    // Neck / Upper Traps
    drawRoundRect(
        color = if (isFront) baseColor else backColor,
        topLeft = Offset(cx - h * 0.035f, h * 0.16f),
        size = Size(h * 0.07f, h * 0.04f),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // 2. Shoulders (Deltoids Left & Right)
    drawOval(
        color = shoulderColor,
        topLeft = Offset(cx - h * 0.175f, h * 0.19f),
        size = Size(h * 0.075f, h * 0.085f)
    )
    drawOval(
        color = shoulderColor,
        topLeft = Offset(cx + h * 0.10f, h * 0.19f),
        size = Size(h * 0.075f, h * 0.085f)
    )

    if (isFront) {
        // 3A. Front Chest (Pectorals)
        drawRoundRect(
            color = chestColor,
            topLeft = Offset(cx - h * 0.11f, h * 0.20f),
            size = Size(h * 0.102f, h * 0.095f),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawRoundRect(
            color = chestColor,
            topLeft = Offset(cx + h * 0.008f, h * 0.20f),
            size = Size(h * 0.102f, h * 0.095f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // 4A. Front Core / Abs (6-pack blocks)
        drawRoundRect(
            color = coreColor,
            topLeft = Offset(cx - h * 0.085f, h * 0.305f),
            size = Size(h * 0.17f, h * 0.165f),
            cornerRadius = CornerRadius(10f, 10f)
        )
        // Center linea alba & transverse lines
        drawLine(
            color = outlineColor,
            start = Offset(cx, h * 0.31f),
            end = Offset(cx, h * 0.46f),
            strokeWidth = 1.5.dp.toPx()
        )
    } else {
        // 3B. Back Lats & Rhomboids (V-Taper)
        val backPath = Path().apply {
            moveTo(cx - h * 0.115f, h * 0.20f)
            lineTo(cx + h * 0.115f, h * 0.20f)
            lineTo(cx + h * 0.08f, h * 0.42f)
            lineTo(cx - h * 0.08f, h * 0.42f)
            close()
        }
        drawPath(path = backPath, color = backColor)
        drawPath(path = backPath, color = outlineColor, style = stroke)

        // 4B. Glutes (Farizom - part of Legs/Posterior chain)
        drawRoundRect(
            color = legColor,
            topLeft = Offset(cx - h * 0.095f, h * 0.43f),
            size = Size(h * 0.09f, h * 0.085f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawRoundRect(
            color = legColor,
            topLeft = Offset(cx + h * 0.005f, h * 0.43f),
            size = Size(h * 0.09f, h * 0.085f),
            cornerRadius = CornerRadius(14f, 14f)
        )
    }

    // 5. Arms (Biceps/Triceps + Forearms)
    // Left Upper Arm
    drawRoundRect(
        color = armColor,
        topLeft = Offset(cx - h * 0.185f, h * 0.275f),
        size = Size(h * 0.06f, h * 0.11f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    // Right Upper Arm
    drawRoundRect(
        color = armColor,
        topLeft = Offset(cx + h * 0.125f, h * 0.275f),
        size = Size(h * 0.06f, h * 0.11f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    // Left Forearm
    drawRoundRect(
        color = armColor.copy(alpha = 0.8f),
        topLeft = Offset(cx - h * 0.20f, h * 0.395f),
        size = Size(h * 0.052f, h * 0.11f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Right Forearm
    drawRoundRect(
        color = armColor.copy(alpha = 0.8f),
        topLeft = Offset(cx + h * 0.148f, h * 0.395f),
        size = Size(h * 0.052f, h * 0.11f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // 6. Upper Legs (Quads / Hamstrings)
    drawRoundRect(
        color = legColor,
        topLeft = Offset(cx - h * 0.098f, h * 0.515f),
        size = Size(h * 0.086f, h * 0.205f),
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawRoundRect(
        color = legColor,
        topLeft = Offset(cx + h * 0.012f, h * 0.515f),
        size = Size(h * 0.086f, h * 0.205f),
        cornerRadius = CornerRadius(14f, 14f)
    )

    // 7. Lower Legs (Calves / Vádli)
    drawRoundRect(
        color = legColor.copy(alpha = 0.85f),
        topLeft = Offset(cx - h * 0.09f, h * 0.735f),
        size = Size(h * 0.072f, h * 0.19f),
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = legColor.copy(alpha = 0.85f),
        topLeft = Offset(cx + h * 0.018f, h * 0.735f),
        size = Size(h * 0.072f, h * 0.19f),
        cornerRadius = CornerRadius(12f, 12f)
    )
}

/**
 * High-detail Margitsziget / Danube / Margit híd GPS Route Map Canvas
 * Matching Mockup 3/4 #3 (`Edzésútvonal`) & Mockup 2/4 #4 (`Kültéri mérés`).
 */
/**
 * Shared saved-workout / live-outdoor GPS map.
 * Styling changes only the map presentation, not its recorded coordinates or tracking lifecycle.
 */
@Composable
fun NativeWorkoutRouteMapBox(
    modifier: Modifier = Modifier,
    interactiveControls: Boolean = true,
    routeKeys: List<String> = emptyList(),
    refreshToken: Int = 0,
    mapContentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
    showZoomControls: Boolean = true
) {
    val palette = LocalTrainPilotPalette.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) {
        com.repforge.app.data.store.TrainPilotSettingsStore(context.applicationContext)
    }
    val mapAppearance by preferences.mapAppearanceFlow.collectAsState(
        initial = com.repforge.app.data.store.MapAppearance.FOLLOW_APP)
    val darkMapMode = mapAppearance.isDark(palette.isDark)
    val preferenceScope = rememberCoroutineScope()
    val mapStyle = remember(context, darkMapMode) {
        if (darkMapMode) com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(
            context, com.repforge.app.R.raw.trainpilot_map_dark
        ) else null
    }
    // A different workout must never momentarily show the previous workout's private route.
    var segments by remember(routeKeys) {
        mutableStateOf<List<List<Pair<Double, Double>>>>(emptyList())
    }
    androidx.compose.runtime.LaunchedEffect(routeKeys, refreshToken) {
        segments = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.repforge.app.ui.journal.WorkoutRouteCoordinates.loadAll(context, routeKeys)?.segments.orEmpty()
                .map { segment -> segment.filter { (lat, lon) ->
                    lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0
                } }
        }
    }
    val points = remember(segments) {
        segments.flatten().map { (lat, lon) -> com.google.android.gms.maps.model.LatLng(lat, lon) }
    }
    val drawableSegments = remember(segments) {
        recordedPolylineSegments(segments).map { segment ->
            segment.map { (lat, lon) -> com.google.android.gms.maps.model.LatLng(lat, lon) }
        }
    }
    val endpoints = remember(segments) { recordedMapEndpoints(segments) }
    val configured = remember(context) { com.repforge.app.NativeMapsConfiguration.configured(context) }
    val playServicesAvailable = remember(context) {
        runCatching { com.google.android.gms.common.GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == com.google.android.gms.common.ConnectionResult.SUCCESS }.getOrDefault(false)
    }
    var loaded by remember { mutableStateOf(false) }
    var timedOut by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(configured, playServicesAvailable, retry) {
        loaded = false
        timedOut = false
        if (configured && playServicesAvailable) {
            kotlinx.coroutines.delay(15_000)
            if (!loaded) timedOut = true
        }
    }
    BoxWithConstraints(modifier.clip(RoundedCornerShape(24.dp)).background(palette.background)
        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
        // Native Google zoom/logo controls must fit between the actual map overlays.
        // On cramped windows gesture zoom remains enabled; avoid buried +/- buttons.
        val googleControlsHeight = maxHeight - mapContentPadding.calculateTopPadding() -
            mapContentPadding.calculateBottomPadding()
        if (!configured || !playServicesAvailable) {
            Text(if (!configured) "Google Maps nincs beállítva. Az útvonal helyben megmarad."
                else "A térképhez Google Play-szolgáltatások szükségesek. Az útvonal helyben megmarad.",
                style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                modifier = Modifier.padding(12.dp))
        } else {
            androidx.compose.runtime.key(retry) {
                val camera = com.google.maps.android.compose.rememberCameraPositionState()
                // Only actual recorded coordinates move the camera. No synthetic GPS location.
                androidx.compose.runtime.LaunchedEffect(loaded, points.firstOrNull()) {
                    if (loaded) points.firstOrNull()?.let { point ->
                        camera.move(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(point, 16f))
                    }
                }
                com.google.maps.android.compose.GoogleMap(
                    modifier = Modifier.fillMaxSize(), cameraPositionState = camera,
                    properties = com.google.maps.android.compose.MapProperties(mapStyleOptions = mapStyle),
                    contentPadding = mapContentPadding,
                    onMapLoaded = { loaded = true; timedOut = false },
                    uiSettings = com.google.maps.android.compose.MapUiSettings(
                        zoomControlsEnabled = interactiveControls && showZoomControls && googleControlsHeight >= 180.dp,
                        scrollGesturesEnabled = interactiveControls,
                        zoomGesturesEnabled = interactiveControls, rotationGesturesEnabled = interactiveControls)
                ) {
                    // Distinct paused/ownership segments are never connected by a synthetic line.
                    drawableSegments.forEach { segment ->
                        com.google.maps.android.compose.Polyline(points = segment, color = palette.primary, width = 8f)
                    }
                    endpoints?.let { recorded ->
                        val startPoint = com.google.android.gms.maps.model.LatLng(
                            recorded.start.first, recorded.start.second)
                        com.google.maps.android.compose.Marker(
                            state = com.google.maps.android.compose.MarkerState(position = startPoint),
                            title = if (recorded.finish == null) "Rajt és cél" else "Rajt",
                            icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)
                        )
                        recorded.finish?.let { (lat, lon) ->
                            com.google.maps.android.compose.Marker(
                                state = com.google.maps.android.compose.MarkerState(
                                    position = com.google.android.gms.maps.model.LatLng(lat, lon)),
                                title = "Cél",
                                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE)
                            )
                        }
                    }
                }
            }
            MapAppearanceSelector(
                appearance = mapAppearance,
                onSelect = { selected -> preferenceScope.launch { preferences.setMapAppearance(selected) } },
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(top = mapContentPadding.calculateTopPadding() + 8.dp, end = 8.dp)
            )
            if (!loaded) {
                Surface(color = palette.surfaceCard.copy(alpha = 0.94f), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (timedOut) "A térkép nem töltődött be. Ellenőrizd az internetet és a Maps-kulcs engedélyezését."
                            else "Google Maps betöltése…", style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary)
                        if (timedOut) TextButton(onClick = { retry += 1 }) { Text("Újrapróbálás") }
                    }
                }
            }
        }
    }
}

@Composable
fun ExercisePostureMiniIllustration(
    exerciseNameOrId: String,
    modifier: Modifier = Modifier.size(54.dp)
) {
    val palette = LocalTrainPilotPalette.current
    val lower = exerciseNameOrId.lowercase()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceElevated,
        modifier = modifier.trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
    ) {
        val primaryGold = palette.primary
        val muscleOrange = Color(0xFFF97316)
        val bodySlate = Color(0xFFCBD5E1)
        val headSlate = Color(0xFF94A3B8)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeBody = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val strokeMuscle = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

            when {
                // 1. SQUAT / GOBLET SQUAT / GUGGOLÁS (Squatting posture + highlighted quads/glutes)
                lower.contains("guggol") || lower.contains("squat") || lower.contains("lábnyom") -> {
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.09f, center = Offset(w * 0.48f, h * 0.22f))
                    // Torso leaning slightly forward
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.48f, h * 0.31f),
                        end = Offset(w * 0.36f, h * 0.56f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Highlighted Quads & Glutes (horizontal thigh in squat position)
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.36f, h * 0.56f),
                        end = Offset(w * 0.62f, h * 0.58f),
                        strokeWidth = strokeMuscle.width,
                        cap = StrokeCap.Round
                    )
                    // Lower leg (shin)
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.62f, h * 0.58f),
                        end = Offset(w * 0.56f, h * 0.84f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Arms holding Kettlebell / Dumbbell at chest (Goblet)
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.46f, h * 0.36f),
                        end = Offset(w * 0.64f, h * 0.42f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = primaryGold, radius = w * 0.08f, center = Offset(w * 0.68f, h * 0.42f))
                    // Up/down motion arrow on left
                    drawLine(
                        color = primaryGold,
                        start = Offset(w * 0.18f, h * 0.28f),
                        end = Offset(w * 0.18f, h * 0.72f),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 2. PUSH-UP / BENCH PRESS / FEKVŐTÁMASZ / FEKVENYOMÁS (Horizontal push posture + chest/triceps highlight)
                lower.contains("fekvő") || lower.contains("fekvenyom") || lower.contains("push") || lower.contains("bench") || lower.contains("mell") -> {
                    // Floor / Bench reference line
                    drawLine(
                        color = headSlate.copy(alpha = 0.35f),
                        start = Offset(w * 0.14f, h * 0.76f),
                        end = Offset(w * 0.86f, h * 0.76f),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.085f, center = Offset(w * 0.76f, h * 0.40f))
                    // Inclined Push-up Body line (shoulders to feet)
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.68f, h * 0.44f),
                        end = Offset(w * 0.18f, h * 0.72f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Highlighted Chest & Anterior Shoulder
                    drawCircle(color = muscleOrange, radius = w * 0.095f, center = Offset(w * 0.63f, h * 0.47f))
                    // Pushing arm to floor
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.65f, h * 0.47f),
                        end = Offset(w * 0.66f, h * 0.75f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                }

                // 3. ROW / PULL / EVEZÉS / HÚZÓDZKODÁS / FELHÚZÁS (Hinged back posture + lats/upper back highlight)
                lower.contains("evez") || lower.contains("row") || lower.contains("hát") || lower.contains("húzó") || lower.contains("felhúz") || lower.contains("deadlift") -> {
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.085f, center = Offset(w * 0.66f, h * 0.26f))
                    // Hinged Torso (Highlighted Back / Lats)
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.60f, h * 0.33f),
                        end = Offset(w * 0.32f, h * 0.54f),
                        strokeWidth = strokeMuscle.width,
                        cap = StrokeCap.Round
                    )
                    // Bent Legs
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.32f, h * 0.54f),
                        end = Offset(w * 0.42f, h * 0.82f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Pulling arm + band/dumbbell
                    drawLine(
                        color = primaryGold,
                        start = Offset(w * 0.54f, h * 0.38f),
                        end = Offset(w * 0.76f, h * 0.56f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = primaryGold, radius = w * 0.065f, center = Offset(w * 0.76f, h * 0.58f))
                }

                // 4. LUNGE / KITÖRÉS / SPLIT SQUAT (Split-stance lunge posture)
                lower.contains("kitör") || lower.contains("lunge") || lower.contains("bolgár") -> {
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.085f, center = Offset(w * 0.50f, h * 0.20f))
                    // Upright Torso
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.50f, h * 0.29f),
                        end = Offset(w * 0.50f, h * 0.54f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Front Lunge Leg (Highlighted Quad/Glute)
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.50f, h * 0.54f),
                        end = Offset(w * 0.72f, h * 0.56f),
                        strokeWidth = strokeMuscle.width,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.72f, h * 0.56f),
                        end = Offset(w * 0.72f, h * 0.82f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Rear Knee-down Leg
                    drawLine(
                        color = muscleOrange.copy(alpha = 0.85f),
                        start = Offset(w * 0.50f, h * 0.54f),
                        end = Offset(w * 0.34f, h * 0.78f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.34f, h * 0.78f),
                        end = Offset(w * 0.18f, h * 0.80f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                }

                // 5. PLANK / OLDALSÓ PLANK / CORE / HASPRÉS (Core stability posture + glowing abs/obliques)
                lower.contains("plank") || lower.contains("core") || lower.contains("has") || lower.contains("dead bug") -> {
                    // Floor line
                    drawLine(
                        color = headSlate.copy(alpha = 0.35f),
                        start = Offset(w * 0.12f, h * 0.76f),
                        end = Offset(w * 0.88f, h * 0.76f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.08f, center = Offset(w * 0.78f, h * 0.48f))
                    // Forearm support on floor
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.68f, h * 0.52f),
                        end = Offset(w * 0.68f, h * 0.74f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.68f, h * 0.74f),
                        end = Offset(w * 0.80f, h * 0.74f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Highlighted Core / Torso bridge
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.68f, h * 0.52f),
                        end = Offset(w * 0.42f, h * 0.58f),
                        strokeWidth = strokeMuscle.width,
                        cap = StrokeCap.Round
                    )
                    // Legs to toes
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.42f, h * 0.58f),
                        end = Offset(w * 0.16f, h * 0.74f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                }

                // 6. OVERHEAD / SHOULDER / ARM PRESS (Standing overhead press / curl posture)
                else -> {
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.085f, center = Offset(w * 0.50f, h * 0.30f))
                    // Upright Torso
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.50f, h * 0.39f),
                        end = Offset(w * 0.50f, h * 0.64f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Highlighted Shoulders & Raised Arms
                    drawCircle(color = muscleOrange, radius = w * 0.075f, center = Offset(w * 0.36f, h * 0.42f))
                    drawCircle(color = muscleOrange, radius = w * 0.075f, center = Offset(w * 0.64f, h * 0.42f))
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.34f, h * 0.42f),
                        end = Offset(w * 0.28f, h * 0.20f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.66f, h * 0.42f),
                        end = Offset(w * 0.72f, h * 0.20f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Overhead barbell / dumbbells
                    drawLine(
                        color = primaryGold,
                        start = Offset(w * 0.18f, h * 0.18f),
                        end = Offset(w * 0.82f, h * 0.18f),
                        strokeWidth = 2.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Legs
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.46f, h * 0.64f),
                        end = Offset(w * 0.40f, h * 0.86f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.54f, h * 0.64f),
                        end = Offset(w * 0.60f, h * 0.86f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * Custom Program Category Emblem Icon matching Mockup 1/4 #3 (`Programok`):
 * - Home + Dumbbell for "Otthoni" programs
 * - Bodyweight / Calisthenics athlete for "Saját testsúlyos" / "Kezdő" programs
 * - Heavy Barbell with plates for "Felső / Alsó" / "Súlyzós" / "Erő" programs
 */
@Composable
fun ProgramTypeEmblemIcon(
    programNameOrId: String,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier.size(50.dp)
) {
    val palette = LocalTrainPilotPalette.current
    val lower = programNameOrId.lowercase()
    val accent = if (isHighlighted) palette.primary else palette.textPrimary

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isHighlighted) palette.primary.copy(alpha = 0.16f) else palette.surfaceElevated,
        modifier = modifier.trainPilotBorder(
            1.dp,
            if (isHighlighted) palette.primary.copy(alpha = 0.5f) else palette.cardBorder,
            RoundedCornerShape(14.dp)
        )
    ) {
        val goldColor = palette.primary
        val secondaryColor = palette.secondary
        val greenColor = palette.success
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when {
                // 1. OTTHONI (House Roof + Dumbbell inside)
                lower.contains("otthon") || lower.contains("home") || lower.contains("alap") -> {
                    val roof = Path().apply {
                        moveTo(w * 0.18f, h * 0.46f)
                        lineTo(w * 0.50f, h * 0.20f)
                        lineTo(w * 0.82f, h * 0.46f)
                    }
                    drawPath(
                        path = roof,
                        color = goldColor,
                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Dumbbell bar & plates inside house
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.30f, h * 0.64f),
                        end = Offset(w * 0.70f, h * 0.64f),
                        strokeWidth = 2.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawRoundRect(
                        color = goldColor,
                        topLeft = Offset(w * 0.24f, h * 0.52f),
                        size = Size(w * 0.10f, h * 0.24f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = goldColor,
                        topLeft = Offset(w * 0.66f, h * 0.52f),
                        size = Size(w * 0.10f, h * 0.24f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

                // 2. SAJÁT TESTSÚLYOS (Calisthenics / Pull-up / Bodyweight figure)
                lower.contains("testsúly") || lower.contains("body") || lower.contains("kezdő") || lower.contains("calisthen") -> {
                    // Pull-up bar at top
                    drawLine(
                        color = goldColor,
                        start = Offset(w * 0.20f, h * 0.22f),
                        end = Offset(w * 0.80f, h * 0.22f),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Athlete head
                    drawCircle(color = greenColor, radius = w * 0.085f, center = Offset(w * 0.50f, h * 0.38f))
                    // Arms reaching up to bar
                    drawLine(
                        color = greenColor,
                        start = Offset(w * 0.42f, h * 0.46f),
                        end = Offset(w * 0.30f, h * 0.24f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = greenColor,
                        start = Offset(w * 0.58f, h * 0.46f),
                        end = Offset(w * 0.70f, h * 0.24f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // V-torso & legs
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.50f, h * 0.46f),
                        end = Offset(w * 0.50f, h * 0.76f),
                        strokeWidth = 3.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 3. SÚLYZÓS / FELSŐ-ALSÓ / ERŐ (Heavy Olympic Barbell with dual plates)
                else -> {
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.14f, h * 0.50f),
                        end = Offset(w * 0.86f, h * 0.50f),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Inner heavy plates
                    drawRoundRect(
                        color = goldColor,
                        topLeft = Offset(w * 0.24f, h * 0.28f),
                        size = Size(w * 0.10f, h * 0.44f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = goldColor,
                        topLeft = Offset(w * 0.66f, h * 0.28f),
                        size = Size(w * 0.10f, h * 0.44f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    // Outer smaller plates
                    drawRoundRect(
                        color = secondaryColor,
                        topLeft = Offset(w * 0.15f, h * 0.35f),
                        size = Size(w * 0.07f, h * 0.30f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                    drawRoundRect(
                        color = secondaryColor,
                        topLeft = Offset(w * 0.78f, h * 0.35f),
                        size = Size(w * 0.07f, h * 0.30f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }
        }
    }
}

fun programEquipmentCategoryLabel(programNameOrId: String): String {
    val lower = programNameOrId.lowercase()
    return when {
        lower.contains("otthon") || lower.contains("alap") -> "Otthoni • Kézi súlyzós & gumiszalag"
        lower.contains("testsúly") || lower.contains("kezdő") -> "Saját testsúlyos • Eszköz nélkül"
        else -> "Súlyzós • Konditermi rúd & kézi súlyzó"
    }
}

/**
 * Custom Smartwatch (Wear OS) Icon Badge matching Mockup 3/4 #2 & Mockup 4/4 #2.
 */
@Composable
fun MockupWatchGraphicBadge(
    modifier: Modifier = Modifier.size(44.dp)
) {
    val palette = LocalTrainPilotPalette.current
    val gold = palette.primary
    val green = palette.success
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Top & bottom watch straps
        drawRoundRect(
            color = Color(0xFF334155),
            topLeft = Offset(w * 0.34f, h * 0.06f),
            size = Size(w * 0.32f, h * 0.88f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        // Watch crown on right
        drawRoundRect(
            color = gold,
            topLeft = Offset(w * 0.82f, h * 0.36f),
            size = Size(w * 0.10f, h * 0.16f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Watch case & screen
        drawCircle(color = Color(0xFF1E293B), radius = w * 0.36f, center = Offset(w * 0.50f, h * 0.50f))
        drawCircle(
            color = gold,
            radius = w * 0.36f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = Stroke(width = 2.dp.toPx())
        )
        // Mini ECG pulse line on watch face
        val pulsePath = Path().apply {
            moveTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.40f, h * 0.52f)
            lineTo(w * 0.46f, h * 0.36f)
            lineTo(w * 0.54f, h * 0.64f)
            lineTo(w * 0.60f, h * 0.48f)
            lineTo(w * 0.72f, h * 0.48f)
        }
        drawPath(pulsePath, color = gold, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Green connected dot at top-right of bezel
        drawCircle(color = green, radius = w * 0.08f, center = Offset(w * 0.74f, h * 0.24f))
    }
}

/**
 * Custom Health Connect Dual-Link Heart Emblem matching Mockup 3/4 #2 & Mockup 4/4 #2.
 */
@Composable
fun MockupHealthConnectBadge(
    modifier: Modifier = Modifier.size(36.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Soft rounded background badge
        drawRoundRect(
            color = Color(0xFF0EA5E9).copy(alpha = 0.16f),
            size = size,
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        // Left interlocking cyan loop
        drawCircle(
            color = Color(0xFF38BDF8),
            radius = w * 0.22f,
            center = Offset(w * 0.40f, h * 0.50f),
            style = Stroke(width = 2.6.dp.toPx())
        )
        // Right interlocking emerald/gold loop
        drawCircle(
            color = Color(0xFF34D399),
            radius = w * 0.22f,
            center = Offset(w * 0.60f, h * 0.50f),
            style = Stroke(width = 2.6.dp.toPx())
        )
    }
}

@Composable
fun ExercisePostureMiniIllustration(
    exerciseName: String,
    size: androidx.compose.ui.unit.Dp
) {
    ExercisePostureMiniIllustration(
        exerciseNameOrId = exerciseName,
        modifier = Modifier.size(size)
    )
}

@Composable
fun ProgramTypeEmblemIcon(
    programTitle: String,
    isHighlighted: Boolean = false,
    size: androidx.compose.ui.unit.Dp
) {
    ProgramTypeEmblemIcon(
        programNameOrId = programTitle,
        isHighlighted = isHighlighted,
        modifier = Modifier.size(size)
    )
}

@Composable
fun MockupWatchGraphicBadge(
    size: androidx.compose.ui.unit.Dp
) {
    MockupWatchGraphicBadge(
        modifier = Modifier.size(size)
    )
}

@Composable
fun MockupHealthConnectBadge(
    size: androidx.compose.ui.unit.Dp
) {
    MockupHealthConnectBadge(
        modifier = Modifier.size(size)
    )
}



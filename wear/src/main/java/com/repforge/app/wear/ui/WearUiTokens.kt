package com.repforge.app.wear

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

internal val Black: Color
    @Composable get() = LocalWearHomeTheme.current.backgroundColor
internal val Card: Color
    @Composable get() = LocalWearHomeTheme.current.surfaceColor
internal val Secondary: Color
    @Composable get() = LocalWearHomeTheme.current.elevatedColor
internal val White: Color
    @Composable get() = LocalWearHomeTheme.current.textColor
internal val Muted: Color
    @Composable get() = LocalWearHomeTheme.current.mutedColor
internal val Gold: Color
    @Composable get() = LocalWearHomeTheme.current.accentColor
internal val Danger = Color(0xFF3E2024)
internal val SaveTone = Color(0xFF19382C)
internal val SaveText = Color(0xFF88E5B3)
internal val DangerText = Color(0xFFFFB4B4)
internal val LocalAutomaticToggle = compositionLocalOf<(() -> Unit)?> { null }
internal val LocalAutomaticCommand = compositionLocalOf<((String) -> Unit)?> { null }
internal val LocalSaveSync = compositionLocalOf<WearSaveSyncState?> { null }
internal val LocalOpenSaveSync = compositionLocalOf<(() -> Unit)?> { null }
internal val LocalUndo = compositionLocalOf<((String)->Unit)?> { null }
internal val LocalTouchLock189 = compositionLocalOf<(() -> Unit)?> { null }
internal val LocalEditorSave = compositionLocalOf<((String,Double,String)->Boolean)?> { null }
internal data class GpsLaunch216(val activityId: String, val start: () -> Unit)
internal data class GpsShortcut216(val state: String, val restSeconds: Int = 0, val open: () -> Unit)
internal val LocalGpsShortcut216 = compositionLocalOf<GpsShortcut216?> { null }
internal val LocalOwnership193 = compositionLocalOf<WearWorkout?> { null }

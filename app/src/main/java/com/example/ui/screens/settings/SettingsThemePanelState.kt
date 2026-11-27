package com.example.ui.screens.settings

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.ui.theme.LocalTrainPilotPalette

/** Route UI state, not persistent application data. Owned outside the optical Scaffold so
 * standard/Liquid switches cannot discard the expanded group or scroll anchor. */
@Stable
class SettingsThemePanelState internal constructor(
    private val expandedState: androidx.compose.runtime.MutableState<Boolean>,
    private val familyState: androidx.compose.runtime.MutableState<String>,
    private val revealState: androidx.compose.runtime.MutableState<Int?>,
    val listState: LazyListState
) {
    var expanded: Boolean
        get() = expandedState.value
        set(value) { expandedState.value = value }
    var familyName: String
        get() = familyState.value
        set(value) { familyState.value = value }
    var revealId: Int?
        get() = revealState.value
        set(value) { revealState.value = value }
}

@Composable
fun rememberSettingsThemePanelState(): SettingsThemePanelState {
    val initialFamily = LocalTrainPilotPalette.current.subGroup.name
    val expanded = rememberSaveable { mutableStateOf(false) }
    val family = rememberSaveable { mutableStateOf(initialFamily) }
    val reveal = rememberSaveable { mutableStateOf<Int?>(null) }
    val list = rememberLazyListState()
    return remember(expanded, family, reveal, list) {
        SettingsThemePanelState(expanded, family, reveal, list)
    }
}

package com.example.ui.screens

import com.example.ui.components.TrainPilotListDivider
import com.example.ui.components.trainPilotListPanel
import com.example.ui.theme.glassHeaderMaterial
import com.example.ui.theme.glassStateTile
import androidx.compose.foundation.background
import com.example.ui.theme.glassCardFill
import com.example.ui.theme.glassControlFill
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.glassTile
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect

import com.example.ui.components.TrainPilotModalBottomSheet
import com.example.ui.components.trainPilotAnimatedExpansion
import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotCombinedClickable
import com.example.ui.components.TrainPilotAlertDialog as AlertDialog
import com.example.ui.components.trainPilotReorderItem
import com.example.ui.components.TrainPilotDeleteButton
import com.example.ui.components.TrainPilotRedCloseButton
import com.example.ui.components.trainPilotSheetTopSafeArea

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import com.repforge.app.ui.theme.TrainPilotButton as Button
import com.repforge.app.ui.theme.TrainPilotGlassPrimaryButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.SubScreen
import com.example.ui.components.SubScreenTopBar
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.INLINE_EDITOR_HOST_PROGRAMS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsListScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleCustomTab: (Boolean) -> Unit,
    onActivateProgram: (String) -> Unit,
    onOpenProgramEditor: (String) -> Unit,
    onOpenExerciseLibrary: () -> Unit,
    onOpenPersonalPlanner: () -> Unit = {},
    onMoveProgramOrder: (Int, Int) -> Unit = { _, _ -> },
    onCreateCustomProgram: (String, String) -> Unit = { _, _ -> },
    onOpenAddCustomExercise: () -> Unit = {},
    onCopyProgram: (String) -> Unit = {},
    onDeleteProgram: (String) -> Unit = {},
    onToggleInlineEditor: (String, String) -> Unit = { id, _ -> onOpenProgramEditor(id) },
    onSaveInlineDrafts: (String, List<com.example.model.StudioProgramPrescriptionDraft>) -> Unit = { _, _ -> },
    onRenameProgram: (String, String) -> Unit = { _, _ -> },
    inlineEditor: @Composable (
        com.example.model.TrainingProgramModel,
        androidx.compose.runtime.snapshots.SnapshotStateMap<Pair<String, String>, com.example.model.StudioProgramPrescriptionDraft>
    ) -> Unit = { _, _ -> }
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val activeProgram = remember(state.programs) { state.programs.firstOrNull { it.isActive } ?: state.programs.firstOrNull() }
    val filteredPrograms = remember(state.programs, state.programsTabCustomOnly, activeProgram?.id) {
        if (state.programsTabCustomOnly) {
            state.programs.filter { it.isCustom }
        } else {
            state.programs.filter { !it.isCustom && it.id != activeProgram?.id }
        }
    }
    val activeAlsoInList = state.programsTabCustomOnly && activeProgram?.isCustom == true
    val activeTagPrefix = if (activeAlsoInList) "active_hero_" else ""
    val activeHost = if (activeAlsoInList) "${INLINE_EDITOR_HOST_PROGRAMS}_hero" else INLINE_EDITOR_HOST_PROGRAMS
    val programScroll = rememberScrollState()
    var showCreateCustomProgramDialog by remember { mutableStateOf(false) }
    var actionSheetProgramId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var actionSheetSnapshot by remember { mutableStateOf<com.example.model.TrainingProgramModel?>(null) }
    var deleteConfirmProgram by remember { mutableStateOf<Pair<String, String>?>(null) }
    // Inline editor: one open card at a time; unsaved exercise drafts are guarded before switching.
    val inlinePending = remember(state.inlineEditorProgramId, state.inlineEditorHost) {
        mutableStateMapOf<Pair<String, String>, com.example.model.StudioProgramPrescriptionDraft>()
    }
    var pendingInlineSwitch by remember { mutableStateOf<Pair<String, String>?>(null) }
    val programReorder = com.example.ui.components.rememberTrainPilotReorderState()
    var renameTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    fun isInlineOpen(programId: String, host: String) =
        state.inlineEditorProgramId == programId && state.inlineEditorHost == host
    // toggle = card tap; otherwise (menu "Edit") only opens the requested card.
    fun requestInlineEditor(programId: String, host: String, toggle: Boolean = true) {
        if (!toggle && isInlineOpen(programId, host)) return
        val openProgram = state.programs.firstOrNull { it.id == state.inlineEditorProgramId }
        if (openProgram != null && hasUnsavedPrescriptionDrafts(openProgram, inlinePending)) pendingInlineSwitch = programId to host
        else onToggleInlineEditor(programId, host)
    }

    pendingInlineSwitch?.let { (targetId, targetHost) ->
        val openProgram = state.programs.firstOrNull { it.id == state.inlineEditorProgramId }
        AlertDialog(
            onDismissRequest = { pendingInlineSwitch = null },
            containerColor = palette.surface,
            title = { Text(trW(state.language, "Mentetlen módosítások", "Unsaved changes", "Ungespeicherte Änderungen", "Modificări nesalvate"), color = palette.textPrimary) },
            text = {
                Text(
                    trW(state.language,
                        "A nyitott program gyakorlatértékei még nincsenek mentve.",
                        "Exercise values in the open program are not saved yet.",
                        "Die Übungswerte des geöffneten Programms sind noch nicht gespeichert.",
                        "Valorile exercițiilor din programul deschis nu sunt încă salvate."),
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                com.repforge.app.ui.theme.TrainPilotSaveButton(
                    onClick = {
                        // Save stays on the open card; switching is a separate tap once the write committed.
                        if (openProgram != null) onSaveInlineDrafts(openProgram.localizedName(state.language), inlinePending.values.toList())
                        pendingInlineSwitch = null
                    },
                    enabled = openProgram != null && !state.libraryMutationBusy,
                    modifier = Modifier.testTag("program_inline_unsaved_save")
                ) { Text(strings.saveBtn, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrainPilotOutlinedButton(
                        onClick = { pendingInlineSwitch = null },
                        modifier = Modifier.testTag("program_inline_unsaved_cancel")
                    ) { Text(strings.cancelBtn, color = palette.textPrimary) }
                    TrainPilotOutlinedButton(
                        onClick = {
                            inlinePending.clear()
                            pendingInlineSwitch = null
                            onToggleInlineEditor(targetId, targetHost)
                        },
                        modifier = Modifier.testTag("program_inline_unsaved_discard")
                    ) { Text(trW(state.language, "Elvetés", "Discard", "Verwerfen", "Renunță"), color = palette.textPrimary) }
                }
            }
        )
    }

    renameTarget?.let { (targetId, currentName) ->
        var newName by remember(targetId) { mutableStateOf(currentName) }
        com.example.ui.components.TrainPilotFormSheet(
            onDismissRequest = { renameTarget = null },
            containerColor = palette.surface,
            title = { Text(trW(state.language, "Program átnevezése", "Rename program", "Programm umbenennen", "Redenumește programul"), color = palette.textPrimary) },
            text = {
                com.example.ui.components.TrainPilotTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("program_rename_field")
                )
            },
            confirmButton = {
                com.repforge.app.ui.theme.TrainPilotSaveButton(
                    onClick = {
                        onRenameProgram(targetId, newName.trim())
                        renameTarget = null
                    },
                    enabled = newName.isNotBlank() && newName.trim() != currentName && !state.libraryMutationBusy,
                    modifier = Modifier.testTag("program_rename_save")
                ) { Text(strings.saveBtn, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TrainPilotOutlinedButton(onClick = { renameTarget = null }) { Text(strings.cancelBtn, color = palette.textPrimary) }
            }
        )
    }

    actionSheetProgramId?.let { targetId ->
        val liveProgram = state.programs.firstOrNull { it.id == targetId }
        val targetProgram = liveProgram ?: actionSheetSnapshot?.takeIf { it.id == targetId }
        val targetName = targetProgram?.localizedName(state.language) ?: targetId
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden || !state.libraryMutationBusy }
        )
        TrainPilotModalBottomSheet(
            onDismissRequest = { if (!state.libraryMutationBusy) actionSheetProgramId = null },
            sheetState = sheetState,
            containerColor = palette.surface,
            contentColor = palette.textPrimary,
            dragHandle = null,
            modifier = Modifier
                .trainPilotSheetTopSafeArea()
                .testTag("program_action_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = targetName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary,
                            modifier = Modifier.testTag("program_action_sheet_title")
                        )
                        val typeLabel = when {
                            targetProgram == null -> trW(state.language, "A kiválasztott program már nem található", "Selected program was not found", "Ausgewähltes Programm nicht gefunden", "Programul selectat nu a mai fost găsit")
                            targetProgram.isActive && targetProgram.isCustom -> trW(state.language, "Jelenleg aktív • Saját program", "Currently active • Custom program", "Aktuell aktiv • Eigenes Programm", "Activ acum • Program propriu")
                            targetProgram.isActive -> trW(state.language, "Jelenleg aktív • Alap program", "Currently active • Built-in program", "Aktuell aktiv • Standardprogramm", "Activ acum • Program de bază")
                            targetProgram.isCustom -> trW(state.language, "Saját program", "Custom program", "Eigenes Programm", "Program propriu")
                            else -> trW(state.language, "Alap program", "Built-in program", "Standardprogramm", "Program de bază")
                        }
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    TrainPilotRedCloseButton(
                        onClick = { if (!state.libraryMutationBusy) actionSheetProgramId = null },
                        testTag = "program_action_sheet_close",
                        enabled = !state.libraryMutationBusy
                    )
                }

                HorizontalDivider(color = palette.cardBorder)

                if (liveProgram != null && targetId == activeProgram?.id) {
                    TrainPilotOutlinedButton(
                        onClick = {
                            actionSheetProgramId = null
                            requestInlineEditor(targetId, activeHost, toggle = false)
                        },
                        enabled = !state.libraryMutationBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("program_action_edit")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(trW(state.language, "Szerkesztés", "Edit", "Bearbeiten", "Editează"))
                    }
                }
                TrainPilotOutlinedButton(
                    onClick = {
                        actionSheetProgramId = null
                        renameTarget = targetId to targetName
                    },
                    enabled = !state.libraryMutationBusy && liveProgram != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("program_action_rename")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trW(state.language, "Átnevezés", "Rename", "Umbenennen", "Redenumește"),
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                TrainPilotOutlinedButton(
                    onClick = {
                        actionSheetProgramId = null
                        onCopyProgram(targetId)
                    },
                    enabled = !state.libraryMutationBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("program_action_copy")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trW(state.language, "Másolás saját tervként", "Copy as custom plan", "Als eigenen Plan kopieren", "Copiază ca plan propriu"),
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (liveProgram?.isCustom == true) {
                    TrainPilotDeleteButton(
                        onClick = {
                            actionSheetProgramId = null
                            deleteConfirmProgram = targetId to targetName
                        },
                        enabled = !state.libraryMutationBusy && !liveProgram.isActive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("program_action_delete"),
                        label = trW(state.language, "Törlés", "Delete", "Löschen", "Șterge")
                    )
                    if (liveProgram.isActive) Text(
                        text = trW(state.language, "Törlés előtt válassz másik aktív programot.",
                            "Select another active program before deleting.",
                            "Wähle vor dem Löschen ein anderes aktives Programm.",
                            "Selectează alt program activ înainte de ștergere."),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        modifier = Modifier.fillMaxWidth().testTag("program_action_delete_unavailable_hint")
                    )
                }
            }
        }
    }

    deleteConfirmProgram?.let { (confirmId, confirmName) ->
        AlertDialog(
            onDismissRequest = { if (!state.libraryMutationBusy) deleteConfirmProgram = null },
            modifier = Modifier.testTag("program_delete_confirm_dialog"),
            containerColor = palette.surfaceCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = trW(
                        state.language,
                        "Program törlése: $confirmName?",
                        "Delete program: $confirmName?",
                        "Programm löschen: $confirmName?",
                        "Ștergi programul: $confirmName?"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("program_delete_confirm_title")
                )
            },
            text = {
                Text(
                    text = trW(
                        state.language,
                        "A(z) „$confirmName” saját program végleg törlődik a tárból. A korábbi naplózott edzések megmaradnak. Ha a programhoz tartoznak tervezett naptári alkalmak, előbb ürítsd őket a naptárból.",
                        "The custom program “$confirmName” will be permanently deleted. Your logged workout history is kept. Clear any planned calendar entries for this program first.",
                        "Das eigene Programm „$confirmName“ wird dauerhaft gelöscht. Das Trainingsprotokoll bleibt erhalten. Entferne zuerst geplante Kalendereinträge dieses Programms.",
                        "Programul personalizat „$confirmName” va fi șters definitiv. Jurnalul antrenamentelor anterioare se păstrează. Șterge mai întâi planurile din calendar ale acestui program."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                com.repforge.app.ui.theme.TrainPilotNegativeButton(
                    onClick = {
                        deleteConfirmProgram = null
                        onDeleteProgram(confirmId)
                    },
                    enabled = !state.libraryMutationBusy,
                    modifier = Modifier.testTag("program_delete_confirm_button")
                ) {
                    Text(trW(state.language, "Törlés", "Delete", "Löschen", "Șterge"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TrainPilotOutlinedButton(
                    onClick = { deleteConfirmProgram = null },
                    enabled = !state.libraryMutationBusy,
                    modifier = Modifier.testTag("program_delete_cancel_button")
                ) {
                    Text(strings.cancelBtn, color = palette.textPrimary)
                }
            }
        )
    }

    if (showCreateCustomProgramDialog) {
        var customProgName by remember { mutableStateOf("") }
        var customProgSub by remember { mutableStateOf(trW(state.language, "Saját egyedi összeállítású edzésterv", "Custom workout plan", "Eigener Trainingsplan", "Plan de antrenament personalizat")) }
        com.example.ui.components.TrainPilotFormSheet(
            onDismissRequest = { showCreateCustomProgramDialog = false },
            containerColor = palette.surfaceCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(trW(state.language, "Új egyedi program létrehozása", "Create new custom program", "Neues eigenes Programm erstellen", "Creează program personalizat nou"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.example.ui.components.TrainPilotTextField(
                        value = customProgName,
                        onValueChange = { customProgName = it },
                        label = { Text(trW(state.language, "Program neve (pl. Kör futás & Erő)", "Program name", "Programmname", "Numele programului")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    com.example.ui.components.TrainPilotTextField(
                        value = customProgSub,
                        onValueChange = { customProgSub = it },
                        label = { Text(trW(state.language, "Rövid leírás / megjegyzés", "Short description / note", "Kurzbeschreibung / Notiz", "Descriere scurtă / notiță")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TrainPilotGlassPrimaryButton(
                    onClick = {
                        showCreateCustomProgramDialog = false
                        onCreateCustomProgram(customProgName, customProgSub)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    )
                ) {
                    Text(trW(state.language, "Létrehozás és szerkesztés", "Create and edit", "Erstellen und bearbeiten", "Creează și editează"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TrainPilotOutlinedButton(onClick = { showCreateCustomProgramDialog = false }) {
                    Text(strings.cancelBtn, color = palette.textPrimary)
                }
            }
        )
    }

    val bottomInset = when {
        state.isWorkoutSessionActive && state.statusBannerMessage != null -> 140.dp
        state.isWorkoutSessionActive -> 84.dp
        state.statusBannerMessage != null -> 68.dp
        else -> 4.dp
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp)
            .padding(bottom = bottomInset)
    ) {
        if (state.currentSubScreen == SubScreen.PROGRAMS_LIST) {
            SubScreenTopBar(
                title = strings.programsShortcutTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.padding(horizontal = TrainPilotCardSpacing.ScreenGutter)
            )
        }
        com.example.ui.theme.GlassScrollChrome(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            rootInset = if (state.currentSubScreen == SubScreen.PROGRAMS_LIST) 0.dp
                else com.example.ui.theme.LocalGlassTopInset.current,
            top = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = TrainPilotCardSpacing.ScreenGutter),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Segmented Pill: Alap programok (Left) | Saját programok (Right)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (palette.isLiquidGlass) Color.Transparent else palette.glassCardFill(palette.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassHeaderMaterial(palette, RoundedCornerShape(24.dp))
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(24.dp))
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    val builtinCount = remember(state.programs) { state.programs.count { !it.isCustom } }
                    val customCount = state.programs.size - builtinCount
                    val tabs = remember(state.language, builtinCount, customCount) { listOf(
                        false to trW(state.language, "Alap programok ($builtinCount)", "Built-in programs ($builtinCount)", "Standardprogramme ($builtinCount)", "Programe de bază ($builtinCount)"),
                        true to trW(state.language, "Saját programok ($customCount)", "Custom programs ($customCount)", "Eigene Programme ($customCount)", "Programe proprii ($customCount)")
                    ) }
                    tabs.forEach { (isCustom, label) ->
                        val selected = state.programsTabCustomOnly == isCustom
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) palette.glassControlFill(palette.primary, GlassMaterialRole.Selection) else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .then(if (selected) Modifier.trainPilotGlassRim(palette, RoundedCornerShape(20.dp), GlassMaterialRole.Selection) else Modifier)
                                .trainPilotClickable(RoundedCornerShape(20.dp)) { onToggleCustomTab(isCustom) }
                                .testTag(if (isCustom) "programs_tab_custom" else "programs_tab_builtin")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) (if (palette.isLiquidGlass) palette.textPrimary else palette.onPrimary) else palette.textSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            if (activeProgram != null) {
                val activeName = activeProgram.localizedName(state.language)
                val activeExpanded = isInlineOpen(activeProgram.id, activeHost)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (palette.isLiquidGlass) Color.Transparent else if (activeExpanded) palette.glassTile(palette.surface) else palette.glassCardFill(palette.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassHeaderMaterial(palette, RoundedCornerShape(16.dp))
                        .trainPilotBorder(1.5.dp, palette.primary, RoundedCornerShape(16.dp))
                        .testTag("programs_active_program_card")
                ) {
                    ProgramCardContent(
                        program = activeProgram,
                        language = state.language,
                        isExpanded = activeExpanded,
                        tagPrefix = activeTagPrefix,
                        emblemSize = 40.dp,
                        emphasizeActiveBadge = true,
                        onActivateProgram = { onActivateProgram(activeProgram.id) },
                        onOpenMoreActions = {
                            actionSheetSnapshot = activeProgram
                            actionSheetProgramId = activeProgram.id
                        },
                        headerModifier = Modifier.trainPilotCombinedClickable(
                            shape = RoundedCornerShape(16.dp),
                            onLongClickLabel = trW(state.language, "Program műveletek: $activeName", "Program actions: $activeName", "Programmaktionen: $activeName", "Acțiuni program: $activeName"),
                            onLongClick = {
                                actionSheetSnapshot = activeProgram
                                actionSheetProgramId = activeProgram.id
                            },
                            onClick = { requestInlineEditor(activeProgram.id, activeHost) }
                        )
                    )
                }
            }

        }

                    Column(
                        Modifier.fillMaxWidth()
                            .glassHeaderMaterial(palette, RoundedCornerShape(18.dp))
                            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                            .then(if (palette.isLiquidGlass) Modifier else Modifier.background(palette.surface, RoundedCornerShape(18.dp)))
                    ) {
                        Text(
                            text = if (state.programsTabCustomOnly) trW(state.language, "Saját készítésű és generált tervek", "Custom and generated plans", "Eigene und generierte Pläne", "Planuri proprii și generate") else strings.moreProgramsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 6.dp)
                                .testTag("programs_fixed_list_header")
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = TrainPilotCardSpacing.ScreenGutter),
                            color = palette.cardBorder.copy(alpha = 0.65f)
                        )
                    }
                }
            },
            bottom = {
        Column(Modifier.fillMaxWidth().padding(top = 4.dp)
            .glassHeaderMaterial(palette, RoundedCornerShape(18.dp))
            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            .then(if (palette.isLiquidGlass) Modifier else Modifier.background(palette.surface, RoundedCornerShape(18.dp)))
            .padding(horizontal = 8.dp, vertical = 4.dp).testTag("programs_fixed_actions"),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrainPilotGlassPrimaryButton(
                    onClick = { showCreateCustomProgramDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("programs_create_custom_program_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(trW(state.language, "Új program", "New program", "Neues Programm", "Program nou"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }

                TrainPilotOutlinedButton(
                    onClick = onOpenAddCustomExercise,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("programs_create_custom_exercise_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(trW(state.language, "Új gyakorlat", "New exercise", "Neue Übung", "Exercițiu nou"), style = MaterialTheme.typography.labelLarge, color = palette.primary, fontWeight = FontWeight.Bold)
                }
            }

            TrainPilotOutlinedButton(
                onClick = onOpenExerciseLibrary,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("programs_exercise_picker")
            ) {
                Text(
                    text = "${strings.exercisePickerTitle} (${state.exercises.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary
                )
            }
        }
            }
        ) { scrollModifier, topInset, bottomBarInset ->
            Column(
                scrollModifier.trainPilotListPanel()
                    .verticalScroll(programScroll).testTag("programs_list_scroll")
                    .padding(horizontal = TrainPilotCardSpacing.ScreenGutter),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Spacer(Modifier.height(topInset))
                if (activeProgram != null && isInlineOpen(activeProgram.id, activeHost)) {
                    LaunchedEffect(activeProgram.id, activeHost) { programScroll.scrollTo(0) }
                    ProgramCardEditorContent(activeProgram, state.language, activeTagPrefix) {
                        inlineEditor(activeProgram, inlinePending)
                    }
                }

                if (filteredPrograms.isEmpty() && state.programsTabCustomOnly) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.glassCardFill(palette.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .trainPilotGlassMaterial(palette, RoundedCornerShape(16.dp))
                            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = trW(state.language, "Még nincs saját generált programod", "No custom generated programs yet", "Noch keine eigenen Programme", "Nu ai încă programe personalizate"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.textPrimary
                            )
                            Text(
                                text = trW(
                                    state.language,
                                    "Készíts személyre szabott edzéstervet a Kezdőprofil (A01) varázslóval vagy hozz létre saját programot!",
                                    "Build a personalized workout plan with the Starter Profile (A01) wizard or create a custom program!",
                                    "Erstelle einen personalisierten Trainingsplan mit dem Startprofil (A01) oder lege ein eigenes Programm an!",
                                    "Creează un plan personalizat cu profilul de bază (A01) sau adaugă un program propriu!"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            TrainPilotGlassPrimaryButton(
                                onClick = onOpenPersonalPlanner,
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(trW(state.language, "Kezdőprofil és egyéni terv (A01)", "Starter profile & custom plan (A01)", "Startprofil & eigener Plan (A01)", "Profil de bază și plan propriu (A01)"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Reorder requires measured neighbours, including offscreen cards. Keep its existing
                // scroll container and stable composition identity; repeated cards use paint, not a lens.
                Column(Modifier.fillMaxWidth()) {
                    filteredPrograms.forEachIndexed { index, program -> key(program.id) {
                        val progName = program.localizedName(state.language)
                        val isExpanded = isInlineOpen(program.id, INLINE_EDITOR_HOST_PROGRAMS)
                        if (index > 0) TrainPilotListDivider()
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (program.isActive || isExpanded)
                                palette.glassStateTile(palette.primary, palette.primary.copy(alpha = .09f)) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                                .trainPilotReorderItem(programReorder, program.id, index, layerWhenIdle = false)
                                .trainPilotAnimatedExpansion()
                                .testTag("program_card_${program.id}")
                        ) {
                            ProgramCardContent(
                                program = program,
                                language = state.language,
                                isExpanded = isExpanded,
                                tagPrefix = "",
                                emblemSize = 36.dp,
                                onActivateProgram = { onActivateProgram(program.id) },
                                onOpenMoreActions = {
                                    actionSheetSnapshot = program
                                    actionSheetProgramId = program.id
                                },
                                headerModifier = Modifier.trainPilotCombinedClickable(
                                    shape = RoundedCornerShape(14.dp),
                                    onLongClickLabel = trW(state.language, "Program műveletek: $progName", "Program actions: $progName", "Programmaktionen: $progName", "Acțiuni program: $progName"),
                                    onLongClick = {
                                        actionSheetSnapshot = program
                                        actionSheetProgramId = program.id
                                    },
                                    onClick = { requestInlineEditor(program.id, INLINE_EDITOR_HOST_PROGRAMS) }
                                ),
                                expandedContent = { inlineEditor(program, inlinePending) },
                                reorderSlot = {
                                    com.example.ui.components.TrainPilotReorderHandle(
                                        state = programReorder,
                                        key = program.id,
                                        index = index,
                                        lastIndex = filteredPrograms.lastIndex,
                                        gap = 1.dp,
                                        label = trW(state.language, "Sorrend: tartsd nyomva és húzd", "Order: press and hold, then drag", "Reihenfolge: gedrückt halten und ziehen", "Ordine: ține apăsat și trage"),
                                        moveUpLabel = trW(state.language, "Feljebb mozgatás", "Move up", "Nach oben", "Mută mai sus"),
                                        moveDownLabel = trW(state.language, "Lejjebb mozgatás", "Move down", "Nach unten", "Mută mai jos"),
                                        testTag = "program_drag_handle_${program.id}",
                                        onMove = { from, to ->
                                            val fromId = filteredPrograms.getOrNull(from)?.id
                                            val toId = filteredPrograms.getOrNull(to)?.id
                                            val tabPrograms = state.programs.filter { it.isCustom == state.programsTabCustomOnly }
                                            onMoveProgramOrder(
                                                tabPrograms.indexOfFirst { it.id == fromId },
                                                tabPrograms.indexOfFirst { it.id == toId }
                                            )
                                        },
                                        modifier = Modifier.padding(end = 2.dp)
                                    )
                                }
                            )
                        }
                    } }
                }
                Spacer(Modifier.height(bottomBarInset))
            }
        }
    }
}

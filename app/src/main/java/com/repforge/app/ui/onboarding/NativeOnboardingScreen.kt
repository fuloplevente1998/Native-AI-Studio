package com.repforge.app.ui.onboarding

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import com.example.model.AppLanguage
import com.example.localization.TrainPilotStrings
import com.example.ui.components.StudioLanguageSelector
import com.example.ui.components.StudioPaletteCategories
import com.example.ui.components.TrainPilotMountainLogo
import com.example.ui.theme.*
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.TrainPilotGlassPrimaryButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.repforge.app.ui.theme.TrainPilotOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repforge.app.R
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors

/**
 * First-launch welcome and resume surface. Profile editing is supplied by the
 * same Studio planner used from Home; this surface does not own a second editor.
 */
@Composable
internal fun NativeOnboardingScreen(
    settings: TrainPilotUserSettings,
    destination: NativeOnboardingPolicy.Destination,
    saving: Boolean,
    failed: Boolean,
    onBegin: () -> Unit,
    onSkip: () -> Unit,
    profileEditor: @Composable (onDismiss: () -> Unit) -> Unit,
    onBack: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onSelectPalette: (Int) -> Unit = {}
) {
    val colors = LocalTrainPilotColors.current
    val palette = LocalTrainPilotPalette.current
    val language = AppLanguage.entries.firstOrNull { it.code.equals(settings.language, true) } ?: AppLanguage.HU
    val welcome = destination == NativeOnboardingPolicy.Destination.WELCOME
    var expandedFamily by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val strings = remember(context, settings.language) { nativeResources(context, settings.language) }
    var profileOpen by remember(destination) { mutableStateOf(false) }

    BackHandler {
        when {
            profileOpen && !saving -> profileOpen = false
            !saving -> onBack()
        }
    }

    // Root-sized backdrop, not a gradient that stretches when a family expands.
    Box(Modifier.fillMaxSize().trainPilotPageBackdrop(palette)) {
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("onboarding_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (welcome) Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("onboarding_brand"),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            TrainPilotMountainLogo(modifier = Modifier.size(58.dp))
            Text("TrainPilot", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = palette.primary)
        }
        Text(
            text = strings.getString(if (destination == NativeOnboardingPolicy.Destination.WELCOME)
                R.string.native_onboarding_welcome else R.string.native_onboarding_profile),
            fontSize = 26.sp, fontWeight = FontWeight.Bold, color = colors.text,
            modifier = Modifier.testTag(if (destination == NativeOnboardingPolicy.Destination.WELCOME)
                "onboarding_welcome" else "onboarding_profile")
        )
        Text(
            text = strings.getString(if (destination == NativeOnboardingPolicy.Destination.WELCOME)
                R.string.native_onboarding_intro else R.string.native_onboarding_profile_intro),
            color = colors.muted, fontSize = 16.sp
        )
        if (welcome) {
            val shape = RoundedCornerShape(18.dp)
            val uiStrings = TrainPilotStrings.forLanguage(language)
            Surface(shape = shape, color = palette.glassCardFill(palette.cardPaint),
                modifier = Modifier.fillMaxWidth().trainPilotGlassMaterial(palette, shape)
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, shape).testTag("onboarding_language_panel")) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(uiStrings.languageTitle, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    StudioLanguageSelector(language, onSelectLanguage, "onboarding_language", enabled = !saving)
                }
            }
            Surface(shape = shape, color = palette.glassCardFill(palette.cardPaint),
                modifier = Modifier.fillMaxWidth().trainPilotGlassMaterial(palette, shape)
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, shape).testTag("onboarding_theme_panel")) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(when (language) {
                        AppLanguage.HU -> "Témák"
                        AppLanguage.EN -> "Themes"
                        AppLanguage.DE -> "Designs"
                        AppLanguage.RO -> "Teme"
                    }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = palette.textPrimary)
                    StudioPaletteCategories(language, palette.id,
                        PaletteCategory.entries.firstOrNull { it.name == expandedFamily },
                        onExpand = { expandedFamily = it?.name },
                        onSelect = { if (!saving) onSelectPalette(it) },
                        choiceTag = "onboarding_palette_")
                }
            }
        }
        if (failed) Text(strings.getString(R.string.native_onboarding_error),
            modifier = Modifier.testTag("onboarding_error"), color = colors.dangerText)
        if (saving) CircularProgressIndicator(modifier = Modifier.testTag("onboarding_saving"))
        TrainPilotGlassPrimaryButton(
            onClick = {
                if (destination == NativeOnboardingPolicy.Destination.WELCOME) onBegin()
                else profileOpen = true
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(
                if (destination == NativeOnboardingPolicy.Destination.WELCOME)
                    "onboarding_begin" else "onboarding_edit_profile"),
            colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)
        ) {
            Text(strings.getString(if (destination == NativeOnboardingPolicy.Destination.WELCOME)
                R.string.native_onboarding_begin else R.string.native_onboarding_edit))
        }
        TrainPilotOutlinedButton(
            onClick = onSkip, enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("onboarding_skip")
        ) { Text(strings.getString(R.string.native_onboarding_skip), color = colors.text) }
        if (destination == NativeOnboardingPolicy.Destination.PROFILE)
            Text(strings.getString(R.string.native_onboarding_resume_info), color = colors.muted,
                fontSize = 14.sp)
    }
    }

    if (profileOpen) {
        profileEditor { if (!saving) profileOpen = false }
    }
}

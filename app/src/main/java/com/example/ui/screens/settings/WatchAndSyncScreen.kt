package com.example.ui.screens

import com.example.ui.theme.glassStateTile
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassTile
import com.example.ui.theme.glassCardFill
import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sync
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.ui.components.watchinfo.StudioWatchInformationCard
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.ui.components.MockupHealthConnectBadge
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * 4/4 #2 ÓRA ÉS SZINKRON (Mockup 4/4 #2 teljes, részletes felépítése)
 */
@Composable
fun WatchAndSyncScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleControlMode: () -> Unit,
    onShowBanner: (String) -> Unit,
    onRequestPermission: (String) -> Unit = {},
    onRefreshWatchInfo: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    LazyColumn(
        modifier = Modifier
            .then(com.example.ui.components.sheetAwareFillMaxSize())
            .background(palette.background)
            .testTag("watch_and_sync_screen"),
        contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        item {
            SubScreenTopBar(
                title = strings.watchAndSyncTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }

        // Section 1: Órakapcsolat (Mockup 4/4 #2)
        item {
            Text(
                text = strings.watchConnectedSub,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }

        item {
            StudioWatchInformationCard(
                data = uiState.watchInfoCardData.copy(loading = uiState.watchInfoLoading || uiState.watchInfoCardData.loading),
                onRefresh = onRefreshWatchInfo
            )
        }

        // Section 2: Aktív edzés vezérlése (Mockup 4/4 #2 side-by-side mode cards + gold button + bullets)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.glassCardFill(palette.surfaceCard),
                modifier = Modifier.trainPilotGlassMaterial(palette, RoundedCornerShape(22.dp))
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
                ) {
                    Text(
                        text = tr(uiState.language, "Aktív edzés vezérlése", "Active Workout Control", "Steuerung des aktiven Trainings", "Controlul antrenamentului activ"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )

                    // Side-by-side cards: Telefon vezérli vs Óra vezérli
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val phoneSelected = uiState.phoneControlsWorkout && !uiState.watchControlPending
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (phoneSelected) palette.primary.copy(alpha = 0.16f) else palette.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .trainPilotBorder(
                                    width = if (phoneSelected) 1.5.dp else 1.dp,
                                    color = if (phoneSelected) palette.primary else palette.cardBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .trainPilotClickable(RoundedCornerShape(16.dp)) {
                                    if (!phoneSelected && !uiState.watchControlPending) onToggleControlMode()
                                }
                                .testTag("watch_mode_phone_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr(uiState.language, "📱 Telefon vezérli", "📱 Phone Controls", "📱 Telefon steuert", "📱 Controlat de telefon"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (phoneSelected) palette.primary else palette.textPrimary
                                    )
                                    if (phoneSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = tr(uiState.language, "Óra megjeleníti", "Watch mirrors state", "Uhr zeigt an", "Ceasul afișează"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        val watchSelected = !uiState.phoneControlsWorkout && !uiState.watchControlPending
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (watchSelected) palette.primary.copy(alpha = 0.16f) else palette.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .trainPilotBorder(
                                    width = if (watchSelected) 1.5.dp else 1.dp,
                                    color = if (watchSelected) palette.primary else palette.cardBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .trainPilotClickable(RoundedCornerShape(16.dp)) {
                                    if (!watchSelected && !uiState.watchControlPending) onToggleControlMode()
                                }
                                .testTag("watch_mode_watch_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr(uiState.language, "⌚ Óra vezérli", "⌚ Watch Controls", "⌚ Uhr steuert", "⌚ Controlat de ceas"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (watchSelected) palette.primary else palette.textPrimary
                                    )
                                    if (watchSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = tr(uiState.language, "Önálló edzés", "Autonomous session", "Eigenständiges Training", "Sesiune autonomă"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onToggleControlMode,
                        enabled = uiState.watchAppAvailable && !uiState.watchControlPending && !uiState.isSavingWorkout,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("watch_handover_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.watchControlPending) tr(uiState.language, "Óra ACK-ra vár…", "Waiting for Watch ACK…", "Warte auf Uhr-ACK…", "Se așteaptă ACK de la ceas…")
                                else if (uiState.phoneControlsWorkout) strings.handoverToWatchAction else strings.reclaimToPhoneAction,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!uiState.watchAppAvailable) Text(
                            tr(
                                uiState.language,
                                "Az órára a párosított TrainPilot Wear OS alkalmazást telepítsd; a Galaxy Wearable önmagában nem elég.",
                                "Install the paired TrainPilot Wear OS app on your watch; Galaxy Wearable alone is not enough.",
                                "Installiere die gekoppelte TrainPilot Wear OS-App auf der Uhr; Galaxy Wearable allein reicht nicht aus.",
                                "Instalează aplicația asociată TrainPilot Wear OS pe ceas; Galaxy Wearable singur nu este suficient."
                            ),
                            style = MaterialTheme.typography.bodySmall, color = palette.warning)
                        Text(
                            text = tr(
                                uiState.language,
                                "• A pulzus és edzésadatok szinkronja órakapcsolatot és külön visszaigazolást igényel",
                                "• Heart rate and workout sync requires watch connection and explicit ACK",
                                "• Puls- und Trainingssynchronisierung erfordert Uhrenverbindung und ACK",
                                "• Sincronizarea pulsului și a antrenamentului necesită conexiune cu ceasul și ACK"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Text(
                            text = tr(
                                uiState.language,
                                "• Offline folytatás: kapcsolat megszakadásakor az óra önállóan folytatja az edzést",
                                "• Offline continuation: if disconnected, the watch continues the workout autonomously",
                                "• Offline-Fortsetzung: Bei Verbindungsabbruch führt die Uhr das Training selbstständig fort",
                                "• Continuare offline: la întreruperea conexiunii, ceasul continuă autonom antrenamentul"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }

        // Section 3: Egészségügyi szinkron (Mockup 4/4 #2: Health Connect & Samsung Health cards)
        item {
            Text(
                text = tr(uiState.language, "Egészségügyi szinkron", "Health Sync", "Gesundheitssynchronisierung", "Sincronizare sănătate"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.glassCardFill(palette.surfaceCard),
                modifier = Modifier.trainPilotGlassMaterial(palette, RoundedCornerShape(18.dp))
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                    .trainPilotClickable(RoundedCornerShape(18.dp)) { onRequestPermission("hc_permission") }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MockupHealthConnectBadge(size = 46.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Health Connect",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (uiState.permissionHealthConnectEnabled) {
                                "${tr(uiState.language, "Engedélyezett", "Granted", "Erteilt", "Acordate")}: ${uiState.healthGrantedCount}/${uiState.healthPermissionCount}"
                            } else {
                                tr(uiState.language, "Nincs olvasási engedély • engedélykérés", "No read permission • request access", "Keine Leseberechtigung • Zugriff anfordern", "Fără permisiune de citire • solicită acces")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.success
                        )
                        Text(
                            text = if (uiState.permissionHealthConnectEnabled) {
                                tr(uiState.language, "Koppints a Health Connect szinkronizálásához", "Tap to sync Health Connect", "Tippen zum Synchronisieren von Health Connect", "Atinge pentru a sincroniza Health Connect")
                            } else {
                                tr(uiState.language, "Pulzus, alvás, lépések, elégetett kalória, edzések", "Heart rate, sleep, steps, calories burned, workouts", "Puls, Schlaf, Schritte, Kalorien, Trainings", "Puls, somn, pași, calorii arse, antrenamente")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.textSecondary
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.glassCardFill(palette.surfaceCard),
                modifier = Modifier.trainPilotGlassMaterial(palette, RoundedCornerShape(18.dp))
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                    .trainPilotClickable(RoundedCornerShape(18.dp)) { onRequestPermission("samsung") }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.glassStateTile(Color(0xFF14B8A6), Color(0xFF14B8A6).copy(alpha = 0.16f)),
                        modifier = Modifier
                            .size(46.dp)
                            .border(1.dp, Color(0xFF14B8A6).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFF14B8A6),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Samsung Health",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (uiState.permissionSamsungHealthEnabled) {
                                tr(uiState.language, "Olvasási engedély igazolva", "Read permission verified", "Leseberechtigung bestätigt", "Permisiune de citire verificată")
                            } else {
                                tr(uiState.language, "Nincs igazolt olvasási engedély", "No verified read permission", "Keine bestätigte Leseberechtigung", "Nicio permisiune de citire verificată")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.success
                        )
                        Text(
                            text = if (uiState.permissionSamsungHealthEnabled) {
                                tr(uiState.language, "Koppints a Samsung Health szinkronizálásához", "Tap to sync Samsung Health", "Tippen zum Synchronisieren von Samsung Health", "Atinge pentru a sincroniza Samsung Health")
                            } else {
                                tr(uiState.language, "Koppints az engedély megadásához és szinkronhoz", "Tap to grant permission and sync", "Tippen für Berechtigung und Synchronisierung", "Atinge pentru permisiune și sincronizare")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.textSecondary
                    )
                }
            }
        }
    }
}

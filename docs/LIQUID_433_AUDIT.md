# #433 – Liquid forrásaudit

Alap: `b14dc497448b2e3b4df0f9715ea65cb675a01775`, 281800. Csak dokumentáció; nincs implementáció vagy vizuális elfogadás. A nyolc Liquid téma a hatókör, a 32 standard paletta és Wear változatlan. A teljes fa 310 telefonos UI/VM/root Kotlin-fájlt tartalmaz; nem mind aktív vizuális komponens. A tényleges launcher, TrainPilotAppRoot, TrainPilotSheetRoutes és CODEMAP alapján kell a megkerülő útvonalakat besorolni. A táblázat teljes fájlokon végzett mintakeresés, nem készülékes teszt. A találat lehet szemantikus szín, import alias vagy standard ág; nem automatikusan hiba. A hiányzó találat nem igazol helyességet.

## Studio képernyők – teljes fájlleltár (49/49)

| Fájl | Sor | Közvetlen festék | Vezérlő | Közös üveg | Optika/lista | Közös press |
|---|---:|---|---|---|---|---|
| `com/example/ui/screens/HomeScreen.kt` | 538 | 297, 307, 386, 420 | 377 | 174, 223, 244, 316, 317, 470, 521, 522 | 10, 53, 154, 159 | 177, 242, 381, 524 |
| `com/example/ui/screens/LoggedPrRecords.kt` | 83 | — | — | — | — | — |
| `com/example/ui/screens/OutdoorGpsDashboard.kt` | 600 | 108, 240, 458, 564 | 249, 460, 476, 482, 484, 547, 562 | 304, 351, 352, 440, 497, 525, 544 | 29, 216, 236 | — |
| `com/example/ui/screens/ScreenText.kt` | 11 | — | — | — | — | — |
| `com/example/ui/screens/StatisticsExerciseTrend.kt` | 89 | — | — | — | — | — |
| `com/example/ui/screens/StudioBodyFitnessTiles.kt` | 181 | — | — | 148, 149 | — | 151 |
| `com/example/ui/screens/StudioStatisticsScreen.kt` | 563 | 210, 242, 522 | 168, 494, 512, 547 | 147, 400, 401, 470 | 7, 24, 28, 138, 159, 519 | 181, 215, 302, 451 |
| `com/example/ui/screens/StudioTimerReadout.kt` | 46 | — | — | — | — | — |
| `com/example/ui/screens/StudioTodayStatusGrid.kt` | 65 | — | — | — | — | — |
| `com/example/ui/screens/StudioVolumeChart.kt` | 103 | 69 | 78 | — | — | — |
| `com/example/ui/screens/StudioWorkoutModesDialog.kt` | 361 | 92, 148, 238, 293, 323 | 249, 258, 288, 313, 320 | 104, 107, 194, 236 | 16, 29, 99, 157 | 109, 237 |
| `com/example/ui/screens/WorkoutTabDayCards.kt` | 239 | 188 | — | 167, 169, 185 | — | 171, 210 |
| `com/example/ui/screens/health/CoachDetailScreen.kt` | 370 | 253, 326 | — | 196, 198, 274, 297, 317, 352 | 21, 39, 161, 175 | 353 |
| `com/example/ui/screens/health/ExternalWorkoutImportPanel.kt` | 215 | — | 45, 103, 106, 111, 128, 161, 166, 190, 195, 200 | — | 4, 118, 188 | — |
| `com/example/ui/screens/health/HealthScreenShared.kt` | 44 | — | — | — | — | — |
| `com/example/ui/screens/health/HealthSourcesContent.kt` | 263 | 95 | — | 114, 115, 138, 139 | — | 100, 119, 142, 221, 231 |
| `com/example/ui/screens/health/HealthTabScreen.kt` | 859 | 221, 340, 382, 401, 513, 644 | 369, 388, 518, 797 | 472, 555, 577, 592 | 3, 40, 44, 718, 722, 838 | 265, 278 |
| `com/example/ui/screens/health/HealthTiles.kt` | 296 | 169, 277 | — | 61, 63, 110, 112, 148, 152, 215 | — | 65, 154 |
| `com/example/ui/screens/health/HealthTodayStatusSheet.kt` | 539 | 264, 421, 524 | 269 | 101, 285, 309, 353, 426, 427, 507 | 34, 112 | 358, 479, 512 |
| `com/example/ui/screens/health/HealthTodayTileExtras.kt` | 270 | 130 | — | 123, 201 | — | 126, 161, 250 |
| `com/example/ui/screens/health/HealthWeightAndGoals.kt` | 450 | 165, 250, 262, 401, 413, 446 | 192, 221, 244, 417, 421, 423, 428, 434 | 135, 136, 307, 308, 440, 442 | 34, 235 | 302, 310, 444 |
| `com/example/ui/screens/log/LogRouteMapScreen.kt` | 144 | 132, 138 | — | 82, 85 | — | — |
| `com/example/ui/screens/log/LogScreenText.kt` | 9 | — | — | — | — | — |
| `com/example/ui/screens/log/LogTabScreen.kt` | 701 | 209, 231, 238, 689 | 114, 236, 652 | 321, 344, 364, 368, 407, 451, 547, 669 | 42, 309, 483 | 346, 370, 409, 455, 599, 671 |
| `com/example/ui/screens/log/LogWorkoutDetailScreen.kt` | 426 | 155, 165, 280 | — | 105, 110, 145, 146, 327, 328, 353, 354 | 37, 133, 217 | 210, 247 |
| `com/example/ui/screens/measure/ActiveBilateralWorkoutScreen.kt` | 838 | 166, 430, 455, 593, 618, 703, 711 | — | 276, 280, 281, 356, 431, 519, 594, 680, 760 | 10, 47, 260, 313 | 450, 613, 705, 728 |
| `com/example/ui/screens/measure/ActiveOutdoorGpsScreen.kt` | 583 | — | — | — | — | — |
| `com/example/ui/screens/measure/ActiveStrengthWorkoutScreen.kt` | 302 | 282 | — | 147, 151, 152 | 4, 32, 134, 193 | 290 |
| `com/example/ui/screens/measure/ActiveTimedWorkoutScreen.kt` | 558 | 367, 392, 523, 538, 544 | — | 198, 202, 203, 268, 470, 515 | 8, 46, 182, 230 | 387, 490 |
| `com/example/ui/screens/measure/IntervalSettingsScreen.kt` | 232 | 224 | 141, 149, 155, 208, 218 | 129, 130, 191, 192, 214 | 27, 80 | — |
| `com/example/ui/screens/measure/MeasurementShared.kt` | 122 | — | — | — | — | 63 |
| `com/example/ui/screens/measure/WorkoutSummaryScreen.kt` | 750 | 439, 508, 538, 620 | 491, 521 | 398, 399, 433, 586, 740, 741 | 34, 76, 239, 562 | 468, 641 |
| `com/example/ui/screens/progress/ProgressTabScreen.kt` | 248 | 227 | — | 117, 118, 145, 203 | 9, 11, 42, 106, 107, 184 | 134, 206 |
| `com/example/ui/screens/progress/ProgressVolumeAndMuscle.kt` | 835 | 420, 453, 470, 493, 510, 529, 546, 565, 621, 686, 732, 779 | — | 133, 137, 237, 251, 286, 387, 392, 437, 486, 522, 558, 598, 601, 761 | 41, 398, 682 | 149, 243, 418, 468, 487, 508, 523, 544, 559, 608, 768 |
| `com/example/ui/screens/settings/BackupRestoreScreen.kt` | 376 | 200 | 224, 269, 272 | 219, 220, 233, 256, 257, 366 | 37, 40, 181, 191, 371 | — |
| `com/example/ui/screens/settings/GoogleCloudSyncCard.kt` | 463 | 159, 214, 248, 302, 313, 334, 345, 348, 418 | 291, 330, 430 | 164, 165, 189, 192, 241, 243 | — | 207, 234, 287, 298, 428 |
| `com/example/ui/screens/settings/PermissionsScreen.kt` | 263 | 215 | 243 | 192 | 21, 68 | 212 |
| `com/example/ui/screens/settings/SettingsMainScreen.kt` | 495 | 120, 149, 480 | — | 208, 261, 279, 302, 450, 453, 474 | 19, 46, 49, 134, 172, 176 | 146, 217, 282, 307, 458 |
| `com/example/ui/screens/settings/WatchAndSyncScreen.kt` | 468 | 254, 333 | 249 | 132, 350, 405, 418 | 35, 80 | 163, 211, 353, 408 |
| `com/example/ui/screens/workout/AddCustomExerciseDialog.kt` | 274 | 92, 111, 136, 160, 186 | 133, 139, 183, 189, 235, 244 | — | 17, 88 | 115, 164, 233, 242 |
| `com/example/ui/screens/workout/CalendarPlannerScreen.kt` | 1081 | 160, 244, 307, 324, 611, 849, 861, 890, 1054, 1064 | 302, 652, 1059 | 270, 360, 417, 420, 460, 486, 627, 632, 755, 796, 799, 825 | 44, 354, 405, 947 | 187, 205, 274, 364, 462, 488, 507, 579, 852, 869 |
| `com/example/ui/screens/workout/ExerciseDetailScreen.kt` | 354 | 135, 156, 281, 315 | 311 | 129, 167, 168, 274 | 31, 106 | 149 |
| `com/example/ui/screens/workout/ExerciseLibraryScreen.kt` | 464 | 265, 274, 361, 384 | 269, 329, 381, 389, 398, 410 | 154, 246, 248, 314 | 36, 145, 290 | 173, 176, 179, 199, 317, 356, 375, 457 |
| `com/example/ui/screens/workout/PlannerControls.kt` | 329 | 261, 288 | 269 | 100, 101, 102, 103, 120, 121, 203, 324 | 5, 6, 7, 320, 323 | 142, 160, 209 |
| `com/example/ui/screens/workout/ProgramCard.kt` | 325 | 150, 221 | — | 133 | — | 127, 175 |
| `com/example/ui/screens/workout/ProgramEditorScreen.kt` | 514 | — | 489, 502 | 220, 267, 268, 388, 477 | 45, 120 | 225, 284, 329 |
| `com/example/ui/screens/workout/ProgramsListScreen.kt` | 792 | 148, 194, 232, 277, 367, 426, 456, 602, 611, 624, 647, 714 | — | 500, 503, 517, 521, 546, 549, 589, 608, 684, 687, 733 | 47, 582, 669 | 450, 522, 565, 620, 712, 750 |
| `com/example/ui/screens/workout/WorkoutScreenText.kt` | 11 | — | — | — | — | — |
| `com/example/ui/screens/workout/WorkoutTabScreen.kt` | 493 | 379 | — | 86, 92, 186, 191, 268, 273, 346, 349, 396, 400, 462, 466 | 41, 173 | 91, 190, 272, 311, 375, 399, 465 |

## Studio közös komponensek – teljes fájlleltár (91/91)

| Fájl | Sor | Közvetlen festék | Vezérlő | Közös üveg | Optika/lista | Közös press |
|---|---:|---|---|---|---|---|
| `com/example/ui/components/ActiveWorkoutBottomBar.kt` | 181 | 85 | — | — | — | 83 |
| `com/example/ui/components/ActiveWorkoutLiveBar.kt` | 133 | 96 | — | 81, 83, 104, 105, 117, 118 | — | 85, 107, 119 |
| `com/example/ui/components/CompactCalendarDialog.kt` | 87 | 43, 69 | — | — | 9, 53, 82 | — |
| `com/example/ui/components/CompactDatePickerDialog.kt` | 121 | 110 | 61, 65, 70, 75 | — | — | — |
| `com/example/ui/components/ExerciseFeedbackChips.kt` | 166 | — | 64, 74, 105, 128 | 59, 60, 61, 97, 124 | — | — |
| `com/example/ui/components/ExerciseFeedbackControl.kt` | 71 | 37, 46 | 40 | — | — | — |
| `com/example/ui/components/ExerciseKindIcon.kt` | 40 | — | — | — | — | — |
| `com/example/ui/components/ExerciseVideoDialog.kt` | 204 | 122, 159, 181, 185, 194 | 148, 190 | — | 14, 131 | — |
| `com/example/ui/components/ExerciseVideoResolver.kt` | 174 | — | — | — | — | — |
| `com/example/ui/components/HomeCoachAdviceCard.kt` | 131 | 100, 118 | 112 | 91, 93, 121 | — | 95 |
| `com/example/ui/components/HomePlannedDateLine.kt` | 41 | — | — | — | — | — |
| `com/example/ui/components/QuickWorkoutHubSheet.kt` | 157 | 152 | — | 78, 80, 86, 136, 138 | 21, 128 | 89, 140 |
| `com/example/ui/components/RecordedRouteMapPresentation.kt` | 21 | — | — | — | — | — |
| `com/example/ui/components/RestDockScrollSizing.kt` | 88 | — | — | — | — | — |
| `com/example/ui/components/StudioActiveWorkoutPresentation.kt` | 138 | 43 | 109 | — | 5, 117 | — |
| `com/example/ui/components/StudioActiveWorkoutTop.kt` | 176 | — | — | 129 | — | 104, 126 |
| `com/example/ui/components/StudioCoachDomainPanels.kt` | 526 | 207, 275, 311, 342 | 109, 184, 188, 316, 373, 409, 411, 419, 429 | 129, 199, 201, 206, 266, 268, 412, 413, 476 | 20, 21, 204, 214 | 143, 469 |
| `com/example/ui/components/StudioCoachReadinessText.kt` | 56 | — | — | — | — | — |
| `com/example/ui/components/StudioCompactControls.kt` | 164 | — | 113 | 62, 99, 100, 110, 131 | — | 100, 132 |
| `com/example/ui/components/StudioCompactRestControls.kt` | 106 | — | 99 | 78 | — | 73 |
| `com/example/ui/components/StudioCustomExerciseActions.kt` | 119 | — | — | — | — | — |
| `com/example/ui/components/StudioDailyHealthStatus.kt` | 64 | — | 42 | — | — | — |
| `com/example/ui/components/StudioExerciseSequenceStrip.kt` | 77 | — | — | 62 | 6, 45 | 65 |
| `com/example/ui/components/StudioHealthMetricsBrowser.kt` | 380 | — | 179, 307, 318 | 156, 162, 164 | 11, 14, 148, 163, 193 | 166, 240 |
| `com/example/ui/components/StudioHealthPermissionAction.kt` | 19 | — | — | — | — | — |
| `com/example/ui/components/StudioHealthRootEffects.kt` | 51 | — | — | — | — | — |
| `com/example/ui/components/StudioHourlyStepsChart.kt` | 165 | — | — | — | — | — |
| `com/example/ui/components/StudioLiveWorkoutHealth.kt` | 155 | 110, 120 | 48, 141 | 49 | 14, 95 | 100 |
| `com/example/ui/components/StudioLoggedWorkoutDeleteActions.kt` | 66 | — | 62 | — | — | — |
| `com/example/ui/components/StudioLoggedWorkoutEditActions.kt` | 38 | — | — | — | — | — |
| `com/example/ui/components/StudioNativeEditorTheme.kt` | 14 | — | — | — | — | — |
| `com/example/ui/components/StudioPaletteCategories.kt` | 137 | — | — | 72, 74 | — | 78, 110 |
| `com/example/ui/components/StudioPausedWorkoutActions.kt` | 64 | — | 31, 58 | — | — | — |
| `com/example/ui/components/StudioPersonalPlannerDialog.kt` | 73 | — | — | — | — | — |
| `com/example/ui/components/StudioPlatformAccess.kt` | 449 | — | — | — | — | — |
| `com/example/ui/components/StudioPrivatePhotos.kt` | 381 | 164 | 318, 345 | 154, 163, 173, 174, 253, 254 | — | 295 |
| `com/example/ui/components/StudioProgramManagementControls.kt` | 119 | — | 90, 117 | — | — | — |
| `com/example/ui/components/StudioRestTimerDock.kt` | 230 | 131 | — | 80, 83, 104, 107, 139 | — | 89, 136, 168, 207 |
| `com/example/ui/components/StudioStrengthSetCard.kt` | 255 | — | 123, 145, 147, 227, 229, 234, 247 | 82, 91, 118, 120, 206, 209 | — | 139, 210 |
| `com/example/ui/components/StudioWorkoutHandoverBar.kt` | 205 | — | — | 101, 103, 106, 107, 142, 155, 158 | — | 152 |
| `com/example/ui/components/StudioWorkoutHealth.kt` | 93 | — | — | — | — | — |
| `com/example/ui/components/StudioWorkoutHealthPreviews.kt` | 97 | — | — | — | — | — |
| `com/example/ui/components/TrainPilotAlertDialog.kt` | 116 | — | — | 71, 111 | 3, 7, 11, 87, 105, 106, 109 | — |
| `com/example/ui/components/TrainPilotChartSelection.kt` | 77 | 74 | — | — | — | — |
| `com/example/ui/components/TrainPilotCoachIcon.kt` | 9 | — | — | — | — | — |
| `com/example/ui/components/TrainPilotControls.kt` | 286 | — | 274 | 94, 121, 122, 228, 246, 268, 271 | 39, 81 | 58, 93, 230, 247 |
| `com/example/ui/components/TrainPilotDeleteControls.kt` | 73 | — | 62 | — | — | 64 |
| `com/example/ui/components/TrainPilotDropdownMenu.kt` | 82 | — | 38, 46, 50, 66 | 41 | — | — |
| `com/example/ui/components/TrainPilotFitText.kt` | 59 | — | — | — | — | — |
| `com/example/ui/components/TrainPilotFullHeightFormSheet.kt` | 76 | 48 | — | — | — | — |
| `com/example/ui/components/TrainPilotListPanel.kt` | 29 | 27 | — | 19, 20 | — | — |
| `com/example/ui/components/TrainPilotModalBottomSheet.kt` | 133 | 56, 97 | — | 99 | 3, 5, 6, 92, 98 | — |
| `com/example/ui/components/TrainPilotPageLayout.kt` | 540 | 470, 512, 519, 528 | — | — | — | — |
| `com/example/ui/components/TrainPilotRangeCalendarSheet.kt` | 204 | 112 | — | 83, 126 | — | 119, 130, 173 |
| `com/example/ui/components/TrainPilotReorder.kt` | 208 | — | — | — | — | — |
| `com/example/ui/components/TrainPilotRouteContent.kt` | 72 | — | — | — | — | — |
| `com/example/ui/components/TrainPilotScreenSheet.kt` | 222 | 101 | — | — | 14, 203 | — |
| `com/example/ui/components/TrainPilotSheetRoutes.kt` | 32 | — | — | — | — | — |
| `com/example/ui/components/TrainPilotSheetSafeArea.kt` | 34 | — | — | — | — | — |
| `com/example/ui/components/WeeklyGoals.kt` | 276 | 183 | — | 127, 129, 159, 161, 182, 266 | — | 103, 132, 163, 243 |
| `com/example/ui/components/WorkoutControlAppearance.kt` | 28 | — | — | 14 | — | — |
| `com/example/ui/components/WorkoutEditorAppearance.kt` | 45 | — | — | 14 | — | — |
| `com/example/ui/components/WorkoutFeedbackPopup.kt` | 113 | 38 | 59, 88 | 58, 103, 104 | — | — |
| `com/example/ui/components/WorkoutModeArt.kt` | 69 | — | — | — | — | — |
| `com/example/ui/components/WorkoutPresentationLabels.kt` | 41 | — | — | — | — | — |
| `com/example/ui/components/common/ComponentText.kt` | 11 | — | — | — | — | — |
| `com/example/ui/components/common/CustomThemeColorControls.kt` | 111 | — | — | — | — | 102 |
| `com/example/ui/components/common/CustomThemeColorWheel.kt` | 119 | — | 110, 112, 114 | — | — | 105 |
| `com/example/ui/components/common/CustomThemeEditorSheet.kt` | 145 | 55 | — | — | 5, 61 | 67, 75 |
| `com/example/ui/components/common/CustomThemePreview.kt` | 143 | 35, 40, 67, 68, 69, 91, 125 | — | — | 7, 55 | 48, 51 |
| `com/example/ui/components/common/CustomThemeSection.kt` | 135 | — | — | — | — | — |
| `com/example/ui/components/common/ExerciseVideoButton.kt` | 112 | 73 | 67 | — | — | — |
| `com/example/ui/components/common/GlassSelectionPill.kt` | 111 | — | — | 107 | — | — |
| `com/example/ui/components/common/GoalsEditDialog.kt` | 336 | 68, 159, 167 | 225, 274 | — | 40, 90 | 109, 135 |
| `com/example/ui/components/common/LiquidGlassControl.kt` | 156 | 129 | 55 | — | 22, 118, 119, 139 | — |
| `com/example/ui/components/common/PalettePickerSheet.kt` | 221 | 77 | — | — | 43, 83 | 135, 167, 192 |
| `com/example/ui/components/common/PersonalPlannerModal.kt` | 1012 | 256, 304, 333, 391, 451, 488, 522, 568, 637, 697, 712, 731, 757, 782, 833, 859, 884 | 301, 313, 565, 577, 779, 791, 881, 893 | — | 56, 358 | 264, 530, 739, 841 |
| `com/example/ui/components/common/ScrollHelpers.kt` | 130 | 48 | — | — | — | — |
| `com/example/ui/components/common/TopHeaderAndNav.kt` | 592 | 152, 181, 188, 534 | 392, 396, 462, 564 | 145, 228, 254, 293, 353, 394, 467, 533 | 12, 278, 393 | 169, 230, 256, 391 |
| `com/example/ui/components/healthreadings/HealthReadingRows.kt` | 248 | 158, 165, 238 | — | 132, 224 | — | — |
| `com/example/ui/components/inappvideo/InAppVideoPolicy.kt` | 166 | — | — | — | — | — |
| `com/example/ui/components/inappvideo/SecureExerciseVideoWebView.kt` | 342 | — | — | — | — | — |
| `com/example/ui/components/mapcontrols/MapAppearanceSelector.kt` | 110 | 71 | 75, 84, 91 | — | — | — |
| `com/example/ui/components/visual/AnatomicalFigure.kt` | 177 | — | — | — | — | — |
| `com/example/ui/components/visual/ExercisePostureIllustration.kt` | 316 | — | — | 35 | — | — |
| `com/example/ui/components/visual/IntegrationBadges.kt` | 122 | 37, 50, 84, 90, 97 | — | — | — | — |
| `com/example/ui/components/visual/MuscleHeatmapCard.kt` | 602 | 386, 488, 496, 509 | 285 | 257, 260, 292, 294, 467, 470 | 541 | 295, 363, 437, 473, 499, 560 |
| `com/example/ui/components/visual/ProgramEquipmentSummary.kt` | 239 | — | — | — | — | — |
| `com/example/ui/components/visual/ProgramTypeEmblem.kt` | 181 | — | — | — | — | — |
| `com/example/ui/components/visual/WorkoutRouteMapBox.kt` | 267 | 110, 205, 256 | 260 | — | — | — |
| `com/example/ui/components/watchinfo/StudioWatchInformationCard.kt` | 186 | 103 | 96 | 69, 71 | — | 67 |

## Témarendszer és natív közös vezérlők – teljes fájlleltár (38/38)

| Fájl | Sor | Színforrás/semleges token | Vezérlő | Közös üveg | Optikai kapu | Press |
|---|---:|---|---|---|---|---|
| `com/example/ui/theme/Color.kt` | 12 | — | — | — | — | — |
| `com/example/ui/theme/CustomThemeCodec.kt` | 38 | — | — | — | — | — |
| `com/example/ui/theme/CustomThemeDraft.kt` | 85 | — | — | — | — | — |
| `com/example/ui/theme/CustomThemeResolver.kt` | 91 | 74 | — | — | — | — |
| `com/example/ui/theme/GlassAwareScaffold.kt` | 139 | — | — | — | 19, 44, 45, 46, 47, 50, 97, 114, 117, 129 | — |
| `com/example/ui/theme/GlassContentBackdrop.kt` | 205 | 170 | — | 61, 77, 83, 90, 128, 203, 204 | 3, 22, 76, 77, 80, 96, 130, 132, 184, 185, 188 | — |
| `com/example/ui/theme/GlassContrast.kt` | 102 | 7, 31 | — | — | — | — |
| `com/example/ui/theme/GlassControlDrag.kt` | 53 | — | — | — | — | — |
| `com/example/ui/theme/GlassMotionState.kt` | 25 | — | — | — | — | — |
| `com/example/ui/theme/GlassNavigationTransition.kt` | 52 | — | — | — | 33, 34 | — |
| `com/example/ui/theme/GlassOptics.kt` | 167 | 72, 82 | — | — | 12, 115 | — |
| `com/example/ui/theme/GlassPowerSave.kt` | 53 | — | — | — | 20 | — |
| `com/example/ui/theme/GlassScene.kt` | 291 | — | — | — | 243, 264 | — |
| `com/example/ui/theme/GlassScenePaintCache.kt` | 77 | — | — | — | — | — |
| `com/example/ui/theme/GlassScrollChrome.kt` | 75 | — | — | — | 17, 23, 38, 40, 62 | — |
| `com/example/ui/theme/GlassShaders.kt` | 328 | 90, 111 | — | — | 3, 23, 91, 94, 96, 97 | — |
| `com/example/ui/theme/GlassSurfaces.kt` | 190 | 24, 107, 109, 120, 167, 174, 181 | — | 34, 54, 80, 86, 131, 165 | 100, 120, 127, 130 | — |
| `com/example/ui/theme/GlassTouchBubble.kt` | 145 | — | — | — | 78 | 7, 69, 75, 79, 86, 91, 94 |
| `com/example/ui/theme/KyantGlass.kt` | 230 | 121 | — | 120 | 33, 34, 82, 111, 120, 141, 199, 207, 211 | 114, 140, 176, 177 |
| `com/example/ui/theme/KyantMaterialSpec.kt` | 80 | — | — | — | 21 | — |
| `com/example/ui/theme/KyantWindowOverlay.kt` | 63 | — | — | — | 36, 43, 55 | — |
| `com/example/ui/theme/LiquidDropMaterial.kt` | 59 | — | — | 39, 57 | 44, 45, 46, 50 | 10, 41, 50, 52, 53 |
| `com/example/ui/theme/LiquidGlassPalettes.kt` | 140 | 34, 54, 60, 75, 88, 90, 91, 97, 104, 112 | — | 118, 126, 133 | — | — |
| `com/example/ui/theme/MeasuredScrollChrome.kt` | 56 | — | — | — | — | — |
| `com/example/ui/theme/Theme.kt` | 98 | 22, 41, 45, 52, 61, 65, 72 | — | — | — | — |
| `com/example/ui/theme/ThemeSelectionColors.kt` | 52 | 21, 37 | — | — | — | — |
| `com/example/ui/theme/TrainPilotGlassEngine.kt` | 225 | 35, 108 | — | 32, 37, 42, 172 | 35, 40, 51, 177 | — |
| `com/example/ui/theme/TrainPilotPalettes.kt` | 741 | 55, 62, 63, 64, 68, 69, 70, 71, 142, 160, 178, 196, 214, 232, 250, 268, 291, 309, 327, 345, 364, 382, 400, 419, 441, 459, 477, 495, 513, 531, 549, 567, 589, 608, 627, 646, 665, 684, 703, 722 | — | — | — | — |
| `com/example/ui/theme/Type.kt` | 89 | — | — | — | — | — |
| `com/repforge/app/ui/theme/NativeStudioTypography.kt` | 50 | — | — | — | — | — |
| `com/repforge/app/ui/theme/NativeThemeTokens.kt` | 62 | — | — | — | — | — |
| `com/repforge/app/ui/theme/TrainPilotActionButtons.kt` | 104 | — | 58, 72, 85 | 60, 74, 87 | — | 59, 73, 86 |
| `com/repforge/app/ui/theme/TrainPilotButton.kt` | 158 | — | 71, 135, 155 | 72, 133 | — | 3, 71, 134, 135, 155 |
| `com/repforge/app/ui/theme/TrainPilotFrames.kt` | 111 | — | — | — | — | — |
| `com/repforge/app/ui/theme/TrainPilotOutlinedButton.kt` | 54 | 30, 42 | 51 | 46, 48 | — | 3, 46, 47, 51 |
| `com/repforge/app/ui/theme/TrainPilotPressFeedback.kt` | 209 | — | 102, 119 | — | 18, 19, 22, 29, 145, 146, 149 | 25, 31, 62, 70, 94, 101, 108, 118, 137, 179, 184, 187, 189, 202 |
| `com/repforge/app/ui/theme/TrainPilotTheme.kt` | 84 | 55 | — | — | — | — |
| `com/repforge/app/ui/theme/TrainPilotVisualSystem.kt` | 39 | — | — | — | — | — |

## Natív UI, beágyazott komponensek és segédlogika – teljes fájlleltár (106/106)

A @Composable előfordulások csak jelöltek; a régi, nem hívott natív képernyők nem kerülnek automatikusan módosításra. A tényleges route és hívó vizsgálata szükséges.

| Fájl | Sor | @Composable | Közvetlen festék | Vezérlő | Közös üveg | Optika/lista | Közös press |
|---|---:|---:|---|---|---|---|---|
| `com/repforge/app/ui/calendar/CalendarScreen.kt` | 316 | 4 | 90, 112 | 62, 65, 70, 77, 96, 98, 101, 102, 105, 116, 131, 132, 135, 162, 163, 170, 171, 185, 229, 234, 242, 258, 274, 284 | — | 10, 11, 14, 57, 67, 156, 227 | — |
| `com/repforge/app/ui/calendar/CalendarViewModel.kt` | 182 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/calendar/NativeCalendarPlanner.kt` | 61 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/calendar/NativeCalendarRepository.kt` | 164 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/coach/CoachScreen.kt` | 216 | 1 | 51, 68, 72, 87, 108, 124, 139 | 49, 114 | — | 10, 63 | — |
| `com/repforge/app/ui/coach/CoachViewModel.kt` | 134 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/coach/NativeCoachAdviceRepository.kt` | 216 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/coach/NativeCoachAdviceText.kt` | 66 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/coach/NativeCoachExerciseAdvice.kt` | 154 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/coach/NativeCoachHistoryProjection.kt` | 52 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/coach/NativeCoachReadiness.kt` | 143 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/coach/NativeCoachSignals.kt` | 85 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/coach/NativeCoachTargets.kt` | 59 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/coach/NativeCoachVariation.kt` | 171 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/coach/NativeSelectedCoachReadings.kt` | 53 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/coach/NativeTrainingModeProgression.kt` | 171 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/coach/WorkoutCoachAdviceViewModel.kt` | 103 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/components/TrainPilotSurfaces.kt` | 162 | 18 | 67, 90, 145, 158 | 156 | — | — | — |
| `com/repforge/app/ui/health/HealthDashboardScreen.kt` | 869 | 6 | 201, 206, 503, 652, 675, 689, 705, 722, 726, 736, 740, 755, 778, 814, 826, 831, 833 | 188, 323, 324, 326, 331, 343, 360, 449, 473, 477, 480, 490, 506, 541, 666, 673, 715, 752, 760, 795, 829 | — | 51, 306, 329, 334, 388, 641 | 338, 390, 812, 857 |
| `com/repforge/app/ui/health/HealthDashboardViewModel.kt` | 756 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/health/HealthDaySources.kt` | 54 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/health/HealthRecordBrowser.kt` | 839 | 4 | 181, 236, 371, 465, 492, 556, 585, 597, 710 | 209, 291, 335, 397, 419, 488, 737, 744 | — | 21, 22, 302, 362 | 451, 647 |
| `com/repforge/app/ui/health/HealthRecordCompactRows.kt` | 100 | 1 | 92 | — | — | — | — |
| `com/repforge/app/ui/health/HealthRecordDayDetails.kt` | 84 | 1 | — | 74, 77 | — | — | — |
| `com/repforge/app/ui/health/HealthWorkoutEnergyDay.kt` | 116 | 1 | — | — | — | — | — |
| `com/repforge/app/ui/health/NativeDetailedHealthMetrics.kt` | 124 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/health/NativeHealthAppRefreshPolicy.kt` | 24 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/health/NativeHealthAutoRefreshGate.kt` | 97 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/health/NativeHealthBackgroundPolicy.kt` | 79 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/health/NativeHealthForegroundAdmission.kt` | 137 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/health/NativeHealthHistory.kt` | 60 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/health/NativeHealthJournal.kt` | 68 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/health/NativeHealthMetrics.kt` | 66 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/health/NativeHealthRecords.kt` | 184 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/health/NativeHourlySteps.kt` | 103 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/health/NativeSleepRecordPresentation.kt` | 73 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/health/SamsungHourlyEnergy.kt` | 75 | 1 | — | — | — | — | — |
| `com/repforge/app/ui/home/HomeScreen.kt` | 396 | 6 | 162, 188, 222 | 122, 182 | — | 27, 118 | 354 |
| `com/repforge/app/ui/home/HomeViewModel.kt` | 168 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/home/NativeHomeRecoveryPresentation.kt` | 93 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/home/NativeHomeRepository.kt` | 66 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/home/QuickWorkoutScreen.kt` | 204 | 1 | — | 107, 132, 141, 156, 165, 168 | — | 14, 102 | 92, 154 |
| `com/repforge/app/ui/journal/ExternalHeartRateZones.kt` | 44 | 1 | — | — | — | — | — |
| `com/repforge/app/ui/journal/ExternalRoutePreview.kt` | 195 | 2 | — | — | — | — | — |
| `com/repforge/app/ui/journal/JournalEditorDialog.kt` | 310 | 6 | 78, 178, 256 | 91, 97, 180, 183, 243, 267 | — | 14, 88, 99 | — |
| `com/repforge/app/ui/journal/JournalEditorFrame.kt` | 85 | 4 | 52, 78 | — | — | — | — |
| `com/repforge/app/ui/journal/JournalEffortDropdown.kt` | 70 | 1 | 56 | 55, 59 | — | — | — |
| `com/repforge/app/ui/journal/JournalExternalWorkoutMetrics.kt` | 130 | 1 | — | — | — | — | — |
| `com/repforge/app/ui/journal/JournalHealthPanel.kt` | 107 | 3 | — | — | — | — | — |
| `com/repforge/app/ui/journal/JournalLinkedExternalSources.kt` | 53 | 1 | — | — | — | — | — |
| `com/repforge/app/ui/journal/JournalScreen.kt` | 343 | 6 | 150, 195 | 82, 85, 90, 170, 281, 290, 297, 337 | — | 14, 15, 96, 189, 257 | 259 |
| `com/repforge/app/ui/journal/JournalSetValueDropdown.kt` | 77 | 1 | 34, 51, 64 | 44, 63, 65, 68 | — | — | — |
| `com/repforge/app/ui/journal/JournalViewModel.kt` | 316 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/journal/JournalWorkoutHealthSummary.kt` | 119 | 2 | — | — | — | — | — |
| `com/repforge/app/ui/journal/NativeJournalRepository.kt` | 96 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/journal/NativeJournalText.kt` | 7 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/journal/WorkoutRouteCoordinates.kt` | 66 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt` | 168 | 3 | 81, 151, 156 | 64 | — | — | — |
| `com/repforge/app/ui/navigation/NativeMainHeader.kt` | 183 | 3 | — | 54, 74, 80, 86, 159 | — | — | 61 |
| `com/repforge/app/ui/navigation/NativeMainMenu.kt` | 62 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/navigation/NativePlanningShortcuts.kt` | 38 | 1 | — | — | — | — | 31 |
| `com/repforge/app/ui/navigation/ProgramGpsStartDialog.kt` | 37 | 1 | — | 30, 34, 35 | — | — | — |
| `com/repforge/app/ui/onboarding/NativeFirstLaunchGate.kt` | 57 | 2 | — | — | — | — | — |
| `com/repforge/app/ui/onboarding/NativeOnboardingPermissions.kt` | 233 | 2 | 183, 192, 204, 224, 229 | 208, 215 | — | 25, 185 | — |
| `com/repforge/app/ui/onboarding/NativeOnboardingPolicy.kt` | 62 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/onboarding/NativeOnboardingScreen.kt` | 115 | 2 | 95 | 86 | — | 15, 66 | — |
| `com/repforge/app/ui/programs/CustomExerciseDialog.kt` | 199 | 2 | — | 172, 196 | — | 13, 110 | 120, 126, 132 |
| `com/repforge/app/ui/programs/ExerciseLibraryScreen.kt` | 263 | 1 | 235, 246 | 168, 171, 184, 190, 242 | — | 21, 22, 172, 232 | 220, 225, 226, 237 |
| `com/repforge/app/ui/programs/ExerciseLibraryViewModel.kt` | 114 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/programs/PersonalProgramPreviewDialog.kt` | 177 | 1 | — | 168, 173 | — | 11, 82 | 86 |
| `com/repforge/app/ui/programs/ProgramEditorDialog.kt` | 233 | 1 | — | 121, 125, 132, 158, 166, 169, 184, 213 | — | 14, 15, 110, 182 | — |
| `com/repforge/app/ui/programs/ProgramPrescriptionDialog.kt` | 92 | 1 | 90 | 81, 88 | — | 10, 49 | — |
| `com/repforge/app/ui/programs/TrainingLandingScreen.kt` | 139 | 1 | 74, 113 | 111 | — | 4, 65 | — |
| `com/repforge/app/ui/programs/TrainingProgramsViewModel.kt` | 289 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/progress/ExternalProgressSummary.kt` | 66 | 1 | — | — | — | — | — |
| `com/repforge/app/ui/progress/NativeCardioCalendarRefresh.kt` | 32 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/progress/NativeCardioModel.kt` | 619 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/progress/NativeCardioProgressAdapter.kt` | 260 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/progress/NativeCardioProgressPanel.kt` | 214 | 2 | — | 66, 152 | 50, 55, 57 | — | 190 |
| `com/repforge/app/ui/progress/NativeMuscleLoadVisual.kt` | 128 | 2 | — | 71, 79 | — | — | — |
| `com/repforge/app/ui/progress/NativeProgressPhotoGallery.kt` | 449 | 4 | 338, 405 | 433 | — | 25, 26, 273, 280, 350 | 298, 357 |
| `com/repforge/app/ui/progress/NativeProgressQuery.kt` | 127 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/progress/NativeStrengthRange.kt` | 28 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/progress/NativeWeightJournalRepository.kt` | 125 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/progress/ProgressScreen.kt` | 446 | 7 | 229, 391, 392, 414 | — | — | 35, 36, 135, 177, 200 | 188, 208 |
| `com/repforge/app/ui/progress/ProgressViewModel.kt` | 280 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/progress/StatisticsMetricTile.kt` | 35 | 1 | — | — | 24, 25 | — | — |
| `com/repforge/app/ui/progress/WeightJournalScreen.kt` | 236 | 4 | 74, 131, 142, 146, 222 | 79, 97, 129, 162, 175, 221 | — | 12, 13, 58, 164 | 166 |
| `com/repforge/app/ui/progress/WeightJournalViewModel.kt` | 108 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/settings/NativeGoogleConsentLauncher.kt` | 32 | 1 | — | — | — | — | — |
| `com/repforge/app/ui/settings/NativeGoogleMaintenance.kt` | 43 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/settings/NativeGoogleMaintenanceUi.kt` | 76 | 3 | — | 35, 54, 58 | — | — | — |
| `com/repforge/app/ui/settings/NativeLanguagePicker.kt` | 67 | 1 | 38 | 55 | — | — | — |
| `com/repforge/app/ui/settings/NativeThemePicker.kt` | 78 | 1 | 42 | 59 | — | — | — |
| `com/repforge/app/ui/settings/NativeTimerDefaultsDialog.kt` | 101 | 3 | 53 | 77, 97 | — | 12, 56 | — |
| `com/repforge/app/ui/settings/NativeTrainingModeComparison.kt` | 144 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/settings/NativeTrainingModePicker.kt` | 287 | 1 | 119, 158, 283 | 130, 132, 144, 146, 153, 265, 279 | — | 13, 45, 198 | — |
| `com/repforge/app/ui/settings/ReplacementRecoveryPanel.kt` | 51 | 1 | — | 40 | — | — | — |
| `com/repforge/app/ui/settings/SettingsScreen.kt` | 510 | 1 | 179, 201, 239, 247, 265, 274, 295, 320, 328, 342, 390, 398, 465, 483, 488, 501, 506 | 226, 235, 243, 255, 261, 317, 324, 358, 375, 394, 405, 453, 461, 481, 499 | — | 28, 30, 35, 145, 476 | 205 |
| `com/repforge/app/ui/settings/SettingsViewModel.kt` | 400 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/settings/TrainingProfileDialog.kt` | 82 | 1 | — | — | — | — | — |
| `com/repforge/app/ui/text/NativeLocalizedResources.kt` | 18 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/workout/ActiveWorkoutScreen.kt` | 1212 | 10 | 303, 339, 403, 447, 466, 490, 516, 525, 559, 620, 641, 649, 672, 691, 732, 741, 772, 782, 833, 855, 859, 865, 904, 939, 983, 985, 1128, 1132, 1147, 1209 | 337, 425, 443, 512, 521, 616, 630, 637, 648, 655, 689, 788, 832, 835, 858, 865, 902, 917, 919, 923, 928, 938, 941, 947, 984, 991, 1102, 1135, 1144, 1145, 1146, 1205 | — | 56, 57, 126, 343, 460, 565 | 570, 733 |
| `com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt` | 1235 | 0 | — | — | — | — | — |
| `com/repforge/app/ui/workout/NativeGpsPermissionRecovery.kt` | 184 | 1 | 181 | 171, 177 | — | — | — |
| `com/repforge/app/ui/workout/NativeTrainingModeProposal.kt` | 106 | 0 | — | — | — | — | — |

## Gyökér és Studio állapotkezelés – leltár (13/26)

| Fájl | Sor | Téma | Navigáció | UI állapot |
|---|---:|---|---|---|
| `com/example/MainActivity.kt` | 1372 | 99, 102, 189, 272, 274, 407, 409, 414, 518, 525, 936, 1263 | 77, 143, 144, 325, 326, 327, 330, 443, 468, 522, 523, 528, 531, 535, 562, 599, 600, 605, 609, 610, 616, 631, 632, 655, 656, 692, 693, 723, 728, 729, 745, 746, 747, 765, 780, 781, 782, 785, 803, 804, 805, 820, 835, 836, 837, 858, 883, 884, 888, 889, 905, 913, 914, 934, 942, 961, 962, 974, 975, 993, 994, 1030, 1031, 1038, 1071, 1078, 1080, 1090, 1091, 1097, 1112, 1116, 1118, 1132, 1133, 1170, 1171, 1189, 1207, 1209, 1235, 1237, 1246, 1266, 1267, 1273, 1281, 1282, 1283, 1284, 1285, 1303, 1311, 1367, 1368, 1369 | 45, 293, 311, 318, 333, 955 |
| `com/example/ui/viewmodel/StudioProviderLabel324.kt` | 26 | — | — | — |
| `com/example/ui/viewmodel/TrainPilotUiState.kt` | 486 | 204, 206 | 111, 116, 122, 215, 216, 217 | — |
| `com/example/ui/viewmodel/TrainPilotViewModel.kt` | 1414 | 111 | 640, 772, 773, 846, 849, 850, 1301 | — |
| `com/example/ui/viewmodel/VmActiveSets.kt` | 298 | — | — | — |
| `com/example/ui/viewmodel/VmAppearance.kt` | 217 | 24, 27, 62, 64, 80 | — | — |
| `com/example/ui/viewmodel/VmBackupSettings.kt` | 182 | 158, 160, 171, 175 | — | — |
| `com/example/ui/viewmodel/VmCalendar.kt` | 256 | — | 27, 38 | — |
| `com/example/ui/viewmodel/VmExerciseLibrary.kt` | 216 | — | 41, 205, 208, 211, 212 | — |
| `com/example/ui/viewmodel/VmExerciseSwitch.kt` | 133 | — | 28, 29, 108 | — |
| `com/example/ui/viewmodel/VmFinishWorkout.kt` | 179 | — | 154, 155 | — |
| `com/example/ui/viewmodel/VmHealthJournal.kt` | 328 | — | — | — |
| `com/example/ui/viewmodel/VmHealthWeight.kt` | 289 | — | 23, 25, 26, 27, 31 | — |

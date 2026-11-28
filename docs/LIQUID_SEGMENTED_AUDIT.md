# Liquid segmented selection audit – release 2820

Base: `b83552ad25c005dc13326642779c25defa7c071e`. Scope: phone Compose mutually exclusive horizontal selectors, Liquid 33–40 only; keep standard/custom non-Liquid themes exactly as before. No new optical engine, per-item capture or route fade.

## Existing shared component users

- Main navigation: TopHeaderAndNav uses GlassSelectionPill; separate navigation shortcuts are actions, not tabs.
- Studio: QuickWorkoutHubSheet, ProgressTabScreen, GoalsEditDialog, StudioStatisticsScreen, HealthTabScreen, LogWorkoutDetailScreen, ProgramEditorScreen, SettingsMainScreen, HealthWeightAndGoals, ProgramsListScreen, ExerciseLibraryScreen, CalendarPlannerScreen, ProgressVolumeAndMuscle.
- Native hosts: JournalScreen, ProgressScreen, WeightJournalScreen, HealthDashboardScreen, ExerciseLibraryScreen, HealthRecordBrowser.

## Migrated in this candidate

| Host | Choice groups |
| --- | --- |
| BackupRestoreScreen | JSON / ZIP / DRIVE |
| HealthSourcesContent | Foreground and background intervals |
| HealthTodayTileExtras | Active / total kcal |
| StudioLiveWorkoutHealth | Provider (when multiple providers available) |
| Native CustomExerciseDialog | Muscle, measurement, load |
| Native PersonalProgramPreviewDialog | Template |
| CustomThemeEditorSheet | Base style, color role |

## Deliberate exclusions / further verification

- Independent actions (save, restore, delete, shortcuts), standalone toggles/favorites, multi-select equipment/exclusions, dropdown fields, color swatches, chart bars, expandable metric cards and semantic exercise feedback grids (pain warning and difficulty colors) are not one-dimensional tabs; do not combine them into a single pill.
- NativeMainHeader is a legacy parallel native navigation declaration, not the active Studio main header; search returned no other caller. CustomThemePreview uses its resolved custom palette rather than the parent Liquid palette. Keep these paths unchanged pending actual reachability evidence.
- Search across LiquidSegmentedTabs, FilterChip, selectableGroup, selected state and backup-specific tags; inspect candidate source files. This is not exhaustive runtime navigation coverage. Device audit for every modal, narrow widths, all eight palettes, four languages, larger font and reduced-motion remains necessary. Existing standard branch code is kept. CI required before claiming compile or test success.

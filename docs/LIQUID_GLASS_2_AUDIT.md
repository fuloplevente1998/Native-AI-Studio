# Liquid Glass 2.0 – G0 audit (2026-11-25)

Base: `430368755840f91c36165e44e8399006c4897254` (main). Independent branch `design/liquid-glass-2`. #379 is an alternative implementation; do not merge or cherry-pick it. #372/#378 are test-only. Source: user-provided G0–G6 specification, 13 original screenshots and Gemini calendar reference. Mockup data/layout changes are not functional requirements.

## V7 material baseline
- GlassOptics: 32→20 dp blur, .40→.06 tint, 1.4→1.8 saturation, .018→.13 bend, .30→.55 bevel, .30→.45 specular. Keep approved blur endpoints until physical comparison; do not weaken contrast. GlassContrast caps composite luminance at .13; a .40 tint leaves little room for the backdrop at Frosted. Header uses same coat and guard as panel, despite a different role.
- TrainPilotGlassEngine samples shared procedural scene in root coordinates but evaluates material per surface. GlassContentBackdrop records local content and applies lens effects separately; opaque surface under uncovered captured area can flatten the header. GlassAwareScaffold underlaps five tabs/settings, not arbitrary sheets or measuring routes. Inspect actual capture bounds on device.
- GlassSelectionPill already moves with two springs; reuse it. Header calendar/settings icon surfaces use opaque palette fills; nav track uses alpha paint rather than a shaped lens. Compact shortcuts use own lens. Do not add a second engine.
- GlassScene owns animated background and photo; reuse, retain lifecycle/low rate and no readback. API31–32 vector/blur fallback, software/old API stable path; active measurement retains cheap static material.

## Surface inventory and rollout
| Surface | Current evidence / unresolved audit | Target phase |
|---|---|---|
| Header and 5 tabs | TopHeaderAndNav: opaque calendar/settings, alpha nav track, shared moving pill | G1/G3 |
| Calendar | CalendarPlannerScreen: existing material on main panels, opaque normal days, selection border, primary filled actions; PlannerControls uses glassTile for fields | G2 |
| Home | HomeScreen, HomeCoachAdviceCard, StudioTodayStatusGrid: mixed material; next-workout/coach recommendation reported opaque; no new cards | G4 |
| Programs | ProgramsListScreen, ProgramCard, ProgramEditorScreen: existing outer material, activation/inner controls to audit | G3/G4 |
| Coach | CoachDetailScreen: readiness, next workout, exercise suggestions; reported opaque panels; no synthetic advice | G4 |
| Log | LogTabScreen, LogWorkoutDetailScreen, LogRouteMapScreen: existing tile path, search/filters/delete/rows/detail audit | G4 |
| Workout | WorkoutTabScreen, WorkoutTabDayCards, ExerciseLibraryScreen, ExerciseDetailScreen: existing outer material, controls audit | G4 |
| Active measurement | ActiveStrength/Timed/Bilateral/OutdoorGps screens, StudioActiveWorkoutTop, StudioWorkoutHandoverBar, StudioStrengthSetCard, StudioRestTimerDock: reported opaque body/control/rest panels; preserve lightweight path | G4/G5 |
| Progress | ProgressTabScreen, ProgressVolumeAndMuscle, MuscleHeatmapCard, StudioVolumeChart: mixed material, dark muscle panel; retain existing male anatomy and data | G4 |
| Statistics | StudioStatisticsScreen: cardio/steps opaque sections reported; charts/selection and empty states unchanged | G4 |
| Health | HealthTabScreen, HealthTiles, HealthWeightAndGoals, WeeklyGoals, HealthSourcesContent: mixed glass, opaque body/fitness/weekly goals reported | G4 |
| Settings | SettingsMainScreen, GoogleCloudSyncCard, WatchAndSyncScreen, BackupRestoreScreen, PermissionsScreen, theme editor/preview: mixed material, cloud card opaque | G4 |
| Secondary | TrainPilotControls, TrainPilotButton, TrainPilotFrames, TrainPilotModalBottomSheet, TrainPilotScreenSheet, dialogs, menus, inputs and snackbars: inventory call sites, preserve meaningful opacity and semantics | G3/G4 |
| Wear | WearHomeTheme/WearUiTokens: opaque only; contrast validation, no protocol changes | G5 |

Source inspection in this initial audit is targeted to the material, header, calendar and existing regression contracts. Other rows are inventory/observations, not claims of a completed call-site audit. Complete call-site inspection before modifying each group.

## Baseline plan (physical, not yet measured)
Galaxy A57: record identical theme/background, font size, brightness, refresh rate and power mode on base and candidate. Capture 0/50/65/100 at calendar, header (top and scrolled), all tabs and sheets; compare eight glass themes, legacy and custom, bright photo. Measure frame timing (p50/p95/p99, jank), CPU/GPU/memory, cold/warm launch, slider drag, long log scroll, workout timer/restore and background idle under matched conditions. API33+ AGSL logs, API31–32 fallback and software fallback. Verify 48dp targets, TalkBack, HU/EN/DE/RO and large fonts. Record source SHA and raw CI XML, no fabricated results. No device/emulator build in this session.

## Scope / next gate
G1 role-based optics within existing renderer, keep contrast and fallback; G2 calendar reference; G3 common controls; G4 complete surface audit/integration; G5 measured optimization and Wear color check; G6 full regressions and physical acceptance checklist. No main merge, version, dependency or protocol change.

## G4 rollout evidence
G1–G3 JVM and R8 verified, G3 run #45611727232 (1224 tests). G4: 15 source files above, including GPS controls while preserving map paint. Home, Programs, Workout, Progress, Stats and Settings already have shared card material at selected audited sites; this is not a blanket verification of all nested controls. Remaining: secondary settings/screens and dialogs, stats subpanels, detailed log/exercise, real device contrast/geometry and fallback review. `LiquidGlassG4Test` protects legacy paint scope and measurement route gating. G4 candidate CI pending.

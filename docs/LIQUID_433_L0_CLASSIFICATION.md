# #433 L0 – központi paint hívások besorolása (részleges)
Baseline: b14dc497, az audit: LIQUID_433_AUDIT.md. A jelen rész nem állítja a 310 fájl összes hívásának lezárását.

| Hívás / útvonal | Besorolás | Döntés |
| --- | --- | --- |
| LiquidGlassPalettes.withGlassClarity: tile és fallback kontrasztmodell | közös | a valódi paletta átadása |
| glassTile / glassControlFill | közös + standard ág | Liquid RGB palettaeredetű; standard/custom legacy érintetlen |
| GlassSurfaces.glassOverlayFill / overlayRoleFill / overlayRoleRim | közös | fix kék és szürke általános menüfestés kiváltása |
| TrainPilotGlassEngine.trainPilotGlassMaterial: LocalGlassOverlay | közös | az aktuális paletta továbbadása |
| TrainPilotGlassEngine.glassSceneModifier és GlassContentBackdrop.liveGlassLens: opak fallback | közös | témaszín, eredeti alpha |
| GlassSurfaces.glassStateTile és KyantGlass accent | szemantikus kivétel | megadott accent marad, közel azonos luminanciahatár |
| programDaySelectionAccent: PlannerControls, RangeCalendarSheet, ProgramEditor | specifikált zafír kivétel | L1-ben nem változik |
| ThemeSelectionColors.selectionColors | közös, már palettafüggő | változatlan; közvetlen felhasználásokat L3-ban felmérni |
| GlassOptics.TINT, fehér/színtörő perem | optikai kivétel | L1 nem módosítja |
| LiquidDropMaterial.liquidDropPaints | közös, már háttérárnyalatot használ | megmarad |
| GlassContentBackdrop.glassHeaderControlPaint | potenciális holtkód | nincs igazolt aktív hívó; csak szignatúra-igazítás, L6 törlési kapuk nyitottak |
| ExerciseLibrary raw primary/surface, natív dropdown és IconButton | megkerülő útvonal jelölt | külön L3 híváshely-vizsgálat |
| Minden más auditált fájl / hívás | nyitott | kézi besorolás és regresszió szükséges |

Ellenőrzés: default main kódkeresés, rögzített audit ág teljes érintett fájljai; main és audit ág Kotlin forrása induláskor azonos (csak két docs fájl tért el). Nincs teljes hívásgráf vagy készülékes vizsgálat.

## L2 központi press híváshelyek
- trainPilotClickable/CombinedClickable → trainPilotPressIndication → glassPressMotion → glassTouchBubble (közös, egy lokális owner).
- TrainPilotButton/ActionButtons/TextButton: glassPressMotion; GlassPrimaryButton/OutlinedButton: explicit glassTouchBubble. A TrainPilotPressFeedback csak Liquid focus/hover tint, standard press tint változatlan.
- LiquidDropMaterial és KyantGlass: korábbi redundáns teljes kontroll press/drag layer eltávolítva, resting highlight/innerShadow megmarad. A GlassSelectionPill saját selection-state mozgása megmarad.
- TopNavPillItem: közös press indication, a régi optics-off Material ripple eltávolítva. Külön nav X, videógomb, backup format, Material raw controls, chart és egyedi slider hívók L3-ban nyitottak.
- Kompakt vizuális gyerek (TrainPilotDeleteIcon, MuscleHeatmapCard): a pointer helyi koordinátájának elsőbbsége, a nagyobb parent hit target változatlan. Készülékes margó/gesztus validáció még szükséges.


## L3 first active sites
- ExerciseLibraryScreen category Surface and row/queued Start: active bypass, now shared bounded control paint, unchanged standard branch. Three top filters: shared chip, explicitly cheap inside chrome. Native favorite IconToggleButton and menu entries remain to classify.
- StudioStatisticsScreen StatisticsTabs: active shared chip, cheap selection in pinned chrome to avoid nested lens. Other chart/record state colors unchanged.
- TrainPilotControls disabled field: shared tile paint, legacy pass-through. Other field/input paths remain to verify.
- ProgramsListScreen fixed action background: existing explicit standard-only background; not removed blindly. Approved selector and active card not edited.


## L5 source triage (not a fix)
- Both MainActivity files, GlassSceneHost, GlassAwareScaffold, TrainPilotRouteContent, SettingsMainScreen, StudioPaletteCategories, PalettePickerSheet, VmAppearance and VmBackupSettings inspected. Active manifest entry is com.repforge.app.MainActivity. No direct navigation in selectPalette/syncNativeSettings. Route keys independent of palette. Settings reveal and DataStore feedback timing require integration reproduction. No causal claim or runtime modification yet.

## L1/L3 compatibility continuation
- LiquidGlassPalettes.withGlassClarity surface/surfaceElevated: active common compatibility fields (Material theme, nativeStudioColors, raw icon fills), now palette-derived bounded opaque hue instead of white coat composite; optical cardPaint preserved. Standard/custom branch returns before calculation.
- PersonalPlannerModal: four raw base and five elevated Surface sites → shared glassTile. DropdownMenu alias already shared, restriction red outlines/labels semantic exceptions. Standard pass-through. Native popup items still separate press review.
- ProgramTypeEmblemIcon: non-highlighted raw elevated surface → glassTile; active highlight explicit state retained.
- LiquidDropMaterial: normal palette-primary accent uses background hue; explicit other accents remain semantic; remember cheap gradient brush.

## L4 first optical integration
- #378 reference (b16bcd90) single Kyant fill/edge. Current fixed chrome duplicate paint/rim: shared live path now precomposites coat and retains one Kyant highlight. Old API/overlay fallback unchanged. Minimum blur and alpha left intact pending visual verification. Tile/Performance remain no backdrop.

## L5 implementation
- VmAppearance / VmBackupSettings: active request/data feedback race guarded by request identity, no route changes. SettingsMainScreen and StudioPaletteCategories: active keyed item/attached tile reveal coordinated with viewport. PalettePickerSheet explicitly retains chosen category. New full root + real store regression (not run).

## L3 native call-site continuation / L6
- Popup items: library, planner, personal planner, custom exercise, map, health weight, workout strength values, journal set/effort, training mode, GPS. Shared native Material semantics, one local Liquid indication. Standard calls retain Material ripple. Favorite toggle and active icon actions similarly wrapped; special video/close/backup visual children use local indication. No new capture.
- Removed two definition-only renderer helpers after symbol/negative-test search. Legacy screen reachability is not fully proven; retain those files. L0 full 310-file classification, chart/slider remaining bespoke interaction, and actual render performance remain open.

## L3 targeted device report follow-up
- ProgressTabScreen: Liquid selection uses the same Selection fill and rounded rim vocabulary as approved ProgramsList; retains 48/42dp and semantics. Scrolling chrome remains guarded; non-underlap Statistics branch uses cheap tile instead of extra material. Standard sizing and selection unchanged.
- HealthTabScreen Today/Weight icon backgrounds use common translucent selection material and rim; standard branch unchanged. ThemeSelectionColors opaque compatibility fill now uses palette-derived Selection composition and includes lens worst-case in readable foreground bounds for Liquid only.
- ProgramsList new-program primary button uses cheap material atop the existing pinned glass action bar (no nested live lens). Approved Programs tab selector untouched. Physical comparison pending.

## L2/L3 adjustable and chart control continuation
- TrainPilotChartTap (shared line graph): existing nearest point + gesture, one local source in Liquid; cancellation on scroll and no change to standard. StudioVolumeChart raw clickable → shared chart click with original semantics. WeeklyGoals, NativeCardioProgressPanel, HealthTodayTileExtras, StudioStatisticsScreen and ProgressVolumeAndMuscle already shared clickable; measurement marks intentionally neutral/semantic, not a background defect.
- LiquidGlassControl, GoalsEditDialog: native Slider via shared Liquid press/drag, exact ranges and callbacks retained. CustomThemeColorWheel remains native (custom color editor, not general Liquid control). TrainPilotSwitch active only; native checkbox active only in custom exercise/cloud sync, passive onboarding checkbox deliberately unchanged. ExerciseFeedbackChips/WorkoutFeedbackPopup native selectable → shared local selection, pain red unchanged.

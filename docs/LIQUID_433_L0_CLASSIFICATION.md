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

# Liquid Glass 2.0 – G0 kiindulási audit (2811)

Forrás: main `43036875` (#377 után) kódja, `rg` statikus felmérés; csak a Liquid Glass család (33–40). Képi állítás
nincs: a „tömör” oszlop a kódban látható kitöltést jelenti (opak `palette.surface*`, `tintedSurface`, `cardPaint`),
nem készülékes megfigyelést. Spec: `docs/LIQUID_GLASS_2_SPEC.md`. Rövidítés: `ex/` = `app/src/main/java/com/example/`.

## 1. Motor és közös anyagút (egyetlen háttér, egyszer fordított programok)
| Elem | Fájl | Mit csinál | G1 szerep |
|---|---|---|---|
| Lap-háttér (élő foltok / saját kép), gyökérben egyszer | `ex/ui/theme/GlassScene.kt`, `GlassAwareScaffold.kt` | `trainPilotGlassScene` a tartalom-dobozon; lapok átlátszók (`trainPilotPageBackdrop`, `trainPilotScreenBackdrop`) | – (háttér) |
| Kártya/csempe/chip anyag (procedurális háttér mintázása gyökér-koordinátában) | `ex/ui/theme/TrainPilotGlassEngine.kt` (`trainPilotGlassMaterial`) | API33 AGSL `SCENE_LENS`; API31–32 Compose-jelenet + tónus; <31 opak | Panel (alap) |
| Élő tartalom-lencse (fejléc, nav pill, téma-előnézet) | `ex/ui/theme/GlassContentBackdrop.kt` (`glassHeaderMaterial`, `liveGlassLens`) | `GraphicsLayer` rögzítés → platform blur → `CONTENT_LENS` | Navigation / Selection |
| Vektoros perem | `TrainPilotGlassEngine.kt` (`trainPilotGlassRim`, `drawGlassHighlights`) | felül erős, oldalt/alul gyenge vonal | szerep szerinti erősség |
| Optika, kontraszt | `GlassOptics.kt`, `GlassContrast.kt`, `GlassShaders.kt` | csúszka → paraméterek; `backdropCap`; `SURFACE_LUMA_MAX = 0,13` | tokenek |
| Keretkapu | `rf/ui/theme/TrainPilotFrames.kt` | semleges keret eltűnik, szemantikus marad | – |
| Mozgás | `GlassSelectionPill.kt`, `GlassMotionState.kt`, `GlassTouchBubble.kt`, `rf/ui/theme/TrainPilotPressFeedback.kt` | csúszó pill, nyomás-rugó, érintési buborék | Selection |

Halmozás: a kártyák nem rögzítenek/blureznek külön réteget – a közös jelenet-programot mintázzák (AGSL-en belüli
9 pontos szórás). Valódi platform-blur csak a fejléc-lencséken fut (fejléc, pill, 2811 előtt a Coach/Napló gyorsgomb
saját lencséje is – ez redundáns volt, G1-ben megszűnt).

## 2. Felületek képernyőnként (main)
| Képernyő / felület | Fájl | Jelenlegi anyag | Tömör maradt? |
|---|---|---|---|
| Felső fejléc | `ex/ui/components/common/TopHeaderAndNav.kt` | élő lencse (Panel), téglalap | N, de sötét (lásd 4.) |
| Felső nav sáv + pill | ugyanott, `GlassSelectionPill.kt` | sáv: `surface` 22% fátyol + perem; pill: élő lencse | sáv: részben (sötét fátyol) |
| Coach / Napló gyorsgomb | `TopHeaderAndNav.kt` (`TopNavPillItem compact`) | saját élő lencse a fejléc-lencse fölött | N (redundáns blur) |
| Naptár / Beállítások gyorsgomb | `TopHeaderAndNav.kt` | `surfaceElevated` opak kitöltés | **I** |
| Kezdőlap kártyák (profil, program hős, mai állapot, gyorscsempék) | `ex/ui/screens/HomeScreen.kt` | `trainPilotGlassMaterial` ×4 | N; belső `tintedSurface` jelvények/„Következő edzésnap” **I** (opak keverék) |
| Mai javaslat (Coach) | `ex/ui/components/HomeCoachAdviceCard.kt` | anyag + `glassTile` | részben (2811/2 szerint tömör hatású) |
| Programok lista, kártya, gyors edzés lap | `ProgramsListScreen.kt`, `ProgramCard.kt`, `QuickWorkoutHubSheet.kt` | anyag ×4, csempe; lap | részben (5 opak `surface*` hely) |
| Edzés fül, napkártyák | `WorkoutTabScreen.kt`, `WorkoutTabDayCards.kt` | anyag ×7, csempe | N |
| **Naptár + tervező** (G2 referencia) | `CalendarPlannerScreen.kt`, `PlannerControls.kt` | 3 panel anyag, napcellák `glassTile` | **I**: napcellák, Újratervezés/Tervezés gombok, 5 opak hely |
| Gyakorlatkönyvtár, részletek, programszerkesztő | `ExerciseLibraryScreen.kt`, `ExerciseDetailScreen.kt`, `ProgramEditorScreen.kt` | nincs anyag | **I** |
| Napló lista / részletek | `LogTabScreen.kt`, `LogWorkoutDetailScreen.kt` | lista: perem + csempe; részletek: nincs | részletek **I** (7 opak hely) |
| Fejlődés | `ProgressTabScreen.kt`, `ProgressVolumeAndMuscle.kt` | anyag ×5, csempe ×9 | izomcsoport, kardió/lépés kártya, napcsempék **I** (2811/2) |
| Statisztikák | `StudioStatisticsScreen.kt` | anyag ×2, csempe | N |
| Egészség, csempék, súly/célok | `HealthTabScreen.kt`, `HealthTiles.kt`, `HealthWeightAndGoals.kt`, `WeeklyGoals.kt` | anyag ×3, csempe-perem | Heti célok, Test és fittség belső csempék **I** |
| Coach részletek | `CoachDetailScreen.kt`, `StudioCoachDomainPanels.kt` | perem + csempe; `tintedSurface` | részben **I** |
| Beállítások főoldal | `SettingsMainScreen.kt` | anyag ×4 | Google Cloud sor **I** (`GoogleCloudSyncCard.kt`, 4 opak hely) |
| Óra és szinkron, engedélyek, mentés | `WatchAndSyncScreen.kt`, `PermissionsScreen.kt`, `BackupRestoreScreen.kt` | csak lap-háttér | **I** (kártyák opakok) |
| Aktív edzés: erő / időzített / kétoldalas | `ex/ui/screens/measure/Active*WorkoutScreen.kt`, `StudioStrengthSetCard.kt` | anyag (mérőképernyőn Compose-út, nincs AGSL) | edzéstest-panel **I**, kétoldalas 7 opak hely |
| Aktív edzés: pihenő dokk, „Vezérlés: telefon” sáv, felső sáv | `StudioRestTimerDock.kt`, `StudioWorkoutHandoverBar.kt`, `StudioActiveWorkoutTop.kt` | nincs anyag | **I** |
| Kültéri GPS, összegzés | `ActiveOutdoorGpsScreen.kt`, `OutdoorGpsDashboard.kt`, `WorkoutSummaryScreen.kt` | nincs anyag | **I** (összegzés 7 opak hely) |
| Lapok (sheet) | `TrainPilotModalBottomSheet.kt` (+ 9 hívó) | saját ablak: procedurális jelenet + anyag | N |
| Párbeszédablak, legördülő menü | pl. `CalendarPlannerScreen.kt` Dialog | opak (szándékos, rendszerablak) | I – elfogadott |
| Téma-előnézet | `LiquidGlassControl.kt` | `liveGlassLens` | N |

## 3. Közös komponensek és használati helyek (fájlszám, `rg -l`)
| Komponens | Fájl | Glass út | Hívó fájlok |
|---|---|---|---|
| `TrainPilotFilterChip` | `ex/ui/components/TrainPilotControls.kt` | nem kijelölt: anyag + perem | 18 (Könyvtár, Statisztika, Egészség, Forrás, Súly, saját téma …) |
| `TrainPilotPrimaryButton` / `SecondaryButton` | ugyanott | anyag + perem, kapszula | 8 / 5 |
| `TrainPilotTextField` | ugyanott | nincs anyag (G3: Input) | 23 |
| `TrainPilotButton` | `rf/ui/theme/TrainPilotButton.kt` | perem + nyomás + buborék | 5 |
| `TrainPilotOutlinedButton` | `rf/ui/theme/TrainPilotOutlinedButton.kt` | buborék, nincs anyag | ~46 |
| `trainPilotClickable` / `glassPressMotion` | `rf/ui/theme/TrainPilotPressFeedback.kt` | nyomás-rugó | 72+ fájl |
| `TrainPilotModalBottomSheet` | `ex/ui/components/TrainPilotModalBottomSheet.kt` | jelenet + anyag | 9 |
| `glassTile` / `glassCardFill` | `ex/ui/theme/LiquidGlassPalettes.kt` | csempe-tónus / átlátszó kártya | 16 / 15 |
| `TopNavPillItem`, `GlassSelectionPill` | `TopHeaderAndNav.kt`, `GlassSelectionPill.kt` | lencse | 1 (fejléc) |

## 4. V7 optikai paraméterek (kódból, `GlassOptics.compute`) és Frosted–Clear elkülönülés
| Csúszka | homály dp | tónus α | telítés | `backdropCap` | spekuláris | fénytörés | kromatikus |
|---|---|---|---|---|---|---|---|
| 0 Frosted | 32 | 0,40 | 1,40 | ~0,002 | 0,30 | 0,018 | 0,004 |
| 50 | ~25 | 0,23 | 1,60 | ~0,042 | 0,375 | 0,074 | 0,018 |
| 65 (alap) | ~23,6 | 0,18 | 1,66 | ~0,060 | 0,40 | 0,091 | 0,028 |
| 100 Clear | 20 | 0,06 | 1,80 | ~0,105 | 0,45 | 0,13 | 0,060 |
Perem: ~1 dp vonal + 3 dp fény; felül 100%, oldalt 40%, alul 27% (× spekuláris), lap-őr `lumaCap` 0,125.

Elemzés (nem hangolás): a homály 32→20 dp csak 1,6×; a lágy foltos háttéren (folt sugara ~300 dp) ez alig látszik,
csak éles tartalom (szöveg, kártyaél) fölött – azaz a fejléc-lencsén. A két vég közti fő különbséget a tónus és a
`backdropCap` hordozza: Frosted végen a háttér az üveg alatt gyakorlatilag fekete (cap ≈ 0,002), a 40% fehéres tónussal
egyenletes szürke – „matt”, de színtelen; Clear végen a szín átjön. Tehát elkülönülnek, de a Frosted nem homály, hanem
fedés miatt más. Javaslat: blurt NEM csökkentünk feltételezésből; készüléken (A57) összevetni 0/50/65/100%-on:
(a) Kezdőlap görgetés tetején (fejléc csak háttér fölött), (b) fejléc szöveges kártyák fölött görgetve, (c) kártya folt-
határon, (d) világos saját háttérkép. Kérdés: Frosted végen maradjon-e több szín (kisebb tónus, nagyobb cap).

## 5. Fejléc gyökérok (2811/1, kódban igazolva)
1. **Nincs alatta szín:** görgetés tetején a fejléc alatt csak a `LocalGlassTopInset` távtartó + a lap-háttér van. A négy
   folt közül csak a 0. (jobb felső) éri el a fejléc-sávot, a bal felét sodrás-fázistól függően alig: a sáv (felső 7%)
   legkisebb folt-súlya ~0,015 (1080×2340), azaz ott a sötét alapszín (pl. `#172333`) látszik.
2. **A korlát sötétre vágja:** a fejléc a kártyák anyagát (Panel) kapta: 65%-on tónus 0,18 → `backdropCap` ≈ 0,060, a lap
   őre 0,125 – a fejléc a saját hátterét a lap fénysűrűségének felére nyomja, Frosted végen ~0-ra; erre jön a fehéres
   tónus → egyenletes szürkéskék sáv. A nav-sáv `surface` 22% fátyla tovább lapítja.
3. A Naptár/Beállítások gyorsgomb opak `surfaceElevated`; a Coach/Napló gomb külön lencsét rajzol a fejléc-lencse fölé.
Javítás (G1, kódolva): Navigation szerep (tónus ×0,45 → 65%-on cap ≈ 0,097, Clear ≈ 0,118; telítés ×1,25), 5. folt a
fejléc bal fele alatt (sáv min. folt-súly ≥ 0,27), sáv = semleges csempe-tónus, gyorsgombok = csempe-tónus + perem
(nincs opak kitöltés, nincs második lencse). A kontraszt-plafon (0,13) változatlan; a szöveg ≥4,5:1 marad.

## 6. Készülékes (nem igazolható itt)
Tényleges képi hatás 0/50/65/100%-on, AGSL fordítás Samsung Skián, A57 frame time (görgetés, pill, csúszka; az 5. folt
~25% több jelenet-számítás pixelenként), API 31–32 Compose út, világos saját háttérkép.

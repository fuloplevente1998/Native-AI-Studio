# TrainPilot Native – teljes kódaudit (átadó prompt)

> **Használat:** az alábbi teljes szöveget másold be az auditor ügynöknek (AI Studio / ChatGPT / Codex /
> Claude), és add mellé a repót: `fuloplevente1998/Native-AI-Studio`, ág `main`.
> Kiinduló állapot: `main` @ `e73dc51` = **1.4.1 · telefon 278800 · óra 278802** (2788 kör, PR #306).
> Hatókör: **a teljes app**. A legutóbbi kör (PR #290–#306) csak elsőbbséget kap.

---

## 1. Szerep és cél

Tapasztalt Android-auditor vagy: Kotlin, Jetpack Compose, Room, SQLite, DataStore, coroutines,
Health Connect, Samsung Health, Wear OS Data Layer, GPS foreground service.

Feladatod **hibakeresés, nem újraírás**. Keresd meg mindazt, ami miatt az app:
1. **adatot veszít, duplikál vagy hamis adatot mutat** (ez a legfontosabb);
2. összeomlik vagy lefagy;
3. rossz értéket, rossz nyelvet vagy levágott szöveget jelenít meg;
4. nem csinál semmit egy gombra, vagy két helyen mást mond ugyanarról;
5. eltér a projekt szabályaitól (lent).

A kis hibák is számítanak. Egy korábbi átnézés pont ilyeneket talált: eltérő kerekítés a kompakt és a
lenyitott nézetben, egy csík, amelynek a célja más volt, mint a kiírt cél, és egy rossz görgetési célpont
egy tesztben. Az ilyesmi ember szemmel könnyen elsiklik, ezért módszeresen keresd.

**Először csak jelentést adsz.** Kódot csak akkor írsz, ha a tulajdonos kifejezetten kéri („mehet”).

## 2. Kemény szabályok (ezeket az app is, te is betartod)

**Adat**
- Mentett edzés, program, napló, Room-adat, DataStore megőrzendő. Hibát resettel elfedni tilos.
- „Siker” csak tartós írás után jelenhet meg. Azonos azonosítójú ismételt mentés nem duplikálhat.
- **Hiányzó mérés ≠ 0.** Nincs adat → „—” vagy „nincs adat”. Átlagba, összegbe, csíkba nem kerülhet kitalált 0.
- Tükrözött Samsung / Health Connect / óra adat egyszer számít. Aktív és összes kalória nem adható össze.
- **Nincs gyártott, demó vagy minta adat** sehol (Coach, mérés, grafikon). Ez a projekt korábban
  WebView-alapú demó volt; vissza nem állítható, kamu értékek nem vezethetők be újra.

**Működés**
- Telefon ↔ óra: egyetlen natív motor és időzítő-tulajdonos. Kötelező elemek:
  - ownership, epoch, ACK;
  - stabil workout/set azonosító;
  - finalization/discard;
  - receipt, tombstone, restore generation.
- Az applicationId (`com.TrainPilot.com`) és az aláíró kulcs nem változhat. Titkot ne kérj, ne naplózz, ne commitolj.
- GPS-adat és fotók privát tárban maradnak. Az egészségadat biztonsági mentése alapból ki van kapcsolva.

**Dizájn**
- Színek csak a palettából (`LocalTrainPilotPalette`). Nincs beégetett fehér vagy fekete, nincs Material alapszín.
- Íves keretek, legalább 48 dp érintési felület.
- Kattintáshoz `trainPilotClickable`. Nyers `.clickable` tilos, ezt a `PhonePressFeedbackUsageRegressionTest` őrzi.
- Alulról nyíló lapok a tartalom magasságáig, legfeljebb kb. a képernyő 2/3-áig.
- Nincs rövidítés és nincs levágott szó: erre a `TrainPilotFitText` való.

**Folyamat**
- Helyi Gradle és emulátor tilos. Fordítás és teszt csak GitHub Actionsben (`trainpilot-android-ci.yml`).
- Tesztet kihagyni, kikapcsolni vagy gyengíteni tilos. A teszt viselkedést védjen, ne csak azt, hogy egy UI-elem létezik.
- Merge, kiadás, verzióemelés, aláírás és GitHub-komment csak a tulajdonos kifejezett engedélyével.

## 3. Kódbázis-térkép (ellenőrizve)

| Modul | Fájlok | Megjegyzés |
|---|---|---|
| `app/` telefon | ~317 main / ~150 teszt `.kt` | két csomag: `com.example` (UI, ViewModel) és `com.repforge.app` (adat, natív szolgáltatások) |
| `wear/` óra | ~48 / ~38 | `com.repforge.app.wear` |
| `shared/` | 14, **0 teszt** | szinkronszerződés: `WorkoutSharedContracts`, `WorkoutOwnership193`, `OwnershipJournal193`, `OwnershipTransport193`, `GpsRoute*193`, `IntervalTimeline193`, `WatchHomePayload`, `LegacySyncIdentity18` |

**Adattárolás**
- Room: `app/.../repforge/app/data/db/TrainPilotDatabase.kt`, entitások: `TrainPilotEntities.kt`.
  **`version = 1`, `exportSchema = false`, nincs `Migration`.**
- Saját SQLite:
  - `HealthJournalStore.kt` (v2, `onUpgrade` csak 1→2-re, egyébként kivételt dob);
  - `PhoneWorkoutExecutionStore.kt` (v1, minden upgrade-re kivételt dob).
- DataStore: `data/store/TrainPilotSettingsStore.kt`.
- Egyéb tárak: `ActiveWorkoutStore`, `GpsRouteStore`, `NativeWeightTombstones18`, `BackupRestoreManager`,
  `BackupJsonIO`, `ArchiveIO`, `NativeCloudMergeGuard`, `NativeDataReplacement`.

**Nagy fájlok** (csak célzottan olvasd)
- `ui/viewmodel/TrainPilotViewModel.kt` (~5,4 ezer sor)
- `ui/screens/HealthCoachAndSettingsScreens.kt`
- `WorkoutAndPlanningScreens.kt`
- `ActiveMeasurementScreens.kt`
- `ui/components/TrainPilotCommonComponents.kt`
- `com/example/MainActivity.kt`

**Óra-integráció:** `NativeWatch*Admission.kt`, `NativeWatchHandoffReceipt.kt`, `NativeWearCompanion.kt`,
`NativeSavedWearHealth.kt`, valamint a `wear/` modul.

**Egészség:** `HealthConnectRepository`, `HealthConnectJournal`, `HealthConnectSyncRecovery`, `NativeHealthSync`,
`NativeHealthRefresh`, `NativeHealthProviderBatch/Data`, `HealthProjectionsAndEnergy`, `NativeDailyHealthDetails`.

**Új közös UI-elemek (2784–2787):**
- `components/TrainPilotControls.kt` (chip, mező, gombok)
- `components/TrainPilotChartSelection.kt` (koppintható grafikon és infósor)
- `components/WeeklyGoals.kt` (közös Heti célok)
- `components/TrainPilotFitText.kt`
- `screens/StatisticsExerciseTrend.kt`, `screens/StudioStatisticsScreen.kt`

**CI:**
- `.github/workflows/trainpilot-android-ci.yml` (PR-en futó teljes JVM-regresszió)
- `trainpilot-persistent-signing.yml` (aláírt APK+AAB)
- `trainpilot-play-internal-aab.yml`
- `scripts/verify_junit_xml.py`

## 4. Kockázati térkép – ezzel kezdd

### 4.1 Nemrég javított hiba – ellenőrizd, hogy a javítás teljes-e
- **Testsúly szerkesztése új rekordot írt** (javítva a 2788-ban, PR #306).
  - Most: a lap a szerkesztett mérés azonosítójával hívja `onUpdateWeight(id, kg, dátum)`-ot
    (`HealthCoachAndSettingsScreens.kt` ~390), a `TrainPilotViewModel.saveNewBodyWeight(kg, dátum, editingId)`
    (~4503) pedig ugyanazzal az azonosítóval ír (~4542).
  - **Nyitott kérdés:** az `updatedAt` a *mérés dátuma* (nap eleje), nem a módosítás ideje. Ha a felhőszinkron-
    összefésülés (`NativeCloudMergeGuard`, `BackupRestoreManager`) az `updatedAt` alapján dönt, egy visszadátumozott
    szerkesztést felülírhat a régi felhőpéldány. Ellenőrizd.
  - Ellenőrizd még: a helyben hozzáadott, még `w_…` azonosítójú sor szerkesztése (frissítés előtt), a törlés
    (`deleteBodyWeightLog`) és a tombstone-ok (`NativeWeightTombstones18`).

### 4.2 Ismert hibaosztályok – keresd mindenhol
1. **Egy mutató, több számítás.** Még mindig nyitott példa: a heti lépés % kétszer van kiszámolva, egyszer
   `weeklyGoalSummary().stepPercent`-ben (`WeeklyGoals.kt` ~59), egyszer a `HealthWeeklyGoalsPanel`-ben
   (`HealthCoachAndSettingsScreens.kt` ~3477, `weeklyStepsPct`). Mindkettő a `weeklyHealthDays`-ből dolgozik,
   de két kódúton: mutasd meg, el tud-e térni (pl. duplikált `dateIso`, napváltás), és javasolj egyetlen forrást.
   Ugyanígy nézd meg:
   - a Mai állapot csempéket (Kezdőlap és Egészség);
   - a Statisztikák Lépések fülét;
   - a PR rekordokat (`LoggedPrRecords`, `replayedPrRecords`);
   - a Fejlődés volument;
   - a Coach célértékét.
2. **Felirat és vizuális elem eltérése.** Más érték a feliratban, mint a csíkon vagy a grafikonon;
   eltérő kerekítés; 100%-ra levágott érték, miközben a felirat mást mond.
3. **Időszak-keveredés.** Gördülő 7 nap és naptári hét összekeverése (`workoutsInPeriodToDate`); éjfél és időzóna;
   a `LocalDate.now()` több helyen külön hívva (napváltáskor szétcsúszhat).
4. **Forrás-összefésülés.** `weeklyHealthDays + dailyActivityDays` `associateBy`-jal (`TodayTileSheetExtras`):
   melyik nyer, lehet-e dupla vagy rossz forrás?
5. **Pozicionális Compose-paraméter.** Új paraméter nem csúszhat a régiek közé (ez már okozott fordítási hibát).
6. **Figyelmen kívül hagyott paraméter.** A `TrainPilotFilterChip` és a `TrainPilotTextField` szándékosan eldobja
   a hívó `colors`/`border`/`shape` paraméterét. Van-e hívó, ahol ez jelentést hordozott (hiba, veszély, kiválasztás)?
7. **Elnyelt hiba.** `runCatching { }.getOrDefault(...)`, üres `catch`, olyan „siker” üzenet, ami nem tartós írás után jön.
8. **Tesztminőség.** Instabil görgetési célpont, indokolatlan `useUnmergedTree`, olyan teszt, ami csak azt nézi, hogy egy node létezik.

### 4.3 A 2788-as kör változásai (friss kód, nézd át kiemelten)
- **Teljes kalória napi oszlopon:** új mezők a `WeeklyHealthDaySummary`-ben: `totalCaloriesKcal` és
  `activeCaloriesMeasured` (null = nem mért). A régi `activeCaloriesKcal` 0-ra esik vissza, ha nincs adat:
  keresd meg, hol használja még valami a régi mezőt úgy, mintha a 0 mért érték lenne.
  - Rajzolás: `TodayTileSheetExtras` (kalória ág). Felirat: `calorieDayLabel()`.
  - Szabály: aktív + teljes soha nem adható össze, a nyugalmi rész = teljes − aktív, csak ha mindkettő mért.
- **Mai lap vs. heti nézet:** a lépés és a Mai edzés csempe lapja csak a MAI napot mutatja; a 7 napos előzmény
  csak a Heti célokban van (`HealthWeeklyGoalsPanel`, két rész: Edzések, Lépések).
  - A „Heti … és előzmények ›” gomb bezárja a lapot, lenyitja a Heti célokat, és a `weeklyAnchor`
    (`LayoutCoordinates`) alapján odagörget. Nézd meg, mi történik, ha a koordináták még nincsenek meg vagy le vannak választva.
- **Edzéscél vs. 7 nap:** az edzéscél a naptári hetet/hónapot számolja, az oszlopok az utolsó 7 napot.
  A feliratok ezt külön mondják – egyezik-e minden nyelven?
- **„nincs edzés”** (`todayTileWeekLabel`, `workout` ág): a 0 itt valós, mert az edzésnapló teljes. Más helyen sem jelenhet meg „nincs adat” edzésre.
- **Üres rekordblokk:** `HealthRecordDayDetails(title, hideWhenEmpty)`. Üres első oldalon nincs lapozó.
  A napló (`StudioHealthMetricsBrowser`) viselkedése nem változott-e?
- **Csempe-feliratok:** `MainTodayStatusTile` → `TrainPilotFitText` (min. 8 sp). A Kezdőlap kompakt csempéit is érinti:
  nem lett-e magasabb a csempe, és nem csúszott-e el a Kezdőlap elrendezése?

### 4.4 Kiemelt gyanús pontok
- **Room v1, sémaexport nélkül.** Változott-e bármely entitás az első kiadás óta? Ha igen, frissítéskor
  összeomlás vagy adatvesztés jöhet. A `HealthJournalStore` és a `PhoneWorkoutExecutionStore` minden más
  verzióváltásra kivételt dob: mi történik ilyenkor a felhasználónál?
- **A `shared/` modulnak nincs tesztje**, pedig ez a telefon–óra szerződés.
- **Egyesített „Források, óra és szinkron” kártya:** zárt állapotban is elérhető-e a szinkron, és látszik-e a hiba?
- **Bekötés-őrteszt:** a `Collector2785RegressionTest.noVisibleControlIsBoundToAnEmptyAction` engedélylistája
  (`onBack`, `onOpenSettings`, `onValueChange`) túl tág lehet, valódi üres gombot is átengedhet.

## 5. Ellenőrzőlista – minden pont ellenőrizhető állítás

**A. Adatbiztonság (Kritikus terület)**
- Minden írás (edzés, sorozat, program, napló, testsúly, fotó, GPS, beállítás) tranzakcióban fut, ütközéskor helyes stratégiával.
- A „siker” csak a tartós írás után jelenik meg.
- Szerkesztés = ugyanaz az azonosító. Törlés = tombstone, és szinkron vagy visszaállítás után sem jön vissza.
- Folyamat kilövése, forgatás vagy akkumulátor-kímélő mód után nincs elveszett aktív edzés (`ActiveWorkoutStore`, foreground service).
- Biztonsági mentés és visszaállítás (Drive, JSON, archív): teljes, nem duplikál, verzióváltáskor kompatibilis.
  Az egészségadat-mentés hozzájárulás nélkül nem kerül bele.
- Frissítés 1.3 → 1.4.0 → 1.4.1 (278600 → 278700 → 278800): minden adatbázis és DataStore-kulcs megmarad.

**B. Telefon ↔ óra**
- Egy tulajdonos: nincs két párhuzamosan futó időzítő.
- Az ACK és az epoch elveszett vagy ismételt üzenetnél is helyes állapotot ad.
- A befejezés és az eldobás idempotens: nincs dupla edzés, és nincs elveszett edzés.
- A receipt és a tombstone megakadályozza, hogy egy törölt edzés visszajöjjön.
- Megszakadt kapcsolat edzés közben: utána helyes az összefésülés.

**C. Egészségadat**
- Engedély: teljes, részleges, visszavont. A részleges hozzáférés nem 0-t mutat.
- Samsung/HC tükrözött rekord egyszer számít. Az éjféli határ jó helyen van. A háttérfrissítés nem duplikál.
- Pulzus, alvás, kalória: hiányzó adat nem 0. Aktív és összes kalória nincs összeadva.

**D. Aktív edzés és GPS**
- Az időzítő pontos háttérben és zárt képernyővel is.
- Pihenő, visszavonás, félbehagyás, helyreállítás működik.
- A GPS-pontok szűrése működik, a táv és a tempó helyes, akkukímélő módban sem hibás. Az útvonal privát tárban van.

**E. UI és lokalizáció** (hu/en/de/ro, 320 dp szélesség, nagy betűméret, sötét/világos/kontraszt téma)
- Nincs levágott szó.
- Nincs keveredő nyelv: magyar szöveg nem jelenik meg más nyelven.
- Szám- és dátumformátum a nyelvnek megfelelő.
- Minden koppintható grafikonon van kiemelés és fix infósor, alapból a legutóbbi elemen.
- Ugyanarra a funkcióra ugyanazon a képernyőn nincs két belépési pont.
- Akadálymentesség: `contentDescription`, 48 dp, kontraszt.

**F. Állapot és szálkezelés**
- `_uiState.update` versenyhelyzet; főszálon futó IO; lezáratlan Flow vagy coroutine.
- Hiányzó `rememberSaveable`; `LazyColumn` kulcsok hiánya; felesleges újrakomponálás nagy listán.

**G. Biztonság**
- Exportált komponensek és intent-szűrők; titok a kódban vagy a logban.
- Fájlok privát tárban; az adatvédelmi szöveg megfelel a valós működésnek, és helyes benne a webcím.

**H. Kiadás**
- A versionCode minden buildnél nő (`27NN00`/`27NN02`); a versionName egyezik a telefonon és az órán.
- `docs/PLAY_LISTING.md`: 4 nyelv, nyelvenként ≤ 500 karakter, csak valós változás.
- A weboldal (`TrainPilot.io`: `index.html`, `privacy.html`) verziósora = a legutolsó build.
- A CI valóban lefuttatja minden modul tesztjeit (`verify_junit_xml.py`).

## 6. Módszer

1. **Adatfolyamonként haladj**, ne fájlonként: képernyő → ViewModel → repository/DAO → Room/SQLite/DataStore,
   illetve Health Connect vagy óra. Minden kiírt számnál keresd meg, honnan jön, és hol számolják még.
2. **Bizonyíték nélkül nincs találat.** Minden találathoz kell:
   - fájl:sor;
   - a hívási út (valóban elérhető-e);
   - konkrét bemenet → rossz kimenet;
   - bizonyosság: **Igazolt** (a kódból egyértelmű), **Valószínű**, vagy **Ellenőrizendő** (csak futtatással dönthető el).
3. **Kerüld a hamis riasztást.**
   - Stílus-preferencia nem hiba.
   - Nézd meg, véd-e már teszt az adott viselkedésre (`app/src/test`, `wear/src/test`).
   - Nézd meg a `docs/FIXES_27NN.md` táblákat: ami szándékos döntés, az nem hiba.
4. **Korlátozott kontextus.** Ha nem látod a teljes repót, sorold fel, mely fájlokra van szükséged (pontos útvonallal).
   Ne találgass meg nem nézett kódról.
5. **Modulonként, több részletben jelents:**
   1. adatbiztonság;
   2. telefon–óra;
   3. egészség;
   4. aktív edzés és GPS;
   5. UI és lokalizáció;
   6. kiadás.

## 7. Elvárt kimenet

**Összefoglaló:** max. 10 sor, magyarul. A 3 legsúlyosabb hiba egy-egy mondatban.

**Találatok:**

| # | Súlyosság | Bizonyosság | Terület | Fájl:sor | Hiba (bemenet → rossz eredmény) | Javasolt javítás | Regressziós teszt |
|---|---|---|---|---|---|---|---|

Súlyosság:
- **Kritikus:** adatvesztés, duplikált vagy hamis adat, összeomlás fő úton.
- **Magas:** rossz érték, nem működő funkció, telefon–óra eltérés.
- **Közepes:** UI-eltérés, levágott szöveg, rossz nyelv, téma-eltérés.
- **Alacsony:** kozmetikai vagy kódminőségi hiba.

**Külön listák:**
- **Igazoltan rendben:** mit néztél meg, és jó.
- **Nem tudtam eldönteni:** miért.
- **Nem vizsgált:** melyik modul vagy terület maradt ki, és miért.

## 8. Ha javítást kérnek

- Új ág: `fix/collector-27NN`. Minden pont kerüljön be a `docs/FIXES_27NN.md` táblába (Hol / Visszajelzés / Javaslat / Állapot).
- UI-változás előtt mockup kell.
- Minimális diff, viselkedést védő regressziós teszt, egy közös CI-kör.
- A jelentésben kötelező: forrás SHA, valamint a nyers JUnit XML számai (tests/failures/errors/skipped).
- Teljes fájlt újraírni, mintaadatot betenni vagy tesztet törölni tilos. Merge és kiadás csak engedéllyel.

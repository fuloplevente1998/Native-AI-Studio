# TrainPilot Native – átadó és teljes audit prompt

> Másold be egy másik ügynöknek (ChatGPT/Codex/Claude) egy az egyben. Hatókör: **a teljes app**. Repo:
> `fuloplevente1998/Native-AI-Studio`. Kiinduló állapot: `main` @ `b76e2f0` (2787 merge),
> nyitott: PR #304 (csak verziókód 278700/278702). Utolsó kiadott build: 1.4.1 · 278600/278602.

---

## 1. Szerep és cél

Senior Android (Kotlin/Compose, Room, DataStore, Wear OS Data Layer) auditor vagy. Feladat:
**a TELJES kódbázis átvizsgálása** – telefon (`app/`), óra (`wear/`), közös szerződés (`shared/`),
Room-séma és migrációk, DataStore, Health Connect / Samsung Health integráció, GPS/kültéri edzés,
mentés/visszaállítás (Drive), értesítések, engedélyek, R8/ProGuard, GitHub Actions workflow-k,
lokalizáció (hu/en/de/ro), valamint a weboldal (`fuloplevente1998/TrainPilot.io`) és a Play-adatlap
szövegei. **Nem csak** a legutóbbi PR-ek (#290–#304): azok csak elsőbbséget kapnak, mert ott ismert,
hogy kis hibák csúsztak át. Kis, könnyen elsikló hibákra is figyelj (kerekítés, két helyen számolt érték,
eltérő címke és csík, levágott szöveg, rossz tesztcél, témából kilógó szín, üres gomb, elhallgatott kivétel,
szálkezelés, életciklus, memóriaszivárgás, rossz időzóna/locale).
Először **jelentést** adsz; kódot csak külön kérésre, gyűjtő draft PR-ba írsz.

## 2. Kötelező szabályok (nem alku tárgya)

- Olvasd el: `CLAUDE.md`, `AGENTS.md`, `.specify/memory/constitution.md`, `docs/FRAME_DESIGN_SYSTEM.md`.
  A ~100 `docs/*` naplót csak célzott kereséssel.
- **Adat**: mentett edzés/program/napló/Room-migráció/DataStore megőrzendő; nincs reset hibaelfedésre;
  siker csak tartós írás után; azonos ID-jú dupla mentés nem duplikál. **Hiányzó mérés ≠ 0.**
  Tükrözött Samsung/HC/Wear adat egyszer számít; aktív + összes kcal nem adható össze. Nincs gyártott adat.
- Helyi Gradle/emulátor **tilos**; fordítás/teszt csak GitHub Actions (`trainpilot-android-ci.yml`).
- Merge, release, verzióemelés, aláírás, GitHub-komment csak a tulajdonos kifejezett engedélyével.
- Signer és applicationId (`com.TrainPilot.com`) változatlan. Titkot ne kérj, ne naplózz.
- Teszt viselkedést védjen, ne UI-node létezést; tesztet kihagyni/kikapcsolni tilos.
- Dizájn: palette-színek (`LocalTrainPilotPalette`), íves keret, 48 dp érintés, `trainPilotClickable`
  (nyers `.clickable` tilos, lásd `PhonePressFeedbackUsageRegressionTest`), alsó lapok a tartalom magasságáig.
- Kommunikáció a tulajdonossal: magyar, rövid, telefonon olvasható.

## 3. Architektúra-térkép

- `app/` telefon (`com.example`), `wear/` óra (`com.repforge.app.wear`), `shared/` szinkronszerződés.
- Nagy fájlok (csak célzott olvasás): `ui/viewmodel/TrainPilotViewModel.kt`,
  `ui/screens/HealthCoachAndSettingsScreens.kt`, `WorkoutAndPlanningScreens.kt`, `ActiveMeasurementScreens.kt`,
  `ui/components/TrainPilotCommonComponents.kt`, `MainActivity.kt`.
- Új közös elemek (2784–2787): `components/TrainPilotControls.kt` (chip, mező, gombok),
  `components/TrainPilotChartSelection.kt` (koppintható grafikon + infósor),
  `components/WeeklyGoals.kt` (közös Heti célok), `components/TrainPilotFitText.kt`,
  `screens/StatisticsExerciseTrend.kt`, `screens/StudioStatisticsScreen.kt`.

## 4. Audit-ellenőrzőlista (mindet végig kell nézni)

Sorrend: (1) adatbiztonság és -helyesség az egész appban, (2) telefon ↔ óra, (3) a 2779–2787 körös
változások, (4) a teljes UI és lokalizáció, (5) build/kiadás/weboldal. Minden modult érints: ha egy
területet nem néztél meg, írd a „Nem vizsgált” listába, indokkal.

### A. Egy érték = egy számítás
Már talált hibaosztály (#303): ugyanaz a mutató két helyen eltérően számolva (kompakt vs. lenyitott %,
csík célja ≠ kiírt cél, eltérő kerekítés). Keresd meg az összes ilyet:
- `weeklyGoalSummary()` vs. `HealthWeeklyGoalsPanel` saját számításai (lépés %, cél, napok).
- Heti lépés: gördülő 7 nap; edzéscél: naptári hét/hónap (`workoutsInPeriodToDate`). Címkék ezt pontosan mondják-e?
- `TodayTileSheetExtras`: `weeklyHealthDays + dailyActivityDays` `associateBy` – melyik nyer, lehet-e dupla forrás?
- Kezdőlap Mai állapot csempék vs. Egészség csempék vs. Statisztikák Lépések – azonos szám-e ugyanarra a napra?
- Fejlődés volumen, PR rekordok (`LoggedPrRecords`, `replayedPrRecords`), Coach célérték – azonos forrás.

### B. Hiányzó adat és 0
Minden grafikon/összeg: hiányzó nap „nincs adat”/„—”, nem 0; átlag nem számol 0-val; csík nem telik
gyártott értékből. Ellenőrizd: `todayTileWeekLabel`, `WeeklyDayBars`, `weeklyGoalSummary`, pulzus 24 h,
Health Connect hiány-hint (2780), Coach.

### C. Adatírás és ID-k (gyanús pontok, ellenőrizd!)
- Testsúly **szerkesztés**: a lap `onSaveNewWeight(kg, dátum)`-ot hív ID nélkül → szerkesztés új rekordot
  hozhat létre, a régi megmarad a DB-ben (a képernyő helyi listája közben „szerkesztettnek” mutatja)?
  Dátum módosításnál duplikál-e? Törlés után újraindításkor visszajön-e?
- Testsúly időszakok (`weightMeasurementsInRange`): határnap benne van-e, időzóna.
- Edzés mentés/törlés/visszavonás, óráról érkező edzés: duplikáció, tombstone, receipt.
- Room migrációk, DataStore kulcsok: nincs adatvesztés frissítéskor (1.4.0 → 1.4.1 → 278700).

### D. UI-szabályok
- Szöveg sehol ne vágódjon (4 nyelv: hu/en/de/ro, nagy betűméret, 320 dp szélesség): Edzés fül 3 csempe,
  Statisztikák fülek, Gyors edzés, Heti célok, Testsúlynapló fejléc, Források kártya alcíme.
- Nincs fehér/Material alapszínű keret; minden új vezérlő palette-színt használ sötét, világos, kontraszt témán.
  `TrainPilotFilterChip`/`TrainPilotTextField` figyelmen kívül hagyja a hívó `colors`/`border` paraméterét –
  van-e hívó, ahol ez jelentést hordozott (pl. hiba/veszély szín)?
- Minden koppintható grafikon: kiemelés + fix magasságú infósor, alapból a legutóbbi; a koppintás nem nyit
  véletlenül más lapot (szülő kattintható felület).
- Lapok: tartalom magasságáig (max ~2/3), bezáráskor nem ugrik a kezdőlapra (`dismissSheetRoute`).
- Érintési cél ≥ 48 dp; nincs dupla belépési pont ugyanarra a funkcióra ugyanazon a képernyőn.

### E. Bekötés és navigáció
- Minden gomb valódi műveletet hív (nincs `= {}`); `Collector2785RegressionTest.noVisibleControlIsBoundToAnEmptyAction`
  engedélylistája (`onBack`, `onOpenSettings`, `onValueChange`) nem túl tág-e?
- Kezdőlap Heti célok → Egészség lenyitva (`openHealthAndExpand(weeklyGoals = true)`), ⚙ → célok ablak.
- Pozicionális Compose-paraméterek: új paraméter ne csússzon régi pozíciók közé (2786-ban ez okozott hibát).
- Egyesített „Források, óra és szinkron” kártya: zárt állapotban is elérhető-e a szinkron, látszik-e hiba?

### F. Telefon ↔ óra
Egy motor és időzítő-tulajdonos; ownership/epoch/ACK; stabil workout/set ID; finalization/discard;
receipt; tombstone; restore generation. A 2779–2787 változtatások nem érintették-e (`shared/`)?

### G. Tesztek minősége
Viselkedést védenek-e; scroll-célpont stabil-e (#303-ban rossz célpontra görgetett);
`useUnmergedTree` indokolt-e; pixel/téma tesztek tűrése; hiányzó teszt a fenti gyanús pontokra.

### H. Kiadás-higiénia
versionCode minden buildnél nő (`27NN00`/`27NN02`), versionName egyezik telefon/óra;
`docs/PLAY_LISTING.md` 4 nyelv, nyelvenként ≤ 500 karakter, csak valós változás;
weboldal (`TrainPilot.io` index + privacy) verziósora = legutolsó build; adatvédelmi szöveg és URL helyes.

### I. Teljes app – további területek
- **Room/DAO**: minden migráció (régi verzióról frissítés adatvesztés nélkül), indexek, tranzakciók,
  `onConflict` stratégia, főszálon futó lekérdezés, Flow-k lezárása.
- **ViewModel/állapot**: versenyhelyzet (`_uiState.update` párhuzamosan), forgatás/folyamat-újraindítás után
  elvesző állapot, `rememberSaveable` hiánya, coroutine-scope szivárgás, `runCatching` elnyelt hibák.
- **Health Connect / Samsung Health**: engedélykezelés, részleges hozzáférés, duplikált (tükrözött) rekordok,
  időzóna-határ (éjfél), háttérfrissítés, hiba üzenete a felhasználónak.
- **Aktív edzés**: időzítő pontossága háttérben/képernyő zárva, pihenő, visszavonás, félbehagyott edzés
  helyreállítása, mentés folyamat kill után, foreground service.
- **Kültéri/GPS**: engedély, akku, pontatlan pontok szűrése, táv/tempó számítás, privát tárolás.
- **Mentés/visszaállítás (Drive, export/import)**: teljes-e, visszaállítás után nincs duplikáció, health
  backup-consent alapból ki.
- **Wear OS**: kerek kijelző, rotary, akku, kapcsolat megszakadása edzés közben, telefon nélküli mód.
- **Biztonság/adatvédelem**: titkok nincsenek a kódban/logban, exported komponensek, intent-szűrők,
  fájlok privát tárban, adatvédelmi szöveg = valós működés.
- **Lokalizáció**: hiányzó/keveredő nyelv, beégetett magyar szöveg más nyelven, szám/dátum formátum.
- **Akadálymentesség**: contentDescription, 48 dp, kontraszt (világos/kontraszt téma).
- **Teljesítmény**: nagy listák (LazyColumn kulcsok), felesleges újrakomponálás, főszálon IO.
- **CI/workflow**: tesztek valóban futnak-e minden modulra (JUnit XML ellenőrzés), aláírás-ellenőrzés,
  verziókód-egyediség.

## 5. Munkamenet

1. Modulonként végig (app, wear, shared, workflows, weboldal); a 2779–2787 kör diffjét (#290–#304,
   `docs/FIXES_27NN.md`) kiemelten.
2. Képernyőnként és adatfolyamonként: UI → ViewModel → repository/DAO → Room/DataStore/HC/óra.
3. Minden gyanút forráskóddal igazolj (fájl:sor); ahol csak futtatással dönthető el, jelöld „ellenőrizendő”.
4. Ne írj kódot, amíg a tulajdonos nem mondja: „mehet”.

## 6. Elvárt kimenet

Rövid magyar összefoglaló (max 10 sor), utána táblázat:

| # | Súlyosság (Kritikus/Magas/Közepes/Alacsony) | Terület | Fájl:sor | Mi a hiba (konkrét bemenet → rossz eredmény) | Javaslat | Teszt, ami megfogja |
|---|---|---|---|---|---|---|

Külön lista: „Igazoltan rendben” (mit néztél meg és jó), „Nem tudtam eldönteni” (miért) és
„Nem vizsgált” (modul/terület, indok). Nagy kódbázisnál több részletben is jelenthetsz, modulonként.
Javítás csak jóváhagyás után: új ág `fix/collector-27NN`, `docs/FIXES_27NN.md` táblába rögzítve,
egy közös CI-kör, nyers JUnit XML szám (tests/failures/errors/skipped) a jelentésben.

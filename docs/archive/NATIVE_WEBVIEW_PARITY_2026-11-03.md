# WebView main → Native Studio: funkcióparitás, 2026-11-03

**Frissítés PR90 / 2748 után:** a teljes26 metrikás böngésző és a kért gyakorlatonkénti visszajelzés most mainen, a közös aláírt APK-ban. PR/signed CI nyers428, JS7 sikeres; [aktuális átadó](CURRENT_STATUS.md). #69 régi formátumok teljes auditja és #75 nyitott; #74 sztornó, #82 később, #95 következő kör. #59 a friss készülékes visszajelzés szerint továbbra is tényleges beágyazási hiba, a belső böngésző működik. Az alábbi régi forráshelyek, státuszok és tünetek történeti kiindulópontok, nem a mostani implementáció hiányának bizonyítékai. A készülékes elfogadás külön marad.

## Forrás és az audit határa

- Régi, kanonikus telefonapp: `fuloplevente1998/TrainPilot` main **`5f026a56b9b6d33c84afa77eb3927b11f0bd6f6a`**, `www/app.js` (20 599 sor), `www/index.html`, `docs/FEATURES.hu.md` és az `ai-studio/` mesterspecifikáció/inventory.
- Vizsgált új telefonapp: `fuloplevente1998/Native-AI-Studio` main **`deebfddd7f0259e589e9791582ad6d2d38fcf4e2`**. A launcher `com.repforge.app.MainActivity` ténylegesen a **TrainPilotAppRoot / com.example** Studio felületet nyitja. Az importált `TrainPilotAppScaffold` és az alatta lévő régi natív képernyők léte nem bizonyít Studio-elérhetőséget.
- A régi 107 soros inventory (81 funkció + 26 metrika) térkép a követelményekhez, nem a mostani állapot bizonyítéka. Régi PARTIAL/OPEN státuszát nem másoltuk át készülékes tényként. A PR279 ötleteit és a régi UI-elképzeléseket nem tekintjük automatikusan a stabil WebView app kész funkcióinak.
- Ez funkciószintű **statikus forrás- és belépésiút-audit**, a lent részletezett hiányokra konkrét bizonyítékkal. Nem teljes 107-pontos készülékes újraminősítés és nem minden tárolási/versenyhelyzet teljes kódauditja. Százalékos „készültséget” nem állítunk.
- Implementáció, aktuális CI, APK-átadás és felhasználói készülékes elfogadás négy külön állapot. A forrásban meglévő, de UI-ból nem hívott motort **bekötési hiánynak**, nem újraírandó funkciónak tekintjük.

## Ami már megvan – ne oszd ki újból teljes újraírásra

| Terület | A mostani natív kód helyzete | Fennmaradó határ |
|---|---|---|
| Profil, saját terv alapjai | Tartós profil, aktív program, terv-előnézet/generálás és elfogadás létezik | A régi history128/variáció minden ágának megőrzése külön audit; nem új onboarding készítése |
| Programok és könyvtár | Létrehozás, aktiválás, keresés/kategória, gyakorlat-hozzáadás, sorrend és prescription szerkesztése | #79-ben bekötve, készülékes elfogadás külön |
| Edzés és életciklus | Natív session, explicit pause/resume, aktív intervallumok, idő/kétoldali/GPS és tartós finalizálás/mentés/elvetés | Előtöltés, RIR/effort, preset/auto kapcsolat lent; nem új motor |
| Napló és törlés | Valós Room lista/részlet, piros listatörlés és megerősített fizikai törlés, fotók, útvonal, Health-források | #79-ben natív szerkesztő/jegyzet/feedback; teljes per-exercise feedback #69 |
| Fejlődés | Dátumozott volumen, idő/edzésszám/PR/izomterhelés; napi/heti oszlopok, előző időszak százalék | Gyakorlatonként minden régi grafikon/1RM/részletszűrő teljessége nincs teljesen újraminősítve |
| Egészség | Külön provider, grant/szinkron állapot, mai és heti adatok, pulzus/recovery projekció | Teljes dátumböngésző/26 metrika, valamint #61 konkrét hibái lent |
| Testsúly, fotók | Dátumos testsúlymentés/törlés, fotó hozzáadás/nézet/törlés és importvédelem létezik | Saját régi mentés és fizikai kamera/picker/restarthoz külön elfogadás |
| Naptár | Havi napok, helyi ISO dátum, tervezés/újratervezés, törlés/Undo és Google adapter létezik | Valós OAuth/Google-naptár és minden ütemezési variáns külön készülékes próba |
| Videó | Ellenőrzött URL, belső YouTube/WebView és belső böngészős fallback mainen | #59 tényleges lejátszás/háttérhang/beágyazási hibák készülékes ellenőrzése |
| Wear | Eredeti ownership/ACK/offline motor, mérések, Tile/komplikáció; valós órainfó DTO/Sync a telefonon | #60 kompakt UI #79-ben; valós páros elfogadás külön |
| Mentés/Drive | JSON/ZIP streamelt import/export, privát fotók, health consent, redo/replacement/tombstone létezik | Saját mentés, valódi process kill, Drive/OAuth és hibatűrés fizikai ellenőrzése |
| Témák, nyelvek | Témakövető naptár/térkép, témák és nyelvválasztó létezik, #67 kompakt Settings | Négy nyelv teljes felületi lokalizációja hiányos; #75 |

## Auditkori eltérések és mostani megvalósítási állapot

### 1. P1 – Előző megfelelő edzés súly-/ismétlés-előtöltése a Studio indításban — [68](https://github.com/fuloplevente1998/Native-AI-Studio/issues/68)

**Most: implementálva #77-ben, main81da2f7, CI402; nem újrakiosztandó kódhiány.**

**Régi viselkedés:** `www/app.js:812–885, tp228PrefillSets`. Inventory: B01, A10.

**Mostani bizonyíték:** `app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:784, 2789, 2829`.

A startPlannedWorkout/startWorkoutWithExercises a buildDefaultSetsForExercise értékeit használja. A régi megfelelő alkalom súlya és ismétlése nem kerül át. A kanonikus session dayId jelenleg session, ezért valódi program/nap-azonosság és legacy-kompatibilitás is kell.

**Elvárt eredmény:** Programtól/naptól függetlenül, azonos gyakorlat-ID/loadType/repUnit mellett megfelelő, mentett és teljesített erősítő alkalomból öröklődjön súly és ismétlés, sorozatonként. Gyors és tervezett indítás; új/saját/importált gyakorlat. Kézi módosítás és aktív draft megmarad. RIR, done, idő, GPS, ownership nem öröklődik. Valós program/nap-azonosság, legacy fallback csak bizonyítható egyezésnél.

**Megrendelési állapot:** A felhasználó kérte: a mostani APK átadása UTÁN implementáljuk.

### 2. P1 – Opcionális tényleges RIR és kanonikus fájdalom/effort visszajelzés — [69](https://github.com/fuloplevente1998/Native-AI-Studio/issues/69)

**Most: részben implementálva #77-ben (opcionális RIR, root-feedback, pain); további gyakorlatonkénti feedback-paritás #69-ben nyitott; a naplószerkesztő és edzésjegyzet #79-ben bekötve.**

**Régi viselkedés:** `www/app.js:20241 tp197RirSave; 16869 TrainPilotWorkoutFeedback`. Inventory: B15, B16, C03.

**Mostani bizonyíték:** `app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:784, 825, 3215`.

A Studio minden új sorozathoz rir=2 értéket ad és actualRir-ként menti, külön megadási jel nélkül. A feedback magyar effort szöveg, míg a kanonikus Coach feedback.rating és gépi pain/hard értéket vizsgál.

**Elvárt eredmény:** A hiányzó actualRir maradjon hiányzó; mért 0 külön kezelve. A felhasználó kifejezett RIR-je megmarad export/import/restart során. A pain/hard és a többi kanonikus effort-állapot eljusson a Coach-hoz, régi adatok migrációja adatvesztés nélkül. A RIR-cél nem tényleges mérés.

**Megrendelési állapot:** A Coach bekötésével közös szerződés; nem szabad párhuzamosan átírni ugyanazt a VM-et.

### 3. P1 – Valódi Coach tanácsok, következő nap és tartós progresszió bekötése — [70](https://github.com/fuloplevente1998/Native-AI-Studio/issues/70)

**Most: implementálva #77-ben, main81da2f7, CI402; nem újrakiosztandó kódhiány.**

**Régi viselkedés:** `www/app.js:3514 rf233ExerciseAdvice; 19989 tp197UiRequestSave`. Inventory: C11–C14, E17.

**Mostani bizonyíték:** `app/src/main/java/com/example/ui/screens/HealthCoachAndSettingsScreens.kt:1770; app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:4123`.

A Studio Coach nem használja a CoachViewModel/NativeCoachAdviceRepository teljes tanácslistáját, az ajánlott nap az első nap. Az updateProgressionSettings csak UiState-et módosít. A kanonikus motor és NativeTrainingModePicker már létezik, új motort nem kell írni.

**Elvárt eredmény:** Adatalapú gyakorlatonkénti ajánlás és indok; NativeCoachTargets szerinti valódi program/nap/naptári cél. A kijelzett nap induljon. Szolgáltató- és frissességvédelem; fájdalom/terhelés megmarad. Módok valós mentése, előnézet és tudatos elfogadás. Adathiánynál nincs kitalált tanács. Studio arculat megmarad.

**Megrendelési állapot:** A felhasználó kérte: az APK átadása UTÁN implementáljuk. Előtöltéssel összehangolt munka.

### 4. P2 – Teljes egészségnapló, dátumszűrő és metrikarészletek elérése a Studio felületen — [71](https://github.com/fuloplevente1998/Native-AI-Studio/issues/71)

**Most: Részben implementálva #79-ben: dátumtartomány/rekordböngésző/alvásszakasz; teljes26 metrikás összesítő még nyitott.**

**Régi viselkedés:** `www/app.js:16075 recordHtml; NativeHealthMetrics 26 kulcsa`. Inventory: D01–D04, D07, D14.

**Mostani bizonyíték:** `app/src/main/java/com/repforge/app/ui/health/HealthRecordBrowser.kt; app/src/main/java/com/repforge/app/ui/health/NativeHealthMetrics.kt; app/src/main/java/com/example/ui/screens/HealthCoachAndSettingsScreens.kt`.

A Studio főleg mai és 7/14 napos összesítéseket mutat. A teljes dátumtól/dátumig lapozó, nyers rekord, alvásszakasz és mind a 26 metrika részletezője nincs elérhetően bekötve. Ezekhez natív háttérkomponens már van.

**Elvárt eredmény:** Dátumszűrés és lapozás valódi tárolón; forrás/időbélyeg/hiány/hiba helyes; összes támogatott metrika és alvásszakasz hozzáférhető. Nincs fake érték, nincs provider-keverés. A mai kártyák kompaktak maradnak.

**Megrendelési állapot:** Rögzített backlog, most nem kezdjük el.

### 5. P2 – Program- és sajátgyakorlat-műveletek valódi bekötése, kedvencek — [72](https://github.com/fuloplevente1998/Native-AI-Studio/issues/72)

**Most: Implementálva #79-ben: valódi program-/nap-/gyakorlatműveletek és tartós kedvencek, kanonikus védelmekkel.**

**Régi viselkedés:** `www/app.js:1540 rf144Favorites; program- és exercise-kezelők`. Inventory: A07, A08, A12, A13.

**Mostani bizonyíték:** `app/src/main/java/com/example/ui/screens/WorkoutAndPlanningScreens.kt:1380; app/src/main/java/com/example/MainActivity.kt:475–566`.

A programtörlés feliratos művelet jelenleg onBack-et hív. A Studio útvonalon nincs teljes programmásolás/törlés, nap hozzáadás/törlés, gyakorlateltávolítás a napból, meglévő saját gyakorlat szerkesztés/törlés és tartós kedvenckezelés. Létrehozás/aktiválás/sorrend/prescription-szerkesztés már létezik.

**Elvárt eredmény:** Minden felsorolt művelet valódi Room-módosítás, hibánál nincs hamis siker; aktív session/import/szinkron és beépített elemek védelme. Kedvencek backupban megmaradnak. Meglévő napló gyakorlatreferenciái nem sérülhetnek.

**Megrendelési állapot:** A felhasználó közös tesztcsomagként kérte, #79-ben implementálva.

### 6. P2 – Naplózott edzés valódi szerkesztése és utólagos visszajelzés a Studio részletben — [73](https://github.com/fuloplevente1998/Native-AI-Studio/issues/73)

**Most: Implementálva #79-ben: a natív naplószerkesztő, edzésjegyzet és utólagos visszajelzés bekötve.**

**Régi viselkedés:** `www/app.js naplórészletek és TrainPilotWorkoutFeedback`. Inventory: C02, C03.

**Mostani bizonyíték:** `app/src/main/java/com/example/MainActivity.kt:709–730; app/src/main/java/com/repforge/app/ui/journal/JournalEditorDialog.kt`.

A Studio naplórészletben a natív törlés/Health/fotó/útvonal elérhető, de a létező JournalEditorDialog/openEditor szerkesztési út nincs bekötve. A meglévő visszajelzés szerkesztése sem teljes.

**Elvárt eredmény:** Mentett sets/reps/súly/tényleges RIR és megjegyzés/visszajelzés tudatos szerkesztése, előnézet/megerősítés a meglévő natív szerkesztőn. Cache/PR/Coach frissüljön, idők/ownership/health nem találhatók ki.

**Megrendelési állapot:** Közös tesztcsomagként kérve és #79-ben bekötve; #57 törlése megmarad.

### 7. P2 – Intervallum-/automata beállítások tartós és kanonikus motorbekötése — [74](https://github.com/fuloplevente1998/Native-AI-Studio/issues/74)

**Régi viselkedés:** `www/app.js:18631 interval config`. Inventory: B07–B09, E17.

**Mostani bizonyíték:** `app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:2746; app/src/main/java/com/repforge/app/data/store/TrainPilotSettingsStore.kt:256`.

Az updateIntervalSettings és toggleAutoMode a Studio UiState-et írja; a teljes globális/edzésenkénti preset és kanonikus setTimerDefaults kapcsolat hiányos. Az alap natív stopper/szünet/folytatás meglétét ez nem vitatja.

**Elvárt eredmény:** A kiválasztott munka/pihenő/kör/auto mód valódi motorhoz és tartós presethez jusson, restart/programváltás után helyesen; hang/haptika jogosultság és háttéridő megmarad. Ne legyen második időzítő.

**Megrendelési állapot:** A pontos preset/start útvonal külön megvalósítási auditot igényel; most backlog.

### 8. P2 – Studio teljes HU/EN/DE/RO lokalizációjának befejezése — [75](https://github.com/fuloplevente1998/Native-AI-Studio/issues/75)

**Régi viselkedés:** `www/app.js tp149T, 4 nyelv`. Inventory: E16.

**Mostani bizonyíték:** `app/src/main/java/com/example/ui/screens/HealthCoachAndSettingsScreens.kt; app/src/main/java/com/example/ui/screens/WorkoutAndPlanningScreens.kt`.

Nyelvválasztás és sok fordítás már van, de több látható képernyőszöveg közvetlen magyar literal; a teljes négy nyelv paritása nincs kész.

**Elvárt eredmény:** Minden felhasználói felirat és állapot lokalizált, dátum/számformátum helyes, hosszú DE/RO feliratok és nagy betű használható. Nem pusztán a négy nyelvgomb léte számít késznek.

**Megrendelési állapot:** Rögzítés, most nem implementáljuk.

## Metrikák és alvás részletei

A teljes `NativeHealthMetrics.definitions` 26 kulcsa már létezik a háttérben. A Studio `HealthTabScreen` nem ezt a teljes részletezőt használja. A bodyFatMassKg, skeletalMuscleMassKg, fatFreeMassKg, totalBodyWaterLiters, sleepScore, basalMetabolicRateKcal és bodyMassIndex például nincs a Studio VM/Health felület teljes metrikamegjelenítési útjába bekötve. A részletes alvásszakaszok, nyers rekord-időbélyegek és tetszőleges korábbi dátumtartomány szintén a #71 munkához tartoznak. Ez nem bizonyítja, hogy az adott készülék/szolgáltató minden metrikát valóban mér vagy olvashatóvá tesz.

## Külön, már rögzített új észrevételek

- #60: kompakt fejléc és két egymás melletti belső panel #79-ben implementálva, készülékes elfogadás külön.
- #61: a téves Samsung nyugalmi engedélyhiány-jelzés #79-ben javítva; Energy0–Coach #77-ben javítva. Az eredeti készülékes adathiány tényleges oka továbbra sem bizonyított.
- #48 és #35: #67 javítás mainen; friss APK-s visszateszt szükséges. A korábbi 2744-es készülékképek a megelőző megoldást cáfolták, nem a most átadandó #67-et igazolják.

## Sztornózott és nem megrendelt terület

**BLE-kezelő (inventory E09/E10 és B14 BLE-része): a felhasználó 2026-11-03-án kifejezetten sztornózta.** Nem kerül kiosztásra/implementációra. A meglévő fájlok és felhasználói adatok törlése nem feladat. Wear OS kapcsolat/egészségmérés ettől külön, megőrzendő működés.

Új, a stabil mainben sem igazolt ötlet, teljesítmény-kísérlet, alternatív főmenü vagy Play-publikálás nem része a most megrendelt Coach/előző alkalom feladatnak.

## Megvalósítási sorrend és ütközések

A felhasználó sorrendje teljesült: **#67 aláírt telefonos APK átadva → #70 Coach + #68 előtöltés mainen**, a szükséges #69 adatértelmezési feltételekkel. A kész kódot ne implementáld újra. A kanonikus motor, valódi program/nap-azonosság, kézi/draft-védelem és provider-izoláció megmarad. A régi `dayId=session` rekordot nem szabad találomra egy A/B naphoz rendelni.

A #72/#73 most #79-ben megvalósítva; #71 részleges, #74/#75 és #69 maradéka külön nyitott. Ugyanazt a `TrainPilotViewModel.kt`, `MainActivity.kt` vagy `HealthCoachAndSettingsScreens.kt` fájlt érintő feladatok nem adhatók párhuzamosan több chatnek közös írásra; új kiosztásnál ismét teljes fájltulajdon kell. Már mainen lévő #58/#62–#67 kódot ne cherry-pickelj újra. Alügynök nincs kérve.

A következő implementáció szükséges regressziói: megfelelő gyakorlat másik programnapról is; eltérő loadType/repUnit kizárása; gyors edzés; importált/hiányos/lejárt/törölt előzmény; sorozatonként eltérő súly; bodyweight/time/GPS kizárás; nincs örökölt actualRir/done; kézi érték/draft/ownership/restore megőrzése; Coach által jelzett nap indul; pain/hard korlátozás és hiányzó provider-adat. Teljes Android/Wear build és nyers XML a tényleges SHA-ra, majd külön készülékes elfogadás.

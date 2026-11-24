# 3. chat – Pulzus és regeneráció adatútja, diagnózisa

Hibajegy: https://github.com/fuloplevente1998/Native-AI-Studio/issues/61

## Közös kiindulópont és szigorú fájltulajdon

Repo: https://github.com/fuloplevente1998/Native-AI-Studio

Friss `main`-ből saját ág és saját worktree; PR célja `main`, de a végleges integráció/merge a koordinátoré. A main már tartalmazza a 2743-as #49 javításokat (358/358 teszt). A Coach rövid leírása és a piros/listabeli törlés #58 alatt külön készül/elérhető: ezt **ne írd újra**, induláskor ellenőrizd a PR állapotát. Ha még nincs mainen, a koordinátorral egyeztesd a bázist.

Olvasd el az aktuális átadót, `.specify/memory/constitution.md`, `docs/FRAME_DESIGN_SYSTEM.md`, a saját hibajegyet és a csomag `FAJLTULAJDON.md` fájlját. A friss main/hibajegy elsőbbséget élvez a régi snapshotokkal szemben. Magyarul kommunikálj; alügynököt ne indíts.

**Csak a saját alább felsorolt fájljaidat/mappáidat írd.** Másik chat fájlját olvashatod. Közös fájlt nem módosíthatsz „egy sor erejéig” sem. A szükséges bekötést írd a saját `INTEGRACIO_<azonosító>.md` fájlodba, pontos paraméterrel/importtal/adatleképezéssel és alapértelmezéssel. Új segédfájlt és tesztet kizárólag a saját névtereden hozz létre. Ha a feladat ennél nagyobb fájlhatárt igényel, jelezd a koordinátornak; önállóan ne foglald el a másik területét.

Foglalt közös fájlok: `MainActivity.kt`, `TrainPilotViewModel.kt`, `HealthCoachAndSettingsScreens.kt`, `StudioTodayStatusGrid.kt`, `HomeScreen.kt`, Gradle/manifestek/közös strings/theme fájlok és a közös átadók. Ezeket csak az integrátor köti be. Meglévő API új paramétere legyen kompatibilis alapértékű; új komponensed/DTO-d önállóan forduljon, ne hivatkozz még nem létező ViewModel-mezőre.

Natív Compose/Room/DataStore/motor, ownership/ACK, finalization, restore/tombstone és providerelválasztás marad. Ne találj ki adatot, ne adj össze providereket, ne kapcsolj ki védelmet. Package/signer/verzió/kiadási workflow nem a feladat-chaté. Android-build/teszt a meglévő Actionsből; nincs helyi Gradle/emulátor. Érdemi regressziók, aktuális SHA és valódi nyers XML eredmény; a folyamatban levő CI nem siker. Készülékes eredményt csak valódi próbáról állíts.

Átadás: saját PR, egyértelmű kész/hiányzó állapot, módosított fájllista, `INTEGRACIO_<azonosító>.md`, teszt-SHA/Actions/XML, szükséges fizikai próba. Új APK és main merge csak a koordinátortól. Másik chat WIP-jét vagy a megőrzött vegyes stash-t ne alkalmazd.

## Kizárólagos fájljaid

- `app/src/main/java/com/repforge/app/NativeDailyHealthDetails.kt`
- `app/src/main/java/com/repforge/app/SamsungSdkReader.java`
- `app/src/main/java/com/repforge/app/SamsungHealthRepository.kt`
- `app/src/main/java/com/repforge/app/ui/coach/NativeCoachReadiness.kt`
- `app/src/main/java/com/repforge/app/ui/coach/NativeCoachSignals.kt`
- `app/src/test/java/com/example/DailyHealthDetailsRegressionTest.kt`

Új fájlok csak ezekben a saját mappákban:

- `app/src/main/java/com/example/ui/components/healthreadings/`
- `app/src/main/java/com/example/model/healthreadings/`
- `app/src/main/java/com/repforge/app/healthreadings/`
- `app/src/test/java/com/example/followup/healthreadings/`

Átadási leírásod: `docs/parallel-handoffs/INTEGRACIO_health.md` (szintén kizárólag a tiéd).

## Feladat és bizonyított nyomok

A Mai állapot és a heti egészségadatok pulzusa/regenerációja hiányzik. A készülékes gyökérok még nem bizonyított. Már kért engedélyek: HC READ_HEART_RATE/READ_RESTING_HEART_RATE; Samsung READ_HEART_RATE/READ_ENERGY_SCORE. Nem állítható, hogy a telefon ezeket ténylegesen megadta vagy az adott naphoz van mérés.

Statikus nyomok: a heti UI-modellben nincs pulzusmező; a Mai állapot kizárólag averageHeartRate-et használ; a kanonikus `NativeCoachReadiness/Signals` és a Studio saját readiness képlete eltér. A `SamsungSdkReader.heart()` mezőkezelése/series/scalar és hibaállapota is vizsgálandó, nem bizonyított előre SDK-hibának.

## Önálló kimenet és elfogadás

- Saját, nullázható pulse/recovery DTO és előkészítő adapter valódi provider/day/series/grant/errors alapján; külön napi és heti sorokhoz. Új UI-komponens a heti pulzus/rövid hiánydiagnózis megjelenítésére a saját mappádban.
- A napi átlag, utolsó időbélyeges mérés, nyugalmi pulzus és megfigyelt tartomány külön fogalom; fallback esetén a felirat is mondja meg, mit mutat. Bounded 96 mintából ne állítsd, hogy az egész nap átlaga. Min/maxból ne találj ki tényleges átlagot.
- Regenerációhoz a meglévő kanonikus Coach-számítást használd valós, friss adatokkal. Samsung Energia és TrainPilot-becslés legyen megkülönböztetve. Adathiány, elutasított grant és SDK-hiba külön, érthető állapot; nincs készülékes engedélyhiba-bizonyíték pusztán üres kijelzésből.
- Nincs HC/Samsung összeadás/fallback, másik nap vagy másik provider adata nem töltheti ki az üreset. Nem szabad előre beégetett 54/87 pontszámmal vagy fiktív HR-bónusszal pótolni hiányt. UI-demo mutáció és readiness összekötés módosítását az integrátornak add át.
- A meglévő permission/foreground/admission/restore védelmek maradjanak. Tényleges olvasói bugot célzottan, bizonyítékkal javíts; ne kérj teljes személyes backupot vagy titkokat.

A közös `TrainPilotViewModel.kt`, `WeeklyHealthDaySummary`, `HealthCoachAndSettingsScreens.kt`, `StudioTodayStatusGrid.kt` és `MainActivity.kt` **csak az integrátoré**. Új mezőneveket, DTO-factoryt, pontos VM-hozzárendelést és heti/today UI-hívást az `INTEGRACIO_health.md` írjon le. A saját kód ne várjon a másik chat implementációjára és még nem létező VM-mezőre.

Teszt: valódi average/series/RHR-only, ismeretlen/null/0 különbség, permission denied, SDK-hiba, régi/jövőbeli/kívüli időbélyeg, heti különböző napok, providerizoláció és elégséges/hiányos readiness input. Fizikai grant/Samsung adat meglétét csak tényleges eszközről lehet igazolni.

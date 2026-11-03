# 2. chat – Két belső óraadat-blokk és Sync ikon

Hibajegy: https://github.com/fuloplevente1998/Native-AI-Studio/issues/60

## Közös kiindulópont és szigorú fájltulajdon

Repo: https://github.com/fuloplevente1998/Native-AI-Studio

Friss `main`-ből saját ág és saját worktree; PR célja `main`, de a végleges integráció/merge a koordinátoré. A main már tartalmazza a 2743-as #49 javításokat (358/358 teszt). A Coach rövid leírása és a piros/listabeli törlés #58 alatt külön készül/elérhető: ezt **ne írd újra**, induláskor ellenőrizd a PR állapotát. Ha még nincs mainen, a koordinátorral egyeztesd a bázist.

Olvasd el az aktuális átadót, `.specify/memory/constitution.md`, `docs/FRAME_DESIGN_SYSTEM.md`, a saját hibajegyet és a csomag `FAJLTULAJDON.md` fájlját. A friss main/hibajegy elsőbbséget élvez a régi snapshotokkal szemben. Magyarul kommunikálj; alügynököt ne indíts.

**Csak a saját alább felsorolt fájljaidat/mappáidat írd.** Másik chat fájlját olvashatod. Közös fájlt nem módosíthatsz „egy sor erejéig” sem. A szükséges bekötést írd a saját `INTEGRACIO_<azonosító>.md` fájlodba, pontos paraméterrel/importtal/adatleképezéssel és alapértelmezéssel. Új segédfájlt és tesztet kizárólag a saját névtereden hozz létre. Ha a feladat ennél nagyobb fájlhatárt igényel, jelezd a koordinátornak; önállóan ne foglald el a másik területét.

Foglalt közös fájlok: `MainActivity.kt`, `TrainPilotViewModel.kt`, `HealthCoachAndSettingsScreens.kt`, `StudioTodayStatusGrid.kt`, `HomeScreen.kt`, Gradle/manifestek/közös strings/theme fájlok és a közös átadók. Ezeket csak az integrátor köti be. Meglévő API új paramétere legyen kompatibilis alapértékű; új komponensed/DTO-d önállóan forduljon, ne hivatkozz még nem létező ViewModel-mezőre.

Natív Compose/Room/DataStore/motor, ownership/ACK, finalization, restore/tombstone és providerelválasztás marad. Ne találj ki adatot, ne adj össze providereket, ne kapcsolj ki védelmet. Package/signer/verzió/kiadási workflow nem a feladat-chaté. Android-build/teszt a meglévő Actionsből; nincs helyi Gradle/emulátor. Érdemi regressziók, aktuális SHA és valódi nyers XML eredmény; a folyamatban levő CI nem siker. Készülékes eredményt csak valódi próbáról állíts.

Átadás: saját PR, egyértelmű kész/hiányzó állapot, módosított fájllista, `INTEGRACIO_<azonosító>.md`, teszt-SHA/Actions/XML, szükséges fizikai próba. Új APK és main merge csak a koordinátortól. Másik chat WIP-jét vagy a megőrzött vegyes stash-t ne alkalmazd.

## Kizárólagos fájljaid

- `wear/src/main/java/com/repforge/app/wear/WearDeviceInfo.kt`
- `app/src/test/java/com/example/WatchDeviceInfoRegressionTest.kt`

Új fájlok csak ezekben a saját mappákban:

- `app/src/main/java/com/example/ui/components/watchinfo/`
- `app/src/main/java/com/example/model/watchinfo/`
- `app/src/test/java/com/example/followup/watchinfo/`

Átadási leírásod: `docs/parallel-handoffs/INTEGRACIO_watch.md` (szintén kizárólag a tiéd).

## Feladat és elfogadás

A meglévő órás kártyán **belül** legyen két kis, keretezett információblokk: például Eszköz (név/modell/akkumulátor) és Rendszer (Android-/TrainPilot-verzió és frissítési állapot). Ne maradjon alatta külön keret nélküli adatismétlés.

- A szöveges Óraadatok frissítése gomb helyett a Mai állapotéhoz hasonló két görbe nyilas Sync ikon, legalább 48 dp érintési felülettel és hozzáférhető leírással.
- A meglévő koppintás/frissítés ugyanazt a valós request/reply adatlekérést használja. Betöltés/hiba/időtúllépés látszik a kártyán belül; busy alatt ne induljon második kérés. Nincs hardcoded óranév, verzió vagy akkumulátor.
- A 2743-as node/requestId ellenőrzést ne módosítsd/kerüld meg. Nem új ownership-, edzés- vagy Health-parancs a feladat.
- Ha buildszám kell, a Wear válaszba kompatibilis opcionális mezőként mehet a tényleges csomagverzió; régebbi válasz hiányzó mezője legyen ismeretlen. Hiányzó akkumulátor nem 0%.

**Önálló kimenet:** új `StudioWatchInformationCard` és saját `WatchInfoCardData` DTO a saját névtereden. A DTO mezői: name, model, androidVersion, appVersion/build opcionálisan, batteryPercent nullable, connected/loading/status/lastSync. A feldolgozó a valódi válasz JSON-jából dolgozzon; ne a régi összefűzött megjelenítési szöveget darabolja.

A `HealthCoachAndSettingsScreens.kt` jelenlegi órakártyáját, illetve a `TrainPilotViewModel.kt` mezőit/request mappingjét **nem írod át**. A kész új komponens hívását és pontos DTO-mappingjét az `INTEGRACIO_watch.md` írja le az integrátornak. A meglévő `NativeWatchDeviceInfo.kt`, közös protocol és listener csak olvasható; szükséges eltérést jelezd.

Teszt: két kártya/mezők, hiányzó érték, egyetlen refresh callback, busy, hiba a kártyán belül. A vegyes `CalendarAndWatchUiRegressionTest.kt` a koordinátoré; saját új tesztet írj. Fizikai válaszhoz telefon/Wear kompatibilis APK szükséges, ezt ne helyettesítsd mintaadattal.

> PR #1 történeti auditjának megőrzött másolata. Forrás: `459bdfec69edbe4870f5d40541e1521a118fd649`; aktuális utód: [CURRENT_STATUS.md](CURRENT_STATUS.md) és [paritásaudit](NATIVE_WEBVIEW_PARITY_2026-11-03.md). A régi nyitott pipák nem aktuális kódhiányok.

# TrainPilot – main: nyitott hibák, javítási lépések és ellenőrzőlista

**Auditált ág/commit:** `main` @ [`960b4f7`](https://github.com/fuloplevente1998/Native-AI-Studio/commit/960b4f733ab57b1e95281451f19c1b2f3263782d) (2026-10-31)  
**Cél:** a jelenlegi AI Studio Jetpack Compose felület megtartása, a már létező natív motorok bekötése, regressziómentes, telepíthető Android-alkalmazás.  
**Végleges alkalmazásazonosító:** `com.TrainPilot.com` – **NEM módosítandó**.  
**Állapotjelzés:** minden `[ ]` pont nyitott, amíg kód + megfelelő ellenőrzés nem igazolja. A korábbi sikeres CI **nem** a `960b4f7` commitot tesztelte.

## Már bekerült a főágba – ne fejlesszük újra

- [x] A testsúlytörlés UI callbackje kapcsolódik a `deleteBodyWeightLog(id)` ViewModel-függvényhez: `MainActivity.kt`.
- [x] Kültéri edzés előkészítésekor a kezdeti sorozat nem teljesített; a tényleges GPS-start külön művelet: `startOutdoorWorkout` / `startGpsTrackingSession`.
- [x] A kezdőlap a már aktív edzéshez visszatérést ismét kezeli (szélső eseteket alább tesztelni).
- [x] Naptáresemény törlésekor meghívódik a `calendarScheduleDao().deleteEntry(eventId)` (az aszinkron konzisztenciát még javítani kell).
- [x] A `play-services-maps` és `maps-compose` dependency bekerült (ez önmagában **nem** élő térkép-integráció).

## P0 – kritikus működés és adatbiztonság

### 1. [ ] Profil + személyre szabott terv: tartós állapot és feltételes kezdőlapi kártya

**Érintett:** `ui/screens/HomeScreen.kt`, `ui/viewmodel/TrainPilotViewModel.kt` (`saveInitialProfileAndPlan`), `ui/components/TrainPilotCommonComponents.kt`, meglévő `TrainPilotSettingsStore`/Room.

- [ ] A profil kitöltöttségét és a terv valódi létét/aktivitását **külön**, tartós forrásból ellenőrizni. Ne csak az alapértelmezett magasság/súly, `remember`, vagy egy memóriabeli flag alapján dönteni.
- [ ] A teljes profiladatot (cél, tapasztalat, hely, eszközök, heti napok, edzésidő, kizárások stb.) a meglévő tárolóba menteni, **újraindításkor visszatölteni**.
- [ ] A `home_personal_planner_mini_card` és `home_profile_new_plan_button` **csak akkor tűnjön el**, ha a profil és a tényleges személyre szabott terv is kész. Hiányos állapotban megfelelő befejező CTA maradjon.
- [ ] A profilt és tervet a Beállítások/Edzéstervek helyen ezután is lehessen szerkeszteni; a meglévő felhasználók adatait nem szabad új kitöltésre kényszeríteni.
- **Elfogadási teszt:** profil + terv mentés -> kártya eltűnik -> app bezár/újraindít -> eltűnt marad; csak profil vagy csak terv -> kártya megfelelően látszik; szerkesztés megmarad.

### 2. [ ] Plank és kétoldali időalapú stopper: sortörés és tartós mérés

**Érintett:** `ui/screens/ActiveMeasurementScreens.kt`, `ActiveTimedWorkoutScreen`, `ActiveBilateralWorkoutScreen`; natív `NativeSetStopwatch`.

- [ ] Egy sorban maradó `MM:SS`, hosszú idő esetén `HH:MM:SS` kijelzés; `maxLines = 1`, `softWrap = false`, a rendelkezésre álló helyhez igazított betűméret/szélesség, lehetőleg tabuláris számjegyek.
- [ ] A Compose `remember` + `LaunchedEffect { delay(1000); sec++ }` helyett/ mögött egy hiteles, natív, tartós stopperállapot; későbbi visszatérésnél ne induljon nulláról és ne fusson a háttérben szünetelve.
- [ ] Bal/jobb oldali stoppert és gyakorlatváltást is ugyanazon szüneteltetési szabályokra kötni.
- **Teszt:** 00:00, 00:59, 01:00, 10:00, 01:00:00; kis képernyő; 1,3–1,5× rendszerbetűméret; háttérbe lépés és visszatérés.

### 3. [ ] Egységes edzésszüneteltetés navigációnál, hosszú megszakításnál

**Érintett:** `TrainPilotViewModel.selectTab`, `navigateToSubScreen`, `navigateBack`, `returnToActiveWorkout`, `MainActivity.kt` (`onPauseAndExit`); natív `PhoneWorkoutEngine.setWorkoutPause`, `ActiveWorkoutStore`, `PhoneWorkoutExecution`.

- [ ] Telefonról vezérelt súlyzós, időalapú és kétoldali edzésnél a tényleges edzésképernyő elhagyásakor **natív szünet**; a `cycleHomeDemoMode(PAUSED_WORKOUT)` nem helyettesíti.
- [ ] Visszatéréskor ugyanaz az edzésazonosító, sorozatok, időmérők és gyakorlat marad; explicit Folytatás szükséges; azonos esemény kétszeri elküldése ne okozzon kettős szünetet.
- [ ] Külön kezelni a szándékosan háttérben futó, elindított GPS-követést és a Wear OS-re átadott vezérlést. Ezeket ne állítsa le általános navigáció, de a tényleges tulajdonos- és szünetállapot legyen hiteles.
- [ ] A `persistCanonicalActiveSessionAsync` jelenleg `workoutPaused = false` értéket és egyetlen nyitott aktív intervallumot épít: a valós natív pause/interval állapotot kell megőrizni; ne írja felül a régi szüneteket.
- **Teszt:** 10:00–10:25 aktív, 10:25–16:00 szünet, 16:00–16:20 aktív => **45 perc** naplózott edzésidő, 5 óra 35 perc szünet nélkül; újraindítás és többszöri pause/resume mellett is.

### 4. [ ] Edzésmentés: aktív idő és konzisztens előzmények

**Érintett:** `TrainPilotViewModel.finishAndSaveWorkout`, `TrainPilotWorkoutFinalization`, Room workout history/DAO.

- [ ] Az időt **ne** `finishedEpochMs - startedEpochMs` különbségből számolja, ha hosszú szünet volt. Aktív intervallumok összegzéséből (az edzésen belüli szokásos pihenők kezelési szabályával) származzon a tényleges edzésidő.
- [ ] Egyszeri, atomikus/idempotens véglegesítés, a valós teljesített sorozatok mentése; kültéri edzéshez ne gyártson súlyzós `done = true` sorozatot a mentés kedvéért.
- [ ] A mentett részletek/összesítők/PR/volumen/kardió azonos, Room-ból visszaolvasott adatot tükrözzenek.
- [ ] A `WORKOUT_SUMMARY` Szüneteltetés és kilépés gombja ténylegesen a natív szüneteltetési API-t hívja.
- **Teszt:** hosszú szünet, befejezés közbeni visszalépés, gyors dupla Mentés, többgyakorlatos edzés, GPS és időalapú edzés; nincs duplikáció és torz idő.

### 5. [ ] Health Connect / Samsung Health / Wear: kizárólag edzésaktív intervallumok

**Érintett:** `NativeWorkoutHealth.kt`, `HealthConnectRepository.kt`, `SamsungHealthRepository.kt`, `WearLiveHealthStore` és napló.

- [ ] A meglévő natív lekérdezés `started...finished` folytonos időablakát aktív intervallumonkénti lekérdezéssel/összesítéssel kiegészíteni, hogy a többórás szünetben mért pulzus és kalória ne kerüljön az **edzés** statisztikájába.
- [ ] A min/max pulzus érvényes mintákból, az átlag pulzus mintaszámmal/időtartammal helyesen súlyozva; kalória az átfedésmentes intervallumok és a forrás-azonosítás alapján, dupla számolás nélkül.
- [ ] Edzés közbeni min/max/átlag pulzus, aktív kalória, táv és lépések **ne** keveredjenek a napi átlag-, nyugalmi és teljes napi Health-adatokkal.
- [ ] Hozzáférés/szenzor hiányánál `Nincs adat` és világos forrásmegjelölés; ne kitalált pulzusérték.
- **Teszt:** fenti 10:00–10:25 és 16:00–16:20 két szakasz, köztes szünetben külön pulzusminta -> ez kizárva; jogosultság megtagadása/hiányzó adat.

### 6. [ ] Naptár: teljes dátum, időzóna, célzott törlés és undo

**Érintett:** `TrainPilotViewModel.persistCalendarStateToRoomAsync`, `deleteCalendarEvent`, naptár DAO.

- [ ] Ne `LocalDate.now()` aktuális hónapjából + `dayOfMonth`-ból képezze minden esemény dátumát: teljes `YYYY-MM-DD` tárolás/visszatöltés.
- [ ] Ne írjon fiktív `T11:00:00Z` végidőt; az esemény valódi időtartamát és helyes zónakezelését használja.
- [ ] A `deleteEntry` és a külön aszinkron `persistCalendarStateToRoomAsync` egymáshoz képest rendezett/tranzakciós legyen; Undo helyesen visszaállítson, idegen/szinkronizált eseményt ne töröljön.
- **Teszt:** hónaphatár, DST/nyári időszámítás, törlés+undo, egymás utáni gyors változtatások, újraindítás.

## P1 – szolgáltatások, UI-adatok, integráció

### 7. [ ] GPS-start és GPS nélküli beltéri mérés egységes állapota

- [ ] A `startForegroundService()` sikeres visszatérése csak indítási kérés: valóban `active` szolgáltatásállapot / hiba visszajelzés után jelezzen sikeres GPS-startot.
- [ ] A GPS nélküli beltéri Start ne csak Compose `isStarted` változót állítsa: natív munkamenetet indítson; idő, szünet és mentés maradjon meg.
- [ ] Háttér, képernyőzár, GPS-fix elvesztése, `onTaskRemoved()` és wakelock időkorlát esetén tesztelt viselkedés.
- **Érintett:** `ActiveMeasurementScreens.kt`, `DistanceTrackingService.kt`, `TrainPilotViewModel.kt`.

### 8. [ ] Valós útvonaltérkép (Google Maps még csak dependency)

- [ ] A `MargitszigetDetailedMapBox` jelenleg statikus Margitsziget-canvas és előre rajzolt út: méréskor ne tűnjön a felhasználó tényleges útvonalának.
- [ ] A meglévő GPS route-pontokból valódi, beágyazott és teljes képernyős térkép; naplóból visszanézhető útvonal; üres/engedély nélküli állapot.
- [ ] A Google Maps SDK kulcsát biztonságos konfigurációból kezelni, **semmilyen kulcsot nem commitolni**; manifest + futtatás ellenőrzése.
- **Érintett:** `AnatomicalBodyAndMapComponents.kt`, `ActiveMeasurementScreens.kt`, `app/build.gradle.kts`, `AndroidManifest.xml`.

### 9. [ ] Testsúlytörlés: sikertelen Room művelet és UI visszaállítás

- [ ] `deleteBodyWeightLog()` a képernyőn már a Room-törlés előtt eltávolítja az elemet; adatbázishiba esetén visszatöltés / hibaüzenet szükséges.
- [ ] Újraindítás után a sikeresen törölt mérés nem térhet vissza; sikertelen törlés nem tűnhet sikeresnek.

### 10. [ ] Health UI: hardcoded demóértékek és valódi szinkronstátuszok

- [ ] A Health részletezőben rögzített `1 780 kcal` alapanyagcsere és `58–146 bpm` napi tartomány ne jelenjen meg valós felhasználói mérésnek.
- [ ] Google Drive/Calendar, Samsung Health és Health Connect sikerüzenetek csak ellenőrzött szolgáltatói művelet után jelenjenek meg; hibák és hiányzó integrációk külön jelzése.
- [ ] Forrás, frissítési idő és becsült/mért érték megkülönböztetése.

### 11. [ ] Wear OS + shared modul integráció és tulajdonosváltás

- [ ] A `settings.gradle.kts` most csak `:app` modult épít. Vizsgálni, mely meglévő natív Wear/shared kódot lehet biztonságosan integrálni.
- [ ] Aktív edzés telefon/óra közti vezérlésváltása ACK + timeout + visszaállítás mellett; hiányzó óránál a telefon maradjon tulajdonos.
- [ ] Külön óra/készülék regressziós teszt, ne csak JVM-unit.

## P0 – build, regresszió és elfogadás

### 12. [ ] A legfrissebb `main` commit ellenőrzése CI-ben

- [ ] A `960b4f7` commitra `assembleDebug` + `testDebugUnitTest` futtatás; a korábbi sikeres CI [`b0b3e02`-höz kötött ellenőrzése](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41228366340) nem igazolja az új Google Maps-függőségeket.
- [ ] Használható, verziózott `.github/workflows` beemelése a `main`-be felülvizsgálat után; a külön CI-ágon már van működő alap.
- [ ] Debug APK artifact, tesztjelentés, fordítási hibák és dependency-verziók rögzítése.
- [ ] Szolgáltatás- és adatkezelési tesztek hozzáadása: profil, stopper, pause/resume, egészségablak, naptár, GPS és Room.
- [ ] Valódi készüléken teszt: Android 14–16, kisméretű kijelző, háttér/lockscreen, GPS és Health jogosultságok; Wear külön.
- [ ] A debug APK ideiglenes CI-kulccsal aláírt tesztverzió; telepített más aláírású azonos csomagot nem frissíthet adatvesztés nélkül.

## Elkészültségi feltételek és PR-kezelés

- A javításokat külön, funkciónként áttekinthető commitokban/PR-okban végezzük, először a P0 pontokat.
- Checklistet **csak** sikeres implementáció és dokumentált ellenőrzés után pipáljunk.
- Minden lezárt pont mellé: érintett fájlok, commit/PR, unit/instrumentált teszt vagy dokumentált manuális ellenőrzés.
- A kész UI nem jelent automatikusan kész natív funkciót; a készülékteszt nélkül maradt esetek legyenek kifejezetten jelölve.
- A jelenlegi `com.TrainPilot.com` csomagnév, UI, meglévő programok, adatok és edzésnapló maradjanak meg.

**A jelen PR kizárólag audit/checklist dokumentációt ad hozzá.** Nem javítja és nem igazolja automatikusan a felsorolt funkciókat.

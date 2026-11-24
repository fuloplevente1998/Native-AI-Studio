# 1. chat – Oktatóvideó az alkalmazáson belül

Hibajegy: https://github.com/fuloplevente1998/Native-AI-Studio/issues/59

## Közös kiindulópont és szigorú fájltulajdon

Repo: https://github.com/fuloplevente1998/Native-AI-Studio

Friss `main`-ből saját ág és saját worktree; PR célja `main`, de a végleges integráció/merge a koordinátoré. A main már tartalmazza a 2743-as #49 javításokat (358/358 teszt). A Coach rövid leírása és a piros/listabeli törlés #58 alatt külön készül/elérhető: ezt **ne írd újra**, induláskor ellenőrizd a PR állapotát. Ha még nincs mainen, a koordinátorral egyeztesd a bázist.

Olvasd el az aktuális átadót, `.specify/memory/constitution.md`, `docs/FRAME_DESIGN_SYSTEM.md`, a saját hibajegyet és a csomag `FAJLTULAJDON.md` fájlját. A friss main/hibajegy elsőbbséget élvez a régi snapshotokkal szemben. Magyarul kommunikálj; alügynököt ne indíts.

**Csak a saját alább felsorolt fájljaidat/mappáidat írd.** Másik chat fájlját olvashatod. Közös fájlt nem módosíthatsz „egy sor erejéig” sem. A szükséges bekötést írd a saját `INTEGRACIO_<azonosító>.md` fájlodba, pontos paraméterrel/importtal/adatleképezéssel és alapértelmezéssel. Új segédfájlt és tesztet kizárólag a saját névtereden hozz létre. Ha a feladat ennél nagyobb fájlhatárt igényel, jelezd a koordinátornak; önállóan ne foglald el a másik területét.

Foglalt közös fájlok: `MainActivity.kt`, `TrainPilotViewModel.kt`, `HealthCoachAndSettingsScreens.kt`, `StudioTodayStatusGrid.kt`, `HomeScreen.kt`, Gradle/manifestek/közös strings/theme fájlok és a közös átadók. Ezeket csak az integrátor köti be. Meglévő API új paramétere legyen kompatibilis alapértékű; új komponensed/DTO-d önállóan forduljon, ne hivatkozz még nem létező ViewModel-mezőre.

Natív Compose/Room/DataStore/motor, ownership/ACK, finalization, restore/tombstone és providerelválasztás marad. Ne találj ki adatot, ne adj össze providereket, ne kapcsolj ki védelmet. Package/signer/verzió/kiadási workflow nem a feladat-chaté. Android-build/teszt a meglévő Actionsből; nincs helyi Gradle/emulátor. Érdemi regressziók, aktuális SHA és valódi nyers XML eredmény; a folyamatban levő CI nem siker. Készülékes eredményt csak valódi próbáról állíts.

Átadás: saját PR, egyértelmű kész/hiányzó állapot, módosított fájllista, `INTEGRACIO_<azonosító>.md`, teszt-SHA/Actions/XML, szükséges fizikai próba. Új APK és main merge csak a koordinátortól. Másik chat WIP-jét vagy a megőrzött vegyes stash-t ne alkalmazd.

## Kizárólagos fájljaid

- `app/src/main/java/com/example/ui/components/ExerciseVideoDialog.kt`
- `app/src/main/java/com/example/ui/components/ExerciseVideoResolver.kt`
- `app/src/test/java/com/example/ui/components/ExerciseVideoResolverTest.kt`

Új fájlok csak ezekben a saját mappákban:

- `app/src/main/java/com/example/ui/components/inappvideo/`
- `app/src/test/java/com/example/followup/inappvideo/`

Átadási leírásod: `docs/parallel-handoffs/INTEGRACIO_video.md` (szintén kizárólag a tiéd).

## Feladat és elfogadás

A 2743-as videógomb külső `ACTION_VIEW` megnyitása helyett az adott gyakorlat videója az alkalmazáson belül legyen lejátszható. A felhasználó in-app YouTube/böngészős megoldást is elfogad.

- Tartsd meg a #40 pontos URL-feloldását: kiválasztott/egyéni/importált gyakorlat saját hivatkozása, nincs általános vagy másik gyakorlathoz tartozó fallback.
- Lehetőség szerint natív Android WebView / beágyazott lejátszó meglévő függőségekkel; tényleges betöltés/lejátszóhiba kezeléssel. YouTube tiltott beágyazás, hibakód (például 101/150/153), törölt videó és hálózati hiba ne maradjon néma fekete terület.
- Használható, tudatos in-app böngészős lehetőség kell, ha a beágyazás nem engedélyezett. Az új felület ne nevezze az általános külső YouTube-app indítását belső lejátszásnak. Custom Tabs esetén igazold a böngészőhöz kötött megnyitást és kezeld a hiányzó szolgáltatót; dependency-igényt a koordinátornak jelezd, a Gradle fájlt ne írd.
- Csak ellenőrzött HTTPS/linkformátumok, biztonságos WebView-beállítások. Ne adj általános natív JavaScript-hidat idegen oldalaknak; fájl-/tartalomelérés és külső intent ne nyíljon kontrollálatlanul.
- Bezárás/visszalépés, betöltés/hiba és a videó vezérlése használható legyen. Az edzés, időzítő, szünet és ownership működése nem változhat a lejátszó miatt.

Az `ExerciseYouTubeVideoModalDialog` meglévő hívásai maradjanak kompatibilisek. `MainActivity.kt` és `ActiveMeasurementScreens.kt` nem a te fájlod: szükséges új callback/fullscreennes bekötés csak az integrációs leírásba kerüljön. Teszteld a helyes URL-t, hibás/hiányzó/tiltott videót, kezelhető fallbacket és a felület életciklusát. Fizikai YouTube-lejátszás nem bizonyítható JVM-fixture-rel.

Referencia: #11/#40 korábbi Goblet guggolás esete és a jelen felhasználói szöveges igény; külön új kép nem szükséges.

# 4. chat – Nagy térkép: tényleges ablakméret és látható vezérlők

Hibajegy: https://github.com/fuloplevente1998/Native-AI-Studio/issues/48

## Közös kiindulópont és szigorú fájltulajdon

Repo: https://github.com/fuloplevente1998/Native-AI-Studio

Friss `main`-ből saját ág és saját worktree; PR célja `main`, de a végleges integráció/merge a koordinátoré. A main már tartalmazza a 2743-as #49 javításokat (358/358 teszt). A Coach rövid leírása és a piros/listabeli törlés #58 alatt külön készül/elérhető: ezt **ne írd újra**, induláskor ellenőrizd a PR állapotát. Ha még nincs mainen, a koordinátorral egyeztesd a bázist.

Olvasd el az aktuális átadót, `.specify/memory/constitution.md`, `docs/FRAME_DESIGN_SYSTEM.md`, a saját hibajegyet és a csomag `FAJLTULAJDON.md` fájlját. A friss main/hibajegy elsőbbséget élvez a régi snapshotokkal szemben. Magyarul kommunikálj; alügynököt ne indíts.

**Csak a saját alább felsorolt fájljaidat/mappáidat írd.** Másik chat fájlját olvashatod. Közös fájlt nem módosíthatsz „egy sor erejéig” sem. A szükséges bekötést írd a saját `INTEGRACIO_<azonosító>.md` fájlodba, pontos paraméterrel/importtal/adatleképezéssel és alapértelmezéssel. Új segédfájlt és tesztet kizárólag a saját névtereden hozz létre. Ha a feladat ennél nagyobb fájlhatárt igényel, jelezd a koordinátornak; önállóan ne foglald el a másik területét.

Foglalt közös fájlok: `MainActivity.kt`, `TrainPilotViewModel.kt`, `HealthCoachAndSettingsScreens.kt`, `StudioTodayStatusGrid.kt`, `HomeScreen.kt`, Gradle/manifestek/közös strings/theme fájlok és a közös átadók. Ezeket csak az integrátor köti be. Meglévő API új paramétere legyen kompatibilis alapértékű; új komponensed/DTO-d önállóan forduljon, ne hivatkozz még nem létező ViewModel-mezőre.

Natív Compose/Room/DataStore/motor, ownership/ACK, finalization, restore/tombstone és providerelválasztás marad. Ne találj ki adatot, ne adj össze providereket, ne kapcsolj ki védelmet. Package/signer/verzió/kiadási workflow nem a feladat-chaté. Android-build/teszt a meglévő Actionsből; nincs helyi Gradle/emulátor. Érdemi regressziók, aktuális SHA és valódi nyers XML eredmény; a folyamatban levő CI nem siker. Készülékes eredményt csak valódi próbáról állíts.

Átadás: saját PR, egyértelmű kész/hiányzó állapot, módosított fájllista, `INTEGRACIO_<azonosító>.md`, teszt-SHA/Actions/XML, szükséges fizikai próba. Új APK és main merge csak a koordinátortól. Másik chat WIP-jét vagy a megőrzött vegyes stash-t ne alkalmazd.

## Kizárólagos fájljaid

- `app/src/main/java/com/example/ui/screens/OutdoorGpsDashboard.kt`
- `app/src/main/java/com/example/ui/screens/ActiveMeasurementScreens.kt`
- `app/src/main/java/com/example/ui/components/AnatomicalBodyAndMapComponents.kt`
- `app/src/test/java/com/example/OutdoorGpsLayoutRegressionTest.kt`

Új fájlok csak ezekben a saját mappákban:

- `app/src/main/java/com/example/ui/components/mapcontrols/`
- `app/src/test/java/com/example/followup/mapcontrols/`

Átadási leírásod: `docs/parallel-handoffs/INTEGRACIO_map.md` (szintén kizárólag a tiéd).

## Feladat és referencia

A #48 **újranyitott** készülékes hiba. A 2743-on is kilóg az alsó indítás/vezérlés a nagy térképből; normál módban az App témája felirat/vezérlő ráfed rendezetlenül a térképre.

Referencia: az eredeti beszélgetés 2026-11-03-i **03:16-os két képe** (teljes és normál térkép). Ezeket az eredeti üzenetből mellékelni kell az új chatnek; a fájlcsomag nem tartalmaz képeket és nem személyes backupból pótolja őket.

## Elfogadás

- A valódi fullscreen Dialog ablakán belül, a rendszer sávjai fölött legyen az X, fejléc, idő, adatok és minden primer Start/Szünet/Kör/Befejezés gomb. Ne csak egy külső test-Box fixture-ben férjenek el.
- Vizsgáld meg a Dialog decorFitsSystemWindows/safeDrawing és AndroidView/GoogleMap méretezését, a korlátozott viewportot és footer/szöveg minimális méreteit. Nem megoldás vakon egy extra fix alsó padding vagy egy túl magas, nem zsugorodó térképfelület.
- Kompakt módban a térkép maradjon alacsony, az adatok és műveletek hozzáférhetők. Az App témája/stílusvezérlők egyértelmű, kontrasztos, kompakt vezérlőben legyenek; ne lebegő csupasz feliratként fedjék a térképet vagy a nagyítás gombját.
- Google-logó, zoom, stílus és a saját overlayek ne fedjék egymást. Normál/szűk ablak és nagy betűméret ellenőrzése; primer gomb nem kerülhet a rajzterületen kívülre.
- Nincs GPS-/időmérés-/ownership-/mentés-/elvetés-logikaváltás, kitalált pont vagy szegmensek összekötése. X a teljes térképet csukja, ne állítsa le a mérést. A már kész automatikus/kézi térképtéma és tényleges rajt/cél marad.

Az `AnatomicalBodyAndMapComponents.kt` más részeit (anatómia/óraikon) ne alakítsd át. A videó chat ugyan nem írhat `ActiveMeasurementScreens.kt`-t, de őrizd meg annak videóhívásait. A `LogAndProgressScreens.kt` és közös VM/MainActivity nem a te fájlod; szükséges saved-map/caller módosítást az integrációs leírásba tedd, új paraméter kompatibilis defaulttal.

Tesztelj valódi Dialog-hosttal is; a korábbi 2743-as 358/358 teszt nem zárta ki ezt a fizikai hibát. A tényleges GoogleMap/hardware méretezés végső igazolása fizikai készülék. Ne állíts ilyen elfogadást pusztán fixture alapján.

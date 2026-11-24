# TrainPilot 2763 · 1.1-beta — javított, forráshoz ellenőrzött audit

Dátum: 2026-11-16. Az eredeti [AUDIT_2763_FULL_2026-11-15.md](AUDIT_2763_FULL_2026-11-15.md) állításainak felülvizsgálata, a felhasználó auditpromptja alapján. Az eredeti dokumentum megmaradt; ez az önálló jelentés a helyesbített következtetéseket tartalmazza.

## 1. Auditált állapot, módszer és bizonyíték

**Auditált main:** `17ecad078a767162f0be303954a7b65e97caa38d`. Telefon: `276300`, Wear OS: `276302`, verziónév: `1.1-beta`, package mindkettőnél `com.TrainPilot.com`. Az `app`, `wear`, `shared`, build- és workflow-források diffje üres a kiadott APK-k `8367634a78824d1955956ce90ec424ee92e891a3` forrásához képest. A köztes módosítások dokumentációsak.

A felülvizsgálat a függvények mellett a produkciós Activity, a Compose-callbackek, a köztes adapterek, az adatbázis és a nyugtaút tényleges bekötését is vizsgálta. Egy hibásnak tűnő, de felületről nem használt függvény nem kapott automatikusan felhasználói hibaminősítést.

| Bizonyíték | Ebben a felülvizsgálatban ténylegesen elvégzett munka |
| --- | --- |
| Forrás és Git | Main SHA, alkalmazáskód-diff, nyitott PR-ek és releváns issue-k ellenőrzése; a lent hivatkozott függvények és hívási útvonalaik célzott olvasása. |
| Korábbi CI | GitHub-on visszaolvasott sikeres státusz; a két kiadási kör helyben meglevő teszt-ZIP-jének SHA-256 digestje és nyers XML-összesítése újra ellenőrizve. |
| Új futtatás | Ebben az auditkörben új Android/JVM teszt, Gradle-build, APK/AAB vagy kiadás nem indult. A forrásolvasás nem készülékes reprodukció. |
| Hozzáférési korlát | Nem történt fizikai Samsung/Health Connect/Wear/GPS próba, friss UI-képernyőkép-ellenőrzés, Play Console-vizsgálat, indítási teljesítménymérés vagy automatikus függőségi sérülékenységvizsgálat. |

### Helyes kiadási hivatkozások

- APK: [run 43675171894](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43675171894), forrás `8367634a78824d1955956ce90ec424ee92e891a3`.
- APK+AAB kiegészítés: [run 43721805883](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43721805883), forrás `089a0917d3e71b75b44e9b62dc59b7efc030ad48`.
- Mindkét kör: **516 telefonos + 189 Wear = 705 teszt, 0 failure/error/skipped**, shared NO-SOURCE. Ez történeti, újra ellenőrzött eredmény, nem új audit-tesztfuttatás.
- Tartós aláíró SHA-256: `a72fd016d94e9d94e2b2282032f8c3820d4f09cba9ff1db05c3fd1de6ffa7dd6`.
- Az eredeti audit `24899374804` / `24899980747` futásai és `30:e0…` aláíróhivatkozása nem egyeznek a 2763-as átadási bizonyítékokkal.
- A [PR248](https://github.com/fuloplevente1998/Native-AI-Studio/pull/248) elsőindítás-javítása külön, még nem mergelt ág. Annak **710 tesztje nem a main tesztszáma**. Tesztelt forrása `23db67c149a204dd9864cf386ce5b8d110a0165b`, CI-je [43814942285](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43814942285).

Gépi összesítés: [AUDIT_2763_REVIEW_EVIDENCE_2026-11-16.json](verification/AUDIT_2763_REVIEW_EVIDENCE_2026-11-16.json). Az első APK-k ellenőrzése: [2763-verified-apks.json](releases/2763-verified-apks.json). Az AAB-átadás dokumentációja a nyitott PR246-ban is követhető.

## 2. Vezetői értékelés és a 14 állítás eredménye

Az eredeti audit **nem kezelhető tizennégy egyformán bizonyított hibából álló javítási listaként**. Hat állítás helytálló, öt részben helytálló, három a megadott produkciós útvonalon cáfolható. P0 súlyosságú, végleges adatvesztést ebben a felülvizsgálatban nem igazoltunk.

A fő kockázatok a kétoldali órás visszavonás, a gyakorlatléptetés helyi állapotkezelése és az egyedi gyakorlat külön háttérmentése. A natív ownership-, generation-, tombstone- és véglegesítési védelmek több helyen megállítják azt a tartós adatkárosítást, amelyet az eredeti audit kész tényként állított. A UI és a motor eltérése ettől még javítandó.

A „helytálló” itt elérhető útvonalon forrásból igazolt hibát jelent; nem jelenti, hogy az adott helyzetet fizikai eszközön újra előidéztük. A reprodukciók és tesztek alább végrehajtandó ellenőrzések.

| Eredeti ID | Eredmény | Javított prioritás | Következtetés |
| --- | --- | --- | --- |
| AUDIT-01 | Részben helytálló | P2, célzott vizsgálat | Általános helyi pending-drain hiányzik, de az újraküldés nincs a pending lista miatt letiltva; a nyugta törli a pending elemet. Végleges elvesztés/P0 nincs bizonyítva. |
| AUDIT-02 | Részben helytálló | P1 | A kétoldali órás Undo hiányos. A várt hibaág és a pihenőmagyarázat pontatlan; telefon-tulajdonú addQuickExercise snapshotját a fogadási védelem elutasítja. |
| AUDIT-03 | Részben helytálló | P1 | A helyi léptetésből hiányzik az admission; a backend ownership-védelme megmarad. Az utolsó lépés közvetlen összegzőnyitása nem szüneteltet szabályosan. |
| AUDIT-04 | Cáfolt a tényleges UI-n | — | A felület a tartós HealthDashboardViewModel.setProvider ágat hívja. |
| AUDIT-05 | Cáfolt a leírt sorrendben | — | A külső indító bevárja a programmentést a célfeloldás előtt; a naptárírás szintén bevárja. |
| AUDIT-06 | Helytálló | P2 | Több órás név HU/EN vagy csak HU feloldású. |
| AUDIT-07 | Helytálló | P2 | A kültéri előnézet újranyitása motorindítás nélkül aktiválhatja a UI-jelzőt. |
| AUDIT-08 | Helytálló | P2 | A produkciós rendszersáv-stílus fix sötét stílus, a világos palettát nem követi. |
| AUDIT-09 | Helytálló | P2 | A betűméret és kompakt mód csak memóriában él. |
| AUDIT-10 | Helytálló | P2 | A Studio intervallum-űrlap értékei nem jutnak el a tartós beállítástárba. |
| AUDIT-11 | Részben helytálló | P1 az egyedi gyakorlatnál | Az exercise-upsert nincs az adatcsere védett írási útján; a programmentés maga már védett. A sikerjelzés túl korai. |
| AUDIT-12 | Helytálló | P3 | Több naptári banner magyarra van beégetve. |
| AUDIT-13 | Cáfolt | — | Az említett OAuth intent-filter és kezelőfüggvény nincs a vizsgált mainben. |
| AUDIT-14 | Részben helytálló | P3, mérendő | Konstruktorban történik katalógusmunka, de lazy cache is van. Mért teljesítményhiba nem igazolt. |

A külön felhasználói elsőindítás-hiba **EXTRA-01 / #247**, lásd a 6. fejezetet; javítása PR248-ban vár integrációra.

## 3. Magas prioritású, részben pontatlanul leírt hibák

### AUDIT-02 — telefon-tulajdonú kétoldali órás Undo: valódi rés, eltérő hibamechanizmus

**Minősítés:** részben helytálló; a visszavonás hiánya forrásból alátámasztott. **P1**, közepes/nagy javítás, közös telefon–Wear integrációval.

**Források:** `wear/src/main/java/com/repforge/app/wear/WorkoutSnapshotStore.kt:356`, `wear/src/main/java/com/repforge/app/wear/MainActivity.kt:828`, `wear/src/main/java/com/repforge/app/wear/WearUndo188.kt:28` és `:81`; `app/src/main/java/com/repforge/app/WearSyncRepository.kt:265`, `:487`, `:584`, `:438`; `app/src/main/java/com/repforge/app/NativeWorkoutEditing.kt:111`.

**Útvonal:** telefon-tulajdonú `mp/oldal` gyakorlatnál a nativePhoneExecution nem engedi a v2 completeSet ágat. A parancs a commitWatchEditorMutation útján készre jelöli és tartósan előkészíti a módosított frame-et. A prepare/amend út nem ugyanaz a rögzítési esemény és Undo-szerződés, mint a natív measured recording.

**Pontosítás:** a WearUndo188 a rekordhoz `native188=false` jelzőt készít, ezért az Undo sem kap v2 protokolljelölést. A WearSyncRepository jelenlegi action-váltójában nincs általános undoSet ág; az ismeretlen művelet „processed” nyugtára jut. Így az audit által állított konkrét `undoTargetNotRecorded` út nem következik ebből a forgatókönyvből. Ráadásul a completeSet mutáció **beállítja a restEndAt-ot**, és a telefon ezt a UI-pihenőhöz figyeli. A pihenő teljes hiányát nem lehet kész tényként kijelenteni.

**A gyors gyakorlatra vonatkozó rész:** a NativeWatchSnapshotAdmission (`:12`) watch-tulajdonú finalSnapshotot követel. A telefon-tulajdonú addQuickExercise snapshotja ezért a WearSyncRepository (`:245`) ágban superseded nyugtát kap, nem alkalmazott új gyakorlatot. A telefonos projektor későbbi „elveszti az új gyakorlatot” magyarázata erre a konkrét útra nem igazolt. Az órás felület telefon-tulajdon mellett kínált hozzáadási lehetőségét külön UX/állapot-egyeztetési tesztben kell vizsgálni.

**Reprodukciós terv:** telefonon indított kétoldali edzés → órán két oldal rögzítése → kész → Undo → a két frame, a nyugta, a rögzítési esemény és a pihenőidő összevetése. Várt: csak a célzott sorozat álljon vissza, a mérési idő és a stabil azonosítók maradjanak épek. A forrás szerinti kockázat a visszavonás elmaradása/konfliktusa.

**Javítás:** a kétoldali mért eredményt és inverzét a meglévő natív eseményszerződéshez kell illeszteni. Nem elég az mp/oldal egységet a súlyzós completeSet engedélylistájába írni; a valódi bal/jobb idő, a recorder és az ACK együtt kezelendő.

**Regresszió:** tényleges Wear-parancskészítés → telefonos router → tartós eredmény → nyugta → Wear Undo. Tartalmazzon ismétlést és felcserélt sorrendet. A meglévő kézi kétoldali és WearUndo188 tesztek külön-külön nem bizonyítják ezt az összekapcsolt útvonalat. Tematikus kapcsolat: #214; az eredeti audit #241 hivatkozása félrevezető, mert #241 a gyakorlatválasztó összeállítási pipájáról szól.

### AUDIT-03 — léptetés előtt hiányzó UI-admission, és nem szüneteltetett összegzőnyitás

**Minősítés:** részben helytálló, forrásból alátámasztott; **P1**, közepes javítás.

**Forrás:** `app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:2701`, `:3766`, `:3807`, `:880`; `app/src/main/java/com/example/ui/screens/ActiveMeasurementScreens.kt:775`; `app/src/main/java/com/repforge/app/StudioWorkoutBridge.kt:43` és `:94`; `app/src/main/java/com/repforge/app/NativeWorkoutEditing.kt:410`.

**Útvonal:** a tényleges Előző/Következő gombok a két ViewModel-metódust hívják. Ezek UI-állapotot módosítanak canEditActiveSets ellenőrzés nélkül, és üres aktív listánál katalógus-fallbacket használnak. Órás tulajdon vagy átadás közben ez helyi kiválasztási eltérést/hibabannert okozhat.

**Korlát:** ebből **nem következik tartós ownership-megkerülés**. A StudioWorkoutBridge és NativeWorkoutEditing a mentésnél ismét ellenőrzi a natív tulajdonost. A hiba a túl korán módosított helyi állapot és annak sikertelen visszaegyeztetése.

**Utolsó gyakorlat:** a nextActiveExercise közvetlenül WORKOUT_SUMMARY-ra állítja a képernyőt. Ez nem hívja a pauseWhenLeavingMeasurement útvonalat; mivel az index nem változik, a bridge gyakorlatváltási szüneteltetése sem oldja meg ezt. A futó sorozat mérési ablaka a rejtett képernyőn tovább tarthat. Másik gyakorlatra lépésnél a bridge már tartalmaz sealPause/pause védelmet, ezt nem szabad hiányzónak állítani.

**Reprodukciós terv:** (a) óra a vezérlő → telefonon Következő; a helyi és tartós kiválasztásnak változatlannak kell maradnia; (b) telefonos utolsó időalapú sorozat mérése közben Következő → összegző; a kanonikus mérésnek szabályosan szünetelnie kell.

**Javítás:** közös admission a helyi mutáció előtt; meglévő gyakorlat kiválasztásához a selectExercise szerződés; utolsó lépéshez szüneteltető navigáció. Az audit switchCurrentActiveExercise javaslatát nem szabad vakon alkalmazni: az a gyakorlatcsere útja, és rögzített adatoknál új elemet is hozzáadhat.

**Regresszió:** UI-gomb → ViewModel → frame ellenőrzése órás ownerrel, átadás közben, üres aktív listával és futó utolsó méréssel. A meglévő assertSetEditsRejected lista nem tartalmazza a next/previous hívásokat. A normál telefonos következő gyakorlatot viszont már védi a StudioSessionMeasurementRegressionTest:765.

### AUDIT-11 — az egyedi gyakorlat külön háttérírása és a túl korai sikerjelzés

**Minősítés:** részben helytálló. **P1** az adatcserével versenyző exercise-upsert miatt; **P2** a korai sikerjelzésnél. Közepes javítás.

**Forrás:** `app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:2283`, `:2343`, `:2437`, `:2502`, `:333`, `:1903`; `app/src/main/java/com/repforge/app/NativeDataReplacement.kt:44` és `:51`.

**Útvonal:** az addCustomExercise előbb módosítja a helyi katalógust és kiírja az „elmentve” szöveget, majd külön IO-coroutine-ban végzi az exercise-upsertet. A kivétel runCatchingben marad; hozzáadáskor a programmentés másik job. Az upsert nincs a dataset-generation védett írási útjába kapcsolva. Írási hiba, folyamatleállás vagy közben végrehajtott visszaállítás esetén a helyi siker nem bizonyítja a kívánt tartós eredményt; késői írás az új datasetbe is kerülhet.

**Pontosítás:** a createNewCustomProgram által hívott persistProgramsToRoomAsync már programWriteMutex és NativeDataReplacement alatt fut, generation-tickettel. Az „egyik sem használ védelmet” általánosítás téves. A programnál a korai sikerbanner külön helytálló észrevétel. Az auditban idézett publishWatchHomeSnapshot hívás nincs a vizsgált programmentő végén, ezért az arra épített versenyhelyzet nem bizonyíték.

**Reprodukciós terv:** egyedi gyakorlat létrehozása programhoz adva → upsert hibája/késleltetése → program betöltése új ViewModelből; külön esetben adatcsere a késleltetett callback előtt. Várt: csak tartós, konzisztens mentés után siker, az új datasetbe ne írjon régi folytatás.

**Javítás:** a meglévő mutateStudioLibrary vagy azonos admission/mutex/generation szerződésű művelet sorosítsa az exercise- és programírást. A ténylegesen összetartozó Room-írások legyenek tranzakciósak; a UI a commitot kövesse, hibával és újrapróbálható állapottal.

**Regresszió:** upsert-kivétel, programírás előtti megszakítás, késleltetett régi folytatás adatcsere után, azonnali újratöltés. A kompakt gyakorlatválasztó és callback tesztjei nem elegendők a tartóssághoz. Külön nyitott jegy még nincs ehhez az auditmegállapításhoz.

## 4. Helytálló közepes és kisebb hibák

### AUDIT-06 — hiányos névfeloldás az órára küldött adatokban

**P2, forrásból igazolt; közepes javítás.** Forrás: `app/src/main/java/com/repforge/app/WearSyncRepository.kt:646`, `:725`, `:727`, `:802`, `:832`.

A gyakorlat-, program- és napnevek több útja HU/EN választást használ, a napló és aktív program neve pedig egyes helyeken közvetlen nameHu. A küldött JSON-ban nem marad meg minden név fordítása, így az óra helyi általános feliratainak fordítása önmagában nem javítja az adatneveket.

Reprodukciós terv: beépített program és mentett edzés → EN/DE/RO telefonnyelv → Home-csomag → órás programlista/Mini napló/indítás. Várt a megfelelő katalógusfordítás; egyedi felhasználói név ne forduljon át vagy vesszen el. Jelen kód alapján HU jelenhet meg a kért nyelv helyett.

Javítás: közös, négy nyelvű névfeloldás az adatküldési útvonalakon, dokumentált fallbackkel és egyedi/historikus nevek megőrzésével. Az óra rendszerének feliratnyelve és a telefonon kiválasztott tartalomnyelv két külön szerződés; ezek egységesítését külön kell eldönteni, nem hallgatólagosan új funkcióként bevezetni.

Regresszió: EN/DE/RO Home és indítás JSON tényleges generálása; beépített és egyedi nevek; hiányzó fordítás. A WearLocalizedTextTest és a 32 paletta átviteltesztje ezt nem fedi le. #220 tematikusan kapcsolódik az órás megjelenéshez, de ez a nyelvi hiba nem tekinthető automatikusan ott megoldottnak.

### AUDIT-07 — kültéri előnézetből fantom aktív UI-állapot

**P2, forrásból igazolt; kicsi/közepes javítás.** Forrás: `app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:3514`, `:1615`, `:1640`; belépés: `app/src/main/java/com/example/MainActivity.kt:339` és `:867`.

A kültéri belépő startOutdoorWorkout útja helyesen előnézetként false aktív jelzőt állít be. Visszalépés és ugyanennek a módnak újranyitása már átmehet a navigateToSubScreen ágán, amely a UI-jelzőt true-ra állítja natív indítás nélkül.

Reprodukciós terv: edzés nélkül kültéri oldal → vissza → kültéri oldal újra, Indít megnyomása nélkül. Várt: előnézet maradjon, nincs aktív edzésjelző vagy elhagyási figyelmeztetés. A UI és natív store eltérése forrásból következik; önmagában ettől még nem bizonyított hamis tartós mérés vagy naplóbejegyzés.

Javítás: navigáció ne hozzon létre edzés-életciklust; előnézeti állapot legyen külön, aktív flag csak visszaigazolt natív frame-ből származzon. Regreszió: kétszeri nyitás → nincs frame/aktív jelző; valódi GPS-es és GPS nélküli indítás → pontosan egy aktív frame; meglévő edzéshez visszatérés ne indítson újat. Kapcsolat: meglévő kültéri regressziók; ez külön újranyitási eset.

### AUDIT-08 — rendszersáv stílusa nem követi a világos palettát

**P2, forrásból igazolt stílusbekötési hiba; kicsi javítás és készülékes ellenőrzés.** Forrás: `app/src/main/java/com/repforge/app/MainActivity.kt:78`; paletták: `app/src/main/java/com/example/ui/theme/TrainPilotPalettes.kt:58`; témakezelés: `app/src/main/java/com/example/ui/theme/Theme.kt:40`.

A produkciós Activity SystemBarStyle.dark stílust állít a transzparens sávokra, miközben a 25–32 paletta ténylegesen világos. Nincs palettaváltást követő stílusfrissítés. A kódhiba igazolt; az összes eszközön „olvashatatlan” megjelenést képernyőkép nélkül nem állítjuk.

Reprodukciós terv: sötét → világos → sötét paletta, hidegindítás és rendszer-visszatérés; óra/értesítési ikonok és alsó navigációs mód ellenőrzése. Javítás: a tényleges háttér/paletta alapján állítsa a rendszerikonok kontrasztját a produkciós Activity. Regreszió: stílusleképezés és Activity-frissítés; fizikai gesztusos és háromgombos navigációs vizuális próba.

### AUDIT-09 — betűméret és kompakt mód nem tartós

**P2, forrásból igazolt; kicsi/közepes javítás.** Forrás: `app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:1814`; `app/src/main/java/com/example/MainActivity.kt:246`; tároló: `app/src/main/java/com/repforge/app/data/store/TrainPilotSettingsStore.kt`.

Mindkét setter csak a ViewModel-állapotot módosítja. Új példány/folyamat esetén az alapérték tér vissza; azonos, túlélő ViewModellel történő Activity-újraépítés önmagában nem megfelelő reprodukció.

Reprodukció: módosítás → új folyamat vagy új, tárolóból kötött ViewModel. Várt: megmaradó beállítás. Javítás: validált, alapértékes DataStore-mezők, betöltés és mentés; a mentés/import/témaszinkron körében a meglévő termékszerződést kell követni. Regreszió: új példány, hibás importérték, korábbi mentés új mezők nélkül; rendszerbetűmérettel együtt használható nagybetűs UI.

### AUDIT-10 — Studio intervallum-beállítások memóriában maradnak

**P2, forrásból igazolt; közepes javítás.** Forrás: `app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:3287`, `:4830`; UI: `app/src/main/java/com/example/ui/screens/ActiveMeasurementScreens.kt:3094`; natív API: `app/src/main/java/com/repforge/app/data/store/TrainPilotSettingsStore.kt:284`.

Az updateIntervalSettings nem ír a tárolóba. A más célból hívott persistStudioTimerSettings szándékosan a már mentett work/round/auto értékeket őrzi; ezért egyszerű utólagos meghívása nem oldaná meg a problémát. Az intervalluműrlap külön callbackkel az időalapú képernyőre navigál, tehát nem igaz, hogy a UI semmilyen továbblépést nem végez.

Reprodukció: eltérő work/rest/round/auto/sound/vibration → mentés/indítás → új folyamat és natív beállítástár olvasása. Várt: a felhasználó által jóváhagyott értékek maradjanak meg. Javítás: az explicit intervallummentés használja a meglévő validált natív preset API-t, a hang/pihenő külön kapcsolói pedig továbbra is őrizzék meg a többi presetmezőt. Ne keverjük össze a sorozatok közötti pihenőt és az intervallumon belüli pihenőt.

Regresszió: tartós teljes preset, egyetlen hangkapcsolás nem írja felül a work/round értéket, hibás/korábbi preset, natív indítás a jóváhagyott beállításokkal.

### AUDIT-12 — naptári műveletek magyar bannerei

**P3, forrásból igazolt; kicsi javítás.** Forrás: `app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:2590`, `:2617`, `:2631`, `:2657`, `:2666`.

Módosítás, áthelyezés, törlés, hozzáadás és visszavonás részben magyar szöveget állít a statusBannerMessage-be. Ez megjelenítési hiba, nem bizonyít adatvesztést vagy szinkronhibát. A fel nem használt helyi változó nem önálló felhasználói hiba.

Reprodukció: EN/DE/RO nyelven az öt művelet és üres undo. Várt: lokalizált eredmény, változatlan műveleti jelentés. Javítás: közös fordítási út, eltérő napjelölések és egyedi programnevek megőrzésével. Teszt: ViewModel-bannerek nyelvenként; külön UI-node létezést ellenőrző teszt nem szükséges.

## 5. Részben helytálló és cáfolt megállapítások

### AUDIT-01 — helyi parancs-újrajátszási hiány, nem bizonyított végleges parancsvesztés

**Részben helytálló; P2 vizsgálati feladat.** Forrás: `app/src/main/java/com/repforge/app/WearCommandListenerService.kt:65`, `:125`, `:135`, `:142`; `app/src/main/java/com/repforge/app/WearSyncStoresAndHandoff.kt:136`, `:218`, `:311`; `wear/src/main/java/com/repforge/app/wear/WearCommandOutbox.kt:138`.

Igaz, hogy a telefonon talált helyi pending-drain csak kompatibilis undoSetet próbál újra. Nem igaz viszont, hogy a pendingben levő parancsot a fogadó feldolgozás nélkül deduplikálja: a router a **seen** állapotot nézi, és az újraérkező parancsot egyébként feldolgozza. A pending-deduplikáció az enqueue műveletben van, nem a végrehajtás előtti admissionben.

Az enqueue nem töröl DataItemet. A complete → ack a pending elemet és seen/receipt adatot egy tartós commitban módosítja, majd a replay küldi a nyugtát. A tényleges prefix `/trainpilot/command-receipt/`; az auditban szereplő másik nyugtaút nem egyezik ezzel. A pending maximuma 64, nem 200; nincs az audit által javasolt markProcessed metódus sem.

Az órás outbox a követett parancsot nyugtáig megtartja; normál újraküldési küszöbe 60 másodperc, reconnectkor force-flush és kézi retry is van. A timeoutjelzés sem jelenti a parancs törlését. A telefonos általános helyi újrajátszás hiánya ettől még okozhat késleltetett helyreállítást, ha nem érkezik új küldés. Ennek pontos életciklus-hatását célzott tesztben kell bizonyítani.

Az „egészség-/témaírás miatti lock-verseny” sem automatikus adatvesztési ablak: a NativeDataReplacement.mutate mutex.withLockkal vár, és a teljes adatcsere aktív edzés mellett tiltott (`NativeDataReplacement.kt:51`, `:90`). Más az átmeneti mutexfoglalt állapot, a tiltott adatcsere és egy régi generation.

**Tesztterv:** készítsünk tartós pending completeSet/updateSet/automaticControl esetet; oldjuk a blokkolást; ellenőrizzük külön a (a) watch újraküldést, (b) újraküldés nélküli telefonos helyreállást, (c) process death/reconnect esetet. Minden esetben egyetlen tartós alkalmazás, helyes nyugta és kiürült pending legyen a követelmény. Ha a helyi drainer bővül, meg kell őriznie az ownership/generation/lezárási admissiont. Becsült munka: közepes, a kiadást blokkoló P1-es szinkronjavítások után; reprodukció esetén újrapriorizálandó.

### AUDIT-04 — egészségforrás-váltó: a felület nem a hibásnak nevezett függvényt hívja

**Cáfolt a jelentésben leírt produkciós úton.** Forrás: `app/src/main/java/com/example/MainActivity.kt:993` és `:1002`; `app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt:383`; `app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:4323`.

A TrainPilotViewModel.togglePrimaryHealthSource valóban memóriát módosító, megmaradt metódus, de a vizsgált produkciós UI nem ezt használja. A két tényleges callback a nativeHealth.setProvider útvonalra köt. Az IO-n, NativeDataReplacement védelem alatt menti a provider preferenciát, majd állapotot frissít és újratölti az egészségnaplót. A Root provider/refreshRevision figyelője a Studio nézetet is újraolvastatja.

Ezért a „nyomd meg a jelenlegi UI-váltót, majd frissítéskor visszaáll” reprodukció nincs a megadott kóddal igazolva. A régi metódus eltávolítása későbbi karbantartás lehet, működő callbacket nem indokolt átirányítani rá.

Készülékes elfogadás ettől külön szükséges a valódi Samsung/HC adatokkal. Hasznos kiegészítő teszt: a produkciós callback → natív provider → DataStore/napló → új példány teljes lánca; a meglévő UI-callback teszt önmagában nem ilyen integrációs bizonyíték. Tematikus kapcsolat #217, nem automatikusan újra megnyitandó provider-bug.

### AUDIT-05 — a megadott indítási race-t már meglévő join védi

**Cáfolt a leírt sorrendben.** Forrás: `app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:2553`, `:460`, `:3372`, `:3429`.

A két mentőnek valóban külön mutexe van, de ebből önmagában nem következik versenyhiba. A persistCalendarStateToRoomAsync a programWriteJobot a naptári tranzakció előtt megvárja. A startPlannedWorkout átadott lambda csak a startCanonicalWorkout **programWriteJob.join** és hibavizsgálata után fut; ezen belül várja meg a naptárírást, majd olvassa a célt. Az eredeti audit a függvény definíciójának sorhelyét futási sorrendnek tekintette.

Az idézett Home-publish hívások a vizsgált mentőfüggvényekben nem találhatók, így az arra épített rész sem bizonyítja az állított race-t. A tényleges alkalmazott naptári célt a NativeHomeRepository és a mentett napazonosító oldja fel.

A meglévő PlannedWorkoutFollowupRegressionTest lefedi az azonnali indítást és a másik programmal történő újratervezés megmaradó sorait; a kettőt egyetlen esetté összekapcsoló teszt még hasznos lehet. Ez **lefedettségi bővítés**, nem bizonyított új P1. Javításként újabb join vakon hozzáadása nem indokolt. Külön, tényleges szinkronfrissítési követelmény vizsgálható, de nem az auditban kitalált publish-hívásokból levezetve.

### AUDIT-13 — nem létező OAuth-útvonalra épülő hibaleírás

**Cáfolt.** Forrás: `app/src/main/AndroidManifest.xml:83`, `:97`, `:102`; `app/src/main/java/com/repforge/app/MainActivity.kt`; keresés az app teljes main forrásában.

A launcher a com.repforge.app.MainActivity; a külső deep link hostja wear. Az oauth2redirect filter nem szerepel a manifestben, és nincs handleGoogleFitRedirect metódus vagy az audit által állított onNewIntent implementáció. A Studio Activity nem exportált.

Az audit konkrét javítási javaslata a jelenlegi forrásban nem létező API-t hívna. Ilyen hibát nem veszünk fel, és nem vezetünk be új OAuth-funkciót egy régi alkalmazás feltételezett útvonala alapján. Ha a Google-kapcsolatnak van készülékes hibája, azt a jelenlegi GoogleSyncRepository/natív consent útvonalon kell külön reprodukálni. Ebben a felülvizsgálatban ilyen külső bejelentkezési próba nem történt.

### AUDIT-14 — konstruktoros katalógusmunka: mérendő költség, nem igazolt lassulás

**Részben helytálló; P3 karbantartási/teljesítménymérési feladat.** Forrás: `app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:941`, `:1267`; `app/src/main/java/com/repforge/app/data/db/TrainPilotDatabase.kt:127`; `app/src/main/java/com/repforge/app/data/db/NativeBuiltinCatalog.kt:8`, `:28`, `:53`.

A ViewModel init valóban szinkron térképez katalógust és állít elő megjelenítési alapállapotot. A NativeBuiltinCatalog JSON-ja, exercise- és programlistája azonban lazy cache. Nem helyes azt állítani, hogy minden ViewModel minden alkalommal újra beolvassa az összes JSON-t. Az első hideg hozzáférésen és a további modellezésen lehet főszálas költség, de startup-trace vagy időmérés nélkül ennek felhasználói súlya nincs bizonyítva.

Maga az init nem állít isWorkoutSessionActive=true jelzőt; a katalógusadatok jelenléte ezért nem önmagában elindított edzés. A Room-visszatöltés már IO-n történik.

Javaslat: előbb hideg/meleg startup és alacsonyabb teljesítményű készülék mérése, utána szükség esetén betöltési állapottal kiváltott előkészítés. Tesztelni kell a friss telepítést és meglévő profil/aktív draft helyreállítását is. Mért hatás hiányában ez nem kiadásblokkoló refaktorálási indok.

## 6. Az auditból kimaradt, felhasználó által jelzett hiba

### EXTRA-01 / #247 — első indítás másik profilszerkesztőt nyit

**P2, forrásból és felhasználói megfigyelésből igazolt.** Main forrás: `app/src/main/java/com/repforge/app/ui/onboarding/NativeOnboardingScreen.kt:113`; összehasonlítás: `app/src/main/java/com/example/MainActivity.kt:265`.

Friss telepítés → Folytatás → profil szerkesztése a régi TrainingProfileDialog-ot nyitja. Home → Profil és új terv már a jóváhagyott, teljes magasságú panelt. Az eredeti audit ezt nem találta meg. A korábbi gate-regresszió csak a Kihagyás és a migrált felhasználó átengedése útvonalát vizsgálta.

**Javítás állapota:** [issue247](https://github.com/fuloplevente1998/Native-AI-Studio/issues/247), draft [PR248](https://github.com/fuloplevente1998/Native-AI-Studio/pull/248). Közös panel és mentési út; onboarding-complete csak tartós profil/terv mentés után; mentés közben dupla indítás és bezárás tiltva. Négy új elsőindítási és egy mentésvédelmi teszt került be. A külön ág teljes CI-je 710 tesztet igazolt, 0failure/error/skipped eredménnyel.

**Nem main-fix:** a PR még nyitott; a 2763-as kiadott csomagok még a régi belépést tartalmazzák. A készülékes elfogadás külön hátravan. Javítási méret: közepes, megvalósítva, integrációra vár.

## 7. Átfogó ellenőrzés területenként

A következő táblázat célzott forrásellenőrzést jelent, nem általános hibamentességi tanúsítást.

| Terület | Ellenőrzött végrehajtási szerződés / megfigyelés | Korlát vagy teendő |
| --- | --- | --- |
| Architektúra | A produkciós launcher a natív Activity, StudioRoot a megjelenítés, a tartós mérés a meglévő natív motoron halad. A másodlagos Studio Activity nem exportált. | A párhuzamos nevű csomagokat nem tekintjük automatikusan két működő motornak; a régi/be nem kötött metódusokat külön kell kezelni. |
| Ownership/folytatás | NativePhoneReturnInstallation ellenőrzi a visszaadási nyugtát, szükség esetén ARM-ot vár, és az explicit Folytatás szerződését őrzi. NativeWorkoutEditing a tartós szerkesztésnél újra ellenőriz tulajdonost. | AUDIT-03 helyi UI-admission hiánya javítandó. Bluetooth/process-death próba kell, két aktív vezérlőt tesztfixture nem zár ki minden fizikai helyzetben. |
| Befejezés/elvetés | TrainPilotWorkoutFinalization: stabil workout ID, Room-tranzakció, tombstone-ellenőrzés, natív lezárás, csak utána mentési nyugta. Meglévő history ismétlése nem automatikusan új edzés. | Telefon-tulajdonú órás finish/discard mindkét irányban fizikai tesztet igényel. P0 állítás helyett a teljes adatút számít. |
| Rögzítés/pihenő | A normál órás completeSet és telefonos következő gyakorlat összekapcsolását meglévő érdemi teszt védi. A set-visszavonásra van külön regresszió. | A kétoldali órás Undo és a közvetlen utolsó Következő nincs ezzel bizonyítva; AUDIT-02/03. |
| Kiválasztás/kész jelzés | StudioExerciseSequenceStrip aktív változásra görget, kész jelölést a sorozatok teljesítése alapján számol. | Teljes chip-szöveg és nagybetűs/kis kijelzős láthatóság készülékes vizuális teszt marad. |
| Program/naptár/Coach | A tervezett indítás program- és naptárírást vár, a natív célt olvassa. Megmaradó sorok program/nap azonosítóihoz van regresszió. | A bannerek nyelve javítandó. Coach-villogás és recomposition gyakoriság friss futó UI nélkül nem minősíthető kész tényként. |
| Egészségforrás | NativeWorkoutHealthSource külön provider-snapshotokat tart; a read ág explicit szolgáltatót választ. A tényleges UI-váltó tartós. | Valós SDK/engedélymegvonás, részleges szinkron, tükrözött mérések és napi összegek fizikai ellenőrzést igényelnek. Hiányzó adatból nem gyártunk nullát vagy becsült mérést. |
| Aktív idő/összegzés | A véglegesítés a kanonikus frame-et és eseményeket egyezteti; a kiválasztott provider aktív ablakai külön lekérdezhetők. Ehhez vannak provider- és summary-regressziók. | Az összes mérést nem olvastuk végig matematikailag újra; a rejtett, tovább futó utolsó mérés kockázata külön AUDIT-03. |
| Kültéri mérés | NativeGpsMeasurement a manuális távolságot figyelembe veszi, a nyers gpsMeasuredMeters és route-binding külön megmaradhat. Az indító backend tényleges frame-et vizsgál. | Előnézeti UI-flag AUDIT-07; valós GPS/GPS nélküli út, engedély és kapcsolatmegszakadás továbbra is fizikai teszt. |
| Mentés/visszaállítás | BackupRestoreManager staging, méretkorlát, hash-ellenőrzés és helyreállítási napló; NativeDataReplacement tiltja az aktív edzés közbeni teljes adatcserét. | Az egyedi gyakorlat védetlen írása kilóg ebből, AUDIT-11. JSON/ZIP régi formátumokat és megszakított checkpointokat meg kell őrizni a javításkor. |
| Egészségadat-mentés | Hozzájárulás alapból false. Export a helyi hozzájárulást olvassa; GoogleSyncRepository:427–439 a távoli merge után is a helyi consentet tartja meg. | Egy import API-ban található consent mező önmagában nem bizonyít távoli engedélyezést; a hívási út ellenőrzése szükséges. |
| Privát fotók/GPS | Fotók belső alkalmazástárban, nem exportált FileProviderrel; GPS route külön azonosított tároláshoz kötött. | A teljes cache-re kiterjedő provider-path önmagában nem bizonyított adatszivárgás explicit URI-grant nélkül. Külön támadási/naplózási vizsgálat nem történt. |
| UI/navigáció | Alakot követő közös gomb-visszajelzés és route SaveableStateHolder meglévő implementációja, érdemi pixel/callback/navigációs tesztekkel. | Nem általános „UI rendben”: világos rendszersáv, lokalizáció, nagy betű és első profil külön teendők. |
| Teljesítmény/akku | Katalógus lazy cache; WearAutomaticService foreground és részleges wake lock, explicit leállítás/release. | Nincs mért startup/ANR/akku eredmény. Hosszú pihenő és kikapcsolt kijelző/Doze próba kell. |
| Release | App/Wear külön Play-verziókód, azonos package és tartós aláíró; mindkettő release R8/resource shrinking beállítású. | A sikeres kiadási kapu nem bizonyítja a Play Console aktuális csatornaállapotát vagy a fizetős tesztelés hozzáféréseit. Ezt az audit nem módosította. |

A Room fő adatbázis jelenleg 1-es sémaverziójú. A „meglévő migrációk rendben” kijelentéshez önmagában a migrációs osztály nevének elolvasása nem lenne elég; a LegacyWebViewMigrator adatimport és a jövőbeli Room-sémamigráció külön terület. Ebben a körben destruktív migrációt nem vezettünk be és sémát nem módosítottunk.

### Az eredeti három futásidejű gyanú helyesbítése

1. **Átadás megszakítása / Activity-újraépítés:** érdemes készüléken vizsgálni, de a „scope törlésekor a finally nem fut” indok nem helyes Kotlin-korutinokra. A finally ténylegesen nullázza a watchHandoffBusy jelzőt; a produkciós scope Activity.lifecycleScope, nem az audit által feltételezett StudioPlatformHost rememberCoroutineScope. A journalban maradó függő állapotot a recovery/ACK-szerződéssel kell vizsgálni.
2. **Provider-váltás és hiányos visszamenőleges adatok:** ez adatfrissességi teszt, nem ok a két provider összeadására vagy csendes fallbackre. A tényleges setProvider újratöltést indít. A „teljes 14 nap automatikusan mindig rendelkezésre áll” feltételezését készülékes mérés nélkül nem fogadjuk el.
3. **Wear pihenő Ambient/Doze alatt:** foreground service és wake lock létezik, ezért nem jelentünk hiányzó ébrentartást. Kijelző-kikapcsolás, OS-korlátozás és valós rezgési késés mérésével kell elfogadni a működést.

## 8. Tesztlefedettség és új regressziók terve

A 705 zöld kiadási teszt valós ellenőrzési alap, nem a teljes termék hibamentességének bizonyítéka. A meglévő teszteket a tényleges ellenőrzött műveletük szerint értékeltük.

| Meglévő teszt / bizonyíték | Amit lefed | Ami ettől még hiányzik |
| --- | --- | --- |
| StudioSessionMeasurementRegressionTest:765 | Órás legacy completeSet → telefonos pihenő, telefonos következő kiválasztás. | Kétoldali parancskészítés → teljes router → Undo-nyugta; utolsó futó mérésből összegző. |
| assertSetEditsRejected ugyanebben a fájlban | Set-módosítások watch-owner/suspended állapotban, tartós frame változatlanságával. | Next/previous nincs a híváslistában. |
| NativeWatchSnapshotAdmissionTest | Elavult owner/epoch/pause/lezárt draft visszautasítása. | Órás UI telefon-tulajdonú hozzáadási kísérletének teljes állapot-egyeztetése. |
| PlannedWorkoutFollowupRegressionTest | Azonnali indítás mentés után; másik programmal újratervezés megtartott sorai. | Másik programra újratervezés és azonnali indítás egyetlen integrációs esetben. Ez nem bizonyított race, hanem célzott bővítés. |
| HealthProviderUiRegressionTest | Választási callback, kijelölés és státuszüzenet. | Produkciós Root-callback + tényleges natív provider tárolás + új folyamat. |
| HealthProviderSelectionRegressionTest | Provider-elkülönítés, aktív időablak, hibás szolgáltatónál nincs csendes fallback. | Fizikai Samsung/HC átfedő eredetek és valódi jogosultságmegvonás. |
| BackupImportValidationRegressionTest | Checksumhiba, consent nélküli export, régi JSON/ZIP, megszakított állapotok. | Egyedi exercise-upsert az adatcsere közben/után. |
| ButtonPressFeedbackRegressionTest / navigációs regressziók | Valódi pixel-, tiltás- és callbackviselkedés; eredeti route visszaállítása. | Minden készülék, minden nyelv és minden fontscale általános vizuális elfogadása. |
| NativeFirstLaunchGateRegressionTest a mainen | Kihagyás és migrált felhasználó. | Folytatás → profilszerkesztés → tartós mentés; ezt PR248 már hozzáadta. |

### Szükséges új tesztek — végrehajtható állapotátmenetek

- **R1, P1:** phone-owner kétoldali készre jelölés az órán → tartós natív eredmény → Undo → ugyanannak a setnek a helyes inverze; ismételt és késleltetett nyugtával is.
- **R2, P1:** phone UI next/previous watch-owner vagy pending handoff mellett → változatlan helyi és tartós kiválasztás; tiltott gomb/callback nem írhat draftot.
- **R3, P1:** utolsó mért sorozat közben Next → összegző → natív óra szünetel, a háttérben eltelt idő nem növeli a mért eredményt.
- **R4, P1:** egyedi gyakorlat/program együtt mentése → upsert-hiba / process death / adatcsere → nincs előre jelzett siker, részleges hivatkozás vagy régi generationből írás.
- **R5, P2:** tartós pending parancs → blokk feloldása → watch újraküldéssel és anélkül külön eset → egyszeri eredmény, nyugta és sorürítés.
- **R6, P2:** kültéri előnézet kétszeri megnyitása → nincs aktív frame; explicit indítás → egyetlen frame. GPS nélküli mód külön szerepeljen.
- **R7, P2:** megjelenítési és intervallumbeállítás → új ViewModel/folyamat → értékek megmaradnak; korábbi/hibás importértékek normalizálása.
- **R8, P2/P3:** beépített/egyedi program és napló HU/EN/DE/RO adatnévfeloldása; naptári bannerek szövege külön, működésmásoló UI-tesztek nélkül.
- **R9, P2:** paletta → produkciós rendszersáv-stílus és Activity-visszatérés; fizikai screenshot elfogadás.
- **R10, kész PR248:** friss telepítés → Folytatás → jóváhagyott panel → értékválasztás → tartós profil/terv → Home; hiba/bezárás nem fejezi be. Main-integráció után a közös CI-ben is szerepeljen.

A teszt-javaslatok az eredeti hibát kiváltó állapotátmenetre irányulnak. Puszta node-létezés, a setter visszaolvasása vagy önmagát ismétlő mock nem elég. UI-only ikon/színváltoztatáshoz nem kérünk mesterséges, implementációt másoló tesztet.

## 9. Fizikai telefon–óra elfogadási mátrix

Az alábbi esetek **nem futottak le ebben az auditban**. Nem írunk elő bizonyíték nélküli egy másodperces szinkron-SLA-t; az időbélyegeket, a késést és a végső konzisztenciát mérni kell.

| ID | Kezdőállapot és lépések | Elfogadási feltétel |
| --- | --- | --- |
| M1 | Friss telepítés; Folytatás és külön Kihagyás; profil mentése; app újraindítása. | Azonos jóváhagyott profilpanel, tartós adatok, egyszeri terv; skip nem hoz létre kitalált profilt. PR248 integráció után. |
| M2 | Telefon-tulajdonú súlyzós és kétoldali edzés; órás szerkesztés/kész/Undo, majd telefonos next. | Azonos set/gyakorlat ID és érték mindkét oldalon; helyes inverz, pihenő és mérési idő. |
| M3 | Phone→Watch→Phone átadás; közben Bluetooth bontás, háttér, Activity-újraépítés. | Egy időzítő-tulajdonos; függő állapot látszik; nincs implicit folytatás vagy kettős rögzítés. |
| M4 | Offline órás két sorozat; telefon kilövése; kapcsolat vissza; duplikált/késői szállítás. | Egyszeri tartós alkalmazás, minden követett parancsnak megfelelő nyugta; hiányzó adat nem fedhető el sikerüzenettel. |
| M5 | Befejezés órán phone-owner mellett; fordítva discard telefonon watch-owner mellett; késői régi snapshot. | Egyszer mentett history vagy tartós elvetés; nincs újraéledő edzés, Mini napló megfelelő frissülése. |
| M6 | Kültéri előnézet vissza/újranyitás; GPS nélkül indítás; manuális távolság; GPS-es ismétlés. | Előnézet nem aktív edzés; explicit indítás pontos; manuális érték és nyers GPS külön megőrizhető. |
| M7 | HC↔Samsung forrásváltás, engedélymegvonás, tükrözött adatok, hiányos nap. | Választott forrás tartós, eredet és időablak követhető; nincs duplázás vagy kitalált nulla. |
| M8 | Beállításmentés, friss process; JSON/ZIP roundtrip; megszakított import; új gyakorlat adatcsere közben. | Adatbiztonság, megfelelő hiba, nincs régi callbackből visszaírás; health consent a helyi szerződést követi. |
| M9 | HU/EN/DE/RO; világos/sötét; kis képernyő és nagy rendszerbetű; billentyűzet, kamerakivágás. | Olvasható rendszersáv és feliratok, elérhető műveletek, teljes alakú érintési jelzés, helyes X/vissza. |
| M10 | Órás automata pihenő 2–3 percig kijelző nélkül; hosszabb edzés és Ambient/Doze. | Valós határidőhöz igazodó mérés/jelzés; mért késés és akkuadat rögzítve, újraindításkor nincs automatikus új mérés. |

## 10. Javítási sorrend és feladathatárok

1. **Első csomag — tényleges P1 adat-/mérési utak:** AUDIT-02 kétoldali esemény/Undo; AUDIT-03 admission és utolsó mérés szüneteltetése; AUDIT-11 sorosított, generation-védett exercise+program mentés. Ezeket a következő nyilvános, fizetős kiadás előtt rendezni és közös CI-vel ellenőrizni kell.
2. **Elsőindítás — már kész külön PR:** #247/PR248 felülvizsgálata és engedélyezett integrációja. Nem kell újra megírni; nincs még a mainben. A P1 csomag közös végső kapujában a hozzáadott teszteket is futtatni kell.
3. **P2 használhatóság és tartósság:** AUDIT-07; AUDIT-09/10 beállítási tárolás; AUDIT-08 rendszersáv; AUDIT-06 névfeloldás. A lokalizáció és a rendszerikonok kisebb, független fejlesztési feladatok lehetnek.
4. **Célzott recovery-vizsgálat:** AUDIT-01 reprodukció és outbox/pending teszt. Ha újraküldés mellett is tényleges beragadást bizonyítunk, a prioritás emelendő. Új drainer nem lazíthatja az owner/generation/terminal védelmeket.
5. **P3:** AUDIT-12 bannerek; AUDIT-14 startupmérés és csak mért indokkal refaktor. A cáfolt AUDIT-04/05/13 javaslatait ne implementáljuk hibajavításként.

### Munkacsomagok és ütközések

| Csomag | Fő érintettség | Végrehajtási szabály |
| --- | --- | --- |
| Szinkron/Undo | Wear parancskészítés, telefonos router, recorder/nyugta | Nehéz integrációs feladat; egyetlen szerződésgazda és end-to-end teszt. |
| Aktív navigáció | TrainPilotViewModel, mérési gombok, natív kiválasztás | Ugyanazon monolit ViewModel miatt a többi VM-módosítással sorban integrálandó. |
| Egyedi mentés | TrainPilotViewModel, Room, replacement admission | Hiba- és visszaállítási teszttel, korai sikerjelzés megszüntetésével. |
| Beállítások | SettingsStore, VM, import/export | Régi adatok alapértékei és mezőnkénti megőrzés kötelező; ne írja felül a másik beállítást. |
| Lokalizáció/rendszersáv | WearSyncRepository, Activity, nyelvi szövegek | Körülhatárolható, a VM-integrációtól részben független. |
| Dokumentáció és mérés | Audit/evidence, startup és fizikai teszt | Kész implementáció, CI és fizikai elfogadás külön állapotként szerepeljen. |

A javítások után egy közös teljes Android CI szükséges a tényleges integrált forrásra. Új installer és main-merge csak az arra szóló felhasználói kérés alapján. Ebben a körben alkalmazáskód, verzió, kiadás és külső szolgáltatás nem változott.

## 11. Karbantarthatóság, korlátok és átadás

- A nagy ViewModel és a régi/nehezen azonosítható metódusok miatt különösen fontos a produkciós callbackből induló elemzés. A függvények szövegének vizsgálata a hívási sorrend nélkül AUDIT-04/05 típusú téves eredményt adhat.
- A katalogizált/programnevek, egyedi történeti nevek és általános Wear UI-feliratok fordítása külön adatút. A felhasználói nevet nem írjuk át automatikusan a katalógus mai fordítására.
- Hibajavításkor a natív motor, stabil ID-k, Room/DataStore, owner/epoch/ACK, replacement-generation, privacy és tombstone védelmek megtartandók. Új párhuzamos motor vagy teljes adatreset nem megoldás.
- Külső SDK-jogosultság, valódi rádiós kapcsolat, Play Console és minden vizuális állapot ellenőrzésére nincs ebben a jelentésben futtatási bizonyíték. Teljes dependency/CVE és valamennyi tranzitív függőség biztonsági auditja sem történt.
- Az audit kód alapján igazolható megállapításokat és végrehajtható ellenőrzési tervet ad. Nem állítja, hogy minden funkciót teljesen végigolvastunk vagy készüléken kipróbáltunk.

**Átadás:** ez a dokumentum és gépi melléklete a docs/audit-2763-validation ágon készült. Az eredeti audit megőrzött forrásanyag. A következő lépés a fenti P1 csomag konkrét javításainak előkészítése, majd engedélyezett integráció és közös CI; nem a cáfolt állítások automatikus megvalósítása.

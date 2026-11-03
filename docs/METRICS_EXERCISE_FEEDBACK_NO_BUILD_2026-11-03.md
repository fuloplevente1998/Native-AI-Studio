# Teljes metrikaböngésző és gyakorlatonkénti visszajelzés – forráskód

**Frissítés: a felhasználó később engedélyezte a közös buildet.** PR90 mainen, a 2748-as aláírt telefonos APK átadva; PR és signed main CI nyers428 tesztje hibamentes. Az alábbi „még nem fordított / nincs a 2747-ben” szöveg a korábbi munkafázist rögzíti, nem a mostani állapot. Jelenlegi bizonyíték: [CURRENT_STATUS.md](CURRENT_STATUS.md).

A felhasználó a kompakt UI után ezt a két feladatot kérte. Ez a következő, **még nem fordított forráscsomag**; a külön átadott 2747-es PR89 APK nem tartalmazza. A kérés szerint ebben a körben nem indítunk új Android/Gradle/JVM buildet. Automata/intervallum további bekötés #74 sztornó, BLE szintén; a meglévő működő motort nem töröljük. Adatvédelmi tájékoztató #82 később.

## Teljes metrikalista (#71)

Egészség → További Health-adatok → Mind a 26 metrika. Témázott, görgethető lebegő ablak, külön dátumválasztóval és Samsung Health / Health Connect csatornával; előző/következő nap, tetszőleges múltbeli ISO-dátum és Ma. Nem változtatja meg a főoldal és az Egészség Mai állapot dátumát/forrását. Az Újraolvasás a helyi naplót olvassa újra; a meglévő egészségszinkron marad a külső adatok frissítési útja.

A meglévő NativeHealthMetrics mind a 26 definícióját használja, a pontos dátumhoz és csatornához tartozó NativeHealthProviderData adapterrel. Nincs korábbi napra, másik szolgáltatóra vagy becsült értékre visszaesés. Csatorna/dátum, metrikaszolgáltató, véges/nemnegatív érték, élettani nulla, hiányzó engedély, failedFields/errors/warnings, Samsung Energy eredet és az adott nap valós időbélyege ellenőrzött. A valós steps/energia0 megmarad; a hiányzó mérés —. Támogatás és tényleges adatok hiányában nem állítjuk, hogy mind a 26 mező kitölthető.

Metrikára koppintva a meglévő rekordböngésző nyílik az adott dátummal, forrással és rekordtípussal. Innen visszatérés megőrzi a metrikalista kiválasztását. A betöltés restore/erase continuation és health generation védelmet kap; a lekérdezés csak olvas.

## Gyakorlatonkénti visszajelzés (#69)

Az edzés összegzőjében és a már elmentett napló szerkesztőjében gyakorlatonként lenyitható, kompakt válaszmező: túl könnyű, könnyű, pont jó, nehéz, túl nehéz, fájdalom; választás törölhető, külön gyakorlatjegyzettel. Nincs előre kijelölt válasz vagy ebből becsült tényleges RIR.

A hat gépi érték a meglévő Coach sémája: easy/light/right/challenging/hard/pain. Csak kifejezett választás írja az exercise.effort mezőt, a jegyzet exercise.feedback.note. Az edzés végén az explicit módosítások a véglegesítőn keresztül a legfrissebb kanonikus draft gyakorlat-ID-jaihoz kerülnek; a felületi projekció nem cserélheti le az elfogadott sorozatokat. A véglegesítő retry-draftja tartalmazza a mentett megjegyzést. Edzés-ID, generation/owner-epoch és adatcsere-continuation védelem; korábbi összegző nem írhat másik edzésre. A root összegző választása/jegyzete is edzéshez kötött.

Naplószerkesztéskor az eredeti JSON-metaadat és sorozat-ID megmarad. Valódi actualRir0 és a hiányzó actualRir külön marad. Root/per-exercise feedback.applied esetén a választás zárolt, jegyzet szerkeszthető, progresszió nem alkalmazódik újra. Érintetlen régi/idegen effort megmarad. Az ismert régi good érték csak kifejezett új válasznál normalizálódik. A fájdalomjelzést és terhelést a meglévő NativeCoachReadiness számítja; nincs új Coach-képlet.

## Ellenőrzési határ

Forrásszintű Kotlin parser és git diff --check; nem Kotlin-típusellenőrzés és nem Android fordítás. Az eredeti parserkorlátos nagy fájlokban az érintetlen hibalevelek összehasonlítva, a módosított összegző külön parse-olva. Új regresszióforrások: 26 mező/rekordkapcsolat, üres vs0, pontos nap/forrás, megtagadás/hibás cache, történeti Energy0; explicit válasz/clear, kanonikus sorozatok/RIR/metadata, applied-zár, Coach pain; meglévő Room szerkesztő roundtrip kiegészítve. **Ezek a JVM-tesztek most nem futottak.**

Következő engedélyezett buildnél teljes Android CI szükséges, majd készüléken dátum/forráscsere, üres nap és engedélyhiba, rekordok/visszatérés; többgyakorlatos edzés eltérő válaszokkal, mentés/újranyitás, naplóban módosítás/törlés, applied-zárolás, új edzés és visszaállítás. A forráscsomag emiatt külön draft PR-ben marad a tesztelhető 2747-es kiadástól.

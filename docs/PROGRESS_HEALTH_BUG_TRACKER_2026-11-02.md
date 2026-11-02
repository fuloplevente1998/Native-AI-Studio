# Natív készülékes hibák és javítások, 2026-11-02

A 2026-11-02-i készülékes visszajelzések közös követője. Kiindulás: a mainre került AI Studio-frissítés és #18 után kiadott, állandó kulcsos `35d9d64` APK (255 sikeres JVM-teszt). A lenti problémákat a felhasználó készüléken jelezte; a korábbi teszteredmény nem jelenti ezek elfogadását.

## Elfogadási lista

- [ ] **Egy naplózott edzés izomterhelése:** az ábra és százalékok az adott edzés tényleges, befejezett sorozataiból készüljenek; másik edzés megnyitásakor ne maradjon korábbi vagy üres alapábra. Ne legyen 3 havi kapcsoló egyetlen edzés statisztikáján.
- [ ] **Összes volumen és grafikon:** az 1 nap / 7 nap / 30 nap / 3 hó valódi helyi dátumszűrést és eltérő oszlopokat alkalmazzon, a fejléc összege és az összesítő kártyák ugyanabból az intervallumból számoljanak. Dinamikus tengely; nincs rögzített heti adatsor vagy kitalált minimum oszlopmagasság.
- [ ] **Fejlődés izomterhelés időszakváltása:** ugyanaz a kiválasztott időszak vezérelje a tényleges Room-lekérdezést, százalékokat, testrajzot és periódusgombot. Gyors váltáskor a késő régi válasz nem írhatja vissza a régi időszakot.
- [ ] **Óranév audit:** rögzített mintanevek és mintabattery eltávolítása minden érintett UI-ból. A képen szereplő `Galaxy Watch7 (ZW4J)` a Wear Data Layer csatlakoztatott node-neve lehet: az igazolt tényleges nevet meg kell tartani, ismeretlen eszközt nem szabad modellel helyettesíteni.
- [ ] **PR #16 integráció:** a naplóban egymás mellett TrainPilot WearOS / Samsung Health / Health Connect forrásválasztók, alapból csukva, saját adatot nyitnak. Forrásadatok/kalóriák nem keverhetők, nem adhatók össze. Meglévő #16 ág újrafelhasználása és friss mainnel együttes ellenőrzése szükséges.
- [ ] **Engedély helyett szinkron:** minden érintett Health Connect / Samsung Health forrásgomb tényleges olvasási grantot ellenőrizzen. Meglévő engedélynél szinkron; hiánynál engedélykérés, majd valódi grant-visszaellenőrzés és szinkron. Részleges/visszavont jog, elutasítás és SDK-hiba nem mutathat sikert. Külön, szándékos engedélykezelés az Engedélyek oldalon maradjon.
- [ ] **Heti célok UI:** a külön „Heti összesítő és adatforrások” panel megszűnik. A Heti lépésszám kártya saját lenyitásában csak a napi lépések és forrásuk jelenjen meg; az Aktív edzésidő saját lenyitásában naponta a Room-edzések és tényleges aktív idejük. A két nyitás független.
- [ ] **Teljes napi lépések és deduplikáció:** teljes helyi nap (ma: éjféltől mostanáig), nem edzésidőablak. A kiválasztott Samsung Health vagy Health Connect napi összesítése legyen az egyetlen hozzájárulás; HC esetén az eredeti deduplikált napi forrásválasztás maradjon. Órás workout-lépések nem teljes napi lépések. Az órából Samsung/HC útján érkező ugyanazon napi adatot nem szabad még egyszer hozzáadni.
- [ ] **Mai állapot az Egészség és Kezdőlapon:** közös, hiteles mai provider-adat; napi lépés, aktív és teljes energia külön értelmezve. A felhasználó Samsung Healthben kb. 6–8 ezer lépést és 1000 kcal-t lát, miközben a natív UI üres. Ezeket a számokat tilos beégetni vagy mérést helyettesítő becslésként használni. Látszódjon forrás, tényleges frissítési idő, szinkronállapot és hiány/hiba; ismeretlen adat ne állítsa azt, hogy nulla.
- [ ] **Edzés közbeni felugró súgók:** az alsó, vezérlőgombot takaró rutin snackbar/útmutató üzenetek (pl. gyakorlat befejezése, következő gyakorlat) megszűnnek. A felső következőgyakorlat-jelzés megmarad; érdemi hiba nem veszhet el.
- [ ] **Edzés közbeni rögzített vezérlés:** az aktív edzésben görgetéstől független helyen legyen elérhető az Átadás órára / Visszavétel telefonra. A tényleges meglévő átadási parancsokat és ACK/ownership/paused/install védelmeket használja, ne pusztán kapcsolót váltson vagy másik képernyőre vigyen.
- [x] **Béta verzió:** látható verzió `0.1.0-beta`, mindkét APK belső Android buildszáma `2737`. A belső kód nem indulhat 1-ről, mert a telepített 2736-ra adattörlés nélkül frissíthetőnek kell maradnia. Appazonosító és állandó signer változatlan.
- [x] **Kapu és átadás:** célzott regressziók + teljes telefon/Wear Actions; nyers XML ellenőrzés. Mainbe merge után állandó saját kulcsos APK-pár, kért új bétaverzió és változatlan appId/signer, hash/metadata-ellenőrzés és feltöltés a TrainPilot Native Drive-mappába. Friss AI Studio-átadó és folytatóprompt.

## Megőrzendő szerződések

`com.TrainPilot.com`, az új felhasználói kérés szerint 2737 / 0.1.0-beta, az állandó saját kulcs, natív Room/DataStore/motor, ownership/finalization/ACK/replacement/tombstone/backup-adatvédelem. Nincs adattörlés, másik szolgáltatóra csendes fallback, fake/demo mérés vagy engedély nélküli sikerjelzés. Aktív edzés/adatcsere alatti tiltásnál a valós ok jelenjen meg; védelmet nem szabad kikapcsolni a szinkron kedvéért.

A javítás folyamatban van. A checklist csak nyers teszt-/APK-bizonyíték után jelölhető késznek; a valódi Samsung/HC készülékes elfogadás külön visszajelzés marad.


GitHub-követő: #19; javító PR: #20.

## Kód- és célzott ellenőrzési állapot

A #20 implementálja a fenti pontokat. A `5df8374a65f81dc759ecfb6e8e96a936698344ff` alkalmazásforráson az Actions `41450724814` SUCCESS: letöltött nyers XML-ben 43 teszt, nulla hiba/kihagyás. Ez célzott ellenőrzés, nem teljes suite. A checkboxok a teljes/készülékes elfogadáshoz maradnak; a kódot ne valósítsd újra pusztán az üres checkbox miatt. A felhasználó kérése szerint innen mainre merge és teljes main-Actions következik.

## Main és teljes ellenőrzés

A #20 mainre merge-elve: `3572e2086cc741cc42bc2c6929c786b0772bf91c`. Teljes signed Actions `41451874030` SUCCESS, letöltött XML: **270 teszt (101 telefon + 169 Wear), 0 failure/error/skip**. Telefon és óra tényleges release APK-ja `0.1.0-beta / 2737`, változatlan állandó signerrel; APK-hash, méret, source checkout és a TrainPilot Native mappa ellenőrizve. Letöltések az aktuális AI Studio-átadóban. A többi checkbox készülékes elfogadáshoz nyitott; nem azt jelenti, hogy a mainbe került kódot újra kell írni.

## Következő készülékes javítás – 2026-11-02

- Mentett edzés után a lezárt execution/ownership pillanatképben megmaradó pause/finalization jelző ne blokkolja örökre az egészségszinkront. Csak kifejezetten inaktív, hiteles lezárt pillanatkép hagyható figyelmen kívül; aktív edzés, sikertelen mentés, átadás, restore és adatcsere védelme megmarad. Magyar, egyértelmű inline diagnosztika, ismételt sikerjelzés nélkül.
- Heti célok: a két önálló lenyitás a két fejléc alatt teljes szélességet kap.
- A Fejlődés harmadik füle Statisztikák: Személyes rekordok és Kardióstatisztika, a meglévő natív PR-/kardiómotorral, tényleges mentett sorozatokból és napi provider-cache-ből.
- Üres naptári nap: önálló tájékoztató sor, két egyenlő szélességű hozzáadásgomb, egyetlen visszavonás.
- Következő belső build: 2738, látható verzió továbbra is 0.1.0-beta. Célzott és teljes ellenőrzés még folyamatban; ez a szakasz nem készülékes elfogadás.

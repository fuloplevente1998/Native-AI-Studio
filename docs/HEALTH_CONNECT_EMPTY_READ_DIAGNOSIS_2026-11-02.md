# Health Connect – üres napi eredmény: diagnózis és reprodukció

Dátum: 2026-11-02. Feladat: 6. chat / #19, az integrációs ág #28.
Kiinduló forrás: `fix/unified-frame-hierarchy` @ `8b03fbd00a636688494796d88442daf49e6e27c1`.

## Megfigyelés (készülék, nem bizonyított ok)

A 2740-es APK-ban a Health Connect volt kiválasztva, de nem jelent meg adat.
Samsung Healthre váltva és szinkronizálva az adatok azonnal megjelentek.
Ez a Samsung út egy alkalommal megfigyelt működését támasztja alá, de
**nem bizonyítja**, hogy a Samsung ugyanazokat a rekordokat átadta a Health
Connectnek, hogy minden HC grant teljes volt, vagy hogy azonos origin és
napi ablak szerepelt a két útvonalon.

Forrás: https://github.com/fuloplevente1998/Native-AI-Studio/issues/19#issuecomment-6587553694

## Kódvizsgálat és reprodukálható diagnosztikai rés

- `NativeHealthRefresh.dayWindow` a helyi időzónában éjféltől a következő
  éjfélig számol, a mai napot a lekérdezés pillanatánál zárja le.
- A `NativeHealthRefresh` az aktuálisan kiválasztott providert olvassa; nem
  kapcsol automatikusan Samsungra. A helyi napok csatornánként tárolódnak.
- A `HealthConnectRepository` a tényleges, *aktuális* HC grantokat ellenőrzi.
  Egyetlen megadott olvasási jog már elég a belépéshez, és hiányzó egyéb jogoknál
  részleges eredményt kezel. Ez nem igazol adatmegosztást.
- Az `hcDailySource` beállítás `auto`, `priority`, vagy konkrét package
  origin lehet. Konkrét originnál az aktivitásaggregátum külön szűrt; ha az
  origin az adott napon nem adott lépést, ezt nem szabad más forrásból pótolni.
- Az SDK-olvasásból eddig a mért mezők, `warnings` és `permissions` adatok
  megvoltak, de a napi UI nem adott külön, egyértelmű üzenetet a **0 találat /
  hiányos grant / SDK-olvasási hiba / nem látott kiválasztott lépés-origin**
  esetekhez. Ez a diagnosztikai hiány kódból reprodukálható. Nem azonos a
  fizikai készülékes HC-üresség még nem azonosított kiváltó okával.

## Diagnosztikai módosítás (nem adatútvonal-váltás)

A `HealthConnectReadDiagnosis` a friss napi HC-olvasásból, a journal korábbi
méréseket őrző részleges merge-je **előtt** képez külön `hcReadDiagnosis` mezőt:

| Kód | Jelentés |
| --- | --- |
| `DATA_PRESENT` | A friss HC-eredményben elérhető legalább egy mért mező/sorozat |
| `NO_DATA` | Sikeres olvasási művelet, de nincs mért mező |
| `NO_DATA_LIMITED_GRANTS` | Nincs mért mező, és nem minden olvasási jog adott |
| `PARTIAL_GRANTS` | Van mért mező, de az engedélyek részlegesek |
| `READ_ERROR` | Legalább egy típus SDK-olvasása hibát jelzett |
| `STEP_ORIGIN_NOT_OBSERVED` | Kifejezetten választott lépés-origin hiányzik a frissen megfigyelt, nem üres originlistából, a lépésszám hiányzik |

A mező tartalmazza a tényleges, grantolt/hiányzó olvasási jogosultságok
**neveit**, a megfigyelt metrikakulcsokat, az originpreferenciát és a
megfigyelt napi lépésorigineket. Nem tartalmaz generált mérési értéket.
A HC-beállításoknál csak a problémás diagnosztikai állapot szövege jelenik
meg; a működő Samsung út, a kiválasztás és a cache többi része változatlan.
Egy SDK-hiba a meglévő `warnings` mechanizmuson is nyomot hagy, és nem
válik hamis nullaértékké.

## Készülékes bizonyítási menet (még NEM történt meg)

1. Ugyanazon a telefonon, ugyanazon helyi napon nézd meg a Health Connect
   adatforrásainál, van-e tényleges lépés-/energia-/pulzusrekord. A Samsung
   Health és a Health Connect között engedélyezett-e az adott adattípus
   átadása, és látható-e a rekord eredete? Telepített app nem bizonyít
   megosztott rekordot.
2. TrainPilot > Egészség > Health Connect: a hozzáférési engedélyeknél
   ellenőrizd a tényleges olvasási típusokat. Válaszd az `auto` eredetet,
   majd végezz teljes szinkront. Rögzítsd a diagnózis szövegét, a helyi
   napot, a szinkron/olvasás idejét és hogy maradt-e látható adat.
3. Ismételd meg a `priority` beállítással, és csak akkor konkrét origin
   kijelölésével, ha az origin valóban szerepel a listában. Rögzítsd,
   hogy mi a különbség. Az origin hiánya csak lépésadat-szűrésre
   vonatkozó bizonyíték; nem igazolja, hogy minden HC adattípus hiányzik.
4. Válts Samsung Healthre és szinkronizálj. Ellenőrizd, hogy a Samsung
   adatai külön jelennek meg, és a HC-nál látott hiány nem alakul át
   csendes Samsung-fallbackké. Visszaváltásnál ellenőrizd, hogy a HC
   napi cache és a grantállapot HC-hoz kötött.
5. Ismételd meg egy engedély szándékos visszavonásával, majd visszaadásával.
   Adatot ne törölj, appot ne telepíts újra, mentést ne importálj bizonyíték nélkül.

## Automatizált teszt és korlát

`HealthConnectReadDiagnosisRegressionTest` a fenti állapotokhoz hat
tárgyilagos regressziót ad, köztük a HC-journal napi tárolást és a Samsung
csatorna változatlanságát. A tesztadat fixture, **nem készülékes mérés**.
A valós Health Connect SDK, Samsung→HC átvitel és egy fizikai eszköz eredménye
ezzel továbbra sem bizonyított. Android build és JVM-tesztek kizárólag a
meglévő Actions kapun keresztül ellenőrizendők. Sikeres CI-t vagy APK-t
csak nyers XML, konkrét SHA és artifact-bizonyíték alapján szabad állítani.

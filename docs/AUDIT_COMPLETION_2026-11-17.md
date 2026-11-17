# Teljes auditjavítás és ellenőrzőlista — 2026-11-17

Alap: main `e7082634fd05f616912b943ca7777ab073db07d5`, kiadott2764.
Közös javítóág: `fix/audit-completion`, követés #255; felhasználói kiegészítések
#256 programmentés, #257 élő stopper, #258 mentés előtti kalória.
Az eredeti audit állításait a helyesbített 2026-11-16-i jelentés szerint kezeljük.
A cáfolt állításokhoz nem készül kitalált javítás.

## Eredeti pontok teljes ellenőrzőlistája

| ID | Tétel | Implementáció / ellenőrzés |
| --- | --- | --- |
| 01 | Telefonon megmaradt órás parancsok | Tartós, soros helyi újrapróba a meglévő dispatcherekkel; ingress-azonosság, owner/generáció/pause/restore/lezárás védelmek. Motor-commit/ACK-rés és késői wire közös proof-egyeztetése. |
| 02 | Kétoldali órás kész/Undo | Már mainen, PR253/2764; új kapuban is fut a regresszió. |
| 03 | Natív léptetés, utolsó mérés-pause | Már mainen, PR253/2764; változatlan ID/érték és ownership védelem. |
| 04 | Egészségforrás-váltás | Cáfolt hibaleírás: tényleges Root callback továbbra is a tartós natív providerutat hívja. |
| 05 | Programmentés és tervezett indítás | Cáfolt sorrendi hiba: meglévő program/calendar join megmarad; a naptári program/nap hivatkozásokat nem írjuk át más programra. |
| 06 | Wear névfeloldás | Valós Home/indítás/naptár/Mini napló payloadok HU/EN/DE/RO feloldása. Egyedi és átnevezett történeti nevek megmaradnak. |
| 07 | Kültéri előnézet | Navigáció nem hoz létre aktív életciklust; újranyitás nem resetel vagy folytat automatikusan meglévő munkamenetet. |
| 08 | Világos rendszersáv | A produkciós Activity a tényleges palettaháttér kontrasztját követi, élő váltáskor és visszatéréskor is. |
| 09 | Betűméret és kompakt mód | Validált DataStore mezők, friss ViewModel visszatöltése, tényleges export/import; hiányzó régi és hibás mezők kezelése. |
| 10 | Intervallum beállítás/indítás | Tartós teljes preset, külön intervallum- és erősítő pihenő, mezőnkénti kapcsolások. Valós kanonikus indítás, egyszer fogyasztható ARM-kérés, meglévő natív intervallumpanel/motor. |
| 11 | Egyedi könyvtármentés | Már mainen, PR253/2764; tranzakció, adatcsere-védelem és commit utáni siker. |
| 12 | Naptári eredményszöveg | Tényleges módosítás/áthelyezés/törlés/hozzáadás/undo visszajelzés négy nyelven. |
| 13 | OAuth-filter | Cáfolt: az említett filter és kezelő nincs a produkciós manifestben/Activityben; új OAuth-út nem került be. |
| 14 | Konstruktor katalógusköltség | Trace-szakasz és első/meleg JVM-konstruktor mérés, katalógusazonossági regresszió. Ez nem fizikai startupbenchmark; igazolatlan lassulásra nem történt kockázatos refaktor. |
| EXTRA01 | Első profilpanel | Már mainen, PR248→PR253/2764; teljes elsőindítási és tartós mentési regresszió. |

Névfallback: kiválasztott nyelv → EN → HU → rendelkezésre álló DE/RO.
A történeti builtin név csak pontos katalógusegyezés és megfelelő identitás
esetén fordul; egyedi/átnevezett/törölt katalógusból származó név megmarad.
Az óra rendszerfeliratainak és a telefonról küldött tartalom nyelvének szerződése
nem olvadt össze hallgatólagosan.

## Kiegészítő felhasználói hibák

- #256: aktív beépített program első szerkesztése saját példányt készít és azt
  aktiválja; a további törlések/mentések ugyanazt az ID-t írják. Az inaktív sablon
  szerkesztése nem vált át más aktív programról, az explicit Másolás továbbra is
  külön művelet. Home naptári céljának elsőbbsége és a történeti hivatkozások
  megmaradnak. Három utolsó gyakorlat törlése és újranyitás regresszióval védett.
- #257: olvasható stopperméret, rövid feliratok, kétoldali tördelés, összecsukott
  intervallumbeállítások. Futás közbeni natív elapsedRealtime → állapot → tényleges
  Compose kijelzés regresszió; az eredeti kiadott no-live tünetet fizikai eszközön
  még nem reprodukáltuk, motorhibát nem jelentünk bizonyíték nélkül javítottnak.
- #258: ugyanazon provider és aktív ablak olvasása mentés előtt is; pulse-only
  live adat nem fedheti el a mentett Wear-kalóriát, idegen edzés elutasítva.
  A lekérés olvasási művelet, lifecycle/session/provider/restore guarddal;
  eltérő időablak, napi kcal és hiányzó értékből kitalált nulla nem használható.

## Közös CI

Állapot: integráció alatt. Új teljes kapu még nem futott erre a csomagra.
A 2764 kiadási alap756 tesztje történeti eredmény; nem az új csomag igazolása.
A végleges sourceSHA, futás, artifact-digest és nyers XML az integráció után kerül ide.
Helyi Gradle/Android SDK/emulátor futtatás nem történt.

## Készülékes ellenőrzőlista — még nem végrehajtott esetek

- [ ] Friss telepítés: Folytatás/Kihagyás, profilmentés, újraindítás, egyszeri terv.
- [ ] Órás kétoldali kész/Undo telefonos vezérlésnél, pihenő és értékek mindkét oldalon.
- [ ] Phone→Watch→Phone átadás, Bluetooth-bontás, háttér/Activity-újraépítés, explicit folytatás.
- [ ] Offline parancsok, telefonkilövés, újrakapcsolódás, ismételt/késői kézbesítés.
- [ ] Órás befejezés, telefonos elvetés, késői snapshot; nincs újraéledés vagy duplázás.
- [ ] Kültéri előnézet/vissza/újranyitás, GPS nélküli és GPS-es indítás, manuális távolság.
- [ ] HC/Samsung forrásváltás, valódi engedélymegvonás, átfedő és hiányzó adatok.
- [ ] Megjelenítés/intervallum új process után; JSON/ZIP roundtrip és megszakított import.
- [ ] HU/EN/DE/RO, világos/sötét, kis kijelző, nagy rendszerbetű, kivágás/billentyűzet.
- [ ] Órás pihenő kijelző nélkül/Ambient/Doze; mért késés és akku; hideg/meleg telefon-startuptrace.
- [ ] A/B utolsó három gyakorlat törlése, mentés, aktív/saját program újranyitása, ismételt mentés.
- [ ] Plank futás közben nagy élő idő, pause/resume, stop után ugyanaz a mért eredmény.
- [ ] Összesítő és mentett napló kalóriája azonos providerrel/aktív ablakkal; hiány esetén nem kitalált nulla.

Main-integráció, új APK/AAB és verzióemelés nem része ennek az új javítási körnek.
A végén átadott CI-eredmény és ez a fizikai lista külön bizonyíték.

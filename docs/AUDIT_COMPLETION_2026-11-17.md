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

Első teljes kapu: [44093701711](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/44093701711),
source `dfd24439ceb59190fb193679c9f8f8f28dc1eb00`: 825 eset (telefon627,
Wear198), 5 sikertelen új UI-regresszió. A produkciós fordítás és release R8 sikerült.
Az első futás nem elfogadott eredmény. A nyers XML és az artifact SHA-256 ellenőrizve.

A javított tesztfeltételek valódi kétoldali időméréses gyakorlatot választanak,
a hiányzó időalapú katalógus ágát külön állapotprojekcióval vizsgálják, mert a
rendes újratöltés szándékosan visszaállítja a builtin katalógust; az Activity-teszt látható ablakon ellenőrzi a rendszersávokat.
A stopper szélessége a tényleges betűmérésből számolódik, nem becsült karakterarányból.
A nagy élő idő, futás közbeni változás, pause és rögzített eredmény feltétele megmaradt.

Ismételt teljes kapu: [44096539287](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/44096539287),
source `5b38b64e8acaf5204543043db524179aea671e5b`: 825 eset, 3 sikertelen
új stopper/intervallum UI-eset; a rendszersáv és a katalógusprojekció tesztje már sikeres.
Ezt a futást sem tekintjük elfogadottnak. A további diagnosztika a tesztek között
megmaradó AndroidViewModelFactory singleton Application-jét és a zsugorított
szövegszélesség kerekítési túlcsordulását választotta le. Az explicit aktuális
Application-factory és a középre igazított teljes kijelzőszélesség megőrzi az eredeti
futás/pause/rögzítés elvárásokat.

Harmadik teljes kapu: [44098205802](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/44098205802),
source `39965dc18fc33f9c7246f274dbe810630e9feddc`: **sikeres**.

- Telefon: **627/627**, Wear: **198/198**, összesen **825/825**; nincs failure,
  error vagy skipped eset. Shared: NO-SOURCE, nem számoltunk hozzá fiktív tesztet.
- **69 új regresszió**, 15 osztályban; a main2764 756 esetét is újrafuttattuk.
- A telefonos és órás produkciós fordítás, a telefonos release R8, a font- és
  videós ellenőrzések is sikeresek.
- Nyers XML-ek független újraszámolása és CI-jelentés-egyezés ellenőrizve;
  tesztartifact `14334279725`, SHA-256
  `542f13b0d45a53136c17b8b05bf83e35ab22e5d5449d6b4bf54dfd49e6cb9ebe`.
  [Géppel ellenőrzött bizonyíték](verification/PR259-ci-evidence.json).
- Diagnosztikai JVM-katalógusmérés: első konstruktor 35,749072 ms, meleg
  12,817616 ms; 140 gyakorlat/13 program. Ezek Robolectric-adatok, nem
  készülékes hideg/meleg indulási benchmarkok.

## Lezárt kód- és regresszióellenőrzések

- [x] AUDIT01: tartós helyi recovery és valódi dispatcher/commit/ACK bizonyíték — 26 új eset.
- [x] AUDIT02–03, EXTRA01: már mainen lévő javítások teljes újrafuttatása.
- [x] AUDIT04–05, 13: cáfolt hibaleírások szerződésének megőrzése.
- [x] AUDIT06, 12: valódi órás payloadok és naptári üzenetek négy nyelven.
- [x] AUDIT07: biztonságos kültéri előnézet és GPS nélküli natív session.
- [x] AUDIT08: mind a 32 paletta, tárolt téma, látható Activity és visszatérés.
- [x] AUDIT09–10: tartós megjelenítés/intervallum, backup roundtrip és valódi natív indítás.
- [x] AUDIT11: korábbi tartós könyvtárjavítás regressziói változatlanul sikeresek.
- [x] AUDIT14: trace/JVM-mérőpont és katalógusazonosság; fizikai javulást nem állítunk.
- [x] #256: három gyakorlat törlése, aktív saját példány, ismételt mentés — 6 eset.
- [x] #257: tényleges Compose élő kijelzés stop előtt, pause/resume, egy- és kétoldali rögzítés — 2 eset.
- [x] #258: mentés előtti kalória és naplóforrás/időablak egyezése, lifecycle/adatcsere védelmek — 10 eset.
A 2764 kiadási alap756 tesztje történeti eredmény; nem az új csomag igazolása.
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

Későbbi külön felhasználói APK+AAB-kérés: PR259 mainen, 2765 · 1.2-beta
kiadva, teljes825 tesztes signed kapu és négy ellenőrzött bináris.
[Kiadási átadás](releases/2765-changes.md); a fizikai lista továbbra is külön állapot.

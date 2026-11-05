# TrainPilot Native – agent munkarend

Ez a fájl a teljes repóra érvényes. Az aktuális felhasználói utasítás elsőbbséget
élvez. A régi átadók és külső tervek forrásanyagok, nem új végrehajtási engedélyek.

## Indulás és önálló haladás

1. Olvasd el a [rövid átadót](docs/WORK_HANDOFF.md), az aktuális feladathoz szükséges
   részt a [státuszból](docs/CURRENT_STATUS.md), majd a tényleges GitHub-jegyet.
2. Ellenőrizd a munkafa, ág/HEAD, nyitott PR-ek és Actions állapotát. A dokumentált
   státusz pillanatkép; ne ossz ki újra másik ágon már készülő javítást.
3. Csak a szükséges forrásokat olvasd; kereséshez `rg`, fájllistához `rg --files`.
   Ne töltsd be minden körben a hosszú történeti átadókat.
4. Magyarul, röviden kommunikálj. A „folytasd”, „kezdjük”, „csináld” végrehajtási
   kérés. Haladj az engedélyezett keretben, rutinszerű javításra ne kérj újra engedélyt.
   Hosszabb munka közben adj érdemi állapotjelzést.
5. Saját rövid ág és izolált worktree kell; más nem commitolt munkáját őrizd meg.
   Új javítást draft PR-ban adj át. Main-merge, kiadás és készre zárás az adott
   feladatra szóló felhasználói elfogadást kövesse; másik feladat engedélye nem elég.
   Ne küldj külön kérés nélkül GitHub-kommentet vagy másoknak üzenetet.

## Modell és thinking – 2026-11-05-i felhasználói szabály

Alapmodell: **GPT-6.1 Sol**. A munkához válassz gondolkodási szintet:

| Feladat | Szint | Konkrét példa |
| --- | --- | --- |
| Egyszerű keresés, státusz, dokumentáció | Light / Low (`low`) | PR-lista és átadó frissítése |
| Fejlesztés és ismert működés bekötése | Medium (`medium`) | Meglévő adatmodell megjelenítése |
| Vizuális dizájn/layout | Medium (`medium`) | Keret, ikon, szín és kompakt elrendezés |
| Kódhiba vizsgálata és javítása | High (`high`) | Hibás API-útvonal, elutasított művelet |
| Összetett debug, versenyhelyzet, CI-kapu hibája | High (`high`) | Dupla edzés, ownership, restore, stopperállapot |

Extra High (`xhigh`) agentet ne indíts: a 2026-11-05-i újabb felhasználói
utasítás legfeljebb High szintet enged hibakereséshez.

A szint feladaton belül is változhat. A dizájnhoz tartozó összetett állapot- vagy
teszthibát a hibakeresési sor szerint kezeld. Az új kérés felülírja az eredeti
WebView-repó régebbi „minden kódolás High” szabályát.

Agent indításakor a modell/szint eszközparaméterét állítsd, ha elérhető. A futó
fő chat felületi modelljét vagy thinking-szintjét ne állítsd átállítottnak, ha nincs
hozzá eszközöd; jelezd röviden a szükséges fázist, és folytasd az engedélyezett munkát.
A fázisjelzés nem új jóváhagyási kör.

## Párhuzamos munka és feladathatárok

- Alagenteket csak felhasználói kérésre indíts. Párhuzamosítás előtt legyen
  issue-, ág- és teljes fájlszintű tulajdonos; ugyanazt a monolit fájlt két agent
  ne szerkessze egyszerre. Közös bekötést az integrátor végezze sorrendben.
- Egy `main` marad, rövid funkció-/dizájnágakkal. A címkék: `type:functional`,
  `type:design`, `type:data-ui`. [Részletes munkamenet](docs/ISSUE_WORKFLOW_FUNCTION_DESIGN.md).
- Közös okot vizsgálni lehet együtt, de bizonyíték nélkül ne állíts közös okot.
  Példa: #128 stopper, #129 gyakorlatcsere, #130 kétszeri elvetés. #126 gombkontraszt
  ettől külön dizájnfeladat; egy színcsere nem javítja a kettős elvetést.
- A PR címéhez ne igazítsd utólag a követelményt: az issue elfogadási feltételeihez
  mérd az eredményt. Másik funkció fejlesztése miatt ne jelents egy jegyet megoldottnak.

## Natív architektúra és adatbiztonság

- Ez a `fuloplevente1998/Native-AI-Studio` natív Kotlin/Compose repó: telefon `app`,
  óra `wear`, közös szerződések `shared`. Az eredeti
  [TrainPilot](https://github.com/fuloplevente1998/TrainPilot) viselkedési referencia;
  annak WebView/Capacitor forrását és buildkapuit ne másold át automatikusan.
- Őrizd meg az egyetlen natív motort, Room/DataStore-t, stabil workout/set ID-ket,
  egy időzítő-tulajdonost, ownership/epoch/ACK, explicit resume, finalization/discard,
  receipt, tombstone és restore/replacement generation védelmeket. Ne reseteld az
  adatokat vagy recovery állapotot hibaelfedéshez. Tartós művelet előtt nincs siker-nyugta.
- Napló, futó session és helyreállítható draft külön felelősség. Ismételt mentés
  ugyanazzal az ID-val nem hozhat létre dupla edzést; késői callback nem támaszthat
  fel elvetett/törölt sessiont. Ne vezess be párhuzamos Studio-motort.
- Valós mérés, pontos időablak és forráseredet kell. Hiányzó adat nem nulla;
  tükrözött Samsung/Health Connect/Wear mérés egyszer számít. Aktív és összes kcal
  nem adható össze. Ne gyárts Coach-javaslatot vagy mérési adatot.
- GPS/fotó privát tár és alapból kikapcsolt health backup-consent megmarad.
  Package `com.TrainPilot.com`, tartós signer és meglévő migrációk megőrzendők.
  Titkot ne kérj/naplózz/commitolj; verziót ne emelj automatikusan.
- A részletes, meglévő szerződések: [constitution](.specify/memory/constitution.md).

## Dizájn és ellenőrzés

- A meglévő palettát és komponenseket használd: `TrainPilotFrames`,
  `TrainPilotOutlinedButton`, egy hangsúlyos külső keret, finomabb belső panelek.
  [Kerethierarchia](docs/FRAME_DESIGN_SYSTEM.md). A piros X jelentése, kompakt
  elrendezés, nagy betűméret, fordítás és érinthetőség maradjon használható.
- A hibát javítsd először; regressziót ne fedj el tesztgyengítéssel vagy tiltással.
  Funkcióhibánál a hibás viselkedést kiváltó esetet védd; a puszta UI-node létezése
  nem bizonyítja a gomb működését. Egyszerű, visszafordítható ikoncseréhez ne írj
  önmagát ellenőrző tesztet. Elavult tesztet csak indokkal távolíts el.
- Teljes Android/Gradle/JVM teszt és APK-fordítás a meglévő **GitHub Actionsben**:
  `trainpilot-android-ci.yml`, illetve `trainpilot-persistent-signing.yml`.
  Helyben feltárás, szerkesztés, diff/szintaxis és indokolt gyors ellenőrzés.
  Ne telepíts helyi Gradle/emulátor környezetet, ne hozz létre kerülő buildkaput.
- PR CI: teljes JVM regresszió, debug APK-csomagolás nélkül. Átadandó telepítő:
  telefon **release**, Wear **R8 + resource shrinking release**, azonos signed runból.
  Ne futtasd újra a teljes kaput minden apró lépésre; a meglévő cache maradjon meg.
- Több elkészült javításnál a felhasználó közös main-integrációt és egyetlen
  közös CI-kört kér (2026-11-05). Az engedélyezett csomagban ne indíts minden
  ágra külön teljes futást; az automatikus PR-kör a javító commitban `[skip ci]`
  jelöléssel kihagyható, utána a teljes main-kapu kötelező. A workflow ellenőrzéseit
  ne gyengítsd. Félkész kód nem része a kész csomagnak; külön APK-kör nem kell.
- Pontos source SHA és nyers XML alapján jelents: tesztszám, failure/error/skipped.
  A letöltött artifact digestjét ellenőrizd. APK-nál package/verzió/hash/tartós signer
  és phone/Wear páros ellenőrzés kell. Teljes aláírt URL-t vagy hosszú logot ne írj ki.
- Implementáció, CI, APK-átadás és fizikai Samsung/HC/Wear/vizuális elfogadás
  külön állapot. A zöld fixture nem készülékes bizonyíték. A kért APK-k az ismert
  TrainPilot Native Drive-mappába kerülnek, régi fájlok megőrzésével és metaadat-visszaolvasással.
- Átadáskor frissítsd a rövid handoffot: ág/commit/PR, konkrét változás,
  ellenőrzés, korlát és következő lépés. Múló státuszt ne halmozz ebbe a szabályfájlba.

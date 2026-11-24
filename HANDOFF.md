# Aktuális átadó

- Main: 1.4.4 kódja, teljes szerkezeti refaktor után (R0–R8, #356–#366, mind zöld CI). Működésváltozás nincs.
  Előbb `docs/CODEMAP.md`, utána csak a megnevezett fájl. Méretőr: `SourceSizeGuardTest`; áthelyezés: `scripts/split_kotlin.py`,
  `scripts/verify_move.py` (`--receiver Cls` kiterjesztésekhez). ViewModel: osztály = állapot + mag, funkciók `Vm*.kt` kiterjesztések;
  csomagon kívüli hívóhoz explicit `import com.example.ui.viewmodel.<név>`.
- Később bontandó (valódi átírás): `MainActivity.TrainPilotAppRoot`, `CalendarPlannerScreen`, `PersonalPlannerModal` – lásd REFACTOR_PLAN.
- Kiadás: 1.4.4 · 280500/280502 aláírt, ellenőrzött. Következő: **1.5.0 · 280600/280602** (2806 gyűjtő, #361).
- 2806 tervezett: Google-fiók leválasztás egy látható gombbal + választó lap (mockup kész, jóváhagyásra vár);
  paletta 12/15 kiemelőszín ≥4,5:1 (#349 része).
- Nyitott issue-k: #349 (32 téma közös kiemelés), #350 (saját témák + áttetszőség, leírás bővítve), #351 (óra mérés auto-indítás,
  jóváhagyásra vár), #359 (külső órás edzésimport – a refaktor feltétele teljesült).

## #359 – külső edzésimport (részleges, draft ág)
- `feature/359-external-workout-import`, draft #369, base `0affd420`: első commit `43ea62c` CI zöld (45454282119). Közös HC modell, lapozott olvasás; Samsung napi egyértelmű UID-jű session jelöltek, explicit Room import tranzakció stabil ID-val, tombstone és adatcsere védelemmel. Átfedés esetén felülvizsgálat, nincs automatikus összevonás. A Room import még nincs UI/automatikus szinkronba kapcsolva; késői módosítás/törlés, metrikák, teljes dedup, Samsung többsession UID, adatvédelmi/backup end-to-end, lokalizáció és készülékteszt hátra. #367/#351 változatlan.

- #359 harmadik fázis: a második commit CI tesztfordítási hibáját (Room nem Closeable) javítja; kézi import a Health forráspanelben, 7/30 napos, lapozott jelöltlista, forrás és eredmény lokalizáció, Room projection frissítése, Napló jelölés. Automatikus import, módosítás/törlés propagáció, részmetrikák és társítás továbbra is nyitott. Új CI ellenőrzendő.

- #359 negyedik fázis: harmadik commit `9798efa` CI zöld (45463123548). Alapból kikapcsolt, helyi és revíziózott automatikus import kapcsoló; csak meglévő előtéri health sync közben igazolt, legfeljebb 30 napos sessionök, élő jogosultság és generáció ellenőrzése, 512 jelölt/flight korlát, nincs új worker, meglévő Room/tombstone/átfedés védelem. Háttérjob, forrásmódosítás/törlés, metrikák és társítás még nincs bekötve. Új CI ellenőrzendő.

- #359 ötödik fázis: HC saját forrású, jogosultsággal olvasható időablak-metrikák; pulzus- és sebességminták, aktív/teljes energia külön, távolság/lépések. Átfedő additív rekordok és másik azonos forrású session esetén nincs bizonytalan összegzés. Forrás snapshot frissítése Room UPDATE-tel (gyakorlat, fotó, helyi időablak megőrzése), forrásidőablak eltérés jelzése, importált edzésnek külön Napló metrikapanel. Samsung közvetlen részmetrikák, GPS, zónák, törlés, társítás és háttérimport továbbra is nyitott. CI ellenőrzendő.

- #359 hatodik fázis: meglévő HC háttérjob opt-in import bekötése (új worker nélkül); HC tombstone alapján tiszta külső másolat eltávolítása, helyi módosítás/gyakorlat/fotó vagy ismeretlen mező esetén megtartás és forrástörlés jelölése; nincs törlés puszta rekordhiányból. A közvetlen Samsung törlésfeed nem igazolt; tesztek és CI ellenőrzendők.

- #359 kézi átfedés-feloldás: meglévő edzéshez explicit társítás (önálló Room rekord nélkül), vagy külön edzésként explicit mentés; forrás azonosító tartós, saját gyakorlat és metrika változatlan, forrásadatok health-consent szerint szűrve. A társított mérés külön bizonyíték, nem automatikusan összesített érték. CI és eszközteszt ellenőrzendő.

- #359 natív Egészség és Napló panel bekötése, device metaadat mindkét HC SDK-n, kör/szegmens kijelzés, Samsung session energia külön típusban, 30 napos külső Progress összesítő (nem szintetikus sorozat), kapcsolt forrás törlésjelzés és HC háttér törlésreconcile. Fizikai eszközteszt és CI még nincs igazolva.

- #359: HC + Samsung azonos forrású pulzusminták, sebességminta alapú tempó, explicit megadott maxpulzus szerinti zónaminta-darabszám (nem zónaidő), Coach explicit külső edzés-effort és hiányjelzés. HC útvonal egyedi felhasználói kérésre, csak memóriában megtekintés; nincs háttér GPS vagy koordináta-mentés. Még CI/készülékteszt szükséges; teljes GPS mentés és új SDK strength/RPE/aktív idő nem állítható késznek.

- #359 phase 10: `de0c0d6` teljes telefon/Wear JVM + R8 CI zöld (45467113537). Szinkron meglévő importált/társított edzésekhez kikapcsolt auto-létrehozás mellett is frissíti az igazolt késői metrikát; régi changelog napok csak meglévő rekordot frissíthetnek; kapcsolt forrásablak-eltérés jelölése és mentés/visszaállítás során incoming törlésjelzés/linktagság megőrzése. Új CI szükséges. GPS továbbra is csak consentelt memóriabeli előnézet; fizikai teszt és SDK-függő extra mezők nyitottak.

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

- #359 phase 11: külön HC útvonalengedély után opcionális helyi mentés `ExternalRouteStore` (AtomicFile, noBackupFilesDir, id/verzió/időablak fingerprint, szigorú méret/koordináta-validáció); újranyitás és törlés, forrástörlés/restore/erase/journal recovery prune, nincs koordináta Room/Drive/JSON/ZIP exportban. Erase az auto-import hozzájárulást is alapértelmezettre állítja. Teszt/CI és készülékes route consent még ellenőrzendő.

- #359 phase 12: Samsung session-level distance/HR/speed/VO2max/duration and original count/type; optional new framework public strength fields (runtime method presence, no alpha dependency upgrade); recorded HC pause interval union, no inferred active duration; external empty-exercise notes/effort edit permitted. SDK-dependent optional fields remain absent when unavailable; CI/device verification pending.

- #359 audit phase 13: 0b54f21 R8 passed, JVM test compilation failed at two implicit Nothing return types in optional field fixtures (45474857892); explicit fixture return types fixed. Pristine source time correction atomically updates canonical Room interval only absent overlaps/annotations, changed annotated imports retain local timeline; background local journal publications use the existing replacement/backup fence; exact metric snapshot rechecked before commit; no generic health refresh on external row; Samsung nullable optional fields and non-spliced HR aggregate; 4-language in-app privacy disclosure added. Website/Play declaration update and actual device matrix still pending. No main merge or release. New full CI required.

- #359 final audit: d6f37c5 full CI 45476020936 passed (raw JUnit: phone 889, Wear 204, shared 0, total 1093; 0 failures/errors/skipped; R8 passed). Final follow-up drains observed candidates per source day before delta checkpoint; failed HC publication marks existing dirty day for retry and makes the read partial, source-deleted snapshots strip provider measurement details and reject same/older-version replay. Added regression fixtures. Remaining physical-device acceptance, Samsung deletion-feed limitation, optional provider export gaps, website/Play release declarations and combined integration are assigned to the release coordinator. Ready for code review/test integration, not a release approval; final follow-up CI must pass. No main merge or APK/signing.

### #359 – Integrátori átadó (2026-11-24)

- Ág: `feature/359-external-workout-import`; [PR #369](https://github.com/fuloplevente1998/Native-AI-Studio/pull/369) – Ready for Review.
- Ellenőrzött kód: `b33ef6bd488f1acc5c45f83869381197d84bd18c`; [CI #45478912719](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/45478912719) zöld: 891 telefon + 204 Wear = 1095 teszt, 0 failure/error/skipped; release R8 sikeres.
- HC + Samsung külső edzések Room-importja (kézi/opt-in automatikus), stabil forrásazonosító, duplikáció/átfedés/tombstone, késői metrikák, Napló/Egészség/Fejlődés/Célok/Coach, külön engedélyezett privát GPS, HU/EN/DE/RO.
- Összefésüléskor a közös health sync, Room DAO, Napló, progress/Coach, backup/privacy és HANDOFF/CODEMAP változásokat szemantikusan egyesítsd. Őrizd meg a restore/erase, ownership és jogosultságvédelmet, a pontos forrást, a nem additív társítást és az újrapróbálható dirty-day importot. Ne írj felül más PR-funkciót; nincs új Wear protokoll, adatbázisverzió vagy kiadási verzió.
- Korlát: Samsung közvetlen törlésfeed nem igazolt; gyártói export, új SDK-metrikák és HC route-consent készülékfüggő. Fizikai teszt, közös CI/build és web/Play adatvédelmi ellenőrzés az integrátoré. Main merge/release nem történt.

Ez a bejegyzés kizárólag dokumentáció; a fent megadott SHA a ténylegesen tesztelt kód.

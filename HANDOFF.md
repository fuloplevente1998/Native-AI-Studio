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

- #351 első rész (`fix/351-wear-auto-measurement`): új telefonos canonical edzéshez külön mérésindítás, meglévő Wear távoli Activity útvonal, élő mérési állapot; ownership nincs átadva. Draft, CI és fizikai óra még ellenőrizendő. Háttér-FGS engedélyezési út és újracsatlakozás külön további vizsgálat; #359/#367 kód érintetlen.

- #351 második rész (PR #370): API 33–35 / 36+ háttér-szenzorengedély deklaráció és külön, opcionális engedélykérés; célzott, 15 másodperces workout/revision/epoch üzenet; háttérből FGS-kísérlet csak illeszkedő élő snapshothoz, GPS nélkül, meglévő Health Services motorral. Rendszerblokkoláskor előtéri fallback, nem hamis ACK. Első CI 870 app teszt / 2 hiba (új JSON fixture NPE), Robolectric futtató javítva. Új CI és fizikai validáció függőben.

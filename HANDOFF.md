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

## #367 – Wear OS átadás az integrátornak (PR #368)
- Ág: `feature/367-wear-redesign`, utolsó kódcommit `55900fadec9aed1e7f6b3aa871d8d64c4c26e7d8`. A main ágat nem módosítottuk; az integrátor fésüli össze a négy PR-t.
- Utolsó teljes zöld kapu: `534197e68cb8cd7235f6a1b61df703e27d9abe2d`, Actions 45478730820: app 881, wear 227, összesen 1108 teszt, 0 failure/error/skipped; R8 sikeres. Az utána hozzáadott 10 mp-es rezgésjelzés CI-je még ellenőrizendő.
- A jelenlegi szerződések, közös fájlok, visszafelé kompatibilitás, fennmaradó készülékes ellenőrzés: [#367 integrációs átadó](docs/WEAR_367_CONTROL_INTEGRATION.md).
- A PR draft; az integrátor a saját ellenőrzése után dönt a ready/merge sorrendről. Nincs verzióemelés, signed build, merge vagy issue-lezárás.

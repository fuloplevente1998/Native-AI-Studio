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

## #349 / #350 – témák (WIP)
- Ág: `feature/349-350-themes`, draft PR #371; kódállapot: `3ec83d8b5d846b9d0fb35551d51fefe1b02dfa7e` (ezen átadót megelőző commit).
- #349: a három jóváhagyott paletta már a mainben volt; az ID/kulcs megmaradt. Új `ThemeSelectionColors.kt`: tényleges felületre kompozitált közös kijelölés, olvasható előtér/keret és telt gomb felirat. Bekötve: natív mapping, szűrőchip, fő navigáció, közös fő gomb. Új `ThemeSelection349Test` a 32 témára és mentett kulcsokra.
- Ellenőrzés: forrásból statikus WCAG-végpontszámítás, 32/32 palettán elérhető a szükséges kontraszt; JVM/Actions és fizikai/vizuális teszt még nincs. Nincs main-merge, nincs release.
- Következő: #349 képernyőaudit, edzésjelvény/átadás (a #351 PR #370 ugyanazt a handover fájlt érinti, összehangolandó), sorozatmezők és állapotszínek, telefon/Wear regresszió. #350: a kibővített színválasztó mockup után tartós saját témamodell, UI, mentés/restore és Wear-szinkron; áttetszőség első kör, blur második kör. #350 implementációja még nem kezdődött.

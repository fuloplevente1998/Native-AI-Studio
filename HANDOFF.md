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
- Ág: `feature/349-350-themes`, draft PR #371; kódállapot: `9f73b7db2d15ebbecee87f9184abf7debc5ce6dd` (ezen átadót megelőző commit).
- #349: 32 beépített téma, változatlan ID/kulcs; a közös kijelölési szín és a telt gombfelirat-kontraszt bekötve a natív mappingbe, fő/alsó navigációba, chipekbe, témaválasztóba, Kezdőlap, Fejlődés és Egészség kijelölt elemeibe. A mezők semleges, 3:1 célú keretet kapnak; aktív edzés értékválasztója és pihenővezérlője javítva (zöld indító tényleges kitöltéséhez illő ikon). A #351 PR #370 handover fájlja érintetlen.
- #350: kibővített szerkesztő drótváz: `docs/CUSTOM_THEME_EDITOR_350.md` (nem végleges látványelfogadás). `CustomThemeDraft.kt`: négy stílus, öt szerep, szigorú HEX, szerepenkénti gyorsszínek, UUID-alapú saját kulcs, 70–90% vagy kikapcsolt (100%) fedettség. A modell még nincs bekötve a palettaválasztóba, DataStore-ba, backupba vagy Wearbe; a kontrasztfeloldó és Compose előnézet hátravan.
- Tesztek: `ThemeSelection349Test` bővítve mezőkeret/zöld indító ellenőrzésével; `CustomThemeDraft350Test` négy teszt. JVM/Actions és fizikai/vizuális teszt nincs; nincs main-merge, release, verzióemelés. Korábbi statikus paletta-végpontszámítás: 32/32 elérhető kontraszt.
- Következő: #349 teljes vizuális audit és handover összehangolás; #350 közös palettafeloldó és szerkesztő, majd a meglévő settings/backup/Wear szerződés integrációja. Blur külön második ütem.

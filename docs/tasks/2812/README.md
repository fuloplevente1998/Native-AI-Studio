# 2812 – javítási feladatok (ChatGPT-nek is átadhatók)

Egy feladat = egy ág/PR-darab. Mindegyikben: mockup (docs/mockups/2812), érintett fájlok, tilos lista, kész-feltétel.
Sorrend: 1 → 2 → 3 → 4. A listák STÍLUSA marad (nem változtatjuk); a Programok akadás külön, teljesítmény-feladat (4).

## Közös szabályok (minden feladatra)
- Alap: `main` (1.5.2, `a31bd559`). Új ág: `fix/2812-<szám>` a main-ből. Draft PR a main felé, NE merge-elj.
- Helyi fordítás/Gradle nincs; a CI (GitHub Actions) futtatja a JVM teszteket. `git diff --check` fusson.
- Nem módosítható: Beállítások oldal kinézete (referencia, „tökéletes"), applicationId `com.TrainPilot.com`, aláírás, verziószám, `wear/`, `shared/`, Room/DataStore/mentett adatok, a lista-sorok stílusa.
- Meglévő tesztet ne gyengíts/töröljön/skipelj; ha a régi viselkedést rögzítő teszt változik, indokold a PR-ben.
- Fájl ≤ 600 sor cél, új fájl > 900 sor tilos (`SourceSizeGuardTest`). Új fájl → `docs/CODEMAP.md` sor.
- Meglévő Kyant anyag-szerepeket használd (`KyantMaterialSpec.kt`: Panel/Tile/Navigation/Selection/Button/Input/Overlay), ne írj új motort, ne duplikálj konstansokat.
- Debug D8 nem tud `return@key`-t parse-olni egyes helyeken – új ilyet ne írj.
- Nincs titok, nincs gyártott adat. Magyar commit-üzenet.
- Írás: a repóba közvetlenül dolgozol. Ág: `fix/2812-<szám>` a `main`-ből; commitolj oda, nyiss DRAFT PR-t a `main` felé. A `main`-re, a `fix/collector-2812` és a többi meglévő ágra NE pusholj; ne merge-elj; PR #372 és #378 soha nem merge-elhető.
- Indulás előtt olvasd: `CLAUDE.md`, `AGENTS.md`, `docs/CODEMAP.md`, `docs/FIXES_2812.md`, a feladat fájlja és a mockup PNG.
- A végén (PR-leírás + chat): 5 soros összefoglaló – mit változtattál (fájlonként), mit NEM tudtál ellenőrizni, kockázatok. A CI-t (draft PR-en automatikus) várd meg, a nyers JUnit számokat (tests/failures/errors/skipped) írd le.

## Feladatok
| # | Fájl | Mockup |
|---|------|--------|
| 1 | TASK_1_header_nav.md – Coach és Napló: áttetsző fejléc/nav, tartalom alájuk csúszik | task1_header_nav.png |
| 2 | TASK_2_sheets_dialogs.md – felcsúszó lapok és dialógusok: üveg + rétegek | task2_sheets_dialogs.png |
| 3 | TASK_3_coach_buttons.md – Coach kártya/elválasztók, Programok A/B gombok | task3_coach_buttons.png |
| 4 | TASK_4_programs_perf.md – Programok akadás kinyitott programnál (stílus változatlan) | nincs (nem látványos) |
| later | nyomásra lencsehatás (GlassTouchBubble), látható finom keretek – külön mockup után | – |

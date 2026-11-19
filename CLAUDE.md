# TrainPilot Native – Claude Code

Stabil projektutasítás. Változó állapot: [HANDOFF.md](HANDOFF.md). Ezt a fájlt csak
tartós szabályváltozáskor módosítsd (cache-barát). Kommunikáció: magyarul, röviden.

## Szabályforrás
- [AGENTS.md](AGENTS.md) a kötelező munkarend és adatvédelem. A benne lévő GPT
  modelltábla Claude Code-ra nem vonatkozik; alagens csak felhasználói kérésre.
- Részletes szerződések: `.specify/memory/constitution.md`, dizájn: `docs/FRAME_DESIGN_SYSTEM.md`,
  issue-folyamat: `docs/ISSUE_WORKFLOW_FUNCTION_DESIGN.md`.
- `docs/CURRENT_STATUS.md`, `docs/WORK_HANDOFF.md` és a ~100 `docs/*` fájl
  történeti napló: alapból NE olvasd; csak célzott `rg`-vel, ha a feladat kéri.

## Architektúra
- `app/` telefon, Kotlin/Compose, package `com.example` (applicationId `com.TrainPilot.com`).
  Nagy fájlok (csak célzott részlet!): `ui/viewmodel/TrainPilotViewModel.kt` (~5.4k sor),
  `ui/screens/{HealthCoachAndSettings,WorkoutAndPlanning,ActiveMeasurement}Screens.kt`,
  `ui/components/TrainPilotCommonComponents.kt`, `MainActivity.kt`.
- `wear/` Wear OS, `com.repforge.app.wear`. `shared/` közös szinkronszerződések (`com.repforge`).
- Telefon ↔ óra: egyetlen natív motor és időzítő-tulajdonos; ownership/epoch/ACK,
  stabil workout/set ID, finalization/discard, receipt, tombstone, restore generation.
  Shared szerződés változásakor mindkét app és a páros verzió érintett.

## Build és teszt
- Helyi Gradle/emulátor telepítés tilos; teljes fordítás/JVM teszt csak GitHub Actionsben.
  Helyben: `rg`, `git diff --check`, statikus ellenőrzés.
- `trainpilot-android-ci.yml`: PR-en automatikus teljes JVM regresszió (Gradle 9.3.1,
  JDK 17, nincs gradlew). `workflow_dispatch` célzott regressziós kapcsolókkal.
  `[skip ci]` commit kihagyja; több javítás → egy közös CI-kör (felhasználói kérésre).
- `trainpilot-persistent-signing.yml`: aláírt telefon release + Wear R8 APK/AAB pár.
  `trainpilot-play-internal-aab.yml`: Play internal. Csak külön engedéllyel.
- Jelentés: forrás SHA, nyers JUnit XML (tests/failure/error/skipped), artifact digest.

## Munkafolyamat
1. Ág/HEAD/nyitott PR ellenőrzés → érintett fájlok `rg`-vel → rövid terv.
2. Minimális diff, célzott regressziós teszt (viselkedést védjen, ne UI-node létezést).
3. Saját ág, draft PR. Merge, release, verzióemelés, Drive-feltöltés, GitHub-komment
   csak kifejezett engedéllyel.
4. Feladat végén frissítsd a `HANDOFF.md`-t (≤40 sor, régit cseréld, ne halmozd).

## Adatvédelem (nem alku tárgya)
- Mentett edzés, program, napló, Room-migráció, DataStore megőrzendő; hibaelfedésre
  nincs reset. Siker csak tartós írás után. Dupla mentés azonos ID-val nem duplikál.
- Hiányzó mérés ≠ 0; tükrözött Samsung/HC/Wear adat egyszer számít; aktív+összes kcal
  nem összeadható. Nincs gyártott Coach- vagy mérési adat.
- GPS/fotó privát tárban; health backup-consent alapból ki. Titkot ne kérj/naplózz/commitolj.
  Tartós signer és package változatlan.

## A felhasználó munkastílusa (tartós)
- Telefonról irányít: rövid, magyar, telefonon olvasható válasz; hosszú naplót ne.
- Visszajelzéseket **gyűjtő draft PR-ba** rögzítjük (`docs/FIXES_<verzió>.md` táblázat);
  kódolás csak „mehet” után, egy közös CI-körrel. UI-változásnál **előbb mockup** (PNG
  Playwrighttal a scratchpadben, SendUserFile), kódolás jóváhagyás után.
- Jóváhagyott minta: zöld CI → merge main → aláírt APK+AAB (`build_aab=true`), 4 fájl
  ellenőrizve (verzió, signer, hash, nyers XML tesztszám) chatben átadva; Drive-csatoló
  ~3 MB/hívás, ezért binárist oda nem. Verzió: `27NN00`/`27NN02`, Play „Újdonságok” ≤500 kar.
- Dizájn: kompakt, témakövető (palette), íves keret, 48 dp érintés; alulról nyíló lapok
  (`TrainPilotSheetRoutes`, `TrainPilotRouteSheet`) a tartalom magasságáig.

## Válaszformátum
Módosítva / Teszt / GitHub / Következő lépés – telefonon olvasható rövidséggel.

# TrainPilot Native – Claude Code

Stabil projektutasítás. Változó állapot: [HANDOFF.md](HANDOFF.md). Ezt a fájlt csak
tartós szabályváltozáskor módosítsd (cache-barát). Kommunikáció: magyarul, röviden.

## Szabályforrás
- [AGENTS.md](AGENTS.md) a kötelező munkarend és adatvédelem. A benne lévő GPT
  modelltábla Claude Code-ra nem vonatkozik; alagens csak felhasználói kérésre.
- Részletes szerződések: `.specify/memory/constitution.md`, dizájn: `docs/FRAME_DESIGN_SYSTEM.md`,
  issue-folyamat: `docs/ISSUE_WORKFLOW_FUNCTION_DESIGN.md`.
- **Kódtérkép: [docs/CODEMAP.md](docs/CODEMAP.md)** – funkció → fájl. Előbb ezt olvasd, utána csak a
  megnevezett fájlt (nagy fájlban `rg -n` + célzott sortartomány). Új funkció/fájl → CODEMAP sor is.
- `docs/archive/` (régi státusz, átadók, kiadási naplók, AI Studio export) történeti: alapból NE olvasd;
  a `.ignore` miatt az `rg` sem keres benne (kell: `rg -uu`). Szétszedési terv: `docs/REFACTOR_PLAN.md`.

## Architektúra
- `app/` telefon, Kotlin/Compose, package `com.example` (applicationId `com.TrainPilot.com`).
  Az élő UI a `com.example` fa (`TrainPilotAppRoot`); a `com.repforge.app` a motor, tárolók, ViewModelek.
  Fájlméret: ≤600 sor cél, >900 sor új fájlnál tilos (`SourceSizeGuardTest`); áthelyezés `scripts/verify_move.py`.
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
  ~3 MB/hívás, ezért binárist oda nem. Verzió: `27NN00`/`27NN02`.
- Play kibocsátási megjegyzés (`docs/PLAY_LISTING.md`): 4 nyelv Play-címkékkel `<hu-HU>` `<de-DE>` `<en-US>` `<ro>`,
  nyelvenként ≤500 kar., külön telefon és óra, a Playen lévő utolsó kiadás óta összes újítás; chatben egy kódblokk.
- Dizájn: kompakt, témakövető (palette), íves keret, 48 dp érintés; alulról nyíló lapok
  (`TrainPilotSheetRoutes`, `TrainPilotRouteSheet`) a tartalom magasságáig.

## Válaszformátum
Módosítva / Teszt / GitHub / Következő lépés – telefonon olvasható rövidséggel.

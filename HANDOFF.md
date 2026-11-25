# Aktuális átadó

- Main: 1.5.0 · 280600/280602 (#361), teljes refaktor után (R0–R8). Előbb `docs/CODEMAP.md`, utána csak a megnevezett fájl.
  Méretőr `SourceSizeGuardTest`; áthelyezés `scripts/split_kotlin.py`, `scripts/verify_move.py`. ViewModel: állapot + mag, funkciók `Vm*.kt`.
- **Integrációs jelölt:** `integration/wear-health-themes` = main `0affd420` + #369 → #370 → #368 → #371 (ebben a sorrendben).
  Szöveges ütközés csak a HANDOFF.md-ben volt; a 10 közös kódfájl automatikusan összefésülődött. Ellenőrizve: dupla deklaráció nincs
  (1 dupla import törölve), MainActivity 1388/1393 sor, méretőr-plafonok rendben, verzió 1.5.1 · 280700/280702. A teljes közös CI dönt.
- **#369 külső edzésimport** (#359): HC + Samsung Room-import (kézi/opt-in auto), stabil forrásazonosító, duplikáció/tombstone, késői metrikák,
  privát GPS (`noBackupFilesDir`). Egyetlen health sync út, nincs új worker. Korlát: Samsung törlésfeed, gyártói export, HC route-consent készüléken.
- **#370 automatikus órás mérés** (#351): célzott kérés (workout ID/revision/epoch/15 s TTL), source node ellenőrzés, ACK csak valós HS start után,
  előtéri fallback; API 33–35 `BODY_SENSORS_BACKGROUND`, 36+ `READ_HEALTH_DATA_IN_BACKGROUND`. Az óra nem veszi át a vezérlést. `docs/WEAR_AUTO_MEASUREMENT_351.md`.
- **#368 Wear dizájn/vezérlés** (#367): v3 kezdőlap (akku-/lépésív), 4 főlap, korona/gesztus, v2 edzésfelület, 3 mp vezérlő-overlay, érintészár,
  pihenő ±15 s, 10 s előjelzés. Szerződés: `controlProtocol367=6`, `dailyStepsGoal`, `restOverridesMs367`, `watchControl367`, `phonePause367`,
  `phoneRest367`, `pauseRevision`, `restRevision`. `docs/WEAR_367_CONTROL_INTEGRATION.md`.
- **#371 témák + Liquid Glass** (#349/#350): 32 gyári téma (ID/kulcs változatlan), 8 glass téma (33–40), saját témák (UUID, színkerék/HEX,
  név Mentés után, teljes képernyős előnézet), Frosted–Clear csúszka csak glass családnál, Wear opak színek. `docs/CUSTOM_THEME_EDITOR_350.md`,
  `docs/LIQUID_GLASS_THEMES.md`. **#372 (theme-test APK) soha nem merge-elhető.**
- **#373 Liquid Glass befejezés:** saját AGSL motor (SDF peremi fénytörés, spekuláris perem, belső árnyék, csak peremi kromatikus szél,
  `LENS_PEAK` kontrasztőr), folytonos 0–100 csúszka élő előnézettel, egyszer fordított programok, valódi backdrop fejléc + csúszó nav pill alatt,
  rugós pill/nyomás, csökkentett mozgás, API31–32/opak fallback, aktív edzésnél nincs üveg. `docs/LIQUID_GLASS_THEMES.md` V4, `LiquidGlassOptics373Test`.
- **2810 gyűjtő (`fix/collector-2810`, draft #377):** saját motor iOS-szerű hangolás (fehéres tónus 40→6%, homály 32→20 dp,
  telítés, vékony perem, árnyék; kontraszt a háttér fénysűrűség-korláttal, `GlassContrast.kt`), több felület glass úton, fejléc alatt
  mind az 5 fül. `docs/FIXES_2810.md` 2–4, 6–7; `docs/LIQUID_GLASS_THEMES.md` V7. #378 (Kyant teszt) soha nem merge.
- **Hátra:** #377 CI zöld → készülékes Liquid Glass elfogadás (képminőség 0/65/100, AGSL Samsungon, frame time, API31–32)
  → main merge, verzió és kiadás csak felhasználói jóváhagyással. 2810/1 (óra olvashatóság) felmérésre.

- **Kyant (2026-11-25, #388, `feat/kyant-glass` main `43036875`-ből):** `docs/KYANT_GLASS_PLAN.md`; K0 `763f2552` / CI #747 `45644836925` zöld: 971 telefon + 248 Wear, 0 failure/error/skipped; shared NO-SOURCE; app+Wear R8 és D8 zöld. Kotlin 2.3.10, telefon Compose BOM 2026.02.00, Kyant 1.0.6, Wear BOM marad. Felhasználó döntése: eredeti package és tartós release signer a végső APK-hoz; nincs main merge/Play külön jóváhagyás nélkül.
- **K1 kódjelölt:** `KyantMaterialSpec.kt` 7 közös szerep, `KyantGlass.kt` egy ablakhoz kötött scene, fejléc külön scene+content, 0–100 és kontrasztőr. `GlassAwareScaffold`, `TrainPilotGlassEngine`, `GlassContentBackdrop`, fő route host, nav/pill, sheet, előnézet, Tile alpha, AGSL press bubble nem fut Kyant forrás mellett. Body nem mintavételezi önmagát; külön ablak fallback. Új szerep-/kontraszttesztek. K1 CI #45647217350 zöld: 973 phone + 248 Wear = 1221, 0 failure/error/skipped, shared NO-SOURCE; R8 és DEX zöld. K2: #379/#380/#386 teljes felületi port, gomb/input/overlay szerepek bekötése, dialogok és képernyőaudit. K3 fizikai teljesítmény, K4 teljes CI, K5 eredeti package+signer, web licenc, APK. Nincs készülékes vagy teljes vizuális elfogadás.

- **K2 integrációs jelölt**: #379 teljes felületi diff és #380 G0–G8 célzott port az azonos mainből, #386 könnyített minta. Sűrű belső elemek Tile/state paint + vektorperem; egy fejléc capture, könnyített vezérlők és aktív mérés; fotó clock/cache. Naptár, program, home, coach, napló, egészség, progress, beállítások, aktív mérés, secondary panelek. K2 regressziók, CI és teljes felületi/overlay elfogadás nyitott; K3–K5 nem kész. #379/#380/#386 lezárva, rögzített SHA-kból referencia; csak #388 nyitott. Main/signing változatlan.

- **K2 follow-up candidate:** audited CI #45652102978 all three jobs green on fc35f2b9 (user reported raw 1233/1233). Common switch paint applied to 11 identified phone source files; mode and language selection bounded tint + selected semantics; default sheet handle theme; shared standard-color alert dialog own-window scene/tint + rim, fallback opaque. No callback/storage/signing/version changes. Candidate CI and device visual review pending; sheet background continuity, remaining menus/snackbars and K3 performance still open.

- **K2 CI #754 / 45654761010** on f1e78f49: 984 phone (19 failures, all UnsupportedClassVersionError), 249 Wear green, total 1233 (1214 passed), 0 error/skipped, shared NO-SOURCE; R8 and DEX green. Likely cause: common alert dialog directly referenced Kyant LayerBackdrop in its composable bytecode on JDK17. Candidate isolates Kyant references in KyantWindowOverlay, invoked only behind existing glass/API/optics gates; legacy path preserves opaque dialog and callbacks. Full rerun required; no tests disabled/changed. K3 held until green.

- **K2 regression correction accepted**: CI #755 / 45657287846 on 57939920: 984 phone + 249 Wear = 1233/1233, 0 failures/errors/skipped; shared NO-SOURCE, R8 and DEX success. Next K2 candidate: continuous static guarded bottom-sheet fill (handle included), body-only static Kyant source, common matte popup menu for 12 audited phone files, status banner overlay and contrast test. New CI and visual/device acceptance pending. No external workout functional changes; K3 profiling remains open.

- **K2 code gate green**: CI #756 / 45659012270 on 6afeb7cc, raw XML 985 phone + 249 Wear = 1234/1234, 0 failure/error/skipped, shared NO-SOURCE; R8 and DEX green. K3 first candidate: bounded UI-thread last-value caches for guarded tones, base and blob brushes (exact geometry/time/color/cap), photo crop and dim filter. No bitmap/view/readback retained, visual math and clock gates unchanged. Three new cache regression tests; new full CI required. Duplicate backdrop recording API audit and physical A/B still pending. No measured speedup claimed.

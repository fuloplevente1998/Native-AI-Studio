# TrainPilot Native – 2815 átadó

- Main `01d35828` + #401 (2815 edzésenergia lefedettség) → 1.5.4 (281000/281002) kiadásra előkészítve; előző aláírt 1.5.3 (280900/280902). Play-on: 1.5.0 (280600).
- `docs/FIXES_2815.md`: eszközön HC energy hiány és Mai állapot vs napló filter. Első passz `WorkoutEnergyCoverage.kt`: raw union/origin/lastEnd, mindkét HC API, részleges/unknown címke, azonos energia-definíciójú separate projection rangsor, coverage-aware retry. Legacy projection változatlan; SDK aggregate contributor nem bizonyított, Samsung unknown.
- Mai állapot és napló külön aktív/total filter, azonos persisted dailySummary és source. `WorkoutEnergyCoverage2815Test` (3). CI ellenőrizendő; fizikai HC/Samsung/Wear és UI teszt nyitott. Main, verzió, signer érintetlen.

- #801 e408132e 1303/0/0/0 + R8. Második passz: Samsung direct empty/error/grant metadata, HC34 workout raw read-only, finish typed projection + forrás/coverage, forráskártya és retry coverage. [skip ci], további audit és egységes CI később; nincs main/build.

- Harmadik passz: `WearWorkoutEnergyEvidence.kt` current receipt guard, phone finalization provisional capture, monotonic durable late summary, rövid aktív órás duration rangsor; `readProvider` közös napi edzésenergia projekció. Wear 3 + Room és SQLite fixture, teljes közös CI ellenőrizendő. Fizikai mérés nyitott.

- Záró passz: durable Wear summary aktív draftban + azonos workout/generation carry-forward, modern live vs saved revision; tényleges command ingress/projection fixture. Kódolási csomag közös CI-re kész; eszközös ellenőrzés nyitott. Main változatlan.

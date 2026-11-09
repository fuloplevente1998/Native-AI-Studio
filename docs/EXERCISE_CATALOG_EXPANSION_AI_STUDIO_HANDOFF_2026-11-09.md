# TrainPilot – exercise catalog expansion / AI Studio implementation handoff (2026-11-09)

> Planning-only draft PR. No runtime/catalog/program source is changed in this PR. AI Studio to implement after user approves list.

## Baseline verified on main
- `app/src/main/resources/native-catalog-main-7473.json`: 106 exercises, 13 programs.
- `app/src/main/resources/native-planner-main-7473.json`: planner roles/areas/needs/favorites/templates.
- Existing `db-floor-press` = Dumbbell Floor Press / 2 dumbbells; distinct from requested barbell floor press.
- Existing barbell back squat, conventional deadlift, bench press, incline bench press, OHP, barbell row and RDL must **not** be duplicated.
- Current home A/B A day contains `db-floor-press`; requested replacement with barbell floor press.

## Proposed 18 new exercises (approval list)
| Proposed stable ID | Hungarian display name | Equipment | English |
| --- | --- | --- | --- |
| barbell-floor-press | Földön fekvenyomás – kétkezes rúddal | kétkezes rúd, biztonságos tartó/segítő | Barbell Floor Press |
| barbell-curl | Bicepszhajlítás – kétkezes rúddal | kétkezes rúd | Barbell Curl |
| barbell-front-squat | Elölguggolás – kétkezes rúddal | rúd + állvány | Barbell Front Squat |
| barbell-hip-thrust | Csípőemelés – kétkezes rúddal | rúd + stabil pad | Barbell Hip Thrust |
| lying-barbell-triceps-extension | Fekvő tricepsznyújtás rúddal | rúd/EZ-rúd + pad | Lying Barbell Triceps Extension |
| sumo-deadlift | Szumó felhúzás – kétkezes rúddal | kétkezes rúd | Sumo Deadlift |
| barbell-good-morning | Jó reggelt gyakorlat – rúddal | rúd + biztonságos állvány | Barbell Good Morning |
| single-arm-db-ohp | Egykezes vállból nyomás | 1 kézisúlyzó | Single Arm Dumbbell Shoulder Press |
| db-chest-fly | Kézisúlyzós tárogatás padon | 2 kézisúlyzó + pad | Dumbbell Chest Fly |
| chest-supported-db-row | Mellkason támasztott evezés | 2 kézisúlyzó + pad | Chest Supported Dumbbell Row |
| concentration-curl | Koncentrált bicepszhajlítás | 1 kézisúlyzó | Concentration Curl |
| db-triceps-kickback | Tricepsznyújtás hátrafelé | 1 kézisúlyzó | Dumbbell Triceps Kickback |
| walking-lunge | Sétáló kitörés | testsúly / kézisúlyzó | Walking Lunge |
| single-leg-db-rdl | Egylábas román felhúzás | kézisúlyzó | Single Leg Dumbbell RDL |
| single-leg-glute-bridge | Egylábas csípőemelés | testsúly | Single Leg Glute Bridge |
| reverse-crunch | Fordított hasprés | testsúly | Reverse Crunch |
| cable-pallof-press | Pallof-nyomás csigán | csigás gép | Cable Pallof Press |
| seated-calf-raise | Ülő vádliemelés | gép | Seated Calf Raise |

Target 124 exercises after approval; no duplicate IDs, labels or near-equivalent entries. Barbell floor press requires safe setup/rack or competent spotter; do not imply safe unracking alone from the floor.

## Program and planner requirements
1. Replace only the relevant home A/B A-day `db-floor-press` program template entry with `barbell-floor-press` after explicit approval. Keep old dumbbell exercise selectable; don't rewrite historical workouts, PRs, saved sessions or user's custom programs.
2. Program template naming should remain user-requested `Otthoni A/B`; verify A/B day structure and references across all 13 programs.
3. Update equipment eligibility, home/gym locations, movement patterns, roles, muscle groups, difficulty, substitutions and planner favorites in native planner, preserving equipment filters and avoiding barbell recommendations to dumbbell-only users.
4. Coach, quick workout, custom program builder, search, progress and exercise detail should consume the same catalog metadata. Verify both phone and Wear can resolve new exercise IDs and display names.
5. Each new entry must include complete Hungarian/English names, concise coaching cues, primary/secondary muscles, setup, controlled execution, safety notes, sets/reps/rest and units/weight semantics. For barbell use total loaded weight (not kg/arm); retain historical weights for existing IDs.
6. Provide **verified** exercise demonstration video links (correct equipment and movement, playable and embeddable where required); no fabricated video URLs. Document attribution/source and fallback for unavailable videos.
7. Test migrations/catalog serialization, ID references, 13 programs, custom and quick workouts, Coach substitutions, search, media links, phone/Wear sync, history, exercise counts and offline behavior.

## Separate device-reported bugs (do not silently bury in feature)
- Rest timer pauses/freezes when Android app backgrounds, even for ~2 seconds. Derive remaining time from monotonic elapsed realtime/deadline; persist/reconcile across pause/resume and configuration changes, don't decrement only via active UI timer.
- Rest timer row sometimes wraps/clips numerals and action labels on narrow phone width; screenshot shows `Kihagy` split onto two lines. Test responsive layout, dynamic font scaling, 00:00/09:59/10:00 and changing digits.
- Existing Wear/phone desynchronization issue: #214 (watch rest timer starts but phone doesn't, watch lags one exercise). Cross-check without conflating separate causes.

## AI Studio acceptance criteria
- New entries only after approval, no duplicates, all videos verified and all fields complete.
- 13 programs compile with valid exercise IDs; requested A-day substitution only.
- All affected UI/Coach/quick/custom/planner flows and phone/Wear pass tests.
- Background rest countdown remains accurate after short and long background intervals, no broken text at common screen/font sizes.
- CI passes, signed phone/Wear APK test pair built, physical device verification documented.

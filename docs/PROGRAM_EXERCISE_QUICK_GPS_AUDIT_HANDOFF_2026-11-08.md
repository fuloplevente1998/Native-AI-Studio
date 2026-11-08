# TrainPilot – teljes program / 106 gyakorlat / egyedi tervek / Gyors edzés / GPS audit és javítás

Dátum: 2026-11-08  
Kiinduló main: `b11ccec89bfafa9e181d1dbaeaeef43bda1ad852`

## 0. Cél

Ez nem egyetlen feliratjavítás. A feladat a TrainPilot teljes edzéskatalógusának és minden olyan útvonalnak az átvizsgálása és rendbetétele, amely a programok, gyakorlatok, egyedi tervek, Gyors edzés vagy GPS-es edzés adatait megjeleníti vagy felhasználja.

A javítás után a felhasználó által látott név, leírás, eszközlista, edzésnapszám, mérési mód és GPS-viselkedés minden esetben a **valós adatból** származzon, ne programnévből vagy hardcoded feltételezésből.

## 1. Kötelező felhasználói döntés

A jelenlegi:

`Otthoni A/B – Alap`

beépített program megjelenített neve legyen egyszerűen:

`Otthoni A/B`

Stabil programazonosító marad:

`home-basic`

Más nyelveken az egyszerű, azonos jelentésű név legyen:

- HU: `Otthoni A/B`
- EN: `Home A/B`
- DE: `Zuhause A/B`
- RO: `Acasă A/B`

### Történeti kompatibilitás

A névváltás **nem írhatja át és nem törölheti** a Napló / `workout_history` korábbi sorait.  
A régi `Otthoni A/B – Alap` névvel mentett történeti rekordok olvashatók maradjanak.  
Ahol régi név alapján történik fordítás, felismerés vagy visszaállítás, ott kompatibilitási alias/fallback szükséges.

A stabil ID-ket ne változtasd meg csak azért, mert a kijelzett név változik.

---

# 2. Már azonosított hibák

## 2.1 Hibás hardcoded eszközcímke

Jelenleg:

`programEquipmentCategoryLabel(programNameOrId, lang)`

a program **nevéből** próbálja kitalálni az eszközöket.

Például minden `otthon`, `home`, `zuhause`, `acasă` vagy `alap` szót tartalmazó programra ezt írhatja:

`Otthoni • Kézi súlyzós & gumiszalag`

Ez hibás.

Az `Otthoni A/B – Alap` / új nevén `Otthoni A/B` jelenlegi gyakorlatlistájában **nincs gumiszalagos gyakorlat**.

A program eszközfelirata ezért nem származhat névből.

## 2.2 A két otthoni A/B program jelenleg tartalmilag azonos

A bundled catalogban:

- `home-basic` – Otthoni A/B – Alap
- `home-level2` – Otthoni A/B – 2. szint

jelenleg ugyanazt a 14 gyakorlatot használja, ugyanabban az A/B felosztásban.

Ez így nem valódi szintlépés.

A 2. szintet valós progresszióvá kell tenni:
- ne legyen egyszerű másolat;
- használjon a 106-os katalógusból szakmailag indokolható nehezebb/haladóbb variációkat és/vagy eltérő prescriptiont;
- ne találj ki nem létező exercise ID-t;
- a program továbbra is legyen koherens az `Otthon` helyszínnel és a megadott szinttel;
- ha egy gyakorlat ténylegesen rudat, padot, húzódzkodórudat stb. igényel, az jelenjen meg az eszközösszegzésben.

## 2.3 Fix `(A/B)` felirat

A Programok képernyő aktív programkártyája jelenleg az edzésnapok száma után fixen `(A/B)` szöveget ír.

Ez hibás az 1, 3 és 4 napos programoknál is.

A kijelzés legyen adatvezérelt:
- `1 edzésnap`
- `2 edzésnap`
- `3 edzésnap`
- `4 edzésnap`

Ha split-nevet is mutatunk, azt a tényleges day code / day name alapján képezzük, ne fix `A/B` stringből.

## 2.4 A jelenlegi kategóriacímke más programokat is félrecímkézhet

A 13 built-in program közül például:
- a `Gumiszalagos A/B` valóban gumiszalagos;
- a `Konditermi Full Body` főleg gépes;
- a `Konditermi Push / Pull / Legs` vegyesen használ rudat, gépeket, csigát és egy gumiszalagos gyakorlatot;
- a `Calisthenics A/B` testsúly + húzódzkodórúd + korlát;
- a `Kettlebell Full Body A/B` kettlebell + testsúly.

Ezekre nem szabad általános név-alapú fallbacket használni.

---

# 3. A 13 beépített program teljes auditja

A bundled catalog jelenleg 13 built-in programot tartalmaz. Mindegyiket vizsgáld át:

1. `home-basic` – Otthoni A/B → átnevezendő
2. `home-level2` – Otthoni A/B – 2. szint
3. `home-varied` – Otthoni változatos
4. `gym-fullbody` – Konditermi Full Body
5. `gym-ab` – Konditermi A/B
6. `home-upper-ab` – Otthoni felsőtest A/B
7. `home-quick-30` – Otthoni gyors 30 perc
8. `calisthenics-ab` – Calisthenics A/B
9. `bands-ab` – Gumiszalagos A/B
10. `kettlebell-ab` – Kettlebell Full Body A/B
11. `gym-upper-lower` – Konditermi felső / alsó
12. `gym-ppl` – Konditermi Push / Pull / Legs
13. `gym-strength-abc` – Konditermi erő A/B/C

Minden programnál ellenőrizd:

- név HU/EN/DE/RO;
- location HU/EN/DE/RO;
- level HU/EN/DE/RO;
- days száma;
- day ID és day name;
- exercises ID-k léteznek-e;
- nincs-e duplikált vagy hibás exercise ID;
- program tényleges gyakorlatai megfelelnek-e a program nevének és szintjének;
- az eszközigény megfelel-e a gyakorlatok `equipment` adatainak;
- nincs-e olyan gyakorlat, amely a helyszínhez nyilvánvalóan ellentmond;
- a kezdő program valóban kezdőbarát-e;
- a 2. szint ne legyen az Alap másolata;
- a napok egymást ésszerűen egészítsék ki;
- a program rövid UI-leírása ne állítson olyat, ami nincs a gyakorlatlistában.

### Ajánlott rövid eszközösszegzések

Ezek irányelvek, nem hardcoded programnév-táblázat. A tényleges gyakorlatadatból kell előállítani az eredményt:

- Otthoni A/B: `Otthon • Kézisúlyzó + rúd + testsúly` – ha a tényleges gyakorlatlista továbbra is ezt igényli
- Otthoni felsőtest A/B: `Otthon • Kézisúlyzó + testsúly`
- Otthoni gyors 30 perc: `Otthon • Kézisúlyzó + testsúly`
- Konditermi Full Body: `Edzőterem • Gépes teljes test`
- Konditermi A/B: `Edzőterem • Gépek + szabad súly`
- Calisthenics A/B: `Saját testsúly • Rúd + korlát`
- Gumiszalagos A/B: `Gumiszalag • Rögzítési ponttal`
- Kettlebell Full Body A/B: `Kettlebell • Teljes test`
- Konditermi felső / alsó: `Edzőterem • Szabad súly + gépek`
- Konditermi Push / Pull / Legs: `Edzőterem • Rúd + gépek + csiga`
- Konditermi erő A/B/C: `Edzőterem • Rúd + gépek`

Az algoritmus az exercise metadata alapján kategorizáljon. Ne a programnévből.

---

# 4. Mind a 106 beépített gyakorlat kötelező auditja

A `NativeBuiltinCatalog` jelenleg explicit ellenőrzi:

`rows.size == 106`

A feladat során mind a 106 sort programozottan és tartalmilag is auditáld.

## 4.1 Strukturális validáció

Minden exercise esetén:

- ID nem üres;
- ID egyedi;
- HU / EN / DE / RO név nem üres;
- `equipment` értelmes;
- `loadType` ismert és kompatibilis;
- `repUnit` ismert és kompatibilis;
- `measurementType` ismert és kompatibilis;
- sets > 0;
- reps értelmezhető a mérési módhoz;
- weight nem negatív;
- rest idő értelmes tartományban;
- muscle adatok nem üresek, ahol szükséges;
- movementPattern ismert;
- difficulty ismert;
- beginnerSafe konzisztens a difficulty/complexity mezőkkel;
- complexity ismert;
- compound érték értelmes;
- goalTags nem tartalmaz hibás/árva értéket;
- style értelmes;
- alternatív exercise ID-k léteznek;
- custom=false a bundled katalógusban;
- media/demo struktúra ne mutasson hibás exercise-re.

## 4.2 Szemantikai ellenőrzés

Ne csak JSON-sémát ellenőrizz.

Példák:
- testsúlyos gyakorlat ne legyen tévesen súlyzós;
- kétkezes rúddal végzett gyakorlat ne legyen `1 kézisúlyzó`;
- gumiszalagos gyakorlat equipmentje ténylegesen gumiszalag legyen;
- gépes gyakorlat ne legyen otthoni kézisúlyzóként címkézve;
- timed exercise ne legyen normál ismétléses logikával kezelve;
- distance exercise ne kapjon strength-set szemantikát;
- unilateral / bilateral adatok legyenek koherensek;
- oldalsó/időalapú gyakorlatok mérése megfelelő legyen;
- default weight és loadType ne mondjon egymásnak ellent;
- magyar név és equipment ne állítson mást, mint az exercise típusa;
- a négy nyelv ne legyen nyilvánvalóan másik gyakorlat fordítása.

## 4.3 Megjelenítési ellenőrzés

Mind a 106 gyakorlat működjön:

- gyakorlatkönyvtárban;
- keresésben;
- szűrőkben;
- Gyors edzés kiválasztásban;
- program editorban;
- egyedi programhoz hozzáadásnál;
- aktív edzésben;
- Naplóban;
- megfelelő mérési UI-n;
- ha GPS-eligible, a GPS indításban;
- ha nem GPS-eligible, ne ajánljon útvonalrögzítést.

## 4.4 Ne legyen hardcoded `106` UI-szám

A felületen jelenleg van `106 gyak.` jellegű hardcoded szöveg.

A kijelzett darabszám mindig a tényleges elérhető exercise collection méretéből jöjjön.

Ha később 107 vagy 105 gyakorlat van, az UI automatikusan kövesse.

A bundled catalog integritásteszt továbbra is tarthat explicit 106-os baseline-t, amíg ez a jóváhagyott katalógusméret, de a felhasználói UI ne legyen hardcoded.

---

# 5. Eszközösszegzés új architektúrája

A jelenlegi:

`programEquipmentCategoryLabel(programNameOrId, lang)`

név-alapú megoldást cseréld le.

## Követelmény

A summary kapja meg a program vagy annak exercises listáját.

Például koncepcionálisan:

`programEquipmentSummary(program, language)`

vagy

`programEquipmentSummary(program.days.flatMap { it.exercises }, language)`

Az algoritmus:
1. összegyűjti a tényleges exercise equipment adatokat;
2. normalizálja őket nagy kategóriákra;
3. deduplikálja;
4. prioritással rövid UI-címkét készít;
5. lokalizál HU/EN/DE/RO nyelvre.

Lehetséges normalizált kategóriák:
- bodyweight
- dumbbell
- barbell
- bench/support
- resistance-band
- kettlebell
- pullup-bar
- dip-bars
- cable
- machine
- cardio-machine
- outdoor/distance
- custom/other

Ne veszítsd el a fontos biztonsági eszközigényt:
- `stabil rögzítés`
- `biztonságos állvány`
- `pad/támasz`
- `húzódzkodórúd`
stb., ha a program szempontjából releváns.

---

# 6. Egyedi programok teljes ellenőrzése

A javítás ne csak built-in programokra működjön.

Érintett:
- kézzel létrehozott saját program;
- személyre szabott/generált program;
- program szerkesztése;
- gyakorlat hozzáadása/törlése/cseréje;
- program aktiválása;
- Drive mentés / visszaállítás;
- újraindítás utáni Room-visszatöltés.

## Követelmények

- Egyedi program equipment summary a tényleges exercises listából készüljön.
- Ne a program nevéből.
- Ha a felhasználó kivesz egy gumiszalagos gyakorlatot, tűnjön el a gumiszalag az összegzésből.
- Ha hozzáad kettlebellt, jelenjen meg.
- Custom program saját leírása ne vesszen el.
- Automatikusan generált program leírása és eszközlistája legyen valós.
- A `userSelectedEquipmentHu` alapján generált terv ne kapjon olyan exercise-t, amelyhez nincs kiválasztott eszköz, kivéve ha az kifejezetten testsúlyos/no-equipment.
- Egy program napjainak száma legyen dinamikus, ne feltételezett A/B.

---

# 7. Egyedi gyakorlatok teljes ellenőrzése

`addCustomExercise(...)` által létrehozott gyakorlatokra is ugyanazok az alapelvek vonatkozzanak.

Ellenőrizd:

- egyedi exercise ID egyedi;
- név megmarad;
- equipment megmarad;
- loadType megfelelően képződik;
- timed beállítás valóban timed UI-t nyit;
- testsúlyos exercise alap súlya 0;
- súlyos exercise ne kapjon véletlen 0/20 kg generikus hibás defaultot olyan esetben, ahol ez félrevezető;
- programba hozzáadható;
- Gyors edzésből indítható;
- Naplóban menthető;
- Drive restore után visszaáll;
- custom exercise törlése/hiánya ne törje el a már meglévő történeti Napló rekordot;
- ha custom exercise distance/GPS típusú lehet, azt csak explicit és valid metadata alapján engedjük.

---

# 8. Gyors edzés teljes audit

Érintett canonical engine:

`NativeWorkoutSession.startQuickWorkout`

és

`NativeWorkoutSession.startQuickExercises`

valamint a UI / ViewModel quick workout útvonal.

## Követelmények

- Beépített és egyedi gyakorlatok egyaránt választhatók.
- A választott exercise tényleges metadata alapján kapja meg:
  - loadType
  - repUnit
  - measurementType
  - sets/reps/weight/rest
  - timed/bilateral állapot.
- Több gyakorlatos Gyors edzés ne duplikálja ugyanazt az exercise ID-t.
- Üres gyors edzés ne induljon.
- A `Gyors edzés` cím lokalizálódjon.
- Gyors edzés mentése a Naplóba ugyanolyan stabil legyen, mint programos edzésé.
- Korábbi history-prefill csak kompatibilis strength exercise-re történjen.
- GPS opció csak GPS-eligible exercise-nél jelenjen meg.
- A `106 gyakorlat` kijelzés legyen dinamikus.
- Custom exercise is jelenjen meg a helyes kategóriában és eszközzel.
- Szűrés/keresés ne ejtsen el gyakorlatot hibás fordítás vagy equipment metadata miatt.

---

# 9. GPS / szabadtéri edzés teljes audit

Érintett területek:
- `NativeWorkoutSession.startQuickWorkout(... recordRoute ...)`
- `NativeOutdoorStart`
- `OutdoorGpsDashboard`
- aktív measurement screens;
- route mentés;
- Napló;
- Wear/phone ownership, ahol releváns.

## Követelmények

### GPS jogosultság / eligibility

A GPS ne programnév alapján működjön.

Csak olyan exercise kapjon GPS route opciót, amely metadata alapján erre alkalmas:
- outdoor/distance típus;
- megfelelő measurementType / repUnit;
- explicit eligibility szabály.

Strength, machine, dumbbell, barbell, band stb. exercise ne kapjon véletlen GPS lehetőséget.

### Indítás

- ismételt tap ne hozzon létre két sessiont;
- aktív sessiont ne írja felül;
- imported draft ne vesszen el;
- GPS indításhoz explicit kiválasztott aktivitás kell;
- a kiválasztott activity ne változhasson csendben másik exercise-re.

### Mérési adatok

Ellenőrizd:
- elapsed time;
- distance;
- pace;
- GPS point/route storage;
- start/stop/pause/resume;
- permission denied;
- GPS disabled;
- nincs fix vagy demo adat;
- beltéri distance exercise kézi távval működjön, ha erre szolgál;
- route csak akkor kerüljön a Naplóba, ha valóban volt route recording.

### Napló

GPS edzés mentése:
- ne törje a normál workout history sémát;
- routeKeys konzisztens legyen;
- distanceKm / pace / duration valós érték;
- történeti GPS rekord olvasható maradjon.

---

# 10. Napló / workout_history védelme

Ez a munka semmilyen körülmények között ne végezzen destruktív migrációt a történeti edzésnaplón.

Nem szabad:
- régi workout_history sorokat tömegesen átnevezni;
- törölni csak azért, mert egy program neve megváltozott;
- exercise ID változtatás miatt eldobni régi history-t;
- custom exercise törlésekor régi history részleteket elveszíteni.

Ha új display név szükséges, render-time alias vagy stabil ID-alapú feloldás legyen, ahol lehetséges.

---

# 11. Lokalizáció

A teljes javítás HU / EN / DE / RO nyelven legyen koherens.

Ellenőrizd:
- 13 program neve;
- location;
- level;
- day name;
- equipment summary;
- `edzésnap` felirat;
- Gyors edzés;
- GPS / distance kapcsolódó rövid címkék;
- custom/generate fallbackek.

Ne maradjon olyan fallback, amely DE/RO nyelven EN vagy HU programnevet ad, ha a bundled katalógusban van rendes fordítás.

---

# 12. Kötelező automatikus audit / regresszió

Ne kézi szemrevételezés legyen az egyetlen védelem.

Készíts vagy bővíts automatizált tesztet / audit scriptet, amely legalább:

## Catalog

- pontosan 106 built-in exercise;
- minden ID egyedi;
- minden program exercise ID létezik;
- 13 built-in program;
- nincs üres programnap;
- nincs ugyanazon napon duplikált exercise ID, kivéve ha tudatosan engedélyezett és dokumentált;
- alternative exercise ID létezik;
- négy nyelvi név jelen van;
- equipment nem üres;
- loadType / repUnit / measurementType kombináció valid;
- negatív weight nincs;
- sets valid;
- GPS eligibility és measurement metadata konzisztens.

## Programok

- `home-basic` kijelzett HU neve pontosan `Otthoni A/B`;
- stable ID továbbra is `home-basic`;
- régi `Otthoni A/B – Alap` string kompatibilitási aliasa működik, ahol szükséges;
- `home-level2` exercise signature nem azonos a `home-basic` signature-rel;
- equipment summary a gyakorlatokból készül;
- `home-basic` summary nem tartalmaz `gumiszalag` szót, ha nincs band exercise;
- `bands-ab` summary tartalmaz gumiszalag jelentést;
- day count nem fix `A/B`.

## Custom

- custom program summary változik exercise hozzáadás/törlés után;
- custom exercise quick workoutból indítható;
- custom program persist/reload után ugyanazt mutatja;
- history nem sérül.

## Quick

- dinamikus exercise count;
- no duplicate selected ID;
- canonical quick session;
- metadata megmarad.

## GPS

- eligible activity GPS-szel indulhat;
- nem eligible strength exercise GPS-szel nem indulhat;
- repeated start nem duplikál sessiont;
- route metadata menthető.

---

# 13. Javasolt fájlok, amelyeket biztosan át kell nézni

## Projekt szabályok

AGENTS.md  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/AGENTS.md

WORK_HANDOFF  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/docs/WORK_HANDOFF.md

CURRENT_STATUS  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/docs/CURRENT_STATUS.md

## A 106 gyakorlat + 13 program bundled forrása

Catalog JSON  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/resources/native-catalog-main-7473.json

Catalog loader / invariant  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/data/db/NativeBuiltinCatalog.kt

## Modellek / fordítás

TrainPilotModels.kt  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/model/TrainPilotModels.kt

## Programok / Gyors edzés UI

WorkoutAndPlanningScreens.kt  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/screens/WorkoutAndPlanningScreens.kt

A jelenlegi hibás equipment label  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/components/AnatomicalBodyAndMapComponents.kt

Common components  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/components/TrainPilotCommonComponents.kt

## Állapot / custom program / custom exercise

TrainPilotViewModel.kt  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt

MainActivity wiring  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/MainActivity.kt

## Canonical workout engine / Quick / GPS

NativeWorkoutSession.kt  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/NativeWorkoutSession.kt

OutdoorGpsDashboard.kt  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/screens/OutdoorGpsDashboard.kt

ActiveMeasurementScreens.kt  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/screens/ActiveMeasurementScreens.kt

## Napló

LogAndProgressScreens.kt  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/screens/LogAndProgressScreens.kt

## Room

DAOs  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/data/db/TrainPilotDaos.kt

Entities  
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/data/db/TrainPilotEntities.kt

---

# 14. Implementációs szabályok

1. Ne dolgozz közvetlenül mainen.
2. Hozz létre külön branchet és PR-t.
3. Ne merge-elj automatikusan.
4. Ne változtasd meg a package ID-t: `com.TrainPilot.com`.
5. Ne emelj verziót csak emiatt.
6. Ne törj Wear kompatibilitást.
7. Ne érintsd destruktívan a workout_history-t.
8. Ne gyengíts meglévő regressziós tesztet csak azért, hogy átmenjen.
9. Ne helyettesíts egy hardcoded név-táblázatot egy másik hardcoded név-táblázattal, ha az adatból levezethető.
10. A built-in és custom útvonal ugyanazt az equipment-summary alapelvet használja.

---

# 15. CI / kész definíció

A munka csak akkor tekinthető késznek, ha:

- új célzott regressziók zöldek;
- teljes phone JVM regression zöld;
- teljes Wear JVM regression zöld;
- 0 failure;
- 0 error;
- 0 skipped;
- compile sikeres;
- a 106 exercise audit PASS;
- a 13 program audit PASS;
- custom program + custom exercise tesztek PASS;
- Quick workout tesztek PASS;
- GPS eligibility / session tesztek PASS.

Main merge csak explicit felhasználói jóváhagyással.

---

# 16. Várt kézi ellenőrzés

Kész PR után legalább ezeket manuálisan nézd meg:

1. Programok lista – `Otthoni A/B`, gumiszalag nélkül.
2. Aktív program kártya – helyes edzésnapszám, nincs fix A/B.
3. Gumiszalagos A/B – helyes band summary.
4. Konditermi Full Body – gépes summary.
5. Kettlebell – kettlebell summary.
6. Custom program – exercise módosításra frissülő summary.
7. Custom exercise – könyvtár, program editor, Gyors edzés.
8. Gyors edzés – tényleges aktuális exercise count.
9. Strength exercise – nincs GPS.
10. Futás/séta/túra jellegű eligible exercise – GPS működik.
11. GPS mentés – helyes distance/time/pace/route a Naplóban.
12. Régi `Otthoni A/B – Alap` Napló sorok továbbra is megjelennek.

---

# 17. Rövid AI Studio prompt

```text
Dolgozd fel a következő TrainPilot átadót teljes egészében, ne csak az első hibát javítsd:

https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/docs/full-program-exercise-audit-20261108/docs/PROGRAM_EXERCISE_QUICK_GPS_AUDIT_HANDOFF_2026-11-08.md

Először olvasd el az AGENTS.md-t és az átadóban felsorolt raw source fájlokat.

A feladat:
- Otthoni A/B – Alap megjelenített neve legyen Otthoni A/B, stable id home-basic maradjon;
- szüntesd meg a név-alapú hardcoded program-eszközcímkézést;
- auditáld és javítsd mind a 13 built-in programot;
- auditáld mind a 106 built-in gyakorlatot strukturálisan és szemantikailag;
- a home-level2 ne legyen a home-basic másolata;
- a program napok száma ne használjon fix (A/B) suffixet;
- ugyanaz a helyes, adatvezérelt logika működjön custom programokra és custom exercise-ekre;
- auditáld a Gyors edzést;
- auditáld a GPS/szabadtéri edzést és eligibility logikát;
- workout_history/Napló történeti adatait ne módosítsd destruktívan;
- készíts automatizált regressziókat/auditot a 106 exercise és 13 program védelmére.

Külön branch + PR.
Mainre ne merge-elj explicit felhasználói jóváhagyás nélkül.
Futtasd a teljes TrainPilot phone + Wear CI-t, és a végén add meg a PR-t, head SHA-t, tesztszámokat és minden javított terület rövid összefoglalását.
```

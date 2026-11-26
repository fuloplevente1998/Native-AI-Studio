# 6. feladat – 2816 közös PR (egészségi összegző, rekordlista, lapok)

Alap: `main` (4dad0b97 utáni, verzió 1.5.4 / 281200 Play-kóddal). Ág: `fix/2816-all`, EGY draft PR, egy közös CI. Szabályok: docs/tasks/2812/README.md (tilos lista, adatvédelem, CI). Megfigyelések: docs/FIXES_2816.md (1–5. sor). A verziószám NEM változik.

## A) Mentett edzés: teljes kcal + pulzus mindig látszódjon (1–2. sor) – LEGFONTOSABB
Fájlok: `app/src/main/java/com/repforge/app/NativeWorkoutHealthSummary.kt` (`project`, `completeTotal`, `select`, `read`), `ui/journal/JournalWorkoutHealthSummary.kt` (csak `result.workoutTotal`-t mutat), `ui/journal/JournalHealthPanel.kt`, `ui/health/HealthWorkoutEnergyDay.kt`.
Hiba: régi és új mentett edzéseknél a pulzus/energia „Nincs adat", vagy csak az aktív (kis) kcal látszik. Elvárt (felhasználó): az edzés időtartamára az ÖSSZES kcal és a pulzus, minden korábbi naplózott edzésnél.
Teendő:
1. `completeTotal`: a szigorú `rows.sumOf{milliseconds} != expectedMs` helyett tolerancia (pl. ≤ 60 s vagy ≤ 2% eltérés), és ha nincs igazolt lefedettség (UNKNOWN), a szolgáltató összesített `totalCalories` értéke az edzés ablakára akkor is látszik, forrás-címkével és „lefedettség nem igazolt" jelzéssel. Csak ismert hiányos (PARTIAL_RAW/MISSING) vagy ismert hibás forrás maradjon ki, és akkor se tűnjön el a megjelenítés: mutasd a legnagyobb lefedettségű egyetlen forrást „részleges" jelzéssel. SOHA ne becsülj, ne adj össze különböző forrást.
2. A mentett edzés összegzése a Samsung-kapcsolótól FÜGGETLENÜL mutassa a már mentett Samsung bizonyítékot (forráscímkével); a kapcsoló csak az új lekérdezést és a csempéket szabályozza.
3. Pulzus: ha van mentett átlag/max pulzus bármely forrásból az ablakra, látszódjon („Nincs adat" csak ha tényleg nincs); `select`/overlap szűrés ne ejtsen el mindent.
4. Aktív kcal külön sorban marad; a fő energia érték a TELJES kcal.
5. Adatvédelem: a nyers mentés, Room, DataStore, health240Json változatlan; csak megjelenítési/projekciós logika. Hiányzó mérés ≠ 0.
6. Regressziós tesztek: (a) régi mentés ms-eltéréssel → a teljes kcal látszik; (b) Samsung kapcsoló ki + mentett Samsung adat → látszik; (c) 45 perc aktív + szünet → csak az aktív ablak teljes kcal; (d) ismert részleges lefedettség → látszik „részleges" jelzéssel, nem eltűnik; (e) valódi hiány → „Nincs adat".

## B) Egészségnapló: részleges rekordlista kivétele (3. sor)
`StudioHealthMetricsBrowser.kt` (~262–305: `journal_records_toggle_$date`, `HealthRecordDayDetails`). Kerüljön ki a felületről a napi rekordlista és a „Rekordok" kapcsoló. A lekérdezés/tárolás és a napi összegek maradnak. A Mai állapot csempék (`HealthTodayTileExtras.kt:256`) listái MARADNAK. Igazítsd a teszteket (ne töröld a viselkedés-ellenőrzést; ahol a lista létezését ellenőrizték, ott azt, hogy a lista nincs a naplóban, a Mai állapotban viszont van).

## C) Mai állapot csempék egy lépésben nyíljanak (4. sor)
`HealthTodayStatusSheet.kt`: a #407 előtti stabil, előre lefoglalt magasság (`height(maxDetailHeight)` a lépés/alvás csempére, de gondold végig mind a hat csempére), hogy a lap egy lépésben nyíljon. Nincs magasságugrás betöltéskor.

## D) Lapok akadása, nem csak glass témán (5. sor)
`TrainPilotModalBottomSheet.kt`, `TrainPilotScreenSheet.kt`, `KyantWindowOverlay.kt`, `GlassScene.kt`. A tünet sima témákon is jelentkezik és két részletben nyílik → a közös lap-magasság/animáció a gyanús (magasságrögzítés, tartalomhoz igazodó magasság). Cél: egy lépéses, stabil nyílás: a Material `ModalBottomSheet` alap viselkedésére egyszerűsíts, a tartalom magasságát ne mérd/rögzítsd újra animáció közben, a glass csak statikus festék legyen a lap tartalmán. Ahol dinamikus magasság kell (rövid–hosszú tartalom), a magasság változás ne animáljon húzás alatt. Írd le az okot (fájl:sor) és mit egyszerűsítettél; a javulás eszközön igazolandó.

## Kész, ha
A–D kész, CI zöld (nyers XML számok a PR-ban), verzió változatlan. A merge-et és a buildet a felhasználó munkamenete végzi, TE NE merge-elj.

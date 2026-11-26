# 7. feladat – TrainPilot edzés: pulzus/energia csak Wear mérésből + „telepítsd az órára" tipp

Alap: `main` (e53a35a6 utáni, 1.5.4 / 281300). Ág: `fix/2817-wear-only`, EGY draft PR, egy közös CI. Szabályok: docs/tasks/2812/README.md. Mockup: docs/mockups/2817/task7_workout_wear_only_tip.png.

## Háttér (eszközön igazolt, felhasználói megfigyelés)
Telefonról indított TrainPilot edzésnél az óra nem kapcsol edzés módba (Health Services munkamenetet csak az órán futó app indíthat), ezért a Health Connect / Samsung ablakos adat gyenge: kicsi, részleges kcal, hiányzó pulzus. Az órán indított (Samsung vagy TrainPilot Wear) edzésnél a mérés teljes. Telefonról nem indítható távolról a Health Services mérés, ezért a TrainPilot Wear app a támogatott út. Döntés: a TrainPilot edzés pulzusa/energiája CSAK a Wear mérésből jön; az Egészségnapló és Mai állapot marad Health Connect-alapú (a napi adatot jól kezeli).

## A) TrainPilot edzés részletei: forrás = csak Wear
Fájlok: `app/src/main/java/com/repforge/app/NativeWorkoutHealthSummary.kt` (`readJournal`/`projectJournal`/`project`), `ui/journal/JournalWorkoutHealthSummary.kt`, `ui/journal/JournalHealthPanel.kt` (Forrás fül), `com/example/ui/screens/log/LogWorkoutDetailScreen.kt`, `ui/components/StudioWorkoutHealth.kt`.
1. A TrainPilotban naplózott (nem importált) edzésnél az „Edzés pulzus és energia" kártya és a Forrás fül CSAK a Wear (TrainPilot WearOS) mérést használja és mutatja. A tárolt Health Connect / Samsung bizonyíték a mentésben MEGMARAD (health240Json, nyers mentés, Room, DataStore változatlan), csak nem jelenik meg és nem vesz részt az összesítésben ennél az edzéstípusnál. Nincs törlés, nincs migráció.
2. Külső (importált) edzésnél (`externalWorkout359`) a saját forrása (Health Connect vagy Samsung) látszik, mint eddig.
3. Wear mérés van: átlag/max pulzus, aktív kcal, összes kcal, forrás „TrainPilot WearOS"; ha a Wear mérés részleges, jelzés „Részleges mérés" (érték nem tűnik el). Nincs becslés, hiányzó mérés ≠ 0.
4. Wear mérés nincs: „Nincs óramérés ehhez az edzéshez." + a tipp (B pont). Pulzus/kcal nem jelenik meg Health Connect-ből erre az edzésre.
5. A korábbi bonyolult választás (lefedettség-rang, részleges forrás-választás HC/Samsung között) a TrainPilot edzés ágon fölösleges: egyszerűsítsd, de a külső edzés és a napi (Egészségnapló) ág viselkedése ne változzon. Meglévő tesztet ne gyengíts; ahol a régi viselkedést ellenőrizték, ott az új szabályt ellenőrizd (Wear-only; HC/Samsung adat tárolva, de rejtett).

## B) Tipp: „Telepítsd az órára a TrainPilot appot"
- Megjelenik az edzés részletein, ha a TrainPilot edzésnek nincs Wear mérése. Szöveg 4 nyelven (hu/en/de/ro): „A pontos pulzushoz és kalóriához telepítsd az órára a TrainPilot appot. A telefonról indított edzés ilyenkor automatikusan elindítja az óra mérését."
- Gombok: „Telepítés az órára" (Play Áruház link a Wear alkalmazásra, `com.TrainPilot.com`, a Play a telefonról az órára telepít) és „Később" (elrejti az adott edzésnél; ne zavarjon minden edzésen).
- Ne jelenjen meg, ha az óra app már párosítva van és az edzésnek volt mérése, vagy ha az edzés importált.
- Üveg és nem üveg palettán működjön; 48 dp érintési cél; új sztring mind a 4 nyelven.

## C) Egészségnapló / Mai állapot: Health Connect egyetlen forrás
Ellenőrzés és minimális igazítás: a napi adatok forrása a Health Connect (a Samsung csak a kapcsolóval, ahogy eddig). Ne módosítsd, ha már így működik; csak győződj meg róla, hogy az A pont nem rontja el.

## Tesztek
Tiszta logikai: TrainPilot edzés + Wear mérés → látszik; + csak HC/Samsung evidence → „Nincs óramérés", HC adat tárolva marad; importált edzés → saját forrás; részleges Wear → jelzéssel látszik; tipp megjelenési szabály (van/nincs Wear mérés, importált, „Később").

## Kész, ha
A–C kész, CI zöld (nyers XML számok), verzió változatlan (281300/281302), a merge-et és a buildet a felhasználó munkamenete végzi.

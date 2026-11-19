# 2772 – gyűjtött javítások

Kódolás csak „mehet” után, egy közös CI-körrel.

| # | Terület | Visszajelzés | Terv | Állapot |
|---|---------|--------------|------|---------|
| 1 | Programok – sorrend húzás | A kártya kerete nem követi a mozgatást, lemarad (üres keret marad a helyén) | Ok: a programkártyán a keret a mozgatás *előtt* van a modifier-láncban, ezért nem mozdul. Megoldás: `trainPilotReorderItem` a keret elé (mint a gyakorlatsoroknál). Teszt: húzás közben a keret a kártyával mozog | kódolva |
| 2 | Egészségnapló szűrő | Túl nagy a naptár; legyen olyan, mint a Napló naptár-szűrője | Kezdő/Zárónap gombok + „Alkalmaz” helyett egy szűrőgomb (📅 időszak ⌄) + frissítés ikon. Megnyitva ugyanaz a közös naptár-lap, mint a Naplóban: 2 nap = intervallum (hónapokon át is), Mai nap, Szűrő törlése (alap: utolsó 30 nap), 6 sor, oldalra húzás, Rendben. Közös komponens a Naplóval | kódolva |
| 3 | Mai nap / Szűrő törlése (Napló + Egészség) | Legyenek jobban kiemelve | Teljes szélességű, 48 dp gombpár a naptár felett: Mai nap = kitöltött elsődleges szín 📍, Szűrő törlése = piros tónusú ✕ | kódolva |
| 4 | Gyors edzés – újragondolás | Gyakorlatlista és kültéri edzés új oldalra visz – furcsa. A mostani gyakorlatválasztó jó, csak ne külön oldal legyen; Edzés fülön maradjon külön Erő / Időzített / Kültéri gomb (Időzített most hiányzik) | Egy „Gyors edzés” lap 3 füllel (💪 Erő · ⏱ Időzített · 🗺 Kültéri). Erő = a mostani gyakorlatválasztó 1:1 (keresés, Kedvencek, Új gyakorlat, kategóriák, ☆ sor ▶ Indítás ⋮); Időzített = ugyanez időalapúra szűrve; Kültéri = mini nézet (tevékenység, GPS kapcsoló, kis térkép, 3 csempe, kézi táv, Indítás) → indítás után a mostani teljes élő nézet. Sorba tett gyakorlatok sávja az Erő fül alján marad. Edzés fül: Gyors edzés kártya + 3 magas csempe (felül cím, alatta nagy ikon; Kültérinél a megszokott útvonal-grafika) → ugyanaz a lap a megfelelő fülön. Gyors edzés gomb: félmagas, utolsó fül | kódolva |
| 5 | Kültéri edzés – kézi táv mező | Túl szögletes, túl nagy; a felette lévő 3 mező nem egyforma magas/széles | Mező: app-stílusú kompakt beviteli mező (48 dp, 12 dp ív, palette színek), Mentés gomb azonos magas. 3 metrika-csempe: azonos szélesség, sormagasságra igazítva (`IntrinsicSize.Min` + `fillMaxHeight`), 1 soros feliratok ellipszissel | kódolva |

## Bekötés-ellenőrzés (régi → új belépési pont)
| Funkció | Régi | Új | Hívott művelet |
|---|---|---|---|
| Erő-gyakorlat indítása | Gyors edzés: Erő legördülő + Indítás | Erő fül: sor ▶ Indítás | `selectLibraryExercise` (erő/időzített szerint) |
| Időzített gyakorlat indítása | Gyors edzés: Időzített legördülő + Indítás | Időzített fül: sor ▶ Indítás | `selectLibraryExercise` → `startQuickTimedWithExercise` |
| Sor (queue) indítása / törlés / hozzáadás | Gyors edzés sáv + Gyakorlatválasztó oldal | Erő/Időzített fül: sáv + ⋮ menü | `startQueuedWorkout`, `add/removeExerciseFromWorkoutQueue` |
| Kedvenc, kedvencszűrő, kategória, keresés, Új gyakorlat, részletek, videó | Gyakorlatválasztó oldal | ugyanez a lapban | `toggleExerciseFavorite`, `toggleFavoriteExercisesOnly`, `selectExerciseCategory`, `updateExerciseSearch`, `toggleAddCustomExerciseDialog`, `openExerciseDetail` |
| Kültéri edzés | Gyors edzés / Edzés fül → GPS oldal | Kültéri fül: tevékenység → Tovább | `startOutdoorWorkout(tevékenység)` → teljes GPS-nézet |
| Edzés fül belépők | Gyakorlatválasztó, Kültéri kártya | Erő / Időzített / Kültéri csempe | `openQuickWorkoutSheet(0/1/2)` |
| Egészségnapló időszak | Kezdőnap/Zárónap + Alkalmaz | Szűrőgomb → időszak-lap | from/to/end állapot, Mai nap, Törlés (30 nap) |
| Indítás kiválasztás nélkül (mai programnap) | Gyors edzés: Indítás | Edzés fül: A/B nap kártya ▶ Indítás (változatlan) | `startPlannedWorkout` – a lapra nem kell (felhasználói döntés) |

## Dizájn-kör (2. rész)
| # | Terület | Visszajelzés | Terv | Állapot |
|---|---------|--------------|------|---------|
| 6 | Világos témák | Sötét foltok: piros ✕ bezárás, sorozat „–” gomb, zöld „Mentés (JSON)”, több helyen | Ok: `TrainPilotActionAppearance` fix sötét színei. Világos palettán világos tónus (zöld: #DDF3E8 / #146C43, piros: #FBE3E3 / #B3261E), sötét palettán marad a mostani. Egy helyen javul minden mentés/törlés/bezárás gomb | mockup küldve |
| 7 | Aktív edzés alsó sáv | 3 gomb, Elvetés középen (vagy jobb javaslat) | A) ‹ Előző · ✕ Elvetés (megerősítéssel) · Következő ›; B) javaslat: ‹ Előző · ⏹ Befejezés (lap: Mentés és befejezés / Elvetés) · Következő › | mockup küldve, döntésre vár |
| 8 | Következő gomb | A gyakorlat neve felesleges (fent látszik) | „Következő ›”; utolsó gyakorlatnál „Befejezés ›” | mockup küldve |

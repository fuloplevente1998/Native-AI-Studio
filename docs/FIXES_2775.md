# 2775 – gyűjtő

| # | Hol | Visszajelzés | Javaslat | Állapot |
|---|-----|--------------|----------|---------|
| 1 | Edzés közben – Coach gomb | Túl nagy | Kompakt chip: 32 dp látható / 48 dp érintés, kis betű + állapotpont (mockup C) | kódolva |
| 2 | Kétoldali stopper (pl. Oldalsó plank) | Duplikált szöveg (cím „Oldalsó plank • Kétoldali stopper”, név újra, „7/7 gyakorlat • Aktív cél”, „Gyakorlat váltása ▾”), más szín (zöld Indítás), nincs gyakorlatsor | Közös fejléc mint Erő: „Program • n/N gyakorlat”, napjelvény + Coach, vízszintes gyakorlatsor, név ⌄ + egy alcím, Videó, ikon(i); Indítás sárga; futó oldal sárga keret (mockup A) | kódolva |
| 3 | Időalapú stopper (pl. Plank) | Ugyanez: nincs gyakorlatsor/napjelvény/Coach, „Koppints a váltáshoz” felesleges | Ugyanaz a közös fejléc; Intervallumok, sorozatok, Milyen volt? marad (mockup B) | kódolva |
| 4 | Minden aktív oldal | Ellenőrzés | Erő egységes; Kültéri GPS saját térképes oldal, nem érintett | ellenőrizve |
| 5 | Erő oldal – ötletek (mockup D) | „Javíthatunk?” | 1) „+ + Új sorozat” dupla + → „+ Új sorozat”; 2) piros „–” → ⋮ menü; 3) kész sorozat zöld sor; 4) „legutóbb” sor csak mentett előzménnyel; 5) alcímből Pihenő ki | kódolva (meglévő Coach-jelölések: chip állapotpont, aktív sor kerete maradnak) |
| 6 | Alsó sáv – Befejezés | Sárga emoji-négyzet (⏹) | Téma szerinti másodlagos gomb + rajzolt ikon (alsó sáv és élő sáv) | kódolva |
| 7 | Témák – teljesen fekete háttér | Sok helyen fekete, korábban témakövető | Ok: 2769 lap háttér `surface`→`background` (Kontraszt témákban #000). Javaslat: lap háttér = background/surface keverék témánként (mockup E) | elvetve (Kontraszt témában szándékos) |

| 8 | Felső gombok rendezése | Coach, Videó, súlyzó-ikon össze-vissza; „A nap” jelvény funkció nélkül | **A) jóváhagyva**: név alatt egy eszközsor: piros Videó (marad) · ✦ Coach (állapotpont) · Részletek (gyakorlat-ikon); „A nap” sima szöveg + „x / y sorozat kész” | kódolva |
| 9 | Szív (edzés egészségadatai) | Kell-e edzés közben? | **1) jóváhagyva**: Erő/Időalapú/Kétoldali fejlécből ki; Összesítőn és Kültérin marad (`StudioWorkoutHealthAction`, `LocalOpenWorkoutHealth` változatlan) | kódolva |

## Bekötés (régi → új belépők)
| Funkció | Régi | Új | Callback |
|---|---|---|---|
| Gyakorlat váltása (időalapú) | névre koppintás | közös címsor név ⌄ | `onOpenExerciseLibrary` (csere mód) |
| Gyakorlat váltása (kétoldali) | „Gyakorlat váltása ▾” | közös címsor név ⌄ | `onOpenExerciseLibrary` |
| Gyakorlatsor (ugrás) | csak Erő | mind a 3 oldal | `onSwitchExercise` |
| Részletek | ikon + „i” jelvény, csak Erő | „Részletek” gomb a gyakorlat-ikonnal, mind a 3 oldal | `onOpenExerciseDetail` (MainActivity bekötve) |
| Coach | napjelvény mellett, csak Erő | eszközsor, mind a 3 oldal | Coach lap (`StudioWorkoutCoachPanel`) |
| Sorozat törlése | piros „−” | ⋮ → „Sorozat törlése” | `onDelete` (változatlan) |
| Befejezés | „⏹” emoji | rajzolt ikon, másodlagos gomb | `WorkoutFinishSheet` (változatlan) |

# 2775 – gyűjtő

| # | Hol | Visszajelzés | Javaslat | Állapot |
|---|-----|--------------|----------|---------|
| 1 | Edzés közben – Coach gomb | Túl nagy | Kompakt chip: 32 dp látható / 48 dp érintés, kis betű + állapotpont (mockup C) | mockup |
| 2 | Kétoldali stopper (pl. Oldalsó plank) | Duplikált szöveg (cím „Oldalsó plank • Kétoldali stopper”, név újra, „7/7 gyakorlat • Aktív cél”, „Gyakorlat váltása ▾”), más szín (zöld Indítás), nincs gyakorlatsor | Közös fejléc mint Erő: „Program • n/N gyakorlat”, napjelvény + Coach, vízszintes gyakorlatsor, név ⌄ + egy alcím, Videó, ikon(i); Indítás sárga; futó oldal sárga keret (mockup A) | mockup |
| 3 | Időalapú stopper (pl. Plank) | Ugyanez: nincs gyakorlatsor/napjelvény/Coach, „Koppints a váltáshoz” felesleges | Ugyanaz a közös fejléc; Intervallumok, sorozatok, Milyen volt? marad (mockup B) | mockup |
| 4 | Minden aktív oldal | Ellenőrzés | Erő egységes; Kültéri GPS saját térképes oldal, nem érintett | ellenőrizve |
| 5 | Erő oldal – ötletek (mockup D) | „Javíthatunk?” | 1) „+ + Új sorozat” dupla + → „+ Új sorozat”; 2) piros „–” → ⋮ menü; 3) kész sorozat zöld sor; 4) „legutóbb” sor csak mentett előzménnyel; 5) alcímből Pihenő ki | mockup |
| 6 | Alsó sáv – Befejezés | Sárga emoji-négyzet (⏹) | Téma szerinti másodlagos gomb + rajzolt ikon (alsó sáv és élő sáv) | mockup |
| 7 | Témák – teljesen fekete háttér | Sok helyen fekete, korábban témakövető | Ok: 2769 lap háttér `surface`→`background` (Kontraszt témákban #000). Javaslat: lap háttér = background/surface keverék témánként (mockup E) | mockup, kérdés |

Bekötés (kódoláskor): név ⌄ → könyvtár csere módban; gyakorlatsor → `onSwitchExercise`; Videó; ikon(i) → részletek; Coach chip → Coach lap; stopper gombok, Rögzítés, Intervallumok változatlan.

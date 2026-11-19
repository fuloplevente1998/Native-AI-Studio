# 2775 – gyűjtő

| # | Hol | Visszajelzés | Javaslat | Állapot |
|---|-----|--------------|----------|---------|
| 1 | Edzés közben – Coach gomb | Túl nagy | Kompakt chip: 32 dp látható / 48 dp érintés, kis betű + állapotpont (mockup C) | mockup |
| 2 | Kétoldali stopper (pl. Oldalsó plank) | Duplikált szöveg (cím „Oldalsó plank • Kétoldali stopper”, név újra, „7/7 gyakorlat • Aktív cél”, „Gyakorlat váltása ▾”), más szín (zöld Indítás), nincs gyakorlatsor | Közös fejléc mint Erő: „Program • n/N gyakorlat”, napjelvény + Coach, vízszintes gyakorlatsor, név ⌄ + egy alcím, Videó, ikon(i); Indítás sárga; futó oldal sárga keret (mockup A) | mockup |
| 3 | Időalapú stopper (pl. Plank) | Ugyanez: nincs gyakorlatsor/napjelvény/Coach, „Koppints a váltáshoz” felesleges | Ugyanaz a közös fejléc; Intervallumok, sorozatok, Milyen volt? marad (mockup B) | mockup |
| 4 | Minden aktív oldal | Ellenőrzés | Erő egységes; Kültéri GPS saját térképes oldal, nem érintett | ellenőrizve |

Bekötés (kódoláskor): név ⌄ → könyvtár csere módban; gyakorlatsor → `onSwitchExercise`; Videó; ikon(i) → részletek; Coach chip → Coach lap; stopper gombok, Rögzítés, Intervallumok változatlan.

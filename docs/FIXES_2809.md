# 2809 – gyűjtő (1.5.1 · 280700/280702 után)

| # | Téma | Hol | Megfigyelés (képernyőképek 2026-11-25, glass 100%) / javaslat | Állapot |
|---|------|-----|----------|---------|
| 1 | Üveg nem látszik – egyszínű háttér | Telefon – minden glass lap | A lap háttere sima, sötét barna színátmenet; a 100%-os üveg mögött nincs mit átlátni, ezért tömör kártyáknak tűnnek. iOS-szerű hatáshoz élénk, lassan mozgó háttér kell: palettaszínű lágy „folt” / mesh-gradiens (opcionálisan saját háttérkép), csak glass témában; reduced motion esetén álló | felmérésre |
| 2 | Maradék keretek | Telefon – glass témák | Még látható: a nagy külső tartalomkeret, keresőmező körvonala, Fejlődés/Statisztika szegmens- és chip-körvonalak, „Aktív”/címke pill keret, nav kijelölő körvonal, kártyakeretek egy része. Ezek nem a `trainPilotBorder`-en át rajzolódnak (`Modifier.border`, `BorderStroke` Surface/OutlinedTextField). Teljes audit: glass témában a semleges keretek el, helyettük üvegperem-fény; szemantikus (veszély/kijelölés) marad | felmérésre |
| 3 | Kártyák túl tömörek 100%-on | Telefon – kártyák, csempék | A belső csempék (Alvás, Lépések, Programkártyák, Napló sorok) tömör barna kitöltésűek. 100%-on alacsony fedettségű tónus + blur/mintavétel kell, a szöveg kontrasztőr marad | felmérésre |

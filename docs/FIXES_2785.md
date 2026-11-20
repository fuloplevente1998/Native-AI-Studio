# 2785 – gyűjtő (1.4.0 / 278400 után)

| # | Hol | Visszajelzés | Javaslat | Állapot |
|---|-----|--------------|----------|---------|
| 1 | App szerte – grafikonok | Egyik oszlopra/pontra sem lehet koppintani, nem látszik az érték | A Lépések grafikon mintájára minden grafikon koppintható: a kiválasztott oszlop/pont kiemelve, felette fix helyű sor (dátum/idő + érték + egység); alapból a legutóbbi. Érintett: Mai állapot lapok 7 napos oszlopai, Testsúlynapló (pontok), Gyakorlatok nagy görbéje, 24 órás pulzusgörbe (érintett időpont + bpm), Kardió (már kész), Fejlődés volumen (ellenőrizni). Teszt grafikononként | kódolandó |
| 2 | Testsúlynapló – időszakok | Minden időszaknál ugyanaz a grafikon | Ok: ha az időszakban <2 mérés van, a teljes listára esett vissza. Javítás: csak az időszak mérései; kevés adatnál kiírja („nincs mérés ebben az időszakban · utolsó: …”) | kódolandó |
| 3 | App szerte – chipek/fülek/szűrők | Fehér, szögletes keret nem illik az apphoz | Közös app-stílusú chip (íves, sötét, kiválasztva arany) minden FilterChip helyett (~20 hely) – mockup | mockup |
| 4 | App szerte – szövegmezők, Új testsúlymérés | Mezők és gombok nem illenek | Közös app-stílusú mező (~55 hely), dátum mező naptárral + Ma/Tegnap/−3/−7 nap, Mégse sötét / Mentés arany – mockup | mockup |
| 5 | Funkció-audit | Minden funkció működik-e | Statikus audit: üres / semmit nem hívó gombok (pl. `= {}` bekötések) listája + javítás; teszt a főbb belépési pontok bekötésére | kódolandó |

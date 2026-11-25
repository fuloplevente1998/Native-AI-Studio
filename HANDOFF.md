# TrainPilot Native – átadó (2812/5)

- Ág: `fix/2812-5-all`; alap: main `f779f5dc`; egy draft PR a main felé.
- Commit-sorrend: A, H, B, C, D, E, G, F (utolsó: verzió).
- Verzió: 1.5.3; telefon 280900; Wear 280902.
- A: statikus lapjelenet, külön optikai háttér, stabil listakulcsok, szűkített cloud állapot.
- H: közös lapmagasság-rögzítés érintés/animáció alatt; Material kezeli a húzást.
- B: glass stílusfelirat kontrasztja, nyolc paletta regressziója.
- C: fókuszfüggő, statikus naplókereső-perem.
- D: kompakt naptár; aktív program szerkesztése, négy nyelven.
- E: főfül-váltás új helyi görgetési kulcs, alroute-visszatérés megőrzi a kulcsot.
- G: Backdrop és Shapes, Apache 2.0, forrás és NOTICE négy nyelven.
- F: csak verzióértékek és ez az átadó.
- CI: a draft PR-on ellenőrizendő; a verziócommit létrehozásakor nincs eredmény.
- Készülékes ellenőrzés: lap ugrálása és akadás, glass/non-glass, kis kijelző és nagy betű.
- Room/DataStore, mentett adatok, applicationId, aláírás változatlan.
- Nincs merge, nincs aláírt build; #372 és #378 nem merge-elhető.

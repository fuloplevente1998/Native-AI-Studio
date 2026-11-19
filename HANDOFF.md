# HANDOFF – aktuális állapot

Frissítve: 2026-11-19 19:05 (magyar idő). Rövid, felülírandó pillanatkép. Előzmény: git log / PR-ek.

## Main – utolsó kiadás 2778 · 1.3.8 (`cabc720`, PR #293)
- Telefon 277800 / Wear 277802; 912/0 teszt; ZIP chatben.
- 2778 (`docs/FIXES_2778.md`): közös pihenő-dokk `StudioRestTimerDock` (Erő/Kétoldali/Időalapú; időalapún
  `showIdle=false`; Kültéri opcionális „Pihenő” gomb), élő sáv rajzolt ❚❚, Egészségnapló részletes rekordok
  alapból becsukva, egysoros sorok; magyarázó szövegek törölve (anatómiai jelmagyarázat marad).
- Nyitott: `PaletteSystemBarsTest` időnként bukik (tárolt téma ≠ VM, RESUMED) – valószínű DataStore-példány
  megosztás tesztek között; tesztizoláció kell (külön tétel).
- Telefon 277700 / Wear 277702; signed run 44566174999; ZIP chatben.
- 2777 óra (`docs/FIXES_2777.md`): kerek kijelzőre igazított listák (18 dp oldalsáv), felirat a kör alatt
  (megerősítés, szerkesztő, összesítő), ■ Befejezés ≠ ✓ Rögzítés, nap betűje (A/B), „ⓘ Részletek”,
  főmenü: Tervezett → naptár (alján ‹ + Programnapok), Kezdőlap sor nincs.
- Képernyő-audit: `WearScreenAuditTest` (192 dp kerek + hosszú nézet) PNG-k a CI teszt-artifactban
  (`wear/build/test-results/wear-screens`). Emulátor nincs a CI-ben; ez helyettesíti.
- 2776 · 1.3.6 (#291): alsó sáv elválasztva, Edzés-csempék szövege, tömör Egészségnapló (mindkét fül),
  `MainActivity.onResume` újraolvassa a tárolt témát (PaletteSystemBarsTest gyökérok).
- 2775 · 1.3.5 (#290): közös aktív fejléc, eszközsor Videó · Coach · Részletek, „legutóbb” sor.
- Play Console: még nincs feltöltve (utolsó Play: 276500/276502); „Újdonságok” `docs/PLAY_LISTING.md`.
- Elvetve: Kontraszt témák fekete háttere szándékos.

## Fontos döntések (ne kérdezd újra)
- Programkártya koppintás = kártyán belüli szerkesztő (1 nyitva), külön szerkesztőoldal megszűnt.
- Lapok: a beágyazott lap fagyását a route-onkénti új példány javítja (#283, ne vond vissza).
- Adatvédelmi tájékoztató marad középső ablak. Privát fotók teljes képe maradhat teljes képernyő.

## CI (mérve)
- PR CI ~4–6 perc; signed APK+AAB ~7–7,5 perc. `[skip ci]` a push utolsó commitján a
  PR CI-t is kihagyja → ilyenkor workflow_dispatch az ágra.

## Nyitott
- Lezárva: #208 (Wear HRV elvetve), #269, #271, #272.

# HANDOFF – aktuális állapot

Frissítve: 2026-11-19 07:35 (magyar idő). Rövid, felülírandó pillanatkép. Előzmény: git log / PR-ek.

## Main – utolsó kiadás 2771 · 1.3.1 (`dd58f64`, PR #286)
- Telefon 277100 / Wear 277102, signed #82, 880/0 teszt; 4 fájl chatben átadva.
- Tartalom (`docs/FIXES_2771.md`): sorrend-húzáskor élő helyadás; Napló naptár lapon
  (6 sor, húzással lapoz); szerkesztőablakok → `TrainPilotFormSheet` (megerősítések
  maradnak középen); Egészség dátumválasztó lap+húzás; statisztika/tanács/Ma-csempe
  részletek görgethető lapon.
- Előző: 2770 · 1.3-beta (`3a52325`, signed #81). Play feltöltés a felhasználónál.
- Következő: új gyűjtő-PR (`docs/FIXES_2772.md`) a felhasználó visszajelzéseire.

## Fontos döntések (ne kérdezd újra)
- Programkártya koppintás = kártyán belüli szerkesztő (1 nyitva), külön szerkesztőoldal megszűnt.
- Lapok: a beágyazott lap fagyását a route-onkénti új példány javítja (#283, ne vond vissza).
- Adatvédelmi tájékoztató marad középső ablak. Privát fotók teljes képe maradhat teljes képernyő.

## CI (mérve)
- PR CI ~4–6 perc; signed APK+AAB ~7–7,5 perc. `[skip ci]` a push utolsó commitján a
  PR CI-t is kihagyja → ilyenkor workflow_dispatch az ágra.

## Nyitott
- Lezárva: #208 (Wear HRV elvetve), #269, #271, #272.

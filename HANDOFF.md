# HANDOFF – aktuális állapot

Frissítve: 2026-11-19 00:50 UTC. Rövid, felülírandó pillanatkép. Előzmény: git log / PR-ek.

## Main – utolsó kiadás 2770 · 1.3-beta (`3a52325`, PR #284)
- Telefon 277000 / Wear 277002, signed #81, 877/0 teszt; 4 fájl (APK+AAB) chatben átadva.
- Tartalom: `docs/FIXES_2770.md` (11 pont: lapok magassága, gyors edzés, ▶+⋮ választó,
  ═ fogantyús sorrend, naptár 6 sor + rögzített tervező, forgatás logó nélkül, kompakt sorok).
- Play feltöltés a felhasználónál; „Újdonságok” szöveg chatben.
- Következő: új gyűjtő-PR (`docs/FIXES_2771.md`) a felhasználó visszajelzéseire.

## Fontos döntések (ne kérdezd újra)
- Programkártya koppintás = kártyán belüli szerkesztő (1 nyitva), külön szerkesztőoldal megszűnt.
- Lapok: a beágyazott lap fagyását a route-onkénti új példány javítja (#283, ne vond vissza).
- Adatvédelmi tájékoztató marad középső ablak. Privát fotók teljes képe maradhat teljes képernyő.

## CI (mérve)
- PR CI ~4–6 perc; signed APK+AAB ~7–7,5 perc. `[skip ci]` a push utolsó commitján a
  PR CI-t is kihagyja → ilyenkor workflow_dispatch az ágra.

## Nyitott
- #269 maradék elfogadási pontjai; #208 Wear HRV; #271/#272 régi draftok.

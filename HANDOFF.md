# HANDOFF – aktuális állapot

Frissítve: 2026-11-19 07:35 (magyar idő). Rövid, felülírandó pillanatkép. Előzmény: git log / PR-ek.

## Main – utolsó kiadás 2772 · 1.3.2 (`993b72c`, PR #287)
- Telefon 277200 / Wear 277202, signed #83 (run 44486428287), 884/0 teszt; 4 fájl chatben.
- Tartalom (`docs/FIXES_2772.md`): gyors edzés = 1 lap 3 füllel (Erő/Időzített/Kültéri), Edzés fül
  3 csempe; közös időszak-naptár (Napló + Egészségnapló); kiemelt Mai nap/Szűrő törlése;
  világos témában világos akciószínek; alsó sáv ‹Előző · ⏹Befejezés · Következő›; Bold-ig betűk.
- Folyamatban: 2773 gyűjtő #288 – Kezdőlap dátum-sor, mai terv éjfélig + napváltás-frissítés
  (mockup küldve, „mehet”-re vár).

## Fontos döntések (ne kérdezd újra)
- Programkártya koppintás = kártyán belüli szerkesztő (1 nyitva), külön szerkesztőoldal megszűnt.
- Lapok: a beágyazott lap fagyását a route-onkénti új példány javítja (#283, ne vond vissza).
- Adatvédelmi tájékoztató marad középső ablak. Privát fotók teljes képe maradhat teljes képernyő.

## CI (mérve)
- PR CI ~4–6 perc; signed APK+AAB ~7–7,5 perc. `[skip ci]` a push utolsó commitján a
  PR CI-t is kihagyja → ilyenkor workflow_dispatch az ágra.

## Nyitott
- Lezárva: #208 (Wear HRV elvetve), #269, #271, #272.

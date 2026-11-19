# HANDOFF – aktuális állapot

Frissítve: 2026-11-19 12:00 (magyar idő). Rövid, felülírandó pillanatkép. Előzmény: git log / PR-ek.

## Main – utolsó kiadás 2774 · 1.3.4 (`4ce1fed`, PR #289)
- Telefon 277400 / Wear 277402, signed run 44510303682, 893/0 teszt; ZIP chatben.
- Tartalom (`docs/FIXES_2774.md`): egy alsó élő sáv (idő/szünet, Befejezés, Folytatás);
  gyakorlatcsere közvetlenül a könyvtárba; Coach chip a napjelvény mellett + ✨ ikon mindenhol;
  gyakorlatfüggő ikon (kind); HC: teljes napi olvasás = szinkron (change-log séma hiba a telefonon).
- 2773 · 1.3.3 (#288): Kezdőlap/Edzés/Coach dátum-sor, mai terv éjfélig.
- Play Console: még nincs feltöltve; leírás `docs/PLAY_LISTING.md`.
- Ismert flake: `PaletteSystemBarsTest` (1× újrafuttatásra zöld) – gyökérokot érdemes javítani.

## Fontos döntések (ne kérdezd újra)
- Programkártya koppintás = kártyán belüli szerkesztő (1 nyitva), külön szerkesztőoldal megszűnt.
- Lapok: a beágyazott lap fagyását a route-onkénti új példány javítja (#283, ne vond vissza).
- Adatvédelmi tájékoztató marad középső ablak. Privát fotók teljes képe maradhat teljes képernyő.

## CI (mérve)
- PR CI ~4–6 perc; signed APK+AAB ~7–7,5 perc. `[skip ci]` a push utolsó commitján a
  PR CI-t is kihagyja → ilyenkor workflow_dispatch az ágra.

## Nyitott
- Lezárva: #208 (Wear HRV elvetve), #269, #271, #272.

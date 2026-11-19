# HANDOFF – aktuális állapot

Frissítve: 2026-11-19 12:45 (magyar idő). Rövid, felülírandó pillanatkép. Előzmény: git log / PR-ek.

## Main – utolsó kiadás 2775 · 1.3.5 (`4bebaa6`, PR #290)
- Telefon 277500 / Wear 277502, signed run 44516841383, 900/0 teszt; ZIP chatben.
- 2775 (`docs/FIXES_2775.md`): Erő/Időalapú/Kétoldali közös fejléc (`StudioActiveWorkoutTop`,
  `StudioActiveExerciseTitleRow`): gyakorlatsor, név ⌄ = csere, eszközsor piros Videó · Coach · Részletek;
  „A nap · x/y sorozat kész” szöveg; szív csak Összesítőn/Kültérin; sorozat: ⋮ törlés, zöld kész,
  „legutóbb” csak mentett előzménnyel; Indítás sárga; Befejezés rajzolt ikon, témakövető.
- 2774 · 1.3.4 (#289): élő alsó sáv idővel, Coach ✦ ikon, gyakorlat-ikon, HC napi olvasás = szinkron.
- Play Console: még nincs feltöltve (utolsó Play: 276500/276502); „Újdonságok” `docs/PLAY_LISTING.md`.
- Elvetve: Kontraszt témák fekete háttere szándékos.
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

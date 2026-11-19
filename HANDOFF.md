# HANDOFF – aktuális állapot

Frissítve: 2026-11-19 00:20 UTC. Rövid, felülírandó pillanatkép. Előzmény: git log / PR-ek.

## Main – utolsó kiadás 2769 · 1.3-beta (`6d24f78`)
- Telefon 276900 / Wear 276902, signed #80, 876/0 teszt; APK+AAB chatben átadva.
- Play Console-ban még 276500/276502 – feltöltés a felhasználónál; „Újdonságok” szöveg
  (HU/EN, ≤500 kar) chatben átadva.

## Folyamatban: 2770 · 1.3-beta – PR #284 (`fix/collector-2770`, head `827a941`)
- Lista `docs/FIXES_2770.md`, 11 pont kész, CI fut: Mentés lap állandó magasság; Privát
  fotók lap; Beállítások→Edzés beállításai = Edzés fül ablaka; Gyors edzés legördülők;
  gyakorlatválasztó „▶ Indítás + ⋮” (B változat); Gyors edzés lap tartalom-magasság;
  sorrend csak ═ fogantyú hosszú nyomás+húzás (▲▼ megszűnt, `TrainPilotReorder.kt`);
  naptár 6 sor + kisebb nyilak + aljára rögzített tervező; forgatáskor nincs logó-animáció
  (`StartupScreen.install(restored)`); Edzés fül kijelölve marad; kompakt szerkesztő-sorok.
- Jóváhagyva: zöld CI → merge → APK+AAB (277000/277002) → 4 fájl + Play szöveg.

## Fontos döntések (ne kérdezd újra)
- Programkártya koppintás = kártyán belüli szerkesztő (1 nyitva), külön szerkesztőoldal megszűnt.
- Lapok: a beágyazott lap fagyását a route-onkénti új példány javítja (#283, ne vond vissza).
- Adatvédelmi tájékoztató marad középső ablak. Privát fotók teljes képe maradhat teljes képernyő.

## CI (mérve)
- PR CI ~4–6 perc; signed APK+AAB ~7–7,5 perc. `[skip ci]` a push utolsó commitján a
  PR CI-t is kihagyja → ilyenkor workflow_dispatch az ágra.

## Nyitott
- #269 maradék elfogadási pontjai; #208 Wear HRV; #271/#272 régi draftok; #279 ez a doksi-PR.

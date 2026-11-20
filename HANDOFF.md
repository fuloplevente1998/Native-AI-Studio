# HANDOFF – aktuális állapot

Frissítve: 2026-11-20 21:45 UTC. Rövid, felülírandó pillanatkép. Előzmény: git log / PR-ek.

## Main – `e73dc51` (2788 merge), build 1.4.1 · 278800/278802 (aláírás fut)
- 2785–2787: koppintható grafikonok, app-stílusú vezérlők, közös Heti célok, Egészség átrendezés.
- 2788 (#306): teljes kalória napi oszlopon (aktív kiemelve, nem összeadva); testsúly szerkesztése
  azonos ID-val (nincs duplikáció); csempe lapja = MA, Heti célok = A HÉT (7 napos előzmény csak ott);
  „nincs edzés”; technikai szavak ki; üres rekordblokk rejtve; teljes csempe-feliratok.

## Folyamatban
- Aláírt APK+AAB 278800 → ellenőrzés → chatben egyenként.
- Audit: `docs/AUDIT_HANDOFF_PROMPT.md` (PR #305) – AI Studio / ChatGPT átnézéshez.
  Nyitott: heti lépés % két kódúton; testsúly `updatedAt` = mérés napja (felhő-összefésülés?).

## Fontos döntések (ne kérdezd újra)
- Csempe-koppintás saját alsó lapot nyit (max ~2/3). Ívelt grafikonok, nincs rövidítés (TrainPilotFitText).
- UI-változás előtt mockup; kódolás „mehet” után, egy CI-körrel.
- Belső azonosító minden buildnél nő, név csak kérésre. Weboldal verziósora = legutolsó build.
- Play-szöveg 4 nyelv, ≤500 kar., 276500/276502 óta minden újítás.

## CI (mérve)
- PR CI ~6–7 perc; signed APK+AAB ~10 perc. Fájlokat egyenként küldjük (ZIP nem tölthető le).

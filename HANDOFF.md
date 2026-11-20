# HANDOFF – aktuális állapot

Frissítve: 2026-11-20 20:30 UTC. Rövid, felülírandó pillanatkép. Előzmény: git log / PR-ek.

## Main – `b76e2f0` (2787 merge), utolsó kiadott build 1.4.1 · 278600/278602
- 2785: koppintható grafikonok + infósor, testsúly csak az időszak mérései, app-stílusú chip/mező (palette).
- 2786: közös Heti célok (Kezdőlap kompakt, Egészség lenyitva), külön Testsúly kártya,
  egy „Források, óra és szinkron” kártya, 1.4.1.
- 2787 (#303): lenyitva a kompakt blokk alakul nagy nézetté, duplikáció nélkül; valós lépés %;
  Edzés fül 3 csempe FitText; Egészségnapló ikon; testsúly fejléc.

## Folyamatban
- PR #304: versionCode 278700/278702 (név 1.4.1) – zöld CI → merge → aláírt APK+AAB → weboldal.
- Audit: `docs/AUDIT_HANDOFF_PROMPT.md` (másik ügynöknek). Gyanú: testsúly szerkesztés ID nélkül ment.

## Fontos döntések (ne kérdezd újra)
- Csempe-koppintás saját alsó lapot nyit (max ~2/3), nem ugrik a naplóba.
- Ívelt grafikonok, nincs rövidítés: TrainPilotFitText. UI-változás előtt mockup.
- Belső azonosító minden buildnél nő; név csak kérésre. Weboldal verziósora = legutolsó build.
- Play-szöveg 4 nyelv, ≤500 kar., 276500/276502 óta minden újítás.

## CI (mérve)
- PR CI ~5–7 perc; signed APK+AAB ~9–10 perc. Fájlokat egyenként küldjük (ZIP nem tölthető le).

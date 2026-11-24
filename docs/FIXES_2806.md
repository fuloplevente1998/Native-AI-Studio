# 2806 – gyűjtő (1.5.0 · 280600 / 280602)

| # | Téma | Hol | Megoldás (javaslat) | Állapot |
|---|------|-----|----------|---------|
| 1 | Google Cloud kártya egyszerűsítés | Beállítások → Google Cloud Szinkron | Lenyitva csak: állapotsor (Drive, Naptár), 2 kapcsoló (automatikus napi szinkron, csak Wi-Fi), 2 gomb („Szinkron most” = Drive + Naptár együtt; „Google-fiók leválasztása”). Megszűnik: Felhő karbantartás, külön Drive/Naptár gomb, összefésülés-jelölő. Leválasztás lap 2 opcionális jelölővel: hozzáférés visszavonása; TrainPilot adatok törlése a Google-fiókból (Drive + naptár). Mockup: scratchpad `m2806/google_simple.png` | kódolva (CI) |
| 1b | Drive-mentések | Drive szinkron | Eszközönként csak az **utolsó 3** mentés marad, csak sikeres új mentés közzététele után törlünk. **A régi WebView-os (legacy) mentések is törölhetők** – felhasználói döntés 2026-11-24: azóta több új, működő TrainPilot-mentés készült | kódolva (CI) |
| 1c | Naptár | Naptár szinkron | Csak a következő **30 nap** tervezett edzései ; a múltbeli TrainPilot-bejegyzéseket a szinkron törli a naptárból, a Naplóban maradnak | kódolva (CI) |
| 2 | #349 része: paletta 12/15 kiemelőszín | Témák | A 12-es (3,42:1) és 15-ös (4,31:1) paletta kiemelő/gombfelirat kontrasztja ≥4,5:1-re, kulcsok változatlanok, regressziós teszt mind a 32 paletta accentjére | kódolva (CI) |

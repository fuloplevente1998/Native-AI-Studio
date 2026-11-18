# HANDOFF – aktuális állapot

Frissítve: 2026-11-18. Rövid, felülírandó pillanatkép (≤40 sor). Előzmény: git log / PR-ek.

## Main
- `a7a03de` (#277 merge). Utolsó kiadás: **2768 · 1.2-beta** (telefon 276800, Wear 276802),
  forrás `5da2b2a` (#275), signed run #76 SUCCESS, 866/0 teszt (nyers XML).
- 2768 APK+AAB a felhasználónak chatben átadva; Drive-ra csak `TrainPilot-2768-ellenorzes.json`
  került (Drive-csatoló ~3 MB/hívás limit). Wear APK méret = 2766 (Wear kód valószínűleg változatlan).
- `docs/CURRENT_STATUS.md` / `docs/WORK_HANDOFF.md` 2765-r2/PR268-nál megállt (elavult).

## CI (mérve)
- PR CI (#276): R8 külön job, app tesztek 2 forkban, Gradle 6g. Régi: ~10 perc (run 346).
- Signed (#277): `signed-pair` ∥ `jvm-tests` → `publish` (csak mindkettő zöldje után
  hozza létre a megszokott artifact-neveket). Régi #76: 16m40s.
- #77 3m08s – build cache miatt (azonos forrás); valós mérés a következő kódváltozásnál.
- Tesztek: 866 eset, tényleges futás ~143 s; a költség a Kotlin-fordítás (~2×3,5 perc).
  Tesztösszevonás/-törlés nem éri meg (≈20–30 s).

## Nyitott
- #278 Programok kártya még kompaktabb/alacsonyabb (2768 visszajelzés); nyilak maradnak;
  „Otthon” alcím konditermi programon kivizsgálandó.
- #269 navigáció + alsó lapok; #208 Wear HRV (funkció). #271/#272 draft doksi/audit PR.
- `public/assets/aistudio/Screenshot_…jpg` = Programok-kártya mockup (megtartva).

## Következő lépés
- #278 megvalósítás külön ágon, draft PR, CI-mérés az új pipeline-on.

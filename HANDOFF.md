# HANDOFF – aktuális állapot

Frissítve: 2026-11-18. Rövid, felülírandó pillanatkép (≤40 sor). Előzmény: git log / PR-ek.

## Main – kiadás 2769 · 1.3-beta (`6d24f78`, #282)
- Telefon 276900, Wear 276902. Signed #80 APK+AAB ~7,5 perc, 876/0 teszt (nyers XML),
  4 fájl hash + tartós signer + páros identitás ellenőrizve; chatben átadva (Drive limit ~3 MB).
- Benne: kártyán belüli programszerkesztő (#280), alulról nyíló lapok (#281), lapfagyás
  javítás (#283), naptár húzás, tartalomhoz igazodó lapmagasság, vékony húzófül,
  kompakt gyakorlat-fejléc. Play Console feltöltés és készülékes elfogadás hátravan.

## CI (mérve)
- PR CI ~4–6 perc (R8 külön job, 2 tesztfork); signed APK+AAB ~7–7,5 perc (régi 16m40s).
- `[skip ci]` a push utolsó commitján a PR CI-t is kihagyja → ilyenkor workflow_dispatch.

## Nyitott
- #269: lapok 1–3. lépés kész; maradék elfogadási pontok (útvonalmátrix, gesztus-regressziók). #208 Wear HRV. #271/#272 draft doksi/audit PR.
- `public/assets/aistudio/Screenshot_…jpg` = Programok-kártya mockup (megtartva).

## Következő lépés
- Készülékes visszajelzés a 2768-r2-re; utána AAB/verzióemelés (2769) csak kérésre.

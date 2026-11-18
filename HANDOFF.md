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
- Mérve kódváltozással: PR CI #351 ~3,5 perc; signed #78 7m29s (régi 16m40s).
- Tesztek: 866 eset, tényleges futás ~143 s; a költség a Kotlin-fordítás (~2×3,5 perc).
  Tesztösszevonás/-törlés nem éri meg (≈20–30 s).

## 2768-r2 (main `6e8a250`, #280, lezárta #278)
- Programok + Kezdőlap: kártyán belüli szerkesztő (1 nyitva), külön szerkesztőoldal UI-ból
  nem érhető el; ⋮ = gyorsműveletek + Átnevezés; mentetlen-draft őr (Kezdőlapon nincs).
- Beépített programok alcíme a katalógusból (profil csak a generált tervre íródik).
- Signed #78 (APK only) 7m29s, 869/0 teszt; telefon APK chatben átadva, verzió nem nőtt.
  Wear forrás változatlan → órás frissítés nem kell. Készülékes elfogadás hátravan.

## 2768-r3 (main `524f865`, #281, #269 1–3. lépés)
- Lapként nyílik (tartalom 1:1): Kapcsolt eszközök, Engedélyek, Mentés, Gyakorlat részletei,
  Napló edzésrészlet; Naptár napja alulról. Adatvédelem marad ablak (felhasználói kérés).
- Signed #79 APK only ~7 perc, 872/0 teszt; telefon APK chatben átadva; Wear változatlan.

## Nyitott
- #269: lapok 1–3. lépés kész; maradék elfogadási pontok (útvonalmátrix, gesztus-regressziók). #208 Wear HRV. #271/#272 draft doksi/audit PR.
- `public/assets/aistudio/Screenshot_…jpg` = Programok-kártya mockup (megtartva).

## Következő lépés
- Készülékes visszajelzés a 2768-r2-re; utána AAB/verzióemelés (2769) csak kérésre.

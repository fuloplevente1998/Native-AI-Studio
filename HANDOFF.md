# Aktuális átadó

- Main: `43036875` (2810, #377 merge). R0–R8 refaktor kész; előbb `docs/CODEMAP.md`, utána csak a megnevezett fájl.
  Méretőr `SourceSizeGuardTest`; áthelyezés `scripts/split_kotlin.py`, `scripts/verify_move.py`.
- **Kyant Liquid Glass (#388, `feat/kyant-glass`):** `docs/KYANT_GLASS_PLAN.md`; Kyant 1.0.6, Kotlin 2.3.10 (globális), telefon Compose BOM 2026.02.00, Wear BOM marad;
  1.5.2 · 280800/280802 verzióemelés ezen az ágon; main beolvasztva, K3 teljesítmény és készülékes elfogadás hátra. #372/#378 soha nem merge; `docs/handoff-2811` (#387) külön.

## `fix/359-367-phone-wear-refinements` (draft PR) – 1. fázis: telefon + alvás
- **#359/2.1 név:** importált külső edzés a meglévő naplószerkesztőben átnevezhető (`journal_editor_title`, ≤80 kar.).
  Tárolás `rawJson.userTitle359`, a `externalWorkout359` forrás-pillanatképen kívül → auto-/újraszinkron nem írja felül;
  forrásazonosító és mérés változatlan. Üres név → alapcímke „Külső edzés”.
- **#359/2.2 forrás:** nincs tárolt generált forrásszöveg (az import soha nem írt jegyzetet/leírást; a forrás csak a listasorban
  és a fejlécben volt *kiszámolva*) → nincs migráció. Most csak a részletek Forrás fülén látszik (`log_detail_external_source`).
- **#359/2.3 auto-import:** kódszinten ellenőrizve, hiba nem igazolt, nincs módosítás. Út: előtéri health refresh
  (`NativeHealthRefresh`) + HC háttér job (Android 14+, `hcBackground`); Samsung csak előtérben. Stabil ID, tombstone,
  átfedés → review (nem duplikál), késői metrika `allowCreate=false` frissítéssel.
- **#359/2.4 Wear nélküli adat:** kódszinten ellenőrizve, nincs módosítás. Telefonos edzés: HC/Samsung ablak-olvasás a napló
  részleteinél (előtér); külső edzés: azonos forrású minták/session-összesítő. Edzésterhelést egyik forrás sem ad, nem számolunk.
  Kockázat (készüléken nézendő): ha az első részletnyitáskor már van pl. lépés, a később érkező pulzust csak a kézi frissítés hozza.
  Samsung több-sessionös pont az adott nap auto-importját visszatartja (szándékos óvatosság).
- **#367/9 alvás:** közös `shared/SleepNightTotal` unió (telefon `NativeDailyHealthDetails.sleepMinutes` is ezt használja).
  A telefon a Today-csempe értékét küldi a home snapshotban (`sleepSummary367`: dátum, perc, provider); az óra a mai értéket
  mutatja forráscímkével, enélkül a saját észlelt szakaszait összegzi (nem csak az utolsót). Más nap értéke soha nem „mai”.

## 2. fázis KÉSZ (CI zöld 70f30f1f, JVM app 977 / wear 268 / shared 3, 0 hiba): Wear UI #367/3.1–3.8 – készülékes ellenőrzés hátra
- **3.1/3.4/3.6:** minden edzésképernyő egy 56 dp elsődleges gombot használ 4 dp-re a pereme alatt (`wearFooterState`: Kész/Mentés → Kihagyás →
  Következő / Befejezés csak rögzített sorozat után); Előző/Következő/korai Befejezés/Elvetés a vezérlő-overlayben. Fix 14 dp pulzussor a gomb felett,
  Legutóbb-sor elsőként tűnik el. Stopper: fix méretű arc (28 sp, 44 dp, rajzolt ▶/Ⅱ/✓), állapotváltáskor nem mozog.
- **3.2 overlay:** minden gomb felirattal (Előző, Szünet/Folytatás/Telefon, Következő, Befejezés = előnézet+mentés, Elvetés = megerősítés, Menü).
- **3.3 pulzus:** élő érték; szünetnél/rövid kiesésnél az utolsó valódi minta halványan ≤120 mp-ig; különben „–” (nincs 0/kitalált). Pihenő képernyőn nincs.
- **3.5 zár:** valós folyamatos 3 mp; ok: ujjmozgás (slop) csendben újraindította a tartást → 16 dp tűrés, visszaszámlálás, „megszakítás nélkül 3 mp”.
- **3.7/3.8** `WearPageSwipe367.kt`: tap bárhol nyit; húzás-kezdés jobbra 24 dp bal sáv (rendszer-vissza), balra csak 6 dp perem (volt 24+24);
  36 dp teljes elmozdulás VAGY ≥16 dp és ≥450 dp/s lökés, vízszintes/függőleges ≥1,5. Menüből nyíló naptár-pager alap Compose pager, nem érintve.
- Teszt `Wear367Phase2Test`; `WearScreenAuditTest` új képek (overlay, plank).
- **Készüléken (este):** gomb/pulzus 176/192/216 mm-en nincs levágva; stopper stabil Start/Pause/Stop; 3 mp tartás old, 2,5 mp nem; szél-tap, balra húzás jobb szélről.
- **Átnézői javítások (utólag):** telefon-alvás csak friss `computedAt`-tal (≤3 ó, `WEAR_PHONE_SLEEP_FRESH_MS`) nyer, különben az óra saját összege;
  `Edzés: %s` mozgássor 4 nyelven (`wearText`); lapozás iránya a slopnál dől el (`wearSwipePageDirection`); `wearMainSwipeCanStart` törölve.

## Hátra
- Készülékes ellenőrzés (fenti + 1. fázis: átnevezés, auto-import, Wear nélküli adat, óra alvás = telefon alvás). Merge/verzió/kiadás csak jóváhagyással.

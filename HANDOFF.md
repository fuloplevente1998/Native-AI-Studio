# Aktuális átadó

- Main: `43036875` (2810, #377 merge). R0–R8 refaktor kész; előbb `docs/CODEMAP.md`, utána csak a megnevezett fájl.
  Méretőr `SourceSizeGuardTest`; áthelyezés `scripts/split_kotlin.py`, `scripts/verify_move.py`.
- Nyitott, külön ágak (ne keverd): `feat/kyant-glass` (draft #388, Kyant Liquid Glass), `docs/handoff-2811` (#387).
  #372 és #378 teszt-APK ágak, soha nem merge-elhetők.

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

## Hátra
- PR CI zöld → készülékes ellenőrzés: átnevezés újraindítás + újraszinkron után, auto-import, Wear app nélküli edzésadat,
  óra alvás = telefon alvás (Samsung + HC). Merge/verzió/kiadás csak felhasználói jóváhagyással.
- **2. fázis: Wear UI #367/3.1–3.8** (gombsor, vezérlők, pulzus, perem, zár 3 mp, plank méret, szélérintés, lapozás) még nincs kész.

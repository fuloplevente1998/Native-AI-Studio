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

## 2. fázis KÉSZ (kód, CI-re vár/zöld): Wear UI #367/3.1–3.8 – készülékes ellenőrzés hátra
- **3.1 gombsor:** minden edzésképernyő (erő, idő/plank, automata, GPS) egyetlen 56 dp elsődleges gombot használ ugyanazon a helyen
  (`wearFooterState`): Kész/Mentés → Kihagyás (pihenő) → Következő (köztes) / Befejezés (utolsó, csak ha van rögzített sorozat).
  Előző/Következő/korai Befejezés/Elvetés a tapintásos vezérlő-overlayben (minden képernyőtípuson ugyanaz). Mentés/befejezés logika változatlan.
- **3.2 overlay:** minden gomb felirattal (Előző, Szünet/Folytatás/Telefon, Következő, Befejezés = összegző előnézet + mentés, Elvetés = megerősítés, Menü).
  Új fordítás: „Elvetés”. Nincs funkcionális duplikáció eltávolítva (a ■/× csak gyorsút a menühöz), megerősítő folyamatok változatlanok.
- **3.3 pulzus:** rögzített sor (`WearPinnedPulse`) a gomb felett minden edzésképernyőn; élő érték; szünetnél/rövid kiesésnél az utolsó valódi minta
  halványan ≤120 mp-ig; különben „–” (nincs 0/kitalált). Outdoor mozgás-sor külön (`wearLiveMovementRow`). Pihenő képernyőn nincs pulzussor (nem volt).
- **3.4 kerek kijelző:** a gomb 4 dp-re a pereme alatt (volt ~14), 4 dp szabad hely a pulzussor és a gomb között, a fejléc-sor fix 16 dp (a 24 dp-s lakat már nem tolja le a törzset);
  Legutóbb-sor elsőként tűnik el, ha nincs hely. **3.6 stopper:** rögzített méretű arc (28 sp, 44 dp, rajzolt ▶/Ⅱ/✓ ikon, tnum), nem mozog állapotváltáskor.
- **3.5 zár:** valós folyamatos 3 mp (HOLD_MS változatlan); okok: ujjmozgás (slop) csendben újraindította a tartást → most 16 dp-s sodródás-tűrés;
  idle poll 100 ms; nyomva tartva visszaszámlálás („2 mp”); szöveg: „megszakítás nélkül 3 mp”.
- **3.7/3.8 lapozás:** `WearPageSwipe367.kt`. Tap bárhol (széleken is) nyit vezérlőt; húzás-kezdés: jobbra (rendszer-vissza irány) 24 dp bal sáv marad,
  balra csak 6 dp perem (eddig mindkét oldalon 24 dp); küszöb 36 dp teljes elmozdulás (slop beleszámít) VAGY ≥16 dp és ≥450 dp/s gyors lökés azonos irányba;
  vízszintes/függőleges ≥1,5. Edzés közbeni főlapozás-tiltás és vissza-védelem változatlan. Naptár hetek-pager (menüből) alap Compose pager, nem érintve.
- Teszt: `Wear367Phase2Test` (állapotgép, pulzus, elrendezés, stopper, zár, küszöbök); `WearScreenAuditTest` új képek (overlay, plank).
- **Készüléken ellenőrizni (este):** egy gombos lábsor 176/192/216 mm-es órán, pulzus látszik plank/erő/outdoor alatt, nincs levágás; stopper stabil
  Start/Pause/Stop; 3 mp tartás oldódik, 2,5 mp nem; szélek: tap és balra húzás a jobb szélről, jobbra húzás bal sávból továbbra a rendszer-vissza.

## Hátra
- PR CI zöld → készülékes ellenőrzés (fenti + 1. fázis: átnevezés, auto-import, Wear nélküli adat, óra alvás = telefon alvás).
  Merge/verzió/kiadás csak felhasználói jóváhagyással.

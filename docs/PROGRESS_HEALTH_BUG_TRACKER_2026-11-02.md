# Natív hibák és javítások – aktuális nyilvántartás, 2026-11-03

Elsődleges állapot: [CURRENT_STATUS.md](CURRENT_STATUS.md). Funkcióparitás és új feladatok: [audit](NATIVE_WEBVIEW_PARITY_2026-11-03.md). Korábbi tracker: [történeti napló](PROGRESS_HEALTH_BUG_TRACKER_2026-11-02_HISTORY.md).

| Jegy | Aktuális állapot | Következő lépés |
|---|---|---|
| #34 | Közös térközjavítás implementálva, lezárva | Konkrét új képernyőhibák külön jegyben |
| #56 | Coach rövid kivonat implementálva, lezárva | Teljes Coach bekötés külön #70 |
| #57 | Piros törlés / listatörlés implementálva, lezárva | Nem újraírandó backend |
| #59 | Belső videó implementálva, készülékes próba miatt nyitott | Lejátszás, hibák, fullscreen, háttérhang |
| #35 | Ismételt szöveg #67-ben kivezetve, nyitott | Friss APK-s Mai állapot ellenőrzés |
| #48 | Új Dialog/mini méret #67-ben, nyitott | Friss APK-val Indítás/Pause/Kör/Befejezés mind látható |
| #60 | Alap órainfó bekötés kész, kompakt átrendezés hiányzik | Név+Sync fejléc, két kisebb blokk egymás mellett |
| #61 | Energy 0–Coach javítva #77-ben, nyitott rész | Samsung téves permission-üzenet |
| #68 / #70 | #77 mainen, CI402; implementációként lezárva | Friss aláírt APK és készülékes próba |
| #69 | RIR/root-feedback szerződés javítva; részleges | Gyakorlatonkénti/utólagos feedback és régi formátumok, #73 |
| #71–#75 | Forrásauditból rögzített backlog | Egészségböngésző, CRUD/kedvencek, naplószerkesztés, preset/auto, teljes lokalizáció |
| BLE | Felhasználó sztornózta | Nem fejlesztendő, adat nem törlendő |

#67 sikeres ellenőrzése: Actions 41589518841, 395 =219 telefon +176 Wear, 0 failure/error/skipped, tényleges PR-head c0a9862. A fizikai elfogadás ettől külön állapot.

#77 igazolása: eae9d6042d48998d9e9e21681443257aa8427897, main81da2f7, Actions41598444479 SUCCESS, letöltött raw XML402 =226 telefon+176Wear, 0 failure/error/skipped. Készülékes elfogadás külön.

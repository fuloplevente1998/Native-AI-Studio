# Natív hibák és javítások – aktuális nyilvántartás, 2026-11-04

## Aktuális hibák – 2026-11-04, 2753-as készülékes visszajelzés

A felhasználó előbb két új hiba rögzítését és a korábbi nyitott jegyek lezárását kérte;
az ezt követő 4409.jpg visszajelzés új #120 feladatként került melléjük.
A 4410.jpg a régi #84 UI-jegy újranyitását indokolja, csak a szerkesztő-dizájnra.
Ebben a körben nincs alkalmazáskód-javítás, tesztfuttatás vagy új APK.

| Aktív jegy | Megfigyelés / elvárt eredmény | Referencia |
|---|---|---|
| [#117](https://github.com/fuloplevente1998/Native-AI-Studio/issues/117) | Naptár szinkron: „A Google-művelet nem sikerült. (404)”. Pontos API/erőforrás és ok kivizsgálandó; a kapcsolt fiók és korábbi Drive-időpont nem naptárszinkron-bizonyíték. | 4407.jpg, felhasználói csatolmány |
| [#118](https://github.com/fuloplevente1998/Native-AI-Studio/issues/118) | Témák kinyitva: a fejléc és mind a négy témaválasztó sor kapjon közös külső keretet az Edzés és értesítések mintájára. A belső panelek lágyabbak, a témák és mentett választás megmaradnak. | 4406.jpg és 4408.jpg, felhasználói csatolmányok |
| [#120](https://github.com/fuloplevente1998/Native-AI-Studio/issues/120) | Edzésrészletek: a WearOS/Samsung/HC források a Forrás fülre, a Statisztikákra egységes pulzus/kcal összesítő. Tükrözött/átfedő mérések csak egyszer, eredet és időablak megőrzésével; eredeti adatok nem törlendők. | 4409.jpg, felhasználói csatolmány |
| [#84](https://github.com/fuloplevente1998/Native-AI-Studio/issues/84) | Újranyitva: naplós edzés- és visszajelzés-szerkesztő kompakt, közös keretes/fényes/színátmenetes app-dizájnja, valódi témahíddal és biztonságos látható Mentéssel. Előzmény #93 PR; más régi UI-részfeladat nem aktivált. | 4410.jpg, felhasználói csatolmány |

**Lezárva a felhasználó kifejezett kérésére:** #35, #59, #69, #71, #75.
A korábban halasztott #82 adatvédelmi tájékoztató `not_planned` státusszal lezárva;
nem jelenti az implementációját. A többi lezárás sem új, tételes paritás-/készülékes
mérés: az eredeti bizonyítékok az egyes jegyekben megmaradnak. Új konkrét tünet
külön jegyben rögzítendő. BLE és #74 korábbi sztornója változatlan.

## Történeti nyilvántartás – a lentebbi „nyitott” állapotok nem aktuálisak


Elsődleges állapot: [CURRENT_STATUS.md](CURRENT_STATUS.md). Funkcióparitás és új feladatok: [audit](NATIVE_WEBVIEW_PARITY_2026-11-03.md). Korábbi tracker: [történeti napló](PROGRESS_HEALTH_BUG_TRACKER_2026-11-02_HISTORY.md).

| Jegy | Aktuális állapot | Következő lépés |
|---|---|---|
| #34 | Közös térközjavítás implementálva, lezárva | Konkrét új képernyőhibák külön jegyben |
| #56 | Coach rövid kivonat implementálva, lezárva | Teljes Coach bekötés külön #70 |
| #57 | Piros törlés / listatörlés implementálva, lezárva | Nem újraírandó backend |
| #59 | #79 javítás CI/APK kész, készülékes elfogadás nyitott | Fekvenyomás/másik videó, fullscreen, bezárás/háttérhang |
| #35 | Ismételt szöveg #67-ben kivezetve, nyitott | Friss APK-s Mai állapot ellenőrzés |
| #48 | Új Dialog/mini méret #67-ben, nyitott | Friss APK-val Indítás/Pause/Kör/Befejezés mind látható |
| #60 | Kompakt kétpaneles órakártya #79-ben implementálva | Közös APK-s elfogadás |
| #61 | Energy0 #77 és téves Samsung permission #79 javítva | Valódi szolgáltatói adatok készülékes ellenőrzése |
| #68 / #70 | #77 mainen, CI402; implementációként lezárva | Friss aláírt APK átadva (41599072569, nyers402); készülékes próba |
| #69 | RIR/root-feedback szerződés javítva; részleges | Gyakorlatonkénti/utólagos feedback és régi formátumok, #73 |
| #72 / #73 | Natív program-/gyakorlat-/kedvenc- és naplószerkesztés #79-ben implementálva | Közös APK-s elfogadás |
| #71 / #74 / #75 | Részletes rekordböngésző #79-ben; teljes maradék nyitott | 26 metrika, preset/auto, teljes lokalizáció |
| BLE | Felhasználó sztornózta | Nem fejlesztendő, adat nem törlendő |

#67 sikeres ellenőrzése: Actions 41589518841, 395 =219 telefon +176 Wear, 0 failure/error/skipped, tényleges PR-head c0a9862. A fizikai elfogadás ettől külön állapot.

#77 igazolása: eae9d6042d48998d9e9e21681443257aa8427897, main81da2f7, Actions41598444479 SUCCESS, letöltött raw XML402 =226 telefon+176Wear, 0 failure/error/skipped. Készülékes elfogadás külön.

#79: `18b828e17e45a2b67ea6e864c04fe72685b46a6c` → main `d69c77e41692f7c2c171e400358772439bff35ea`, CI41670555328 SUCCESS, nyers411, 0 failure/error/skipped + inline JS7. Signed41671892820 és egyetlen telefonos APK átadva; [aktuális bizonyíték/tesztlista](CURRENT_STATUS.md).

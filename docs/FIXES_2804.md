# 2804 – gyűjtő (1.4.3 / 280300 után → 280400 / 280402)

| # | Issue | Hol | Megoldás | Állapot |
|---|-------|-----|----------|---------|
| 1 | #347 | Aktív edzés – pihenő indításkor | Erő- és kétoldalas képernyőn az érintetlen (nem futó, alapértékű) pihenő rejtve (`showIdle = false`) | kódolva (CI) |
| 2 | #347 | Gyakorlatcsere – felső sáv | Ok: a sáv a nyitott lap (gyakorlatkönyvtár) útvonalát nézte. Most a lap alatti képernyőt (`sheetHostState`), így végig a helyén marad; a Következő gomb állapota sem vész el | kódolva (CI) |
| 3 | #347 | Pihenő nagy módja | Kipipálás után kompakt; nagy mód csak a felhasználó saját lefelé görgetésére (NestedScroll UserInput) vagy a nyílra, az automatikus görgetés nem nyitja | kódolva (CI) |
| 4 | #347 | Pihenő feliratok | „Nagy mód” / „Lapos mód” felirat ki; csak nyíl, képernyőolvasós címke: „Pihenő kibontása” / „Pihenő összecsukása” | kódolva (CI) |
| 5 | #348 | „Legutóbb” sor | Csak ha a korábbi súly vagy ismétlés eltér a mezőben láthatótól (2 tizedesig) | kódolva (CI) |
| 6 | #349 | Kontrasztos paletták 9/13/16 | Grafit & Arany, Kobaltkék & Lime, Petrol & Menta a táblázat színeivel; mentett kulcsok változatlanok; állapotszínek (piros, szív, alvás) olvashatóságra világosítva (≥4,5:1) | kódolva (CI) |
| 7 | #349 | Közös kiemelési szabályok (32 téma) | Külön lépés, képernyőnkénti átnézéssel és mockuppal | tervezés |
| 8 | #352 | Google Cloud Szinkron kártya | Mockup: [`2804_352_cloud_sync_card.png`](mockups/2804_352_cloud_sync_card.png) → jóváhagyás után kód | mockup jóváhagyásra vár |
| 9 | #351 | Óra automatikus mérésindítás | Elemzés és javaslat (telefon ↔ óra szerződés érintett) | tervezés |
| 10 | #350 | Saját témák | A keret (#349) után | később |

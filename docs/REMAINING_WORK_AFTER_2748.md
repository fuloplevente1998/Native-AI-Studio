# Hátralévő munka – PR105/PR106 után

A dizájn és a profil/statisztika/kereső/kedvenc javítások mainen. Az AI Studio integráció PR105-ben CI444-gyel ellenőrzött; a profilopciók és a gyors edzés feliratai PR106-ban javítva, CI446 sikeres. [2751 telefonos tesztlista](TEST_2751_PHONE.md).

## Még ellenőrzendő

1. **#75 – Nyelvek.** A profilopciók ismert fordítási rése bezárva; a teljes telefonos felület dinamikus hiba- és adatállapotait HU/EN/DE/RO nyelven készüléken is ellenőrizni kell. Ez nem a mentett felhasználói tartalom átnevezése.
2. **#59 – Videó.** Külön API-ready állapot és automatikus belső böngészős fallback kész; tényleges YouTube-lejátszás és háttérbe kerülés/bezárás telefonos próbája szükséges.
3. **#69 – Régi visszajelzések.** A konkrét támogatott JSON-utak és aliasok az átadó 7. pontjában. Ismeretlen válasz megőrzése tesztelt; további valós régi mentésekkel történő import/szerkesztés visszateszt marad. Nincs általános ígéret minden történeti formátumra.
4. **#82 – Adatvédelem.** Beállításból megnyitható, a közvetlen Samsung SDK/Health Connect/Wear és GPS-útvonaltárolás leírása pontosítva. Publikálás előtt teljes tartalmi/jogi és személyes konfigurációs felülvizsgálat szükséges; jogi dátum vagy kapcsolattartó nincs kitalálva.

## Készülékes elfogadás

Metrikák (#71), per-exercise válaszok (#69), kompakt felület (#84), témák/fontok és Egészség-tömörítés (#35) együtt tesztelendő. Az elfogadott fullscreen GPS-gombelrendezést megőrizzük.

## Sztornó

BLE-kezelő és #74 automata/intervallum további bekötés nem feladat. Meglévő motor/adat nem törlendő.

Aktuális kiadás és bizonyíték: [CURRENT_STATUS.md](CURRENT_STATUS.md).

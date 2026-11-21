# 2788 – gyűjtő (1.4.1 / 278700 után)

Mockup: [`docs/mockups/2788_calories_total.png`](mockups/2788_calories_total.png)

| # | Hol | Visszajelzés | Javaslat | Állapot |
|---|-----|--------------|----------|---------|
| 1 | Mai állapot – Aktív kalória lap, 7 napos oszlop | Csak aktív kalóriát mutat; a teljes napi égetés hasznosabb | Napi oszlop = teljes égetés, benne kiemelve az aktív, alatta a nyugalmi (teljes − aktív, csak ha mindkettő mért); infósor „teljes X kcal · ebből aktív Y”; hiányzó teljes = „nincs adat”, nincs becslés; 7 napos teljes kalória beolvasása (SH/HC) | kódolva (CI) |
| 2 | Testsúlynapló – szerkesztés | (audit) Szerkesztés új UUID-val ment (`saveNewBodyWeight`), a régi rekord megmarad → duplikáció | Szerkesztés az eredeti ID-val (upsert), dátumváltásnál is; teszt: szerkesztés után 1 rekord | kódolva (CI) |
| 3 | Mai állapot csempe ↔ Heti célok | A lépés és az edzés 7 napos grafikonja két helyről ugyanúgy nyílik (csempe lapja és Heti célok) | Szabály: csempe lapja = MA (mai érték, napi cél, mai részletek, Edzés indítása), Heti célok = A HÉT (7 napos grafikon + napok listája). Lépés/Mai edzés lapon a 7 napos grafikon helyett „Heti … és előzmények ›” gomb (bezárja a lapot, lenyitja a Heti célokat). Kalória/Alvás/Pulzus lapon a 7 nap marad. Mockup: [`2788_today_vs_weekly.png`](mockups/2788_today_vs_weekly.png) | kódolva (CI) |
| 4 | Heti célok lenyitva | Két külön kártya + külön lenyíló „napi bontás” = sok koppintás | Egy-egy rész (Edzések, Lépések): összeg/cél, csík, napnevekkel ellátott koppintható oszlopok, napok listája – egy helyen | kódolva (CI) |
| 5 | Edzés napi bontás | Mai napon „nincs adat”; múltbeli napon „nincs mai edzés” | Edzésnél „nincs edzés” (a napló teljes, a 0 valós); múltbeli napon is „nincs edzés” | kódolva (CI) |
| 6 | Szövegek | Technikai szavak: „Room edzésnapló”, „helyi Room napló”, „deduplikált forrás” | Felhasználói szöveg (pl. „Edzésnapló”, „Forrás: Samsung Health”) | kódolva (CI) |
| 7 | Lépés csempe lapja | Üres „Mai rekordok · Nincs tárolt egyedi mérés · Előző / 1. oldal / Következő” | Ha nincs rekord, a blokk (és a lapozó) rejtve | kódolva (CI) |
| 8 | Egészség – Mai állapot csempék | Feliratok levágódnak: „Aktív ka…”, „Napi átl…”, „Samsung …” | Teljes szó, TrainPilotFitText (dinamikus méret), 4 nyelven teszttel | kódolva (CI) |
| 9 | Közös fotógaléria adatbetöltése | A testsúly-bejegyzések Room-olvasása nem-suspend fotóbetöltőből indult | A fotóadat-projekció korutin-kompatibilis; betöltési regresszió hozzáadva | kódolva, GitHub CI ellenőrzés alatt |
| 10 | Egészség – testsúly mentése/szerkesztése | Közvetlen DAO-upsert; restore/sync közben ütközhetett, hiányzó adatbázis mellett a UI megtévesztő sikert jelezhetett | Védett add/edit repository, eredeti ID és ismeretlen portable mezők megőrzése, hibajelzés sikertelen írásnál | kódolva, GitHub CI ellenőrzés alatt |
| 11 | Egészség – testsúly törlése | Dátum vagy ID alapján is törölt; azonos napi külön mérés is elveszhetett | Pontos ID-s törlés tranzakcióval és tombstone-nal, kapcsolt fotófájlok takarításával | kódolva, regresszió hozzáadva, GitHub CI ellenőrzés alatt |
| 12 | Testsúlyfotók számlálója | Csatolás után a Health sor frissült, a közös fotószám elavult maradhatott | Tartós csatolás után a napló és az összes fotótípus közös ViewModel-projekciójának frissítése | kódolva, GitHub CI ellenőrzés alatt |
| 13 | Privát galéria – önálló fotó | Edzéshez és testsúlyhoz nem kötött fotót nem lehetett hozzáadni | Kamera/galéria „Egyéb” út; Room-metaadat, export/import, Drive-szinkron és többeszközös összefésülés; meglévő paletta megtartása | kódolva, regresszió hozzáadva, GitHub CI ellenőrzés alatt |
| 14 | Progress – alapértelmezett nézet | A két soros galériakártya miatt a tanácskártya 24 dp-rel lejjebb került a viewport aljánál | A belépő egysoros, 48 dp-es lett; a meglévő viewport-regresszió marad, nem lazult | javítva, újabb teljes GitHub CI-futás szükséges |

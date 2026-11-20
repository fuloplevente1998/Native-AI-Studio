# 2788 – gyűjtő (1.4.1 / 278700 után)

Mockup: [`docs/mockups/2788_calories_total.png`](mockups/2788_calories_total.png)

| # | Hol | Visszajelzés | Javaslat | Állapot |
|---|-----|--------------|----------|---------|
| 1 | Mai állapot – Aktív kalória lap, 7 napos oszlop | Csak aktív kalóriát mutat; a teljes napi égetés hasznosabb | Napi oszlop = teljes égetés, benne kiemelve az aktív, alatta a nyugalmi (teljes − aktív, csak ha mindkettő mért); infósor „teljes X kcal · ebből aktív Y”; hiányzó teljes = „nincs adat”, nincs becslés; 7 napos teljes kalória beolvasása (SH/HC) | jóváhagyásra vár |
| 2 | Testsúlynapló – szerkesztés | (audit) Szerkesztés új UUID-val ment (`saveNewBodyWeight`), a régi rekord megmarad → duplikáció | Szerkesztés az eredeti ID-val (upsert), dátumváltásnál is; teszt: szerkesztés után 1 rekord | jóváhagyásra vár |
| 3 | Mai állapot csempe ↔ Heti célok | A lépés és az edzés 7 napos grafikonja két helyről ugyanúgy nyílik (csempe lapja és Heti célok) | Szabály: csempe lapja = MA (mai érték, napi cél, mai részletek, Edzés indítása), Heti célok = A HÉT (7 napos grafikon + napok listája). Lépés/Mai edzés lapon a 7 napos grafikon helyett „Heti … és előzmények ›” gomb (bezárja a lapot, lenyitja a Heti célokat). Kalória/Alvás/Pulzus lapon a 7 nap marad. Mockup: [`2788_today_vs_weekly.png`](mockups/2788_today_vs_weekly.png) | jóváhagyásra vár |
| 4 | Heti célok lenyitva | Két külön kártya + külön lenyíló „napi bontás” = sok koppintás | Egy-egy rész (Edzések, Lépések): összeg/cél, csík, napnevekkel ellátott koppintható oszlopok, napok listája – egy helyen | jóváhagyásra vár |
| 5 | Edzés napi bontás | Mai napon „nincs adat”; múltbeli napon „nincs mai edzés” | Edzésnél „nincs edzés” (a napló teljes, a 0 valós); múltbeli napon is „nincs edzés” | jóváhagyásra vár |
| 6 | Szövegek | Technikai szavak: „Room edzésnapló”, „helyi Room napló”, „deduplikált forrás” | Felhasználói szöveg (pl. „Edzésnapló”, „Forrás: Samsung Health”) | jóváhagyásra vár |
| 7 | Lépés csempe lapja | Üres „Mai rekordok · Nincs tárolt egyedi mérés · Előző / 1. oldal / Következő” | Ha nincs rekord, a blokk (és a lapozó) rejtve | jóváhagyásra vár |
| 8 | Egészség – Mai állapot csempék | Feliratok levágódnak: „Aktív ka…”, „Napi átl…”, „Samsung …” | Teljes szó, TrainPilotFitText (dinamikus méret), 4 nyelven teszttel | jóváhagyásra vár |

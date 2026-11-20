# 2788 – gyűjtő (1.4.1 / 278700 után)

Mockup: [`docs/mockups/2788_calories_total.png`](mockups/2788_calories_total.png)

| # | Hol | Visszajelzés | Javaslat | Állapot |
|---|-----|--------------|----------|---------|
| 1 | Mai állapot – Aktív kalória lap, 7 napos oszlop | Csak aktív kalóriát mutat; a teljes napi égetés hasznosabb | Napi oszlop = teljes égetés, benne kiemelve az aktív, alatta a nyugalmi (teljes − aktív, csak ha mindkettő mért); infósor „teljes X kcal · ebből aktív Y”; hiányzó teljes = „nincs adat”, nincs becslés; 7 napos teljes kalória beolvasása (SH/HC) | jóváhagyásra vár |
| 2 | Testsúlynapló – szerkesztés | (audit) Szerkesztés új UUID-val ment (`saveNewBodyWeight`), a régi rekord megmarad → duplikáció | Szerkesztés az eredeti ID-val (upsert), dátumváltásnál is; teszt: szerkesztés után 1 rekord | jóváhagyásra vár |

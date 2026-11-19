# 2778 – gyűjtő

| # | Hol | Visszajelzés | Javaslat | Állapot |
|---|-----|--------------|----------|---------|
| 1 | Élő sáv (alul, minden oldal felett) | A szünet jel narancs emoji-négyzet, nem követi a témát | Rajzolt ❚❚ ikon a téma színében (mint a kezdőlap kártyán) | kódolva |
| 2 | Kétoldali (Oldalsó plank) – pihenő sáv | Kompakt → nagy nem vált; koppintásra sem lesz akkora, mint az Erő oldalon | Közös pihenő-dokk komponens az Erő oldal nagy kártyájával (görgetés/koppintás → nagy, „Lapos mód” → kicsi) – `StudioRestTimerDock` | kódolva |
| 3 | Időalapú (Plank) | Nincs pihenő-sáv az alsó sávban | Igen: ugyanaz a pihenő-dokk + visszaszámlálás (MainActivity bekötve) | kódolva |
| 4 | Egészségnapló – részletes rekordok | Alapból nyitva; a sorok tovább nyithatók nagy blokkokra – nem kell | Alapból becsukva, a „Részletes rekordok” fejléc nyit/csuk (csempe koppintás is kinyitja); a sorok nem nyithatók tovább (nincs nagy kártya) | kódolva |
| 5 | Távolsági/GPS gyakorlat | Pihenő opcionális | „Pihenő” gomb a kültéri oldalon: megmutatja a közös pihenő-dokkot (alapértelmezett pihenő betöltve, szünetelve); „Pihenő elrejtése” | kódolva |

## Bekötés
| Funkció | Hol | Callback |
|---|---|---|
| Pihenő -15/+15, indítás/szünet, kihagyás | Erő, Kétoldali, Időalapú, Kültéri (opcionális) | `adjustRestTimer`, `toggleRestTimerRunning`, `skipRestTimer` (változatlan VM) |
| Kompakt ↔ nagy | mind | dokk saját állapota (Erő: görgetésre is nagy) |

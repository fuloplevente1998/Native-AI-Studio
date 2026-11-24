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

## 6 – Felesleges magyarázó szövegek (jóváhagyva, kódolva)
Elv: cím + rövid alcím marad; hibaüzenet, megerősítés, adatvédelmi hozzájárulás marad.

| Hol | Most | Javaslat |
|---|---|---|
| Kezdőlap – Profil és új terv | cím + „Személyes edzéstervező • stílus • szabály” + ugyanilyen feliratú gomb | cím + alcím „Hipertrófia • RIR/Coach”, gomb helyett › (az egész kártya koppintható) |
| Kezdőlap – Gyors edzés | „Indíts egy rövid edzést most” | csak cím |
| Edzés fül – Gyakorlatkönyvtár | „140 gyakorlat · keresés, kedvencek, új gyakorlat” | „140 gyakorlat” |
| Gyors edzés lap – Kültéri | „Indítás után a megszokott élő GPS-nézet nyílik: …” | törlés |
| Kétoldali edzés | „Koppints egy sorozatra…”, „Indítsd el a stoppereket…” | törlés |
| Edzés összesítő | „Csatolj fotót az edzésről a fejlődés követéséhez (opcionális).” | „Fotó (opcionális)” |
| Edzés részletei (Napló) | „Elöl- és hátulnézeti anatómiai terhelés ezen az edzésen”, „Pulzus és kalória: csak hiteles … mérés esetén” | törlés |
| Statisztika – Lépések | „Teljes napi lépések, egy kiválasztott forrásból. Ha …” | törlés |
| Egészség – mérések | „A kiválasztott adatforrás tárolt mérései (nem élő érték).”; „Az összesítő az átfedést egyszer veszi figyelembe…” | törlés |
| Egészség – napi állapot | „Mai napi mérés még nincs lekérve. A Frissítés ellenőrzi…” | „Mai mérés még nincs lekérve.” |
| Beállítások – szinkron | „Automatikus frissítés appnyitáskor…”, „Health Connect háttérfrissítés: kb. 15 percenként…” | törlés (a hibás állapot üzenete marad) |
| Beállítások – Google | „Jelentkezz be Google-fiókkal az automatikus többeszközös Drive-szinkronhoz (…)” | „Drive-mentés és Naptár szinkron” |
| Edzésmódok | „A pihenőidő, a hang és a rezgés módosítása azonnal mentődik.”; „A változtatás nem ír át korábbi naplót…” | törlés / 1 rövid sor |
| Új program / saját gyakorlat lap | „Adj nevet és leírást…(pl. …):”, „Adj meg saját gyakorlatot…” | törlés (a mezők címkéje elég) |
| Naptár újratervezés | 3 mondatos magyarázat | 1 rövid sor |
| Milyen volt az edzés? | „Az egész edzésről szól; a gyakorlatok válaszait nem módosítja.” | törlés |

Marad: Coach javaslat szövege (tartalom), hibák, törlés-megerősítések, mentési hozzájárulás, „TrainPilot-becslés, nem Samsung-pontszám”.

Megtartva (kérés szerint/indok): anatómiai jelmagyarázat Kisebb · Közepes · Nagyobb; a Statisztika és a Beállítások szinkron-részletek csak kinyitásra látszanak (felhasználó kéri), ezért maradtak.

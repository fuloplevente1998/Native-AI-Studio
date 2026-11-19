# 2774 – gyűjtött javítások

Kódolás csak „mehet” után, egy közös CI-körrel.

| # | Terület | Visszajelzés | Terv | Állapot |
|---|---------|--------------|------|---------|
| 1 | Alsó „Edzés folyamatban” sáv | Ne csak a visszatérés legyen, hanem az eltelt idő és a szünet állapot | Sáv: program·nap, eltelt idő (ugyanaz, mint az edzés oldal tetején), „Szünetel” jelzés; gomb: Vissza ▶ / Folytatás ▶. Minden oldalon marad | mockup küldve |
| 2 | Gyakorlatcsere | A névre koppintva kis legördülő lista + „Választás a 140 gyakorlatkönyvtárból…” | A legördülő megszűnik: névre koppintva egyből a könyvtár-lap nyílik csere módban (erő, időzített, kétoldali). Gyakorlatok közti váltás a felső sávban marad | mockup küldve |
| 3 | Health Connect „Korlátozott szinkron” | Narancs hiba, utolsó sikeres HC szinkron 11.17 | Ok: a telefon Health Connect moduljának változásnapló-táblája hibás (`change_log_request_table` … `row_offset` hiányzik) – rendszer/HC hiba, nem az appé. Az app már most is teljes napi olvasással pótolja. Javítás: ha a napi olvasás hibátlan, számítson sikeres szinkronnak (utolsó sikeres idő frissül), az üzenet semleges tájékoztatás legyen; javaslat: Play Áruház → Health Connect / Google Play rendszerfrissítés | diagnózis kész |
| 4 | Coach edzés közben | Túl rejtett (a lista legalján, a feedback alatt) | Egysoros Coach-sáv közvetlenül a gyakorlat fejléce alatt (javaslat: „5,5 → 6 kg”, vagy szürkén „nincs módosítás”); koppintásra a mostani Coach-panel lapon (áttekintés, Alkalmazás). Bekötés változatlan | mockup küldve |
| 5 | Edzés közben jobb felső ikon | Mindig súlyzó (plank, fekvőtámasz is); koppintásra részletek | Gyakorlatfüggő ikon: időalapú → óra, testsúlyos → alak, gép/csiga → gép, súlyzó/rúd → súlyzó; kis ⓘ jelzés, hogy részleteket nyit. Erő/időzített/kétoldali | mockup küldve |

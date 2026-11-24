# 2779 – gyűjtő

| # | Hol | Visszajelzés | Javaslat | Állapot |
|---|-----|--------------|----------|---------|
| 1 | Edzés fül | Alul görgetésjelző nyíl; ne legyen görgethető | Nyíl ki, oldal fix (nem görgethető); a Gyors edzés sor laposabb, hogy minden kiférjen | kész – nyíl ki, Gyors edzés sor lapos; görgetés csak tartalék kis kijelzőn |
| 2 | Edzés fül – 3 csempe | A nagy ikonok/grafikák nem azonos magasságban kezdődnek | Alcím fix 2 soros helye → ikon/grafika azonos magasságban | kész |
| 3 | Edzés fül – Jelenlegi program | 3 napos programnál csak A és B látszik; 5 napos saját program? | 3 nap egymás mellett; ≥4 napnál vízszintesen lapozható sor (vagy 2×n rács) – mockup | kész – ≤3 nap egymás mellett, ≥4 nap oldalra húzható (pontok), a következő nappal kezd |
| 4 | Edzés fül – napkártyák | Eltérő magasság (csak az A napon van dátum) | Azonos (magasabb) magasság; minden nap kap „Következő” dátumot, ha van tervezett | kész – egyforma magasság, minden tervezett napnak dátum (kitalált dátum nincs) |
| 5 | Coach – cél | „Cél: 25” – mi? | Mértékegység: ism., ism./kar (egykezes), mp (időalapú) | kész |
| 6 | Kezdőlap – Coach kártya | Újratölt, villan, fel-le ugrik | Állandó magasság (helyfoglaló betöltés alatt), ne cserélődjön a tartalom | kész – ok: minden egészség-újratöltés előbb lenullázta az értékeket; most csak forrásváltáskor/hibánál ürül; összegzés min. 3 sor |
| 7 | App szerte | Ne ugráljon a kép fel-le | Betöltés alatt fix méretű helyfoglalók (Kezdőlap, Coach, Egészség) | kész (ugyanaz az ok: Mai állapot csempék is villogtak) |
| 8 | Fejlődés – PR rekordok | Mindig 0 | Hibakeresés (PR számítás/forrás) | kész – a UI egy soha nem töltött listát olvasott; most a mentett edzések PR-jaiból |
| 9 | Fejlődés – grafikon alatti csempék | Max. 4 sor (edzés, volumen, idő, PR); 3 hónapnál 33–35 edzésből csak 4 | Teljes lista alulról nyíló lapon (görgethető) | kész – a lap a teljes listát mutatja |
| 10 | Napló – szűrő törlése | Csak a naptárt kinyitva érhető el; hónapváltás után szűrve marad | „Szűrő törlése” gomb a naptár fölött/mellett is, amikor szűrés aktív | kész – „✕ Szűrő törlése” a lista fejlécében |
| 11 | Pihenő-dokk | Kompakt ↔ nagy váltás | Gyors, szép animáció (méret + áttűnés) | kész – áttűnés + méretanimáció (~0,2 s) |
| 12 | Napló / Statisztikák | Rendbe rakás – ötlet, mockup | Mockup | kész – Erő · Kardió · Lépések; 3 csempe + 14 napos grafikon; napi lista 2 sor + kinyitható |
| 13 | Egészség – Health Connect forrás | HC kiválasztva kevés adat (aktív kcal „—”, regeneráció „—”); Samsungnál megvan | Hibakeresés: HC napi mezők + Coach regeneráció forrás | vizsgálva – a HC napi sor a kiválasztott forrás aggregátuma; ha az a forrás nem ír aktív kcal-t/nyugalmi pulzust, „—”. Eszközadat kell |
| 14 | Coach – végső érték | Samsunggal a Samsung értéket viszi tovább, nélküle az app becslését | Ellenőrizni a végső érték szabályát (forrásválasztás) | vizsgálva – Samsungnál most is a Samsung érték, de a terhelés/fájdalom korlátozza (≤54/≤74). Döntés kell |
| 15 | Programok – Otthoni A/B | Az A napban 10 gyakorlat, nem törölhető (korábban 7) | Ok: a gyors edzés/könyvtár „+” a legutóbb szerkesztett program napjába is beírt, a gyári sort helyben. Javítva: csak a szerkesztőből nyitott könyvtár ír programnapba (másolat-mentéssel); indításkor a gyári Otthoni A/B napról lekerülnek a hozzáfűzött extrák (saját kg/ism. marad) | kész |
| 16 | CI – `PaletteSystemBarsTest` | Időnként bukik | Tesztizoláció | nyitott |

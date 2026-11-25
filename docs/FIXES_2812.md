# 2812 – gyűjtő (1.5.2 · 280800/280802, közös build a1: main `a31bd559`)

Eszközös visszajelzés a közös buildről (Kyant Liquid Glass + #389/#390). Képek érkeznek; kódolás csak „mehet” után, egy közös CI-körrel; UI-változás előtt mockup.

| # | Téma | Hol | Megfigyelés | Állapot |
|---|------|-----|-------------|---------|
| 0 | Beállítások oldal | Beállítások | „Tökéletes” – referencia, nem változtatandó | rendben ✓ |
| 1 | Programok oldal akadozik | Programok | Görgetéskor/animációnál lassú, akadozik (release build). Mérés: képkockaidő; gyanú: sok egyidejű kártya-üveg/árnyék/lencse a hosszú listán | hiba |
| 2 | Coach és Napló: a tartalom nem csúszik a felső navbar alá | Coach, Napló | A tartalom nem a fejléc alá görget, ezért a fejlécen nem látszik a liquid effekt (a Beállításoknál igen) – a Kyant fejléc-capture ezekre az útvonalakra nem terjed ki | hiba |
| 3 | Felnyíló lapokon alig különböznek a rétegek | Lapok (sheet/overlay) | A tartalom, a gombok és a háttér alig megkülönböztethető; hiányzik a rétegzés (felület-szint, perem, kontraszt) | hiba |
| 4 | Hiányzó „drop” effektek a Kyant projekthez képest | Teljes app | A korábbi Kyant tesztben volt csepp-/lencsehatás stb.; most csak üveg + áttetszőség. Pontos elemek a képek/leírás alapján (nyomás-lencse, csúszó nav-lencse, kijelölés) | tisztázandó |
| 5 | Egészségnapló lap teljesen tömör | Egészségnapló (lap) | A lap és a napi kártyák egyszínű szürkéskék, a háttér/jelenet nem látszik; a kártyák alig világosabbak a lapnál, a Samsung Health / Health Connect gombok csak vékony körvonalasak → nincs rétegzés (lap › kártya › gomb) | hiba |
| 6 | Napló: a lista a fejlécnél levágódik | Napló | A tartalom nem csúszik a fejléc alá (az „Összes hónap” sor a fejléc élén vágódik le), a fejléc lapos sötétkék sáv; a Kyant effekt nem látszik (lásd 2.) | hiba |
| 7 | Dialógus/lap: a kijelölt elem alig különbözik | Edzés beállításai és progresszió (dialógus) | Üveg és háttér jó, de a Normál / Dupla progresszió (kijelölt) csak vékony perem-eltéréssel különbözik a többi soroktól; a kapcsolók/sorok/háttér közti kontraszt kevés. A kitöltött „Előnézet” gomb jó referencia | hiba |
| 8 | Programok: tömör A/B-nap gombok a kibontott kártyán | Programok | Kibontott programkártyán az „A nap” tömör világos, a „B nap” tömör sötét gomb – kilóg az üveg-rendszerből | hiba |
| 9 | Megfigyelés: a fejléc alá görgetett tartalom nyers | Programok, Beállítások | A fejléc alatt a tartalom éles szövege (pl. „Alap programok”, „Google Cloud Szinkron”) a nav-ikonok mögött átlátszik, nem elmosott. A Beállításokon a felhasználó szerint „tökéletes” – nem változtatandó; Programokon ugyanez + akadozás (1.) | figyelni |

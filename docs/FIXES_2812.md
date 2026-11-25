# 2812 – gyűjtő (1.5.2 · 280800/280802, közös build a1: main `a31bd559`)

Eszközös visszajelzés a közös buildről (Kyant Liquid Glass + #389/#390). Képek érkeznek; kódolás csak „mehet” után, egy közös CI-körrel; UI-változás előtt mockup.

| # | Téma | Hol | Megfigyelés | Állapot |
|---|------|-----|-------------|---------|
| 0 | Beállítások oldal | Beállítások | „Tökéletes” – referencia, nem változtatandó | rendben ✓ |
| 1 | Programok oldal akadozik | Programok | Görgetéskor/animációnál lassú, akadozik (release build). Mérés: képkockaidő; gyanú: sok egyidejű kártya-üveg/árnyék/lencse a hosszú listán | hiba |
| 2 | Coach és Napló: a tartalom nem csúszik a felső navbar alá | Coach, Napló | A tartalom nem a fejléc alá görget, ezért a fejlécen nem látszik a liquid effekt (a Beállításoknál igen) – a Kyant fejléc-capture ezekre az útvonalakra nem terjed ki | hiba |
| 3 | Felnyíló lapokon alig különböznek a rétegek | Lapok (sheet/overlay) | A tartalom, a gombok és a háttér alig megkülönböztethető; hiányzik a rétegzés (felület-szint, perem, kontraszt) | hiba |
| 4 | Hiányzó „drop” effektek a Kyant projekthez képest | Teljes app | A korábbi Kyant tesztben volt csepp-/lencsehatás stb.; most csak üveg + áttetszőség. Pontos elemek a képek/leírás alapján (nyomás-lencse, csúszó nav-lencse, kijelölés) | tisztázandó |

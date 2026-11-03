# 2748 – kompakt UI, témák, teljes metrikalista, gyakorlat-visszajelzés

A felhasználó engedélyezte az összes mostani módosítás együttes CI/main/APK körét; a korábbi build nélküli korlát erre a kiadásra feloldva.

- A #91 átlagvolumen-alcím eltávolítás, #92 Coach navigáció/alcím, #93 kompakt naplószerkesztő/dropdown/biztonságos Mentés és #94 elkülönülő kontrasztos felületek a mainből megtartva.
- GPS: a felirat 8dp hézaggal távolabb kerül a kapcsolótól. A felhasználó szerint már működő fullscreen gomb- és viewport-elrendezéshez nem nyúlunk.
- Finom, statikus fade/árnyék a fő Egészség-csoportok, Coach következő nap, összvolumen és izomterhelés kártyák szélein; a kezdőlapi meglévő segédet használja. Belső mezők nem kapnak külön erős árnyékot. Továbbra is kompakt térközök, 48dp érintési felületek; sorozatszerkesztőben a checkbox/felirat/gomb között külön hézag.
- A korábbi #90 teljes26 metrikája és gyakorlatonkénti visszajelzése most a közös kiadásba kerül. A gyakorlatonkénti nehézség is dropdown a már frissített naplószerkesztő mintájára, látható kijelöléssel és külön válasz-törléssel. Részletes adat- és mentési védelem: METRICS_EXERCISE_FEEDBACK_NO_BUILD_2026-11-03.md (korábbi ellenőrzési állapot).
- Telefon és Wear versionCode2748; a Wear alkalmazáslogika változatlan. #74 automata/intervallum és BLE sztornó. #82 adatvédelmi szöveg később; #75 teljes lokalizáció külön marad.

Integráció előtt teljes PR CI, utána a pontos main-forrásból állandó aláírású kiadási build és nyers XML/metaadat/APK-hash/aláíró ellenőrzés szükséges. Készüléken külön ellenőrizendő a témák/kijelölések, napló Mentés billentyűzettel, metrikák nap/forrásváltása és gyakorlatválaszok naplóba mentése. A GPS-gombok korábbi készülékes elfogadását a felhasználó közölte; a többi változásra nem állítunk készülékes sikert.

# Átadás — 2817 Wear-only napló

## #425 – Programok, Napló, fotógaléria és fix Liquid Glass sávok (2026-11-27)
- Draft [PR #425](https://github.com/fuloplevente1998/Native-AI-Studio/pull/425), ág `fix/programs-compact-pinned-header`, main-alap, kapcsolódó #419; implementáció és vizuális referenciák commitja `2acf3b43`.
- Programok: redundáns jelenlegi cím nélkül, nagyobb Aktív jelölés, szűkebb térköz, 4 dp alsó margó a meglévő scaffold safe inseten felül; fix További programok és fix alsó sáv.
- Napló: kereső, dátumválasztó, edzésszám fix, nincs Edzés részletei shortcut; az egyedi sor a saját edzését nyitja.
- Fotómegtekintő: 28 dp külső ív, 24 dp tényleges fotómaszk, aránytartó méretezés és rendezett fejléc/metaadat.
- GlassScrollChrome: mért felső/alsó sáv és elkülönített listacapture; a Programok, Napló, Gyakorlatkönyvtár, Egészségnapló és privát fotógaléria fix sávjai mögé csúszik a lista. A helyi ablakra opt-in ág nem mintáz idegen ablakot; teljes optikai kapu, energiatakarékos fallback megőrizve. A sávok magassága a listán belüli kezdő/végső helyfoglalás.
- A felhasználó által elfogadott elrendezések alapján 3 nyilvános, újrarajzolt SVG: `docs/mockups/pr-425-programs.svg`, `pr-425-journal.svg`, `pr-425-photo-viewer.svg`. A privát személyfotó nincs a public repóban; semleges képhelyőrző. Az eredeti PNG-k nem kerültek át.
- Forrásszintű szerkezeti ellenőrzés és két regressziós teszt kiegészítése megtörtént; CI/JVM és fizikai eszközös optikai, görgetési, nagy betűméretes és navigációs ellenőrzés még nincs. Nincs merge, APK, verzióemelés. A commitok a közös későbbi CI-kör szerint skip ci jelölésűek.

## Telefonos felület 1.5.5 után

- Ág `fix/ui-followup-2814`, alap `f625b7e7`. #418 mentett útvonal első nézete és külön nyitható Heti célok. #419 közös Programok-panel és fotólista, szelektív lapmagasság, Mentés és visszaállítás gombok és az adatvédelmi lap bezárógombja. #420 nyolc külön Liquid Glass háttér és Kyant érintési mozgás, peremfény és belső árnyék.
- #421 és #419: a Napló, az Egészségnapló és a Gyakorlatkönyvtár közös listafelületet és sorelválasztókat kapott. Az Egészségnapló összecsukott napjai tömörebbek, a dátumválasztó Liquid módban áttetsző. A listák és a könyvtár fő műveletgombjai érintéskor követik az ujjat és finoman nyúlnak.
- A főág teljes JVM/R8 kapuja indítja az aláírt telepítők készítését. A regresszió ellenőrzi a heti célokat, a lapmagasságot és az összecsukott napot. Fizikai készüléken ellenőrizendő a térkép első nézete, a gesztusok, a dátumválasztó és a görgetés.

## 2815 – Listák és Liquid vezérlők

- `fix/2815-list-glass` a `751aaab5` főági verzióról. 1.5.5, telefon 281500, Wear 281502.
- #419: közös Programok-panel és elválasztók a Naplóban, az edzés gyakorlatlistájában, az Egészségnaplóban, a Gyakorlatkönyvtárban, a függőleges fotólistában és a fejlődési rekordlistában. A két naplórészlet rögzített magasságú görgethető ablakot kapott. Az Edzés beállításai párbeszédablak magasabbra nőhet.
- #420: a Naptár Újratervezés gombjának rétegét és a könnyű rajzolást használó gombok ujjkövető mozgását vezettük át az érintett közös vezérlőkbe és az Edzés csempéire.
- A PR kapuján a teljes JVM és az R8 fut. Készüléken ellenőrizendő a lenyitás, a peremfény, a nyúlás és a görgetés.

## #419 – Fix fejlécek és műveletsávok

- `fix/pinned-list-controls`, alap `0fa5c617`. A Programok aktív kártyája, a fülek és az alsó műveletek fix helyet kaptak. A szerkesztés a hárompontos menüből érhető el. A Gyakorlatkönyvtár szűrői és az összeállítás sávja a görgethető listán kívül vannak.
- Az Egészségnapló dátumszűrője rögzített, a lista hét napos lekérdezésekkel töltődik visszafelé. A galéria alsó sávja a két fotóhozzáadási műveletet tartalmazza.
- Készüléken ellenőrizendő a négy lap görgetése és az Edzés indítása sáv megjelenése.

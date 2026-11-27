# Átadás — 2817 Wear-only napló
- Ág: fix/2817-wear-only, alap e53a35a6 (281300/281302).
- A: TrainPilot edzés részletei kizárólag érvényes mentett Wear pulzus/energia; Forrás csak Wear; külső edzés saját importmérése változatlan. HC/Samsung bizonyíték megőrizve, csak a megjelenítésből kizárva. A részletek megnyitása nem indít provider-olvasást.
- B: négy nyelvű óratelepítési tipp, Play-link/web fallback, Később edzésenként külön UI-preferenciában; nincs adatbázis- vagy health240Json-írás.
- C: napi HC elsődlegesség és Samsung kiegészítő kapcsoló változatlan, külön regresszió.
- D: #414-ből csak helyi idő és statikus üvegfesték, a HC-elsőbbség nem került át. A megszüntetett HC/Samsung ablaklista nem igényel időformázást.
- Az eredeti Wear mérés látható akkor is, ha a közvetlen kapcsolatot később kikapcsolták; nincs új mérésindítás.
- Felhasználói pontosítás: a TrainPilot-edzés Statisztikák kártyáján csak összes Wear kcal látható, az aktív kcal mentve marad, nem helyettesíti a hiányzó összes kcal-t; napi és importált adatok változatlanok.
- CI és fizikai eszközös ellenőrzés szükséges. Nincs merge, signing, verzióemelés. Helyi Git DNS-hiba.

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

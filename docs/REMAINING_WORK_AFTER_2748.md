# Mi maradt a 2748-as csomag után?

## Kifejezetten hátralévő fejlesztések

1. **#95 – Aktív edzés sorozatsora.** Egységes lekerekített négyszög: sorszám, OK, törlés. A tényleges gyakorlat loadType/repUnit metaadata szerinti kg/kar vagy kg és ismétlés feliratok, bolgár guggolásnál /láb nélkül; igazodó fejléc, hézagok, látható kijelölés. A felhasználó ezt a következő körre kérte, ezért nincs a 2748-as APK-ban.
2. **#59 – Beágyazott videó.** A legfrissebb készülékes visszajelzés szerint nem működik, de az appon belüli böngészőben elindul. Az ágyazás valódi hibáját javítani kell, vagy a működő appon belüli lejátszási utat megbízhatóan elérhetővé tenni, félrevezető kész állapot nélkül. Ez nem pusztán visszateszt.
3. **#75 – Teljes HU/EN/DE/RO lokalizáció.** A még beégetett és részben angolra visszaeső feliratok kivezetése. A részlegesen meglévő fordítás nem teljes felületi paritás.
4. **#82 – Adatvédelmi tájékoztató.** Később, a régi mainben ténylegesen használt szöveget megkeresve a Beállítások / Adatvédelem részbe; nem kitalált jogi szöveg.
5. **#69 maradéka – Régi visszajelzés-formátumok teljes kompatibilitási auditja.** A jelenlegi gyakorlatonkénti válasz/jegyzet és a kanonikus gépi értékek bekötése kész. Az ismeretlen régi/fordított effort értékeket és a korábbi, eredetében nem bizonyítható RIR2 értékeket nem írjuk át találgatással. Az audit és szükség esetén bizonyított, veszteségmentes normalizálás külön feladat.

## Kód helyett készülékes elfogadás maradt

- A teljes26 metrikás felület és rekordböngésző (#71), gyakorlatonkénti válaszok és naplószerkesztés (#69), UI tömörítés (#84), témák/fade, valamint a Mentés és legördülők a közös telefonos próbában ellenőrizendők. Nem kell ezeket a funkciókat újra megírni.
- #35 Egészség: az ismétlődő magyarázatok és helypazarlás készülékes visszatesztje.
- #48 GPS: a felhasználó a gombelrendezést már jónak jelezte; a 2748-ban csak a GPS-felirat melletti hézag változik. A korábbi képernyőn kívül lógó gombok javítását nem tekintjük új nyitott fejlesztésnek.

## Nem hátralévő feladat

**BLE-kezelő és #74 automata/intervallum további bekötés: sztornó.** A meglévő működő motort és tárolt adatokat megőrizzük, a sztornó nem törlési utasítás.

Aktuális APK és ellenőrzési bizonyíték: [CURRENT_STATUS.md](CURRENT_STATUS.md). Részletes telefonos próba: [TEST_2748_PHONE.md](TEST_2748_PHONE.md).

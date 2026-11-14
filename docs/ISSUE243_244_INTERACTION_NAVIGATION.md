# Teljes gombfelületű visszajelzés és visszatérés a nyitó nézetre

A #243 nyomott gomb visszajelzést és a #244 visszanavigálást a korábbi #241 visszavonással együtt a draft PR242 gyűjti. Az elfogadott geometria, paletta és natív edzésvezérlés megmarad; új APK/AAB vagy verzióemelés nincs kérve.

## #243

A közös kitöltött, körvonalas, mentés-, negatív és szöveges gombok teljes, ténylegesen festett háttere animálódik. A Material gomb mérete, sarka, érintési tartománya és natív kattintáskezelése megmarad. Az egy képkockán belüli Press/Release is legalább egy rövid teljes felületű animációt indít; nem csak a képkockánként összevont pressed boolean alapján rajzol. A növekvő ripple ezeken lokálisan kikapcsolt, az új állapotjelzés fókuszt és hover-t is jelez. A képernyők bekötésének többsége csak import alias; nem új gombelrendezés. A Progress hosszú edzéssorai és a Beállítások navigációs sorai ugyanilyen alakhoz vágott jelzést kapnak.

Valódi közös gombokon Canvas-képpontos teszt: széles gomb mindkét vége, alapértelmezett 40 dp festett/48 dp érintési méret, 56/72 dp magasság, lekerekített sarkok, felengedés/megszakítás, tiltás, billentyűfókusz és Enter. A valódi Progress-edzéssor nyomását és a megszakítás utáni navigációhiányt a navigációs UI-teszt is méri.

## #244

A naplórészlet megnyitása a Napló alfülre állította a globális kijelölést, miközben a visszatérési route csak a főlapot és aloldalt tartalmazta. A route most a böngészési fókuszt is rögzíti: alfül, edzés-/gyakorlat-részlet, fotó szűrése, szerkesztett program/nap. A visszaállítás nem tárol és nem ír vissza motort, mérést, edzésadatot vagy programtartalmat. Aktív mérési képernyőre visszatérve a kanonikus gyakorlatkijelölés is megmarad. A meglévő elhagyási/pause/ownership kapuk változatlanok.

A visszatérő böngészőnézetek SaveableStateHolder révén megőrzik a görgetésüket. A már kipopolt nézet lokális draftja törlődik a cache-ből; élő mérés/összegzés draftjai nem kerülnek ebbe a cache-be. Az X és a rendszer-vissza ugyanazt a navigációt használja. Az ismételt, azonos részlet- vagy aloldalnyitás nem gyárt dupla visszalépési lépést.

Regresszió: Fejlődés/Napló/Statisztikák → részlet; egymásra nyitott edzések, fotó és térkép; gyakorlatválasztó/részlet; Beállítások alnézetei. A valódi Progress és naplórészlet X-ét és rendszer-visszáját, az eredeti görgetési pozíciót és az elvetett lokális draft tisztulását Compose UI-teszt fedi.

Teljes közös CI a kész kóddal futtatandó. A #241 korábbi 696 tesztje a korábbi forrás bizonyítéka; a mostani változásokra új közös eredmény szükséges.

A 43633039679 előzetes kört a végső gyors-koppintási regresszió hozzáadása miatt leállítottuk; nem validációs bizonyíték. A kész végleges kóddal egy közös teljes futás indul.

A 43633617617 körben az alkalmazáskód lefordult, a tesztfordítás két API/import hiányon állt meg (ExperimentalTestApi és toArgb). Ezek javítva; a görgetés-összehasonlítás normalizált szemantikai értékre szigorítva. Ez a futás nem kész tesztbizonyíték.

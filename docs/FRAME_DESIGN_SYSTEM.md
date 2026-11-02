# Telefonos kerethierarchia és forrás-harmonika

A felhasználó 2026-11-02-i natív és régi appból küldött referenciái alapján.

- Külső oldal / egyetlen összetartozó csoport: 1 dp, finom grafit/arany átmenettel.
- Belső panel: legfeljebb 0,75 dp, halványabb palettaszínnel.
- Körvonalas vezérlő: 0,5 dp, visszafogott vonallal. Kijelöléskor a töltés és a szín is jelöl.
- Explicit hibajelző/piros keretek megőrzik jelentésüket és láthatóságukat.

A közös oldalhost fogja körül a navigáció alatti tartalmat, a saját görgetőkonténerek
megtartásával. A Studio és natív telefonos képernyők meglévő palettás keretei közös
szélesség-/színerősség-kezelést használnak; az OutlinedButton közös adaptere megtartja
a Material kattintási, tiltási, hozzáférhetőségi és tartalomviselkedést. Nincs új háttérmotor.
Forrás: `TrainPilotFrames.kt`, `TrainPilotOutlinedButton.kt`.

A három naplózott egészségforrás egy közös keretet kap. A három gomb vékonyabb;
a kinyitott tartalom nem kap külön erős kártyakeretet. Egyetlen, edzésenként mentett
`expandedSource` van: másik forrás nyitása bezárja az előzőt, újabb koppintás bezárja
a kiválasztottat. A források mentett adatai és engedély-/frissítési műveletei elkülönülnek.

A régi JS/CSS-ben több helyen 1 CSS px-es keret szerepel. Ez nem garantál azonos
fizikai méretet minden WebView zoom/density mellett; az új rendszer az arányokat
és a felhasználó által kért vizuális hierarchiát rögzíti Android dp-ben.

Elfogadás: egyetlen nyitott forrás, megőrzött provider-adatok és state restoration;
Kezdőlap, Programok, Edzés, Fejlődés, Egészség, Naptár, Coach, Beállítások és részleteik
egységes keretezése. Szűk képernyőn és nagyobb betűmérettel se sérüljön a használhatóság.
Fizikai kinézet és készülékes elfogadás a kiadott APK tesztjén igazolható.

## Fejlődési volumenábra

A régi app képe szerinti kiosztás: 1 nap egy napi oszlop, 7 nap hét napi oszlop,
30 nap öt, 3 hó tizenhárom hétnyi csoport. A legutolsó csoport lehet rövidebb; minden
oszlop csak a kiválasztott intervallum adatait összegzi. A teljes összeg és az oszlopösszeg
egyezik. A kompakt grafikonon az egyetlen oszlop nem tölti ki a teljes rajzterületet,
az oszlopok mögött finom rácsvonalak és olvasható skála jelenik meg.

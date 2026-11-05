# Közös telefonos elfogadás – következő javított APK

Ez a lista a következő, az új mainből fordított telefonos APK-hoz tartozik.
A korábban átadott 2753 nem tartalmazza ezeket a javításokat. CI nem helyettesíti
a valós Google/Samsung/Health Connect/Wear és vizuális készülékes próbát.

1. Google-naptár: valós fiókkal szinkron, majd ismétlés. Nincs dupla esemény vagy
   helyi adatvesztés. Hiba esetén a művelettípus látszik, nincs sikerállítás.
2. Témák: nyitva a fejléc és mind a négy kategória egy külső keretben; összecsukás
   és palettaválasztás működik. Matt, kontrasztos, glow és világos téma is olvasható.
3. Naplózott edzés: a Statisztikákon egy pulzus/energia összesítő, a Forrás fülön
   három szolgáltatói panel. Tükrözött adat nem növeli a számot; részleges/hiányzó
   mérés jelzett, eredeti forrás/időablak elérhető. Aktív és teljes kcal nem összegződik.
4. Naplószerkesztő: kompakt boxok és mezőpárok; nagy betűvel és billentyűzettel is
   elérhető Mentés. Dátum, megjegyzés, sorozat, nullás/hiányzó RIR és alkalmazott
   visszajelzés jelentése megmarad mentés és újranyitás után.
5. Egészségnapló: egyetlen belépő; a napok lenyílnak, 26 metrika és a részletes
   rekordok/alvásfázisok ugyanott jelennek meg, külön részletoldal nélkül.
   Szolgáltató/eredet/nap váltáskor nincs idegen adat vagy tükrözött dupla napi összeg.
6. Napló lapozása: több napi és 25-nél több mérési rekordnál előre/hátra stabil;
   összecsukás/kinyitás őrzi a részletoldalt, visszaállítás után nem marad régi cache.
7. Dátumválasztó: app-arculat, hónap/év váltás, nagy betű; Mégse nem módosít,
   kezdőnap nem lehet zárónap után. Nincs visszatérő rendszer-DatePicker ebben az útban.
8. Aktív edzés: az Átadás órának kis óraikont kapott. Az eddigi ACK/ownership és
   tiltott/folyamatban állapotok megmaradnak; ikon nem indít másik műveletet.
9. Vissza/Előző és Elvetés felismerhető kerete/színe, használható érintési cél.
   Megerősítés Mégse ága nem vet el; órás tulajdon/busy tiltás és piros X változatlan.
10. Coach: valódi megfelelő előzménynél magától megjelenő kanonikus tanács/indok,
    hiánynál és hibánál eltérő érthető állapot. Naplószerkesztés után frissül;
    nincs néma súlymódosítás vagy fájdalom mellett erőltetett emelés.
11. Stopper: első Indítás után nő az idő; dupla Indítás nem rögzít. Szünet és
    nulla idő nem teljesítés. Kifejezett pozitív rögzítés megmarad, duplán nem könyvel.
    Kétoldalas sorozat csak mindkét pozitív oldal után kész; kézi +/- nem mért munkaidő.
12. Gyors plank → súlyzós gyakorlat → plank csere közvetlenül és könyvtárból:
    megfelelő felület, ugyanaz az edzés, korábbi mért adatok megőrizve. Régi
    callback/restore utáni választás nem ír új sessionre. Elvetés után nincs
    visszanyíló régi stopper; késői második elvetés nem töröl új edzést.
13. Naptár: Tervezés mindig látható; napra koppintás továbbra is a meglévő
    adott napi lebegő ablakot és műveleteit nyitja.

# Készülékes javítások: #43–#45, #47–#48

A 2742-es készülékes visszajelzések alapján; az alkalmazásazonosító és az állandó signer változatlan, a következő build 2743 /0.1.0-beta.

- #43: a Studio mentett edzés részletében megerősített Edzés törlése. A meglévő JournalViewModel út közös NativeHistoryDeletion művelettel törli a pontos Room-sort, relációit és kizárólagos privát fájljait. A tombstone és egy csak azonosítókat tartalmazó, tranzakcióban mentett KV-takarítási nyugta védi a tartós törlést; a fájltakarítás hibája újrapróbálható és következő naplónyitáskor helyreállítható. Megosztott/aktív hivatkozások megmaradnak. Dataset-continuation és újraolvasott cél védi a régi megerősítést. A Studio statisztikák a natív újratöltést használják.
- #44: naptárrács, napcellák, műveletek, tervező és dialógusok rögzített sötét felületei a közös palettát követik.
- #45: térkép alapból az app témáját követi; kézi világos/sötét választás és visszatérés az App témája módhoz. A korábban ténylegesen elmentett Boolean-választás megmarad, a hiányzó régi kulcs automatikus mód.
- #47: Home órakártya / Kapcsolt eszközök megnyitása és órakártya/Frissítés valódi, csak olvasó kérés/választ indít. Node- és requestId-egyezés után jelenik meg akkumulátor/eszköz/verzió; timeout és ismeretlen akkumulátor külön látszik. Nincs edzés-, ownership- vagy Health-parancs. Mindkét új APK szükséges a fizikai próbához.
- #48: korlátozott kompakt térképmagasság; teljes képernyőn a fejléc, idő, X, adatok és primer vezérlők a térképen belüli overlayek, safeDrawing rendszerinsetekkel. A Google térképmargója helyet foglal a fejlécnek és alsó overlaynek; a kicsi térképen gesztusos zoom marad.

Regressziók: valódi Room/privátfájl-törlés, idegen hivatkozás, változott cél és megszakadt takarítás helyreállítása; Studio megerősítés/Mégse/célváltás; naptár tényleges világos/sötét rajzolása; órakártya callback/busy, kérésazonosító és ismeretlen akkumulátor; kompakt magasság és teljes képernyős gombok térképen belüli láthatósága. Actions és nyers XML eredménye a futás után rögzítendő. A fizikai megjelenés, valódi telefon–óra válasz és fájltörlés készülékes elfogadása külön marad.

A képi referenciák az eredeti beszélgetés 01:51-es világos naptár és 02:05-ös GPS-képei. A repo nem tartalmazza a képfájlokat.

## A munka közben kért további javítások

- #50: Google Cloud szakasz alapból csukva, mentett nyitottsággal a Studio és natív Beállításokban. Folyamatállapot/hiba és a külön hozzájárulási/megerősítési felületek becsukva is működnek.
- #51: Home és Egészség ugyanazt a hatos Mai állapot rácsot használja; Home-on kompakt méret, rövid Coach-kártya a Coach oldalra navigálva, megőrzött görgetési fallback kis ablak/nagy betű esetén. A Home hős kártya aktív edzésnél a munkamenetet mutatja és nyitja, nem a közben kijelölt programot.
- #52: új GPS-session új natív azonosítót és indulási időt kap, nincs lezárt execution ID újrahasználat. A natív előkészítés szinkron eredménye alapján kerül aktívnak a UI-ba; sikertelen előkészítés nem hoz létre látszólagos sessiont. A programId kültéri sessionnél független a kijelölt tervtől. Natív állapot újratöltésekor a pillanatkép Mainen friss, lezárt állapot nem marad aktív egy korábbi UI-copy miatt. Stale elvetés nem töröl másik edzést; a pontosan egyező, már lezárt session UI-ja mellékhatás nélkül lezárható.

- #53: importáláskor a heti cél nem a teljes napló méretéből jön. Közös helyi napi/naptáriheti dátumszűrés; mentéskor is újraszámolás. A Mai edzések részlet a valódi mai darabszámot/edzést mutatja és ezt a konkrét rekordot nyitja. Az importált előzmények dátumai/adattartalma változatlan.
- #54: a volumen fejlécében az előző, azonos hosszúságú, közvetlenül megelőző időszakhoz mért százalék jelenik meg a régi app képletével, iránnyal és színnel. Nincs pozitív előző volumen: „—”; nulla változás: 0%, aktuális nulla/előző pozitív: −100%. Mind a négy időszak és az adatváltozás regresszióval lefedve.

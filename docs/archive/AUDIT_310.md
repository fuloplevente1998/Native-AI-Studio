# #310 – készülékes hibajegyek és a 2789 audit folytatása

Az alap az `aed077b` main, amely tartalmazza a #307 súlynapló/fotó változásait és a #308 aláírt buildkapuját. A telefonos kiadási kód 278900, a Wear OS kód 278902, a verziónév 1.4.1. Az alábbiak a javítási ágra vonatkoznak. [Hibajegy #310](https://github.com/fuloplevente1998/Native-AI-Studio/issues/310)

| Pont | Változás és ellenőrzés |
| --- | --- |
| 1, 3, 4 | Az Egészség hat csempéje közös, fordított X bezárót kapott. A szokásos nézetben olvasható forrásnév látszik. A kalóriaösszegzés kiemeli a kiválasztott dátumot, és külön jeleníti meg az aktív és teljes energiát. |
| 2 | A koppintható telefonos grafikonokon a kiválasztott értéket a téma kiemelőszíne és egy címke vagy akadálymentesítési állapot jelzi. Az izomterhelés saját jelmagyarázata követhető marad. |
| 5 | Külön jelzés készül az engedélykérés indítási hibájára, hiányos eredményére és be nem fejezett jóváhagyására. A felhőművelet előtt ellenőrizzük a szükséges hozzáférést. A Playen tapasztalt eltérés oka készüléken vagy a Play/OAuth beállításokban igazolható. |
| 6, 10 | A helyi JSON/ZIP és a Drive átviszi a célértékeket, a naptári sorokat és a mentett tervezési paramétereket. A megjelenítési nézet a visszaállított rekordokból épül. A régi részleges mentések és a lemondások külön regressziót kaptak. |
| 7 | A mentési képernyő és a fő Google Cloud kártya megnevezi a Drive egészségadatkörét. A mért napi lépések helyi JSON/ZIP mentésbe kerülhetnek engedéllyel. A külső forrásból történő visszaolvasást fizikai telefonon kell ellenőrizni. |
| 8, 9 | A Drive belépés, szinkron és helyi import megnevezése elkülönül. A panel jelzi az akadályt, a folyamatot, az eredményt és a megszakítást. A Google-engedélykérés az alkalmazás gyökerében marad regisztrálva tabváltás és görgetés közben. |
| 11–14 | A célkártya alapból csukott, a címe követi a periódust. A súlynapló rövid grafikont kap a legutóbbi mérésekből. Az Egészségnapló és a Források, óra és szinkron saját görgethető panelen nyílik, a főoldal a helyén marad. |
| 15–18 | A Gyors edzés mindhárom megnyitásnál a rendelkezésre álló magasságot használja. A napló gyakorlatai csukva indulnak és kompaktabbak. A telefonos panelek egységes, finom témaszínű körvonalat kapnak. |

A #307 audit folytatásában ellenőrzött út az Egészség → Testsúlynapló → mentett mérés → kamera vagy fotóválasztó. A mérés azonosítója dátumszerkesztéskor megmarad. Visszaállítás miatt elutasított csatolás után a frissen importált fájl törlődik. Ha több bejegyzés ugyanarra a fájlra mutat, a pontos rekord törlése után a többi kép megmarad. A galéria a kiválasztott rekord dátumát és adatait mutatja. A mentési formátumok részletei a [mentési adatleltárban](BACKUP_DATA_SCOPE_310.md) vannak.

## Készüléken ellenőrizendő

- Play és GitHub APK azonos verzióval és külön tesztadattal. Play által terített tanúsítvány SHA-1, Android OAuth-regisztráció és a Play-folyamat tényleges eredménykódja. A #309 Health Connect draft külön ellenőrzést kap.
- Helyi JSON/ZIP és Drive siker, hiba, megszakítás. Egyező célértékek, naptári sorok, fotók és forrással/dátummal ellenőrzött szolgáltatói lépések a művelet után.
- Az új panelek és grafikonok kis képernyőn, nagy betűmérettel, HU/EN/DE/RO nyelven, világos és sötét témában. A panelszegély és az X fizikai ellenőrzése kamerakivágás mellett.

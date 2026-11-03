# Közös telefonos tesztcsomag – 2026-11-03

A felhasználó kérése: a beleférő javítások készüljenek el együtt, majd egy APK-t tesztel. A már bevált natív programátrendezést és súly/ismétlés/pihenő szerkesztést kell követni. BLE-kezelő sztornó; nem készül új motor vagy DB.

## A csomag tartalma

- #59: a készüléken jelzett fekete YouTube-videó javítása. Látható HTML Lejátszás gomb és előnézet, appazonos origin/referrer, iframe-cookie támogatás és minimumméret, elakadt/blokkolt indítás/hiba nem lesz hamis ready állapot. A tényleges script 7 helyi eseménytesztje sikeres. [Részletek](YOUTUBE_BLACK_PLAYER_2026-11-03.md).
- #60: kompakt órakártya, név és frissítés felül, két kisebb Eszköz/Rendszer panel normál méretnél egymás mellett; keskeny vagy nagy betűs nézetben tördel. Ugyanaz a hitelesített óraadat-kérés és a hiány/hiba állapot marad.
- #61: a Samsung közvetlen híd által nem támogatott nyugalmi pulzus nem engedélyhiány. A tényleges megtagadott pulzusengedély továbbra is hiány, a hibás/cache adat nem mérés. A korábbi Energy 0 javítás megmarad.
- #72: program másolása, valódi törlése, edzésnap hozzáadása/törlése, gyakorlat eltávolítása a napból és névmentés. A meglévő NativeProgramEditor végez atomikus, snapshot/restore/aktív-edzés és hivatkozás-védett műveletet. Beépített tervből saját másolat készül; az eredeti nem törlődik. Naplóban/naptárban használt program vagy nap nem törölhető. Aktív program törlése előtt másik tervet kell választani. Nincs automatikus edzéstörténet-törlés.
- #72: a meglévő sajátgyakorlat-szerkesztő és valódi törlés elérhető a Studio részletből. A kanonikus validálás, nyelvi/nyers metaadatok és hivatkozásvédelem megmarad. Kedvenc jelölése/levétele valós Room-dokumentumot ment, a könyvtár szűrhető a kedvencekre. Importált ismeretlen kedvencazonosítók nem vesznek el.
- #73: a naplózott edzésrészletből megnyílik a meglévő NativeJournalEditor/JournalEditorDialog. Súly, ismétlés, opcionális tényleges RIR, időpontok és visszajelzés tudatosan módosítható. A native CAS, aktív-edzés/tombstone/restore és korábban alkalmazott feedback védelme érvényes. A sikeres mentés frissíti a Studio napló/PR/Coach bemenetét. A `right` visszajelzés a meglévő Pont jó chiphez tartozik.
- #71 részleges: a Studio egészségnaplóból elérhető a meglévő részletes rekordböngésző dátumtól/dátumig, eredet/típus, lapozás és alvásszakasz/sorozatrészlet kezelésével, a kiválasztott Samsung vagy HC csatornán. Nem nyitunk BLE-kezelőt és nem váltunk észrevétlenül forrást. A teljes 26 metrikás összesítő külön paritásfeladat marad.

## Megőrzött szerződések és ellenőrzés

A régi programmezők és sorrend továbbra is működnek. A kanonikus művelet megvárja a korábbi Studio-mentést, a köztes szerkesztést blokkolja, az eredményt visszaolvassa; nincs optimista törlés. Egy régi adatbetöltés nem írhatja vissza a már törölt programot. Sajátgyakorlat- és programrészlet frissítésekor a kiválasztott elem megmarad. Package, signer, verzió és Wear-kód változatlan.

Új regressziók: valódi programmásolat/nap/gyakorlat/név/törlés Room-visszaolvasással és eredeti terv megőrzésével; aktív program törlésének megtagadása; kedvencek újrakötés után; sajátgyakorlat szerkesztés/törlés/kedvenc-takarítás; naplóban kézi értékek, valódi 0 RIR és fájdalomfeedback mentése azonos edzésazonosítóval; explicit UI törlési megerősítés; az órablokkok tényleges egymás melletti elhelyezése; Samsung nem támogatott/engedélyhiány megkülönböztetése. A teljes Android CI és az egyetlen végleges aláírt APK bizonyítékát az aktuális átadó rögzíti.

## Egyben végzendő telefonos próba

1. YouTube fekvenyomás és másik gyakorlat: Lejátszás, pause, fullscreen, bezárás/háttér után nincs hang; belső böngészős lehetőség is használható.
2. Óraadatok frissítése, két kompakt panel, nagy betűs tördelés. Samsung valódi pulzus továbbra is látszik, indokolatlan nyugalmi engedélyhiba nincs.
3. Egy saját tesztprogrammásolatban nap hozzáadás/törlés, gyakorlat eltávolítás, átrendezés és súly/ismétlés/pihenő mentése; app újranyitása után is megmarad. Aktiválás tudatos. Törlés után a másolat valóban eltűnik; az eredeti és a napló megmarad.
4. Saját tesztgyakorlat szerkesztése, kedvenc jelölés/szűrés, hivatkozás nélküli tesztgyakorlat törlése. Használt elemnél érthető elutasítás, nincs adatvesztés.
5. Egy naplózott edzés megnyitása és szerkesztése: mentés és Mégse külön, a tényleges RIR 0/üres külön. Az adatok újranyitáskor helyesek; aktív vagy törölt edzés nem szerkeszthető.
6. Egészségnapló részletes böngésző: dátumtartomány, eredet/típus, lapozás; a kiválasztott adatforrás és hiányállapot megmarad.

A teljes #69 gyakorlatonkénti/utólagos feedback-paritás, #71 26 metrikás összesítő, #74 preset/automata motorbekötés és #75 teljes négynyelvű felület még nem tekinthető késznek. A korábbi #35/#48 térkép/szöveg javítások készülékes elfogadása is a közös próbába tartozik.

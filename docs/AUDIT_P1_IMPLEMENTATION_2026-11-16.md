# 2763 audit — első P1 javítócsomag

Alap: main `17ecad078a767162f0be303954a7b65e97caa38d`.
Közös ág: `fix/audit-p1`. A jelentés a
[AUDIT_2763_REVIEWED_2026-11-16.md](AUDIT_2763_REVIEWED_2026-11-16.md)
ellenőrzött megállapításai alapján készült. Nem tekinti a korábbi audit
cáfolt állításait javítandó hibának.

## Tartalom

| Jegy | Módosítás | Regressziós bizonyíték |
| --- | --- | --- |
| #247 / PR248 | Az első profilbeállítás a Home-mal közös jóváhagyott tervezőt használja; az onboarding csak tartós siker után kész. A korábbi javítóág bekerült ebbe a csomagba. | Korábbi PR248 kapu: 710 teszt. Ebben a csomagban ismét a teljes közös kapu része. |
| #250 | A telefon által vezérelt kétoldali órás készre jelölés natív mért eseménnyé válik; az eseményhez kötött visszavonás és a nyugta kezelése a meglévő tartós végrehajtási úton történik. | 16 új telefonos és 9 Wear regresszió a valós parancskészítővel, végrehajtóval, tartós állapottal, outboxszal és nyugtával. |
| #251 | A következő/előző léptetés a meglévő natív exercise/set ID-kat választja ki. Órás vezérlés, átadás, felfüggesztés vagy mentés alatt nem írhat; az utolsó gyakorlatból az összesítésre lépés szünetelteti a futó mérést. | 7 ViewModel/natív folyamat- és 3 tényleges képernyő-interakciós teszt. |
| #252 | Az egyedi gyakorlat és program csak tartós siker után jelenik meg késznek. A gyakorlat és a programnap kapcsolata közös Room-tranzakció, adatkészlet-generációs védelemmel; hiba nem zárja be sikeresen a szerkesztőt. | 5 natív tranzakciós és 5 ViewModel-regresszió; a korábbi quick-workout teszt immár valós Room-mentést és a mentett gyakorlatokat használja. |

## Lényeges működés

### Órás kétoldali készre jelölés és visszavonás

A tényleges órás küldési út közös parancskészítője a kétoldali explicit OK
esetén `completeMeasuredSet` parancsot készít a valós bal/jobb idővel és súllyal.
A telefon a meglévő natív mért rögzítést, tartós eseményazonosságot és célzott
Undo-t használja. A manuális edzés visszavonásának nem előfeltétele az
automatikus időzítés bekapcsolása. A visszavonás után régi OK ismétlése nem
jelölheti újra készre a sorozatot.

A teljes OK-payload a korábbi órás mezőszerkesztések részleges kézbesítésekor
is alkalmazható, kizárólag igazolt, megszakítatlan azonos órás forrás/célpont
szerkesztési láncon. A workout/generáció/epoch/pause/base revision egyeztetés
megmarad; közbenső telefonos vagy más célpontú írás megszakítja ezt a láncot.
Az ilyen lánc alatt a készre jelölés előtt beérkező Undo halasztva várhat a
rögzítésre. Hibás/elavult mért parancs tartós hibanyugtát kap; restore alatt
újrapróbálható marad. A régi protokollból érkező kétoldali készre jelölés is
ugyanezen naplózott úton halad.

Az új mért hibanyugta csak egyező rekord, workout, generáció, epoch és
változatlan érték-guard mellett korrigálhatja az optimista órás állapotot.
Régebbi végrehajtási verzió vagy későbbi helyi parancs esetén nem telepít
elavult snapshotot. A közös builder, valós telefonos dispatcher/végrehajtó,
tartós adatbázis, Wear outbox és nyugtakezelő regressziói együtt vizsgálják a
normál és fordított kézbesítést, duplikációt, hibás/stale értéket, idegen írást,
restore-t és késői nyugtát.

Két körben külön Sol/High read-only review történt; a talált négy konkrét
ordering/dispatch/receipt hibát a végleges implementáció javította.

### Léptetés és mérés

A ViewModel a natív frame workout ID-ját és generációját egyezteti a megnyitott
edzéssel. Nem gyárt katalógusból helyettesítő edzést, és nem írja vissza az
alapértelmezett sorozatértékeket. A natív kiválasztásra a közös állapotfolyam
vetíti rá a felületet. A három aktív edzésképernyő léptetőgombjai is le vannak
tiltva, amikor a telefon nem módosíthat. Az összesítés közös navigációs útja
szünetelteti a natív egyoldali és kétoldali stoppert is.

### Tartós könyvtármódosítás

A meglévő `mutateStudioLibrary` kapu vár a korábbi programírásokra,
sorosítja a módosítást, és az induláskori adatkészlet-folytatást ellenőrzi.
Az új `NativeCustomLibraryMutation` a meglévő `NativeProgramEditor` szabályait
használja: megőrzi az egyedi előírásokat és a beépített program másolásának
szabályát. Hiányzó/cserélődött programnap vagy sikertelen tranzakció nem hagy
félkész új gyakorlatot. A program aktiválása a Room-mentést követi; a felületi
siker csak a teljes művelet után történik. A Room és DataStore nem közös
adatbázis-tranzakció.

A két új nyilvános ViewModel-út nem közöl optimista mentési sikert. Bezárt vagy
nem elérhető adatbázisnál hibaüzenet marad, és a katalógus nem tartalmaz nem
mentett sort. Ha a felhasználó közben máshová navigál, az új program mentése
nem nyitja meg váratlanul a korábbi lap szerkesztőjét.

## Ellenőrzés

A helyi körben forrásellenőrzés és `git diff --check` történt. Android/Gradle
fordítás és regresszió kizárólag a meglévő
`trainpilot-android-ci.yml` teljes kapujában fut. A kész integráció után egy
közös kézi futás szükséges; APK/AAB-csomagolás és verzióemelés nincs ebben a
feladatban.

CI-státusz: **első futás sikertelen**, run `43881467244`, forrás
`0b7a426312c72e7ad31da0e43349be15bc9d7a1b`, draft PR253.
Nyers XML: telefon557/4 failure, Wear198/0 failure, összesen755/4;
error/skipped0. Fordítás és R8 sikerült. ZIP-digest ellenőrizve;
[első CI bizonyíték](verification/PR253-first-ci-evidence.json).
A pending teszt fixture valós átadási guardra javítva, a három mentési
kimenetelhez célzott coroutine-diagnosztika került. Új közös kapu következik;
alkalmazáskódot igazolatlan ok miatt nem módosítottunk.
Összesen 45 új regresszió a korábbi 710 tesztes állapothoz képest.

## Elfogadás és további munka

A draft PR, a sikeres CI és a fizikai elfogadás külön állapot. Main-merge,
kiadás és az issue-k készre zárása nem történt. Fizikai telefon–óra ellenőrzés:

1. Telefonos vezérlés mellett kétoldali értékek módosítása az órán, gyors készre
   jelölés, majd visszavonás; telefonon és órán ugyanaz az eredmény és pihenő.
2. Lassú/újracsatlakozó Data Layer mellett ismételt vagy felcserélt kézbesítés,
   közben telefonos szerkesztés; régi parancs nem írhatja felül az új értéket.
3. Órás vezérlés/átadás közben tiltott telefonos léptetés; futó időmérésből az
   utolsó gyakorlatnál összesítés, majd kifejezett folytatás.
4. Egyedi gyakorlat programnaphoz és önállóan; újraindítás után megmaradó adat.
   Sikertelen mentésnél szerkeszthető, nyitott panel és látható hiba.
5. Friss telepítésen a profil/terv panel és sikeres onboarding-mentés.

A jelentés P2/P3 tételei (lokalizáció, kültéri előnézet, világos rendszersáv,
megjelenési/időköz-beállítások, pending replay vizsgálata, teljesítménymérés)
ezután külön, igazolt reprodukcióval javítandók. A csomag nem állítja, hogy
minden auditfeladat elkészült.

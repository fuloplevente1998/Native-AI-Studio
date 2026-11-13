# TrainPilot 2758 · 1.1-beta – változások a 2757-es kiadás óta

Verziókód: telefon **275800**, Wear OS **275802**. A felhasználó által megadott név: **1.1-beta**.

## Telefon

- Az órás vezérléshez tartozó sorozatok telefonos szerkesztése már a helyi módosítás előtt ellenőrzi a natív vezérlési állapotot. A tiltott művelet nem hoz létre fantom készre jelölést vagy pihenőindítást.
- Egyetlen lenyitható Edzés beállításai blokk tartalmazza a hangot, rezgést, pihenőidőt, edzésstílust és progressziót. A külön, duplikált kapcsolók megszűntek. A módváltás továbbra is előnézetet és jóváhagyást kér.
- A mentés, import és visszaállítás gombjai a többsoros felirattal együtt magasodnak.
- A kezdőlap hat napi mutatója közös témaszínű keretet kapott. A Coach-ajánlás olvashatóbb, hosszabb szövegnél helyben lenyitható.
- A Gyors edzés teljes elérhető magasságban nyílik, félállapot nélkül.
- A Profil és új terv, az új egyedi gyakorlat és a naplóedzés szerkesztése teljes magasságú panelt kapott, a görgetéstől független mentési lábléccel. A mezők és a meglévő adatkezelési szerződések megmaradtak.
- Az aktív súlyzós edzés sorozatai kártyásak: a súly, ismétlés és RIR helyben választható legördülőkből. Egyedi súly (például 6,25 kg) is megadható. Az értékválasztás különbözik a sorozat rögzítésétől; a rögzítésnek továbbra is külön gombja van.
- A tizedes súlyok megjelenítése nem ír ki Float-pontatlanságból származó számjegyeket: például 19,7 kg jelenik meg 19,700001 helyett.
- Nagy betűméretnél a sorozat értékmezői egymás alá rendeződnek. A naplóban mentés közben a bezárás tiltott marad.

## Wear OS

- A dinamikus sorozat-, oldal-, kör-, stopper- és menüfeliratok is a meglévő HU/EN/DE/RO fordításokat használják az óra nyelve szerint.
- Az adatmodell, szinkronazonosítók és mérési motor nem változott. A telefon–óra témaszinkron (#220) nem része ennek a csomagnak.

## Ellenőrzés és korlát

Aláírt main-forrás: `38f6ecdd0347e81f6f7491871eb6c18c102e80e9` (#225 és #226). A közös APK+AAB és teljes JVM-futás sikeres: [Actions43450904480](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43450904480).

Nyers XML és ZIP-digest ellenőrizve: **477 telefonos + 187 Wear = 664 teszt, 0failure/error/skipped**; shared NO-SOURCE. Az új regressziók tényleges űrlapokat és callbackeket ellenőriznek: értékmegőrzés, egyedi súly és tizedes megjelenítés, explicit rögzítés, vezérlésfüggő tiltás, rögzített lábléc, bezárás/lehúzás és szövegtördelés. Az előzetes663-as PR-kapu bizonyítéka külön `2758-ci-evidence.json`.

Az APK/AAB párok azonos aláírt futásból készültek. A fájlhash, forrásSHA, package, verzió és tartós aláíró ellenőrizve. TelefonAPK5,5MB, WearAPK4,7MB; a telefon R8/resource-shrink méretcsökkentése megmaradt.

A készülékes Samsung Health/Health Connect/Wear és nagybetűs/világos/sötét vizuális elfogadás külön lépés; az automatikus teszt nem helyettesíti. Új funkciócsomag és új adatmodell nem része a kiadásnak. Play-feltöltést a felhasználó végezhet a két külön AAB-val és a telefonos/órás Play-jegyzetekkel.

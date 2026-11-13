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
- Nagy betűméretnél a sorozat értékmezői egymás alá rendeződnek. A naplóban mentés közben a bezárás tiltott marad.

## Wear OS

- A dinamikus sorozat-, oldal-, kör-, stopper- és menüfeliratok is a meglévő HU/EN/DE/RO fordításokat használják az óra nyelve szerint.
- Az adatmodell, szinkronazonosítók és mérési motor nem változott. A telefon–óra témaszinkron (#220) nem része ennek a csomagnak.

## Ellenőrzés és korlát

A közös telefon/Wear JVM és telefonos release R8 kapu sikeres: Actions43446531333, forrása56139249c1f0108d5787eeed8ba25483b8534b9. Nyers XML és ZIP-digest ellenőrizve: 476 telefonos + 187 Wear = 663 teszt, 0failure/error/skipped; shared NO-SOURCE. A main-merge c6b0a98bd5ab84c09ab963616a022be8974d4a8d fájlfája az ellenőrzött forráséval azonos. Az új regressziók a valódi űrlapokat és callbackeket használják: értékmegőrzés, egyedi súly, explicit rögzítés, vezérlésfüggő tiltás, lábléc, bezárás és szövegtördelés. A main aláírt csomagfutása (43448589388) még folyamatban; a kész APK/AAB fájlok azonosságát és aláíróját az átadás előtt ellenőrizzük.

A készülékes Samsung Health/Health Connect/Wear és nagybetűs/világos/sötét vizuális elfogadás külön lépés; az automatikus teszt nem helyettesíti. Új funkciócsomag és új adatmodell nem része a kiadásnak.

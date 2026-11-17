# #262 — Kompakt és reszponzív vezérlők

Alap: main7bcc1f9, kiadott2765 ·1.2-beta. A képen a gyakorlatváltó csak az
állapotjelvény maradék szélességét kapta; a kis gombok négyzetes48dp minimumot
használtak, a két oldal mezői külön szövegtördelés alapján méreteződtek.

- Közös filled/outlined/text/save/negative gombcsalád: kisebb alap belső margó,
  több hely a fordításnak, megmaradó forma, teljes press feedback és érinthetőség.
  A változás e családok program-, napló-, Coach-, egészség- és beállítási
  hívóit is érinti, nem csak a képen szereplő kétoldali oldalt.
- Közös kompakt műveletcsoport: valódi betűmérés alapján oszlopszám, széles
  gombok; szűk helyen/nagy betűvel új sor. Egy-/kétoldali stopper és kézi oldalszerkesztő.
- Adaptív kétműveletes csoport: stopper, Home folytatás/elvetés, program és
  nap mentés/törlés/hozzáadás. Növelhető minimummagasság, változatlan callback.
- Teljes szélességű gyakorlatváltó; rövid lokalizált Rögzítés/Record/Speichern/
  Salvează, teljes hozzáférhető Stop-and-record név.
- Azonos szélességű/magasságú oldalmezők, rövid egysoros címke, teljes
  hozzáférhető oldalnév. Szűk nézet/nagy betű esetén RIR/készrejelölés külön sor.

Natív időmérés, ownership, kiválasztott stabil sorozat és mentési szerződés
változatlan. Nem minden egyedi appképernyő fizikai ellenőrzését állítjuk:
a közös családok és a megnevezett zsúfolt csoportok javulnak. Betűzsugorítás
vagy globális fix magasság helyett valódi helyet és átrendezést alkalmazunk.

Három új UI-regresszió: betűlayout és callback, egyenlő oldalgeometria és
szerkesztés/RIR/pipa, valódi kétoldali képernyő gyakorlatváltója és gombjai.
HU/EN/DE/RO,1×/1,5×/2× betű,260/320dp komponens és360dp képernyő.
Korábbi élő futás/pause/rögzítés tesztjei változatlanul futnak.
Teljes közös CI44130988511 SUCCESS, forrás
ef84382db4b5553f432a22f90ace74b8f08f9c60. Telefon121 XML/630 teszt,
Wear36 XML/198 teszt, összes828,0failure/error/skipped; shared NO-SOURCE.
A letöltött artifact SHA-256 egyezik a GitHub-metaadattal; a nyers XML
önálló újraszámolása egyezik a CI-verifier eredményével. Produkciós telefon/
Wear fordítás és release R8 sikeres. [Bizonyíték](verification/PR263-ci-evidence.json).

A három új teszt60 nyelv/betű/szélesség elrendezési kombinációt ellenőriz.
A korábbi élő stopper/pause/rögzítés két regressziója is zöld. A készülékes
vizuális elfogadás ettől külön marad. A lezáró dokumentáció nem változtatja a kódot.
Új APK/AAB, main-merge és verzióemelés ebben a körben nincs kérve.

Későbbi felhasználói jóváhagyás: PR263 és PR265 mainen,835 zöld tesztes
signed kapu és ellenőrzött2765-r2 telefonos APK átadva.
[Kiadási átadás](releases/2765-r2-changes.md). Fizikai elfogadás külön marad.

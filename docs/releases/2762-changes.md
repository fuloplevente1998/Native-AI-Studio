# TrainPilot 2762 · 1.1-beta

Telefon: 276200 · Wear OS: 276202

## Telefon — változások a 2761-es kiadáshoz képest

- A gyakorlatválasztóban a kedvencjelölés balra, a név és a részletek középre, a három művelet jobbra került.
- A Videó, Hozzáadás és Indítás közös, vékony, lekerekített keretet kap. A három finoman színezett érintési rész külön működik, legalább 48 dp méretű.
- A hosszú nevek szavanként tördelhetők; szűk vagy nagybetűs nézetben a gombcsoport szükség esetén a szöveg alá kerül. A teljes gyakorlatadatlap továbbra is megnyitható.
- A kereső gyakorlatkeresést jelez; a Kedvencek és az Új gyakorlat belépők normál nézetben egy sorban férnek el.
- A mentési oldal rövidebb tartalomlistát és alacsonyabb JSON/ZIP/DRIVE választót kap. A Google Drive kártya csak a DRIVE nézetben jelenik meg; az ide nem tartozó naptári sikerüzenet kimarad.
- Az Egészségadatok sor infógombja mögött a teljes mentési tájékoztató olvasható. A hozzájárulás alapértéke és az export/import működése megmarad.

## Wear OS

Ebben a kiadásban nincs új órás alkalmazáslogika. A 276202-es órás csomag a telefonos kiadás verziópárja; az edzésmérés és a szinkron meglévő működését a teljes regressziós kör is ellenőrzi.

## Ellenőrzés és telepítés

A PR239 közös ellenőrzése 695 tesztet futtatott, 0 failure/error/skipped eredménnyel (telefon506, Wear189). A HU/DE Compose-képeket átnéztük; a készülékes vizuális és telefon–óra elfogadás külön szükséges.

Aláírt forrás: 155ea6295b0025280b08e30203ff32cac9912b19.
A signed run43620628637 SUCCESS. A kiadási körben is telefon96 fájl/506 teszt + Wear35/189 =695 teszt, 0failure/error/skipped; shared NO-SOURCE. A három ZIP digestje, a négy fájl hashe, a bináris APK/AAB-manifeszt package/verziója és a tartós aláíró helyben ellenőrizve. Maps és páros identitás a CI-kapuban igazolt. A részletek a 2762-verified-artifacts.json fájlban találhatók.

Mindkét alkalmazást frissítsd. Az APK-k közvetlen telepítésre, a két AAB a Play Console megfelelő telefonos és Wear OS-csatornájához készült. A Play Console-ba történő feltöltést a felhasználó végzi.

## Letöltések

- [Telefon APK](https://drive.google.com/file/d/1cy2zD4m0jBm-vhhukZ2U1TejCmce2Pxv/view?usp=drivesdk)
- [Wear OS APK](https://drive.google.com/file/d/18UMnSTAOGLAYC760uizl2r00uHFD-2A8/view?usp=drivesdk)
- [Telefon AAB](https://drive.google.com/file/d/1NwPw8nBuQIABdaeXKy5QWa6-pL9zM8BI/view?usp=drivesdk)
- [Wear OS AAB](https://drive.google.com/file/d/1e5KpFpIGECOUmub4v3OFG1O6F51MiQiF/view?usp=drivesdk)
- [Telefon Play-jegyzet](https://drive.google.com/file/d/1h9UxWjhrhLzk9Bx9SwsMchk359zXhwSm/view?usp=drivesdk)
- [Wear OS Play-jegyzet](https://drive.google.com/file/d/1TW7tvYU3J9drH5ckrFBWUeETzQtVoR8D/view?usp=drivesdk)
- [Ellenőrzési bizonyíték](https://drive.google.com/file/d/1sFMFBi6E5nFgz_XOhvI-XWqMVYDwlrDp/view?usp=drivesdk)

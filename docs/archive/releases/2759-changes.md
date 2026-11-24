# TrainPilot 2759 · 1.1-beta – változások a 2758-as kiadás óta

Telefon verziókód: **275900**. Wear OS verziókód: **275902**.
A felhasználó jóváhagyta a 2759-es javítókiadást, változatlan 1.1-beta névvel.

## Telefon

- A Gyors edzés, a profil/tervező, az egyedi gyakorlat és a naplószerkesztő helyet hagy a státuszsávnak és a kamerakivágásnak. A húzófül és a bezárás is a biztonságos területen belül jelenik meg.
- A napló sorozatkártyái kompaktabbak: normál nézetben a súly, ismétlés és RIR egy sorban választható, egyedi értékek is beírhatók. Kisebb, piros eltávolításikonok váltják a nagy gombokat. Nagy betűméretnél az elrendezés alkalmazkodik.
- Az aktív súlyzós edzés kisebb térközöket és közös címke/érték vezérlőket használ. A sorozat rögzítése és visszavonása egyetlen pipával történik, a natív vezérlési és jogosultsági ellenőrzések megtartásával.
- A kis X- és videógombok nyomási effektje a látható alakjukon belül marad. A fejlécen, kezdőlapi kártyákon és módválasztóknál is követi a lekerekítést. A legalább 48dp érintési cél és a látható nyomási visszajelzés megmarad.
- A naplószerkesztő rögzített mentési lábléce a billentyűzet és a panel mozgatása mellett is megőrzi a látható terület kezelését.

- A pihenősáv a ténylegesen mért felirat- és gombszélesség szerint egy vagy több sorba rendeződik. Nagy betűvel is külön marad az idő és a vezérlés; a gombok középre igazított szöveget és legalább 48dp érintési célt kapnak. A súlyzós és az időalapú felület ugyanazt a komponenst használja.
- A felső, oldalra görgethető gyakorlatsor az aktív gyakorlat változásakor automatikusan annak elejéhez görget, az utolsó gyakorlatnál is. A készre jelölés továbbra is a valóban rögzített sorozatokból származik.

## Wear OS

- A telefonos javítókiadással együtt készült, optimalizált Wear OS csomag.
- Ebben a kiadásban nincs órás alkalmazáskód-, mérési vagy szinkronprotokoll-változás. Az új verziókód a páros kiadás része; a telefon–óra témaszinkron (#220) külön feladat marad.

## Ellenőrzés

A telefonos javítások PR#228-cal mainre kerültek. Előzetes CI: [43494316049](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43494316049), forrás 1019f605ce737341f66c2776a5b0cdc15ebf944a. Nyers XML és artifact-digest ellenőrizve: 486 telefonos + 187 Wear = **673 teszt**, 0 failure/error/skipped; shared NO-SOURCE.

A végleges közös aláírt main APK+AAB futás sikeres: [43565800117](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43565800117), forrás `985969595083575d5b8dfe183df64cc8228d94b2` (#228, #229, #230, #231).

Nyers XML és ZIP-digest ellenőrizve: **490 telefonos + 187 Wear = 677 teszt**, 0 failure/error/skipped; shared NO-SOURCE. A négy új dock regresszió a teljes körben is zöld. A megelőző célzott kör 69 telefonos + 187 Wear tesztje is sikeres (43565270201, `2759-targeted-evidence.json`).

Az APK/AAB csomagok azonos aláírt futásból készültek. A forrásSHA, package, verziók, fájlhashek, telefon–óra páridentitás és tartós signer ellenőrizve; az AAB-k tanúsítványa helyben is összevetve. TelefonAPK: **5 495 467 byte** (5,5 MB); WearAPK: **4 669 627 byte** (4,7 MB), R8 és resource shrinking megmaradt.

A korábbi, kiegészítés nélküli 43505815125 futás leállítva; a 43508709582 futás három pihenőfelirat-ellenőrzésen megbukott. Ezekből telepítőt nem adtunk át. A végleges fenti futás az összes javítást tartalmazza.

Az APK-k telepítéshez, az AAB-k a két Play Console tesztcsatorna frissítéséhez valók; a feltöltést a felhasználó végezheti a külön telefon/Wear jegyzetekkel. A készülékes kamerakivágásos, világos/sötét és nagybetűs vizuális elfogadás külön ellenőrzés. A teszt nem helyettesíti a fizikai Samsung/HC/Wear ellenőrzést.

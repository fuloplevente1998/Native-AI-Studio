# TrainPilot 2759 · 1.1-beta – változások a 2758-as kiadás óta

Telefon verziókód: **275900**. Wear OS verziókód: **275902**.
A felhasználó jóváhagyta a 2759-es javítókiadást, változatlan 1.1-beta névvel.

## Telefon

- A Gyors edzés, a profil/tervező, az egyedi gyakorlat és a naplószerkesztő helyet hagy a státuszsávnak és a kamerakivágásnak. A húzófül és a bezárás is a biztonságos területen belül jelenik meg.
- A napló sorozatkártyái kompaktabbak: normál nézetben a súly, ismétlés és RIR egy sorban választható, egyedi értékek is beírhatók. Kisebb, piros eltávolításikonok váltják a nagy gombokat. Nagy betűméretnél az elrendezés alkalmazkodik.
- Az aktív súlyzós edzés kisebb térközöket és közös címke/érték vezérlőket használ. A sorozat rögzítése és visszavonása egyetlen pipával történik, a natív vezérlési és jogosultsági ellenőrzések megtartásával.
- A kis X- és videógombok nyomási effektje a látható alakjukon belül marad. A fejlécen, kezdőlapi kártyákon és módválasztóknál is követi a lekerekítést. A legalább 48dp érintési cél és a látható nyomási visszajelzés megmarad.
- A naplószerkesztő rögzített mentési lábléce a billentyűzet és a panel mozgatása mellett is megőrzi a látható terület kezelését.

## Wear OS

- A telefonos javítókiadással együtt készült, optimalizált Wear OS csomag.
- Ebben a kiadásban nincs órás alkalmazáskód-, mérési vagy szinkronprotokoll-változás. Az új verziókód a páros kiadás része; a telefon–óra témaszinkron (#220) külön feladat marad.

## Ellenőrzés

A telefonos javítások PR#228-cal mainre kerültek. Előzetes CI: [43494316049](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43494316049), forrás 1019f605ce737341f66c2776a5b0cdc15ebf944a. Nyers XML és artifact-digest ellenőrizve: 486 telefonos + 187 Wear = **673 teszt**, 0 failure/error/skipped; shared NO-SOURCE.

A közös aláírt main APK+AAB futás, a csomagok és a Drive-átadás bizonyítéka a kiadás végén kerül ide. A készülékes vizuális elfogadás külön ellenőrzés.

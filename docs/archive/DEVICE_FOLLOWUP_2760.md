# Készülékes javításgyűjtő — 2760 után

A 2760-as készülékes képek alapján jelentett hibák közös javítása. A kódjavítások ellenőrizve. A felhasználó most 2761 · 1.1-beta telefonos és órás teszt-APK-t kér; az AAB-páros csak a készülékes jóváhagyása után készül.

### Funkció / adatmegjelenítés
- [x] Kezdőlapi következő terv frissül a naptári újratervezés után; indítás ugyanazt a tervet használja.
- [x] A/B nap valós azonosítója látszik a kezdőlapon és a naplóban; az elavult beépített „Alap” név kezelése felhasználói névátírás nélkül.
- [x] Coach következő edzés gomb a közös indítási folyamatot nyitja.
- [x] Naptári újratervezéskor másik program választható.
- [x] Tervezett időtartam 15 perces lépcsőkben, 90 perc fölötti választék.
- [x] Privát fotók dátum szerint, függőleges elrendezésben.

### Dizájn
- [x] Egyetlen indításikon a gyakorlat részleteinél.
- [x] „A nap” / „B nap”: kb. 10%-kal nagyobb, finoman kiemelt jelvény a kezdőlapon.
- [x] Laposabb, íves pihenődokk, nagyobb idő és takarékosabb nyíl-elhelyezés, érintési célok megtartásával.
- [x] Lekerekítésen belüli nyomási effekt; programszerkesztő/részletek fejlécének levágása javítva.
- [x] Naptári visszavonás felismerhető ikon/gomb.
- [x] Finoman látható beállítási/szinkron gombok, vékony keretekkel.
- [x] Egyszerű Mentés / Visszaállítás / Importálás szövegek, régi formátumok támogatásának megtartásával.

### Ellenőrzés
- [x] Funkcionális állapotváltások és lényeges geometriák regressziói.
- [x] Közös GitHub Actions JVM/R8 kapu, nyers XML eredménnyel.
- [ ] Készülékes vizuális elfogadás (külön állapot, CI nem helyettesíti).

Munkamegosztás: koordinátor — kezdőlap/naptár adatfolyam, Coach útvonal és naplójelölések, integráció; Terra Medium — egymástól elkülönített tervezés/részletek, beállítások/fotók/mentés és pihenődokk; Luna Low — kizárólag forrásfeltárás. Minden monolit fájlnak egy tulajdonosa van; közös CI-kör készül. #210/#211 történeti naptári tisztítás nincs ebbe a javításba beemelve.

## Ellenőrzött kód és következő lépés

Forrás: 52ee89af1166fca621efeaf50353bf1bf55d16fb. Teljes CI: https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43592120564 — SUCCESS. A letöltött JUnit ZIP SHA-256 digestje egyezik az artifact-metaadattal; a nyers XML helyi összesítése telefon 94 fájl / 504 teszt, Wear 35 fájl / 189 teszt, összesen 693, 0 failure/error/skipped; shared NO-SOURCE. Bizonyíték: docs/verification/PR236-ci-evidence.json.

A kezdőlap a közös natív Home/Coach célválasztást figyeli Room-invalidációval. Az indítás bevárja a program és a naptár mentését, majd a tartós terv program-, nap- és alkalomazonosítóját adja át a meglévő motornak. A naptári események saját program/nap azonosítót őriznek; az újratervező választása alkalmazásig helyi piszkozat. A napló a mentett edzés saját napját mutatja. Egyedi nevek és a hiányos régi napadatok nem íródnak át. A Coach Indítás a meglévő natív start/preflight útvonalat használja.

A felületi csomagban 15–240 perces tervezés, egyszeres Play ikon, nappill, laposabb és végig íves pihenődokk, nagyobb idő, finom gombháttér, alakba vágott nyomási effekt, rögzített szerkesztő/részlet fejléc, függőleges dátumos fotók és egyszerű Mentés / Visszaállítás / Importálás szerepel. A régi JSON/ZIP import és a health-consent megmaradt.

Nyolc új regressziós eset és frissített meglévő elvárások védik a viselkedést. Az ellenőrzés közben talált régi callback-aláírás, elavult mentési szöveg, Home szemantikai kereső és katalógus inicializálásától függő naplócím-fordítás javítva; teszt nem lett kihagyva vagy gyengítve. A fejléc tesztje tényleges görgetést is ellenőriz.

## Kért tesztcsomag

- [x] Aláírt, ellenőrzött 2761 · 1.1-beta telefonos APK (276100) és Wear APK (276102), közös forrásból.
- [x] APK-páros átadása a megszokott Drive-mappában.

Az új build a meglévő main/android-signing kaput használja, új build_aab=false bemenettel. A main-korlát, tartós aláíró, Maps-ellenőrzés, teljes JVM-kapu és ideiglenes kulcs törlése megmarad. Az APK-only és APK+AAB feladatválasztás shell-szinten ellenőrizve; APK-only módban nincs bundle-feladat vagy AAB-ellenőrzés/feltöltés. AAB csak a felhasználó készülékes jóváhagyása után; Play Console feltöltés nincs ebben a kérésben.

## APK-páros átadva

A mainre integrált forrás a9b903f353a0e5dc2b2e8e01e6d3291bc560ccc1; signed run43593234136 SUCCESS. Helyben ellenőrzött 693/693 JVM teszt, ZIP-digest, APK-hash, tényleges manifest-verzió és tartós signer. Mindkét APK és a külön telefonos/órás leírás a Drive-on, metaadat-visszaolvasással. Részletek: [2761 APK átadás](releases/2761-apk-changes.md). AAB nem készült; készülékes elfogadásra vár.

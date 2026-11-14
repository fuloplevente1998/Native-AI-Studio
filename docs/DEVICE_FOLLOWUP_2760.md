# Készülékes javításgyűjtő — 2760 után

A 2760-as készülékes képek alapján jelentett hibák közös javítása. Az alkalmazáskód változik; új APK/AAB és verzióemelés ebben a csomagban nincs. A meglévő kiadás érintetlen.

### Funkció / adatmegjelenítés
- [ ] Kezdőlapi következő terv frissül a naptári újratervezés után; indítás ugyanazt a tervet használja.
- [ ] A/B nap valós azonosítója látszik a kezdőlapon és a naplóban; az elavult beépített „Alap” név kezelése felhasználói névátírás nélkül.
- [ ] Coach következő edzés gomb a közös indítási folyamatot nyitja.
- [ ] Naptári újratervezéskor másik program választható.
- [ ] Tervezett időtartam 15 perces lépcsőkben, 90 perc fölötti választék.
- [ ] Privát fotók dátum szerint, függőleges elrendezésben.

### Dizájn
- [ ] Egyetlen indításikon a gyakorlat részleteinél.
- [ ] „A nap” / „B nap”: kb. 10%-kal nagyobb, finoman kiemelt jelvény a kezdőlapon.
- [ ] Laposabb, íves pihenődokk, nagyobb idő és takarékosabb nyíl-elhelyezés, érintési célok megtartásával.
- [ ] Lekerekítésen belüli nyomási effekt; programszerkesztő/részletek fejlécének levágása javítva.
- [ ] Naptári visszavonás felismerhető ikon/gomb.
- [ ] Finoman látható beállítási/szinkron gombok, vékony keretekkel.
- [ ] Egyszerű Mentés / Visszaállítás / Importálás szövegek, régi formátumok támogatásának megtartásával.

### Ellenőrzés
- [ ] Funkcionális állapotváltások és lényeges geometriák regressziói.
- [ ] Közös GitHub Actions JVM/R8 kapu, nyers XML eredménnyel.
- [ ] Készülékes vizuális elfogadás (külön állapot, CI nem helyettesíti).

Munkamegosztás: koordinátor — kezdőlap/naptár adatfolyam, Coach útvonal és naplójelölések, integráció; Terra Medium — egymástól elkülönített tervezés/részletek, beállítások/fotók/mentés és pihenődokk; Luna Low — kizárólag forrásfeltárás. Minden monolit fájlnak egy tulajdonosa van; közös CI-kör készül. #210/#211 történeti naptári tisztítás nincs ebbe a javításba beemelve.

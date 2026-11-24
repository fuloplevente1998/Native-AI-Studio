# TrainPilot 2757 –változások a2756 kiadáshoz képest

Előző forrás:6b384519f28fb5c06c128cda45312a1a09087cc9,
telefon275600/Wear275602. Új kódok:telefon275700/Wear275702; név1.0-beta.
Az új funkciókat tartalmazó audit5. pontja kimaradt.

## Telefon

- Pipa visszavétel nem indít új pihenőt és nem ír át határidőt.
- Az időcél valós mm:ss formátumú:90másodperc=01:30.
- A tervezés és újratervezés közös szabályt használ: napi/kétnapi/3napos
  H–Sze–P/4napos H–K–Cs–P ritmus,1–12hét, év- és hónaphatáron túl.
- Az összegző a natív aktív időablakból számol; a szünet nem edzésidő.
  Kalória csak a megfelelő edzéshez kötött valós mérésből, forrásduplikáció nélkül.
- Megszűnt a gyors gyakorlatlisták40/25 elemes néma csonkolása.
- Éles indulási útvonalon működő, tartós onboarding; meglévő felhasználó megőrzése.
- Studio Activity külső exportja lezárva. Kiadói név/support cím kitöltve.
- Android13+ rendszeres fotóválasztó; régi Xiaomi/GET_CONTENT fallback megmarad.
- Világos témában palettás beviteli mezők/chipek, javított toolbar keret,
  nagyobb érintési célok, nyelv szerinti szám/dátum/idő és javított Coach címkék.
- Régi, használaton kívüli telefonos scaffold/programlista eltávolítva.
  UI-state/naptár/összegző külön egységek; a teljes VM-domainbontás külön munka.
- Telefonos releaseR8 és resource shrinking bekapcsolva; Samsung reflection
  megőrzési szabályokkal. A tényleges APK-méretet a build után kell ellenőrizni.

## Wear OS

- Meglévő menü-, edzésvezérlő-, mérési, napló-, visszajelző és hozzáférhetőségi
  feliratok HU/EN/DE/RO fordítása bővítve, az óra rendszerének nyelve szerint.
- Automata edzésértesítések lokalizálva; a mérések/szinkron azonosítói változatlanok.
- Nem regisztrált régi Wear motor/listener/UI eltávolítva.
- Optimalizált R8+resource shrinking release továbbra is az átadandó változat.

Új telefon–óra nyelvszinkron-protokoll, szuperszett vagy más audit5. funkció nincs.
A közös aláírt main-futás sikeres:43407855167.
Forrás:0c0e0621e139c0ac73850a8cc1c38d63947839a0.
Nyers XML645=459telefon+186Wear,0failure/error/skipped,shared NO-SOURCE.
A ZIP digestek,négy fájl hash-e,verziókódjai,tartós signer és páridentitás ellenőrizve.
AAB-k aláíró tanúsítványa helyben is a rögzített TrainPilot-kulccsal egyezik.

## Ellenőrzött méretek

| Fájl | Bájt | MB |
|---|---:|---:|
| app-release.apk | 5476879 | 5.48 |
| wear-release.apk | 4669631 | 4.67 |
| app-release.aab | 10044535 | 10.04 |
| wear-release.aab | 5578790 | 5.58 |

Az előző2756 telefonos APK20628244bájt volt; az új5476879bájt,73,4%-kal kisebb.
A Wear továbbra is R8+resource shrinking release.

## Letöltések a TrainPilot Native Drive-mappában

| Eszköz | APK | AAB | Kód |
|---|---|---|---|
| Telefon | [Release APK](https://drive.google.com/file/d/1ZirXKXr9_txvNyAcgpRFC8rIKFS0L1jU/view?usp=drivesdk) | [Play AAB](https://drive.google.com/file/d/1EUS7mxzzP-oerL8kqCDZ46ArHRB6a9_x/view?usp=drivesdk) | 275700 |
| Wear OS | [Optimalizált APK](https://drive.google.com/file/d/1tRpbY0_AjlHYQ-MhnXvZLpSUtSs3hNAS/view?usp=drivesdk) | [Play AAB](https://drive.google.com/file/d/1BN_Q-u1TzaeGe5P0lXn754zt7NNOS5gN/view?usp=drivesdk) | 275702 |

[Telefonos Play-szöveg](https://drive.google.com/file/d/1sVkQyotjXsNcXzPGn2J2koOrBqZ9wnk6/view?usp=drivesdk) · [Wear Play-szöveg](https://drive.google.com/file/d/1tEt-sqCw5TnM5uzMsEVG39ZAPDtg-v8A/view?usp=drivesdk)

A fájlok Drive-neve,MIME-típusa,mérete és szülőmappája visszaolvasva.
A korábbi fájlok megmaradtak. Play Console-feltöltés nem történt;
a felhasználó frissíti a zárt Alpha és Wear OS Beta csatornákat az AAB-kkal.
Kiadásnévnek használható:1.0-beta-2757.
Fizikai eszközös/vizuális/PlayConsole jóváhagyás külön elfogadás.

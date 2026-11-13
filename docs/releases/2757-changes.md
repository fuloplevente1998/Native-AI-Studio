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
A645tesztes forrásaudit már sikeres; a végleges main aláírt APK+AAB közös futása
még következik. Fájllinkek és buildbizonyíték csak a tényleges ellenőrzés után.
Fizikai eszközös/vizuális/PlayConsole jóváhagyás külön elfogadás.

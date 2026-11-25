# 4. feladat – Programok oldal akadás kinyitott programnál (STÍLUS VÁLTOZATLAN)

Nincs mockup: kinézet nem változhat.

## Hiba
Programok oldalon akkor akad a görgetés/nyitás, ha valamelyik program ki van nyitva. Napló és Haladás/Statisztika (sok lista) feltehetően ugyanígy. Debug build lassú, ezért az ok kódból feltételezés; mérni release-szerű teszt APK-n fogunk.

## Fájlok
`app/src/main/java/com/example/ui/screens/workout/ProgramsListScreen.kt` (nyitott kártya ~462. sor környéke), `ui/screens/log/LogTabScreen.kt`, `ui/screens/progress/ProgressTabScreen.kt`, `ui/theme/GlassSurfaces.kt`, `GlassContentBackdrop.kt`.

## Teendő (csak teljesítmény, látható változás nélkül)
1. Keresd meg a kinyitott program kártyában, a gyakorlat-sorokban/gombokban a soronkénti glass/backdrop/blur réteget és a nyitás-animáció közbeni újrarögzítést. Ahol van, cseréld ugyanolyan kinézetű, de homály nélküli áttetsző festékre (Tile szerep) – a megjelenés maradjon.
2. LazyColumn elemeknek stabil `key` és `contentType`; ne hozz létre új lambdát/listát minden recompositionnél (`remember`, `derivedStateOf`), drága számítás ne fusson kompozícióban.
3. A kinyíló tartalmon ne legyen `graphicsLayer`/`blur`/`RenderEffect`; az animált magasság ne okozzon háttér-újrarögzítést (a kártya ne legyen külön capture-forrás).
4. Egyszerre legfeljebb ~3 homályos réteg legyen a képernyőn; lap nyitva esetén a mögötte lévő oldal homálya kikapcsolható, ha egyszerűen megoldható.
5. A jelentésben írd le: mi volt a feltételezett ok a kódban (fájl:sor), mit változtattál. Mérési adatot ne találj ki.

## Tilos
A lista kinézete, a Beállítások, adatműveletek.

## Kész, ha
Kinézet pixelre ugyanaz; CI zöld; a PR-leírásban az ok + a változtatás. A tényleges javulást eszközön, release-szerű teszt APK-val ellenőrizzük.

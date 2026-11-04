# AI Studio: main változások a 2749-es APK után

A PR104 profil- és statisztikajavításait őrizd meg a nyelvi munka integrálásakor. Frissen olvasd vissza a maint, és a saját fordításaidat illeszd rá; a munka elején lementett teljes képernyőfájlok visszamásolása eltüntetné ezeket a javításokat.

Érintett fájlok:
- `app/src/main/java/com/example/ui/components/TrainPilotCommonComponents.kt`: a kezdőprofil minden egész cm és kg értéket kínál a meglévő tartományban; csak a többválasztós felszerelés-dropdown marad.
- `app/src/main/java/com/example/ui/screens/StudioStatisticsScreen.kt`: a rekordgrafikon külön 160 dp rajzterületet kap; konkrét kardióaktivitásnál nem látszik a napi lépésösszesítő, az összes aktivitásnál visszatér.
- `app/src/main/java/com/repforge/app/ui/progress/NativeCardioProgressPanel.kt`: a napi aktivitással azonos kártyastílus, keskeny kijelzőn két oszlop, a fölösleges külön Kardió cím és leírás nélkül.
- `app/src/main/java/com/repforge/app/ui/progress/StatisticsMetricTile.kt`: közös napi aktivitás/kardió metrikakártya. A fordított címkék és az egységek a hívó oldalon maradnak.
- `app/src/test/java/com/example/ProfileAndRecordLayoutRegressionTest.kt`: profilértékek, többes kijelölés, grafikonmagasság és aktivitásszűrő ellenőrzése.

Nincs backend-, adatformátum-, videó- vagy nyelvi erőforrás-módosítás ebben a csomagban.

A ténylegesen kiadott 2749 APK forrása `8e700a8cf2191eb4c4a9570a9457ad3b825e9a30`; az még nem tartalmazza a PR104-et. Az APK bizonyítékai: `RELEASE_2749_VERIFIED_ARTIFACTS.json`. Következő közös kiadáshoz a verziókód legyen nagyobb a main és a már átadott APK verziójánál.

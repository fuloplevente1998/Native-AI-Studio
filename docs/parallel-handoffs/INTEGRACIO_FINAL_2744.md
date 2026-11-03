> Történeti feladat/átadás. Az aktuális státusz és a felhasználó legújabb sorrendje: [CURRENT_STATUS.md](../CURRENT_STATUS.md). A már mainen lévő kódot ne implementáld vagy merge-eld újra.

# TrainPilot 2744 – közös integrációs átadás

## Forrás és hatókör

Kiinduló `main`: `ee826e6a744ae5e6332c020e0005134f7dc698b2`, app+Wear build 2743.
Közös ág: `integrate/trainpilot-2744-parallel`.
A #58, #62, #63, #64, #65 forrása egy ütközésmentes, több szülős integrációs commitba került:
`315a7ba2c578cac37890e3828bf66b2d38de4d2b`. A négy önálló munka saját
fájljainak módosításai és `INTEGRACIO_{video,watch,health,map}.md` átadói megmaradtak.
A közös komponenseket kizárólag az integrátor kötötte be.

## Közös módosítások

- Watch: `WatchInfoCardData` a hitelesített, node+requestId ellenőrzött `NativeWatchDeviceInfo.Result`
  node/JSON válaszból, egyetlen eredeti kéréssel. Loading, timeout, hiba, node-váltás
  és Sync ikon a két belső panelben. A korábbi keret nélküli ismétlődő UI sor kikerült.
- Health: a kiválasztott `HealthJournalStore` szolgáltató izolált napi és heti pulse
  projekciója és az eredeti Room edzés JSON-naplón futó kanonikus Coach recovery.
  Nem használ kitalált hiányzó mérésértéket; a demo switch nem ír readiness-score-t.
  A Home/Health ugyanazokat a `TrainPilotUiState` mezőket olvassa.
- Map: a #63 valódi fullscreen Dialog / safe viewport és kompakt választója, extra
  caller-módosítás nélkül (az átadó szerint a szükséges hívás saját fájlban elkészült).
- Video: a #65 eredeti hívási API-n keresztüli, HTTPS-re korlátozott WebView
  és belső böngészős fallback. Új Gradle/Manifest-függőség nem kellett.
- App és Wear `versionCode=2744`, `versionName=0.1.0-beta`,
  `applicationId=com.TrainPilot.com`; eredeti aláírókonfiguráció változatlan.
  Az APK metadata verifikátor release-elvárása 2744.

## Ellenőrzési határ

Az önálló PR-ek zöld CI-je nem bizonyítja a kombinált ág sikerét.
Kötelező: teljes kombinált Android/JVM CI, nyers JUnit XML test/failure/error/skipped
számlálás, valós checkout SHA, app+Wear debug és állandó kulcsos release pár,
csomag+verzió+SHA256+signer+Maps-pair ellenőrzés.
A CI által készített új ideiglenes debug kulcs **nem** alkalmas korábbi
telepített APK helyben frissítésére.
A permanens signerrel építő GitHub workflow csak megbízható `main` referencián
indítható és a meglévő `android-signing` környezetet használja.

Fizikai készülékes ellenőrzés NYITOTT: valódi YouTube 101/150/153 és háttérhang,
Wear kapcsolódás/offline/timeout/reconnect, HC/Samsung engedély és tényleges
pulzus/regeneráció, GPS fullscreen és primer gombok/navigáció.
A tesztadat és CI nem helyettesíti ezt. Nincs Play/GitHub Release.

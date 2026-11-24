# INTEGRACIO_map – #48, valódi Dialog-viewport és kompakt GPS-térkép

## Állapot és eredet
- Kiindulás: `main` `ee826e6a744ae5e6332c020e0005134f7dc698b2`.
- Saját ág: `fix/gps-map-dialog-viewport-48`; PR cél: `main`. A végső merge és APK a koordinátor feladata.
- A #58 Coach-/naplótörlés-PR a munka indításakor nyitott, mainbe nem olvadt; annak fájljait nem módosítottam.
- A feladatban hivatkozott 2026-11-03 03:16-os két készülékkép nem része a repónak / itt nem állt rendelkezésre. Képi összevetés és készülékes elfogadás ezért nyitott.

## Kizárólagosan módosított fájlok
- `app/src/main/java/com/example/ui/screens/OutdoorGpsDashboard.kt`
- `app/src/main/java/com/example/ui/screens/ActiveMeasurementScreens.kt`
- `app/src/main/java/com/example/ui/components/AnatomicalBodyAndMapComponents.kt` (csak térképblokk/import)
- `app/src/main/java/com/example/ui/components/mapcontrols/MapAppearanceSelector.kt` (új, saját névtér)
- `app/src/test/java/com/example/OutdoorGpsLayoutRegressionTest.kt`
- `app/src/test/java/com/example/followup/mapcontrols/MapAppearanceSelectorRegressionTest.kt` (új, saját névtér)
- `docs/parallel-handoffs/INTEGRACIO_map.md`

## Megvalósított megjelenítési változások
1. A teljes képernyős megjelenítés `OutdoorGpsFullscreenDialog` hostot kap: `DialogProperties(usePlatformDefaultWidth=false, decorFitsSystemWindows=false)`; a tényleges Dialog-ablak `MATCH_PARENT × MATCH_PARENT`, azon belül `WindowInsets.safeDrawing`. Nem egy külső teszt-Box kap látszólagos insetet.
2. A teljes képernyős fejléc/X/idő, metrikák és Start/Szünet/Kör/Befejezés a mért, rendszersávok közötti ablakban maradnak. Az alsó metrikák saját korlátozott, görgethető sávot kapnak, az elsődleges gombok a footerben fixen hozzáférhetők.
3. A normál térkép 120–180 dp közé korlátozott, a függőleges teret inkább az adatoknak és a műveleteknek hagyja.
4. A GoogleMap a fejléc+footer tényleges pixelméretéből származó contentPaddinget kapja. Ha a térkép szabad magassága 180 dp alá csökken, a Google saját +/- gombjai elmaradnak, de a térkép gesztusos zoomja megmarad.
5. A térképtéma a két külön lebegő vezérlő helyett egy keretezett, átlátszatlan, kompakt választó: appkövetés, világos, sötét. A kiválasztást a meglévő `TrainPilotSettingsStore.setMapAppearance` menti; korábbi kézi beállítás megőrzött.

## Integrációs API és pontos bekötés
Az élő mérés hívását **már saját fájlban átvezettem**; közös `MainActivity`/`ViewModel` módosítás nem kell:

```kotlin
if (isFullscreenMapOpen) {
    OutdoorGpsFullscreenDialog(
        state = display,
        strings = strings,
        actions = actions,
        onDismissRequest = { isFullscreenMapOpen = false },
        mapContent = routeMap
    )
}
```

A `routeMap` a meglévő `LocalOutdoorMapContentPadding.current` értékét adja `NativeWorkoutRouteMapBox(mapContentPadding = mapPadding, routeKeys = listOf(routeOwnerKey), refreshToken = realGpsFixesCount, showZoomControls = mapPadding.calculateTopPadding() > 0.dp)` hívásnak. További paraméter/VM-mező nem kell, az eddigi `NativeWorkoutRouteMapBox` szignatúra és alapértékek változatlanok.

A mentett térkép közös `LogAndProgressScreens.kt` hívójának módosítása nem szükséges: az új választó a térképkomponensen belül, ugyanazzal a meglévő DataStore-beállítással működik.

A `back` csak az ablakot csukja; `pause`, `finish`, `start`, körök, `DistanceTrackingService`, útvonal/segmentek, natív ownership/finalization és backup **nem változtak**. Az anatómiai/óraikon és videóhívások változatlanok.

## Regressziók és bizonyítékok
- `OutdoorGpsLayoutRegressionTest`: meglévő kompakt, szűk, nagy betűs és insetes fixture; új tényleges `Dialog`-host elférés és X nem állítja le az edzést; szűk ablak + nagy betű.
- `MapAppearanceSelectorRegressionTest`: appkövetés → sötét → világos → appkövetés, külön callbackekkel.
- GitHub Actions teszt-SHA, run ID és **nyers XML számlálás**: PR Actions befejeződése után rögzítendő; a futó CI nem siker.
- Fizikai ellenőrzés szükséges: 03:16-os teljes/normál referencia, gesztusok, stílus-választó és Google-logó/zoom; 320–393 dp keskeny nézet, normál/nagy betű, rendszer navigáció/gesztussáv, valódi GoogleMap hardverréteg. Készülékes elfogadás nincs állítva.

## Nyitott, koordinátori teendő
A négy párhuzamos PR ellenőrzése/összefésülése, CI XML-jelentések, majd a meglévő állandó signerrel történő külön release-folyamat kizárólag a koordinátoré. Sem `main` merge, sem aláírt új APK nem történt ebben a feladatban.

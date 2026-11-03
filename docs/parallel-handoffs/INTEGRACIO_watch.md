# INTEGRACIO_watch – #60 óraadatok, külön fejlesztői átadás

## Határ és státusz

Forráság: `feat/watch-info-panels-60`, friss `main` alapból.
Hibajegy: https://github.com/fuloplevente1998/Native-AI-Studio/issues/60
A #58 PR a kiinduláskor **nyitott** volt. A közös képernyő/VM nincs módosítva; a koordinátor
a #58 és a többi független feladat megfelelő sorrendjében integráljon.
Ez a PR önállóan fordítható komponenst és tesztet ad, **nem a végleges telefonos bekötést**.

### Módosított fájlok (csak saját tulajdon)

- `wear/src/main/java/com/repforge/app/wear/WearDeviceInfo.kt`
- `app/src/main/java/com/example/model/watchinfo/WatchInfoCardData.kt` (új)
- `app/src/main/java/com/example/ui/components/watchinfo/StudioWatchInformationCard.kt` (új)
- `app/src/test/java/com/example/followup/watchinfo/WatchInfoCardRegressionTest.kt` (új)
- `docs/parallel-handoffs/INTEGRACIO_watch.md` (ez az átadó)

A meglévő `WatchDeviceInfoRegressionTest.kt` és a közös
`CalendarAndWatchUiRegressionTest.kt` módosítása nem szükséges.
`NativeWatchDeviceInfo.kt`, `WatchDeviceInfo.kt`, `MainActivity.kt`,
`TrainPilotViewModel.kt`, `HealthCoachAndSettingsScreens.kt`, Gradle/manifest érintetlen.

## 1. Adatút – pontos API

A telefon továbbra is kizárólag ezt használja:
`com.repforge.app.NativeWatchDeviceInfo.request(context)`.

Válasz: `NativeWatchDeviceInfo.Result(nodeName: String, data: JSONObject)`.
A válaszoló `WearDeviceInfo.reply()` kompatibilisen, opcionális
`"appBuild"` numerikus (valós PackageInfo longVersionCode/versionCode) mezőt is küld.
A régi óraválaszban nincs `appBuild` ⇒ `null`, a felület „—”-t mutat.
`battery` hiány / `JSONObject.NULL` / hibás / tartományon kívüli ⇒ `null`, nem 0%.
`model`, `os`, `appVersion` nem keménykódolt; tényleges Wear mezők.
A requestId változatlanul a korábbi `WatchDeviceInfo.requestId` / `accepts` ellenőrzéshez
kötött, és a telefonos listener ugyanazt a forrás-node-ot és REPLY útvonalat ellenőrzi.

Importok:

```kotlin
import com.example.model.watchinfo.WatchInfoCardData
import com.example.model.watchinfo.WatchInfoStatus
import com.example.ui.components.watchinfo.StudioWatchInformationCard
```

A DTO: `name: String?`, `model: String?`, `androidVersion: String?`,
`appVersion: String?`, `appBuild: Long?`, `batteryPercent: Int?`,
`connected: Boolean`, `appAvailable: Boolean`, `loading: Boolean`,
`status: String?`, `statusKind: WatchInfoStatus`, `lastSync: String?`.
Minden nullable mező `null`, a kapcsolati/busy mezők `false`,
`statusKind` `IDLE` alapértékkel indulnak. Nem lesz ismeretlen = nulla.

### Integrátor – TrainPilotUiState és requestWatchDeviceInfo()

A `TrainPilotUiState` órás mezői mellett vegyél fel kompatibilis alapértékű mezőt:

```kotlin
val watchInfoCardData: WatchInfoCardData = WatchInfoCardData(),
```

Az aktuális `requestWatchDeviceInfo()` legelején maradjon a meglévő
`if (_uiState.value.watchInfoLoading) return` tiltás. A meglévő loading-beállítással együtt:

```kotlin
watchInfoCardData = it.watchInfoCardData.copy(
    loading = true,
    status = "Óraadatok lekérése…",
    statusKind = WatchInfoStatus.IDLE
)
```

A meglévő `NativeWatchDeviceInfo.request(context)` **sikeres**, ellenőrzött válaszánál
a kapott valódi JSON alapján állítsd elő, az eredeti `nodeName` felhasználásával:

```kotlin
val time = java.time.LocalTime.now()
    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
val card = WatchInfoCardData.fromVerifiedReply(
    nodeName = result.nodeName,
    reply = result.data,
    connected = true,
    appAvailable = true,
    loading = false,
    status = "Óraadatok frissítve: $time",
    statusKind = WatchInfoStatus.SUCCESS,
    lastSync = time
)
_uiState.update { it.copy(watchInfoCardData = card /* régi kompatibilis mezők, ha szükséges */) }
```

A korábbi `watchDeviceDetails` összefűzött szövegét **ne bontsd vissza**; a
komponenshez ne azt add át. A régi mezőket más hívók miatt megtarthatod, de az
új kártya kizárólag `watchInfoCardData` mezőt használjon.

Timeout esetén a meglévő `TimeoutCancellationException` ág ugyanazt a
felhasználónak szóló hibát adja, de `watchInfoCardData.copy(status = ...,
statusKind = WatchInfoStatus.TIMEOUT, loading = false)` értéket is írjon.
Egyéb hiba `statusKind = WatchInfoStatus.ERROR`, a már létező
`error.message ?: "Az óraadatok nem kérhetők le."` szöveggel.
A `finally` ágban mindig legyen `loading = false` a DTO-n és a legacy
`watchInfoLoading` mezőn is. A hiba a régi érvényes adatot nem hamisítja meg;
a hibás frissítés állapota látható lesz a rendszerpanelen.

A `updatePlatformAccess(...)` meglévő connect-állapotában frissítsd a DTO
`connected` és `appAvailable` mezőit is (`watchNames.isNotEmpty()`, illetve
`companionReady`). Ha a kapcsolat eltűnt, vagy más eszköz került a korábbi helyére,
nullázd a korábbi modell/OS/verzió/build/akkumulátor/utolsó sikeres olvasás adatait:
új `WatchInfoCardData(connected = watchNames.isNotEmpty(),
appAvailable = companionReady, name = watchNames.singleOrNull())`.
Azonos, továbbra is elérhető node esetén a régi ellenőrzött adatok megtarthatók.
Ha több companion van, a request továbbra is hiba a meglévő natív védelmen át.

## 2. UI bekötése, pontos csere

`app/src/main/java/com/example/ui/screens/HealthCoachAndSettingsScreens.kt`,
`WatchAndSyncScreen`: az `Órakapcsolat` fejléce utáni jelenlegi
`watch_device_info_card` Surface-`item` és az utána álló külön,
keret nélküli `watchDeviceDetails` / `watchInfoMessage` /
szöveges `watch_refresh_device_info` gomb `item` **együtt** cserélendő:

```kotlin
item {
    StudioWatchInformationCard(
        data = uiState.watchInfoCardData,
        onRefresh = onRefreshWatchInfo
    )
}
```

A következő „Aktív edzés vezérlése” szekció és a teljes óra/edzés backend érintetlen.
Az új kártya maga is kattintható, és a címsorában a két görbe nyilas
`Icons.Default.Sync` ikon 48 × 48 dp-es `IconButton`, „Óraadatok frissítése”
hozzáférhető leírással. Busy állapotban mindkét frissítési út tiltott.
A külső oldalkeret `TrainPilotFrames.page`, a két belső Eszköz és Rendszer panel
`TrainPilotFrames.panelWidth` szerinti. A státusz és időtúllépés a
`watch_info_system_panel` **belsejében** szerepel.

Megőrzött teszt tagek: `watch_device_info_card`,
`watch_refresh_device_info`, `watch_device_info_status`.
Új tagek: `watch_info_device_panel`, `watch_info_system_panel`.
A képernyő szintű `onRefreshWatchInfo` az eredeti VM-kérést hívja.
Nincs új request parancs, új listener, edzés/Health- vagy ownership-út.

## 3. Ellenőrzés és elfogadás

Új saját regressziók:
`com.example.followup.watchinfo.WatchInfoCardRegressionTest`:
két panel és a JSON mezők; ismeretlen/null/érvénytelen akkumulátor és build;
egyszeri ikonfrissítés; betöltéskor tiltott kártya+ikon; timeout belül;
kártyára koppintás ugyanazon callback; „null” szöveg tiltása.
A meglévő `WatchDeviceInfoRegressionTest` továbbra is védi a requestId,
malformed/oversized és akkumulátortartomány működését.
A közös `CalendarAndWatchUiRegressionTest` az integrátori bekötés után
változatlanul vagy szükség szerint az integrátor által frissítve vizsgálandó.

CI: csak a valóban lezárult GitHub Actions build és a tényleges nyers JUnit XML
számít bizonyítéknak. Az itt átadott független PR létrehozása önmagában
**nem** fizikai készülékes és nem integrációs elfogadás.
Fizikai telefon + Wear APK-párral szükséges: egyetlen valódi kapcsolt node,
név/modell/battery/Android/TrainPilot tényleges válasz, offline/timeout,
újracsatlakozás, hiányzó akkumulátor, régebbi Wear (appBuild nélkül),
egyszerre többször tap, nagy betűméret/karcsú kijelző.
Új aláírt APK, verzióléptetés, merge és kiadás kizárólag a koordinátortól.

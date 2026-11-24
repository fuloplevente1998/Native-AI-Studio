# 5. chat – Integrátor, csak a négy önálló munka után

A friss main/átadó és a #58 állapotából indulj. A Coach rövid leírása és piros/listabeli törlés #58-ban készül, ne implementáld újra. A legutóbb kiadott APK 2743, a négy kiosztott munka abban még nincs benne.

A 01–04 chat saját fájlokat és komponenseket készít. A fő feladatod a külön PR-ek review-ja, egyesítése és a **csak neked fenntartott** közös fájlok bekötése. A fejlesztők nem írhatják át ezeket; ne szervezd vissza nekik ugyanazt a monolit fájlt több tulajdonossal.

## Közös fájlok – kizárólag te

- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt`
- `app/src/main/java/com/example/ui/screens/HealthCoachAndSettingsScreens.kt`
- `app/src/main/java/com/example/ui/screens/StudioTodayStatusGrid.kt`
- `app/src/main/java/com/example/ui/screens/HomeScreen.kt`
- `app/src/main/java/com/repforge/app/NativeWatchDeviceInfo.kt`
- `shared/src/main/java/com/repforge/shared/WatchDeviceInfo.kt`
- `wear/src/main/java/com/repforge/app/wear/WearDataListenerService.kt`
- `app/build.gradle.kts`
- `wear/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/AndroidManifest.xml`
- `wear/src/main/AndroidManifest.xml`
- `scripts/verify_android_apk_metadata.py`
- `app/src/test/java/com/example/CalendarAndWatchUiRegressionTest.kt`

A közös strings/theme/resource/gradle és aktuális átadó/munkalista módosításai szintén nálad maradnak. A 01–04 chat kizárólagos fájljaiba csak az átadásuk után, integráció során nyúlj; közben ne írj párhuzamos implementációt.

## Integráció sorrendje és szerződései

1. Gyűjtsd be a négy `docs/parallel-handoffs/INTEGRACIO_*.md` fájlt és PR-t. Ellenőrizd a fájllistát a FAJLTULAJDON.md szerint. Idegen fájlt módosító PR-t ne olvassz be automatikusan; helyezd át a bekötést a saját integrációs commitodba.
2. Saját integrációs ágban a négy kód együtt, aztán a közös bekötések. Új videófüggőség esetén egyetlen egyeztetett Gradle-változtatás; az eredeti pontos URL-feloldás és natív életciklusvédelem megmarad.
3. Óra: a Watch DTO a valódi `NativeWatchDeviceInfo.Result` node+JSON adataiból, a VM-ben; az új kétblokkos komponens váltja a régi kártyát és keret nélküli plusz mezőket. Sync ikon ugyanarra az egyetlen védett kérésre mutasson, megőrzött busy/nonce/node/timeout kezeléssel. Ne duplázd a lekérést a kártya és ikon callbackjéből.
4. Egészség: a napi/heti állapotmezők és DTO-k bekötése a HealthReadings helperbe, közös TodayGrid és heti panel. A mérés típusa/forrása és a hiány/engedély/SDK-hiba legyen helyes. Kanonikus Coach-readiness, ne párhuzamos kitalált képlet; meglévő demo score-override nem pótolhat hiányzó mérést. A Home és Health ugyanazt a kiválasztott provider-adatot mutassa.
5. Térkép: minden új defaultos paraméter és ha kell, saved-route caller bekötése nálad; tényleges Dialog/safe viewport és primer gombok fizikai tesztje külön szükséges. X nem edzéselvetés. A #58 listatörlése és Coach-preview, továbbá a 2743 számláló/százalék javítása megmarad.
6. Végleges közös forráson teljes telefon/Wear Actions, nyers XML és aktuális SHA. A külön PR-ek korábbi zöldje nem bizonyítja az összefűzött kódot. Jelöld a kódot, CI-t, APK-t és készülékes elfogadást külön.
7. Következő közös buildszámot a tényleges legfrissebb main/kiadás után válassz (2743 után várhatóan 2744; ne feltételezd ellenőrzés nélkül). Marad `com.TrainPilot.com`, `0.1.0-beta`, állandó signer. Kész/ellenőrzött javításból reviewzható PR, majd az engedélyezett main-integráció és teljes állandó kulcsos build. Nincs új signer, kulcskérés vagy Play/GitHub release.
8. Ellenőrzött, külön telefon/Wear APK a meglévő TrainPilot Native Drive mappába: `1ywHgrManynjV9uNvZfVncOmCr3SbHIb9`. Forrás-SHA, nyers XML, package/verzió, signer, fájlhash, Maps/pair és Drive méret/szülő readback. Átadó/munkalista/hibajegyek frissítve, fizikai következtetést csak tényleges próbából állíts.

Most csak kiosztás készül: a 01–04 feladat nem implementált. Alügynököt ne indíts külön userutasítás nélkül; a felhasználó külön chatekbe adja a fájlokat.

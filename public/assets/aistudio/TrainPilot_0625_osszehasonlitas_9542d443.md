# TrainPilot – AI Studio 06:25 export összehasonlítás

**Dátum:** 2026. október 28.  
**Audit típusa:** statikus forráskód-összevetés, ZIP-integritás és korábbi, GitHub Actionsből származó 05:42 forrásokkal végzett diff. **Nem futott APK-build, emulátoros teszt vagy telepített frissítés.** A PR #270-et nem módosítottuk e vizsgálat során.

## Vizsgált források

- **Új, 06:25 ZIP:** `trainpilot-oct-28-2026-6-25-49-am.zip`, 8 867 425 bájt, SHA-256 `f53b51e8104fb07a53ed4c23a6999cd42b90a3ad15821a8af846e1c5dfa753cd`; 479 ZIP-bejegyzés, 379 fájl, CRC PASS. A fájl feltöltött példánya ellenőrizve; GitHub-fastruktúra szerint ugyanilyen méretű fájl van a `Native-AI-Studio/main` ágon.
- **05:42 ZIP:** `trainpilot-oct-28-2026-5-42-24-am.zip`, 8 861 301 bájt, SHA-256 `8f91e79d604de2415ee57bd6094c453f1512a0f2b1c93caf354da31f75b6a308`. A három korábban módosított Kotlin-fájlt a 05:42 GitHub Actions forrás-auditjából vettük.
- **21:02 ZIP:** `trainpilot-oct-27-2026-9-02-00-pm.zip`, 8 846 684 bájt; SHA-256 `db5ce026e2559366a7d3366332ab64affef7448839a7727f0dbfce447d7d65d9`.
- **Jóváhagyott UI-vizuál:** az eredeti **18:49-es** AI Studio export. Kivétel: **az eredeti arany súlyzó launcher-vektorja**, illetve az eredeti, részletes natív izomábra.
- **PR #270:** a kanonikus natív motor, adatbázis, migráció, Wear OS, GPS, backup, valódi beállítástároló és aláírási/frissítési munkafolyamat az irányadó.

## Kódeltérések

06:25 a 21:02 exporthoz képest: **13 változtatott fájl, 0 új, 0 törölt.** Az eredeti 18:49-es Studio 16 Kotlin-fájlból **mind a 16 jelen van, 5 byteazonos**; a többi Studio-fájlban változott kód vagy szöveg. A natív `com.repforge.app.MainActivity.kt` **bájtazonos a 05:42 auditban rögzítettel**. A migrációs olvasó és a Studio ViewModel már módosult a 05:42-eshez képest.

## Funkcionális állapot

| Terület | 06:25 tényleges forrás | Ítélet |
|---|---|---|
| Régi adatok migrációja | `LegacyLocalStorageReader` 60 másodperces időkorlátot használ; rendererhiba esetén `resumeWithException`, nem `emptyMap()`. `MainActivity` megtartja a `migrationReady`, error, retry és recovery ágakat. | **Javult, de régi adatokkal még nem tesztelt.** |
| Edzésmentés | A `finishAndSaveWorkout` gyakorlat-/sorozat-JSON-t épít, `LegacyWebViewMigrator.saveWorkoutJsonToRoom` segítségével ment, majd Roomot olvas vissza, és csak ezután jelez sikert. | **Nagy előrelépés, de nem a kanonikus edzéslezáró motor.** |
| Edzésadatok pontossága | Csak az épp kiválasztott gyakorlatot menti; ha nincs teljesített sorozat, a nem teljesítetteket is elmenti. Az időtartam a `timedCurrentSec` alapján származtatott és minimum 1 perc; a program és nap részben művi. A teljes volument sorozatonként egészre vágva összegzi. | **További javítás szükséges.** |
| Edzéséletciklus | Room-mentés után `ActiveWorkoutStore.clear(ctx)`, de nem látható a kanonikus `TrainPilotWorkoutFinalization` vagy a `PhoneWorkoutExecution` tulajdonos- és lezárási protokolljának hívása. | **Adat-/session-konzisztencia kockázata.** |
| Testsúly | Határellenőrzés, Room upsert, `getWeightJournalSummary` visszaolvasás, majd siker/hiba UI. | **Valódi funkció kezdeménye.** Restore guard és ütközési/async állapot teszt szükséges. |
| Health | A mesterséges `+120` lépés megszűnt. `HealthBackgroundJob.schedule`, majd az egészségnapló jelenlegi vetületét olvassa. | **Nem igazolt friss szinkron:** az ütemezés nem egyenlő provider-sikerrel; a UI ennek ellenére sikert jelezhet. |
| Health mezők | Az új ViewModel `sleepDurationSeconds` kulcsot keres, míg a natív `HomeViewModel` `sleepMinutes` kulcsot használ, és a natív kód másutt nem említi a `sleepDurationSeconds`-et. A hiányzó mérésnél a Studio-alapérték is megmaradhat. | **Nagy valószínűséggel hibás alvásmegjelenítés / demóérték.** |
| Wear | Aktív edzést ellenőriz, de csak a Watch home snapshot publikálását hívja; a helyi `phoneControlsWorkout` flag átáll, nincs natív handoff-ACK. | **Nem valódi átadás.** |
| Studio vizuál | Eredeti arany súlyzóvektor beépült a fejlécbe, eltávolítva a DEMO módválasztó és fejléc DEMO-kitűző. | **Javulás; pixelpontos vizsgálat nem készült.** |
| Natív izomábra | Valódi `NativeMuscleLoadVisual` renderelő használata. | **Rajz valódi, adatok nem:** `AnatomicalMuscleHeatmapCard` belül továbbra is 45/66/59/75/100/61% mintaszámokat szorzóval alakít át. |
| DEMO-adatok | A `TrainPilotUiState` továbbra is `DemoDataProvider.defaultPrograms`, `initialLoggedWorkouts` stb. állapotból indul. | **Nem éles adatmodell.** |
| Backup, szinkron, fotók | Több DEMO címkét egyszerűen eltávolítottak; például mentés, visszaállítás, Google-szinkron, jogosultságok, kamera/galéria gomboknál `onShowBanner("…siker…")` maradt valódi művelet helyett. | **Kritikus hamis siker-visszajelzés.** |
| Telepítés és Wear APK | A ZIP Gradle `applicationId = "com.aistudio.trainpilot.tpux01"`, és `settings.gradle.kts` csak `:app` modult tartalmaz. | **Nem frissíti a kiadott TrainPilotot; nincs külön Wear alkalmazás buildje.** |
| Tesztek | Három generikus Example-teszt, a Robolectric-teszt minimális string-assert; saját ADB teszt **NOT RUN**, AI Studio beszámolója állít `assembleDebug` PASS-t, független buildlog/APK nem áll rendelkezésre. | **A build és valódi működés nincs függetlenül igazolva.** |

## Mit kell tovább kérni az AI Studiótól?

1. **Ne csak távolítsa el a DEMO feliratokat:** a backup, restore, fotó, programtörlés, célok, Health-jogosultság, Google-szinkron mind a natív műveletek tényleges siker-/hibaeredményét használja.
2. Az edzés lezárását a **kanonikus `TrainPilotWorkoutFinalization`/`PhoneWorkoutExecution`** folyamatba kösse. Ne migrátoron keresztül szimulált egysoros edzést mentsen; több gyakorlat, valódi idő és összesített sets adatok, ownership és backup/restore-védelem kell.
3. Törölje az éles kezdőállapotból a `DemoDataProvider` napló-/egészség-/program-mintaadatait, és kizárólag natív Room/Health/Coach bemenetekből dolgozzon. A hiányzó adatok `—` vagy valódi üres állapot legyenek.
4. A Health-szinkron csak **provider-visszaigazolás és adatfrissülés** után jelentsen sikert; egyeztesse a mezőneveket (`sleepMinutes`). A Wear-handoff kérés és tényleges ACK között tartsa a függő állapotot.
5. A már jóváhagyott **18:49-es** Studio UI-kártyákat, öt felső lapot, 32 palettát, arany súlyzó launcher-logót és eredeti anatómiai grafikát vizuálisan őrizze; anatómiai izomterhelési százalékokat valódi natív progress query adjon.
6. Az AI Studio sandboxcsomagjával ne állítsa, hogy a telepített `com.repforge.app` frissíthető. Valódi teszthez egyező app identity, aláírási kulcs, Phone+Wear modul és exact-source APK/ADB/signed-upgrade bizonyíték kell.

## Összkövetkeztetés

**A 06:25-ös ZIP a 05:42-eshez képest jelentős technikai előrelépés**, főleg a Room-visszaellenőrzés és a mesterséges Health-lépésnövelés megszüntetése miatt. **Nem kész natív kiadás**: továbbra is vannak minták, helytelenül jelentett sikeres műveletek, és a natív edzés-/Wear-végrehajtási protokoll nincs teljesen bekötve. A PR #270 natív motorját ez a ZIP nem helyettesítheti; csak ellenőrzött UI-, adatprojekció- és hibakezelési megoldásokat szabad átvenni.

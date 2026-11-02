# TrainPilot Native – aktuális AI Studio átadás, 2026-11-02

Ez az elsődleges checkpoint. A 2026-11-01-es átadó és a korábbi PR-leírások történeti háttérként használhatók; a kulcsok, Maps, tesztszám és APK-k tekintetében az alábbi állapot az irányadó.

## Projekt és ellenőrzött forrás

- Folytatandó repo: https://github.com/fuloplevente1998/Native-AI-Studio, **a legfrissebb main**.
- Utolsó validált alkalmazáskód: main **`977cdd5a09f41f4f92a6eaa21c8f83ea84492679`**, PR #15 merge.
- PR #15 forrás-HEAD: `337d48234ce2f06d97892e9c7651ae89ce81cf4c`; a forrás és a main teljes Git-fája azonos: `33176ff07800b25360c06081ea58dbc78f5af409`.
- Ezután az átadó dokumentáció külön PR-ban frissülhet; a következő agent ellenőrizze az aktuális main HEAD-et és a checkpoint óta érkezett kódváltozásokat. A dokumentáció frissítése nem új APK-build.
- Audit: [PR #1](https://github.com/fuloplevente1998/Native-AI-Studio/pull/1). A már megvalósított pontokat ne készítsd újra.
- Bemásolható feladat: [AI_STUDIO_CONTINUE_PROMPT.md](AI_STUDIO_CONTINUE_PROMPT.md).

**Aktuális felhasználói döntés: most kizárólag a natív app és a hozzá tartozó natív Wear app folytatódik.** A régi mainnel közös órás alkalmazás későbbi feladat. Alügynökök indítása most nem kért.

## Megőrzendő szerződések

- Meglévő Kotlin/Jetpack Compose Studio alkalmazás; grafit/arany UI, eredeti arany súlyzó logó, natív anatómia és felső navigáció maradjon. Nincs új webes/WebView helyettesítő app.
- Telefon és óra applicationId: **`com.TrainPilot.com`** (kis-/nagybetű számít). VersionCode **2736**, versionName **`1.3.71-native-test`**; ne változtasd meg önkényesen.
- Egyetlen meglévő natív edzésmotor, Room/DataStore, GPS- és Health-tárolók. Ne építs párhuzamos adatbázist/motort.
- Nincs fake/demo Health-, Wear-, GPS-, kalória-, pulzus-, akkumulátor- vagy sikerjelzés. Ismeretlen adat nem helyettesíthető kitalált számmal.
- Ownership/finalization, pending átadás, installed ACK, backup/restore generáció, tombstone, migráció, staging/redo és adatvédelmi kapu maradjon érvényes. Felhasználói adat vagy recovery receipt nem törölhető hibakerülésként.
- A helyi munkát őrizd meg. Külön javítóág/PR, érdemi kódváltozás után Actions és nyers teszteredmény; tesztet ne hagyj ki/ne gyengíts. Automatikus Play/GitHub release-publikálás nem engedélyezett.

## Legutóbbi igazolt build és letöltés

**Állandó saját kulcsos phone/Wear release-pár:** [Actions 41410559088](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41410559088), **SUCCESS**. A checkout a fenti `977cdd5` main commit.

Letöltött nyers XML: **248 teszt = 79 telefon + 169 Wear; 0 failure, 0 error, 0 skip**. A shared task NO-SOURCE; közös kódját a telefonos/órás tesztek gyakorolják. Az alkalmazásváltozatok és JVM-tesztek sikeresek; ez nem fizikai készülékes elfogadás.

| Csomag | Méret | APK SHA256 |
|---|---:|---|
| Telefon release | 19 398 407 bájt | `cf82e74c5463b8cf01dd6dd18f2f52018030491cb0db320acbe26412d6b1def9` |
| Natív Wear release | 4 636 867 bájt | `011a77223b26cd1bb9975a66b2a896c22d1d372817c361d1f3cb194fdb8249b6` |

- [Telefon APK, ZIP nélkül](https://drive.google.com/file/d/13lgopg5hVnXrNlB7DrhQbFSYoTFHzBix/view)
- [Natív Wear APK, ZIP nélkül](https://drive.google.com/file/d/1khG7CuDJhiaEwcRMu5VXnJInuiOzsLF4/view)
- [Eredeti APK-pár és metadata artifact](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41410559088/artifacts/13125607634)
- [Nyers tesztjelentés](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41410559088/artifacts/13125523153)
- Előző teljes PR-CI: [41410163497](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41410163497), szintén 248/248.

**Minden további APK a [TrainPilot Native Drive-mappába](https://drive.google.com/drive/folders/1ywHgrManynjV9uNvZfVncOmCr3SbHIb9) kerüljön.** Mappaazonosító: `1ywHgrManynjV9uNvZfVncOmCr3SbHIb9`. Ne a Drive gyökérbe töltsd fel. A fájlnév tartalmazza a telefon/óra megkülönböztetést és a forrás rövid commitját. Őrizd meg a régi fájlokat és linkeket; ellenőrizd a szülőmappát, méretet és hash-t.

## AI Studio main-frissítés átnézése, 2026-11-02

A felhasználó kérésére átnézett új alkalmazásforrás: **`256e4536ee3c3a94bc5c78b93a33259bb2b15657`** (`feat: enhance cloud sync, equipment UI, and coaching`), 10 fájl, +1623/−214 sor. A fenti 977cdd5 a korábbi validált checkpoint. Az új forrásból indított [Actions 41434365835](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41434365835) a felhasználó új, két hibát előbb javító kérése miatt le lett állítva; ez nem igazolt új build. A javítások [PR #18](https://github.com/fuloplevente1998/Native-AI-Studio/pull/18) alatt készülnek; a végleges javított forrás és signed bizonyíték a buildblokkban lesz.

A frissítés a meglévő SettingsViewModel/GoogleSyncRepository útvonalat használó Google-bejelentkezési, Drive- és Naptár-kezelő kártyát, felszerelésválasztást/szűrést, napi/heti Health-összesítést és naplóalapú fejlődési statisztikát vezet be. A release signing konfiguráció, appazonosítók, verziók és a PR #15 alapvető mentési/adatvédelmi javításai változatlanok. A PR #16 három forráskártyája **nem része ennek a main commitnak**; a már elkészült ágat őrizd meg, és a friss mainnel együtt ellenőrizd integráció előtt.

A `256e453` kódáttekintésekor talált pontok, amelyeket a felhasználó kérésére a #18 javít:

- `LogAndProgressScreens.kt`, `MainStatisticsRecordsAndCardioSection`: a „Heti kilométer” az összes `loggedWorkouts` távolságát összegzi dátumszűrés nélkül; a „7 napos átlag” csak a pozitív lépésszámú napokkal oszt. Az intervallumot/nevezőt a felirat jelentéséhez kell igazítani.
- A heti térfogat/távolság oszlopok ma kezdődő utolsó heti ablakkal készülnek; a dátum nélküli rekordokat az utolsó hétbe sorolják. A `savedStartIso.take(10)`/`startsWith(dayKey)` UTC ISO-időből helyi naphoz rendelés nélkül számol. Tisztázd a naptári vagy gördülő heti határt, használj helyi zónába konvertált időpontot, ismeretlen dátumot ne rendelj önkényesen a mai héthez.
- `TrainPilotViewModel.refreshAfterCloudSync()` ismét `bindNativeEnvironment()`-et hív, amely új hosszú életű Flow-gyűjtéseket indít a meglévők megtartásával. Tegyél egyszerivé/lecserélhetővé a környezetkötést, vagy használj külön adatfrissítést; ismételt képernyőmegnyitás/szinkron ne sokszorozza az adatfigyelőket. Ezt célzott regresszióval ellenőrizd.

A felhasználó az áttekintés után kifejezetten a két hiba javításával együtt kéri az APK-t. A #18 az utolsó hét helyi napját használja a heti kilométerhez és oszlopokhoz; a nulla lépéses napokat is számolja az átlagban; a napi edzésszámhoz is helyi dátumot használ. A felhőfrissítés már csak adatot tölt, az explicit újrakötés pedig lecseréli a korábbi Flow-figyelőket. Három dátumteszt és egy valódi Room-frissítést/élő feliratkozást/hozzájárulás-változást ellenőrző regresszió egészíti ki a kaput. Ezek eredményét és a main merge-et a végleges buildblokk rögzítse; a fizikai Google/Samsung működés továbbra is készülékes ellenőrzés.

## Folyamatban lévő, új felhasználói UI-kérés

[PR #16](https://github.com/fuloplevente1998/Native-AI-Studio/pull/16): a naplóban három külön lenyitható kártya legyen **TrainPilot WearOS → Samsung Health → Health Connect**, alapból csukva, saját forrásadatokkal és saját lekérő/engedélykérő felülettel. A kód és regressziók elkészültek, a külön ág `6f9f11ee9c23c1e1a5204bcc5842b1dd21150fed`. Az [Actions 41413776118](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41413776118) SUCCESS; a letöltött nyers XML szerint **249 teszt, 0 failure/error/skip** (artifact `13126373991`). Ez még a 977cdd5 alapra épült, a későbbi AI Studio mainnel és #18-cal együtt nincs validálva. PR #16 draft/open, a mainbe még nem került be; integráció után friss CI és APK szükséges.

A legutóbbi felhasználói képen a natív app órás panelje már 403,6 kcal-t (a régi UI-ban kerekítve 404), 101,6 bpm átlagot és 3627 mintát mutatott. Ez készülékes visszajelzés az adott rekord órás adatának megjelenéséről; nem általános fotó/összes Samsung/Google funkció elfogadás. A saját fotók visszaállításáról új visszaigazolás még nem érkezett.

## Aláírás és Google-beállítások – már nem hiányzó alapkonfiguráció

A tulajdonos a saját Windows-gépén új állandó JKS-t generált. A privát kulcs és jelszavak nem kerültek chatbe/Gitbe/artifactba. **Ne generálj új kulcsot, és ne írd felül a meglévőt.**

Mindkét legutóbbi APK tényleges nyilvános signer:

- SHA1: `CD:2D:A9:EA:35:23:3F:7A:68:D8:F5:CE:07:4A:9A:21:68:95:19:3C`
- SHA256: `a72fd016d94e9d94e2b2282032f8c3820d4f09cba9ff1db05c3fd1de6ffa7dd6`

Ez egyezik az előző állandó kulcsos natív build signerével. Az ilyen natív telepítés frissíthető adatmegőrzéssel. A régi ideiglenes CI-debug vagy más AI Studio signerű telepítéshez ez nem bizonyít frissíthetőséget; uninstall/adattörlés nem hibakerülés.

- Manuális workflow: `.github/workflows/trainpilot-persistent-signing.yml`, csak mainről, **`android-signing`** environmenttel; a dispatch pontos commitját építi.
- Environment-secret nevek: `TRAINPILOT_RELEASE_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`, `TRAINPILOT_RELEASE_CERT_SHA256`. Csak a nevüket kezeld, értéküket ne olvasd ki/naplózd/commitold.
- Maps: `TRAINPILOT_MAPS_API_KEY` repository-secret; a workflow `MAPS_API_KEY` fallbacket is elfogad. A legutóbbi signed metadata **mapsApiKeyConfigured=true**. A régi „kulcs hiányzik” státusz történeti.
- A meglévő Gradle release signingConfigs `KEYSTORE_PATH`/jelszó környezeti változóit használjuk, alias `upload`. Telefon és óra egy futásból, azonos signerrel. A runner privát kulcsfájlja always lépésben törlődik; siker nélkül APK-upload nincs.
- Az ordinary PR debug CI továbbra is ideiglenes tesztkulcsos; ezt ne keverd az állandó kulcsos készülékes párral.
- [Beállítási útmutató](ANDROID_KEYS_SETUP.md); a segédet ne futtasd újra új kulcs létrehozására.

A felhasználó új TrainPilot Google Cloud projektet hozott létre, Maps/Drive/Calendar API-t engedélyezett, Android OAuth-klienst állított be a `com.TrainPilot.com` + fenti SHA1 pároshoz. A Calendar engedélyezését külön visszaigazolta. Branding/Testing/Production státusza nem lett külső eszközzel ellenőrizve. A Branding kitöltése nem automatikus produkciós/verified státusz.

A natív Google-kód AuthorizationRequesttel ezeket használja: `openid`, `userinfo.email`, `userinfo.profile`, `https://www.googleapis.com/auth/drive.appdata`, `https://www.googleapis.com/auth/calendar.app.created`, `https://www.googleapis.com/auth/calendar.calendarlist.readonly`. Külön naptár-API-kulcs vagy appba tett OAuth client secret nem kell. Tényleges bejelentkezés, engedély, Maps-csempe, Drive-mentés/visszaállítás és saját naptárszinkron még készülékes ellenőrzés.

## Már elkészült változások – ne kezdd újra

| Terület | Állapot / releváns forrás |
|---|---|
| Natív alapműködés | Korábbi #2–#10: tartós profil/terv, stopper/szünet, aktív időablak, kültéri befejezés/GPS, naptár, tényleges engedélykérés, backup-adatvédelem, natív Wear `:wear` / közös `:shared` integráció. Részletes történet: [korábbi átadó](AI_STUDIO_HANDOFF_2026-11-01.md). |
| Backup OOM / telefonra visszaadás | BackupJsonIO streamelt staging/replay; Health darabolt SQLite redo/rollback, nincs teljes napló másolata legacy:nativeHealthJournal Room TEXT-ben. NativePhoneReturnInstallation: installed receipt → ARM → paused timer install → editorprojekció; kézi suspended-flag reset tilos. |
| UI / napló / kézi súly | #12: kompakt fejléc, Fejlődés/Napló navigáció, ±0,5 kg kézi súlylépés (Coach/progresszió szabályait nem módosítja), egységes kisebb X 48 dp tap-céllal, összecsukott témák és erősebb közös témakeret. |
| HC / közvetlen Samsung | #13: explicit forrásválasztó; napi csatornák és provider-workout snapshotok elkülönítve; nincs Samsung→HC fallback. Tartós siker/részleges/hiba/blokkolás visszajelzés; valódi grantpróba; mintagörbék/mintanapok eltávolítva. Élő edzésen szív gomb csak olvasási lekérés, nem átadás/szenzor-/naplóírás. |
| Állandó aláírás | [#14](https://github.com/fuloplevente1998/Native-AI-Studio/pull/14): tulajdonosi kulcs és manuális, ellenőrzött phone/Wear workflow, már sikeresen futtatva. |
| Fotók / régi Wear / hozzájárulás | [#15](https://github.com/fuloplevente1998/Native-AI-Studio/pull/15): alábbi részletek; mainen és a legutóbbi signed APK-ban. |

### A legutóbbi felhasználói hibajelentés és #15

A felhasználó korábbi TrainPilot/RepForge ZIP-et állított vissza az új natív appba. A képeket nem látta; egy 2026-10-29-i edzés órás 404 kcal-ja is hiányzott. Később pontosította: a két app **Health Connect értékei egyeznek** (8 aktív / kb. 72,2 teljes kcal), a **404 kcal az óra Health Services mérése**, 101,6 átlagpulzussal, 3627 mintával. Ezt nem szabad Samsung/HC értékkel helyettesíteni vagy összeadni. A tényleges ZIP-et nem kaptuk meg; a Health-mentési kapcsoló állapotáról adott visszajelzés nem a ZIP bájtjainak vizsgálata.

Bizonyított kódhibák/javítások:

1. A régi webes app `healthWear129` mezőt tárolt; a natív mapper csak `wear`/`healthSummary` aliasokat olvasott. **NativeSavedWearHealth** új importnál vetíti a régi mért adatot, és már importált raw-only rekordból is olvas, forrás/schema/workout-alias ellenőrzéssel. Napló, forrásjelző és watch mininapló közös helperét használják. Room-sémaváltás nincs.
2. A Studio fotóoldal még mintakártyát és valós mentés nélküli sikerüzenetet használt. **StudioPrivatePhotos → ProgressViewModel → NativeProgressPhotoData/Gallery** már a Room-fotóindexből és `workout_photos/<id>.jpg` fájlokból jelenít meg valódi képet, teljesképernyős nézettel. Naplóból az adott workoutId-ra szűr, beállításokból teljes galéria. Ez **csak olvasási galéria**; új kép létrehozása/csatolása ezen a Studio-oldalon még nem kész funkció. Ne hozz vissza hamis sikerjelzést.
3. A Studio backup Health-kapcsoló csak memóriában változott, true alapértékkel; az exportnál a teljes napló és a workout-health eltérően volt szűrve. Most a meglévő **DataStore healthBackupConsent** tartós állapotát kezeli, alapból false.
4. JSON/ZIP hozzájárulással a helyben tárolt teljes natív egészségnaplót (Samsung csatornát is) és HC/Samsung/Wear workout-összesítéseket tartalmaz. Nélküle az egészségadat-aliasok kimaradnak, a helyi adat és fotó megmarad. **Drive a workout-összesítéseket szinkronizálja, a teljes napi provider-naplót nem**; ezt a felirat külön kimondja.
5. Opt-out Drive-projekció nem küld egészségadatot. A helyi install másolatába azonos workout/időablak ellenőrzéssel visszailleszti a saját helyi cache-t. Egy remote snapshot nem kapcsolhatja be a helyi megosztási hozzájárulást. Korábbi exportokat/felhőmentéseket a kapcsoló nem töröl.

Fő fájlok: `NativeSavedWearHealth.kt`, `LegacyWebViewMigrator.kt`, `StudioWorkoutHealth.kt`, `JournalHealthPanel.kt`, `WearSyncRepository.kt`, `StudioPrivatePhotos.kt`, `NativeProgressPhotoGallery.kt`, `TrainPilotViewModel.kt`, `NativeBackupPrivacy.kt`, `NativeCloudMergeGuard.kt`, `GoogleSyncRepository.kt`. [Részletes munkaleírás](LEGACY_RESTORE_PHOTOS_WEAR_2026-11-02.md).

## Bizonyíték és korlát

- **Automatizáltan igazolt:** LegacyRestoreEvidenceRegressionTest 6/6: valódi JPEG-gel régi ZIP → restore → bájtegyezés → Room-index → dekódolt, látható kép → teljesképernyős nézet; régi órás és HC érték külön, raw-only fallback, alias/idegen forrás/ID ellenőrzés; consent false/true × JSON/ZIP, Samsung napi/workout adat, fotó- és helyi adatmegőrzés, egészségadatmentes Drive-projekció/local-only cache és eltérő ablak elutasítása.
- **Korábbi sikertelen körök rendezve:** hiányzó Flow import; kattintható képkártya összevont semantics-fájának tesztelése; PAUSED Robolectric főszálon várakozó Room-UI callbackek. A teszt az unmerged képelem valódi láthatóságát ellenőrzi és a szüneteltetett Android loopert futtatja. Nincs timeoutnövelés, skip vagy lazított adatfeltétel. A végleges PR és signed kör is 248/248.
- **Még nem bizonyított:** a felhasználó saját ZIP-ének tartalma és a legújabb APK fizikai elfogadása. A valódi app/process kill nem azonos a Robolectric/hibainjektálással. A képernyőképek számai csak teszt-fixture adatok, nem termékbe égetett mérések.
- **Közvetlen Samsung SDK:** a naplós Wear-import javítása nem bizonyítja a Samsung SDK fizikai olvasását. SDK/app/cert regisztráció és valódi grant/hibakód alapján vizsgáld; HC-re ne ess vissza csendben.

## Folytatás: prioritás és konkrét készülékes menet

1. **P0 – a legutóbb átadott natív APK elfogadása.** Először frissítés után, újraimport nélkül nézd meg ugyanazt az edzést: órás panel, pulzus/energia, és a helyi fotók. A már megmaradt raw adat/fájl így visszanyerhető. Ne írj felül újabb helyi adatot újraimporttal bizonyíték nélkül. Ha még hiányzik, vizsgáld a tényleges backup/row mezőt, aliasokat, fotómeta/fájl egyezést, tombstone-t és a hibajelzést; ne gyárts értéket képernyőképből.
2. **P0 – adatvédelem a valós UI-n.** A hozzájárulás most a tényleges beállítást tükrözi, alapból off. Bekapcsolva készíts JSON/ZIP-et mindhárom health-forrással, ellenőrizd a Samsung csatornát és provider snapshotokat. Kikapcsolva export ne tartalmazza őket, helyben maradjanak meg. Drive-on csak a dokumentált workout-scope; ne sugallj teljes napi ledger-backupot.
3. **P0 – edzésfolytonosság.** Súlyzós/idős/kétoldali/kültéri edzés indítás, képernyő elhagyása, szünet, folytatás, befejezés, Room-mentés. A 10:00–10:25 + 16:00–16:20 eset pontosan 45 perc; a szünet egészségadatai kizárandók.
4. **P1 – valós Google integrációk.** Maps-csempe és valódi GPS-útvonal; Google bejelentkezés és tényleges Drive appdata mentés/visszaállítás; saját kezelt naptár létrehozása/szinkronja/hiba. API-kulcs jelenléte, API-engedélyezés és Branding nem sikeres adatszinkron. Testing módban a telefon fiókja legyen Test user.
5. **P1 – Galaxy Watch7.** Natív pár capability/ACK, telefon→óra→telefon, explicit Folytatás, utána mentés, kapcsolatbontás és szenzorengedélyek. A node-kapcsolat nem átadási ACK. Függő vagy sérült tulajdonosi journal nem oldható fel flagátírással/időtúllépéses resetekkel.
6. **P1 – új fotó csatolása.** A Studio galéria olvasása már valós. Új kamera/picker művelethez a meglévő WorkoutPhotoRepository és natív cél-workout/Room írási szabályok használhatók; nincs második fotótár és nincs siker tényleges tárolás előtt.
7. **Nyitott videóhiba:** [Issue #11](https://github.com/fuloplevente1998/Native-AI-Studio/issues/11): a gyakorlat videója üres/nem indul. Rögzítve, tényleges lejátszás nem javított/igazolt. Ne állítsd késznek egy YouTube keresőlinkkel vagy üres előnézettel.
8. **PR #1 maradék audit / UI:** friss main alapján ellenőrizd a még nyitott pontokat; normál telefonméreten a fő vezérlés legyen látható, nagy rendszerbetű mellett elérhető. Nincs széles dizájn-/motorkicserélés.

## GitHub Actions munkamenet

Fejlesztői/PR-kapu, JDK 17 + Gradle 9.3.1 + Android 36.1:

```text
:app:assembleDebug :app:testDebugUnitTest
:wear:assembleDebug :wear:assembleOptimized :wear:testDebugUnitTest :shared:testDebugUnitTest
```

Készülékes állandó kulcsos pár a mainből, a meglévő manuális workflow-val:

```text
:app:assembleRelease :wear:assembleRelease
:app:testDebugUnitTest :wear:testDebugUnitTest :shared:testDebugUnitTest
```

Olvasd a nyers XML-t, ne csak az általános hiba végét. Nagy natív JVM/Compose kódnál a Gradle-lépés percekig nem ír új sort; ez nem automatikusan emulátor/cache-hiba. Tartsd meg a checksumos Robolectric SDK prefetch-et; korábbi Maven 429-et így kezeli. Nincs helyi Gradle/emulátoros kapukerülés: a teljes ellenőrzést Actionsben végezd.

Minden eredményhez rögzítsd: pontos forrás-HEAD és CI-checkout, szükség esetén a faegyezés, tesztösszeg/fail/error/skip, verzió/appId, APK SHA256 és tényleges signer, Drive-linkek és készülékes korlátok. Ne keverj régi és új futások csomagjait. A legfrissebb mainbe további appkód csak megfelelő ellenőrzés után kerüljön; a felhasználó korábbi engedélye alapján a kész, ellenőrzött javítások PR-on át mainre kerültek. Új destruktív lépést vagy publikálást ez nem engedélyez.

## Régi app / órás kompatibilitás – most halasztva

A felhasználó felvetette az egy órás appot a régi és a natív telefonhoz, majd kifejezetten a natív folytatását kérte. A régi TrainPilot main ellenőrzött `5f026a56b9b6d33c84afa77eb3927b11f0bd6f6a` commitjában telefon/óra `com.repforge.app`, az új natív párban `com.TrainPilot.com`. Wear Data Layerhez azonos package és signer szükséges. A mostani APK-ról ne állítsd, hogy mindkettővel közvetlenül kompatibilis. A régi app/azonosító/kulcs átírása vagy új transport külön megtervezendő feladat, most nem része a munkának.

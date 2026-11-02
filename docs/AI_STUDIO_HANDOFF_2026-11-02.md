# TrainPilot Native – aktuális AI Studio átadás, 2026-11-02

Ez az elsődleges checkpoint. A 2026-11-01-es átadó és a korábbi PR-leírások történeti háttérként használhatók; a kulcsok, Maps, tesztszám és APK-k tekintetében az alábbi állapot az irányadó.

## Aktuális folytatás – #22, béta 2738

A [#22](https://github.com/fuloplevente1998/Native-AI-Studio/pull/22) mainre került: **`b96982dd62212d53f80ee1af366d77bfaacd439d`**. Innen folytasd az AI Studio-munkát. Az alábbi #20 / 2737 blokk történeti checkpoint, a #22 módosításait ne valósítsd újra.

- A Fejlődés harmadik füle **Statisztikák**, az önálló Erő és 1RM helyett. Két alfül: Személyes rekordok / Kardióstatisztika. A rekordlista a `PersonalRecordCalculator226` meglévő kanonikus akkumulátorát használja, lapozott skalár Room-lekérdezéssel, törölt és befejezetlen sorozatok nélkül. Súly, ismétlés, idő és táv a tényleges mérési típusnak megfelelően jelenik meg. A becsült 1RM a súlyos gyakorlat részleteiben marad. Nincs katalógusból származó mérési minta.
- A kardió heti grafikonja, tempója/ideje és azonos távú rekordjai a meglévő `ProgressViewModel` / `NativeCardioProgressPanel` motorból jönnek. A napi aktivitás kétoszlopos kártyái és napi sorai a kiválasztott provider 14 napi cache-ét használják; a hét naptári hét. A korábbi gördülő heti célösszeg továbbra is csak 7 napból számol. Ismeretlen lépés vagy nem elkülöníthető edzésen kívüli rész: **—**. Nincs szolgáltató- vagy órás workout-lépés összeadás.
- A Heti lépésszám és Aktív edzésidő független lenyitásai a két fejléc **alatt, teljes szélességen** jelennek meg.
- Az üres naptári nap normál tájékoztató sort, két egyenlő szélességű hozzáadásgombot és egyetlen visszavonást kapott; a hozzáadás és visszavonás meglévő callbackje maradt.
- A mentett edzés utáni szinkront a lezárt execution/ownership pillanatképben megőrzött pause/finalization jelző blokkolta. Kizárólag a hiteles forrásból származó, **kifejezetten `active=false`** lezárt pillanatkép vetíthető ki az admission ellenőrzéséből. Semmilyen tárolt frame/receipt/recovery marker nem törlődik. Aktív vagy sikertelenül mentett edzés, pending átadás, restore, backup és adatcsere továbbra is tilt. A main-thread vezérlés foglaltsága önmagában nem bizonyít átadást: a pontos mentési/átadási okot az IO-oldali tartós állapot adja. A magyar hiba inline jelenik meg, ismételt sikerjelzés nélkül.

**Verzió:** telefon és óra `0.1.0-beta / 2738`, `com.TrainPilot.com`, változatlan állandó signer. A 2737-es bétára adatmegőrző frissítés szükséges; nincs kulcscsere vagy adattörlés.

**Célzott bizonyíték:** [Actions 41491162595](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41491162595) SUCCESS, letöltött nyers XML **50 teszt, 0 failure/error/skip**. Forrás: `50384fc93bc12aff28d05e3e354cd52429440794`; a main merge teljes Git-fája azonos: `773096c3c3450f08f80ca5ee2a55d676a93ef8ac`. A tesztek a valódi finalizálási mentést, SQLite által visszautasított tartós írást, megmaradó blokkolást, rekordtípusokat, navigációt és elrendezési méreteket is vizsgálják.

**Teljes build:** [signed Actions 41492179428](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41492179428) **SUCCESS**, rögzített main-forrás `b96982dd62212d53f80ee1af366d77bfaacd439d`. Letöltött nyers XML: **277 teszt = 108 telefon + 169 Wear, 0 failure/error/skip**; shared NO-SOURCE. A tényleges APK-k package/verziója, hash-e, CI-checkoutja és állandó signere ellenőrizve. Signer SHA256: `a72fd016d94e9d94e2b2282032f8c3820d4f09cba9ff1db05c3fd1de6ffa7dd6`. Maps-konfiguráció, Data Layer pár és Wear capability/hardverkövetelmény igazolva.

| Csomag | Méret (bájt) | APK SHA256 |
|---|---:|---|
| Telefon 2738 release | 19464875 | `7a492760f3db6c90e1670091b1c1a3b7f10d1cc928646ad3248b821c1526b18d` |
| Natív Wear 2738 release | 4636863 | `9cf0f31c1c890e3b8c3b415217d1a4f458d3abca6991cadc928f9551d94abcd7` |

- [Telefon APK, ZIP nélkül](https://drive.google.com/file/d/18RYsx0OXugqxwRM5j0c0LcbIbJBSXEKh/view)
- [Natív Wear APK, ZIP nélkül](https://drive.google.com/file/d/1op7tJccyPg5Rc927Ipy_74vfH5rQxhxy/view)
- [Eredeti APK-pár + public metadata](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41492179428/artifacts/13166126011)
- [Teljes nyers tesztjelentés](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41492179428/artifacts/13165781493)
- [Célzott nyers tesztjelentés](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41491162595/artifacts/13163908378)

Mindkét új Drive-fájl mérete és szülőmappája külön metadata-visszaolvasással ellenőrizve: **TrainPilot Native**, `1ywHgrManynjV9uNvZfVncOmCr3SbHIb9`. A korábbi fájlok megmaradtak. Ez az átadófrissítés csak dokumentáció; az APK alkalmazásforrása a fenti main merge.

A fizikai Samsung Health / Health Connect adatolvasás, valamint a valódi óra–telefon működés készülékes elfogadása továbbra is külön feladat. A #19 követő nyitott checkboxai ezt is jelölik; ne írd újra a már mainre került megoldást pusztán emiatt.

## Korábbi béta-folytatás – #20 és #19

A felhasználó a készülékes hibák javítása után **a mainből szeretné AI Studióval folytatni**. Közös hibakövető: [#19](https://github.com/fuloplevente1998/Native-AI-Studio/issues/19), javító PR: [#20](https://github.com/fuloplevente1998/Native-AI-Studio/pull/20), teljes elfogadási lista: [PROGRESS_HEALTH_BUG_TRACKER_2026-11-02.md](PROGRESS_HEALTH_BUG_TRACKER_2026-11-02.md). A #20 célzott ellenőrzésének eredményét és a mainből indított teljes Actions eredményét külön kell kezelni.

**Új felhasználói verziókérés:** telefon/óra `0.1.0-beta`, belső Android buildszám mindkettőn `2737`. A 2736-nál kisebb kód nem települne normál frissítésként a korábbi appra; a kód ezért nő, a látható verzió új béta-sorozat. Package továbbra is `com.TrainPilot.com`, az állandó signer és privát kulcs változatlan. Ne indítsd 1-ről a belső kódot és ne kérj adattörlést.

A #20 kódja:

- 1/7/30/90 helyi nap szerinti közös volumen-/statisztikaszűrés, valós napi vagy heti oszlopok, adatalapú tengely, nulla adatnál nulla magasság. A testrajz ugyanazon időablak Room-sorozataiból frissül; egy naplózott edzés külön, saját sorozatokból készülő ábrát kap. A bicepsz/tricepsz a kar csoportjába kerül, a kis valódi terhelés nem tűnik el az egész százalékra kerekítés miatt.
- #16 újrafelhasználva, a három, alapból csukott forrásválasztó **egymás mellett**: TrainPilot WearOS / Samsung Health / Health Connect. Mindegyik a saját mentett adatait nyitja; nincs kalóriaösszeadás vagy szolgáltatói helyettesítés. A külön #16 ágat ne valósítsd újra.
- A forrásgomb tényleges olvasási grantot ellenőriz: meglévő hozzáférésnél szinkron, hiánynál hozzájárulás és tényleges visszaellenőrzés. Az Engedélyek oldalon külön szándékos kezelés marad. Az aktív edzés csak engedélykérő gombja külön út, nem indít teljes napi adatírást; a meglévő csak olvasási élő előnézet változatlan.
- A külön heti összesítő panel megszűnik. A Heti lépésszám és Aktív edzésidő saját, független napi bontást kap, az utóbbiban a Room-edzések és aktív percek jelennek meg. Nincs minden korábbi edzés idejét a hétre vetítő fallback.
- A napi lépések és energia a kiválasztott szolgáltató teljes helyi napi naplójából jönnek. A HC eredeti egyedi napi forrásválasztója és a Samsung SDK napi aggregátuma marad a számítás alapja. Külön provider-/óraértékeket nem adunk össze; egy órás workout lépésszáma nem teljes napi mérés. A Samsung/HC felől érkező órás napi eredet egyszer számít.
- A Kezdőlap és Egészség közös Mai adatok komponense forrást, utolsó igazolt sikeres szinkront, hozzáférés-/részleges-/hibaállapotot és Frissítés műveletet mutat. A hiányzó mai mező „—”/nincs lekért adat; aktív és nyugalmival együtt mért energia külön mező. A felhasználó 6–8 ezer lépés/1000 kcal észlelése diagnosztikai viszonyítás, nem termékbe írható adat.
- Edzés közben nincs vezérlőgombot takaró alsó státusz-pill. A rutin indulási/következőgyakorlat-üzenetek törölve; érdemi állapot/hiba az edzés fejlécének lefoglalt helyén jelenik meg. Az átadás/visszavétel a Scaffold rögzített felső részében a meglévő natív parancsot hívja; a `StudioWorkoutBridge.pauseForHandover` előbb a valódi motorral szüneteltet, lezárja az aktív időintervallumot, és azonosítót ellenőriz. Ezt a célzott natív regresszió is vizsgálja. Ownership, ACK, installed receipt, ARM/szünet, aktív GPS és adatcsere tiltása megmarad.
- A valódi Wear node-nevet (például a felhasználó `Galaxy Watch7 (ZW4J)` nevét) megtartjuk. Rögzített mintanevek/minta-akkumulátorok nem jelenhetnek meg.

Célzott forrás: `5df8374a65f81dc759ecfb6e8e96a936698344ff`. [Actions 41450724814](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41450724814): **SUCCESS, letöltött nyers XML szerint 43 teszt, 0 failure/error/skip**. [Nyers jelentés](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41450724814/artifacts/13144568757), [APK-metadata](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41450724814/artifacts/13144966264). A tényleges CI-checkout megegyezik a fenti forrással; a debug/optimalizált pár verziója és package-e ellenőrizve, a debug signer nem készülékes frissítési kulcs. A célzott futás a meghatározott regressziókat és a debug/optimalizált APK-fordítást ellenőrzi; nem teljes JVM-suite. A felhasználó kérése alapján a #20 e célzott ellenőrzés után mainre került, az átadó dokumentációjával együtt (alkalmazáskód-eltérés nélkül a tesztelt forráshoz képest), majd **mainből teljes Actions / állandó kulcsos APK-pár**. A #20 mainre merge-elve. Az alábbi teljes main-futás és állandó kulcsos béta APK-pár is igazolt; a célzott 43 teszt és a teljes 270 teszt eredményét ne keverd össze.

Készülékes elfogadás továbbra is külön: valódi Samsung/HC engedély és teljes napi számai/hibakódja, az új szűrők/anatómia, a három forrás és a telefon↔óra átadás. #19 marad a folytatható lista. A saját régi ZIP/fotók elfogadása és az új fotó csatolása, illetve videóhiba #11 sem tekinthető automatikusan megoldottnak.

## Korábbi #20 béta main-build és APK-k

Main / signed checkout: **`3572e2086cc741cc42bc2c6929c786b0772bf91c`**, [#20](https://github.com/fuloplevente1998/Native-AI-Studio/pull/20) merge. [Teljes állandó kulcsos Actions 41451874030](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41451874030): **SUCCESS**, letöltött nyers XML **270 teszt = 101 telefon + 169 Wear; 0 failure/error/skip**. A shared task NO-SOURCE. A célzott forrás `5df8374` és PR-head `c8f194c` között csak a fenti átadó dokumentáció változott; a PR-head és a main merge Git-fája azonos.

Mindkét tényleges release APK: **`com.TrainPilot.com`, `2737 / 0.1.0-beta`**. A tényleges signer megegyezik az előző tulajdonosi kulccsal: SHA256 **`a72fd016d94e9d94e2b2282032f8c3820d4f09cba9ff1db05c3fd1de6ffa7dd6`**. Maps-konfiguráció jelen van, a Data Layer package/signer pár és Wear capability ellenőrizve. Ez nem igazolja a valódi Samsung/HC olvasás készülékes eredményét.

| Csomag | Méret | APK SHA256 |
|---|---:|---|
| Telefon béta release | 19 464 875 bájt | `ba97e62fcb4ffa387209d6095fa7005c92379527d0a7298f451fae7a725becd1` |
| Wear OS béta release | 4 636 863 bájt | `02a18995f64109555d0db96a893e06124f69f691b404ff79bbc3a34d7f3148db` |

- [Telefon béta APK](https://drive.google.com/file/d/1qztMA9a1c-0O_9ZXr0j7dm6VlM7FR5Im/view)
- [Wear OS béta APK](https://drive.google.com/file/d/1RpezY4dAGdexDSZi2tuwQm9iDBgL74cZ/view)
- [Eredeti APK-pár + metadata](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41451874030/artifacts/13146057644)
- [Teljes nyers tesztjelentés](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41451874030/artifacts/13144834463)

Mindkét fájl a TrainPilot Native mappában van; a feltöltés után méret és szülőmappa külön metadata-olvasással ellenőrizve. A régi APK-k megmaradtak. A #16 külön PR-ját a #20-ba történt integráció váltja ki; a régi külön ágat nem kell újra merge-elni vagy újra megvalósítani.

## Korábbi, igazolt checkpoint – #18/#17, történeti háttér

Az alábbi APK-tábla még a `35d9d64` / `2736 / 1.3.71-native-test` kiadásé, nem az új béta. A régi példányokat és linkeket megőrizzük. A legfrissebb forrásra és a #16 állapotára a fenti #20 blokk az irányadó.

## Projekt és történeti #18 forrás

- Folytatandó repo: https://github.com/fuloplevente1998/Native-AI-Studio, **a legfrissebb main**.
- A #18 ellenőrzött alkalmazáskódja: main **`35d9d64ad99ed0cc67e5ffa72eb89ee28a7c14e6`**, PR #18 merge (AI Studio-frissítés + két kért hibajavítás).
- PR #18 forrás-HEAD: `ea5b897edcb5166edaffe770af22f76435cfec27`; a CI-checkout `cea5ee79c922b651e4bcfd2697ce63dd7c057389`; a forrás, CI és main teljes Git-fája azonos: `337f04da5ee8edc78e40f3009384e676097e3558`. [CI 41437300036](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41437300036): SUCCESS, letöltött XML szerint **255 teszt (86 telefon + 169 Wear), 0 failure/error/skip**. A 977cdd5 / PR #15 korábbi checkpoint.
- Ezután az átadó dokumentáció külön PR-ban frissülhet; a következő agent ellenőrizze az aktuális main HEAD-et és a checkpoint óta érkezett kódváltozásokat. A dokumentáció frissítése nem új APK-build.
- Audit: [PR #1](https://github.com/fuloplevente1998/Native-AI-Studio/pull/1). A már megvalósított pontokat ne készítsd újra.
- Bemásolható feladat: [AI_STUDIO_CONTINUE_PROMPT.md](AI_STUDIO_CONTINUE_PROMPT.md).

**Aktuális felhasználói döntés: most kizárólag a natív app és a hozzá tartozó natív Wear app folytatódik.** A régi mainnel közös órás alkalmazás későbbi feladat. Alügynökök indítása most nem kért.

## Megőrzendő szerződések

- Meglévő Kotlin/Jetpack Compose Studio alkalmazás; grafit/arany UI, eredeti arany súlyzó logó, natív anatómia és felső navigáció maradjon. Nincs új webes/WebView helyettesítő app.
- Telefon és óra applicationId: **`com.TrainPilot.com`** (kis-/nagybetű számít). Aktuális béta versionCode **2738**, versionName **`0.1.0-beta`**; az alábbi korábbi APK-táblában még a történeti verzió szerepel.
- Egyetlen meglévő natív edzésmotor, Room/DataStore, GPS- és Health-tárolók. Ne építs párhuzamos adatbázist/motort.
- Nincs fake/demo Health-, Wear-, GPS-, kalória-, pulzus-, akkumulátor- vagy sikerjelzés. Ismeretlen adat nem helyettesíthető kitalált számmal.
- Ownership/finalization, pending átadás, installed ACK, backup/restore generáció, tombstone, migráció, staging/redo és adatvédelmi kapu maradjon érvényes. Felhasználói adat vagy recovery receipt nem törölhető hibakerülésként.
- A helyi munkát őrizd meg. Külön javítóág/PR, érdemi kódváltozás után Actions és nyers teszteredmény; tesztet ne hagyj ki/ne gyengíts. Automatikus Play/GitHub release-publikálás nem engedélyezett.

## Történeti #18 build és letöltés

**Állandó saját kulcsos phone/Wear release-pár:** [Actions 41438108278](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41438108278), **SUCCESS**. A checkout a fenti `35d9d64` main commit, a PR #18 javításaival.

Letöltött nyers XML: **255 teszt = 86 telefon + 169 Wear; 0 failure, 0 error, 0 skip**. A shared task NO-SOURCE; közös kódját a telefonos/órás tesztek gyakorolják. Az alkalmazásváltozatok és JVM-tesztek sikeresek; ez nem fizikai készülékes elfogadás.

| Csomag | Méret | APK SHA256 |
|---|---:|---|
| Telefon release | 19 447 559 bájt | `de12cd02b8270ab86814e784e4cf1393a9a894d8777cc94661b442200f03e48a` |
| Natív Wear release | 4 636 867 bájt | `3dd4228f00cd250ba76dd888e3dfb640e58fedfe8c8494603b5f4857e7a98f9f` |

- [Telefon APK, ZIP nélkül](https://drive.google.com/file/d/17Zeo7lI6UghlW_2RwqbehmfnjZR0ATcK/view)
- [Natív Wear APK, ZIP nélkül](https://drive.google.com/file/d/1MtXXvN9-gEp_lLeakA5Wckwk5lGdkzIk/view)
- [Eredeti APK-pár és metadata artifact](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41438108278/artifacts/13138253377)
- [Nyers tesztjelentés](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41438108278/artifacts/13138546648)
- Teljes PR-CI: [41437300036](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41437300036), szintén 255/255. A Drive-fájlok mérete és a TrainPilot Native szülőmappa feltöltés után külön metadata-olvasással ellenőrizve. A korábbi APK-k megmaradtak.

**Minden további APK a [TrainPilot Native Drive-mappába](https://drive.google.com/drive/folders/1ywHgrManynjV9uNvZfVncOmCr3SbHIb9) kerüljön.** Mappaazonosító: `1ywHgrManynjV9uNvZfVncOmCr3SbHIb9`. Ne a Drive gyökérbe töltsd fel. A fájlnév tartalmazza a telefon/óra megkülönböztetést és a forrás rövid commitját. Őrizd meg a régi fájlokat és linkeket; ellenőrizd a szülőmappát, méretet és hash-t.

## AI Studio main-frissítés átnézése, 2026-11-02

A felhasználó kérésére átnézett új alkalmazásforrás: **`256e4536ee3c3a94bc5c78b93a33259bb2b15657`** (`feat: enhance cloud sync, equipment UI, and coaching`), 10 fájl, +1623/−214 sor. A fenti 977cdd5 a korábbi validált checkpoint. Az új forrásból indított [Actions 41434365835](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41434365835) a felhasználó új, két hibát előbb javító kérése miatt le lett állítva; ez nem igazolt új build. A javítások [PR #18](https://github.com/fuloplevente1998/Native-AI-Studio/pull/18) alatt elkészültek, 255 teszttel mainre merge-elve; a végleges javított forrás és signed bizonyíték a fenti buildblokkban található.

A frissítés a meglévő SettingsViewModel/GoogleSyncRepository útvonalat használó Google-bejelentkezési, Drive- és Naptár-kezelő kártyát, felszerelésválasztást/szűrést, napi/heti Health-összesítést és naplóalapú fejlődési statisztikát vezet be. A release signing konfiguráció, appazonosítók, verziók és a PR #15 alapvető mentési/adatvédelmi javításai változatlanok. A PR #16 három forráskártyája **nem része ennek a main commitnak**; a már elkészült ágat őrizd meg, és a friss mainnel együtt ellenőrizd integráció előtt.

A `256e453` kódáttekintésekor talált pontok, amelyeket a felhasználó kérésére a #18 javít:

- `LogAndProgressScreens.kt`, `MainStatisticsRecordsAndCardioSection`: a „Heti kilométer” az összes `loggedWorkouts` távolságát összegzi dátumszűrés nélkül; a „7 napos átlag” csak a pozitív lépésszámú napokkal oszt. A #18 dátumszűréssel és hét napos nevezővel javította.
- A heti térfogat/távolság oszlopok ma kezdődő utolsó heti ablakkal készülnek; a dátum nélküli rekordokat az utolsó hétbe sorolják. A `savedStartIso.take(10)`/`startsWith(dayKey)` UTC ISO-időből helyi naphoz rendelés nélkül számol. A #18 gördülő hét napot, helyi zónába konvertált időpontot használ, az ismeretlen dátumot kizárja a heti oszlopokból.
- `TrainPilotViewModel.refreshAfterCloudSync()` ismét `bindNativeEnvironment()`-et hív, amely új hosszú életű Flow-gyűjtéseket indít a meglévők megtartásával. A #18 külön adatfrissítést és explicit kötéskor lecserélhető megfigyelőket használ; az ismételt szinkront, kötést és élő hozzájárulás-változást célzott regresszió ellenőrzi.

A felhasználó az áttekintés után kifejezetten a két hiba javításával együtt kéri az APK-t. A #18 az utolsó hét helyi napját használja a heti kilométerhez és oszlopokhoz; a nulla lépéses napokat is számolja az átlagban; a napi edzésszámhoz is helyi dátumot használ. A felhőfrissítés már csak adatot tölt, az explicit újrakötés pedig lecseréli a korábbi Flow-figyelőket. Három dátumteszt és egy valódi Room-frissítést/élő feliratkozást/hozzájárulás-változást ellenőrző regresszió egészíti ki a kaput. A #18 mind a négy új regressziója sikeres, a fenti 255 tesztes CI és main merge ellenőrizve. A javított signed build [41438108278](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41438108278) a 35d9d64 mainből indult, **SUCCESS**, a letöltött release APK-k hash-e, verziója, appazonosítója és állandó signere ellenőrizve; a fizikai Google/Samsung működés továbbra is készülékes ellenőrzés.

## Három forrásválasztó: a #20 újrafelhasználja a #16 megoldását

A korábbi [#16](https://github.com/fuloplevente1998/Native-AI-Studio/pull/16) önálló ágának `6f9f11e` forrásán 249 teszt volt sikeres. A #20 ezt cherry-pickkel beemelte, egymás melletti fejlécválasztókra igazította, és a friss mainnel együtt ellenőrzi. A régi, külön #16 eredménye önmagában nem bizonyítja az új béta tesztjeit.

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
- **Korábbi sikertelen körök rendezve:** hiányzó Flow import; kattintható képkártya összevont semantics-fájának tesztelése; PAUSED Robolectric főszálon várakozó Room-UI callbackek. A teszt az unmerged képelem valódi láthatóságát ellenőrzi és a szüneteltetett Android loopert futtatja. Nincs timeoutnövelés, skip vagy lazított adatfeltétel. A #15 végleges PR és signed köre 248/248 volt; az aktuális #18 utáni kör 255/255 (fent).
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

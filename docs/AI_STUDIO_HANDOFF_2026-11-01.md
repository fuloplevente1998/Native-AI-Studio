# TrainPilot – natív AI Studio átadás, 2026-11-01

## Melyik projektet kell folytatni?

**Repo:** https://github.com/fuloplevente1998/Native-AI-Studio

A végleges átadás után a repo **legfrissebb `main` ágából** indulj. Ne a régi
TrainPilot PR #270 checkoutból, a régi AI Studio ZIP-ből vagy egy korábbi munkapéldányból.
A bemásolható feladat: [AI_STUDIO_CONTINUE_PROMPT.md](AI_STUDIO_CONTINUE_PROMPT.md).
A README erre az átadásra mutat; a korábbi MAIN_TEST_CHECKPOINT dokumentum történeti adat.

Az átadási összevonás a #8 (mentési tesztek), #9 (Maps/méretezés) és #10 (natív Wear)
PR-eket egyesíti. Az összevont végső forráscommit, CI-futás, nyers tesztjelentés,
APK-linkek és hash-ek a **PR #10 végleges leírásában** találhatók:
https://github.com/fuloplevente1998/Native-AI-Studio/pull/10
A végső main-összevonás csak sikeres közös ellenőrzés után történhet meg.

## Kötelező invariánsok

- Telefon és óra applicationId: **`com.TrainPilot.com`**.
- Verzió: **2736 / `1.3.71-native-test`**. Ne változtasd meg önkényesen.
- Meglévő natív Kotlin/Jetpack Compose projekt; nincs WebView/webes helyettesítés.
- A grafit/arany Studio felület, eredeti logó, natív anatómia, felső navigáció és
  a működő natív funkciók megőrzendők. Célzott hibajavítás kell, nem új UI/app.
- Nincs párhuzamos edzésmotor vagy telefonos adatbázis. A Room/DataStore és a natív
  execution/ownership/Health/GPS tárolók maradnak a hiteles források.
- Felhasználói edzés/profil/mentés nem törölhető hibakerülésként. A migráció,
  adatcsere-zár, redo receipt és finalization/ownership szabályok nem lazíthatók.
- Nincs fake/demo Health-, Wear-, GPS-, akku-, kalória- vagy pulzusadat. Ismeretlen
  érték maradjon nem elérhető; a rendszerkapcsolat nem azonos az alkalmazás-ACK-val.
- Release signing változatlan, kulcs/titok nem kerülhet Gitbe vagy logba.
- A #1 audit maradjon meg; csak valóban javított és igazolt pontot jelölj késznek.

## Már megvalósított kód

### Telefonos natív működés és mentés

A korábbi #2–#7 javítások már a kiinduló mainben voltak: tartós profil/terv,
natív stopper és szünet, aktív időablakos idő/Health-hozzárendelés, naptár, valós
engedélykérés és állapot, kültéri befejezés, valós naplóadatok. Ezeket ne készítsd újra.

A mentés meglévő `BackupRestoreManager` / `NativeReplacementJournal` /
`NativeDataReplacement` folyamatán át működik. Ismeretlen/sérült import nem
normalizálható csendben üres előzménnyé; az import típusa a fájl bájtjaiból dől el.
Hiányzó redo receipt melletti árva zárolást csak explicit helyreállítás oldhat fel;
meglévő sérült receipt zárolt marad. Felhasználói adat vagy receipt nem törölhető
az alkalmazásba belépés kikényszerítéséhez.

A #8 tesztjei fotóbájtok és hozzájárulással mentett Health-napló visszaállítását,
SHA256-hibás fotóarchívum elutasítását, hozzájárulás nélküli Health-export tiltását
és kilenc restore checkpoint utáni redo-t ellenőrzik. A szimulált Error nem valódi
Android process kill: a Kotlin finally blokkok lefutnak. A fotóteszt bináris
megőrzést, nem képdekódolást vizsgál. A Robolectric fixture a saját korábbi Room/
Health kapcsolatát lezárja, hogy az új context fájlját nyissa meg; ez nem appadat-törlés.

### Készüléken jelentett nagy mentés memóriahibája

A felhasználó JSON/ZIP és RepForge importnál 37 748 744 bájtos allokációs hibát
jelentett, 256 MB-os Android heap mellett. A `BackupJsonIO` folyamatosan olvas/ír:
nincs teljes dokumentumos byte-array + String + újraparszolt normalizációs másolat.
A ZIP eredeti manifest/hash ellenőrzése továbbra is az átírás előtt történik.
A szinkron staging lokális JSON-fái nem maradnak meg a felfüggeszthető replay keretében.
A Health redo/rollback JSON közvetlenül darabolt SQLite sorokba kerül, olvasása is
streamelt; a régi egymezős pending snapshot továbbra is olvasható.
A Health teljes naplója nem duplikálódik `legacy:nativeHealthJournal` Room TEXT sorba.
A 20 MB-os JSON, 18 MB-os Health és fotó/archívum korlátok megmaradnak.

A regressziók nagy Unicode-dokumentumot, dokumentumos `toString()` nélküli Health
staging/install/rollback/reopen folyamatot, és több megabájtos Health JSON/canonical
ZIP/manifest nélküli RepForge ZIP importot ellenőriznek. Ezek JVM/Robolectric tesztek;
a konkrét felhasználói mentés és 256 MB-os készülékes heap még külön ellenőrzendő.

### Napló: az edzés időablakához tartozó egészségadatok

A Studio naplórészletező korábban csak forrásjelzőket mutatott, a már meglévő natív
`JournalHealthPanel` és `NativeWorkoutHealth` nem volt bekötve. A
`StudioWorkoutHealth` adapter most a meglévő `JournalViewModel` Room-megfigyelését,
frissítési és lifecycle/adatcsere/törlés elleni védelmét használja a Studio felületen.
A mentett Wear és Health Connect értékek külön látszanak; érvényes cache esetén
nincs új provider-lekérdezés. Hiányzó/elavult Health Connect összesítésnél egyszeri,
előtérben végzett olvasás indul a részletek megnyitásakor. Kézi frissítés és a valódi
rendszerengedély-kéréshez kapcsolt gomb is elérhető. A sikertelen olvasás nem akadályozza
az edzés mentését, és nem törli a korábbi összesítést.

Az edzés `activeIntervals` mezője a mérvadó: a szünetet a provider-lekérdezés nem
foglalja magában. Azonos külső kezdő/végdátum sem tesz érvényessé olyan cache-t,
amely a szünet alatti adatokat is összesítette. Átlagpulzus/átlagsebesség a tényleges
mintaszám szerint kombinálódik; forrásnevek és időarányos energia figyelmeztetése
megmaradnak. A Wear és Health Connect kalóriákat nem szabad összeadni. Ismeretlen
érték nem válik becsült vagy nulla mérési adattá. A konkrét készülék Health Connect
adatelérhetősége és szinkronkésése továbbra is fizikai ellenőrzést igényel.

### Kültéri képernyő és Google Maps

Az `OutdoorGpsDashboard.kt` a meglévő natív callbackeket használja. A térkép kapja
a fennmaradó magasságot, a fő gombok a látható képernyőn maradnak. A 360×480-as
normál tartalomnézet nem igényel görgetést. Nagy rendszerbetűnél vagy nagyon rövid
ablakban a másodlagos tartalom görgethető; a betűt nem zsugorítjuk és a gombokat
nem rejtjük el. A teljes képernyős dialog safe drawing insetet használ.
Az aktivitásválasztó indítás előtt elérhető. A fő navigáció/logó változatlan.

A Maps SDK a valóban rögzített útvonalszegmenseket rajzolja, nincs kitalált helyzet
vagy vonal a szünetek között. Kulcs nélkül, hiányzó Play-szolgáltatásoknál és
betöltési időtúllépéskor külön állapot látszik. A GPS-rögzítés ettől független.

A build kulcsforrása: `TRAINPILOT_MAPS_API_KEY` env/Gradle property, vagy az ignorált
local/secrets.properties megfelelő kulcsa. Actions: `TRAINPILOT_MAPS_API_KEY` vagy
`MAPS_API_KEY` repository secret. A kulcs értéke ne kerüljön chatbe, Gitbe vagy logba.
Az eddig ellenőrzött #9/#10 APK-kban a `mapsApiKeyConfigured` **false** volt.
Mindig a legfrissebb build `trainpilot-apk-metadata` artifactjából ellenőrizd újra.
Ez konfigurációs jelenlétet mér, nem Cloud-engedélyezést vagy valódi térképcsempét.

A tényleges térképhez Google Cloud Maps SDK for Android engedélyezés és megfelelő
billing/key restriction szükséges. Android-korlátozás: **com.TrainPilot.com + az
adott APK tényleges signer SHA1-e**. Ne JavaScript/Gemini/server kulcsot helyettesíts be.

### Natív Wear OS

Az órás alkalmazás forrását a `TrainPilot/feat/native-android-migration` ág
`648e971034201511d69585cd5deecaa27eddf942` állapotából előkészített integrációval
vettük át. Most ugyanebben a repóban van **`:wear`** modulként. Tíz meglévő közös
natív fájl byte-ra változatlanul került a **`:shared`** modulba; mindkét app ezt használja.
Az eredeti óraoldali natív UI/szenzor/edzés/ownership megoldás és tesztek megmaradtak.

NodeClient-elérhetőség önmagában nem bizonyítja a TrainPilot óraapp jelenlétét.
A `trainpilot_workout_companion193` capability igazolja az app elérhetőségét az
adott egyetlen csomóponton. A Galaxy Wearable rendszerpárosító, nem a TrainPilot óraapp.

Az átadás a látható órás Activityt az aktuális workoutId/revision alapján nyitja meg.
Csak friss, explicit, egyező kérés indíthat órás tulajdonoskérést. A meglévő natív
request/offer/commit/installed protokoll dolgozik; ezt ne cseréld egyszerű booleanra.
A telefonról órára adás csak az `installed` ACK után, a forrás journal `settled`
állapotában kész. Visszavételnél egyező céloldali `installed` receipt és natív
resume-import szükséges, Main looperen. Handoff ID, workout ID és epoch egyezzen.

A felhasználó készülékén a telefonról órára adás sikerült, a visszaadáskor az óra
„telefon szinkronra vár” állapotot jelzett, a telefonos mentés pedig natív foglaltságot.
A `NativePhoneReturnInstallation` az igazolt visszaadást Main looperen készíti elő,
szükség esetén ARM-olja a foreground timer service-t az új generációra, telepíti a
szünetelő időzítőt, majd a natív szerkesztő kanonikus projekcióját publikálja.
A `nativeEditorProjection` helyi telepítéskor kerül vissza; a Wear encoder nem küldi.
ARM-hiba után a zár megmarad, újrapróbálás ugyanazt a generációt használja.
A telefonos mentés csak igazolt visszaadást fejezhet be, órás tulajdont nem kerülhet meg.
A fizikai visszaadás + mentés új APK-val még készüléken visszaellenőrzendő.

Az ajánlott/elfogadott/committed natív átadás és sérült journal nem oldható fel puszta
időtúllépéssel. Csak a protokoll szerint lejárt, még nem ajánlott kérés próbálható újra.
Átadás után explicit Folytatás kell; háttér-receiver nem indít automatikusan szenzort.
Aktív telefonos GPS-rögzítést a meglévő natív szabály nem enged átadni; ezt nem kerültük meg.

Az új telefonos session ugyanabba a kanonikus frame-be írja az ownershipProtocol193=1,
ownerEpoch193=0 mezőket. A már folyó régi session nem kerül átírásra. Protokoll nélküli
korábbi session biztonságosan telefonon folytatható, az átadási kísérlet magyarázatot ad.
Új session indítása nem resetelhet órás/átadott tulajdonú candidate-et telefonosra.

## Tesztelés és aláírás

CI: Gradle 9.3.1, JDK 17, Android compileSdk 36.1.

```
:app:assembleDebug :app:testDebugUnitTest
:wear:assembleDebug :wear:assembleOptimized :wear:testDebugUnitTest :shared:testDebugUnitTest
```

Az önálló #9 kör 37/37 tesztet igazolt; a Wear-integráció és natív session-javítás
külön köre 46 telefonos + 169 Wear = 215 tesztet igazolt. A #8-cal közös átadási kapu
**50 telefonos + 169 Wear = 219 teszt** volt a memória/visszavételi javítások előtti baseline.
A további regressziók végleges eredménye a közös CI-ben ellenőrzendő; a végleges futás
és nyers XML a PR #10 átadási leírásában található. A shared tasknak nincs önálló
tesztforrása; közös kódját a telefonos/órás protokolltesztek is használják.

A CI egyetlen átmeneti debug kulccsal építi a telefon/óra párt. Apksigner-ellenőrzés
bizonyítja azonos applicationId/signer párosításukat. Ez **nem** bizonyítja a korábbi
release vagy másik CI-futás kulcsával való frissítési folytonosságot. Más kulcsú
telepítés frissítéséhez az eredeti engedélyezett signerrel épített APK kell; adatot
ne törölj ennek megkerülésére. Új release kulcsot ne hozz létre.

Mindig egyetlen sikeres futás telefonos ÉS órás APK-ját használd. A metadata tartalmazza
mindkét SHA256-ot, a tényleges signer SHA1/SHA256-ot és a CI merge checkout commitját.
A PR HEAD és a CI által buildelt merge commit külön azonosító; a forrásfák egyezését
az átadás ellenőrizte. Régi külön futások APK-it ne keverd össze páros tesztnél.

## Wear APK méret: debug és optimalizált teszt nem ugyanaz

A megvizsgált korábbi natív Wear release APK 5 109 898 bájt volt, DEX-kódja
4 264 656 bájt. Az új, zsugorítás nélküli debug APK 39 262 279 bájt, ebből
38 087 480 bájt a kilenc DEX-fájl. A legnagyobb osztálycsoportok Compose/Wear,
Guava, ProtoLayout és Health Services; telefonos apposztályt nem találtunk benne.
A méretet nem a felhasználói adatbázis vagy fotó okozta.

A `:wear:assembleOptimized` külön optimalizált **teszt-APK**: a meglévő release
R8/resource-shrinking beállításokat örökli, de kifejezetten a telefon tesztkulcsával
írjuk alá. Nem engedélyezett release-kulcsot helyettesítő release-kiadás. A nagy debug
APK megmarad hibakereséshez; készülékes páros teszthez az optimalizált Wear artifactot
és az azonos futás telefonos APK-ját válaszd. A metadata az optimalizált fájlon ellenőrzi
az app/signer-egyezést, tényleges méretcsökkenést és a companion capability erőforrás
megmaradását. Az ellenőrzött `824b663` körben 4 636 867 bájt lett (a debug 39 262 700 bájt). A valós órás működés/optimalizált képernyő továbbra is készülékes próba.

## Nyitott feladatok, prioritási sorrendben

1. Maps-kulcs tényleges megérkezése, Cloud SDK/billing/Android-korlátozások, valódi
   csempék és rögzített útvonal megjelenése a telefonon. Hiányzó jogosultságot/kulcsot
   nevezd meg pontosan; ne írj sikeres integrációt a puszta nemüres kulcs alapján.
2. Galaxy Watch7-en a páros APK tényleges elérhetősége, kétirányú tulajdonosi ACK,
   explicit Folytatás, valós szenzorengedélyek és adatok, bontás/újracsatlakozás.
3. Fizikai képernyőteszt a kültéri normál/teljes képernyős nézeten, rendszer-sávokkal,
   nagy betűvel és hosszú hibaszöveggel. Más oldalak változó tartalma külön auditálandó.
4. Valódi JSON/ZIP export-import és app/process újraindítás, fotómegjelenítés,
   Health-adatok és felhasználói profil/napló megőrzése; Google Drive appintegráció
   tényleges bejelentkezési/hibakezelési esetei. A teszt-Error nem helyettesít telefonos próbát.
5. A #1 audit még nyitott tételeit vesd össze a legfrissebb kóddal. Már javított dolgot
   ne valósíts újra. Ne írj át szélesen működő UI-t vagy natív motort.

## Munkamód

A következő javítások külön ágon/PR-ban készüljenek, ne közvetlen main-pushsal.
Minden érdemi javítás után releváns teszt, majd Actions build/nyers jelentés.
Ne merge-ölj vagy publikálj release-t automatikusan új felhasználói felhatalmazás nélkül.
A már megadott felhatalmazáson belül önállóan dolgozz; ne állj meg újabb általános auditnál.

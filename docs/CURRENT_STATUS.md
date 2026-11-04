# TrainPilot Native – aktuális állapot, 2026-11-04

## Legújabb készülékes visszajelzés – 2026-11-04 / 2753

**13 aktív hiba/feladat:** [#117 – Naptár szinkron / Google 404](https://github.com/fuloplevente1998/Native-AI-Studio/issues/117),
[#118 – Témák kinyitott fejlécének és négy sorának közös kerete](https://github.com/fuloplevente1998/Native-AI-Studio/issues/118),
[#120 – Edzésrészletek: szolgáltatók a Forrás fülre, deduplikált pulzus/kcal a Statisztikákra](https://github.com/fuloplevente1998/Native-AI-Studio/issues/120),
[#84 – Újranyitva: naplós edzés- és visszajelzés-szerkesztő kompakt app-dizájnja](https://github.com/fuloplevente1998/Native-AI-Studio/issues/84),
[#122 – Egységes, naponta lenyitható Egészségnapló](https://github.com/fuloplevente1998/Native-AI-Studio/issues/122),
[#123 – Rendszer-dátumválasztó helyett az app saját naptárdizájnja](https://github.com/fuloplevente1998/Native-AI-Studio/issues/123),
[#125 – Aktív edzés Átadás órának vezérlőjére kis WearOS óraikon](https://github.com/fuloplevente1998/Native-AI-Studio/issues/125),
[#126 – Edzés Vissza/Előző és Elvetés gombjainak elkülönülő kerete/árnyalata](https://github.com/fuloplevente1998/Native-AI-Studio/issues/126),
[#70 – Újranyitott funkcióhiba: Edzés oldali Coach-javaslatok hiánya](https://github.com/fuloplevente1998/Native-AI-Studio/issues/70),
[#128 – Stopper: Indítás nem indít; leállítás/rögzítés felülvizsgálandó](https://github.com/fuloplevente1998/Native-AI-Studio/issues/128),
[#129 – Gyakorlatcsere után bent maradt stopperes felület](https://github.com/fuloplevente1998/Native-AI-Studio/issues/129),
[#130 – Kétszeri elvetés: kettős indítás vagy bent maradt aktív állapot gyanúja](https://github.com/fuloplevente1998/Native-AI-Studio/issues/130),
[#131 – Naptár Tervezés állandóan látható blokkban](https://github.com/fuloplevente1998/Native-AI-Studio/issues/131).
Referencia: a felhasználó 4407.jpg / 4406.jpg / 4408.jpg képei. Most csak rögzítés;
alkalmazáskód-javítás és új APK nem készült. A 404 oka a képből nem bizonyított.

Újabb referencia: **4409.jpg**, a naplózott edzés Statisztikák füle. A három
TrainPilot WearOS / Samsung Health / Health Connect panel a Forrás fülre kerüljön;
a Statisztikákon egy edzésszintű pulzus/kcal összesítő kell. Azonos/tükrözött mérés
csak egyszer számíthat; a forráseredet, pontos időablak és eredeti tárolt adatok
megmaradnak. Nincs források közötti vak kcal-összeadás, átlagok átlagolása vagy
hiányzó mérés helyettesítése. #120 most nyilvántartás, nem elkészült implementáció.

**4410.jpg:** a naplós edzés-/visszajelzés-szerkesztő a kompakt app-arculatot,
közös kereteket, fényes hangsúlyokat, színátmeneteket és elkülönülő boxokat kövesse.
Előzmény a már mergelt #93 PR; a korábbi #84 UI-jegy erre a konkrét dizájnra
újranyitva, új párhuzamos jegy nélkül. A régi Fejlődés/Naplólista/Coach részfeladatok
nem aktiválódtak újra. A mentés, viewport/IME, eredeti adatok és feedback-védelmek
megőrzendők; most csak rögzítés.

**#122:** a További egészségadatok és az Egészségnapló közös felületet kapjon,
az Egészségnapló dizájnját megtartva. A napok lenyitása mutassa a részletes,
egységes mérési adatokat. A „Részletes egészségmérések • dátum és forrás” külön
gombja és oldala megszüntetendő; a meglévő metrika-/rekord-/alvásfázis-részletek,
dátum/forrás és dedup ugyanebben a naplóban maradjanak elérhetők. Referenciák:
4415.jpg (két belépő), 4414.jpg (metrikák), 4413.jpg (részletes rekordok).

**#123:** a 4412.jpg rendszerstílusú Android DatePickerje helyett a már meglévő
app-naptár/dátumválasztó arculata használandó. A natív DatePickerDialog hívás a
HealthRecordBrowser.pickDate útvonalon azonosítva. A #122 utáni új közös felületre
is érvényes; nem a Google-naptárszinkron #117-es hibája. A dátum-/intervallum-
validáció és eredeti adatok változatlanok. Mindkét új jegy most csak rögzítés.

**Aktív edzés – új szöveges visszajelzés:** #125 az Átadás órának gombon kis,
a meglévő WearOS órakártyával egyező stílusú óraikont kér. #126 a környezetébe
olvadó Vissza/Előző és Edzés elvetése gombra kis keretet vagy elkülönülő színt/árnyalatot kér; a navigáció
és átadási/ownership műveletek megmaradnak.

**Coach – tényleges működési hiba (#70 újranyitva):** az Edzés oldali TrainPilot
Coach a felhasználó szerint semmit nem javasol, miközben közel 30 edzése mentett.
Ez nem dizájnprobléma. A megfelelő gyakorlat-/előzményadat, natív tanács és UI
bekötése kivizsgálandó; az ok még nem bizonyított. Valós, használható előzménynél
legyen kanonikus ajánlás/indok, valódi adathiánynál/hibánál érthető állapot.
A meglévő motor és jóváhagyási/fájdalom-/adatvédelmi védelmek maradjanak meg.
Mindhárom visszajelzés most rögzítés, nem kész javítás.

**Új mérési/session funkcióhibák:** #128-ban az Indítás nem indítja a stoppert;
a leállítás és rögzítés teljes útja ellenőrizendő. #129-ben Gyors edzésnél kétoldalas
plank súlyzós gyakorlatra cserélése után a stopperes UI bent marad. #130-ban a
felhasználó kétszeri elvetést tapasztalt, és felvetette a kettős indítás lehetőségét.
A kétszeri elvetés a megfigyelés; a két aktív edzés, bent maradt projekció és közös
ok még kivizsgálandó. E három jegy összefüggése együtt vizsgálandó a meglévő natív
session/admission/measurement/discard útvonalon, állapotreset és adatvesztés nélkül.
A kézi stopper hibája nem a sztornózott #74 intervallum/automata továbbfejlesztése.

**Elvetés megjelenése:** a jobb keret/szín/árnyalat a #126 dizájnjegy része,
a Vissza/Előző gomb mellett. Ez a #130 működési hibájától külön követendő.
**#131:** a Naptár Tervezés blokkja állandóan látszódjon az app dizájnjában,
külön nyitógomb nélkül. A napra koppintással nyíló adott-napi lebegő ablak és
műveletei a felhasználó kérésére változatlanok.

A nyitott GitHub-jegyek besorolása: **5 működési** (`type:functional`), **6 dizájn**
(`type:design`), **2 összetett adat/felület** (`type:data-ui`). A korábbi bug/enhancement
címkék megmaradnak. Javításonként rövid külön ág/PR ajánlott; tartós külön
funkció-/dizájn-főág helyett egy main, szükség esetén közös okú hibacsoport egy ágon.
[Munkaágak és címkézés](ISSUE_WORKFLOW_FUNCTION_DESIGN.md). Most továbbra is rögzítés.

A felhasználó kérésére a korábbi #35, #59, #69, #71, #75 lezárva; a halasztott
#82 `not_planned` státusszal lezárva, implementációs állítás nélkül. A korábbi
szakaszok nyitott checkboxai történetiek, nem önálló új feladatok.
[Aktív hibanyilvántartás](PROGRESS_HEALTH_BUG_TRACKER_2026-11-02.md).

A 2753-as release APK-pár időközben a megszokott TrainPilot Native Drive-mappába is
felkerült; a név, APK MIME, pontos méret és szülőmappa visszaolvasva:
**[telefonos release](https://drive.google.com/file/d/1VSHpl38FkLFFh7Nt5MlG0wGxRl2oQ39_/view?usp=drivesdk)**,
**[optimalizált Wear release](https://drive.google.com/file/d/1GGMk-L5wRmJkusZ_FO6quGT2mT-Gt11-/view?usp=drivesdk)**.


## Legfrissebb ellenőrzött kiadás: 2753 – edzésfotók / PR113

**[Aláírt telefonos release + optimalizált Wear release APK-pár letöltése](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41905695506/artifacts/13360792555)**.
A ZIP-ben a telefon `app/build/outputs/apk/release/app-release.apk`, az óra
`wear/build/outputs/apk/release/wear-release.apk`. Telefon **20 433 912 bájt**,
Wear **4 636 863 bájt**, változatlan tartós signer és `com.TrainPilot.com` package,
`0.1.0-beta / 2753`. A GitHub artifact 7 napig őrzött; a későbbi Drive-átadás
ellenőrzött linkjei fent találhatók. A pontos hash-ek és nyilvános cert a [kiadási bizonyítékban](RELEASE_2753_PHOTOS_VERIFIED_ARTIFACTS.json).

Aláírt forrás: `99f2adf74d0e485dad8e37940a981b428b518f99`, signed run41905695506
SUCCESS. Letöltött nyers XML: **460 = 284 telefon + 176 Wear**, 0 failure/error/skipped,
benne 7 új fotóregresszió. A közös modul NO-SOURCE. A tényleges APK-k hash-e,
v2 cert-ujjlenyomata, package/verzió/Maps/páros/capability-metaadata és a GitHub
ZIP digest is ellenőrizve. A későbbi PR115 csak workflow/dokumentáció, app/Wear/shared
forrása az aláírt kiadással azonos.

A `bbd7ad7` AI Studio import fotós és kompakt edzésvezérlős csomagja benne van.
Edzés végén és korábbi Napló-edzésnél is van kamera/galéria, három kategóriával.
PR113 javítja a naplós fotóhozzáadás parent-REPLACE miatti kaszkád-adatvesztését,
és az összegző fotójának eltűnését a natív projektálás/mentés során. A mentés
atomi, a késői callback törlés/restore/generáció után védett. Korábban már elveszett
adat visszaállítását nem állítjuk. **[Telefonos tesztlista](TEST_2753_PHOTOS_PHONE.md)**;
a tényleges kamera/galéria és vizuális készülékes elfogadás még nyitott.

## CI gyorsítás – PR114 / PR115

Két generált sablonteszt kivezetve, a funkcionális regressziók megmaradtak.
A signed main végre elmenti a Gradle User Home cache-t; a logban a mentés igazolt.
A PR CI ezentúl csak a teljes JVM-teszteket futtatja, külön debug/optimalizált
telepítőcsomagolás nélkül. Release APK és annak szigorú ellenőrzése a signed jobban.
Új, hideg regression-only CI41906780189 SUCCESS: 460 nyers teszt, 0 hiba/kihagyás,
Gradle **6m18.75s**, a korábbi fotós CI Gradle-lépése 13m03s volt. Ezek külön
mért futások, nem minden következő módosításra szóló időígéretek.
Meleg main-cache próba41908345166 SUCCESS: Gradle-lépés **23 mp**, teljes job
**1m26s**, 76 taskból 52 FROM-CACHE. A telefon/Wear tesztek eredménye cache-ből
jött, azonos alkalmazás- és tesztforrásra; nem újonnan végrehajtott készülékes
próba. Letöltött 460 nyers eredmény és aktuális source-report ellenőrizve.
Új forrásváltozás továbbra is invalidálhat taskokat.
[Időprofil és cache-részletek](ANDROID_BUILD_PERFORMANCE_2026-11-04.md).

Az alábbi bejegyzések korábbi, történeti kiadások; a fenti 2753 az aktuális átadás.


## PR108 – második AI Studio import, 2752-es forrás előkészítve

A külső YouTube/rendszerböngésző a videógomb alapértelmezett útja; a belső lejátszó helyett hibánál linkmásolás/újrapróbálás jelenik meg. Aktív edzés, gyakorlatutasítások, programmetaadatok, izomterhelés, időszak- és felugrófeliratok fordítása bővült. A saját nevek/leírások és a konkrét felszerelésadatok megőrzése felülvizsgálva. A korábbi GPS-adatvédelmi és ismeretlen feedback-védelmek megmaradnak.

CI41797678036 SUCCESS: **450 =274 telefon+176 Wear**, 0 failure/error/skipped, letöltött nyers XML ellenőrizve. Font12 és JS7 PASS; Maps/package/páros metaadat igazolva. [Gépi bizonyíték](AI_STUDIO_SECOND_INTEGRATION_CI_2026-11-04.json).

**Coach/progresszió: külön PR109**, nincs ebben az importban, nem készre jelentett. A külön ágat a PR108 utáni friss mainhez kell illeszteni, különösen a MainActivity.kt, TrainPilotViewModel.kt, TrainPilotModels.kt és WorkoutAndPlanningScreens.kt átfedéseinél a fordításokat megőrizve.

2752-es verziókód a forrásban, de új aláírt APK most nem készült. A legutóbb átadott telepítő továbbra is az alábbi 2751; az nem tartalmazza a PR108-at. A külső videómegnyitás és a nyelvi megjelenítés tényleges készülékes próbája a következő közös kiadással esedékes.

## Legfrissebb aláírt telefonos kiadás: 2751 / PR106

**[Telefonos APK letöltése – 2751](https://drive.google.com/file/d/1VTlz4bDex5pZR_FnIZsLSQ1qH0O2jVLO/view?usp=drivesdk)**. A megszokott tartós aláíróval a meglévő appra frissíthető, elég a telefonos appot telepíteni. Package `com.TrainPilot.com`, versionName `0.1.0-beta`. Az óra alkalmazáslogikája nem változott.

Tartalmazza a PR101/104 dizájn-, profil-, grafikon-, aktivitásszűrés- és kedvencjavításokat, a PR105-ben felülvizsgált AI Studio csomagot, valamint a PR106 profilopció-/felszerelésleírás-/kizárás-/gyorsedzés-fordításait. A mentett kanonikus profilértékek és a felhasználói tartalom megőrzését külön regresszió ellenőrzi.

- Aláírt forrás: `754da45bae8fc3bffd1190bdc6d2ea54757fdbd7`, signed run41781088959 SUCCESS.
- PR CI41779927504 és az aláírt kiadási folyamat is **446 =270 telefon+176 Wear** tesztet futtatott, 0 failure/error/skipped. Mindkét nyers XML ellenőrizve.
- Telefon: 20 335 612 bájt; SHA256 `0e896a790ad7aad4c639fe4cd33d1ff1051b0bed75e474f4c498972ea5a1342e`. A signer, verzió, package, Maps-konfiguráció és phone/Wear páros egyezés ellenőrizve, Drive-metaadat visszaolvasva.
- [Gépi kiadási bizonyíték](RELEASE_2751_VERIFIED_ARTIFACTS.json), [telefonos tesztlista](TEST_2751_PHONE.md).

A tényleges videólejátszás és a teljes telefonos vizuális/nyelvi elfogadás még visszatesztelendő. CI és APK-ellenőrzés nem fizikai készülékes elfogadás. [Hátralévő ellenőrzések](REMAINING_WORK_AFTER_2748.md).

## Korábbi integrációs és kiadási állapotok (történeti)


## AI Studio Drive integráció – 2750 előkészítés

Bázis: PR104, `0ec7065087f241809026f88344e4c8651df467ce`. A korábbi profil-, statisztika-, kereső- és kedvencjavítások megmaradnak. Fordítások, videós belső böngésző fallback és régi feedback-olvasók bővültek; az adatvédelmi szöveg Samsung/GPS állításai pontosítva. A teljes lokalizáció és a YouTube készülékes elfogadása még nem igazolt. A tényleges támogatott JSON-utak és korlátok az [átadó 7. pontjában](AI_STUDIO_LANGUAGES_VIDEO_DATA_HANDOFF_2748.md).

Legutóbb átadott aláírt APK: **2749**, [telefonos letöltés](https://drive.google.com/file/d/1bNYaIqRfdtmKpajAmRHvjndZClW7128V/view?usp=drivesdk). [Ellenőrzött kiadási bizonyíték](RELEASE_2749_VERIFIED_ARTIFACTS.json). A 2750-es verziókód előkészítve, de új aláírt kiadás még nem készült.

Az integráció CI41775858274 sikeres: 444 teszt ({'app': 268, 'wear': 176}), 0 hiba/kihagyás; a nyers XML letöltve és ellenőrizve. [Bizonyíték](AI_STUDIO_INTEGRATION_CI_2026-11-04.json). Fontellenőrzés: 12 PASS; videó JS: 7 PASS. A ZIP korábbi compile_applet/443-as jelentése helyett ez a tényleges eredmény irányadó. A PR104 forrásellenőrzése: 439 teszt, 0 hiba (CI41769004493).

## Korábbi kiadások és dizájnellenőrzések (történeti)


## Friss main dizájn – PR101, 2026-11-04

A dizájncsomag mainen: **4737d72c75f191b18980bc40885633c1c43b5c99**. Teljes CI434 =258 telefon+176Wear, 0 failure/error/skipped, 12 valódi fontarc glyph/súly/hash/licenc ellenőrzés és JS7 PASS. [Bizonyíték](DESIGN_CI_2026-11-04.json), [változások](DESIGN_FONT_THEME_IMPLEMENTATION.md).

Elkészült forrásban: #95 egységes sorozatvezérlők/egységfeliratok, #97 ismételt napcímek, #98 teljes ékezetes fontok és valódi súlyok, #99 glow/világos felületek és natív kijelölések, valamint a telefon/óra átadási sáv kompakt dizájnja. Motor és parancsok változatlanok; az aktív egységek csak UI-projekcióban maradnak meg. A készülékes vizuális elfogadás a következő APK-val szükséges.

Az AI Studio most átveheti a **friss main** képernyőinek nyelvi bekötését, a kész dizájn megőrzésével. [Main/raw átadó](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/docs/AI_STUDIO_LANGUAGES_VIDEO_DATA_HANDOFF_2748.md), [állapotjelző](DESIGN_WORK_STATUS.md). További AI Studio-scope: videóhiba, régi feedback audit, végül adatvédelmi tájékoztató.

**Az alábbi2748 továbbra is a legutóbb átadott aláírt APK, régebbi f74133a alkalmazásforrással; nem tartalmazza az új PR101-dizájnt.** Ez a kör nem készített új aláírt kiadást. A korábbi #95-hátralévő státuszt a fenti friss implementáció felülírja.

## Legfrissebb kiadás: 2748 / PR90

**[Telefonos APK letöltése – 2748](https://drive.google.com/file/d/1SquNqT68gJtxt5zGQUuqlPlVU-_kEr9C/view?usp=drivesdk)**. Elég a telefonos appot frissíteni; a Wear alkalmazáslogika változatlan, órás újratelepítés nem szükséges. Package `com.TrainPilot.com`, versionName `0.1.0-beta`, versionCode2748, a megszokott állandó aláíró.

- Aláírt alkalmazásforrás: `f74133a1bb1e690ce56166c1f0370582e4eedbc6` (PR90 main-merge). A tesztelt PR-head `a98260c122159530e6af6f0f6b9b16358a756a36`, tényleges PR-CI checkout `db3b344fac8d92c7008f74e57f4b02d9a75c3815`; app/Wear/shared/scripts/workflows fák egyezése ellenőrizve.
- [PR CI41721740450](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41721740450) SUCCESS: letöltött nyers XML **428 =252 telefon +176 Wear**, 0 failure/error/skipped; **7 videó-JS ellenőrzés** sikeres.
- [Aláírt main build41723677980](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41723677980) SUCCESS: ismét letöltött nyers428, nulla hiba/kihagyás; mindkét APK verzió/package/hash/v2 cert fingerprint és CI-apksigner/pinned signer, Maps/pair/capability ellenőrzés sikeres.
- Telefon: **19 743 944 bájt**, SHA256 `f3f1aae2306142fbf47d52f2240a864b54843bb85294e3b5fd6e346eafb7c832`. Drive-fájl neve, MIME, mérete, mappája és tényleges linkje visszaolvasva. [Gépi bizonyíték](RELEASE_2748_VERIFIED_ARTIFACTS.json).

## Mi van a 2748-ban?

A #91 átlagvolumen-alcím eltávolítása; #92 Coach rövidebb készenléti kártya és Edzés oldalra navigálás automatikus indítás nélkül; #93 kompakt keretes naplószerkesztő, legördülős nehézség és a látható Android-ablakra korlátozott Mentés; #94 elkülönülő kontrasztos kártya/belső panel. A matt/világos/glow alapszínek megmaradnak. A fő Health/Coach/Fejlődés kártyák finom fade kiemelést, a GPS-felirat és a sorozatszerkesztő vezérlői külön hézagot kapnak. A felhasználó által már jónak jelzett fullscreen GPS gombelrendezés változatlan.

#71: mind a26 meglévő metrika pontos nap- és Health Connect/Samsung forrásválasztással, a korábbi részletes rekordböngészővel; nincs idegen nap/forrás vagy becslés helyettesítés. #69 kért új része: gyakorlatonkénti válasz/jegyzet az összegzőben és naplóban, dropdownnal, explicit kanonikus gépi értékekkel; a véglegesítő legfrissebb draftjára kerül, sorozat/RIR/metadata, applied-zár és restore/owner/generation védelemmel. Nem új motor vagy Coach-képlet.

**[Telefonos tesztlista](TEST_2748_PHONE.md)**. CI és APK-ellenőrzés nem készülékes elfogadás; a témák, Mentés/billentyűzet, metrikák és per-exercise visszajelzés közös próbája még szükséges.

## Mi maradt?

**[Tételes hátralévő munkák](REMAINING_WORK_AFTER_2748.md)**: #95 egységes aktív sorozatsor és helyes kg/kar, kg, ism. feliratok (bolgár guggolásnál /láb nélkül), #59 beágyazott videó tényleges hibája, #75 teljes lokalizáció, #82 későbbi adatvédelmi szöveg és #69 régi formátumok teljes auditja. #95 a felhasználó kifejezett kérésére a következő kör, nem része a 2748-nak.

A legfrissebb készülékes visszajelzés szerint a beágyazott videó nem indul, **a belső böngészős mód működik**. Ez nyitott hiba; a 2748 nem tartalmaz új, külön erre készült javítást. A videó JS-tesztjei nem jelentik a YouTube készülékes elfogadását. **BLE és #74 automata/intervallum további bekötés sztornó.**

## Korábbi, történeti átadók

Az alábbi korábbi APK- és backlog-leírások történetiek; a fenti 2748-as állapot az irányadó.


## Legutóbbi ellenőrzés: PR89 / 2747

PR89 main-merge `d7efe6ccc0d96db658ce3336358fb2e498a4facc`, PR CI41711925392 és signed main41712528009 SUCCESS. A letöltött nyers XML **420 = 244 telefon + 176 Wear**, 0 failure/error/skipped. A signed metaadat source/package/verzió/Maps/páros egyezése, mindkét APK hash-e és a tényleges v2 aláíró tanúsítvány hash-e ellenőrizve; signer változatlan. [Nyilvános bizonyíték](RELEASE_2747_VERIFIED_ARTIFACTS.json).

**[Legfrissebb telefonos APK: 2747 / PR89](https://drive.google.com/file/d/1JkLyIuOYtINKNBatGg2Ems_lHUyXDlHk/view?usp=drivesdk)** – 19 645 636 bájt. A feltöltött Drive-fájl neve, mérete, szülőmappája és letöltési linkje visszaolvasva. A 2747 telefonos/UI módosításaihoz nem szükséges új órás app: a Wear-forrás csak versionCode-ban tér el a korábbi kiadástól. Készüléken a fullscreen GPS alsó gombjai, a kompakt izomterhelés-fejléc és a napló jobb szélső törlése továbbra is visszatesztelendők.

A teljes metrikalista és a gyakorlatonkénti visszajelzés **külön, még nem fordított forráscsomagban** készült, nem része a fenti 2747 APK-nak; [állapot és következő ellenőrzések](METRICS_EXERCISE_FEEDBACK_NO_BUILD_2026-11-03.md). **#74 automata/intervallum további bekötése sztornó; BLE sztornó; #82 adatvédelmi tájékoztató később.** #75 teljes négynyelvű paritás nyitott. Az alábbi 2744-es átadó és a régi backlog történeti, a fenti frissebb állapot az irányadó.

Ez az elsődleges átadó. A felhasználó a beleférő javításokat egyetlen telefonos APK-ban kérte, közös készülékes próbára. A régebbi átadók és a korábbi „backlog / nincs bekötve” kijelentések történetiek; a lentiek az aktuális állapot.

## Legfrissebb telefonos APK

**[A közös javítócsomag letöltése](https://drive.google.com/file/d/1EBiZCJ7hEiGi5QHw87umNLZbEEmTgcVu/view?usp=drivesdk)**. Elég a telefonos appot frissíteni; Wear- és shared-forrás nem változott, órás újratelepítés nem szükséges. VersionCode2744, versionName0.1.0-beta, package `com.TrainPilot.com`, az állandó signer változatlan.

Alkalmazásforrás: `d69c77e41692f7c2c171e400358772439bff35ea` (#79 main-merge), tesztelt PR-head `18b828e17e45a2b67ea6e864c04fe72685b46a6c`; alkalmazás/Wear/shared/scripts/workflow fák egyeznek. A dokumentációs utókövetés ezeket nem módosítja.

- [Teljes Android CI 41670555328](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41670555328) SUCCESS. Letöltött nyers XML: **411 = 235 telefon + 176 Wear**, 0 failure/error/skipped, forrás-SHA ellenőrizve. A tényleges inline lejátszóscript **7 eseménytesztje** külön sikeres. [CI-bizonyíték](NATIVE_BATCH_CI_2026-11-03.json).
- [Aláírt main build 41671892820](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41671892820) SUCCESS; pontos forrás `d69c77e41692f7c2c171e400358772439bff35ea`; letöltött signed metadata és nyers XML ellenőrizve. Mindkét APK package/verzió/signer/Maps/pair/capability/hash ellenőrzése sikeres. Telefon: **19629256 bájt**, SHA256 `f8f3c76006cd7e5d01b937318e1f62fc9a7bd19a5f1076ed4b7b270c24f1c069`. A feltöltött Drive-fájl méretét és a meglévő mappát visszaolvastuk; csak a telefonos fájlt adtuk át. [Aláírt bizonyíték](NATIVE_BATCH_SIGNED_BUILD_2026-11-03.json).

## Mi került a közös csomagba

- #59: YouTube előnézet és explicit Lejátszás, appazonos origin/referrer, iframe-cookie/minimumméret, elakadt/blokkolt indítás és késői callback korrekt hibája; használható belső böngésző. A készülékes fekete kép javításának elfogadása még nyitott, a pontos eredeti ok nincs bizonyítottnak nevezve.
- #60: kompakt órakártya, név+Sync fejléc és két kisebb, normál méretnél egymás melletti panel; keskeny/nagy betűs tördelés, valós óraadatok.
- #61: a Samsung nem támogatott nyugalmi pulzusa nem engedélyhiány; valódi megtagadás és SDK-hiba külön marad. A #77 Energy0 javítása megmarad. Ez nem bizonyítja, hogy az adott órán minden metrika rendelkezésre áll.
- #72: valódi programcopy/delete, nap hozzáadás/törlés, napból gyakorlateltávolítás, névmentés; meglévő sajátgyakorlat-szerkesztő/törlés és tartós kedvencek/szűrés. A bevált natív átrendezés és helyi súly/ismétlés/pihenő minta megmarad. Beépített tervből saját másolat, eredeti/napló/naptár/aktív-edzés/restore védelmekkel.
- #73: valódi natív naplószerkesztő: sorozatok, súly/ismétlés/tényleges RIR, időpontok, edzésjegyzet és utólagos visszajelzés. Azonos edzés-ID, CAS/tombstone/restore, régi megjegyzés és már alkalmazott feedback védelme; siker után Studio napló/PR/Coach frissül.
- #71 részleges: a meglévő részletes egészségrekord-böngésző a Studio egészségnaplóból elérhető, a kiválasztott HC/Samsung csatornán; dátumtartomány, eredet/típus, lapozás, alvásszakasz/sorozat-részletek. A teljes26 metrikás összesítő még külön feladat.

**[A közös telefonos tesztlista](NATIVE_TEST_BATCH_2026-11-03.md)**. Implementáció, CI, APK-átadás és fizikai elfogadás külön állapot; készülékes sikert még nem állítunk. #59 és a korábbi #35/#48 felhasználói visszatesztre nyitott.

## Ami még nincs teljesen kész

#69 teljes gyakorlatonkénti/utólagos visszajelzés-paritás és régi formátumok auditja; #71 teljes26 metrikás összesítő; #74 preset/intervallum/automata motor tartós bekötésének maradéka; #75 teljes HU/EN/DE/RO felület. Ezért a teljes app még nem nevezhető teljes paritásúnak. **BLE-kezelő sztornó**, meglévő backend/adat nem törlendő. [Paritásaudit](NATIVE_WEBVIEW_PARITY_2026-11-03.md), [hibakövető](PROGRESS_HEALTH_BUG_TRACKER_2026-11-02.md).

## Korábbi igazolt átadások

A #77 Coach/előző megfelelő alkalom súly-/ismétlés-előtöltés mainen és a mostani APK-ban is megmarad. [Megvalósítás](COACH_PREFILL_IMPLEMENTATION_2026-11-03.md), korábbi appsource81da2f7, CI402 és [signed proof](COACH_PREFILL_SIGNED_BUILD_2026-11-03.json). A még korábbi #67 térkép/Mai állapot/Settings forrásdeebfdd, CI395 és [első telefonos átadás](MAIN_TEST_BUILD_2026-11-03_2744.json) történetileg megőrzött. Ezek az idősebb APK-k nem tartalmazzák a mostani #79 javításait.

A Studio Compose arculat, egyetlen natív motor/Room/DataStore, tulajdonos/ACK/epoch/explicit resume/finalization/replacement/tombstone, alapból kikapcsolt health backup-consent és privát GPS/fotó védelem változatlan. [Constitution](../.specify/memory/constitution.md). Teljes Android build/regresszió GitHub Actionsben, kulcsot nem kérünk és nem naplózunk. Alügynök nincs kérve.
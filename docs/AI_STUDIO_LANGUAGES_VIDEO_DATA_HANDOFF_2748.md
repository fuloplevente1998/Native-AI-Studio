# AI Studio – nyelvek, videó és fennmaradó adatfeladatok, egyben

Ez az egyetlen átadandó munkaleírás. Az összes szükséges feladat és main/raw referencia ebben van; nem kell másik chat vagy nem látható fejlesztői branch.

## 0. Feladat, kiindulás és sorrend

A felhasználó a nyelveket és a fennmaradó nem-dizájn munkát egyetlen AI Studio-munkafolyamatnak adja. **A dizájnon Codex dolgozik.** Vidd végig ellenőrizhető lépésekben a saját csomagod: **nyelvi erőforrások és readonly leltár, videó és régi feedback audit → friss main dizájn utáni teljes nyelvi bekötés → adatvédelmi tájékoztató a csomag végén → ellenőrzött integráció**. Az adatvédelmet korábban későbbre kérte; ezért a többi javítás után következzen. BLE-kezelő és az automata/intervallum további bekötése sztornó, ne legyenek a csomag részei.

Repo: https://github.com/fuloplevente1998/Native-AI-Studio . **A tényleges legfrissebb mainből indulj.** Az alábbi raw linkek mind a mainre mutatnak. Az ellenőrzött 2748 alkalmazásforrás f74133a1bb1e690ce56166c1f0370582e4eedbc6, utána a 6f24a078651827033b929d4f429c29be98da5074 dokumentációs követés. A munkaleírás későbbi docs-commitja nem új alkalmazásbuild.

Az AI Studio csak a main nézetet látja, ezért a mainből beolvasott projekten dolgozz; ne várj hozzáférést korábbi javítóbranchhez. A változtatást a rendelkezésedre álló patch/commit/GitHub-export módon add vissza. Ne állíts branch-létrehozást, PR-t, main-merge-öt vagy APK-t, ha a tényleges eszközöd nem végezte el. A felhasználó a teljes implementációt kéri, nem csak tervet; ahol az eszközöd engedi, csináld végig, korlátnál konkrét alkalmazható módosításokat adj át.

A tényleges app **Kotlin/Compose natív telefonapp**. A Studio út a com.example/MainActivity.kt és com.example/ui képernyők, meglévő com.repforge natív komponensekkel. Ne generáld újra a régi WebView vagy ai-studio export alapján az alkalmazást. A régi TrainPilot-repo csak referencia, nem szerkesztési cél.

Már megvan: teljes26 metrikás böngésző, gyakorlatonkénti feedback/jegyzet, natív naplószerkesztő/dropdown, Coach bekötés, előző megfelelő edzés előtöltése, program-/gyakorlatműveletek és kedvencek. Ne implementáld újra ezeket. A 2748 PR és signed CI428 =252 telefon+176Wear tesztje hibamentes, JS7 sikeres. Ez forrás-/buildbizonyíték, nem minden készülékes funkció elfogadása.

### Először olvasd el

- [docs/CURRENT_STATUS.md](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/docs/CURRENT_STATUS.md)
- [docs/REMAINING_WORK_AFTER_2748.md](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/docs/REMAINING_WORK_AFTER_2748.md)
- [.specify/memory/constitution.md](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/.specify/memory/constitution.md)
- [docs/FRAME_DESIGN_SYSTEM.md](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/docs/FRAME_DESIGN_SYSTEM.md)
- [docs/RELEASE_2748_VERIFIED_ARTIFACTS.json](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/docs/RELEASE_2748_VERIFIED_ARTIFACTS.json)
- [docs/TEST_2748_PHONE.md](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/docs/TEST_2748_PHONE.md)
- [app/src/main/java/com/example/MainActivity.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/MainActivity.kt)
- [app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt)

## 1. Fájltulajdon: a dizájnon Codex dolgozik

A felhasználó legújabb döntése felülírja a korábbi teljes-kiosztást: **a dizájnt Codex végzi, az AI Studio a nyelveket, videót és régi feedback-auditot kapja**, az adatvédelem a végén következhet.

**Először mindig olvasd a main állapotjelzőt:** https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/docs/DESIGN_WORK_STATUS.md

Amíg `in_progress`, az alábbi fájlok csak olvashatók az AI Studiónak:
- app/src/main/java/com/example/ui/screens/ActiveMeasurementScreens.kt
- app/src/main/java/com/example/ui/screens/WorkoutAndPlanningScreens.kt
- app/src/main/java/com/example/ui/components/StudioWorkoutHandoverBar.kt
- app/src/main/java/com/example/ui/components/WorkoutPresentationLabels.kt
- app/src/main/java/com/example/ui/theme/Type.kt
- app/src/main/java/com/repforge/app/ui/theme/NativeStudioTypography.kt
- app/src/main/java/com/example/ui/theme/TrainPilotPalettes.kt
- app/src/main/java/com/example/ui/theme/Theme.kt
- app/src/main/java/com/example/ui/components/StudioNativeEditorTheme.kt
- app/src/main/res/font/* és app/src/main/assets/font-licenses/*

Eközben dolgozhatsz a meglévő nyelvi erőforrásokon és TrainPilotStrings.kt-n, valamint új nyelvi erőforrásfájlokon; a fenti képernyők beégetett feliratainak kiváltását egy pontos resource/bekötési térképben add át. A videó saját komponensein és a régi feedback backend auditján párhuzamosan is haladhatsz, ha nem változtatod meg a képernyők által használt callback/API szerződést.

Amikor a main állapotjelző `ready_for_translation_binding`, a dizájn már mainen van: olvasd be a friss fájlokat, és kösd be bennük is a nyelveket. Ne alkalmazd vissza a munka elején elmentett régi teljes képernyőfájlokat. A teljes fordítás ekkor fejezhető be ütközés nélkül. A scope végéig egyetlen AI Studio-munkafolyamat felel a kapott feladatokért; nincs külön chatkiosztás.

A tipográfiai hiba okát Codex bizonyította: mindhárom jelenlegi csomagolt fontból hiányzik őűŐŰ és több román karakter. A fontcserét és valódi súlyregisztrációt Codex javítja; az AI Studio ne helyettesítse az ékezetes Unicode-szöveget o/u betűkkel, és ne írjon személyes adatot át a font miatt.

## 2. Tényleges videóhiba (#59)

**Készüléken megerősített aktuális állapot: a beágyazott lejátszó nem működik, az appon belüli böngészőben a videó elindul.** A hiba nincs kijavítottnak minősítve a 2748-ban. Ne tekintsd pusztán újratesztelési feladatnak. A jelenlegi hibakódot nem ismerjük; 101/150/153 vagy engedélyhiba konkrét bizonyíték nélkül nem állítható okként.

A cél: a felhasználó az appon belül megbízhatóan elindíthassa a kiválasztott gyakorlat valódi videóját. Vizsgáld meg az iframe/WebView valódi origin/referrer/API eseményútját és a beágyazási korlátozásokat; az alább linkelt régi main ugyanennek a gyakorlatvideónak a működő referenciája. Ha a beágyazást a YouTube ténylegesen elutasítja vagy nem használható, legyen egyértelmű és működő átmenet a már készüléken működő belső böngészős módba, félrevezető „készen áll” fekete képernyő nélkül. A működő belső út alapértelmezése is mérlegelhető a megbízható appon belüli indítás érdekében; ne ragaszkodj a hibás iframe-hez. Ne próbáld korlátozások megkerülésével, kitalált felhasználóazonossággal vagy SSL/native bridge védelmek kikapcsolásával megoldani.

A helyes videó-ID/URL megmarad; ne cserélj tetszőleges másik videóra. A YouTube saját vezérlői, a teljes rendelkezésre álló modal-méret, a bezárás/háttér utáni pause és a hiba/ready állapot legyen valós. Ne nyisd ki automatikusan a külső YouTube-appot. Az aktív edzés ideje/GPS/tulajdonosa ne változzon a videóablaktól. Az app által engedélyezett HTTPS/URL/ID, tiltott natív bridge/fájlelérés/jogosultságkérés és SSL-hibánál megszakítás megmarad.

A régi referencia main5f026a56b9b6d33c84afa77eb3927b11f0bd6f6a: www/app.js openDemo ~11297–11308 közvetlen www.youtube.com/embed, playsinline/rel=0 és YouTube-vezérlő. A végső override is olvasandó, ne csak az első azonos nevű függvény. A régi külső ACTION_VIEW fallback nem az új appon belüli megoldás kötelező mintája.

- [app/src/main/java/com/example/ui/components/ExerciseVideoDialog.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/components/ExerciseVideoDialog.kt)
- [app/src/main/java/com/example/ui/components/inappvideo/InAppVideoPolicy.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/components/inappvideo/InAppVideoPolicy.kt)
- [app/src/main/java/com/example/ui/components/inappvideo/SecureExerciseVideoWebView.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/components/inappvideo/SecureExerciseVideoWebView.kt)
- [app/src/main/java/com/example/ui/components/ExerciseVideoResolver.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/components/ExerciseVideoResolver.kt)
- [app/src/test/java/com/example/followup/inappvideo/InAppVideoPolicyTest.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/test/java/com/example/followup/inappvideo/InAppVideoPolicyTest.kt)
- [app/src/test/java/com/example/ui/components/ExerciseVideoResolverTest.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/test/java/com/example/ui/components/ExerciseVideoResolverTest.kt)
- [scripts/test_exercise_video_player.mjs](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/scripts/test_exercise_video_player.mjs)
- [docs/COMPACT_UI_VIDEO_NO_BUILD_2026-11-03.md](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/docs/COMPACT_UI_VIDEO_NO_BUILD_2026-11-03.md)
- Régi kanonikus app.js: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/main/www/app.js

Már a mostani JS7 teszt is sikeres, miközben a készüléken a beágyazás hibás. További eseményteszt csak a tényleges állapot/fallback hibáját igazolja; működő YouTube-lejátszást végül készüléken is ellenőrizni kell. A forrásjavítást addig is készítsd el és teszteld, ne állj meg egy újabb általános javaslatnál.

## 3. Régi visszajelzés-formátumok fennmaradó auditja (#69)

A jelenlegi új per-exercise válaszok kanonikus értékei easy/light/right/challenging/hard/pain, a jegyzet exercise.feedback.note; a root edzésfeedback külön van. Ezek már a meglévő Coach-hoz és a kanonikus véglegesítőhöz kötöttek, a natív naplószerkesztőből utólag is módosíthatók. Nem feladat egy második feedback-motor.

Olvasd össze a régi WebView feldolgozást a natív import/mentés/Coach olvasóival. A bizonyított régi gépi aliasok, fordított értékek, note/applied/effortScore/rating mezők és ismeretlen adatmegőrzés útját auditáld. Csak forrásból vagy létező, személyes adatot nem tartalmazó fixture-ből bizonyított megfeleltetést normalizálj. Hiányzó RIR marad hiányzó; a tényleges0 külön marad. Régi automatikusan mentett RIR2 eredetét nem lehet utólag kitalálni, ne írj át tömegesen adatot. Ismeretlen effort/formátum maradjon sértetlen. Nem áll rendelkezésre ebben az átadóban a felhasználó korábbi személyes ZIP/JSON mentése; ne állítsd, hogy megvizsgáltad.

Tartsd meg a root/per-exercise feedback.applied zárat: már alkalmazott válaszból nem lesz második automatikus progresszió; jegyzet annotációként szerkeszthető. Stabil workout/set ID, sorozatérték, actualRir hiány/0, tombstone/CAS/replacement és export/import roundtrip megmarad. Az audit eredménye konkrét formátumtábla, bizonyított javítás és regresszió legyen; ha egy régi formátum bizonyíték hiányában nem értelmezhető, írd le ezt és őrizd meg.

- [app/src/main/java/com/repforge/app/StudioWorkoutFeedback.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/StudioWorkoutFeedback.kt)
- [app/src/main/java/com/repforge/app/NativeJournalEditor.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/NativeJournalEditor.kt)
- [app/src/main/java/com/repforge/app/TrainPilotWorkoutFinalization.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/TrainPilotWorkoutFinalization.kt)
- [app/src/main/java/com/repforge/app/data/migration/LegacyWebViewMigrator.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/data/migration/LegacyWebViewMigrator.kt)
- [app/src/main/java/com/repforge/app/ui/coach/NativeCoachReadiness.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/ui/coach/NativeCoachReadiness.kt)
- [app/src/main/java/com/repforge/app/ui/coach/NativeCoachAdviceRepository.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/ui/coach/NativeCoachAdviceRepository.kt)
- [app/src/test/java/com/example/ExerciseFeedbackRegressionTest.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/test/java/com/example/ExerciseFeedbackRegressionTest.kt)
- [app/src/test/java/com/example/StudioPrefillBindingRegressionTest.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/test/java/com/example/StudioPrefillBindingRegressionTest.kt)
- [docs/METRICS_EXERCISE_FEEDBACK_NO_BUILD_2026-11-03.md](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/docs/METRICS_EXERCISE_FEEDBACK_NO_BUILD_2026-11-03.md)

Régi app.js ugyanaz a main/raw link, mint a videónál. Kereshető referencia: TrainPilotWorkoutFeedback (~16869), tp197RirSave (~20241); a végső override és a mentett séma is számít. A sorok tájékoztató jellegűek, mindig a valóban beolvasott main fájlban keress.

## 4. Teljes telefonos HU/EN/DE/RO felület (#75)

Az előző részek végleges UI-szövegei után végezd. A Studio képernyők, natív popupok, editorok, üres/hiba/engedély/mentési állapotok, contentDescription és dinamikus feliratok is a választott appnyelvet kövessék. A teljes forrásfában keress beégetett magyar/angol feliratot, ne csak egy strings.xml-t fordíts le.

Használd a meglévő AppLanguage/LocalizedStrings és nativeResources mintákat, a megfelelő values/values-en/values-de/values-ro erőforrásokat. Nem kell párhuzamos nyelvi rendszer vagy minden személyes adat átírása. Felhasználói program-/gyakorlatnév, jegyzet, régi naplózott tartalom ne forduljon át automatikusan. Kanonikus JSON machine values/ID/enum ne legyen lefordítva; csak a megjelenítés. Számok, tizedesek, dátumok, mennyiségek és paraméterhelyek legyenek konzisztensen lokalizáltak. Ne szorítsd a rendszer fontScale-t1.0-ra hosszabb fordítás miatt; tördelés/kompakt layout kell.

- [app/src/main/java/com/example/model/TrainPilotModels.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/model/TrainPilotModels.kt)
- [app/src/main/java/com/example/ui/screens/HealthCoachAndSettingsScreens.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/screens/HealthCoachAndSettingsScreens.kt)
- [app/src/main/java/com/example/ui/screens/LogAndProgressScreens.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/screens/LogAndProgressScreens.kt)
- [app/src/main/java/com/example/ui/screens/WorkoutAndPlanningScreens.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/screens/WorkoutAndPlanningScreens.kt)
- [app/src/main/java/com/example/ui/screens/ActiveMeasurementScreens.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/screens/ActiveMeasurementScreens.kt)
- [app/src/main/res/values/strings.xml](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/res/values/strings.xml)
- [app/src/main/res/values-en/strings.xml](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/res/values-en/strings.xml)
- [app/src/main/res/values-de/strings.xml](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/res/values-de/strings.xml)
- [app/src/main/res/values-ro/strings.xml](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/res/values-ro/strings.xml)

További aktuális belépési utak:
- https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/localization/TrainPilotStrings.kt
- https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/components/WorkoutPresentationLabels.kt

Ezek kezdőpontok, nem a teljes erőforrás-leltár: a repóban a többi values* XML-t és localization/ fájlt is olvasd. HU és RO glyph-készletet az1. rész után újra ellenőrizd az újonnan választott szövegeken.

## 5. Adatvédelmi tájékoztató a csomag végén (#82)

A felhasználó eredeti kérése: a régi main adatvédelmi szövege legyen elérhető az új app **Beállítások / Adatvédelem** részében. Korábban ezt későbbre kérte; az egyben átadott teljes csomagban a többi funkció után végezd.

Van már natív PrivacyPolicy/HealthPrivacyActivity és a régebbi natív SettingsScreen-ben hozzá megnyitó, de a tényleges Studio Settings belépési útját kell bekötni. Először a meglévő kódot és a régi privacy.html-t hasonlítsd össze, ne írj új tájékoztatót a semmiből. A témázott, görgethető, kompakt popup vagy natív oldal a többi appfelülethez igazodjon, működő visszalépéssel és HU/EN/DE/RO nyelvvel.

**Konkrét elavulási veszély:** a jelenlegi natív PrivacyPolicy szövegben még szerepel, hogy a Samsung Health felirat nem közvetlen Samsung-kapcsolatot jelent; az új appban már közvetlen Samsung SDK-olvasás is van. Ezt a tényállítást ne másold ellenőrzés nélkül. A valós adatfolyamot/mentési hozzájárulást az aktuális backendből ellenőrizd, és a meglévő tájékoztató ellenőrizhetően elavult technikai részét pontosítsd. Fejlesztőnevet, emailt, dátumot, jogalapot, hatósági jóváhagyást vagy Play-megfelelőséget ne találj ki. A meglévő konfigurált kapcsolatadatokat használd; hiányt jelölj az átadóban. A hozzáférési UI nem módosíthatja/kapcsolhatja be az egészségadat-backup consentet.

- [app/src/main/java/com/example/ui/screens/HealthCoachAndSettingsScreens.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/example/ui/screens/HealthCoachAndSettingsScreens.kt)
- [app/src/main/java/com/repforge/app/PrivacyPolicyAndActivity.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/PrivacyPolicyAndActivity.kt)
- [app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt)
- [app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt)
- [app/src/main/java/com/repforge/app/data/migration/NativeBackupPrivacy.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/data/migration/NativeBackupPrivacy.kt)
- [app/src/main/java/com/repforge/app/NativeHealthBackupGate.kt](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/src/main/java/com/repforge/app/NativeHealthBackupGate.kt)
- Régi privacy HTML: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/main/www/privacy.html
- Régi natív privacy: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/main/android/app/src/main/java/com/repforge/app/PrivacyPolicy.java

## 6. Integráció, ellenőrzés és átadás

Egyetlen felelősként vidd végig a csomagot. A csak main nézet miatt minden új rész előtt olvasd vissza a tényleges aktuális fájlokat; ne alkalmazz vakon régi exportot vagy a 2744/2745-ös patch-eket. Ha a kódmódosítás GitHub-exporton jut vissza, pontos teljes patch-et/commitot és a forrás-main azonosítóját add meg, hogy adatvesztés nélkül integrálható legyen. A felhasználó teljes munkaeredményt kér a fenti saját scope-ban.

1. Rövid kiinduló leltár után ténylegesen készítsd el a kapott2–5. részt, az1. rész fájltulajdon-szabályával. Önálló ellenőrizhető commitok és változásjegyzék. Nem kell minden apró változás után APK.
2. A repo meglévő követelményei szerinti teljes Android CI; a 428-as kiinduló tesztkészlet és a JS-eseménytesztek is maradjanak érvényesek, szükséges új regressziókkal. A glyph-ellenőrzést, az egységfeliratok helyes metaadat-alapját, különböző napcímeket és valós videóhibás/fallback állapotot értelmesen ellenőrizd. Ne oldj meg adat-/motorhibát tesztkihagyással vagy adatnullázással.
3. Kód csak zöld ellenőrzés után mainre a rendelkezésre álló GitHub-folyamaton. Ismeretlen eszközkorlát esetén ne állítsd, hogy már mainen van. Javítsd az ütközéseket az aktuális mainnel, őrizd meg a közben bekerült felhasználói munkákat.
4. Egy végső, aktuális mainből készült aláírt telefonos teszt-APK. A versionCode legyen nagyobb a tényleges main és a legutóbb átadott APK kódjánál; kiinduló2748 alapján2749 lehet, de előbb olvasd vissza, közben más kiadás is készülhetett. Verzióellenőrző és a meglévő phone/Wear páros metaadat-szerződés együtt frissüljön. Wear-logikához nem kell hozzányúlni a telefonos felület miatt.
5. Meglévő állandó aláíró/signing pipeline, package com.TrainPilot.com, Maps-konfiguráció és páros azonosítók maradjanak. Ne kérj/jeleníts meg kulcsot, jelszót vagy Maps API értéket. A publikus cert fingerprint/hash/verzió/CI-forrás ellenőrizhető. Nyers JUnit XML, helyes forrás-SHA, APK-méret/hash/verzió/signer ellenőrzés és tényleges letöltési link kell. Ha nincs jogosultságod buildhez/feltöltéshez, konkrét kész kódot és a hiányzó műveletet add át; ne generálj kitalált APK-linket.
6. Friss CURRENT_STATUS, issue-státuszok, rövid telefonos tesztlista; implementáció, CI és fizikai elfogadás külön jelölve. Az új szövegek négy nyelven, a betűk és témák 393dp körüli telefonon/nagyított betűnél, popupok/mentés nyitott billentyűzettel; videó beágyazás vagy megbízható appon belüli lejátszási út készüléken is ellenőrizendő.

- [app/build.gradle.kts](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/app/build.gradle.kts)
- [wear/build.gradle.kts](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/wear/build.gradle.kts)
- [.github/workflows/trainpilot-android-ci.yml](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/.github/workflows/trainpilot-android-ci.yml)
- [.github/workflows/trainpilot-persistent-signing.yml](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/.github/workflows/trainpilot-persistent-signing.yml)
- [scripts/verify_android_apk_metadata.py](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/scripts/verify_android_apk_metadata.py)
- [scripts/verify_junit_xml.py](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/scripts/verify_junit_xml.py)
- [docs/RELEASE_2748_UI_METRICS_FEEDBACK.md](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/docs/RELEASE_2748_UI_METRICS_FEEDBACK.md)

## Végig megőrzendő korlátok

- Egyetlen meglévő natív motor, Room/DataStore és health/GPS tárolás; nincs párhuzamos adatbázis vagy motor.
- Stabil workout/set ID, ownership/ACK/epoch, explicit resume, finalization/discard, replacement/restore, tombstone és privát fotó/GPS-védelem.
- Valós mérés, tényleges forrás és időpont; hiányzó érték nem nulla, actualRir hiány és0 külön, átfedő források nem összeadandók.
- Alapból kikapcsolt egészségadat-backup consent; a téma/nyelv/privacyszöveg ezt nem engedélyezheti.
- Az elfogadott fullscreen GPS-gombelrendezés változatlan. A mostani kis egységfelirat-javítás nem jelent új mérési/volumenszámítást.
- A BLE-kezelő és automata/intervallum további bekötése sztornó. A meglévő működő backend/adat ettől nem törlendő.
- Forrásparser, képernyőkép, mock és videó-API eseményteszt önmagában nem tényleges készülékes működési bizonyíték. Pontosan írd le, mi futott és mi maradt visszatesztre.

**Elvárt végső válasz:** mely issue-k készültek el, érintett commit/main-forrás, tényleges ellenőrzések és számok, valódi APK-link vagy eszközkorlát esetén alkalmazható átadó, megmaradt bizonytalan adatok/hibahelyzetek rövid listája. Ne csak javaslatot vagy újabb általános tervet adj vissza.

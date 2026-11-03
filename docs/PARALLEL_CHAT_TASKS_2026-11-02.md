## Legfrissebb munkakiosztás – 2026-11-03, PR #58

A Coach rövid összefoglalója és a piros, naplólistából is elérhető edzéstörlés a [#58 PR](https://github.com/fuloplevente1998/Native-AI-Studio/pull/58) része; CI-ellenőrzés folyamatban. Ebből most nincs új APK vagy verzióemelés, az utolsó kiadott pár 2743.

A többi munkán itt nem dolgozunk: [négy külön fejlesztői feladat és integrátor](chat-tasks-independent-2743/README.md), [kizárólagos fájltulajdon](chat-tasks-independent-2743/FAJLTULAJDON.md). Nyitott: #59 appon belüli videó, #60 óraadatok két blokkban, #61 pulzus/regeneráció adatútja, újranyitott #48 nagy térkép a friss készülékes képek alapján. Az 1–4 párhuzamosan készülhet; a közös képernyőket/ViewModelt kizárólag az 5. integrátor köti be az átadások után. A korábbi #48-kész jelölést ez az új megfigyelés felülírja; a JVM-ellenőrzés nem készülékes elfogadás.

# TrainPilot – külön ChatGPT/Codex chateknek kiosztható feladatok

## Aktuális checkpoint – 2743, a készülékes javítások mainen

PR #49 appforrás: `b0d315a1cf70ab65a48ce4e2bdec4a71d027216c`.
Main merge: `a8605b0b5c2e68ef11a7ec224ad912a232f61bbc`, azonos app/Wear/shared/scripts/workflow fákkal.
PR Actions [41561039880](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41561039880) SUCCESS;
letöltött nyers XML **358 = 182 telefon + 176 Wear**, nulla failure/error/skip,
artifact `13197675023`.

Teljes main signed Actions [41561684993](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41561684993)
**SUCCESS**, ugyanazon appforrásból. Letöltött nyers XML: **358 = 182 telefon + 176 Wear**, 0 failure/error/skip.
Verzió **2743 /0.1.0-beta**, mindkét package `com.TrainPilot.com`, változatlan állandó signer SHA256
`a72fd016d94e9d94e2b2282032f8c3820d4f09cba9ff1db05c3fd1de6ffa7dd6`.
APK/metadata hash, package/verzió, Maps/pair/capability/hardware flag és a tényleges új device-info
DEX-útvonal mindkét APK-ban ellenőrizve. Signed artifact `13197093085`, jelentés `13197887370`.

- [Telefon APK](https://drive.google.com/file/d/1SJMkiTMbUm3NjWa12JFEcIpGgQuBrOrj/view): 19514564 bájt; SHA256 `fb1bd01ad6cf0e9c1faa6fb571de673fd9c40bfe9bc7c0a0d1f9962cbea54b44`.
- [Wear APK](https://drive.google.com/file/d/1t8gyLDIyYTqlMpeOqzPN5P3on7VCThjK/view): 4636863 bájt; SHA256 `be5e05397d2dfd5f147aee95f0980a914fa27c3622a708af2251639f5bfec8b1`.

Mindkét külön APK a meglévő TrainPilot Native `1ywHgrManynjV9uNvZfVncOmCr3SbHIb9` mappában;
a bájtméret és szülőmappa külön Drive metadata-readbackkel igazolt. Korábbi fájlok megmaradtak,
nincs kulcscsere, adattörlés vagy Play/GitHub release. Fizikai elfogadás még nem igazolt.


Implementált (ne oszd ki újra):

| Hibajegy | Kész kódjavítás |
|---|---|
| #43 / korábbi 8. feladat | Megerősített mentettedzés-törlés a Studio részletben; tényleges Room/privátfájl-törlés, minimális tombstone és tartós/újrapróbálható takarítási nyugta; megosztott/aktív adatok védelme |
| #44 | Világos/sötét naptár, napcellák, műveletek és tervező az app palettájával |
| #45 | Térkép alapból app-téma szerint; korábbi valóban mentett kézi mód megmarad, App témája gombbal visszaállítható a követés |
| #47 | Órakoppintás / Kapcsolt eszközök / Frissítés valódi, csak olvasó Wear kérés/válasz; egyező node/requestId, valódi akkumulátor/eszköz/verzió, hiány/timeout állapot |
| #48 | Kisebb kompakt térkép; fullscreen fejléc/idő/X/primer vezérlők a térképen belül, safe insetekkel és mért overlayméretű Google-margókkal |
| #50 | Google Cloud összecsukható a Studio/natív Beállításokban; műveleti hibák/visszajelzések és consent megmarad |
| #51 | Home/Egészség közös 3×2 Mai állapot, kompakt Home és Coach-navigáció; normál elférés, nagy betű/szűk ablak esetén hozzáférhető görgetés |
| #52 | GPS újraindítás új natív ID-val/idővel, natív eredmény nélküli fantom UI nélkül; aktív Home-kártya a saját sessionjét mutatja/nyitja a kijelölt programtól függetlenül; stale elvetés védelme marad |
| #53 | Import/mentés/törlés után valódi helyi napi/heti edzésszám, naptári hét hétfőtől máig; nem a teljes napló mérete. A 28 korábbi rekord nem törlődik/íródik át |
| #54 | Tényleges volumen-százalék az előző, azonos hosszúságú 1/7/30/90 naphoz; előjel/irány/szín, előző pozitív volumen hiányában „—” |

Készülékes elfogadás külön: [tesztlista](DEVICE_TEST_CHECKLIST_2026-11-03.md).
Az órás lekérés új válaszadójához **mindkét 2743-as APK-t telepíteni kell**.
A HC eredeti ürességének fizikai oka továbbra sem bizonyított; a már kész #41 providerizolációt ne írd újra.
A korábbi „nyitott/kiosztható” 8. feladat és #43–#54 státuszok történetiek.

## Korábbi checkpointok és készülékes megfigyelések

## Aktuális készülékes teszt – 2026-11-03, PR #38–#41

A main `3f3195a` tartalmazza a #38–#41-et és a #42 2742-es kiadást. A felhasználó
az összevont APK-t teszteli. A #34 térközjavítás (#39), #35 Egészség szövegblokk (#38),
#11 videójavítás (#40) és a providerizoláció (#41) **implementálva**, készülékes
elfogadásuk a mostani teszt feladata. A HC eredeti ürességének oka továbbra sem bizonyított.

Nyitott munkák:

- [#43 – Naplózott edzés törlése](https://github.com/fuloplevente1998/Native-AI-Studio/issues/43): a korábbi 8. feladat; a Studio részletnézet gombja és a teljes tartós törlés auditja még hiányzik. Ez nem az aktív edzés kész Elvetés művelete.
- [#44 – Világos naptár](https://github.com/fuloplevente1998/Native-AI-Studio/issues/44): a világos appban sötét naptárpanelek és rossz kontraszt, felhasználói referencia 01:51-es kép.
- [#45 – Térkép alapértelmezett témakövetése](https://github.com/fuloplevente1998/Native-AI-Studio/issues/45): alapból az app világos/sötét módját kövesse; kézi kapcsoló megmarad. Ez új igény, a jelenlegi külön mentett kapcsoló még nem követi a témát.
- HC készülékes okfeltárás: a #41 providerkeveredési hibajavítása nem bizonyítja a Samsung→HC adatmegosztás vagy a HC-üresség konkrét okát.

Tesztsor: [DEVICE_TEST_CHECKLIST_2026-11-03.md](DEVICE_TEST_CHECKLIST_2026-11-03.md).
A régebbi „#34/#35/#11 még nincs implementálva” státuszok történeti állapotok.

## Korábbi állapotok

Önálló, külön chatnek átadható MD-fájlok: [nyitott feladatok indexe](chat-tasks-open-2741/README.md). Minden feladat a közös munkaszabályokat is tartalmazza.

## Aktuális munkalista – a 2741-es kiadás utáni visszajelzésekkel

Ez az aktuális kiosztható lista; az alatta megőrzött korábbi kiosztás történeti terv.
Az 1–6 fejlesztés és integráció kész (main, 2741, mindkét teljes futásban 315/315 teszt).
A készülékes elfogadás külön állapot, nem indok a kész funkciók újraírására.

| Munka | Állapot | Kiosztás / elfogadás |
|---|---|---|
| 1–6: keretek/forrásharmonika, grafikon, aktív edzés elvetése, térkép, Home blokk, HC-diagnózis | Fejlesztés és kiadás kész | Készüléken ellenőrizendő; új észrevételek külön hibákban |
| 7: integráció/kiadás | A 2741-re kész | A következő javításokhoz ismét integrátori szerep szükséges |
| 8: naplózott edzés valódi törlése | Nyitott, kiosztható | Külön chat: megerősítés, tényleges tárolóbeli törlés és kapcsolódó privát fájlok kezelése, szükséges tombstone/restore-védelmek megtartásával; nem az aktív edzés kész Elvetés gombja |
| [#34 – Kisebb térközök az app egészében](https://github.com/fuloplevente1998/Native-AI-Studio/issues/34) | Új, nyitott UI-hiba | A #35-tel egy UI-chatnek; Kezdőlap és minden telefonos képernyő, megőrzött érintési felületek/olvashatóság |
| [#35 – Ismétlődő Egészség-szövegblokk](https://github.com/fuloplevente1998/Native-AI-Studio/issues/35) | Új, nyitott UI-hiba | A #34-gyel egy UI-chatnek; a Mai állapot fejléc alatti plusz sorok eltávolítása, kártyák/részletek/frissítés/érdemi hibadiagnózis megtartása |
| [#11 – Oktatóvideó nem indul](https://github.com/fuloplevente1998/Native-AI-Studio/issues/11) | Nyitott, kiosztható | Külön videós hibajavító chat |
| HC üres adat készülékes okfeltárása | Nyitott készülékes vizsgálat | Külön diagnosztikai chat; a 6. feladat diagnosztikai kódja kész, a konkrét ok nem bizonyított |

A #34 és #35 a 2741 kipróbálása után jelzett **új javítás**, nem az 1–6 újranyitása.
Referenciák az eredeti beszélgetésben: 23:09 Kezdőlap (térközök), 23:10 Egészség
(térközök és a kártyákat ismétlő szöveg). A képeket az érintett chatnek az eredeti
üzenetből kell mellékelni; a repo nem tartalmaz külön képfájlokat.

Új munka a friss `main`-ből, saját ágban/worktree-ben induljon, PR-célág `main`.
A korábbi #28 integrációs ág már merge-elve; az alábbi régi ág-/2740-utasítások
nem érvényes kiindulópontok. Olvasd el az aktuális átadót, a konkrét hibajegyet,
`.specify/memory/constitution.md` és `docs/FRAME_DESIGN_SYSTEM.md` szabályait.
Alügynököt ne indíts; csak a kiosztott területet módosítsd. A közös fájlokat jelezd az
integrátornak, különösen a #34 app-szintű stílusváltoztatásait. A buildszám, signer,
Actions-ellenőrzés és a végleges APK-pár az integrátor feladata.

## Felülíró integrációs állapot – 1–6 elkészült

A felhasználó kérésére az 1–3 itt befejezve, a külön chatek 4–6. javításai is
integrálva. #28 main merge: `2d7171b9cce7762553e053ab98b9828d3e7f2fc5`.
PR-forrás: `c965d01e08bcd0cf6d77ea02e407005b266398b8`, Actions `41529082323`
SUCCESS; letöltött nyers XML **315 = 139 telefon + 176 Wear**, nulla failure/error/skip.
A HC-olvasás diagnózisa elkészült; a készülékes üresség oka még nem bizonyított.
A kész kódokat ne implementáld újra. A kiadott APK-pár legfrissebb bizonyítékát és
linkjeit az aktuális átadó legfelső blokkjából vedd. A 8. feladat, naplózott edzés
valódi törlése, külön marad; az aktív/szüneteltetett edzés elvetése már kész.

Az alábbi kiosztás **történeti munkaterv**, nem az aktuális befejezetlenség listája.

# Közös utasítás – minden chatnek

Repo: https://github.com/fuloplevente1998/Native-AI-Studio
Integrációs ág / draft PR: `fix/unified-frame-hierarchy`, #28.
Már elmentett munkakód: `50d707c` (nem kész, nem igazolt APK). Ebből a snapshotból
vagy az integrációs ág legfrissebb, ellenőrzött állapotából készíts saját ágat és
külön worktree-t. A feladat-PR célága az integrációs ág legyen, ne automatikusan main.
A jelenlegi main 2740-es APK-ja az utolsó kiadott, 296 teszttel igazolt csomag.

Olvasd el az aktuális átadót, #19-et, #28-at, a `.specify/memory/constitution.md`-t
és `docs/FRAME_DESIGN_SYSTEM.md`-t. A már megkezdett kódot ellenőrizd és fejezd be;
ne implementáld újra pusztán egy nyitott készülékes checkbox miatt.
Csak a kiosztott területen dolgozz. Közös fájl módosítását jelezd az integrátornak.
Alügynököt ne indíts. Magyarul kommunikálj. Natív Compose/Room/DataStore/motor,
ownership/ACK, finalization, tombstone és backup/restore védelmek maradjanak.
Ne törölj adatot, ne adj össze provideradatokat, ne használj kitalált mérést.
Verziót, signert, titkokat és kiadási workflow-t ne módosíts önállóan. Android-build
és teszt a meglévő Actionsből; ne állíts sikeresnek folyamatban levő futást.
A main merge, közös buildszám és a végleges signed APK-pár az integrátor feladata.

A „régi app” a korábbi WebView-változat neve. A referenciaképek számai kizárólag
példák; nem kerülhetnek mérési adatként a termékbe.


# Referenciaképek – a felhasználó ebben a beszélgetésben küldte

A képfájlok nem állnak külön fájlként rendelkezésre ebben a munkakörnyezetben.
E csomag nem tartalmaz képernyőképeket: az alábbi képeket az eredeti üzenetből
kell az érintett új chathez mellékelni. A külön, korábban feltöltött backup ZIP
személyes mentés és fotók, nem a felületi referenciaképek gyűjteménye.

| Kép | Azonosítás | Szerep | Chat |
|---|---|---|---|
| A | 21:22, natív Fejlődés/edzésrészlet: WearOS + Samsung + HC mind nyitva | Javítandó állapot, ne ezt másold | 1 |
| B | 21:25, régi app Kezdőlap: Személyes edzéstervező / Mai állapot, finom belső keretek | Kívánt kerethierarchia | 1 |
| C | 21:29, régi app 30 nap: 13 085 kg, oszlopok 4, 11, 18, 25, 1 | Heti csoportok és kompakt ábra | 2 |
| D | 21:29, régi app 1 nap: 1254 kg, egy keskeny középső oszlop | Egy napi oszlop helyes szélessége | 2 |
| E | 21:40, natív világos Google Maps, Bábonymegyer környéke | Jelenlegi állapot; sötét kapcsoló + végpontok hiányoznak | 4 |
| F | 21:41, natív Kezdőlap: Folytatás, Gyors edzés, Galaxy Watch7, majd keret nélküli Mai adatok blokk | Elvetés hiánya és eltávolítandó adatblokk | 3, 5 |

A 3 hónapos görgetéshez külön kép nem kell: a felhasználó pontosította, hogy minden
oszlop egy hét, oldalra görgethető, megnyitáskor a legújabb hetek látszanak.
A régi app kódjában a `showLatestBucket()` ezt `scrollLeft = scrollWidth - clientWidth`
beállítással végzi: időrend marad balról jobbra, a kezdeti nézet a jobb szélen áll.

---

# Chat 1 – Keretrendszer és adatforrás-harmonika

Referencia: A (hibás natív) + B (kívánt régi megjelenés).

A teljes telefonos felületen külső oldal-/csoportkeret legyen hangsúlyosabb,
belső paneleké vékonyabb, gomboké halványabb. Kiinduló közös értékek: 1 dp / 0,75 dp /
0,5 dp. Az eredeti CSS-ben több 1 px-es keret van, de a density/zoom miatt nem
feltétlenül azonos a fizikai megjelenés. A hiba-/fókuszjelzések olvashatósága maradjon.
A három egészségforrásból egyszerre csak egy nyílhat; másik nyitása zárja az előzőt,
ugyanarra kattintás mindet zárja. Egy közös csoportkeret, vékony gombkeretek,
erős külön tartalomkeret nélkül. Forrásadatok és műveletek elkülönülnek.

Megkezdve: TrainPilotFrames.kt, TrainPilotOutlinedButton.kt, MainActivity közös host,
JournalHealthPanel.kt, a meglévő telefonos keretek migrációja és Journal regresszió.
Ezeket ellenőrizd, ne készíts új párhuzamos rendszert. Elsődleges fájltulajdon:
`com/repforge/app/ui/theme/TrainPilotFrames.kt`, `TrainPilotOutlinedButton.kt`,
`ui/journal/JournalHealthPanel.kt`, keretrendszer-dokumentum és kapcsolódó tesztek.
A HomeScreen és LogAndProgressScreens tartalmi átalakítását egyeztesd a 2/3/5. chattel.
Elfogadás: egy nyitott panel, helyes restore/providerértékek; minden fő útvonalon
következetes keret, nagy betűméretnél is használható gombok.

---

# Chat 2 – Volumengrafikon a régi app működésével

Referencia: C + D. A felhasználó pontos elvárása:
- 1 nap: egy nap egy oszlop, keskeny és középen, nem teljes grafikon-szélességű.
- 7 nap: egy oszlop egy nap.
- 30 nap és 3 hó: egy oszlop egyheti összesítés; az intervallum szélén részleges hét lehet.
- 3 hónap: vízszintesen görgethető; kezdetben a jobb szél, a legfrissebb hetek látszanak.
Az időrendet ne fordítsd meg. Ne ugorjon vissza újra a legfrissebbre minden kézi
kiválasztáskor. Valódi napi szűrés, fejlécösszeg és oszlopösszeg egyezzen. Szép,
adatból számolt tengely, finom rács, olvasható dátumok; hiányból ne legyen ál-oszlop.

Megkezdve: StudioProgressWindow.kt, StudioVolumeChart.kt, kapcsolódó Window/UI tesztek
és a LogAndProgressScreens hívási pontjai. Elsődleges tulajdon e komponensek;
a képernyő nagy közös fájljában csak a volumenrészhez nyúlj. Ellenőrizd a legutolsó
részleges hetet, időzónát/DST-t, a keskeny egynapos ábrát és a kezdeti jobb oldali scrollt.

---

# Chat 3 – Elvetés a Folytatás mellett

Referencia: F felső része (most csak Folytatás van).
Szüneteltetett / félbehagyott aktív edzésnél a Folytatás mellett legyen Elvetés gomb.
Az elvetés kérjen megerősítést. Csak az akkor megjelenített edzést érintheti;
időközben új edzésre váltás nem törölhet másikat. Mégse semmit nem változtat.
Órás tulajdon, pending átadás, mentés/adatcsere esetén a meglévő védelmek érvényesek.
Korábban mentett edzés nem törölhető ezzel az aktív-edzés művelettel.

Megkezdve: StudioPausedWorkoutActions.kt; HomeScreen/MainActivity bekötés;
TrainPilotViewModel.stopActiveWorkoutSession(expectedWorkoutId) azonosítóellenőrzése.
A valódi művelet a meglévő TrainPilotWorkoutFinalization.discardWorkout marad.
Elsődleges tulajdon az új komponens és annak tesztjei; a HomeScreen adatblokkja az 5.
chaté. ViewModelben csak az elvetési metódust módosítsd, ha szükséges.
Teszt: megerősítés előtt nincs callback, Mégse nem töröl, megerősítés egyszer,
azonosítóváltás és tiltott állapot nem enged rossz törlést, tárolási hiba nem állít sikert.

---

# Chat 4 – Térkép sötét kapcsolóval és végpontokkal

Referencia: E (jelenlegi natív világos térkép, nem kész sötét referenciaterv).
Külön kapcsolóval lehessen sötét/világos térképstílust választani. Jelölje a tényleges
rögzített útvonal első és utolsó pontját, eltérő, jól látható jelöléssel. Egyetlen pont
vagy azonos rajt/cél esetén ne állítson két külön helyet. Üres útvonalnál nincs kitalált
hely/útvonal. Az útvonal szünet-/tulajdonosváltási réseit tilos egyenes vonallal összekötni.
A meglévő Google Maps konfiguráció, jogi logó, kamerakezelés és privát koordináták védelme
maradjon. Stílusváltás ne változtassa meg a mentett GPS-adatot vagy indítson új mérést.

Még nincs implementálva. Elsődleges fájl: AnatomicalBodyAndMapComponents.kt /
NativeWorkoutRouteMapBox, a map-style resource, szükség esetén a meglévő DataStore helyi
UI-beállítása. A komponens a napló és az aktív mérés térképén is működjön.
A LogAndProgressScreens.kt nagy közös fájlját csak egyeztetve módosítsd. Maps-kulcsot ne cserélj.

---

# Chat 5 – Keret nélküli Mai adatok blokk eltávolítása a Kezdőlapról

Referencia: F, a Galaxy Watch kártya alatti „Mai adatok • Samsung Health” és az alatta
levő szinkronidő/lépés/aktív és teljes energia/státusz sorok.
Ez a rész a Kezdőlapon ne jelenjen meg. A tényleges egészségkártyák (alvás, pulzus,
lépések stb.) maradjanak. A részletes adat a megfelelő Egészség oldali lenyitásokból
legyen elérhető; ezek a felhasználó szerint már megfelelően működnek.

Még nincs implementálva. Elsődleges tulajdon: HomeScreen.kt ezen adatblokkja.
A StudioDailyHealthStatus közös komponenst ne töröld és ne rontsd el az Egészség oldali
használatát; ne állítsd le az adatolvasást és ne töröld a cache-t. A 3. chat által
bekötött Folytatás/Elvetés részt őrizd meg. Teszt: a Kezdőlapon eltűnt a duplikált
szövegblokk, a kártyák és az Egészség oldali részletek/forrásadatok megmaradtak.

---

# Chat 6 – Health Connect üres eredmény, külön diagnózis

Ez a #19-ben külön rögzített megfigyelés, nem igazolt ok: Health Connect kijelölésével
nem volt adat; Samsung Healthre váltás és szinkron után azonnal működött.
Ellenőrizd a tényleges grantokat, Samsung→HC adatmegosztást, kiválasztott origint,
helyi napi dátumablakot, provider-cache váltást és az üres eredmény/hiba megkülönböztetését.
Ne állíts automatikusan alkalmazáshibát, ha HC-ben nincs forrásadat. Ne adj össze
SH és HC értékeket, és ne legyen csendes fallback. A már működő Samsung adatút maradjon.

Külön vizsgálati ág, a javítás előtt rögzített ok és reprodukció szükséges. Referencia:
#19 készülékes kommentek; F csak a működő Samsung út megfigyelése, HC-hibát nem bizonyít.
Elsődleges terület: HealthConnectRepository, provider-/grant/cache réteg. A 3. chat
elvetési ViewModel-részét és a többi UI-feladatot ne módosítsd.

---

# Integrátor – a külön feladatok összefésülése

Fogadd a külön PR-okat az integrációs ágra, ellenőrizd a közös fájlok konfliktusait és
az aktuális #28 CI-t. Az alap munkakód még nem igazolt. Ne olvassz össze pusztán azért,
mert egy előző SHA másik buildje sikeres volt. Egységes keretrendszer és elnevezés legyen.
A végleges PR címe/leírása a végső működést fedje le.

Futtasd a releváns regressziókat, majd a teljes telefon/Wear Actions-t, olvasd a nyers
XML-t. A felhasználó korábban engedélyezte a kész, ellenőrzött javítás mainre vitelét.
Ezután mainről állandó kulcsos teljes signed build. A jelenlegi munkakód 2741-re léptet;
a feladat-chatek ne léptessék külön a verziót. Maradjon com.TrainPilot.com / 0.1.0-beta,
ugyanaz a signer mindkét APK-ban. Ellenőrizd a forrás-SHA-t, manifestet, verziót, signer-
és fájlhash-t; a két külön APK a TrainPilot Native Drive-mappába kerüljön, méret/szülő
readbackkel. Frissítsd az átadót/promptot/#19-et. Eszközös elfogadás csak a felhasználó
valódi tesztjével kész. A 2740-es APK ettől külön, korábban igazolt kiadás.

---

# Chat 8 – Naplózott edzés valódi törlése

Ez külön feladat a 3. chat aktív/szüneteltetett edzésének elvetésétől.
A naplózott edzés részletein legyen Edzés törlése gomb és megerősítő párbeszéd.
A törlés valóban távolítsa el a kiválasztott mentett edzést a Room tárolóból és a
hozzá tartozó, más adathoz nem szükséges sorozat-/gyakorlat-/fotó-/GPS adatokat és
helyi fájlokat. Nem elegendő a listából vagy a memóriabeli UI-ból elrejteni.
Más edzés, megosztott fájl vagy másik aktív session nem sérülhet. Siker után a napló,
statisztikák, PR-/volumen-/kardióösszesítések a tényleges maradék tárolt adatokból frissüljenek.

Meglévő kiindulás: JournalViewModel.requestDeleteWorkout/cancelDeleteWorkout/
confirmDeleteWorkout és WorkoutHistoryDao.deleteWorkoutByIdOrKey. A backend Room-
tranzakcióban töröl és tombstone-t ír, de a Studio képernyőn nincs bekötött törlés.
Ne csak a gombot kösd be: auditáld a kapcsolt helyi fájlok valódi takarítását is,
az SQL cascade önmagában nem bizonyítja a fotó/GPS fájl eltávolítását.
Használd újra a meglévő privát fotó-/route-/szinkrontörlési mechanizmusokat.
A minimális tartós törlési jelző megmarad a régi sync/import példányból történő
visszaéledés megakadályozásához; a felhasználó nem egy új archív másolatot kér.
Egy korábban külön exportált ZIP-et ez nem ír át; ennek eltüntetését ne állítsd.

Elsődleges tulajdon: JournalViewModel törlési út, szükséges tároló-/fájltakarítás,
LogAndProgressScreens edzésrészletének törlési gombja és MainActivity bekötése.
A 2. chat grafikonrészét és a 3. chat aktív-session elvetését ne írd felül.
Teszt: Mégse nem módosít; csak azonosított mentett edzés törölhető; újranyitás/újraindítás
után sincs a tárolóban; nincs fölös helyi fájl; megosztott/idegen adat megmarad;
a régi sync nem hozza vissza; SQL-/fájlhiba nem mutathat teljes, hamis sikert.
Külön referencia nem szükséges: az elvárt működés a fenti, gombhely a naplózott edzés részlete.

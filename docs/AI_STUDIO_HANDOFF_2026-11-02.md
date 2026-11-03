## Legfrissebb munkakiosztás – 2026-11-03, PR #58

A Coach rövid összefoglalója és a piros, naplólistából is elérhető edzéstörlés a [#58 PR](https://github.com/fuloplevente1998/Native-AI-Studio/pull/58) része; CI-ellenőrzés folyamatban. Ebből most nincs új APK vagy verzióemelés, az utolsó kiadott pár 2743.

A többi munkán itt nem dolgozunk: [négy külön fejlesztői feladat és integrátor](chat-tasks-independent-2743/README.md), [kizárólagos fájltulajdon](chat-tasks-independent-2743/FAJLTULAJDON.md). Nyitott: #59 appon belüli videó, #60 óraadatok két blokkban, #61 pulzus/regeneráció adatútja, újranyitott #48 nagy térkép a friss készülékes képek alapján. Az 1–4 párhuzamosan készülhet; a közös képernyőket/ViewModelt kizárólag az 5. integrátor köti be az átadások után. A korábbi #48-kész jelölést ez az új megfigyelés felülírja; a JVM-ellenőrzés nem készülékes elfogadás.

# TrainPilot Native – aktuális AI Studio átadás, 2026-11-02

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

## Új, nyitott feladatok a 2741 után

- [#34](https://github.com/fuloplevente1998/Native-AI-Studio/issues/34): kisebb kártya-/ablakközi térközök az egész telefonos appban, Kezdőlapon is.
- [#35](https://github.com/fuloplevente1998/Native-AI-Studio/issues/35): az Egészség / Mai állapot kártyái fölötti redundáns szövegblokk eltávolítása; a kártyák, részletek és érdemi diagnózis megőrzése.

Mindkettő rögzített, **még nem javított** készülékes UI-hiba; együtt kioszthatók egy
UI-chatnek. A korábbi 8. naplózottedzés-törlés, #11 videóhiba és a HC készülékes
okfeltárása továbbra is külön nyitott munka. A részletes, aktuális munkalista:
[PARALLEL_CHAT_TASKS_2026-11-02.md](PARALLEL_CHAT_TASKS_2026-11-02.md).
Az 1–6 fejlesztése/2741-es kiadása kész; fizikai elfogadásuk külön marad.

## Aktuális checkpoint: #28 / 2741, az 1–6 integrálva

Main appforrás: `2d7171b9cce7762553e053ab98b9828d3e7f2fc5`; PR-forrás:
`c965d01e08bcd0cf6d77ea02e407005b266398b8`. Az app/Wear/shared/scripts/workflow
fák eltérése a két forrás között üres. PR Actions [41529082323](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41529082323)
SUCCESS; letöltött nyers XML **315 = 139 telefon + 176 Wear**, nulla failure/error/skip,
artifact `13181831099`.

Teljes main signed Actions [41530039869](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41530039869) **SUCCESS**,
ugyanazon mainforrásból. Letöltött nyers XML: **315 = 139 telefon + 176 Wear**, 0 failure/error/skip.
Verzió **2741 / 0.1.0-beta**, mindkét package `com.TrainPilot.com`; állandó signer SHA256
`a72fd016d94e9d94e2b2282032f8c3820d4f09cba9ff1db05c3fd1de6ffa7dd6`.
APK-hash/metadata, Maps-konfiguráció, phone/Wear pair identity/capability és Wear hardware flag ellenőrizve.
Signed artifact `13182796971`, nyers jelentés `13183071147`.

- [Telefon APK](https://drive.google.com/file/d/1qDgWk5fYe6D-KlWViBDsWtoyFKuUWhmh/view): 19498180 bájt; SHA256 `75544522684b570836a6a04a1089367e5d75158577ddf47cb197c2f49c923fbc`.
- [Wear APK](https://drive.google.com/file/d/1unGr2HqcboKYvzB9Q1IgBIgFphKPV7Vc/view): 4636863 bájt; SHA256 `157ff7a1b9491dceb98415040015511f055782b117d6e4d068257d1c074f1b27`.

Mindkét külön APK a TrainPilot Native `1ywHgrManynjV9uNvZfVncOmCr3SbHIb9` mappában;
a méret és a szülőmappa külön metadata-readbackkel igazolva. A korábbi fájlok megmaradtak;
nincs kulcscsere, adattörlés vagy Play/GitHub release. Ez tesztelhető kiadás, nem fizikai elfogadás.


Implementált, ne kezdd újra:

1. Közös oldal-/csoportkeret, halványabb belső panelek és vékony körvonalas gombok;
   a három naplózott egészségforrásból egyszerre egy nyílik, adataik elkülönülnek.
2. 1/7 nap napi, 30/90 nap heti volumenoszlopok. Keskeny egynapos oszlop;
   90 nap vízszintesen görgethető, induláskor a legfrissebb heteknél, kézi választást megtartva.
3. Szüneteltetett aktív edzésnél Elvetés a Folytatás mellett, megerősítéssel;
   ugyanaz a natív elvetési út, edzésazonosító-/ownership-/foglaltállapot-védelemmel.
4. #30: mentett sötét/világos térképkapcsoló, valódi rajt-/célpont, megőrzött GPS-szegmenshatárok.
5. Main `e9bfba6`: a Kezdőlap duplikált, keret nélküli Mai adatok blokkja kikerült;
   az egészségkártyák és az Egészség oldal működése megmaradt. A #29 azonos javítása lezárva.
6. #31: friss HC-olvasás külön diagnózisa (üres adat, grant, SDK-hiba, forrásszűrő),
   elkülönült HC/Samsung cache. **A készülékes HC-üresség kiváltó oka nem bizonyított.**

Ez még nem fizikai készülékes elfogadás. Tesztelendő: a referenciákhoz illő keretek,
forrásharmonika, négy időszak grafikonja, szüneteltetett edzés megerősített elvetése,
mentett térképmód és rögzített végpontok, Kezdőlap és HC-diagnózis. #19 marad nyitva.
A **8. feladat**, naplózott/saved edzés valódi törlése külön marad; ne keverd az aktív
edzés kész elvetésével. Spec Kit/Codex már integrálva (#26); a közös kerethierarchia
1.0.1-es alkotmányba emelése megállapodott megjelenítési szabály, adatmigráció nélkül.

## Történeti checkpointok

## Aktuális checkpoint: #25 / 2740, valamint Codex Spec Kit #26

A napi egészségképernyő #19-ben jelzett hat javítása a #25 alatt mainre került: `5300efdabb6ff019b842d13898d21aaae11e0d3c`. A Samsung pulzussorozat fordítási névütközését a `d220cfc37b12dce3c518f8aadd584e5a647ea268` javította. PR CI `41513584412` SUCCESS: letöltött nyers XML 296/296, nulla failure/error/skip.

Teljes main signed Actions [41514520016](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41514520016) SUCCESS, ugyanazon rögzített main-forrás. Letöltött nyers XML: **296 = 120 telefon + 176 Wear**, nulla failure/error/skip. Tényleges APK-pár: **2740 / 0.1.0-beta**, `com.TrainPilot.com`, változatlan állandó signer SHA256 `a72fd016d94e9d94e2b2282032f8c3820d4f09cba9ff1db05c3fd1de6ffa7dd6`. Maps konfiguráció/pair/capability ellenőrizve. Ez még nem fizikai Samsung/HC/Wear elfogadás.

- [Telefon APK](https://drive.google.com/file/d/1qm3emR0lSQjXjzizPuynEjf8A6X8qtd9/view)
- [Wear APK](https://drive.google.com/file/d/1avYIZfY4ClPb5JqfXR2EyUJQLS_YJNwg/view)
- Eredeti signed artifact: `13175931207`; teljes nyers tesztjelentés: `13175756491`.

- phone: 19481259 bájt; SHA256 `d2c7da1e350d6fe11c5f1933e552e8f2333c14b9c9df86077aba5d277bea6e66`.
- wear: 4636863 bájt; SHA256 `968733a2eca126ba28b2174a622b740d2aa483362f8a355fe51c30cc2b6f370c`.

A két külön APK a TrainPilot Native `1ywHgrManynjV9uNvZfVncOmCr3SbHIb9` mappába került; méret/szülő külön metadata-readback igazolva. Korábbi APK-k megmaradtak. Nincs adattörlés, kulcscsere vagy Play/GitHub release.

A #25 a dátum melletti frissítést, a kalóriarészletek lenyitását, a valódi időbélyeges pulzussorozatot és hiányjelzést, az ébredés napjára rendelt nappali/éjszakai alvásszakaszok átfedésmentes összegét, a kompakt heti sorokat és a szinkronállapot frissítését implementálja. Ne kezdd újra; készülékes visszajelzés alapján folytasd.

A #26 mainre került (`3a077f8`): hivatalos Spec Kit 1.2.6 Codex-integráció és csomagolt bug kiegészítő, TrainPilot constitution, magyar útmutató: [SPEC_KIT_CODEX.md](SPEC_KIT_CODEX.md). Csak fejlesztési skillek/dokumentumok, az APK alkalmazáskódja változatlan. Integrációs status OK, shell syntax és diff ellenőrizve. Új Codex-menetben a `$speckit-bug-assess`, `$speckit-bug-fix`, `$speckit-bug-test` használható. Nem futott automatikus bug-fix workflow és nem történt készülékes elfogadás. Alügynököt a felhasználó nem kért.


Ez az elsődleges checkpoint. A 2026-11-01-es átadó és a korábbi PR-leírások történeti háttérként használhatók; a kulcsok, Maps, tesztszám és APK-k tekintetében az alábbi állapot az irányadó.

## Legfrissebb igazolt checkpoint – #24 / 2739 (2026-11-02)

**Forrás és merge.** [PR #24](https://github.com/fuloplevente1998/Native-AI-Studio/pull/24) mainre merge-elve: `95ef1f896265da78698ca201efa9d6e001f6f0d9`. A végleges PR-head `727b47ffe5c5222b5dc3343cf75db7e50bbd9687`; a main merge fája és a végleges PR-head fája azonos: `d3a3add6b9267164ab17333ac06fff46bd1d2d1e`. A build elindításához kizárólag a signing workflow egy szűkített main `push` triggerét adtuk hozzá `50b4bc09491597cd4559351f0bdc4f2921716a97` commitban; az alkalmazásforrás a #24 merge óta nem változott. A régi `workflow_dispatch` továbbra is használható; az automatikus trigger csak a `.github/workflows/trainpilot-persistent-signing.yml` main ági változására él.

**#24 regressziók.** [Végleges PR Actions 41499001995](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41499001995) **SUCCESS**. A letöltött nyers JUnit XML **291/291 tesztet** igazol (**115 telefon + 176 Wear**, 0 failure/error/skip, 59 suite). A `WatchDiscardRecoveryRegressionTest`, `WearClosureRecoveryTest`, `WearSaveSyncTest`, `PhoneReturnInstallationRegressionTest`, `HealthSyncAdmissionRegressionTest` és `WorkoutOwnership193Test` is hibamentes. [Nyers PR XML-artifact](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41499001995/artifacts/13169075945). A PR eredetileg draft/futó állapotú említései történetiek, nem aktuálisak.

**Teljes állandó kulcsos main build.** [Signed Actions 41503008508](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41503008508) **SUCCESS**, tényleges checkout `50b4bc09491597cd4559351f0bdc4f2921716a97`, ami csak a CI-indító workflow fájlban tér el a #24 alkalmazásforrástól. A teljes signed run nyers XML-je **291/291 teszt** (**115 telefon + 176 Wear**, 0 failure/error/skip). [Nyers signed tesztjelentés](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41503008508/artifacts/13169179500); [APK-pár és valós metadata](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41503008508/artifacts/13171020761).

**Ellenőrzött APK-k:** mindkét alkalmazás azonosítója `com.TrainPilot.com`, verzióneve `0.1.0-beta`, belső Android buildszáma **2739**. Mindkettő tényleges állandó signer SHA256 ujjlenyomata: `a72fd016d94e9d94e2b2282032f8c3820d4f09cba9ff1db05c3fd1de6ffa7dd6`, egyezik a korábbi 2738-assal és a pinelt titkos környezeti beállítással. Nem készült új aláírókulcs. A CI ellenőrizte a Maps konfiguráció jelenlétét, a Data Layer csomag-/signer-azonosságot és a Wear capability/hardverkövetelményt.

| Csomag | Méret (bájt) | Valós APK SHA256 |
|---|---:|---|
| Telefon release 2739 | 19464875 | `fc8a5c79c8f3d7d29f95e18afed1d2fc03781ce5f377a29b2f159a9cc8b23c95` |
| Natív Wear release 2739 | 4636863 | `9a1940129d8a538580e4a69cde83ea5144ff63fe12b74fe23980a3a652e864fd` |

- [Telefon release APK](https://drive.google.com/file/d/1uE5ssymKDuRVW0HCULmhonKKSj-qozQk/view)
- [Natív Wear release APK](https://drive.google.com/file/d/1gzyJK8fetWHou12nalrMhg2i24-lawLv/view)

A ZIP-ből külön kinyert APK-k SHA256-ja megegyezik a signed artifact metaadataival. Mindkét külön Drive-fájl mérete és `1ywHgrManynjV9uNvZfVncOmCr3SbHIb9` szülőmappája külön Google Drive metadata-olvasással igazolt; a 2738 és régebbi APK-k megmaradtak. **Nem történt Play/GitHub release-közzététel, adattörlés vagy kulcscsere.**

**#24 működés és továbblépés.** A hiteles terminális Wear receipt, elveszett/elutasított válasz utáni látható újrapróba és a már tartósan törölt edzés óvatos visszaigazolása implementált, JVM-regresszióval igazolt; **fizikai telefon–Galaxy Watch készülékes elfogadás még szükséges**. Ne implementáld újra. A natív motor, Room/DataStore, ownership/ACK, finalization, tombstone, replacement és backup/restore védelmek kötelezőek. A #19-ben felvett új Mai állapot, délutáni alvás, pulzuselőzmények, Aktív kalória lenyitás, dátum melletti szinkrongomb, kompakt teljes szélességű heti listák külön nyitott feladatok: a mostani APK-ellenőrzés **nem** tekinti őket javítottnak.

## Korábbi igazolt checkpoint – #22, béta 2738

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

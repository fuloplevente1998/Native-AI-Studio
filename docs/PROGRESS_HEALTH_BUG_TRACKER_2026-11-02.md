# Natív készülékes hibák és javítások, 2026-11-02

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

## Aktuális 1–6 integráció – #28 / 2741

A 2d7171b mainforrás tartalmazza a közös keretek/forrásharmonika, volumenábra,
szüneteltetett edzés elvetése, térképmód/végpontok, Home duplikált blokk eltávolítása
és HC frissolvasás-diagnózis javításait. PR CI 41529082323 SUCCESS; nyers XML
315 teszt (139 telefon +176 Wear), nulla hiba/kihagyás. Main signed 41530039869 SUCCESS, letöltött XML szintén 315/315; állandó kulcsos 2741 APK-pár Drive-ra feltöltve és méret/szülő readbackkel ellenőrizve. Kiadási bizonyíték és APK-linkek
az aktuális átadó legfelső blokkjában. A HC készülékes üresség okát ez nem bizonyítja.
#19 nyitva a fizikai elfogadáshoz; a 8. naplózottedzés-törlés külön feladat.

## Történeti ellenőrzőlista

A 2026-11-02-i készülékes visszajelzések közös követője. Kiindulás: a mainre került AI Studio-frissítés és #18 után kiadott, állandó kulcsos `35d9d64` APK (255 sikeres JVM-teszt). A lenti problémákat a felhasználó készüléken jelezte; a korábbi teszteredmény nem jelenti ezek elfogadását.

## Elfogadási lista

- [ ] **Egy naplózott edzés izomterhelése:** az ábra és százalékok az adott edzés tényleges, befejezett sorozataiból készüljenek; másik edzés megnyitásakor ne maradjon korábbi vagy üres alapábra. Ne legyen 3 havi kapcsoló egyetlen edzés statisztikáján.
- [ ] **Összes volumen és grafikon:** az 1 nap / 7 nap / 30 nap / 3 hó valódi helyi dátumszűrést és eltérő oszlopokat alkalmazzon, a fejléc összege és az összesítő kártyák ugyanabból az intervallumból számoljanak. Dinamikus tengely; nincs rögzített heti adatsor vagy kitalált minimum oszlopmagasság.
- [ ] **Fejlődés izomterhelés időszakváltása:** ugyanaz a kiválasztott időszak vezérelje a tényleges Room-lekérdezést, százalékokat, testrajzot és periódusgombot. Gyors váltáskor a késő régi válasz nem írhatja vissza a régi időszakot.
- [ ] **Óranév audit:** rögzített mintanevek és mintabattery eltávolítása minden érintett UI-ból. A képen szereplő `Galaxy Watch7 (ZW4J)` a Wear Data Layer csatlakoztatott node-neve lehet: az igazolt tényleges nevet meg kell tartani, ismeretlen eszközt nem szabad modellel helyettesíteni.
- [ ] **PR #16 integráció:** a naplóban egymás mellett TrainPilot WearOS / Samsung Health / Health Connect forrásválasztók, alapból csukva, saját adatot nyitnak. Forrásadatok/kalóriák nem keverhetők, nem adhatók össze. Meglévő #16 ág újrafelhasználása és friss mainnel együttes ellenőrzése szükséges.
- [ ] **Engedély helyett szinkron:** minden érintett Health Connect / Samsung Health forrásgomb tényleges olvasási grantot ellenőrizzen. Meglévő engedélynél szinkron; hiánynál engedélykérés, majd valódi grant-visszaellenőrzés és szinkron. Részleges/visszavont jog, elutasítás és SDK-hiba nem mutathat sikert. Külön, szándékos engedélykezelés az Engedélyek oldalon maradjon.
- [ ] **Heti célok UI:** a külön „Heti összesítő és adatforrások” panel megszűnik. A Heti lépésszám kártya saját lenyitásában csak a napi lépések és forrásuk jelenjen meg; az Aktív edzésidő saját lenyitásában naponta a Room-edzések és tényleges aktív idejük. A két nyitás független.
- [ ] **Teljes napi lépések és deduplikáció:** teljes helyi nap (ma: éjféltől mostanáig), nem edzésidőablak. A kiválasztott Samsung Health vagy Health Connect napi összesítése legyen az egyetlen hozzájárulás; HC esetén az eredeti deduplikált napi forrásválasztás maradjon. Órás workout-lépések nem teljes napi lépések. Az órából Samsung/HC útján érkező ugyanazon napi adatot nem szabad még egyszer hozzáadni.
- [ ] **Mai állapot az Egészség és Kezdőlapon:** közös, hiteles mai provider-adat; napi lépés, aktív és teljes energia külön értelmezve. A felhasználó Samsung Healthben kb. 6–8 ezer lépést és 1000 kcal-t lát, miközben a natív UI üres. Ezeket a számokat tilos beégetni vagy mérést helyettesítő becslésként használni. Látszódjon forrás, tényleges frissítési idő, szinkronállapot és hiány/hiba; ismeretlen adat ne állítsa azt, hogy nulla.
- [ ] **Edzés közbeni felugró súgók:** az alsó, vezérlőgombot takaró rutin snackbar/útmutató üzenetek (pl. gyakorlat befejezése, következő gyakorlat) megszűnnek. A felső következőgyakorlat-jelzés megmarad; érdemi hiba nem veszhet el.
- [ ] **Edzés közbeni rögzített vezérlés:** az aktív edzésben görgetéstől független helyen legyen elérhető az Átadás órára / Visszavétel telefonra. A tényleges meglévő átadási parancsokat és ACK/ownership/paused/install védelmeket használja, ne pusztán kapcsolót váltson vagy másik képernyőre vigyen.
- [x] **Első #20 béta verzió:** látható verzió `0.1.0-beta`, mindkét APK belső Android buildszáma `2737`. A belső kód nem indulhat 1-ről, mert a telepített 2736-ra adattörlés nélkül frissíthetőnek kell maradnia. Appazonosító és állandó signer változatlan.
- [x] **Kapu és átadás:** célzott regressziók + teljes telefon/Wear Actions; nyers XML ellenőrzés. Mainbe merge után állandó saját kulcsos APK-pár, kért új bétaverzió és változatlan appId/signer, hash/metadata-ellenőrzés és feltöltés a TrainPilot Native Drive-mappába. Friss AI Studio-átadó és folytatóprompt.

## Megőrzendő szerződések

`com.TrainPilot.com`, az aktuális folytatás szerint 2739 / 0.1.0-beta, az állandó saját kulcs, natív Room/DataStore/motor, ownership/finalization/ACK/replacement/tombstone/backup-adatvédelem. Nincs adattörlés, másik szolgáltatóra csendes fallback, fake/demo mérés vagy engedély nélküli sikerjelzés. Aktív edzés/adatcsere alatti tiltásnál a valós ok jelenjen meg; védelmet nem szabad kikapcsolni a szinkron kedvéért.

A kód a #20 és #22 alatt mainre került. A nyers teszt-/APK-bizonyíték és a valódi Samsung/HC készülékes elfogadás külön kezelendő; egy nyitott checkbox önmagában nem jelent implementálatlan kódot.


GitHub-követő: #19; javító PR: #20.

## Kód- és célzott ellenőrzési állapot

A #20 implementálja a fenti pontokat. A `5df8374a65f81dc759ecfb6e8e96a936698344ff` alkalmazásforráson az Actions `41450724814` SUCCESS: letöltött nyers XML-ben 43 teszt, nulla hiba/kihagyás. Ez célzott ellenőrzés, nem teljes suite. A checkboxok a teljes/készülékes elfogadáshoz maradnak; a kódot ne valósítsd újra pusztán az üres checkbox miatt. A felhasználó kérése szerint innen mainre merge és teljes main-Actions következik.

## Main és teljes ellenőrzés

A #20 mainre merge-elve: `3572e2086cc741cc42bc2c6929c786b0772bf91c`. Teljes signed Actions `41451874030` SUCCESS, letöltött XML: **270 teszt (101 telefon + 169 Wear), 0 failure/error/skip**. Telefon és óra tényleges release APK-ja `0.1.0-beta / 2737`, változatlan állandó signerrel; APK-hash, méret, source checkout és a TrainPilot Native mappa ellenőrizve. Letöltések az aktuális AI Studio-átadóban. A többi checkbox készülékes elfogadáshoz nyitott; nem azt jelenti, hogy a mainbe került kódot újra kell írni.

## Következő készülékes javítás – 2026-11-02

- Mentett edzés után a lezárt execution/ownership pillanatképben megmaradó pause/finalization jelző ne blokkolja örökre az egészségszinkront. Csak kifejezetten inaktív, hiteles lezárt pillanatkép hagyható figyelmen kívül; aktív edzés, sikertelen mentés, átadás, restore és adatcsere védelme megmarad. Magyar, egyértelmű inline diagnosztika, ismételt sikerjelzés nélkül.
- Heti célok: a két önálló lenyitás a két fejléc alatt teljes szélességet kap.
- A Fejlődés harmadik füle Statisztikák: Személyes rekordok és Kardióstatisztika, a meglévő natív PR-/kardiómotorral, tényleges mentett sorozatokból és napi provider-cache-ből.
- Üres naptári nap: önálló tájékoztató sor, két egyenlő szélességű hozzáadásgomb, egyetlen visszavonás.
- Következő belső build: 2738, látható verzió továbbra is 0.1.0-beta. A #22 mainre merge-elve: `b96982dd62212d53f80ee1af366d77bfaacd439d`. Célzott Actions `41491162595` SUCCESS: nyers XML-ben 50/50, nulla hiba/kihagyás. Teljes main signed Actions `41492179428` SUCCESS: letöltött XML **277/277 = 108 telefon + 169 Wear, nulla failure/error/skip**. A 2738-as állandó kulcsos APK-pár hash/package/verzió/CI-forrás ellenőrizve; feltöltve a TrainPilot Native mappába, méret/szülő readback igazolva. Aktuális linkek és hash-ek az AI Studio-átadó legfelső #22 blokkjában. Ez nem fizikai Samsung/HC vagy Wear készülékes elfogadás.

## Órás elvetés visszaigazolása – következő 2739-es javítás

Készülékes jelzés: a telefonon nincs látható edzés/törlési lehetőség, az órán választott elvetés után „A telefonos törlés még várakozik”; az átadás működik. A Wear closure eddig a superseded/processed/applied válaszokat nem képezte hibára, így a szinkronjelzés hibája mellett az összegző végtelenül várakozónak látszott, újrapróbálás nélkül.

Javítás: hiteles, teljes azonossággal ellenőrzött terminális receipt, időtúllépés/elutasítás esetén látható újrapróba. Telefonon a már tartósan törölt azonosító külön visszaigazolható. Aktív vagy mentett edzés nélküli, megerősített, az eredeti DataItemhez és órás snapshothoz kötött árva elvetés csak a saját törlési jelzőjét rögzítheti. Másik aktív edzés/motor nem törölhető; mentett history, pending ownership/restore/finalization védelem marad. Az aktív órás elvetés natív lezárásának hibáját nem szabad lenyelni vagy sikerrel nyugtázni. A #24 mainre merge-elve: `95ef1f896265da78698ca201efa9d6e001f6f0d9`. A végleges PR [Actions 41499001995](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41499001995) SUCCESS, letöltött nyers XML **291/291 = 115 telefon + 176 Wear, 0 failure/error/skip**. A teljes állandó kulcsos main [Actions 41503008508](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41503008508) SUCCESS, nyers XML **291/291**, valódi két release APK `com.TrainPilot.com` / `2739` / `0.1.0-beta`, változatlan signer. Külön telefon- és Wear-APK a TrainPilot Native Drive-mappában, méret/szülő readback igazolt; linkek, APK SHA256 és CI checkout az elsődleges [átadóban](AI_STUDIO_HANDOFF_2026-11-02.md). A hiteles Wear closure/ACK/újrapróba JVM szinten igazolt, valódi Galaxy Watch készüléken még elfogadandó.

## Új, rögzítésre kért egészségképernyős hibák – kb. 19:03

Új készülékes hibák és felületi kérések – 2026-11-02, kb. 19:03, a felhasználó kifejezetten rögzítést kért. Nyitott pontok, még nem javított/elfogadott működés.

- [ ] **Hiányos Mai állapot:** a mai nap teljes, kiválasztott Samsung Health provider-adatainak ellenőrzése. A képen napi lépés 5360, aktív energia 228 kcal, teljes energia (nyugalmival) 1462 kcal, utolsó sikeres szinkron 18:56, felül „Szinkron…”/„hozzáférés ellenőrzése”. Vizsgálandó a hiányzó mezők és az elhúzódó szinkronállapot oka. Ezek megfigyelt példák, nem beégetendő értékek.
- [ ] **Kalóriarészletek helye:** az Aktív kalória kártya lenyitásába kerüljön a jelenleg fölötte külön megjelenő napi összes energia, aktív energia és kapcsolódó napi részletek. A mért aktív és nyugalmi/teljes energia értelmezése, forrása maradjon külön; nincs duplikált összegzés vagy hiányból számolt kitalált érték.
- [ ] **Pulzuselőzmények:** a felhasználó szerint csak a pillanatnyi pulzus látszik, a korábbi mai mérések nem. A képen aktuális 61 bpm, nyugalmi 0 bpm és „Napi tartomány: nincs mért adat”. Ellenőrizni kell a tényleges napi mérési sorozatot/tartományt és a megjelenítést; hiányzó nyugalmi pulzus nem jelenhet meg mért 0 bpm-ként. Aktuális érték és időbeli előzmény nem helyettesítheti egymást.
- [ ] **Mai alvás / délutáni alvás:** a felhasználó ma délután kb. 3 órát aludt, de a Mai állapot alvásmezője „—”. Ellenőrizni kell a szolgáltatói alvásszakaszok lekérését, helyi dátumhoz rendelését és a nappali alvás megjelenítését. A kb. 3 óra diagnosztikai viszonyítás, nem beégetendő érték; átfedő alvásszakaszok nem duplázhatók.
- [ ] **Szinkrongomb a fejlécben:** a Mai állapot mai dátuma mellé kerüljön a meglévő szinkron/frissítés művelet és kompakt állapotjelzése; a jelenlegi lejjebb elhelyezett gomb helyett. Keskeny képernyőn is olvasható és elérhető maradjon.
- [ ] **Kompakt heti bontás:** a Heti lépésszám / Aktív edzésidő lenyitott 7 napi listája legyen sűrűbb és áttekinthetőbb, kevesebb görgetéssel. A már javított teljes szélesség, független nyitás, tényleges napi adatok/forrás és hiányjelzés maradjon meg.

Kapcsolódó aktuális órás hiba: telefonon nincs látható edzés/törlési lehetőség, ezért a felhasználó az órán választotta az elvetést; az óra telefonos törlési visszaigazolásra vár. Ez külön javítás, a fenti egészségképernyős pontok most rögzített további teendők.

## #25 kód és APK igazolás – 2740

A hat egészségképernyős pont implementálva, main `5300efd`, PR CI `41513584412` és full signed `41514520016` SUCCESS. Mindkét letöltött XML 296/296, nulla hiba/kihagyás. Állandó kulcsos 2740-es telefon/Wear pár ellenőrizve és a TrainPilot Native mappába feltöltve, méret/szülő readback rendben. Aktuális linkek/hashes az átadó legfelső blokkjában. A készülékes elfogadáshoz a checkboxok nyitva maradnak; ez nem implementálatlan kódot jelent.

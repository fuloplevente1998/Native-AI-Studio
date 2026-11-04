# TrainPilot Native – aktuális állapot, 2026-11-04

## AI Studio integrált csomag – versionCode 2750 (PR104 `0ec70650` bázison)

A legfrissebb main (`0ec706509a267119553ef7b9c8c78d1c881bf563`, PR104) dizájn-, profil- és statisztikajavításainak maradéktalan megőrzésével elkészült az AI Studio csomag (`versionCode = 2750` az `:app`, `:wear` és `scripts/verify_android_apk_metadata.py` fájlokban):

- **#59 – Appon belüli gyakorlatvideó-lejátszás javítása:** A beágyazott HTML valós HTTPS app-originről (`https://com.trainpilot.com/`) töltődik be; a `frame-ready` (`iframe.onload`) és a valódi `ready` (`YT.Player` `onReady`) állapot különválik, így elmarad a korai fekete képernyő. Beágyazási hiba (`100`/`101`/`150`/`153`), timeout vagy YouTube-navigáció esetén a modal automatikusan átvált a készüléken bizonyítottan működő appon belüli böngészős (`BROWSER`) módra külső app nyitása nélkül, megtartva a pontos videó-ID-t és minden SSL/bridge biztonsági korlátot.
- **#69 – Régi visszajelzés-formátumok teljes auditja:** A bizonyított régi WebView edzés- és gyakorlat-visszajelzések (`good` → `right`, HU/EN/DE/RO fordított címkék, `effortScore` `1..5`, `rating`, `note`/`userNote`, `actualRir`/`feedback.rir`) normalizálva a kanonikus (`easy`/`light`/`right`/`challenging`/`hard`/`pain`) csatornára az importban, a naplószerkesztőben és a Coach-olvasókban. A hiányzó `actualRir` külön marad a `0`-tól, a `feedback.applied` zár védi a duplikált progressziót, az ismeretlen értékek sértetlenül megmaradnak.
- **#75 – Teljes telefonos HU/EN/DE/RO lokalizáció:** Minden Studio képernyő, natív popup/szerkesztő, üres/hiba/engedély/mentési állapot, `contentDescription` és dinamikus felirat négy nyelven (`HU`, `EN`, `DE`, `RO`) követi a választott alkalmazásnyelvet, miközben a felhasználói edzés-/program-/gyakorlatnevek, jegyzetek és kanonikus JSON gépi kulcsok változatlanok maradnak.
- **#82 – Adatvédelmi tájékoztató frissítése és bekötése:** A `PrivacyPolicy.text(lang)` négy nyelven pontosítja a közvetlen Samsung Health SDK-olvasást, a Health Connectet, a Wear OS kísérőméréseket és az alapból kikapcsolt `healthBackupConsent` működését; a Studio Beállítások képernyőn (`settings_open_privacy_row` → `settings_privacy_dialog`) közvetlenül megnyitható.
- **PR101 (`4737d72c`) és PR104 (`0ec70650`) megőrzése:** Megmaradt az 1 cm / 1 kg profil-lépésköz, az egyetlen többválasztós felszerelés-dropdown, a 160 dp rekordgrafikon-rajzterület, az aktivitásszűrő (konkrét kardiónál rejtett napi lépésösszesítő), a `StatisticsMetricTile` / `NativeCardioProgressPanel` és a kompakt gyakorlatválasztó keresővel + kedvenc-csillaggal.
- **Tényleges helyi ellenőrzések:**
  - `compile_applet`: **SUCCESS**
  - Nyers JUnit XML (`python3 scripts/verify_junit_xml.py`): **443 teszt = 267 telefon + 176 Wear** (`0 failure`, `0 error`, `0 skipped`)
  - `node --test scripts/test_exercise_video_player.mjs`: **7/7 PASS**
  - `python3 scripts/verify_font_glyphs.py`: **PASS** (12 statikus fontarc, valódi súlyok, hash-ek, licencek és HU/DE/RO/ASCII glyph-készlet)

## Friss main dizájn – PR101 és PR104, 2026-11-04

A dizájncsomag mainen: **4737d72c75f191b18980bc40885633c1c43b5c99**, majd a PR104 profil/statisztika/kedvenc javítások: **0ec706509a267119553ef7b9c8c78d1c881bf563**. Teljes CI434 =258 telefon+176Wear (PR101), majd a 2750-es AI Studio csomaggal **443 = 267 telefon + 176 Wear**, 0 failure/error/skipped, 12 valódi fontarc glyph/súly/hash/licenc ellenőrzés és JS7 PASS. [Bizonyíték](DESIGN_CI_2026-11-04.json), [változások](DESIGN_FONT_THEME_IMPLEMENTATION.md).

Elkészült forrásban: #95 egységes sorozatvezérlők/egységfeliratok, #97 ismételt napcímek, #98 teljes ékezetes fontok és valódi súlyok, #99 glow/világos felületek és natív kijelölések, a telefon/óra átadási sáv kompakt dizájnja, valamint #59, #69, #75 és #82 teljes AI Studio implementációja. Motor és parancsok változatlanok; az aktív egységek csak UI-projekcióban maradnak meg. A készülékes vizuális és videós elfogadás a GitHub Actions által aláírt `2750`-es APK-val történik.

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
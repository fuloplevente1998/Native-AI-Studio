# TrainPilot Native – aktuális állapot, 2026-11-03

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

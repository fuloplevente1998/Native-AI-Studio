# TrainPilot Native – aktuális állapot, 2026-11-03

Ez az elsődleges belépési pont. A régi dátumozott átadók történeti megfigyelések; a lentiek felülírják a bennük szereplő „aktuális”, „még nincs implementálva” és régi APK kijelentéseket.

## Új közös javítócsomag – #79

A felhasználó a fekete YouTube-videó jelzése után a beleférő javításokat egy közös APK-ban kéri. Az új implementáció és egyben végzendő tesztlista: [NATIVE_TEST_BATCH_2026-11-03.md](NATIVE_TEST_BATCH_2026-11-03.md). A videó/kompakt óraadat/Samsung permission és program-/sajátgyakorlat-/naplószerkesztés most implementálva, a teljes végleges CI és új aláírt APK még ellenőrzés alatt. A részletes egészségböngésző bekötése részleges #71. Ez felülírja az alábbi korábbi backlog-jelzéseket ezeknél a pontoknál. **A korábban átadott81da2f7 APK ezt a csomagot még nem tartalmazza.** BLE sztornó, #69/#71 teljes maradéka, #74/#75 nem kész. Fizikai elfogadás nincs állítva.

## Kód és ellenőrzés

- Main alkalmazásforrás: **`81da2f7d3190ff5a558e2a07ccf1fe54104c90e8`** (#77). Tesztelt PR-head: `eae9d6042d48998d9e9e21681443257aa8427897`; app/Wear/shared/scripts/workflow fák egyeznek.
- [Actions 41598444479](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41598444479) SUCCESS; letöltött nyers XML: **402 =226 telefon +176 Wear**, 0 failure/error/skipped, artifact `13213141866` és tényleges source-SHA visszaellenőrizve.
- #77: a kanonikus Coach/target/advice/readiness és előző megfelelő alkalom súly-/ismétlés-előtöltése a Studio UI-ba kötve. Valódi program/nap indul, jövőbeli cél a naptárba visz. A #228 előtöltő programtól/naptól függetlenül azonos ID/loadType/repUnit alapján működik; nincs örökölt actualRir/done/idő/GPS. Stable ID, kézi érték és eredeti napló megmarad. A hiányzó RIR és tényleges 0 külön, a hat visszajelzés kanonikus machine érték. Fájdalom/alacsony készenlét a szövegben is korlátoz; nincs helyi fake súly/RIR számítás. [Részletek](COACH_PREFILL_IMPLEMENTATION_2026-11-03.md).
- Korábbi #67 appforrás: `deebfddd7f0259e589e9791582ad6d2d38fcf4e2`, tesztelt PR-head `c0a9862aaadf1e3d4ad20bc12420f8d828114c2d`; Actions 41589518841, nyers395 =219 telefon+176Wear, 0 failure/error/skipped. A mini/fullscreen térkép, Mai állapot pulzustrend/szöveg és Settings fejléc/csoport javítása innen származik. Ezeket ne kezdd újra.
- #58/#62–#66: Coach rövid kivonat, piros részlet-/listatörlés, belső videó, valós órainfó, pulzus/recovery projekció, témakövetés, naptár, valós naplóadat és volumen-százalék már létezik.

## APK – külön állapot a main kódjától

**Legfrissebb telefonos APK, Coach + előtöltés:** [TrainPilot-Native-2744-81da2f7-telefon-Coach-elotoltes.apk](https://drive.google.com/file/d/1Vy6uZlUSbY-6uxxkUhEo9Cu1bkc2wcsL/view?usp=drivesdk). A [signed main build 41599072569](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41599072569) SUCCESS, pontos forrás `81da2f7d3190ff5a558e2a07ccf1fe54104c90e8`. Letöltött nyers XML: **402 =226 telefon +176 Wear**, 0 failure/error/skipped. Signed artifact `13213214357`, raw XML `13213399004`; package/verzió, állandó signer, Maps/pair és mindkét APK-hash ellenőrizve. Telefon: **19 580 104 bájt**, SHA256 `4552dc295b2d7a6e2b3f2c1392e22bfe38d958c2d8607b18fc4c1525cc29ae49`. Drive-méret és szülőmappa visszaolvasva. [Gépi bizonyíték](COACH_PREFILL_SIGNED_BUILD_2026-11-03.json). **Elég a telefonos app frissítése**; #67/#77 csak telefonos alkalmazáskódot változtatott, órás telepítés nem szükséges. Fizikai elfogadás még nincs igazolva.

### Korábban, a Coach-fejlesztés előtt átadott APK

A #67-et tartalmazó aláírt main build [41591380374](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41591380374) SUCCESS, pontos forrás `deebfddd7f0259e589e9791582ad6d2d38fcf4e2`. Letöltött nyers XML: **395 =219 telefon +176 Wear**, 0 failure/error/skipped. Signed artifact `13210208864`, raw XML `13210463700`. Package/verzió, állandó signer, Maps/pair és APK-hash ellenőrizve. A #76 dokumentációs main-merge után az alkalmazásfa változatlan.

**Telefonos APK átadva:** [TrainPilot-Native-2744-deebfdd-telefon.apk](https://drive.google.com/file/d/1Ap5ysNwA8ZA35xV-Zidx_JeKqxlwHLP-/view?usp=drivesdk), 19 563 720 bájt, SHA256 `f82cd35ccd057e53ebc2e92e325dd8f2e48cfe291158e8674d2e5f8cc889ed33`. Drive-méret/szülőmappa külön visszaolvasva: `1ywHgrManynjV9uNvZfVncOmCr3SbHIb9`. Korábbi fájlok megmaradtak. Az órás APK is ugyanazon buildben ellenőrzött (4 636 863 bájt, SHA256 `f6b73f685ce444e1886a8660dfeb107839ac9b1b0546b2522fde53a5573dd753`), de a felhasználó most csak a telefon frissítését kérte, új órás telepítés nem szükséges. A Coach/előző alkalom bekötés még nem része ennek az átadott APK-nak.

A #67 csak telefonos fájlokat változtatott. Ehhez önmagában az órás appot nem kell frissíteni. A workflow technikailag párt épít és ellenőrzi a package/signer/pair szerződést; ez nem jelent kötelező órás újratelepítést. Verzió most 2744 /0.1.0-beta, package mindkettőn `com.TrainPilot.com`, állandó signer változatlan. Nincs automatikus verzióemelés.

Korábbi igazolt aláírt build: 41580088708, forrás `56301f7d68016f3074c366ff4162617adf75e8cd`, 393 teszt; EZ MÉG NEM TARTALMAZZA #67-et. A régi 2743-as Drive-linkek történeti kiadások, nem a legfrissebb javítások.

## Jóváhagyott munka és megmaradó backlog

1. A #67 aláírt telefonos APK átadása kész. A felhasználó ezt megkapta a Coach/előző alkalom fejlesztése ELŐTT.
2. #70 Coach és #68 előtöltés **kódban/CI-ben kész, mainen**, implementációként lezárva. A legújabb, ezeket tartalmazó aláírt main build **41599072569 SUCCESS**, nyers402; az ellenőrzött telefonos APK átadva, lásd az APK-szakaszt. Fizikai elfogadás nem állított.
3. #69 alap RIR/root-feedback szerződése javítva; a teljes gyakorlatonkénti/utólagos feedback és régi formátumok áttekintése még nyitott, adatot tömegesen nem írunk át. #61 Energy 0 része javítva, Samsung permission-üzenet része még nyitott.
4. #71–#75 és a #60 kompakt órakártya backlog. A program-/gyakorlatműveletek a meglévő natív átrendezés és helyi súly/ismétlés/pihenő szerkesztés mintáját kövessék. #35/#48 készülékes visszaigazolásra vár. **#59 új készülékes hibajelzés: fekete YouTube-kép „készen áll” üzenettel; [javítás és ellenőrzési állapot](YOUTUBE_BLACK_PLAYER_2026-11-03.md).** A korábban átadott81da2f7 APK ezt a javítást még nem tartalmazza.
5. **BLE-kezelő sztornó**, a felhasználó kifejezett kérése. Nem fejlesztendő, meglévő adat/backend törlése sem feladat.

A funkcióhiányok forráshoz kötött listája: [WebView–natív paritásaudit](NATIVE_WEBVIEW_PARITY_2026-11-03.md).
Aktuális készülékes nyitott pontok: [hibakövető](PROGRESS_HEALTH_BUG_TRACKER_2026-11-02.md).

## Megőrzendő szerződések

Studio Compose arculat, egyetlen natív motor és Room/DataStore. Nincs második DB, háttérben néma ownership-váltás, hamis siker vagy kitalált mérés. ACK/epoch, explicit Folytatás, finalization, restore/replacement, tombstone, default-off egészségmentési consent és privát GPS/fotó védelem megmarad. [Constitution](../.specify/memory/constitution.md), [keretek](FRAME_DESIGN_SYSTEM.md).

A CI kódot/fixture-t igazol. A Maps valódi ablak, YouTube-lejátszás, Samsung/HC-hozzáférés, Wear átadás és a saját mentés készülékes elfogadása külön bizonyíték; hiányuk nem jelenti automatikusan a már kész backend hiányát. Alügynök nincs kérve. GitHub Actions a teljes build/regresszió helye; kulcsot, jelszót ne kérj és ne naplózz.

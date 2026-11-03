# TrainPilot Native – aktuális állapot, 2026-11-03

Ez az elsődleges belépési pont. A régi dátumozott átadók történeti megfigyelések; a lentiek felülírják a bennük szereplő „aktuális”, „még nincs implementálva” és régi APK kijelentéseket.

## Kód és ellenőrzés

- Native main alkalmazásforrás: `deebfddd7f0259e589e9791582ad6d2d38fcf4e2` (#67 merge). PR-forrás: `c0a9862aaadf1e3d4ad20bc12420f8d828114c2d`; app/Wear/shared/scripts/workflow fáik egyeznek.
- [Actions 41589518841](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41589518841) SUCCESS. Letöltött nyers JUnit XML: **395 = 219 telefon + 176 Wear**, 0 failure/error/skipped; artifact `13208903929`. A jelentés commitja egyezik a PR-head-del.
- #58, #62–#66 és #67 mainen. A Coach rövid kivonata, piros részlet-/listatörlés, belső videó, valós órainfó, pulzus/recovery projekció, témakövetés, naptár, valódi naplóadat és százalékos volumen-grafikon már létezik; ezeket ne kezdd újra.
- #67: nagyobb hasznos mini térkép, rendszerhez illesztett Dialog, Android látható ablakhoz mért Indítás-gomb; napi Pulzus kártyába költözött trend és az ismételt alapnézeti szövegek eltávolítása; kompakt Settings fejléc, egyetlen Témák cím és közös edzésbeállítási keret.

## APK – külön állapot a main kódjától

A #67-et tartalmazó aláírt main build [41591380374](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41591380374) SUCCESS, pontos forrás `deebfddd7f0259e589e9791582ad6d2d38fcf4e2`. Letöltött nyers XML: **395 =219 telefon +176 Wear**, 0 failure/error/skipped. Signed artifact `13210208864`, raw XML `13210463700`. Package/verzió, állandó signer, Maps/pair és APK-hash ellenőrizve. A #76 dokumentációs main-merge után az alkalmazásfa változatlan.

**Telefonos APK átadva:** [TrainPilot-Native-2744-deebfdd-telefon.apk](https://drive.google.com/file/d/1Ap5ysNwA8ZA35xV-Zidx_JeKqxlwHLP-/view?usp=drivesdk), 19 563 720 bájt, SHA256 `f82cd35ccd057e53ebc2e92e325dd8f2e48cfe291158e8674d2e5f8cc889ed33`. Drive-méret/szülőmappa külön visszaolvasva: `1ywHgrManynjV9uNvZfVncOmCr3SbHIb9`. Korábbi fájlok megmaradtak. Az órás APK is ugyanazon buildben ellenőrzött (4 636 863 bájt, SHA256 `f6b73f685ce444e1886a8660dfeb107839ac9b1b0546b2522fde53a5573dd753`), de a felhasználó most csak a telefon frissítését kérte, új órás telepítés nem szükséges. A Coach/előző alkalom bekötés még nem része ennek az átadott APK-nak.

A #67 csak telefonos fájlokat változtatott. Ehhez önmagában az órás appot nem kell frissíteni. A workflow technikailag párt épít és ellenőrzi a package/signer/pair szerződést; ez nem jelent kötelező órás újratelepítést. Verzió most 2744 /0.1.0-beta, package mindkettőn `com.TrainPilot.com`, állandó signer változatlan. Nincs automatikus verzióemelés.

Korábbi igazolt aláírt build: 41580088708, forrás `56301f7d68016f3074c366ff4162617adf75e8cd`, 393 teszt; EZ MÉG NEM TARTALMAZZA #67-et. A régi 2743-as Drive-linkek történeti kiadások, nem a legfrissebb javítások.

## Következő jóváhagyott munka

1. A fenti aláírt telefonos APK átadása kész; a következő két funkció fejlesztése megkezdhető.
2. [#70 – Coach valódi bekötése](https://github.com/fuloplevente1998/Native-AI-Studio/issues/70), és [#68 – előző megfelelő edzésből súly/ismétlés előtöltése](https://github.com/fuloplevente1998/Native-AI-Studio/issues/68). A szükséges RIR/effort szerződést #69 alapján megőrizni/javítani; nem lehet kitalált RIR-ből vagy elvesző fájdalomjelzésből progressziót számolni.
3. A többi paritáshiány csak rögzített backlog. A program-/gyakorlatműveletek, teljes egészségböngésző, intervallum- és nyelvi teljesség nem kapott most implementációs felhatalmazást.
4. **BLE-kezelő sztornó**, a felhasználó kifejezett kérése. Nem fejlesztendő, nem kell új hibajegy; meglévő backend/adat törlése sem feladat.

A funkcióhiányok forráshoz kötött listája: [WebView–natív paritásaudit](NATIVE_WEBVIEW_PARITY_2026-11-03.md).
Aktuális készülékes nyitott pontok: [hibakövető](PROGRESS_HEALTH_BUG_TRACKER_2026-11-02.md).

## Megőrzendő szerződések

Studio Compose arculat, egyetlen natív motor és Room/DataStore. Nincs második DB, háttérben néma ownership-váltás, hamis siker vagy kitalált mérés. ACK/epoch, explicit Folytatás, finalization, restore/replacement, tombstone, default-off egészségmentési consent és privát GPS/fotó védelem megmarad. [Constitution](../.specify/memory/constitution.md), [keretek](FRAME_DESIGN_SYSTEM.md).

A CI kódot/fixture-t igazol. A Maps valódi ablak, YouTube-lejátszás, Samsung/HC-hozzáférés, Wear átadás és a saját mentés készülékes elfogadása külön bizonyíték; hiányuk nem jelenti automatikusan a már kész backend hiányát. Alügynök nincs kérve. GitHub Actions a teljes build/regresszió helye; kulcsot, jelszót ne kérj és ne naplózz.

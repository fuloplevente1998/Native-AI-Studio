# TrainPilot 2763 · 1.1-beta — ellenőrzött APK-páros

Telefon276300, Wear276302. PR242, signed forrás8367634a78824d1955956ce90ec424ee92e891a3.
Actions43675171894 SUCCESS, build_aab=false.

A felhasználó később a 2763-as AAB-párt is kérte; az elkészült és Drive-ra
került. Részletek: [2763-aab-changes.md](2763-aab-changes.md). Az alábbi
APK-only futás és eredeti átadási adatok történeti bizonyítékként megmaradnak.

A két külön Drive-változáslista 2026-11-15-én frissült: a felhasználó által
Play Console-alapnak megadott 2756-os kiadás (telefon275600, Wear275602) óta
összegyűlt összes változást tartalmazza, a 2757–2763 kiadási leírások alapján.
A fájlok végén külön, 500 karakter alatti Play Console-szöveg is található.
Az alábbi szakaszok továbbra is a 2762→2763 különbséget rögzítik.

## Telefon — a 2762-höz képest

- A gyakorlatválasztó pipája kiveszi az adott gyakorlatot az összeállított listából; frissül a névsor/darabszám, az utolsó elemnél a sáv eltűnik, a gyakorlat újra hozzáadható.
- A közös gombok teljes látható, íves felülete jelzi az érintést, gyors koppintáskor is. A natív méretek, érintési tartomány, tiltás és elrendezés megmarad.
- A részletek X-e és a rendszer-vissza az eredeti lapra, alfülre és görgetési helyre tér vissza. A kipopolt nézet elvetett helyi draftja nem jelenik meg újranyitáskor; a mérőmotor/pause/ownership védelmek megmaradnak.

## Wear OS

Új órás alkalmazáslogika nincs ebben a körben; a telefonos kiadás verziópárja. A meglévő natív mérés és szinkron változatlan, R8/resource shrinking release.

## Ellenőrzés és átadás

Nyers XML: telefon99 fájl/516 teszt + Wear35/189 =705, 0failure/error/skipped; shared NO-SOURCE. Két artifact-ZIP SHA-256 digestje, a tényleges APK-k hash/package/verzió/v2 signer adatai helyben ellenőrizve. Mindkét APK tartós aláírója egyezik; Maps, páros identitás és Wear-companion feltételek a CI-ben ellenőrizve. AAB ellenőrzés és feltöltés SKIPPED, AAB artifact nincs.

TelefonAPK5 528 235 bájt, WearAPK4 669 627 bájt. A meglévő TrainPilot Native Drive-mappába két APK, két külön leírás és bizonyíték került; mind az öt neve/mérete/parentje visszaolvasva. Régi fájlok megmaradtak. Play-feltöltés nem történt. Készülékes elfogadás hátravan; mindkét app frissítendő.

- [Telefon APK](https://drive.google.com/file/d/1o6gIxwpbkSagtoYRbWDhFo_Pq_1ZI53z/view?usp=drivesdk)
- [Wear OS APK](https://drive.google.com/file/d/1wiMX-k6tx2DiEqXTDrpJB0pfLx-fp74P/view?usp=drivesdk)
- [Telefonos változások](https://drive.google.com/file/d/1necHkPGpBTUD62hrUAQSF-Ak_xhHmEjP/view?usp=drivesdk)
- [Órás változások](https://drive.google.com/file/d/1KIFJEb37p4zNLpOfjV1JUItjxznORcr6/view?usp=drivesdk)
- [Ellenőrzési bizonyíték](https://drive.google.com/file/d/1Lpdnm_ET2nDPwG1bqRzoDWt3aeql81AD/view?usp=drivesdk)

A részletes adatok: 2763-verified-apks.json. Az átadást rögzítő commit csak dokumentáció; az alkalmazáskód a fenti signed forrás.

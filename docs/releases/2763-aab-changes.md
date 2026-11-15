# TrainPilot 2763 · 1.1-beta — Play Console AAB-pár

A felhasználó 2026-11-15-én az APK-k után az AAB-párost is kérte a Drive-ra.
A meglévő trusted-main signing workflow `build_aab=true` módban sikeres:
[Actions 43721805883](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43721805883).
Forrás: `089a0917d3e71b75b44e9b62dc59b7efc030ad48`.

| Csomag | Verziókód | Méret | Drive |
| --- | ---: | ---: | --- |
| Telefon AAB | 276300 | 10 158 922 bájt | [Letöltés](https://drive.google.com/file/d/1Q52YEiNRYBFOnyzqRjyFAwuH8_2EM7AC/view?usp=drivesdk) |
| Wear OS AAB | 276302 | 5 580 504 bájt | [Letöltés](https://drive.google.com/file/d/13qrXuYJWLsm4TQkmtgoPV3VvAWiMcWUa/view?usp=drivesdk) |

Mindkettő `com.TrainPilot.com`, `1.1-beta`. A telefonos AAB a telefonos zárt
tesztcsatornához, az órás AAB a külön Wear OS zárt tesztcsatornához tartozik.
Play Console-feltöltés nem történt; azt a felhasználó végzi.

Az alkalmazás-, teszt- és buildforrások a korábban átadott APK-k
`8367634a78824d1955956ce90ec424ee92e891a3` forrásával megegyeznek; a köztes
commit csak az átadást dokumentálta. A most újraépült APK-k minden ZIP-bejegyzése
azonos a korábbiakéval, kivéve a beágyazott forráscommitot tartalmazó
`META-INF/version-control-info.textproto` fájlt. A korábban átadott Drive APK-k
megmaradtak; új működés vagy verzióemelés nem került ebbe a körbe.

A három artifact-ZIP SHA-256 digestje, a nyers JUnit XML, a négy bináris hash-e,
a tényleges APK/AAB-manifeszt package/verziója és a tartós aláíró ellenőrizve.
Mindkét AAB JAR-aláírása helyben is ellenőrzött. A közös kapu 516 telefonos és
189 Wear OS tesztet futtatott: **705 teszt, 0 failure/error/skipped**;
shared NO-SOURCE. A Maps és páros identitás ellenőrzése sikeres.

A két AAB és a [nyilvános ellenőrzési adatok](https://drive.google.com/file/d/1Yec2Gi3MBbSwhjHJWfotC7vvqEZWSxSo/view?usp=drivesdk)
a meglévő TrainPilot Native Drive-mappába kerültek. Név, bájtméret és szülőmappa
visszaolvasva; korábbi fájlok és megosztási állapot megőrizve. A két meglévő
TXT-változáslista a 2756→2763 közötti összesített változásokat tartalmazza,
és most már az elkészült AAB-kat is jelzi. Teljes tartalmuk visszaolvasva.
A rövid Play Console-szövegek 422/395 karakteresek.

- [Telefonos változások](https://drive.google.com/file/d/1necHkPGpBTUD62hrUAQSF-Ak_xhHmEjP/view?usp=drivesdk)
- [Wear OS változások](https://drive.google.com/file/d/1KIFJEb37p4zNLpOfjV1JUItjxznORcr6/view?usp=drivesdk)

Részletes bizonyíték: `2763-verified-aabs.json`. Az eredeti APK-átadás bizonyítéka
történeti állapotként megmaradt. A fizikai készülékes elfogadás külön lépés.

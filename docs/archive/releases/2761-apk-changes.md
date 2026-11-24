# TrainPilot 2761 · 1.1-beta — teszt-APK páros

Forrás: `a9b903f353a0e5dc2b2e8e01e6d3291bc560ccc1` (PR #236), signed run [43593234136](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43593234136), SUCCESS. Kizárólag APK-mód (`build_aab=false`); AAB nem készült. AAB-páros a felhasználó készülékes jóváhagyása után készülhet, Play-feltöltés nélkül.

| Csomag | Kód | Méret | Letöltés |
| --- | --- | --- | --- |
| Telefon release | 276100 | 5 495 467 bájt | [APK](https://drive.google.com/file/d/15AvJxFTAaSBn0sK3tgKysP77RaTzwoRp/view?usp=drivesdk) |
| Wear R8/shrink release | 276102 | 4 669 627 bájt | [APK](https://drive.google.com/file/d/1uEWjaFM4VdWDJSWLTwRjk1Vb7TjWMov3/view?usp=drivesdk) |

Mindkét csomag: `com.TrainPilot.com`, `1.1-beta`. A két ZIP SHA-256 digestje, a nyers XML, a bináris fájlhashek és a tényleges manifest-verziók helyben ellenőrizve. Az APK v2 signing blokkjából kiolvasott tanúsítvány SHA-256-ja mindkét fájlnál a tartós aláíró: `a72fd016d94e9d94e2b2282032f8c3820d4f09cba9ff1db05c3fd1de6ffa7dd6`. A kriptográfiai aláírás-, Maps- és páridentitás-ellenőrzést a signed kapu is elvégezte.

Teljes JVM-kapu: telefon 94 fájl / 504 teszt + Wear 35 fájl / 189 teszt = 693, 0 failure/error/skipped; shared NO-SOURCE. Mindkét AAB-lépés SKIPPED, nincs AAB artifact. Részletes bizonyíték: [2761-verified-apks.json](2761-verified-apks.json).

A telefonos javítások: kezdőlapi naptári cél frissülése és azonos alkalom indítása; valós A/B nap a naplóban; másik program csak újratervezés alkalmazásakor; megtartott sorok saját program/nap ID-ja; Coach közvetlen natív indítás; 15–240 perc; egyszeres Play; nagyobb nappill és idő; laposabb íves pihenődokk; alakba vágott nyomási effekt; görgetéskor is látszó fejléc; dátumos függőleges fotók; finom gombháttér; rövid Mentés / Visszaállítás / Importálás. Régi import és egyedi nevek megmaradnak.

A Wear a meglévő natív mérőmotorral és 2760-as napló-/téma-szinkronnal párosított kiadás. Mindkét APK frissítendő. A készülékes vizuális és telefon/óra elfogadás hátravan; a zöld regresszió ezt nem helyettesíti.

A két APK, a [telefonos leírás](https://drive.google.com/file/d/13eFVjbFbCJfh_spsBVm29GH6DwihfXr5/view?usp=drivesdk) és az [órás leírás](https://drive.google.com/file/d/1uitklXOSXJAUKwDTbdAhO2dLm-PKFRbB/view?usp=drivesdk) a meglévő TrainPilot Native Drive-mappában van. Az [átadási bizonyíték](https://drive.google.com/file/d/1t8Tuaa3YYjLEDz0ekmsvAa_5-IWo5rpy/view) is feltöltve. Mind az öt fájl neve/mérete/parent ID-ja visszaolvasva, korábbi fájlok megőrizve.

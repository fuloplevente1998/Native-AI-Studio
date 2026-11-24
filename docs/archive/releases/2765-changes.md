# TrainPilot 2765 · 1.2-beta

PR259 auditjavítás és PR260 kiadási metaadat mainen. Felhasználó által kért
telefonos és Wear APK+AAB páros, aláírt forrás `bf0bfde5b79972d7dd56e5b7852aa3e41a4f1787`.
[Signed run44121032371](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/44121032371) SUCCESS, `build_aab=true`.
A négy fájl ugyanabból a futásból származik.

| Eszköz | Verziókód | APK | AAB |
| --- | ---: | --- | --- |
| Telefon | 276500 | [Letöltés](https://drive.google.com/file/d/1xgSMNUqaRvRPVeH5vOVd0ANNlx-q5xD_/view?usp=drivesdk) | [Letöltés](https://drive.google.com/file/d/1AJQ53koK9X47u7AdgDMZWxzc_nBBxDb6/view?usp=drivesdk) |
| Wear OS | 276502 | [Letöltés](https://drive.google.com/file/d/1U0eJxsPrEfCOHSen29dbqESfyb4lJbDw/view?usp=drivesdk) | [Letöltés](https://drive.google.com/file/d/133w5x3Uu1ahj0qRYRgOymyiBCiJuA24G/view?usp=drivesdk) |

## Változások a 2764-hez képest

Telefon: aktív beépített program szerkesztésekor saját példány aktiválása,
azután ismételt mentés azonos ID-val; nagy, stabil szélességű élő stopper és
olvasható kétoldali felület; kalória mentés előtt is a naplóval egyező forrásból
és aktív ablakból. Tartós betűméret/kompakt mód és intervallumpreset, tényleges
natív intervallumindítás, biztonságos kültéri előnézet és témakövető rendszersáv.

Telefon és óra együtt: tartós helyi parancs-újrapróba és commit/ACK bizonyíték
egyeztetése, meglévő owner/generáció/pause/restore/lezárási védelemmel;
HU/EN/DE/RO névfeloldás a tényleges órás payloadokban és naptári visszajelzés.
A Wear saját képernyőkódja ebben a körben nem változott. A külön PR220 teljes
témaszinkron-/dizájntervét ez a kiadás nem jelenti lezártnak.

Részletes leírás: [telefon](2765-phone-changes.txt), [Wear](2765-wear-changes.txt).
Rövid Play-leírás: [telefon](2765-phone-play-notes.txt), [Wear](2765-wear-play-notes.txt).
[A teljes audit és készülékes ellenőrzőlista](../AUDIT_COMPLETION_2026-11-17.md).

## Ellenőrzött eredmény

A signed kapuban telefon120 XML/627 teszt, Wear36 XML/198 teszt;
összesen825, 0failure/error/skipped, shared NO-SOURCE. Telefon és Wear release
R8/resource shrinking. A három letöltött artifact ZIP SHA-256 digestje egyezik
a GitHub-metaadattal; nyers XML önállóan újraszámolva, CI-jelentéssel összevetve.
A négy tényleges manifest package/verziója, fájlhash és tartós signer ellenőrizve.
APK-aláírás a CI-ben, AAB JAR-aláírás a CI-ben és helyben is ellenőrizve.
Maps-konfiguráció, Data Layer páros identitás és Wear-companion feltételek a kapuban igazoltak.

Package: `com.TrainPilot.com`; mindkét verziónév `1.2-beta`.
Signer SHA-256: `a72fd016d94e9d94e2b2282032f8c3820d4f09cba9ff1db05c3fd1de6ffa7dd6`.
Telefon APK5 562 155 bájt, Wear APK4 669 631 bájt;
telefon AAB10 240 100 bájt, Wear AAB5 584 255 bájt.

Drive-on kilenc új fájl: négy bináris, két külön változáslista, két rövid
Play-leírás és bizonyíték. Mind a kilenc neve/mérete/parentje visszaellenőrizve;
korábbi fájlok megmaradtak. [Bináris bizonyíték](2765-verified-artifacts.json),
[Drive-átadás](2765-drive-delivery.json).

Mindkét appot frissíteni kell. Play Console-feltöltés és fizikai telefon–óra
elfogadás még hátravan; a regresszió ezt nem helyettesíti. Az eredeti kiadott
Plank no-live tünet motorhibaként nem lett reprodukálva, az új élő UI/futás/pause/
rögzítés automatizáltan ellenőrzött. A katalógus JVM-mérőpont nem igazol készülékes
startupgyorsulást. A lezáró dokumentáció változatlanul hagyja a tesztelt kódot.

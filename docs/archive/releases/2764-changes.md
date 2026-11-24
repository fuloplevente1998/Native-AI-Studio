# TrainPilot 2764 · 1.1-beta

Felhasználó által jóváhagyott main-integráció: PR253, a benne foglalt PR248 és
PR249 is mainre került. Aláírt forrás:
`80306fdf78802ecdbba3a38168a0b914607f693a`.
[Signed run43940583135](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43940583135)
SUCCESS, `build_aab=true`. A telefonos és órás APK/AAB ugyanabból a futásból származik.

| Eszköz | Verziókód | Telepíthető csomag | Play Console csomag |
| --- | ---: | --- | --- |
| Telefon | 276400 | [APK](https://drive.google.com/file/d/1XsU5TCyK3WebVaNfEHHHdWye_0BjWBqD/view?usp=drivesdk) | [AAB](https://drive.google.com/file/d/1k2YoibR8Blv9g3kTJ1EZTXr36ulLX3PR/view?usp=drivesdk) |
| Wear OS | 276402 | [APK](https://drive.google.com/file/d/1XT6akzbPXwwv7C62toz7W5Xktr4tYHLv/view?usp=drivesdk) | [AAB](https://drive.google.com/file/d/12jtyd0I8LfIg4GZ9WK9yM_fuLLsIx15O/view?usp=drivesdk) |

## Változások a 2763-hoz képest

- Telefon: az első profil/terv panel a jóváhagyott közös tervező; az onboarding
  csak tartós siker után fejeződik be. Egyedi gyakorlat/program megbízható mentése
  és látható hibakezelése. Natív gyakorlatléptetés, vezérlési védelem és az utolsó
  futó mérés szüneteltetése az összesítés előtt.
- Wear és telefon együtt: telefon-tulajdonú kétoldali kész/Undo tartós natív
  eseménnyel; célzott pihenő-visszavonás, részleges/felcserélt/duplikált kézbesítés
  kezelése és elavult hibanyugták elleni védelem.

Részletes külön leírás: [telefon](2764-phone-changes.txt), [Wear](2764-wear-changes.txt).
Rövid Play-leírás: [telefon](2764-phone-play-notes.txt), [Wear](2764-wear-play-notes.txt).
Az audit P2/P3 feladatai ettől külön maradnak.

## Ellenőrzés és átadás

A signed kapu ismét lefuttatta a teljes JVM regressziót: telefon105 XML/558 teszt,
Wear36/198; összes756, 0failure/error/skipped, shared NO-SOURCE. Telefon és Wear
release R8 + resource shrinking. A letöltött három artifact ZIP SHA-256 digestje
a GitHub metaadatával egyezik. A tényleges APK-manifeszt és AAB-protobuf-manifeszt
package/verzió adatai, a négy fájl hash-e és a tartós aláíró helyben ellenőrizve.
Az APK aláírása a CI-ben, a két AAB JAR-aláírása a CI-ben és helyben is ellenőrzött.

Package mindkét eszközön `com.TrainPilot.com`, verziónév `1.1-beta`.
Tartós signer SHA-256:
`a72fd016d94e9d94e2b2282032f8c3820d4f09cba9ff1db05c3fd1de6ffa7dd6`.
Maps-konfiguráció, Data Layer páros identitás és Wear-companion feltételek a CI-kapuban igazoltak.
Telefon APK5 524 687 bájt, Wear APK4 669 627 bájt;
telefon AAB10 165 700 bájt, Wear AAB5 584 257 bájt.

A meglévő TrainPilot Native Drive-mappába kilenc új fájl került: négy bináris,
két külön részletes változáslista, két rövid Play-leírás és ellenőrzési bizonyíték.
Mindegyik neve/mérete/parentje visszaolvasva; a régi kiadások megmaradtak.
[Bináris bizonyíték](2764-verified-artifacts.json) · [Drive-átadás](2764-drive-delivery.json).

Mindkét alkalmazás frissítendő. A Play Console-feltöltés a következő felhasználói
lépés: a telefonos AAB a telefonos, a Wear AAB a Wear OS tesztcsatornára kerül.
A készülékes elfogadás külön hátravan; a regresszió nem helyettesíti azt.
A lezáró dokumentációs commit nem változtat alkalmazás-/teszt-/buildforrást.

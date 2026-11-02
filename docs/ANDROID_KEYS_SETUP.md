# TrainPilot – saját kulcsok beállítása

Soha ne másolj API-kulcsot, jelszót vagy privát aláírókulcsot chatbe, issue-ba vagy Gitbe.
Új aláírókulcs készül: az eltérő kulccsal korábban telepített appot ez nem frissíti.
Meglévő felhasználói adatot/mentést ne törölj telepítési hibakerülésként.

## 1. Állandó telefon/óra aláírás

Hivatalos GitHub-oldal: https://github.com/fuloplevente1998/Native-AI-Studio/settings/environments

Hozz létre `android-signing` environmentet. Deployment branches: kizárólag `main`;
ha a GitHub-csomagod engedi, állíts be kötelező manuális jóváhagyót is.
A kulcsokat az environment Secrets részébe tedd, ne általános repository secrets közé.

Saját számítógépen, Python 3 és JDK keytool mellett:

```sh
python3 scripts/setup_android_signing.py
```

A jelszót a saját terminál rejtett bekérése kezeli; nem írja ki és nem menti fájlba.
A kulcs a Git-repón kívül, `~/.trainpilot-signing/trainpilot-release.jks` helyen készül;
RSA 3072, alias `upload`. Meglévő kulcsot a program nem ír felül.
A JKS és a base64-fájl is privát: őrizd meg titkosított biztonsági mentésben,
a jelszót külön jelszókezelőben. A base64 nem további titkosítás.

Environment secrets:

| Név | Érték |
|---|---|
| `TRAINPILOT_RELEASE_KEYSTORE_BASE64` | A generált, helyi base64-fájl tartalma |
| `STORE_PASSWORD` | A terminálban megadott jelszó |
| `KEY_PASSWORD` | Ugyanaz a jelszó ennél az új kulcsnál |
| `TRAINPILOT_RELEASE_CERT_SHA256` | A kiírt nyilvános SHA256 ujjlenyomat |

Ha saját `gh` CLI bejelentkezésed admin jogosultságú és az environment már létezik,
`python3 scripts/setup_android_signing.py --upload` új kulcs készítésekor közvetlenül,
stdinről feltölti ezeket az environment titkai közé. Titok nem kerül a parancssorba.
Csak az egyik létrehozási módot futtasd; meglévő kulcs esetén a kézi űrlapot használd.

## 2. Google Maps

- Google Cloud projekt és hitelesítő adatok: https://console.cloud.google.com/apis/credentials
- Ugyanebben a projektben engedélyezd a **Maps SDK for Android** API-t és a szükséges billinget.
- API-kulcs application restriction: **Android apps**, package **com.TrainPilot.com**,
  SHA1: az új állandó kulcs `PUBLIC_CERTIFICATE.txt` fájljából.
- API restriction: **Maps SDK for Android**. Ne hagyd korlátozás nélkül.
- Hivatalos repository-secret űrlap:
  https://github.com/fuloplevente1998/Native-AI-Studio/settings/secrets/actions/new
- Név: `TRAINPILOT_MAPS_API_KEY`; Secret: a Cloud API-kulcs.

A kulcs az APK-ba szükségszerűen bekerül; az Android/package/cert és API-korlátozások
védik a felhasználását. A változó CI-tesztkulcs SHA1-ével ne konfiguráld a végleges appot.
Az új aláíráshoz kötött Maps kulcs a régi ideiglenes debug buildben nem feltétlenül működik.

## 3. Ellenőrzött signed APK-pár

A `TrainPilot persistent signed APK pair` workflow csak mainről, kézzel indítható,
`android-signing` environmenttel. A meglévő release signingConfigs változatlanok;
a keystore ideiglenes runner-fájlból, jelszavak environment secretből jönnek.
A workflow futtatja a teljes JVM-csomagot, release phone/Wear buildet,
és a rögzített certhez ellenőrzi az APK-k signerét. Csak siker után tölt fel APK-t,
privát kulcsot soha. Nincs Play/release publikálás. ApplicationId/verzió változatlan.

A képernyős térképcsempéket, fizikai telefon/óra kapcsolatot és a Samsung SDK olvasást
készüléken is igazolni kell. A Samsung közvetlen SDK saját app/cert regisztrációt,
a Google OAuth/Fit/Drive integrációk külön projekt-/client-konfigurációt igényelhetnek;
ezek nem helyettesíthetők egy általános „Google kulccsal”.

Állapot a PR létrehozásakor: beállítási kód előkészítve; valódi új tulajdonosi kulcs,
Maps API-kulcs és első állandó signerrel épített APK még nincs létrehozva/igazolva.

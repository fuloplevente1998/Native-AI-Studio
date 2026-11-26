# Funkció- és dizájnfeladatok követése

A 2026-11-04-i felhasználói kérdés alapján a nyitott jegyek típuscímkékkel
elkülönülnek. A meglévő bug/enhancement címkék megmaradnak:

- `type:functional`: működési/állapot-/vezérlési/mérési/szinkronhiba.
- `type:design`: ikon, keret, kontraszt, kompakt elrendezés, témakövetés.
- `type:data-ui`: adat- és felületösszevonás deduplikációs/adatmegőrzési szerződéssel.

## Javasolt munkamenet

1. Egy main marad; nem hozunk létre tartós, egymástól eltérő funkció és dizájn főágat.
2. Egy körülhatárolt javítás rövid saját ágon, reviewzható PR-ben készül. Például
   `fix/128-stopwatch-controls`, `ui/126-workout-control-contrast`.
3. Igazolt közös okú hibák együtt javíthatók egy ágon/PR-ben, mindegyik jegyhez
   reprodukcióval és elfogadási bizonyítékkal. #128/#129/#130 most együtt vizsgálandó;
   a közös ok még nem bizonyított. A session/ownership/adatmegőrzés hibáit célszerű
   a kozmetikai módosítások előtt stabilizálni.
4. A gomb működését ne alakítsuk át eldugott dizájnmódosításként: #126 Elvetés
   kontrasztja és #130 kétszeri elvetése külön követett feladat marad.
5. Közös fájlokat érintő ágak egymás után integrálandók, a következő ág a friss
   mainhez igazítandó. Nem szabad régi UI-kóddal felülírni az új funkciójavítást.
6. Funkcióváltozáshoz érdemi regresszió, felülethez megfelelő téma-/méret-/ablak-
   ellenőrzés kell; fizikai készülékes elfogadás külön állapot. Az új CI nem
   csomagol debug APK-kat, a kiadás signed telefon release + optimalizált Wear release.

Ez a dokumentum a szervezést és meglévő branch/PR-szabályt pontosítja.
A visszajelzés-rögzítés nem jelent engedélyezett új APK-kört vagy elkészült javítást.

## Verziószám-emelés és kiadás

Felhasználói döntés (2026-11-26): a már ellenőrzött kód puszta belső
verziószám-emelése miatt ne ismételjük meg a teljes regressziót. A kiadás előtt
ellenőrizzük a Playen már használt telefonos és Wear-kódokat, a két új kódot,
valamint a Play-listing szövegét; lehetőleg mindez kerüljön a teljes CI elé.
A meglévő séma (telefon 00, Wear 02) és a versionName maradjon meg, kivéve
külön felhasználói döntés esetén.

### Tervezett szűkített kiadási kapu (még nincs implementálva)

1. A forrás egy már zöld, megbízható main CI SHA leszármazottja; a korábbi futás
   JVM, nyers JUnit XML és R8 eredménye igazolt.
2. A két SHA teljes diffjét ellenőrizni kell: csak az app/build.gradle.kts
   és wear/build.gradle.kts versionCode értékei és hozzájuk tartozó kommentek,
   valamint kiadási dokumentáció változhatnak. Ellenőrizni kell, hogy a kód nem
   használja a versionCode-ot viselkedési vagy migrációs döntéshez. Minden más,
   illetve bizonytalan változás teljes CI-t igényel.
3. Az új kódok magasabbak és nem foglaltak a Playen, az alkalmazásazonosító,
   versionName, függőségek, signer és kiadási konfiguráció változatlan.
4. Az új main SHA-ról egyetlen friss signed telefon/Wear APK+AAB pár készüljön.
   Az R8/resource shrinking, csomag-metaadat, tartós signer és artifact digest
   ellenőrzések kötelezőek. A korábbi CI-t nem szabad az új SHA saját teljes
   regressziójaként feltüntetni.
5. Új forrás esetén az elavult aláíró futást állítsuk le, ha még fut és a
   megfelelő jogosultság/eszköz rendelkezésre áll. Ne állítsuk le az új CI-t,
   ne indítsunk duplikált signingot. Ha nincs leállítási lehetőség, a régi
   artifactot ne adjuk át kiadásként. Dokumentáljuk a pontos SHA-kat és futásokat.

A jelenlegi trainpilot-android-ci és trainpilot-persistent-signing workflow
változatlanul teljes main CI-t követel az automatikus aláíráshoz. A szűkített
útvonalat külön jóváhagyott és tesztelt workflow-módosításnak kell bevezetnie,
nem kézi ellenőrzéskihagyással. A dokumentáció módosítása önmagában nem
engedélyez új kiadást vagy Play-publikálást. Futó kiadás közben csak külön
dokumentációs ágat használjunk, amíg az aktuális SHA-hoz kötött aláírás meg nem történt.

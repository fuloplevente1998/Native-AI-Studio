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

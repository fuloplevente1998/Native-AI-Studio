# Android buildidő – mért alapállapot és első gyorsítás

## Mért alapállapot

Aláírt main: `d5621240c122eb97451bf9980b8f5b77b38641bb`, Actions `41885739541`.
A job 18:28:21–18:45:03 UTC között futott (16 perc 42 mp), ebből a Gradle
18:28:49–18:44:49 (16 perc). A run korábban indult, de a tényleges job előtt várt.
A letöltött nyers JUnit XML: 455 teszt, 89 suite; összegzett suite-idő 79,71 mp
(telefon 77,24 mp / 279 teszt, Wear 2,47 mp / 176 teszt). Ez nem a teljes Gradle
Test task falióra-ideje: a JVM/Robolectric indulás és tesztfordítás ezen felül van.

A fő költség a hideg fordítás és a release/debug variánsok előállítása. A logban:
`Gradle User Home cache not found. Will initialize empty.` és a végén
`Cache is read-only: will not save state for use in subsequent builds.`
210 taskból 183 futott, 27 volt FROM-CACHE. A Robolectric SDK cache már működött.

## Változások

- Az aláírt, kizárólag mainen futó job immár elmenti a Gradle User Home cache-t.
  Az első sikeres új futás felmelegíti a mainen és PR-ekből is visszaolvasható
  alapértelmezett ági cache-t. A dependency/transform/task-output cache-ben nincs
  privát JKS: az továbbra is RUNNER_TEMP alatt él és törlődik.
- Nincs configuration-cache exportálás titkosítási kulcs nélkül. A teljes projekt,
  a keystore és a jelszavakat is tartalmazható konfiguráció nem kerül saját cache-be.
- Mindkét build `--profile` kapcsolót kap. A `trainpilot-build-profile` artifact
  a Gradle feladatok tényleges időit őrzi, külső Build Scan publikálás nélkül.
- Csak a két generált sablonteszt került ki: `2 + 2 == 4` és az appnév egyszerű
  resource-olvasása. Ezek kivezetése önmagában nem magyaráz többperces gyorsulást.
  A funkcionális, backup/restore, Health, ownership, Wear és UI regressziók maradnak.
- A signed job a nyers XML-t ugyanazzal a checkerrel ellenőrzi, mint a PR CI,
  és a közös modul eredményét / NO-SOURCE állapotát is dokumentálja.

## Következő mérés

A cache-melegítés előtti és utáni futásnál külön nézendő a várakozás, Gradle-idő,
FROM-CACHE taskok és profil. Konkrét gyorsulást csak ezek összevetése után állítunk.
A forrásváltozás továbbra is invalidálhat fordítást. A kiadási futás debug JVM-
tesztjei egyelőre megmaradnak: a Compose teszt-Activity a debug manifestből jön,
így a puszta testReleaseUnitTest-re csere nem bizonyítottan egyenértékű.

## Csak release telepítők, külön teszt-APK csomagolás nélkül

A felhasználó csak release telefonos APK-t és optimalizált Wear APK-t kér.
A signed folyamat már ezt állítja elő: Wear release `isMinifyEnabled=true`,
`isShrinkResources=true`. A PR CI ezentúl csak a teljes JVM/Robolectric csomagot
futtatja, a valós telefon/Wear forrás és erőforrás lefordításával. Nem hívja az
assembleDebug / assembleOptimized taskokat, így nem dexel és R8-csomagol felesleges
telepítőket. Az ideiglenes debug kulcs és a debug APK artifactok is megszűnnek.
A debug UnitTest variáns a meglévő Compose teszt-Activity miatt egyelőre megmarad,
telepíthető debug APK nélkül. Ez nem tesztszűrés: a teljes tesztcsomag változatlan.

A tényleges APK-k fordítása, pinned aláírása, package/verzió/Maps/páros-ellenőrzése
az aláírt release job kapuja marad. A PR tesztriport nem állít APK-elfogadást.
Az új útvonalon a tasklistából és a Gradle profilból ellenőrizhető, hogy nem fut
assemble, dexBuilder, APK-package vagy minify/R8 a PR teszteléséhez.

## Első igazolt eredmények

- PR115 / CI41906780189: üres Gradle User Home indulás mellett **6m18.75s**
  Gradle, 76 task (71 végrehajtva, 5 FROM-CACHE), 460 nyers teszt, 0 hiba/kihagyás.
  A dekódolt tasklogban nincs APK assemble/package, DEX vagy minify/R8 task.
  A korábbi fotós CI41902937087 Gradle-lépése 13m03s volt. A modulok termékforrása
  azonos; a két sablonteszt már hiányzik. Ezek külön mérések, runner-ingadozás lehetséges.
- Signed main41905695506: **13m14.13s** hideg Gradle, 210 task (183 végrehajtva,
  27 FROM-CACHE), ugyanaz a 460 teszt. A Gradle main-cache, dependency és transform
  bejegyzések elmentése a post-step logban igazolt. A telefon release 20 433 912,
  az R8-optimalizált Wear release 4 636 863 bájt. A signed job továbbra is mindkét
  variánst fordítja a meglévő debug tesztharness miatt; PR CI-ben nincs APK-csomagolás.

- Meleg-cache mérés mainen: CI41908345166, source05d820a. Gradle-lépés 23 mp,
  teljes job 1m26s, 76 taskból 52 FROM-CACHE és 24 végrehajtott. Az app/Wear
  compile és test taskok is FROM-CACHE; a visszaállított 460 XML eredmény
  hiba/kihagyás nélküli, a GitHub ZIP digest és a source-report visszaellenőrizve.
  A termék- és tesztforrás azonos a signed99f2adf forrással; a workflow/docs
  változott. Ez bizonyítja a main-cache visszaolvasását másik jobból. Az új kód
  újrafordítást igényelhet: a 23 mp nem minden következő APK-ra szóló ígéret.

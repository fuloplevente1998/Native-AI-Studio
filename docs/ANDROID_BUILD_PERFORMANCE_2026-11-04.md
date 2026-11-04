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

# #269 — AI Studio mainre került változásainak első forrásauditja (2026-11-18)

## Forrás és elkülönített bizonyíték

- 2766 kiadási kiindulás: `f03b586ed6dd980ba4a9962cd1e821382105ad71`.
- AI Studio commit: `1a56b3ecec477b3cef4561c9b4b5e62c0bf0648c`, egy commit, 12 módosított/új fájl.
- Az AI Studio már mainen van: a külön draft PR kizárólag a későbbi ellenőrzést és javításokat tartalmazhatja; a mainen lévő commitot nem lehet új PR-ként újra egyesíteni.
- Képernyőkép: nincs ebben az auditban készülékes, ugyanazon buildről származó bizonyíték.
- AI Studio által jelentett teszt: StarterPlan 7, StarterTemplateAudit 8, NavigationOrigin 5, összesen 20; `compile_applet` sikeresnek jelentve. Ezek nem azonosak a GitHub Actions natív telefon/Wear teljes JVM kapujával; forrás-SHA és nyers JUnit XML nincs itt igazolva.

## Megállapítások a tényleges forrásból

1. A `com.repforge.app.MainActivity` migráció/recovery kapu után `NativeFirstLaunchGate`-et nyit; a `com.example.MainActivity` szintén ezt a gate-et hívja. A gate `profileEditor` callbackje a `StudioPersonalPlannerDialog(viewModel, completeOnboarding=true)` komponensre mutat. A régi `TrainingProfileDialog` is a közös `PersonalPlannerModalDialog`-ot csomagolja. Ez forrásszintű egységesítés, nem valódi friss telepítéses, választás/mentés/visszatöltés bizonyíték.
2. A `StudioStarterPlan` 60+ perces, nem újrakezdő és nem kímélő otthoni izomépítésnél hét helyet enged, a kiválasztást `minutesFor`-ral szűri. A célzott regressziók a hat- és héthelyes napot, kizárást, helyettesítést és sorrendet vizsgálják. A teszt tényleges futtatása a commiton még nem ellenőrzött.
3. A `NavigationOriginUiRegressionTest` valódi Compose UI-val tesztel egy Fejlődés → naplórészlet → vissza útvonalat, görgetett állapotot és lenyomásjelzést. A #269 teljes Programok/Napló/Naptár/Gyakorlatlista gesztus- és tartós mutációs elfogadását ez önmagában nem bizonyítja.

## Következő, elkülönített javítási szakaszok

1. Első indítás: launcher manifest és mindkét activity útvonal, tiszta alkalmazásadat, profilválasztó, mentés és visszatöltés, kihagyás/vissza/hiba; célzott `FirstRunPlannerRegressionTest` bővítés.
2. Programok: rövid/hosszú/⋯ egyező stabil rekord-ID-val, gyári/aktív/saját korlátok, megerősített törlés, dupla írás elleni védelem; Compose és tartós adat regresszió.
3. Napló/Naptár/Gyakorlatlista: külön ID- és visszatérési állapot audit, hibás callbackek javítása, megőrzött szűrő/hónap/görgetés, biztonságos törlés; egymástól elkülönített regressziók.
4. Tervgenerátor: hat/hét hely, 60 perces időkorlát, kizárások, fő nyomógyakorlat és tricepsz sorrend teljes natív CI-n; csak bizonyított hibát módosítani.

## Integrációs határ

Nincs új APK/AAB, verzióemelés, main-merge vagy Play-feltöltés. A 2766 / 859 teszt az előző kiadás bizonyítéka, nem az AI Studio commit ellenőrzése. Teljes telefon+Wear GitHub Actions futás, JUnit XML, source SHA és fizikai készülékes elfogadás függőben.

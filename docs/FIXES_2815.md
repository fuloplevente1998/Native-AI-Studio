# 2815 – készülékes visszajelzés a #400 aláírt buildről

Alap: main `01d35828`, 1.5.3, 280900/280902. #400 már merge-ölve; új javítás külön ágon.

## A – edzésenergia: hiányos HC lefedettség és forrásonkénti eltérés

Készülékes reprodukció: az előző napi edzés mentett, időablakos Health Connect teljes energiája megjelenik az egészségnaplóban, az aktív energia hiányzik, és az érték lényegesen alacsonyabb a felhasználó által várt értéknél. A részletes kalóriarekordok az edzés kezdete körül megszakadnak; Samsung Health saját aktivitási grafikonján az edzés időszakában aktivitás látható. A visszaolvasás részleges sikerét igazolja a mentett edzés energia sora, a tényleges szolgáltatói lefedettséget nem. A pontos Samsung SDK válasz és a szinkron lezárult állapota még nem ismert. A személyes mérési értékeket és képet nem mentjük a repóba.

### Forráskód-audit
- `HealthConnectRepository.readTrainingWindow` és `HealthConnectLegacyBridge.readTrainingWindow`: külön aktív/total aggregátum és raw energia rekordok; az aggregate kérés időablaka nem bizonyítja a tényleges adatok időbeli lefedettségét. A meglévő session-energy coverage nem azonos az aktív/total aggregátum coverage-ével.
- `NativeWorkoutActiveWindows.health`: a kért ablakonkénti meglévő értékeket összevonja, de a raw időintervallumokon belüli hiányokat nem vizsgálja.
- `NativeWorkoutHealthSummary`: a kért query-window hosszából számít lefedettséget; azonos definíciójú, hiányos magasabb prioritású forrás elfedhet jobb lefedettségű forrást. Az eltérő energia-definíciókat nem szabad összehasonlítani vagy összeadni.
- `HealthWorkoutEnergyDay`: az aktív/total érték mellett csak a snapshot partial flag jelenik meg; nincs külön energia-lefedettség. A napi aggregátum nem helyettesíti az edzésmérést.
- `SamsungSdkReader.workout`: ActivitySummary aktív/total külön lekérés már létezik; ellenőrizni kell a közvetlen Samsung mérés tényleges válaszát. Az óránkénti aggregátumból tilos az edzéspercekre arányosítással mért értéket előállítani.

### Következő javítás elfogadási feltételei
1. Mindkét HC API: aktív/total külön raw coverage union a tényleges aktív intervallumra vágva; forrásonkénti eredet, rekordok utolsó ideje, mért/hiányos/ismeretlen állapot. SDK aggregátumot megőrizni, hiányzó energiát nem nullázni, nem extrapolálni. A query ablak nem coverage.
2. Samsung direkt időablakos aktív/total olvasás és permission/error/empty státusz ellenőrzése, kézi visszaolvasás és korábbi retry megtartása. Ha nincs időbeli bizonyíték, coverage ismeretlen; nem adunk össze tükrözött forrásokat.
3. Összegzés/egészségnapló: a mért részérték ne tűnjön teljes edzésenergiának; azonos definíció esetén a jobb bizonyított lefedettség kapjon megfelelő elsőbbséget, eltérő definíciók külön. Várható értéket nem írunk be kézzel vagy becsléssel.
4. Eszközön: lezárt sync után külön Samsung Health és HC napi részletek, mentett edzés forráskártyái; forrás saját rekordhiánya vs TrainPilot import. Szintetikus regresszió: egy korai rövid rekord és hosszabb edzésablak, hiányzó aktív/meglevő total, teljesebb alternatíva, késői rekord, overlap, pause, midnight, permissions, data replacement.
5. Kódolás előtt pontos scope; ez a commit csak visszajelzés és audit. Main, verzió, signer és build változatlan.

## B – Mai állapot és Egészségnapló energiarekordok összevetése
Új készülékes bizonyíték: a Mai állapot aktív kalória felcsúszó lapján többperces aktuális energiarekordok jelennek meg. A korábbi naplóképen az összes kalória csempe volt kijelölve; eltérő nap és eltérő rekordtípus, ezért ez nem igazolja a hiányzó tegnapi aktív rekordok meglétét, sem a konkrét forrás azonosságát.
- `HealthTodayTileExtras.kt`: calories -> `NativeDetailedHealthMetrics.recordType("activeCalories")` -> ActiveCaloriesBurnedRecord; provider a primaryHealthSourceSamsung alapján. `StudioHealthMetricsBrowser.kt`: Kalória chip ugyanez; összes kalória csempe TotalCaloriesBurnedRecord. Mindkettő ugyanazt a `HealthRecordDayDetails` / `NativeHealthRecords.load` olvasót használja. Az összes energia csempe szűrője nem látszik külön chipként; a Kalória chip címkéje nem egyértelmű.
- A felcsúszó lap nem ad át dailySummary-t az órás Samsung aggregátum megjelenítéséhez, ellentétben a naplóval. Ez külön UI-paritási hiány, nem bizonyított SDK-hiba.
- Elfogadás: azonos nap + provider + recordType + refresh revision esetén a két felület ugyanazokat a tényleges rekordokat mutassa, a forrás és a két kalóriadefiníció láthatóan különüljön el; a Samsung grouped aggregate mindkét helyen külön jelölve, a napi/edzésösszegből ne generáljunk rekordot. Az összehasonlítást a szinkron befejezése után végezzük. A tegnapi edzés utáni aktív rekordhiány továbbra is nyitott.

## Első implementáció (jóváhagyva)
- HC mindkét API: raw energia interval union külön aktív/total, origin + lastEnd, permission/error -> unknown; nincs kcal extrapoláció. A raw coverage nem bizonyítja az aggregate contributor pontos azonosságát.
- Mentett részletek: új explicit partial/unknown megjelölés, azonos definíciójú, teljes raw coverage előnyben a separateEnergy projekcióban; legacy single-energy változatlan. Automatikus retry HC hiányos/unknown coverage mellett a meglévő 48h keretben; ismert per-window coverage nem csökkenhet automatikusan.
- Mai állapot és Egészségnapló: aktív/teljes külön chip, forrás címke, ugyanaz a persisted daily snapshot a Samsung hourly aggregátumhoz. Mai lap ugyanazon record queryn, nincs új SDK olvasás.
- Új 3 policy regressziós teszt. Samsung SDK közvetlen válasz és az edzéskor megszűnő rekordok oka továbbra is eszközös ellenőrzés; Samsung coverage unknown, nem állítjuk, hogy a 300 kcal visszanyerhető. CI ellenőrizendő. Main érintetlen.

## Második passz – köztes CI nélkül
- #801 (e408132e) nyers XML 1303/0/0/0, R8 sikeres, feltételes DEX skipped. Ez nem igazolja az új head-et.
- Samsung direct aggregate: per-field permission fence (tiltott érték null), empty/failed/denied/unknown coverage külön meta, nincs mesterséges raw idő. A HC API34 workout olvasás immár read-only, mint a legacy: nem futtathat szűk workout window alapján nyers naplórekord-törlő reconcile-t; a napi/change-feed sync marad a raw napló tulajdonosa. A mentett edzés CAS csatolása megmarad.
- Mentés előtti összegző is typed projection, aktív és teljes külön, forrás és coverage címke. A kiválasztott provider + Wear szabály megmarad, mentett edzés automatikusan mindkét providerre kérdez. Legacy egyértékű projection azonos definícióján belül coverage-rangsor. Forráskártya metric coverage. Retry a jobb coverage-et akkor is befogadja, ha a kulcs már létezett. Új Samsung policy teszt.
- [skip ci] köztes commit. További audit: Wear receipt frissesség/ownership, raw napló és Today paritás regresszió, egészségszinkron újraolvasás időablak/forrás egyezése. Készülékes Samsung/HC valódi adatok továbbra sem igazoltak.

## Harmadik passz – Wear és naplóparitás
- Modern live Wear receipt: node/epoch/session/outer+inner timestamp/state guard; a régi flat csomag csak előnézeti kompatibilitás. Telefonos finalizer a friss nested mérést a meglévő mentésbe foglalhatja, provisional partial; későbbi durable summary csak ugyanarra a workoutra, előrehaladó revízióval, azonos ismert órával, meglevő mérési kulcsok megőrzésével. Nem indít új órás szenzort vagy írja át az edzésmotort. A Wear CALORIES_TOTAL továbbra sem active. Rövidebb tényleges Wear activeDuration a prioritást csökkenti, nem számít kcal-t időből.
- Mentett napi edzésenergia a közös provider-specifikus typed projectionből jön, így szünetes/részleges ablak értéke sem tűnik el (nem napi aggregate). Új Room és SQLite fixture: napi/Today azonos provider+nap+recordType; hiányos aktív ablak napi és detail értéke azonos. Wear 3 regresszió. Fizikai 22:57 hiány, SDK és BT adatátvitel még eszközön igazolandó.

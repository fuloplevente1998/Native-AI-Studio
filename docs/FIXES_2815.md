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

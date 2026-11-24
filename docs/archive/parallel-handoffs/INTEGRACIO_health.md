# INTEGRACIO_health – pulzus és regeneráció (#61, 03. chat)

## Forrás / fájltulajdon

- Bázis: `main` @ `ee826e6a744ae5e6332c020e0005134f7dc698b2`.
- Saját ág: `fix/health-pulse-recovery-61`; PR-cél: `main`. A #58 Coach-preview/törlés PR **nem** ennek a változtatásnak a bázisa; az integrátor veszi át a saját munkáját.
- Saját változtatások: `app/src/main/java/com/repforge/app/healthreadings/HealthReadingsProjection.kt`, `app/src/main/java/com/example/ui/components/healthreadings/HealthReadingRows.kt`, `app/src/main/java/com/repforge/app/SamsungSdkReader.java`, `app/src/test/java/com/example/followup/healthreadings/HealthReadingsRegressionTest.kt`, ez a dokumentum.
- Közös fájlhoz **nem nyúltunk**. A VM, képernyő és Home/Today bekötése csak az integrátoré.

## Azonosított tényleges rés

1. A heti `WeeklyHealthDaySummary` nem tartalmaz pulzusadatot, így a forrástól függetlenül hiányzik a heti pulzusnézet.
2. A Today-komponensben a régi `currentHrBpm` a `averageHeartRate` értékből származik, de az nem élő pulzus. A heti/daily pulse-megjelenítéshez a nyugalmi pulzus, a napra aggregált átlag, a legutóbbi *időbélyeges* mérés és az észlelt tartomány más-más fogalom.
3. A `TrainPilotViewModel.kt` jelenlegi 3795–3812. sora saját readiness-képletet használ (hiányzó RHR esetén +15 HR-bónusszal). Ez eltér a `NativeCoachReadiness.calculate` kanonikus számítástól; kizárólag az integrátor cserélheti.
4. A Samsung SDK pulzus `SERIES_DATA` eddig elfogadta a kezdőidő előtti, de a napba átnyúló sorozatpontokat. A szűrés most a minta kezdőidőbélyegét kéri a `[start,end)` intervallumban, érvénytelen bpm-et kizár; a teljes időablak és az előtér/engedély guard változatlan.
5. Készülékes gyökérok továbbra sem ismert: az üres képernyő **nem bizonyítja** az engedély megtagadását vagy a Samsung SDK hibáját.

## Új, önálló API (beköthető, nincs új ViewModel-mezőre hivatkozás)

Importok:

```kotlin
import com.repforge.app.healthreadings.HealthReadingsProjection
import com.repforge.app.healthreadings.PulseDayReading
import com.repforge.app.healthreadings.RecoveryDayReading
import com.example.ui.components.healthreadings.HealthPulseWeekRows
import com.example.ui.components.healthreadings.HealthPulseDayRow
import com.example.ui.components.healthreadings.HealthRecoveryRow
```

A hívásokhoz mindig egy explicit, kiválasztott `provider` kell, pontosan `HealthJournalStore.HC` vagy `HealthJournalStore.SAMSUNG`; ne válassz automatikusan másikat. Példa a meglévő `refreshNativeHealth` napi betöltő részében, a journal `generation` és provider-azonossági kapuja **után**, azonos `now`/`zone` mellett:

```kotlin
val zone = java.time.ZoneId.systemDefault()
val now = java.time.Instant.now()
val provider = store.preferences().optString("provider", HealthJournalStore.HC)
val today = now.atZone(zone).toLocalDate()
val rows = (13 downTo 0).mapNotNull { offset ->
    val d = today.minusDays(offset.toLong())
    // Ezt az API-t a meglévő, kizárólag kiválasztott journal-csatornára szűrt útvonal biztosítja.
    runCatching { com.repforge.app.NativeHealthProviderData.day(store, provider, d) }.getOrNull()
        ?.also { it.put("day", d.toString()) }
}
val todayPulse: PulseDayReading = HealthReadingsProjection.pulseDay(
    provider, today, rows.firstOrNull { it.optString("day") == today.toString() }, now, zone)
val week: List<PulseDayReading> = HealthReadingsProjection.pulseWeek(
    provider, today.minusDays(6), rows, now, zone)
// A workoutHistory-nak a kanonikus Coach számára használt VALÓDI Room-naplóból
// kell érkeznie, ugyanazzal a 14 napos beolvasási szabállyal; nem demo fixture.
// Pl. workoutHistoryDao().getCoachRecentHistory(...).map(::JSONObject)
val todayRecovery: RecoveryDayReading = HealthReadingsProjection.recoveryToday(
    provider, rows, workoutHistory, now, zone)
```

**Figyelem:** a `NativeHealthProviderData.day` forrásnév és napi kulcs az aktuális VM-ben már létezik. A `rows` mindig csak az éppen kiválasztott csatornából készülhet. A fenti `put("day", d.toString())` csak akkor szükséges, ha a már kiválasztott provider-napi DTO-ból a napkulcs hiányozna. Idegen provider-sorok nem kapcsolhatók be. Tilos másik nap vagy forrás méréseivel feltölteni hiányzó eredményt. A `pulseWeek` dátumonként önálló, üres sorokat is visszaad.

A `WeeklyHealthDaySummary` adatosztály integrátori, kompatibilis bővítése:

```kotlin
val pulse: PulseDayReading? = null
```

A hétvégi loopban, a már meglévő `d`, `dayObj`, `provider`, `now`, `zone` paraméterekkel:

```kotlin
pulse = HealthReadingsProjection.pulseDay(provider, d, dayObj, now, zone)
```

A `TrainPilotUiState` integrátori, kompatibilis bővítése:

```kotlin
val todayPulseReading: PulseDayReading? = null,
val todayRecoveryReading: RecoveryDayReading? = null,
```

A `_uiState.update { st.copy(...) }` melletti hozzárendelés: `todayPulseReading = todayPulse`, `todayRecoveryReading = todayRecovery`. Provider-, dátum-, generáció- vagy restore-váltáskor mindkettő legyen null, és csak ugyanazon generationhez tartozó projekció kerülhessen a képernyőre. Üres/hibás olvasás maradjon meg a `ReadingState` enum szerint, **ne** képezzünk `0 bpm` kijelzést nullból.

UI: az existing egészségképernyőn a heti napi bontás után `HealthPulseWeekRows(uiState.weeklyHealthDays.mapNotNull { it.pulse })`; a Home/Today-nál `uiState.todayPulseReading?.let { HealthPulseDayRow(it) }` és `uiState.todayRecoveryReading?.let { HealthRecoveryRow(it) }`. Ezek szándékosan saját belső sorok, nincs új külső erős keret, a `TrainPilotFrames` hostot változatlanul használjuk.

## Kötelező integrátori változások

- `TrainPilotViewModel.kt`: a 3795–3812. sorok képletét és az 1606–1610. sorok demó által felülírt, valódinak látszó 54/87 pontszámát ne használja mérés helyettesítésére. A kanonikus Coach-valóságeredményt a fenti `todayRecovery` szállítja; a `score` (Coach-terhelési korláttal) **nem** azonos a `samsungEnergy` értékkel. `source` alapján kell címkézni a regenerációs jelzést.
- `HealthCoachAndSettingsScreens.kt`, `StudioTodayStatusGrid.kt` és `HomeScreen.kt`: a `currentHrBpm` megnevezés ne legyen „aktuális pulzus” időbélyeges mérés nélkül. Az adapter `mainLabelHu` az igazolt adattípust írja. Az időbélyeg nélküli RHR soha ne váljon utolsó méréssé.
- Heti sor esetén ne a `NativeDailyHealthDetails.pulseSeries` 96-os listájából számíts teljes napi átlagot: annak csak legutolsó/megfigyelt mintaszerepe van. Samsung `averageHeartRate` a lekért, többnyire nem időarányos minták átlaga; a felirat ezt külön jelzi. HC esetén a napi aggregált mérési mező a forrás.
- `READ_HEART_RATE` / HC `READ_RESTING_HEART_RATE` / Samsung `READ_ENERGY_SCORE` állapota az aktuális API eredményből jön; ha az `permissions` és `errors` nincs tárolva, a kliens nem állíthatja bizonyítottnak a hiányzó grantot vagy SDK-hibát.
- **Ne változzon**: foreground, provider kiválasztás, generáció/admission/restore, permission utóellenőrzés, journal merge, backup és secrets/kiadási flow.

## Megvalósított regressziós tesztkör

`com.example.followup.healthreadings.HealthReadingsRegressionTest` (Robolectric): HC átlag+timestamp+RHR, 96-ra határolt sor, RHR-only, nulla/null/hiány, más provider/nap, 7 napos izoláció, grant denied és cache elrejtés, SDK hiba, előző nap/jövő/hibás időpont, valódi Samsung Energy, túl régi/zero Energy, HC-ban idegen Energy kizárás, valós alvásból TrainPilot-becslés, energiaengedély-hiba külön.

A meglévő `DailyHealthDetailsRegressionTest` változatlan. A tesztek JVM-fixture adatokkal futnak; ez **nem** készülékes Samsung/Health Connect bizonyíték.

## CI / készülékes átvétel

A PR `pull_request` automatikusan indítja a `trainpilot-android-ci.yml` teljes debug fordítását és JVM suite-ját. Az eredményt az aktuális PR-head SHA-hoz, Actions run ID-hoz és a letöltött **nyers XML** tesztjelentéshez kell kötni. Az Actions indulása nem PASS; nincs állandó kulcsos új APK e feladatból. Ezeket az ellenőrzési eredményeket külön frissítjük, ha elérhetők.

Fizikai eszközön (még nem elvégzett): kiválasztott HC/Samsung grant állapota, valódi aznapi pulzus/nyugalmi pulzus/series, time zone-határ, Samsung Energia és SDK hibakód ugyanarról a készülékről, permission visszavonás utáni visszaolvasás. Teljes személyes backup, tokentitok vagy signing key nem szükséges.

**Átadás:** önálló fejlesztői PR, main merge és teljes integráció kizárólag a koordinátoré.

# Health Connect üres napi adatok – statikus audit és készülékes bizonyítási kapu

Dátum: 2026-11-02. Kiinduló `main`: `bedcaa0aa7b8dfd973d10f7a1a457325ee7611df`.
Kapcsolódó: #19 (készülékes visszajelzés), #31 (elkészült HC olvasásdiagnózis), #35 (külön UI-feladat).
A `docs/HEALTH_CONNECT_EMPTY_READ_DIAGNOSIS_2026-11-02.md` reprodukciós
tervét és a jelenlegi alkalmazáskódot ellenőriztük; a #31 diagnosztikát nem írtuk újra.

## Bizonyítékfokozatok

| Állítás | Tény / státusz |
| --- | --- |
| A korábbi, 2740-es APK Health Connect-kiválasztás mellett nem mutatott napi adatot; Samsungra váltva és szinkronizálva megjelent adat | Készülékes felhasználói megfigyelés (#19), nem okbizonyítás |
| Samsung Health a problémás napon Health Connect-rekordot írt | **Nem igazolt** |
| A TrainPilot tényleges HC read-grantjai és az Android HC elérhetősége a problémás olvasáskor megfelelő volt | **Nem igazolt** |
| A kiválasztott `hcDailySource` originnek volt az adott helyi napon lépésrekordja | **Nem igazolt** |
| A `HealthProviderProjection.day` Samsung-ágban HC/legacy metrikákból és alvásból pótolt hiányt | **Kódból bizonyított megjelenítési hiba, javítva ezen az ágon** |
| Ez a megjelenítési hiba okozta az üres HC SDK-olvasást | **Nem igazolt; a HC-olvasót a javítás nem módosítja** |
| Fizikai készülékes teszt ezen a PR-en | **Nem történt** |

## Kódút ellenőrzése

- `NativeHealthRefresh.dayWindow` a helyi naptári nap kezdete és a következő éjfél között olvas; a mai napnál `now` zárja a tartományt. `readDay` a felhasználó által kiválasztott providert használja, Samsungra automatikusan nem vált.
- `HealthConnectRepository.Api34.readHealthDay`: tényleges megadott olvasási engedélyek, rekordok, azok `dataOrigin.packageName` értéke és a `hcDailySource` (`auto` / `priority` / konkrét origin) alapján képez napi adatot. A Samsung saját SDK-jából látott rekord **nem bizonyít** HC-rekordot.
- `HealthConnectReadDiagnosis.inspect` friss, journal-merge előtti HC-olvasásról készül, `readAt` idővel, grantlistával, megfigyelt lépésoriginekkel, warnings/SDK-hiba alapú státusszal. A részleges HC-journal régi mért mezőket megtarthat; **a friss diagnózis nem azonos a megőrzött cache-értékekkel**. A hiány nem nulla.
- A HC és a Samsung napi rekordjai külön csatornán tárolódnak. Viszont a `HealthProviderProjection.day` a közös `journal` és `projection()` nézetben **korábban** a kijelölt Samsung-adatok hiányait HC/legacy metrikákkal és alvással egészítette ki. Ezt a szigorú providerelválasztás érdekében megszüntettük: csak a kiválasztott provider saját értékei jelennek meg; a másik provider története explicit csatornában elérhető. Az explicit engedélyezett BLE-lépésút változatlan.
- A #35 redundáns szövegblokkját és a `HealthDashboardScreen/ViewModel` közös felületi állományait ez a módosítás nem érinti. Integrátornak jelezendő közös adatréteg: `HealthJournalStore.kt`, `HealthProjectionsAndEnergy.kt`.

## Minimális készülékes bizonyítás – *csak anonimizált státusz, nem egészségmentés*

**A vizsgált eszköz és próbák még nincsenek meg:** telefonmodell, Android-verzió,
TrainPilot APK-verzió/build és HC-verzió/elérhetőség: *ismeretlen*.
Helyi dátum, időablak, tényleges rekord- és jogosultságeredmények: *nem rögzített*.
Az előző készülékes megfigyelés APK-ja 2740; a 2741 külön elfogadása szükséges.

1. Ugyanazon a telefonon és helyi napon (a telefon időzónáját is rögzítve)
   nyisd meg a Health Connectet, és ellenőrizd az érintett **konkrét** adattípusok
   (elsőként lépés és aktív energia) tényleges rekordjait és forrásalkalmazását.
   A Samsung Health helyi adatképernyője nem helyettesíti ezt az ellenőrzést.
2. A Health Connect alkalmazásengedélyeiben nézd meg a TrainPilot `READ_STEPS`,
   `READ_ACTIVE_CALORIES_BURNED` és szükség szerint más érintett read-grantját.
   A teljes jogosultságlista és az Android/HC SDK elérhetősége is vizsgálandó;
   nem szabad a jogosultságokat megkerülni.
3. TrainPilot > Egészség > HC: állítsd `auto`-ra a napi forrást, végezz **teljes**
   szinkront, és rögzítsd a kiválasztott helyi napot, az olvasás idejét,
   a napi adat *van/nincs* státuszát, a `hcReadDiagnosis` megjelenített
   magyarázatát és az esetleges hiba/engedélyüzenetet.
4. Próbáld `priority` módban. Konkrét csomagorigineket kizárólag
   a ténylegesen listázott forrásokból válassz; jegyezd fel, hogy a lépések
   és a többi metrika külön hogyan változnak. Ha a HC-ben nem volt rekord
   vagy read-grant, az ok a beállítás/átadás oldalon lehet; **ne** állíts
   SDK-kódhibát pusztán a Samsung UI-ban látott szám alapján.
5. Válts Samsungra, szinkronizálj, majd HC-ra vissza. Ellenőrizd, hogy
   a HC hiányzó adatait nem pótolta a Samsung cache, és a másik provider
   saját nyers története továbbra is megmaradt. Engedély-visszavonásos próba
   csak szándékosan, utólagos visszaadással, adat- és cache-törlés nélkül.

**Anonimizált átadandó eredmény:** készülékmodell + Android + TrainPilot build;
helyi nap/óra/időzóna; a HC-ben ténylegesen látható rekordok típusa és
alkalmazáseredete (rekordértékek nélkül is elegendő); releváns read-grantok
megadva/hiányoznak; `hcDailySource` értéke; HC diagnóziskód/üzenet;
SDK-támogatott/hiba; Samsung külön szinkronjának eredménye.
Nincs szükség teljes JSON egészségmentésre, személyes mérésekre, fiókadatokra
vagy tokenekre.

## Kód és kapu

A PR csak a közös, napi forrásmegjelenítés providerkeverését javítja;
nincs új HC-olvasási útvonal, fallback, értékösszeadás, mérésbecslés,
adat-/cache-törlés, grantmegkerülés vagy változás az edzés-ownership,
ACK/finalization, backup/restore és kiadási kapuk körül.

Regresszió: `HealthDailyProviderIsolationRegressionTest` (három új eset:
hiányzó Samsung-metrika/alvás, hiányzó teljes Samsung-nap, HC/Samsung
tárolás és explicit HC történet elkülönítése).
A meglévő `HealthConnectReadDiagnosisRegressionTest` hat tesztje megmarad.
A PR-ról automatikus Android Actions teljes teszt szükséges; **a CI, az
alkalmazásbuild és a készülékes ok igazolása külön státusz**, csak tényleges
futás és nyers teszt-XML után tekinthető sikeresnek. Az integrátor végzi
a main-integrációt és az állandó kulcsos APK-kat.

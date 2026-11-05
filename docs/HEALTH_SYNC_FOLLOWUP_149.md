# #149 / #151 – szolgáltatói szinkron és app-életciklus

A felső és alsó kézi frissítő ugyanazt a két szolgáltatós műveletet használja.
Samsung Health és Health Connect sorosan fut, a kijelzési provider átállítása nélkül.
Egyik forrás hibája nem némítja el a másikat. A napló kis gombja a saját providerét
frissíti, majd a natív művelet lezárási revisionje újraolvastatja a naplót.
Csak teljesen sikeres forrásfrissítés lépteti annak sikeres időbélyegét; részleges
eredmény/hiányzó engedély külön állapot. HC háttérsiker is HC-hoz kerül.

Az app-gyökér egyetlen HealthDashboardViewModelt tart. RESUMED app megnyitáskor,
majd előtérben ötpercenként próbálja mindkét forrást frissíteni. Alapérték bekapcsolt,
korábban explicit eltárolt false megmarad. AUTO sosem nyit engedélykérő ablakot.
IO/halott SDK-kapcsolat hiba legfeljebb két gyors újrapróbálást kap 30/120s után;
részleges adat vagy engedélyhiány önmagában nem retry-ok. A napi IO hibák is
továbbadják a retry jelzést, sikeres checkpoint nélkül.

App háttérbe kerülése érvényteleníti az automatikus flightot; az újrabelépés megvárja
a megszakított blokkoló SDK-olvasás végét. Egy közös gate és a natív provider-read
védi az egyetlen olvasást. Navigáció nem állít le app-szintű műveletet, de háttér,
restore/replacement continuation, dataset generation, provider-selection változás,
ownership/finalization/aktív edzés, grant-változás továbbra megszakítja/blokkolja.
Samsung/HC között is megmarad az induláskori batch-azonosság, nem fogad új datasetet.

A meglévő HC JobScheduler periódus minimum15perc marad, csak külön engedéllyel/
bekapcsolással és Android ütemezéssel. Samsung saját rendszer-szinkronjának idejét
az app nem állítja. Napi HC/Samsung/Wear számokat nem adunk össze.

A companion UI PR157 tartalmazza az egyszerűsített naplót és a callback signature-t;
az integrátor előbb ezt illeszti mainre. Új natív regressziók a provider-sorrendet,
részleges/hibás eredményt, cancellation/generation megszakítást, gate/opt-in/cooldown
védelmet és a bounded retryt fedik. Helyi Gradle/CI nincs; közös main-kapu következik.
Készülékes Samsung/HC ellenőrzés és APK-átadás ettől külön lépés.

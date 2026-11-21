# Statikus auditösszefoglaló – 2788 és közös fotógaléria

Dátum: 2026-11-21

Audit alapja: `e73dc514b361cfab2aad0d661ae6ca93973e5e5d` (`main`) és az audit kezdetekor meglévő, nem commitolt munkafa-változások; a javítások a `fix/2788-audit` ágon készülnek.

Módszer: forráskód- és tesztlefedettség-vizsgálat, majd a jóváhagyott javítások és célzott regressziós esetek.

## Vezetői összefoglaló

Az első statikus ellenőrzés fordítási blokkolót, védtelen testsúly-írási/törlési útvonalat, elavuló fotószámlálót és hiányzó önálló „Egyéb” fotó-hozzáadást talált; ezekre javítások és célzott regressziós tesztek készültek. A teljes CI első tesztfordítása után egy új, Progress nézetbeli magasságregresszió is előjött; a galériabelépő most 48 dp-esre tömörült, hogy a viewport-követelmény megmaradjon. Tulajdonosi tisztázás alapján az egészségadat-mentési kapcsoló nem korlátozza a Drive-ra feltöltött fotókat: a fotószinkron a Google Drive-hozzáféréshez kötött.

## Hatókör és teljesség

Az átadó a teljes `app`/`wear`/`shared` alkalmazást kéri; a PR #290–#306 csak kiemelt prioritás. Ez a fájl jelenleg **részjelentés**: a 2788-as változások, a testsúly-/fotónapló és néhány kapcsolódó adat-, egészség-, óra- és CI-útvonal szerepel benne.

Még nincs végigellenőrizve a teljes telefon–óra helyreállítási mátrix, minden egészségforrás- és engedélyállapot, az aktív edzés/GPS teljes háttér- és eszközviselkedése, a teljes UI/lokalizációs/akadálymentességi mátrix, az app-szintű coroutine/állapotversenyek, az exportált komponensek és titokkezelés, valamint a kiadási metaadatok és webes/Play-listing egyezése. A `shared` modulhoz a jelenlegi fán nem találtam tesztfájlokat. Ezekről a jelentés nem állítja, hogy hibamentesek; az audit lezárásához külön ellenőrzés kell.

## Megállapítások

| ID | Súlyosság | Bizonyosság | Megállapítás és bizonyíték | Javasolt javítás és regresszió |
|---|---|---|---|---|
| A1 | Magas – javítva | Igazolt, statikus | `NativeProgressPhotoData.load()` nem `suspend`, mégis meghívja a `suspend BodyWeightDao.getAllWeights()` függvényt. | A betöltés korutin-kompatibilis; célzott fordítás szükséges a megerősítéshez. |
| A2 | Nem hiba – tisztázva | Felhasználói elvárás megerősítve | Az edzésfotók már korábban is a Drive-szinkron részeként mentek fel, ha a Google Drive-hozzáférés engedélyezett volt. Az egészségadat-mentési kapcsoló a mért egészségadatokat és összefoglalókat szabályozza, nem a Drive-fotókat. | A korábbi fotóviselkedés megmarad; az edzés-, súlynapló- és „Egyéb” fotók a hitelesített Drive-szinkron részei. |
| A3 | Magas – javítva | Igazolt | Az Egészség oldali testsúlytörlés közvetlen DAO-hívásokat használt, tombstone és `NativeDataReplacement`-védelem nélkül. | A törlés most a pontos rekordot, tranzakcióban és tombstone-nal távolítja el. Regresszió: az azonos napi másik mérés megmarad. |
| A4 | Magas – javítva | Igazolt | Az Egészség oldali testsúlymentés/szerkesztés közvetlen `upsertWeight()`-et hívott mutation lease nélkül. | Az add/edit a védett repository-t használja, a dátum- és ismeretlen portable mezőket megőrzi; hiányzó adatbázis esetén nincs siker-visszajelzés. |
| A5 | Közepes – javítva | Igazolt feltétel, feltételes előfordulás | A törlés az ID szerinti törlés után dátum alapján is törölt, így dátumformátumú ID több azonos napi mérést érinthetett. | A dátum szerinti törlés kikerült; regresszió ellenőrzi az azonos napi külön rekord megmaradását. |
| A6 | Közepes – javítva | Igazolt | Fotócsatolás után a Health Compose sor frissült, de a közös ViewModel fotószámlálója elavult maradhatott. | A tartós csatolás után frissül a súlynapló- és privát fotószám-projekció. |
| A7 | Közepes – javítva | Igazolt követelményrés | A globális galériában nem lehetett edzéstől és testsúlyméréstől független fotót hozzáadni. | Az „Egyéb fotó” hozzáadás kamerával/galériából elérhető; a metaadat backupba kerül, a Drive-fotószinkron kezeli. |
| A8 | Közepes – javítva | CI-vel igazolt | A Progress oldal új, két soros galériakártyája 24 dp-rel a 620 dp-es ellenőrzött viewport alá tolta a Fejlődési javaslat kártyát. | A felső galériabelépő 48 dp-es, egysoros változat; a meglévő `UiUx219RegressionTest` ezt az elrendezési követelményt védi. |

## Ellenőrzött területek – külön hibát nem találtam

- **Room-séma:** a Room entitások már az első séma-verzióban szerepeltek; a jelenlegi `version = 1` önmagában nem mutat új entitás miatti migrációs hibát.
- **2788 heti lépések:** az adatbetöltés dátumonként egy bejegyzést állít elő; a kompakt és részletes összesítés a mért napokat használja, hiányzó adatot nem alakít nullává.
- **2788 kalória:** a teljes és aktív energia külön mező marad; a kijelzés nem adja össze őket, és a hiányzó értéket nem becsüli.
- **Telefon–óra ownership:** az átnézett 193-as átadási protokoll eszközazonosítót, epochot, átadási állapotot és egyező workoutot ellenőriz; az elavult üzenetek elutasítottak. Fizikai eszközös átadás nem volt tesztelve.
- **GPS:** az útvonal privát fájlban tárolódik, korlátozott méretű és pontszámú, a bemenet ellenőrzött, a törlés explicit felhasználói művelethez kötött.
- **Egészségforrás:** a vizsgált vetítés forrást választ, nem összegez átfedő Samsung/Health Connect értékeket.
- **CI-kapu:** a repository workflow app, Wear és shared unit teszteket futtat, majd nyers JUnit XML-t ellenőriz.

## Ellenőrzés és korlátok

- `git diff --check`: sikeres.
- A JDK 17, Gradle 9.3.1, Android Platform 36 és Build Tools 36.0.0 hordozható telepítése a `H:\GitHub Copilot\.tools` alatt elkészült.
- A helyi Gradle-fordítás még a forráskód előtt, függőséglekérésnél állt meg: a JVM-en keresztüli Foojay/Android Gradle Plugin letöltés nem sikerült, bár ugyanazon végpontokat a PowerShell eléri. A módosított források fordításig és a regressziós tesztek futásáig még nem jutottak.
- GitHub Actions `44846049059`: az R8 minifikáció sikerült, a unit tesztforrások fordítása két új tesztírási hibán állt meg; ezek javítva.
- GitHub Actions `44846732312`: alkalmazás/Wear fordítás sikeres; nyers XML összesítése 972 teszt, 1 failure, 0 error, 0 skipped. Az egyetlen failure a Progress viewport-regresszió volt (24 dp túlcsordulás), amelyet a legutóbbi, még nem CI-zöldített módosítás javít. Az R8 minifikáció sikerült.
- A végleges, legutóbbi forrás SHA-hoz még szükséges egy teljes GitHub Actions-futás zöld eredménye és annak nyers XML-számai.
- Kamera/galéria OEM-viselkedés, Drive-hálózati payload tényleges futása, fizikai telefon–óra átadás és teljes készülékes vizuális elfogadás nem történt.
- A munkafa auditkor módosított állapotban volt; a fenti SHA a commit-alap, nem a nem commitolt változások teljes azonosítója.
- Repo verzióadata: telefon `versionCode 278800`, `versionName 1.4.1`.

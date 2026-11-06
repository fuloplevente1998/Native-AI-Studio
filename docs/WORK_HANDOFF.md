# Rövid munkátadás – 2026-11-05

Belépési pont: [AGENTS.md](../AGENTS.md). A részletes múlt a
[CURRENT_STATUS.md](CURRENT_STATUS.md)-ben található; az élő GitHub-állapot elsőbbséget élvez.


## Folyamatban – #177 programmentés és témától független akciógombok

Root egyedül, design/program-save-actions ág. Új globális Mentés a programszerkesztő
alján, a Törlés bal oldalán; név és még nem külön mentett gyakorlatmezők a meglévő
NativeProgramEditor tranzakciójával íródnak. Nap/exercise ID célzás, ismeretlen
mezők megőrzése, aktív session/restore/ticket védelem és korábbi írások bevárása
megmarad. Siker csak tartós írás + Room-újraolvasás után. A napváltásokhoz tartozó
lokális mezők stabil kulcsot kapnak; gyors stepper-kattintás nem veszít változást.
Szerkesztőből egyedi gyakorlat gomb eltávolítva; könyvtárban a Kedvencek mellé,
48dp-s hangsúlyos gombként került. Közös fix zöld/piros hátteres+keretes akció-
komponensek telefonon (Studio és native), mentés/törlés/elvetés/visszavonás;
óra mentés/visszavonás/lezárás vezérlői is fix zöld/pirosak. Futás közbeni
felirat-felismerés nincs; callback, engedély és megerősítés változatlan.
Két új regresszió: nyitott UI-mezők globális mentése és tényleges Room-írás/
ismeretlen mező/másik nap/hibás adat elutasítása. Meglévő gombteszt save/delete
függetlenséggel kiegészítve. Diff-check tiszta, közös signed kapu még következik.

Az első közös #177 signed42143215569 fordításkor megállt: a könyvtári új FlowRow
hiányzó ExperimentalLayoutApi opt-in jelölése miatt. Célzott annotációpótlás;
teszt és műveleti kód nem változott. Ebből a futásból nincs átadott APK.

## #175 ikonos gyakorlat-visszajelzés – ellenőrzött forrás, új csomagra vár

PR176 mainen, signed42131928587 SUCCESS, forrás9784d91b62251c4feb5df474e4cc2226ff15bbda.
Nyers XML + artifact digest + verification commit függetlenül visszaellenőrizve:
566 teszt, 0 failure/error/skipped (385 telefon +181 Wear). A futás közben érkező
#177 új kérés miatt külön köztes APK-t nem adtunk át; a következő teljes közös
csomag tartalmazza mindkét feladatot. Verzió/signer változatlan.

## #175 ikonos gyakorlat-visszajelzés és összesítő – implementáció

Root egyedül, design/exercise-feedback-chips ág. A képen szereplő öt nehézségi
válasz + külön Fájdalom közvetlen, kompakt, tördelődő ikonos gombként jelenik meg
az aktív gyakorlat alatt és a Következő előtti kérdésben. Minden Fájdalom-gomb
alapból finom piros keretet kap, kiválasztva erősebb kiemelést; a fájdalmas
összesítő-kártyák is finom piros keretet kapnak. Megjegyzés opcionálisan nyitható.
A már megválaszolt gyakorlat Következő gombja nem kérdez újra. Összesítőn a meglévő
válasz látszik Módosítás/Pótlás gombbal; összbenyomás külön, alapból összecsukva.
Mentési/Coach logika változatlan: workout feedback nem írja felül az explicit
exercise effortot; a Coach hiányzó saját válasznál fallbackként használhatja az
összválaszt, a globális fájdalom óvatossági jelzés. Négy új regresszió és a meglévő
felugró teszt közvetlen gombos UX-re igazítása. Diff-check tiszta, teljes signed
Actions-kapu és fizikai/vizuális elfogadás még hátravan. Verzió/signer változatlan.

## Korábbi átadás – #171 aktív edzés, 2753 / 7a90d39

PR172 és PR173 mainen, root egyedül, új agent nélkül. A gyakorlatcsík korábbi
indexalapú pipája kizárólag vizuális volt; most csak nem üres, teljesen kész
sorozatlista jelöl kész gyakorlatot. Közös kompakt nap/idő sor a kanonikus dayName
és activeIntervals alapján. Explicit szünet alatt megáll, újranyitás nem nullázza
és nem indít új órát. A képernyő-kikapcsolás háttérmérési szerződése megmarad.
Coach-store a képernyővel él, lazy-scroll nem törli; azonos cél háttérfrissítése
közben megmarad a tanács. Visszajelzés strength/timed/bilateral sorozatok után;
Következő előtt opcionális app-stílusú kérdés. Kihagyás nem gyárt választ;
session/generation/gyakorlat/képernyő-váltás bezárja a korábbi kérdést.
A meglévő stabil ID-s visszajelzés-mentés és összegzés megmarad.

Az első signed run42126841059 egy új navigációs teszten bukott (561/562 sikeres).
Valódi ok: a snapshot már frissült, de a még aktív katalógus/presentation job guardja
eldobta a gyors visszaváltást. PR173 a sorban lévő gyakorlatot közvetlen, pontos
session/generation/exercise/set ID-s natív selecttel választja, a meglévő snapshot
observerrel. A könyvtári csere saját védett útja megmarad. Nincs timeout-emelés,
tesztgyengítés vagy kihagyás; ugyanaz a regresszió két gyors kattintást is tesztel.

Végső signed forrás **7a90d3909bfdb7d7a70ed2faeea5d05154d0e84e**,
run **42128194228 SUCCESS**: **562 =381 telefon +181 Wear**,
0 failure/error/skipped, shared NO-SOURCE. Hét új regresszió és kibővített Coach
UI-teszt. Letöltött ZIP digest, nyers XML/source/count, tényleges APK-hash/bináris
manifest/APK v2 tanúsítvány és CI tartós signer/páros/capability kapu ellenőrizve.
Változatlan2753 / 0.1.0-beta / com.TrainPilot.com, korábbi tartós signer.

- [Telefon release](https://drive.google.com/file/d/1ub26ZT5kzlFkkz3Lq7SCM7XOkjJCXhvP/view?usp=drivesdk)
- [Wear optimalizált release](https://drive.google.com/file/d/10m3o3mqPpf-DuR2QUkCpWkAIraCOAobl/view?usp=drivesdk)
- TrainPilot Native mappa, korábbi APK-k megőrizve; név/MIME/bájt/parent visszaolvasva.
  [Gépi bizonyíték](RELEASE_2753_7A90D39_VERIFIED_ARTIFACTS.json).
- Fizikai/vizuális próba még hátravan, #171 nyitva marad: mini-sáv gyors oda-vissza,
  részleges/teljes pipák, Alap A/B nap, explicit szünet és újranyitás, Coach görgetés,
  visszajelzés kiválasztás/kihagyás és órás váltás közbeni modal-bezárás.
- Nincs helyi Gradle/emulátor; a későbbi dokumentációs commit nem módosít appforrást.

## Korábbi átadás – telefonos elvetés lezárja az órás sessiont, 2753 / d2766f7

#168 / PR169 mainen: a közös natív discard finalizer a tartós törlés után publikálja
a lezárt snapshotot és frissíti az órás kezdőlapot. A Studio és a natív képernyő
ugyanazt az útvonalat használja. Peer-visszakapcsoláskor újraküldhető a megőrzött
lezárás. Az óra azonos workout ID / owner epoch, nem régebbi revision és kezdés
utáni időbélyeg mellett fogadja el a lezárást helyi sequence-változás esetén is.
Másik session, régi epoch/revision/időbélyeg nem törölhet aktív edzést.
A munka kizárólag root által, új agent nélkül készült.

Signed forrás **d2766f715b1f98276a0460eedba75f43266b547a**, run **42114786836 SUCCESS**.
Egy közös signed build és teljes JVM-kapu: **555 =374 telefon +181 Wear**,
0 failure/error/skipped, shared NO-SOURCE. Három új regresszió: commit utáni
publikáció szállítási hibával; helyi sequence melletti lezárás; idegen/elavult
lezárás elutasítása. Tesztgyengítés és külön ismételt teljes CI nélkül.
Letöltött ZIP-digest, nyers XML/source/count, tényleges APK-hash/bináris manifest/
APK v2 tanúsítvány és a CI persistent-signer/páros kapuja ellenőrizve.
Változatlan2753 / 0.1.0-beta / com.TrainPilot.com, korábbi tartós signer.

- [Telefon release](https://drive.google.com/file/d/1v7ZPkmJjq4ogZf2pQUJrSTQufe3H_v7c/view?usp=drivesdk)
- [Wear optimalizált release](https://drive.google.com/file/d/1CKvcr7q74Z772HhjDXfXP2COjQVxmsZW/view?usp=drivesdk)
- TrainPilot Native mappa, régi fájlok megőrizve; név/MIME/bájt/parent visszaolvasva.
  [Gépi bizonyíték](RELEASE_2753_D2766F7_VERIFIED_ARTIFACTS.json).
- Mindkét alkalmazás frissítendő. Fizikai próba: telefonos aktív Elvetés után az óra
  zárjon, kapcsolatkimaradás/visszatérés után se maradjon futó session.
- #168 készülékes elfogadásig nyitva. A mentett naplóedzés törlését ez a javítás
  nem célozza; a felhasználónak feltett pontosító kérdésre még nincs válasz.
- Ez a későbbi dokumentációs commit nem változtat az ellenőrzött appforráson.

## Korábbi átadás – órás indítás és előtöltés, 2753 / 184ee46

#165 / PR166 mainen: a Studio az új, órán indított sessiont élőben átveszi a
meglévő snapshot-projekcióba, navigáció és telefonos motorindítás nélkül. Az órás
aktuális listát és stabil ID-s gyakorlatválasztást követi; másik aktív UI-sessiont
és hibás/pending/finalizing új sessiont nem ír felül. Telefonos előtöltés már volt;
az órára küldött program/quick payload most ugyanazt a natív per-set history
súly/ismétlés előtöltést használja. Mért idő/teljesítés/RIR nem öröklődik.
Két regresszió az óratulajdonos live-start/selection állapotát és a watch-payload
history-előtöltést védi. A munkát kizárólag root végezte, új agent nélkül.

Forrás184ee4613efeae9d8f9d5b40138d9a7b848a7fbf. Main-CI42107477482 és
signed run42108228191 SUCCESS: 552 =373telefon +179Wear, 0failure/error/skipped,
shared NO-SOURCE. Nyers XML, source, ZIP digest, letöltött APK-hash, bináris manifest,
APK v2 tanúsítvány és CI apksigner/persistent pin ellenőrizve. Változatlan2753,
0.1.0-beta és com.TrainPilot.com; release phone +R8/shrunk release Wear ugyanabból a jobból.

- [Telefon release](https://drive.google.com/file/d/1BAxrBRFj0Ws-LdjF1b3nGEYUD5nzgL71/view?usp=drivesdk)
- [Wear optimalizált release](https://drive.google.com/file/d/1IqSqhWmpPwfuMtmmaLzNSACbCEtZSgY-/view?usp=drivesdk)
- TrainPilot Native mappa; név/MIME/bájt/parent metadata readback rendben;
  korábbi APK-k megőrizve. [Gépi bizonyíték](RELEASE_2753_184EE46_VERIFIED_ARTIFACTS.json).
- Fizikai telefon–óra/előttöltés próba a felhasználónál még hátravan, #165 nyitva marad.


## Legújabb átadott APK-pár – 2753 / 93733ec

Az AI Studio mainre pusholt singleton-/teszt-életciklus javítása bekerült a valódi
aláírt buildbe. Forrás **93733ec43ca723cb9fd33d488afdac562800bbcc**.
Signed run **42100315171 SUCCESS**: 550 = 371 telefon +179 Wear,
0 failure/error/skipped, shared NO-SOURCE. Mindkét letöltött ZIP API-digestje,
nyers XML/count/source, APK-hash, bináris manifest és APK v2 tanúsítvány-fingerprint
függetlenül visszaellenőrizve; a job apksigner/pinned-signer kapuja is sikeres.
Package com.TrainPilot.com; változatlan2753/0.1.0-beta; régi tartós aláíró.
Telefon release és Wear R8/resource-shrunk release ugyanabból a jobból.

- [Telefon release APK](https://drive.google.com/file/d/1J-hKuoVRT7jympxbuZFgVWIBjUedr8eQ/view?usp=drivesdk): 20 515 832 bájt.
- [Wear optimalizált release APK](https://drive.google.com/file/d/10VuBwoIEyGOR-bazIZ3ZWakqb04-9wmZ/view?usp=drivesdk): 4 636 863 bájt.
- Mindkettő a TrainPilot Native mappában, név/MIME/size/parent metaadat-visszaolvasással
  ellenőrizve. Régi fájlok megőrizve. [Gépi bizonyíték](RELEASE_2753_93733EC_VERIFIED_ARTIFACTS.json).
- A korábbi81f2999 signed run42061730570 három UI-idling teszthibával bukott;
  abból nem adtunk át telepítőt. A93733ec új valódi signed kapuja teljesen zöld.
- Készülékes Samsung/HC/Wear/kalória/vizuális elfogadás továbbra külön ellenőrzés;
  a jegyek pusztán CI-re nem zárhatók. Nem fut további agent vagy build.

## Végleges új készülékes csomag – main, teljes CI igazolva

- #149–#153 javításai: PR159 csomag, PR160 Robolectric-környezet pótlás,
  PR161 kanonikus Wear gyakorlatválasztás/projekció; mind mainre került.
- Végső alkalmazásforrás main **cc8e1c4d3384d1f562dff0d7dd8817840ef69049**.
  Run **42059924773 SUCCESS: 550 = 371 telefon + 179 Wear**, 0 failure/error/skipped;
  shared NO-SOURCE. Tesztelt commit cde018ad8f3d860b6fb9210875885295c73fb791,
  tesztelt és main Git-tree pontosan azonos: f436936a3673ef485064e57833428d208e634015.
- A végső nyers XML-verifier kimenetét a kész job logjából olvastuk vissza.
  A végső ZIP külön bájtellenőrzése a megszakadt környezet miatt nem történt meg;
  ezt a gépi bizonyíték kifejezetten jelzi, nem állít független hash-ellenőrzést.
  Az előző main-run547 nyers XML/ZIP/source/digest ellenőrzése megtörtént.
- Az első közös run hat új tesztje Android JSONObject stub miatt bukott;
  kizárólag három hiányzó Robolectric runner pótlása kellett, elvárás nem gyengült.
- Képernyő-kikapcsolás többé nem automatikus workout pause; explicit pause/back
  megmarad. Mindkét Folytatás az aktuális ownerhez intézett szigorú kérést használ.
  Órás gyakorlatválasztás együtt frissíti a Studio méréscélpontját és sorozatait,
  más oldalon végzett böngészés/navigáció megőrzésével.
- [Gépi bizonyíték](DEVICE_FEEDBACK_CI_VERIFICATION_2026-11-05.json).
  Új APK nem készült, fizikai Samsung/HC/Wear/kalória és vizuális elfogadás még nincs;
  #149–#153 ezért nyitva marad. Utolsó átadott telepítők továbbra2753/79c2736.
- A munkakörnyezet az utolsó kódpush után unavailable lett. A végső CI és merge
  GitHub-csatlakozóval befejezve, workflow-változtatás nélkül. Nincs futó agent.

## Új készülékes csomag – közös integráció

A #149–#153 visszajelzések kódja együtt: PR154 pihenő, PR155 naptár,
PR156 Wear session/folytatás, PR157 napló UI, PR158 univerzális healthsync.
`fix/device-feedback-integration` megőrzi az összes eredeti ág commitját.
A közös VM-hook bekötve: a telefonos Folytatás watch-owner esetén az órán kér
explicit folytatást, optimista tulajdonosváltás nélkül. Az app onStop többé
nem publikál automatikus workout pause-t pusztán képernyő-kikapcsolásra;
a már futó natív stopwatch checkpointolódik, indítás/folytatás nem történik.
Kifejezett Szünet/Vissza/Elhagyás változatlan. Új regresszió védi ezt.

Az egészség-napló és app-életciklus kész bekötése: [#149 részletek](HEALTH_SYNC_FOLLOWUP_149.md).
Az eredeti WIP-státusz a [korábbi pillanatképben](DEVICE_FEEDBACK_FOLLOWUP_2026-11-05.md)
már történeti; az integráció után nincs hátramaradt UI/VM-hook.
A következő kapu egyetlen teljes main-CI, eredménye külön rögzítendő.
Új APK és fizikai Samsung/HC/Wear elfogadás még nincs; a jegyeket nem zárjuk pusztán CI-re.

**Új visszajelzések a kiadott APK után:** [folyamatban lévő javítások és pontos
korlátok](DEVICE_FEEDBACK_FOLLOWUP_2026-11-05.md). A korábbi13as csomag elkészült;
az új health/Wear/pihenő/naptár csomag még nem került telepítőbe.

## Aktuális main

**Mind a 13 visszajelzéshez tartozó javítás mainre került.** A felhasználó kifejezett
kérésére az elkészült ágakat csoportosan integráljuk, utána egy közös teljes main-CI
fut, külön agentenkénti kapuk helyett. A workflow és cache védelmei változatlanok.

- Első csomag: #133–#141, majd #142 csak hibás foreground-ARM tesztfixture javítása.
  Bevizsgált teljes forrás `fb1f7c7bf42514dd700dd8ac5ea47ad26b2219d4`;
  run **41977716578 SUCCESS, 495 = 319 telefon + 176 Wear**, 0 failure/error/skipped,
  shared NO-SOURCE. Nyers XML/source/hash ellenőrizve.
  [Gépi bizonyíték](BATCH_MAIN_CI_2026-11-05.json). #143 csak dokumentáció.
- Második csomag: #144/#145, majd #146 a napi összegző blokkhatárának fordítási
  javítása. **Végső bevizsgált forrás: `5b3285c1eab6af0e9d8383f948ff1d61fcf4f66b`.**
  **Run41979889219 SUCCESS: 523 = 347 telefon + 176 Wear, 0 failure/error/skipped**,
  shared NO-SOURCE. Nyers XML, source és ZIP digest visszaellenőrizve.
  [Végső gépi bizonyíték](FINAL_MAIN_CI_2026-11-05.json). Nincs külön agentenkénti CI.
  Az előző run41979378818 fordításkor megállt; sikeres tesztet arra nem állítunk.

| Jegy | Mainre került javítás |
| --- | --- |
| #117 | #138: elavult Google-naptárhivatkozás ellenőrzése, korlátozott stabil-ID-s eseményhelyreállítás, biztonságos hibatípus |
| #118 | #134: Témák fejléce és négy kategóriája közös keretben |
| #120 | #141: szolgáltatók a Forrás fülön, hiteles pulzus/energia összesítő a Statisztikákon |
| #84 | #139: kompakt, palettás napló-/visszajelzés-szerkesztő és adaptív mezők |
| #122 | #144: egy napló, naponta lenyitható 26 metrika és inline rekord/alvásfázis |
| #123 | #144: app-stílusú dátumpicker, megőrzött validáció és Mégse |
| #125 | #133: Wear óraikon az átadásnál |
| #126 | #140: felismerhető Előző/Vissza és veszélyszínű Elvetés, változatlan guardok |
| #70 | #145: mindig olvasható kanonikus Coach-tanács, quick/csere és legacy history támogatás |
| #128/#129/#130 | #137: kanonikus gyorsindítás/csere, explicit stopper, megőrzött mérések, elvetés utáni projekciótisztítás |
| #131 | #135: állandó Tervezés, adott napi lebegő ablak megőrizve |

A #136 rögzíti az agentmunkarendet és a GPT-6.1 Sol low/medium/high/xhigh szabályt.
Az első agentkör limit miatt leállt; WIP-kódja megőrizve, friss mainre igazítva
két új agenttel befejezve. **A Coach/egészségnapló már nem félkész WIP.**

## Fontos megőrzött szerződések és korlátok

- Egyetlen natív motor, stabil workout/set ID, owner/epoch/ACK, explicit resume,
  finalization/discard/restore/tombstone védelmek. Nincs adatreset, új képlet vagy séma.
- Kézi natív mérés rögzíthető automatikus tervvezérlés nélkül; a foreground ARM,
  tulajdonos és célpont ellenőrzése megmarad. Kézi +/- másodperc nem mért work-idő.
- Health: hiány nem nulla, aktív/teljes kcal nem adható össze, átfedő aggregátum
  nem osztható fel becsléssel. Igazolt diszjunkt ablak és valódi mintaszám használható.
  [Edzés-health szerződés](WORKOUT_HEALTH_SUMMARY_2026-11-05.md).
- Egészségnapló: provider/origin/helyi nap szerinti olvasás, 7 nap / 25 rekord /
  20 minta lapozás; nincs új napi összeg a rekordokból. Origin-napi összesítő hiányában
  a metrika ismeretlen, a tényleges origin-rekordok elérhetők. A másik native Dashboard
  közös rekordadaptere megmarad, annak pickere is app-stílusú.
  [Részletes átadó](HEALTH_JOURNAL_122_123_IMPLEMENTATION_2026-11-05.md).
- Coach: a régi explicit-mode kapu a megjelenítést némította. Új read-only VM a meglévő
  kanonikus repositoryt használja; az explicit súlyjóváhagyás és fájdalom/RIR/ownership
  őrök változatlanok. Tick nem indít új számolást, azonos elemszámú history-edit igen.
  Nincs automatikus súlymódosítás. A konkrét 30 felhasználói edzés nem volt elérhető.
- A screenshot eredeti Google 404-es kérésének pontos oka nem bizonyított.
  A kódban kezelt hibautakhoz tesztek vannak, valós fiókos próba szükséges.

## Kiadás és következő ellenőrzés

**A javított APK-pár átadva a Drive-on:** signed forrás
`79c2736dac267d4cb0c7dc630a088c20159af7f2`, run41981126439 SUCCESS.
Telefon release 20 483 064 bájt, R8/shrunk Wear release 4 636 863 bájt.
Nyers 523 teszt, 0 failure/error/skipped, actual APK hash/v2 signer, source és
CI package/verzió/Maps/páros/capability ellenőrizve, Drive-metaadat visszaolvasva.
[Kiadási bizonyíték és letöltések](RELEASE_2753_79C2736_VERIFIED_ARTIFACTS.json).
Verziókód **2753**, versionName `0.1.0-beta` és signer változatlan; az új fájlnévben
`79c2736` szerepel. A korábbi `99f2adf` forrású 2753 másik, régebbi APK-pár.

A készülékes elfogadás továbbra is külön feladat, a jegyeket nem zártuk le pusztán
CI alapján. [Közös telefonos ellenőrzőlista](TEST_MAIN_FOLLOWUP_2026-11-05.md).
A következő lépés a kiadott APK-k készülékes visszatesztje;
a forrás, a signed build és a teljes közös CI már ellenőrzött. A bizonyítékot rögzítő későbbi
dokumentációs commit nem változtatja az alkalmazás/teszt/workflow forrását.
APK-kéréskor csak a meglévő signed workflow: telefon release + R8/shrunk Wear release.

#152 / draft PR156 további javítás: pontos owner/epoch/pauseRevision célzású
telefon→óra explicit resume transport; késői watch snapshot epoch/pause/sequence
befogadási guard; már mentett history és alias újraaktiválásának tiltása.
Öt új regresszió, kibővített Room-alias védelem; statikus ellenőrzés tiszta,
JVM/CI még nem futott. A közös VM resume-hookot a root integrátor köti be;
állapot és fizikai mérési korlátok a készülékes followup dokumentumban.

## #151 UI-folytatás – integrációra vár

Ág: `design/health-provider-journal` (draft PR átadáskor). A naplóban csak a felső
Samsung Health / Health Connect választó marad; origin csak rekord-metaadat.
Kis szolgáltatófrissítő natív callbackkel, busy/hiba állapot és revision alapú
újraolvasás; lenyitott napokon csak AVAILABLE metrikák, a valós nulla megmarad.
A felső és alsó kézi szinkron ugyanazt a callbacket használja. Footer mindkét
provider sikeres idejét és az engedélyezett automatikus ütemezést jelzi; a meglévő
Wear-kártya címe „TrainPilot Wear OS – edzésmérések”.

A módosított Compose-regressziók a provider-projekciót, összes eredet rekordjait,
hiány/tiltás/hiba elrejtését, nullát, natív revision-frissítést, busy/hiba állapotot
és a két szinkrongomb callbackjét fedik. `git diff --check` rendben; helyi
Gradle/CI nem futott. Az ág a párhuzamos #149 natív UiState-mezőket és a root
`onReadHealthSource` bekötést igényli, ezért teljes CI csak a közös integráción.
Az origin-selector és hiányzó metrikasor régi tesztelvárásait a #151 kifejezett
új UX-e miatt cseréltük le; dátum/lapozás/generation védelmek megmaradtak.

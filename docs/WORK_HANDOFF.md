# Rövid munkátadás – 2026-11-05

Belépési pont: [AGENTS.md](../AGENTS.md). A részletes múlt a
[CURRENT_STATUS.md](CURRENT_STATUS.md)-ben található; az élő GitHub-állapot elsőbbséget élvez.

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

**Új APK nem készült, verzió és signer nem változott.** Legutóbb átadott telepítő:
2753, signed forrás `99f2adf`; ez nem tartalmazza az új main-javításokat.
[Kiadási bizonyíték](RELEASE_2753_PHOTOS_VERIFIED_ARTIFACTS.json).

A készülékes elfogadás továbbra is külön feladat, a jegyeket nem zártuk le pusztán
CI alapján. [Közös telefonos ellenőrzőlista](TEST_MAIN_FOLLOWUP_2026-11-05.md).
APK-kérés esetén a következő lépés a signed build és a készülékes visszateszt;
a forrás és a teljes közös CI már ellenőrzött. A bizonyítékot rögzítő későbbi
dokumentációs commit nem változtatja az alkalmazás/teszt/workflow forrását.
APK-kéréskor csak a meglévő signed workflow: telefon release + R8/shrunk Wear release.

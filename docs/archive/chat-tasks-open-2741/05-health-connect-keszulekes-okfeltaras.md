# Health Connect – Az üres napi adat készülékes okfeltárása

## Feladat

A készülékes megfigyelés: Health Connect kiválasztva nem volt napi adat; Samsung Healthre váltva és szinkronizálva azonnal lett. **A kiváltó ok még nem bizonyított.** Ezt vizsgáld ki, ne tételezd fel, hogy Samsung biztosan írt HC-adatot.

A 6. feladat/#31 diagnosztikai kódja már mainen és a 2741-ben van. Ne implementáld újra: `HealthConnectReadDiagnosis`, a friss olvasás `hcReadDiagnosis` metaadata és az Egészség oldali diagnózis kész. Olvasd el: `docs/HEALTH_CONNECT_EMPTY_READ_DIAGNOSIS_2026-11-02.md`.

## Vizsgálati sorrend és elfogadás

- Auditáld a már meglévő reprodukciós tervet és kódutat. Különítsd el a megfigyelést, hipotézist és bizonyított okot.
- A problémás helyi napon ellenőrizendő a tényleges HC-rekord, annak eredete, az érintett konkrét read-grantok, a HC SDK elérhetősége/hibája, dátum/időzóna és a `hcDailySource` forrásszűrő. A Samsung saját SDK-n látott adat nem bizonyítja a HC-be írt rekordot.
- A friss HC-olvasás eredményét külön kezeld a korábbról megőrzött cache-mezőktől; üres/missing érték nem mérési nulla. HC és Samsung eredmény/cache maradjon külön.
- Tényleges készülékes bizonyíték hiányában a statikus vizsgálatot és diagnosztikai lépéseket végezd el; a szükséges minimális környezeti információt ezután kérd. Ne kérj teljes személyes egészségmentést vagy titkokat, és ne állíts készüléken igazolt okot fixture alapján.
- Ha bizonyítható apphiba van, célzott javítás és regresszió szükséges. Ha adatmegosztási/forrás-/engedélybeállítás okozza, dokumentáld a megfigyelt okot és a megfelelő felhasználói lépéseket; ne kerülgesd a grantokat.

Nincs csendes HC→Samsung vagy Samsung→HC fallback, értékösszeadás, kitalált mérés, adat-/cache-törlés hibakerülésként vagy ownership/finalization/adatcsere-kapu kikapcsolása. A működő Samsung adatút maradjon meg. A #35 UI-javítása mellett a lényegi HC-diagnózis továbbra is elérhető legyen; az esetleges közös `HealthDashboardScreen/ViewModel` módosítást jelezd az integrátornak.

Átadásban szerepeljen a ténylegesen vizsgált készülék/appverzió, helyi dátum/ablak, anonimizált diagnosztikai eredmény, bizonyított ok vagy fennmaradó bizonytalanság. Kódmódosításnál releváns Actions/raw XML; készülékes eredményt kizárólag valódi próbáról állíts. Külön képi referencia önmagában nem szükséges, a HC rekord/engedély/forráseredmény a lényeges bizonyíték.

## Közös háttér és munkaszabályok

Repo: https://github.com/fuloplevente1998/Native-AI-Studio

Az 1–6 fejlesztés kész, #28 mainen. Igazolt appforrás `2d7171b9cce7762553e053ab98b9828d3e7f2fc5`, kiadás **2741 / 0.1.0-beta**. PR CI `41529082323` és main signed `41530039869`: mindkettő letöltött XML szerint 315/315 teszt, nulla hiba/kihagyás. Ez nem fizikai elfogadás. Ne implementáld újra az elkészült 1–6-ot.

A friss `main`-ből saját ág/worktree, PR célja `main`; a régi #28 integrációs ág már lezárt. Olvasd el: `docs/AI_STUDIO_HANDOFF_2026-11-02.md`, `docs/AI_STUDIO_CONTINUE_PROMPT.md`, `.specify/memory/constitution.md`, `docs/FRAME_DESIGN_SYSTEM.md` és az érintett hibajegy. Az aktuális main/hibajegy elsőbbséget élvez a fájlban megnevezett régi kódnyomokkal szemben.

Végezd el a kiosztott feladatot, magyarul kommunikálj. Alügynököt ne indíts. Csak a feladat területét módosítsd; a közös fájlokat jelezd az integrátornak. Natív Compose/Room/DataStore/motor marad. Ownership/ACK, finalization, tombstone, backup/restore és providerelválasztás nem gyengülhet. Nincs kitalált mérés, csendes providerfallback vagy adateltüntetés hibakerülés céljából. Verziót, alkalmazásazonosítót, signert, titkokat és kiadási workflow-t ne módosíts önállóan. Android-build/teszt a meglévő Actionsből, nem helyi Gradle-ből.

Átadás: reviewzható PR, végleges működés leírása, forrás-SHA, az aktuális SHA tényleges Actions-eredménye és releváns nyers XML. A kész kódot, a CI-t és a készülékes elfogadást külön jelöld; a folyamatban levő futást ne állítsd sikeresnek. A közös verzióléptetés, main-integráció és a következő állandó kulcsos APK-pár az integrátor dolga.


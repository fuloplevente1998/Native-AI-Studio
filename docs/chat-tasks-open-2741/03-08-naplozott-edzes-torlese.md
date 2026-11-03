# 8. feladat – Naplózott edzés valódi törlése

**Felülíró állapot – 2743:** implementálva, #49 mainen. Ez történeti feladatleírás; a kész kódot ne írd újra. A készülékes elfogadást az aktuális átadó és tesztlista szerint ellenőrizd.

## Feladat

A naplózott/mentett edzés részletein legyen **Edzés törlése** gomb és megerősítés. A kijelölt edzés valóban törlődjön a tartós tárból, ne csak a listából vagy memóriából tűnjön el.

Ez külön feladat: az aktív/szüneteltetett edzés Folytatás melletti **Elvetés** funkciója már kész. Az edzésvégi elvetés nem bizonyítja a mentett history-sor és fájljai takarítását.

## Kiindulás

Auditáld és használd újra a meglévő `JournalViewModel.requestDeleteWorkout`, `cancelDeleteWorkout`, `confirmDeleteWorkout` és `WorkoutHistoryDao.deleteWorkoutByIdOrKey` útvonalat. A jelenlegi kód tranzakcióban tombstone-t ír és historyt töröl, majd privát route-takarítást végez; ez önmagában nem bizonyítja minden kapcsolt fájl sikeres takarítását. Vizsgáld meg a hibakezelést is. A Studio részletképernyő és MainActivity bekötése is szükséges lehet.

## Elfogadás

- Mégse nem módosít adatot. A megerősítés stabil edzésazonosítóhoz kötött; részletváltás, dupla koppintás, foglaltság vagy régi callback nem törölhet másik edzést.
- Siker után az adott history és hozzátartozó, más adathoz nem szükséges sorozat-/gyakorlat-/fotó-/GPS adatok és helyi fájlok valóban eltűnnek. SQL cascade nem bizonyít fájltörlést.
- Másik edzés, megosztott fájl és másik aktív session nem sérülhet. A szükséges minimális tartós tombstone/restore- és szinkronvédelem maradjon, régi import/sync ne élessze vissza a törölt edzést.
- A napló, PR-/volumen-/kardióstatisztikák a tényleges maradék adatokból frissüljenek. Újranyitás/újraindítás után sincs visszatérő sor.
- SQL-/fájlhiba nem jelenhet meg hamis teljes sikerként; a takarítás biztonságos újrapróbálhatóságát gondold végig a meglévő rendszerben.
- Egy korábban exportált ZIP-et ez nem ír át; ne állítsd annak vagy külső szolgáltató adatainak törlését.

Célzott regressziók elkülönített tesztadatokon: Mégse, pontos célazonosító, tartós törlés, saját fájlok takarítása, megosztott/idegen adatok megőrzése, tároló-/fájlhiba és visszaéledés elleni védelem. Actionsből futtasd őket. Valódi személyes backupot ne használj törlési tesztadatként. Külön képi referencia nem szükséges; gombhely a naplózott edzés részlete.

## Közös háttér és munkaszabályok

Repo: https://github.com/fuloplevente1998/Native-AI-Studio

Az 1–6 fejlesztés kész, #28 mainen. Igazolt appforrás `2d7171b9cce7762553e053ab98b9828d3e7f2fc5`, kiadás **2741 / 0.1.0-beta**. PR CI `41529082323` és main signed `41530039869`: mindkettő letöltött XML szerint 315/315 teszt, nulla hiba/kihagyás. Ez nem fizikai elfogadás. Ne implementáld újra az elkészült 1–6-ot.

A friss `main`-ből saját ág/worktree, PR célja `main`; a régi #28 integrációs ág már lezárt. Olvasd el: `docs/AI_STUDIO_HANDOFF_2026-11-02.md`, `docs/AI_STUDIO_CONTINUE_PROMPT.md`, `.specify/memory/constitution.md`, `docs/FRAME_DESIGN_SYSTEM.md` és az érintett hibajegy. Az aktuális main/hibajegy elsőbbséget élvez a fájlban megnevezett régi kódnyomokkal szemben.

Végezd el a kiosztott feladatot, magyarul kommunikálj. Alügynököt ne indíts. Csak a feladat területét módosítsd; a közös fájlokat jelezd az integrátornak. Natív Compose/Room/DataStore/motor marad. Ownership/ACK, finalization, tombstone, backup/restore és providerelválasztás nem gyengülhet. Nincs kitalált mérés, csendes providerfallback vagy adateltüntetés hibakerülés céljából. Verziót, alkalmazásazonosítót, signert, titkokat és kiadási workflow-t ne módosíts önállóan. Android-build/teszt a meglévő Actionsből, nem helyi Gradle-ből.

Átadás: reviewzható PR, végleges működés leírása, forrás-SHA, az aktuális SHA tényleges Actions-eredménye és releváns nyers XML. A kész kódot, a CI-t és a készülékes elfogadást külön jelöld; a folyamatban levő futást ne állítsd sikeresnek. A közös verzióléptetés, main-integráció és a következő állandó kulcsos APK-pár az integrátor dolga.


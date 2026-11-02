# UI #34 – Kisebb kártyaközi térközök az egész appban

## Feladat

Hibajegy: https://github.com/fuloplevente1998/Native-AI-Studio/issues/34

Csökkentsd következetesen az ablakok/kártyák és kártyasorok közötti térközöket a telefonos app egészében, a Kezdőlapon és a részletképernyőkön is. Ez új, 2741 utáni UI-javítás; a kész kerethierarchiát meg kell tartani.

Referencia: a felhasználó eredeti beszélgetésének **23:09-es Kezdőlap** és **23:10-es Egészség** képe. A képfájlok nem részei ennek az MD-nek; az eredeti üzenetből mellékelhetők. Személyes backup ZIP nem képernyőkép-referencia.

## Megvalósítás és elfogadás

- Auditáld a főoldalak és részleteik külső paddingjét, függőleges/horizontális kártyaközeit. Közös, következetes térközrendszert használj; a végleges értékeket a tényleges elrendezés alapján válaszd meg.
- Elsősorban a kártyák **közötti** üres teret csökkentsd. Ne zsugorítsd el a gombok érintési felületét vagy a szöveget, ne vágj le tartalmat.
- Az oldal/csoport erősebb kerete, belső panelek halványabb kerete és vékony gombkerete maradjon; használd a meglévő `TrainPilotFrames` / `TrainPilotOutlinedButton` rendszert.
- Keskeny kijelzőn és nagyobb betűmérettel se legyen átfedés vagy kezelhetetlen vezérlő. Ellenőrizd a görgetést, üres/hibás állapotokat is.
- Nincs változás a mérésekben, tárolókban, edzésmotorban vagy a Wear felületén.

A #35-tel közös UI-chatnek is adható. Külön chat esetén egyeztetendő közös fájlok: `HomeScreen.kt`, `HealthCoachAndSettingsScreens.kt`, közös UI-komponensek és stílusok. Ne írj felül másik feladatból érkező változást. Meglévő releváns UI-regressziók Actionsből; ne írj pusztán térközkonstansokat visszaellenőrző teszteket. A fizikai kinézet elfogadása a felhasználó készülékes tesztje.

## Közös háttér és munkaszabályok

Repo: https://github.com/fuloplevente1998/Native-AI-Studio

Az 1–6 fejlesztés kész, #28 mainen. Igazolt appforrás `2d7171b9cce7762553e053ab98b9828d3e7f2fc5`, kiadás **2741 / 0.1.0-beta**. PR CI `41529082323` és main signed `41530039869`: mindkettő letöltött XML szerint 315/315 teszt, nulla hiba/kihagyás. Ez nem fizikai elfogadás. Ne implementáld újra az elkészült 1–6-ot.

A friss `main`-ből saját ág/worktree, PR célja `main`; a régi #28 integrációs ág már lezárt. Olvasd el: `docs/AI_STUDIO_HANDOFF_2026-11-02.md`, `docs/AI_STUDIO_CONTINUE_PROMPT.md`, `.specify/memory/constitution.md`, `docs/FRAME_DESIGN_SYSTEM.md` és az érintett hibajegy. Az aktuális main/hibajegy elsőbbséget élvez a fájlban megnevezett régi kódnyomokkal szemben.

Végezd el a kiosztott feladatot, magyarul kommunikálj. Alügynököt ne indíts. Csak a feladat területét módosítsd; a közös fájlokat jelezd az integrátornak. Natív Compose/Room/DataStore/motor marad. Ownership/ACK, finalization, tombstone, backup/restore és providerelválasztás nem gyengülhet. Nincs kitalált mérés, csendes providerfallback vagy adateltüntetés hibakerülés céljából. Verziót, alkalmazásazonosítót, signert, titkokat és kiadási workflow-t ne módosíts önállóan. Android-build/teszt a meglévő Actionsből, nem helyi Gradle-ből.

Átadás: reviewzható PR, végleges működés leírása, forrás-SHA, az aktuális SHA tényleges Actions-eredménye és releváns nyers XML. A kész kódot, a CI-t és a készülékes elfogadást külön jelöld; a folyamatban levő futást ne állítsd sikeresnek. A közös verzióléptetés, main-integráció és a következő állandó kulcsos APK-pár az integrátor dolga.


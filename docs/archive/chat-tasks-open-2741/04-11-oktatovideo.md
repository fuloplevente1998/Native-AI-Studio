# Videó #11 – Nem induló oktatóvideók

**Felülíró állapot – 2743:** implementálva, #40 mainen. Ez történeti feladatleírás; a kész kódot ne írd újra. A készülékes elfogadást az aktuális átadó és tesztlista szerint ellenőrizd.

## Feladat

Hibajegy: https://github.com/fuloplevente1998/Native-AI-Studio/issues/11

Javítsd az üres/sötét, nem induló oktatóvideót. Konkrét reprodukció: **Otthoni A/B – Alap → A nap → Goblet guggolás → oktatóvideó**. A felhasználó több gyakorlatnál is jelezte, de a teljes érintett lista nem bizonyított. Az esetleges rossz videó-hozzárendelés egyelőre hipotézis.

## Vizsgálat és elfogadás

- Kiinduló hely: `TrainPilotCommonComponents.kt`, `resolveCatalogExerciseVideoAndNotes`, `ExerciseYouTubeVideoModalDialog`. A tényleges kiválasztott gyakorlat tárolt `videoUrl` mezőjét is kövesd végig, beleértve az egyéni/importált gyakorlatokat.
- Különítsd el a rossz/hiányzó URL-t, formátumkezelést, katalógus-fallbacket, WebView/YouTube beágyazási és hálózati hibát. Üres előnézetből nem állapítható meg a gyökérok.
- A megfelelő gyakorlathoz ellenőrzött, megfelelő tartalom induljon. Hiányzó találatot ne fedj el egy általános vagy másik gyakorlathoz tartozó videóval.
- Törölt, nem beágyazható, hibás vagy hiányzó videónál érthető állapot és használható külső megnyitás legyen, ha van érvényes hivatkozás; ne maradjon néma sötét terület.
- Megnyitás/bezárás ne rontsa el az aktív edzést, időzítőket, szünetet, ownershipet vagy navigációt.

A releváns feloldási/URL-/hibakezelési regressziókat Actionsből ellenőrizd. Fizikai telefonos lejátszást JVM-teszt nem igazol: külön írd le a tényleges próbát vagy a még hiányzó készülékes ellenőrzést. Ha videólinket cserélsz, ellenőrizd a tartalmát, ne találj ki hivatkozást. A korábbi referencia a #11-ben leírt felhasználói kép; a mostani 23:09/23:10 UI-képek ehhez nem referenciák.

## Közös háttér és munkaszabályok

Repo: https://github.com/fuloplevente1998/Native-AI-Studio

Az 1–6 fejlesztés kész, #28 mainen. Igazolt appforrás `2d7171b9cce7762553e053ab98b9828d3e7f2fc5`, kiadás **2741 / 0.1.0-beta**. PR CI `41529082323` és main signed `41530039869`: mindkettő letöltött XML szerint 315/315 teszt, nulla hiba/kihagyás. Ez nem fizikai elfogadás. Ne implementáld újra az elkészült 1–6-ot.

A friss `main`-ből saját ág/worktree, PR célja `main`; a régi #28 integrációs ág már lezárt. Olvasd el: `docs/AI_STUDIO_HANDOFF_2026-11-02.md`, `docs/AI_STUDIO_CONTINUE_PROMPT.md`, `.specify/memory/constitution.md`, `docs/FRAME_DESIGN_SYSTEM.md` és az érintett hibajegy. Az aktuális main/hibajegy elsőbbséget élvez a fájlban megnevezett régi kódnyomokkal szemben.

Végezd el a kiosztott feladatot, magyarul kommunikálj. Alügynököt ne indíts. Csak a feladat területét módosítsd; a közös fájlokat jelezd az integrátornak. Natív Compose/Room/DataStore/motor marad. Ownership/ACK, finalization, tombstone, backup/restore és providerelválasztás nem gyengülhet. Nincs kitalált mérés, csendes providerfallback vagy adateltüntetés hibakerülés céljából. Verziót, alkalmazásazonosítót, signert, titkokat és kiadási workflow-t ne módosíts önállóan. Android-build/teszt a meglévő Actionsből, nem helyi Gradle-ből.

Átadás: reviewzható PR, végleges működés leírása, forrás-SHA, az aktuális SHA tényleges Actions-eredménye és releváns nyers XML. A kész kódot, a CI-t és a készülékes elfogadást külön jelöld; a folyamatban levő futást ne állítsd sikeresnek. A közös verzióléptetés, main-integráció és a következő állandó kulcsos APK-pár az integrátor dolga.


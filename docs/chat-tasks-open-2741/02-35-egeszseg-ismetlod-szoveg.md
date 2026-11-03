# UI #35 – Az Egészség oldal ismétlődő szövegblokkjának eltávolítása

**Felülíró állapot – 2743:** implementálva, #38 mainen. Ez történeti feladatleírás; a kész kódot ne írd újra. A készülékes elfogadást az aktuális átadó és tesztlista szerint ellenőrizd.

## Feladat

Hibajegy: https://github.com/fuloplevente1998/Native-AI-Studio/issues/35

Az Egészség / Mai állapot panel fejléc és a kis mérési kártyák közötti plusz szöveges blokk szűnjön meg. A napi értékek a meglévő, hozzájuk illő kártyákban/részletekben maradjanak.

Referencia: az eredeti beszélgetés **23:10-es Egészség** képe. A blokk: „Mai adatok • Samsung Health”, „Utolsó sikeres szinkron: …”, „Napi lépés: … • Aktív energia: …”, „Samsung Health: sikeres szinkron”. Alatta már megjelennek az Alvás, Aktív kalória, Pulzus, Lépések, Mai edzés, Regeneráció kártyák. A kép külön mellékelhető az eredeti üzenetből; ennek az MD-nek nem része. A képen szereplő értékek nem kerülhetnek a kódba mérésként.

## Megvalósítás és elfogadás

- Kiinduló hívási hely: `app/src/main/java/com/example/ui/screens/HealthCoachAndSettingsScreens.kt`, `StudioDailyHealthStatus(uiState, …)`. Ellenőrizd az aktuális kódot; ne töröld vakon a másutt használt közös komponenst.
- Az érintett panelen a több soros redundáns blokk ne jelenjen meg; a meglévő kis kártyák, lenyitások és napi dátumváltás maradjanak.
- Forrás/szinkronidő és érdemi hiba, grant, részleges állapot a hozzájuk illő meglévő részletekben továbbra is elérhető legyen. A dátum melletti Frissítés és a #31 HC-diagnózis működését őrizd meg; a sikeres szinkron rutinszövegének ismétlése nem szükséges.
- Nem napiadat-szinkronjavítás: ne változtasd a HC/Samsung lekérést, cache-t, számítást, és ne keverd a providereket.
- A Kezdőlap korábbi azonos jellegű tisztítása már kész; azt ne hozd vissza.

A #34-gyel együtt kiosztható UI-chatnek. Külön chat esetén a `HealthCoachAndSettingsScreens.kt` módosítását jelezd az integrátornak. Releváns meglévő regressziók Actionsből; igazold, hogy mérési kártyák/frissítés/hibák megmaradnak. A végleges látvány készüléken fogadható el.

## Közös háttér és munkaszabályok

Repo: https://github.com/fuloplevente1998/Native-AI-Studio

Az 1–6 fejlesztés kész, #28 mainen. Igazolt appforrás `2d7171b9cce7762553e053ab98b9828d3e7f2fc5`, kiadás **2741 / 0.1.0-beta**. PR CI `41529082323` és main signed `41530039869`: mindkettő letöltött XML szerint 315/315 teszt, nulla hiba/kihagyás. Ez nem fizikai elfogadás. Ne implementáld újra az elkészült 1–6-ot.

A friss `main`-ből saját ág/worktree, PR célja `main`; a régi #28 integrációs ág már lezárt. Olvasd el: `docs/AI_STUDIO_HANDOFF_2026-11-02.md`, `docs/AI_STUDIO_CONTINUE_PROMPT.md`, `.specify/memory/constitution.md`, `docs/FRAME_DESIGN_SYSTEM.md` és az érintett hibajegy. Az aktuális main/hibajegy elsőbbséget élvez a fájlban megnevezett régi kódnyomokkal szemben.

Végezd el a kiosztott feladatot, magyarul kommunikálj. Alügynököt ne indíts. Csak a feladat területét módosítsd; a közös fájlokat jelezd az integrátornak. Natív Compose/Room/DataStore/motor marad. Ownership/ACK, finalization, tombstone, backup/restore és providerelválasztás nem gyengülhet. Nincs kitalált mérés, csendes providerfallback vagy adateltüntetés hibakerülés céljából. Verziót, alkalmazásazonosítót, signert, titkokat és kiadási workflow-t ne módosíts önállóan. Android-build/teszt a meglévő Actionsből, nem helyi Gradle-ből.

Átadás: reviewzható PR, végleges működés leírása, forrás-SHA, az aktuális SHA tényleges Actions-eredménye és releváns nyers XML. A kész kódot, a CI-t és a készülékes elfogadást külön jelöld; a folyamatban levő futást ne állítsd sikeresnek. A közös verzióléptetés, main-integráció és a következő állandó kulcsos APK-pár az integrátor dolga.


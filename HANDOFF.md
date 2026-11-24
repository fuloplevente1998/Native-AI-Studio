# Aktuális átadó

- Main: 1.4.4 kódja, teljes szerkezeti refaktor után (R0–R8, #356–#366, mind zöld CI). Működésváltozás nincs.
  Előbb `docs/CODEMAP.md`, utána csak a megnevezett fájl. Méretőr: `SourceSizeGuardTest`; áthelyezés: `scripts/split_kotlin.py`,
  `scripts/verify_move.py` (`--receiver Cls` kiterjesztésekhez). ViewModel: osztály = állapot + mag, funkciók `Vm*.kt` kiterjesztések;
  csomagon kívüli hívóhoz explicit `import com.example.ui.viewmodel.<név>`.
- Később bontandó (valódi átírás): `MainActivity.TrainPilotAppRoot`, `CalendarPlannerScreen`, `PersonalPlannerModal` – lásd REFACTOR_PLAN.
- Kiadás: 1.4.4 · 280500/280502 aláírt, ellenőrzött. Következő: **1.5.0 · 280600/280602** (2806 gyűjtő, #361).
- 2806 tervezett: Google-fiók leválasztás egy látható gombbal + választó lap (mockup kész, jóváhagyásra vár);
  paletta 12/15 kiemelőszín ≥4,5:1 (#349 része).
- Nyitott issue-k: #349 (32 téma közös kiemelés), #350 (saját témák + áttetszőség, leírás bővítve), #351 (óra mérés auto-indítás,
  jóváhagyásra vár), #359 (külső órás edzésimport – a refaktor feltétele teljesült).

## #349 / #350 – témák (WIP)
- Ág: `feature/349-350-themes`, draft PR #371; utolsó kód/teszt SHA: `f1dca82ca66e8e2c0ca52e9c92dc281c76f16592` (dokumentációt megelőző).
- #349: 32 beépített téma, változatlan ID/kulcs; közös kijelölés, olvasható gombfelirat/mezőkeret, fő/alsó nav, chipek, témaválasztó, Kezdőlap/Fejlődés/Egészség és aktív edzés színjavításai. Az edzésidő-jelvény és az óraátadás gomb közös színkezelése is bekötve; a #351 PR #370 ugyanazon fájlban végzett logikai módosításait integrációkor meg kell őrizni.
- #350: öt szerepes szerkesztő, négy alapstílus, HSV/HEX/gyorsszínek, HU/EN/DE/RO, két élő miniatűr előnézet, explicit kontrasztjavaslat. Beállítások és palettaválasztó Saját témák csoport, DataStore CRUD stabil UUID-val, az alkalmazás külön művelet, aktív szerkesztés frissít, aktív törlés az 1-esre áll vissza. Mentés-visszaállítás: hordozható `settings.customThemes350` rekordok; aktív hiányzó/sérült téma fallback. Wear a meglévő témacsomagban kapja az effektív, opak színeket és teljes UUID-t.
- Áttetszőség első ütem: 70–90% fedettség vagy kikapcsolva 100%, saját téma biztonságos monoton gradientje, tényleges alfa a Kezdőlap/Fejlődés/Egészség/Beállítások kijelölt felső szintű kártyáin és navigációján, modális lap belső ismert gradientjén. Ismeretlen/nested hátterek opak effektív színt tartanak. Nem teljes app-szintű audit, nem képválasztó vagy valódi blur; blur külön második ütem.
- Tesztek megírva: `CustomThemeDraft350Test`, `CustomThemeResolver350Test` (szélsőértékek, gradient, javítás stabilitás), `CustomThemeStore350Test` (CRUD, restore, sérült rekord), Wear UUID regresszió. CI/JVM/fordítás és készülékes vizuális/telefon-óra teszt még nincs. A kibővített drótváz nem végleges látványelfogadás. Nincs main-merge, verzióemelés, kiadás.
- Következő: célzott kódreview, Compose/valós eszközös UI- és TalkBack-ellenőrzés, #349 handover integráció, tényleges restore/Wear és teljes közös CI. Az aktív téma/szerkesztő állapot, áttetszőség további képernyői és nagy betűméret külön vizsgálandók. Párhuzamos #351/#359/#367 változások integrációjakor a közös dokumentációt és fájlokat össze kell fésülni.

- Tesztelés előkészítése: a felhasználó külön telefonos Theme Test APK-t engedélyezett, eltérő package/aláírással, nem kiadási buildként. Külön tesztágon, a production PR és a signer érintése nélkül. A kódreview során szerkesztőfókusz és törlés színjelzése javítva; fordítás, vizuális és integrációs ellenőrzés továbbra sem igazolt.

# 5. feladat – 2812 második kör, EGY közös PR (minden javítás)

Alap: `main` (1.5.2, `f779f5dc` vagy újabb). Ág: `fix/2812-5-all`. Egy draft PR a main felé, EGY közös CI-kör; a README közös szabályai érvényesek.
Gyűjtő (a teljes megfigyelés-lista): docs/FIXES_2812.md, sorok 12–18 (+ 1–11 már mainen: #397). Mockup: docs/mockups/2812/task5_theme_search_calendar_plus.png (13, 15, 16, 18).
Sorrend: A → B → C → D → E → G, feladatonként külön commit; a verzióemelés (F) az UTOLSÓ commit.

## A) Felnyíló lapok akadása (12. sor) + Google Cloud lap (14. sor)
Hol: `TrainPilotSheetRoutes` lapjai (WATCH_AND_SYNC = Kapcsolt eszközök, PERMISSIONS_SCREEN = Adatvédelem és engedélyek, BACKUP_RESTORE = Mentés/Google Cloud, PRIVATE_PHOTOS, EXERCISE_*, LOG_WORKOUT_DETAIL). Eszközön a legrosszabb: Adatvédelem és engedélyek, Kapcsolt eszközök; Google Cloud szinkron is akadós.
Feltételezett ok (kódból; ellenőrizd, mérés nincs): minden lap saját ablak-üvegréteget kap (`KyantWindowOverlay.kt`: külön scene + Overlay lencse), benne hosszú görgethető tartalom; nyitás/görgetés közben újrarajzol.
Teendő (kinézet MARAD, csak rajzolás egyszerűsödik):
1. A lap lencséje/homálya csak a lap kerete/háttere; a tartalom statikus festék (nincs soronkénti/kártyánkénti backdrop/blur/RenderEffect, nincs `graphicsLayer` a tartalmon).
2. Nyitás/zárás animáció és görgetés alatt ne rögzüljön újra a scene (statikus tartalom → egyszer rögzít, `remember`-rel).
3. A lapon belüli listák/oszlopok stabil kulccsal; drága számítás ne fusson kompozícióban (`remember`/`derivedStateOf`).
4. `GoogleCloudSyncCard.kt` (BACKUP_RESTORE): az állapot-frissítés (szinkron állapot, időbélyeg) ne rajzolja újra az egész lapot – állapotot a kártyába szűkítsd (`derivedStateOf`, külön composable, stabil paraméterek).
5. Rétegek ≤ 3 homályos réteg egyszerre a képernyőn.
Teszt: tiszta logika kiemelhető legyen (pl. mikor kell lencse/rögzítés) és tesztelt; meglévő teszt nem gyengül.

## B) Témák: glass téma neve olvasható (13. sor)
Beállítások › Témák: `StudioPaletteCategories` (rg: `StudioPaletteCategories`), `SettingsMainScreen.kt:~226`. A glass paletták stílusneve fekete, alig olvasható; a többi szöveg világos. Javítás: glass palettán a név a témakövető világos szövegszínt kapja (kontraszt ≥ 4,5:1 a csempe háttéren, a már meglévő `GlassContrast` segédekkel). Regressziós teszt mind a 8 glass palettára (33–40). Nem-glass palettán változatlan.

## C) Napló kereső keret (15. sor)
`CompactJournalSearchField` (`LogTabScreen.kt:~538`): vékony ~1 dp perem, felül világosabb, kártya-peremmel azonos színszármazás; fókuszban erősebb; ne kapjon külön homályréteget; üveg és nem üveg palettán is.

## D) Naptár kompaktabb (16. sor) + Programok alja dupla „+" (18. sor)
- Naptár tervező: `CalendarPlannerScreen.kt`, `PlannerControls.kt`. Az alsó gombsor ~10%-a lelóg → kisebb cellamagasság/térköz, szorosabb fejléc/jelmagyarázat, hogy a gombsor teljesen látsszon (mockup jobb alsó pár); kis kijelzőn/nagy betűmérettel marad a görgetés; 48 dp érintési cél megmarad a gomboknál.
- Programok alja: `ProgramsListScreen.kt:~672–725`. A három gombon `Icons.Default.Add` van; a `createOrEditProgramAction` szövege (`TrainPilotStrings.kt`, keresd az értékét) valószínűleg „+"-szal kezdődik → dupla „+". Egy jelölés maradjon gombonként; a két „új program" gomb szövege jól megkülönböztethető (mind a 4 nyelv: hu/en/de/ro).

## E) Oldalváltáskor az oldal tetejére (17. sor)
Fő fülek: Kezdőlap, Programok, Edzés, Naptár, Haladás, Egészség, Napló, Coach. Ha főoldalról MÁSIK főoldalra váltok, a célfül az oldal tetejéről induljon – minden főoldalra egységesen, ne csak egyre. Egy közös szabály a fő-fül váltásnál (a lista/görgetés állapot nullázása, pl. a fül-azonosító kulcsolja az állapotot vagy `scrollToItem(0)` a váltáskor). NEM nullázandó: alroute-ból (részletek, lap, dialógus) visszalépéskor a görgetési hely marad; mentett adat/DataStore nem változik. Pure-logic teszt: „fül-váltás → nullázás, alroute-visszalépés → megőrzés".

## G) Adatvédelmi tájékoztató: harmadik fél licencek (19. sor)
Hol: `com.repforge.app.PrivacyPolicy.text(lang)` (`app/src/main/java/com/repforge/app/PrivacyPolicyAndActivity.kt`), megjelenítés: `SettingsMainScreen.kt:~111–136` (dialógus). Jelenleg nincs benne licenc-említés. Teendő: új „Harmadik fél licencek" szakasz mind a 4 nyelven (hu/en/de/ro): Kyant Backdrop (io.github.kyant0:backdrop:1.0.6) és Shapes (io.github.kyant0:shapes:1.2.0), Apache License 2.0, © Kyant (Kyant0), forrás https://github.com/Kyant0/AndroidLiquidGlass, módosítatlan bináris függőségek a Liquid Glass megjelenítéshez. Vedd fel a `app/src/main/assets/third-party-licenses/kyant-backdrop-NOTICE.txt` összes felsorolt könyvtárát (olvasd el). Ha a licencszöveg megjeleníthető az appból (asset), hivatkozz rá. Ne írj új adatkezelési állítást, csak a licenc-szakaszt. Teszt: a szöveg mind a 4 nyelven tartalmazza a „Kyant" és „Apache" kulcsszavakat.

## F) Verzióemelés (UTOLSÓ commit, csak ha A–E kész)
Fájlok: `app/build.gradle.kts`, `wear/build.gradle.kts` (`rg "280800|280802"`). Új érték: versionName `1.5.3`, telefon `280900`, Wear `280902`. A séma: `27NN00`/`27NN02`. Más fájl változatlan (applicationId `com.TrainPilot.com`, aláírás). `HANDOFF.md` frissítés (≤40 sor, régit cseréld).

## Kész, ha
Minden A–E és G pont kész, CI zöld a PR-en (nyers JUnit számok a PR-leírásban), a verzió 1.5.3 (280900/280902). Ezután az ág mainre megy (a merge-et és a buildet a felhasználó kéri / a Claude Code session végzi) – TE NE merge-elj.

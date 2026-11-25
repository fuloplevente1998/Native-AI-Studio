# 2814 – gyakorlat utáni visszajelzés témázása

Alap: main `dc5c361ec99f28e7e1a6a1b97a1b7a3f9e89a50b`, 1.5.3 (280900/280902), #399 már merge-ölve.
Felhasználói észrevétel: a gyakorlat utáni „Gyakorlat visszajelzés” felugró ablak teljesen monokróm szürke; a teljes rész javítandó. A 2814 feladatcsomag-azonosító, nem verzió.

## Forrás és feltételezett ok
- `app/src/main/java/com/example/ui/components/StudioActiveWorkoutPresentation.kt:75–115`: a rememberExerciseFeedbackNext a közös TrainPilotAlertDialogban a StudioActiveExerciseFeedback/ExerciseFeedbackChips komponenseket jeleníti meg. Mind a Tovább, mind a Kihagyás továbbvisz, vissza/dismiss csak becsuk; a session/generation/exercise/route kulcsokat és a mentési guardokat megtartani.
- `ExerciseFeedbackChips.kt`: normál választási csempék glassTile semleges festéket, a választott elem elsődleges akcentust, a fájdalom negatív akcentust kap; megjegyzés mező, törlés és jegyzetkapcsoló is auditálandó. A megosztott komponens a gyakorlat alatti visszajelzést és összegző-szerkesztőt is kiszolgálja; nem szabad az ottani megjelenést véletlenül megváltoztatni.
- `TrainPilotAlertDialog.kt`: a külön ablak Kyant optikája a LocalGlassOpticsEnabled kapuhoz kötött, ami aktív mérési képernyőn zárt; a közös glassOverlayFill statikus opak fallbacket kap. A tényleges eszközön látott szürkeség pontos mértéke még nem ellenőrzött. Nem kapcsoljuk vissza emiatt az egész edzésképernyő drága optikáját.
- `GlassSurfaces.kt`, `KyantWindowOverlay.kt`: ablakhoz kötött saját jelenet, közös role-festék és kontraszt, statikus ablakjelenet; nincs szülőablak-mintavételezés vagy csempénkénti blur.

## Jóváhagyott vizuális terv
- A felugró ablak egységes, palettaszínű üvegfelület, olvasható cím és gyakorlatnév. A visszajelző párbeszédhez célzott, saját ablakos, lehetőleg statikus optika, támogatott API-n és energiatakarékosságon kívül; máskülönben palettaszínű, kontrasztos fallback.
- Hat meglévő válasz: Túl könnyű, Könnyű, Pont jó, Nehéz, Túl nehéz, Fájdalom. Finom szemantikus árnyalatok, külön kijelölés (perem/állapot), fájdalom külön negatív jelentés. Az aktuális paletta és a kontraszt szabályai elsőbbséget élveznek; nincs előre kijelölt válasz és nincs csak színnel jelzett állapot.
- Megjegyzés és mező, Válasz törlése, Kihagyás (másodlagos), Tovább (elsődleges) a közös TrainPilot vezérlőkkel. Nagy betűk, kis kijelző, billentyűzet és görgetés; a gombok nem vágódhatnak le.
- A meglévő összegző, napló és inline visszajelzés megosztott komponenseit ellenőrizni, célzott változtatás; a felhasználói kérés elsődleges hatóköre a gyakorlat utáni popup és annak teljes tartalma.

## Elfogadás / regresszió
- Mind a nyolc glass paletta, frosted/clear végpontok, egyedi és világos/sötét nem-glass téma; szöveg ≥4,5:1, értelmes vezérlő-kontraszt, energiatakarékos és API fallback.
- A megfelelő elhagyott gyakorlat kapja az explicit választ/jegyzetet; Kihagyás nem gyárt választ, Tovább egyszer léptet, távoli gyakorlatváltás elveti a régi promptot. A meglévő StudioActiveFeedbackRegressionTest és ExerciseFeedbackRegressionTest megőrzendő, célzott témázási ellenőrzéssel bővítendő.
- Nem változik motor, visszajelzés tárolt értékei, adatbázis, ownership, Wear protokoll vagy verzió. Fizikai vizuális elfogadás külön.
- Állapot: a glass és nem-glass mockup jóváhagyva; implementáció a #400 draftban, CI ellenőrizendő.

## 2. Edzés közbeni szerkesztőablakok (készülékes visszajelzés)
- Súly, ismétlés, RIR és sorozattörlés menü: a régi Modifier.background(p.surface) négyszögletes tartalomhátteret rajzolt, a legacy menü Material alapformát használt. Célzott WorkoutEditorDropdownMenu: 14 dp alak, opak palettafesték, 1 dp perem; glass/Glow színátmenetes peremfény, matt témán szilárd keret. A menü nem vesz mintát másik ablakból.
- Kétoldali sorozatszerkesztő is a célzott ablakstílust használja. A meglévő mentés/törlés és értékvalidáció változatlan.
- Kompakt visszajelzés: 3×2 rács normál szélességnél, nagy betűnél 2 vagy 1 oszlop; az opcionális jegyzet mező közvetlenül elérhető, tartalom görgethető, akciók fixen elérhetők. A kiválasztás pipa, nincs alapértelmezett válasz. A sorozatsor műveletmenüje marad, nem kerül be új azonnali törlőgomb.

## 3. L0–L8: teljes telefonos Liquid Drop audit és első integráció (jóváhagyva)
- Alap: #400 368253b5, CI #790 1287/0/0/0, R8 zöld. Korábbi visszajelzés, 14 dp / 1 dp szerkesztőmenü, kétoldali modal megőrzött; #399 main már tartalmazza a görgetési és egészség-lap javításokat.
- L1: a meglévő Kyant Button/Selection lencse erősítve, árnyék, depthEffect. Közös LiquidDropMaterial: valódi lencse csak az ablak saját meglévő jelenetéből, API/energiatakarékosság/overlay/route kapu; a sűrű listák és mérési kártyák olcsó, kontrasztkorlátos gradiens/perem és belső árnyék. Nem új renderelő vagy extra capture.
- L2–L5: közös elsődleges és alap outlined gomb, chip, beviteli keret, szemantikus műveletek pereme; fejléc/fülkijelölés, sorozatmezők, completed jelölés, gyakorlatválasztó, időzített/kétoldali blokkok, start/pause/next, pihenő, health/Home, program, napló, fejlődés/statisztika, Coach, naptár, beállítások célzott bekötések. Közös komponenssel a többi hívó is kapja a peremjavítást. A térkép és az órás renderelő változatlan. A veszélyes akciók nem vesztik el szemantikus színüket.
- L6: a korábban jóváhagyott popup megmarad, választás kap belső peremet. Külön ablakon nincs szülőablak mintavétel.
- L7–L8: JVM policy/kontraszt teszt és közös CI, készülékes A/B 8 paletta, 0/65/100, nagy betű, energiatakarékosság, 60/120 Hz görgetés és tartós mérés. Nem állítunk mért FPS-javulást. A további egyedi felületek (pl. részletes grafikonvezérlők, egyedi theme editor, kapcsoló hüvelykujja, GPS) optikai vizsgálata készüléken szükséges; nem mindegyik kapott valódi lencsét. Nincs merge, signing vagy verzióemelés.

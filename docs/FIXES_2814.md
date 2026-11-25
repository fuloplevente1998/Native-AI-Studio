# 2814 – gyakorlat utáni visszajelzés témázása

Alap: main `dc5c361ec99f28e7e1a6a1b97a1b7a3f9e89a50b`, 1.5.3 (280900/280902), #399 már merge-ölve.
Felhasználói észrevétel: a gyakorlat utáni „Gyakorlat visszajelzés” felugró ablak teljesen monokróm szürke; a teljes rész javítandó. A 2814 feladatcsomag-azonosító, nem verzió.

## Forrás és feltételezett ok
- `app/src/main/java/com/example/ui/components/StudioActiveWorkoutPresentation.kt:75–115`: a rememberExerciseFeedbackNext a közös TrainPilotAlertDialogban a StudioActiveExerciseFeedback/ExerciseFeedbackChips komponenseket jeleníti meg. Mind a Tovább, mind a Kihagyás továbbvisz, vissza/dismiss csak becsuk; a session/generation/exercise/route kulcsokat és a mentési guardokat megtartani.
- `ExerciseFeedbackChips.kt`: normál választási csempék glassTile semleges festéket, a választott elem elsődleges akcentust, a fájdalom negatív akcentust kap; megjegyzés mező, törlés és jegyzetkapcsoló is auditálandó. A megosztott komponens a gyakorlat alatti visszajelzést és összegző-szerkesztőt is kiszolgálja; nem szabad az ottani megjelenést véletlenül megváltoztatni.
- `TrainPilotAlertDialog.kt`: a külön ablak Kyant optikája a LocalGlassOpticsEnabled kapuhoz kötött, ami aktív mérési képernyőn zárt; a közös glassOverlayFill statikus opak fallbacket kap. A tényleges eszközön látott szürkeség pontos mértéke még nem ellenőrzött. Nem kapcsoljuk vissza emiatt az egész edzésképernyő drága optikáját.
- `GlassSurfaces.kt`, `KyantWindowOverlay.kt`: ablakhoz kötött saját jelenet, közös role-festék és kontraszt, statikus ablakjelenet; nincs szülőablak-mintavételezés vagy csempénkénti blur.

## Javasolt vizuális terv (jóváhagyás előtt)
- A felugró ablak egységes, palettaszínű üvegfelület, olvasható cím és gyakorlatnév. A visszajelző párbeszédhez célzott, saját ablakos, lehetőleg statikus optika, támogatott API-n és energiatakarékosságon kívül; máskülönben palettaszínű, kontrasztos fallback.
- Hat meglévő válasz: Túl könnyű, Könnyű, Pont jó, Nehéz, Túl nehéz, Fájdalom. Finom szemantikus árnyalatok, külön kijelölés (perem/állapot), fájdalom külön negatív jelentés. Az aktuális paletta és a kontraszt szabályai elsőbbséget élveznek; nincs előre kijelölt válasz és nincs csak színnel jelzett állapot.
- Megjegyzés és mező, Válasz törlése, Kihagyás (másodlagos), Tovább (elsődleges) a közös TrainPilot vezérlőkkel. Nagy betűk, kis kijelző, billentyűzet és görgetés; a gombok nem vágódhatnak le.
- A meglévő összegző, napló és inline visszajelzés megosztott komponenseit ellenőrizni, célzott változtatás; a felhasználói kérés elsődleges hatóköre a gyakorlat utáni popup és annak teljes tartalma.

## Elfogadás / regresszió
- Mind a nyolc glass paletta, frosted/clear végpontok, egyedi és világos/sötét nem-glass téma; szöveg ≥4,5:1, értelmes vezérlő-kontraszt, energiatakarékos és API fallback.
- A megfelelő elhagyott gyakorlat kapja az explicit választ/jegyzetet; Kihagyás nem gyárt választ, Tovább egyszer léptet, távoli gyakorlatváltás elveti a régi promptot. A meglévő StudioActiveFeedbackRegressionTest és ExerciseFeedbackRegressionTest megőrzendő, célzott témázási ellenőrzéssel bővítendő.
- Nem változik motor, visszajelzés tárolt értékei, adatbázis, ownership, Wear protokoll vagy verzió. Fizikai vizuális elfogadás külön.
- Állapot: gyűjtő és javasolt terv; még nincs implementáció vagy új CI. A munkarend szerint jóváhagyott látvány után kódolás, célzott regresszió és közös CI.

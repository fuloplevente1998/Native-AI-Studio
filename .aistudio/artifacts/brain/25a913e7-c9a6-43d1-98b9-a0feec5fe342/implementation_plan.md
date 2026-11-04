# Megvalósítási Terv: Kompakt Edzés Vezérlők és Edzésfotók (Edzés Végén + Naplóban)

## 1. Célkitűzés és Áttekintés
A visszajelzések és a mellékelt képernyőkép alapján három fő területet fejlesztünk tovább a **Graphite & Gold** dizájnrendszerrel összhangban, mind a 4 támogatott nyelven (`HU`, `EN`, `DE`, `RO`):

1. **Aktív edzés és Gyors edzés – Kompakt (38 dp) gombok és egyértelmű vizuális hierarchia**:
   - A jelenlegi túl nagy (52–56 dp-s), egyforma olívabarna hátterű négyzetek helyett **38 dp magas, kompakt sorozat-sorokat** vezetünk be.
   - Színben és stílusban élesen elkülönítjük a **sorozatszámot**, a **szerkeszthető beviteli mezőket** (`kg`, `ism.`, `RIR`), a **teljesítés gombot (`OK` / `✓`)** és a **törlés gombot (`—`)**.
   - A gyorsléptető gombokat (`-0.5 kg`, `+0.5 kg`, `-1 reps`, `+1 reps`, `RIR 0..3`) kompaktabb, jobb kontrasztú pill-gombokká alakítjuk.
2. **Edzés végi fotó csatolás (Kamera / Galéria + Edzés előtt / után / egyéb)**:
   - Az **Edzés összegzése (`WorkoutSummaryScreen`)** képernyőn mentés előtt egy dedikált **Edzésfotók** kártyát jelenítünk meg.
   - A felhasználó kiválaszthatja a kategóriát (**Edzés előtt** / `before`, **Edzés után** / `after`, **Egyéb** / `other`), majd a **Kamera** vagy **Galéria** gombbal fotót csatolhat az edzéshez, előnézettel és törlési lehetőséggel.
3. **Fotók kezelése és jelölése a Naplóban (`Napló` lista + `Edzés részletei`)**:
   - **Napló lista kártyák (`LogWorkoutHistoryCard`)**: A térkép (`Map`) ikonhoz hasonlóan egy **fotó indikátor ikont (`PhotoLibrary` / kamera jelvény fotószámmal)** jelenítünk meg minden olyan edzésnél, amelyhez tartozik fotó (`entry.photoCount > 0`), amellyel közvetlenül is megnyithatók az edzés fotói.
   - **Edzés részletei (`LogWorkoutDetailScreen`)**: A `Fotók (X)` gomb **0 fotó esetén is aktív** lesz (`Fotók (0) • + Hozzáadás`), és a megnyíló fotókezelő felületen (`StudioPrivatePhotos`) utólag is lehet új fotót hozzáadni (Kamera / Galéria + Edzés előtt / után / egyéb kategóriával) vagy meglévő fotót törölni a naplózott edzéshez.

---

## 2. Vizuális és UX Terv

### A. Aktív Edzés / Gyors Edzés Sorozat-sorok (`ActiveMeasurementScreens.kt`)
- **Sorozat kártya és méretezés**:
  - A sorozat-elemek magasságát `52–56.dp`-ről **`38.dp`**-re csökkentjük, keskenyebb belső térközzel (`padding = 8.dp`), így egy képernyőn több sorozat és a pihenőidőzítő is kényelmesen elfér.
- **Színkódolt szerepek (jobb megkülönböztethetőség)**:
  - **Sorozatszám (`#1`, `#2`)**: Kompakt `28.dp × 38.dp` jelvény, sötét háttérrel és arany (`palette.primary`) szöveggel.
  - **Beviteli mezők (`kg`, `reps`, `RIR`)**: Mély grafitszürke beviteli mező háttér (`Color(0xFF141310)`), világos kontrasztos kerettel (`palette.sectionBorder`), szerkesztéskor arany fókusz-kerettel, fehér félkövér számokkal.
  - **Teljesítés gomb (`OK` / `✓`)**:
    - Nem teljesített állapotban: arany keretes, enyhén arany-tónusú kiemelt akciógomb (`palette.primary`).
    - Teljesített állapotban: élénk zöld (`palette.success`) kitöltésű gomb pipával (`✓`), a teljesített sor pedig finom zöld keretet kap.
  - **Sorozat törlése (`—` / kuka ikon)**: Kompakt `34.dp × 38.dp` gomb, halvány vöröses (`palette.danger`) körvonallal és ikonnal, hogy ne olvadjon össze a beviteli mezőkkel.
- **Gyorsléptető sáv (`-0.5 kg`, `+0.5 kg`, `-1 reps`, `+1 reps`, `RIR 0..3`)**:
  - Kompakt `28.dp` magas gombok: a súly/ismétlés léptetők világosabb grafitszürke (`Color(0xFF25221B)`) felületet és arany/fehér szöveget kapnak, az aktív RIR chip pedig arany (`palette.primary`) kiemelést kap.

### B. Edzésfotók Kártya az Edzés Befejezésekor (`WorkoutSummaryScreen`)
- Az **Edzés összegzése** képernyőn (a hangulatválasztó kártya alatt, a Mentés gomb előtt) elhelyezünk egy **Edzésfotók (`Workout Photos`)** kártyát:
  - **Kategória választó chipek**:
    - `Edzés előtt` (`before`)
    - `Edzés után` (`after`)
    - `Egyéb` (`other`)
  - **Forrás gombok**:
    - **Kamera (`Fotó készítése`)**: `WorkoutPhotoRepository.prepareCameraCaptureIntent()` + `handleCameraResult(...)` használata.
    - **Galéria (`Galéria`)**: `WorkoutPhotoRepository.createPickImageIntent()` / Android Photo Picker + `importPickedUri(...)` használata (tárhely-engedély nélkül, Google Play irányelveknek megfelelően).
  - **Csatolt fotók előnézete**:
    - Vízszintes bélyegkép-sáv (`LazyRow`), minden képen a kategória címkével (`Edzés előtt` / `Edzés után` / `Egyéb`), nagyítási és eltávolítási (`×`) gombbal.
  - **Mentéskor**:
    - A csatolt fotók bekerülnek az aktív edzés `photos` JSON tömbjébe (`id`, `label`, `createdAt`, `width`, `height`, `bytes`), amelyet a `TrainPilotWorkoutFinalization.finishWorkout(...)` atomikusan elment a Room `workout_photos` táblába és a `workout_history.rawJson`-ba.

### C. Fotók Jelölése és Utólagos Kezelése a Naplóban (`LogAndProgressScreens.kt` & `StudioPrivatePhotos.kt`)
- **1. Fotó indikátor a Napló listában (`LogWorkoutHistoryCard`)**:
  - Ha `entry.photoCount > 0`, a kártya jobb oldalán (a térkép ikon mellett, a törlés gomb előtt) megjelenik egy **kamera/fotó ikon a fotók számával** (`journal_photos_badge_${entry.id}`).
  - Rákoppintva közvetlenül az adott edzés fotói nyílnak meg (vagy a kártyára kattintva az Edzés részletei).
- **2. Edzés részletei (`LogWorkoutDetailScreen`)**:
  - A `Fotók (${workout.photoCount})` gomb **mindig engedélyezett** (`enabled = true`), még `0` fotó esetén is.
- **3. Fotókezelő nézet (`StudioPrivatePhotos.kt` / `NativeProgressPhotoGallery.kt`)**:
  - Amikor egy konkrét naplózott edzésből nyitjuk meg (`privatePhotoWorkoutId != null`), a galéria tetején megjelenik a **Fotó hozzáadása ehhez az edzéshez** vezérlősáv:
    - Kategória választó: **Edzés előtt**, **Edzés után**, **Egyéb**.
    - **Kamera** és **Galéria** gombok.
  - Az új fotó hozzáadásakor a fotó fájlt a `WorkoutPhotoRepository` tárolja el, majd a Room `workout_photos` táblát és a `workout_history.rawJson` `photos` mezőjét tranzakcióban frissítjük, így a `photoCount` és a Napló lista azonnal frissül.

---

## 3. Érintett Fájlok
- `/app/src/main/java/com/example/ui/screens/ActiveMeasurementScreens.kt`:
  - `ActiveStrengthScreen` sorozat-sorok kompakt (`38.dp`) átalakítása és színkontraszt javítása.
  - `WorkoutSummaryScreen` bővítése az **Edzésfotók** kártyával (kategória választó + Kamera/Galéria + előnézet).
- `/app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt` & `/app/src/main/java/com/example/model/TrainPilotModels.kt`:
  - Aktív edzéshez csatolt fotók állapotkezelése (`activeWorkoutPhotos`), kategória (`before`, `after`, `other`) mentése az `ActiveWorkoutStore` snapshotba és a `finishAndSaveWorkout()` folyamatba.
  - Naplózott edzéshez utólagos fotó csatolás (`attachPhotoToLoggedWorkout`) és fotó eltávolítás támogatása, `loggedWorkouts` `photoCount` azonnali frissítésével.
- `/app/src/main/java/com/example/ui/screens/LogAndProgressScreens.kt`:
  - `LogWorkoutHistoryCard`: fotó indikátor jelvény/gomb megjelenítése (`entry.photoCount > 0` esetén), a térkép ikon mintájára.
  - `LogWorkoutDetailScreen`: a `log_detail_photos_button` engedélyezése `0` fotó esetén is.
- `/app/src/main/java/com/example/ui/components/StudioPrivatePhotos.kt` & `/app/src/main/java/com/repforge/app/ui/progress/NativeProgressPhotoGallery.kt`:
  - Kategória (`before`, `after`, `other`) megjelenítés és – kiválasztott edzés esetén – utólagos Kamera/Galéria fotó-hozzáadási sáv biztosítása.

---

## 4. Ellenőrzési Terv
- Unit és Robolectric tesztek futtatása a kompakt edzésvezérlők, az edzés végi fotómentés, valamint a Napló lista fotó-indikátor és utólagos fotó-hozzáadás ellenőrzésére.
- Teljes projekt fordítás (`compile_applet`) a hibamentes működés igazolására.

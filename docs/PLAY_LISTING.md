# Play Console – áruházi adatlap (tervezet, 1.5.4)

Csak meglévő, az appban működő funkciók. Egészségügyi állítás nincs.

## Rövid leírás (≤80 karakter)
- HU: `Edzésnapló, fotógaléria, programok, Wear OS óra és GPS-futás egy helyen.`
- EN: `Workout log, photo gallery, programs, Wear OS watch and GPS runs in one place.`

## Teljes leírás – HU
TrainPilot: edzéstervező és edzésnapló telefonra és Wear OS órára.

**Edzés**
• Erő-, időzített és kétoldali gyakorlatok: súly, ismétlés, RIR, pihenőidő-számláló
• Gyors edzés egy lapon: Erő, Időzített, Kültéri fül, több mint 140 gyakorlat kereséssel, kategóriákkal, kedvencekkel, videóval
• Edzés közben: Előző · Befejezés · Következő; befejezés mentéssel vagy elvetéssel
• Kültéri edzés GPS-útvonallal és térképpel (futás, séta, kerékpár, túra), kézi távmegadással

**Programok és tervezés**
• Beépített és saját programok, kártyán belüli szerkesztés, sorrend húzással
• Naptár-tervező: edzésnapok, időpontok, ismétlődő ritmus; a mai tervezett edzés a nap végéig elérhető
• Kezdőlap: a következő edzés egy érintéssel indítható

**Óra (Wear OS)**
• Az edzés átadható az órának és vissza; egy edzés, egy időzítő – nincs dupla rögzítés

**Fejlődés és egészség**
• Napló kereséssel és időszak-szűrővel, rekordok, statisztikák, izomcsoport-terhelés
• Health Connect és Samsung Health adatok (engedéllyel): lépés, alvás, pulzus, kalória – a hiányzó mérés nem számít nullának
• Coach: napi felkészültség és edzésjavaslat a saját adataid alapján

**Adataid nálad**
• Mentés és visszaállítás JSON/ZIP fájlba vagy Google Drive-ra
• GPS-útvonalak és fotók privát tárban; egészségadat csak kifejezett beleegyezéssel kerül mentésbe
• Több témaszín (világos és sötét), magyar, angol, német és román nyelv

## Full description – EN
TrainPilot: workout planner and training log for your phone and Wear OS watch.

**Training**
• Strength, timed and bilateral exercises: weight, reps, RIR, rest timer
• Quick workout in one sheet: Strength, Timed, Outdoor tabs; 140+ exercises with search, categories, favorites and video
• During a workout: Previous · Finish · Next; finish by saving or discarding
• Outdoor workouts with GPS route and map (run, walk, cycle, hike) and manual distance

**Programs and planning**
• Built-in and custom programs, inline editing, drag to reorder
• Calendar planner: workout days, times, repeating rhythm; today's planned workout stays available all day
• Home: start your next workout with one tap

**Watch (Wear OS)**
• Hand the workout over to the watch and back; one workout, one timer – no double logging

**Progress and health**
• Log with search and period filter, records, statistics, muscle-group load
• Health Connect and Samsung Health data (with permission): steps, sleep, heart rate, calories – missing readings never count as zero
• Coach: daily readiness and a workout suggestion based on your own data

**Your data stays yours**
• Backup and restore to JSON/ZIP files or Google Drive
• GPS routes and photos in private storage; health data is only included in backups with explicit consent
• Multiple light and dark themes; Hungarian, English, German and Romanian

## Újdonságok – 1.5.4

A Playen lévő 1.5.0 (280600/280602) óta készült változások. A szövegek nyelvenként legfeljebb 500 karakteresek.

### Telefon (281100)

```text
<hu-HU>
• Egyedi témák és Liquid Glass megjelenés. Finomított navigáció, Napló és naptár.
• Külső edzések importja a Health Connectből és a Samsung Healthből.
• Aktív és teljes edzéskalória külön, forrással és lefedettségi jelzéssel. A kézi visszaolvasás megőrzi a korábbi méréseket, ha kevesebb adat érkezik.
</hu-HU>
<de-DE>
• Eigene Designs und Liquid Glass. Verbesserte Navigation, Trainingsprotokoll und Kalender.
• Externe Trainings aus Health Connect und Samsung Health importieren.
• Aktive und gesamte Trainingskalorien getrennt, mit Quelle und Datenabdeckung. Eine unvollständige manuelle Aktualisierung erhält vorhandene Messwerte.
</de-DE>
<en-US>
• Custom themes and Liquid Glass. Refined navigation, workout log and calendar.
• Import external workouts from Health Connect and Samsung Health.
• Active and total workout calories are shown separately, with source and coverage. Manual refresh preserves saved measurements when less data is returned.
</en-US>
<ro>
• Teme personalizate și Liquid Glass. Navigare, jurnal de antrenamente și calendar îmbunătățite.
• Importă antrenamente externe din Health Connect și Samsung Health.
• Caloriile active și totale apar separat, cu sursa și acoperirea datelor. Actualizarea manuală păstrează măsurătorile salvate când primește mai puține date.
</ro>
```

### Wear OS (281102)

```text
<hu-HU>
• Lapozható főképernyők, egyszerűbb menü és világos óratémák.
• Átláthatóbb edzésvezérlés, rögzített pulzussor és érintészár.
• A telefonról indított edzés órás mérésének automatikus indítása támogatott kapcsolattal és engedélyekkel.
</hu-HU>
<de-DE>
• Hauptansichten zum Wischen, einfacheres Menü und helle Uhrendesigns.
• Klarere Trainingssteuerung, feste Herzfrequenzanzeige und Berührungssperre.
• Automatische Messung auf der Uhr bei Trainingsstart am Smartphone mit unterstützter Verbindung und den nötigen Berechtigungen.
</de-DE>
<en-US>
• Swipeable home screens, a simpler menu and light watch themes.
• Clearer workout controls, a pinned heart-rate row and touch lock.
• Automatic watch recording for workouts started on your phone with a supported connection and the required permissions.
</en-US>
<ro>
• Ecrane principale glisabile, meniu simplificat și teme luminoase pentru ceas.
• Comenzi de antrenament mai clare, puls afișat la poziție fixă și blocarea atingerilor.
• Măsurare automată pe ceas la pornirea antrenamentului de pe telefon, cu o conexiune compatibilă și permisiunile necesare.
</ro>
```

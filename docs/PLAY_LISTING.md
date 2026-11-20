# Play Console – áruházi adatlap (tervezet, 2780 · 1.4.0 alapján)

Csak meglévő, az appban működő funkciók. Egészségügyi állítás nincs.

## Rövid leírás (≤80 karakter)
- HU: `Edzésnapló, programok, Wear OS óra és GPS-futás egy helyen.`
- EN: `Workout log, programs, Wear OS watch and GPS runs in one place.`

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

## Újdonságok – Play kibocsátási megjegyzések (276500/276502 óta → 1.4.0, 278400/278402)

Szabály: mindig **4 nyelv** Play-címkékkel (`<hu-HU>`, `<de-DE>`, `<en-US>`, `<ro>`), nyelvenként ≤500 karakter,
**külön telefonos és órás** szöveg; a chatben egy másolható kódblokkban küldjük. Az összegzés mindig a Playen lévő
utolsó kiadás (most 276500/276502) óta összes újítást tartalmazza.

### Telefon (278400)
```
<hu-HU>
• Edzés közben: egységes fejléc, gyakorlatsor, Videó · Coach · Részletek, pihenő mindenhol, kész sorozat zöld, előző érték
• Edzés fül: minden nap dátummal; Gyors edzés 1 lapon
• Programok: kompakt kártyák, szerkesztő, húzható sorrend
• Fejlődés: PR rekordok, teljes listák; Statisztikák: Gyakorlatok · Kardió · Lépések
• Egészségnapló: tömör nézet, szűrők
• Dizájn: alulról nyíló lapok, új Coach ikon, világos témák
• Óra: kerek kijelzőre igazítva
• Javítások: Otthoni A/B, villogás, Health Connect
</hu-HU>
<de-DE>
• Im Training: ein Kopfbereich, Übungsleiste, Video · Coach · Details, Pause überall, fertige Sätze grün, letzte Werte
• Training: jeder Tag mit Datum; Schnelltraining auf einem Blatt
• Programme: kompakte Karten, Editor, Reihenfolge ziehen
• Fortschritt: PR-Rekorde, volle Listen; Statistik: Übungen · Cardio · Schritte
• Gesundheit: kompakte Tagesansicht, Filter
• Design: Bottom Sheets, neues Coach-Symbol, helle Themes
• Uhr: für runde Displays
• Fixes: Home A/B, Flackern, Health Connect
</de-DE>
<en-US>
• During workouts: one header, exercise strip, Video · Coach · Details, rest everywhere, done sets green, last values
• Workout tab: every day with its date; Quick workout in one sheet
• Programs: compact cards, editor, drag to reorder
• Progress: PR records, full lists; Statistics: Exercises · Cardio · Steps
• Health journal: compact day view, filters
• Design: bottom sheets, new Coach icon, light themes
• Watch: fits the round screen
• Fixes: Home A/B, flicker, Health Connect
</en-US>
<ro>
• În antrenament: antet unic, bară de exerciții, Video · Coach · Detalii, pauză peste tot, serii gata verzi, ultimele valori
• Antrenament: fiecare zi cu data; antrenament rapid pe o foaie
• Programe: carduri compacte, editor, ordine prin tragere
• Progres: recorduri PR, liste complete; Statistici: Exerciții · Cardio · Pași
• Sănătate: vizualizare zilnică compactă, filtre
• Design: foi glisante, icon Coach nou, teme luminoase
• Ceas: ecran rotund
• Remedieri: Home A/B, pâlpâire, Health Connect
</ro>
```

### Óra (278402)
```
<hu-HU>
• Kerek kijelzőre igazított menük, gombok és szövegek (Galaxy Watch 7 40 mm-en is)
• Egyszerűbb főmenü, „Beállítás” külön
• Naptár: Programnapok sor, vissza gomb
• Edzés közben: ikonos léptetés, új Befejezés gomb, „Részletek”
• Pulzus és mérés a kezdőképernyőn
• Javítás: levágott sorok, tördelés
</hu-HU>
<de-DE>
• Menüs, Tasten und Texte für runde Displays (auch Galaxy Watch 7 40 mm)
• Einfacheres Hauptmenü, eigene „Einstellungen“
• Kalender: Zeile Programmtage, Zurück-Taste
• Im Training: Pfeil-Symbole, neue Beenden-Taste, „Details“
• Puls und Messung auf dem Startbildschirm
• Fixes: abgeschnittene Zeilen, Umbruch
</de-DE>
<en-US>
• Menus, buttons and text fitted to round screens (incl. Galaxy Watch 7 40 mm)
• Simpler main menu, separate Settings
• Calendar: Program days row, back button
• During workouts: icon arrows, new Finish button, Details
• Heart rate and measurement on the home screen
• Fixes: clipped rows, text wrapping
</en-US>
<ro>
• Meniuri, butoane și texte adaptate ecranului rotund (inclusiv Galaxy Watch 7 40 mm)
• Meniu principal mai simplu, Setări separat
• Calendar: rând Zile program, buton înapoi
• În antrenament: săgeți cu iconițe, buton nou Finalizare, Detalii
• Puls și măsurare pe ecranul principal
• Remedieri: rânduri tăiate, încadrare text
</ro>
```

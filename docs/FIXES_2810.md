# 2810 – gyűjtő (1.5.1 · 280700/280702, 2809 build után)

| # | Téma | Hol | Megfigyelés (eszköz, 2026-11-25) / javaslat | Állapot |
|---|------|-----|----------|---------|
| 0 | Fantom edzés frissítés után | Óra | Frissítés után nem indult edzés az órán – 2809/5 rendben | ellenőrizve ✓ |
| 1 | Óra – telefonos Liquid Glass témánál alig olvasható | Wear | Glass palettánál az óra opak tartaléka (nincs üveg) – ez rendben, de a szöveg/kiemelés kontrasztja kevés. Glass paletta → óra saját, kontrasztőrzött sötét változat (szöveg ≥4,5:1, vezérlő ≥3:1), regressziós teszt mind a 8 glass palettára | felmérésre |
| 2 | Üveg vegyes: sok kártya még tömör sötét | Telefon – Edzés csempék, Beállítások sorai, Naptár, lap (sheet) belső mezői, Heti célok, sorozat sorok | Egyes kártyák már üvegesek (Kezdőlap fő kártyák, Egészség), mások tömör `surface` kitöltésűek → nem egységes. Minden kártya/csempe ugyanazon az üveg-festéken menjen | felmérésre |
| 3 | Aktív edzés képernyőn nincs háttér | Telefon – edzés közben | Edzés közben sima sötét lap, foltok/kép nélkül; a csempék tömörek | felmérésre |
| 4 | Maradék keretek | Programlista kártyák, Fejlődés lista sorai, Mai javaslat, sheet körvonal, Statisztika kártya | Még látható semleges körvonal (valószínűleg `selection`/primer-alfa alapú, ezért a kapu megtartja) | felmérésre |
| 5 | Teszt APK: külső Compose Liquid Glass motor | Telefon – csak teszt ág (soha nem merge, mint #372) | `io.github.kyant0:backdrop` 1.0.6 (Apache-2.0, 2026-02). Igényli: Compose 1.10.x (most BOM 2024.09 = 1.7) és Kotlin stdlib 2.3 → csak a teszt ágon emelve. Cél: összehasonlítás a saját AGSL motorral; licenc NOTICE az appban. Referencia: Lelonio/Square ugyanezt a motort használja (Kyant backdrop v2.0.0 forrásként beemelve, Compose BOM 2026.06) – a Square saját kódja **GPL-3.0**, abból nem másolunk, csak az Apache-2.0 Kyant motort; a Square jegyzete szerint régebbi Compose-zal a bináris AAR futásidőben elhasalt → Compose emelés kell | javaslat |

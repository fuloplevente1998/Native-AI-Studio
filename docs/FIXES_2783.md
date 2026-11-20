# 2783 – gyűjtő (1.4.0 / 278200 után)

| # | Hol | Visszajelzés | Javaslat | Állapot |
|---|-----|--------------|----------|---------|
| 1 | Fejlődés – PR rekordok lap | A csempén a szám a kiválasztott időszakra (1/7/30 nap, 3 hó) szűr, de a lenyíló lista a teljes listát mutatja | A lap ugyanazt a szűrt listát mutassa, mint a csempe száma; teszt: időszakváltásra a lista és a szám egyezik | kész |
| 2 | Egészség – Mai állapot (6 csempe) | Csempére koppintva most egy köztes gomb jön elő | Ha van adat (pl. alvás), a csempe közvetlenül a megfelelő Egészségnapló-adatot nyissa (mai nap, adott típus szűrve); adat nélkül marad a mostani viselkedés; teszt csempénként a megnyitott típusra | kész – alvás, aktív kcal, lépés; pulzus a saját pulzusgörbéjét nyitja (mint eddig), edzés és regeneráció marad a régi lap |
| 3 | Coach oldal – gyakorlatonkénti javaslatok | Alul sok a szöveg | Egy kártya, gyakorlatonként 1 sor (név · chip: Tartás/+/− · cél egységgel · sorozatszám); közös ok egyszer felül; részlet koppintásra; „/kar” → „ism./kar” | kész |
| 4 | Statisztikák – Erő | Gyakorlatonként mini fejlődési grafikon, kinyitva a nagy | Kártyán mini görbe (utolsó 8 edzés) + változás; koppintásra a mostani nagy grafikon időszak-chipekkel; mértékegység típus szerint; kevés adatnál nincs görbe | kész |
| 5 | Elnevezés | „Erő” nem fedi a tartalmat (testsúlyos, időzített is) | Statisztikák fül: „Erő” → „Gyakorlatok” (Exercises / Übungen / Exerciții); az Edzés fül és a Gyors edzés „Erő” csempéje/füle: javaslat „Ismétléses” (Reps / Wiederholungen / Repetări) | kész – Statisztikák: Gyakorlatok · Kardió · Lépések; Edzés fül + Gyors edzés: Ismétléses · Időzített · Kültéri |

# 2776 – gyűjtő

| # | Hol | Visszajelzés | Javaslat | Állapot |
|---|-----|--------------|----------|---------|
| 1 | Edzés közben – alsó sáv | Nagyon beleolvad a sorozatkártyákba (azonos szín) | A) sötétebb (téma-háttér) sáv, felül témaszínű vonal + árnyék, lista alja elhalványul; B) lebegő, körben keretes sáv résekkel → **A jóváhagyva** | kódolva |
| 2 | Edzés fül – 3 csempe (Erő/Időzített/Kültéri) | A szöveg nem látszik („súly × ismétl…”, „14…”, „30…”, „GP…”) | Alcím 2 sorba törhet; › nyíl a cím mellé, így a jelvény („140 gyak.”, „GPS élő”) teljes szélességet kap; nagy betűnél tör, nem vág | kódolva |
| 3 | Egészségnapló – egy nap kinyitva | Szép blokkok, de nagyon hosszú | 4 csempe + 9 soros lista → egy 3×3 rács; nyers rekordok 1 soros sorok (azonos időablak lépés+kcal+táv egy sorban), szűrő fülek (Mind/Lépés/Kalória/Táv/Pulzus/Alvás), forrás és „módosítva” a sor részleteiben; bezárt nap 1 soros összegzés (mockup) | mockup |

## Bekötés
Csak megjelenés: az alsó sáv gombjai/pihenő-vezérlői és a csempék `onClick`-jei változatlanok
(`Collector2772UiRegressionTest.workoutTabTilesAndExerciseLibraryRowCallTheirActions` ellenőrzi).

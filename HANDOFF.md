# TrainPilot Native – Health Connect-központú átállás

- Alap: main `5d3b8a09`, 1.5.4 (281100/281102), #402. Aláírt APK/AAB és Play szöveg átadva. A Play-kiadási jegyzet alapja 1.5.0 (280600/280602).
- #403 a teljes fejlesztési pont. #351 és #359 fennmaradt készülékes esetei ide kerültek. #392 helyett a #397 ment mainre.
- Első szakasz, `feat/403-health-connect`. Health Connect pulzus mindkét Android-adapterben az aktív időablak mintáiból. Deduplikáció és egységes eredet. Késői pulzus Room-csatolása a korábbi energia megtartásával. Egy forrás mérőszámonként a mentett edzésben, Health Connect elsőbbséggel. Aktív és összes kcal külön.
- Célzott pulzus-, napló-, lefedettségi és Room-regresszió. A fizikai Health Connect adatátadás ellenőrzése nyitott.
- Következő szakasz a napi szinkron alapértelmezése, a Samsung Health és Wear OS kiegészítőkapcsolók, majd a kompakt Egészségnapló a #403 szerint. A Wear OS árazása későbbi termékdöntés.

## Második szakasz és edzés-visszajelzés

- #404, `feat/403-health-connect`. Telefonon Health Connect elsőbbség a napi szinkronban, a napi megjelenítésben és a Coach bemenetén. A közvetlen Samsung Health és TrainPilot Wear OS külön opt-in. A Samsung kiegészítő a meglévő lépés- és Coach-fallbackot táplálja. A már tárolt adatok eredete megmarad.
- Órás csempék és új átadás csak engedélyezett integrációval és igazolt kapcsolattal. Aktív órás tulajdonos esetén a visszavétel elérhető. A mockup a [Drive-on](https://drive.google.com/file/d/1aKeFCvQjnEV8kZDedUroSbeXE20QiI1A/view).
- Az edzés közbeni visszajelzés és a befejezéskor felugró visszajelzés közös szerkesztőkomponenst használ. A dialog saját görgetése és léptetése megmarad. Telefonos változások.
- Következő a kompakt Egészségnapló, több napi mérőszám tartalék forrása eredetjelzéssel és a készülékes ellenőrzés. Liquid Glass #405 külön követhető.

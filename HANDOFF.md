# TrainPilot Native – Health Connect-központú átállás

- Alap: main `5d3b8a09`, 1.5.4 (281100/281102), #402. Aláírt APK/AAB és Play szöveg átadva. A Play-kiadási jegyzet alapja 1.5.0 (280600/280602).
- #403 a teljes fejlesztési pont. #351 és #359 fennmaradt készülékes esetei ide kerültek. #392 helyett a #397 ment mainre.
- Első szakasz, `feat/403-health-connect`. Health Connect pulzus mindkét Android-adapterben az aktív időablak mintáiból. Deduplikáció és egységes eredet. Késői pulzus Room-csatolása a korábbi energia megtartásával. Egy forrás mérőszámonként a mentett edzésben, Health Connect elsőbbséggel. Aktív és összes kcal külön.
- Célzott pulzus-, napló-, lefedettségi és Room-regresszió. A fizikai Health Connect adatátadás ellenőrzése nyitott.
- Következő szakasz a napi szinkron alapértelmezése, a Samsung Health és Wear OS kiegészítőkapcsolók, majd a kompakt Egészségnapló a #403 szerint. A Wear OS árazása későbbi termékdöntés.

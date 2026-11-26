# TrainPilot Native – Health Connect-központú átállás

- #403, #404, `feat/403-health-connect`. Alap: main `5d3b8a09`, 1.5.4 (281100/281102), #402. A korábbi Play-jegyzet alapja 1.5.0 (280600/280602).
- A telefon a Health Connectet használja elsődlegesen napi és mentett edzésadatokhoz. Samsung Health és TrainPilot Wear OS külön engedélyezhető. A napi tartalék egy mérőszámhoz egy forrást választ. A meglévő mérések eredete megmarad.
- Az edzés pulzusa az aktív időablak mintáiból számolható. Utólag érkező pulzus a korábbi energia megőrzésével kerül a mentett edzéshez. Az aktív és az összes kcal külön jelenik meg.
- Egy kompakt Egészségnapló van, napi összesítővel, megnyitható mentett edzésekkel és lenyitható mérési rekordokkal. A mért érték forrása a részletekben érhető el.
- Új órás kapcsolatjelzés és átadás csak a bekapcsolt integrációnál és igazolt kapcsolatnál jelenik meg. A futó órás edzés vezérlése visszavehető. Az órás alkalmazáskód változatlan. [Beállítási mockup](https://drive.google.com/file/d/1aKeFCvQjnEV8kZDedUroSbeXE20QiI1A/view)
- A telefonon az edzés közbeni visszajelzés és a befejezés után megjelenő lap közös szerkesztőt használ.
- A #351 és #359 készülékes esetei a #403-ban követhetők. A teljes PR-CI és a készülékes próba két külön ellenőrzés. Liquid Glass #405 külön követhető.

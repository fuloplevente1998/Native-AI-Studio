# TrainPilot Native – Health Connect-központú átállás

- #403, #404, `feat/403-health-connect`. Alap: main `5d3b8a09`, 1.5.4 (281100/281102), #402. A korábbi Play-jegyzet alapja 1.5.0 (280600/280602).
- A telefon a Health Connectet használja elsődlegesen napi és mentett edzésadatokhoz. Samsung Health és TrainPilot Wear OS külön engedélyezhető. A napi tartalék egy mérőszámhoz egy forrást választ. A meglévő mérések eredete megmarad.
- Az edzés pulzusa az aktív időablak mintáiból számolható. Utólag érkező pulzus a korábbi energia megőrzésével kerül a mentett edzéshez. Az aktív és az összes kcal külön jelenik meg.
- Egy kompakt Egészségnapló van, napi összesítővel, megnyitható mentett edzésekkel és lenyitható mérési rekordokkal. A mért érték forrása a részletekben érhető el.
- Új órás kapcsolatjelzés és átadás csak a bekapcsolt integrációnál és igazolt kapcsolatnál jelenik meg. A futó órás edzés vezérlése visszavehető. Az órás alkalmazáskód változatlan. [Beállítási mockup](https://drive.google.com/file/d/1aKeFCvQjnEV8kZDedUroSbeXE20QiI1A/view)
- A telefonon az edzés közbeni visszajelzés és a befejezés után megjelenő lap közös szerkesztőt használ.
- A #351 és #359 készülékes esetei a #403-ban követhetők. A Health Connect-csomag `24951d4d` CI-je 1319 teszten ment át, ebből 276 órás. A készülékes próba nyitott. [CI](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/45873078732)
- A #405 és a #403 közös buildbe kerül. A #378 a Kyant-referencia a mostani üvegfelületeken. A fő és körvonalas gombok Kyant-lencséje rugós animációt kap, az érintésfény az aktuális felületre rajzol, a Mai állapot rövid lapja közvetlenül festi a fagyasztott hátteret. Áttetsző Fejlődés, Kardió és Lépések vezérlők és kártyák. Az első nyitás teljesítményét és a látványt készüléken kell ellenőrizni.

## #405 felcsúszó lapok

- Ág `fix/sheet-transition-405`, alap `fc7a4001`. A Mai állapot részletlapjai tartalomhoz igazodnak a korábbi felső magasságkorláttal.
- A közös üveglap azonos helyi koordinátarendszerben rajzolja a hátteret és a Kyant-lencsét. A mögöttes háttér animációja a legutolsó lap bezárásakor folytatódik. A magasságrögzítés feloldása csak megváltozott magasságnál kér új mérést.
- A regressziótesztek a dinamikus méretet, a rögzítés feloldását és az egymásra nyitott lapokat fedik. A készülékes képkocka-idő és az első nyitás ellenőrzése nyitott.

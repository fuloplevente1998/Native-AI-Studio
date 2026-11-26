# TrainPilot Native – Health Connect-központú átállás

- Kiadási versionCode emelés felhasználói kérésre: 281200 telefon / 281202 Wear, versionName 1.5.4 változatlan. A korábbi 281100/281102 már a Playen van. A #409 Play listing dokumentáció egyesítve, új kódokkal; a korábbi #823 forrás nem kiadandó. Új teljes main CI és aláírt páros szükséges; nincs automatikus Play-publikálás.

- Main hotfix (felhasználói engedély): Samsung kiegészítő kikapcsolva csak a Végső Coach készenléti fő kijelzés marad, a pontszámok bontása nem foglal helyet. Coach és Mai állapot/regeneráció, natív Coach alternatív felület; kapcsoló változására újraolvasás. Számítás változatlan. UI teszt mindkét állapotra. A #821 1324/1324 tesztje a korábbi közös PR-ra vonatkozott; új main CI és signing szükséges.

- #820 CI: 1045 telefonos tesztből 1 hibás, R8 sikeres. A régi Home-regresszió még feltétlen Samsung Energy csempét várt. A teszt most kikapcsolt, bekapcsolt és ismét kikapcsolt állapotot ellenőriz, a hat csempe és Coach navigációs ellenőrzése változatlan.

- Közös #407 + #408 kiadás: a sheet tag ütközés javítva; a teljes integrált CI után merge és main signing.

- #408 folytatás: napi aktív/napi teljes energia összesítés; edzésben kizárólag teljes kcal az aktív intervallumok uniójára (2/10 órás pause nem része). Egy teljes forrás, részleges időablak nem teljes érték. Samsung közvetlen opcionális UI, megőrzött rekordok, HC elsődleges. CI és készülékes ellenőrzés szükséges.

- Kapcsolt eszközök javítása, `fix/connected-devices-health-sync`: témakövető háttér a közös üveglap felett, HC szinkron a meglévő `hc` akcióval, látható folyamat/eredmény/hiba és kettős indítás tiltása. Samsung csempe csak bekapcsolt kiegészítőnél, engedély és kapcsoló külön. Wear útvonal változatlan. UI regresszió hozzáadva; CI és készülékes ellenőrzés nyitott.

- #403, #404, `feat/403-health-connect`. Alap: main `5d3b8a09`, 1.5.4 (281100/281102), #402. A korábbi Play-jegyzet alapja 1.5.0 (280600/280602).
- A telefon a Health Connectet használja elsődlegesen napi és mentett edzésadatokhoz. Samsung Health és TrainPilot Wear OS külön engedélyezhető. A napi tartalék egy mérőszámhoz egy forrást választ. A meglévő mérések eredete megmarad.
- Az edzés pulzusa az aktív időablak mintáiból számolható. Utólag érkező pulzus a korábbi energia megőrzésével kerül a mentett edzéshez. A napi aktív és teljes kcal külön összesített, az edzés teljes kcal-ja az explicit szüneteket kizáró intervallumokból származik.
- Egy kompakt Egészségnapló van, napi összesítővel, megnyitható mentett edzésekkel és lenyitható mérési rekordokkal. A mért érték forrása a részletekben érhető el.
- Új órás kapcsolatjelzés és átadás csak a bekapcsolt integrációnál és igazolt kapcsolatnál jelenik meg. A futó órás edzés vezérlése visszavehető. Az órás alkalmazáskód változatlan. [Beállítási mockup](https://drive.google.com/file/d/1aKeFCvQjnEV8kZDedUroSbeXE20QiI1A/view)
- A telefonon az edzés közbeni visszajelzés és a befejezés után megjelenő lap közös szerkesztőt használ.
- A #351 és #359 készülékes esetei a #403-ban követhetők. A Health Connect-csomag `24951d4d` CI-je 1319 teszten ment át, ebből 276 órás. A készülékes próba nyitott. [CI](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/45873078732)
- A #405 és a #403 közös buildbe kerül. A #378 a Kyant-referencia a mostani üvegfelületeken. A fő és körvonalas gombok Kyant-lencséje rugós animációt kap, az érintésfény az aktuális felületre rajzol, a Mai állapot rövid lapja közvetlenül festi a fagyasztott hátteret. Áttetsző Fejlődés, Kardió és Lépések vezérlők és kártyák. Az első nyitás teljesítményét és a látványt készüléken kell ellenőrizni.

## #405 felcsúszó lapok

- Ág `fix/sheet-transition-405`, alap `fc7a4001`. A Mai állapot részletlapjai tartalomhoz igazodnak a korábbi felső magasságkorláttal.
- A közös üveglap azonos helyi koordinátarendszerben rajzolja a hátteret és a Kyant-lencsét. A mögöttes háttér animációja a legutolsó lap bezárásakor folytatódik. A magasságrögzítés feloldása csak megváltozott magasságnál kér új mérést.
- A regressziótesztek a dinamikus méretet, a rögzítés feloldását és az egymásra nyitott lapokat fedik. A készülékes képkocka-idő és az első nyitás ellenőrzése nyitott.

- A folytatásban a normál böngészőlapok (gyakorlatkönyvtár/részlet, edzésrészlet, gyors edzés, izomedzések, egészségnapló) kényszerített teljes magassága megszűnik; a hosszú tartalom korlátosan görgethető. A szándékosan teljes magasságú billentyűzetes szerkesztőket megőrizzük. A gyors edzés regressziója rövid/hosszú lapot és visszaváltást is ellenőriz. A Coach főoldal hiányzó/hibás adatnál explicit Health Connect frissítőgombot ad. Készülékes próba és új CI szükséges.

- Coach regresszió (#403) ugyanebben a PR-ben. A korábban Samsungra mentett forrásbeállítás mellett a Coach és az edzés közbeni ajánlás is Health Connect alapú. A Coach frissítése a Health Connect mai méréseit kéri.

# 2813 – 1.5.3 készülékes visszajelzések (280900/280902)

Tesztelt alap: main `8a787567777da5c3ed0c5be9b12bc36614c549fe`, felhasználói visszajelzés: az utolsó build egyébként jó.
Ez gyűjtő dokumentum, nem jóváhagyott implementáció; a 2813 a feladatcsomag azonosítója, nem kiadási verzió.

| # | Hely | Készülékes megfigyelés | Elvárt eredmény |
|---|---|---|---|
| 1 | Egészség → Mai állapot → Lépések, Alvás | A lap felnyílik, majd hirtelen feljebb ugrik; csak ekkor látszik a teljes tartalom. | Egyetlen folyamatos nyitás a végleges, tartalomhoz igazított (korlátozott) magasságra; nincs utólagos ugrás. |
| 2 | Fejlődés → Statisztikák → naplózott eredmények | A lista a Fejlődés/Statisztikák másodlagos sáv mögé ér, nem a fő felső navigáció alá; nincs tartalmi áttetszőség. | A lista a fő navigáció és a másodlagos tabsáv alatt is ténylegesen folytatódik. A sávok rögzítettek és a meglévő Liquid Glass optikát használják. |
| 3 | Folyamatban lévő edzés → felső időtartam/órás mérés/vezérlés sáv | A görgetett tartalom megáll a második felső sávnál; nem jut a fő felső panel alá, nincs megfelelő áttetszőség. | A görgetett tartalom mindkét felső sáv mögött látható, elmosva; az edzésadatok és a vezérlés végig olvashatók és működnek. |
| 4 | Aktív edzés → alsó edzésvezérlő | A görgetett tartalom nem folytatódik az alsó dokk mögött. | Az alsó dokk is rögzített, tartalmat mintavételező Liquid Glass réteg, a lista alágörgethető, az utolsó sor teljesen elérhető. |

## Célzott forrásellenőrzés (main)
- 1: `ex/ui/screens/health/HealthTodayStatusSheet.kt:86–109` – tartalomhoz igazodó TrainPilotScreenSheet, belső 66%-os magasságkorlát és scroll; vizsgálandó, hogy adatfrissülés vagy első/animáció alatti mérés módosítja-e a horgonyokat. `ex/ui/components/TrainPilotScreenSheet.kt`, `TrainPilotModalBottomSheet.kt` – a #398 magasságrögzítésének megőrzése; gyökérok még nem bizonyított.
- 2: `ex/ui/screens/progress/ProgressTabScreen.kt:107–179` – a felső spacer és a rögzített másodlagos tabsáv a listát megelőzi a Columnban; `ex/ui/screens/StudioStatisticsScreen.kt:125–129` – külön LazyColumn. Ez megakadályozza a lista tényleges alágörgetését. Vizsgálandó egy közös, lokális tartalom-capture és overlay, mért in-scroll felső távtartóval, önmintavételezés nélkül.
- 3: `ex/MainActivity.kt:538–568` – az aktív edzés átadási sávja a topBar része. `ex/ui/theme/GlassAwareScaffold.kt:41–47,65–82,91–93` – aktív mérési route-ok nem részei a glassUnderlapRoute-nak; a body teljes felső paddinget kap, így a rögzített felső sávok nem mintavételezhetik az alattuk gördülő listát. `ex/ui/components/StudioWorkoutHandoverBar.kt:96` – statikus glassTile Surface.
- 4: `ex/ui/screens/measure/ActiveStrengthWorkoutScreen.kt:137–143,210–245`, `ActiveTimedWorkoutScreen.kt:186–188,500–519`, `ActiveBilateralWorkoutScreen.kt:261–264,769–793` – súlyozott lista és külön, rögzített alsó Surface testvérek a Columnban; a lista nem érhet a dokk mögé. A kültéri GPS eltérő felépítését és térképbiztonságát külön ellenőrizni, nem automatikusan átalakítani.

## Elfogadási feltételek
- A lépés/alvás lap nyitásakor, frissülő és üres adatoknál, nagy betűkkel és kis kijelzőn sem ugrik. A rövid lap rövid marad; hosszú lap görgethető; lehúzás, X és rendszer-vissza változatlan.
- A glass témákban a fő fejléc, másodlagos sáv és alsó dokk a tényleges mozgó tartalmat mintavételezi (nem pusztán átlátszó egyszínű festék). Első és utolsó listaelem teljesen elérhető; a fix vezérlők nem fedik el az érinthető célokat.
- Nem-glass téma és kikapcsolt optika: meglévő olvasható elrendezés megőrzése; API/fallback és energiatakarékos mód ellenőrzése. Nincs önmintavételezés, blur minden soron vagy új fölösleges élő capture.
- Aktív edzés: nincs timer-, mérési-, GPS-, ownership/ACK-, mentési vagy Wear/shared motorváltozás. A pihenő-dokk, billentyűzet/inset, vezérlőgombok, visszalépés, edzéslezárás működik. Room/DataStore, applicationId és aláírás változatlan.
- A már jó Beállítások és a többi képernyő érintetlen. Előbb jóváhagyott mockup, majd kód és célzott regressziók, közös teljes CI. Fizikai készülékes elfogadás külön.

`ex/` = `app/src/main/java/com/example/`.

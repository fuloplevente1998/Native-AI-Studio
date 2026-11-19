# 2771 – gyűjtött javítások

Kódolás csak „mehet” után, egy közös CI-körrel.

| # | Terület | Visszajelzés | Terv | Állapot |
|---|---------|--------------|------|---------|
| 1 | Sorrend (═ fogantyú) | Húzáskor a többi elem nem ad helyet, csak elengedéskor kerül a helyére | Húzás közben a húzott elem alatti/feletti sorok animálva (≈150 ms) egy sornyit odébb csúsznak a célhelyig; elengedéskor egy mentés, mint eddig. `TrainPilotReorder.kt` (`displacementFor(index)`), teszt: húzás közbeni eltolás + végső sorrend | gyűjtve |
| 2 | Napló naptár-szűrő | Oldalt nem fér el (7×48 dp cella túl széles, V oszlop levágva); lenyílva kiszorítja a naplózott edzéseket | Cellák `weight(1f)` + arány, mindig kifér. Megnyitás nem a listába ágyazva: A) alulról nyíló lap „Rendben” gombbal (javasolt) vagy B) lebegő panel a lista fölött. Bezárva a szűrő gombon a tartomány (pl. „Nov 9 – 17”). `LogAndProgressScreens.kt` | mockup küldve, döntésre vár |

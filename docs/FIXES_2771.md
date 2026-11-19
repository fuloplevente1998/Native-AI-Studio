# 2771 – gyűjtött javítások

Kódolás csak „mehet” után, egy közös CI-körrel.

| # | Terület | Visszajelzés | Terv | Állapot |
|---|---------|--------------|------|---------|
| 1 | Sorrend (═ fogantyú) | Húzáskor a többi elem nem ad helyet, csak elengedéskor kerül a helyére | Húzás közben a húzott elem alatti/feletti sorok animálva (≈150 ms) egy sornyit odébb csúsznak a célhelyig; elengedéskor egy mentés, mint eddig. `TrainPilotReorder.kt` (`displacementFor(index)`), teszt: húzás közbeni eltolás + végső sorrend | gyűjtve |

# 2771 – gyűjtött javítások

Kódolás csak „mehet” után, egy közös CI-körrel.

| # | Terület | Visszajelzés | Terv | Állapot |
|---|---------|--------------|------|---------|
| 1 | Sorrend (═ fogantyú) | Húzáskor a többi elem nem ad helyet, csak elengedéskor kerül a helyére | Húzás közben a húzott elem alatti/feletti sorok animálva (≈150 ms) egy sornyit odébb csúsznak a célhelyig; elengedéskor egy mentés, mint eddig. `TrainPilotReorder.kt` (`displacementFor(index)`), teszt: húzás közbeni eltolás + végső sorrend | kódolva |
| 2 | Napló naptár-szűrő | Oldalt nem fér el (7×48 dp cella túl széles, V oszlop levágva); lenyílva kiszorítja a naplózott edzéseket | Cellák `weight(1f)` + arány, mindig kifér. Megnyitás nem a listába ágyazva: A) alulról nyíló lap „Rendben” gombbal (javasolt) vagy B) lebegő panel a lista fölött. Bezárva a szűrő gombon a tartomány (pl. „Nov 9 – 17”). Hónapváltás oldalra húzással is, mint a Naptár oldalon (azonos gesztus, állandó magasság: 6 sor). `LogAndProgressScreens.kt` | kódolva (A + húzás) |

## Audit-javaslatok (döntésre vár, „mehet” előtt mockup)
| # | Hely | Most | Javaslat |
|---|------|------|----------|
| 3 | Naptár: esemény szerkesztése, edzés áthelyezése, Tervezési ritmus | középső ablak | alulról nyíló lap |
| 4 | Programok: Új egyedi program, Átnevezés, Új edzésnap | középső ablak | alulról nyíló lap (billentyűzet felett) |
| 5 | Egészség: Mérés szerkesztése, Egészségadatok, Edzés egészségadatai; Célok beállítása | középső ablak | alulról nyíló lap |
| 6 | Coach: Gyakorlatváltozat előnézete | középső ablak | alulról nyíló lap |
| 7 | Egészség-böngésző dátumválasztó (`CompactDatePickerDialog`) | ablak, nyilak, fix cellák | lap + oldalra húzás + rugalmas cellák |
| 8 | Napló statisztika-kártya, Coach tanács részletei, Ma-csempe kibontás | listán belül nyílik, kiszorít | lap vagy kártyán belüli kompakt kibontás |
| – | Megerősítések (törlés, elvetés, mentetlen, naptár ürítése), Adatvédelmi | középső ablak | **marad** |

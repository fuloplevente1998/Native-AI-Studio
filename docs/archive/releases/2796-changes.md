# TrainPilot 1.4.1 — változások a 278900 / 278902 óta

Telefon **279600**, Wear OS **279602**. Az összehasonlítás alapja a 2789-es `aed077b`, a kiadás forrása `de6f47c07d3edc756cb7c244f43624e3355a47da`.

## Telefon

- **2790.** A helyi és Drive-mentés megőrzi a célokat, a naptári edzéseket és a tervezési beállításokat. A mentési panel jelzi a folyamatot és az eredményt. Egy naptári edzés szerkesztése megőrzi a többi esemény szinkronadatait. A testsúlymérés dátumának átírásakor megmarad a fotó. Az Egészségnapló és a forráskezelés külön lapon nyitható. [#311](https://github.com/fuloplevente1998/Native-AI-Studio/pull/311)
- **2791.** Óránkénti lépésgrafikon, stabilabb szolgáltatóváltás az Egészségnaplóban, rendezettebb témaválasztó és kompaktabb pihenővezérlők. Az izomterhelés részleteiből megnyithatók a mentett edzések. [#313](https://github.com/fuloplevente1998/Native-AI-Studio/pull/313)
- **2792.** A Napló külön gombot kapott a felső navigációban. Kompaktabb Edzésvolumen és PR-rekordok, részletesebb izomcsoportok és egyoldalas Kardió statisztika. Az edzésbeállítások a program nevét mutatják. Javult a fényes témák gombkerete és a Health Connect engedélykezelése. [#314](https://github.com/fuloplevente1998/Native-AI-Studio/pull/314) · [#319](https://github.com/fuloplevente1998/Native-AI-Studio/pull/319) · [#320](https://github.com/fuloplevente1998/Native-AI-Studio/pull/320)
- **2793.** Lejjebb vettük az Edzésvolumen időszakválasztóinak magasságát. Frissültek a heti célok és a lépésgrafikonok, valamint rendezettebb és kihagyható első indítási engedélybeállítás került az appba. [#322](https://github.com/fuloplevente1998/Native-AI-Studio/pull/322) · [#323](https://github.com/fuloplevente1998/Native-AI-Studio/pull/323)
- **2794–2795.** A Kezdőlap, Edzés, Fejlődés és Egészség valódi kártyái rendezhetők, az egymás mellettiek oldalirányban is. A Gyors edzés megkapta az Edzés módgrafikáit. A gyakorlatszám a katalógusból jön. Az edzésstílus és a progressziós szabály a profilszerkesztőben is kiválasztható. [#325](https://github.com/fuloplevente1998/Native-AI-Studio/pull/325) · [#326](https://github.com/fuloplevente1998/Native-AI-Studio/pull/326)
- **2796.** A Kezdőlap és az Egészség hat Mai állapot csempéje ugyanazt a részletlapot nyitja. A lépésekhez a Samsung Health órás méréseit használjuk. Hiány esetén engedélyezett másik forrásból is megjelenhet adat, a lépésszámok összeadása nélkül. A Regeneráció kompakt Coach-nézetet nyit. Az igazolt mai Samsung Energy Score Health Connect mellett is látható. [#327](https://github.com/fuloplevente1998/Native-AI-Studio/pull/327)

## Wear OS

A 278902 óta az órás alkalmazás saját forráskódja változatlan. A 279602-es aláírt csomag az 1.4.1-es telefonos kiadás kísérője.

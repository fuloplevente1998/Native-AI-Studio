# 2772 – gyűjtött javítások

Kódolás csak „mehet” után, egy közös CI-körrel.

| # | Terület | Visszajelzés | Terv | Állapot |
|---|---------|--------------|------|---------|
| 1 | Programok – sorrend húzás | A kártya kerete nem követi a mozgatást, lemarad (üres keret marad a helyén) | Ok: a programkártyán a keret a mozgatás *előtt* van a modifier-láncban, ezért nem mozdul. Megoldás: `trainPilotReorderItem` a keret elé (mint a gyakorlatsoroknál). Teszt: húzás közben a keret a kártyával mozog | gyűjtve |
| 2 | Egészségnapló szűrő | Túl nagy a naptár; legyen olyan, mint a Napló naptár-szűrője | Kezdő/Zárónap gombok + „Alkalmaz” helyett egy szűrőgomb (📅 időszak ⌄) + frissítés ikon. Megnyitva ugyanaz a közös naptár-lap, mint a Naplóban: 2 nap = intervallum (hónapokon át is), Mai nap, Szűrő törlése (alap: utolsó 30 nap), 6 sor, oldalra húzás, Rendben. Közös komponens a Naplóval | mockup küldve |
| 3 | Mai nap / Szűrő törlése (Napló + Egészség) | Legyenek jobban kiemelve | Teljes szélességű, 48 dp gombpár a naptár felett: Mai nap = kitöltött elsődleges szín 📍, Szűrő törlése = piros tónusú ✕ | mockup küldve |
| 4 | Gyors edzés – újragondolás | Gyakorlatlista és kültéri edzés új oldalra visz (gyors edzésből és az Edzés fülről is) – furcsa | Egy lap, belső lépésekkel (‹ vissza a lapon belül): kezdő = 3 csempe (💪 Erő, ⏱ Időzített, 🏃 Kültéri) + sor indítása. Erő/Időzített → keresés + kategória + lista (▶ Indítás, ⋮ videó/sorba/részletek) a lapban; a külön „Gyakorlatválasztó” beolvad. Kültéri → tevékenység + GPS/óra állapot + Indítás a lapban; élő GPS-képernyő csak indítás után. Edzés fül gombjai ugyanezt a lapot nyitják a megfelelő lépésnél | mockup küldve |
| 5 | Kültéri edzés – kézi táv mező | Túl szögletes, túl nagy; a felette lévő 3 mező nem egyforma magas/széles | Mező: app-stílusú kompakt beviteli mező (48 dp, 12 dp ív, palette színek), Mentés gomb azonos magas. 3 metrika-csempe: azonos szélesség, sormagasságra igazítva (`IntrinsicSize.Min` + `fillMaxHeight`), 1 soros feliratok ellipszissel | mockup küldve |

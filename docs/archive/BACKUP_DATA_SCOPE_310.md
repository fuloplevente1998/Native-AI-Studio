# Mentési adatkör és visszaállítás

| Adat | Helyi JSON | Helyi ZIP | Google Drive |
| --- | --- | --- | --- |
| Edzésnapló, testsúly, programok, gyakorlatok és hordozható beállítások | Igen | Igen | Igen |
| Heti vagy havi edzéscél, napi lépéscél | Igen | Igen | Igen |
| Tervezett, elvégzett és kihagyott naptári edzések, mentett tervezési beállítások | Igen | Igen | Igen |
| Privát fotók azonosítói és hivatkozásai | Igen | Igen | Igen |
| Privát fotók képfájljai | — | Ellenőrzött mellékletek | Külön Drive-appadatfájlok |
| Edzéshez mentett egészségösszesítések | Hozzájárulással | Hozzájárulással | Hozzájárulással |
| Napi egészségnapló, benne a mért lépések, mérési dátum és forrásadatok | Hozzájárulással | Hozzájárulással | — |
| Privát GPS-nyomvonal koordinátái, Google-fiók bejelentkezési adatai | — | — | — |

A lépéscél egy beállítás. A mért napi lépések helyi egészségnaplóból vagy a
Health Connect és Samsung Health engedélyezett forrásaiból származnak.
A Drive az alkalmazás saját mentését szinkronizálja. A külön Google Naptár
szinkron kezelése saját engedélyhez kötött.

A `scheduled` sorok hordozzák a naptári eseményazonosítókat, dátumot, kezdő és
záró időpontot, program- és napazonosítót, valamint az állapotot. Visszaállításkor
ugyanabban a Room-tranzakcióban épül fel a Naptártervező `native:calendarState`
megjelenítési dokumentuma. A lemondási jelölők megmaradnak a szinkronhoz.
A szerkesztési visszavonási előzményt az eszköz tárolja. A naptári tételt
módosító visszaállítás után új előzmény indul. A naptár tartalmát egy teljes új
helyi mentés felülírja. A `calendarScheduleVersion` jelölő nélküli régi mentés
naptári tartalma kiegészíti a meglévőt, és a hiányzó cél- vagy tervezési
beállítások érintetlenül maradnak.

A valódi telefonos JSON/ZIP/Drive próba és a szolgáltatói lépések újbóli
beolvasásának ellenőrzése külön készülékes feladat.

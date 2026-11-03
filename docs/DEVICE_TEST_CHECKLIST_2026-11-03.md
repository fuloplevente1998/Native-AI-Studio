# Készülékes teszt – 2742, PR #38–#41

A telepített app tényleges verzióját a Beállításokban ellenőrizd; a tesztjegybe írd bele.
A felhasználó az összevont APK-t teszteli. Az alábbiak elfogadási feltételek, nem már
igazolt készülékes eredmények.

- [ ] #39 / #34: Kezdőlap, Egészség, Edzés, Fejlődés kártyái között kisebb, következetes térköz; nincs levágott tartalom, túl kicsi gomb, hibás görgetés. Világos és sötét témában is próbáld.
- [ ] #38 / #35: az Egészség Mai állapot kártyái felett nem ismétlődik a több soros napi mérési blokk. A kártyák, dátum melletti frissítés, részletek és érdemi hiba/engedélydiagnózis működnek.
- [ ] #40 / #11: Goblet guggolás videója, egy másik gyakorlat és egy egyéni/importált videólink a megfelelő tartalmat nyitja. Hiányzó/hibás linknél érthető állapot, nincs véletlen általános helyettesítő videó vagy néma üres terület. Videóból visszalépve az edzés és időzítő működik.
- [ ] #41: Samsung és HC kiválasztását/szinkronját külön próbáld ugyanarra a helyi napra és egy korábbi napra. Ha valamelyik szolgáltatónál ténylegesen hiányzik egy adat/nap, ne pótolja a másik cache-éből. Forrásváltás, részletek és újranyitás után is helyes szolgáltató szerepel. Két forrásban jogosan azonos mérés önmagában nem hiba; az eredeti HC-üresség okát csak tényleges rekord/grant/origin/ablak bizonyítja.
- [ ] Általános regresszió: programból edzés indítás, szünet/Folytatás, mentés és visszanyitás működik; meglévő naplóadatok megmaradtak. Ha használod az órát, telefon–óra átadás és visszatérés is ellenőrizendő.

## Már jelzett, még nyitott

- #43: a naplózott edzés valódi törlése/gombja még nem készült el; ez a korábbi 8. feladat, nem része a #38–#41-nek. Az aktív edzés Elvetés gombjával ne helyettesítsd.
- #44: világos témában sötét naptár és rossz kontraszt (01:51-es felhasználói kép).
- #45: a térképnek alapból az app témáját kell követnie; jelenleg külön mentett térképmód van.

Hibajelzéshez elég: appverzió, érintett oldal, pontos lépések, elvárt és tényleges eredmény,
valamint szükség esetén képernyőkép. Teljes személyes egészségmentés nem szükséges.

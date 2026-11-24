# Edzésrészletek – pulzus/energia összesítő és források (#120)

A három WearOS / Samsung Health / Health Connect szolgáltatói panel és a
forrásműveletek csak a Forrás fülön jelennek meg. A Statisztikák egyetlen, a
mentett edzéshez és időablakhoz kötött pulzus-/energiaösszesítőt kap.

- Átfedéskor látható sorrend: mentett WearOS, Samsung Health, Health Connect.
  Az eredeti szolgáltatói adatok megmaradnak; a bizonyított közös rekordazonosító
  és a puszta időbeli átfedés külön jelzést kap.
- Csak igazolt, nem átfedő részablakok adhatók össze. Nincs arányos szétosztás,
  hiányzó időszak becslése vagy súlyozatlan átlagok átlagolása. Több pulzusátlaghoz
  valódi mintaszám kell. A részleges és régi ablakadat korlátja látszik.
- Aktív, teljes és edzéshez rendelt energia nem adódik össze. Vegyes vagy ismeretlen
  energiatípusú aggregátumokból nem készül új, látszólag pontos érték.
- Új olvasások megőrzik a már lekért aktív részablakokat a meglévő health240 JSON-ban;
  nincs új adatbázisséma, mérési lekérdezés vagy motor.

Érdemi regressziók: tükrözött források, diszjunkt/átfedő időablakok, mintaszámos
pulzus, nulla/hiány/érvénytelen érték, elavult edzésablak, vegyes energia és
szolgáltatói hibák; meglévő napló-UI teszt a Statisztikák/Forrás elhelyezést is védi.

Ellenőrzés: helyi diff ellenőrizve, teljes Gradle/CI a felhasználó kérésére a
közös main-integráció után fut. Ez a leírás még nem állít sikeres teljes tesztet
vagy készülékes elfogadást. Valós szolgáltatói és vizuális ellenőrzés külön szükséges.

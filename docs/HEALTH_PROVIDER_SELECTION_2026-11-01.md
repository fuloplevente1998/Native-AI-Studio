# Health adatforrás-választás javítása

Kiindulás: main 7a236fb, #12 után. #1 audit folytatása.

- Samsung/Health Connect gombok konkrét forrást választanak; az aktuális gomb újranyomása nem vált másik forrásra.
- A napi értékek kizárólag a kiválasztott csatornából jönnek. Hiányzó Samsung-nap nem helyettesíthető HC-adatokkal.
- Az edzésnaplóban helyi forrásválasztó és az aktív időablakokra korlátozott SDK-olvasás van; nincs átirányítás az Egészség főoldalra.
- Az egyes szolgáltatók edzés-összesítései a meglévő health240 dokumentum providers objektumában külön tárolódnak. A régi lapos HC-mentések olvashatók, a Wear adatok külön maradnak.
- Futó edzésen szív gomb nyit külön, csak olvasási lekérdezést. Nem adja át a vezérlést, nem ír edzéstörténetet vagy szenzoradatot. Generáció/ownership/szünet/foreground változás megszakítja a lekérést.
- A Samsung rögzített „1/2” státusza helyett tényleges SDK-engedélypróba jelenik meg. A telepített szolgáltató nem bizonyít sikeres olvasást.
- A szinkron eredménye tartós szövegként is látható: siker/részleges/hiba/blokkolt. Az utolsó teljes szinkron időpontja szolgáltatónként külön marad; a korábbi globális dátum nem Samsung-siker bizonyítéka.
- A scoped meta a meglévő export/restore whitelist és törlési szabályok része, nincs adatbázisséma-váltás.

## Ellenőrzés

Új regressziók: elkülönített/legacy összesítések, SDK-router mindkét irányban és fallback tiltása, 45 perc aktív idő, részleges SDK-hibák megőrzése, pontos napi csatorna, szolgáltatónkénti szinkronidő és törlés, élő lekérés ownership/finalization védelme.

CI állapota és nyers tesztjelentés az új javító PR-ben lesz rögzítve. A Samsung SDK valós olvasását és az app/cert regisztrációt fizikai eszközön kell igazolni; az SDK telepítése/engedélypróba önmagában ezt nem bizonyítja. Maps Cloud-kulcs és #11 videóhiba továbbra is külön nyitott feladat.

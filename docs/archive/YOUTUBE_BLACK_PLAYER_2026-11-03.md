# YouTube: fekete oktatóvideó – #59

## Megfigyelés és elvárt működés

A felhasználó a 2744/81da2f7 APK tesztelésekor, a „Fekvenyomás rúddal” oktatóvideónál fekete felületet látott. Az app közben „A lejátszó készen áll” szöveget mutatott. Ez készülékes hibajelzés, nem sikeres elfogadás. A kép nem bizonyítja, hogy a videó tiltott, hálózati hiba vagy WebView-renderelési hiba történt.

Az adott gyakorlathoz rendelt videó az appon belül induljon; az API inicializálása, a lejátszás kérése és a tényleges lejátszás külön állapot. Elakadáskor legyen egyértelmű hiba és használható belső böngészős lehetőség.

## Változás

- Az IFrame API a saját `div` elemből hozza létre a lejátszót, a kiválasztott, validált videóazonosítóval. Nem csatolódik egy előre, versengve betöltődő iframe-hez.
- HTTPS appazonosság a WebView base URL/referrer és az IFrame `origin` helyén: `https://com.trainpilot.com`; az általános appassets-origin helyett. A WebView engedi a kiválasztott oldal harmadik féltől származó sütijeit a YouTube iframe/consent számára. A HTTPS-, fájl-, natív bridge- és jogosultságvédelmek megmaradnak.
- Látható videóelőnézet és HTML Lejátszás gomb. A kattintás valódi webes felhasználói gesztus; nem automatikus lejátszás. API-ready nem rejti el az előnézetet, csak a tényleges playing esemény.
- Az indítás külön állapot, 18 másodperc után hiba, ha nincs lejátszás. Az autoplay-blokkolás és 100/101/150/153 hibák láthatók, késői ready/paused callback nem törölheti őket. Újrapróbálás tudatos újratöltés.
- A beágyazás legalább 200 dp magas, a YouTube minimum viewportjához. A belső böngésző az elérhető dialógusmagassághoz igazodó, magasabb nézetet kap, hogy a YouTube-oldal és a beleegyezés kezelhető legyen.

Edzés, időzítő, tároló, tulajdonos/ACK/epoch, program, katalógusvideó-választás, package/signer/verzió és Wear-kód nem változik. Nincs külső alkalmazás indítása.

## Ellenőrzés

A tényleges beágyazott JavaScript 7 hálózat nélküli eseménytesztje sikeres (`node --test scripts/test_exercise_video_player.mjs`): nincs autoplay, kattintás indít, kizárólag playing rejti az előnézetet, hiba után késői callback nem tesz kész állapotba, blokkolt indítás hiba. A teszt nem állít valós YouTube-lejátszást. Két JVM-regresszió az állapotsorrendet, késői callbacket, hibamegőrzést és a package/origin egyezést vizsgálja. A végleges közös csomag CI41670555328 SUCCESS, letöltött nyers411, nulla failure/error/skipped. Signed41671892820 és telefonos APK átadva; [bizonyíték](CURRENT_STATUS.md).

Készülékes elfogadás még nyitott: a jelzett videó és másik gyakorlat, Lejátszás, pause/resume, fullscreen, X/Bezárás és háttérbe lépés után nincs hang; beágyazási/hálózati hiba és a belső böngészős YouTube-oldal/consent használható. A pontos fekete-kép ok készülékes visszateszt nélkül nem tekinthető bizonyítottnak.

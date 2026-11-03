# Készülékes teszt – 2743 / PR #49

A telepített telefon- és Wear-verziót ellenőrizd: az új órás válaszhoz mindkét 2743-as APK szükséges.
A teljes Actions és 358 JVM/Compose-regresszió nem helyettesíti az alábbi fizikai próbákat.

- [ ] Mentett edzés részlete: Edzés törlése gomb, Mégse változtatás nélkül. Egy erre létrehozott rövid tesztedzést ments, majd erősítsd meg a törlést: naplóból/statisztikából és újranyitás után is eltűnik, más edzés/fotó/útvonal megmarad. Ez külön művelet az aktív session Elvetésétől.
- [ ] Világos/sötét témákban naptár minden panelje, napcellája, gombja és tervezője olvasható, témaváltás/újranyitás után is.
- [ ] Térkép App témája módban mindkét app-témát követi; kézi kapcsoló és automatikus módra visszatérés működik. Korábban kézzel mentett választás esetén az App témája gombbal állítsd automatikusra.
- [ ] Home órakártya / Kapcsolt eszközök órakártyája valódi akkumulátor/eszközadatot kér. Lekérés állapota, válasz és válaszhiány/timeout egyértelmű; nem indul edzés és nem változik ownership.
- [ ] Normál GPS-térkép alatt elférnek az adatok/vezérlők. Fullscreen idő, X, Szünet/Kör/Befejezés mind a térképen belül és a rendszer sávjai felett; visszatérés nem állítja le a mérést. Google-logó és térképvezérlők nem takaródnak.
- [ ] GPS-edzés alatt másik program kiválasztható anélkül, hogy a session címe/ID-ja/adatai megváltoznának. Telefonos elvetés után új GPS-edzés indítható, nincs closedOrStaleExecution. Tényleges ownership-átadáskor az eredeti védelmek megmaradnak.
- [ ] Google Cloud alapból összecsukva, fejlécre nyílik/becsukódik. Frissítés/engedély/megerősítés és érdemi hiba nem vész el becsukástól.
- [ ] Főoldalon hat Mai állapot cella ugyanazt mutatja, mint az Egészség oldal. Coach rövid, koppintásra a részletes oldalra visz. Normál betűméretnél elfér; nagy betűnél tartalom/gomb nem vész el.
- [ ] Importált 28 edzés megmarad a naplóban; mai/heti számláló csak a megfelelő helyi nap/hét edzéseit veszi. Heti cél hétfőtől máig. Mentés/törlés után helyes újraszámolás.
- [ ] Fejlődés: 1/7/30/90 napnál a volumen százaléka a közvetlen előző azonos intervallumhoz mér, növekedés/csökkenés/0% megfelelő. Pozitív előző volumen hiányában „—”; oszlopösszeg és kiválasztott időszak változatlan.
- [ ] Korábbi #38–#41: nincs ismétlődő Mai állapot mérési blokk; kisebb térközök, megfelelő oktatóvideó/külső megnyitás és elkülönült HC/Samsung adatok továbbra is működnek. A HC eredeti ürességének oka nem bizonyított.

Hibajelzés: tényleges verzió, érintett oldal, lépések, elvárt/tényleges eredmény, szükség esetén kép.
Teljes személyes egészségmentés nem szükséges.

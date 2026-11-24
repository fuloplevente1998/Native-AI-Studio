# Kompakt nézet, felugró ablakok és WebView-videó – build nélküli munka

A felhasználó a mostani módosításokhoz kifejezetten nem kér Android buildet/APK-t. Az előző, d69c77e forrású 2744-es APK és annak411 JVM +7JS tesztje történeti kiadás; az itt leírt új Kotlin-módosítások nem tekinthetők azzal lefordítottnak vagy készüléken elfogadottnak. A commitok `[skip ci]` jelölése a kért build nélküli kör miatt szerepel.

## Felületi változások

- A naptári napra koppintás lebegő dátumablakot nyit; nincs állandó napműveleti kártya a hónap alatt. Hozzáadás, módosítás, áthelyezés, törlés, napló és Undo a meglévő callbackeken működik. A havi tervező külön Tervezés ablakba kerül, az alapnézetben rövid tervösszefoglaló és Undo marad. Az adaptív cellamagasság és kisebb térköz normál telefonméretnél a hónap/összefoglaló elférését célozza; kis ablak/nagy betű esetén a hozzáférhető görgetés megmarad.
- Statisztika: a két alfület ismétlő cím és a felesleges leíró fejléc eltávolítva; kisebb kártyapadding és közök, az értelmezési magyarázat külön lenyitható. A gyakorlatcím legfeljebb két sor, a kattintási felület legalább48dp.
- Közös kártyaköz8→6dp; a programeditor ismételt címe és a Mai állapot fölösleges alcíme kivezetve. A tartalom/vezérlés nem helyettesül hamis adattal.
- Kezdőlap: nagyobb Coach-kártya rövid tanáccsal, meglévő készenléti pontszámmal és részletes Coach-belépéssel. Gyorsedzés és óra normál szélességen egymás mellett; szűk/nagybetűs nézetben tördel. A hat mai kártya és a Folytatás/Elvetés megmarad. Finom, statikus témaszínű árnyék/átmenetes keret, nem új animált navigáció.
- Közös TrainPilotAlertDialog49 meglévő híváshoz29 fájlban: kompakt padding, témaszínek, finom erős külső keret, árnyék, nagybetűnél tördelő gombsor; a hívó megerősítési/mentési/busy/bezárási védelmei megmaradnak. A meglévő görgethető űrlapokat nem tesszük még egy végtelen magas görgetőbe. A Studio témája a natív részkomponensek színeit is megadja. A konkrét edzés-egészségadat ablak két forrásválasztója egymás mellé kerül, a hosszú technikai szöveg megszűnik.

## Videó: ellenőrzött régi referencia

A felhasználó új képe: a köztes JavaScriptes Play indítást a YouTube blokkolta; a belső mobiloldal oldalirányban levágott. Referencia: `fuloplevente1998/TrainPilot` main5f026a56b9b6d33c84afa77eb3927b11f0bd6f6a, `www/app.js:11297–11308` közvetlen www.youtube.com/embed iframe, playsinline/rel=0 és a YouTube saját vezérlője; a végső forrásellenőrző override az ellenőrzött videót ehhez az alaphoz delegálja. A natív külső fallback ott ACTION_VIEW, ezt nem hozzuk vissza automatikus külső appnyitásként.

A lockolt és helyben olvasott Capacitor Android7.4.3 Bridge.java:590 egyaránt `setMediaPlaybackRequiresUserGesture(false)` beállítást használ. Az új, appon belüli megoldás ehhez igazítja az ellenőrzött saját iframe-et: autoplay=0, nincs saját Play-réteg vagy playVideo-hívás; az API csak állapotot figyel és háttérben szüneteltet. A megfigyelő API hiánya nem teszi hibássá az önmagában működő iframe-et. Bezárás/háttér esetén API előtti pause-parancs is van. HTTPS, engedélyezett videó-ID, tiltott natív bridge/fájl/jogosultságkérés és SSL-hibánál megszakítás megmarad.

A böngészős mód külön, a teljes fennmaradó modal-területet használja; a vezérlők azon kívül maradnak. Wide viewport, automatikus áttekintő méret, fix100% webes szövegméret és böngészőben nagyítás segít az oldal levágása ellen. A videóazonosító és a kapcsolódó edzésállapot változatlan.

## Ellenőrzés és következő elfogadás

`git diff --check` és a tényleges inline JS7 gyors eseményteszt sikeres. Kotlin forrásparser a módosításoknál új szintaktikai hibát nem jelzett; a csak importban változott, korábban is grammar-hibás törzseket a kiinduló forrással összehasonlítottuk. Ez nem típusellenőrzés/Android fordítás. A naptár/statisztika UI regressziók elvárása frissítve, JVM-tesztek és APK nem futottak/készültek a felhasználó kérésére. A régi411 eredmény nem bizonyítja ezt a kódot.

Későbbi közös builddel: normál393dp telefon és hathetes hónap, Undo és hosszú programnév; üres/tervezett/teljesített nap lebegő ablak, szerkesztés/áthelyezés és bezárás; Tervezés/Újratervezés; nagy betű/kis ablak. Statisztikai és Home-kártyák láthatósága, összes belépő működése, világos/sötét téma és modal-megerősítések. Videó valódi lejátszása, saját vezérlő, fullscreen, pause, X/háttér utáni hang, belső böngésző szélessége. A videó készülékes elfogadása még nyitott.

## Új felhasználói irány

A Samsung-energiapontszám az órán Samsung Health megnyitása után megjelent a telefonos TrainPilotban. Ez szinkronkésésre utal, nem igazolt tartós engedélyhiba; a mérés nincs kézzel pótolva.

A mostani felület után a teljes metrikalista és a gyakorlatonkénti visszajelzés következik. Automata/intervallum további bekötés #74 sztornó, a meglévő működő időzítő/motor nem törlendő. BLE szintén sztornó. Az adatvédelmi tájékoztató a régi mainből később a Beállítások / Adatvédelem részbe kerüljön, most csak rögzítés.

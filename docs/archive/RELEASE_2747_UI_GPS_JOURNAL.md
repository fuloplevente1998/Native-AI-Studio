# TrainPilot 2747 – három készülékes UI-visszajelzés

2026. november 3-i Android 16-os 2746-os screenshotok és #87/#88 alapján.

## 1. Izomcsoport-terhelés (#87)
A kártya külső függőleges paddingja 10 -> 6dp, a fejléc alatti 4dp spacer megszűnt. A jobb felső 3 hó/30 nap/7 nap/1 nap kiválasztó **48dp -> 41dp vizuális méretű**, de a kattintható Box **48x48dp** marad; az időszakváltás logikája változatlan.

## 2. Kültéri GPS fullscreen (#88)
A korábbi Dialog `fillMaxSize` feltételezte, hogy a Window mindig pontosan a kijelző látható keretén belül van. A készülékes képen ez nem teljesült, és a start gomb lelógott.

Az új `outdoorVisibleViewportPx` a Dialog Compose root magasságát, a View `getLocationOnScreen` helyét, `getWindowVisibleDisplayFrame` látható keretét és az Android valódi navigációs alsó insetjét használja. A dashboardot a szűkebb *safe viewportba* méretezi; a térkép kapja a fennmaradó helyet, a metrika- és akciósor a lekicsinyített nézet aljához igazodik. A 12dp tartalék elkerüli a gesztusérintést. A motor, GPS-rögzítés, időmérés, Kör és Befejezés változatlan.

A Robolectric teszt a látható keret számítását és a tényleges Dialog-Start gomb elhelyezkedését is ellenőrzi. Ettől függetlenül az Android 16 tényleges rendszerinseteit **készüléken is újra ellenőrizni kell**.

## 3. Napló – ikonok sorrendje
A teljes naplókártya koppintásra továbbra is a külön Részletek képernyőre navigál, ezért a jobb oldali díszítő nyíl megszűnik. Az útvonalat tartalmazó futóedzéseknél a 48dp térképgomb a tartalom után, a 48dp érintési területű törlés *előtt* helyezkedik el. Minden naplóelem jobb szélén a törlés van. A törlés megerősítése változatlan.

## Release 2747 ellenőrzés
- [ ] CI: app/Wear debug build, összes JVM, videó JS, JUnit XML regresszió
- [ ] Folyamatos signing: azonos `com.TrainPilot.com`, 2747, korábbi release cert, Maps
- [ ] Android 16 valódi 393dp készülék: indulás előtti start / futás alatti pause, lap, finish
- [ ] A Naplóban útvonal nélküli és útvonalas sorra kattintás, törlés megerősítéssel
- [ ] Fejlődés fejléc és 41dp vizuális időszakválasztó, 48dp érintés

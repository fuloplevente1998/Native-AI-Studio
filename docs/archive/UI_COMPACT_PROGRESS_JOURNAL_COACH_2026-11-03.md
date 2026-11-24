# Mobil UI: sűrített Fejlődés / Napló, hangsúlyos következő Coach edzés

Forrás: [#84](https://github.com/fuloplevente1998/Native-AI-Studio/issues/84), felhasználói 2745-ös készülékképernyők (2026-11-03).

## 1. Fejlődés
- Izomcsoport-terhelés alacsonyabb belső paddinggel, a két anatómiai ábra megtartásával.
- Hat izomcsoport felirata egy sorban marad: a korábbi 34dp-s címkeszélesség betűnkénti sortörést okozott a keskeny jobb oldali oszlopban.
- Kisebb sorok és közök; az értelmező figyelmeztetés röviden látszik, a teljes szöveg egy koppintással megnyitható.
- A Fejlődési javaslat ugyanott marad, az immár kisebb izomkártya után; a legkisebb kijelzőkön/nagy betűknél a görgetést nem szabad letiltani.

## 2. Napló
- 48dp magas, továbbra is szerkeszthető kereső; billentyűzet Search akció.
- Kisebb bal ikon, keskenyebb kártyapadding, három szövegsor: dátum/idő, programnév, időtartam + sorozat + kilogramm.
- Futásnál nem koholunk súlyzós volumenadatot: idő + táv + tempó látszik, elérhető az útvonal megnyitása.
- A törlő ikon vizuálisan 48dp -> 41dp, de az érintési felület továbbra is 48dp; törlési megerősítés és részletes nézet nem módosult.
- A részletekben maradnak az elhagyott gyakorlat-előnézetek és PR-adatok; a lista teljes eleme a részletekre nyit.

## 3. Coach
- A kanonikus célhoz a program tényleges, nyelv szerinti neve kerül; nem a naptár vagy a pontszámok helyi másolatából állítjuk elő.
- A következő edzésnap kiemelt színű kártya, nagy napjel, dátum és egyértelmű főművelet.
- A kiemelt kártya a készenléti blokk után, a Kulcsjelek elé kerül; a korábban lent elhelyezett javaslatlistában nem duplikálódik.
- A naptárból tervezett edzés esetén változatlanul naptárra navigál, az indítható programnapnál pedig a tényleges program- és napazonosítót kapja az indítás.

## Ellenőrzés
- [ ] GitHub Actions Android/Wear compile és JS/JVM regression.
- [ ] Hat magyar izomfelirat egysoros és kompakt kártyamagasság.
- [ ] Naplókereső gépelhető, kompakt naplókártya megnyitható/törölhető.
- [ ] Coach programnév/badge valós adatból, egyetlen target kártya, tervezett nap naptárra lép.
- [ ] Valódi 393dp-s készüléken napló 5–6 bejegyzés és a Fejlődési javaslat görgetés nélküli megjelenésének ellenőrzése (a JVM képernyőteszt nem helyettesíti).

Ez **UI-fejlesztési PR**, nem aláírt APK-release; a 2745-ös korábbi kiadást nem írja felül.

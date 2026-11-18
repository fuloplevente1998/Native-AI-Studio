# #267 — Programok fejléc és értesítési státuszikon

Alap main0a0beb9 / kiadott2765-r2.

- A Programok cím és X gomb ugyanazt a ScreenGutter belső margót kapja,
  mint a lista tartalma. SubScreenTopBar opcionális modifiert fogad; a többi
  oldal alapelrendezése nem változik. A rossz helyre vivő fejlécbeli+ eltávolítva.
  A saját program létrehozásának meglévő műveletei megmaradnak.
- A launcher már TrainPilot, de a telefon négy foreground/edzésértesítési
  útja még R.mipmap.ic_launcher legacy ikont adott. Most külön ic_stat_trainpilot
  fehér alfa-maszkot használ a két intervallumértesítés, a GPS-szolgáltatás és
  az egészségháttér-szolgáltatás. Geometriája a meglévő TrainPilot súlyzóé,
  átlátszó háttérrel, launcher-csempe és régi kék/fehér színezés nélkül.
  Csatorna, értesítési ID, PendingIntent, vezérlés és lifecycle változatlan.

Új renderelt regresszió: látható fehér súlyzó, átlátszó sarkok és szabad háttér;
az igazi foreground ARM-teszt a kibocsátott Notification smallIcon erőforrását
is ellenőrzi. A meglévő UI/élőmérés/835 regresszió változatlanul fut.
Első teljes CI44157967188, forrásd98a7dc36c5ba71824164e60dedcf624cbe1d020:
836 esetből835 sikeres,1 hiba. Az új renderelt ikon és a tényleges értesítés
ellenőrzése sikeres. A régi FirstRunPlannerRegressionTest closingPlanner esete
az edit-profile kattintás után timeoutolt: a PROFILE panel már megjelent,
de a DataStore-írás finally ága még nem engedélyezte a gombot, a túl korai
kattintás így jogszerűen hatástalan volt. A teszt most a valóban engedélyezett
gombot várja meg, és ugyanúgy ellenőrzi a valódi panelt/mentetlenséget.
Produkciós onboarding kódot nem módosítottunk.

2026-11-17: felhasználó további kisebb javítások gyűjtését és a CI halasztását
kérte. Nincs újrafuttatás vagy új build; a teszt-szinkronizálás javítása még
nem ellenőrzött CI-ben. A további commitok [skip ci] jelölésűek; a teljes kapu
csak új, kifejezett felhasználói kérésre indul.
Nincs még új main-integráció, APK/AAB vagy verzióemelés; fizikai SystemUI
megjelenítés a következő készülékes ellenőrzés.

# 2773 – gyűjtött javítások

Kódolás csak „mehet” után, egy közös CI-körrel.

| # | Terület | Visszajelzés | Terv | Állapot |
|---|---------|--------------|------|---------|
| 1 | Kezdőlap – tervezett edzés kártya | Plusz sor: milyen dátumra van tervezve, következő vagy aktuális | Új sor a kártyán: „📅 Ma, 18:00” / „📅 Ma, 10:00 · még ma elvégezhető” (ha az időpont elmúlt) / „📅 Következő: csüt., nov. 21. · 10:00” / „↻ Következő a sorban” (nincs naptári időpont). Forrás: a kiválasztott naptárbejegyzés `startIso`-ja (`HomeWorkoutTarget` bővül) | kódolva |
| 2 | Mai tervezett edzés megtartása | 10:00-ra tervezett mai edzés 10:01 után is maradjon a nap végéig, éjfél után ugorjon a következőre | Kiválasztás már a nap elejétől keres (elmúlt időpont ma nem ugrik tovább) – regressziós teszttel rögzítjük. Hiányzik: éjfélkor nincs frissítés → napváltáskor (helyi éjfél) újraszámolás a Kezdőlapon és a Coachban | kódolva |
| 3 | Edzés fül – Gyakorlatlista gomb | Kell egy Gyakorlatlista belépő (3 csempe alá vagy legalulra) | A) a 3 csempe alá (javasolt) vagy B) legalulra az Edzés beállításai alá: „📚 Gyakorlatlista · 140 gyakorlat” → ugyanaz a lista lapon, böngészés/kezelés indítás nélkül (részletek, videó, kedvenc, új gyakorlat) | kódolva (A, „Gyakorlatkönyvtár”) |
| 4 | Programlista ⋮ menü | Aktiválás és Szerkesztés felesleges (kint van / koppintásra nyílik) | ⋮ lap: csak Átnevezés, Másolás saját tervként, Törlés. Aktiválás a kártya gombján, szerkesztés kártyára koppintva – bekötés változatlan | kódolva |
| 5 | Gyakorlatkönyvtár – régi külön oldal | Edzés közben (gyakorlat cseréje) a régi teljes oldalas könyvtár nyílt, nem a felcsúszó | Belépők, amik még a külön oldalt nyitják: (a) aktív edzés – gyakorlat cseréje (erő/időzített/kétoldali), (b) Programok lista – „Gyakorlatválasztó (140)” gomb, (c) programszerkesztő – „+ Gyakorlat hozzáadása”. Javítás: az `EXERCISE_LIBRARY` útvonal is alulról nyíló lap lesz (`TrainPilotSheetRoutes`), ugyanazokkal a műveletekkel (csere / hozzáadás / böngészés); a lap alatt a kiinduló képernyő marad. Már lapon: Gyors edzés, Edzés fül csempék | kódolva |
| 6 | Könyvtár csere módban | Edzés közbeni cserénél a gomb „Indítás” volt | Csere módban „⇄ Csere” felirat és ikon (`exerciseSwapMode`), művelet változatlan (`selectLibraryExercise` → csere) | kódolva |
| 7 | Coach – következő edzésnap | A dátum-sor máshol is (pl. Coach) | Coach kártyán a nyers dátum helyett ugyanaz a sor, mint a Kezdőlapon (`homePlannedDateLine`) | kódolva |
| 8 | Edzés fül – Jelenlegi program | Dátum itt is; van hely | A ténylegesen következő nap kiemelve (eddig mindig az 1. nap), alatta ugyanaz a dátum-sor | kódolva |

# 2773 – gyűjtött javítások

Kódolás csak „mehet” után, egy közös CI-körrel.

| # | Terület | Visszajelzés | Terv | Állapot |
|---|---------|--------------|------|---------|
| 1 | Kezdőlap – tervezett edzés kártya | Plusz sor: milyen dátumra van tervezve, következő vagy aktuális | Új sor a kártyán: „📅 Ma, 18:00” / „📅 Ma, 10:00 · még ma elvégezhető” (ha az időpont elmúlt) / „📅 Következő: csüt., nov. 21. · 10:00” / „↻ Következő a sorban” (nincs naptári időpont). Forrás: a kiválasztott naptárbejegyzés `startIso`-ja (`HomeWorkoutTarget` bővül) | kódolva |
| 2 | Mai tervezett edzés megtartása | 10:00-ra tervezett mai edzés 10:01 után is maradjon a nap végéig, éjfél után ugorjon a következőre | Kiválasztás már a nap elejétől keres (elmúlt időpont ma nem ugrik tovább) – regressziós teszttel rögzítjük. Hiányzik: éjfélkor nincs frissítés → napváltáskor (helyi éjfél) újraszámolás a Kezdőlapon és a Coachban | kódolva |

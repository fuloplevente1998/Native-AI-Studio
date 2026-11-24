# 2782 – gyűjtő (1.4.0 / 278100 után)

| # | Hol | Visszajelzés | Javaslat | Állapot |
|---|-----|--------------|----------|---------|
| 1 | Fejlődés – PR rekordok | Még mindig 0 (33 mentett edzésnél) | Ok keresése: régi mentett edzéseknél hiányzó PR-adat (`prRecordsJson226`) vagy a számítás nem fut; ha hiányzik, a mentett sorozatokból számolni (adat nem módosul) | kész – a mentési út számol PR-t, de régi/órás mentéseknél üres lehet a tárolt lista; ilyenkor a mentett sorozatokból ugyanazzal a szabállyal (korábbi legjobb megdöntése) |
| 2 | Napló – edzés részletei lap (Gyakorlatok / Statisztikák / Forrás) és hasonló füles lapok | Fülváltáskor ugrál a lap magassága | Füles alsó lapok fix (legnagyobb / közel teljes) magasságon, a tartalom görget a lapon belül; ugyanez a többi füles lapon | kész – Napló edzés részletei, Gyakorlatkönyvtár, Gyakorlat részletei, Gyors edzés lap teljes magasságon |
| 3 | Statisztikák – Kardió | Rendezzük a Lépésekhez hasonlóan | Szűrő chipek; 3 csempe (Ez a hét / Előző hét / Összesen: km + edzés); 8 hetes koppintható grafikon fix helyű kijelölt sorral (táv, edzés, idő, tempó); rekordok 2 sor + kinyitható | kész |
| 4 | Statisztikák – Erő | Gyakorlatonként lenyitva fejlődési görbe | Marad így | marad |
| 5 | Alulról nyíló lapok | Bezáráskor néha a Kezdőlapra ugrik | Minden lap bezárása oda térjen vissza, ahonnan nyitották (fül + allap); mindegyik lap ellenőrzése, regressziós teszt a visszatérésre | kész – ok: dupla bezárás (rendszer-vissza + lap) a második lépéssel a Kezdőlapra vitt; most csak az épp nyitott lap záródik |
| 6 | Gyakorlatkönyvtár – új gyakorlat | „African curl” (TikTok videó) | Álló, kézisúlyzós, két kézzel egyszerre bicepsz curl, alsó/felső félismétlések + teljes ismétlés kombinálva (a „21-es” curlhöz hasonló). Új gyári gyakorlat 4 nyelven, izom: bicepsz, típus: kézisúlyzó, ismétlés; katalógus 140→141 + teszt | elhalasztva (felhasználói döntés) |

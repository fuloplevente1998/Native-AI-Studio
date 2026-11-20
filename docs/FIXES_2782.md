# 2782 – gyűjtő (1.4.0 / 278100 után)

| # | Hol | Visszajelzés | Javaslat | Állapot |
|---|-----|--------------|----------|---------|
| 1 | Fejlődés – PR rekordok | Még mindig 0 (33 mentett edzésnél) | Ok keresése: régi mentett edzéseknél hiányzó PR-adat (`prRecordsJson226`) vagy a számítás nem fut; ha hiányzik, a mentett sorozatokból számolni (adat nem módosul) | vizsgálat |
| 2 | Napló – edzés részletei lap (Gyakorlatok / Statisztikák / Forrás) és hasonló füles lapok | Fülváltáskor ugrál a lap magassága | Füles alsó lapok fix (legnagyobb / közel teljes) magasságon, a tartalom görget a lapon belül; ugyanez a többi füles lapon | javaslat |
| 3 | Statisztikák – Kardió | Rendezzük a Lépésekhez hasonlóan | Szűrő chipek; 3 csempe (Ez a hét / Előző hét / Összesen: km + edzés); 8 hetes koppintható grafikon fix helyű kijelölt sorral (táv, edzés, idő, tempó); rekordok 2 sor + kinyitható – mockup | mockup |
| 4 | Statisztikák – Erő | Gyakorlatonként lenyitva fejlődési görbe | Marad így | marad |
| 5 | Alulról nyíló lapok | Bezáráskor néha a Kezdőlapra ugrik | Minden lap bezárása oda térjen vissza, ahonnan nyitották (fül + allap); mindegyik lap ellenőrzése, regressziós teszt a visszatérésre | kódolandó |

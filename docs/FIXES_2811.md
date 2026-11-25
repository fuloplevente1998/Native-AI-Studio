# 2811 – gyűjtő (1.5.1 · 280700/280702, 2810 build után)

Eszközös visszajelzés a 2810 buildről (13 képernyőkép, glass téma). Kódolás csak „mehet” után, egy közös CI-körrel; UI-változás előtt mockup.

| # | Téma | Hol | Megfigyelés / javaslat | Állapot |
|---|------|-----|------------------------|---------|
| 1 | Felső nav/fejléc nem átlátszó | Minden fül | Legtöbb képen laposan szürkéskék/sötét sáv; csak ott színes (kék), ahol a háttér kék foltja éppen alatta van. Valószínű ok (hipotézis, kódban még nem igazolt): a fejléc csak a háttérréteget látja, tetején nincs alatta tartalom, és a fénysűrűség-plafon sötétre vágja. Javaslat: fejlécnek saját, magasabb plafon + a felső háttérfolt mindig a fejléc alatt marad; mockup előbb | felmérésre |
| 2 | Még tömör (nem üveg) felületek | Naptár napcellái és Újratervezés/Tervezés gombok; Kezdőlap „Következő edzésnap” és „Mai javaslat”; Edzés közben: edzéstest-panel, „Vezérlés: telefon” sáv, pihenőidő panel; Fejlődés: izomcsoport-terhelés, kardió/lépés nagy kártya + napcsempék, alsó javaslat-lap; Egészség: Heti célok, Test és fittség belső csempék; Beállítások: Google Cloud sor | Ezek egyszínű sötét kitöltésűek, a háttér nem látszik át | felmérésre |
| 3 | Edzés közben a test-panel elzárja a hátteret | Aktív edzés | A teljes sorozat-terület egy nagy, tömör, sötét lekerekített panel; csak a sorozatsorok üvegesek | felmérésre |
| 4 | Telítettség/olvashatóság | Programlista, Napló | A kártyák erős piros/zöld/lila színt kapnak; olvasható, de a foltok nagyon dominálnak. Javaslat: foltok telítettsége állítható / alacsonyabb alap | felmérésre |
| 5 | Teljesítmény (optimalizálás) | Minden glass felület | Minden kártya külön elmosást számol. Javaslat: a háttér egyszer elmosva, a kártyák ezt mintázzák + tónus + perem; animációs órajel csak látható képernyőn. Készüléken mérni (frame time) előtte-utána | javaslat |
| 6 | Óra – glass témánál alig olvasható | Wear | Változatlan, a 2810/1 sor átvéve | felmérésre |

# 2808 – gyűjtő (1.5.1 · 280700/280702 után)

| # | Téma | Hol | Megfigyelés / javaslat | Állapot |
|---|------|-----|----------|---------|
| 1 | Óra kezdőlap pulzussor villog | Wear kezdőlap (v3) | Videó 2026-11-24 22:46: a sor kb. 0,5 mp-enként váltakozik „♥ 87 bpm · Korábbi adat” és „♥ – · Mérés bekapcsolva” között. Egy állapot legyen: élő érték ha van friss minta, különben az utolsó érték időbélyeggel; a „Mérés bekapcsolva” csak ikon/szín, ne cserélje a számot | felmérésre |
| 2 | Lépés-ív 0% | Wear kezdőlap jobb ív | Este 22:46-kor 0% – valószínűleg nincs napi lépésadat az órán (passzív adat / engedély / telefonról jövő érték). Hiányzó adatnál „–” legyen, ne 0%; ellenőrizni a napi lépés forrását és az ACTIVITY_RECOGNITION engedélyt | felmérésre |

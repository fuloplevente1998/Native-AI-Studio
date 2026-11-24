# 2808 – gyűjtő (1.5.1 · 280700/280702 után)

| # | Téma | Hol | Megfigyelés / javaslat | Állapot |
|---|------|-----|----------|---------|
| 1 | Óra kezdőlap pulzussor villog | Wear kezdőlap (v3) | Videó 2026-11-24 22:46: a sor kb. 0,5 mp-enként váltakozik „♥ 87 bpm · Korábbi adat” és „♥ – · Mérés bekapcsolva” között. Egy állapot legyen: élő érték ha van friss minta, különben az utolsó érték időbélyeggel; a „Mérés bekapcsolva” csak ikon/szín, ne cserélje a számot | felmérésre |
| 2 | Lépés-ív 0% | Wear kezdőlap jobb ív | Este 22:46-kor 0% – valószínűleg nincs napi lépésadat az órán (passzív adat / engedély / telefonról jövő érték). Hiányzó adatnál „–” legyen, ne 0%; ellenőrizni a napi lépés forrását és az ACTIVITY_RECOGNITION engedélyt | felmérésre |
| 3 | Kezdőlap ívek | Wear kezdőlap (v3) | Az akku- és lépés-ív a lünetta mentén, a kijelző peremével koncentrikusan fusson (most beljebb, rövid szakaszon). Fotó 22:53 | felmérésre |
| 4 | Ív alatti %-jelzők | Wear kezdőlap | „🔋64%” és „🚶0%” rosszul ül: az ikon az ív végébe lóg/levágódik. Az ív végén belül, olvashatóan, ütközés nélkül; hiányzó lépésadatnál „–” | felmérésre |
| 5 | Pihenő – Visszavonás sáv | Wear pihenő képernyő | Az alsó széles „Visszavonás” sáv edzés közben levágódik, takarja a gyűrűt és a gombokat. Kisebb, a kör alakhoz illő gomb vagy a vezérlő-overlay/lap része legyen. Fotó 22:51 | felmérésre |
| 6 | Világos témák az órán | Wear minden felület | Világos témánál a felületek szinte felismerhetetlenek (videó 22:46: kimosott, alacsony kontraszt). Az órán sötét alap vagy kontrasztőr: szöveg ≥4,5:1, vezérlők ≥3:1 | felmérésre |
| 7 | Eltűnt kezdőlapi gomb | Wear kezdőlap | A korábbi kezdőlapról eltűnt egy gomb („Befejezés” vagy hasonló – ellenőrizni a régi kezdőlap kódjában, pl. aktív edzés folytatása/befejezése vagy mentés/szinkron). Visszaállítani a v3 elrendezésbe | felmérésre |

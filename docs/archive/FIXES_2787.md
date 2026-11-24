# 2787 – gyűjtő (1.4.1 / 278600 után)

Mockup: [`docs/mockups/2787_health_weekly_goals.png`](mockups/2787_health_weekly_goals.png)

| # | Hol | Visszajelzés | Javaslat | Állapot |
|---|-----|--------------|----------|---------|
| 1 | Egészség – Heti célok lenyitva | „Edzések ezen a héten” kétszer (fejléc csík + külön kártya); lenyitva a kompakt résznek kell nagy nézetté alakulnia | Lenyitáskor a kompakt két csík maga alakul át nagy nézetté (fejlécben csak cím, ⌃, ⚙); nincs külön összegző csík és külön kártya | kódolva, tesztelésre vár |
| 2 | Egészség – Heti célok lenyitva | Lépés kétszer (fejléc % + „Heti lépésszám” kártya) | A lépés csíkból „Lépések” nagy rész: összeg / cél · %, csík, mért napok, koppintható napi oszlopok, infósor napi céllal | kódolva, tesztelésre vár |
| 3 | Egészség – Aktív edzésidő | Mutassa, hány edzés és összesen mennyi idő | Az edzés csíkból „Edzések” nagy rész: „2 / 3 edzés · 136 perc összesen”, csík, koppintható napi oszlopok (perc), alatta az edzések listája (dátum, név, perc) – valós Room-napló | kódolva, tesztelésre vár |
| 4 | Egészség – Heti célok alja | „Célok beállítása” sáv felesleges, a ⚙ gomb ugyanazt nyitja | Alsó gomb törlése | kódolva, tesztelésre vár |
| 5 | Heti célok – lépés % | 83 273 / 70 000 mellett „100%” látszik | Valós % (pl. 119%), a csík tele marad | kódolva, tesztelésre vár |
| 6 | Egészségnapló kártya | Az ikon furcsán néz ki | A többi kártyához igazított kerek ikon, napló jelkép (nem szív) | kódolva, tesztelésre vár |
| 7 | Testsúlynapló fejléc | (mockupban észrevett) cím és alcím levágódik | „Testsúlynapló”, alatta „64,2 kg · dátum” | kódolva, tesztelésre vár |

| 8 | Edzés fül – Erő / Időzített / Kültéri csempék | A három csempe feliratai még levágódnak | Meglévő TrainPilotFitText dinamikus betűméretezés újrahasznosítása; egy sor előnyben, sortörés csak szükség esetén; kis képernyő és nagy betűméret tesztje | dinamikus betűméret kódolva, tesztelésre vár |

## Kiadási ellenőrzőlista – PR #303

A javításokhoz regressziós tesztek kerültek a kódba, de futtatásuk és a teljes build még nem igazolt. A PR draft marad, amíg a CI és az eszköztesztek nem zöldek.

- [ ] A nyolc UI-javítás implementálva és tesztelve (a korábbi #2776 csempejavítás regresszióját is ellenőrizni).
- [ ] Android és Wear OS build/tesztek zöldek; verziókódok egyediek, verziónév egyezik.
- [ ] PR #303 csak ezután kerülhet `main` ágba.
- [ ] Aláírt APK/AAB artefaktumok és ellenőrzött metaadatok, majd Drive-feltöltés.
- [ ] Play Console „Mi az új?” lokalizált `<hu-HU>` és `<en-US>` blokkokkal; csak valós változásokkal.
- [ ] Play Áruház rövid/hosszú leírásának felülvizsgálata és szükség szerinti frissítése.
- [ ] `fuloplevente1998/TrainPilot.io`: kiadott verzió, újdonságok, funkcióleírás és letöltési hivatkozások szinkronizálása.
- [ ] Weboldal és Play listing ellenőrzése, kiadási átadó frissítése.

# Liquid Glass – #350 készülékes visszajelzés utáni új irány

2026-11-24: a 70–90%-os saját téma fedettségcsúszkája nem ad érzékelhető üveghatást. A saját témák ismét tömör felületeket használnak; a régi rekordok színei és UUID-ja megmaradnak, az opacity 100-ra normalizálódik.

## Tervezett ötödik gyári család (látványterv előtti javaslat)
- Az eredeti 1–32 paletta és kulcsa változatlan; 8 új, önálló Liquid Glass téma, javasolt ID 33–40, külön kulcsok.
- Egyetlen családszintű, élőben állítható üvegcsúszka, csak a Liquid Glass blokkban. Értéke tartós, más családokra semmilyen hatása nincs.
- Javasolt skála: 0 = Frosted (erősebb szórás/homály, több fedés), 50 = köztes, 100 = Clear Liquid (kevesebb homály, erősebb áttetszés, peremfény és optikai hatás). A 100 nem jelent láthatatlan gombot vagy szöveget.
- Javasolt 8 tónus: grafit, ezüst/jég, zafír, türkiz, smaragd, ametiszt, rózsaszín, borostyán. A végleges háttér/akcentus párok látványterv alapján.
- Anyag: egyetlen közös glass token/réteg, kártya, felső/alsó sáv, pill, gomb, kijelölés, modal; nem globális alpha az egész komponensre. Szöveg/ikon opak, normál szöveg >=4,5:1, használathoz szükséges kontúr >=3:1. Biztonsági, piros/zöld és aktív edzés vezérlők olvashatósága elsődleges.
- A valódi backdrop blur és fénytörés külön renderelési munka, nem azonos egy alpha csúszkával. Androidon az iOS anyag nem natív; hasonló megjelenés saját Compose rétegekkel érhető el. RenderEffect önmagában csak a saját renderelt tartalmat homályosítja; mögöttes tartalomhoz külön réteg és mintavétel szükséges.
- Mozgás: finom rugós nyomás/felengedés, kijelölő pill animáció, átmenetek; csökkentett mozgás támogatása, aktív edzés és régi/gyengébb eszköz esetén költséges hatások elhagyása. Wear: opak effektív paletta, üveg/blur nélkül.
- Teszt: 32 régi paletta változatlan, 8 új azonosító egyedi, csúszka csak új családnál, újraindítás/restore, 0/50/100 vizuális eltérés, kontraszt, frame time, Samsung készüléken és Android verzió szerinti fallback.
- Következő: látványterv jóváhagyása (8 tónus, két végpont, főképernyő és aktív edzés), majd külön célzott implementáció. Ez a dokumentum nem állít kész glass renderelést.

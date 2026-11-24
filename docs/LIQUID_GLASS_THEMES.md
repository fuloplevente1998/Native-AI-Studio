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

## Elfogadott irány és jelenlegi implementáció (2026-11-24)
- Felhasználó jóváhagyta az önálló TrainPilot Glass Engine irányt. Nem vendorizálunk Kyant forrást, shaderkódot vagy függőséget; csak általános optikai elvek és hivatalos Android/Compose API-k alapján, önálló kóddal dolgozunk. Ez nem jogi garancia minden esetleges IP-igény ellen.
- A 33–40 paletta és a családszintű 0–100 slider megvan kódban; külön DataStore érték, hordozható mentés, Wear opak contrast fallback. Az eredeti 32 paletta az allPalettes-ben változatlan, a selectablePalettes tartalmazza mind a 40-et.
- Saját téma: kompakt színkerék és kétdimenziós SV mező, HEX, opcionális natív finombeállítás és gyorsszínek. A korábbi törlődialógus/opacity javítások megmaradtak.
- TrainPilotGlassEngine.kt: eredeti AGSL kontrollált háttérshader, többpontos elmosás, peremeltolás, fényperem; cache-elt shader, régi/szoftveres renderelés fallback; aktív edzés alatt költséges effekt tiltva. Kijelölt Home/Progress/Health/Settings kártyák, top/bottom nav, modális belső panel és közös gomb/chip perem.
- GlassContentBackdrop.kt: külön, helyi Compose GraphicsLayer forrás + lencseréteg, API33 RuntimeShader RenderEffect, API31–32 blur, régebbi opak fallback, nincs bitmap readback vagy ablakrögzítés. Az élő minta mögött valóban görgethető grafikai tartalom van.
- A tényleges, görgetett alkalmazástartalom teljes körű élő mintavétele még NEM kötött: fő felületeken a kontrollált procedurális háttér van. A modális külön ablakok, valódi tartalom a lebegő nav mögött, pill/nyomás rugós animáció, teljes komponensaudit és GPU-teljesítmény még hátra. Az élő renderer fizikailag készüléken nem igazolt.
- Új tesztek: LiquidGlass350Test (paletta-azonosítók, kontraszt, optikai paraméterek, színkerék geometria); CustomThemeStore350Test bővítve tartós slider/restore/Wear esetekkel. Fordítás és tesztfuttatás még nincs; a korábbi #629 APK régi állapot.
- Hivatalos API-referencia: https://developer.android.com/reference/android/graphics/RuntimeShader ; https://developer.android.com/reference/android/graphics/RenderEffect ; https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/layer/GraphicsLayer .

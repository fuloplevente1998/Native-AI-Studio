# Kyant Liquid Glass – elfogadott integrációs terv (2026-11-25)

## Cél és hatókör
A felhasználó jóváhagyta: az aktuális mainből induló egyetlen Kyant üveganyag-rendszer.
A készüléken gördülékeny #378 Kyant APK a motorreferencia, a #380 üvegminősége és a korábbi mockupok
a vizuális referencia; #379 széles körű átlátszósági bekötései és #386 kompatibilis javításai átvehetők.
Nem párhuzamos renderelőmotorok, nem ágak vak merge-je. Egy központi anyag API, szerepenként eltérő
festés/blur/lencse/perem; az eredeti saját motor kizárólag kompatibilitási fallback, nem választható második motor.
A mockupok adatait, nem létező funkcióit nem másoljuk. A korábbi 18 készülékes kép hibajegyzéke az elfogadás része.

## Anyagok
| Szerep | Cél |
| --- | --- |
| Scene | Egy közös, látható háttér ablakonként; élő színes háttér vagy privát felhasználói kép |
| Navigation | Erősen áttetsző, a háttérrel folytonos fejléc; görgetett tartalmat mintavételez, ahol biztonságos |
| Selection | Kiemelt lencse, irányított perem, nem tömör fehér/szürke blokk |
| Button | Áttetsző, jól elkülönülő, megtartott piros/zöld szemantika |
| Panel | Közepesen áttetsző nagy kártya |
| Tile | Könnyű, enyhén matt belső csempe, egy közös háttérből, sűrű listában nincs saját blur |
| Input | Finom mattítás, jól látható fókusz/állapot |
| Overlay | Mattabb, olvasható sheet/dialog, saját ablakhoz kötött háttér |
| Performance | Aktív mérésnél könnyített réteg, nincs folyamatos drága effekt |

0–100 Frosted–Clear a teljes üvegcsaládra, szerepenként skálázott paraméterekkel.
Csak paletták 33–40. 32 eredeti téma, saját témák, tárolási kulcsok, Wear téma- és adatprotokoll,
edzésmotor, Coach, Health, Room/DataStore és navigációs logika megőrzendő.
Szöveg ≥4,5:1, funkcionális kontúr ≥3:1, ≥48dp érintés, TalkBack, HU/EN/DE/RO, reduced motion.
Nincs új bitmap-readback, titok, felesleges réteg; a Kyant támogatását és fallbacket valós API szerint kezeljük.

## Szakaszok (sorrend, zöld kapu)
- K0: main `430368755840f91c36165e44e8399006c4897254` → feat/kyant-glass. Kotlin 2.3.10 globálisan; telefon Compose BOM 2026.02.00 (UI/foundation 1.10.3), Wear BOM 2024.09.00 marad;
  Kyant Backdrop 1.0.6, Apache-2.0 notice/licenc, ExternalRoutePreview korábbi D8 hibájának viselkedésazonos javítása.
  Nincs vizuális módosítás. Teljes app/wear/shared JVM + telefon/Wear Release R8 + valódi debug DEX ellenőrzés.
- K1: egy központi Kyant integráció, meglévő main kontraszt és téma-szerződés alapján. #378 rétegbekötések kézi adaptációja, külön ablak/fallback, szereptokenek.
- K2: #379/#380/#386 célzott felületi javításainak átültetése Kyant szerepekre. Mind az öt főlap, Coach/Napló/Naptár/Beállítások,
  aktív edzés és óravezérlés, statisztika, egészség/napló/források, téma-, engedély-, fotó-, program- és edzésbeállítások;
  gomb, csempe, kártya, input, kapcsoló, választó, sheet/dialog/menu/snackbar, üres/hiba állapot. Rendszer engedélykérést nem alakítunk.
- K3: közös réteg, stabil cache, háttérben/álló fotón nincs órajel, könnyített sűrű és aktív mérési felületek.
  Opcionális debug-only képkockaidő mérés; nincs mért javulás állítása fizikai A/B nélkül.
- K4: szűretlen teljes telefon, Wear, shared JVM, nyers XML: 0 failure/error/skipped, noSource csak valóban teszt nélküli modulnál;
  font/video ellenőrzés, app+wear R8, regressziók. Vizuális ellenőrzőlista minden oldal, 8 üvegpaletta, 0/50/65/100, régi/saját téma,
  világos fotó, nagy betű, csökkentett mozgás, Galaxy A57.
- K5: külön, soha nem merge-elhető tesztág a sikeres K4 SHA-ból, külön csomagnév/eldobható debug signer;
  ugyanazon SHA teljes CI és R8 zöld után APK, aapt/signature/SHA256/forrás ellenőrzés, Drive átadás.
  Nem éles aláírás vagy kiadás. Main merge csak felhasználói készülékes elfogadás után.

## Rögzített referencia
- #378 `b16bcd902d81d1393fd2268060be52937b080f23` – működő Kyant teszt; nem merge.
- #379 `3ac9aba17283707be020e0747b1807570ce8a5bf` – széles felületi áttetszőség.
- #380 G6 `04b942359b1da614019f4ad2bddbd827aa129bb4` – üveganyag, UI, optimalizálás, zöld CI; későbbi félkész G7 nem validált.
- #386 `91d150e1cc137d197b389933af8fde2cef3770fb` – célzott Kyant felületi port, nem mainre merge.
A régi ágak megmaradnak referenciának. Csak feat/kyant-glass ágra írunk; nincs verzióCode/Name emelés,
nincs main módosítás, nincs Play/persistent signing. Helyi Gradle/emulátor tilos.

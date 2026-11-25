# Liquid Glass 2.0 – kézi (készülékes) elfogadási lista (2811 G6)

Célkészülék: Samsung Galaxy A57 (API 35+), plusz egy API 31–32 eszköz, ha van. Glass paletta (33–40) + csúszka.
Minden pont: képernyőfotó; hibánál paletta, csúszka-érték, képernyő, lépések. Mérés nélkül nincs „gyorsabb” állítás.

## 1. Anyag és csúszka
- [ ] 0 / 50 / 65 / 100%: Frosted matt, Clear tisztább; a kártyák közepe tiszta, a perem felül erősebb, nincs neon csík.
- [ ] Külső kártya (Panel) hangsúlyosabb a belső csempénél (Tile); nincs dupla keret.
- [ ] Világos saját háttérkép mellett is olvasható szöveg és ikon.

## 2. Fejléc és navigáció (2811/1)
- [ ] Görgetés tetején a fejléc a lap folytatása: nincs sötétebb sáv, nincs alsó lépcső/éles vonal (mind az 5 fül, Napló, Coach, Beállítások).
- [ ] Görgetett tartalom a fejléc alatt homályosan, a peremen törve látszik; a feliratok olvashatók.
- [ ] Gyorsgombok (Coach, Napló, Naptár, Beállítások) és a nav-kapszula üveg csempék; a pill csúszik, rugózik, nem villog.
- [ ] „Animációk ki” mellett nincs csúszás/nyúlás, a kijelölés helyes.

- [ ] Aktív edzésen a nav-elemek és gyorsgombok áttetszők; a „Vezérlés: telefon” sáv körül nincs tömör téglalap sáv.
- [ ] Beállítások (Témák kinyitva) fejléce is átlátszó, a Témák sorai üveg csempék.

## 3. Naptár + tervező (G2)
- [ ] Napcellák: normál / mai (akcentus szám) / kijelölt (lencse + körvonal) / tervezett (A/B, akcentus tónus) / teljesített (pipa).
- [ ] Mai nap akcentus gyűrű; az alcím nem vágódik le a Visszavonás ikon mellett; a piros panelen is olvasható szöveg.
- [ ] Újratervezés, Tervezés üveg gomb; Ürítés piros jelentéssel; hónapléptetés és húzás működik.

## 4. Közös komponensek (G3)
- [ ] Nincs tömör fehér/szürke gomb; mentés zöld, törlés piros jelentése megmarad.
- [ ] Mezők, chipek, kijelölt elemek üveg; fókusz/hiba körvonal látszik. Menük és párbeszédablakok opakok (elfogadott).

## 5. Képernyők (G4)
- [ ] Kezdőlap (Következő edzésnap, Mai javaslat), Programok, Napló + részletek, Coach, Fejlődés (férfi testábra), Statisztikák,
      Egészség (Heti célok, Test és fittség csempék), Beállítások + aloldalak (Google Cloud sor, Kapcsolt eszközök).
- [ ] Aktív edzés (erő, időzített, kétoldalas, intervallum, GPS): nincs nagy opak panel a sorozatok mögött; pihenő, „Vezérlés” sáv,
      alsó dokk üveg/olvasható; teljesített / aktuális / hátralévő sorozat elkülönül; időzítő pontos.
- [ ] Régi paletták (1–32) és egy saját téma: semmi nem változott.

- [ ] Aktív edzés „Milyen volt?” chipek üvegesek; Kapcsolt eszközök lap: minden kártya egységesen üveg.

## 6. Teljesítmény (G5) – A57
- [ ] Görgetés Kezdőlap/Egészség, pill-út, csúszka-húzás, hosszú lista, aktív edzés: frame time előtte/utána (Profile GPU / `gfxinfo`).
- [ ] Energiatakarékos mód: a háttér nem sodródik, a kinézet ugyanaz. Háttérbe tett app: nincs animáció.
- [ ] AGSL hiba esetén (logcat) csendes opak fallback, nincs összeomlás. API 31–32: homály + színmátrix út.

## 7. Óra
- [ ] Wear: csak szín-összhang (világos/sötét), olvasható; navigáció, mérés, átadás változatlan.

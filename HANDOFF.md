# TrainPilot Native – 2813 átadó

- Alap: main `8a787567`; saját ág `fix/collector-2813`, draft PR #399.
- Visszajelzések: `docs/FIXES_2813.md`; 1.5.3 (280900/280902), verzió változatlan.
- Lépések/Alvás: előre lefoglalt, 66%-ban korlátozott görgethető lapmagasság.
- Fejlődés: mért, fix üveg tabsáv és a tényleges lista alágörgetése; statisztikai lista is.
- Aktív erő/időzített/kétoldali: felső navigáció + órás sáv és alsó dock mögött gördülő tartalom.
- Külön chrome-optika; a mérési kártyák, press, mozgás és adatmotor régi kapuja változatlan; GPS térkép és energiatakarékos fallback érintetlen.
- Új `GlassScrollChrome.kt`: a listát rögzíti, a sávok nem mintavételezik önmagukat, felső/alsó in-scroll padding mérve.
- CI #786: debug és release Kotlin-fordítási hiba (Backdrop vs LayerBackdrop). A listalencse most típusbiztos, ablakhoz kötött LayerBackdropot használ, egyébként a régi paddinges fallbacket.
- CI #787: R8 sikeres, nyers XML 1285/1/0/0 (telefon 1006/1/0/0; Wear 276/0/0/0; shared 3/0/0/0). Egyetlen hiba: új görgetési fixture közvetlen Kyant betöltése, UnsupportedClassVersionError. A változatlan éles geometria külön MeasuredScrollChrome komponensbe került; a teszt továbbra is a tényleges első/utolsó sor és fix sávok helyzetét ellenőrzi, Android-only optika nélkül.
- Regresszió: `GlassScrollChrome2813Test.kt`, `LiquidGlass2810Test.kt`; a teljes CI az aktuális PR head-en ellenőrizendő.
- Készüléken vizsgálandó: nyitási ugrás, valódi áttetszőség, teljesítmény, nagy betűméret, utolsó sor és vezérlőgombok.
- Nincs merge, aláírás vagy verzióemelés, nincs tároló/motor/Wear/shared változás.

# 2. feladat – felcsúszó lapok és dialógusok: üveg + rétegek

Mockup: docs/mockups/2812/task2_sheets_dialogs.png

## Hiba
Egészségnapló és más felcsúszó lap tömör szürke; a lapon belüli kártya/gomb/kijelölt elem alig különbözik a háttértől. Dialógusban („Edzés beállításai és progresszió") a kijelölt sor csak körvonal.

## Fájlok
`app/src/main/java/com/example/ui/components/TrainPilotModalBottomSheet.kt` (a `glass` feltétel: csak ha `containerColor` ∈ {background, surfaceCard, surface+customPaint} – a lapok többsége más színt kap, ezért tömör marad), `TrainPilotScreenSheet.kt`, `TrainPilotAlertDialog.kt`, `ui/screens/health/HealthTabScreen.kt` (Egészségnapló lap), `ui/theme/KyantWindowOverlay.kt`, `KyantMaterialSpec.kt`, `GlassSurfaces.kt`.

## Teendő
1. Keresd meg, miért nem lesz üveg az Egészségnapló lap (milyen `containerColor`-t kap; `rg -n "TrainPilotModalBottomSheet\(" app/src/main`). Üveg palettánál (`palette.isLiquidGlass`) MINDEN felcsúszó lap és dialógus üvegfestéket kapjon (Overlay/Panel szerep), ne tömör `surface`/`surfaceCard` kitöltést.
2. Rétegsorrend, mind a meglévő `KyantMaterialSpec` szerepekkel: lap (Overlay/Panel, legerősebb homály + árnyék) › kártya (Tile: +~10% fehér, vékony ~1 dp perem, felül világosabb) › gomb (Button: +~14%, erősebb perem) › kijelölt (Selection: kék tónus + fényes perem + halvány ragyogás).
3. Lapon belül a kártyák/gombok NE hozzanak külön backdrop-blur réteget (csak Tile festék); egy lapon egy homályos réteg.
4. Dialógus kijelölt sora: kitöltött kék tónus + fényes perem (mockup alja), ne csak körvonal.
5. Nem-glass palettán a kinézet VÁLTOZATLAN marad (tömör lap).
6. Regressziós teszt: a „mikor üveg a lap" döntés legyen tiszta függvény (pl. `sheetUsesGlass(palette, containerColor)`), és tesztelj rá: glass palettán minden lap-szín üveg, nem-glass palettán nem. (A Kyant osztályok JVM-ben nem töltődnek be – ne hívd őket a tesztben.)

## Tilos
Lista-sor stílus, Beállítások, a motor átírása.

## Kész, ha
Egészségnapló + Edzés beállításai dialógus üveg, 4 réteg megkülönböztethető, CI zöld.

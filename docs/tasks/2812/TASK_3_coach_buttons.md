# 3. feladat – Coach kártya, elválasztók, Programok A/B gombok

Mockup: docs/mockups/2812/task3_coach_buttons.png

## Hiba
- Coach: „Következő edzésnap" kártya tömör szürke (a „Mai felkészültség" üveg mellett kilóg).
- Coach: „Mai javaslat gyakorlatonként" sorai között éles, vastag fehér elválasztó.
- Programok, kinyitott program: A-nap gomb tömör világos, B-nap tömör sötét → egységtelen.

## Fájlok
`app/src/main/java/com/example/ui/components/StudioCoachDomainPanels.kt`, a Coach képernyő (`rg -n "Következő edzésnap" app/src/main`), `ui/screens/workout/ProgramsListScreen.kt` (A/B nap gombok), `ui/theme/GlassSurfaces.kt`.

## Teendő
1. „Következő edzésnap" kártya ugyanazt az üveg kártya-festéket kapja, mint a „Mai felkészültség" (ne tömör `surface`).
2. Az elválasztók: 1 dp, fehér ~14–18% alfa (témakövető: `onSurface.copy(alpha=.16f)`), ne fehér.
3. A/B nap gomb: alap = Button festék + vékony perem; kijelölt = Selection (kék tónus, fényes perem, halvány ragyogás). Ne legyen tömör világos/sötét kitöltés. Nem-glass palettán a régi kinézet marad.
4. Ne adj sorokhoz új blur/backdrop réteget (lásd 4. feladat: akadás).

## Tilos
Lista-sor stílus általában, adatok, Beállítások.

## Kész, ha
Mockup szerint; nem-glass palettán nincs változás; CI zöld. Ha az alfa/állapot döntés tiszta függvényként kiemelhető, tesztelj rá.

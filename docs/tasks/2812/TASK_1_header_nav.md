# 1. feladat – Coach és Napló: áttetsző fejléc/nav, tartalom alájuk csúszik

Mockup: docs/mockups/2812/task1_header_nav.png

## Hiba (eszközön, 1.5.2)
Coach és Napló fülön a fejléc tömör sötétkék, a lista teteje a fejlécnél levágódik, nincs liquid hatás. Beállításokon, Kezdőlapon, Programokon, Edzésen, Haladáson, Egészségen jó.

## Ok (kódból, ellenőrizd)
`app/src/main/java/com/example/ui/theme/GlassAwareScaffold.kt`: `GlassUnderlapTabs` = HOME, PROGRAMS, WORKOUT, PROGRESS, HEALTH. A `MainTab.COACH` és `MainTab.LOG` hiányzik → `glassUnderlapRoute` hamis → nincs áttetsző fejléc/rögzített tartalom, és a body `innerPadding`-et kap.

## Teendő
1. Vedd fel `MainTab.COACH`-ot és `MainTab.LOG`-ot a `GlassUnderlapTabs`-ba (a `CALENDAR`-t NE, csak ha a képernyője is rendelkezik spacerrel – nézd meg és indokold).
2. A két képernyő görgethető tartalma ELEJÉN kapja meg a `LocalGlassTopInset.current` magasságú távtartót (mintául: `ProgressTabScreen.kt:106`, `HealthTabScreen.kt:724`). Fájlok: `app/src/main/java/com/example/ui/screens/log/LogTabScreen.kt`, a Coach fül képernyője (keresd: `rg -n "MainTab.COACH" app/src/main`; a „Mai javaslat gyakorlatonként" a `StudioCoachDomainPanels.kt`-ban van). Ha nem a fő lista kapja, a Coach/Napló tartalma a fejléc alá kerülne és eltűnne – ezért a távtartó kötelező.
3. Alul is: a tartalom a nav alá csúszhat, de a legalsó elem ne maradjon a nav mögött (alsó padding a nav magasságával).
4. Frissítsd `LiquidGlass2810Test.kt` (kb. 147–165. sor): a készlet most 7 fül; a teszt „minden aláhúzott képernyő spacerrel indul" ellenőrzése terjedjen ki a két új fülre (forráskód-alapú assert, mint a meglévők).

## Tilos
Beállítások, a már működő fülek viselkedése, KyantGlass motor.

## Kész, ha
- Coach és Napló: a fejléc áttetsző, lista a fejléc alatt elmosódva látszik, induláskor nem levágott.
- `LiquidGlass2810Test` frissítve, CI zöld.

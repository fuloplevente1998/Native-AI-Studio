# Liquid Glass 2.0 – specifikáció (felhasználói, 2026-11-25) – tömör változat

Forrás: a felhasználó hosszú specifikációja + Gemini célmockupok (naptár, Fejlődés, Kezdőlap, 12 képernyős áttekintés).
A mockup CSAK vizuális referencia (anyagminőség, perem, mélység); hibás/nem létező adatait, feliratait, állapotait NEM másoljuk.
Munkarend: `CLAUDE.md` (egy alagens, átnéző, Haiku-ellenőrzés), gyűjtő PR: #379 (`fix/collector-2811`); nincs külön `design/liquid-glass-2` ág.

## Kemény korlátok
- Saját Compose/AGSL; nincs külső motor, nincs másolt shader, nincs bitmap-readback; Kotlin/Compose/Gradle nem emelkedik; nincs verzióemelés/kiadás/merge külön engedély nélkül; #372 és #378 sosem merge.
- Csak a Liquid Glass családot (palettes 33–40) érinti; az eredeti 32 téma, saját témák, azonosítók, tárolási kulcsok, mentés/visszaállítás, Frosted–Clear csúszka (0–100, tartós) változatlan.
- Wear: nincs GPU-motor az órán; csak szín-összhang (világos/sötét, kontraszt); órás navigáció/mérés/átadás/protokoll nem változik.
- Adat- és üzleti logika érintetlen (edzésmotor, Room/DataStore, ID-k, ownership/epoch/ACK, Coach számítás és súlyozás, statisztika, Samsung/HC adat, hiányzó adat ≠ 0, GPS/fotó adatvédelem, szinkron). Nincs gyártott Coach- vagy mérési adat.
- Az izomcsoport-terhelés a meglévő férfi testábrát tartja (nem a mockup alakja).

## Anyag (5.1–5.3, 6)
Mintavétel a közös háttérből, állítható blur, peremi geometriafüggő fénytörés, háttérszín-átvétel, semleges tónus, irányfüggő spekuláris perem, belső árnyék/vastagság, lágy külső árnyék, csak peremi kromatikus hatás, tartalomfüggő kontrasztőr. Üveg közepe tiszta; nincs erős neoncsík; a külső kártya hangsúlyosabb a belső csempénél; nincs felesleges halmozott blur.
Anyagszerepek (tokenek a meglévő rendszerben, nem új motor): Navigation, Panel, Tile, Button, Selection, Input, Overlay, Performance.
Háttér: lágy foltok, egységes az egész képernyőn, nem indul újra kártyánként, nem ugrál képernyőváltáskor, energiatakarékos módban egyszerűsíthető, háttérben nem animál; saját háttérkép marad; világos képen is olvasható.
Tokenek: sugarak (panel 20–28 dp, csempe 12–18, gomb 14–20/kapszula), margó 12–16, térköz 8–12, érintés ≥48 dp; tónus, blur, fénytörés, perem, kijelölés, nyomás, siker/veszély/figyelmeztetés, kontraszt egységesen.

## Csúszka (7)
0% Frosted (erős blur, nagyobb fedés, matt) · 50% Balanced · 100% Clear (tisztább közép, karakteres fénytörés, látható vastagság). Folytonos, élő; V7 paraméterek felülvizsgálata (Clear 20 dp vs Frosted elkülönülés); blurt csak készülékes összevetés alapján hangolni.

## Fejléc/nav (8) és animáció (11)
Felső gyorsgombok egységes üveg; fő nav összefüggő üvegkapszula + mozgó lencse/kijelölő (GlassSelectionPill továbbfejlesztése), rugózó, nem villog, csökkentett mozgásnál animáció nélkül is jó. Nincs tömör sötét fejléc; a görgetett tartalom átlátszik rajta. GlassTouchBubble/GlassMotionState/GlassSelectionPill/TrainPilotPressFeedback finomítása; nincs folyamatos pulzálás/neon.

## Képernyők (9)
Kezdőlap · Programok (szegmentált választó, kompakt Aktiválás, kevesebb csonkolás) · **Naptár+tervező = elsődleges referencia** (állapotok: normál/mai/kiválasztott/tervezett/teljesített; lencse a mai/kiválasztott napon; A/B, pipák; Ürítés veszélyes jelzéssel) · Napló (törlés piros) · Coach · Edzés/aktív edzés (teljesített/aktuális/hátralévő sorozat elkülönül; pihenőidő nagy, stabil szélességű számjegy; alsó panel nem takar; aktív mérésen könnyített anyag; futó edzés nem kapcsolja ki az üveget máshol; időzített/kétoldalas/intervallum/GPS ellenőrzés) · Fejlődés · Statisztikák (grafikon torzítatlan, kontrasztos, kijelölt pont, üres időszak) · Egészség (6 csempe, Test és fittség, célok) · Beállítások (minden aloldal; téma élő előnézete ugyanazt az optikát használja; HU/EN/DE/RO) · másodlagos felületek (sheet, dialógus, menü, választók, snackbar, kereső, kapcsoló, csúszka, üres/hiba állapotok; indokolt opak felület elfogadott; rendszer-engedélykérés érintetlen).
Tipográfia: egységes hierarchia; monospace csak időzítő/szám/mérés; ikonméret egységes.

## Olvashatóság/hozzáférhetőség (12)
Szöveg ≥4,5:1, funkcionális ikon/kontúr ≥3:1, szöveg nem örökli az áttetszőséget, állapot nem csak színnel, 48 dp érintés, TalkBack, nagy betűméret, kis kijelző, 4 nyelv hossza. Helyi fényerő-szabályozás, nem az egész app sötétítése; dekoratív perem ≠ funkcionális kontúr.

## Kompatibilitás és teljesítmény (14, 15)
API 33+: teljes AGSL; API 31–32: RenderEffect-blur + egyszerűsített Compose anyag; régebbi/szoftveres/shaderhiba: stabil, hasonló fallback. Shader-cache, stabil uniform, kevés rekompozíció, korlátozott GPU/memória; a háttér nem kell teljes frissítéssel. Célkészülék: Samsung Galaxy A57; mérés előtte/utána (görgetés, frame time, GPU/CPU, memória, indítás, témaváltás, csúszka, hosszú lista, aktív edzés, háttér/előtér, akku). Mérés nélkül nincs javulás-állítás.

## Szakaszok (19)
G0 audit/kiindulás · G1 központi anyag (paraméterek, szerepek/tokenek, fallback) · G2 **naptár + tervező** referencia · G3 közös komponensek (fejléc, nav, gombok, kártyák, csempék, kijelölők, mezők, menük) · G4 teljes képernyő-integráció · G5 optimalizálás (aktív edzés, GPU/memória, animáció, Wear vizuális ellenőrzés) · G6 regresszió + készülékes elfogadás. Mindegyik az előzőre épül, nincs duplikált megoldás.

## Tesztek és elfogadás (20, 21)
Meglévők: LiquidGlass350/Optics373/Nav373/2808/2809/2810 tesztek, GlassBackdropStore2809Test, ThemeSelection349Test, SourceSizeGuardTest, téma-perzisztencia (telefon+Wear). Kontraszt/kompatibilitás/funkció elvárást nem gyengítünk. Készülékes: minden főoldal + aloldal képernyőfotó; csúszka 0/50/65/100%; 8 glass paletta, eredeti témák, 1 saját téma, világos saját háttérkép. Elfogadás: naptár a mockup minőségét közelíti; egységes rendszer; tömör fehér/szürke gombok helyett üveg (szemantikus/teljesítménykritikus opak maradhat); nincs neon/dupla keret; olvasható; nincs levágott vezérlő; régi témák épek; edzés és telefon–óra működik; nincs jelentős teljesítményromlás; CI zöld. Draft PR tartalma: kiindulási SHA, ág, anyagösszegzés, érintett komponensek/képernyők, előtte–utána képek, paraméterváltozások, mérések, tesztek, korlátok, fallback, készülékes státusz, hátralévők. Dokumentáció: LIQUID_GLASS_THEMES, CODEMAP, HANDOFF.

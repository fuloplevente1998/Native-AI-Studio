# Átadás — 2817 Wear-only napló
- Ág: fix/2817-wear-only, alap e53a35a6 (281300/281302).
- A: TrainPilot edzés részletei kizárólag érvényes mentett Wear pulzus/energia; Forrás csak Wear; külső edzés saját importmérése változatlan. HC/Samsung bizonyíték megőrizve, csak a megjelenítésből kizárva. A részletek megnyitása nem indít provider-olvasást.
- B: négy nyelvű óratelepítési tipp, Play-link/web fallback, Később edzésenként külön UI-preferenciában; nincs adatbázis- vagy health240Json-írás.
- C: napi HC elsődlegesség és Samsung kiegészítő kapcsoló változatlan, külön regresszió.
- D: #414-ből csak helyi idő és statikus üvegfesték, a HC-elsőbbség nem került át. A megszüntetett HC/Samsung ablaklista nem igényel időformázást.
- Az eredeti Wear mérés látható akkor is, ha a közvetlen kapcsolatot később kikapcsolták; nincs új mérésindítás.
- Felhasználói pontosítás: a TrainPilot-edzés Statisztikák kártyáján csak összes Wear kcal látható, az aktív kcal mentve marad, nem helyettesíti a hiányzó összes kcal-t; napi és importált adatok változatlanok.
- CI és fizikai eszközös ellenőrzés szükséges. Nincs merge, signing, verzióemelés. Helyi Git DNS-hiba.


## #425 – Programok, Napló, fotógaléria és fix Liquid Glass sávok (2026-11-27)
- Draft [PR #425](https://github.com/fuloplevente1998/Native-AI-Studio/pull/425), ág `fix/programs-compact-pinned-header`, main-alap, kapcsolódó #419; implementáció és vizuális referenciák commitja `2acf3b43`.
- Programok: redundáns jelenlegi cím nélkül, nagyobb Aktív jelölés, szűkebb térköz, 4 dp alsó margó a meglévő scaffold safe inseten felül; fix További programok és fix alsó sáv.
- Napló: kereső, dátumválasztó, edzésszám fix, nincs Edzés részletei shortcut; az egyedi sor a saját edzését nyitja.
- Fotómegtekintő: 28 dp külső ív, 24 dp tényleges fotómaszk, aránytartó méretezés és rendezett fejléc/metaadat.
- GlassScrollChrome: mért felső/alsó sáv és elkülönített listacapture; a Programok, Napló, Gyakorlatkönyvtár, Egészségnapló és privát fotógaléria fix sávjai mögé csúszik a lista. A helyi ablakra opt-in ág nem mintáz idegen ablakot; teljes optikai kapu, energiatakarékos fallback megőrizve. A sávok magassága a listán belüli kezdő/végső helyfoglalás.
- A felhasználó által elfogadott elrendezések alapján 3 nyilvános, újrarajzolt SVG: `docs/mockups/pr-425-programs.svg`, `pr-425-journal.svg`, `pr-425-photo-viewer.svg`. A privát személyfotó nincs a public repóban; semleges képhelyőrző. Az eredeti PNG-k nem kerültek át.
- Forrásszintű szerkezeti ellenőrzés és két regressziós teszt kiegészítése megtörtént; CI/JVM és fizikai eszközös optikai, görgetési, nagy betűméretes és navigációs ellenőrzés még nincs. Nincs merge, APK, verzióemelés. A commitok a közös későbbi CI-kör szerint skip ci jelölésűek.
- Integrációs review: az eredeti Wear-only átadó megőrizve; fix alsó sávok helyi mintavétele és ablakhatárok ellenőrzése.

## #425 – Jóváhagyott 6–7. pont: aktív edzés és Coach (2026-11-27)

- Aktív edzés: a közös `StudioWorkoutHandoverBar` balra Vezérlés: + aktuális tulajdonos ikon, alatta a meglévő igazolt mérésállapot. Jobbra azonos méretű idő- és átadáskeret, SwapHoriz + cél eszköz ikon; lokalizált akadálymentes címke, legalább 48 dp érintési cél. A függő/pause/hibastátusz, engedélyezési feltétel, elapsed/ACK/ownership és callback megmaradt.
- Coach: kompakt, tartalomfüggő felkészültségi kártya; egyetlen A/B/C/D jelölés, valódi leíró napnév megtartva, kisebb jelvény. A közös dátumformázó opcionális kompakt megjelenítése csak az ismételt jövőbeli előtagot hagyja el; Home változatlan. Normál portréban fix felső kártyák és önálló, fix fejléces, GlassScrollChrome-mal mintázott javaslatpanel. Kicsi kijelző/nagy betűméret esetén teljes oldalas scroll; túl magas felső blokk külön elérhető.
- Kód: `StudioWorkoutHandoverBar.kt`, `CoachDetailScreen.kt`, `StudioCoachDomainPanels.kt`, `HomePlannedDateLine.kt`. A regressziók az eszközváltást, keretgeometriát, önálló Coach görgetést, sor műveletet, napjelölést és változatlan dátumszemantikát védik. Referenciák: `docs/mockups/pr-425-handover.svg`, `docs/mockups/pr-425-coach.svg` (szerkeszthető, szintetikus látványtervek).
- A 6–7. pont implementálva, az új CI és a fizikai készülékes validálás szükséges. A korábbi zöld forrás `4d6976ea`: 1338/1338 teszt, 0 failure/error/skipped; nem bizonyíték az új commitokra. Külön gyűjtött hátralévő feladat: Egészségnapló nem Glass csempék és Programok alsó gombjai. Nincs main merge, kiadás vagy verzióemelés.

## Telefonos felület 1.5.5 után

- Ág `fix/ui-followup-2814`, alap `f625b7e7`. #418 mentett útvonal első nézete és külön nyitható Heti célok. #419 közös Programok-panel és fotólista, szelektív lapmagasság, Mentés és visszaállítás gombok és az adatvédelmi lap bezárógombja. #420 nyolc külön Liquid Glass háttér és Kyant érintési mozgás, peremfény és belső árnyék.
- #421 és #419: a Napló, az Egészségnapló és a Gyakorlatkönyvtár közös listafelületet és sorelválasztókat kapott. Az Egészségnapló összecsukott napjai tömörebbek, a dátumválasztó Liquid módban áttetsző. A listák és a könyvtár fő műveletgombjai érintéskor követik az ujjat és finoman nyúlnak.
- A főág teljes JVM/R8 kapuja indítja az aláírt telepítők készítését. A regresszió ellenőrzi a heti célokat, a lapmagasságot és az összecsukott napot. Fizikai készüléken ellenőrizendő a térkép első nézete, a gesztusok, a dátumválasztó és a görgetés.

## 2815 – Listák és Liquid vezérlők

- `fix/2815-list-glass` a `751aaab5` főági verzióról. 1.5.5, telefon 281500, Wear 281502.
- #419: közös Programok-panel és elválasztók a Naplóban, az edzés gyakorlatlistájában, az Egészségnaplóban, a Gyakorlatkönyvtárban, a függőleges fotólistában és a fejlődési rekordlistában. A két naplórészlet rögzített magasságú görgethető ablakot kapott. Az Edzés beállításai párbeszédablak magasabbra nőhet.
- #420: a Naptár Újratervezés gombjának rétegét és a könnyű rajzolást használó gombok ujjkövető mozgását vezettük át az érintett közös vezérlőkbe és az Edzés csempéire.
- A PR kapuján a teljes JVM és az R8 fut. Készüléken ellenőrizendő a lenyitás, a peremfény, a nyúlás és a görgetés.

## #419 – Fix fejlécek és műveletsávok

- `fix/pinned-list-controls`, alap `0fa5c617`. A Programok aktív kártyája, a fülek és az alsó műveletek fix helyet kaptak. A szerkesztés a hárompontos menüből érhető el. A Gyakorlatkönyvtár szűrői és az összeállítás sávja a görgethető listán kívül vannak.
- Az Egészségnapló dátumszűrője rögzített, a lista hét napos lekérdezésekkel töltődik visszafelé. A galéria alsó sávja a két fotóhozzáadási műveletet tartalmazza.
- Készüléken ellenőrizendő a négy lap görgetése és az Edzés indítása sáv megjelenése.

## #425 – Integrációs ellenőrzés és naptárcsempék

- `review/pr425`, alap `d0cd9b9b`. A képernyőszélesség a külső Compose konténerből jut el a keskeny edzésvezérlő elrendezéshez.
- A fő naptár, a Napló és az Egészségnapló közös napcsempe-geometriát, peremet és állapotfestést használ. A tartományvégek és a belső napok külön kiemelést kapnak. A mai napot aláhúzás is jelzi az időszakválasztókban.
- Az Alap programok fülön az Új program teljes szélességű. A három művelet a Saját programok fülön érhető el. A hagyományos témák egészségnaplós értékei külön háttérrel és finom kerettel jelennek meg.
- A teljes JVM és R8 CI szükséges. Készüléken ellenőrizendő a naptárfestés és a görgetés.
- A 2816-os készülékes próbához a belső verzió telefonon 281600, Wear OS-en 281602. Megjelenített verzió 1.5.5.

## #426 – Fotónapló és Programok üvegfelületei

- `fix/photo-journal-contrast`, alap `60ac9631`. A nagykép átlátszatlan hátteret, a bélyegképek kivágott, lekerekített sarkot, a fotóhozzáadó gombok Liquid témában sötétebb hátteret kaptak.
- A Programok listája a címsáv és az alsó műveletsáv mögött görgethető. A fülek és az aktív kártya a listán kívül maradnak. Az alsó sáv peremét a gomb peremfénye emeli ki.
- Fizikai készüléken ellenőrizendők a képkivágások és a görgetés közbeni üveghatás.
- A Fejlődés gyakorlatstatisztikáiban a rekordlista a Gyakorlatok / Kardió / Lépések választósáv mögött görög, a felső navigáció és fő fülek előtt véget ér.
- A naptári és személyes tervező, valamint a naplózott edzés visszajelzési menüje lekerekített, beljebb húzott kijelölést használ.
- A „További programok” cím és a lista közös keretet kapott, a cím alá erősebb elválasztó került. Az alsó műveletsáv külön felület.
- A 40-es Liquid Glass téma neve Arany. A meglévő témakulcs a mentett választások miatt megmaradt. A háttér fényeinek színárnyalata a TrainPilot sárgájából származik.
- A naptári Újratervezés a Liquid témák saját üveges ablakanyagát használja. A Bezárás kerete 36 dp, az érintési felület 48 dp.
- A dátumablakból indítható a még le nem járt tervezett edzés. A gomb az adott alkalom azonosítóját adja át; a normál edzésmotor ellenőrzi a mentett alkalmat és a naplózást ahhoz kapcsolja.

## Készülékes finomítások – 281600 tesztelése (2026-11-27)
- Alap: main `5ba380f5`, ág `fix/liquid-theme-selection-stability`. A kiadási APK tesztje után gyűjtött módosítások; verzió, adatmodell, signer változatlan.
- Témaválasztás: a normál ↔ Liquid Glass átmenetnél az explicit kiválasztott csempe görgetési horgonya, a stabil Témák listaelem, nyitott kategória és a külön felnyíló palettaválasztó állapota megőrzendő. A forrásban nincs explicit visszanavigálás a selectPalette útvonalon; az eszközön észlelt pontos kiváltó ok még nincs reprodukálva. JVM-próba a változó felső insettel; valódi Kyant/készülékes próba szükséges.
- Biztonsági mentés: az összecsukott Google Drive kártya külön, 48 dp érintési célú szinkronikonja a meglévő Drive-only műveletet hívja. A kibontott Drive+Naptár művelet változatlan. Aktív edzés, mentés/visszaállítás, karbantartás, foglaltság és bejelentkezési feltételek megtartva. A kártya nem nyílik ki a gyorsszinkron érintésére.
- Heti/Havi célok: Egészség oldali Edzések/Lépések összecsukva kisebb függőleges margó és térköz, kinyitva megmaradó diagramok/tartalom szűkebb térközzel, 48 dp fejléc érintés; mérések és célképletek változatlanok.
- Három JVM regresszió hozzáadva (témaváltás és viewport, gyorsszinkron callback/tiltások, célkártya geometria és diagram). CI még nem futott; gyűjtő draft, közös integráció az elfogadást követően.
- További gyűjtött funkcióhiba: a naptár egy nem létező naplóbejegyzéshez is teljesített státuszt/pipát mutathat. Ellenőrizni a valódi scheduleId–workoutId kapcsolatot, törlés/mentés/elvetés és restore/undo útvonalakat; árva státusz helyreállítása adatvesztés nélkül. Ez még nem implementált; az ok nincs készülékesen bizonyítva. Eredeti képernyőképek nem kerülnek a public repóba.

- Fix Liquid Glass belső sávok: `glassHeaderMaterial` közös sötétítő rétege 50–60% fekete az üveg és a valódi vezérlők között; Kyant fejlécnél 12–16 dp minimum blur (a csúszka szerinti erősebb blur megmarad). A fő navbar változatlan referencia; normál témák, alácsúszás, ablaklokális capture, pointer shield és mért insetek változatlanok. Érintett: Gyors edzés és Gyakorlatkönyvtár (közös), Napló, Egészségnapló (külön felhasználói visszajelzés), Programok, Coach, Fejlődés, fotógaléria és a közös fejlécanyagot használó aktív edzés fix sávjai. Új JVM optikai paraméter/kontraszt teszt; készülékes ellenőrzés szükséges, különösen világos Liquid hátterekkel és 100% clarity mellett. Nincs új build vagy merge.

- Fotógaléria felhasználói pontosítás: a lista/fejléc/műveleti kártyák korábbi üveganyaga megmarad; a teljes képernyős fotónéző külön 100–95% opacitású háttérrel rendelkezik, lineárisan a Liquid clarity 0–100% alapján. A második opak hátteret csak Liquid esetben hagyjuk el; a fotó, metadata, sarkok, nem Glass témák és a normál ablakok opak politikája változatlan. Kontrasztkorlát fehér háttérre is; a teljes csúszkatartományt ellenőrző JVM-próba hozzáadva, futtatás és fizikai eszközpróba hátravan. A felhasználó privát fotója nem kerül a repóba.

- Jóváhagyott felcsúszó panel mockup: a közös `TrainPilotModalBottomSheet` Liquid esetén semleges, enyhén palettaárnyalt füstüveg-festéket használ a korábbi külön fagyasztott színes jelenet helyett. A közös `subtleGlassWindowOpacity` a fotónézővel azonos 100–95% opacitás (0–5% tényleges háttér-átlátszóság) a clarity 0–100 tartományban. Az alatta lévő valós ablak a normál kompozitálás révén látszhat, nincs másik ablakról capture/screenshot vagy új cross-window blur; a meglévő scrim és helyi fix fejlécek optikája megmarad. A szöveg, fotó, gombok nem kapnak globális alpha-t; középre nyíló alert, popup és nem Liquid téma változatlan. Az alapjelenet szüneteltetése, bezárás, swipe, insets, fókusz és szerkesztési guard érintetlen. Érintett minden közös modal/screen/route/form sheet, így Gyors edzés, Gyakorlatkönyvtár, Mentés, Egészségnapló, dátumválasztók, témák stb. A tiszta JVM szín/kontraszt teszt a teljes csúszkatartományt ellenőrzi; a készülékes ablakkompozitálás és a mockuphoz viszonyított látvány még ellenőrizendő. 5% mellett csak finom átsejlés várható.

- Fotógaléria: a redundáns „Fotók az edzésekről” látható cím eltávolítva a lista- és teljes képernyős nézetből (a meglévő Fotógaléria navigációs fejléc és az akadálymentes fotóleírás megmarad). A fotónézőben a dátum/edzésadatok mellé került a 48 dp bezárógomb, így több hely jut a képnek. A képarány, kerekítés, háttér és adatkezelés változatlan. CI/készülékes ellenőrzés még szükséges.

## Liquid felső navigáció fade – 281600 utáni észrevétel
- Külön ág: `fix/liquid-nav-fade`, alap main `f72f0988`. A fejléc rajzolási rétegén 240 ms új háttér-fade, közös semleges, enyhén palettaárnyalt festék; nincs régi frame capture, második lencse vagy teljes fejléc-alpha. Az ikonok és szöveg azonnal olvasható, a kijelölő pill és a navigáció változatlan. Gyors váltás megszakítja az előző tween-t. Motion off, energiatakarékos mód vagy zárt optikai kapu esetén csak a közös állandó tónus marad.
- A `rememberKyantHeaderScene` a naptár párnázott forrásán is ugyanazt a konzervatív színkorrekciót használja, mint a tartalmat mintázó fejléc. Naptár layout/tervezés, underlap lista, nem Liquid témák változatlanok. Az eltérő mögöttes tartalom miatt a pixelazonosság nem cél; készülékes látványteszt szükséges. Kontrasztpróba hozzáadva, még nem futott.
- A már elindított main CI és signing forrását ez a draft nem módosítja. Main merge csak az új feladat elfogadása után.

## 2817 / 1.5.6 – közös aláírt build (2026-11-27)
- Felhasználó által jóváhagyva: #427 és #428 a mainen; a DpRect JVM fordításjavítás (`d4eb3f94`) megmarad. A #428 squash `b1b60533`.
- Belső verzió telefon 281700, Wear 281702, külső 1.5.6; egyszerre emelve. A signer és a teljes main CI → automatikus signed APK/AAB workflow változatlan.
- Az előző main futást a concurrency megszakítja; a 2817 forrás teljes JVM/R8 eredménye és aláírt csomagjai még ellenőrizendők. A naptári árva teljesítési státusz külön nyitott funkcióhiba.

- 2817 CI #46029522490: R8 sikeres, nyers XML 1352 teszt / 3 failure / 0 error / 0 skipped. Fotónéző és felcsúszó panel: sRGB Color 8 bites alpha miatt a folytonos ideális érték nem ábrázolható minden lépésen. A tényleges festék átlátszóságát lefelé kvantáljuk, így sosem nagyobb az előírt értéknél (100%-os csúszkán 12/255, ~4,71%). A két teszt a pontos byte-értéket, felső határt, eltérést és változatlan kontrasztfeltételt ellenőrzi. Témaválasztó JVM-fixture a támogatott optika nélküli megjelenítést használja a Kyant UnsupportedClassVersionError miatt; tényleges kiválasztás, inset és láthatóság ellenőrzése megmarad. Release R8 és fizikai próba külön kapu. Verzió 1.5.6 / 281700–281702 változatlan; teljes main CI újraindul.

## 2817 készülékes visszajelzés – fix üvegsávok és felcsúszó panelek tónusa
- Alap `b901af30`, ág `fix/liquid-chrome-color`. A Programok 0% és 100% clarity képein a fix fejléc és alsó sáv túl fekete, a felhasználó mindenhol jelezte; a #427 50–60% fekete réteg közvetlen ok a kódban. A #428 fő navbar semleges fade-je is közös palettatónusra állítva. Külön visszajelzés: minden felcsúszó ablak szürke, de az átlátszóság megfelelő.
- `liquidChromeTint`: paletta háttér/elsődleges szín keverése, korlátozott .105 → .080 relatív luminancia. Belső fix chrome 88 → 82% színezett fedőréteg, 12–16 dp minimum blur változatlan. Az alácsúszás, keret, gombok és mért terület változatlan; a nagyobb színes fedés célja a szellemírás visszaszorítása fekete szorzás nélkül. Fő navbar 65 → 88% színezett fade (240 ms), naptári közös optikai korrekció megmarad. A közös `slidingSheetBackground` ugyanezt a palettatónust kapja a korábbi semleges szürke helyett; az 5% maximumhoz lefelé kvantált alpha és a tényleges ablakkompozitálás változatlan, nincs külön színes procedurális jelenet. A fotónéző, középre nyíló alert, normál téma és adatkezelés változatlan.
- JVM próbák: nyolc paletta teljes clarity tartományára látható tónus, szellem-kontraszt, tényleges szövegkontraszt, eltérő palettaszínek, csúszka-irány, sheet hue és a korábbi pontos alpha teszt megmarad. A korábbi teszt a feketébe sötétedést jutalmazta, ezért a szellemkontraszt és látható tónus a helyes feltétel. Fizikai vizuális és CI ellenőrzés még nincs; nincs main merge vagy új verzió/build. A privát képek nem kerülnek a repóba.

## #429 – adaptív testreszabás footer, folytonos felső háttér és peremfény (2817 utáni kérés)
- Kezdőlap, Edzés és Fejlődés áttekintő: `pageFooterArrangement` csak a szabad viewport helyet adja az utolsó Testreszabás gomb elé; túlcsorduláskor szokásos kártyaköz és görgethető utolsó elem, szerkesztéskor nincs footer. Egészség és Statisztika változatlan. Az elrendezési számításra teszt készült; a valódi viewport és a különböző betűméretek vizuális ellenőrzendők.
- Mind a nyolc Liquid téma egyetlen root-space háttérfény-lejtőt kap: 45% sötétítés a tetején, 32% képernyőmagasságig folyamatosan nullára fut; Compose/Kyant (saját fotóval is) és AGSL ugyanazt a szabályt követi. A fő navbar tartós festéke és egyenes kerete kikerült. A már meglévő lencse háttere külön, csak saját méretű offscreen réteg, az alsó 35%-on feather-maszkkal; nem a szöveg vagy érintési cél halványodik. 240 ms útvonal-fade marad, maximum 24% átmeneti színes coat, nyugalomban nulla. Naptár színkorrekció változatlan. Tényleges eszközön ellenőrizni az alácsúszó szöveg láthatóságát a feather zónában és az offscreen költséget.
- Első közös prizmafény-lépés: `glassPrismaticRimRole` Panel/Navigation/Button/Selection/Input; közös vector rim irányított paletta- és fehér fénycsík, meglévő Kyant valódi lencse Highlight szélessége/erőssége emelve. A fix sáv palettabevonata fölött is peremfény. Tile/Performance, letiltott optika, energiatakarékosság nem kap bővített effektet; nincs új content capture. Ez nem a generált mockup pixelazonos fénytörése és nem új lencse minden csempére; valódi teljes-app optikai kiterjesztés külön teljesítmény- és készülékauditot igényel. Normál témák, 0–5% sheet alpha, fotónéző, funkciók és verzió változatlan.
- Az új draft kontraszt-, árnyalat-, fény-lejtő-, szerepkapu- és footer-elosztás teszteket tartalmaz. CI/fizikai vizsgálat nem történt, main nem módosult. Az előző 12–18% belső fix chrome coat-átlátszóság maradt; ez nem a felcsúszó panelek 0–5%-a.

- #429 a mainen `46384851`. Az első főági JVM-futás 1355 próbából egyet jelzett a Jég színtelítettségére, az R8 sikeres. A panel festékszíne a téma háttérárnyalatából készül a meglévő fényességkorláttal és átlátszósággal. Az ellenőrzés a CI-ben fut.

## Liquid vezérlők 100%-os beállításon

- `fix/liquid-control-frames`, alap `d69bee8b`. A felső Naptár és Beállítások a Coach és Napló lekerekített formáját használja. A semleges Liquid vezérlőfestés és a fix sávok a választott háttér árnyalatát kapják a meglévő fényességkorláttal.
- A További programok címsávja 4 dp-vel magasabb. Az alsó sáv vékonyabb, a lista mögötte továbbra is görgethető. A sáv háttérmintája elmosott, a hangsúlyos perem az Új program gombhoz tartozik.
- JVM-próba védi a Jég és az Arany vezérlőfestését 100%-on. Az üveghatás és a gombok pereme fizikai készüléken ellenőrizendő.
- A közös görgetés fölötti Liquid üveg 18–22 dp minimum elmosást használ. A Napló erősebb, helyi matt réteget kapott. A lista mintavétele és rajzolása a fő navigáció alatt kezdődik, a kereső és a dátumválasztó mögé továbbra is becsúszik.
- Az aktív edzés Coach és Részletek gombja enyhe Liquid peremfényt kap, a felső vezérlés és a pihenősáv belső csempéi palettaárnyalatot használhatnak. A Beállítások és a Témák választó azonos Liquid családon belüli váltáskor is a kiválasztott témát hozza látható helyre.

## Beállítások – Google Cloud gyorsszinkron

- `fix/settings-cloud-quick-sync`, alap `9e0fa11a`. Az összecsukott Beállítások-kártya jobb oldalán a Drive + Naptár szinkron indítható. A gyorsgomb kerek, 36 dp-es vizuális kerettel és 48 dp-es érintési felülettel rendelkezik, a kibontott művelettel azonos útvonalon és tiltásokkal működik. A Biztonsági mentés külön gyorsikonja kikerült. A meglévő Drive-mentés működése megmaradt.
- A naptár árva teljesítéspipája külön nyitott hibapont. A Liquid megjelenés, a témaváltás és az érintési nyúlás fizikai eszközön ellenőrizendő.
- Liquid témában a közös Bezárás kerete 8 dp-vel szélesebb a magasságánál, teljesen lekerekített, 48 dp-es érintési felülettel. A naptári Újratervezés és a Liquid fotónéző is ezt használja.

## #432 – Liquid formavilág finomítása (2026-11-27)

- A felhasználó kérésére a #432 meglévő `fix/settings-cloud-quick-sync` ágán folytatva; forráscommit `8c296e33`. A közös piros X már vízszintesen elnyújtott (38×46 dp vizuális méret: szélesség 46, magasság 38; 48 dp érintési cél), a hagyományos forma változatlan.
- `TrainPilotControls.kt`: Liquid szűrőchipek és egysoros beviteli mezők kapszula alakúak, a több soros mezők 20 dp ívet kapnak; a közös elsődleges/másodlagos gombok tiltott állapotban is megtartják Liquid alakjukat. A nem Liquid paletták eredeti 14 dp geometriája, színek, méretezés és műveletek változatlanok.
- Szándékosan nincs globális alakcsere: nagy panelek, speciális diagramok, egyedi vezérlők és Wear érintetlenek. Forrásszintű célzott módosítás; fordítás, JVM CI és készülékes vizuális ellenőrzés még szükséges. Nincs merge, APK vagy verzióemelés; közös későbbi CI-kör.


## #432 – jóváhagyott Liquid mockupok és teljes telefonos felület auditja

- Hatókör: kizárólag a 8 Liquid Glass paletta (`isLiquidGlass`); a 4 standard színes és az összes többi nem Liquid téma eredeti geometriája, festése, művelete és szövege megmarad. A 3 jóváhagyott mockup vizuális referencia, nem pixelazonos renderelési szerződés. Személyes képernyőképek nem kerülnek a repóba.
- Közös X: alapértelmezett Liquid festett méret 48×28 dp, teljesen lekerekített, 48×48 dp érintési cél és eredeti piros jelentés, standard 38×38 dp. A meglévő közös komponenshez terelve a Health napló/források, Mai állapot, heti célok előzménye, napi lépésstatisztika, aktív edzés visszajelzése, natív egészségrekord, naplórészlet/fotó, útvonal és anatómia bezárását. Eredeti nem Liquid ágak, visszahívások, tiltás, tesztcímkék, natív akadálymentes szöveg megmaradnak. A törlés, szűrőtörlés, megjegyzéstörlés és banner-elvetés nem képernyőbezárás, nem kevertük össze.
- A Kezdőlapon Liquid edzésindító kapszula, 22 dp gyorsparancskártyák, 20 dp Mai állapot csempék, lekerekített heti célok vezérlő; Egészségnapló dátumtartomány kapszula. A standard ágak eredeti dp-értékei változatlanok. A három jóváhagyott oldal többi kártyáját a közös anyagút javítja, nem erőltetünk globális 50%-os sarkot a nagy, többsoros panelekre.
- Optika: meglévő Kyant `Panel` lencse magasság/eltolás szorzó 1→1.12, `Input` .25→.35, a többi anyagszerep és clarity/kontrasztkorlát változatlan. Közös irányított perem a modal külső felületén és nem kijelölt belső szerepeken; Tile csak a meglévő egyetlen cached edge gradient színirányát változtatja. Nincs új layer capture, új shader vagy új lencse minden csempére. Energia-/méréskapu és 0–5% sheet transparency, külön ablak mintavételi tilalma megmarad. A modal perem vektoros közelítés, nem valódi háttér-refrakció.
- Forrásaudit: a CODEMAP alapján közös anyagok, navigáció, vezérlők, X-kódhelyek és képernyőcsoportok áttekintése; GitHub kódkeresés a default branchből, a találatok ellenőrzése a #432 ágán. A teljes alkalmazás készülékes auditja még nyitott. Kötelező vizuális mátrix: Home/Programok/Edzés/Fejlődés/Egészség; Coach, Napló, Naptár, Beállítások/kapcsolt eszközök; felcsúszó és középső modalok, dátumválasztók, keresők, program-/gyakorlat-/edzésrészletek, aktív/pihenő/intervall/kültéri GPS, fotó/térkép, hibák/tiltott gombok. Mind a 8 Liquid 0/50/100%-on, a 4 standard előtte/utána; nagy font, kis kijelző, scroll és alácsúszás, TalkBack, 48 dp, saját háttér, akkukímélés, régebbi API; aktív mérés jank/battery és állapot működés. Az egyedi kártyák és a tényleges fénytörés teljes appos kiterjesztése csak ezen audit alapján, nem blanket live lens.
- Optikai szerep- és X geometria regresszió frissítve; utolsó forráscommit `7a6bd5e5`. Forrásellenőrzés, nem lefuttatott CI vagy készülékteszt. Nincs merge, APK, verzióemelés. A közös későbbi CI-kör és fizikai vizuális elfogadás szükséges.

- Felhasználói elfogadás: #432 PR CI → sikeres ellenőrzés után main merge → teljes main CI → tartós aláírású telefon és Wear APK + AAB. A korábbi 46×38 dp Liquid X méretet a jóváhagyott 48×28 dp váltotta fel. Kiadási verzióemelésre külön utasítás nincs. A készülékes elfogadás továbbra is nyitott.
- Felhasználói pontosítás: emelt verziókkal készül a kiadás: 1.5.7, telefon 281800, Wear 281802; a korábbi CI forrása már nem kiadási jelölt. A teljes PR és main CI az új forráson szükséges.

## #433 – Liquid teljes forrásleltár, teljesítmény és referenciaaudit

- Ág `fix/433-liquid-consistency`, alap main `b14dc497` (281800). A felhasználó kérésére előbb audit, utána célzott implementáció. `docs/LIQUID_433_AUDIT.md`: 310/310 telefonos UI/VM/Activity fájl, 66 716 sor sorszámos mintaleltára, 0 olvasási hiba; 8 főfül, 19 külön SubScreen ág és 7 sheet. Nem teljes hívásgráf vagy készülékes elfogadás.
- Rögzített #378 gördülékeny Kyant referencia `b16bcd902d81d1393fd2268060be52937b080f23`, Kyant 1.0.6 mindkét build. A doc összeveti a fix chrome coat/minimum blur, live=false nav pill, shader→vektor bubble, összetett press és nested capture útvonalakat; elkülöníti a már jó cache és Tile/Performance gate megőrzését. Kézi call-site besorolás, #378/281800/új A/B frame time és jank, teljes vizuális/standard regresszió még nyitott.
- Régi natív UI és render segédfüggvények csak igazoltan hívás nélküli kód esetén törölhetők; manifest/teszt/keep/alias és R8 ellenőrzés, aktív natív beágyazások, VM, motor, adat és fallback megőrzése. Témaválasztás route/scroll state külön regresszió. #433 bővítve; audit dokumentáció, nem runtime módosítás, nincs CI/merge/build/verzióemelés. A #378 test ág nem merge-elhető.


## #433 – L0 célzott besorolás + L1 első központi paint javítás (2026-11-27)
- Ág `fix/433-liquid-consistency`, audit HEAD `48b73fde`; main `b14dc497` (281800/281802). A teljes 310 fájlos audit megmaradt. L0 csak a központi paint hívókra lezárt, a többi képernyő híváshelyenkénti besorolása nyitott.
- Közös aktív út: `LiquidGlassPalettes.kt` glassTile/glassControlFill és withGlassClarity; `GlassSurfaces.kt` overlayRoleFill, glassOverlayFill, overlayRoleRim; `TrainPilotGlassEngine.kt` és `GlassContentBackdrop.kt` kompatibilis fallback. A közös fix szürke tile/elevated/selection és fix kék overlay festék helyett a paletta háttérárnyalata, külön luminancia- és alpha-értékek. Az eredeti slider görbék, optikai motor, Tile/Performance backdrop nélküli szerep, 0–5% sheet window szabály és standard/custom pass-through megmarad.
- Szemantikus kivétel: glassStateTile meglévő accent-jelentése és kb. azonos luminancia-limit; korábban specifikált zafír naptár/programnap kivétel változatlan. Fehér optikai csillanás/GlassOptics.TINT változatlan. ThemeSelectionColors már palettafüggő; nem cseréljük le vakon. Potenciális holtkód glassHeaderControlPaint csak szignatúra-igazítás, törlés L6-ban. A képernyőszintű raw Material/background utak L3-ban, Kyant live coat és fix chrome L4-ben vizsgálandóak.
- Négy meglévő regresszió az új palettaargumentumra igazítva (kék-only assert helyett kontraszt; nincs tesztkapu-gyengítés), új `LiquidGlass433PaintTest` a 8×101 érték, árnyalat, alpha, kontraszt, Tile/Performance kapu, 32 standard és saját téma pass-through védelmére. A forrásváltozások ellenőrzése pontos szöveges cserékkel; JVM/CI/R8 és készülékes ellenőrzés még nem futott. A teszt nem vizuális bizonyíték.
- Következő: L1 közvetlen palette.surface/Card és live/fallback coat hívók vizsgálata, L0 hátralévő híváshelyek, L2 press owner és L3–L8. Nincs merge, verzióemelés, signing vagy Play feltöltés.


## #433 – L2 első közös press owner (2026-11-27)
- L1 commit: `1c61c455d763c1dab499089a7a1bf04dca0c8555`. Közös Liquid press: TrainPilotPressFeedback a teljes alak press tintet csak standardnál tartja meg; Liquid esetben külön focus/hover tint. A glassPressMotion egy lokális GlassTouchBubble, nem egész gomb scale/kiemelés. A LiquidDropMaterial és KyantGlass redundáns press-layer/spring/drag eltávolítva; a nyugalmi optika és a GlassSelectionPill animált selection/drag állapota megmarad. TopNavPillItem: közös indikáció, optics-off dupla ripple nélkül.
- GlassTouchBubble vektoros, egy interaction collector + aktív animációs job, valódi Press.pressPosition vagy helyi pointer, rövid release biztosított, cancel nem gerjeszt új pulzust; old API/active measurement/overlay is, reduced motion/power save statikus 80 ms rövid jelzés, disabled/standard nincs buborék. Nincs új RenderEffect/capture. Fókusz/hover továbbra is látható; eseménykezelés/48dp változatlan.
- LiquidGlass2808Test kapu és pozíció elvárás aktualizálva, LiquidGlass433PressTest széles kontroll három pontjára rövid press és távoli felület változatlanságának pixeltesztje. Sem JVM/R8, sem fizikai pixel/gesztus mérés nem futott. Forrás-szintű ellenőrzés és branch lease; nincs teljes körű L2 elfogadás.
- L3 maradék raw press hívók: videógomb, nav bezárás, backup formátum, menü/Material3, saját chart/slider; L0 teljes call-site besorolás, L1 további palette/optikai coat, L3–L8 nyitott. Nincs main merge, build, verzióemelés, signer vagy Play feltöltés.


### #433 L3 – first active call-site corrections (2026-11-27)
- ExerciseLibraryScreen: category chips and row Start/Swap + queued Start now use bounded central selection/control paints and Liquid foreground, cheap vector rims, never a new capture. Existing favorite/custom filter chips opt out of live lenses. Standard color/shape branches and existing tags/actions unchanged.
- StatisticsTabs opts out of live filter lens inside the pinned header; shared FilterChip liveGlass option defaults to true for other callers. Disabled common field fill also follows the shared tile paint.
- L3 is partial: remaining screen/profile/active workout/icon/dropdown bypasses and the program editor layer still need classification and fixes. Approved ProgramsList selector remains unchanged. L4–L8 pending, no runtime tests or CI run yet. No version/merge/build.


### #433 L5 source triage / continuation checkpoint
- L1 1c61c455, L2 49328d7e, first L3 e3408b7f committed on existing branch. No green CI or physical verification yet. Do not mark ready or merge.
- Active root read: com.repforge.app.MainActivity.setContent collects settingsFlow, calls syncNativeSettings on changes, resolves currentPalette, TrainPilotTheme -> NativeFirstLaunchGate -> CompositionLocalProvider -> GlassSceneHost -> TrainPilotAppRoot. GlassAwareScaffold already keeps Scaffold at a stable composition call site. TrainPilotRouteContent keys navigation route/epoch, not palette.
- SettingsMainScreen retains expandedMainThemesSection, expandedThemeFamily, revealThemeId via rememberSaveable and its LazyListState. Existing reveal effect waits a single frame, scrolls to hard-coded item 2 when themes item absent; StudioPaletteCategories independently brings active tile into view and clears request. These are candidate visibility/remeasure races, not proven route navigation. VmAppearance.selectPalette updates only selectedPaletteId/banner and persists with cancelled previous job; VmBackupSettings.syncNativeSettings updates selectedPaletteId from DataStore and does not directly navigate. Need full root + real DataStore regression before modifying this path; rapid stale feedback is another candidate. No L5 runtime fix yet.
- Next: complete L0 classification and L3 bypasses (profile, health, workout, menus, favorites, new-program overlay); compare Kyant #378 integration and audit fixed chrome, duplicate rim/capture, cached paint/brush, per-row observers (L4); do L5 root regression; L6 prove reachability before deleting; L7 CI/R8/XML; L8 A57 frame metrics and visual approval. Keep 32 standard and custom appearance, Wear OS, protocols, version/signing unchanged. L2 removes broad press scaling as well as tint from common Liquid controls; selection pill transition retained, visual parity still needs review. No new live effect on dense rows. Intermediate commits skip CI; do not treat skipped as success.


### #433 L1/L3 – opaque compatibility surfaces and planner (2026-11-27)
- Continued on PR #434, head 4c50e17a. The user's updated release order is complete implementation → green PR CI → authorized main integration → full main regression and signed phone/Wear APK+AAB → physical acceptance by user; no Play upload. Do not equate signed CI with physical acceptance.
- `withGlassClarity` now separates the optical white coat from the representative opaque palette surface and elevated surface: same approximate original luminance bounded to .115/.124, hue derived from background, all old optical contrast reference surfaces retained. This reaches raw Material/native icon and legacy surface users without changing standard/custom palette paths. Tile seeds computed once per resolution instead of per underlay. Profile planner's nine raw Surface paints now route through central cheap tiles (existing dropdown window and red restriction semantics unchanged). Non-highlighted ProgramTypeEmblem uses cheap themed tile; highlighted variant's explicit state tint unchanged. Shared LiquidDropMaterial treats the normal primary accent as palette paint, retains other semantic accents and remembers cheap brush. No additional capture/lens.
- Existing 8×101 contract now includes opaque compatibility paints. Not executed yet. Native controls that bypass the palette and L2 raw press paths remain to review; actual device contrast/appearance and L4–L8 pending. Intermediate commit skips CI per AGENTS.

# Dizájncsomag – 2748 utáni forrásmódosítások

A felhasználó új döntése szerint Codex készíti a dizájnt, AI Studio a nyelveket/videót/fennmaradó adatfeladatokat. A main/raw átadó és a fájltulajdon-állapot az AI_STUDIO_LANGUAGES_VIDEO_DATA_HANDOFF_2748.md és DESIGN_WORK_STATUS.md fájlban.

## Változások

- #97: az A nap/A, B nap/B ismétlés elrejtve, ha a cím és a napkód jelentése azonos. Az egyedi napcím megmarad; nincs programadat-átnevezés vagy indítási napindex-változás.
- #98: mindhárom korábbi fontból ténylegesen hiányzott őűŐŰ és több román betű. Teljes, licencelt upstream fontokból statikus400/500/600/700 példányok kerültek be. A Studio és a natív typography ugyanazokat a valódi fontcsaládokat használja, nem regisztrálja a400-as körvonalat több súlyként. A rendszer fontScale megmarad. A script az aktuális fájlok cmap4/12 és OS/2 tartalmából, SHA-ból, licencekből ellenőrzi a HU/DE/RO/ASCII karaktereket és a valódi súlyokat. Forráscommit, upstream és kész fájlok SHA-ja: FONT_SOURCES_AND_GLYPHS.json.
- #99: a glow17–24 és light25–32 család külön háttér/kártya/belső felületet kap. A matt1–8 és kontrasztos9–16 palettadefiníció változatlan. A világos témák fehérek/világosak maradnak, a glow mély oldalháttere megmarad. A fő/másodlagos szöveg, hangsúlyszínek és a szemantikus egészség/hiba/figyelmeztetés színek a három alapszínen és a tipikus12/16/22% kijelölési háttéren is ellenőrzött kontrasztot kapnak, a színhangulat megőrzésével. A light/glow tint is átlátszatlan kártyaszínre keveredik, így a panelek háttere nem véletlenül az alatta lévő oldaltól függ. A Studio és a beágyazott natív editorok közös színleképezést kapnak; világos témában a natív kijelölés nem marad a régi fix sötét szín, és az accentText is az új világos felülethez igazodik. A dropdown kijelölés átlátszatlan, kártyaszínre kevert kitöltés. Audit: GLOW_LIGHT_SURFACE_AUDIT.json és GlowLightPaletteContrastTest.
- #95: erősítő sorozatok sorszáma, adatmezői, OK és törlés egységes lekerekített négyszög forma. Interaktív elemek48dp alapméret, nagyobb betűnél növelhető; aktív/teljesített/törlés állapot elkülönül. A fejléc és az adatsorok közös oszlopszélességekkel és keskeny/nagybetűs méretnél közös vízszintes görgetéssel működnek. Per_hand → kg/kar, total/single_dumbbell → kg; bolgár guggolásnál az ismétlés csak ism., /láb nélkül. Bodyweight/assisted/time külön megjelenítési címke. Mérés, súly/ismétlés/oldalszám, volumen és RIR számítása nem változik.
- Telefon/óra státusz: kompakt, témázott keret, irányt jelző ikon és középre igazított művelet. A telefon/óra és az átadás folyamatban állapot külön látható; nincs termékfelületi ACK-szöveg. Óravezérlés elérhetetlensége megjelenik. Nem aktív előkészítő képernyőn nincs felesleges átadási sor, de a valódi státusz/hibaszöveg megmarad. Nagy betűnél/szűk ablakban a gomb külön sorba törik.

## Megőrzött határok

Nincs motor, adatbázis, ownership/ACK/generation, átadás vagy visszavétel parancsváltozás. A gomb engedélyezési feltételei megmaradnak, a mentés és függő átadás továbbra is blokkolja az új parancsot; watch-owned visszavétel a korábbi módon engedélyezett. A már elfogadott fullscreen GPS elrendezés nem változott. AI Studio-fordítás előtt friss main fájlokat kell olvasni, régi képernyő-exportot nem visszaírni.

## Ellenőrzések

Fontellenőrző script és forrásparser/diff-check; megjelenítési napnév/egység tesztek, glow/light szöveg/kijelölés-kontraszt, átadás pending/mentés/óravezérlés és nagybetűs kereten belüli gomb regresszió. A végleges CI-eredmény az aktuális átadóban. Készülékes vizuális elfogadás külön szükséges. A forráscsomag új aláírt APK-ja a következő közös kiadás feladata, a korábban átadott2748 APK továbbra is f74133a forrású.

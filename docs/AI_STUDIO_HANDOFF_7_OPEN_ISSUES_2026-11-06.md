# Google AI Studio handoff – 7 nyitott TrainPilot tétel lezárása

Dátum: 2026-11-06  
Repo: `fuloplevente1998/Native-AI-Studio`  
Kiinduló branch: `main`  
A handoff előtti alkalmazás-main SHA: `58f4b4782a1121a37047fa5be400daa37f4246e6`  
A handoffot rögzítő docs-only commit: `7723f8f067ef34bf81cd18783f9060cea2947515`  
**Mindig a legfrissebb `main` HEAD-ről dolgozz; a fenti SHA-k tájékoztató kiindulási pontok.**

## Cél

Fejezd be a repóban jelenleg nyitva maradt 7 felhasználói tételt:

- #117
- #195
- #196
- #197
- #198
- #199
- #200

A munka végén minden implementáció kerüljön mainre csak sikeres regressziós CI után, majd az **aktuális main SHA-ból** indítsd el a meglévő:

`TrainPilot persistent signed APK pair`

GitHub Actions workflow-t (`.github/workflows/trainpilot-persistent-signing.yml`) **workflow_dispatch** használatával.

A workflow-t ne úgy indítsd, hogy mesterségesen módosítod a workflow YAML fájlt csak a push trigger kedvéért. A végleges signed build az exact merge utáni main SHA-ból készüljön.

---

# Kötelező munkarend

1. Először olvasd el a repó gyökerében az `AGENTS.md`-t, valamint az érintett natív adat-/UI-szerződéseket és a meglévő implementációt.
2. Ne írj második motort vagy párhuzamos állapotkezelést meglévő natív logika mellé. A meglévő Room / ViewModel / controller / ownership / restore / finalization útvonalakat használd.
3. Ne javíts hibát úgy, hogy csak elrejted a hibaüzenetet. A kiváltó állapotot javítsd.
4. Ne találj ki GPS-pontot, pulzust, kalóriát, távolságot, szinkroneredményt vagy más egészség-/edzésadatot.
5. Meglévő felhasználói adatok, Room rekordok, programok, naplók, Google kapcsolat és beállítások nem törölhetők workaroundként.
6. A TrainPilot jelenlegi kártyás/keretes, témakövető dizájnját őrizd meg. Ne vezess be képernyőnként külön hardcode-olt arculatot.
7. A változtatásokhoz adj érdemi regressziós teszteket. Ne csak snapshot/string teszt legyen, ha a probléma működési.
8. A PR-en fusson le a `TrainPilot Native Android Regression Tests` workflow. Mainre csak zöld CI után emelj.
9. Az issue-kba írd be röviden a megoldást, a kapcsolódó teszteket és a PR-t.
10. Fizikai készülékes működést csak akkor állíts bizonyítottnak, ha ténylegesen történt készülékes ellenőrzés. CI-ből ne állíts live Google/GPS/Wear elfogadást.
11. A 7 tétel lehet egy közös integrációs PR, de a commitok legyenek logikailag elkülönítettek és visszakövethetők issue-szám szerint.
12. A végén ellenőrizd, hogy nincs regresszió a telefon–Wear ownership, session ID, generation/epoch, restore, tombstone/finalization és tartós mentési szerződésekben.

---

# #117 – Naptár szinkron: nincs igazolható siker / nincs egyértelmű visszajelzés

Issue:
https://github.com/fuloplevente1998/Native-AI-Studio/issues/117

## Jelenlegi készülékes állapot

A korábbi Google 404 hiba már megszűnt.

A funkció azonban továbbra sem tekinthető késznek, mert:
- vagy maga a Naptár szinkron nem fut le ténylegesen;
- vagy lefut, de az alkalmazás nem ad olyan egyértelmű visszajelzést, amelyből a felhasználó tudja, hogy sikerült.

## Feladat

- Kövesd végig a Beállítások → Google Cloud Szinkron → Naptár szinkron teljes natív útvonalát.
- A műveletnek ténylegesen be kell fejeződnie.
- Siker esetén legyen egyértelmű, látható státusz, például:
  - „Naptár szinkron sikeres”
  - utolsó sikeres szinkron időpontja
  - szükség esetén rövid eredmény (pl. hány esemény frissült), de csak tényleges adat alapján.
- Hiba esetén legyen konkrét, értelmezhető hibaállapot; ne maradjon csendben.
- Elavult távoli naptár-/eseményhivatkozás javítható, de:
  - ne töröld a helyi naptárt vagy edzésadatokat;
  - ne bontsd a Google kapcsolatot workaroundként;
  - ne hozz létre duplikált eseményeket.
- A már javított 404-kezelést ne rontsd vissza.

## Elfogadás

- A gombnyomásnak van determinisztikus loading → success/error végállapota.
- Siker esetén az UI ténylegesen jelzi a sikeres szinkront és annak időpontját/státuszát.
- Hiba esetén a felhasználó érti, hogy nem sikerült.
- A tesztek fedjék a siker, hiba, elavult remote reference és ismételt szinkron esetét.
- Live Google siker csak live hitelesítéssel állítható; ha ez CI-ben nem lehetséges, az issue-ban ezt pontosan dokumentáld.

---

# #195 – Edzés képernyő és edzésösszegzés kompaktabb, egyértelműbb UI

Issue:
https://github.com/fuloplevente1998/Native-AI-Studio/issues/195

## 1. A/B nap jelölése

Jelenleg egy kicsi különálló „A” betű jelöli az aktív napot.

Kért állapot:
- a külön kis „A” / „B” jelölés szűnjön meg;
- az „Otthoni A/B – Alap” programazonosítás mellett/részeként az aktuális A vagy B nap kapjon jól látható, a meglévő app-stílusú **keret + háttér** kijelölést;
- első pillantásra legyen egyértelmű, melyik nap aktív.

## 2. Edzés teljes időtartama

- A jelenlegi apró számláló helyett az eltelt edzésidő kerüljön a felső vezérlő fejléc középső területére, ahol a „Vezérlés: telefon” és az „Átadás órának” elemek vannak.
- Legyen nagyobb, kontrasztosabb, könnyen leolvasható.
- A meglévő aktív/szüneteltetett időszámítási szabályt ne írd át hibásan; csak a helye/megjelenése változzon, kivéve ha a tesztek tényleges logikai hibát bizonyítanak.

## 3. Edzés összegzés – szív ikon

- Az egészségügyi adatok lekérésére szolgáló szív ikon kerüljön fel az „Edzés összegzés” felső sorába.
- Ne foglaljon külön nagy üres függőleges területet.
- Az alatta lévő tartalom csússzon feljebb.

## 4. „Edzés összbenyomása” blokk

- A blokk fejlécének magassága legyen kb. akkora, mint a felette lévő „Gyakorlatok visszajelzései” blokk.
- Új cím: **„Edzés összesített visszajelzése”**
- Az opcionális jelzés maradhat másodlagos, kisebb szövegként.
- A cél, hogy a fontos edzésösszegző műveletek minél kisebb telefonon is lehetőleg görgetés nélkül elférjenek.

## Elfogadás

- Aktív A/B nap egyértelmű.
- Edzésidő jól olvasható a felső vezérlő sorban.
- Szív ikon a felső összegző sorban van.
- Az összesített visszajelzés fejléc kompakt.
- A meglévő mentés, visszajelzés, health lookup és edzés-finalization működés változatlanul működik.

---

# #196 – Napló dátumtartomány-szűrő hibája

Issue:
https://github.com/fuloplevente1998/Native-AI-Studio/issues/196

## Biztos reprodukció

- Kiválasztott tartomány: 2026. nov. 1. – nov. 8.
- Az app 0 edzést mutat.
- A felhasználónak közben 2 rögzített edzése volt nov. 1. és nov. 5. között.

## Feladat

- Javítsd a Fejlődés → Napló dátumtartomány-szűrését.
- Vizsgáld meg:
  - kezdőnap inkluzív-e;
  - zárónap inkluzív-e;
  - local date / timezone / epoch konverzió;
  - mentett edzés started/end időpont és a naptár kiválasztás összehasonlítása;
  - nincs-e kettős vagy rossz kliensoldali szűrő.
- Ne módosíts mentett edzésdátumokat azért, hogy a lista működjön.

## UI egyszerűsítés

Az alsó nagy „Nincs rögzített edzés erre a szűrésre” + leírás + külön nagy „Szűrő törlése” kártya ne jelenjen meg ebben a formában.

- A felső „Szűrő törlése” vezérlő marad.
- Nulla találatnál elég egy kompakt üzenet:
  **„Nincs rögzített edzés a kiválasztott időszakban.”**

## Elfogadás

- Nov. 1–8. tartomány helyesen visszaadja az intervallumba eső edzéseket.
- Kezdő- és zárónap is helyesen kezelődik.
- DST / local-zone regresszió ne keletkezzen.
- A nagy alsó ismétlődő CTA eltűnik.
- Keresés + dátumszűrés együtt is helyesen működik.

---

# #197 – Kardió statisztikák alapnézete

Issue:
https://github.com/fuloplevente1998/Native-AI-Studio/issues/197

## Probléma

A Fejlődés → Statisztikák → Kardió statisztikák kinyitásakor a „Minden mozgás” nézet az alapértelmezett, ezért régi aktivitás (például kb. 2 héttel korábbi biciklizés) úgy kerül előre, mintha aktuális lenne.

## Feladat

- A Kardió statisztikák kinyitásakor alapértelmezetten a **„Napi aktivitás / lépések”** kártya legyen kiválasztva.
- A „Minden mozgás” továbbra is elérhető legyen manuálisan.
- A váltás ne törölje/adja hozzá újra az egészségadatokat és ne indítson felesleges többszörös provider-readet.

## Elfogadás

- Első nyitáskor Napi aktivitás/lépések.
- Régi mozgás nem jelenik meg megtévesztően alapértelmezett aktuális állapotként.
- „Minden mozgás” külön továbbra is működik.
- Provider- és adatdeduplikáció nem romlik.

---

# #198 – Globális görgetési jelzés

Issue:
https://github.com/fuloplevente1998/Native-AI-Studio/issues/198

## Feladat

Készíts egy újrahasznosítható, témakövető **scroll-more indicator** komponenst a releváns, teljes oldalas függőlegesen görgethető főképernyőkhöz.

Viselkedés:
- alul, középen kis lefelé mutató chevron/nyíl;
- csak akkor látszik, ha ténylegesen van még tartalom lejjebb;
- az oldal aljára érve eltűnik;
- nem takar ki fontos gombot vagy tartalmat;
- lehet nagyon finom, lassú animációja;
- ne villogjon;
- görgetés közben halványulhat/eltűnhet;
- ne kerüljön minden belső listára, carouselre vagy apró scrollable komponensre.

## Implementációs elv

- Egy közös komponens/segéd legyen, ne képernyőnként másolva.
- A scroll-state alapján döntsön.
- Figyelj a safe drawing / navigation bar insetre és különböző kijelzőmagasságokra.
- Accessibility szempontból ne legyen félrevezető interaktív elem, ha csak jelzés.

## Elfogadás

- Csak akkor látszik, amikor van lejjebb tartalom.
- Alul eltűnik.
- Nem takar CTA-t.
- Legalább több fő scrollos képernyőn közös komponensként működik.
- 320dp/szűk és nagy betűméretű regresszió legyen.

---

# #199 – Kültéri edzés: GPS nélkül is működjön + manuális távolság + tartós natív mentés

Issue:
https://github.com/fuloplevente1998/Native-AI-Studio/issues/199

Ez a legfontosabb működési tétel. Ne csak UI-javítás legyen.

## 1. `controllerNotArmed` GPS nélküli indítás

Jelenlegi hiba:

`A natív művelet nem alkalmazható: controllerNotArmed`

GPS kikapcsolva a „Beltéri időmérés • GPS kikapcsolva” módnak működnie kell.

Követelmény:
- GPS nélkül is induljon;
- eltelt idő működjön;
- Szünet működjön;
- Kör működjön;
- Befejezés működjön;
- ne kelljen GPS fix;
- ne gyárts fake GPS koordinátát;
- az ARM/controller/admission okot javítsd, ne csak a piros szöveget tüntesd el.

## 2. Nagy Vissza nyíl

A kültéri edzés felső kártyájában lévő nagy bal oldali Vissza nyíl kerüljön ki.

A biztonságos navigáció és aktív session-védelem maradjon.

## 3. Manuális távolság

A Távolság / Átlagtempó / Kalória-pulzus metrikák alatt legyen kompakt:

**„Távolság manuális megadása”**

- szerkeszthető az edzés befejezéséig;
- pozitív valós érték;
- hibás/negatív érték ne menthető;
- lehet km-alapú felület a meglévő lokalizáció/egységszabályokkal;
- ha van idő + manuális távolság, az átlagtempó ebből számolható.

## 4. Tartós natív adat – KÖTELEZŐ

Sem a GPS távolság, sem a manuális távolság nem maradhat csak Compose/UI state-ben.

GPS eset:
- tényleges GPS route/point adatokból számolt valós távolság kerüljön a natív edzésadatba;
- finalization után legyen Roomból visszaolvasható.

Manuális eset:
- a megadott távolság ugyanúgy kerüljön a natív edzésadatba / tartós tárolásba;
- process death / restore után se vesszen el, ha az aktív session helyreáll;
- finalization után Napló / Statisztikák / edzésösszegzés elérhesse.

Ha GPS és manuális érték egyszerre van:
- implementálj **egyértelmű, dokumentált és tesztelt precedence szabályt**;
- ne történjen csendes, megmagyarázhatatlan felülírás;
- a felhasználói kézi érték ne vesszen el váratlanul.

## Tesztminimum

- GPS OFF start → running → pause/resume → finish.
- GPS OFF nem ad `controllerNotArmed`.
- GPS ON valós point-sorozat → távolság persist.
- Manuális távolság → persist → reload/readback.
- Restore aktív session manuális távolsággal.
- Invalid manual distance.
- GPS + manual precedence.
- Finalized workout Napló/stat readback.
- Ne sérüljön owner/session/generation/finalization guard.

---

# #200 – Duplikált alapértelmezett pihenőidő

Issue:
https://github.com/fuloplevente1998/Native-AI-Studio/issues/200

## Feladat

Az „Alapértelmezett pihenőidő” már szerepel az **Edzés beállításai** részen belül.

Ezért:
- a Beállítások főoldalán / második különálló példányként ne jelenjen meg;
- csak az Edzés beállításai alatt maradjon;
- a már mentett érték maradjon;
- ne jöjjön létre új preference/DataStore kulcs;
- ne változzon a pihenőidő tényleges használata az edzésben.

## Elfogadás

- Egyetlen UI-belépési pont.
- Régi mentett érték változatlanul betöltődik.
- Módosítás után az edzés ugyanazt a közös beállítást használja.

---

# Javasolt végrehajtási sorrend

A függőségek és regressziós kockázat miatt:

1. **#199** – kültéri natív mérés/persistence/controller
2. **#196** – Napló dátum/readback (a #199 perzisztált adatainak későbbi megjelenése miatt is)
3. **#117** – Google Naptár szinkron + státusz
4. **#195** – aktív edzés + összegző UI
5. **#197** – kardió alapnézet
6. **#200** – settings duplikáció
7. **#198** – közös scroll indicator, utolsó UI-integrációként, hogy az új layoutokra is rákerüljön ahol indokolt

Ez csak ajánlott sorrend. Ha a kód tényleges függőségei mást mutatnak, dokumentáltan eltérhetsz.

---

# CI és main integráció

## PR előtt / PR-en

- `git diff --check`
- érintett regressziós tesztek
- teljes telefon/Wear/shared JVM regresszió a meglévő GitHub Actions CI-n keresztül
- ne csökkents tesztlefedettséget timeout-emeléssel, skip-pel vagy assertion gyengítéssel
- PR csak akkor merge-elhető, ha a `TrainPilot Native Android Regression Tests` sikeres

## Issue kezelés

Mind a 7 issue-ba kerüljön:
- rövid root cause / megoldás;
- PR link;
- releváns teszt;
- CI run;
- ami live készülékes ellenőrzés nélkül maradt, azt egyértelműen jelöld.

Ne zárj le egy issue-t pusztán azért, mert „kód elkészült”, ha a saját issue-ja kifejezetten olyan live elfogadást ír elő, amit nem tudtál ellenőrizni. Különösen #117-nél ne állíts sikeres Google szinkront bizonyíték nélkül.

---

# VÉGSŐ LÉPÉS – persistent signed telefon + Wear APK pair

Miután:
- a végső PR zöld;
- minden elfogadható változtatás mainre merge-elve;
- a main SHA-t visszaolvastad és rögzítetted;

indítsd el **manuálisan workflow_dispatch-csel**:

Workflow név:
`TrainPilot persistent signed APK pair`

Workflow fájl:
`.github/workflows/trainpilot-persistent-signing.yml`

GitHub Actions REST megfelelője:

`POST /repos/fuloplevente1998/Native-AI-Studio/actions/workflows/trainpilot-persistent-signing.yml/dispatches`

Body:

`{"ref":"main"}`

## Signed build elfogadás

A run:
- exact aktuális `main` SHA-t checkoutoljon;
- telefon release APK-t építsen;
- Wear release APK-t építsen;
- fusson le a teljes JVM tesztkészlet a workflow jelenlegi szerződése szerint;
- signer/package/version/Maps metaellenőrzés maradjon aktív;
- a workflow ne legyen gyengítve vagy megkerülve.

A végén dokumentáld:
- main SHA;
- signed workflow run URL/ID;
- SUCCESS/FAILURE;
- telefon APK artifact;
- Wear APK artifact;
- tesztszám és failure/error/skipped;
- signer/package/version ellenőrzés eredménye.

Ha a signed workflow hibás, ne állíts kész release-t: javítsd a valódi hibát, majd ugyanebből a végleges main állapotból vagy az azt javító új main SHA-ból futtasd újra.

---

# Done definíció

A handoff akkor tekinthető befejezettnek, ha:

- a 7 issue implementációja elkészült;
- a regressziós tesztek bekerültek;
- a PR CI zöld;
- a végleges kód mainen van;
- nincs ismert adatvesztés, fake adat vagy ownership/session regresszió;
- az issue-k állapota valós bizonyítékot tükröz;
- a final main SHA-ból a **TrainPilot persistent signed APK pair** workflow elindult;
- a signed run eredménye és artifactjai dokumentálva vannak.

Ne készíts új párhuzamos alkalmazást, új adatmotort vagy UI-only workaroundot. A meglévő TrainPilot natív architektúrát fejezd be és javítsd.

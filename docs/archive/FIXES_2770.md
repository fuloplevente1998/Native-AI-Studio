# 2770 javításgyűjtő

Felhasználói visszajelzések a 2769 · 1.3-beta után. Egy közös CI-kör a végén (felhasználói kérésre).

| # | Terület | Kérés | Állapot |
| --- | --- | --- | --- |
| 1 | Mentés és visszaállítás (lap) | JSON / ZIP / DRIVE fülváltáskor ne ugráljon a lap magassága: mindig a legmagasabb (Drive) fül magassága legyen, rögzítve. | kész (CI függőben) |
| 2 | Privát edzésfotók | Alulról nyíló lapon nyíljon (Beállításokból és a naplóból is), ne külön oldalon; teljes képes nézet maradhat teljes képernyő. | kész (CI függőben) |
| 3 | Beállítások → Edzés beállításai | Ne a Beállítások oldalon belül/felugróan, hanem az Edzés fülön már meglévő „Edzés beállításai” megnyitással azonos, teljes magas lapon nyíljon (ugyanaz a komponens). | kész (CI függőben) |
| 4 | Gyors edzés – „Váltás” legördülők (Súlyzós, Időalapú) | A lenyíló lista kövesse az app dizájnt: íves sarkok, keret (TrainPilotFrames), a mező teljes szélességében nyíljon (most keskenyebb, balra tolt menü), téma szerinti háttér. | kész (CI függőben) |
| 5 | Gyakorlatválasztó – sor 3 gombja (Videó / + / ▶) | **B változat (jóváhagyva):** a soron csak „▶ Indítás” (sárga) + ⋮; Videó és Hozzáadás a ⋮ / hosszú nyomás menüben; sorra koppintás = részletek; ☆ marad. | kész (CI függőben) |
| 6 | Gyors edzés lap | Csak a tartalom magasságáig nyíljon (ne teljes kijelző), mint a többi lap. | kész (CI függőben) |
| 7 | Programok sorrendje + programon belüli gyakorlatok sorrendje | Csak a ═ fogantyún hosszú nyomással indul a mozgatás (sor kiemelve, ujjat követi), felengedéskor egyetlen mentés; ▲/▼ nyilak elhagyva, csak a ═ fogantyú marad (jóváhagyva); kártya máshol hosszú nyomás = ⋮ menü; TalkBack „Feljebb/Lejjebb” műveletek. Megszünteti az ugrálást. | kész (CI függőben) |
| 8 | Naptár | Hónapnyilak kisebbek (48 dp érintési terület marad, kisebb ikon/keret, alacsonyabb fejléc). A hónaprács mindig 6 sor magas (rövidebb hónapnál üres sor), így hónapváltáskor (nyíl vagy húzás) nem ugrál. A tervező rész a képernyő aljára rögzítve, a havi nézet fölötte fix helyen. | kész (CI függőben) |
| 9 | Elforgatás | Forgatáskor újra lefut a logós nyitóanimáció (`com.repforge.app.MainActivity` minden onCreate-ben `StartupScreen.install`). Csak hidegindításkor (savedInstanceState == null) játsszuk le; újraépítéskor a rendszer-splash azonnal záruljon, overlay nélkül. Állapot (fül, görgetés, nyitott lap) megmarad. | kész (CI függőben) |
| 10 | Felső navigáció | Az Edzés fülről nyitott felületeken (Gyors edzés, Gyakorlatválasztó, gyakorlat részletei, kültéri edzés stb.) az Edzés fül maradjon kijelölve. | kész (CI függőben) |
| 11 | Program szerkesztő (kártyán belül) – gyakorlatsorok | Kompaktabb sorok, mint a programkártyák: kisebb belső margó (48 dp sor), kisebb név/adat betű, Részletek felirat helyett ⓘ ikon (48 dp, TalkBack-címke). | kész (CI függőben) |

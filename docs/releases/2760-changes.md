# TrainPilot 2760 · 1.1-beta – változások a 2759-es kiadás óta

Verziókód: telefon **276000**, Wear OS **276002**. A felhasználó kérte a javításokat tartalmazó új közös APK+AAB csomagot.

## Telefon

- A napló, a paletta és a katalógus közös Watch Home csomagja méretkorlátos tömörített átvitelt használ, ha nagyobb az inline keretnél. A telefon megvárja a Data Layer helyi commitját, és a háttérfrissítés újrapróbálható.
- A visszaállítás és Drive-szinkron utólagos órás projekciója nem befolyásolja a kanonikus helyi mentés sikerét. A háttérfrissítés a rögzített adatbázispéldány élettartamát követi.
- A csukott pihenősáv egyetlen 48dp sorra került vissza, ikonokkal és a helyhez igazodó időfelirattal. Szűk helyen a ±15 másodperces vezérlők a meglévő lenyitott nézetben érhetők el.
- Normál, 355dp szélességnél a sorozatszám, súly/ismétlés/RIR, pipa és törlés egy körülbelül 60dp magas sorba fér. A nagybetűs elrendezés és 48dp érintési célok megmaradtak.
- A Tárcsa/Bemelegítés művelet kisebb ikonvezérlőt kapott, így a gyakorlatcímnek több hely jut.

## Wear OS

- Az óra ugyanazt a teljes palettát veszi át, amelyet a telefonon választottak: mind a 32 paletta átviteli ellenőrzése sikeres.
- Az app megnyitáskor új Home adatcsomagot kér. A nagyobb csomagokat az összes fogadási út dekódolja; sérült vagy régi csomag nem írja felül a friss cache-t.
- A Mini napló továbbra is a legutóbbi **8** mentett edzés olvasható másolata. A telefon teljes naplója megmarad.
- Az új átviteli mód használatához mindkét appot frissíteni kell; az appadatok törlése nem szükséges.

## Ellenőrzés és állapot

A javítások előzetes teljes CI-je: [43571746498](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43571746498), alkalmazás/teszt forrás b25fd65bc0ad0c1182bb9004d32d0e735ef470db. A runner nyers JUnit-ellenőrzője szerint **496 telefonos + 189 Wear = 685 teszt**, 0 failure/error/skipped; shared NO-SOURCE. A telefonos R8 is sikeres. A valódi katalógust és maximális Mini naplót használó korábbi fixture 196440 byte-ról 8347 byte DataMapra csökkent, adatvesztés nélkül.

A közös aláírt main kiadás sikeres: [43578727032](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43578727032), forrás `fcf465785ba1bdc14f3148fa72be480a7ec9b1c2`. **685 JVM teszt, 0 failure/error/skipped**, shared NO-SOURCE. A runner ellenőrizte a package-et, verziókat, Maps-konfigurációt, páros identitást és a pinned APK/AAB aláírót. A fájlhashek és a GitHub által közölt ZIP-digestei a `2760-ci-evidence.json` fájlban szerepelnek.

A munkakörnyezet helyreállt. Ugyanezen signed futás ZIP-jeit helyben letöltöttük és a GitHub által közölt SHA-256 digesttel összevetettük. A nyers XML független összesítése **496 telefonos + 189 Wear = 685 teszt, 0 failure/error/skipped**. Mind a négy bináris hash-e és source/package/verzió metaadata egyezik; az APK-metaadatok és mindkét AAB tanúsítványa a korábbi tartós aláíróhoz ellenőrizve. A két APK-ban az új Home codec jelenléte is ellenőrzött.

A négy külön fájl, a két Play-jegyzet és az átadási bizonyíték a TrainPilot Native Drive-mappába feltöltve, név/méret/parent visszaellenőrizve. A végleges Drive-linkek és metaadat-visszaolvasás a handoffban és a `2760-verified-artifacts.json` bizonyítékban szerepelnek. A GitHub ZIP-ek tartalék letöltések:

- [Telefon + Wear APK ZIP](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43578727032/artifacts/14107842389)
- [Telefon + Wear AAB ZIP](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/43578727032/artifacts/14107921912)

Az artifactok 2026. november 21-én járnak le. A fizikai telefon/óra elfogadás külön lépés; mindkét app frissítendő.

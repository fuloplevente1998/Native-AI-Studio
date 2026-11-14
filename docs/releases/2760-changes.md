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

A közös aláírt main APK+AAB futás eredménye és a csomagok ellenőrzése a kiadás lezárásakor kerül ide. Készülékes elfogadás külön lépés.

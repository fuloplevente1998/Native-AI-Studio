# TrainPilot 2765-r2 · 1.2-beta — telefonos javító APK

Felhasználói kérésre PR263 és PR265 mainen. Signed forrás
`89dbad963160093d35a8c34e3d7a02c1c1e0dfc7`, [run44153314270](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/44153314270) SUCCESS,
`build_aab=false`. A meglévő aláírt kapu mindkét APK-t és a teljes regressziót
ellenőrizte; átadásra most a telefonos APK került.

**[Telefon APK letöltése](https://drive.google.com/file/d/10ZTjNsbngUlMb6roiq7CYl0WCNrVMUn5/view?usp=drivesdk)** ·
[Változáslista](https://drive.google.com/file/d/1YpaIyBG1nXPU4o2uPZhu8lJFEG70d0Xu/view?usp=drivesdk)

Verzió unchanged: telefon276500, Wear276502, mindkettő1.2-beta. A2765-r2 a
javító fájl jelölése, nem új Play-verziókód. Az új telefonos DEX eltér a korábbi
2765-től; az APK-ban a naptárürítés és a széles gyakorlatváltó konkrét kódjelölői
ellenőrizve. Az órás DEX byte szerint a korábbival egyezik: Wear-frissítés nem kell.
A korábbi APK/AAB fájlok megmaradtak, új AAB nem készült ehhez az APK-only kéréshez.

## Benne lévő javítások

- Szélesebb közös gombok, tartalom/betűméret szerinti műveletsorok,
  teljes szélességű gyakorlatváltó és azonos bal/jobb időmezők.
- Inaktív saját program/nap törlése történeti hivatkozások mellett, a mentett
  napló/snapshotok megőrzésével. Aktív program, builtin és élő tervek védve maradnak.
- Naptárban balra Ürítés, jobbra Tervezés. Megerősített tömeges tervtörlés,
  befejezett edzések megőrzése, cancellation és régi felhőtervek elleni védelem,
  változott preview/adatcsere elutasítása, teljes rollback írási hibánál.

## Ellenőrzés

Signed main-kapu: telefon123 XML/637 teszt + Wear36 XML/198 =835,
0failure/error/skipped, shared NO-SOURCE. Mindkét release APK R8/shrink;
letöltött két ZIP SHA-256, nyers XML egyezése, tényleges manifest/verzió,
fájlhash és tartós signer ellenőrizve. Maps/páros identitás/Wear-companion kapu zöld.
Package: com.TrainPilot.com. Tartós signer SHA-256:
`a72fd016d94e9d94e2b2282032f8c3820d4f09cba9ff1db05c3fd1de6ffa7dd6`.
Telefon APK: 5562155 bájt,
SHA-256 `44ed336e5a5d388399a574ba073a3d11a7f1de73305bced02facdf70df3b4f1d`.

Drive-on három új fájl: telefon APK, külön változáslista, ellenőrzési bizonyíték;
név/méret/parent visszaolvasva, korábbi fájlok megőrizve.
[Bináris bizonyíték](2765-r2-verified-apks.json) · [Drive-átadás](2765-r2-drive-delivery.json).

A legelső44153159433/a68f17e signed futás a második PR összevonása előtt
indult, ezért leállítottuk és nem adtunk át belőle APK-t. A PR265 retarget után
csak az átadó dokumentum ütközött; az alkalmazás-/teszt-/buildforrás a korábban
835 teszttel ellenőrzött8d18b76 forrással egyezik. A fenti új futás már a teljes
main csomagot ellenőrizte. A lezáró dokumentáció nem változtatja a kódot.
Fizikai készülékes elfogadás külön hátravan; új Play-feltöltés nem történt.

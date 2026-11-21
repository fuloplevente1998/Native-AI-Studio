# Aktuális átadó

2026-11-21 UTC. Forrás és tesztbizonyíték alapján frissítendő pillanatkép.

- Main `aed077beccc0beab9c377381cc3cc955c64921e9`, #307 fotógaléria és #308 CI automatizálás. Kiadás 1.4.1, telefon 278900, Wear 278902.
- Ugyanezen main teljes regressziója és aláírt buildje sikeres.
  [Regresszió](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/44850878401) · [Aláírt build](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/44851025435)
- A #310 javításait a #311 hozza mainre. Kiadás 1.4.1, telefon 279000, Wear 279002. Tételes áttekintés a [#310 auditban](docs/AUDIT_310.md).
- A verziókód módosítása előtti PR-forráson 992 JVM-teszt és a release R8 sikeres. [PR CI](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/44940870322)
- A [#309 Health Connect PR](https://github.com/fuloplevente1998/Native-AI-Studio/pull/309) külön draft, készülékes próba függőben.
- Az ág ellenőrzése `trainpilot-android-ci.yml` teljes mód, JVM app/Wear/shared és release R8. Mainre integrálás csak a kiadás jóváhagyása után.
- A mainen a sikeres teljes regresszió azonos SHA-ról indítja az APK/AAB buildet. A Play-feltöltés külön lépés.

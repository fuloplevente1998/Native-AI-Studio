# #247 — első indítás ugyanazzal a jóváhagyott profil/terv panellel

Felhasználói készülékes jelzés, 2026-11-16. Main alap: 17ecad0
(2763-as alkalmazáskód + új auditdokumentum). A folytatott onboarding a régi
TrainingProfileDialog-ot, a Home a teljes magasságú PersonalPlannerModalDialog-ot
nyitotta. A korábbi NativeFirstLaunchGateRegressionTest csak a kihagyást és a
migrált felhasználó átengedését fedte le, nem a Folytatás → profil → mentés utat.

## Változás

- Egyetlen StudioPersonalPlannerDialog köti a jóváhagyott panelt a meglévő
  ViewModelhez; ezt használja a Home és az első indítás is.
- A profil és a generált A/B terv a meglévő, programWriteMutex és
  NativeDataReplacement által védett Room/DataStore mentési úton marad.
- Első indításkor az onboarding COMPLETE állapota csak a profil, a programnapok
  és az aktív program tartós mentése után, az eredeti generation-ticket és
  profil-CAS ellenőrzéssel íródik. A korábbi profil ismeretlen mezői megmaradnak.
- Mentés közben a megerősítés, X, Mégse és lehúzás nem zárja be a panelt.
  Hiba esetén a panel és a szerkesztett értékek megmaradnak; a hiba helyben látszik.
- Az onboarding folytatott profiloldala nem zárja be a panelt pusztán a
  profileJson-frissítés miatt. A skip és meglévő felhasználók szabálya változatlan.

## Ellenőrzés

Új FirstRunPlannerRegressionTest: tényleges gate → Folytatás → új panel →
értékválasztás → mentés → Room/DataStore + kezdőlap; bezárás mentés nélkül;
megszakadt onboarding folytatása és ismeretlen profilmező megőrzése;
hiányzó perzisztencia esetén helyben maradó hiba. A meglévő panelteszt új esete
a mentés alatti dupla kattintást és bezárást ellenőrzi. A két korábbi gate-teszt
a produkciós, közös ViewModel-paraméterre igazítva.

A teljes meglévő Actions CI43814942285 SUCCESS, tesztelt alkalmazás-/tesztforrás
23db67c149a204dd9864cf386ce5b8d110a0165b. Artifact-ZIP SHA-256 digest és nyers XML
helyben ellenőrizve: telefon100 fájl/521 teszt + Wear35/189 =710,
0failure/error/skipped; shared NO-SOURCE. A négy új elsőindítás-teszt, a hat
profil/egyedi-gyakorlat panelteszt és a két korábbi gate-teszt mind zöld.
Bizonyíték: verification/PR248-ci-evidence.json. A záró commit csak dokumentáció,
az alkalmazás- és tesztforrás változatlan.
Új APK/AAB, verzióemelés, main-merge és fizikai elfogadás nincs ebben a körben.

## Kapcsolat az új audittal

A docs/AUDIT_2763_FULL_2026-11-15.md tizennégy megállapítása külön ellenőrizendő;
ez a javítás nem jelenti azok automatikus elfogadását vagy megoldását.
Az audit bizonyítékoszlopában szereplő 24899374804/24899980747 futásazonosítók
és a 30:e0… aláíró nem egyeznek a tényleges 2763-as átadási bizonyítékkal:
APK-run43675171894, AAB-run43721805883, tartós signer a72fd016…ffa7dd6.
A tényleges CI mindkét kiadási körben 705 tesztet igazolt, készülékes
hibamentességet nem. Az eredeti auditdokumentumot nem írtuk át.

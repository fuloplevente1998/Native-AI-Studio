# Aktuális átadó

- Alap és korábbi kiadás 2791, `d57bd17585d77feab0c0dfa37d7295738083fce6`, #313. [Aláírt 2791](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/44968021745)
- A #312 folytatása a `feat/312-journal-top-nav` ágon. A kiadás a további javítások után következik.
- A Napló a felső ikonsorban nyílik. A Fejlődésen az áttekintés és a statisztikák maradnak. A kiválasztott edzés és térkép visszatérési útvonala megmarad.
- A Fejlődés az egyértelmű katalóguscélok alapján külön számolja a részletes izomcsoportok befejezett sorozatait. A kijelölt csoport összegző sora kompaktabb.
- A sikeres teljes main JVM/R8 ellenőrzés azonos SHA-ról indítja az aláírt APK/AAB buildet.

- #312/8 + /11: a #316 draft PR (`fix/312-progress-volume-pr-compact`) a `feat/312-journal-top-nav` ágra épül. Az Edzésvolumen csukva alacsonyabb, a 4 × 48 dp időszakválasztó egy soros, a mini grafikon tengely nélkül jelenik meg. Cím/grafikon/nyíl megnyit, az oszlopkijelölés 90 napnál a részletes nézetben látható marad. A PR-panel kompakt és hosszú listán belül görgethető; az eredeti adatok megmaradnak.
- Ellenőrzés: diff- és bekötési statikus ellenőrzés 12/12 rendben, négy célzott Robolectric/Compose regresszió hozzáadva, **még nem futtatva**. A #314 kiinduló ág korábbi 44969651338 CI futása hibás; teljes regresszió/készülékes vizsgálat/összeillesztés külön. Main-merge/kiadás nincs.

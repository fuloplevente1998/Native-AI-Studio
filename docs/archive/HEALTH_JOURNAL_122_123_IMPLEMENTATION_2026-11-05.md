# Egységes egészségnapló – #122 / #123

A két egymást követő egészségszekció és a külön részletoldal helyett az eredeti,
kompakt keretes Egészségnapló egyetlen bejárata marad. A napi kártya lenyitása
helyben mutatja a 26 meglévő metrikát, a valódi rekordokat és az alvásfázisokat.
A Studio külön részletgombja és navigációja megszűnik; a közös rekordkártyát
használó másik natív dashboard adaptere megmarad.

## Adat- és állapotszerződés

- Egy kiválasztott provider/origin/helyi nap mentett projekcióját kapja a napi
  összefoglaló és a 26 metrika. Samsung és HC értékek nem adódnak össze;
  a meglévő szolgáltatói prioritásos összesítő érintetlen marad.
- Hiányzó origin-napi összesítő esetén az érték ismeretlen: a nyers rekordokból
  nem készül új napi képlet. Az origin tényleges napi rekordjai ekkor is elérhetők.
  Más nap/provider nem pótolja a hiányt. A valódi 0, engedélyhiány és olvasási hiba
  külön állapot. A rendelkezésre álló mérésidő és forrás megmarad; a rossz naphoz
  vagy jövőhöz tartozó mérési idő nem kerül be az értékek közé.
- Egyszerre 7 helyi nap, naponta 25 rekord és rekordnyitásban 20 minta/alvásfázis
  jelenik meg. A lapozás az eredeti stabil idő/row-ID kurzort használja.
  A nap összecsukása megtartja a lapot és a folyamatban lévő olvasást;
  az új restore-generáció üríti a részletcache-t és kurzort.
- Csak meglévő helyi store-olvasások történnek. Nincs SDK-szinkron, új adatbázis,
  schema/DAO-módosítás, mérési írás, consent-változás vagy új health-képlet.

## Dátumválasztás

A közös `CompactCalendarDialog` és az app palettája/keretei adják a hétfővel
kezdődő naptárrácsot. Hónap- és évléptetés, kiválasztott nap, piros bezárás,
Mégse és megerősítés elérhető. Mégse nem írja át a tartományt; az alkalmazás
megerősítés után is külön validálja a kezdő-/zárónapot. Az eredeti helyi dátum és
forrás megmarad. Az érintett health-útvonalakon nincs `android.app.DatePickerDialog`.

## Ellenőrzés és átadás

16 új regresszió tesztforrása készült: 9 store/adapter (provider/origin/nap,
tüköradatok, 57 azonos idejű rekord stabil 25/25/7 lapozása, idempotens import,
restore, missing/0/denied/error, eredet/idő, alvási unió, 20-as részletlapozás,
DST), 3 Compose naplóinterakció és 4 naptárinterakció a Matt/Kontrasztos/Glow/
Világos témákkal, négy nyelven, 320 dp szélességen és kétszeres betűmérettel.

A forrás diff/whitespace ellenőrzése megtörtént; a tesztek nem futottak helyben
és külön ági CI sem indult. A felhasználó által kért közös main-integráció utáni
egyetlen CI-kör ellenőrzi a fordítást és regressziókat. Ez még nem készülékes
Samsung/HC/adat- és vizuális elfogadás. Verzió, signer és APK nem változott.

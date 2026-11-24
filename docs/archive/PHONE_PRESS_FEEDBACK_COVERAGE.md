# Telefonos lenyomásjelzés — PR268 gyűjtő, 2026-11-17

A korábbi ButtonPressFeedbackRegressionTest a Material-gomb wrappert,
a RoundedPressFeedbackRegressionTest az X vizuális/érintési felületét védte.
Ez nem fedte le a külön foundation.clickable sorokat. A témacsoportokon a
12dp belső margón belül elhelyezett Row téglalapos ripple-t festett.

## Implementáció

- A négy témacsoport fejléce most teljes szélességű, a 12dp tartalmi margó
  a formát követő kattintás/rajzolás után van. A választék külön gyerek;
  a palettaválasztás nem aktiválja újra a csoportfejlécet.
- A telefon teljes Kotlin-forrásában átnézett önálló clickable műveletek
  a meglévő trainPilotClickable megoldást használják: közös, íves felületet
  követő halvány jelzés, rövid koppintás, fókusz és hover támogatás.
  Kezdőlap, felső navigáció, programok, naptár, gyakorlatválasztó, aktív edzés,
  mezőválasztók, fotók, napló, fejlődés, egészség és beállítások is érintettek;
  a még elérhető korábbi natív felületek is szerepelnek az átvezetésben.
- Eredeti alak, tartalom, művelet, enabled feltétel, szemantikus címkék és
  tesztazonosítók megmaradnak; nincs globális sugár-/szín-/méretcsere.
- A kisebb törlésikon és izomtérkép-időszakjelző továbbra is nagyobb érintési
  célpontot tart: a szülőn nincs ripple, a megosztott interakcióállapotot a
  tényleges, alak szerint vágott vizuális gyerek rajzolja.
- A grafikon oszlopainak/anatómiai rajznak kiválasztási jelzése és a befejezési
  műveletet védő inputfogó réteg külön szerep; nem gombként alakítottuk át őket.
  A már külön vizuális gyermekre kötött X/videó interakció megmarad.
- Wear OS kód változatlan.

## Lefedettség és állapot

Új valódi SettingsMainScreen pixelregresszió: mind a négy zárt témacsoport
érintési mérete egyezik a festett kártyával, a jelzés a szélső belső margón is
látható, a külső íves sarok változatlan. A fejléc megnyitható, gyerekpaletta
kiválasztható. Az új törlésikon-teszt a 48dp érintési keret átlátszó részéről
indított koppintásnál is csak a kisebb vizuális gombon vár visszajelzést.

PhonePressFeedbackUsageRegressionTest a teljes app/src/main/java készletet
vizsgálja: új ad hoc clickable csak közös formakövető megoldáson keresztül,
vagy explicit, dokumentált kivételként kerülhet be. A nagyobb érintési
célpontoknak csak indication=null szülő és külön vizuális jelzés megengedett.
Ez architekturális őr; a tényleges rajzolást a pixeltesztek ellenőrzik.

Statikus diff --check és forráshasználat-áttekintés rendben (171 közös
trainPilotClickable hívás/definíció; nem tesztszám). A regressziók **még nem
futottak**. A felhasználó kérésére továbbra sincs CI/build/main-merge/verzióemelés;
fordítás, teljes JVM-kapu és készülékes vizuális elfogadás hátravan.

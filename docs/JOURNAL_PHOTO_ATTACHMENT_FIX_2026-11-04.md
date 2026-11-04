# Naplós fotóhozzáadás – adatmegőrzési javítás

A `bbd7ad7` AI Studio import kamerás és galériás naplós fotómentése
`insertWorkout(...copy(rawJson = ...))` hívást használt. A DAO `REPLACE`
konfliktuskezelése a szülő edzéssort törli és újra beszúrja. Az `ON DELETE CASCADE`
miatt ezzel a régi gyakorlatok, sorozatok és az éppen beszúrt fotó is törlődhetett.
A SQLite reprodukció igazolja a kaszkádot; a sikeres fordítás ezt nem észleli.

A `NativeJournalPhotos` közös kamera/galéria útvonala egyetlen Room-tranzakcióban
frissíti a szülő `rawJson` mezőjét UPDATE-tel, és beszúrja az új fotót. Megőrzi
az összes többi oszlopot, relációt és korábbi fotót. A duplikált fotó visszagörgeti
az egész műveletet. Az edzésazonosító, kategória és adatcsere-jegy a külső
kamera/galéria indításakor rögzül. Törölt, tombstone-nal jelölt, aktív vagy
közben visszaállított adatkészlethez tartozó edzés nem kap késői fotóírást.
A felület a hibát jelzi, a coroutine-megszakítást továbbadja.

Regresszió: `JournalPhotoAttachmentRegressionTest` – meglévő gyakorlat/sorozat,
fotók és idegen edzés megőrzése; három kategória; duplikált fotó atomi rollback;
késői callback törlés, tombstone és restore után. A tényleges kamera/galéria és
készülékes megjelenítés továbbra is telefonos tesztet igényel.

Verziókód, package, signer, motor, séma és migráció változatlan. CI/APK állapotot
csak a befejezett futás és letöltött nyers eredmények ellenőrzése után jelentünk.

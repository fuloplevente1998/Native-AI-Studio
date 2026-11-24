# Health Connect row_offset hiba — 2026-11-17

Készülékes képen a Health Connect változástoken-kérése SQLite hibával áll le:
`change_log_request_table has no column named row_offset`. Ez szolgáltatói
sémahiba; nem engedélyhiány, és az app nem javítja/reseteli a rendszer adatbázisát.

A kizárólag HC-adapterhez bekötött, pontos hibaillesztés felismeri a becsomagolt
kivételt. Token létrehozásakor, első változásoldalnál és a teljes olvasást követő
változásellenőrzésnél is megengedett a tartalék napi olvasás. Az ismert 30 napos
ablak, a tartós dirty napok és már megismert változásnapok olvashatók; a meglévő
530 napos korlát marad. Ez nem teljes történeti/deletions egyeztetés.

A napi adatok a meglévő SDK-adapteren, foreground/engedély/szolgáltató/időzóna/
generation és publication guard mellett tárolódnak. Az előző checkpoint/token
és sikeridő nem változik: ismeretlen törléseket/régi változásokat nem nyugtázunk.
A jelentés korlátozott/részleges, nem teljes siker. A valódi napi olvasási hibák
megmaradnak; nincs adatkitalálás, nullázás, másik szolgáltatóra váltás vagy új
háttérpróbálkozási ciklus. A rendszer helyreállása után az eredeti token ismét
használható, vagy a meglévő lejárt-token ág teljes olvasást végez.

A telefonos fő- és részletes health felület és a többforrásos eredmény HU/EN/DE/RO
érthető üzenetet mutat; nyers SQL nem kerül a hibaüzenetbe. Rendszer/HC frissítés
ellenőrzését javasoljuk, adatvesztő újratelepítést nem.

Regressziók: létrehozás/első poll/catch-up hiba, napi részleges hiba, eredeti
checkpoint megőrzése, későbbi egészséges futás, cancellation/permission/különböző
SQLite hiba elutasítása, foreground elvesztése, lokalizált technikaihiba-szűrés.
**Tesztfuttatás/CI/build szünetel a felhasználó kérésére.** Csak `git diff --check`
futott; fordítás és készülékes HC-ellenőrzés még hátravan. PR268 gyűjtő része,
nincs main-integráció, verzióemelés vagy új APK/AAB.

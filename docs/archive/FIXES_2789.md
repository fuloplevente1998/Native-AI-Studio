# 2789 – Health Connect engedélykezelő javítás

| Hol | Visszajelzés | Javaslat / változtatás | Állapot |
|---|---|---|---|
| Beállítások → Adatvédelem és engedélyek → Health Connect | A megnyitás `Permission Denial ... MANAGE_HEALTH_PERMISSIONS ... requires GRANT_RUNTIME_PERMISSIONS` hibával megszakad Samsung Androidon | Nem privilegizált Health Connect beállítási út; a hiányzó jogosultságokat közvetlen engedélykérővel kérjük, teljes grantnál HC Settings, nem elérhető/tiltott beállításnál saját alkalmazás-beállítás fallback | Javítóág; CI és készülékes próba szükséges |
| Egészség → Engedélyek, Beállítások → Health Connect | Engedélykérésre kattintva nem nyílik meg rendszerengedélykérő, mert a gomb a szinkronnal osztozik a korábbi `anyReadAllowed` kapun | Külön, szinkron nélküli művelet, részleges engedély esetén csak a hiányzó olvasási jogosultságok; a meglévő grantok nem vesznek el | Javítóág; CI és készülékes próba szükséges |

**Megőrzés:** nincs adat-/Room-/DataStore-/tombstone-/aláírás-/verziókód-változás. A normál Health Connect szinkron gomb változatlan.

**Elfogadás:** a Settings és Health gomb egyaránt megnyitja a megfelelő engedélykérést vagy beállítást; nincs `GRANT_RUNTIME_PERMISSIONS` SecurityException; nulla/részleges/teljes jogosultság, elutasítás és hiányzó beállítási Activity kezelése; PR JVM+R8 CI zöld, majd fizikai Samsung készülékes ellenőrzés.

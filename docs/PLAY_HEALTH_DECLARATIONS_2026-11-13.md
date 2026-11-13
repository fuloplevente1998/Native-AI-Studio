# Play Health / FGS forrásmátrix –2026-11-13

Ez a meglévő deklarációk kódauditja, nem új engedélykérés és nem Google-jóváhagyás.
A Console Data safety/Health apps/Health Connect/FGS űrlapjait a tényleges
funkciók és adatkezelés szerint kell kitölteni; nem módosítjuk találgatás alapján.

| Engedélycsoport | Tényleges útvonal és felhasználói cél |
|---|---|
| 17 HC olvasási típus | HealthConnectRepository.READ_PERMS és Api34 olvasó/összesítő. Pulzus,nyugalmi pulzus,aktív/összes kcal,edzés,alvás,HRV,lépések,súly,testzsír,SpO2,VO2max,táv,sebesség,vérnyomás,vércukor,légzésszám. Kiválasztott szolgáltató napi/napló/edzés megjelenítése. |
| READ_HEALTH_DATA_IN_BACKGROUND | HealthBackgroundJob támogatási és külön engedélyvizsgálat; időszakos egészségnapló frissítése. Nem automatikusan granted. |
| WRITE_EXERCISE | HealthConnectRepository.Api34.write: ExerciseSessionRecord, stabil clientRecordId/clientVersion; külön írásengedély szükséges. Nem általános egészségadat-írás. |
| FGS location | DistanceTrackingService: felhasználó által elindított kültéri edzés GPS táv/útvonal, látható értesítéssel. |
| FGS specialUse | IntervalTimerService; a manifest subtype már megadja a workout countdown/audio-haptic/Wear clock projection célt. Console-hoz ugyanezt és a tényleges indítás/leállítás folyamatát kell bemutatni. |
| FGS connectedDevice | HealthBackgroundService BLE kapcsolat és adatlekérés, BleConnectionLease és configured/permission ellenőrzéssel. Nem a HC időszakos job helyettesítője. |
| Fotók | Saját privát tárhely; Android13+ rendszeres fotóválasztó. Nincs READ_MEDIA_*/széles tárolási engedély. Kamera külön felhasználói akció. |
| Egészségadat felhőmentése | TrainPilotSettingsStore healthBackupConsent alapból false. Nem állítható be automatikusan a kiadási űrlap kedvéért. |

Az olvasási engedélyeket nem töröltük pusztán a számuk miatt: van megfelelő
rekordolvasó funkció. A WRITE_EXERCISE és background jogosultságok továbbra is
külön vannak a17 olvasás listájától. A permission rationale és usage alias
exportálása platformkövetelmény; csak a kaput megkerülő StudioActivity lett lezárva.

Készülékes és Console-beli ellenőrzés szükséges: jogosultság megtagadása/visszavonása,
háttérfrissítés és értesítés/leállítás, egészségadat nélküli mód, SamsungSDK opcionális
elérhetősége, felhőmentés beleegyezés, tényleges store deklaráció és videóbizonyíték.

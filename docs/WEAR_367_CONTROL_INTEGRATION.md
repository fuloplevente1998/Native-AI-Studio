# #367 – Wear OS integrációs átadó

**PR:** https://github.com/fuloplevente1998/Native-AI-Studio/pull/368  
**Ág:** feature/367-wear-redesign  
**Kiinduló main:** 0affd4204a33cc308d7389ac84feb3feca4ae28a  
**Utolsó kódcommit:** 55900fadec9aed1e7f6b3aa871d8d64c4c26e7d8  
**Átadási határ:** a négy párhuzamos PR összeillesztése és a közös teszt/build az integrátor feladata.

## Megvalósított elemek
- V3 kezdőlap: óra, telefon, akku/steps ív, napi cél, valós mérés/szinkron, kiemelt indítás, beállítás, menü.
- Főoldali lapozás Home → Journal → Health → Calendar, koronával is; peremvédett gesztus. Napló tényleges helyi tükörből, mai napot kiemelő naptár, napi órás passzív adatok.
- Edzés: nagy értékek, meglévő szerkesztő/korona, előző teljesítmény, Kész, 3 mp inaktivitás után eltűnő vezérlő-overlay és külön vezérlőlap, külön befejezés/elvetés megerősítés. Meglévő érintészár és hosszan tartott feloldás megőrizve; a fizikai gomb/korona feloldási viselkedését készüléken ellenőrizni kell.
- Összegzés: lezárás előtti előnézet, tényleges completed set és súlyvolumen, mérési adatok, külön aktív/összes energia; lezárás után csak valódi receipt jelzi a szinkront.
- Pihenő: ±15 mp a telefon meglévő lépésköze szerint (a korai mockup +30 mp-ét nem dupláztuk), 0–600 mp hátralévő idő növelési határ; egyetlen időzítő-tulajdonos; 10 mp előjelzés + meglévő végjelzés és utolsó három másodperc automatikus jelzése. Automatikus jelzés a meglévő vibration beállítást követi; manuális pihenő korábbi alapértelmezett rezgése megmarad.
- Napi adatok: Health Services PassiveMonitoringClient, helyi cache, explicit opt-in, ACTIVITY_RECOGNITION; lépés/kcal/táv az óráról, lépéscél a telefonról. Pulzus foreground passzív callback csak megfelelő szenzorengedéllyel; alvás utolsó megfigyelt intervallum, nem teljes Samsung Health alvási összesítő. Nem támogatott/hiányzó adat „–”, nincs becsült adat vagy napi cache feltöltése.

## Szerződés és integrációs pontok
- Telefonos home snapshot: opcionális dailyStepsGoal, controlProtocol367=6. Régi telefon esetén a nem támogatott vezérlőgombok tiltva/fallback. A telefon továbbra is a cél egyetlen forrása.
- Protocol 1: watch-owned manual rest/pause, watchControl367. Mutáció + pontos parancs helyben tartósan együtt mentve; controlIntent367 helyi, nem wire mező. Outbox egyedi ID/sequence, nem összevonható; csak kanonikus receipt ACK. Freeze/resume meglévő út; owner epoch/generation/pauseRevision védelem.
- Protocol 2: phonePause367 explicit pause/resume, PhoneWorkoutExecution.setWorkoutPause, tranzakcióban remotePause367 proof.
- Protocol 3: phoneRest367 legacy rest-only; IntervalTimerService.adjustRest, key/phase/duration guard.
- Protocol 4: watch-owned automaticRest; aktuális remaining módosítása, elapsed elszámolása előbb; későbbi konfiguráció változatlan.
- Protocol 5: telefonos többlépcsős pihenő; phoneRest367 exact phaseDurationMs/restRevision, adjustPhaseRest367; restOverridesMs367 a közös idővonal opcionális mezője (initial, only, step:round), 24h összkorlát; timer checkpoint proof a mentés előtt nem ACK-olható. Régi clock projection és régi rest-only út megőrizve.
- Protocol 6: transferredRest watchControl367, átadott checkpoint helyi módosítása ugyanazon idővonalon, valós ledger és pozíció megőrzése; NativeTransferredRestAdmission367 rekonstruálja a fázist és validálja a proof/ledger értékeket. A telefon/óra olvasói a felülírásokat továbbadják.
- Kötelező megőrizni az epoch/generation/receipt, tombstone, replacement lease, undo és finalization guardokat. Ne írjuk újra az ownership journal vagy aktív mérési motort.

### Célzottan egyeztetendő közös fájlok
- Wear: MainActivity.kt, WorkoutSnapshotStore.kt, WearOwnership193.kt, WearAutomatic.kt, WearAutomaticService.kt, WearTransferredTimer193.kt, WearDataListenerService.kt, WatchHomeStore.kt, WearSaveSyncState.kt, AndroidManifest.xml, ui/WearAppRoot.kt. A #351 méréshez tartozó integrációval a meglévő Health/GPS lifecycle megtartandó.
- Telefon: WearSyncRepository.kt (home snapshot és új command dispatch), VmAppearance.kt (lépéscél frissítés), PhoneWorkoutExecutionStore.kt, PhoneWorkoutEngine.kt, IntervalTimerService.kt, PhoneSharedBridges.kt, NativeWatchDiscardAdmission.kt, NativeWatchControlAdmission367.kt, NativeTransferredRestAdmission367.kt, PhoneRemotePause367.kt, PhoneRemoteRest367.kt. A #359 import és a témák módosításaival a már létező felelősségek megőrzendők.
- Shared: IntervalTimeline193.kt (opcionális phase override és haptic cue). Handoff/CODEMAP dokumentációt egyesítse az integrátor, ne felülírja a másik három PR információit.
- A lista integrációs hotspot, nem párhuzamos PR diff-ellenőrzés; más ágakhoz nem nyúltunk.

## Ellenőrzési állapot
- 534197e / Actions 45478730820: teljes JVM és R8 siker; 881 app + 227 Wear = 1108 teszt, 0 failure/error/skipped; shared jelentés 0 teszt. Korábbi telefonos PaletteSystemBarsTest timeout ebben a futásban nem ismétlődött; tesztet nem gyengítettünk.
- 55900fa: 10 mp haptic kiegészítés, új WearAutomaticTest; utolsó kódcommit ellenőrzése még nyitott. A dokumentációs commit nem módosít futtatható kódot.
- Render smoke test: 176/192/216dp, HU/EN/DE/RO, 130% font. Vizuális screenshot-inspekció és fizikai Galaxy Watch nincs igazolva. A másodlagos sűrű naptárrács kisebb gombjai és a legkisebb kerek kijelzőn a valódi érintési terület ellenőrzendők.
- Készülékes checklist az integrátornak: fő navigáció és edge-back, korona, overlay tap/3s és érintészár; napi engedély és valódi adatok; phone/watch-owned pause/resume, rest ± minden ownership úton, 0/600 határ, haptics, hálózatvesztés + replay, process death, visszaátadás, mentés/elvetés/receipt, téma/nyelv, fizikai méretek.
- A CI zöldsége nem fizikai elfogadás. Az integrátor a négy PR összeillesztése után közös teljes CI-vel ellenőrizze a végső main-jelöltet. Nincs main merge, release, verzióemelés vagy issue-close ezen az ágon.

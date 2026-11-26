# TrainPilot Native – 2814 átadó

- Alap main `dc5c361e`, #399 merge; ág `fix/collector-2814`, draft #400, 1.5.3 / 280900–280902.
- #790 zöld (1287/0/0/0, R8); a korábbi kompakt visszajelző és súly/ismétlés/RIR/törlés 14 dp / 1 dp menü változatlanul megmarad.
- Jóváhagyott teljes telefonos Liquid Drop audit: `docs/FIXES_2814.md`, L0–L8. Kyant Button/Selection erősebb lencse, depth és shadow; közös `LiquidDropMaterial` saját ablak/route/power/API kapu, olcsó cache-elt domború perem sűrű listákon.
- Közös gombok, input határ, chipek, aktív sorozat/stopper/pihenő/dokk, health/Home, program, Coach, napló, naptár, progress/statisztika, settings célzott felületei.
- API 31–32 blur, 33+ lens; energiatakarékos és külön ablak belső vezérlők olcsó paint; GPS map, Wear és motor változatlan; nem minden csempe önálló lencse.
- Korábbi regressziók megtartva, új `LiquidDrop2814Test.kt`. Közös CI az új head-en ellenőrizendő; fizikai 8 paletta/slider/IME/jank és GPU-mérés még hátravan. Egyedi grafikonvezérlők, témaeditor, kapcsoló thumb és GPS további készülékes auditja szükséges.
- Nincs merge, verzióemelés, aláírt build.

- #791: 1290/0/0/0, telefon 1011, Wear 276, shared 3; R8 zöld, feltételes DEX skipped. Új head: GPS panelek, egészség ikongomb, switch thumb; Samsung edzésintervallum active/total és permission javítás. `docs/WORKOUT_HEALTH_2814.md` H1–H4 külön nyitott feladat: automatikus élő és lezárás utáni csatolás, késői reconcile, napi napló audit. CI újra ellenőrizendő.

- H2 első passz: `WorkoutHealthAutoReconcile.kt` a meglévő foreground provider flight végén, 48h/4 query/15min throttle, mentett aktív intervallumok, autoOnOpen+privacy guard, nincs mentésfüggőség. `WorkoutHealthAuto2814Test` policy regresszió. H1/H3/H4 nyitott; új CI ellenőrizendő.

- H1/H3 első passz: `StudioWorkoutHealthPreviews.kt` 2 perces foreground preview, mutexelt provider read, session/node/epoch/freshness, nested Wear, külön aktív/total detail projekció, mindkét provider késői retry. `WorkoutLiveHealth2814Test`. Napi napló hiánya és valós eszközös H4 nyitott. #792 cancelled, #793 R8 zöld, JVM ellenőrizendő; új head CI szükséges.

- #794 R8 zöld, két JVM hiba: régi total UI tag és MainActivity line cap; fix a meglévő test/cap megőrzésével. `StudioHealthRootEffects.kt`, `HealthWorkoutEnergyDay.kt`: mentett provider/ablak energia külön naplórész, nem raw rekord, nem napi összeg. Új CI ellenőrizendő.

- Napló energia projection regresszió: tényleges Room fixture, HC 100 kcal, másik provider, megváltozott aktív ablak és külső edzés kizárva; meglévő UI tesztben mindkét energia assertion.

- #796 head 973de52f zöld, 1296/0/0/0, R8. Utolsó célzott javítás: késői részleges energia befogadása csak meglévő top-level és per-window mezők megőrzésével; stale ablak újraolvasás, prepare interval guard, pontos HC hiány üzenet. Új CI ellenőrizendő; nincs merge/build.

- #797 zöld (nyers XML ellenőrizendő). Régi 1.5.3 képernyős reprodukció: Samsung napi energia megvan, nyers rekord hiány. Grouped SDK activity summary active/total napi JSON, külön órás naplómegjelenítés, új `SamsungHourlyEnergy2814Test`; új CI és valós SDK/bucket/latency ellenőrzés szükséges.

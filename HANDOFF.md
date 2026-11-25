# TrainPilot Native – 2814 átadó

- Alap main `dc5c361e`, #399 merge; ág `fix/collector-2814`, draft #400, 1.5.3 / 280900–280902.
- #790 zöld (1287/0/0/0, R8); a korábbi kompakt visszajelző és súly/ismétlés/RIR/törlés 14 dp / 1 dp menü változatlanul megmarad.
- Jóváhagyott teljes telefonos Liquid Drop audit: `docs/FIXES_2814.md`, L0–L8. Kyant Button/Selection erősebb lencse, depth és shadow; közös `LiquidDropMaterial` saját ablak/route/power/API kapu, olcsó cache-elt domború perem sűrű listákon.
- Közös gombok, input határ, chipek, aktív sorozat/stopper/pihenő/dokk, health/Home, program, Coach, napló, naptár, progress/statisztika, settings célzott felületei.
- API 31–32 blur, 33+ lens; energiatakarékos és külön ablak belső vezérlők olcsó paint; GPS map, Wear és motor változatlan; nem minden csempe önálló lencse.
- Korábbi regressziók megtartva, új `LiquidDrop2814Test.kt`. Közös CI az új head-en ellenőrizendő; fizikai 8 paletta/slider/IME/jank és GPU-mérés még hátravan. Egyedi grafikonvezérlők, témaeditor, kapcsoló thumb és GPS további készülékes auditja szükséges.
- Nincs merge, verzióemelés, aláírt build.

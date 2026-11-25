# TrainPilot Native – 2814 átadó

- Main alap: `dc5c361e` (#399 merge); ág `fix/collector-2814`, draft PR #400; verzió 1.5.3 (280900/280902).
- Felhasználó mindkét (glass és matt) visszajelző mockupot jóváhagyta; `docs/FIXES_2814.md`.
- Kompakt, külön post-exercise feedback popup: szemantikus színek, explicit pipa, adaptív 3/2/1 oszlop, közvetlen jegyzet, törlés, két akció; eredeti session/generation/gyakorlat kulcs és callbacks.
- Célzott, statikus saját üvegablak csak megfelelő route/API/optika mellett; mattra visszaesés; nem nyitjuk meg a teljes mérés optikai kapuját.
- Súly/ismétlés/RIR/törlés dropdown: valódi 14 dp popup shape, 1 dp perem, glass/Glow peremfény; négyszögletes modifier-háttér elhagyva. Kétoldali editor is célzott ablakstílus.
- A korábbi inline, summary és napló visszajelző nem változik. Az azonnali sorozattörlő gomb helyett a meglévő menü marad; mentés/adatok/motor/Wear változatlan.
- Regresszió: `WorkoutEditorAppearance2814Test.kt` és a meglévő StudioActiveFeedbackRegressionTest; CI ellenőrizendő. Fizikai készülékes megjelenés, IME, nagy betű és optika külön ellenőrzendő.
- Nincs merge, verzióemelés vagy signed build.

# 2777 – Wear OS app átnézés (Galaxy Watch 7 40 mm, 192 dp kerek)

Kérés: felületek, sortörés, kijelzőről lelógó elemek, egyszerűbb/átláthatóbb menük – a teljes óra app.

1. Képernyő-audit: `WearScreenAuditTest` minden route-ot kirajzol (kerek 192 dp + hosszú lista nézet),
   PNG-k a CI teszt-artifactban (`wear/build/test-results/wear-screens`).
2. Hibalista + mockup → jóváhagyás → javítás egy CI-körben.

| # | Képernyő | Hiba | Javaslat | Állapot |
|---|----------|------|----------|---------|
| 1 | Kezdőlap | Íves „Mérés be/Mérésre vár” kilóg; „Beállít.” levágva; 3 kör gomb zsúfolt | Felső állapotsor (telefon · akku · ♥ pulzus); nagy „Indítás”; Napló · Menü · Beállítás kis körök, felirat a kör alatt | kódolva |
| 2 | Főmenü + „Tervezett edzések” almenü | 6–7 elem, széles gombok levágva a kör szélén; felesleges almenü | Gyors · Tervezett (naptár + Programnapok gomb) · Napló · Mérések; Beállítások/Telefon a lista végén; „Kezdőlap” = vissza; gombok a biztonságos sávon belül | kódolva |
| 3 | Edzés – erő | Előző/Rögzítés/Következő körök egymásra lógnak, feliratok kilógnak | Egy sor, nem fedik egymást, felirat a kör alatt | kódolva |
| 4 | Edzés – időalapú | Két ✓ (Mentés + Befejezés) egymáson | ‹ Előző · ▶ Indítás/Szünet · ✓ Rögzítés; Befejezés a menüben / utolsó sorozat után | kódolva |
| 5 | Programnapok | Avatarban „na” (a „nap” szóból) | Nap betűje (A, B); sorok beljebb | kódolva |
| 6 | Megerősítések (Befejezés, Törlés, Szerkesztő mentés) | Gombfelirat kilóg a körből | Felirat a kör alatt; 1–2 soros szöveg | kódolva |
| 7 | GPS-útvonal, Edzés vezérlése, Beállítások, Mérések | 4–8 soros magyarázat, lelóg | 1 sor + „ⓘ Részletek” | kódolva |
| 8 | Mentés és szinkron | „Nincs várakozó művelet” kétszer | Egyszer | kódolva |
| 9 | Összesítő | „Helyben megőrizve · mentés várakozik” levágva | Rövid állapot („Telefonra vár”), cím „Edzés kész” | kódolva |
| 10 | Időzítő eszközök | Alsó elem félig kilóg a kerek kijelzőn | Biztonságos sáv + görgetés | kódolva |
| 11 | Tervezett összevonása | Köztes „Tervezett edzések” lap felesleges | **A jóváhagyva**: Főmenü „Tervezett” → naptár; alul ‹ (főmenü) + „Programnapok”; Programnapok vissza → naptár | kódolva |

## Bekötés (óra)
| Funkció | Régi | Új |
|---|---|---|
| Naptár | Főmenü → Tervezett → Naptár | Főmenü → Tervezett |
| Programnapok | Főmenü → Tervezett → Programnapok; Kezdőlap (nincs ajánlott edzés) | Naptár alján „Programnapok”; Kezdőlap változatlan |
| Kezdőlap a főmenüből | „Kezdőlap” sor | vissza mozdulat |
| Edzés befejezése | ✓ az utolsó gyakorlatnál | ■ Befejezés (ugyanaz a megerősítés) |
| Hosszú magyarázatok | mindig kint | „ⓘ Részletek” |

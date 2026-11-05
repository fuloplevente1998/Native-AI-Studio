# Rövid munkátadás – 2026-11-05

Belépési pont: [AGENTS.md](../AGENTS.md). Részletes kiadási és készülékes
előzmény: [CURRENT_STATUS.md](CURRENT_STATUS.md). Az élő GitHub-állapot elsőbbséget
élvez az alábbi pillanatképpel szemben.

## Ellenőrzött kiindulás

- Main: `0d867e0c63e3c44e3364d210b46e1cae650d8380` (#132), a három új dizájn-PR
  még draft; az ellenőrzéskor mainen nem volt új alkalmazásváltozás.
- Legutóbb átadott APK: 2753, signed alkalmazásforrás `99f2adf`.
  [Kiadási bizonyíték](RELEASE_2753_PHOTOS_VERIFIED_ARTIFACTS.json).
- 13 aktív jegy: 5 funkció, 6 dizájn, 2 adat/felület; a lista a CURRENT_STATUS elején.

## Más chatek már elkészített PR-jei – nem újra kiosztandók

| PR / jegy | Változás | Ellenőrzött head / Actions run |
| --- | --- | --- |
| [#133](https://github.com/fuloplevente1998/Native-AI-Studio/pull/133) / #125 | Wear óraikon az átadásnál | `f6488af` / `41936088481` |
| [#134](https://github.com/fuloplevente1998/Native-AI-Studio/pull/134) / #118 | Témák közös kerete | `60fcfaa` / `41936439180` |
| [#135](https://github.com/fuloplevente1998/Native-AI-Studio/pull/135) / #131 | Tervezés állandóan látható | `f6f2be4` / `41937900312` |

Mindhárom diff átnézve, konkrét működési regresszió nem azonosítva. A letöltött
artifact SHA256 és commitigazolás egyezik; mindhárom nyers XML összesítése
460 = 284 telefon + 176 Wear, 0 failure/error/skipped. #135 tesztje a blokk
jelenlétét védi, a tervezési callbackek működését nem. Készülékes megjelenés még
ellenőrizendő; nem mergelt és nem új APK-ba csomagolt eredmények.

## Most engedélyezett végrehajtás

A felhasználó 2026-11-05-én kérte az agent-szabályok rögzítését és a munka
megkezdését; korábban külön párhuzamos agentmunkát is kért.

- `fix/128-130-session-stopwatch`: #128/#129/#130 közös vizsgálata és bizonyított
  okok javítása, session/Studio/stopper fájlok és saját regressziók. GPT-6.1 Sol xhigh.
- `fix/117-calendar-sync`: #117 Google Calendar 404, Google sync/tervezési/API
  szabály fájlok és saját regressziók. GPT-6.1 Sol high.
- `docs/native-agent-workflow`: gyökér AGENTS, rövid átadó és aktuális státusz.

A két funkcióág külön worktree-ben dolgozik. Közös fájl felmerülésekor előbb
egyeztetés kell az integrátorral. #70 Coach és a fennmaradó adat-/dizájnfeladatok
ebben az első körben még nincsenek ezekhez az agentekhez rendelve.

Következő lépés: bizonyított gyökérok, célzott regresszió és draft PR, majd a
friss PR-head Actions-eredményének ellenőrzése. Nincs automatikus main-merge,
issue-lezárás, verzióemelés vagy új APK/release ebben a munkafázisban.

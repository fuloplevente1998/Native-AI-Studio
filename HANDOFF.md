# HANDOFF – aktuális állapot

Frissítve: 2026-11-18. Rövid, felülírandó pillanatkép (≤40 sor). Előzmény: git log / PR-ek.

## Main
- `9368ff9` AI Studio: Programok kártya 3 zóna + lenyitható Részletek (közvetlen push).
  Main CI run 347 (44381881703) kézzel indítva (push nem triggereli az android-ci-t).
- Előző kiadás: `ad835c2` = 2767 · 1.2-beta, signed run #75 SUCCESS.
- `docs/CURRENT_STATUS.md` / `docs/WORK_HANDOFF.md` 2765-r2/PR268-nál megállt (elavult).

## Folyamatban: 2768 kiadás – PR #275 (`claude/trainpilot-native-handoff-g28fst`)
- Verzió 276800 / 276802, 1.2-beta. Részletek gomb érintési cél 26→48 dp + teszt.
- Felhasználó jóváhagyta: zöld PR CI után merge → signed APK+AAB (#76) → Drive.

## Nyitott PR-ek
| PR | Állapot |
| --- | --- |
| #274 | CI 346 FAIL, ütközik a mainnel (9368ff9 felülírta) → lezárás javasolt |
| #272, #271 | draft (AI Studio audit, munkafolyamat-doksi) |

## Nyitott / megjegyzés
- #269 navigáció + alsó lapok; elfogadás 0/7.
- `public/assets/aistudio/Screenshot_…_ChatGPT….jpg` a 9368ff9-ben – kell-e a repóban?
- TODO: Actions futásidő optimalizálása (felhasználói kérés).

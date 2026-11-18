# HANDOFF – aktuális állapot

Frissítve: 2026-11-18. Rövid, felülírandó pillanatkép (≤40 sor). Előzmény: git log / PR-ek.

## Main
- `ad835c2` = PR #273 merge, **2767 · 1.2-beta** (telefon 276700, Wear 276702).
- Signed run #75 (44327419072) SUCCESS ezen a SHA-n. Tesztszám (865/0) a felhasználó
  szerint; nyers XML-ből ebben a munkamenetben nincs újraellenőrizve.
- `docs/CURRENT_STATUS.md` legfelső bejegyzése 2765-r2, `docs/WORK_HANDOFF.md`
  PR268-ig tart → mindkettő elavult a 2766/2767 kiadásokhoz képest.

## Nyitott PR-ek (mind draft)
| PR | Ág | Tartalom | Állapot |
| --- | --- | --- | --- |
| #274 | `fix/program-equipment-collapse-menu-edge-2767` | Programkártya: felszerelés lenyitható, ⋮ jobb szélre; 1 fájl (+86/−43) | `[skip ci]`, **CI még nem futott**, mergeable |
| #272 | `audit/ai-studio-269-20261118` | #269 AI Studio audit | draft |
| #271 | `docs/handoff-workflow-20261118` | munkafolyamat-doksi | draft |

## Nyitott issue
- #269 telefonos navigáció + alsó lapok (Programok elsőként), friss telepítésű
  profiltervező egyesítése, A/B terv közvetlen bicepsz/tricepsz. Elfogadás: 0/7 pipa.

## Következő lépés
- #274: CI futtatása (felhasználói jóváhagyással) → zöld után review/merge döntés.
- #271 vs. CLAUDE.md/HANDOFF.md átfedés tisztázása.

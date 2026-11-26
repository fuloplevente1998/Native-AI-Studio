# Átadás — napló health/glass utójavítás

- Ág: fix/journal-health-glass-followup; alap main e53a35a6 (a #411 már merge-elve).
- Naplóban HC elsődleges; közvetlen Samsung/Wear csak bekapcsolva látható és választható. Mentett rekordok változatlanok.
- Kcal forrásprioritás javítva, részleges mérés jelölve; nincs összegzés források között. Valódi készüléken 48,3/6,4 kcal és pulzus ellenőrzendő.
- Edzésfejléc és mérési időablakok helyi időben. Belső timestamp változatlan.
- Napló health panelek és fülek statikus glass role festéket kapnak, sheet gesztus/méret változatlan.
- Célzott regressziós tesztek hozzáadva; PR CI és készülékes ellenőrzés szükséges. Nincs merge, nincs új verzió vagy signing.

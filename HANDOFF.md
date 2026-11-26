# Átadás — 2817 Wear-only napló
- Ág: fix/2817-wear-only, alap e53a35a6 (281300/281302).
- A: TrainPilot edzés részletei kizárólag érvényes mentett Wear pulzus/energia; Forrás csak Wear; külső edzés saját importmérése változatlan. HC/Samsung bizonyíték megőrizve, csak a megjelenítésből kizárva. A részletek megnyitása nem indít provider-olvasást.
- B: négy nyelvű óratelepítési tipp, Play-link/web fallback, Később edzésenként külön UI-preferenciában; nincs adatbázis- vagy health240Json-írás.
- C: napi HC elsődlegesség és Samsung kiegészítő kapcsoló változatlan, külön regresszió.
- D: #414-ből csak helyi idő és statikus üvegfesték, a HC-elsőbbség nem került át. A megszüntetett HC/Samsung ablaklista nem igényel időformázást.
- Az eredeti Wear mérés látható akkor is, ha a közvetlen kapcsolatot később kikapcsolták; nincs új mérésindítás.
- Felhasználói pontosítás: a TrainPilot-edzés Statisztikák kártyáján csak összes Wear kcal látható, az aktív kcal mentve marad, nem helyettesíti a hiányzó összes kcal-t; napi és importált adatok változatlanok.
- CI és fizikai eszközös ellenőrzés szükséges. Nincs merge, signing, verzióemelés. Helyi Git DNS-hiba.

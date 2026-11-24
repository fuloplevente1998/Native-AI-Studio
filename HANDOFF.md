# Aktuális átadó

- Kiadás: 1.4.4 · telefon 280500 · Wear 280502 · main `1994d09` (#353 + #354). Aláírt APK/AAB pár: signing run 45393952281,
  signer `cd2da9ea…`, nyers JUnit XML: telefon 863 / Wear 204, 0 hiba. TrainPilot.io verziósor frissítve (#12).
- 2804 (#353): pihenő-dokk és edzéssáv (#347), „Legutóbb” sor (#348), kontrasztpaletta 9/13/16 (#349), kompakt Google-kártya (#352).
- 2805 (#354): opt-in napi Google Drive/naptár szinkron (`GoogleAutoSyncWorker`, `NativeSyncSchedule`); Health: tartós HC háttérjob
  (15/30/60 perc), előtér 2/5/15 perc, visszatérés 2 perces fojtással, edzésmentés utáni olvasás. Élő Google-próba hátravan.
- Nyitott: #349 közös kiemelési szabályok (paletta 12/15 accent <4,5:1), #350 saját témák, #351 óra-mérés automatikus indítása
  (javaslat kész, jóváhagyásra vár), heti lépés% egy forrásból.
- Következő: kód-szétszedés a `docs/REFACTOR_PLAN.md` szerint (R0–R8), jóváhagyásra vár. Szint: R1–R5 low, R0/R7 medium, R6/R8 high.

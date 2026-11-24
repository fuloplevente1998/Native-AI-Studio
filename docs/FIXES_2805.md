# 2805 – gyűjtő (1.4.3 / 2804 után → 280500 / 280502)

| # | Téma | Hol | Megoldás | Állapot |
|---|------|-----|----------|---------|
| 1 | Google auto-szinkron | Beállítások → Google Cloud Szinkron, lenyitva | Új „Automatikus szinkron” blokk: napi Drive-mentés, naptár automatikus frissítése, „Csak Wi-Fi-n” (alapból minden ki, Wi-Fi be). WorkManager, 24 órás ablak, alacsony akkunál kihagy, aktív edzés közben később próbálja; soha nem kér engedélyt, nem fésül össze archívumot, karbantartás-szünetnél nem fut | kódolva (CI) |
| 2 | Google állapot | Kártyafej és Drive/Naptár doboz | „Késik” csak bekapcsolt automatikánál és >72 óra után; sosem futott = „Még nem futott”; a háttérhiba piros a következő sikerig | kódolva (CI) |
| 3 | Kézi részek | Ugyanott | Változatlanok, „Kézi műveletek” fejléc alatt: Drive szinkron + összefésülés, Naptár szinkron, Felhő karbantartás (lekapcsolás, visszavonás, törlések). Mockup: `scratchpad m2805/full_order.png` | kódolva (CI) |
| 4 | Health háttér | HC háttérfrissítés | A feladat túléli az újraindítást (`setPersisted(true)`, `RECEIVE_BOOT_COMPLETED`); választható 15/30/60 perc; a régi, nem tartós feladatot lecseréli | kódolva (CI) |
| 5 | Health előtér | Szinkron részletei | Választható 2/5/15 perc (azonnal életbe lép); visszatéréskor frissít, ha az utolsó automatikus olvasás ≥2 perce volt | kódolva (CI) |
| 6 | Health edzés után | Edzés mentése | Sikeres (tartós) mentés után azonnali automatikus olvasás, ha az automatikus frissítés be van kapcsolva | kódolva (CI) |

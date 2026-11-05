# Új készülékes visszajelzések a 79c2736 APK után

Ez a csomag még nincs az átadott APK-ban. Kód, CI és készülékes elfogadás külön állapot.

- #153 / PR #154, `fix/rest-timer-zero`, `76963ba`: a Studio pihenő nullán
  leáll/eltűnik; strength és bilateral közös tick; VM-műveletek aktuális session
  és owner/restore/pause/finalization ellenőrzéssel. Három regresszió hozzáadva.
- #150 / PR #155, `design/calendar-one-page`, `d35afc8`: közös fejléc és hat
  kompakt mező, legalább48dp célok, kis ablakos görgetési tartalék; három UI-regresszió.
- #149/#151, `fix/universal-health-sync`, worktree `/workspace/TrainPilot-universal-health`:
  félkész, nem commitolt natív provider override/batch és VM-bekötés. Megőrizendő.
  Hátravan a két frissítőgomb közös útja, provider-időbélyegek, app-nyitáskori
  és foreground auto/retry, egyszerűsített napló és valódi adat nélküli sorok szűrése.
  HC háttér minimum15perc/opt-in/Android ütemezés; ne ígérj garantált15percet.
- #152, `fix/wear-session-sync`: részjavítások, a teljes jegy nem kész.
  A telefonos commitEdit most a tartós projekciót az órának is közzéteszi.
  Telefon tulajdonú natív edzésen az óra set-/navigációs módosítása az engine-be
  commitolódik, stabil exercise/set ID és pause/owner ellenőrzés után.
  A Home nagy Folytatás nem az esetleg hiányzó legacy startedEpochMs alapján
  választ indítást a létező session helyett. A Wear a localSequence és watchSequence
  maximumát olvassa; az explicit natív strength-rögzítés manuális terv mellett is
  engedett a megmaradó ARM/target/owner/restore védelemmel.

Az új regressziók még nem futottak; a felhasználó közös main-CI-t kért, külön
agentenkénti kör nélkül. Csak kész, felülvizsgált javítás integrálható, nem a health WIP.
Nincs új APK vagy készülékes mérési bizonyíték.

## Nyitott Wear-vizsgálat

Az óra frissítése után megjelenő edzésről még nem bizonyított, hogy lezárt session
támad fel vagy helyes draft recovery. Ne törölj helyi adatot a hiba elfedésére.
Explicit átadás után a visszaforduló owner/pause és a két eszközről folytatás teljes
folyamata még vizsgálandó. A telefon Folytatás watch-owner esetén jelenleg csak
a telefonos nézetre navigál, nem ad ki új órás resume parancsot.
Pihenő önmagában nem pause az egészségservice-ben: az a workoutPaused állapotot
követi. A200 vs350–500kcal önmagában nem bizonyít szenzorhibát; valódi mért
ablakot és activeIntervals adatait kell összevetni, képletfelszorzás nélkül.

Samsung Health a Galaxy Watch saját napi méréseit a Samsung külön szinkronján át
tartalmazhatja. A TrainPilot Wear saját HR/kcal adatai workoutId-hoz kötöttek,
nem íródnak automatikusan Samsung Healthbe. A kért „TrainPilot Wear OS –
edzésmérések” cím és az adatok felületi rendezése még hátravan; napi összegbe
duplán összeadni tilos.

## Agent-állapot

A compact_calendar_followup és rest_timer_zero_followup agentek használati limit
miatt leálltak. Megmaradt kódjukat az integrátor fejezte be a fenti PR-okhoz.
Nincs aktívan tovább dolgozó agent. Új utasítás: kizárólag low/medium/high,
összetett hibakeresés is high; extra high indítása tiltott (AGENTS.md frissítve).

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

## #152 folytatás – watch-owner és késői kézbesítés

A `fix/wear-session-sync` / draft PR #156 további kódjavításai:

- A telefon explicit folytatáskérést címezhet az óra által birtokolt, szünetelő
  workout pontos ID/epoch/pauseRevision értékére. Az óra tényleges telefon-node
  eredetet, legfeljebb15másodperces frissességet, committed ownershipot és helyi
  closure/undo állapotot ellenőriz, majd a meglévő `WearOwnership193.resume` menti
  a folytatást és annak tartós outbox-kérését. A telefon nem változtat optimistán
  ownert vagy pause állapotot. Kapcsolat hiányában látható újrapróbálható hiba van.
  A Home és sticky gomb meglévő közös `returnToActiveWorkout` útjához a root
  integrátor köti be ezt a hívást a közös tulajdonú `TrainPilotViewModel.kt`-ban;
  e bekötés nélkül a draft ág önmagában még csak navigál watch-owner esetén.
- A telefon a watch finalSnapshot írása előtt ellenőrzi az envelope és payload
  workout/epoch egyezését, a kanonikus owner/epochot, a pauseRevisiont és a delivery
  sequence monotonitását. Késői régi snapshot `superseded` nyugtát kap; nem fordítja
  vissza a pause/owner állapotot vagy újabb editet. Snapshot nélküli implicit
  folytatás nem gyárt új pauseRevisiont a telefonon. Draft nélküli újraaktiválás
  csak explicit start parancson érkezhet, periodic mirroron nem.
- A resurrection szűrő a törlési tombstone mellett a már mentett Room history ID-t,
  syncId-t, workoutKey-t és legacy started aliast is ellenőrzi. A helyi draftokat
  nem törli, valódi helyreállítást nem állít automatikusan hibásnak.

Ezek bizonyított kódrések javításai, nem a készülékes hibák közös gyökerének
bizonyítékai. A meglévő natív editor publication/set-edit/max-sequence/manual
strength részjavítások reviewja során az ARM, target, restore és tulajdonos őrök
megmaradtak. Öt új tiszta logikai regresszió és a meglévő Room legacy-alias teszt
kibővítése készült. `git diff --check`, továbbá a módosított Kotlin fájlok
határoló-/literális-JSON ellenőrzése tiszta; fordító/JVM teszt még nem futott.
Helyi Gradle nincs és nem települt; külön CI nem indult. Következő kapu a közös
root-integráció teljes main-CI-je, majd fizikai telefon–óra páros próba.

A mérési szolgáltatás kódútja továbbra is kizárólag `workoutPaused` alapján kéri
az ExerciseClient pause/resume műveletét; restEndAt vagy automata rest fázis nem
állítja meg az egészségedzést. Auto-pause kikapcsolt. Nincs kcal-skálázás vagy
mérési képletváltoztatás. Valódi activeIntervals, Health Services activeDuration,
forrás és szenzorablak nélkül a200 vs350–500kcal eltérés oka továbbra sem eldönthető.

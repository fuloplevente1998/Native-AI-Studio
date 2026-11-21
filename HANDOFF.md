# HANDOFF – aktuális állapot

Frissítve: 2026-11-21 UTC. Rövid, felülírandó pillanatkép. Korábbi állapot: git log, PR-ek, docs/CURRENT_STATUS.md.

## Main és jelenlegi regresszió
- Main: `8f134a96b6e9627954acb8df80367b5245974ecc` (PR #307: 2788 audit, testsúly és közös fotógaléria).
- Teljes main-regresszió: Actions #44849574400, `workflow_dispatch`, **success** (JVM + release R8).
- A fenti run már lezárult az automatizálás bevezetése előtt; utólag nem vált ki buildet.

## Automatikus kiadási kapcsolat – ci/auto-signed-after-main-regressions
- `trainpilot-android-ci.yml`: releváns main push után teljes JVM + R8 kapu (PR és kézi mód megmarad).
- `trainpilot-persistent-signing.yml`: sikeres, megbízható main teljes-regresszió `workflow_run` lezárása után indul; korábbi közvetlen push-trigger megszűnik.
- Külön `release-source` kapu: main HEAD = tesztelt SHA, teljes JUnit XML-verifikáció és R8-lépés siker, célzott/hiányos körből nincs aláírt build.
- Az aláírt APK/AAB és a saját további JVM-ellenőrzés azonos, tesztelt SHA-t használ; forrás-metaadat ellenőrizhető.
- A meglévő manuális `workflow_dispatch` (`build_aab` opció) megmarad. A Play feltöltés nem automatikus.

## Ellenőrzés és hátralévő lépés
- Workflow YAML, Actions GitHub-kifejezések és minimális diff ellenőrzése szükséges.
- PR ellenőrzés, main-integráció csak elfogadott CI-kapuk után; a main push elindítja az új teljes regressziót, annak sikerét követi az aláírt build.
- Nincs helyi Gradle/emulátor, titok, automatikus verzióemelés, külső Play/Drive-közzététel.

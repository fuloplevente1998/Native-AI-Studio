# #264 — Inaktív program törlése és naptári tervek ürítése

A PR263 reszponzív vezérlőire épülő külön javítóág. Nincs új main-integráció,
installer vagy verzióemelés ebben a körben.

## Program és edzésnap

A mentett edzés és naptári rekord saját program-/napnevet tartalmaz, nem
függő gyermekrekordja a programs táblának. Korábbi napló vagy lezárt naptári
hivatkozás ezért már nem akadályozza az inaktív saját program/nap törlését.
A történeti snapshotokhoz, sorozatokhoz és naplóhoz nem nyúlunk.
Aktív program, beépített sablon és futó/átadás alatti edzés védelme megmarad.
A még élő, történettel nem lezárt tervezett alkalmak akadályozzák a törlést:
pontos, négy nyelvű üzenet kéri ezek előzetes törlését a naptárból.
Törölt/kihagyott/lezárt naptári tételek nem okoznak örök blokkolást.

## Naptár

Balra Ürítés (teljes hozzáférhető név: Naptár ürítése), jobbra Tervezés.
Kis betűvel egymás mellett, nagy betűnél reszponzívan; négy nyelven.
Megerősítés előtt tartós Room-előnézet készül. Megerősítés nélkül nincs törlés.
A tervezett és kihagyott, naplóval nem lezárt alkalmak tűnnek el; a befejezett
edzések, kapcsolt napló és önálló history snapshotok megmaradnak.

Az ürítés egyetlen tranzakció: status=skipped/cancelled=true cancellation
rekord, monoton updatedAt, native:calendarState projekció frissítése és régi
Undo/planner receipt érvénytelenítése. Nem fizikai DELETE ALL: megmarad a
managed Google Calendar törlési jelzés és a régi Drive-tervek elleni timestamp.
A párbeszédablak jelzi, hogy nincs Undo; a korábbi Undo nem támaszthatja fel
a kitörölt terveket. A művelet nem nyúl más Google-naptárakhoz.

Adatcsere-generáció, pontos Room/doc/planner CAS és aktív workout/ownership
ellenőrzés a végrehajtásnál is. Sikertelen íráskor teljes rollback;
siker-visszajelzés és UI-eltávolítás csak a tartós commit után.
A korábbi folyamatban lévő naptármentés bevárása és revision/mutex megakadályozza,
hogy egy régi optimista UI-kép visszaírja a törölt terveket.

## Ellenőrzés

Hat új natív/VM regresszió és egy valódi CalendarPlannerScreen UI-regresszió:
history/lezárt naptár megőrzése, program/nap törlés, aktív/builtin/élő terv
védelme, nyilvános ürítés→törlés út, tartós cancellation, managed Google-delete,
régi felhőterv merge, változott preview/restore/futó edzés elutasítása,
DB-hiba utáni rollback, bal/jobb gomb és megerősítés/Mégse négy nyelven.
Közös teljes CI44150814973 SUCCESS, forrás
8d18b7680640ecf101c5923f9563d1dcb49bc776. Telefon123 XML/637 teszt és
Wear36 XML/198, összes835,0failure/error/skipped; shared NO-SOURCE.
A PR263 reszponzív javításaival együtt fordult a produkciós telefon/Wear és
futott a teljes regresszió, release R8-val. Letöltött artifact SHA-256 egyezik,
a nyers XML önálló újraszámolása egyezik a CI-verifier eredményével.
[Bizonyíték](verification/PR265-ci-evidence.json). A lezáró commit csak dokumentáció.
Fizikai készülékes elfogadás és Play-feltöltés külön marad.

Későbbi felhasználói jóváhagyás: PR263 és PR265 mainen,835 zöld tesztes
signed kapu és ellenőrzött2765-r2 telefonos APK átadva.
[Kiadási átadás](releases/2765-r2-changes.md). Fizikai elfogadás külön marad.

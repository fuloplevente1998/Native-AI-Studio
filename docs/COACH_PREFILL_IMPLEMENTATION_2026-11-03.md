# Studio Coach és előző alkalom bekötése – #68/#70, #69 adatfeltételek

Az előzetes telefonos APK (2744/deebfdd, #67) átadása megtörtént a fejlesztés előtt; ennek bizonyítékát a CURRENT_STATUS és MAIN_TEST_BUILD_2026-11-03_2744.json rögzíti. Ez a csomag még nem az itt leírt új funkcióbuild.

## Mi változik

- A tervezett Studio indítás a NativeWorkoutSession.startProgramWorkout, a többgyakorlatos gyors indítás ugyanazon canonical session builder/buildExercise/installPrepared útvonalát használja. A natív commit előtt nincs létrejöttnek mutatott UI-edzés. A valódi program/nap/schedule és setId megmarad.
- A meglévő #228 előtöltő motor működik a Studio indításból is: azonos exercise ID/loadType/repUnit; az előző megfelelő kész sorozat súlya/ismétlése pozíciónként. Másik program/nap/gyors edzés is forrás lehet. Draft, teljesítettség, actualRir, idő, GPS/ownership nem másolódik.
- A Studio köztes sorozatmodell megőrzi a natív setId-t és külön jelöli, ha a felhasználó valóban megadott RIR-t. A mért 0 és a hiány eltér; az üres RIR-mező nem gyárt 2-es tényleges értéket. A hat edzésfeedback gépi easy/light/right/challenging/hard/pain mezőt ment; automatikus „pont jó” nincs.
- A programmapper/persist megőrzi a tartományos prescriptiont, a tényleges weight/rest kulcsot és a meglévő raw programmetaadatot; a régi Studio weightKg/restSec is olvasható. A programmentések sorba rendezettek, az indítás megvárja őket, restore közben régi snapshot nem írható vissza.
- A Coach oldalon a meglévő CoachViewModel, NativeCoachTargets, NativeCoachAdviceRepository és kanonikus progresszió számol. A megjelenített programnap indítható, a jövőbeli terv a naptárt nyitja; nincs automatikus A nap.
- Home/recovery és Coach ugyanazt a kiválasztott providerhez tartozó, grant/hiba/nap/frissesség szerint felvett bemenetet és NativeCoachReadiness motort használja. A validált Samsung Energy 0 tényleges adat a Coach-nál is. A fájdalomfeedback akkor is korlátoz, ha a gyakorlatoknak nincs külön effort mezőjük.
- Az aktív képernyő helyi, saját súly/ismétlés/RIR heurisztikája megszűnik. A meglévő natív snapshot/owner/előzmény/settings ellenőrzött súlyjavaslat előnézet és külön jóváhagyás után alkalmazható. RIR-t nem állít be. A panel megszűnésekor a natív VM ticker/megfigyelői leállnak.
- A globális stílus/progresszió mentése a NativeTrainingModeChange prepare/approve és DataStore CAS útján tartós, programoverride-ok megőrzésével. Aktív session/adatcsere védelmei érvényesek; nincs mód automatikus engedélyezése az oldal megnyitásától.

## Ellenőrzés és határ

Új regressziók: valós B nap indítása, másik program előzménye, eltérő sorozatértékek, eredeti napló érintetlensége, stable ID/manual edit/load metadata, gyors edzés, nincs örökölt actualRir/done, mért 0/hiány, kiválasztott provider és Samsung 0, kanonikus pain, valódi start callback és jövőbeli cél naptárba navigálása. GitHub Actions teljes Android/JVM kapu és nyers XML eredmény szükséges; a tényleges eredményt a futás után kell feljegyezni.

Változatlan package/signer/verzió, egyetlen natív motor/DB, ownership/ACK/epoch/finalization/replacement/tombstone/privacy. Ez nem BLE-fejlesztés, nem új program-/gyakorlat-CRUD, nem a teljes health browser vagy i18n projekt. A #61 másik Samsung permission-diagnózis hibája külön nyitott marad, ha nincs e csomagban javítva. Fizikai készülékes elfogadást a CI nem bizonyít.

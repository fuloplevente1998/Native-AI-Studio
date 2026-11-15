A 2762-es készülékes visszajelzésben a kijelölt gyakorlat pipája ismételt hozzáadást hívott. A #238 UI/mentési javításait a felhasználó elfogadta és készre záratta; a visszavonás külön #241.

A pipa most a meglévő removeExerciseFromWorkoutQueue műveletet kapja, a plusz megőrzi a hozzáadást. A kiválasztott elemek száma, névsora és a fejléc megléte a tényleges ViewModel-listából frissül. A visszavonási felirat HU/EN/DE/RO nyelvű, a pipa finom piros jelzést kap. Az elrendezés és érintési méretek megmaradnak.

Az eltávolítás csak az összeállított listát módosítja: a mentett program és a futó edzés sorozatai nem törlődnek. Már kivett/hiányzó ID eltávolítása no-op. A visszavonási banner lokalizált.

Új UI→valódi ViewModel regresszió ellenőrzi mind a négy nyelven a hozzáadás/középső elem kivétele/újra hozzáadás/teljes ürítés ciklust, a darabszámot és sorrendet, valamint a többi adat megőrzését. A meglévő kompakt méret- és callbackteszt is megmarad. Teljes CI a meglévő workflow-ban; új APK/AAB és verzióemelés ehhez az utófeladathoz nincs kérve. A kiadott 2762-es fájlok változatlanok.

Ellenőrizve: CI43629146558 SUCCESS, forrás39bf7c0ac1264be6a6b9acd2c73faa4335ea2d7b. Nyers696 teszt,0failure/error/skipped; letöltött ZIP-digest egyezik. Draft PR242, főágba még nem került, új telepítő nincs. Következő lépés az új funkció elfogadása és a később kért közös integráció/kiadás.

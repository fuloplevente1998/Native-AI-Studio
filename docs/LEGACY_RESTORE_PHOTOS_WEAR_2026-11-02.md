# Korábbi RepForge mentés: fotók és órás mérések

## Bizonyított kódhibák

- A régi webes TrainPilot `www/app.js` az órás Health Services összesítést `healthWear129` néven tárolta (schema 1, source wear_health_services, workoutId/watchId/revision). A natív migrátor csak wear/healthSummary mezőt vetített a wearMetricsJson oszlopba. A nyers mező ettől még megmaradhatott a Room rawJson-ban.
- A Studio privátfotó-oldal mintaképeket mutatott, a kamera/picker callback pedig valós mentés nélkül írt sikerüzenetet. A meglévő natív fotógaléria és a Room-index nem volt erre az útvonalra kötve.
- A felhasználó megerősítette: azonos edzésnél a két app HC-értékei egyeznek. A hiányzó 404 kcal a korábbi app „TrainPilot óra” Health Services adata, nem Samsung/HC aktív kalória. Nem kell a HC számát felülírni vagy hozzáadni az órás számhoz.

## Javítás

- NativeSavedWearHealth: régi mért payload azonosítás forrás/schema/workout identitás alapján; új importnál a meglévő Room mezőbe vetítés, már importált rekordnál nyers JSON-ból olvasási fallback. Nincs kitalált mérés és nincs séma-/workout-id átírás.
- Napló, forrásjelző és órás mininapló ugyanazt a kompatibilis olvasást használja. HC/Samsung lekérés a régi Wear-mérést nem helyettesíti.
- A Studio fotóoldal a meglévő ProgressViewModel/NativeProgressPhotoData/NativeProgressPhotoGallery rendszert használja. Valódi privát JPEG dekódolás, teljesképernyős nézet, a naplóból nyitva pontos workoutId szerinti szűrés; beállításokból a teljes galéria.
- A mintaképek és a mentés nélküli sikerüzenetek eltávolítva. Ez a képernyő valódi, csak olvasási galéria; új fotó létrehozását/csatolását nem állítja késznek.
- A meglévő ZIP hash-, méret-, staging-, redo-, törlés- és adatcserevédelmek változatlanok. Nincs új adatbázis vagy párhuzamos fotótár.

## Tesztelés

Új regresszió: régi formátumú ZIP valódi JPEG-gel → restore → byte-egyezés → Room-index → Studio képdekódolás → teljesképernyős megjelenítés. Külön: legacy Wear 404 kcal és 101,6 bpm megtartása, HC 72,2 teljes / 8 aktív kcal külön, korábban importált raw-only rekord fallbackje, időbélyeges legacy identitás, idegen workout/source elutasítása.

A képernyőképen látott értékek csak a teszt fixture-ben szerepelnek. A felhasználó tényleges ZIP-fájlja még nincs megvizsgálva. Ha a régi exportból az órás Health-adatot eleve kihagyták (healthIncluded/hozzájárulás), a hiányzó bájtok nem gyárthatók le a képernyőképből. A régi ZIP-et őrizd meg; cél a már meglévő raw adatok megjelenítése újraimport nélkül.

CI és a tényleges készülékes ellenőrzés állapotát a javító PR leírása tartalmazza. A közvetlen Samsung SDK-val végzett olvasás másik útvonal; azt ez a kompatibilitási javítás nem állítja készüléken igazoltnak.


## Hozzáadott adatvédelmi ellenőrzés

A natív egészségnapló a Samsung csatornát is tárolja; hozzájárulással a JSON/ZIP ezt a naplót és a HC/Samsung/Wear workout-összesítéseket is menti. A Drive szándékosan workout-összesítéseket szinkronizál, a teljes napi provider-naplót nem. Ezt a felirat most külön kimondja.

A Studio kapcsoló korábban csak memóriában változott és true volt az alapállapota; most a meglévő DataStore hozzájáruláshoz kötött, alapból false. A hozzájárulás nélküli export az összes egészségadat-alias mezőt kizárja, nem csak a teljes naplót. A kikapcsolás nem törli a helyi méréseket. Drive-nál a feltöltött projekció egészségadatmentes; a helyi install külön, azonos workout/ablak ellenőrzéssel megtartja a saját cache-t, és egy távoli snapshot nem kapcsolhatja be ezen az eszközön az egészségadat-megosztást.

Új tesztek: consent false/true × JSON/ZIP, Samsung napi csatorna és workout-provider, HC/Wear adatok kizárása/megőrzése, fotók változatlansága, egészségadatmentes Drive-projekció és kizárólag lokális cache-visszaillesztés, eltérő időablak elutasítása. Az első CI 244/245 siker; az új fotógaléria tesztje controller-adatbetöltésre várva timeoutolt. A következő futás közvetlen indexellenőrzéssel és részletes controllerállapottal különíti el az adatbázis- és megjelenítési hibát; az időkorlát nem változott.

# Javítási státusz –2026-11-11, közös audit +Wear témaszinkron

Kiinduló main:a6a55a07e48094f93dd0ec66b1deeb109e063b4e.
Ág:fix/nov11-audit-and-wear-theme, root egyedül, új agent nélkül.
Az alábbi négy eredeti finding javítása implementálva; a közös regressziós CI még
következik. Main-merge/kiadás/Play-feltöltés nem része ennek a forrásátadásnak.

- A1: a watch-owner pihenő olvasható/tükrözött marad; telefonos vezérlőgombok
  letiltva, nincs tiltott phone-write/hamis mentési hiba. Ownership guard megmarad.
- A2: minden automatikus tick monoton határidőből számol; explicit +/- és Szünet
  előbb elszámolja az eltelt időt. Lejárt abszolút automation deadline mellé érkező
  régi remainingMs nem indíthat új pihenőt. Új ismételt1400ms drift, háttérből pause
  és lejárt automation-deadline teszt.
- A3: Google-naptár cache eseményenkénti eventCheckedAt időt használ; más esemény
  frissítése nem hosszabbítja meg a régiét. Korábbi aggregate-only cache egyszer
  újraellenőrződik. Új fake-HTTP regresszió lejárt esemény + friss aggregate esetre.
- A4: a célok mentése dataset-ticket és NativeDataReplacement lease alatt ír;
  siker csak Room commit után, hibánál régi érték és látható hiba. Új tényleges
  Room-visszaolvasás, SQLite-triggerrel kiváltott íráshiba és restore-fence teszt.

## Wear OS kiegészítés –PR220 alapján

Forrás:docs/WEAR_THEME_SYNC_AND_STORE_MEDIA_2026-11-11.md, nyitott PR220.
A megosztott ChatGPT-linket kétszer nem sikerült letölteni (TimeoutError), ezért
nem állítjuk a teljes beszélgetés megismerését. A PR konkrét követelményei alapján:

- W1: telefon ->Wear témaszinkron javítása. A Studio színválasztó Color.value
  bitmaszkolása helyett toArgb-t használ. Ez a packed Compose-színek miatt
  korábban hibás/fekete accentet adhatott. A canonical watch-home themeJson a
  tényleges32 telefonpalettából képez teljes palettát: háttér, kártya, belső panel,
  szöveg, másodlagos szöveg, keret, accent és onAccent. Régi/hibás accentérték
  érvényes palette key mellett sem rontja el a továbbított színeket.
- W2: témaváltás tartós settings-write után azonnal új watch-home állapotot publikál;
  szállítási hiba nem állítja vissza a tárolt témát. Újracsatlakozáskor a telefon
  aktív edzés nélkül is publikál. Wear a home snapshotot commit-tal tárolja;
  hiányzó/hibás téma nem írhatja felül az utolsó érvényeset, régebbi packet nem nyer.
- W3: Wear UI fix háttér/kártya/szövegszínei composition-local palettából jönnek,
  a főoldalon és az érintészár képernyőjén is. A Wear MaterialTheme a kapott színeket használja. Matt/kontraszt/
  glow/világos kategóriák megőrződnek. A fix piros/zöld negatív/mentési műveletszínek
  szándékosan maradnak, a kör alakú elrendezés és érintési méretek változatlanok.
- Új tesztek: mind a32 telefonpaletta szerializációja sérült régi accent mellett;
  publikálás csak commit után; offline transzport után megmaradó beállítás;
  Wear családok/full palette round-trip; restart/hiányos/hibás/régi homepacket.
- Valódi Data Layer, process-kill és kontraszt vizuális elfogadása fizikai telefon+
  Wear eszközön még szükséges. A tesztek nem jelentenek ilyen hardveres bizonyítékot.

## Play médiák és zárt teszt

Csak valódi aktuális Wear-build screenshot használható app screenshotként.
A PR által említett432x432 átlátszó PNG nincs csatolva ebben a munkakörnyezetben;
nem készült helyette generált vagy átrajzolt UI. A javítás elfogadása után valós
új képernyőkép kell; ha szükséges, csak az átlátszóságot kell eltüntetni az eredeti
felület változtatása nélkül.
A Reddit-meghívó kész, mindhárom felhasználótól származó hivatkozással:
docs/REDDIT_CLOSED_TEST_INVITE_2026-11-11.md. Nem publikáltuk.
A privacy URL helyes a kiinduló mainben; a legutóbbi Play AAB még a korábbi linket
hordozta. A következő jóváhagyott Play-csomaghoz új, fel nem használt verziókód kell.

---

# TrainPilot main átvizsgálás – 2026-11-11

Vizsgált forrás: a6a55a0 (teljes SHA: a worktree HEAD), Native-AI-Studio.
Módszer: statikus kód- és híváslánc-ellenőrzés, korábbi 3dde06f kiadáshoz képesti diff, GitHub Actions/PR/issue állapot. Android/Gradle/új CI/build nem indult, készülékes működés nem volt vizsgálható ebben az auditban. A felsorolás célzott felülvizsgálat eredménye, nem az egész app hibamentességi igazolása. Appkód és GitHub-jegyek nem módosultak.

## 1. [P2] Órás tulajdonos mellett a telefonos pihenőgomb helyi állapotot módosít, majd tiltott telefonos mentést kér

Forrás: app/src/main/java/com/repforge/app/StudioRestTimer.kt:23–46; app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:3552–3568,1152–1164; app/src/main/java/com/repforge/app/NativeWorkoutEditing.kt:401.

A mirrorAllowed engedi az órához tartozó sessionnél az adjust/toggle/skip helyi állapotmódosítását. Ezek után a VM persistCanonicalActiveSessionAsync-et hív, ami StudioWorkoutBridge.persist -> NativeWorkoutEditing.commitEdit útvonalon próbál telefonos szerkesztést menteni. A native guard helyesen tiltja a watch-owner/suspended frame írását. Watch parancs küldése ezen a három callbacken nincs.

Kiváltás: átadott, órán futó edzés pihenője megjelenik a telefonon; telefonon Kihagy/+15/Szünet. Várható következmény: helyi eltérés és mentési hiba, az óra nem követi a módosítást; következő snapshot visszaállíthatja a kijelzést.

Javítás: mirror módban csak olvasható kontrollok, vagy meglévő fenced, ACK-os órás parancsútvonal. A native tulajdonosi guardot meg kell tartani.

## 2. [P2] A pihenőszámláló lassulhat kisebb, ismétlődő késéseknél

Forrás: app/src/main/java/com/repforge/app/StudioRestTimer.kt:122–138; ActiveMeasurementScreens.kt pihenő-tick LaunchedEffect.

1500ms alatti callback-köznél a kód pontosan egy másodpercet von le, majd az új remaining alapján újraindítja a határidőt. Emiatt az egymást követő 1100–1400ms késések összeadódnak, a tényleges határidő kitolódik. A legalább1500ms késést kezelő reconcile nem fedi le ezt.

Forráslogikát követő számszerű modell:90 másodperc,1400ms callbackek ->90 tick után126 valódi másodperc. Ez külön Python-számítás, nem futtatott Android-teszt.

Javítás: minden automatikus tick a rögzített monoton határidőből számoljon; határidőt csak explicit felhasználói módosítás állítson át. Teszt: ismételt1100/1400ms tickek, valamint háttérből visszatérés utáni azonnali Szünet.

## 3. [P2] A naptárszinkron cache megakadályozhatja a régi események ismételt ellenőrzését

Forrás: app/src/main/java/com/repforge/app/GoogleSyncRepository.kt:573–584,615–620.

A cache összes eseménye egy közös checkedAt időt használ. Bármely egyetlen sikeres módosítás az egész tároló frissességét meghosszabbítja, a régi fingerprint-ekkel együtt. Ha24 órán belül mindig van új/módosított esemény, az egyéb események Google-oldali törlése/szerkesztése korlátlanul észrevétlen maradhat, miközben a szinkron sikeresnek látszik.

Kiváltás: E1 szinkron; E1 külső törlése; rendszeres <24óránkénti E2/E3 módosítás és szinkron. E1 fingerprint egyezése miatt nincs hálózati művelet rá.

Javítás: eseményenkénti megerősítési idő vagy külön, részleges íráskor nem hosszabbítható teljes reconciliation idő. Teszt: külső törlés + többnapos részleges frissítések.

## 4. [P2] A célok mentése tartós írás előtt jelez sikert, és nem védi a datasetváltást

Forrás: app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:2064–2108.

A UI és a „Célok elmentve” üzenet az async Room írás előtt frissül. Sikertelen írás csak Log.e-t ad, hiányzó db/scope mellett is marad a sikerjelzés. Nincs NativeDataReplacement continuation/mutate védelem, így egy későn futó régi mentés az időközben visszaállított célokat is felülírhatja.

Javítás: az aktuális datasethez kötött, fenced tartós írás után jelezzen sikert; hiba esetén látható hiba és újrapróbálási lehetőség. Teszt: írási hiba, illetve mentés/restore ütközés.

## Kiadási eltérés – fontos a Play zárt teszthez

Legutóbbi sikeres GitHub Play AAB futás:43063410245, forrás fad1b01569452b4c48350de258f8e632459fa091.
Main:a6a55a0. A különbség egy app/build.gradle.kts változás: PRIVACY_POLICY_URL a régi GitHub issues linkről a publikus privacy oldalra változott. Ez nincs benne a fenti AAB-ban. A Console-ba ténylegesen feltöltött bináris eredetét a Console-on itt nem ellenőriztem; ha ezt a futást használtad, még a régi link van benne.

Következő Play-csomagban legyen benne a javított privacy URL, új, még fel nem használt verziókóddal. A zöld AAB buildstátusz nem igazolja a Play által visszaadott végleges terjesztési aláíró és a közvetlen APK-s telepítések közti frissíthetőséget: ezt a tényleges Play-tesztelői telepítéssel kell ellenőrizni.

## Ajánlott sorrend

1. Wear-owner pihenővezérlés és monoton pihenőidő.
2. Célmentés tartóssága és adat-visszaállítás elleni védelem.
3. Naptárcache lejárata.
4. Egy közös regressziós kör, majd Play-csomag privacy URL-lel, készülékes telefon/Wear ellenőrzéssel.

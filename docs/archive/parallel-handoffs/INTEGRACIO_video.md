# INTEGRACIO_video – #59 oktatóvideó alkalmazáson belül

## Státusz / izoláció

- Saját ág: fix/inapp-exercise-video-59 (friss main alap).
- Cél: külön PR a mainre; merge, aláírt APK és eszközös elfogadás kizárólag koordinátor.
- #58 Coach-preview / naplótörlés PR a fejlesztés indulásakor még nyitott volt. Közös fájlhoz nem nyúltam, #58 kódját nem másoltam át.
- Csak a #01 számára kiosztott ExerciseVideoDialog, ExerciseVideoResolver, resolver teszt, új inappvideo/ és followup/inappvideo/ mappák, valamint ez az átadó változott.
- Nincs új dependency vagy Manifest-módosítás: meglévő Android WebView, Compose és lifecycle-runtime-compose; az INTERNET jogosultság már szerepel.

## Integrációs szerződés

A com.example.MainActivity.kt jelenlegi hívása változatlanul működik:

ExerciseYouTubeVideoModalDialog(exerciseTitle, onDismiss, exerciseId, storedVideoUrl, isCustom, selectedNotesHu, allowCatalogByTitle)

Import: com.example.ui.components.ExerciseYouTubeVideoModalDialog

- exerciseTitle = uiState.videoModalExerciseTitle.
- exerciseId = selectedExercise?.id ?: requestedId: pontos kiválasztott gyakorlat.
- storedVideoUrl = selectedExercise?.videoUrl: kiválasztott/egyéni/importált gyakorlat saját URL-je.
- isCustom = selectedExercise?.isCustom == true: egyéni rekord nem örököl katalógusvideót.
- selectedNotesHu = selectedExercise?.stepsHu?.joinToString(" ")?.takeIf { it.isNotBlank() }.
- allowCatalogByTitle = nameMatches.size <= 1: több azonos nevű találatból nincs tippelés.
- onDismiss = { viewModel.closeExerciseVideoModal() }: csak a videómodal záródik.
- Minden opcionális argumentum korábbi alapértéke változatlan; nem szükséges új VM-mező, MainActivity/ActiveMeasurementScreens módosítás.

## Viselkedés

- Ellenőrzött YouTube ID / HTTPS watch, short, live, embed, youtu.be → pontos videó youtube-nocookie.com/embed/<id> iframe-je Android WebView-ben, YouTube IFrame API eseményekkel.
- A 2/5/100/101/150/153 lejátszóhibák, hálózati, HTTP, SSL és 18 másodperces betöltési időtúllépés olvasható hibával jelennek meg. Külső YouTube alkalmazás automatikusan nem indul.
- Belső böngésző gomb: ugyanaz a pontos, normalizált YouTube URL alkalmazáson belüli másik WebView-ban; 101/150/153 blokknál is elérhető. Egyéb jóváhagyott HTTPS videóoldal eleve ebben nyílik.
- Böngészős vissza, beágyazás újrapróbálása, újratöltés, másolás, piros X és alsó bezárás; böngészőben az aktuális domain látható. Hátlapi Vissza a böngészőelőzményen halad, majd YouTube-nál a beágyazott nézetre tér vissza; X mindig bezár.
- A WebView kizárólag HTTPS nyilvános DNS hostokat navigálhat; intent:, http:, file:, content:, javascript:, IP és helyi host tiltott. JS az online lejátszáshoz engedélyezett; natív JS-interface nincs. Mixed content, fájl-/content-access, popup és kamera-/mikrofon-/földrajzijogosultság tiltott; SSL hibát nem kerülünk meg.
- A YouTube/fullscreen Android dialóguson belül marad. Bezáráskor a WebView leáll; Activity pause esetén a HTML5/YouTube videó szünetel. Edzésmotor, foreground timer, ownership, Health, tároló és mentési szerződés nincs érintve.

## Regresszió és bizonyíték

- ExerciseVideoResolverTest: kiválasztott URL elsőbbsége, egyéni/importált és hiányzó videó, Goblet pontos ID, sérült/színlelt URL, helyi/portos unsafe cél.
- InAppVideoPolicyTest: videó ID HTML-szűrése, kizárólag HTTPS böngésző-navigáció, 101/150/153/100/5 hibaüzenet, beágyazás szünet API.
- Actions SHA / futás / nyers XML: ellenőrizendő; futó CI-t nem tekintünk sikeresnek.
- Fizikai próba kell: Goblet guggolás ID, másik egyéni/importált videó, tiltott embed (101/150/153), törölt videó (100), offline/HTTPS-hiba, elfordítás, képernyőzár/háttérből visszatérés, kétszeri modalnyitás, fullscreen, hardveres Vissza/X, videóhang leállása záráskor, aktív edzés stopperének és ownershipének megőrzése. Tényleges YouTube-lejátszás JVM-teszttel nem bizonyítható.
- Az iframe-tiltás a YouTube szerverén jelentkezhet. A belső böngésző nem garantálja a régióhoz/jogosultsághoz kötött videók lejátszhatóságát.

## Módosított fájlok

1. app/src/main/java/com/example/ui/components/ExerciseVideoDialog.kt
2. app/src/main/java/com/example/ui/components/ExerciseVideoResolver.kt
3. app/src/main/java/com/example/ui/components/inappvideo/InAppVideoPolicy.kt
4. app/src/main/java/com/example/ui/components/inappvideo/SecureExerciseVideoWebView.kt
5. app/src/test/java/com/example/ui/components/ExerciseVideoResolverTest.kt
6. app/src/test/java/com/example/followup/inappvideo/InAppVideoPolicyTest.kt
7. docs/parallel-handoffs/INTEGRACIO_video.md

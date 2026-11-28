# Liquid Glass teljesítményaudit · PR #450

Baseline `bd0ea5122050bbf8b055328672c8ac16abcdabac` és [CI #917](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/46178598235). Candidate a [#450](https://github.com/fuloplevente1998/Native-AI-Studio/pull/450) végső integrált feje. Az egzakt commitot és a mérhető JVM eredményt a PR átadója rögzíti.

| Forrásút | Megfigyelt költség vagy korlát | Candidate |
| --- | --- | --- |
| `GlassScene`, `GlassPowerSave`, `GlassMotionState` | A root scene állapota 83 ms-os ciklust használ, aktív lifecycle és engedélyezett mozgás mellett. A mérés, háttérbe kerülés, ablakfagyasztás és takarékosság korlátozza az aktivitást. | A ciklus és tulajdonos érintetlen. A csepp fényének nincs órája vagy coroutine-ja. |
| `GlassScenePaintCache`, `GlassAwareScaffold`, `GlassContentBackdrop`, `KyantGlass` | A festék méret/paletta szerinti újrafelhasználása és a komponált hátterek saját scene-jéhez/meglévő layeréhez kötött shader. A nested ablak fagyasztott vagy statikus helyi felület. | Nincs új backdrop, capture vagy shaderfordítási út. |
| `GlassScrollChrome`, `MeasuredScrollChrome`, `LiquidDropMaterial` | Meglévő mért fejléc, anyagszerep és optikai fallback. Független fallback út az eszközcapability és az edzés módja szerint. | A görgetés/fejléc layout és a statikus szerepek változatlanok. |
| `GlassSelectionPill` | A pill helyzetét eddig is rugó és drag adta. A fény erre a két értékre reagál. | Az ovális clipen belül egy mérethez cache-elt ecset. Csak mozgáskor rajzol egy korlátozott alphájú foltot a meglévő draw során. Reduced-motion, measurement, power save és háttér esetén nincs ez a dekoráció. |
| `StudioCoachRecommendations` | A közös indokhoz a törzs minden kompozícióban teljes köztes listát és distinct halmazt készített. A megnyitott sor változásakor a gyakorlatadat jellemzően azonos. | Lista és nyelvi erőforrás szerint megőrzött indok, rövidre zárható `all` vizsgálattal. A független viewport és kattintások megmaradnak. |
| `NativeProgressPhotoGallery`, `PrivateProgressPhoto` | Stabil galériakulcsok és lusta lista. A preview privát fájlból, `id`/méret függően, méretezve, IO diszpécseren készül. Viewport elhagyásakor újranyitás új dekódolást igényelhet. | A privát bitmap életciklus és IO út érintetlen. A cache bővítéséhez ismételt nyitás és PSS mérés adhat alapot. |
| Naplószerkesztő és pickerek, sync és backup, aktív edzés, órakapcsolat | Az ablakok saját életciklusa és a műveletek meglévő állapotai. | A visszajelzés ugyanazokat az eredményeket olvassa; nem indul új adatgyűjtés, háttérszinkron vagy kapcsolatfigyelés. |

## Készülékes mérés

Azonos fizikai telefon, Android-verzió, képfrissítés, fényerő, energiamód, adatkészlet és téma. Melegítés után legalább három azonos baseline/candidate ismétlés. A meglévő Perfetto FrameTimeline vagy `dumpsys gfxinfo … framestats` adjon mediánt, p95/p99-et és jank arányt a tényleges frame budgethez képest (60 Hz → 16,7 ms, 120 Hz → 8,3 ms). Rögzíts PSS-t, ismételt ablaknyitás után megtartott erőforrásokat, elérhető CPU/GPU adatot és azonos terhelésen energiaadatot. A battery stat önmagában nem fogyasztásmérő.

| Forgatókönyv | Fizikai státusz |
| --- | --- |
| 30 s gyors felső/alsó navigáció és belső választók; 60 s Coach/napló/programlista | NEM MÉRVE – készülékes ellenőrzés szükséges |
| 10 egymás utáni sheet nyitás/zárás, nested naplópicker, privát galéria, export/szinkron visszajelzés | NEM MÉRVE – készülékes ellenőrzés szükséges |
| 2 perc nyugalom, háttérből visszatérés, aktív edzés és Wear kapcsolat | NEM MÉRVE – készülékes ellenőrzés szükséges |
| 8 Liquid paletta 0/50/100 clarity; normál kontroll, energiatakarékosság, reduced motion; nagy font, safe area, billentyűzet, HU/EN/DE/RO | NEM MÉRVE – készülékes ellenőrzés szükséges |

Vizsgálati küszöb az ismételt futásokra: p95 vagy PSS több mint 10% romlás, illetve jank több mint 2 százalékpont romlás. A JVM eredmény a működési szerződéseket fedi le; fizikai FPS, PSS, energia és optikai elfogadás készülék nélkül nyitott.

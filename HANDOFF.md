# Aktuális átadó

- Main: `f779f5dc` (2812 1–4. feladat, #397). Verzió 1.5.2 · telefon 280800 / Wear 280802 (az óra változatlan, nem kell frissíteni).
  R0–R8 refaktor kész; előbb `docs/CODEMAP.md`, utána csak a megnevezett fájl. Méretőr `SourceSizeGuardTest`; `scripts/split_kotlin.py`, `scripts/verify_move.py`.
- **Liquid Glass = Kyant** (`io.github.kyant0:backdrop` 1.0.6, Apache-2.0, NOTICE `app/src/main/assets/third-party-licenses/kyant-backdrop-NOTICE.txt`):
  Kotlin 2.3.10 globális, telefon Compose BOM 2026.02.00, Wear BOM marad, AGP 9.1.1. Anyagszerepek `KyantMaterialSpec.kt`, ablak-rétegek `KyantWindowOverlay.kt`
  (lap/dialógus: egy lencse, belül `LocalGlassOverlay` festék, nincs további homály). #372/#378 soha nem merge; `docs/handoff-2811` (#387) külön.
- **Kész és kiadott (aláírt build, ellenőrizve, CI zöld 255 XML / 1278 teszt):** #359/#367 telefon+Wear finomítások (#389, #390), Kyant (#388), 2812 1–4:
  Coach+Napló áttetsző fejléc/nav (`GlassUnderlapTabs` + `LocalGlassTopInset`), üveg lapok/dialógusok rétegei, Coach kártya/elválasztó, A/B napválasztó,
  hosszú listák rajzolási terhelése (`Tile` szerep, stabil kulcs, `remember`).

## 2812 gyűjtő (draft #391, `fix/collector-2812`, `docs/FIXES_2812.md`) – nyitott megfigyelések (eszköz, 1.5.2 `f779f5dc`)
- 12/14 Felnyíló lapok akadnak (Adatvédelem és engedélyek, Kapcsolt eszközök, Google Cloud lap): ablak-üvegréteg + hosszú tartalom a feltételezett ok.
- **20 Lap lehúzáskor magától fel-le ugrál (legfontosabb):** horgonyok/magasság/fogantyú/nestedScroll feltételezések, ok kódból még nincs igazolva.
- 13 Témák: glass téma neve fekete; 15 Napló kereső keret nélkül; 16 Naptár alacsonyabb (alsó gombsor lelóg); 17 oldalváltáskor az oldal tetejére;
  18 Programok alján dupla „+"; 19 adatvédelmi tájékoztató: Kyant/Apache licenc 4 nyelven (nincs benne).
- Később: nyomásra lencsehatás (`GlassTouchBubble`), látható finom keretek, óra glass olvashatóság (2810/1), valós terhelés-javaslat (Coach), Play listing, #349/#350, Haze/Glasmio összevetés.

## Munkamód (ChatGPT agent, repóba ír)
- Feladatleírások: `docs/tasks/2812/` (README közös szabályok; TASK_1–4 mainen kész; **TASK_5_all.md = egy közös PR, A–H + verzió 1.5.3 / 280900 / 280902**), mockupok `docs/mockups/2812/`.
- Ág `fix/2812-5-all` main-ből, draft PR, CI zöldig; ezután a Claude Code session átnézi, mainre emeli (squash) és aláírt APK+AAB-t buildel
  (`trainpilot-persistent-signing.yml`, `build_aab=true`; 4 fájl: verzió, signer `cd2da9ea…`, hash, nyers XML). A ChatGPT NEM merge-el.
- Merge/build/verzióemelés csak a felhasználó kifejezett kérésére (a 5. feladathoz megadta: zöld CI után emelés + build).

## Hátra
- 5. feladat kiadása/átnézése/merge/build; utána készülékes ellenőrzés (lap húzás, akadás release-szerű teszt-APK-val méréshez, Coach/Napló fejléc).
- Wear/telefon régi eszközellenőrzések (átnevezés, auto-import, óra alvás = telefon alvás, gomb/pulzus 176/192/216 mm) – korábbi 1.5.2 buildben kipróbálva a felhasználónál.

# Dizájnmunka állapota és fájltulajdon

state: `ready_for_translation_binding`
owner: AI Studio (nyelvi bekötés, a kész dizájn megőrzésével)
design_implementation_owner: Codex
design_main_source: 4737d72c75f191b18980bc40885633c1c43b5c99
verified_ci: 41733206574 / 434 JVM, 0 hiba; 12 fontarc + JS7 PASS
base_app_release: 2748 / f74133a1bb1e690ce56166c1f0370582e4eedbc6

A dizájncsomag a PR101-ben sikeres CI után mainre került. A friss main képernyőinek nyelvi bekötését az AI Studio átveheti. Implementáció és bizonyíték: DESIGN_FONT_THEME_IMPLEMENTATION.md. A korábban átadott2748 APK nem tartalmazza ezt az új forráscsomagot.

Codex elvégezte: #95 aktív sorozatsor és egységfeliratok, #97 ismétlődő napcímek, #98 fontok/ékezetek, #99 glow/világos témák, valamint a telefon/óra vezérlési státuszsáv kompakt témázott megjelenése. Nem módosítja az ownership/handover motort.

Az AI Studio ezek képernyő/theme/font fájljait addig ne írja; a pontos lista és a main/raw források az [AI Studio átadóban](AI_STUDIO_LANGUAGES_VIDEO_DATA_HANDOFF_2748.md). Erőforrásfordítás, videó és backend feedback-audit végezhető. A teljes nyelvi bekötés előtt olvasd vissza ezt a main fájlt.

Amikor state `ready_for_translation_binding`, a dizájn mainen van, a friss képernyők nyelvi bekötése átvehető az AI Studiónak. Régi teljes képernyőfájllal nem szabad felülírni az új dizájnt.

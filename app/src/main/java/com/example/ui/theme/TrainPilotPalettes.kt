package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.model.AppLanguage

enum class PaletteCategory(val labelHu: String, val labelEn: String, val iconEmoji: String) {
    ALL("Összes téma", "All Themes", "🎨"),
    MATTE("Matt témák", "Matte Themes", "🌑"),
    CONTRAST("Kontrasztos témák", "High Contrast", "⚡"),
    GLOW("Fényes / Glow témák", "Glossy & Glow", "✨"),
    LIGHT("Világos témák", "Light Themes", "☀️"),
    BASIC("Alap témák", "Basic Themes", "🌑"),
    VIVID("Vivid témák", "Vivid Themes", "✨");
}

data class TrainPilotPalette(
    val id: Int,
    val key: String,
    val nameHu: String,
    val nameEn: String,
    val nameDe: String,
    val nameRo: String,
    val category: PaletteCategory,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val cardBorder: Color,
    val success: Color = if (id in 25..32) Color(0xFF059669) else Color(0xFF34D399),
    val warning: Color = if (id in 25..32) Color(0xFFD97706) else Color(0xFFFBBF24),
    val danger: Color = if (id in 25..32) Color(0xFFDC2626) else Color(0xFFF87171),
    val isDark: Boolean = id !in 25..32,
    val primaryBright: Color = primary,
    val tertiary: Color = secondary,
    val sleepAccent: Color = if (id !in 25..32) Color(0xFF818CF8) else Color(0xFF4F46E5),
    val heartAccent: Color = if (id !in 25..32) Color(0xFFFB7185) else Color(0xFFE11D48),
    val stepsAccent: Color = if (id !in 25..32) Color(0xFF34D399) else Color(0xFF059669),
    val energyAccent: Color = if (id !in 25..32) Color(0xFFFBBF24) else Color(0xFFD97706)
) {
    // 4 distinct theme families requested by user:
    // 1..8   = Matt témák (valódi matt, alacsony telítettségű, bársonyos felületek)
    // 9..16  = Kontrasztos témák (mélyfekete háttér, éles keretek, magas kontraszt)
    // 17..24 = Fényes / Glow témák (neon ragyogás és glow kiemelés)
    // 25..32 = Világos témák (valódi világos háttér, színes keretek és kijelölések)
    val subGroup: PaletteCategory
        get() = when (id) {
            in 1..8 -> PaletteCategory.MATTE
            in 9..16 -> PaletteCategory.CONTRAST
            in 17..24 -> PaletteCategory.GLOW
            in 25..32 -> PaletteCategory.LIGHT
            else -> PaletteCategory.MATTE
        }

    val hasGlow: Boolean get() = subGroup == PaletteCategory.GLOW
    val isHighContrast: Boolean get() = subGroup == PaletteCategory.CONTRAST
    val isMatte: Boolean get() = subGroup == PaletteCategory.MATTE
    val glowColor: Color get() = if (hasGlow) primary.copy(alpha = 0.34f) else Color.Transparent

    val surfaceCard: Color get() = surface
    val textMuted: Color = textSecondary.copy(alpha = 0.90f)

    fun localizedName(lang: AppLanguage): String = when (lang) {
        AppLanguage.HU -> nameHu
        AppLanguage.EN -> nameEn
        AppLanguage.DE -> nameDe
        AppLanguage.RO -> nameRo
    }

    fun localizedName(langCode: String): String = when (langCode.uppercase()) {
        "HU" -> nameHu
        "EN" -> nameEn
        "DE" -> nameDe
        "RO" -> nameRo
        else -> nameHu
    }
}

object TrainPilotPalettes {
    val allPalettes: List<TrainPilotPalette> = listOf(
        // =========================================================================
        // 1..8: MATT TÉMÁK (Valódi matt, pasztellesített-tompított színek, bársonyos sötétszürke felületek)
        // =========================================================================
        TrainPilotPalette(
            id = 1,
            key = "graphite_gold",
            nameHu = "Matt Grafit & Óarany (Alap)",
            nameEn = "Matte Graphite & Antique Gold",
            nameDe = "Matt-Graphit & Altgold",
            nameRo = "Grafit Mat & Auriu Antic",
            category = PaletteCategory.BASIC,
            background = Color(0xFF17191E),
            surface = Color(0xFF21252C),
            surfaceElevated = Color(0xFF2A2F38),
            primary = Color(0xFFC8A25A),
            onPrimary = Color(0xFF1A1508),
            secondary = Color(0xFF6B9A9C),
            textPrimary = Color(0xFFE5E9F0),
            textSecondary = Color(0xFFA3AEB9),
            cardBorder = Color(0xFF333944)
        ),
        TrainPilotPalette(
            id = 2,
            key = "obsidian_amber",
            nameHu = "Matt Bazalt & Homokkő",
            nameEn = "Matte Basalt & Sandstone",
            nameDe = "Matt-Basalt & Sandstein",
            nameRo = "Bazalt Mat & Gresie",
            category = PaletteCategory.BASIC,
            background = Color(0xFF18191B),
            surface = Color(0xFF222428),
            surfaceElevated = Color(0xFF2B2E33),
            primary = Color(0xFFC48B52),
            onPrimary = Color(0xFF1C1208),
            secondary = Color(0xFF7A9EB8),
            textPrimary = Color(0xFFE6E8EC),
            textSecondary = Color(0xFFA5ABB6),
            cardBorder = Color(0xFF343840)
        ),
        TrainPilotPalette(
            id = 3,
            key = "midnight_teal",
            nameHu = "Matt Palaszürke & Zsálya",
            nameEn = "Matte Slate & Sage",
            nameDe = "Matt-Schiefer & Salbei",
            nameRo = "Ardezie Mată & Salvie",
            category = PaletteCategory.BASIC,
            background = Color(0xFF161B1D),
            surface = Color(0xFF1F272A),
            surfaceElevated = Color(0xFF283236),
            primary = Color(0xFF6CA192),
            onPrimary = Color(0xFF0E1B17),
            secondary = Color(0xFF8896B3),
            textPrimary = Color(0xFFE2EAE8),
            textSecondary = Color(0xFF9FB0AE),
            cardBorder = Color(0xFF313D42)
        ),
        TrainPilotPalette(
            id = 4,
            key = "cobalt_slate",
            nameHu = "Matt Ködös Acélkék",
            nameEn = "Matte Dusty Steel Blue",
            nameDe = "Matt-Stahlblau",
            nameRo = "Albastru Oțel Mat",
            category = PaletteCategory.BASIC,
            background = Color(0xFF161920),
            surface = Color(0xFF1F242E),
            surfaceElevated = Color(0xFF282F3B),
            primary = Color(0xFF7295BA),
            onPrimary = Color(0xFF0E1722),
            secondary = Color(0xFF6FA397),
            textPrimary = Color(0xFFE3E8F0),
            textSecondary = Color(0xFFA0ADBE),
            cardBorder = Color(0xFF323A48)
        ),
        TrainPilotPalette(
            id = 5,
            key = "emerald_carbon",
            nameHu = "Matt Erdei Moha & Karbon",
            nameEn = "Matte Forest Moss & Carbon",
            nameDe = "Matt-Waldmoos & Karbon",
            nameRo = "Mușchi de Pădure Mat & Carbon",
            category = PaletteCategory.BASIC,
            background = Color(0xFF161A17),
            surface = Color(0xFF1E2520),
            surfaceElevated = Color(0xFF27302A),
            primary = Color(0xFF73A37E),
            onPrimary = Color(0xFF0E1A11),
            secondary = Color(0xFFB59B6B),
            textPrimary = Color(0xFFE4ECE6),
            textSecondary = Color(0xFFA2B3A6),
            cardBorder = Color(0xFF313C34)
        ),
        TrainPilotPalette(
            id = 6,
            key = "crimson_steel",
            nameHu = "Matt Terrakotta & Acél",
            nameEn = "Matte Terracotta & Steel",
            nameDe = "Matt-Terrakotta & Stahl",
            nameRo = "Teracotă Mată & Oțel",
            category = PaletteCategory.BASIC,
            background = Color(0xFF1A1718),
            surface = Color(0xFF252022),
            surfaceElevated = Color(0xFF30292C),
            primary = Color(0xFFC27874),
            onPrimary = Color(0xFF1F0E0D),
            secondary = Color(0xFFCCA36E),
            textPrimary = Color(0xFFECE4E5),
            textSecondary = Color(0xFFB5A3A6),
            cardBorder = Color(0xFF3B3236)
        ),
        TrainPilotPalette(
            id = 7,
            key = "royal_plum",
            nameHu = "Matt Levendula & Hamu",
            nameEn = "Matte Muted Lavender & Ash",
            nameDe = "Matt-Lavendel & Asche",
            nameRo = "Lavandă Mată & Cenușă",
            category = PaletteCategory.BASIC,
            background = Color(0xFF18161C),
            surface = Color(0xFF221F28),
            surfaceElevated = Color(0xFF2C2834),
            primary = Color(0xFF9C84B8),
            onPrimary = Color(0xFF181122),
            secondary = Color(0xFF6FA3B0),
            textPrimary = Color(0xFFE9E5F0),
            textSecondary = Color(0xFFADA5BA),
            cardBorder = Color(0xFF373242)
        ),
        TrainPilotPalette(
            id = 8,
            key = "arctic_frost",
            nameHu = "Matt Északi Kavics & Olíva",
            nameEn = "Matte Nordic Pebble & Olive",
            nameDe = "Matt-Kiesel & Olive",
            nameRo = "Pietriș Nordic Mat & Măsliniu",
            category = PaletteCategory.BASIC,
            background = Color(0xFF181A18),
            surface = Color(0xFF222521),
            surfaceElevated = Color(0xFF2B2F2A),
            primary = Color(0xFF8FA677),
            onPrimary = Color(0xFF141A0E),
            secondary = Color(0xFF7C9AA6),
            textPrimary = Color(0xFFE6EAE4),
            textSecondary = Color(0xFFA7B0A4),
            cardBorder = Color(0xFF353A34)
        ),

        // =========================================================================
        // 9..16: KONTRASZTOS TÉMÁK (Mélyfekete háttér #000000/#030508, hófehér szöveg, erős keret, éles kontraszt)
        // =========================================================================
        TrainPilotPalette(
            id = 9,
            key = "copper_forge",
            nameHu = "Kontraszt Fekete & Tiszta Arany",
            nameEn = "High Contrast Black & Pure Gold",
            nameDe = "Kontrast Schwarz & Reingold",
            nameRo = "Contrast Negru & Aur Pur",
            category = PaletteCategory.BASIC,
            background = Color(0xFF000000),
            surface = Color(0xFF0A0D12),
            surfaceElevated = Color(0xFF141922),
            primary = Color(0xFFFFD60A),
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFF00E5FF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFDCE4F2),
            cardBorder = Color(0xFF475569)
        ),
        TrainPilotPalette(
            id = 10,
            key = "forest_pine",
            nameHu = "Kontraszt Éjfekete & Smaragd",
            nameEn = "High Contrast Pitch & Emerald",
            nameDe = "Kontrast Schwarz & Smaragd",
            nameRo = "Contrast Negru & Smarald",
            category = PaletteCategory.BASIC,
            background = Color(0xFF000000),
            surface = Color(0xFF07100C),
            surfaceElevated = Color(0xFF102018),
            primary = Color(0xFF00F59B),
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFFFFD166),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFD5EDE2),
            cardBorder = Color(0xFF2D5A44)
        ),
        TrainPilotPalette(
            id = 11,
            key = "sandstone_warm",
            nameHu = "Kontraszt Onyx & Jégkék",
            nameEn = "High Contrast Onyx & Ice Blue",
            nameDe = "Kontrast Onyx & Eisblau",
            nameRo = "Contrast Onix & Albastru Gheață",
            category = PaletteCategory.BASIC,
            background = Color(0xFF020408),
            surface = Color(0xFF090F18),
            surfaceElevated = Color(0xFF121C2B),
            primary = Color(0xFF38BDF8),
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFFFACC15),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFD8E6F8),
            cardBorder = Color(0xFF334E68)
        ),
        TrainPilotPalette(
            id = 12,
            key = "ocean_abyss",
            nameHu = "Kontraszt Karbon & Bíborvörös",
            nameEn = "High Contrast Carbon & Crimson",
            nameDe = "Kontrast Karbon & Karmesin",
            nameRo = "Contrast Carbon & Stacojiu",
            category = PaletteCategory.BASIC,
            background = Color(0xFF030203),
            surface = Color(0xFF11090C),
            surfaceElevated = Color(0xFF1E1116),
            primary = Color(0xFFFF4769),
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFF38BDF8),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFF2D8DF),
            cardBorder = Color(0xFF5C2B39)
        ),
        TrainPilotPalette(
            id = 13,
            key = "titanium_lime",
            nameHu = "Kontraszt Titán & Volt Lime",
            nameEn = "High Contrast Titanium & Volt Lime",
            nameDe = "Kontrast Titan & Volt-Limette",
            nameRo = "Contrast Titan & Lime Volt",
            category = PaletteCategory.BASIC,
            background = Color(0xFF020402),
            surface = Color(0xFF0B110B),
            surfaceElevated = Color(0xFF152015),
            primary = Color(0xFFB8FF01),
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFF00E5FF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFE0F2D8),
            cardBorder = Color(0xFF3E592C)
        ),
        TrainPilotPalette(
            id = 14,
            key = "burgundy_velvet",
            nameHu = "Kontraszt Obszidián & Narancs",
            nameEn = "High Contrast Obsidian & Blaze Orange",
            nameDe = "Kontrast Obsidian & Feuerorange",
            nameRo = "Contrast Obsidian & Portocaliu Intens",
            category = PaletteCategory.BASIC,
            background = Color(0xFF040200),
            surface = Color(0xFF120C07),
            surfaceElevated = Color(0xFF21160D),
            primary = Color(0xFFFF8800),
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFF00F0FF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFF5E2D0),
            cardBorder = Color(0xFF5E3A1C)
        ),
        TrainPilotPalette(
            id = 15,
            key = "nordic_sage",
            nameHu = "Kontraszt Éjfél & Ultraibolya",
            nameEn = "High Contrast Midnight & Ultraviolet",
            nameDe = "Kontrast Mitternacht & Ultraviolett",
            nameRo = "Contrast Miez de Noapte & Ultraviolet",
            category = PaletteCategory.BASIC,
            background = Color(0xFF020106),
            surface = Color(0xFF0D0918),
            surfaceElevated = Color(0xFF18112B),
            primary = Color(0xFFC084FC),
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFF2DD4BF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFE6DCF5),
            cardBorder = Color(0xFF4B3675)
        ),
        TrainPilotPalette(
            id = 16,
            key = "espresso_bronze",
            nameHu = "Kontraszt Mélyfekete & Türkiz",
            nameEn = "High Contrast Deep Black & Cyan",
            nameDe = "Kontrast Tiefschwarz & Türkis",
            nameRo = "Contrast Negru Profund & Turcoaz",
            category = PaletteCategory.BASIC,
            background = Color(0xFF000304),
            surface = Color(0xFF071216),
            surfaceElevated = Color(0xFF0F2128),
            primary = Color(0xFF00F5D4),
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFFFFB703),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFD5F2EE),
            cardBorder = Color(0xFF255963)
        ),

        // =========================================================================
        // 17..24: FÉNYES / GLOW TÉMÁK (Neon ragyogás, világító kártyaszegélyek és aktív elemek)
        // =========================================================================
        TrainPilotPalette(
            id = 17,
            key = "neon_cyber_lime",
            nameHu = "Glow Cyber Lime & Magenta",
            nameEn = "Glow Cyber Lime & Magenta",
            nameDe = "Glow Cyber-Limette & Magenta",
            nameRo = "Glow Cyber Lime & Magenta",
            category = PaletteCategory.VIVID,
            background = Color(0xFF070C09),
            surface = Color(0xFF0F1913),
            surfaceElevated = Color(0xFF17281E),
            primary = Color(0xFF9EFF00),
            onPrimary = Color(0xFF0A1400),
            secondary = Color(0xFFFF2E93),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFC7E6D2),
            cardBorder = Color(0xFF559918)
        ),
        TrainPilotPalette(
            id = 18,
            key = "hyper_cyan",
            nameHu = "Glow Hiper Cián & Korall",
            nameEn = "Glow Hyper Cyan & Coral",
            nameDe = "Glow Hyper-Cyan & Koralle",
            nameRo = "Glow Hiper Cian & Coral",
            category = PaletteCategory.VIVID,
            background = Color(0xFF060C14),
            surface = Color(0xFF0D1826),
            surfaceElevated = Color(0xFF15253B),
            primary = Color(0xFF00F0FF),
            onPrimary = Color(0xFF001B21),
            secondary = Color(0xFFFF5E7E),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFC2DEF2),
            cardBorder = Color(0xFF1C7FA6)
        ),
        TrainPilotPalette(
            id = 19,
            key = "plasma_orange",
            nameHu = "Glow Plazma Narancs & Arany",
            nameEn = "Glow Plasma Orange & Gold",
            nameDe = "Glow Plasma-Orange & Gold",
            nameRo = "Glow Portocaliu Plasmă",
            category = PaletteCategory.VIVID,
            background = Color(0xFF0E0806),
            surface = Color(0xFF1B100C),
            surfaceElevated = Color(0xFF291812),
            primary = Color(0xFFFF6D00),
            onPrimary = Color(0xFF1F0A00),
            secondary = Color(0xFFFFD600),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFE6C8BC),
            cardBorder = Color(0xFF994414)
        ),
        TrainPilotPalette(
            id = 20,
            key = "electric_indigo",
            nameHu = "Glow Elektromos Indigó",
            nameEn = "Glow Electric Indigo",
            nameDe = "Glow Elektrisches Indigo",
            nameRo = "Glow Indigo Electric",
            category = PaletteCategory.VIVID,
            background = Color(0xFF080917),
            surface = Color(0xFF11132B),
            surfaceElevated = Color(0xFF1B1E42),
            primary = Color(0xFF7C8BFF),
            onPrimary = Color(0xFF070921),
            secondary = Color(0xFF00F5D4),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFC8CFF2),
            cardBorder = Color(0xFF4753B8)
        ),
        TrainPilotPalette(
            id = 21,
            key = "synth_wave",
            nameHu = "Glow Synthwave Magenta & Cián",
            nameEn = "Glow Synthwave Magenta & Cyan",
            nameDe = "Glow Synthwave Magenta & Cyan",
            nameRo = "Glow Synthwave Magenta & Cian",
            category = PaletteCategory.VIVID,
            background = Color(0xFF0E0616),
            surface = Color(0xFF1B0D2B),
            surfaceElevated = Color(0xFF291440),
            primary = Color(0xFFFF2A9D),
            onPrimary = Color(0xFF210012),
            secondary = Color(0xFF00F0FF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFE3C4F5),
            cardBorder = Color(0xFF9E1E66)
        ),
        TrainPilotPalette(
            id = 22,
            key = "venom_green",
            nameHu = "Glow Venom NeonZöld",
            nameEn = "Glow Venom Neon Green",
            nameDe = "Glow Venom-Neongrün",
            nameRo = "Glow Verde Neon Venom",
            category = PaletteCategory.VIVID,
            background = Color(0xFF050E08),
            surface = Color(0xFF0B1C11),
            surfaceElevated = Color(0xFF122C1B),
            primary = Color(0xFF00E676),
            onPrimary = Color(0xFF00210E),
            secondary = Color(0xFFC6FF00),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFBEE6C8),
            cardBorder = Color(0xFF168A4A)
        ),
        TrainPilotPalette(
            id = 23,
            key = "solar_flare",
            nameHu = "Glow Napkitörés Arany",
            nameEn = "Glow Solar Flare Gold",
            nameDe = "Glow Sonneneruption-Gold",
            nameRo = "Glow Erupție Solară",
            category = PaletteCategory.VIVID,
            background = Color(0xFF100B05),
            surface = Color(0xFF1F160A),
            surfaceElevated = Color(0xFF2E2110),
            primary = Color(0xFFFFC400),
            onPrimary = Color(0xFF1F1500),
            secondary = Color(0xFFFF5722),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFE8D4B8),
            cardBorder = Color(0xFF9E7411)
        ),
        TrainPilotPalette(
            id = 24,
            key = "arctic_neon",
            nameHu = "Glow Sarkvidéki Auróra",
            nameEn = "Glow Arctic Aurora",
            nameDe = "Glow Arktische Aurora",
            nameRo = "Glow Aurorăini Arctică",
            category = PaletteCategory.VIVID,
            background = Color(0xFF050C12),
            surface = Color(0xFF0B1924),
            surfaceElevated = Color(0xFF122738),
            primary = Color(0xFF18FFFF),
            onPrimary = Color(0xFF001E21),
            secondary = Color(0xFF69F0AE),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFBEE0F2),
            cardBorder = Color(0xFF15879C)
        ),

        // =========================================================================
        // 25..32: VILÁGOS TÉMÁK (Valódi világos app háttér, fehér kártyák, sötét szöveg, színes keretek)
        // =========================================================================
        TrainPilotPalette(
            id = 25,
            key = "ruby_overdrive",
            nameHu = "Világos Korall & Alabástrom",
            nameEn = "Light Coral & Alabaster",
            nameDe = "Hell-Koralle & Alabaster",
            nameRo = "Coral Deschis & Alabastru",
            category = PaletteCategory.VIVID,
            background = Color(0xFFFAF6F6),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFF5E9EC),
            primary = Color(0xFFE11D48),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFD97706),
            textPrimary = Color(0xFF1E1317),
            textSecondary = Color(0xFF574148),
            cardBorder = Color(0xFFEAB8C4),
            isDark = false
        ),
        TrainPilotPalette(
            id = 26,
            key = "ultra_violet",
            nameHu = "Világos Királyi Ibolya &Gyöngy",
            nameEn = "Light Royal Violet & Pearl",
            nameDe = "Hell-Violett & Perle",
            nameRo = "Violet Regal Deschis & Perlă",
            category = PaletteCategory.VIVID,
            background = Color(0xFFF6F5FA),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFECE8F7),
            primary = Color(0xFF7C3AED),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF059669),
            textPrimary = Color(0xFF171226),
            textSecondary = Color(0xFF4A4163),
            cardBorder = Color(0xFFCBBBF2),
            isDark = false
        ),
        TrainPilotPalette(
            id = 27,
            key = "aqua_mint_vivid",
            nameHu = "Világos Menta & Kristály",
            nameEn = "Light Mint & Crystal",
            nameDe = "Hell-Minze & Kristall",
            nameRo = "Mentă Deschisă & Cristal",
            category = PaletteCategory.VIVID,
            background = Color(0xFFF1F8F6),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFE2F2EE),
            primary = Color(0xFF0D9488),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFEA580C),
            textPrimary = Color(0xFF0F2421),
            textSecondary = Color(0xFF3E5E59),
            cardBorder = Color(0xFFA7DCD4),
            isDark = false
        ),
        TrainPilotPalette(
            id = 28,
            key = "golden_phoenix",
            nameHu = "Világos Napsütés & Borostyán",
            nameEn = "Light Warm Daylight & Amber",
            nameDe = "Hell-Tageslicht & Bernstein",
            nameRo = "Lumină Caldă & Chihlimbar",
            category = PaletteCategory.VIVID,
            background = Color(0xFFFAF7F0),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFF3EBDC),
            primary = Color(0xFFD97706),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF0284C7),
            textPrimary = Color(0xFF1F180D),
            textSecondary = Color(0xFF5C4D38),
            cardBorder = Color(0xFFE8CC9C),
            isDark = false
        ),
        TrainPilotPalette(
            id = 29,
            key = "glacier_mint",
            nameHu = "Világos Gleccserkék & Ezüst",
            nameEn = "Light Glacier Blue & Silver",
            nameDe = "Hell-Gletscherblau & Silber",
            nameRo = "Albastru Ghețar Deschis",
            category = PaletteCategory.VIVID,
            background = Color(0xFFF2F7FB),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFE4EFF8),
            primary = Color(0xFF0284C7),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF059669),
            textPrimary = Color(0xFF0F1E2E),
            textSecondary = Color(0xFF3E5469),
            cardBorder = Color(0xFFAAD1EE),
            isDark = false
        ),
        TrainPilotPalette(
            id = 30,
            key = "sunset_mirage",
            nameHu = "Világos Barack & Rózsakvarc",
            nameEn = "Light Peach & Rose Quartz",
            nameDe = "Hell-Pfirsich & Rosenquarz",
            nameRo = "Piersică Deschisă & Cuarț Roz",
            category = PaletteCategory.VIVID,
            background = Color(0xFFFBF6F4),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFF5E8E4),
            primary = Color(0xFFEA580C),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFDB2777),
            textPrimary = Color(0xFF24140E),
            textSecondary = Color(0xFF61453B),
            cardBorder = Color(0xFFEDBFA8),
            isDark = false
        ),
        TrainPilotPalette(
            id = 31,
            key = "matrix_emerald",
            nameHu = "Világos Alpesi Smaragd",
            nameEn = "Light Alpine Emerald",
            nameDe = "Hell-Alpinsmaragd",
            nameRo = "Smarald Alpin Deschis",
            category = PaletteCategory.VIVID,
            background = Color(0xFFF2F8F4),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFE3F2E8),
            primary = Color(0xFF059669),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF0284C7),
            textPrimary = Color(0xFF0E2419),
            textSecondary = Color(0xFF3B5E4C),
            cardBorder = Color(0xFFA6DBC0),
            isDark = false
        ),
        TrainPilotPalette(
            id = 32,
            key = "royal_amethyst_gold",
            nameHu = "Világos Zafírkék & Arany",
            nameEn = "Light Sapphire & Gold",
            nameDe = "Hell-Saphir & Gold",
            nameRo = "Safir Deschis & Auriu",
            category = PaletteCategory.VIVID,
            background = Color(0xFFF4F6FB),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFE6ECF7),
            primary = Color(0xFF2563EB),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFD97706),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF475569),
            cardBorder = Color(0xFFB4C8F0),
            isDark = false
        )
    )

    fun getById(id: Int): TrainPilotPalette =
        allPalettes.firstOrNull { it.id == id } ?: allPalettes.first()

    fun getByKey(key: String): TrainPilotPalette? =
        allPalettes.firstOrNull { it.key.equals(key, ignoreCase = true) }
}

package com.example

import com.example.ui.theme.TrainPilotPalettes
import com.repforge.app.ui.theme.liquidGlassDropsFrame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2808 #13: Liquid Glass themes are frameless; semantic frames stay; legacy themes keep every frame. */
class LiquidGlassFrames2808Test {
    @Test fun glassPalettesDropNeutralFramesButKeepSemanticOnes() {
        val glass = TrainPilotPalettes.selectablePalettes.filter { it.isLiquidGlass }
        assertTrue(glass.size == 8)
        for (p in glass) {
            assertTrue("palette ${p.id} card frame", liquidGlassDropsFrame(p, p.cardBorder))
            assertTrue("palette ${p.id} section frame", liquidGlassDropsFrame(p, p.sectionBorder))
            for (semantic in listOf(p.primary, p.danger, p.success, p.warning))
                assertFalse("palette ${p.id} keeps semantic frame", liquidGlassDropsFrame(p, semantic.copy(alpha = 0.6f)))
        }
    }

    @Test fun legacyPalettesKeepAllFrames() {
        for (p in TrainPilotPalettes.allPalettes) assertFalse("palette ${p.id}", liquidGlassDropsFrame(p, p.cardBorder))
    }
}

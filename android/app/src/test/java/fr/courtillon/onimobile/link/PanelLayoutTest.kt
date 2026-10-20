package fr.courtillon.onimobile.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uniffi.oni_link_ffi.Face
import uniffi.oni_link_ffi.PanelRow
import uniffi.oni_link_ffi.PanelTab
import uniffi.oni_link_ffi.Param

class PanelLayoutTest {
    private fun row(param: Param, tab: PanelTab, block: String) =
        PanelRow(param, tab, block, param.name, "0", 0.5f, false)

    private val rows = listOf(
        row(Param.GAIN_RED, PanelTab.ROLL, "Source Balance"),
        row(Param.TEMPERATURE, PanelTab.COLOR, "Color"),
        row(Param.TINT, PanelTab.COLOR, "Color"),
        row(Param.EXPOSURE, PanelTab.COLOR, "Lighting"),
        row(Param.TEXTURE, PanelTab.DETAILS, "Texture"),
    )

    @Test
    fun aTabsRowsGatherUnderTheirBlocksInTheDesktopsOrder() {
        val color = PanelLayout.blocks(rows, PanelTab.COLOR)
        assertEquals(listOf("Color", "Lighting"), color.map { it.title })
        assertEquals(listOf(Param.TEMPERATURE, Param.TINT), color[0].rows.map { it.param })
        assertEquals(listOf("Texture"), PanelLayout.blocks(rows, PanelTab.DETAILS).map { it.title })
        // Twin: another tab's rows never leak into this one.
        assertFalse(color.flatMap { it.rows }.any { it.param == Param.GAIN_RED })
    }

    @Test
    fun theScannerFaceHoldsItsScreenAndKeypadWhereTheDesktopDoes() {
        assertTrue(PanelLayout.showsKeypad(Face.SCANNER, PanelTab.COLOR))
        assertTrue(PanelLayout.showsScreen(Face.SCANNER, PanelTab.ROLL))
        assertFalse(PanelLayout.showsScreen(Face.SCANNER, PanelTab.DETAILS))
        // Twin: Pro shows neither, on any tab.
        for (tab in PanelLayout.tabs) {
            assertFalse(PanelLayout.showsKeypad(Face.PRO, tab) || PanelLayout.showsScreen(Face.PRO, tab))
        }
    }

    @Test
    fun theTabsAndFacesAreTheDesktopsWords() {
        assertEquals(listOf("Roll", "Color", "Details"), PanelLayout.tabs.map(PanelLayout::title))
        assertEquals(listOf("Pro", "Scanner"), PanelLayout.faces.map(PanelLayout::title))
        // Twin: the desktop writes Color, never the British spelling.
        assertNotEquals("Colour", PanelLayout.title(PanelTab.COLOR))
    }
}

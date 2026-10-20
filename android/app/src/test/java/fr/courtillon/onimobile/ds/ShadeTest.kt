package fr.courtillon.onimobile.ds

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** The theme's shades are Slint's `brighter` and `darker`, which scale the HSV value. */
class ShadeTest {
    private fun bytes(c: Color) = listOf(c.red, c.green, c.blue).map { Math.round(it * 255) }

    @Test
    fun theKeyGreyShadesAsTheDesktopShadesIt() {
        // #313131 is 49 grey: its value times 1.25 is 61, divided by 1.25 is 39.
        assertEquals(listOf(61, 61, 61), bytes(DS.Colour.raisedHover))
        assertEquals(listOf(39, 39, 39), bytes(DS.Colour.raisedPressed))
        // A hue keeps its hue: the accent's darker shade stays blue, its channels in proportion.
        val accent = darker(DS.Colour.accent, 0.2f)
        assertEquals(DS.Colour.accent.blue / 1.2f, accent.blue, 1f / 255)
        assertEquals(DS.Colour.accent.red / 1.2f, accent.red, 1f / 255)
        // Twin: a lighter grey by the same factor is another grey, so the check reads the factor.
        assertNotEquals(bytes(DS.Colour.raisedHover), bytes(brighter(DS.Colour.raised, 0.15f)))
    }
}

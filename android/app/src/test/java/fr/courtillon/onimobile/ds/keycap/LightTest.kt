package fr.courtillon.onimobile.ds.keycap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** The wall shades the website's keycap stylesheet measures on the desktop component. */
class LightTest {
    private fun clamped(style: KeycapStyle, wall: Wall) =
        minOf(1.0, Light(style).shade(wall.normal(style)))

    @Test
    fun wallsShadeAsTheDesktopComponent() {
        val style = KeycapStyle.small
        assertEquals(0.7946, Light(style).normaliser, 1e-4)
        assertEquals(1.0, clamped(style, Wall.TOP), 1e-4)
        assertEquals(0.9729, clamped(style, Wall.LEFT), 1e-3)
        assertEquals(0.7530, clamped(style, Wall.RIGHT), 1e-3)
        assertEquals(0.4629, clamped(style, Wall.BOTTOM), 1e-3)
    }

    /** The twin: the light from the other side of the vertical must move the left and right walls. */
    @Test
    fun aMirroredLightIsSeen() {
        val mirrored = KeycapStyle.small.copy(lightAzimuth = 80.0)
        assertNotEquals(0.9729, clamped(mirrored, Wall.LEFT), 1e-3)
    }

    @Test
    fun theSmallVariantScalesLengthsButNotTheLegend() {
        val small = KeycapStyle.small
        assertEquals(4.2f, small.wallTop, 1e-4f)
        assertEquals(10.5f, small.fontSize, 1e-4f)
        assertEquals(1.5f, small.iconStroke, 1e-4f)
    }
}

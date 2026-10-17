package fr.courtillon.onimobile.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import uniffi.oni_link_ffi.Channel
import uniffi.oni_link_ffi.Frame

class ScannerTextTest {
    @Test
    fun numbersReadAsTheDesktopWritesThem() {
        assertEquals("FRAME 03/36", ScannerText.frame(Frame(3u, 36u)))
        assertEquals("FRAME --/--", ScannerText.frame(null))
        assertEquals("−4", ScannerText.spelled(-4))
        assertEquals("+12", ScannerText.spelled(12))
        assertEquals("0", ScannerText.spelled(0))
        assertEquals("−15 %", ScannerText.percent(-0.149f))
        assertEquals("0 %", ScannerText.percent(0.004f))
        assertEquals("+0.5 EV", ScannerText.stops(0.5f))
        assertEquals("0 EV", ScannerText.stops(-0.004f))
        assertEquals("−", ScannerText.legend(-1))
        assertEquals("+3", ScannerText.legend(3))
        assertEquals("B&W", ScannerText.channel(Channel.MONOCHROME))
    }

    /** The twin: a hyphen where the desktop writes a minus sign must not pass. */
    @Test
    fun theMinusIsNotAHyphen() {
        assertNotEquals("-4", ScannerText.spelled(-4))
        assertNotEquals("-0.5 EV", ScannerText.stops(-0.5f))
    }
}

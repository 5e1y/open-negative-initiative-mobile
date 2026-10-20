package fr.courtillon.onimobile.link

import fr.courtillon.onimobile.ds.DS
import fr.courtillon.onimobile.ds.Knob
import fr.courtillon.onimobile.ds.SliderFill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import uniffi.oni_link_ffi.Param

/** Each slider's rail as the desktop's panes draw it (`color_tab.slint`, `roll_tab.slint`). */
class SliderLooksTest {
    @Test
    fun eachSliderWearsThePanesRail() {
        assertEquals(SliderFill.Bipolar(DS.Colour.channelBlue, DS.Colour.channelYellow), SliderLooks.look(Param.TEMPERATURE, "Temperature").fill)
        assertEquals(SliderLook(SliderFill.Ramp(DS.Colour.white), Knob.WHITE), SliderLooks.look(Param.WHITE_POINT, "White point"))
        assertEquals(SliderLook(SliderFill.Ramp(DS.Colour.black), Knob.BLACK), SliderLooks.look(Param.BLACK_POINT, "Black point"))
        assertEquals(SliderFill.Metal, SliderLooks.look(Param.CHROME, "Chrome").fill)
        assertEquals(SliderFill.Level, SliderLooks.look(Param.EXPOSURE, "Exposure").fill)
        // A negative's gains read inverted, and their rails with them.
        assertEquals(SliderFill.Bipolar(DS.Colour.channelRed, DS.Colour.channelCyan), SliderLooks.look(Param.GAIN_RED, "Red ↔ Cyan").fill)
        // Twin: a positive's gain runs the other way.
        assertNotEquals(SliderLooks.look(Param.GAIN_RED, "Red ↔ Cyan"), SliderLooks.look(Param.GAIN_RED, "Cyan ↔ Red"))
    }
}

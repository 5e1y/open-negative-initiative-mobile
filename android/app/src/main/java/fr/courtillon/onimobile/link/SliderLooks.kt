package fr.courtillon.onimobile.link

import androidx.compose.ui.graphics.Color
import fr.courtillon.onimobile.ds.DS
import fr.courtillon.onimobile.ds.Knob
import fr.courtillon.onimobile.ds.SliderFill
import uniffi.oni_link_ffi.Param

/** A slider's rail and knob as the desktop's panes draw it. */
data class SliderLook(val fill: SliderFill, val knob: Knob = Knob.GREY)

object SliderLooks {
    private val hues = mapOf(
        "Red" to DS.Colour.channelRed, "Cyan" to DS.Colour.channelCyan,
        "Green" to DS.Colour.channelGreen, "Magenta" to DS.Colour.channelMagenta,
        "Blue" to DS.Colour.channelBlue, "Yellow" to DS.Colour.channelYellow,
    )

    private fun bipolar(low: Color, high: Color) = SliderLook(SliderFill.Bipolar(low, high))

    /** `GainPanel`'s rows go from the end their label names first, which the film's sense swaps. */
    private fun ends(label: String): SliderLook? {
        val (low, high) = label.split(" ↔ ").takeIf { it.size == 2 } ?: return null
        return bipolar(hues[low] ?: return null, hues[high] ?: return null)
    }

    fun look(param: Param, label: String): SliderLook = when (param) {
        Param.TEMPERATURE, Param.FLAT_TEMPERATURE -> bipolar(DS.Colour.channelBlue, DS.Colour.channelYellow)
        Param.TINT, Param.FLAT_TINT -> bipolar(DS.Colour.channelGreen, DS.Colour.channelMagenta)
        Param.PAPER_RED -> bipolar(DS.Colour.channelCyan, DS.Colour.channelRed)
        Param.PAPER_GREEN -> bipolar(DS.Colour.channelMagenta, DS.Colour.channelGreen)
        Param.PAPER_BLUE -> bipolar(DS.Colour.channelYellow, DS.Colour.channelBlue)
        Param.GAIN_RED, Param.GAIN_GREEN, Param.GAIN_BLUE -> ends(label) ?: SliderLook(SliderFill.Level)
        Param.WHITE_POINT -> SliderLook(SliderFill.Ramp(DS.Colour.white), Knob.WHITE)
        Param.BLACK_POINT -> SliderLook(SliderFill.Ramp(DS.Colour.black), Knob.BLACK)
        Param.CHROME -> SliderLook(SliderFill.Metal)
        else -> SliderLook(SliderFill.Level)
    }
}

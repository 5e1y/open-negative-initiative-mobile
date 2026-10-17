package fr.courtillon.onimobile.link

import uniffi.oni_link_ffi.Channel
import uniffi.oni_link_ffi.Clicks
import uniffi.oni_link_ffi.Frame
import uniffi.oni_link_ffi.Letter
import uniffi.oni_link_ffi.Machine
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** The scanner screen's words and numbers, written as the desktop's `LabScreen` and `LabStepKeys` write them. */
object ScannerText {
    private const val MINUS = "−"

    fun frame(frame: Frame?): String =
        frame?.let { String.format(Locale.ROOT, "FRAME %02d/%02d", it.index.toInt(), it.count.toInt()) } ?: "FRAME --/--"

    fun machine(machine: Machine): String = when (machine) {
        Machine.ONI -> "ONI"
        Machine.FRONTIER -> "FRONTIER"
    }

    fun channel(channel: Channel): String = when (channel) {
        Channel.NEGATIVE -> "NEGA"
        Channel.POSITIVE -> "POSI"
        Channel.MONOCHROME -> "B&W"
    }

    fun spelled(steps: Byte): String =
        if (steps.toInt() == 0) "0" else (if (steps < 0) MINUS else "+") + abs(steps.toInt())

    fun percent(value: Float): String {
        val shown = (value * 100).roundToInt()
        return if (shown == 0) "0 %" else (if (shown < 0) MINUS else "+") + "${abs(shown)} %"
    }

    fun stops(value: Float): String =
        if (abs(value) < 0.005f) "0 EV"
        else (if (value < 0) MINUS else "+") + String.format(Locale.ROOT, "%.1f EV", abs(value))

    /** A step's legend: outer keys their count, inner ones their sign. */
    fun legend(step: Int): String {
        val sign = if (step < 0) MINUS else "+"
        return if (abs(step) == 1) sign else sign + abs(step)
    }
}

val Letter.title: String
    get() = when (this) {
        Letter.D -> "D"
        Letter.C -> "C"
        Letter.M -> "M"
        Letter.Y -> "Y"
        Letter.W -> "TEMP"
    }

operator fun Clicks.get(letter: Letter): Byte = when (letter) {
    Letter.D -> d
    Letter.C -> c
    Letter.M -> m
    Letter.Y -> y
    Letter.W -> w
}

package fr.courtillon.onimobile.ui

import androidx.compose.runtime.Composable
import fr.courtillon.onimobile.ds.DS
import fr.courtillon.onimobile.ds.KeyRow
import fr.courtillon.onimobile.ds.Keypad
import fr.courtillon.onimobile.ds.Section
import fr.courtillon.onimobile.ds.Tabs
import fr.courtillon.onimobile.link.ScannerText
import fr.courtillon.onimobile.link.title
import androidx.compose.ui.graphics.Color
import uniffi.oni_link_ffi.Channel
import uniffi.oni_link_ffi.Command
import uniffi.oni_link_ffi.Effect
import uniffi.oni_link_ffi.Letter
import uniffi.oni_link_ffi.Machine
import uniffi.oni_link_ffi.ScannerState

private val legends = listOf(-3, -1, 1, 3).map(ScannerText::legend)
private val machines = listOf(Machine.ONI, Machine.FRONTIER)

/**
 * `labKeypad` as the desktop's Color tab lays it on the Scanner face: the machine, then Density,
 * Color, Look and Effects, each letter's keys −3, −, the letter (its reset), +, +3.
 */
@Composable
fun ScannerKeypad(state: ScannerState, enabled: Boolean, press: (Command) -> Unit) {
    Section("Machine") {
        Tabs(listOf("ONI", "Frontier"), machines.indexOf(state.machine), enabled = enabled) {
            press(Command.SetMachine(machines[it]))
        }
    }
    Section("Density") { Keypad { LetterKeys(Letter.D, enabled, press) } }
    Section("Color") {
        Keypad { for (letter in listOf(Letter.C, Letter.M, Letter.Y)) LetterKeys(letter, enabled, press) }
    }
    Section("Look") { Keypad { LetterKeys(Letter.W, enabled, press) } }
    Section("Effects") {
        Keypad { EffectKeys(Effect.CONTRAST, "CONTRAST", "softer" to "harder", DS.ScannerStyle.bandDark, DS.ScannerStyle.bandLight, enabled, press) }
        Keypad { EffectKeys(Effect.VIGNETTE, "VIGNETTE", "lighter corners" to "darker corners", DS.ScannerStyle.bandLight, DS.ScannerStyle.bandDark, enabled, press) }
        if (state.channel != Channel.MONOCHROME) {
            Keypad { EffectKeys(Effect.CHROME, "CHROME", "less" to "more", DS.ScannerStyle.bandDark, DS.ScannerStyle.bandLight, enabled, press) }
        }
    }
}

/** The desktop's tooltips, as the keys' spoken names: "D −3: toward dark". */
private fun helps(title: String, ends: Pair<String, String>, reset: String): List<String> =
    legends.mapIndexed { i, legend -> "$title $legend: toward ${if (i < 2) ends.first else ends.second}" }
        .let { it.take(2) + reset + it.drop(2) }

@Composable
private fun LetterKeys(letter: Letter, enabled: Boolean, press: (Command) -> Unit) {
    val (minus, plus) = bands(letter)
    KeyRow(
        letter.title, minus.second, plus.second, helps(letter.title, minus.first to plus.first, letter.title), enabled, legends,
        stepped = { press(Command.Step(letter, it.toByte())) },
        reset = { press(Command.Reset(letter)) },
    )
}

@Composable
private fun EffectKeys(effect: Effect, title: String, ends: Pair<String, String>, minus: Color, plus: Color, enabled: Boolean, press: (Command) -> Unit) {
    val name = title.lowercase().replaceFirstChar { it.uppercase() }
    KeyRow(
        title, minus, plus, helps(name, ends, "$name back to rest"), enabled, legends,
        stepped = { press(Command.StepEffect(effect, it.toByte())) },
        reset = { press(Command.ResetEffect(effect)) },
    )
}

/** Each letter's two sides, named and in the desktop's band colours. */
private fun bands(letter: Letter): Pair<Pair<String, Color>, Pair<String, Color>> = when (letter) {
    Letter.D -> ("dark" to DS.ScannerStyle.bandDark) to ("light" to DS.ScannerStyle.bandLight)
    Letter.C -> ("red" to DS.ScannerStyle.bandRed) to ("cyan" to DS.ScannerStyle.bandCyan)
    Letter.M -> ("green" to DS.ScannerStyle.bandGreen) to ("magenta" to DS.ScannerStyle.bandMagenta)
    Letter.Y -> ("blue" to DS.ScannerStyle.bandBlue) to ("yellow" to DS.ScannerStyle.bandYellow)
    Letter.W -> ("cool" to DS.ScannerStyle.bandCool) to ("warm" to DS.ScannerStyle.bandWarm)
}

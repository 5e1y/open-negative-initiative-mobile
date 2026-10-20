package fr.courtillon.onimobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.courtillon.onimobile.ds.DS
import fr.courtillon.onimobile.ds.keycap.Feel
import fr.courtillon.onimobile.ds.keycap.KeyLabel
import fr.courtillon.onimobile.ds.keycap.KeyStack
import fr.courtillon.onimobile.ds.keycap.Keycap
import fr.courtillon.onimobile.ds.keycap.KeycapGlyph
import fr.courtillon.onimobile.ds.keycap.KeycapStyle
import fr.courtillon.onimobile.ds.keycap.Rgba
import fr.courtillon.onimobile.link.ScannerText
import fr.courtillon.onimobile.link.title
import uniffi.oni_link_ffi.Channel
import uniffi.oni_link_ffi.Command
import uniffi.oni_link_ffi.Effect
import uniffi.oni_link_ffi.Letter
import uniffi.oni_link_ffi.ScannerState

private val minusSteps = listOf(-3, -1)
private val plusSteps = listOf(1, 3)

/** The scanner's keypad as the desktop draws it: per letter −3, −1, the letter (its reset), +1, +3. */
@Composable
fun ScannerKeypad(state: ScannerState, enabled: Boolean, press: (Command) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DS.Space.sm)) {
        Stack { LetterRow(Letter.D, enabled, press) }
        Stack {
            for (letter in listOf(Letter.C, Letter.M, Letter.Y)) LetterRow(letter, enabled, press)
        }
        Stack { LetterRow(Letter.W, enabled, press) }
        Stack {
            EffectRow(Effect.CONTRAST, "CONTRAST", DS.Scanner.bandDark, DS.Scanner.bandLight, enabled, press)
            EffectRow(Effect.VIGNETTE, "VIGNETTE", DS.Scanner.bandLight, DS.Scanner.bandDark, enabled, press)
            if (state.channel != Channel.MONOCHROME) {
                EffectRow(Effect.CHROME, "CHROME", DS.Scanner.bandDark, DS.Scanner.bandLight, enabled, press)
            }
        }
    }
}

@Composable
private fun LetterRow(letter: Letter, enabled: Boolean, press: (Command) -> Unit) {
    val (minus, plus) = sides(letter)
    Row(Modifier.fillMaxWidth()) {
        for (by in minusSteps) Band(ScannerText.legend(by), minus, enabled) { press(Command.Step(letter, by.toByte())) }
        Cap(letter.title, enabled, Modifier.width(DS.Scanner.wideKeyWidth)) { press(Command.Reset(letter)) }
        for (by in plusSteps) Band(ScannerText.legend(by), plus, enabled) { press(Command.Step(letter, by.toByte())) }
    }
}

@Composable
private fun EffectRow(effect: Effect, title: String, minus: Rgba, plus: Rgba, enabled: Boolean, press: (Command) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        for (by in minusSteps) Band(ScannerText.legend(by), minus, enabled) { press(Command.StepEffect(effect, by.toByte())) }
        Cap(title, enabled, Modifier.width(DS.Scanner.wideKeyWidth)) { press(Command.ResetEffect(effect)) }
        for (by in plusSteps) Band(ScannerText.legend(by), plus, enabled) { press(Command.StepEffect(effect, by.toByte())) }
    }
}

/** Each letter's two sides, in the desktop's band colours. */
private fun sides(letter: Letter): Pair<Rgba, Rgba> = when (letter) {
    Letter.D -> DS.Scanner.bandDark to DS.Scanner.bandLight
    Letter.C -> DS.Scanner.bandRed to DS.Scanner.bandCyan
    Letter.M -> DS.Scanner.bandGreen to DS.Scanner.bandMagenta
    Letter.Y -> DS.Scanner.bandBlue to DS.Scanner.bandYellow
    Letter.W -> DS.Scanner.bandCool to DS.Scanner.bandWarm
}

// Caps

/** The small variant at the remote's legend size and margin, in a given albedo and ink. */
fun scannerStyle(albedo: Rgba, ink: Rgba): KeycapStyle {
    val small = KeycapStyle.small
    val padding = maxOf(DS.Scanner.keyPaddingH.value, small.wallLeft, small.wallRight)
    return small.copy(albedo = albedo, labelColor = ink, fontSize = DS.Scanner.keyLegendSize, paddingH = padding)
}

private val greyStyle = scannerStyle(DS.Palette.keycap, DS.Palette.textOnFill)

/** Keys flush on one socket plate. */
@Composable
fun Stack(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    KeyStack(greyStyle, modifier.fillMaxWidth()) { content() }
}

/** A grey key: the letters, the effects and every key that is not a step; free width fills its share. */
@Composable
fun RowScope.Cap(
    title: String,
    enabled: Boolean,
    modifier: Modifier = Modifier.weight(1f),
    ornament: (@Composable BoxScope.() -> Unit)? = null,
    action: () -> Unit,
) {
    Keycap(KeyLabel.Text(title), action, greyStyle, modifier.height(DS.Scanner.keyHeight), enabled, Feel.LIGHT, ornament)
}

/** A grey key outside a row, at its own width. */
@Composable
fun LoneCap(title: String, enabled: Boolean, modifier: Modifier = Modifier, action: () -> Unit) {
    Keycap(KeyLabel.Text(title), action, greyStyle, modifier.height(DS.Scanner.keyHeight), enabled, Feel.LIGHT)
}

/** A key coloured whole by its side, inked dark or light by the colour's luminance. */
@Composable
private fun RowScope.Band(legend: String, fill: Rgba, enabled: Boolean, action: () -> Unit) {
    val ink = if (fill.isLight) DS.Palette.textOnLight else DS.Palette.textOnFill
    Keycap(
        KeyLabel.Text(legend), action, scannerStyle(fill, ink),
        Modifier.weight(1f).height(DS.Scanner.keyHeight), enabled, Feel.RIGID,
    )
}

@Composable
fun RowScope.IconCap(glyph: KeycapGlyph, name: String, enabled: Boolean, action: () -> Unit) {
    Keycap(
        KeyLabel.Icon(glyph, name), action, greyStyle,
        Modifier.weight(1f).height(DS.Scanner.keyHeight), enabled, Feel.LIGHT,
    )
}

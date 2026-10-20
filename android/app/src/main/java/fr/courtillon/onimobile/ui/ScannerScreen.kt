package fr.courtillon.onimobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.courtillon.onimobile.ds.DS
import fr.courtillon.onimobile.link.ScannerText
import fr.courtillon.onimobile.link.get
import fr.courtillon.onimobile.link.title
import uniffi.oni_link_ffi.Letter
import uniffi.oni_link_ffi.ScannerState

/**
 * The scanner's screen, as the desktop's `LabScreen`: the frame, the machine and channel, every
 * letter's count, then the three effects, in phosphor on glass behind scanlines.
 */
@Composable
fun ScannerScreen(state: ScannerState, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val glow = Shadow(DS.Palette.lcdInk.copy(alpha = 0.8f), Offset.Zero, with(density) { DS.Scanner.phosphorGlow.toPx() })
    fun mono(size: Float, weight: FontWeight) = TextStyle(
        color = DS.Palette.lcdInk,
        fontFamily = FontFamily.Monospace,
        fontWeight = weight,
        fontSize = with(density) { size.dp.toSp() },
        shadow = glow,
    )
    val label = mono(DS.Scanner.screenLabelSize, FontWeight.Medium)
    val value = mono(DS.Scanner.screenValueSize, FontWeight.Bold)
    val effects = buildList {
        add("CT " + ScannerText.percent(state.contrast))
        add("VIG " + ScannerText.stops(state.vignette))
        state.chrome?.let { add("CHR " + ScannerText.percent(it)) }
    }
    val header = buildList {
        add(ScannerText.frame(state.frame))
        if (state.remeasure) add("REMEASURE ROLL")
        add(ScannerText.machine(state.machine) + " · " + ScannerText.channel(state.channel))
    }
    val spoken = (header + Letter.entries.map { it.title + " " + ScannerText.spelled(state.clicks[it]) } + effects)
        .joinToString(", ")

    Column(
        modifier
            .background(DS.Palette.lcdGlass)
            .drawWithContent {
                drawContent()
                val pitch = DS.Scanner.scanlinePitch.toPx()
                val line = DS.Stroke.line.toPx()
                var y = 0f
                while (y < size.height) {
                    drawRect(DS.Scanner.scanline, Offset(0f, y), Size(size.width, line))
                    y += pitch
                }
            }
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(DS.Space.md),
    ) {
        SpreadRow(header, label)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DS.Space.sm)) {
            for (letter in Letter.entries) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(letter.title, style = label, maxLines = 1)
                    Spacer(Modifier.padding(top = DS.Space.xxs))
                    Text(ScannerText.spelled(state.clicks[letter]), style = value, maxLines = 1, softWrap = false)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        SpreadRow(effects, label)
    }
}

/** Words spread edge to edge, as an `HStack` with a `Spacer` between each. */
@Composable
private fun SpreadRow(words: List<String>, style: TextStyle) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (word in words) Text(word, style = style, maxLines = 1)
    }
}

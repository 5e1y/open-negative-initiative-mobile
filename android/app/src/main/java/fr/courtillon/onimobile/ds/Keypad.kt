package fr.courtillon.onimobile.ds

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay

/** `LabKeyboard.band`: dark ink on a light fill, light ink on a dark one, by Rec. 709 luma. */
fun isLight(fill: Color): Boolean =
    0.2126f * fill.red + 0.7152f * fill.green + 0.0722f * fill.blue > DS.ScannerStyle.lightLuma

/**
 * One key of the keypad, coloured whole by its fill; `wide` pins it beside keys of free width. The
 * cap stays down `keyHold` however short the tap, as `KeycapPress` keeps it.
 */
@Composable
fun RowScope.Key(
    legend: String,
    modifier: Modifier = Modifier,
    fill: Color = DS.ScannerStyle.capLight,
    wide: Boolean = false,
    help: String = legend,
    enabled: Boolean = true,
    clicked: () -> Unit,
) {
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    var down by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (pressed) down = true else if (down) {
            delay(DS.ScannerStyle.keyHoldMs)
            down = false
        }
    }
    val face by animateColorAsState(if (down || pressed) darker(fill, 0.2f) else fill, tween(DS.Motion.quickMs.toInt()), label = "key")
    val sized = if (wide) modifier.width(DS.ScannerStyle.wideKey) else modifier.weight(1f)
    Box(
        sized
            .height(DS.ScannerStyle.keyHeight)
            .alpha(if (enabled) 1f else DS.State.disabled)
            .background(face, RoundedCornerShape(DS.ScannerStyle.keyRadius))
            .clickable(touch, null, enabled = enabled, role = Role.Button, onClick = clicked)
            .semantics { contentDescription = help }
            .padding(horizontal = DS.ScannerStyle.keyPadding),
        contentAlignment = Alignment.Center,
    ) {
        val ink = ink(if (isLight(fill)) Tone.ON_LIGHT else Tone.ON_FILL)
        BasicText(
            legend,
            style = textStyle(TextRole.LABEL, centred = true).copy(
                color = ink, fontSize = DS.ScannerStyle.legend, fontWeight = DS.ScannerStyle.labelWeight,
            ),
            maxLines = 1,
        )
    }
}

/**
 * A letter's or a finish's five keys: three and one toward `minus`, the title back to rest, one and
 * three toward `plus`; `helps` holds the five spoken names in that order.
 */
@Composable
fun KeyRow(
    title: String,
    minus: Color,
    plus: Color,
    helps: List<String>,
    enabled: Boolean,
    legends: List<String>,
    stepped: (Int) -> Unit,
    reset: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DS.ScannerStyle.keyGap)) {
        Key(legends[0], fill = minus, help = helps[0], enabled = enabled) { stepped(-3) }
        Key(legends[1], fill = minus, help = helps[1], enabled = enabled) { stepped(-1) }
        Key(title, wide = true, help = helps[2].ifEmpty { title }, enabled = enabled, clicked = reset)
        Key(legends[2], fill = plus, help = helps[3], enabled = enabled) { stepped(1) }
        Key(legends[3], fill = plus, help = helps[4], enabled = enabled) { stepped(3) }
    }
}

/** `LabStack`: the keypad's rows on one socket. */
@Composable
fun Keypad(modifier: Modifier = Modifier, rows: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(DS.ScannerStyle.socket, RoundedCornerShape(DS.ScannerStyle.socketRadius))
            .padding(DS.ScannerStyle.keyGap),
        verticalArrangement = Arrangement.spacedBy(DS.ScannerStyle.keyGap),
        content = rows,
    )
}

/** One count of `LabScreen`: a letter's title over its value. */
data class LabCell(val title: String, val value: String)

/**
 * `LabScreen`: the scanner's screen, its frame, its machine, every letter's count and the finishing's
 * values, in the glass's ink behind scanlines.
 */
@Composable
fun LabScreen(frame: String, machine: String, remeasure: Boolean, cells: List<LabCell>, finish: List<String>, spoken: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .height(DS.ScannerStyle.screen)
            .background(DS.ScannerStyle.glass)
            .scanlines()
            .semantics { contentDescription = spoken }
            .padding(DS.Space.m),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Lcd(frame)
            if (remeasure) Lcd("REMEASURE ROLL")
            Lcd(machine)
        }
        Box(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DS.Space.s)) {
            for (cell in cells) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DS.Space.xxs)) {
                    Lcd(cell.title)
                    Lcd(cell.value, large = true)
                }
            }
        }
        Box(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (line in finish) Lcd(line)
        }
    }
}

@Composable
private fun Lcd(text: String, large: Boolean = false) {
    BasicText(
        text,
        style = textStyle(TextRole.LABEL).copy(
            color = DS.ScannerStyle.lcdInk,
            fontFamily = FontFamily.Monospace,
            fontSize = if (large) DS.ScannerStyle.screenValue else DS.ScannerStyle.screenLabel,
            fontWeight = if (large) DS.ScannerStyle.valueWeight else DS.ScannerStyle.labelWeight,
        ),
        maxLines = 1,
        softWrap = false,
    )
}

private fun Modifier.scanlines(): Modifier = drawWithContent {
    drawContent()
    val pitch = DS.ScannerStyle.scanlinePitch.toPx()
    val line = DS.Stroke.line.toPx()
    var y = 0f
    while (y < size.height) {
        drawRect(DS.ScannerStyle.scanline, Offset(0f, y), Size(size.width, line))
        y += pitch
    }
}

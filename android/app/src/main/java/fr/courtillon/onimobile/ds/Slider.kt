package fr.courtillon.onimobile.ds

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription

/** How the rail fills up to the knob: the track's fill, a tint's ramp, two ends' gradient, or metal. */
sealed interface SliderFill {
    data object Level : SliderFill
    data class Ramp(val tint: Color) : SliderFill
    data class Bipolar(val low: Color, val high: Color) : SliderFill
    data object Metal : SliderFill
}

enum class Knob { GREY, WHITE, BLACK }

/** What a hand does to a slider; the owner holds the value and writes the readout. */
interface SliderHand {
    /** The knob at `position` on the track, 0 to 1, at every move of a held drag. */
    fun slide(position: Float)
    /** The hand off the knob. */
    fun release()
    /** A double tap: back where the desktop's double click returns it. */
    fun reset()
}

/**
 * `DSSlider`: the label and its readout, then a track whose whole row grabs. A drag moves the knob
 * by the finger's travel from where it stood, never jumping to the touch; a double tap resets it.
 */
@Composable
fun Slider(
    label: String,
    readout: String,
    fraction: Float,
    hand: SliderHand,
    modifier: Modifier = Modifier,
    fill: SliderFill = SliderFill.Level,
    knob: Knob = Knob.GREY,
    enabled: Boolean = true,
) {
    var held by remember { mutableStateOf(false) }
    var dragged by remember { mutableFloatStateOf(0f) }
    var lastTap by remember { mutableLongStateOf(0L) }
    val at = (if (held) dragged else fraction).coerceIn(0f, 1f)
    val resting by rememberUpdatedState(fraction)
    val owner by rememberUpdatedState(hand)
    val doubleTap = LocalViewConfiguration.current.doubleTapTimeoutMillis
    val grow by animateFloatAsState(if (held) DS.SliderStyle.knobGrow else 1f, tween(DS.Motion.quickMs.toInt()), label = "grow")

    Column(
        modifier
            .alpha(if (enabled) 1f else DS.State.disabled)
            .semantics {
                contentDescription = label
                stateDescription = readout
                progressBarRangeInfo = ProgressBarRangeInfo(at, 0f..1f)
                if (enabled) {
                    setProgress { target ->
                        owner.slide(target.coerceIn(0f, 1f))
                        owner.release()
                        true
                    }
                }
            },
        verticalArrangement = Arrangement.spacedBy(DS.SliderStyle.labelGap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(DS.Space.s)) {
            Label(label, Modifier.weight(1f))
            Label(readout, role = TextRole.VALUE, tone = Tone.SECONDARY)
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(DS.SliderStyle.row)
                .padding(horizontal = DS.SliderStyle.knobWidth / 2)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val travel = size.width.toFloat().coerceAtLeast(1f)
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (down.uptimeMillis - lastTap <= doubleTap) {
                            lastTap = 0L
                            down.consume()
                            owner.reset()
                            waitForUpOrCancellation()
                            return@awaitEachGesture
                        }
                        val start = resting
                        var dx = 0f
                        val grabbed = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                            change.consume()
                            dx = over
                        }
                        if (grabbed == null) {
                            // A tap, maybe a double tap's first; a vertical swipe is the page's scroll.
                            val up = currentEvent.changes.firstOrNull { it.id == down.id }
                            if (up != null && up.changedToUpIgnoreConsumed() && !up.isConsumed) lastTap = up.uptimeMillis
                            return@awaitEachGesture
                        }
                        held = true
                        dragged = (start + dx / travel).coerceIn(0f, 1f)
                        owner.slide(dragged)
                        horizontalDrag(grabbed.id) { change ->
                            dx += change.positionChange().x
                            change.consume()
                            dragged = (start + dx / travel).coerceIn(0f, 1f)
                            owner.slide(dragged)
                        }
                        held = false
                        owner.release()
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val travel = maxWidth
            val half = DS.SliderStyle.knobWidth / 2
            // Drawn past the travel by the knob's half-width at both ends, filled up to the value.
            Box(
                Modifier
                    .offset(x = -half)
                    .requiredWidth(travel + DS.SliderStyle.knobWidth)
                    .height(DS.SliderStyle.track)
                    .clip(RoundedCornerShape(DS.SliderStyle.trackRadius))
                    .background(rail(fill)),
            ) {
                if (fill !is SliderFill.Bipolar) {
                    Box(Modifier.width(half + travel * at).fillMaxHeight().background(level(fill)))
                }
            }
            Box(
                Modifier
                    .offset(x = travel * at - DS.SliderStyle.knobWidth * grow / 2)
                    .requiredWidth(DS.SliderStyle.knobWidth * grow)
                    .requiredHeight(DS.SliderStyle.knobHeight * grow)
                    .clip(RoundedCornerShape(DS.SliderStyle.knobRadius))
                    .background(knobFace(knob))
                    .border(DS.Stroke.line, DS.SliderStyle.knobEdge, RoundedCornerShape(DS.SliderStyle.knobRadius)),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(DS.SliderStyle.gripGap - DS.Stroke.line)) {
                    repeat(2) {
                        Box(
                            Modifier
                                .width(DS.Stroke.line)
                                .height(DS.SliderStyle.grip)
                                .background(if (knob == Knob.WHITE) DS.Colour.inkOnLight else DS.SliderStyle.gripInk),
                        )
                    }
                }
            }
        }
    }
}

private fun rail(fill: SliderFill): Brush = when (fill) {
    is SliderFill.Bipolar -> Brush.horizontalGradient(listOf(fill.low, fill.high))
    else -> Brush.horizontalGradient(listOf(DS.Colour.track, DS.Colour.track))
}

private fun level(fill: SliderFill): Brush = when (fill) {
    is SliderFill.Ramp -> Brush.horizontalGradient(
        listOf(fill.tint.copy(alpha = DS.SliderStyle.rampStart), fill.tint.copy(alpha = DS.SliderStyle.rampEnd)),
    )
    SliderFill.Metal -> Brush.verticalGradient(
        0f to DS.Colour.metalDark, 0.35f to DS.Colour.metalLight, 0.65f to DS.Colour.metalLight, 1f to DS.Colour.metalDark,
    )
    else -> Brush.horizontalGradient(listOf(DS.Colour.trackFill, DS.Colour.trackFill))
}

private fun knobFace(knob: Knob): Color = when (knob) {
    Knob.WHITE -> DS.Colour.white
    Knob.BLACK -> DS.Colour.black
    Knob.GREY -> DS.SliderStyle.knob
}

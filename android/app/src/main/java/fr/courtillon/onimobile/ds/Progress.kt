package fr.courtillon.onimobile.ds

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/** `DSProgress`: a bar filled to `fraction`, or a segment sweeping to and fro while it is unknown. */
@Composable
fun Progress(fraction: Float?, modifier: Modifier = Modifier) {
    val sweep = rememberInfiniteTransition(label = "sweep")
    val swing by sweep.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(DS.Motion.sweepMs.toInt(), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "swing",
    )
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(DS.ProgressStyle.height)
            .background(DS.Colour.track, RoundedCornerShape(DS.Radius.track)),
    ) {
        val width = if (fraction != null) maxWidth * fraction.coerceIn(0f, 1f) else maxWidth * DS.ProgressStyle.segment
        val x = if (fraction != null) maxWidth * 0f else (maxWidth - width) * swing
        Box(Modifier.offset(x = x).width(width).fillMaxHeight().background(DS.Colour.selected, RoundedCornerShape(DS.Radius.track)))
    }
}

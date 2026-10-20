package fr.courtillon.onimobile.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape

enum class Ground { GROUND, CANVAS, WELL, SKELETON }

fun ground(ground: Ground) = when (ground) {
    Ground.GROUND -> DS.Colour.ground
    Ground.CANVAS -> DS.Colour.canvas
    Ground.WELL -> DS.Colour.well
    Ground.SKELETON -> DS.Colour.skeleton
}

/** `Surface` and `Picture`'s ground: the window's, the canvas's dark or a photo's well, rounded or not. */
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    ground: Ground = Ground.GROUND,
    rounded: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val shape = if (rounded) RoundedCornerShape(DS.Radius.badge) else RectangleShape
    Box(modifier.clip(shape).background(ground(ground), shape), contentAlignment = Alignment.Center, content = content)
}

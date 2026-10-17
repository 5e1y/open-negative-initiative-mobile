package fr.courtillon.onimobile.ds.keycap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Trinil glyphs on a 24-unit grid. Where one stroke ends on another's corner the two are one subpath,
 * since flat caps leave a notch where separate strokes meet.
 */
enum class KeycapGlyph(val path: String) {
    CHEVRON_LEFT("M17.1879 21.75L7.54393 12.1061C7.48536 12.0475 7.48535 11.9525 7.54393 11.8939L17.1879 2.25"),
    CHEVRON_RIGHT("M8.25 21.75L17.8939 12.1061C17.9525 12.0475 17.9525 11.9525 17.8939 11.8939L8.25 2.25"),
    UNDO("M9.75 15.75H2.25V8.24995M2.25 15.75L6 12C12.75 5.24999 20.0996 8.3985 21.75 15"),
    REDO("M14.25 15.75H21.75V8.24995M21.75 15.75L18 12C11.25 5.24999 3.90036 8.3985 2.25 15"),
}

private const val VIEW_BOX = 24f
private const val MITER_LIMIT = 1.78829f

/** A glyph stroked at a fixed weight whatever its size: the path is scaled, never the stroke. */
@Composable
fun KeycapIcon(glyph: KeycapGlyph, size: Float, stroke: Float, color: Color) {
    val path = remember(glyph) { PathParser().parsePathString(glyph.path).toPath() }
    Canvas(Modifier.size(size.dp)) {
        val k = this.size.width / VIEW_BOX
        scale(k, k, pivot = Offset.Zero) {
            drawPath(
                path,
                color,
                style = Stroke(stroke.dp.toPx() / k, cap = StrokeCap.Butt, join = StrokeJoin.Miter, miter = MITER_LIMIT),
            )
        }
    }
}

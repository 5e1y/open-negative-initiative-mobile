package fr.courtillon.onimobile.ds

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

/** A glyph tinted by an ink, square at the size it is given; its words are its owner's. */
@Composable
fun Icon(glyph: ImageVector, modifier: Modifier = Modifier, ink: Color = DS.Colour.inkSecondary) {
    Image(rememberVectorPainter(glyph), null, modifier, colorFilter = ColorFilter.tint(ink))
}

/** The desktop's Trinil glyphs (the SVGs of `ui/icons`), their strokes and caps as drawn there. */
object Icons {
    val undo = trinil(24f, 1.5f, "M2.25 8.24995V15.75M2.25 15.75H9.75M2.25 15.75L6 12C12.75 5.24999 20.0996 8.3985 21.75 15")
    val redo = trinil(24f, 1.5f, "M21.75 8.24995V15.75M21.75 15.75H14.25M21.75 15.75L18 12C11.25 5.24999 3.90036 8.3985 2.25 15")
    // `chevron-right.svg` draws one unit at any size: its share of a 16-point glyph.
    val chevronRight = trinil(22f, 1.375f, "M6.1487 20.75L15.7926 11.1061C15.8513 11.0475 15.8513 10.9525 15.7926 10.8939L6.1487 1.25")
    /** `chevron-right.svg` mirrored, which the set lacks. */
    val chevronLeft = trinil(22f, 1.375f, "M15.8513 20.75L6.2074 11.1061C6.1487 11.0475 6.1487 10.9525 6.2074 10.8939L15.8513 1.25")
    val warning = trinil(
        24f, 1.5f,
        "M12 16.5V17.25M12 13.5V9M10.678 3.94661L2.65935 18.787C2.11934 19.7864 2.84306 21 3.97903 21H19.9887" +
            "C21.1237 21 21.8475 19.7882 21.3094 18.7889L13.3184 3.94852C12.7525 2.89758 11.2455 2.89649 10.678 3.94661Z",
    )

    private fun trinil(side: Float, stroke: Float, path: String): ImageVector =
        ImageVector.Builder(defaultWidth = side.dp, defaultHeight = side.dp, viewportWidth = side, viewportHeight = side)
            .addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .build()
}

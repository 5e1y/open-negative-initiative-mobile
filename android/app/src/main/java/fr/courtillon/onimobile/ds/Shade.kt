package fr.courtillon.onimobile.ds

import androidx.compose.ui.graphics.Color

/** Slint's `brighter`: the HSV value times `1 + factor`, so the theme's shades are the desktop's. */
fun brighter(color: Color, factor: Float): Color = scaled(color, 1f + factor)

/** Slint's `darker`: the HSV value divided by `1 + factor`. */
fun darker(color: Color, factor: Float): Color = scaled(color, 1f / (1f + factor))

private fun scaled(color: Color, by: Float): Color {
    val hsv = FloatArray(3)
    val max = maxOf(color.red, color.green, color.blue)
    val min = minOf(color.red, color.green, color.blue)
    hsv[2] = max
    hsv[1] = if (max == 0f) 0f else (max - min) / max
    hsv[0] = hue(color, max, min)
    val v = (hsv[2] * by).coerceIn(0f, 1f)
    return Color.hsv(hsv[0], hsv[1], v, color.alpha)
}

private fun hue(c: Color, max: Float, min: Float): Float {
    val d = max - min
    if (d == 0f) return 0f
    val h = when (max) {
        c.red -> ((c.green - c.blue) / d).mod(6f)
        c.green -> (c.blue - c.red) / d + 2f
        else -> (c.red - c.green) / d + 4f
    }
    return h * 60f
}

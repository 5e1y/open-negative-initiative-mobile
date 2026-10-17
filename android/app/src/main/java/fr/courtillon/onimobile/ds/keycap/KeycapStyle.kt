package fr.courtillon.onimobile.ds.keycap

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** A colour in sRGB, 0 to 1 per channel, as the desktop's `KeycapStyle.RGBA`. */
data class Rgba(val r: Float, val g: Float, val b: Float, val a: Float = 1f) {
    val color: Color get() = Color(r, g, b, a)

    /** Every frustum face comes from the albedo this way; alpha is untouched, a shade is not transparency. */
    fun shaded(shade: Double): Color = Color(
        min(1.0, r * shade).toFloat(), min(1.0, g * shade).toFloat(), min(1.0, b * shade).toFloat(), a,
    )

    /** A fill pale enough to take dark ink, by Rec. 709 luminance. */
    val isLight: Boolean get() = 0.2126 * r + 0.7152 * g + 0.0722 * b > 0.55
}

/**
 * Everything a key is drawn and animated from, lengths in dp, copied from the desktop's `KeycapStyle`
 * at its hardened values. The desktop's stays authoritative: if it moves, copy it again.
 */
data class KeycapStyle(
    val paddingH: Float = 32f,
    val cornerRadius: Float = 16f,
    val socketColor: Rgba = Rgba(0f, 0f, 0f),
    // Four bevel runs: more wall below than above tilts the face away from the viewer.
    val wallTop: Float = 8f,
    val wallBottom: Float = 12f,
    val wallLeft: Float = 8f,
    val wallRight: Float = 8f,
    val rise: Float = 6f,
    val faceRadius: Float = 14f,
    val dishDepth: Float = 4f,
    val seamBlur: Float = 3f,
    val albedo: Rgba = Rgba(1f, 1f, 1f),
    val ambient: Double = 0.25,
    val diffuse: Double = 1.0,
    val lightAzimuth: Double = 100.0,
    val lightElevation: Double = 33.0,
    /** How far the albedo is the flat face's middle rather than a ceiling. */
    val midtoneMatch: Double = 1.0,
    val socketShadowSoftness: Float = 0.5f,
    val socketShadowRest: Float = 2f,
    val socketShadowPressed: Float = 6f,
    val shadowBlur: Float = 8f,
    val shadowY: Float = 10f,
    val shadowOpacity: Float = 0.2f,
    val scalePressed: Float = 0.95f,
    val travel: Float = 1f,
    val fontSize: Float = 14f,
    val iconSize: Float = 28f,
    val iconStroke: Float = 2.5f,
    val tracking: Float = 0.5f,
    val labelColor: Rgba = Rgba(0.1298f, 0.1298f, 0.1298f),
) {
    /** Centres the label on the face, apart from the padding so retuning the walls never moves the height. */
    val labelCentring: Float get() = (wallTop - wallBottom) / 2

    fun travel(depth: Float) = travel * depth
    fun scale(depth: Float) = 1 + (scalePressed - 1) * depth
    fun socketShadow(depth: Float) = socketShadowRest + (socketShadowPressed - socketShadowRest) * depth
    fun shadowOffset(depth: Float) = shadowY * (1 - depth)
    fun shadowStrength(depth: Float) = shadowOpacity * (1 - depth)

    /** Every length but the font size and the stroke, which are weights rather than proportions. */
    fun scaled(factor: Float) = copy(
        paddingH = paddingH * factor, cornerRadius = cornerRadius * factor,
        wallTop = wallTop * factor, wallBottom = wallBottom * factor,
        wallLeft = wallLeft * factor, wallRight = wallRight * factor,
        rise = rise * factor, faceRadius = faceRadius * factor, dishDepth = dishDepth * factor,
        seamBlur = seamBlur * factor, socketShadowRest = socketShadowRest * factor,
        socketShadowPressed = socketShadowPressed * factor, shadowBlur = shadowBlur * factor,
        shadowY = shadowY * factor, travel = travel * factor, iconSize = iconSize * factor,
    )

    /** The face inside the walls; below `MIN_FACE_SHARE` the walls shrink but keep their shading. */
    fun faceRect(width: Float, height: Float): FaceRect {
        val squeeze = min(fit(wallTop + wallBottom, height), fit(wallLeft + wallRight, width))
        return FaceRect(
            left = wallLeft * squeeze,
            top = wallTop * squeeze,
            width = max(0f, width - (wallLeft + wallRight) * squeeze),
            height = max(0f, height - (wallTop + wallBottom) * squeeze),
        )
    }

    private fun fit(runs: Float, extent: Float): Float {
        val allowed = extent * (1 - MIN_FACE_SHARE)
        return if (runs > allowed && runs > 0) allowed / runs else 1f
    }

    companion object {
        const val MIN_FACE_SHARE = 0.4f
        /** The shortest time a key stays down: without it a tap plays a tenth of the press and all the release. */
        const val MIN_HOLD_MS = 140L

        /** The desktop's small variant: every length at 0.525, the legend and stroke set rather than scaled. */
        val small: KeycapStyle = KeycapStyle().scaled(0.525f).copy(fontSize = 10.5f, iconStroke = 1.5f)
    }
}

data class FaceRect(val left: Float, val top: Float, val width: Float, val height: Float)

data class Normal(val x: Double, val y: Double, val z: Double)

/** The four wall faces; each is planar, so it has one normal and one flat shade. */
enum class Wall {
    TOP, BOTTOM, LEFT, RIGHT;

    fun run(style: KeycapStyle): Double = when (this) {
        TOP -> style.wallTop
        BOTTOM -> style.wallBottom
        LEFT -> style.wallLeft
        RIGHT -> style.wallRight
    }.toDouble()

    fun normal(style: KeycapStyle): Normal {
        val run = run(style)
        val rise = style.rise.toDouble()
        val length = max(1e-9, sqrt(run * run + rise * rise))
        return when (this) {
            TOP -> Normal(0.0, -rise / length, run / length)
            BOTTOM -> Normal(0.0, rise / length, run / length)
            LEFT -> Normal(-rise / length, 0.0, run / length)
            RIGHT -> Normal(rise / length, 0.0, run / length)
        }
    }
}

/** The key's light: five values in place of nine hand-picked colours, as the desktop's `Frustum.Light`. */
class Light(private val style: KeycapStyle) {
    val direction: Normal = run {
        val e = Math.toRadians(style.lightElevation)
        val a = Math.toRadians(style.lightAzimuth)
        Normal(cos(e) * cos(a), -cos(e) * sin(a), sin(e))
    }

    /** What every shade is divided by: 1 at `midtoneMatch = 0`, the flat face's shade at 1. */
    val normaliser: Double = run {
        val flatFace = style.ambient + style.diffuse * max(0.0, direction.z)
        1 + (flatFace - 1) * min(1.0, max(0.0, style.midtoneMatch))
    }

    /** Unclamped: a shade above 1 is a highlight, and `Rgba.shaded` clips each channel. */
    fun shade(n: Normal): Double {
        val lambert = max(0.0, n.x * direction.x + n.y * direction.y + n.z * direction.z)
        return (style.ambient + style.diffuse * lambert) / max(1e-6, normaliser)
    }

    fun colour(wall: Wall): Color = style.albedo.shaded(shade(wall.normal(style)))

    /** Seven stops read as a curve: at five a seam shows on a magnified face, at nine nothing changes. */
    fun faceStops(faceHeight: Float): Array<Pair<Float, Color>> = Array(DISH_STOPS) { index ->
        val t = index.toDouble() / (DISH_STOPS - 1)
        t.toFloat() to style.albedo.shaded(shade(dishNormal(t, style.dishDepth.toDouble(), faceHeight.toDouble())))
    }

    companion object {
        const val DISH_STOPS = 7

        /** The scoop's normal at `t`, 0 at the face's top edge to 1 at its bottom. */
        fun dishNormal(t: Double, depth: Double, height: Double): Normal {
            if (height <= 0) return Normal(0.0, 0.0, 1.0)
            val slope = 4 * depth * (2 * t - 1) / height
            val length = sqrt(1 + slope * slope)
            return Normal(0.0, -slope / length, 1 / length)
        }
    }
}

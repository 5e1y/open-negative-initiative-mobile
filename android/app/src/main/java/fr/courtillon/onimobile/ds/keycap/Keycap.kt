package fr.courtillon.onimobile.ds.keycap

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A word or a glyph, never both: a small cap has no room for both to be read. */
sealed interface KeyLabel {
    data class Text(val text: String) : KeyLabel
    data class Icon(val glyph: KeycapGlyph, val name: String) : KeyLabel
}

/** The tap felt at press-down, ahead of the action and of anything it sends. */
enum class Feel(val constant: Int) {
    LIGHT(HapticFeedbackConstants.KEYBOARD_TAP),
    RIGID(HapticFeedbackConstants.VIRTUAL_KEY),
}

/** Fades the legend alone: a translucent cap lets its ground through and reads lighter than its surface. */
private const val DISABLED_OPACITY = 0.3f

/** The reference press and release curves; they decelerate from full speed with no overshoot. */
private val pressSpec = tween<Float>(100, easing = CubicBezierEasing(0f, 0f, 0f, 1f))
private val releaseSpec = tween<Float>(20, easing = CubicBezierEasing(0.79f, 0.03f, 0.43f, 1f))

/**
 * The desktop's keycap: flat-shaded walls, a dished face and the socket's rim shadow, sinking as it is
 * pressed. The caller sizes it; the action fires on release, the cap stays down at least `MIN_HOLD_MS`.
 */
@Composable
fun Keycap(
    label: KeyLabel,
    onClick: () -> Unit,
    style: KeycapStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    feel: Feel? = null,
    ornament: (@Composable BoxScope.() -> Unit)? = null,
) {
    val depth = remember { Animatable(0f) }
    val press = remember { PressHold() }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val context = LocalContext.current
    val click by rememberUpdatedState(onClick)
    val light = remember(style) { Light(style) }
    val shape = RoundedCornerShape(style.cornerRadius.dp)
    val name = when (label) {
        is KeyLabel.Text -> label.text
        is KeyLabel.Icon -> label.name
    }

    Box(
        modifier
            .semantics {
                role = Role.Button
                contentDescription = name
                if (!enabled) disabled()
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        press.release?.cancel()
                        val pressedAt = SystemClock.uptimeMillis()
                        feel?.let { view.performHapticFeedback(it.constant) }
                        KeycapSound.press(context)
                        scope.launch { depth.animateTo(1f, pressSpec) }
                        tryAwaitRelease()
                        val remaining = KeycapStyle.MIN_HOLD_MS - (SystemClock.uptimeMillis() - pressedAt)
                        press.release = scope.launch {
                            if (remaining > 0) delay(remaining)
                            depth.animateTo(0f, releaseSpec)
                            // The release sound follows the cap rising, not the finger lifting.
                            KeycapSound.release()
                        }
                    },
                    onTap = { click() },
                )
            }
            .drawBehind { drawShadowAndBezel(style, depth.value) },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val s = style.scale(depth.value)
                    scaleX = s
                    scaleY = s
                    this.shape = shape
                    clip = true
                },
        ) {
            // The walls blur as one layer; `Clamp` extends their edge so no fringe of bezel shows through.
            Spacer(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationY = style.travel(depth.value).dp.toPx()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val blur = style.seamBlur.dp.toPx()
                            renderEffect = BlurEffect(blur, blur, TileMode.Clamp)
                        }
                    }
                    .drawBehind { drawWalls(style, light) },
            )
            Spacer(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { translationY = style.travel(depth.value).dp.toPx() }
                    .drawBehind { drawFace(style, light) },
            )
            Spacer(Modifier.matchParentSize().drawBehind { drawSocketShadow(style, depth.value) })
        }
        // The label rides the face: it sinks and shrinks with it, never touching layout.
        Box(
            Modifier
                .matchParentSize()
                .padding(horizontal = style.paddingH.dp)
                .graphicsLayer {
                    translationY = (style.labelCentring + style.travel(depth.value)).dp.toPx()
                    val s = style.scale(depth.value)
                    scaleX = s
                    scaleY = s
                    alpha = if (enabled) 1f else DISABLED_OPACITY
                },
            contentAlignment = Alignment.Center,
        ) {
            when (label) {
                is KeyLabel.Text -> BasicText(
                    label.text,
                    style = TextStyle(
                        color = style.labelColor.color,
                        fontSize = with(LocalDensity.current) { style.fontSize.dp.toSp() },
                        fontWeight = FontWeight.Bold,
                        letterSpacing = with(LocalDensity.current) { style.tracking.dp.toSp() },
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
                is KeyLabel.Icon -> KeycapIcon(label.glyph, style.iconSize, style.iconStroke, style.labelColor.color)
            }
            ornament?.invoke(this)
        }
    }
}

/** The pending lift of a tap held for `MIN_HOLD_MS`, cancelled by the next press so it cannot lift that one. */
private class PressHold {
    var release: Job? = null
}

/** Keycaps flush on one socket plate, gap zero, so the rows read as one object, as the desktop's `LabStack`. */
@Composable
fun KeyStack(style: KeycapStyle, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.background(style.socketColor.color, RoundedCornerShape(style.cornerRadius.dp)),
        content = content,
    )
}

// Drawing

/** The cast shadow falls outside the cap, then the bezel the cap sits in shows wherever the cap shrinks. */
private fun DrawScope.drawShadowAndBezel(style: KeycapStyle, depth: Float) {
    val radius = style.cornerRadius.dp.toPx()
    val strength = style.shadowStrength(depth)
    if (strength > 0) {
        val offset = style.shadowOffset(depth).dp.toPx()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.socketColor.color.copy(alpha = strength).toArgb()
            maskFilter = BlurMaskFilter(style.shadowBlur.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
        }
        drawIntoCanvas {
            it.nativeCanvas.drawRoundRect(0f, offset, size.width, size.height + offset, radius, radius, paint)
        }
    }
    drawRoundRect(style.socketColor.color, cornerRadius = CornerRadius(radius))
}

private fun DrawScope.faceRect(style: KeycapStyle): FaceRect {
    val face = style.faceRect(size.width / density, size.height / density)
    return FaceRect(face.left * density, face.top * density, face.width * density, face.height * density)
}

/** Four flat quadrilaterals, run under the face so the blur has material on both sides of every seam. */
private fun DrawScope.drawWalls(style: KeycapStyle, light: Light) {
    val face = faceRect(style)
    val bleed = minOf(2 * style.seamBlur.dp.toPx(), face.width / 2, face.height / 2)
    val l = face.left + bleed
    val t = face.top + bleed
    val r = face.left + face.width - bleed
    val b = face.top + face.height - bleed
    val w = size.width
    val h = size.height
    for (wall in Wall.entries) {
        val corners = when (wall) {
            Wall.TOP -> listOf(0f to 0f, w to 0f, r to t, l to t)
            Wall.BOTTOM -> listOf(0f to h, w to h, r to b, l to b)
            Wall.LEFT -> listOf(0f to 0f, 0f to h, l to b, l to t)
            Wall.RIGHT -> listOf(w to 0f, w to h, r to b, r to t)
        }
        val path = Path().apply {
            moveTo(corners[0].first, corners[0].second)
            corners.drop(1).forEach { lineTo(it.first, it.second) }
            close()
        }
        drawPath(path, light.colour(wall))
    }
}

private fun DrawScope.drawFace(style: KeycapStyle, light: Light) {
    val face = faceRect(style)
    drawRoundRect(
        brush = Brush.verticalGradient(
            *light.faceStops(face.height / density),
            startY = face.top,
            endY = face.top + face.height,
        ),
        topLeft = Offset(face.left, face.top),
        size = Size(face.width, face.height),
        cornerRadius = CornerRadius(style.faceRadius.dp.toPx()),
    )
}

/** The rim darkening as the cap sinks, strongest at the edge: a centred stroke, blurred, inside the clip. */
private fun DrawScope.drawSocketShadow(style: KeycapStyle, depth: Float) {
    val spread = style.socketShadow(depth).dp.toPx()
    if (spread <= 0) return
    val radius = style.cornerRadius.dp.toPx()
    val blur = spread * style.socketShadowSoftness
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.STROKE
        strokeWidth = spread * 2
        color = style.socketColor.color.toArgb()
        if (blur > 0) maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
    }
    drawIntoCanvas { it.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, radius, radius, paint) }
}

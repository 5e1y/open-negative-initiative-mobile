package fr.courtillon.onimobile.ds

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class ButtonKind { PLAIN, ACCENT, DESTRUCTIVE }
enum class ButtonSize { REGULAR, SMALL }

/** `ButtonInk.fill`: a key's face by its kind, lit when `on`, darker while pressed. */
fun buttonFill(kind: ButtonKind, on: Boolean, pressed: Boolean): Color = when {
    on -> DS.Colour.on
    kind == ButtonKind.ACCENT -> if (pressed) darker(DS.Colour.accent, 0.2f) else DS.Colour.accent
    kind == ButtonKind.DESTRUCTIVE -> if (pressed) DS.Colour.destructivePressed else DS.Colour.destructive
    else -> if (pressed) DS.Colour.raisedPressed else DS.Colour.raised
}

/** `ButtonInk.ink`. */
fun buttonInk(kind: ButtonKind, on: Boolean): Tone = when {
    on -> Tone.ON_LIGHT
    kind == ButtonKind.PLAIN -> Tone.PRIMARY
    else -> Tone.ON_FILL
}

/**
 * The desktop's `Button`: a flat rounded key holding a word, a glyph or both; `on` lights it as a
 * held toggle, and its lamp, when shown, says whether what it names is in use.
 */
@Composable
fun Button(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    help: String = text,
    kind: ButtonKind = ButtonKind.PLAIN,
    size: ButtonSize = ButtonSize.REGULAR,
    on: Boolean = false,
    enabled: Boolean = true,
    lamp: Boolean? = null,
    clicked: () -> Unit,
) {
    val small = size == ButtonSize.SMALL
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    val fill by animateColorAsState(buttonFill(kind, on, enabled && pressed), tween(DS.Motion.quickMs.toInt()), label = "fill")
    val height = if (small) DS.ButtonStyle.smallHeight else DS.ButtonStyle.height
    val pad = if (small) DS.ButtonStyle.smallPadding else DS.ButtonStyle.padding
    val glyph = if (small) DS.ButtonStyle.smallIcon else DS.ButtonStyle.icon
    val tone = buttonInk(kind, on)
    Box(
        modifier
            .height(height)
            .defaultMinSize(minWidth = height)
            .alpha(if (enabled) 1f else DS.State.disabled)
            .clip(RoundedCornerShape(if (small) DS.ButtonStyle.smallRadius else DS.ButtonStyle.radius))
            .background(fill)
            .clickable(touch, null, enabled = enabled, role = Role.Button, onClick = clicked)
            .semantics { contentDescription = help },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.padding(horizontal = if (text.isEmpty()) 0.dp else pad),
            horizontalArrangement = Arrangement.spacedBy(DS.Space.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, Modifier.size(glyph), ink(tone))
            if (text.isNotEmpty()) Label(text, role = TextRole.BUTTON, tone = tone, tracking = DS.ButtonStyle.tracking)
        }
        if (lamp != null) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = pad / 2)
                    .size(DS.ButtonStyle.lamp)
                    .background(if (lamp) DS.Colour.confirm else DS.Colour.track, CircleShape),
            )
        }
    }
}

/** A square key holding one glyph; its `help` is its name. */
@Composable
fun IconButton(icon: ImageVector, help: String, modifier: Modifier = Modifier, enabled: Boolean = true, clicked: () -> Unit) {
    Button("", modifier.width(DS.ButtonStyle.height), icon = icon, help = help, enabled = enabled, clicked = clicked)
}

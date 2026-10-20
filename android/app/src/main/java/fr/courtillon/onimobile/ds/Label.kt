package fr.courtillon.onimobile.ds

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/** What a word is: its face and size. */
enum class TextRole { LABEL, VALUE, CAPTION, TITLE, SECTION, HEADING, BUTTON }

/** Its ink. */
enum class Tone { PRIMARY, SECONDARY, TERTIARY, ON_FILL, ON_LIGHT, ON_WARNING }

fun ink(tone: Tone): Color = when (tone) {
    Tone.PRIMARY -> DS.Colour.ink
    Tone.SECONDARY -> DS.Colour.inkSecondary
    Tone.TERTIARY -> DS.Colour.inkTertiary
    Tone.ON_FILL -> DS.Colour.inkOnFill
    Tone.ON_LIGHT -> DS.Colour.inkOnLight
    Tone.ON_WARNING -> DS.Colour.inkOnWarning
}

fun textStyle(role: TextRole, tone: Tone = Tone.PRIMARY, centred: Boolean = false): TextStyle = TextStyle(
    color = ink(tone),
    fontFamily = if (role == TextRole.HEADING) DS.Type.brandFamily else DS.Type.family,
    fontWeight = if (role == TextRole.BUTTON) DS.Type.strong else DS.Type.regular,
    fontSize = when (role) {
        TextRole.VALUE -> DS.Type.value
        TextRole.CAPTION, TextRole.BUTTON -> DS.Type.chip
        TextRole.TITLE -> DS.Type.title
        TextRole.SECTION -> DS.Type.section
        TextRole.HEADING -> DS.Type.heading
        TextRole.LABEL -> DS.Type.label
    },
    textAlign = if (centred) TextAlign.Center else TextAlign.Start,
)

/** The desktop's `Label`: every word on screen, its role setting the face and size, its tone the ink. */
@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    role: TextRole = TextRole.LABEL,
    tone: Tone = Tone.PRIMARY,
    centred: Boolean = false,
    wrap: Boolean = false,
    tracking: TextUnit = TextUnit.Unspecified,
) {
    BasicText(
        text,
        modifier,
        style = textStyle(role, tone, centred).copy(letterSpacing = tracking),
        maxLines = if (wrap) Int.MAX_VALUE else 1,
        softWrap = wrap,
        overflow = TextOverflow.Ellipsis,
    )
}

/** `Note`: a quiet line under a control, wrapped. */
@Composable
fun Note(text: String, modifier: Modifier = Modifier) {
    Label(text, modifier, role = TextRole.VALUE, tone = Tone.TERTIARY, wrap = true)
}

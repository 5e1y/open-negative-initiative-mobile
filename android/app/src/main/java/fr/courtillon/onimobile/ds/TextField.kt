package fr.courtillon.onimobile.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization

/** `DSTextField`: the field on the canvas's dark, its edge lit while it holds the focus. */
@Composable
fun TextField(value: String, placeholder: String, modifier: Modifier = Modifier, edited: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(DS.FieldStyle.radius)
    BasicTextField(
        value = value,
        onValueChange = edited,
        singleLine = true,
        textStyle = textStyle(TextRole.LABEL),
        cursorBrush = SolidColor(DS.Colour.accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
        modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        decorationBox = { field ->
            Box(
                Modifier
                    .height(DS.FieldStyle.height)
                    .background(DS.Colour.canvas, shape)
                    .border(
                        if (focused) DS.Stroke.line else DS.Stroke.hairline,
                        if (focused) DS.Colour.accent else DS.Colour.separator,
                        shape,
                    )
                    .padding(horizontal = DS.FieldStyle.padding),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && !focused) Label(placeholder, tone = Tone.TERTIARY)
                field()
            }
        },
    )
}

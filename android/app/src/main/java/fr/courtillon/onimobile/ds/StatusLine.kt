package fr.courtillon.onimobile.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** `DSStatusBar`: one quiet line, after the lamp the desktop's keys carry, lit while all is well. */
@Composable
fun StatusLine(text: String, lit: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier.heightIn(min = DS.StatusStyle.height),
        horizontalArrangement = Arrangement.spacedBy(DS.Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(DS.ButtonStyle.lamp).background(if (lit) DS.Colour.confirm else DS.Colour.track, CircleShape))
        Label(text, role = TextRole.VALUE, tone = Tone.SECONDARY)
    }
}

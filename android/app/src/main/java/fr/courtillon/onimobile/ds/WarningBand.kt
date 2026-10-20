package fr.courtillon.onimobile.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** `DSWarning`: a triangle, then the sentence under it, on a pale orange band; keys may follow. */
@Composable
fun WarningBand(text: String, modifier: Modifier = Modifier, keys: @Composable ColumnScope.() -> Unit = {}) {
    Column(
        modifier
            .fillMaxWidth()
            .background(DS.Colour.warningSoft, RoundedCornerShape(DS.Radius.badge))
            .padding(DS.Space.s),
        verticalArrangement = Arrangement.spacedBy(DS.Space.xs),
    ) {
        Icon(Icons.warning, Modifier.size(DS.ButtonStyle.icon), DS.Colour.inkOnWarning)
        Label(text, tone = Tone.ON_WARNING, wrap = true)
        keys()
    }
}

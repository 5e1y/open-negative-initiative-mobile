package fr.courtillon.onimobile.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * `KeycapPlate`: the keys of one gesture family flush on one socket, a hairline showing between them;
 * a key given `Modifier.weight(1f)` shares the plate's width, as a filling plate's worded keys do.
 */
@Composable
fun KeyPlate(modifier: Modifier = Modifier, keys: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .background(DS.ButtonStyle.socket, RoundedCornerShape(DS.ButtonStyle.radius + DS.ButtonStyle.plateInset))
            .padding(DS.ButtonStyle.plateInset),
        horizontalArrangement = Arrangement.spacedBy(DS.ButtonStyle.plateInset),
        content = keys,
    )
}

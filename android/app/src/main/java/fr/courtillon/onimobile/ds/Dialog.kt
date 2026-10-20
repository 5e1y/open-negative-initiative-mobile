package fr.courtillon.onimobile.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog as Window

/**
 * `DSDialogPanel`: a question on the panel's ground in a rounded card, then its keys on one plate at
 * the card's trailing edge, the default last.
 */
@Composable
fun Dialog(question: String, detail: String, dismissed: () -> Unit, keys: @Composable RowScope.() -> Unit) {
    Window(onDismissRequest = dismissed) {
        val shape = RoundedCornerShape(DS.SheetStyle.radius)
        Column(
            Modifier
                .fillMaxWidth()
                .background(DS.Colour.ground, shape)
                .border(DS.Stroke.hairline, DS.Colour.separator, shape)
                .padding(DS.SheetStyle.padding),
            verticalArrangement = Arrangement.spacedBy(DS.SheetStyle.gap),
        ) {
            Label(question, role = TextRole.SECTION, wrap = true)
            Label(detail, tone = Tone.SECONDARY, wrap = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                KeyPlate(keys = keys)
            }
        }
    }
}

package fr.courtillon.onimobile.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A rule across the panel, between two blocks. */
@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(DS.Stroke.line).background(DS.Colour.separator))
}

/** `DSSection`: a rule above the block, then its title, then its controls at the section's gap. */
@Composable
fun Section(
    title: String,
    modifier: Modifier = Modifier,
    separated: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (separated) Divider()
        Column(
            Modifier.padding(
                start = DS.SectionStyle.inset,
                end = DS.SectionStyle.inset,
                top = DS.SectionStyle.before,
                bottom = DS.SectionStyle.after,
            ),
            verticalArrangement = Arrangement.spacedBy(DS.SectionStyle.gap),
        ) {
            if (title.isNotEmpty()) Label(title, role = TextRole.SECTION)
            content()
        }
    }
}

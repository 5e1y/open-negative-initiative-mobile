package fr.courtillon.onimobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import fr.courtillon.onimobile.ds.DS
import fr.courtillon.onimobile.link.PanelLayout
import fr.courtillon.onimobile.link.ScannerText
import uniffi.oni_link_ffi.Command
import uniffi.oni_link_ffi.PanelRow
import uniffi.oni_link_ffi.PanelTab

/**
 * A tab's sliders as keys, under their blocks' titles: a step either side of the readout, which the
 * desktop writes, and the readout's own key returns the slider where a double click would.
 */
@Composable
fun PanelPane(rows: List<PanelRow>, tab: PanelTab, enabled: Boolean, press: (Command) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DS.Space.md)) {
        for (block in PanelLayout.blocks(rows, tab)) {
            Column(verticalArrangement = Arrangement.spacedBy(DS.Space.sm)) {
                Text(block.title, color = DS.Palette.textPrimary, fontSize = DS.Type.section, fontWeight = DS.Type.strong)
                for (row in block.rows) SliderKeys(row, enabled, press)
            }
        }
    }
}

@Composable
private fun SliderKeys(row: PanelRow, enabled: Boolean, press: (Command) -> Unit) {
    val (minus, plus) = PanelLayout.steps.partition { it < 0 }
    Column(verticalArrangement = Arrangement.spacedBy(DS.Space.xs)) {
        Text(row.label, color = if (row.modified) DS.Palette.textPrimary else DS.Palette.textSecondary, fontSize = DS.Type.label)
        Track(row)
        Stack {
            Row(Modifier.fillMaxWidth()) {
                for (by in minus) Cap(ScannerText.legend(by), enabled) { press(Command.StepParam(row.param, by.toByte())) }
                Cap(row.value, enabled, Modifier.width(DS.Scanner.wideKeyWidth)) { press(Command.ResetParam(row.param)) }
                for (by in plus) Cap(ScannerText.legend(by), enabled) { press(Command.StepParam(row.param, by.toByte())) }
            }
        }
    }
}

/** Where the slider stands on the desktop: its track, and the knob as a mark across it. */
@Composable
private fun Track(row: PanelRow) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(DS.Space.xs)
            .background(DS.Palette.track, RoundedCornerShape(DS.Radius.track))
            .clearAndSetSemantics { contentDescription = "${row.label} ${row.value}" },
    ) {
        val mark = DS.Space.xs
        val at = (maxWidth - mark) * row.position.coerceIn(0f, 1f)
        Box(Modifier.offset(x = at).width(mark).fillMaxHeight().background(DS.Palette.lcdInk, RoundedCornerShape(DS.Radius.track)))
    }
}

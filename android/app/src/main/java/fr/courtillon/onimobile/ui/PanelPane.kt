package fr.courtillon.onimobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import fr.courtillon.onimobile.ds.Section
import fr.courtillon.onimobile.ds.Slider
import fr.courtillon.onimobile.ds.SliderHand
import fr.courtillon.onimobile.link.LinkModel
import fr.courtillon.onimobile.link.PanelLayout
import fr.courtillon.onimobile.link.SliderLooks
import uniffi.oni_link_ffi.Command
import uniffi.oni_link_ffi.PanelRow
import uniffi.oni_link_ffi.PanelTab

/** A tab's sliders under their blocks' titles, each the desktop's: its readout, its track, its rest. */
@Composable
fun PanelPane(rows: List<PanelRow>, tab: PanelTab, enabled: Boolean, link: LinkModel) {
    for (block in PanelLayout.blocks(rows, tab)) {
        Section(block.title) {
            for (row in block.rows) {
                val look = SliderLooks.look(row.param, row.label)
                key(row.param) {
                    Slider(row.label, row.value, row.position, hand(row, link), fill = look.fill, knob = look.knob, enabled = enabled)
                }
            }
        }
    }
}

private fun hand(row: PanelRow, link: LinkModel) = object : SliderHand {
    override fun slide(position: Float) = link.slide(row.param, position)
    override fun release() = link.letGo(row.param)
    override fun reset() = link.press(Command.ResetParam(row.param))
}

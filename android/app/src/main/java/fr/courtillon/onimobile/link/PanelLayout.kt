package fr.courtillon.onimobile.link

import uniffi.oni_link_ffi.Face
import uniffi.oni_link_ffi.PanelRow
import uniffi.oni_link_ffi.PanelTab

/** One block of a tab, titled as the desktop's panel titles it, with its sliders in order. */
data class Block(val title: String, val rows: List<PanelRow>)

/** What each tab holds on each face, as the desktop's panel lays it out. */
object PanelLayout {
    /** The desktop's tab order, `Crop` left to the desktop; the panel opens on Color. */
    val tabs = listOf(PanelTab.ROLL, PanelTab.COLOR, PanelTab.DETAILS)
    val faces = listOf(Face.PRO, Face.SCANNER)
    val opening = PanelTab.COLOR

    fun title(tab: PanelTab): String = when (tab) {
        PanelTab.ROLL -> "Roll"
        PanelTab.COLOR -> "Color"
        PanelTab.DETAILS -> "Details"
    }

    fun title(face: Face): String = when (face) {
        Face.PRO -> "Pro"
        Face.SCANNER -> "Scanner"
    }

    /** `labScreen`: the Scanner face shows its screen above every tab but Details. */
    fun showsScreen(face: Face, tab: PanelTab): Boolean = face == Face.SCANNER && tab != PanelTab.DETAILS

    /** `labKeypad`: the machine and the keypad stand where Pro's Color blocks stand. */
    fun showsKeypad(face: Face, tab: PanelTab): Boolean = face == Face.SCANNER && tab == PanelTab.COLOR

    /** The tab's sliders in blocks, a block being a run of rows under one title. */
    fun blocks(rows: List<PanelRow>, tab: PanelTab): List<Block> {
        val out = mutableListOf<Block>()
        for (row in rows.filter { it.tab == tab }) {
            val last = out.lastOrNull()
            if (last != null && last.title == row.block) {
                out[out.size - 1] = last.copy(rows = last.rows + row)
            } else {
                out += Block(row.block, listOf(row))
            }
        }
        return out
    }
}

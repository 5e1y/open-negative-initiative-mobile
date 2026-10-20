package fr.courtillon.onimobile.ui

import androidx.compose.runtime.Composable
import fr.courtillon.onimobile.ds.LabCell
import fr.courtillon.onimobile.ds.LabScreen
import fr.courtillon.onimobile.link.ScannerText
import fr.courtillon.onimobile.link.get
import fr.courtillon.onimobile.link.title
import uniffi.oni_link_ffi.Letter
import uniffi.oni_link_ffi.ScannerState

/** The scanner's screen as the desktop's `LabScreen` fills it: frame, machine, counts, finishing. */
@Composable
fun ScannerScreen(state: ScannerState) {
    val frame = ScannerText.frame(state.frame)
    val machine = ScannerText.machine(state.machine) + " · " + ScannerText.channel(state.channel)
    val cells = Letter.entries.map { LabCell(it.title, ScannerText.spelled(state.clicks[it])) }
    val finish = buildList {
        add("CT " + ScannerText.percent(state.contrast))
        add("VIG " + ScannerText.stops(state.vignette))
        state.chrome?.let { add("CHR " + ScannerText.percent(it)) }
    }
    val header = listOfNotNull(frame, "REMEASURE ROLL".takeIf { state.remeasure }, machine)
    val spoken = (header + cells.map { it.title + " " + it.value } + finish).joinToString(", ")
    LabScreen(frame, machine, state.remeasure, cells, finish, spoken)
}

package fr.courtillon.onimobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.courtillon.onimobile.ds.Button
import fr.courtillon.onimobile.ds.ButtonKind
import fr.courtillon.onimobile.ds.ButtonSize
import fr.courtillon.onimobile.ds.DS
import fr.courtillon.onimobile.ds.Dialog
import fr.courtillon.onimobile.ds.IconButton
import fr.courtillon.onimobile.ds.Icons
import fr.courtillon.onimobile.ds.KeyPlate
import fr.courtillon.onimobile.ds.Label
import fr.courtillon.onimobile.ds.Section
import fr.courtillon.onimobile.ds.StatusLine
import fr.courtillon.onimobile.ds.Tabs
import fr.courtillon.onimobile.ds.TextRole
import fr.courtillon.onimobile.ds.WarningBand
import fr.courtillon.onimobile.link.LinkHealth
import fr.courtillon.onimobile.link.LinkModel
import fr.courtillon.onimobile.link.LinkStatus
import fr.courtillon.onimobile.link.PanelLayout
import kotlinx.coroutines.delay
import uniffi.oni_link_ffi.Channel
import uniffi.oni_link_ffi.Clicks
import uniffi.oni_link_ffi.Command
import uniffi.oni_link_ffi.Face
import uniffi.oni_link_ffi.Machine
import uniffi.oni_link_ffi.ScannerState

/** What the screen shows before the desktop's first answer. */
private val resting = ScannerState(
    revision = 0u, applied = 0u, frame = null, machine = Machine.ONI, channel = Channel.NEGATIVE,
    clicks = Clicks(0, 0, 0, 0, 0), contrast = 0f, vignette = 0f, chrome = 0f,
    remeasure = false, canUndo = false, canRedo = false,
)

/**
 * The remote: the link's state on top, the face, the frame and history keys, then the desktop
 * panel's Roll, Color and Details tabs; the Scanner face keeps its screen and keypad where the
 * desktop does. Every control is out of reach while the desktop cannot hear it.
 */
@Composable
fun RemoteScreen(link: LinkModel) {
    var importing by rememberSaveable { mutableStateOf(false) }
    var tabIndex by rememberSaveable { mutableIntStateOf(PanelLayout.tabs.indexOf(PanelLayout.opening)) }
    if (importing) {
        ImportScreen(link) { importing = false }
        return
    }
    val state = link.state ?: resting
    val face = link.panel?.face ?: Face.PRO
    val tab = PanelLayout.tabs[tabIndex]
    val live = link.status == LinkModel.Status.CONNECTED
    val placed = live && state.frame != null
    val press: (Command) -> Unit = { link.press(it) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        LinkBar(link) { importing = true }
        Section("", separated = false) {
            // `faceBinding`: the faces, present while a photograph is worked.
            Tabs(PanelLayout.faces.map(PanelLayout::title), PanelLayout.faces.indexOf(face), enabled = live) {
                press(Command.SetFace(PanelLayout.faces[it]))
            }
            // Previous and next frame, then undo and redo, each answered by the desktop's own history.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                KeyPlate {
                    IconButton(Icons.chevronLeft, "Previous frame", enabled = placed) { press(Command.Walk(-1)) }
                    IconButton(Icons.chevronRight, "Next frame", enabled = placed) { press(Command.Walk(1)) }
                }
                KeyPlate {
                    IconButton(Icons.undo, "Undo", enabled = live && state.canUndo) { press(Command.Undo) }
                    IconButton(Icons.redo, "Redo", enabled = live && state.canRedo) { press(Command.Redo) }
                }
            }
            if (PanelLayout.showsScreen(face, tab)) ScannerScreen(state)
            Tabs(PanelLayout.tabs.map(PanelLayout::title), tabIndex) { tabIndex = it }
        }
        if (PanelLayout.showsKeypad(face, tab)) ScannerKeypad(state, live, press)
        PanelPane(link.panel?.rows.orEmpty(), tab, live, link)
    }
}

/** The link's state, said plainly: a quiet line while the desktop answers, a warning band when not. */
@Composable
private fun LinkBar(link: LinkModel, onImport: () -> Unit) {
    var forgetting by remember { mutableStateOf(false) }
    // An answer late by more than a moment is said, so a press is never taken as made.
    var waiting by remember { mutableStateOf(false) }
    LaunchedEffect(link.unanswered) {
        waiting = false
        if (link.unanswered) {
            delay(LinkStatus.PATIENCE_MS)
            waiting = true
        }
    }
    val name = link.desktop?.name ?: "the desktop"
    val line = LinkStatus.line(link.status, name, link.roundTripMs, waiting, link.problem)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = DS.SectionStyle.inset, vertical = DS.Space.s),
        verticalArrangement = Arrangement.spacedBy(DS.Space.s),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DS.Space.s)) {
            Label(link.desktop?.name ?: "Desktop", Modifier.weight(1f), role = TextRole.TITLE)
            Button("IMPORT", enabled = link.canImport, clicked = onImport)
            Button("FORGET", size = ButtonSize.SMALL, clicked = { forgetting = true })
        }
        when (line.health) {
            LinkHealth.CONNECTED, LinkHealth.WAITING -> StatusLine(line.text, lit = line.health == LinkHealth.CONNECTED)
            LinkHealth.RECONNECTING, LinkHealth.OFFLINE -> WarningBand(line.text)
        }
    }
    if (forgetting) {
        Dialog(
            "Forget ${link.desktop?.name ?: "the desktop"}?",
            "The phone will need the desktop's pairing code again.",
            dismissed = { forgetting = false },
        ) {
            Button("CANCEL") { forgetting = false }
            Button("FORGET DESKTOP", kind = ButtonKind.DESTRUCTIVE) {
                forgetting = false
                link.forget()
            }
        }
    }
}

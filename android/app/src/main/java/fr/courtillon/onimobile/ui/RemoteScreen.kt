package fr.courtillon.onimobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import fr.courtillon.onimobile.ds.DS
import fr.courtillon.onimobile.ds.Tabs
import fr.courtillon.onimobile.ds.keycap.KeycapGlyph
import fr.courtillon.onimobile.link.LinkModel
import fr.courtillon.onimobile.link.PanelLayout
import fr.courtillon.onimobile.link.ScannerText
import uniffi.oni_link_ffi.Channel
import uniffi.oni_link_ffi.Clicks
import uniffi.oni_link_ffi.Command
import uniffi.oni_link_ffi.Face
import uniffi.oni_link_ffi.Machine
import uniffi.oni_link_ffi.ScannerState
import kotlin.math.roundToInt

/** What the screen shows before the desktop's first answer. */
private val resting = ScannerState(
    revision = 0u, applied = 0u, frame = null, machine = Machine.ONI, channel = Channel.NEGATIVE,
    clicks = Clicks(0, 0, 0, 0, 0), contrast = 0f, vignette = 0f, chrome = 0f,
    remeasure = false, canUndo = false, canRedo = false,
)

/**
 * The remote: the connection on top, the face, the frame and history keys, then the desktop panel's
 * Roll, Color and Details tabs; the Scanner face keeps its screen and keypad where the desktop does.
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
    val panel = link.panel
    val face = panel?.face ?: Face.PRO
    val tab = PanelLayout.tabs[tabIndex]
    val live = link.status == LinkModel.Status.CONNECTED
    val placed = live && state.frame != null
    val press: (Command) -> Unit = { link.press(it) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DS.Space.sm, vertical = DS.Space.sm),
        verticalArrangement = Arrangement.spacedBy(DS.Space.sm),
    ) {
        Bar(link) { importing = true }
        val problem = link.problem
        if (problem != null && !live) {
            Text(problem, fontSize = DS.Type.label, color = DS.Palette.textSecondary)
        }
        // `faceBinding`: the faces, present while a photograph is worked.
        Tabs(PanelLayout.faces.map(PanelLayout::title), PanelLayout.faces.indexOf(face), enabled = live) {
            press(Command.SetFace(PanelLayout.faces[it]))
        }
        // Previous and next frame, then undo and redo, each answered by the desktop's own history.
        Stack {
            Row(Modifier.fillMaxWidth()) {
                IconCap(KeycapGlyph.CHEVRON_LEFT, "Previous frame", placed) { press(Command.Walk(-1)) }
                IconCap(KeycapGlyph.CHEVRON_RIGHT, "Next frame", placed) { press(Command.Walk(1)) }
                IconCap(KeycapGlyph.UNDO, "Undo", live && state.canUndo) { press(Command.Undo) }
                IconCap(KeycapGlyph.REDO, "Redo", live && state.canRedo) { press(Command.Redo) }
            }
        }
        if (PanelLayout.showsScreen(face, tab)) {
            ScannerScreen(state, Modifier.fillMaxWidth().height(DS.Scanner.screenHeight).clip(RoundedCornerShape(DS.Radius.card)))
        }
        Tabs(PanelLayout.tabs.map(PanelLayout::title), tabIndex) { tabIndex = it }
        if (PanelLayout.showsKeypad(face, tab)) {
            Machines(state, live, press)
            ScannerKeypad(state, live, press)
        }
        PanelPane(panel?.rows.orEmpty(), tab, live, press)
    }
}

/** `labKeypad`'s Machine: the two machines as two keys, a lamp lit on the one in use. */
@Composable
private fun Machines(state: ScannerState, enabled: Boolean, press: (Command) -> Unit) {
    Stack {
        Row(Modifier.fillMaxWidth()) {
            for (machine in listOf(Machine.ONI, Machine.FRONTIER)) {
                val lit = state.machine == machine
                Cap(
                    ScannerText.machine(machine),
                    enabled,
                    ornament = { Lamp(if (lit) DS.Palette.lampOn else DS.Palette.lampOff, Modifier.align(Alignment.CenterEnd)) },
                ) { press(Command.SetMachine(machine)) }
            }
        }
    }
}

@Composable
private fun Bar(link: LinkModel, onImport: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var forgetting by remember { mutableStateOf(false) }
    val lamp = when (link.status) {
        LinkModel.Status.CONNECTED -> DS.Palette.lampOn
        LinkModel.Status.CONNECTING -> DS.Palette.lampWait
        else -> DS.Palette.lampOff
    }
    val reading = when (link.status) {
        LinkModel.Status.CONNECTED -> link.roundTripMs?.let { "${maxOf(1, it.roundToInt())} ms" } ?: "—"
        LinkModel.Status.CONNECTING -> "CONNECTING"
        else -> "OFFLINE"
    }
    val name = link.desktop?.name ?: "Desktop"

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DS.Space.sm),
    ) {
        Lamp(lamp)
        Text(
            name, color = DS.Palette.textPrimary, fontWeight = DS.Type.strong, fontSize = DS.Type.title,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
        )
        Text(reading, color = DS.Palette.textSecondary, fontFamily = FontFamily.Monospace, fontSize = DS.Type.value)
        Box(Modifier.weight(1f))
        LoneCap("IMPORT", link.canImport, Modifier.width(DS.Scanner.wideKeyWidth), onImport)
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = DS.Palette.lcdInk)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Forget Desktop") }, onClick = {
                    menu = false
                    forgetting = true
                })
            }
        }
    }
    if (forgetting) {
        AlertDialog(
            onDismissRequest = { forgetting = false },
            title = { Text("Forget $name?") },
            text = { Text("The phone will need the desktop's pairing code again.") },
            confirmButton = {
                TextButton(onClick = {
                    forgetting = false
                    link.forget()
                }) { Text("Forget Desktop", color = DS.Palette.destructive) }
            },
            dismissButton = { TextButton(onClick = { forgetting = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun Lamp(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(DS.Scanner.lampSize).background(color, CircleShape))
}

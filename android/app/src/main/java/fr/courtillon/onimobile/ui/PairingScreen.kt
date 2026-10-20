package fr.courtillon.onimobile.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import fr.courtillon.onimobile.ds.Button
import fr.courtillon.onimobile.ds.DS
import fr.courtillon.onimobile.ds.Ground
import fr.courtillon.onimobile.ds.KeyPlate
import fr.courtillon.onimobile.ds.Label
import fr.courtillon.onimobile.ds.Progress
import fr.courtillon.onimobile.ds.Section
import fr.courtillon.onimobile.ds.Surface
import fr.courtillon.onimobile.ds.TextField
import fr.courtillon.onimobile.ds.TextRole
import fr.courtillon.onimobile.ds.Tone
import fr.courtillon.onimobile.ds.WarningBand
import fr.courtillon.onimobile.link.LinkModel
import kotlinx.coroutines.launch

/** The first meeting: the desktop's QR code through the camera, or its link pasted. */
@Composable
fun PairingScreen(link: LinkModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var pasted by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    // Bumped after a failed attempt so a fresh scanner reads the same code again.
    var attempt by remember { mutableIntStateOf(0) }
    var camera by remember {
        mutableStateOf(
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Camera.ALLOWED
            } else {
                Camera.UNASKED
            },
        )
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        camera = if (granted) Camera.ALLOWED else Camera.REFUSED
    }
    LaunchedEffect(Unit) { if (camera == Camera.UNASKED) ask.launch(Manifest.permission.CAMERA) }

    fun pair(uri: String) {
        if (busy) return
        busy = true
        scope.launch {
            if (!link.pair(uri)) attempt += 1
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Section("", separated = false) {
            Label("ONI Mobile", role = TextRole.HEADING)
            Label(
                "Show ONI's pairing code on the desktop and hold the phone up to it. Both must be on the same network.",
                tone = Tone.SECONDARY,
                wrap = true,
            )
        }
        Section("Pairing Code") {
            Surface(Modifier.fillMaxWidth().aspectRatio(1f), ground = Ground.WELL, rounded = true) {
                when (camera) {
                    Camera.ALLOWED -> key(attempt) { QrScanner(Modifier.fillMaxSize()) { pair(it) } }
                    Camera.REFUSED -> Label(
                        "The camera is off for ONI Mobile. Allow it in Settings, or paste the pairing link below.",
                        Modifier.padding(DS.Space.m),
                        tone = Tone.SECONDARY,
                        centred = true,
                        wrap = true,
                    )
                    Camera.UNASKED -> Unit
                }
            }
            if (busy) Progress(null)
            link.problem?.let { WarningBand(it) }
        }
        Section("Pairing Link") {
            TextField(pasted, "oni-link://pair?…") { pasted = it }
            KeyPlate(Modifier.fillMaxWidth()) {
                Button("PASTE", Modifier.weight(1f)) { pasted = clipboard.getText()?.text.orEmpty() }
                Button("PAIR", Modifier.weight(1f), enabled = !busy && pasted.isNotBlank()) { pair(pasted) }
            }
        }
    }
}

private enum class Camera { UNASKED, ALLOWED, REFUSED }

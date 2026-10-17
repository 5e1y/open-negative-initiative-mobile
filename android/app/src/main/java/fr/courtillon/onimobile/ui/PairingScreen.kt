package fr.courtillon.onimobile.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import fr.courtillon.onimobile.ds.DS
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

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(DS.Space.md),
        verticalArrangement = Arrangement.spacedBy(DS.Space.md),
    ) {
        Text("ONI Mobile", color = DS.Palette.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Show ONI's pairing code on the desktop and hold the phone up to it. Both must be on the same network.",
            color = DS.Palette.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(DS.Palette.lcdGlass),
            contentAlignment = Alignment.Center,
        ) {
            when (camera) {
                Camera.ALLOWED -> key(attempt) { QrScanner(Modifier.fillMaxSize()) { pair(it) } }
                Camera.REFUSED -> Text(
                    "The camera is off for ONI Mobile. Allow it in Settings, or paste the pairing link below.",
                    color = DS.Palette.textSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(DS.Space.md),
                )
                Camera.UNASKED -> Unit
            }
            if (busy) CircularProgressIndicator(color = DS.Palette.lcdInk)
        }
        link.problem?.let { Text(it, color = DS.Palette.lampWait, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.padding(top = DS.Space.sm))
        BasicTextField(
            value = pasted,
            onValueChange = { pasted = it },
            singleLine = true,
            textStyle = TextStyle(color = DS.Palette.lcdInk, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            cursorBrush = SolidColor(DS.Palette.lcdInk),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { field ->
                Box(Modifier.background(DS.Palette.lcdGlass, RoundedCornerShape(8.dp)).padding(DS.Space.sm)) {
                    if (pasted.isEmpty()) {
                        Text(
                            "oni-link://pair?…", color = DS.Palette.textSecondary,
                            fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                        )
                    }
                    field()
                }
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DS.Space.sm)) {
            LoneCap("PASTE", true, Modifier.weight(1f)) { pasted = clipboard.getText()?.text.orEmpty() }
            LoneCap("PAIR", !busy && pasted.isNotBlank(), Modifier.weight(1f)) { pair(pasted) }
        }
    }
}

private enum class Camera { UNASKED, ALLOWED, REFUSED }

package fr.courtillon.onimobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import fr.courtillon.onimobile.ds.Surface
import fr.courtillon.onimobile.link.LinkModel
import fr.courtillon.onimobile.ui.PairingScreen
import fr.courtillon.onimobile.ui.RemoteScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val link: LinkModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        // Dials on every return to the screen; hangs up once the app is no longer seen.
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = link.resume()
            override fun onStop(owner: LifecycleOwner) = link.suspend()
        })
        if (savedInstanceState == null) pairFrom(intent)

        setContent {
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    if (link.desktop == null) PairingScreen(link) else RemoteScreen(link)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pairFrom(intent)
    }

    /** A pairing code read by the phone's own camera arrives as a link that opens the app. */
    private fun pairFrom(intent: Intent?) {
        val uri = intent?.data?.takeIf { it.scheme == "oni-link" }?.toString() ?: return
        lifecycleScope.launch { link.pair(uri) }
    }
}

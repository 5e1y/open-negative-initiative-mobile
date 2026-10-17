package fr.courtillon.onimobile.ds

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.courtillon.onimobile.ds.keycap.Rgba

/** The desktop app's tokens the remote draws with, copied from its `DSTokens`, which stays authoritative. */
object DS {
    object Space {
        val xxs = 2.dp
        val xs = 4.dp
        val sm = 8.dp
        val md = 16.dp
    }

    object Palette {
        val lcdGlass = Color(0.05f, 0.05f, 0.05f)
        val lcdInk = Color(0.93f, 0.93f, 0.93f)
        val canvas = Color(0xFF1B1B1B)
        val textOnFill = Rgba(1f, 1f, 1f)
        val textOnLight = Rgba(0.12f, 0.12f, 0.12f)
        /** An albedo, lit and shaded by the keycap chrome, so it reads darker than its own value. */
        val keycap = Rgba(0x31 / 255f, 0x31 / 255f, 0x31 / 255f)
        val textPrimary = Color.White
        val textSecondary = Color.White.copy(alpha = 0.55f)
        val lampOn = Color(0.36f, 0.86f, 0.47f)
        val lampWait = Color(0.95f, 0.64f, 0.23f)
        val lampOff = Color(0.32f, 0.32f, 0.32f)
    }

    object Scanner {
        val bandCool = Rgba(0.23f, 0.55f, 0.86f)
        val bandWarm = Rgba(0.95f, 0.64f, 0.23f)
        val bandGreen = Rgba(0.18f, 0.64f, 0.48f)
        val bandMagenta = Rgba(0.84f, 0.20f, 0.50f)
        val bandRed = Rgba(0.84f, 0.25f, 0.22f)
        val bandCyan = Rgba(0.12f, 0.66f, 0.78f)
        val bandBlue = Rgba(0.22f, 0.36f, 0.84f)
        val bandYellow = Rgba(0.93f, 0.78f, 0.18f)
        val bandDark = Rgba(0.28f, 0.28f, 0.30f)
        val bandLight = Rgba(0.86f, 0.86f, 0.84f)

        val wideKeyWidth = 96.dp
        /** Under the desktop's 46: eight rows, the screen and the bar fit a phone without scrolling. */
        val keyHeight = 44.dp
        const val KEY_LEGEND_SIZE = 13f
        val keyPaddingH = 8.dp
        const val SCREEN_VALUE_SIZE = 30f
        const val SCREEN_LABEL_SIZE = 12f
        val scanlinePitch = 3.dp
        val phosphorGlow = 5.dp
        val lampSize = 8.dp
    }
}

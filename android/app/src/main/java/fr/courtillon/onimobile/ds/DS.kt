package fr.courtillon.onimobile.ds

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fr.courtillon.onimobile.ds.keycap.Rgba

/**
 * What each token is for, as the desktop's `ui/ds/theme.slint` binds it: every value is a generated
 * desktop token from `Tokens`, so a change of look on the desktop is one regeneration here.
 * Screens read `DS`, never `Tokens`.
 */
object DS {
    object Space {
        val xxs = Tokens.Space.xxs
        val xs = Tokens.Space.xs
        val sm = Tokens.Space.sm
        val md = Tokens.Space.md
        val lg = Tokens.Space.lg
    }

    /** The four radii `DSShape.rounded` allows, by what they round. */
    object Radius {
        val track = Tokens.Radius.track
        val badge = Tokens.Radius.badge
        val card = Tokens.Radius.card
        val control = Tokens.Radius.control
    }

    object Stroke {
        val hairline = Tokens.Stroke.hairline
        val line = Tokens.Stroke.line
        val emphasis = Tokens.Stroke.emphasis
    }

    /** Sizes in points on the desktop, scaled points here so the phone's text size setting holds. */
    object Type {
        val label = Tokens.Typo.labelSize.value.sp
        val value = Tokens.Typo.valueSize.value.sp
        val section = Tokens.Typo.sectionSize.value.sp
        val title = Tokens.Typo.titleSize.value.sp
        val heading = Tokens.Typo.headingSize.value.sp
        val strong = FontWeight.SemiBold
    }

    /** Opacities of a state, never of a colour. */
    object State {
        const val disabled = Tokens.State.missing
        const val idle = Tokens.State.idleLabel
    }

    object Motion {
        const val quickMs = Tokens.Motion.curtainDurationMs
    }

    object Palette {
        val canvas = Tokens.Palette.canvas
        val ground = Tokens.Palette.panel
        val raised = Tokens.Palette.keycap
        val lcdGlass = Tokens.Palette.lcdGlass
        val lcdInk = Tokens.Palette.lcdInk
        val textOnFill = Tokens.Palette.textOnFill.rgba
        val textOnLight = Tokens.Palette.textOnLight.rgba
        /** An albedo, lit and shaded by the keycap chrome, so it reads darker than its own value. */
        val keycap = Tokens.Palette.keycap.rgba
        val textPrimary = Tokens.System.primary
        val textSecondary = Tokens.Palette.textSecondary
        val track = Tokens.Palette.track
        val trackFill = Tokens.Palette.trackFill
        val separator = Tokens.Palette.separator
        val destructive = Tokens.Tint.destructive
        /** The desktop's button lamp: confirm when lit, the track's grey when not. */
        val lampOn = Tokens.Tint.confirm
        val lampWait = Tokens.Palette.warning
        val lampOff = Tokens.Palette.track
    }

    object Icon {
        val size = Tokens.Control.iconSize
    }

    /** `TabsStyle`: the segmented tabs, a raised pill under the selection. */
    object Tabs {
        val height = Tokens.Control.fieldHeight
        val radius = Radius.badge
        val inset = Space.xxs
        val pillRadius = Radius.track
    }

    /** `SliderStyle`'s track, which the phone draws to show where a slider stands. */
    object Slider {
        val track = Tokens.Control.trackHeight
        val knob = Tokens.Control.handleSize
    }

    /** `ScannerStyle`. */
    object Scanner {
        val bandCool = Tokens.Scanner.bandCool.rgba
        val bandWarm = Tokens.Scanner.bandWarm.rgba
        val bandGreen = Tokens.Scanner.bandGreen.rgba
        val bandMagenta = Tokens.Scanner.bandMagenta.rgba
        val bandRed = Tokens.Scanner.bandRed.rgba
        val bandCyan = Tokens.Scanner.bandCyan.rgba
        val bandBlue = Tokens.Scanner.bandBlue.rgba
        val bandYellow = Tokens.Scanner.bandYellow.rgba
        val bandDark = Tokens.Scanner.bandDark.rgba
        val bandLight = Tokens.Scanner.bandLight.rgba

        val wideKeyWidth = Tokens.Scanner.wideKeyWidth
        val keyHeight = Tokens.Scanner.keyHeight
        val keyLegendSize = Tokens.Scanner.keyLegendSize.value
        val keyPaddingH = Tokens.Scanner.keyPaddingH
        val screenHeight = Tokens.Control.mainHistogramHeight
        val screenValueSize = Tokens.Scanner.screenValueSize.value
        val screenLabelSize = Tokens.Scanner.screenLabelSize.value
        val scanlinePitch = Tokens.Scanner.scanlinePitch
        /** `ScannerStyle.scanline`, a theme value no Swift token holds: black at 35 %. */
        val scanline = Color(0x59000000)
        val phosphorGlow = Tokens.Scanner.phosphorGlow
        val lampSize = Tokens.Control.lampSize
        val faceBar = Tokens.Control.modeBarWidth
    }
}

/** A token colour as the keycap's albedo. */
val Color.rgba: Rgba get() = Rgba(red, green, blue, alpha)

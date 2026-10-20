package fr.courtillon.onimobile.ds

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.courtillon.onimobile.R

/**
 * The desktop's `ui/ds/theme.slint`, group for group: what each value is for, bound to the generated
 * `Tokens`, so a change of look on the desktop is one regeneration here. Screens read `DS` only
 * through the components beside this file.
 */
object DS {
    /** `Colour`: surfaces from back to front, then inks, then the meaning colours. */
    object Colour {
        val ground = Tokens.Palette.panel
        val canvas = Tokens.Palette.canvas
        val well = Tokens.Palette.tile
        val raised = Tokens.Palette.keycap
        val raisedHover = brighter(Tokens.Palette.keycap, 0.25f)
        val raisedPressed = darker(Tokens.Palette.keycap, 0.25f)
        val ink = Tokens.System.primary
        val inkSecondary = Tokens.Palette.textSecondary
        val inkTertiary = Tokens.Palette.textTertiary
        val inkOnFill = Tokens.Palette.textOnFill
        val inkOnLight = Tokens.Palette.textOnLight
        val inkOnWarning = Tokens.Palette.textOnWarning
        val warningSoft = Tokens.Palette.warningSoft
        val accent = Tokens.Palette.accent
        val on = Tokens.System.primary
        val destructive = Tokens.Tint.destructive
        val destructivePressed = darker(Tokens.Tint.destructive, 0.2f)
        val confirm = Tokens.Tint.confirm
        val separator = Tokens.Palette.separator
        val selected = Tokens.Palette.selected
        val track = Tokens.Palette.track
        val trackFill = Tokens.Palette.trackFill
        val skeleton = Tokens.Palette.skeleton
        val white = Tokens.System.white
        val black = Tokens.System.black
        val metalDark = Tokens.Scanner.metalDark
        val metalLight = Tokens.Scanner.metalLight

        // `GainChannel`'s hues and complements: the sensor's own channels, no Swift token holds them.
        val channelRed = Color(0xFFFF453A)
        val channelCyan = Color(0xFF64D2FF)
        val channelBlue = Color(0xFF0A84FF)
        val channelYellow = Color(0xFFFFD60A)
        val channelGreen = Color(0xFF30D158)
        val channelMagenta = Color(0xFFFF00FF)
    }

    object Space {
        val xxs = Tokens.Space.xxs
        val xs = Tokens.Space.xs
        val s = Tokens.Space.sm
        val m = Tokens.Space.md
        val l = Tokens.Space.lg
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

    /** Points on the desktop, scaled points here, so the phone's text size setting holds. */
    @OptIn(ExperimentalTextApi::class)
    object Type {
        val family = FontFamily(
            Font(R.font.inter, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
            Font(R.font.inter, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
            Font(R.font.inter, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
            Font(R.font.inter, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
        )
        val brandFamily = FontFamily(Font(R.font.enriqueta))
        val regular = FontWeight(Tokens.Typo.labelWeight)
        val strong = FontWeight.SemiBold
        val label = Tokens.Typo.labelSize.value.sp
        val value = Tokens.Typo.valueSize.value.sp
        val chip = Tokens.Typo.chipSize.value.sp
        val title = Tokens.Typo.titleSize.value.sp
        val section = Tokens.Typo.sectionSize.value.sp
        val heading = Tokens.Typo.boxTitleSize.value.sp
    }

    /** Opacities of a state, never of a colour. */
    object State {
        const val disabled = Tokens.State.missing
        const val idle = Tokens.State.idleLabel
    }

    object Motion {
        const val quickMs = Tokens.Motion.curtainDurationMs
        const val slideMs = Tokens.Motion.revealResponse * 1000f
        const val sweepMs = Tokens.Motion.sweepDurationMs
    }

    object ButtonStyle {
        val height = Tokens.Control.fieldHeight
        val smallHeight = Tokens.Control.iconButtonSize
        val radius = Radius.badge
        val smallRadius = Radius.track
        val padding = Space.m
        val smallPadding = Space.s
        val groupGap = Space.xxs
        /** `KeycapPlate`'s socket: the keys sit flush on it, a hairline showing between them. */
        val socket = Tokens.Palette.tile
        val plateInset = Stroke.hairline
        val icon = Tokens.Control.iconSize
        val smallIcon = Tokens.Control.toggleGlyph
        val label = Type.chip
        val weight = Type.strong
        val tracking = Stroke.hairline.value.sp
        val lamp = Tokens.Control.lampSize
    }

    object SliderStyle {
        val row = Tokens.Control.handleLength
        val track = Tokens.Control.trackHeight
        val trackRadius = Radius.track
        val knobWidth = Tokens.Control.handleThickness
        val knobHeight = Tokens.Control.handleLength
        val knobRadius = Radius.badge
        const val knobGrow = Tokens.Control.knobGrow
        val grip = Tokens.Control.knobLine.dp
        val gripGap = Tokens.Control.knobLineGap.dp
        val knob = Colour.raisedHover
        val knobEdge = Tokens.Palette.trackLine
        val gripInk = Colour.inkSecondary
        const val rampStart = 0.45f
        const val rampEnd = 0.9f
        val labelGap = Space.xs
    }

    object SectionStyle {
        val inset = Space.m
        val before = Space.l
        val after = Space.l
        val gap = Space.s
    }

    object TabsStyle {
        val height = ButtonStyle.height
        val radius = ButtonStyle.radius
        val inset = Space.xxs
        val pillRadius = Radius.track
    }

    object FieldStyle {
        val height = Tokens.Control.fieldHeight
        val radius = ButtonStyle.radius
        val padding = Space.s
    }

    object SheetStyle {
        val radius = Radius.card
        val padding = Space.l
        val gap = Space.m
    }

    object ProgressStyle {
        val height = Tokens.Control.trackHeight
        const val segment = Tokens.Control.progressSegment
    }

    object StatusStyle {
        val height = Tokens.Control.statusRowHeight
    }

    /** `ScannerStyle`: the Lab keypad's keys and bands, the scanner's screen. */
    object ScannerStyle {
        val keyHeight = Tokens.Scanner.keyHeight
        val wideKey = Tokens.Scanner.wideKeyWidth
        val legend = Tokens.Scanner.keyLegendSize.value.sp
        val keyPadding = Tokens.Scanner.keyPaddingH
        val keyRadius = Radius.track
        val keyGap = Space.xxs
        /** `KeycapStyle.minHold`: the shortest time a cap stays down, so a tap is seen. */
        const val keyHoldMs = 140L
        val socketRadius = Radius.badge
        val socket = Tokens.Palette.tile
        val capLight = Tokens.Palette.keycap
        val bandDark = Tokens.Scanner.bandDark
        val bandLight = Tokens.Scanner.bandLight
        val bandRed = Tokens.Scanner.bandRed
        val bandCyan = Tokens.Scanner.bandCyan
        val bandGreen = Tokens.Scanner.bandGreen
        val bandMagenta = Tokens.Scanner.bandMagenta
        val bandBlue = Tokens.Scanner.bandBlue
        val bandYellow = Tokens.Scanner.bandYellow
        val bandCool = Tokens.Scanner.bandCool
        val bandWarm = Tokens.Scanner.bandWarm
        /** `LabKeyboard.band`: a fill this light in Rec. 709 luma takes the dark ink. */
        const val lightLuma = 0.55f
        val glass = Tokens.Palette.lcdGlass
        val lcdInk = Tokens.Palette.lcdInk
        val screen = Tokens.Control.mainHistogramHeight
        val screenLabel = Tokens.Scanner.screenLabelSize.value.sp
        val screenValue = Tokens.Scanner.screenValueSize.value.sp
        val scanlinePitch = Tokens.Scanner.scanlinePitch
        /** `LabScreen`'s scanlines: black at 35 %. */
        val scanline = Color(0x59000000)
        val valueWeight = FontWeight.Bold
        val labelWeight = FontWeight.Medium
    }
}

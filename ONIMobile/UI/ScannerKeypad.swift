import SwiftUI
import UIKit

/// The scanner's keypad as the desktop draws it: per letter −3, −1, the letter (its reset), +1, +3.
struct ScannerKeypad: View {
    let state: ScannerState
    let press: (Command) -> Void

    var body: some View {
        VStack(spacing: DS.Space.sm) {
            KeyStack { letter(.d) }
            KeyStack { ForEach([Letter.c, .m, .y], id: \.self) { letter($0) } }
            KeyStack { letter(.w) }
            KeyStack {
                effect(.contrast, "CONTRAST", minus: DS.Scanner.bandDark, plus: DS.Scanner.bandLight)
                effect(.vignette, "VIGNETTE", minus: DS.Scanner.bandLight, plus: DS.Scanner.bandDark)
                if state.channel != .monochrome {
                    effect(.chrome, "CHROME", minus: DS.Scanner.bandDark, plus: DS.Scanner.bandLight)
                }
            }
        }
        // One texture for the keys: every cap carries two blurs and a shadow.
        .drawingGroup()
    }

    private func letter(_ letter: Letter) -> some View {
        let sides = Self.sides(letter)
        return HStack(spacing: 0) {
            ForEach([Int8(-3), -1], id: \.self) { by in
                Self.band(ScannerText.legend(by), fill: sides.minus.fill) { press(.step(letter: letter, by: by)) }
                    .accessibilityHint("\(letter.title) toward \(sides.minus.word)")
            }
            Self.cap(letter.title, width: DS.Scanner.wideKeyWidth) { press(.reset(letter: letter)) }
                .accessibilityHint("\(letter.title) back to zero")
            ForEach([Int8(1), 3], id: \.self) { by in
                Self.band(ScannerText.legend(by), fill: sides.plus.fill) { press(.step(letter: letter, by: by)) }
                    .accessibilityHint("\(letter.title) toward \(sides.plus.word)")
            }
        }
    }

    private func effect(_ effect: Effect, _ title: String, minus: KeycapStyle.RGBA, plus: KeycapStyle.RGBA) -> some View {
        HStack(spacing: 0) {
            ForEach([Int8(-3), -1], id: \.self) { by in
                Self.band(ScannerText.legend(by), fill: minus) { press(.stepEffect(effect: effect, by: by)) }
            }
            Self.cap(title, width: DS.Scanner.wideKeyWidth) { press(.resetEffect(effect: effect)) }
                .accessibilityHint("\(title.capitalized) back to rest")
            ForEach([Int8(1), 3], id: \.self) { by in
                Self.band(ScannerText.legend(by), fill: plus) { press(.stepEffect(effect: effect, by: by)) }
            }
        }
    }

    static func sides(_ letter: Letter) -> (minus: (word: String, fill: KeycapStyle.RGBA),
                                            plus: (word: String, fill: KeycapStyle.RGBA)) {
        switch letter {
        case .d: (("dark", DS.Scanner.bandDark), ("light", DS.Scanner.bandLight))
        case .c: (("red", DS.Scanner.bandRed), ("cyan", DS.Scanner.bandCyan))
        case .m: (("green", DS.Scanner.bandGreen), ("magenta", DS.Scanner.bandMagenta))
        case .y: (("blue", DS.Scanner.bandBlue), ("yellow", DS.Scanner.bandYellow))
        case .w: (("cool", DS.Scanner.bandCool), ("warm", DS.Scanner.bandWarm))
        }
    }

    // MARK: Caps

    /// A grey key: the letters, the effects and every key that is not a step.
    static func cap(_ title: String, width: CGFloat? = nil, action: @escaping () -> Void) -> Keycap {
        styled(Keycap(title, action: action), albedo: .init(DS.Palette.keycap), width: width)
            .keycapTextColor(.init(DS.Palette.textOnFill))
            .keycapHaptic(.light)
    }

    /// A key coloured whole by its side, inked dark or light by the colour's luminance.
    static func band(_ legend: String, fill: KeycapStyle.RGBA, action: @escaping () -> Void) -> Keycap {
        let light = 0.2126 * fill.r + 0.7152 * fill.g + 0.0722 * fill.b > 0.55
        let ink = light ? DS.Palette.textOnLight : DS.Palette.textOnFill
        return styled(Keycap(legend, action: action), albedo: fill, width: nil)
            .keycapTextColor(.init(ink))
            .keycapHaptic(.rigid)
    }

    static func icon(_ glyph: KeycapIcon.Glyph, name: String, action: @escaping () -> Void) -> Keycap {
        styled(Keycap(icon: glyph, name: name, action: action), albedo: .init(DS.Palette.keycap), width: nil)
            .keycapTextColor(.init(DS.Palette.textOnFill))
            .keycapHaptic(.light)
    }

    /// Every key fills its share so plates align; `width` pins a key beside others of free width.
    static func styled(_ key: Keycap, albedo: KeycapStyle.RGBA, width: CGFloat?) -> Keycap {
        key.keycapVariant(.small)
            .keycapTextSize(DS.Scanner.keyLegendSize)
            .keycapPaddingH(DS.Scanner.keyPaddingH)
            .keycapSize(width: width.map(Double.init), height: Double(DS.Scanner.keyHeight))
            .keycapFills(width == nil)
            .keycapColor(albedo)
    }
}

/// Rows of keys flush on one socket plate, as the desktop's `LabStack`.
struct KeyStack<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        let style = Keycap.Variant.small.applied(to: KeycapStyle.hardened)
        VStack(spacing: 0) { content }
            .background {
                DSShape.rounded(CGFloat(style.cornerRadius + style.contourWidth))
                    .fill(style.socketColor.color)
            }
            .fixedSize(horizontal: false, vertical: true)
    }
}

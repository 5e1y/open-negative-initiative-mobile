import SwiftUI
import UIKit

/// A physical-looking key button whose settable surface is deliberately narrow: every other dial stays frozen
/// at `KeycapStyle.hardened`, since combinations of them bring back the render defects it was tuned out of.
public struct Keycap: View {
    private let label: Label
    private let action: () -> Void

    private var textSize: Double?
    private var textColor: KeycapStyle.RGBA?
    private var color: KeycapStyle.RGBA?
    private var socket: KeycapStyle.RGBA?
    private var voice: KeycapButtonStyle.Voice = .shipped
    private var feel: UIImpactFeedbackGenerator.FeedbackStyle?
    private var variant: Variant = .large
    private var explicitWidth: Double?
    private var explicitHeight: Double?
    private var fillsWidth = false
    private var paddingH: Double?
    /// Turns the glyph alone: rotating the whole key would tilt its walls with it.
    private var glyphRotation: Angle = .zero
    private var ornament: KeycapButtonStyle.Ornament?

    public enum Variant: String, Sendable, CaseIterable {
        case large, small, mini

        public var factor: Double {
            switch self {
            case .large: 1
            case .small: 0.525
            case .mini: 0.38
            }
        }

        /// Set per variant, not scaled: a legend that shrinks with its cap stops being legible.
        public var fontSize: Double {
            switch self {
            case .large: 14
            case .small: 10.5
            case .mini: 9
            }
        }

        var iconStroke: Double {
            switch self {
            case .large: 2.5
            case .small: 1.5
            case .mini: 1
            }
        }

        public func applied(to style: KeycapStyle) -> KeycapStyle {
            var out = style.scaled(by: factor)
            out.iconStroke = iconStroke
            out.fontSize = fontSize
            return out
        }
    }

    /// A word or a glyph, never both: a 70 pt cap has no room for both to be read.
    private enum Label {
        case text(String)
        case icon(KeycapIcon.Glyph, name: String)
        case band(String, fill: KeycapStyle.RGBA, ink: KeycapStyle.RGBA)
    }

    public init(_ title: String, action: @escaping () -> Void) {
        label = .text(title)
        self.action = action
    }

    public static let bandInset = 0.2

    public init(band title: String, fill: KeycapStyle.RGBA, ink: KeycapStyle.RGBA,
                action: @escaping () -> Void) {
        label = .band(title, fill: fill, ink: ink)
        self.action = action
    }

    public init(icon: KeycapIcon.Glyph, name: String, action: @escaping () -> Void) {
        label = .icon(icon, name: name)
        self.action = action
    }

    // MARK: The dials

    /// Point size of the word, and the side of the glyph on an icon cap.
    public func keycapTextSize(_ size: Double) -> Keycap { copy { $0.textSize = size } }

    public func keycapTextColor(_ colour: KeycapStyle.RGBA) -> Keycap { copy { $0.textColor = colour } }

    /// Tints the albedo only, so the light keeps deciding the shading of every face.
    public func keycapColor(_ colour: KeycapStyle.RGBA) -> Keycap { copy { $0.color = colour } }

    /// Colours the aperture, its contour and its rim shadow as one field: split, they drift apart and a
    /// pressed cap shows a bright ring.
    public func keycapSocketColor(_ colour: KeycapStyle.RGBA) -> Keycap { copy { $0.socket = colour } }

    public func keycapSound(_ sound: KeycapSound?) -> Keycap {
        copy { $0.voice = sound.map { .custom($0) } ?? .silent }
    }

    public func keycapHaptic(_ feel: UIImpactFeedbackGenerator.FeedbackStyle?) -> Keycap {
        copy { $0.feel = feel }
    }

    public func keycapVariant(_ variant: Variant) -> Keycap { copy { $0.variant = variant } }

    /// Outer size override; the variant still sets contour, legend and stroke.
    public func keycapSize(width: Double? = nil, height: Double? = nil) -> Keycap {
        copy { $0.explicitWidth = width; $0.explicitHeight = height }
    }

    /// Takes all the width offered, chrome included; `.frame(maxWidth:)` only stretches the container and
    /// leaves gaps of socket around a naturally sized cap.
    public func keycapFills(_ fills: Bool = true) -> Keycap { copy { $0.fillsWidth = fills } }

    /// A text cap sized before any layout on the widest of its word and `words`: a host that measures
    /// an item once, as a toolbar does, keeps the size it was first proposed whatever the cap says next.
    public func keycapFitted(to words: [String] = []) -> Keycap {
        guard case .text(let title) = label else { return self }
        let measured = style
        let widest = ([title] + words).map(measured.labelWidth).max() ?? 0
        return copy { $0.explicitWidth = widest + 2 * measured.paddingH }
    }

    public func keycapGlyphRotation(_ angle: Angle) -> Keycap { copy { $0.glyphRotation = angle } }

    /// The legend's side margin, absolute like the text size, for a cap too narrow for its word;
    /// it must stay above the walls or the legend rides onto them.
    public func keycapPaddingH(_ padding: Double) -> Keycap { copy { $0.paddingH = padding } }

    /// A mark on the face beside the legend — a lamp, a dot — riding the press with it,
    /// where one laid over the key would stay still while the face sinks.
    public func keycapOrnament<Mark: View>(_ alignment: Alignment = .topTrailing,
                                          @ViewBuilder _ mark: () -> Mark) -> Keycap {
        copy { $0.ornament = KeycapButtonStyle.Ornament(view: AnyView(mark()), alignment: alignment) }
    }

    private func copy(_ change: (inout Keycap) -> Void) -> Keycap {
        var out = self
        change(&out)
        return out
    }

    // MARK: Body

    public var body: some View {
        Button(action: action) { content }
            .buttonStyle(KeycapButtonStyle(style: style, voice: voice, feel: feel, ornament: ornament))
            .accessibilityLabel(accessibilityName)
    }

    public var styleForChecks: KeycapStyle { style }

    public static var disabledOpacity: Double { KeycapTokens.disabledOpacity }

    private var style: KeycapStyle {
        // The variant first: the caller's colours and text size are absolute and must not be scaled by it.
        var out = variant.applied(to: KeycapStyle.hardened)
        out.width = explicitWidth
        out.fillsWidth = fillsWidth
        out.height = explicitHeight
        if let textSize { out.fontSize = textSize }
        if let paddingH { out.paddingH = max(paddingH, out.wallLeft, out.wallRight) }
        if let textColor { out.labelColor = textColor }
        if let color { out.albedo = color }
        if let socket { out.socketColor = socket }
        return out
    }

    @ViewBuilder
    private var content: some View {
        switch label {
        case .text(let title):
            Text(title)
        case .icon(let name, _):
            KeycapIcon(name: name, size: style.iconSize, stroke: style.iconStroke,
                       cap: style.iconLineCap)
                .rotationEffect(glyphRotation)
                .frame(width: style.fontSize)
        case .band(let title, let fill, let ink):
            Text(title)
                .foregroundStyle(ink.color)
                .padding(.horizontal, style.fontSize * Self.bandInset)
                .padding(.vertical, style.fontSize * 0.15)
                .frame(maxWidth: .infinity)
                .background(RoundedRectangle(cornerRadius: style.fontSize * 0.3, style: .continuous)
                                .fill(fill.color))
        }
    }

    private var accessibilityName: String {
        switch label {
        case .text(let title): title
        case .icon(_, let name): name
        case .band(let title, _, _): title
        }
    }
}

public extension KeycapStyle {
    static let hardened = KeycapStyle()
}

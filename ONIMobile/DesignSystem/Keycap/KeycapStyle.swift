import SwiftUI
import UIKit

/// Everything a keycap button is drawn and animated from: its look and its motion, never its size or
/// its title, which belong to the layout and to `Button`.
public extension KeycapStyle {
    /// Typed so untyped literal tuples do not make the compiler give up.
    typealias Numbers = (response: Double, damping: Double)
}

public struct KeycapStyle: Codable, Equatable, Sendable {

    public init() {}

    // MARK: Shape

    /// Room around the label, hence the intrinsic size; a layout may give more, never less. 32 × 26 puts
    /// a 14 pt label on a 100 × 70 cap, where the walls leave the face room to read as a key.
    public var paddingH: Double = 32
    public var paddingV: Double = 26
    public var cornerRadius: Double = 16
    public var squircle: Bool = true

    // MARK: The bezel — the aperture the cap sits in
    // One still shape behind the cap: a shrinking cap opens a gap showing the bezel's colour.

    /// Colours the aperture, its contour and the rim shadow alike: one material seen three ways, so two
    /// values that must agree cannot drift apart.
    public var socketColor = RGBA(r: 0, g: 0, b: 0)
    /// Stays out of `scaled(by:)`, as a stroke width does; `KeycapPlate` and the corner checks read it
    /// even at zero, so a returning contour must find them working.
    public var contourWidth: Double = 0

    // MARK: The frustum
    // Four independent bevel runs: more wall below than above tilts the face away from the viewer.

    public var wallTop: Double = 8
    public var wallBottom: Double = 12
    public var wallLeft: Double = 8
    public var wallRight: Double = 8
    public var rise: Double = 6
    public var faceRadius: Double = 14
    public var dishDepth: Double = 4
    public var seamBlur: Double = 3

    // MARK: No backing layer
    // A layer under the cap shows through any translucency and makes a disabled cap lighter than its
    // ground; leaving it out costs the default white corner 0.027 of lightness.

    // MARK: Light
    // Five values in place of nine hand-picked colours.

    public var albedo = RGBA(r: 1, g: 1, b: 1)
    /// Ambient, diffuse, azimuth, `midtoneMatch` and the cast shadow are one operating point: lower ambient
    /// without higher diffuse underexposes, higher diffuse without `midtoneMatch` clips.
    public var ambient: Double = 0.25
    public var diffuse: Double = 1
    public var lightAzimuth: Double = 100
    public var lightElevation: Double = 33

    /// How far the albedo is the colour of the flat face's middle rather than a ceiling. Without it the
    /// face renders 22.8 % darker than asked; at 1 a pale albedo clips three of seven surfaces.
    public var midtoneMatch: Double = 1

    // MARK: The socket's shadow
    // Laid over the cap and outside its clip, so it covers the boundary pixels that would otherwise
    // composite as `α × lit wall` and leave a bright hairline round the corners.

    public var socketShadowSoftness: Double = 0.5
    public var socketShadowOffset: Double = 0
    public var socketShadowRest: Double = 2
    public var socketShadowPressed: Double = 6

    // MARK: The cast shadow
    // Applied after the button's own clip, since it falls outside it.

    public var shadowColor = RGBA(r: 0, g: 0, b: 0)
    public var shadowBlur: Double = 8
    public var shadowY: Double = 10
    public var shadowOpacity: Double = 0.2
    public var pressShadowY: Double = 0
    public var pressShadowOpacity: Double = 0

    // MARK: Press

    public var scalePressed: Double = 0.95
    public var travel: Double = 1
    public var hoverPressFraction: Double = 0.25
    /// The shortest time the cap stays down: without it a tap plays a tenth of the press and all the release.
    public var minHold: Double = 0.14
    public var pressTiming: Timing = .referencePress
    public var pressResponse: Double = 0.1
    public var pressDamping: Double = 0.8
    public var separateRelease: Bool = true
    public var releaseTiming: Timing = .referenceRelease
    public var releaseResponse: Double = 0.26
    public var releaseDamping: Double = 0.55

    // MARK: Label

    public var fontSize: Double = 14
    /// Its own quantity: a glyph fills a cap by being large where a word does by being long. Scales with
    /// the variant, which `fontSize` does not, since a legend has a legibility floor and a glyph none.
    public var iconSize: Double = 28
    public var iconStroke: Double = 2.5
    /// Flat and flush: `square` extends each end by half the stroke and doubles the stroke where
    /// subpaths meet. Only the cap is squared; the joins stay round.
    public var iconLineCap: LineCap = .butt
    public var weight: WeightChoice = .bold
    public var design: DesignChoice = .default
    public var tracking: Double = 0.5
    public var labelColor = RGBA(r: 0.1298, g: 0.1298, b: 0.1298)
    /// Nudge, for a label that has to sit on the dish's high point rather than dead centre.
    public var labelOffsetY: Double = 0

    /// Centres the label on the face; kept apart from `paddingV` so retuning the walls never changes the height.
    public var labelCentring: Double { (wallTop - wallBottom) / 2 }

    // MARK: Sound

    /// Pitch wander in cents either side: two clicks at one pitch read as one sample played twice.
    /// Varispeed also shifts the 148 ms click by ±10 ms at 120, higher being shorter.
    public var pitchJitter: Double = 120
    public var soundVolume: Double = 0.5

    // MARK: - Nested value types

    public struct RGBA: Codable, Equatable, Sendable {
        public var r: Double
        public var g: Double
        public var b: Double
        public var a: Double = 1

        public init(r: Double, g: Double, b: Double, a: Double = 1) {
            self.r = r; self.g = g; self.b = b; self.a = a
        }

        public init(white: Double, a: Double = 1) {
            self.init(r: white, g: white, b: white, a: a)
        }

        public var color: Color { Color(.sRGB, red: r, green: g, blue: b, opacity: a) }

        /// Every frustum face comes from the albedo this way; alpha is untouched, since a shade darkens a
        /// material without making it transparent.
        public func shaded(_ shade: Double) -> Color {
            Color(.sRGB, red: min(1, r * shade), green: min(1, g * shade),
                  blue: min(1, b * shade), opacity: a)
        }

        /// Read in extended sRGB, the space UIKit resolves a SwiftUI colour into.
        public init(_ color: Color) {
            var (r, g, b, a): (CGFloat, CGFloat, CGFloat, CGFloat) = (0, 0, 0, 1)
            UIColor(color).getRed(&r, green: &g, blue: &b, alpha: &a)
            self.init(r: Double(r), g: Double(g), b: Double(b), a: Double(a))
        }

        public init?(hex: String) {
            var digits = hex.trimmingCharacters(in: .whitespaces)
            if digits.hasPrefix("#") { digits.removeFirst() }
            if digits.count == 3 { digits = digits.map { "\($0)\($0)" }.joined() }
            guard digits.count == 6, let packed = UInt32(digits, radix: 16) else { return nil }
            self.init(r: Double((packed >> 16) & 0xFF) / 255,
                      g: Double((packed >> 8) & 0xFF) / 255,
                      b: Double(packed & 0xFF) / 255)
        }

        public var hex: String {
            String(format: "#%02X%02X%02X",
                   Int((r * 255).rounded()), Int((g * 255).rounded()), Int((b * 255).rounded()))
        }
    }

    public enum LineCap: String, Codable, CaseIterable, Sendable {
        case round, square, butt
    }

    public enum WeightChoice: String, Codable, CaseIterable, Sendable {
        case ultraLight, thin, light, regular, medium, semibold, bold, heavy, black

        var uiWeight: UIFont.Weight {
            switch self {
            case .ultraLight: .ultraLight
            case .thin: .thin
            case .light: .light
            case .regular: .regular
            case .medium: .medium
            case .semibold: .semibold
            case .bold: .bold
            case .heavy: .heavy
            case .black: .black
            }
        }

        public var value: Font.Weight {
            switch self {
            case .ultraLight: .ultraLight
            case .thin: .thin
            case .light: .light
            case .regular: .regular
            case .medium: .medium
            case .semibold: .semibold
            case .bold: .bold
            case .heavy: .heavy
            case .black: .black
            }
        }
    }

    public enum DesignChoice: String, Codable, CaseIterable, Sendable {
        case `default`, rounded, monospaced, serif

        var uiDesign: UIFontDescriptor.SystemDesign {
            switch self {
            case .default: .default
            case .rounded: .rounded
            case .monospaced: .monospaced
            case .serif: .serif
            }
        }

        public var value: Font.Design {
            switch self {
            case .default: .default
            case .rounded: .rounded
            case .monospaced: .monospaced
            case .serif: .serif
            }
        }
    }

    /// Four springs, the reference's two easing curves and a custom spring. The curves decelerate from
    /// full speed with no overshoot, which no spring can approximate.
    public enum Timing: String, Codable, CaseIterable, Sendable {
        case custom, grab, settle, lift, travel
        case referencePress
        case referenceRelease

        /// Reads `timingCurve` and `numbers` rather than restating them, so the CSS port shares one
        /// source; exactly one of the two answers for any case.
        public func animation(response: Double, damping: Double) -> Animation {
            if let curve = timingCurve {
                return .timingCurve(curve.x1, curve.y1, curve.x2, curve.y2, duration: curve.duration)
            }
            let n: KeycapStyle.Numbers = numbers(response: response, damping: damping)
            let out: Animation = .spring(response: n.response, dampingFraction: n.damping)
            return out
        }

        public func numbers(response: Double, damping: Double) -> KeycapStyle.Numbers {
            switch self {
            case .custom, .referencePress, .referenceRelease: (response, damping)
            case .grab: KeycapTokens.Motion.grab
            case .settle: KeycapTokens.Motion.settle
            case .lift: KeycapTokens.Motion.lift
            case .travel: KeycapTokens.Motion.travel
            }
        }

        /// For measurement only, `nil` for the two reference curves.
        public func spring(response: Double, damping: Double) -> Spring? {
            switch self {
            case .referencePress, .referenceRelease: return nil
            default:
                let n = numbers(response: response, damping: damping)
                return Spring(response: n.response, dampingRatio: n.damping)
            }
        }

        public var timingCurve: (x1: Double, y1: Double, x2: Double, y2: Double, duration: Double)? {
            switch self {
            case .referencePress: (0, 0, 0, 1, 0.1)
            case .referenceRelease: (0.79, 0.03, 0.43, 1, 0.02)
            case .custom, .grab, .settle, .lift, .travel: nil
            }
        }
    }

    // MARK: - Depth
    // One depth: 0 at rest, `hoverPressFraction` hovered, 1 pressed; interpolating here covers every pressed quantity.

    public func travel(at depth: Double) -> Double { travel * depth }

    public func scale(at depth: Double) -> Double { 1 + (scalePressed - 1) * depth }

    public func socketShadow(at depth: Double) -> Double {
        socketShadowRest + (socketShadowPressed - socketShadowRest) * depth
    }

    public func shadowOffset(at depth: Double) -> Double { shadowY + (pressShadowY - shadowY) * depth }

    public func shadowStrength(at depth: Double) -> Double {
        shadowOpacity + (pressShadowOpacity - shadowOpacity) * depth
    }

    // MARK: - Derived

    public var pressAnimation: Animation {
        pressTiming.animation(response: pressResponse, damping: pressDamping)
    }

    public var releaseAnimation: Animation {
        separateRelease
            ? releaseTiming.animation(response: releaseResponse, damping: releaseDamping)
            : pressAnimation
    }

    public var font: Font { .system(size: fontSize, weight: weight.value, design: design.value) }

    /// The least share of the cap the face keeps; below it the walls shrink but keep their shading, so a
    /// small cap degrades into a small cap rather than a block of wall.
    public static let minFaceShare: Double = 0.4

    // MARK: - Variants

    /// An explicit outer size, padding included; `nil` sizes the cap itself. A height under the legend's
    /// clips it, so a caller's undersized cap shows rather than silently growing.
    public var width: Double?
    public var height: Double?

    /// Takes all the offered width: `.frame(maxWidth: .infinity)` widens only the container, never the
    /// chrome drawn inside the style. An explicit `width` wins.
    public var fillsWidth = false

    /// Scales every length except `unscaledLengths`; an optional length added later is scaled here by hand.
    public func scaled(by factor: Double) -> KeycapStyle {
        var out = self
        out.paddingH *= factor
        out.paddingV *= factor
        out.cornerRadius *= factor
        out.wallTop *= factor
        out.wallBottom *= factor
        out.wallLeft *= factor
        out.wallRight *= factor
        out.rise *= factor
        out.faceRadius *= factor
        out.dishDepth *= factor
        out.seamBlur *= factor
        out.socketShadowRest *= factor
        out.socketShadowPressed *= factor
        out.socketShadowOffset *= factor
        out.shadowBlur *= factor
        out.shadowY *= factor
        out.pressShadowY *= factor
        out.travel *= factor
        out.labelOffsetY *= factor
        out.iconSize *= factor
        return out
    }

    /// Weights, not proportions, so `scaled(by:)` leaves them alone; `KeycapChecks` reads this list.
    public static let unscaledLengths = ["contourWidth", "fontSize", "iconStroke"]

    /// One line of the label, whatever the label is, so an icon cap is exactly as tall as a text cap.
    /// Read off `UIFont` and ceiled, since a ruler view would join the layout and a half point rounds apart.
    public var labelHeight: Double {
        let font = uiFont
        return ceil(Double(font.ascender - font.descender + font.leading))
    }

    public func labelWidth(_ text: String) -> Double {
        let line = NSAttributedString(string: text, attributes: [.font: uiFont, .kern: tracking])
        return ceil(Double(line.size().width))
    }

    private var uiFont: UIFont {
        let font = UIFont.systemFont(ofSize: fontSize, weight: weight.uiWeight)
        guard let descriptor = font.fontDescriptor.withDesign(design.uiDesign) else { return font }
        return UIFont(descriptor: descriptor, size: fontSize)
    }

    public func faceRect(in size: CGSize) -> CGRect {
        let squeeze = min(fit(wallTop + wallBottom, in: size.height),
                          fit(wallLeft + wallRight, in: size.width))
        return CGRect(x: wallLeft * squeeze, y: wallTop * squeeze,
                      width: max(0, size.width - (wallLeft + wallRight) * squeeze),
                      height: max(0, size.height - (wallTop + wallBottom) * squeeze))
    }

    private func fit(_ runs: Double, in extent: Double) -> Double {
        let allowed = extent * (1 - Self.minFaceShare)
        guard runs > allowed, runs > 0 else { return 1 }
        return allowed / runs
    }
}

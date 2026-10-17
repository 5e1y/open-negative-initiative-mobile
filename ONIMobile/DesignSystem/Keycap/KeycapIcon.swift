import SwiftUI

/// A Trinil glyph stroked at a fixed weight whatever its size: the path is scaled, never the stroke.
/// The paths are the SVGs' own, absolute commands only, so no SVG renderer is needed on iOS.
public struct KeycapIcon: View {
    public struct Glyph: RawRepresentable, Hashable, Sendable {
        public let rawValue: String

        public init(rawValue: String) {
            self.rawValue = rawValue
        }
    }

    let name: Glyph
    let size: Double
    let stroke: Double
    let cap: KeycapStyle.LineCap

    public init(name: Glyph, size: Double, stroke: Double = KeycapIcon.strokeWidth,
                cap: KeycapStyle.LineCap = .square) {
        self.name = name
        self.size = size
        self.stroke = stroke
        self.cap = cap
    }

    public var body: some View {
        if let data = Self.paths[name] {
            Self.shape(data, cap: cap)
                .stroke(style: StrokeStyle(lineWidth: CGFloat(stroke), lineCap: cap.swiftUI,
                                           lineJoin: cap == .round ? .round : .miter,
                                           miterLimit: Self.miterLimit))
                .frame(width: size, height: size)
        } else {
            RoundedRectangle(cornerRadius: 3, style: .continuous)
                .fill(KeycapTokens.warning)
                .frame(width: size, height: size)
                .overlay {
                    Text("?")
                        .font(.system(size: size * 0.7, weight: .bold))
                        .foregroundStyle(KeycapTokens.onWarning)
                }
        }
    }

    public static let authoredViewBox: Double = 24
    public static let strokeWidth = KeycapTokens.emphasisStroke
    static let miterLimit: CGFloat = 1.78829

    static let paths: [Glyph: String] = [
        .chevronLeft: "M17.1879 21.75L7.54393 12.1061C7.48536 12.0475 7.48535 11.9525 7.54393 11.8939L17.1879 2.25",
        .chevronRight: "M8.25 21.75L17.8939 12.1061C17.9525 12.0475 17.9525 11.9525 17.8939 11.8939L8.25 2.25",
        .undo: "M2.25 8.24995V15.75M2.25 15.75H9.75M2.25 15.75L6 12C12.75 5.24999 20.0996 8.3985 21.75 15",
        .redo: "M21.75 8.24995V15.75M21.75 15.75H14.25M21.75 15.75L18 12C11.25 5.24999 3.90036 8.3985 2.25 15",
    ]

    /// Square caps stack where one subpath starts on another's end, so those moves become joins.
    static func shape(_ data: String, cap: KeycapStyle.LineCap) -> GlyphShape {
        GlyphShape(data: cap == .round ? data : joinPath(data) ?? data)
    }

    /// Drops every `M` landing where the pen already is, so a corner becomes a join; it only removes
    /// tokens and returns nil on any relative command, so the outline never changes.
    static func joinPath(_ path: String) -> String? {
        var out = ""
        var point: (x: Double, y: Double)?
        for (letter, numbers) in commands(path) {
            guard "MLHVCZ".contains(letter) else { return nil }
            if letter == "M", numbers.count >= 2, let current = point,
               abs(numbers[0] - current.x) < 1e-6, abs(numbers[1] - current.y) < 1e-6 {
                // The redundant move is dropped; pairs past the first are implicit line-tos, kept as an `L`.
                if numbers.count > 2 {
                    out += "L" + numbers.dropFirst(2).map { String($0) }.joined(separator: " ")
                }
                continue
            }
            out += String(letter) + numbers.map { String($0) }.joined(separator: " ")
            switch letter {
            case "M", "L", "C":
                if numbers.count >= 2 { point = (numbers[numbers.count - 2], numbers[numbers.count - 1]) }
            case "H":
                if let x = numbers.last, let p = point { point = (x, p.y) }
            case "V":
                if let y = numbers.last, let p = point { point = (p.x, y) }
            default:
                break
            }
        }
        return out
    }

    /// The path's commands as letters and their numbers, split on letters, spaces and commas.
    static func commands(_ path: String) -> [(Character, [Double])] {
        var out: [(Character, [Double])] = []
        var token = ""
        func flush() {
            if let value = Double(token), !out.isEmpty { out[out.count - 1].1.append(value) }
            token = ""
        }
        for character in path {
            if character.isLetter {
                flush()
                out.append((character, []))
            } else if character == " " || character == "," || character == "\n" {
                flush()
            } else if character == "-", !token.isEmpty {
                flush()
                token = "-"
            } else {
                token.append(character)
            }
        }
        flush()
        return out
    }
}

/// An absolute-command path in the glyph's 24-unit box, scaled to the frame it is given.
struct GlyphShape: Shape {
    let data: String

    func path(in rect: CGRect) -> Path {
        let k = min(rect.width, rect.height) / CGFloat(KeycapIcon.authoredViewBox)
        func p(_ x: Double, _ y: Double) -> CGPoint {
            CGPoint(x: rect.minX + CGFloat(x) * k, y: rect.minY + CGFloat(y) * k)
        }
        var path = Path()
        var pen = CGPoint.zero
        for (letter, n) in KeycapIcon.commands(data) {
            switch letter {
            case "M" where n.count >= 2:
                path.move(to: p(n[0], n[1]))
                stride(from: 2, to: n.count - 1, by: 2).forEach { path.addLine(to: p(n[$0], n[$0 + 1])) }
            case "L":
                stride(from: 0, to: n.count - 1, by: 2).forEach { path.addLine(to: p(n[$0], n[$0 + 1])) }
            case "H":
                n.forEach { path.addLine(to: CGPoint(x: rect.minX + CGFloat($0) * k, y: pen.y)) }
            case "V":
                n.forEach { path.addLine(to: CGPoint(x: pen.x, y: rect.minY + CGFloat($0) * k)) }
            case "C":
                stride(from: 0, to: n.count - 5, by: 6).forEach {
                    path.addCurve(to: p(n[$0 + 4], n[$0 + 5]), control1: p(n[$0], n[$0 + 1]),
                                  control2: p(n[$0 + 2], n[$0 + 3]))
                }
            case "Z":
                path.closeSubpath()
            default:
                break
            }
            pen = path.currentPoint ?? pen
        }
        return path
    }
}

public extension KeycapIcon.Glyph {
    static let chevronLeft = Self(rawValue: "chevron-left")
    static let chevronRight = Self(rawValue: "chevron-right")
    static let redo = Self(rawValue: "redo")
    static let undo = Self(rawValue: "undo")
}

extension KeycapStyle.LineCap {
    var swiftUI: CGLineCap {
        switch self {
        case .round: .round
        case .square: .square
        case .butt: .butt
        }
    }
}

import Foundation

/// The scanner screen's words and numbers, written as the desktop's `LabScreen` and `LabStepKeys` write them.
enum ScannerText {
    static func frame(_ frame: Frame?) -> String {
        frame.map { String(format: "FRAME %02d/%02d", Int($0.index), Int($0.count)) } ?? "FRAME --/--"
    }

    static func machine(_ machine: Machine) -> String {
        switch machine {
        case .oni: "ONI"
        case .frontier: "FRONTIER"
        }
    }

    static func channel(_ channel: Channel) -> String {
        switch channel {
        case .negative: "NEGA"
        case .positive: "POSI"
        case .monochrome: "B&W"
        }
    }

    static func spelled(_ steps: Int8) -> String {
        guard steps != 0 else { return "0" }
        return (steps < 0 ? "−" : "+") + String(abs(Int(steps)))
    }

    static func percent(_ value: Float) -> String {
        let shown = Int((value * 100).rounded())
        return shown == 0 ? "0 %" : (shown < 0 ? "−" : "+") + "\(abs(shown)) %"
    }

    static func stops(_ value: Float) -> String {
        abs(value) < 0.005 ? "0 EV" : String(format: "%@%.1f EV", value < 0 ? "−" : "+", abs(value))
    }

    /// A step's legend: outer keys their count, inner ones their sign.
    static func legend(_ step: Int8) -> String {
        let sign = step < 0 ? "−" : "+"
        return abs(step) == 1 ? sign : sign + String(abs(Int(step)))
    }
}

extension Letter: CaseIterable {
    public static var allCases: [Letter] { [.d, .c, .m, .y, .w] }

    var title: String {
        switch self {
        case .d: "D"
        case .c: "C"
        case .m: "M"
        case .y: "Y"
        case .w: "TEMP"
        }
    }
}

extension Clicks {
    subscript(letter: Letter) -> Int8 {
        switch letter {
        case .d: d
        case .c: c
        case .m: m
        case .y: y
        case .w: w
        }
    }
}

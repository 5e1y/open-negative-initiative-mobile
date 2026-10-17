import SwiftUI

/// The desktop app's tokens the remote draws with, copied from its `DSTokens`, which stays authoritative.
enum DS {
    enum Space {
        static let xxs: CGFloat = 2
        static let xs: CGFloat = 4
        static let sm: CGFloat = 8
        static let md: CGFloat = 16
        static let lg: CGFloat = 24
    }

    enum Stroke {
        static let line: CGFloat = 1
    }

    enum Palette {
        static let lcdGlass = Color(white: 0.05)
        static let lcdInk = Color(white: 0.93)
        static let canvas = Color(.sRGB, red: 27 / 255, green: 27 / 255, blue: 27 / 255, opacity: 1)
        static let textOnFill = Color.white
        static let textOnLight = Color(white: 0.12)
        /// An albedo, lit and shaded by the keycap chrome, so it reads darker than its own value.
        static let keycap = Color(.sRGB, red: 0x31 / 255, green: 0x31 / 255, blue: 0x31 / 255, opacity: 1)
        static let textSecondary = Color.white.opacity(0.55)
        static let lampOn = Color(.sRGB, red: 0.36, green: 0.86, blue: 0.47, opacity: 1)
        static let lampWait = Color(.sRGB, red: 0.95, green: 0.64, blue: 0.23, opacity: 1)
        static let lampOff = Color(white: 0.32)
    }

    enum Scanner {
        static let bandCool = KeycapStyle.RGBA(r: 0.23, g: 0.55, b: 0.86)
        static let bandWarm = KeycapStyle.RGBA(r: 0.95, g: 0.64, b: 0.23)
        static let bandGreen = KeycapStyle.RGBA(r: 0.18, g: 0.64, b: 0.48)
        static let bandMagenta = KeycapStyle.RGBA(r: 0.84, g: 0.20, b: 0.50)
        static let bandRed = KeycapStyle.RGBA(r: 0.84, g: 0.25, b: 0.22)
        static let bandCyan = KeycapStyle.RGBA(r: 0.12, g: 0.66, b: 0.78)
        static let bandBlue = KeycapStyle.RGBA(r: 0.22, g: 0.36, b: 0.84)
        static let bandYellow = KeycapStyle.RGBA(r: 0.93, g: 0.78, b: 0.18)
        static let bandDark = KeycapStyle.RGBA(r: 0.28, g: 0.28, b: 0.30)
        static let bandLight = KeycapStyle.RGBA(r: 0.86, g: 0.86, b: 0.84)

        static let wideKeyWidth: CGFloat = 96
        /// Under the desktop's 46: eight rows, the screen and the bar must fit an iPhone 15 without scrolling.
        static let keyHeight: CGFloat = 44
        static let keyLegendSize: CGFloat = 13
        static let keyPaddingH: CGFloat = 8
        static let screenValueSize: CGFloat = 30
        static let screenLabelSize: CGFloat = 12
        static let scanlinePitch: CGFloat = 3
        static let phosphorGlow: CGFloat = 5
        static let lampSize: CGFloat = 8
    }
}

enum DSShape {
    static func rounded(_ radius: CGFloat) -> RoundedRectangle {
        RoundedRectangle(cornerRadius: radius, style: .continuous)
    }
}

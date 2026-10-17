import SwiftUI

/// Constants a keycap borrows from `DSTokens`, quoted rather than referenced to keep the component
/// self-contained. The originals stay authoritative: if they move, copy them again.
enum KeycapTokens {

    enum Motion {
        static let grab: KeycapStyle.Numbers = (response: 0.22, damping: 0.62)
        /// A value arriving from elsewhere than the hand; the slight overshoot says the value moved.
        static let settle: KeycapStyle.Numbers = (response: 0.38, damping: 0.72)
        static let lift: KeycapStyle.Numbers = (response: 0.16, damping: 0.55)
        /// Critically damped, overshoot 0.0000, which is why it and not `settle` carries a displacement.
        static let travel: KeycapStyle.Numbers = (response: 0.52, damping: 1)
    }

    static let emphasisStroke: Double = 2

    /// Fades the legend alone, never the cap: a translucent cap lets its ground through and can read
    /// lighter than the surface it sits on.
    static let disabledOpacity: Double = 0.3

    static let warning = Color.orange
    static let onWarning = Color.black
}

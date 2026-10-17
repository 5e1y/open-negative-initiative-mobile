import SwiftUI

/// Keycaps flush on one plate, gap zero, so the row reads as one object; the radius grows with the contour
/// so the plate follows it. The variant must match the children's, since a plate cannot ask their size.
public struct KeycapPlate<Content: View>: View {
    private let variant: Keycap.Variant
    /// Must land on the `HStack` under the background: a frame around the plate leaves the row centred.
    private let fills: Bool
    private let content: Content

    public init(variant: Keycap.Variant = .small, fills: Bool = false,
                @ViewBuilder content: () -> Content) {
        self.variant = variant
        self.fills = fills
        self.content = content()
    }

    public var body: some View {
        let style = variant.applied(to: KeycapStyle.hardened)
        HStack(spacing: 0) { content }
            .frame(maxWidth: fills ? .infinity : nil)
            .background {
                RoundedRectangle(cornerRadius: style.cornerRadius + style.contourWidth,
                                 style: style.squircle ? .continuous : .circular)
                    .fill(style.socketColor.color)
            }
    }
}

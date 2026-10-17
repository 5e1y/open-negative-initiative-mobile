import SwiftUI
import UIKit

/// The keycap as a `ButtonStyle`, so it composes with `.frame`, `.disabled`, shortcuts and any label.
/// `KeycapPress` holds a tap down for `minHold`, or the press is cut off before it visibly moves.
public struct KeycapButtonStyle: ButtonStyle {
    public var style: KeycapStyle = KeycapStyle()
    public var voice: Voice = .shipped
    /// The tap felt at press-down, ahead of the action and of anything it sends.
    public var feel: UIImpactFeedbackGenerator.FeedbackStyle?
    public var ornament: Ornament?

    public struct Ornament {
        let view: AnyView
        let alignment: Alignment
    }

    public enum Voice {
        case shipped
        case silent
        case custom(KeycapSound)

        /// Resolved at click time, never in a `body`: the shipped sound builds an audio engine.
        var resolved: KeycapSound? {
            switch self {
            case .shipped: KeycapSound.shipped
            case .silent: nil
            case .custom(let sound): sound
            }
        }
    }

    public func makeBody(configuration: Configuration) -> some View {
        KeycapPress(style: style, voice: voice, feel: feel, ornament: ornament, configuration: configuration)
    }
}

public extension ButtonStyle where Self == KeycapButtonStyle {
    static var keycap: KeycapButtonStyle { KeycapButtonStyle() }

    static func keycap(_ style: KeycapStyle) -> KeycapButtonStyle {
        KeycapButtonStyle(style: style)
    }

    static func keycap(_ style: KeycapStyle, sound: KeycapSound?) -> KeycapButtonStyle {
        KeycapButtonStyle(style: style,
                          voice: sound.map { .custom($0) } ?? .silent)
    }
}

/// A view of its own because a `ButtonStyle` cannot hold the `@State` a tap's duration needs.
private struct KeycapPress: View {
    let style: KeycapStyle
    let voice: KeycapButtonStyle.Voice
    let feel: UIImpactFeedbackGenerator.FeedbackStyle?
    let ornament: KeycapButtonStyle.Ornament?
    let configuration: ButtonStyleConfiguration

    @Environment(\.isEnabled) private var isEnabled

    @State private var held = false
    @State private var pressedAt: Date?
    @State private var release: Task<Void, Never>?

    @MainActor
    private var heard: KeycapSound? { KeycapSound.isMuted ? nil : voice.resolved }

    private var depth: Double { held ? 1 : 0 }

    var body: some View {
        // Computed and typed outside the chain, which otherwise exceeds the type checker's time limit.
        let contentWidth: CGFloat? = style.width.map { CGFloat(max(0, $0 - style.paddingH * 2)) }
        let contentHeight: CGFloat = style.height.map { CGFloat(max(0, $0 - style.paddingV * 2)) }
            ?? CGFloat(style.labelHeight)
        let fillWidth: CGFloat? = style.width == nil && style.fillsWidth ? .infinity : nil
        return configuration.label
            .font(style.font)
            .tracking(style.tracking)
            // An overrun says the cap is too small for the word; a reflow would hide it.
            .lineLimit(1)
            .fixedSize(horizontal: false, vertical: true)
            // Fixed here so every label gives the same cap, and the explicit outer size loses its padding.
            // `fillsWidth` works only here, under the padding: below it the chrome has already sized itself.
            .frame(width: contentWidth, height: contentHeight)
            .frame(maxWidth: fillWidth)
            .foregroundStyle(style.labelColor.color)
            .overlay(alignment: ornament?.alignment ?? .center) { ornament?.view }
            // The label rides the face: offset then scale, the chrome's order, so it never drifts off the
            // surface at the bottom of the press. Neither touches layout.
            .offset(y: style.labelCentring + style.labelOffsetY + style.travel(at: depth))
            .scaleEffect(style.scale(at: depth))
            .opacity(isEnabled ? 1 : KeycapTokens.disabledOpacity)
            .padding(.horizontal, style.paddingH)
            .padding(.vertical, style.paddingV)
            .background { KeycapChrome(style: style, depth: depth) }
            .onChange(of: configuration.isPressed) { _, isPressed in
                if isPressed { begin() } else { end() }
            }
    }

    @MainActor
    private func begin() {
        release?.cancel()
        release = nil
        pressedAt = .now
        if let feel { KeycapHaptic.strike(feel) }
        withAnimation(style.pressAnimation) { held = true }
        heard?.press(jitterCents: style.pitchJitter, volume: style.soundVolume)
    }

    @MainActor
    private func end() {
        let elapsed = pressedAt.map { Date.now.timeIntervalSince($0) } ?? .infinity
        let remaining = style.minHold - elapsed
        guard remaining > 0 else { return lift() }
        // Typed outside the call, which otherwise exceeds the type checker's time limit.
        let nanoseconds = UInt64(remaining * 1_000_000_000)
        release = Task {
            try? await Task.sleep(nanoseconds: nanoseconds)
            guard !Task.isCancelled else { return }
            lift()
        }
    }

    @MainActor
    private func lift() {
        release = nil
        pressedAt = nil
        withAnimation(style.releaseAnimation) { held = false }
        // Here and not in `end()`: the release sound follows the cap rising, not the finger lifting.
        heard?.release(volume: style.soundVolume)
    }
}

/// One generator per weight, kept and re-prepared so the Taptic Engine answers the next press at once.
@MainActor
enum KeycapHaptic {
    private static var generators: [Int: UIImpactFeedbackGenerator] = [:]

    static func strike(_ weight: UIImpactFeedbackGenerator.FeedbackStyle) {
        let generator = generators[weight.rawValue] ?? UIImpactFeedbackGenerator(style: weight)
        generators[weight.rawValue] = generator
        generator.impactOccurred()
        generator.prepare()
    }
}

/// Bezel, cap, socket shadow and cast shadow, in the order that matters.
public struct KeycapChrome: View {
    public let style: KeycapStyle
    public let depth: Double
    public var socketShadowInsideClip = false

    public init(style: KeycapStyle, depth: Double, socketShadowInsideClip: Bool = false) {
        self.style = style
        self.depth = depth
        self.socketShadowInsideClip = socketShadowInsideClip
    }

    public var body: some View {
        ZStack {
            bezel

            KeycapSurface(style: style, depth: depth,
                          socketShadowInsideClip: socketShadowInsideClip)
            // Outside the cap's clip, the only place that covers its lit edge pixels (hairline 0.19 inside, 0.00 outside).
            .overlay { socketShadowInsideClip ? nil : socketShadow }
            .scaleEffect(style.scale(at: depth))
            .padding(style.contourWidth)
        }
        .clipShape(shape(style.cornerRadius + style.contourWidth))
        // After the clip, which would otherwise delete the cast shadow.
        .shadow(color: style.shadowColor.color.opacity(style.shadowStrength(at: depth)),
                radius: style.shadowBlur,
                y: style.shadowOffset(at: depth))
    }

    private var bezel: some View {
        shape(style.cornerRadius + style.contourWidth)
            .fill(style.socketColor.color)
    }

    private var socketShadow: some View {
        SocketShadow(color: style.socketColor.color,
                     spread: style.socketShadow(at: depth),
                     offset: style.socketShadowOffset,
                     softness: style.socketShadowSoftness,
                     cornerRadius: style.cornerRadius,
                     squircle: style.squircle)
    }

    private func shape(_ radius: Double) -> RoundedRectangle {
        RoundedRectangle(cornerRadius: radius, style: style.squircle ? .continuous : .circular)
    }
}

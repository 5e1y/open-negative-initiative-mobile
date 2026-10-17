import SwiftUI

/// The keycap's chrome, drawn into whatever size a layout gives it: blurred walls and the dished face
/// inside the silhouette's clip, the socket shadow outside it (see `KeycapChrome`).
public struct KeycapSurface: View {
    public let style: KeycapStyle
    public let depth: Double
    public var socketShadowInsideClip = false

    public init(style: KeycapStyle, depth: Double, socketShadowInsideClip: Bool = false) {
        self.style = style
        self.depth = depth
        self.socketShadowInsideClip = socketShadowInsideClip
    }

    public var body: some View {
        let light = Frustum.Light(style)

        GeometryReader { proxy in
            let size = proxy.size
            let face = style.faceRect(in: size)

            ZStack(alignment: .topLeading) {
                walls(size: size, face: face, light: light)

                dishedFace(face, light: light)

                if socketShadowInsideClip {
                    SocketShadow(color: style.socketColor.color,
                                 spread: style.socketShadow(at: depth),
                                 offset: style.socketShadowOffset,
                                 softness: style.socketShadowSoftness,
                                 cornerRadius: style.cornerRadius,
                                 squircle: style.squircle)
                }
            }
            .frame(width: size.width, height: size.height, alignment: .topLeading)
            .offset(y: style.travel(at: depth))
        }
        .clipShape(shape(style.cornerRadius))
    }

    // MARK: The four walls

    /// One canvas, four flat-shaded quadrilaterals, grown past the silhouette and under the face so the
    /// blur has material beyond both edges and leaves no white fringe.
    private func walls(size: CGSize, face: CGRect, light: Frustum.Light) -> some View {
        let bleed = 2 * style.seamBlur
        // In the canvas's own coordinates, starting at zero: offsetting afterwards shifts everything twice
        // and brings the white fringe back.
        let outer = CGRect(x: 0, y: 0, width: size.width + 2 * bleed, height: size.height + 2 * bleed)
        let inner = face.offsetBy(dx: bleed, dy: bleed).insetBy(dx: bleed, dy: bleed)

        return Canvas(opaque: false, rendersAsynchronously: false) { context, _ in
            for wall in Frustum.Face.allCases {
                let normal = wall.normal(run: wall.run(style), rise: style.rise)
                context.fill(wall.path(base: outer, face: inner),
                             with: .color(light.colour(normal: normal)))
            }
        }
        // Laid in the grown box and put back: `.blur()` renders into a layer its view's size, cutting
        // anything outside before blurring.
        .frame(width: outer.width, height: outer.height)
        .blur(radius: style.seamBlur)
        .offset(x: -bleed, y: -bleed)
    }

    // MARK: The top face

    private func dishedFace(_ face: CGRect, light: Frustum.Light) -> some View {
        shape(style.faceRadius)
            .fill(LinearGradient(stops: light.faceStops(style, faceHeight: face.height),
                                 startPoint: .top, endPoint: .bottom))
            .frame(width: face.width, height: face.height)
            .offset(x: face.minX, y: face.minY)
    }

    private func shape(_ radius: Double) -> RoundedRectangle {
        RoundedRectangle(cornerRadius: radius, style: style.squircle ? .continuous : .circular)
    }
}

/// The socket's rim darkening as the cap sinks, strongest at the edge: a centred stroke blurred in a grown
/// layer, since `strokeBorder` sets the peak 4 px in and halves the density at the boundary.
public struct SocketShadow: View {
    public let color: Color
    public let spread: Double
    /// How far outside the silhouette the band is centred, carrying the darkening over the bezel; the
    /// inward reach stays `spread - offset`.
    public let offset: Double
    /// Softness as a fraction of the spread; the darkest point stays at the edge up to 1 since `room` grows
    /// with it.
    public let softness: Double
    public let cornerRadius: Double
    public let squircle: Bool

    public init(color: Color, spread: Double, offset: Double, softness: Double,
                cornerRadius: Double, squircle: Bool) {
        self.color = color
        self.spread = spread
        self.offset = offset
        self.softness = softness
        self.cornerRadius = cornerRadius
        self.squircle = squircle
    }

    private var room: Double { spread * (1 + 3 * softness) + 1 }

    public var body: some View {
        GeometryReader { proxy in
            RoundedRectangle(cornerRadius: cornerRadius + offset,
                             style: squircle ? .continuous : .circular)
                .stroke(color, lineWidth: spread * 2)
                .frame(width: proxy.size.width + 2 * offset,
                       height: proxy.size.height + 2 * offset)
                .padding(room)
                .blur(radius: spread * softness)
                .padding(-room)
        }
        .allowsHitTesting(false)
    }
}

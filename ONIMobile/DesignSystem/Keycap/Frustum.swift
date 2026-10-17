import SwiftUI

/// The keycap's four wall faces, as geometry and as shading. Each planar face has one normal, hence one
/// flat luminance: a gradient across the body would describe a cylinder and read as a soft lozenge.
public enum Frustum {

    /// The scoop's normal at `t`, 0 at the face's top edge to 1 at its bottom; the slope's sign darkens the
    /// top half under a light from above. A zero-height face shades as a plane rather than divide by zero.
    public static func dishNormal(at t: Double, dishDepth d: Double,
                                  faceHeight h: Double) -> (x: Double, y: Double, z: Double) {
        guard h > 0 else { return (0, 0, 1) }
        let slope = 4 * d * (2 * t - 1) / h
        let length = (1 + slope * slope).squareRoot()
        return (0, -slope / length, 1 / length)
    }

    /// Seven stops read as a curve: at five a seam shows on a magnified face, at nine nothing changes.
    /// The CSS port emits this same number, so the two cannot drift apart.
    public static let dishStops = 7

    public enum Face: CaseIterable, Hashable {
        case top, bottom, left, right

        public func normal(run: Double, rise: Double) -> (x: Double, y: Double, z: Double) {
            let length = max(1e-9, (run * run + rise * rise).squareRoot())
            switch self {
            case .top: return (0, -rise / length, run / length)
            case .bottom: return (0, rise / length, run / length)
            case .left: return (-rise / length, 0, run / length)
            case .right: return (rise / length, 0, run / length)
            }
        }

        public func run(_ style: KeycapStyle) -> Double {
            switch self {
            case .top: style.wallTop
            case .bottom: style.wallBottom
            case .left: style.wallLeft
            case .right: style.wallRight
            }
        }

        /// The outer corners are the silhouette's square ones: the assembled walls are clipped to the
        /// rounded outline at once, since rounding each face alone leaves four notches where they meet.
        public func path(base: CGRect, face: CGRect) -> Path {
            var path = Path()
            let corners: [CGPoint]
            switch self {
            case .top:
                corners = [.init(x: base.minX, y: base.minY), .init(x: base.maxX, y: base.minY),
                           .init(x: face.maxX, y: face.minY), .init(x: face.minX, y: face.minY)]
            case .bottom:
                corners = [.init(x: base.minX, y: base.maxY), .init(x: base.maxX, y: base.maxY),
                           .init(x: face.maxX, y: face.maxY), .init(x: face.minX, y: face.maxY)]
            case .left:
                corners = [.init(x: base.minX, y: base.minY), .init(x: base.minX, y: base.maxY),
                           .init(x: face.minX, y: face.maxY), .init(x: face.minX, y: face.minY)]
            case .right:
                corners = [.init(x: base.maxX, y: base.minY), .init(x: base.maxX, y: base.maxY),
                           .init(x: face.maxX, y: face.maxY), .init(x: face.maxX, y: face.minY)]
            }
            path.addLines(corners)
            path.closeSubpath()
            return path
        }
    }

    public struct Light {
        public var albedo: KeycapStyle.RGBA
        public var ambient: Double
        public var diffuse: Double
        public var azimuth: Double
        public var elevation: Double
        public var midtoneMatch: Double

        public init(_ style: KeycapStyle) {
            albedo = style.albedo
            ambient = style.ambient
            diffuse = style.diffuse
            azimuth = style.lightAzimuth
            elevation = style.lightElevation
            midtoneMatch = style.midtoneMatch
        }

        /// What every shade is divided by: 1 at `midtoneMatch = 0`, the flat face's shade at 1. Blending the
        /// divisor rather than the shade yields one image at an intermediate exposure, not a mix of two.
        public var normaliser: Double {
            // The flat face's normal is (0, 0, 1), so its Lambert term is the light's z, sin(elevation).
            let flatFace = ambient + diffuse * max(0, direction.z)
            return 1 + (flatFace - 1) * min(1, max(0, midtoneMatch))
        }

        public var direction: (x: Double, y: Double, z: Double) {
            let e = elevation * .pi / 180
            let a = azimuth * .pi / 180
            return (cos(e) * cos(a), -cos(e) * sin(a), sin(e))
        }

        /// Unclamped: a shade above 1 is a highlight over the albedo, and `RGBA.shaded` clips each channel.
        /// Clamping here flattens a dark cap's top wall at 1 so a press would not darken it.
        public func shade(normal n: (x: Double, y: Double, z: Double)) -> Double {
            rawShade(normal: n)
        }

        /// The amount of light before display; `clipping(of:)` and `clippingThreshold` read it. Clipping is
        /// reported, never corrected: a shoulder would invent a response curve.
        public func rawShade(normal n: (x: Double, y: Double, z: Double)) -> Double {
            (ambient + diffuse * lambert(normal: n)) / max(1e-6, normaliser)
        }

        /// `max(0, n · l)`, the part of a shade a press does not move; the CSS port bakes it into its
        /// stylesheet, which keeps one shading model instead of two.
        public func lambert(normal n: (x: Double, y: Double, z: Double)) -> Double {
            let l = direction
            return max(0, n.x * l.x + n.y * l.y + n.z * l.z)
        }

        /// The faces whose colour, albedo times shade, passes white in a channel; a shade above 1 alone is a
        /// healthy highlight. Empty when nothing clips.
        public func clipping(of style: KeycapStyle) -> [(face: Face, raw: Double)] {
            let brightest = max(albedo.r, max(albedo.g, albedo.b))
            return Face.allCases.compactMap { face in
                let raw = rawShade(normal: face.normal(run: face.run(style), rise: style.rise))
                return raw * brightest > 1.001 ? (face, raw) : nil
            }
        }

        /// The `n · l` above which a face comes out pure white, following the normalisation. Above 1
        /// nothing can clip, which `ambient + diffuse <= 1` guarantees.
        public var clippingThreshold: Double {
            diffuse > 0 ? (normaliser - ambient) / diffuse : .infinity
        }

        public func colour(normal n: (x: Double, y: Double, z: Double)) -> Color {
            albedo.shaded(shade(normal: n))
        }

        public func faceShade(at t: Double, dishDepth d: Double, faceHeight h: Double) -> Double {
            shade(normal: Frustum.dishNormal(at: t, dishDepth: d, faceHeight: h))
        }

        public func faceStops(_ style: KeycapStyle, faceHeight: Double) -> [Gradient.Stop] {
            let steps = Frustum.dishStops
            return (0..<steps).map { index in
                let t = Double(index) / Double(steps - 1)
                let shade = faceShade(at: t, dishDepth: style.dishDepth, faceHeight: faceHeight)
                return Gradient.Stop(color: style.albedo.shaded(shade), location: t)
            }
        }
    }
}

import AVFoundation

/// The click, its pitch wandering a few tens of cents per press so repeats read as a struck object, not a
/// sample. Varispeed shifts speed with pitch as a stiffer key does; one player lets fast clicks overlap.
public final class KeycapSound: @unchecked Sendable {
    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()
    private let varispeed = AVAudioUnitVarispeed()
    private let down: AVAudioPCMBuffer
    private let up: AVAudioPCMBuffer?
    /// Drawn at the press and reused at the release, since two pitches would sound like two keys.
    /// Interleaved presses share it, the last winning, which a hand cannot hear.
    private var rate: Float = 1

    /// The shipped click, loaded lazily at the first press so no view body wakes the audio hardware;
    /// `nil` when the resource is missing.
    public static let shipped = resources.flatMap { KeycapSound(named: "click", bundle: $0) }

    /// The app's Resources/Keycap folder, copied whole into the bundle; missing yields nil, never a crash.
    static let resources: Bundle? = {
        Bundle.main.resourceURL.flatMap { Bundle(url: $0.appendingPathComponent("Keycap")) }
    }()

    @MainActor public static var isMuted = false

    public init?(named name: String, extension ext: String = "wav", bundle: Bundle = .main) {
        func load(_ suffix: String) -> AVAudioPCMBuffer? {
            guard let url = bundle.url(forResource: "\(name)-\(suffix)", withExtension: ext),
                  let file = try? AVAudioFile(forReading: url),
                  let buffer = AVAudioPCMBuffer(pcmFormat: file.processingFormat,
                                                frameCapacity: AVAudioFrameCount(file.length)),
                  (try? file.read(into: buffer)) != nil
            else { return nil }
            return buffer
        }
        guard let down = load("down") else { return nil }
        self.down = down
        self.up = load("up")

        engine.attach(player)
        engine.attach(varispeed)
        engine.connect(player, to: varispeed, format: down.format)
        engine.connect(varispeed, to: engine.mainMixerNode, format: down.format)
    }

    public func press(jitterCents: Double, volume: Double) {
        // A ratio, because pitch is geometric: an additive jitter would be audible at one pitch only.
        let cents = Double.random(in: -jitterCents...jitterCents)
        rate = Float(pow(2, cents / 1200))
        play(down, volume: volume)
    }

    public func release(volume: Double) {
        guard let up else { return }
        play(up, volume: volume)
    }

    private func play(_ buffer: AVAudioPCMBuffer, volume: Double) {
        if !engine.isRunning {
            do { try engine.start() } catch { return }
        }
        varispeed.rate = rate
        player.volume = Float(volume)
        if !player.isPlaying { player.play() }
        // Without `.interrupts` the release queues behind the press instead of sounding at the lift;
        // the press lasts 65 ms against a 140 ms floor, so nothing audible is cut.
        player.scheduleBuffer(buffer, at: nil, options: .interrupts)
    }
}

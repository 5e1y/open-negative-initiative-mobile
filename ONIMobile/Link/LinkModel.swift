import Foundation
import Observation

/// The desktop this phone pairs with, kept in the Keychain to reconnect without the QR code.
struct PairedDesktop: Codable, Equatable {
    var key: Data
    var name: String
    var addresses: [String]
}

/// The link as the screens see it: one session at a time, its screen published on the main actor.
@MainActor
@Observable
final class LinkModel {
    enum Status: Equatable {
        case unpaired, connecting, connected, offline
    }

    private(set) var desktop: PairedDesktop?
    private(set) var state: ScannerState?
    private(set) var status: Status
    private(set) var roundTripMs: Double?
    private(set) var problem: String?
    /// True when the connected desktop announced a receiver for photos.
    private(set) var canImport = false

    @ObservationIgnored private var session: RemoteSession?
    @ObservationIgnored private var dialing: Task<Void, Never>?
    @ObservationIgnored private var active = false
    @ObservationIgnored private let phoneName: String

    init(phoneName: String) {
        self.phoneName = phoneName
        let stored = Keychain.desktop()
        desktop = stored
        status = stored == nil ? .unpaired : .offline
    }

    /// Pairs from a scanned or pasted code; the desktop is kept only once it has answered.
    @discardableResult
    func pair(uri: String) async -> Bool {
        let uri = uri.trimmingCharacters(in: .whitespacesAndNewlines)
        do {
            let info = try parsePairingCode(uri: uri)
            hangUp()
            status = .connecting
            let session = try await RemoteSession.pair(codeUri: uri, identity: Keychain.identity(), phoneName: phoneName)
            let paired = PairedDesktop(key: session.hostKey(), name: session.hostName(), addresses: info.addresses)
            Keychain.save(paired)
            desktop = paired
            attach(session)
            return true
        } catch LinkError.Failed {
            problem = "The desktop hung up: the code may have expired or been used. Show a fresh one."
        } catch {
            problem = Self.describe(error)
        }
        status = desktop == nil ? .unpaired : .offline
        return false
    }

    /// Dials the paired desktop until it answers, for as long as the app is in front.
    func resume() {
        active = true
        guard let desktop, session == nil, dialing == nil else { return }
        status = .connecting
        dialing = Task { await dial(desktop) }
    }

    /// Hangs up when the app leaves the screen, where iOS would suspend the socket anyway.
    func suspend() {
        active = false
        hangUp()
        if desktop != nil { status = .offline }
    }

    /// Drops the desktop and its key from this phone; the desktop keeps trusting it until revoked there.
    func forget() {
        hangUp()
        Keychain.forgetDesktop()
        desktop = nil
        state = nil
        roundTripMs = nil
        problem = nil
        status = .unpaired
    }

    func press(_ command: Command) {
        guard let session, status == .connected else { return }
        state = session.press(command: command)
    }

    /// Sends files byte for byte to the desktop, which files them without asking.
    func send(_ files: [URL]) async -> ImportOutcome {
        guard let session, canImport else {
            return ImportOutcome(sent: 0, skipped: 0, problem: "The desktop is not connected.")
        }
        do {
            let result = try await session.sendFiles(paths: files.map(\.path))
            return ImportOutcome(sent: result.sent.count, skipped: result.skipped.count, problem: nil)
        } catch {
            return ImportOutcome(sent: 0, skipped: 0, problem: Self.describe(error))
        }
    }

    // MARK: Session

    private func dial(_ desktop: PairedDesktop) async {
        var pause: UInt64 = 1
        while !Task.isCancelled {
            do {
                let session = try await RemoteSession.connect(
                    addresses: desktop.addresses, hostKey: desktop.key,
                    identity: Keychain.identity(), phoneName: phoneName)
                guard !Task.isCancelled else { return session.close() }
                dialing = nil
                attach(session)
                return
            } catch {
                problem = Self.describe(error)
                status = .offline
                // A desktop that revoked this phone refuses it every time: dialing again only drains the battery.
                if case LinkError.Refused = error { dialing = nil; return }
            }
            try? await Task.sleep(nanoseconds: pause * 1_000_000_000)
            pause = min(pause * 2, 8)
            status = .connecting
        }
    }

    private func attach(_ session: RemoteSession) {
        self.session = session
        problem = nil
        canImport = session.canSendFiles()
        apply(session.current(), from: session)
        session.setListener(listener: Relay { [weak self, weak session] display in
            MainActor.assumeIsolated {
                guard let self, let session else { return }
                self.apply(display, from: session)
            }
        })
    }

    /// A screen from a session already replaced is dropped; a lost connection dials again.
    private func apply(_ display: DisplayState, from source: RemoteSession) {
        guard source === session else { return }
        state = display.state
        roundTripMs = display.roundTripMs
        guard display.connected else {
            hangUp()
            status = .offline
            if active { resume() }
            return
        }
        status = .connected
    }

    private func hangUp() {
        dialing?.cancel()
        dialing = nil
        session?.close()
        session = nil
        canImport = false
    }

    static func describe(_ error: Error) -> String {
        switch error as? LinkError {
        case .BadCode?: "This is not an ONI pairing code."
        case .Version?: "The desktop runs another version of ONI Link: update both apps."
        case .Refused?: "The desktop no longer knows this phone. Forget it here, then pair again."
        case .Unreachable?: "The desktop does not answer. Check that both are on the same network."
        case .ImportUnavailable?: "The desktop does not take photos."
        case .ImportRefused?: "The desktop refused the photos. Pair again from the desktop's code."
        case .Failed(let message)?: message
        case nil: error.localizedDescription
        }
    }
}

/// One batch sent to the desktop, as the Import screen reports it.
struct ImportOutcome: Equatable {
    var sent: Int
    var skipped: Int
    var problem: String?
}

/// Hands each screen from the link thread to the main queue, in the order the link sent them.
final class Relay: DisplayListener {
    private let deliver: @Sendable (DisplayState) -> Void

    init(_ deliver: @escaping @Sendable (DisplayState) -> Void) {
        self.deliver = deliver
    }

    func onDisplay(display: DisplayState) {
        let deliver = deliver
        DispatchQueue.main.async { deliver(display) }
    }
}

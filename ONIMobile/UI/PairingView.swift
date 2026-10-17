import AVFoundation
import SwiftUI

/// The first meeting: the desktop's QR code through the camera, or its URI pasted.
struct PairingView: View {
    @Environment(LinkModel.self) private var link
    @State private var pasted = ""
    @State private var busy = false
    /// Bumped after a failed attempt so a fresh scanner reads the same code again.
    @State private var attempt = 0
    @State private var camera = AVCaptureDevice.authorizationStatus(for: .video)

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Space.md) {
            Text("ONI Mobile")
                .font(.title2.weight(.semibold))
            Text("Show ONI's pairing code on the desktop and hold the phone up to it. Both must be on the same network.")
                .font(.callout)
                .foregroundStyle(DS.Palette.textSecondary)
            viewfinder
            if let problem = link.problem {
                Text(problem)
                    .font(.footnote)
                    .foregroundStyle(DS.Palette.lampWait)
            }
            Spacer(minLength: 0)
            TextField("oni-link://pair?…", text: $pasted)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .font(.system(.footnote, design: .monospaced))
                .padding(DS.Space.sm)
                .background(DS.Palette.lcdGlass, in: DSShape.rounded(8))
            HStack(spacing: DS.Space.sm) {
                PasteButton(payloadType: String.self) { strings in
                    Task { @MainActor in pasted = strings.first ?? "" }
                }
                .labelStyle(.titleAndIcon)
                ScannerKeypad.cap("PAIR") { pair(pasted) }
                    .disabled(busy || pasted.isEmpty)
            }
        }
        .padding(DS.Space.md)
        .task {
            if camera == .notDetermined {
                camera = await AVCaptureDevice.requestAccess(for: .video) ? .authorized : .denied
            }
        }
    }

    @ViewBuilder
    private var viewfinder: some View {
        ZStack {
            DS.Palette.lcdGlass
            if camera == .authorized {
                QRScanner { pair($0) }
                    .id(attempt)
            } else if camera != .notDetermined {
                Text("The camera is off for ONI Mobile. Allow it in Settings, or paste the pairing link below.")
                    .font(.footnote)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(DS.Palette.textSecondary)
                    .padding(DS.Space.md)
            }
            if busy {
                ProgressView().controlSize(.large)
            }
        }
        .aspectRatio(1, contentMode: .fit)
        .clipShape(DSShape.rounded(16))
    }

    private func pair(_ uri: String) {
        guard !busy else { return }
        busy = true
        Task {
            if !(await link.pair(uri: uri)) { attempt += 1 }
            busy = false
        }
    }
}

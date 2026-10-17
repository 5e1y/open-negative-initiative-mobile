import AVFoundation
import SwiftUI
import UIKit

@main
struct ONIMobileApp: App {
    @State private var link = LinkModel(phoneName: UIDevice.current.name)
    @Environment(\.scenePhase) private var phase

    init() {
        // The keys' click mixes with other audio and obeys the silent switch.
        try? AVAudioSession.sharedInstance().setCategory(.ambient)
    }

    var body: some Scene {
        WindowGroup {
            Group {
                if link.desktop == nil {
                    PairingView()
                } else {
                    RemoteView()
                }
            }
            .environment(link)
            .preferredColorScheme(.dark)
            .tint(DS.Palette.lcdInk)
            .background(DS.Palette.canvas.ignoresSafeArea())
            // Dials on launch and on every return to the foreground; hangs up in the background.
            .onChange(of: phase, initial: true) { _, phase in
                switch phase {
                case .active: link.resume()
                case .background: link.suspend()
                default: break
                }
            }
        }
    }
}

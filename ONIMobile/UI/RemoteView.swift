import SwiftUI

/// The remote: the connection on top, the scanner's screen, the frame and machine keys, the keypad.
struct RemoteView: View {
    @Environment(LinkModel.self) private var link
    @State private var forgetting = false
    @State private var importing = false

    var body: some View {
        let state = link.state ?? Self.resting
        let live = link.status == .connected
        ScrollView {
            VStack(spacing: DS.Space.sm) {
                bar
                ScannerScreen(state: state)
                    .frame(height: 120)
                    .clipShape(DSShape.rounded(12))
                if let problem = link.problem, !live {
                    Text(problem)
                        .font(.footnote)
                        .foregroundStyle(DS.Palette.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                Group {
                    walk(state)
                    machine(state)
                    ScannerKeypad(state: state) { link.press($0) }
                }
                .disabled(!live)
            }
            .padding(.horizontal, DS.Space.sm)
        }
        .scrollBounceBehavior(.basedOnSize)
        .confirmationDialog("Forget \(link.desktop?.name ?? "this desktop")?", isPresented: $forgetting,
                            titleVisibility: .visible) {
            Button("Forget Desktop", role: .destructive) { link.forget() }
        } message: {
            Text("The phone will need the desktop's pairing code again.")
        }
        .sheet(isPresented: $importing) { ImportView() }
    }

    private var bar: some View {
        HStack(spacing: DS.Space.sm) {
            Circle()
                .fill(lamp)
                .frame(width: DS.Scanner.lampSize, height: DS.Scanner.lampSize)
                .shadow(color: lamp, radius: link.status == .connected ? 3 : 0)
            Text(link.desktop?.name ?? "Desktop")
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
            Text(reading)
                .font(.system(.footnote, design: .monospaced))
                .foregroundStyle(DS.Palette.textSecondary)
            Spacer(minLength: DS.Space.sm)
            ScannerKeypad.cap("IMPORT", width: DS.Scanner.wideKeyWidth) { importing = true }
                .disabled(!link.canImport)
            Menu {
                Button("Forget Desktop", role: .destructive) { forgetting = true }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .font(.title3)
                    .foregroundStyle(DS.Palette.lcdInk)
                    .frame(width: 32, height: 32)
            }
            .accessibilityLabel("More")
        }
    }

    private var lamp: Color {
        switch link.status {
        case .connected: DS.Palette.lampOn
        case .connecting: DS.Palette.lampWait
        case .offline, .unpaired: DS.Palette.lampOff
        }
    }

    private var reading: String {
        switch link.status {
        case .connected: link.roundTripMs.map { String(format: "%.0f ms", max(1, $0.rounded())) } ?? "—"
        case .connecting: "CONNECTING"
        case .offline, .unpaired: "OFFLINE"
        }
    }

    /// Previous and next frame, then undo and redo, each answered by the desktop's own history.
    private func walk(_ state: ScannerState) -> some View {
        KeyStack {
            HStack(spacing: 0) {
                ScannerKeypad.icon(.chevronLeft, name: "Previous frame") { link.press(.walk(by: -1)) }
                    .disabled(state.frame == nil)
                ScannerKeypad.icon(.chevronRight, name: "Next frame") { link.press(.walk(by: 1)) }
                    .disabled(state.frame == nil)
                ScannerKeypad.icon(.undo, name: "Undo") { link.press(.undo) }
                    .disabled(!state.canUndo)
                ScannerKeypad.icon(.redo, name: "Redo") { link.press(.redo) }
                    .disabled(!state.canRedo)
            }
        }
    }

    /// The two machines as two keys, a lamp lit on the one in use.
    private func machine(_ state: ScannerState) -> some View {
        KeyStack {
            HStack(spacing: 0) {
                ForEach([Machine.oni, .frontier], id: \.self) { machine in
                    let lit = state.machine == machine
                    ScannerKeypad.cap(ScannerText.machine(machine)) { link.press(.setMachine(machine: machine)) }
                        .keycapOrnament(.trailing) {
                            Circle()
                                .fill(lit ? DS.Palette.lcdInk : DS.Palette.lampOff)
                                .frame(width: DS.Scanner.lampSize, height: DS.Scanner.lampSize)
                                .shadow(color: DS.Palette.lcdInk, radius: lit ? 3 : 0)
                        }
                        .accessibilityAddTraits(lit ? .isSelected : [])
                }
            }
        }
    }

    /// What the screen shows before the desktop's first answer.
    static let resting = ScannerState(
        revision: 0, applied: 0, frame: nil, machine: .oni, channel: .negative,
        clicks: Clicks(d: 0, c: 0, m: 0, y: 0, w: 0), contrast: 0, vignette: 0, chrome: 0,
        remeasure: false, canUndo: false, canRedo: false)
}

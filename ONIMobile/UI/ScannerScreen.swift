import SwiftUI

/// The scanner's screen, as the desktop's `LabScreen`: the frame, the machine and channel, every
/// letter's count, then the three effects.
struct ScannerScreen: View {
    let state: ScannerState

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text(ScannerText.frame(state.frame))
                Spacer()
                if state.remeasure {
                    Text("REMEASURE ROLL")
                    Spacer()
                }
                Text(ScannerText.machine(state.machine) + " · " + ScannerText.channel(state.channel))
            }
            .font(.system(size: DS.Scanner.screenLabelSize, weight: .medium, design: .monospaced))
            Spacer(minLength: DS.Space.xs)
            HStack(spacing: DS.Space.sm) {
                ForEach(Letter.allCases, id: \.self) { letter in
                    cell(letter.title, ScannerText.spelled(state.clicks[letter]))
                }
            }
            Spacer(minLength: DS.Space.xs)
            HStack {
                Text("CT " + ScannerText.percent(state.contrast))
                Spacer()
                Text("VIG " + ScannerText.stops(state.vignette))
                if let chrome = state.chrome {
                    Spacer()
                    Text("CHR " + ScannerText.percent(chrome))
                }
            }
            .font(.system(size: DS.Scanner.screenLabelSize, weight: .medium, design: .monospaced))
        }
        .padding(DS.Space.md)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .foregroundStyle(DS.Palette.lcdInk)
        .shadow(color: DS.Palette.lcdInk.opacity(0.8), radius: DS.Scanner.phosphorGlow)
        .background(DS.Palette.lcdGlass)
        .overlay {
            Canvas { context, size in
                var y: CGFloat = 0
                while y < size.height {
                    context.fill(Path(CGRect(x: 0, y: y, width: size.width, height: DS.Stroke.line)),
                                 with: .color(.black.opacity(0.35)))
                    y += DS.Scanner.scanlinePitch
                }
            }
            .allowsHitTesting(false)
        }
        .accessibilityElement(children: .combine)
    }

    private func cell(_ title: String, _ value: String) -> some View {
        VStack(spacing: DS.Space.xxs) {
            Text(title)
                .font(.system(size: DS.Scanner.screenLabelSize, weight: .medium, design: .monospaced))
            Text(value)
                .font(.system(size: DS.Scanner.screenValueSize, weight: .bold, design: .monospaced))
                .lineLimit(1)
                .minimumScaleFactor(0.5)
        }
        .frame(maxWidth: .infinity)
    }
}

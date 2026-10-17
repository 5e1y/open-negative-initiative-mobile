import Photos
import PhotosUI
import SwiftUI
import UIKit

/// Sends photos from the library to the desktop, originals only, one batch per pick.
struct ImportView: View {
    @Environment(LinkModel.self) private var link
    @Environment(\.dismiss) private var dismiss
    @State private var picking = false
    @State private var working: String?
    @State private var batches: [Batch] = []

    struct Batch: Identifiable {
        let id = UUID()
        let count: Int
        let outcome: ImportOutcome
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: DS.Space.md) {
                Text("Pick photos to send to \(link.desktop?.name ?? "the desktop"). Each goes as its original file: a ProRAW scan arrives as its DNG.")
                    .font(.callout)
                    .foregroundStyle(DS.Palette.textSecondary)
                ScannerKeypad.cap("CHOOSE PHOTOS") { choose() }
                    .disabled(working != nil || !link.canImport)
                if let working {
                    HStack(spacing: DS.Space.sm) {
                        ProgressView()
                        Text(working).monospacedDigit()
                    }
                }
                if !link.canImport {
                    Text(link.status == .connected ? "This desktop does not take photos." : "Connect to the desktop first.")
                        .font(.footnote)
                        .foregroundStyle(DS.Palette.textSecondary)
                }
                List(batches) { batch in
                    VStack(alignment: .leading, spacing: DS.Space.xxs) {
                        Text(summary(batch)).font(.body.monospacedDigit())
                        if let problem = batch.outcome.problem {
                            Text(problem).font(.footnote).foregroundStyle(DS.Palette.textSecondary)
                        }
                    }
                    .listRowBackground(DS.Palette.lcdGlass)
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
            }
            .padding(DS.Space.md)
            .background(DS.Palette.canvas)
            .navigationTitle("Import")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } }
            }
        }
        .sheet(isPresented: $picking) {
            PhotoPicker { results in
                picking = false
                Task { await send(results) }
            }
            .ignoresSafeArea()
        }
    }

    /// Full library access lets the asset's raw resource be read; without it the picker's file is used.
    private func choose() {
        Task {
            if PHPhotoLibrary.authorizationStatus(for: .readWrite) == .notDetermined {
                _ = await PHPhotoLibrary.requestAuthorization(for: .readWrite)
            }
            picking = true
        }
    }

    private func send(_ results: [PHPickerResult]) async {
        guard !results.isEmpty else { return }
        // A batch of DNGs outlasts a glance away from the phone: iOS grants it time to finish.
        var background = UIBackgroundTaskIdentifier.invalid
        background = UIApplication.shared.beginBackgroundTask(withName: "Import") {
            UIApplication.shared.endBackgroundTask(background)
        }
        let folder = FileManager.default.temporaryDirectory.appendingPathComponent("import-\(UUID().uuidString)")
        let total = results.count
        let outcome: ImportOutcome
        do {
            try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
            working = "Reading 0 of \(total)"
            let files = try await PhotoExport.originals(results, into: folder) { done in
                working = "Reading \(done) of \(total)"
            }
            working = "Sending \(total)"
            outcome = await link.send(files)
        } catch {
            outcome = ImportOutcome(sent: 0, skipped: 0, problem: "The photos could not be read: \(error.localizedDescription)")
        }
        try? FileManager.default.removeItem(at: folder)
        batches.insert(Batch(count: total, outcome: outcome), at: 0)
        working = nil
        UIApplication.shared.endBackgroundTask(background)
    }

    private func summary(_ batch: Batch) -> String {
        let sent = "\(batch.outcome.sent) of \(batch.count) sent"
        return batch.outcome.skipped > 0 ? sent + ", \(batch.outcome.skipped) declined" : sent
    }
}

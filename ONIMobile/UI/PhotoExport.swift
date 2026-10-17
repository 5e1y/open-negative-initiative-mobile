import Photos
import PhotosUI
import SwiftUI
import UniformTypeIdentifiers

/// Writes each picked photo's original file into a folder: the camera's own bytes, the DNG of a
/// ProRAW or RAW+JPEG asset, never a rendition, since the desktop converts from the raw data.
enum PhotoExport {
    static func originals(_ results: [PHPickerResult], into folder: URL,
                          progress: @MainActor (Int) -> Void) async throws -> [URL] {
        var files: [URL] = []
        for result in results {
            files.append(try await original(result, into: folder))
            await progress(files.count)
        }
        return files
    }

    /// Through the asset's resources when the library is readable, else through the picker's own file.
    private static func original(_ result: PHPickerResult, into folder: URL) async throws -> URL {
        if let id = result.assetIdentifier,
           let asset = PHAsset.fetchAssets(withLocalIdentifiers: [id], options: nil).firstObject,
           let resource = rawest(PHAssetResource.assetResources(for: asset)) {
            let destination = unique(resource.originalFilename, in: folder)
            let options = PHAssetResourceRequestOptions()
            options.isNetworkAccessAllowed = true
            try await withCheckedThrowingContinuation { (done: CheckedContinuation<Void, Error>) in
                PHAssetResourceManager.default().writeData(for: resource, toFile: destination, options: options) { error in
                    if let error { done.resume(throwing: error) } else { done.resume() }
                }
            }
            return destination
        }
        return try await providedFile(result.itemProvider, into: folder)
    }

    /// The raw resource first, then the original photo; edits and renditions are other resource types.
    static func rawest(_ resources: [PHAssetResource]) -> PHAssetResource? {
        let raw = resources.first { resource in
            [.photo, .alternatePhoto].contains(resource.type)
                && UTType(resource.uniformTypeIdentifier)?.conforms(to: .rawImage) == true
        }
        return raw ?? resources.first { $0.type == .photo } ?? resources.first
    }

    /// The picker's file in its current representation, a raw type preferred, copied before iOS deletes it.
    private static func providedFile(_ provider: NSItemProvider, into folder: URL) async throws -> URL {
        let types = provider.registeredTypeIdentifiers
        let raw = types.first { UTType($0)?.conforms(to: .rawImage) == true }
        guard let type = raw ?? types.first(where: { UTType($0)?.conforms(to: .image) == true }) else {
            throw CocoaError(.fileReadUnsupportedScheme)
        }
        return try await withCheckedThrowingContinuation { (done: CheckedContinuation<URL, Error>) in
            _ = provider.loadFileRepresentation(forTypeIdentifier: type) { url, error in
                guard let url else { return done.resume(throwing: error ?? CocoaError(.fileNoSuchFile)) }
                let destination = unique(url.lastPathComponent, in: folder)
                do {
                    try FileManager.default.copyItem(at: url, to: destination)
                    done.resume(returning: destination)
                } catch {
                    done.resume(throwing: error)
                }
            }
        }
    }

    /// Two picks of the same name from different albums both reach the desktop.
    private static func unique(_ name: String, in folder: URL) -> URL {
        let stem = (name as NSString).deletingPathExtension
        let ext = (name as NSString).pathExtension
        var candidate = folder.appendingPathComponent(name)
        var n = 2
        while FileManager.default.fileExists(atPath: candidate.path) {
            candidate = folder.appendingPathComponent(ext.isEmpty ? "\(stem)-\(n)" : "\(stem)-\(n).\(ext)")
            n += 1
        }
        return candidate
    }
}

/// The system photo picker, many at a time, handing back assets in their current representation.
struct PhotoPicker: UIViewControllerRepresentable {
    let picked: ([PHPickerResult]) -> Void

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var configuration = PHPickerConfiguration(photoLibrary: .shared())
        configuration.filter = .images
        configuration.selectionLimit = 0
        configuration.preferredAssetRepresentationMode = .current
        let picker = PHPickerViewController(configuration: configuration)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ picker: PHPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(picked: picked) }

    final class Coordinator: NSObject, PHPickerViewControllerDelegate {
        let picked: ([PHPickerResult]) -> Void

        init(picked: @escaping ([PHPickerResult]) -> Void) {
            self.picked = picked
        }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            picked(results)
        }
    }
}

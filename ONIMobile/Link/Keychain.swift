import Foundation
import Security

/// The phone's identity and its paired desktop, as generic passwords kept on this device only:
/// a restored backup on another phone must pair again rather than carry the key across.
enum Keychain {
    private static let service = "fr.courtillon.oni-mobile.link"

    /// The phone's key pair, made and stored on first use.
    static func identity() -> Data {
        if let stored = read("identity") { return stored }
        let made = generateIdentity()
        write(made, to: "identity")
        return made
    }

    static func desktop() -> PairedDesktop? {
        read("desktop").flatMap { try? JSONDecoder().decode(PairedDesktop.self, from: $0) }
    }

    static func save(_ desktop: PairedDesktop) {
        guard let data = try? JSONEncoder().encode(desktop) else { return }
        write(data, to: "desktop")
    }

    static func forgetDesktop() {
        SecItemDelete(query("desktop") as CFDictionary)
    }

    // MARK: Items

    private static func query(_ account: String) -> [String: Any] {
        [kSecClass as String: kSecClassGenericPassword,
         kSecAttrService as String: service,
         kSecAttrAccount as String: account]
    }

    private static func read(_ account: String) -> Data? {
        var request = query(account)
        request[kSecReturnData as String] = true
        request[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        guard SecItemCopyMatching(request as CFDictionary, &result) == errSecSuccess else { return nil }
        return result as? Data
    }

    private static func write(_ data: Data, to account: String) {
        SecItemDelete(query(account) as CFDictionary)
        var item = query(account)
        item[kSecValueData as String] = data
        item[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        SecItemAdd(item as CFDictionary, nil)
    }
}

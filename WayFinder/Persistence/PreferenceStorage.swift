import Foundation

enum PreferenceStorage {
    private static let preferenceIdKey = "com.wayfinder.preference.id"
    
    static func savePreferenceId(_ id: String) {
        UserDefaults.standard.set(id, forKey: preferenceIdKey)
    }
    
    static func fetchPreferenceId() -> String? {
        UserDefaults.standard.string(forKey: preferenceIdKey)
    }
    
    static func clearPreferenceId() {
        UserDefaults.standard.removeObject(forKey: preferenceIdKey)
    }
}

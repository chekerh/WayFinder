import Foundation
import SwiftUI
import ObjectiveC
import UIKit

enum AppLanguage: String, CaseIterable, Identifiable {
    case french = "fr"
    case english = "en"
    
    var id: String { rawValue }
    
    var localizedNameKey: LocalizedStringKey {
        switch self {
        case .french: return "language_french"
        case .english: return "language_english"
        }
    }
    
    var locale: Locale {
        Locale(identifier: rawValue)
    }
    
    var layoutDirection: LayoutDirection {
        .leftToRight
    }
}

final class LanguageManager: ObservableObject {
    private let storageKey = "WayFinder.selectedLanguage"
    @Published var selectedLanguage: AppLanguage {
        didSet { applyLanguage() }
    }
    
    init() {
        if let saved = UserDefaults.standard.string(forKey: storageKey),
           let language = AppLanguage(rawValue: saved) {
            selectedLanguage = language
        } else {
            selectedLanguage = .french
        }
        applyLanguage(syncDefaults: false)
    }
    
    var locale: Locale { selectedLanguage.locale }
    var layoutDirection: LayoutDirection { selectedLanguage.layoutDirection }
    
    private func applyLanguage(syncDefaults: Bool = true) {
        Bundle.setLanguage(selectedLanguage.rawValue)
        if syncDefaults {
            UserDefaults.standard.set(selectedLanguage.rawValue, forKey: storageKey)
        }
        UIView.appearance().semanticContentAttribute = .forceLeftToRight
        
        // Notifier toutes les vues du changement de langue
        DispatchQueue.main.async {
            self.objectWillChange.send()
            // Envoyer une notification pour forcer le rafraîchissement
            NotificationCenter.default.post(name: NSNotification.Name("LanguageDidChange"), object: nil)
        }
    }
}

private var bundleKey: UInt8 = 0

private class LocalizedBundle: Bundle, @unchecked Sendable {
    override func localizedString(forKey key: String, value: String?, table tableName: String?) -> String {
        if let bundle = objc_getAssociatedObject(self, &bundleKey) as? Bundle {
            return bundle.localizedString(forKey: key, value: value, table: tableName)
        }
        return super.localizedString(forKey: key, value: value, table: tableName)
    }
}

extension Bundle {
    static func setLanguage(_ language: String) {
        guard let path = Bundle.main.path(forResource: language, ofType: "lproj"),
              let languageBundle = Bundle(path: path) else {
            objc_setAssociatedObject(Bundle.main, &bundleKey, nil, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
            return
        }
        objc_setAssociatedObject(Bundle.main, &bundleKey, languageBundle, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        object_setClass(Bundle.main, LocalizedBundle.self)
    }
}


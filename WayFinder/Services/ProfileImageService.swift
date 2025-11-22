import Foundation
import SwiftUI

/// Service centralisé pour gérer l'état et la persistance de l'image de profil
@MainActor
final class ProfileImageService: ObservableObject {
    static let shared = ProfileImageService()
    
    @Published var profileImageUrl: String? {
        didSet {
            if let url = profileImageUrl, !url.contains("pravatar.cc") {
                // Ne pas sauvegarder l'image de test
                UserStorage.saveProfile(displayName: nil, profileImageUrl: url, email: currentEmail)
                // Forcer la synchronisation
                UserDefaults.standard.set(url, forKey: UserStorage.profileImageUrlKey)
                UserDefaults.standard.synchronize()
                
                // Notifier tous les observateurs
                NotificationCenter.default.post(
                    name: NSNotification.Name("UserProfileImageDidUpdate"),
                    object: nil,
                    userInfo: ["profileImageUrl": url]
                )
            } else if let url = profileImageUrl, url.contains("pravatar.cc") {
                // Si c'est une image de test, ne pas la sauvegarder et la supprimer
                profileImageUrl = nil
            }
        }
    }
    
    @Published var currentEmail: String?
    
    private var observers: [NSObjectProtocol] = []
    
    private init() {
        // Supprimer l'image de test si elle existe avant de charger
        clearTestImageIfExists()
        // Charger l'image persistée au démarrage
        Task { @MainActor in
            self.loadPersistedImage()
        }
        
        // Observer les changements dans UserDefaults
        let observer = NotificationCenter.default.addObserver(
            forName: UserDefaults.didChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor [weak self] in
                self?.loadPersistedImage()
            }
        }
        observers.append(observer)
        
        // Observer les notifications de mise à jour
        let imageObserver = NotificationCenter.default.addObserver(
            forName: NSNotification.Name("UserProfileImageDidUpdate"),
            object: nil,
            queue: .main
        ) { [weak self] notification in
            Task { @MainActor [weak self] in
                if let newUrl = notification.userInfo?["profileImageUrl"] as? String {
                    if self?.profileImageUrl != newUrl {
                        self?.profileImageUrl = newUrl
                    }
                }
            }
        }
        observers.append(imageObserver)
    }
    
    deinit {
        observers.forEach { NotificationCenter.default.removeObserver($0) }
    }
    
    /// Charge l'image persistée depuis UserStorage
    func loadPersistedImage() {
        let storedUrl = UserStorage.fetchProfileImageUrl()
        // Ne pas charger l'image de test (pravatar.cc)
        if let storedUrl = storedUrl, !storedUrl.isEmpty, !storedUrl.contains("pravatar.cc") {
            if profileImageUrl != storedUrl {
                profileImageUrl = storedUrl
                print("✅ [ProfileImageService] Loaded stored image: \(storedUrl)")
            }
        } else if let storedUrl = storedUrl, storedUrl.contains("pravatar.cc") {
            // Supprimer l'image de test si elle existe
            clearTestImage()
        }
        currentEmail = UserDefaults.standard.string(forKey: UserStorage.userEmailKey)
    }
    
    /// Supprime l'image de test si elle existe (appelé au démarrage)
    private func clearTestImageIfExists() {
        let defaults = UserDefaults.standard
        let storedUrl = defaults.string(forKey: UserStorage.profileImageUrlKey)
        
        // Si l'image stockée est l'image de test, la supprimer
        if let url = storedUrl, url.contains("pravatar.cc") {
            defaults.removeObject(forKey: UserStorage.profileImageUrlKey)
            
            // Supprimer aussi les images liées aux emails si ce sont des images de test
            if let email = defaults.string(forKey: UserStorage.userEmailKey) {
                let emailKey = "\(UserStorage.userImageUrlPrefix)\(email)"
                if let emailImageUrl = defaults.string(forKey: emailKey), emailImageUrl.contains("pravatar.cc") {
                    defaults.removeObject(forKey: emailKey)
                }
            }
            
            // Supprimer toutes les clés d'images de test
            let keys = defaults.dictionaryRepresentation().keys
            for key in keys {
                if key.hasPrefix(UserStorage.userImageUrlPrefix) {
                    if let imageUrl = defaults.string(forKey: key), imageUrl.contains("pravatar.cc") {
                        defaults.removeObject(forKey: key)
                    }
                }
            }
            
            defaults.synchronize()
            profileImageUrl = nil
            print("🗑️ [ProfileImageService] Removed test image from storage")
        }
    }
    
    /// Supprime l'image de test si elle existe
    private func clearTestImage() {
        clearTestImageIfExists()
    }
    
    /// Met à jour l'image de profil
    func updateProfileImage(_ imageUrl: String?, email: String? = nil) {
        if let email = email {
            currentEmail = email
        }
        profileImageUrl = imageUrl
    }
    
    /// Restaure l'image après reconnexion
    func restoreAfterLogin(email: String) {
        currentEmail = email
        // Essayer de restaurer l'image liée à cet email
        if let imageUrl = UserStorage.fetchProfileImageUrl() {
            profileImageUrl = imageUrl
        }
    }
}


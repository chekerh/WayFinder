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

enum UserStorage {
    static let displayNameKey = "com.wayfinder.user.displayName"
    static let profileImageUrlKey = "com.wayfinder.user.profileImageUrl"
    static let userEmailKey = "com.wayfinder.user.email"
    static let userImageUrlPrefix = "com.wayfinder.user.profileImageUrl."
    
    static func saveProfile(_ profile: UserProfile) {
        saveProfile(displayName: profile.displayNameValue, profileImageUrl: profile.resolvedProfileImageUrl, email: profile.email)
    }
    
    static func saveProfile(displayName: String?, profileImageUrl: String?, email: String? = nil) {
        let defaults = UserDefaults.standard
        
        // Sauvegarder le nom
        if let name = displayName {
            defaults.set(name, forKey: displayNameKey)
        } else {
            defaults.removeObject(forKey: displayNameKey)
        }
        
        // Sauvegarder l'email si fourni
        if let email = email {
            defaults.set(email, forKey: userEmailKey)
        }
        
        // Sauvegarder l'image avec deux clés : une globale et une liée à l'email
        if let imageUrl = profileImageUrl {
            // Clé globale pour accès rapide
            defaults.set(imageUrl, forKey: profileImageUrlKey)
            
            // Clé liée à l'email pour persistance après déconnexion
            if let email = email ?? defaults.string(forKey: userEmailKey) {
                let emailKey = "\(userImageUrlPrefix)\(email)"
                defaults.set(imageUrl, forKey: emailKey)
                print("✅ [UserStorage] Saved profile image URL for email \(email): \(imageUrl)")
            } else {
                defaults.set(imageUrl, forKey: profileImageUrlKey)
                print("✅ [UserStorage] Saved profile image URL: \(imageUrl)")
            }
            
            // Forcer la synchronisation immédiate
            defaults.synchronize()
        } else {
            // Ne supprimer que la clé globale, pas celle liée à l'email
            defaults.removeObject(forKey: profileImageUrlKey)
            defaults.synchronize()
        }
    }
    
    static func fetchDisplayName() -> String? {
        UserDefaults.standard.string(forKey: displayNameKey)
    }
    
    static func fetchProfileImageUrl() -> String? {
        let defaults = UserDefaults.standard
        
        // Essayer d'abord la clé globale (pour compatibilité)
        if let imageUrl = defaults.string(forKey: profileImageUrlKey) {
            return imageUrl
        }
        
        // Sinon, essayer de récupérer depuis l'email stocké
        if let email = defaults.string(forKey: userEmailKey) {
            let emailKey = "\(userImageUrlPrefix)\(email)"
            if let imageUrl = defaults.string(forKey: emailKey) {
                // Restaurer dans la clé globale
                defaults.set(imageUrl, forKey: profileImageUrlKey)
                defaults.synchronize()
                return imageUrl
            }
        }
        
        return nil
    }
    
    static func clear() {
        let defaults = UserDefaults.standard
        // Ne supprimer que le nom, pas l'image de profil ni l'email
        defaults.removeObject(forKey: displayNameKey)
        // NE PAS supprimer profileImageUrlKey pour garder l'image après déconnexion
        // defaults.removeObject(forKey: profileImageUrlKey)
        // NE PAS supprimer userEmailKey pour pouvoir récupérer l'image
        // defaults.removeObject(forKey: userEmailKey)
    }
    
    static func clearAll() {
        // Fonction pour tout supprimer (si nécessaire)
        let defaults = UserDefaults.standard
        defaults.removeObject(forKey: displayNameKey)
        defaults.removeObject(forKey: profileImageUrlKey)
        defaults.removeObject(forKey: userEmailKey)
        
        // Supprimer toutes les images liées aux emails
        let keys = defaults.dictionaryRepresentation().keys
        for key in keys {
            if key.hasPrefix(userImageUrlPrefix) {
                defaults.removeObject(forKey: key)
            }
        }
        defaults.synchronize()
    }
}

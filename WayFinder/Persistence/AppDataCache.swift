import Foundation

/// Cache centralisé pour toutes les données de l'application
final class AppDataCache {
    static let shared = AppDataCache()
    private init() {}
    
    // MARK: - Caches spécifiques
    
    /// Cache pour les journeys (expire après 1 heure)
    lazy var journeysCache: DataCache<[Journey]> = {
        DataCache<[Journey]>(key: "com.wayfinder.cache.journeys", expirationInterval: 3600)
    }()
    
    /// Cache pour les posts de discussion (expire après 30 minutes)
    lazy var postsCache: DataCache<[DiscussionPost]> = {
        DataCache<[DiscussionPost]>(key: "com.wayfinder.cache.posts", expirationInterval: 1800)
    }()
    
    /// Cache pour les recommandations (expire après 2 heures)
    lazy var recommendationsCache: DataCache<PersonalizedRecommendationsPayload> = {
        DataCache<PersonalizedRecommendationsPayload>(key: "com.wayfinder.cache.recommendations", expirationInterval: 7200)
    }()
    
    /// Cache pour les notifications (expire après 15 minutes)
    lazy var notificationsCache: DataCache<[Notification]> = {
        DataCache<[Notification]>(key: "com.wayfinder.cache.notifications", expirationInterval: 900)
    }()
    
    /// Cache pour le profil utilisateur (expire après 1 jour)
    lazy var userProfileCache: DataCache<UserProfile> = {
        DataCache<UserProfile>(key: "com.wayfinder.cache.user_profile", expirationInterval: 86400)
    }()
    
    /// Cache pour les favoris (expire après 1 heure)
    lazy var favoritesCache: DataCache<[String: Bool]> = {
        DataCache<[String: Bool]>(key: "com.wayfinder.cache.favorites", expirationInterval: 3600)
    }()
    
    /// Cache pour les hôtels par destination et type (expire après 2 heures)
    /// Clé: "destination_type" (ex: "BCN_hotel")
    func hotelsCache(for destination: String, type: String) -> DataCache<[Accommodation]> {
        let cacheKey = "com.wayfinder.cache.hotels.\(destination.lowercased()).\(type.lowercased())"
        return DataCache<[Accommodation]>(key: cacheKey, expirationInterval: 7200)
    }
    
    /// Cache pour les mémoires de la carte (expire après 1 heure)
    lazy var mapMemoriesCache: DataCache<MapMemoriesResponse> = {
        DataCache<MapMemoriesResponse>(key: "com.wayfinder.cache.map_memories", expirationInterval: 3600)
    }()
    
    // MARK: - Méthodes utilitaires
    
    /// Vide tous les caches
    func clearAll() {
        journeysCache.clear()
        postsCache.clear()
        recommendationsCache.clear()
        notificationsCache.clear()
        userProfileCache.clear()
        favoritesCache.clear()
        mapMemoriesCache.clear()
        
        // Vider tous les caches d'hôtels
        let defaults = UserDefaults.standard
        let keys = defaults.dictionaryRepresentation().keys
        for key in keys {
            if key.hasPrefix("com.wayfinder.cache.hotels.") {
                defaults.removeObject(forKey: key)
                defaults.removeObject(forKey: "\(key)_updated_at")
            }
        }
        defaults.synchronize()
        
        print("🗑️ [AppDataCache] Cleared all caches")
    }
    
    /// Vide les caches expirés
    func clearExpired() {
        // Les caches gèrent eux-mêmes l'expiration lors du chargement
        // Cette méthode peut être utilisée pour un nettoyage proactif si nécessaire
        print("🧹 [AppDataCache] Expired caches will be ignored on next load")
    }
}


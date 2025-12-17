import Foundation

/// Cache générique pour stocker n'importe quel type Codable avec expiration optionnelle
final class DataCache<T: Codable> {
    private let cacheKey: String
    private let expirationInterval: TimeInterval?
    
    /// Initialise un cache avec une clé et une durée d'expiration optionnelle
    /// - Parameters:
    ///   - key: Clé unique pour ce cache
    ///   - expirationInterval: Durée en secondes avant expiration (nil = pas d'expiration)
    init(key: String, expirationInterval: TimeInterval? = nil) {
        self.cacheKey = key
        self.expirationInterval = expirationInterval
    }
    
    /// Sauvegarde les données dans le cache
    func store(_ data: T) {
        do {
            let encoder = JSONEncoder()
            encoder.dateEncodingStrategy = .iso8601
            let encoded = try encoder.encode(data)
            
            UserDefaults.standard.set(encoded, forKey: cacheKey)
            
            // Sauvegarder la date de mise à jour si expiration activée
            if expirationInterval != nil {
                UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: "\(cacheKey)_updated_at")
            }
            
            UserDefaults.standard.synchronize()
            print("💾 [DataCache] Stored data for key: \(cacheKey)")
        } catch {
            print("❌ [DataCache] Error storing data for key \(cacheKey): \(error)")
        }
    }
    
    /// Charge les données depuis le cache
    /// - Returns: Les données en cache si disponibles et non expirées, nil sinon
    func load() -> T? {
        guard let data = UserDefaults.standard.data(forKey: cacheKey) else {
            print("⚠️ [DataCache] No cached data for key: \(cacheKey)")
            return nil
        }
        
        // Vérifier l'expiration si activée
        if let expirationInterval = expirationInterval {
            let updatedAt = UserDefaults.standard.double(forKey: "\(cacheKey)_updated_at")
            if updatedAt == 0 {
                print("⚠️ [DataCache] No update timestamp for key: \(cacheKey)")
                return nil
            }
            
            let age = Date().timeIntervalSince1970 - updatedAt
            if age > expirationInterval {
                print("⚠️ [DataCache] Cache expired for key: \(cacheKey) (age: \(Int(age))s, max: \(Int(expirationInterval))s)")
                return nil
            }
        }
        
        do {
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            let decoded = try decoder.decode(T.self, from: data)
            print("✅ [DataCache] Loaded cached data for key: \(cacheKey)")
            return decoded
        } catch {
            print("❌ [DataCache] Error decoding cached data for key \(cacheKey): \(error)")
            return nil
        }
    }
    
    /// Vérifie si le cache existe et n'est pas expiré
    func hasValidCache() -> Bool {
        guard UserDefaults.standard.data(forKey: cacheKey) != nil else {
            return false
        }
        
        if let expirationInterval = expirationInterval {
            let updatedAt = UserDefaults.standard.double(forKey: "\(cacheKey)_updated_at")
            if updatedAt == 0 {
                return false
            }
            
            let age = Date().timeIntervalSince1970 - updatedAt
            return age <= expirationInterval
        }
        
        return true
    }
    
    /// Supprime le cache
    func clear() {
        UserDefaults.standard.removeObject(forKey: cacheKey)
        UserDefaults.standard.removeObject(forKey: "\(cacheKey)_updated_at")
        UserDefaults.standard.synchronize()
        print("🗑️ [DataCache] Cleared cache for key: \(cacheKey)")
    }
    
    /// Retourne l'âge du cache en secondes
    func cacheAge() -> TimeInterval? {
        let updatedAt = UserDefaults.standard.double(forKey: "\(cacheKey)_updated_at")
        guard updatedAt > 0 else { return nil }
        return Date().timeIntervalSince1970 - updatedAt
    }
}


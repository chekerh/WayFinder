import Foundation
import Combine

@MainActor
final class MapMemoriesViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var mapMemories: MapMemoriesResponse?
    @Published var googleMapsApiKey: String?
    
    private let socialService = SocialService.shared
    
    func loadMapMemories() async {
        guard !isLoading else { return }
        
        // Essayer de charger depuis le cache d'abord
        if let cachedMemories = AppDataCache.shared.mapMemoriesCache.load() {
            print("✅ [MapMemoriesViewModel] Loaded \(cachedMemories.countries.count) countries, \(cachedMemories.totalMemories) total memories from cache")
            mapMemories = cachedMemories
            
            // Mettre à jour en arrière-plan sans bloquer
            Task {
                do {
                    let freshMemories = try await socialService.getMapMemories()
                    AppDataCache.shared.mapMemoriesCache.store(freshMemories)
                    await MainActor.run {
                        mapMemories = freshMemories
                    }
                    print("✅ [MapMemoriesViewModel] Updated cache with \(freshMemories.countries.count) countries, \(freshMemories.totalMemories) total memories")
                } catch {
                    print("⚠️ [MapMemoriesViewModel] Failed to update cache: \(error.localizedDescription)")
                }
            }
            return
        }
        
        // Vérifier si un token est présent avant de faire l'appel
        guard let token = TokenStorage.fetch(), !token.isEmpty else {
            print("⚠️ [MapMemoriesViewModel] No token found, cannot load map memories")
            errorMessage = "Non autorisé. Veuillez vous connecter."
            isLoading = false
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        do {
            let response = try await socialService.getMapMemories()
            mapMemories = response
            // Sauvegarder dans le cache
            AppDataCache.shared.mapMemoriesCache.store(response)
            print("✅ [MapMemoriesViewModel] Loaded \(response.countries.count) countries, \(response.totalMemories) total memories")
        } catch let error as DecodingError {
            var detailedError = "Erreur de décodage JSON: "
            switch error {
            case .typeMismatch(let type, let context):
                detailedError += "Type mismatch for \(type) at path \(context.codingPath.map { $0.stringValue }.joined(separator: "."))"
            case .valueNotFound(let type, let context):
                detailedError += "Value not found for \(type) at path \(context.codingPath.map { $0.stringValue }.joined(separator: "."))"
            case .keyNotFound(let key, let context):
                detailedError += "Key '\(key.stringValue)' not found at path \(context.codingPath.map { $0.stringValue }.joined(separator: "."))"
            case .dataCorrupted(let context):
                detailedError += "Data corrupted at path \(context.codingPath.map { $0.stringValue }.joined(separator: ".")): \(context.debugDescription)"
            @unknown default:
                detailedError += error.localizedDescription
            }
            errorMessage = detailedError
            print("❌ [MapMemoriesViewModel] Decoding error: \(detailedError)")
        } catch {
            errorMessage = error.localizedDescription
            print("❌ [MapMemoriesViewModel] Error loading map memories: \(error.localizedDescription)")
        }
        
        isLoading = false
    }
    
    func loadGoogleMapsApiKey() async {
        do {
            let response = try await socialService.getGoogleMapsApiKey()
            googleMapsApiKey = response.apiKey
        } catch {
            print("⚠️ [MapMemoriesViewModel] Error loading Google Maps API key: \(error.localizedDescription)")
            // Don't set error message, just log it - MapKit can work without it
        }
    }
}


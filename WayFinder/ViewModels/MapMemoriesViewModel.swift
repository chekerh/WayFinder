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
        
        isLoading = true
        errorMessage = nil
        
        do {
            let response = try await socialService.getMapMemories()
            mapMemories = response
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


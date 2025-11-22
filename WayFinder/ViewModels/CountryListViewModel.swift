import Foundation

@MainActor
final class CountryListViewModel: ObservableObject {
    @Published var countries: [Country] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let service: CountryService
    
    nonisolated init(service: CountryService? = nil) {
        // Accéder à .shared depuis un contexte non isolé
        if let service = service {
            self.service = service
        } else {
            // Utiliser MainActor.assumeIsolated pour accéder à .shared (CountryService est Sendable)
            self.service = MainActor.assumeIsolated {
                CountryService.shared
            }
        }
    }
    
    func load(regionId: String) async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        print("🔄 [CountryList] Loading countries for region: \(regionId)")
        do {
            countries = try await service.fetchCountries(regionId: regionId)
            print("✅ [CountryList] Loaded \(countries.count) countries")
        } catch {
            print("❌ [CountryList] Error: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
}



import Foundation

@MainActor
final class CountryListViewModel: ObservableObject {
    @Published var countries: [Country] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let service: CountryService
    
    init(service: CountryService = .shared) {
        self.service = service
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



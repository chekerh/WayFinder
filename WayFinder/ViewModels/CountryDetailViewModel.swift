import Foundation

@MainActor
final class CountryDetailViewModel: ObservableObject {
    @Published var detail: CountryDetail?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let service: CountryService
    private let countryId: String
    
    nonisolated init(countryId: String, service: CountryService? = nil) {
        self.countryId = countryId
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
    
    func load() async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        print("🔄 [CountryDetail] Loading detail for country: \(countryId)")
        do {
            detail = try await service.fetchCountryDetail(id: countryId)
            print("✅ [CountryDetail] Loaded detail for: \(detail?.name ?? "unknown")")
        } catch {
            print("❌ [CountryDetail] Error: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
}



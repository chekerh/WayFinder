import Foundation

@MainActor
final class CountryDetailViewModel: ObservableObject {
    @Published var detail: CountryDetail?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let service: CountryService
    private let countryId: String
    
    init(countryId: String, service: CountryService = .shared) {
        self.countryId = countryId
        self.service = service
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



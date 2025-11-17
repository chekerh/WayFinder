import Foundation

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var greetingName: String = "Javier"
    @Published var regions: [RecommendationRegion] = []
    @Published var highlights: [RecommendationHighlight] = []
    @Published var selectedRegionId: String?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    // Pays de la région sélectionnée chargés depuis l'API
    @Published var countries: [Country] = []
    @Published var isLoadingCountries = false
    @Published var countriesErrorMessage: String?
    
    private let recommendationService: RecommendationService
    private let countryService: CountryService
    private let preferenceStorage: PreferenceStorage.Type
    
    init(recommendationService: RecommendationService = .shared,
         countryService: CountryService = .shared,
         preferenceStorage: PreferenceStorage.Type = PreferenceStorage.self) {
        self.recommendationService = recommendationService
        self.countryService = countryService
        self.preferenceStorage = preferenceStorage
    }
    
    func load() async {
        if isLoading { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        do {
            let preferenceId = preferenceStorage.fetchPreferenceId()
            let response = try await recommendationService.fetchRecommendations(preferenceId: preferenceId)
            greetingName = response.user.firstName
            regions = response.regions
            highlights = response.highlights
            if selectedRegionId == nil {
                selectedRegionId = response.regions.first?.id
                // Charger les pays de la première région
                if let firstRegionId = response.regions.first?.id {
                    await loadCountries(for: firstRegionId)
                }
            } else if let selectedRegionId, !response.regions.contains(where: { $0.id == selectedRegionId }) {
                self.selectedRegionId = response.regions.first?.id
                if let firstRegionId = response.regions.first?.id {
                    await loadCountries(for: firstRegionId)
                }
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
    
    func selectRegion(_ region: RecommendationRegion) {
        selectedRegionId = region.id
        // Charger les pays de la région sélectionnée depuis l'API
        Task {
            await loadCountries(for: region.id)
        }
    }
    
    func loadCountries(for regionId: String) async {
        guard !isLoadingCountries else { return }
        isLoadingCountries = true
        defer { isLoadingCountries = false }
        countriesErrorMessage = nil
        
        print("🔄 [HomeViewModel] Loading countries for region: \(regionId)")
        do {
            countries = try await countryService.fetchCountries(regionId: regionId)
            print("✅ [HomeViewModel] Loaded \(countries.count) countries for region \(regionId)")
        } catch {
            print("❌ [HomeViewModel] Error loading countries: \(error.localizedDescription)")
            countriesErrorMessage = error.localizedDescription
            countries = [] // Réinitialiser en cas d'erreur
        }
    }
}

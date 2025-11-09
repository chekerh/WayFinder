import Foundation

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var greetingName: String = "Javier"
    @Published var regions: [RecommendationRegion] = []
    @Published var highlights: [RecommendationHighlight] = []
    @Published var selectedRegionId: String?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let recommendationService: RecommendationService
    private let preferenceStorage: PreferenceStorage.Type
    
    init(recommendationService: RecommendationService = .shared,
         preferenceStorage: PreferenceStorage.Type = PreferenceStorage.self) {
        self.recommendationService = recommendationService
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
            } else if let selectedRegionId, !response.regions.contains(where: { $0.id == selectedRegionId }) {
                self.selectedRegionId = response.regions.first?.id
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
    
    func selectRegion(_ region: RecommendationRegion) {
        selectedRegionId = region.id
    }
}

import Foundation

@MainActor
final class RewardsViewModel: ObservableObject {
    @Published var userPoints: UserPoints?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    let service: RewardsService
    
    nonisolated init(service: RewardsService? = nil) {
        if let service = service {
            self.service = service
        } else {
            self.service = MainActor.assumeIsolated {
                RewardsService.shared
            }
        }
    }
    
    /// Charge les points de l'utilisateur
    func loadUserPoints() async {
        guard !isLoading else { 
            print("⚠️ [RewardsViewModel] Load already in progress, skipping")
            return 
        }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        print("🔄 [RewardsViewModel] Loading user points")
        do {
            userPoints = try await service.getUserPoints()
            print("✅ [RewardsViewModel] Loaded points: \(userPoints?.totalPoints ?? 0)")
        } catch {
            print("❌ [RewardsViewModel] Error loading points: \(error.localizedDescription)")
            
            // Si c'est une erreur de rate limiting, afficher un message plus clair
            if error.localizedDescription.contains("Too Many Requests") || 
               error.localizedDescription.contains("ThrottlerException") {
                errorMessage = "Trop de requêtes. Veuillez patienter quelques instants."
            } else {
                errorMessage = error.localizedDescription
            }
        }
    }
    
    /// Calcule la réduction possible basée sur les points
    /// 1 point = 0.1 TND de réduction (ou autre devise)
    func calculateDiscount(pointsToUse: Int, currency: String = "TND") -> Double {
        // Taux de conversion: 1 point = 0.1 unité de devise
        let discountRate = 0.1
        return Double(pointsToUse) * discountRate
    }
    
    /// Calcule le nombre maximum de points utilisables pour une réduction
    func getMaxUsablePoints(for totalPrice: Double) -> Int {
        guard let availablePoints = userPoints?.availablePoints else { return 0 }
        // On peut utiliser tous les points disponibles, mais la réduction ne peut pas dépasser le prix total
        let maxDiscount = totalPrice
        let maxPointsForDiscount = Int(maxDiscount / 0.1) // 1 point = 0.1 TND
        return min(availablePoints, maxPointsForDiscount)
    }
}


import Foundation
import SwiftUI
import UIKit

@MainActor
class OutfitWeatherViewModel: ObservableObject {
    @Published var uiState: OutfitWeatherUiState = .idle
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var outfitHistory: [Outfit] = []
    @Published var isLoadingHistory: Bool = false
    
    private let outfitService = OutfitWeatherService.shared
    
    var outfit: Outfit? {
        if case .success(let outfit) = uiState {
            return outfit
        }
        return nil
    }
    
    func uploadOutfit(imageData: Data, bookingId: String) async {
        isLoading = true
        errorMessage = nil
        uiState = .loading
        
        do {
            let response = try await outfitService.uploadOutfitImage(imageData: imageData, bookingId: bookingId)
            uiState = .success(response.analysis)
            isLoading = false
        } catch {
            var errorMsg = error.localizedDescription
            
            // Améliorer le message d'erreur pour les timeouts
            if let urlError = error as? URLError {
                switch urlError.code {
                case .timedOut:
                    errorMsg = String(localized: "outfit_timeout_message")
                case .notConnectedToInternet:
                    errorMsg = String(localized: "outfit_no_internet")
                case .cannotConnectToHost:
                    errorMsg = String(localized: "outfit_server_error")
                default:
                    break
                }
            }
            
            errorMessage = errorMsg
            uiState = .error(errorMsg)
            isLoading = false
        }
    }
    
    func loadOutfit(outfitId: String) async {
        isLoading = true
        errorMessage = nil
        uiState = .loading
        
        do {
            let outfit = try await outfitService.getOutfit(outfitId: outfitId)
            uiState = .success(outfit)
            isLoading = false
        } catch {
            let errorMsg = error.localizedDescription
            errorMessage = errorMsg
            uiState = .error(errorMsg)
            isLoading = false
        }
    }
    
    func loadOutfitHistory(bookingId: String) async {
        isLoadingHistory = true
        do {
            let outfits = try await outfitService.getOutfitsForBooking(bookingId: bookingId)
            outfitHistory = outfits.sorted { outfit1, outfit2 in
                // Sort by outfit_date if available, otherwise by createdAt
                if let d1 = outfit1.outfitDate, let d2 = outfit2.outfitDate {
                    return d1 > d2
                }
                if outfit1.outfitDate != nil {
                    return true
                }
                if outfit2.outfitDate != nil {
                    return false
                }
                return (outfit1.createdAt ?? "") > (outfit2.createdAt ?? "")
            }
            isLoadingHistory = false
        } catch {
            print("❌ [OutfitWeatherViewModel] Error loading history: \(error)")
            isLoadingHistory = false
        }
    }
    
    func deleteOutfit(outfitId: String, bookingId: String) async {
        do {
            try await outfitService.deleteOutfit(outfitId: outfitId)
            // Recharger l'historique après suppression
            await loadOutfitHistory(bookingId: bookingId)
        } catch {
            print("❌ [OutfitWeatherViewModel] Error deleting outfit: \(error)")
            errorMessage = error.localizedDescription
        }
    }
    
    func resetState() {
        uiState = .idle
        errorMessage = nil
        isLoading = false
    }
}

enum OutfitWeatherUiState: Equatable {
    case idle
    case loading
    case success(Outfit)
    case error(String)
    
    static func == (lhs: OutfitWeatherUiState, rhs: OutfitWeatherUiState) -> Bool {
        switch (lhs, rhs) {
        case (.idle, .idle), (.loading, .loading):
            return true
        case (.success(let lhsOutfit), .success(let rhsOutfit)):
            return lhsOutfit.id == rhsOutfit.id
        case (.error(let lhsError), .error(let rhsError)):
            return lhsError == rhsError
        default:
            return false
        }
    }
}


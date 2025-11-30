import Foundation
import SwiftUI
import UIKit

@MainActor
class OutfitWeatherViewModel: ObservableObject {
    @Published var uiState: OutfitWeatherUiState = .idle
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    
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
    
    func resetState() {
        uiState = .idle
        errorMessage = nil
        isLoading = false
    }
}

enum OutfitWeatherUiState {
    case idle
    case loading
    case success(Outfit)
    case error(String)
}


import Foundation
import SwiftUI

enum DestinationVideoUiState {
    case idle
    case loading
    case success(destinations: [DestinationWithVideoStatus])
    case error(message: String)
}

enum VideoGenerationState {
    case idle
    case generating
    case success(message: String)
    case error(message: String)
}

@MainActor
class DestinationVideoViewModel: ObservableObject {
    @Published var uiState: DestinationVideoUiState = .idle
    @Published var generationState: VideoGenerationState = .idle
    
    private let service: JourneyService
    
    init(service: JourneyService = JourneyService.shared) {
        self.service = service
    }
    
    func loadUserDestinations(userId: String) async {
        uiState = .loading
        do {
            let response = try await service.getUserDestinations(userId: userId)
            uiState = .success(destinations: response.destinations)
        } catch {
            uiState = .error(message: error.localizedDescription)
            print("❌ [DestinationVideoViewModel] Error loading destinations: \(error.localizedDescription)")
        }
    }
    
    func generateVideo(userId: String, destination: String) async {
        generationState = .generating
        do {
            let response = try await service.generateDestinationVideo(userId: userId, destination: destination)
            generationState = .success(message: response.message)
            // Reload destinations to update status
            await loadUserDestinations(userId: userId)
        } catch {
            generationState = .error(message: error.localizedDescription)
            print("❌ [DestinationVideoViewModel] Error generating video: \(error.localizedDescription)")
        }
    }
    
    func checkVideoStatus(userId: String, destination: String) async {
        do {
            let status = try await service.getDestinationVideoStatus(userId: userId, destination: destination)
            // Update the destination status in the list if needed
            if case .success(var destinations) = uiState {
                if let index = destinations.firstIndex(where: { $0.destination == destination }) {
                    var updatedDestination = destinations[index]
                    // Note: DestinationWithVideoStatus is a struct, so we need to create a new one
                    let newDestination = DestinationWithVideoStatus(
                        destination: updatedDestination.destination,
                        videoStatus: status.status,
                        videoUrl: status.videoUrl,
                        imageCount: status.imageCount,
                        errorMessage: status.errorMessage
                    )
                    destinations[index] = newDestination
                    uiState = .success(destinations: destinations)
                }
            }
        } catch {
            print("❌ [DestinationVideoViewModel] Error checking video status: \(error.localizedDescription)")
        }
    }
}


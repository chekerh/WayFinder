import Foundation
import SwiftUI

@MainActor
final class JourneyViewModel: ObservableObject {
    @Published var journeys: [Journey] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var uploadProgress: Double = 0.0
    @Published var isUploading = false
    
    private let service: JourneyService
    
    init(service: JourneyService = JourneyService.shared) {
        self.service = service
    }
    
    func loadJourneys(limit: Int = 20, skip: Int = 0) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        
        do {
            journeys = try await service.getJourneys(limit: limit, skip: skip)
        } catch {
            errorMessage = error.localizedDescription
            print("❌ [JourneyViewModel] Error loading journeys: \(error.localizedDescription)")
        }
    }
    
    func createJourney(
        images: [UIImage],
        bookingId: String?,
        destination: String?,
        description: String?,
        tags: [String]?,
        isPublic: Bool = true
    ) async throws -> Journey {
        isUploading = true
        uploadProgress = 0.0
        errorMessage = nil
        defer { 
            isUploading = false
            uploadProgress = 0.0
        }
        
        do {
            // Prepare images
            var imageDataList: [Data] = []
            let totalImages = images.count
            
            for (index, image) in images.enumerated() {
                // Compress and prepare image
                if let imageData = prepareImageForUpload(image) {
                    imageDataList.append(imageData)
                }
                uploadProgress = Double(index + 1) / Double(totalImages) * 0.5 // 50% for compression
            }
            
            guard !imageDataList.isEmpty else {
                throw NSError(domain: "JourneyViewModel", code: -1, userInfo: [NSLocalizedDescriptionKey: "Aucune image valide à uploader"])
            }
            
            // Upload to backend
            uploadProgress = 0.5
            let journey = try await service.createJourney(
                images: imageDataList,
                bookingId: bookingId,
                destination: destination,
                description: description,
                tags: tags,
                isPublic: isPublic
            )
            
            uploadProgress = 1.0
            journeys.insert(journey, at: 0)
            return journey
        } catch {
            errorMessage = error.localizedDescription
            print("❌ [JourneyViewModel] Error creating journey: \(error.localizedDescription)")
            throw error
        }
    }
    
    func canShareJourney() async throws -> CanShareJourneyResponse {
        return try await service.canShareJourney()
    }
    
    func loadMyJourneys(limit: Int = 20, skip: Int = 0) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        
        do {
            journeys = try await service.getMyJourneys(limit: limit, skip: skip)
        } catch {
            errorMessage = error.localizedDescription
            print("❌ [JourneyViewModel] Error loading my journeys: \(error.localizedDescription)")
        }
    }
    
    func regenerateVideo(journeyId: String) async throws {
        do {
            _ = try await service.regenerateVideo(journeyId: journeyId)
            // Recharger les voyages pour voir le statut mis à jour
            await loadJourneys()
        } catch {
            errorMessage = error.localizedDescription
            print("❌ [JourneyViewModel] Error regenerating video: \(error.localizedDescription)")
            throw error
        }
    }
    
    func likeJourney(journeyId: String) async {
        do {
            _ = try await service.likeJourney(journeyId: journeyId)
            // Recharger pour mettre à jour le statut like
            await loadJourneys()
        } catch {
            print("❌ [JourneyViewModel] Error liking journey: \(error.localizedDescription)")
        }
    }
    
    func deleteJourney(journeyId: String) async {
        do {
            try await service.deleteJourney(journeyId: journeyId)
            // Retirer le voyage de la liste localement pour une mise à jour immédiate
            journeys.removeAll { $0.id == journeyId }
            // Recharger la liste complète pour s'assurer de la synchronisation
            await loadJourneys()
        } catch {
            errorMessage = error.localizedDescription
            print("❌ [JourneyViewModel] Error deleting journey: \(error.localizedDescription)")
            // Même en cas d'erreur, recharger pour voir l'état actuel
            await loadJourneys()
        }
    }
    
    private func prepareImageForUpload(_ image: UIImage) -> Data? {
        // Réduire la taille de l'image si nécessaire (max 1920px, 2MB)
        let maxDimension: CGFloat = 1920
        let maxFileSize = 2 * 1024 * 1024 // 2MB
        
        var resizedImage = image
        let largestSide = max(image.size.width, image.size.height)
        
        if largestSide > maxDimension {
            let scale = maxDimension / largestSide
            let newSize = CGSize(width: image.size.width * scale, height: image.size.height * scale)
            
            UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
            image.draw(in: CGRect(origin: .zero, size: newSize))
            resizedImage = UIGraphicsGetImageFromCurrentImageContext() ?? image
            UIGraphicsEndImageContext()
        }
        
        // Essayer différentes qualités de compression
        var compression: CGFloat = 0.8
        var imageData = resizedImage.jpegData(compressionQuality: compression)
        
        while let data = imageData, data.count > maxFileSize && compression > 0.3 {
            compression -= 0.1
            imageData = resizedImage.jpegData(compressionQuality: compression)
        }
        
        return imageData
    }
}


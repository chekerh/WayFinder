import Foundation
import SwiftUI

@MainActor
final class JourneyCommentsViewModel: ObservableObject {
    @Published private(set) var journey: Journey
    @Published private(set) var comments: [JourneyComment] = []
    @Published var commentText: String = ""
    @Published var isLoadingJourney = false
    @Published var isLoadingComments = false
    @Published var isSubmittingComment = false
    @Published var errorMessage: String?
    
    private let service: JourneyService
    
    init(journey: Journey, service: JourneyService = JourneyService.shared) {
        self.journey = journey
        self.service = service
    }
    
    var journeyId: String {
        journey.id
    }
    
    func refresh() async {
        await loadJourneyDetail()
        await loadComments()
    }
    
    func loadJourneyDetail() async {
        guard !isLoadingJourney else { return }
        isLoadingJourney = true
        defer { isLoadingJourney = false }
        
        do {
            let detail = try await service.getJourney(by: journeyId)
            journey = detail
        } catch {
            errorMessage = error.localizedDescription
        }
    }
    
    func loadComments() async {
        guard !isLoadingComments else { return }
        isLoadingComments = true
        defer { isLoadingComments = false }
        
        do {
            comments = try await service.getJourneyComments(journeyId: journeyId, limit: 100, skip: 0)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
    
    func submitComment() async {
        let trimmedContent = commentText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedContent.isEmpty else { return }
        guard !isSubmittingComment else { return }
        
        isSubmittingComment = true
        defer { isSubmittingComment = false }
        
        do {
            _ = try await service.addJourneyComment(
                journeyId: journeyId,
                content: trimmedContent,
                parentCommentId: nil
            )
            commentText = ""
            journey.commentsCount += 1
            await loadComments()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
    
    func toggleLike() async {
        do {
            let response = try await service.likeJourney(journeyId: journeyId)
            if response.liked {
                if !journey.isLiked {
                    journey.likesCount += 1
                }
                journey.isLiked = true
            } else {
                if journey.isLiked {
                    journey.likesCount = max(0, journey.likesCount - 1)
                }
                journey.isLiked = false
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}


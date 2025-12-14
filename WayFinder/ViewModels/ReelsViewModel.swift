import Foundation
import SwiftUI

@MainActor
class ReelsViewModel: ObservableObject {
    @Published var uiState: ReelsUiState = .idle
    @Published var items: [ReelContentItem] = []
    
    private var currentPage = 0
    private let pageSize = 10
    private var hasMore = true
    
    private let discussionService = DiscussionService.shared
    private let journeyService = JourneyService.shared
    @Published private(set) var currentUserId: String?
    
    init() {
        loadCurrentUserId()
    }
    
    private func loadCurrentUserId() {
        Task {
            do {
                let profile = try await UserService.shared.fetchProfile()
                await MainActor.run {
                    self.currentUserId = profile.id
                }
            } catch {
                print("⚠️ [ReelsViewModel] Could not load current user ID: \(error)")
            }
        }
    }
    
    func loadReelsFeed(refresh: Bool = false) async {
        do {
            if refresh {
                currentPage = 0
                hasMore = true
            }
            
            if !hasMore && !refresh {
                return
            }
            
            uiState = .loading
            
            // Fetch posts and journeys in parallel
            async let postsTask = fetchPosts()
            async let journeysTask = fetchJourneys()
            
            let (postsResult, journeysResult) = try await (postsTask, journeysTask)
            
            // Convert to ReelContentItem
            let postItems = postsResult.posts.map { post in
                // Debug: Print post user info
                let user = post.user
                print("📸 [ReelsViewModel] Post \(post.id) - User: \(user.id), Username: \(user.username ?? "nil"), Avatar: \(user.profileImageUrl ?? "nil")")
                return ReelContentItem.postItem(post)
            }
            let journeyItems = journeysResult.map { journey in
                // Debug: Print journey user info
                if let user = journey.user {
                    print("📸 [ReelsViewModel] Journey \(journey.id) - User: \(user.id), Username: \(user.username), Avatar: \(user.profileImageUrl ?? "nil")")
                } else {
                    print("⚠️ [ReelsViewModel] Journey \(journey.id) - User is nil")
                }
                return ReelContentItem.journeyItem(journey)
            }
            
            // Merge and sort by engagement (likes + comments) and recency
            let allItems = (postItems + journeyItems).sorted { lhs, rhs in
                let lhsEngagement = lhs.likesCount + lhs.commentsCount
                let rhsEngagement = rhs.likesCount + rhs.commentsCount
                
                if lhsEngagement != rhsEngagement {
                    return lhsEngagement > rhsEngagement
                }
                
                // Sort by recency (newer first)
                return lhs.createdAt > rhs.createdAt
            }
            
            let existingItems: [ReelContentItem] = (refresh || currentPage == 0) ? [] : items
            
            items = existingItems + allItems
            hasMore = allItems.count == pageSize && (postsResult.posts.count == pageSize || journeysResult.count == pageSize)
            
            uiState = .success(items: items, hasMore: hasMore)
            currentPage += 1
        } catch {
            print("❌ [ReelsViewModel] Error loading reels feed: \(error)")
            uiState = .error(message: error.localizedDescription)
        }
    }
    
    private func fetchPosts() async throws -> (posts: [DiscussionPost], hasMore: Bool) {
        do {
            let posts = try await discussionService.getPosts(
                limit: pageSize,
                skip: currentPage * pageSize,
                destination: nil
            )
            return (posts, posts.count == pageSize)
        } catch {
            print("❌ [ReelsViewModel] Error loading posts: \(error)")
            return ([], false)
        }
    }
    
    private func fetchJourneys() async throws -> [Journey] {
        do {
            return try await journeyService.getJourneys(
                limit: pageSize,
                skip: currentPage * pageSize
            )
        } catch {
            print("❌ [ReelsViewModel] Error loading journeys: \(error)")
            return []
        }
    }
    
    func likeItem(_ item: ReelContentItem) async {
        do {
            switch item {
            case .postItem(let post):
                let updatedPost = try await discussionService.likePost(id: post.id)
                // Update state optimistically
                if let index = items.firstIndex(where: { $0.id == item.id }) {
                    if case .postItem = items[index] {
                        items[index] = .postItem(updatedPost)
                        uiState = .success(items: items, hasMore: hasMore)
                    }
                }
            case .journeyItem(let journey):
                _ = try await journeyService.likeJourney(journeyId: journey.id)
                // Reload journey to get updated state
                let updatedJourney = try await journeyService.getJourney(by: journey.id)
                // Update state optimistically
                if let index = items.firstIndex(where: { $0.id == item.id }) {
                    if case .journeyItem = items[index] {
                        items[index] = .journeyItem(updatedJourney)
                        uiState = .success(items: items, hasMore: hasMore)
                    }
                }
            }
        } catch {
            print("❌ [ReelsViewModel] Error liking item: \(error)")
            // Reload on error
            await loadReelsFeed(refresh: true)
        }
    }
    
    func retry() {
        Task {
            await loadReelsFeed(refresh: true)
        }
    }
}

enum ReelsUiState {
    case idle
    case loading
    case success(items: [ReelContentItem], hasMore: Bool)
    case error(message: String)
}


import Foundation

/// Unified content item for reels feed (similar to Android ReelContentItem)
enum ReelContentItem: Identifiable {
    case postItem(DiscussionPost)
    case journeyItem(Journey)
    
    var id: String {
        switch self {
        case .postItem(let post):
            return "post_\(post.id)"
        case .journeyItem(let journey):
            return "journey_\(journey.id)"
        }
    }
    
    var likesCount: Int {
        switch self {
        case .postItem(let post):
            return post.likesCount
        case .journeyItem(let journey):
            return journey.likesCount
        }
    }
    
    var commentsCount: Int {
        switch self {
        case .postItem(let post):
            return post.commentsCount
        case .journeyItem(let journey):
            return journey.commentsCount
        }
    }
    
    var viewsCount: Int {
        switch self {
        case .postItem(let post):
            return post.viewsCount
        case .journeyItem(let journey):
            return journey.viewsCount
        }
    }
    
    var createdAt: String {
        switch self {
        case .postItem(let post):
            if let date = post.createdAt {
                let formatter = ISO8601DateFormatter()
                formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
                return formatter.string(from: date)
            }
            return ""
        case .journeyItem(let journey):
            return journey.createdAt ?? ""
        }
    }
    
    var isLiked: Bool {
        switch self {
        case .postItem(let post):
            // Will be determined by likedBy list in ViewModel
            return false
        case .journeyItem(let journey):
            return journey.isLiked
        }
    }
    
    var thumbnailUrl: String? {
        switch self {
        case .postItem(let post):
            return post.imageUrl
        case .journeyItem(let journey):
            return journey.videoUrl ?? journey.imageUrls.first
        }
    }
    
    var isVideo: Bool {
        switch self {
        case .postItem:
            return false
        case .journeyItem(let journey):
            return journey.videoUrl != nil
        }
    }
    
    var caption: String {
        switch self {
        case .postItem(let post):
            return post.content
        case .journeyItem(let journey):
            return journey.description ?? journey.captionText ?? ""
        }
    }
    
    var creatorName: String {
        switch self {
        case .postItem(let post):
            let firstName = post.user.firstName ?? ""
            let lastName = post.user.lastName ?? ""
            let name = "\(firstName) \(lastName)".trimmingCharacters(in: .whitespaces)
            return name.isEmpty ? (post.user.username ?? "User") : name
        case .journeyItem(let journey):
            if let user = journey.user {
                let firstName = user.firstName ?? ""
                let lastName = user.lastName ?? ""
                let name = "\(firstName) \(lastName)".trimmingCharacters(in: .whitespaces)
                return name.isEmpty ? (user.username ?? "Traveler") : name
            }
            return "Traveler"
        }
    }
    
    var creatorAvatar: String? {
        switch self {
        case .postItem(let post):
            return post.user.profileImageUrl
        case .journeyItem(let journey):
            return journey.user?.profileImageUrl
        }
    }
    
    var destination: String? {
        switch self {
        case .postItem(let post):
            return post.destination
        case .journeyItem(let journey):
            return journey.destination
        }
    }
    
    var tags: [String] {
        switch self {
        case .postItem(let post):
            return post.tags
        case .journeyItem(let journey):
            return journey.tags
        }
    }
}

extension ReelContentItem: Equatable {
    static func == (lhs: ReelContentItem, rhs: ReelContentItem) -> Bool {
        lhs.id == rhs.id
    }
}


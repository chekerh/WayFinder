import Foundation

struct Journey: Decodable, Identifiable {
    let id: String
    let userId: String
    let bookingId: String?
    let destination: String
    let destinationCountry: String? // Nom du pays (ex: "South Korea")
    let imageUrls: [String]
    let slides: [JourneySlide]
    let videoUrl: String?
    let videoStatus: String
    let musicTheme: String?
    let captionText: String?
    let description: String?
    let tags: [String]
    var likesCount: Int
    var commentsCount: Int
    var viewsCount: Int
    let isPublic: Bool
    var isLiked: Bool
    let createdAt: String?
    let updatedAt: String?
    let user: UserPreview?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case userId = "user_id"
        case bookingId = "booking_id"
        case destination
        case destinationCountry = "destination_country"
        case imageUrls = "image_urls"
        case slides
        case videoUrl = "video_url"
        case videoStatus = "video_status"
        case musicTheme = "music_theme"
        case captionText = "caption_text"
        case description
        case tags
        case likesCount = "likes_count"
        case commentsCount = "comments_count"
        case viewsCount = "views_count"
        case isPublic = "is_public"
        case isLiked = "is_liked"
        case createdAt
        case updatedAt
        case user
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        id = try container.decode(String.self, forKey: .id)
        userId = try container.decode(String.self, forKey: .userId)
        
        // Gérer booking_id qui peut être une string, un objet, ou null
        if let bookingIdValue = try? container.decode(String.self, forKey: .bookingId) {
            // Si c'est "[object Object]", on l'ignore
            bookingId = bookingIdValue == "[object Object]" ? nil : bookingIdValue
        } else {
            // Si ce n'est pas une string, on essaie de décoder comme objet et extraire l'id
            bookingId = nil
        }
        
        destination = try container.decode(String.self, forKey: .destination)
        destinationCountry = try? container.decode(String.self, forKey: .destinationCountry)
        imageUrls = (try? container.decode([String].self, forKey: .imageUrls)) ?? []
        slides = (try? container.decode([JourneySlide].self, forKey: .slides)) ?? []
        videoUrl = try? container.decode(String.self, forKey: .videoUrl)
        videoStatus = (try? container.decode(String.self, forKey: .videoStatus)) ?? "pending"
        musicTheme = try? container.decode(String.self, forKey: .musicTheme)
        captionText = try? container.decode(String.self, forKey: .captionText)
        description = try? container.decode(String.self, forKey: .description)
        tags = (try? container.decode([String].self, forKey: .tags)) ?? []
        likesCount = (try? container.decode(Int.self, forKey: .likesCount)) ?? 0
        commentsCount = (try? container.decode(Int.self, forKey: .commentsCount)) ?? 0
        viewsCount = (try? container.decode(Int.self, forKey: .viewsCount)) ?? 0
        isPublic = (try? container.decode(Bool.self, forKey: .isPublic)) ?? true
        isLiked = (try? container.decode(Bool.self, forKey: .isLiked)) ?? false
        createdAt = try? container.decode(String.self, forKey: .createdAt)
        updatedAt = try? container.decode(String.self, forKey: .updatedAt)
        user = try? container.decode(UserPreview.self, forKey: .user)
    }
}

extension Journey: Equatable {
    static func == (lhs: Journey, rhs: Journey) -> Bool {
        lhs.id == rhs.id &&
        lhs.likesCount == rhs.likesCount &&
        lhs.commentsCount == rhs.commentsCount &&
        lhs.isLiked == rhs.isLiked
    }
}

struct JourneySlide: Decodable {
    let imageUrl: String
    let caption: String?
    let id: String?
    
    enum CodingKeys: String, CodingKey {
        case imageUrl
        case caption
        case id = "_id"
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        imageUrl = try container.decode(String.self, forKey: .imageUrl)
        caption = try? container.decode(String.self, forKey: .caption)
        id = try? container.decode(String.self, forKey: .id)
    }
}

struct UpdateJourneyRequest: Encodable {
    let description: String?
    let tags: [String]?
    let isPublic: Bool?
    let musicTheme: String?
    let captionText: String?
    
    enum CodingKeys: String, CodingKey {
        case description
        case tags
        case isPublic = "is_public"
        case musicTheme = "music_theme"
        case captionText = "caption_text"
    }
}

struct JourneyComment: Decodable, Identifiable {
    let id: String
    let journeyId: String
    let userId: String
    let content: String
    let parentCommentId: String?
    let createdAt: String?
    let updatedAt: String?
    let user: UserPreview?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case journeyId = "journey_id"
        case userId = "user_id"
        case content
        case parentCommentId = "parent_comment_id"
        case createdAt
        case updatedAt
        case user
    }
}

struct CreateJourneyCommentRequest: Encodable {
    let content: String
    let parentCommentId: String?
    
    enum CodingKeys: String, CodingKey {
        case content
        case parentCommentId = "parent_comment_id"
    }
}

struct JourneyLikeResponse: Decodable {
    let liked: Bool
    let message: String
}

struct CanShareJourneyResponse: Decodable {
    let canShare: Bool
    let confirmedBookingsCount: Int
    let message: String
    
    enum CodingKeys: String, CodingKey {
        case canShare
        case confirmedBookingsCount
        case message
    }
}


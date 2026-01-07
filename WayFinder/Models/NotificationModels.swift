import Foundation

enum NotificationType: String, Codable {
    case bookingConfirmed = "booking_confirmed"
    case bookingCancelled = "booking_cancelled"
    case bookingUpdated = "booking_updated"
    case priceAlert = "price_alert"
    case paymentSuccess = "payment_success"
    case paymentFailed = "payment_failed"
    case tripReminder = "trip_reminder"
    case postLiked = "post_liked"
    case postCommented = "post_commented"
    case journeyLiked = "journey_liked"
    case journeyCommented = "journey_commented"
    case general = "general"
}

struct Notification: Codable, Identifiable {
    let id: String
    let userId: String
    let type: NotificationType
    let title: String
    let message: String
    let data: NotificationData?
    let isRead: Bool
    let readAt: Date?
    let actionUrl: String?
    let createdAt: Date?
    let updatedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case userId
        case type
        case title
        case message
        case data
        case isRead
        case readAt
        case actionUrl
        case createdAt
        case updatedAt
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Handle MongoDB _id which can be a string or object
        if let idString = try? container.decode(String.self, forKey: .id) {
            id = idString
        } else if let idDict = try? container.decode([String: String].self, forKey: .id),
                  let idValue = idDict["$oid"] {
            id = idValue
        } else {
            throw DecodingError.dataCorruptedError(forKey: .id, in: container, debugDescription: "Invalid id format")
        }
        
        // Handle userId which can be a string or object
        if let userIdString = try? container.decode(String.self, forKey: .userId) {
            userId = userIdString
        } else if let userIdDict = try? container.decode([String: String].self, forKey: .userId),
                  let userIdValue = userIdDict["$oid"] {
            userId = userIdValue
        } else {
            userId = ""
        }
        
        type = try container.decode(NotificationType.self, forKey: .type)
        title = try container.decode(String.self, forKey: .title)
        message = try container.decode(String.self, forKey: .message)
        data = try container.decodeIfPresent(NotificationData.self, forKey: .data)
        isRead = try container.decode(Bool.self, forKey: .isRead)
        
        // Handle dates - MongoDB returns ISO8601 strings
        if let readAtString = try? container.decodeIfPresent(String.self, forKey: .readAt) {
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            readAt = formatter.date(from: readAtString) ?? ISO8601DateFormatter().date(from: readAtString)
        } else {
            readAt = nil
        }
        
        actionUrl = try container.decodeIfPresent(String.self, forKey: .actionUrl)
        
        if let createdAtString = try? container.decodeIfPresent(String.self, forKey: .createdAt) {
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            createdAt = formatter.date(from: createdAtString) ?? ISO8601DateFormatter().date(from: createdAtString)
        } else {
            createdAt = nil
        }
        
        updatedAt = nil
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(userId, forKey: .userId)
        try container.encode(type, forKey: .type)
        try container.encode(title, forKey: .title)
        try container.encode(message, forKey: .message)
        try container.encodeIfPresent(data, forKey: .data)
        try container.encode(isRead, forKey: .isRead)
        
        // Encode dates as ISO8601 strings
        if let readAt = readAt {
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            try container.encode(formatter.string(from: readAt), forKey: .readAt)
        } else {
            try container.encodeNil(forKey: .readAt)
        }
        
        try container.encodeIfPresent(actionUrl, forKey: .actionUrl)
        
        if let createdAt = createdAt {
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            try container.encode(formatter.string(from: createdAt), forKey: .createdAt)
        } else {
            try container.encodeNil(forKey: .createdAt)
        }
        
        if let updatedAt = updatedAt {
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            try container.encode(formatter.string(from: updatedAt), forKey: .updatedAt)
        } else {
            try container.encodeNil(forKey: .updatedAt)
        }
    }
}

struct NotificationData: Codable {
    let bookingId: String?
    let destinationId: String?
    let price: Double?
    let oldPrice: Double?
    let postId: String?
    let commentId: String?
    let journeyId: String?
    let likerId: String?
    let commenterId: String?
}

struct UnreadCountResponse: Decodable {
    let count: Int
}

struct CreateNotificationRequest: Encodable {
    let type: String
    let title: String
    let message: String
    let data: [String: String]?
    let actionUrl: String?
}


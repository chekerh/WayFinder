import Foundation

enum NotificationType: String, Decodable {
    case bookingConfirmed = "booking_confirmed"
    case bookingCancelled = "booking_cancelled"
    case bookingUpdated = "booking_updated"
    case priceAlert = "price_alert"
    case paymentSuccess = "payment_success"
    case paymentFailed = "payment_failed"
    case tripReminder = "trip_reminder"
    case general = "general"
}

struct Notification: Decodable, Identifiable {
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
}

struct NotificationData: Decodable {
    let bookingId: String?
    let destinationId: String?
    let price: Double?
    let oldPrice: Double?
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


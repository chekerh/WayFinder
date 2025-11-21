import Foundation

struct Itinerary: Decodable, Identifiable {
    let id: String
    let userId: String
    let title: String
    let description: String?
    let destination: String
    let startDate: String
    let endDate: String
    let days: [DayPlan]
    let tags: [String]
    let isPublic: Bool
    let totalBudget: Double?
    let currency: String?
    let createdAt: Date?
    let updatedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case userId
        case title
        case description
        case destination
        case startDate
        case endDate
        case days
        case tags
        case isPublic
        case totalBudget
        case currency
        case createdAt
        case updatedAt
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Handle MongoDB _id
        if let idString = try? container.decode(String.self, forKey: .id) {
            id = idString
        } else if let idDict = try? container.decode([String: String].self, forKey: .id),
                  let idValue = idDict["$oid"] {
            id = idValue
        } else {
            throw DecodingError.dataCorruptedError(forKey: .id, in: container, debugDescription: "Invalid id format")
        }
        
        // Handle userId
        if let userIdString = try? container.decode(String.self, forKey: .userId) {
            userId = userIdString
        } else if let userIdDict = try? container.decode([String: String].self, forKey: .userId),
                  let userIdValue = userIdDict["$oid"] {
            userId = userIdValue
        } else {
            userId = ""
        }
        
        title = try container.decode(String.self, forKey: .title)
        description = try container.decodeIfPresent(String.self, forKey: .description)
        destination = try container.decode(String.self, forKey: .destination)
        startDate = try container.decode(String.self, forKey: .startDate)
        endDate = try container.decode(String.self, forKey: .endDate)
        days = try container.decodeIfPresent([DayPlan].self, forKey: .days) ?? []
        tags = try container.decodeIfPresent([String].self, forKey: .tags) ?? []
        isPublic = try container.decodeIfPresent(Bool.self, forKey: .isPublic) ?? false
        totalBudget = try container.decodeIfPresent(Double.self, forKey: .totalBudget)
        currency = try container.decodeIfPresent(String.self, forKey: .currency)
        createdAt = nil
        updatedAt = nil
    }
}

struct DayPlan: Decodable {
    let date: String
    let activities: [Activity]
    let notes: String?
}

struct Activity: Decodable, Identifiable {
    var id: String { "\(name)-\(location ?? "")" }
    let name: String
    let description: String?
    let location: String?
    let startTime: String?
    let endTime: String?
    let category: String?
    let cost: Double?
    let currency: String?
    let notes: String?
}

struct CreateItineraryRequest: Encodable {
    let title: String
    let description: String?
    let destination: String
    let startDate: String
    let endDate: String
    let days: [DayPlanDto]?
    let tags: [String]?
    let isPublic: Bool?
    let totalBudget: Double?
    let currency: String?
}

struct UpdateItineraryRequest: Encodable {
    let title: String?
    let description: String?
    let destination: String?
    let startDate: String?
    let endDate: String?
    let days: [DayPlanDto]?
    let tags: [String]?
    let isPublic: Bool?
    let totalBudget: Double?
    let currency: String?
}

struct DayPlanDto: Encodable {
    let date: String
    let activities: [ActivityDto]?
    let notes: String?
}

struct ActivityDto: Encodable {
    let name: String
    let description: String?
    let location: String?
    let startTime: String?
    let endTime: String?
    let category: String?
    let cost: Double?
    let currency: String?
    let notes: String?
}


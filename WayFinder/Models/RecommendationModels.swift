import Foundation

struct RecommendationUser: Decodable {
    let firstName: String
    let lastName: String?
    
    init(firstName: String, lastName: String? = nil) {
        self.firstName = firstName
        self.lastName = lastName
    }
}

struct RecommendationRegion: Decodable, Identifiable, Hashable {
    let id: String
    let title: String
    let imageUrl: String?
}

struct RecommendationHighlight: Decodable, Identifiable, Hashable {
    let id: String
    let title: String
    let subtitle: String
    let rating: Double?
    let imageUrl: String?
    let regionId: String?
}

struct RecommendationResponse: Decodable {
    let user: RecommendationUser
    let regions: [RecommendationRegion]
    let highlights: [RecommendationHighlight]
    
    init(user: RecommendationUser,
         regions: [RecommendationRegion],
         highlights: [RecommendationHighlight]) {
        self.user = user
        self.regions = regions
        self.highlights = highlights
    }
}

struct PersonalizedRecommendationsPayload: Decodable {
    let generatedAt: String?
    let preferencesUsed: [String: String]?
    let destinations: [PersonalizedDestination]?
    let offers: [PersonalizedOffer]?
    let activities: [PersonalizedActivity]?
    
    enum CodingKeys: String, CodingKey {
        case generatedAt = "generated_at"
        case preferencesUsed = "preferences_used"
        case destinations
        case offers
        case activities
    }
}

struct PersonalizedDestination: Decodable, Identifiable {
    let id: String
    let name: String
    let imageUrl: String?
    let matchScore: Double?
    let highlights: [String]?
    let estimatedCost: EstimatedCost?
    let reason: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case name
        case imageUrl = "image_url"
        case matchScore = "match_score"
        case highlights
        case estimatedCost = "estimated_cost"
        case reason
    }
}

struct PersonalizedOffer: Decodable, Identifiable {
    let id: String
    let type: String
    let destination: String
    let price: Double
    let matchScore: Double?
    let reason: String?
    
    enum CodingKeys: String, CodingKey {
        case id, type, destination, price
        case matchScore = "match_score"
        case reason
    }
}

struct PersonalizedActivity: Decodable, Identifiable {
    let id: String
    let name: String
    let type: String
    let destination: String
    let price: Double
    let matchScore: Double?
    let reason: String?
    
    enum CodingKeys: String, CodingKey {
        case id, name, type, destination, price
        case matchScore = "match_score"
        case reason
    }
}

struct EstimatedCost: Decodable {
    let flight: Double
    let hotelPerNight: Double
    let currency: String
    
    enum CodingKeys: String, CodingKey {
        case flight
        case hotelPerNight = "hotel_per_night"
        case currency
    }
}

extension RecommendationHighlight {
    init(destination: PersonalizedDestination) {
        self.init(
            id: destination.id,
            title: destination.name,
            subtitle: destination.reason ?? destination.highlights?.joined(separator: ", ") ?? "Suggestion personnalisée",
            rating: destination.matchScore.map { ($0 * 5).clamped(to: 0...5) },
            imageUrl: destination.imageUrl,
            regionId: nil
        )
    }
}

private extension Double {
    func clamped(to range: ClosedRange<Double>) -> Double {
        return min(max(self, range.lowerBound), range.upperBound)
    }
}

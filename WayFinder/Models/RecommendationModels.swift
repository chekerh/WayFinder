import Foundation

struct RecommendationUser: Decodable {
    let firstName: String
    let lastName: String?
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
}

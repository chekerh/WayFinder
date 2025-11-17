import Foundation

struct Country: Decodable, Identifiable, Hashable {
    let id: String
    let name: String
    let summary: String
    let thumbnailImageUrl: String?
    let heroImageUrl: String?
    let regionId: String
}

struct CountryDetail: Decodable, Identifiable {
    let id: String
    let name: String
    let summary: String
    let heroImageUrl: String?
    let description: String
    let bestSeason: String?
    let currency: String?
    let language: String?
    let timezone: String?
    let extraImages: [String]?
}



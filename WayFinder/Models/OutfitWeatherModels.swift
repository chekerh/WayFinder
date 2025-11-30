import Foundation

// MARK: - Outfit Model
struct Outfit: Decodable, Identifiable {
    let id: String
    let userId: String
    let bookingId: String
    let imageUrl: String
    let detectedItems: [String]
    let weatherData: WeatherData?
    let recommendation: OutfitRecommendation?
    let isApproved: Bool
    let createdAt: String?
    let updatedAt: String?
    let outfitDate: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case userId = "user_id"
        case bookingId = "booking_id"
        case imageUrl = "image_url"
        case detectedItems = "detected_items"
        case weatherData = "weather_data"
        case recommendation
        case isApproved = "is_approved"
        case createdAt
        case updatedAt
        case outfitDate = "outfit_date"
    }
}

// MARK: - Weather Data Model
struct WeatherData: Decodable {
    let temperature: Int
    let condition: String
    let humidity: Int?
    let windSpeed: Double?
    
    enum CodingKeys: String, CodingKey {
        case temperature
        case condition
        case humidity
        case windSpeed = "wind_speed"
    }
}

// MARK: - Outfit Recommendation Model
struct OutfitRecommendation: Decodable {
    let isSuitable: Bool
    let score: Int
    let feedback: String
    let suggestions: [String]
    
    enum CodingKeys: String, CodingKey {
        case isSuitable = "is_suitable"
        case score
        case feedback
        case suggestions
    }
}

// MARK: - Analyze Outfit Request
struct AnalyzeOutfitRequest: Encodable {
    let bookingId: String
    let imageUrl: String
    
    enum CodingKeys: String, CodingKey {
        case bookingId = "booking_id"
        case imageUrl = "image_url"
    }
}

// MARK: - Upload Outfit Response
struct UploadOutfitResponse: Decodable {
    let message: String
    let imageUrl: String
    let analysis: Outfit
    
    enum CodingKeys: String, CodingKey {
        case message
        case imageUrl = "image_url"
        case analysis
    }
}


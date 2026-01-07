//
//  AccommodationModels.swift
//  WayFinder
//
//  Models for hotel/accommodation data from Amadeus and Google Places APIs
//

import Foundation

struct Accommodation: Codable, Identifiable, Equatable {
    let id: String
    let name: String
    let type: String // "hotel", "apartment", "hostel", etc.
    let price: Double
    let currency: String
    let rating: Double
    let imageUrl: String?
    let location: String
    let address: String?
    let cityCode: String?
    let description: String?
    let photos: [String]
    let reviews: [HotelReview]
    let amenities: [String]
    let contact: HotelContact?
    let latitude: Double?
    let longitude: Double?
    let checkInTime: String?
    let checkOutTime: String?
    let roomType: String?
    let boardType: String? // "ROOM_ONLY", "BREAKFAST", etc.
    let guestRating: Double?
    let userRatingsTotal: Int?
    let pricePerNight: Double?
    let totalPrice: Double?
    
    enum CodingKeys: String, CodingKey {
        case id, name, type, price, currency, rating, imageUrl, location
        case address, cityCode, description, photos, reviews, amenities
        case contact, latitude, longitude, checkInTime, checkOutTime
        case roomType, boardType, guestRating, userRatingsTotal
        case pricePerNight, totalPrice
    }
    
    static func == (lhs: Accommodation, rhs: Accommodation) -> Bool {
        lhs.id == rhs.id
    }
}

struct HotelContact: Codable, Equatable {
    let phone: String?
    let email: String?
    let website: String?
}

struct HotelReview: Codable, Equatable {
    let authorName: String
    let rating: Double
    let text: String
    let time: Int64?
    
    enum CodingKeys: String, CodingKey {
        case authorName = "author_name"
        case rating, text, time
    }
    
    // Custom initializer for different key formats
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Try author_name first, then authorName
        if let name = try? container.decode(String.self, forKey: .authorName) {
            authorName = name
        } else {
            authorName = "Anonymous"
        }
        
        rating = try container.decodeIfPresent(Double.self, forKey: .rating) ?? 0
        text = try container.decodeIfPresent(String.self, forKey: .text) ?? ""
        time = try container.decodeIfPresent(Int64.self, forKey: .time)
    }
    
    init(authorName: String, rating: Double, text: String, time: Int64? = nil) {
        self.authorName = authorName
        self.rating = rating
        self.text = text
        self.time = time
    }
}

struct AccommodationSearchResponse: Codable {
    let data: [Accommodation]
    let meta: [String: String]?
}

struct HotelDetailsResponse: Codable {
    let data: Accommodation
    let meta: [String: String]?
}

// MARK: - Accommodation Type for UI Selection

struct AccommodationType: Identifiable {
    let id: String
    let name: String
    let icon: String
    let description: String
}

extension AccommodationType {
    static let allTypes: [AccommodationType] = [
        AccommodationType(
            id: "hotel",
            name: "Hôtel",
            icon: "bed.double.fill",
            description: "Confort et service professionnel"
        ),
        AccommodationType(
            id: "airbnb",
            name: "Airbnb",
            icon: "house.fill",
            description: "Comme chez soi"
        ),
        AccommodationType(
            id: "hostel",
            name: "Auberge",
            icon: "person.3.fill",
            description: "Économique et social"
        ),
        AccommodationType(
            id: "resort",
            name: "Resort",
            icon: "sun.max.fill",
            description: "Tout inclus et détente"
        ),
        AccommodationType(
            id: "apartment",
            name: "Appartement",
            icon: "building.2.fill",
            description: "Indépendance et espace"
        )
    ]
}

// MARK: - Trip Type for Hotel Filtering

enum AccommodationTripType: String, CaseIterable {
    case business = "business"
    case romantic = "romantic"
    case family = "family"
    case adventure = "adventure"
    case relaxation = "relaxation"
    case cultural = "cultural"
    case camping = "camping"
    case solo = "solo"
    
    var displayName: String {
        switch self {
        case .business: return String(localized: "Affaires")
        case .romantic: return String(localized: "Romantique")
        case .family: return String(localized: "Famille")
        case .adventure: return String(localized: "Aventure")
        case .relaxation: return String(localized: "Détente")
        case .cultural: return String(localized: "Culturel")
        case .camping: return String(localized: "Camping")
        case .solo: return String(localized: "Solo")
        }
    }
    
    var icon: String {
        switch self {
        case .business: return "briefcase.fill"
        case .romantic: return "heart.fill"
        case .family: return "figure.2.and.child.holdinghands"
        case .adventure: return "figure.hiking"
        case .relaxation: return "leaf.fill"
        case .cultural: return "building.columns.fill"
        case .camping: return "tent.fill"
        case .solo: return "person.fill"
        }
    }
    
    var description: String {
        switch self {
        case .business: return String(localized: "Voyage professionnel")
        case .romantic: return String(localized: "Escapade en amoureux")
        case .family: return String(localized: "Aventures en famille")
        case .adventure: return String(localized: "Exploration et sensations fortes")
        case .relaxation: return String(localized: "Repos et bien-être")
        case .cultural: return String(localized: "Découverte du patrimoine")
        case .camping: return String(localized: "Nature et plein air")
        case .solo: return String(localized: "Voyage en solo")
        }
    }
}

// MARK: - Board Type for Hotel Meals

enum HotelBoardType: String, CaseIterable {
    case roomOnly = "ROOM_ONLY"
    case breakfast = "BREAKFAST"
    case halfBoard = "HALF_BOARD"
    case fullBoard = "FULL_BOARD"
    case allInclusive = "ALL_INCLUSIVE"
    
    var displayName: String {
        switch self {
        case .roomOnly: return String(localized: "Chambre seule")
        case .breakfast: return String(localized: "Petit-déjeuner inclus")
        case .halfBoard: return String(localized: "Demi-pension")
        case .fullBoard: return String(localized: "Pension complète")
        case .allInclusive: return String(localized: "Tout inclus")
        }
    }
}

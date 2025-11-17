import Foundation

enum BookingStatus: String, Decodable {
    case pending = "pending"
    case confirmed = "confirmed"
    case cancelled = "cancelled"
    
    var displayName: String {
        switch self {
        case .pending: return "En attente"
        case .confirmed: return "Confirmé"
        case .cancelled: return "Annulé"
        }
    }
}

struct BookingOffer: Decodable, Identifiable {
    let id: String
    let destination: String
    let price: Double
    let type: String? // "flight", "hotel", etc.
    let currency: String? // Optionnel car peut ne pas être dans le JSON
    let departureDate: String?
    let returnDate: String?
    let airline: String?
    let description: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case destination
        case price
        case type
        case currency
        case departureDate = "departure_date"
        case returnDate = "return_date"
        case airline
        case description
    }
}

struct Booking: Decodable, Identifiable {
    let id: String
    let destination: String
    let status: BookingStatus
    let confirmationNumber: String
    let createdAt: String
    let price: Double?
    let currency: String?
    let departureDate: String?
    let returnDate: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case destination
        case status
        case confirmationNumber = "confirmation_number"
        case createdAt = "created_at"
        case price
        case currency
        case departureDate = "departure_date"
        case returnDate = "return_date"
    }
}

struct ConfirmBookingRequest: Encodable {
    let destination: String
    let offerId: String?
    
    enum CodingKeys: String, CodingKey {
        case destination
        case offerId = "offer_id"
    }
}

struct ConfirmBookingResponse: Decodable {
    let id: String
    let confirmationNumber: String
    let status: BookingStatus
    let destination: String
    let createdAt: String
    
    enum CodingKeys: String, CodingKey {
        case id
        case confirmationNumber = "confirmation_number"
        case status
        case destination
        case createdAt = "created_at"
    }
}


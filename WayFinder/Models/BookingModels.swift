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
        case id = "_id"
        case status
        case confirmationNumber = "confirmation_number"
        case createdAt = "createdAt"
        case price = "total_price"
        case currency
        case offerId = "offer_id"
        case tripDetails = "trip_details"
        case departureDate = "departure_date"
        case returnDate = "return_date"
    }
    
    enum TripDetailsKeys: String, CodingKey {
        case destination
        case departure_date
        case return_date
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Decode _id to id
        id = try container.decode(String.self, forKey: .id)
        
        // Decode status
        status = try container.decode(BookingStatus.self, forKey: .status)
        
        // Decode confirmation_number
        confirmationNumber = try container.decode(String.self, forKey: .confirmationNumber)
        
        // Decode createdAt - Mongoose timestamps return createdAt
        // Try as String first, then as Date
        let dateFormatter = ISO8601DateFormatter()
        dateFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let createdAtStr = try? container.decode(String.self, forKey: .createdAt) {
            createdAt = createdAtStr
        } else if let createdAtDate = try? container.decode(Date.self, forKey: .createdAt) {
            createdAt = dateFormatter.string(from: createdAtDate)
        } else {
            // Fallback: use current date as ISO8601 string
            createdAt = dateFormatter.string(from: Date())
        }
        
        // Decode total_price to price
        price = try? container.decode(Double.self, forKey: .price)
        
        // Decode currency (might not exist)
        currency = try? container.decode(String.self, forKey: .currency)
        
        // Decode destination from trip_details.destination or use offer_id as fallback
        var decodedDestination: String? = nil
        var decodedDepartureDate: String? = nil
        var decodedReturnDate: String? = nil
        
        // Try to get destination and dates from trip_details
        if let tripDetails = try? container.nestedContainer(keyedBy: TripDetailsKeys.self, forKey: .tripDetails) {
            decodedDestination = try? tripDetails.decode(String.self, forKey: .destination)
            decodedDepartureDate = try? tripDetails.decode(String.self, forKey: .departure_date)
            decodedReturnDate = try? tripDetails.decode(String.self, forKey: .return_date)
        }
        
        // Fallback to offer_id if destination is not in trip_details
        if decodedDestination == nil || decodedDestination?.isEmpty == true {
            decodedDestination = try? container.decode(String.self, forKey: .offerId)
        }
        
        // Fallback to "N/A" if still no destination
        destination = decodedDestination ?? "N/A"
        
        // If dates weren't found in trip_details, try root level
        if decodedDepartureDate == nil {
            decodedDepartureDate = try? container.decode(String.self, forKey: .departureDate)
        }
        if decodedReturnDate == nil {
            decodedReturnDate = try? container.decode(String.self, forKey: .returnDate)
        }
        
        // Initialize dates once
        departureDate = decodedDepartureDate
        returnDate = decodedReturnDate
    }
    
    // Initializer pour les previews et tests
    init(id: String, destination: String, status: BookingStatus, confirmationNumber: String, createdAt: String, price: Double?, currency: String?, departureDate: String?, returnDate: String?) {
        self.id = id
        self.destination = destination
        self.status = status
        self.confirmationNumber = confirmationNumber
        self.createdAt = createdAt
        self.price = price
        self.currency = currency
        self.departureDate = departureDate
        self.returnDate = returnDate
    }
}

// Note: ConfirmBookingRequest is now handled directly with JSONSerialization in BookingService
// This struct is kept for reference but not used directly
struct ConfirmBookingRequest: Encodable {
    let offerId: String
    let paymentDetails: [String: String]
    let totalPrice: Double?
    
    enum CodingKeys: String, CodingKey {
        case offerId = "offer_id"
        case paymentDetails = "payment_details"
        case totalPrice = "total_price"
    }
}

struct ConfirmBookingResponse: Decodable {
    let id: String
    let confirmationNumber: String
    let status: BookingStatus
    let destination: String?
    let createdAt: String
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case confirmationNumber = "confirmation_number"
        case status
        case destination
        case offerId = "offer_id"
        case createdAt = "createdAt"
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Decode _id to id
        id = try container.decode(String.self, forKey: .id)
        
        // Decode confirmation_number to confirmationNumber
        confirmationNumber = try container.decode(String.self, forKey: .confirmationNumber)
        
        // Decode status
        status = try container.decode(BookingStatus.self, forKey: .status)
        
        // Try to decode destination, fallback to offer_id if not present
        let decodedDestination = try? container.decode(String.self, forKey: .destination)
        destination = decodedDestination ?? (try? container.decode(String.self, forKey: .offerId))
        
        // Decode createdAt
        createdAt = try container.decode(String.self, forKey: .createdAt)
    }
}

struct TravelDates: Codable {
    let departure: String?
    let returnDate: String?
    
    enum CodingKeys: String, CodingKey {
        case departure
        case returnDate = "return"
    }
}

struct OfferComparison: Decodable {
    let offerId: String
    let alternatives: [BookingOffer]?
    let recommendations: [String]?
    let bestPrice: Double?
    let averagePrice: Double?
    
    enum CodingKeys: String, CodingKey {
        case offerId = "offer_id"
        case alternatives
        case recommendations
        case bestPrice = "best_price"
        case averagePrice = "average_price"
    }
}

struct CreateBookingRequest: Encodable {
    let destinationId: String?
    let offerId: String?
    let travelDates: TravelDates?
    let passengers: Int?
    let totalPrice: Double?
    let currency: String?
    let status: String?
    
    enum CodingKeys: String, CodingKey {
        case destinationId = "destination_id"
        case offerId = "offer_id"
        case travelDates = "travel_dates"
        case passengers
        case totalPrice = "total_price"
        case currency
        case status
    }
}

struct UpdateBookingRequest: Encodable {
    let status: String?
    let travelDates: TravelDates?
    let passengers: Int?
    let totalPrice: Double?
    let currency: String?
    
    enum CodingKeys: String, CodingKey {
        case status
        case travelDates = "travel_dates"
        case passengers
        case totalPrice = "total_price"
        case currency
    }
}


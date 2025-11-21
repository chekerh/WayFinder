import Foundation

struct FlightDestination: Codable, Identifiable, Hashable {
    let id: String
    let name: String
    let city: String
    let country: String
    let imageUrl: String?
    let price: Double?
    let currency: String
    let description: String?
    let departureDate: String?
    let arrivalDate: String?
    let airline: String?
}


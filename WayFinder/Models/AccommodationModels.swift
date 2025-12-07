import Foundation

struct Accommodation: Identifiable, Codable {
    let id: String
    let name: String
    let type: String
    let price: Double
    let currency: String
    let rating: Double
    let imageUrl: String?
    let location: String
    let amenities: [String]
}

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
            description: "Expérience locale authentique"
        ),
        AccommodationType(
            id: "hostel",
            name: "Auberge",
            icon: "person.3.fill",
            description: "Économique et convivial"
        ),
        AccommodationType(
            id: "resort",
            name: "Résort",
            icon: "beach.umbrella.fill",
            description: "Luxe et détente"
        ),
        AccommodationType(
            id: "apartment",
            name: "Appartement",
            icon: "building.2.fill",
            description: "Indépendance et espace"
        )
    ]
}


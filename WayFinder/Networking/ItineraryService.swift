import Foundation

@MainActor
final class ItineraryService {
    static let shared = ItineraryService()
    private init() {}
    
    /// Crée un itinéraire
    func createItinerary(
        title: String,
        destination: String,
        startDate: String,
        endDate: String,
        description: String? = nil,
        days: [DayPlanDto]? = nil,
        tags: [String]? = nil,
        isPublic: Bool? = nil,
        totalBudget: Double? = nil,
        currency: String? = nil
    ) async throws -> Itinerary {
        let request = CreateItineraryRequest(
            title: title,
            description: description,
            destination: destination,
            startDate: startDate,
            endDate: endDate,
            days: days,
            tags: tags,
            isPublic: isPublic,
            totalBudget: totalBudget,
            currency: currency
        )
        
        let encoder = JSONEncoder()
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "itinerary",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: Itinerary.self)
    }
    
    /// Récupère tous les itinéraires de l'utilisateur
    func getAllItineraries(includePublic: Bool = false) async throws -> [Itinerary] {
        var queryItems: [URLQueryItem]? = nil
        if includePublic {
            queryItems = [URLQueryItem(name: "includePublic", value: "true")]
        }
        
        let builder = DefaultRequest(
            method: "GET",
            path: "itinerary",
            queryItems: queryItems
        )
        return try await APIService.shared.request(builder, decodeTo: [Itinerary].self)
    }
    
    /// Récupère un itinéraire par ID
    func getItinerary(id: String) async throws -> Itinerary {
        let builder = DefaultRequest(
            method: "GET",
            path: "itinerary/\(id)"
        )
        return try await APIService.shared.request(builder, decodeTo: Itinerary.self)
    }
    
    /// Met à jour un itinéraire
    func updateItinerary(
        id: String,
        title: String? = nil,
        destination: String? = nil,
        startDate: String? = nil,
        endDate: String? = nil,
        description: String? = nil,
        days: [DayPlanDto]? = nil,
        tags: [String]? = nil,
        isPublic: Bool? = nil,
        totalBudget: Double? = nil,
        currency: String? = nil
    ) async throws -> Itinerary {
        let request = UpdateItineraryRequest(
            title: title,
            description: description,
            destination: destination,
            startDate: startDate,
            endDate: endDate,
            days: days,
            tags: tags,
            isPublic: isPublic,
            totalBudget: totalBudget,
            currency: currency
        )
        
        let encoder = JSONEncoder()
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "PUT",
            path: "itinerary/\(id)",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: Itinerary.self)
    }
    
    /// Supprime un itinéraire
    func deleteItinerary(id: String) async throws {
        let builder = DefaultRequest(
            method: "DELETE",
            path: "itinerary/\(id)"
        )
        let _: EmptyResponse = try await APIService.shared.request(builder, decodeTo: EmptyResponse.self)
    }
    
    /// Ajoute une activité à un jour spécifique
    func addActivity(
        itineraryId: String,
        dayDate: String,
        activity: ActivityDto
    ) async throws -> Itinerary {
        let encoder = JSONEncoder()
        let body = try encoder.encode(activity)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "itinerary/\(itineraryId)/days/\(dayDate)/activities",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: Itinerary.self)
    }
    
    /// Supprime une activité d'un jour spécifique
    func removeActivity(
        itineraryId: String,
        dayDate: String,
        activityIndex: Int
    ) async throws -> Itinerary {
        let builder = DefaultRequest(
            method: "DELETE",
            path: "itinerary/\(itineraryId)/days/\(dayDate)/activities/\(activityIndex)"
        )
        return try await APIService.shared.request(builder, decodeTo: Itinerary.self)
    }
}


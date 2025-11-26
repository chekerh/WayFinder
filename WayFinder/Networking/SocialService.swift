import Foundation

final class SocialService {
    static let shared = SocialService()
    private init() {}
    
    func shareTrip(_ request: ShareTripRequest) async throws -> SharedTrip {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "social/share-trip",
            body: body
        )
        
        return try await APIService.shared.request(builder, decodeTo: SharedTrip.self)
    }
    
    func updateSharedTrip(id: String, request: UpdateSharedTripRequest) async throws -> SharedTrip {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "PUT",
            path: "social/share-trip/\(id)",
            body: body
        )
        
        return try await APIService.shared.request(builder, decodeTo: SharedTrip.self)
    }
    
    func deleteSharedTrip(id: String) async throws -> [String: String] {
        let builder = DefaultRequest(
            method: "DELETE",
            path: "social/share-trip/\(id)"
        )
        
        return try await APIService.shared.request(builder, decodeTo: [String: String].self)
    }
    
    func getSharedTrip(id: String) async throws -> SharedTrip {
        let builder = DefaultRequest(
            method: "GET",
            path: "social/share-trip/\(id)"
        )
        
        return try await APIService.shared.request(builder, decodeTo: SharedTrip.self)
    }
    
    func getUserSharedTrips(userId: String, limit: Int = 20, skip: Int = 0) async throws -> [SharedTrip] {
        let queryItems = [
            URLQueryItem(name: "limit", value: "\(limit)"),
            URLQueryItem(name: "skip", value: "\(skip)")
        ]
        
        let builder = DefaultRequest(
            method: "GET",
            path: "social/user/\(userId)/shared-trips",
            queryItems: queryItems
        )
        
        return try await APIService.shared.request(builder, decodeTo: [SharedTrip].self)
    }
    
    func getSocialFeed(limit: Int = 20, skip: Int = 0) async throws -> [SharedTrip] {
        let queryItems = [
            URLQueryItem(name: "limit", value: "\(limit)"),
            URLQueryItem(name: "skip", value: "\(skip)")
        ]
        
        let builder = DefaultRequest(
            method: "GET",
            path: "social/feed",
            queryItems: queryItems
        )
        
        return try await APIService.shared.request(builder, decodeTo: [SharedTrip].self)
    }
    
    func likeSharedTrip(id: String) async throws -> LikeResponse {
        let builder = DefaultRequest(
            method: "POST",
            path: "social/share-trip/\(id)/like"
        )
        
        return try await APIService.shared.request(builder, decodeTo: LikeResponse.self)
    }
}


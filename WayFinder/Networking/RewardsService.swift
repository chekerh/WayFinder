import Foundation

@MainActor
final class RewardsService {
    static let shared = RewardsService()
    private init() {}
    
    /// Récupère les points de l'utilisateur
    func getUserPoints() async throws -> UserPoints {
        let builder = DefaultRequest(
            method: "GET",
            path: "rewards/points"
        )
        return try await APIService.shared.request(builder, decodeTo: UserPoints.self)
    }
    
    /// Utilise des points pour une réduction
    func redeemPoints(points: Int, description: String, metadata: [String: Any]? = nil) async throws -> RedeemPointsResponse {
        var json: [String: Any] = [
            "points": points,
            "description": description
        ]
        if let metadata = metadata {
            json["metadata"] = metadata
        }
        
        let body = try JSONSerialization.data(withJSONObject: json)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "rewards/redeem",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: RedeemPointsResponse.self)
    }
}

struct UserPoints: Decodable {
    let totalPoints: Int
    let availablePoints: Int
    let lifetimePoints: Int
    let recentTransactions: [PointsTransaction]?
    
    enum CodingKeys: String, CodingKey {
        case totalPoints = "total_points"
        case availablePoints = "available_points"
        case lifetimePoints = "lifetime_points"
        case recentTransactions = "recent_transactions"
    }
}

struct PointsTransaction: Decodable {
    let transactionId: String
    let userId: String
    let points: Int
    let type: String
    let source: String
    let description: String?
    let transactionDate: Date
    
    enum CodingKeys: String, CodingKey {
        case transactionId = "transaction_id"
        case userId = "user_id"
        case points
        case type
        case source
        case description
        case transactionDate = "transaction_date"
    }
}

struct RedeemPointsResponse: Decodable {
    let transactionId: String
    let remainingPoints: Int
    
    enum CodingKeys: String, CodingKey {
        case transactionId = "transaction_id"
        case remainingPoints = "remaining_points"
    }
}


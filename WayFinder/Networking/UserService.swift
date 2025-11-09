import Foundation

final class UserService {
    static let shared = UserService()
    private init() {}

    func fetchProfile() async throws -> UserProfile {
        guard let token = TokenStorage.fetch() else {
            throw APIError.custom("Token manquant")
        }

        let builder = DefaultRequest(
            method: "GET",
            path: "user/profile",
            headers: [
                "Authorization": "Bearer \(token)",
                "Content-Type": "application/json"
            ]
        )

        return try await APIService.shared.request(builder, decodeTo: UserProfile.self)
    }
}


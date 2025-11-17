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
    
    /// Met à jour le nom de l'utilisateur
    func updateName(firstName: String, lastName: String) async throws -> UserProfile {
        guard let token = TokenStorage.fetch() else {
            throw APIError.custom("Token manquant")
        }
        
        let request = UpdateNameRequest(firstName: firstName, lastName: lastName)
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "PUT",
            path: "user/profile/name",
            headers: [
                "Authorization": "Bearer \(token)",
                "Content-Type": "application/json"
            ],
            body: body
        )
        
        return try await APIService.shared.request(builder, decodeTo: UserProfile.self)
    }
    
    /// Met à jour le mot de passe
    func updatePassword(currentPassword: String, newPassword: String) async throws {
        guard let token = TokenStorage.fetch() else {
            throw APIError.custom("Token manquant")
        }
        
        let request = UpdatePasswordRequest(currentPassword: currentPassword, newPassword: newPassword)
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "PUT",
            path: "user/profile/password",
            headers: [
                "Authorization": "Bearer \(token)",
                "Content-Type": "application/json"
            ],
            body: body
        )
        
        let _: EmptyResponse = try await APIService.shared.request(builder, decodeTo: EmptyResponse.self)
    }
    
    /// Met à jour l'email
    func updateEmail(email: String) async throws -> UserProfile {
        guard let token = TokenStorage.fetch() else {
            throw APIError.custom("Token manquant")
        }
        
        let request = UpdateEmailRequest(email: email)
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "PUT",
            path: "user/profile/email",
            headers: [
                "Authorization": "Bearer \(token)",
                "Content-Type": "application/json"
            ],
            body: body
        )
        
        return try await APIService.shared.request(builder, decodeTo: UserProfile.self)
    }
}

// MARK: - Request Models
struct UpdateNameRequest: Encodable {
    let firstName: String
    let lastName: String
    
    enum CodingKeys: String, CodingKey {
        case firstName = "first_name"
        case lastName = "last_name"
    }
}

struct UpdatePasswordRequest: Encodable {
    let currentPassword: String
    let newPassword: String
    
    enum CodingKeys: String, CodingKey {
        case currentPassword = "current_password"
        case newPassword = "new_password"
    }
}

struct UpdateEmailRequest: Encodable {
    let email: String
}

struct EmptyResponse: Decodable {
}


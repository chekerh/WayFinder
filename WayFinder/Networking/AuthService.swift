import Foundation

@MainActor
final class AuthService {
    static let shared = AuthService()
    private init() {}

    func login(email: String, password: String) async throws -> UserProfile {
        let identifier = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let username: String
        if let atIndex = identifier.firstIndex(of: "@") {
            username = String(identifier[..<atIndex])
        } else {
            username = identifier
        }

        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let data = try encoder.encode(LoginRequest(username: username, password: password))

        let builder = DefaultRequest(
            method: "POST",
            path: "auth/login",
            headers: ["Content-Type": "application/json"],
            body: data
        )

        let response = try await APIService.shared.request(builder, decodeTo: LoginResponse.self)
        TokenStorage.save(token: response.accessToken)

        if let user = response.user {
            return user
        }

        return try await UserService.shared.fetchProfile()
    }

    func register(username: String,
                  email: String,
                  firstName: String,
                  lastName: String,
                  password: String) async throws -> RegisterResponse {
        let requestBody = RegisterRequest(
            username: username,
            email: email,
            firstName: firstName,
            lastName: lastName,
            password: password
        )

        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let data = try encoder.encode(requestBody)

        let builder = DefaultRequest(
            method: "POST",
            path: "auth/register",
            headers: ["Content-Type": "application/json"],
            body: data
        )

        return try await APIService.shared.request(builder, decodeTo: RegisterResponse.self)
    }

    func logout() {
        TokenStorage.delete()
    }
}


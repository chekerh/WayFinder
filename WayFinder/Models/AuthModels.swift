import Foundation

struct LoginRequest: Encodable {
    let username: String
    let password: String
}

struct GoogleLoginRequest: Encodable {
    let idToken: String
}

struct AppleLoginRequest: Encodable {
    let identityToken: String
    let email: String?
    let firstName: String?
    let lastName: String?
}

struct LoginResponse: Decodable {
    let accessToken: String
    let refreshToken: String?
    let user: UserProfile?
    let onboardingCompleted: Bool?
}

struct UserProfile: Decodable {
    let id: String?
    let email: String?
    let username: String?
    let firstName: String?
    let lastName: String?
    let avatarUrl: String?
}

struct RegisterRequest: Encodable {
    let username: String
    let email: String
    let firstName: String
    let lastName: String
    let password: String
}

struct RegisterResponse: Decodable {
    let message: String
    let user: UserProfile?
}


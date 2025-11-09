import Foundation

struct LoginRequest: Encodable {
    let username: String
    let password: String
}

struct LoginResponse: Decodable {
    let accessToken: String
    let refreshToken: String?
    let user: UserProfile?
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
    let id: String
    let username: String
    let email: String
}

enum BookingStatus: String, Decodable {
    case pending
    case confirmed
    case cancelled
}

struct Booking: Decodable {
    let id: String
    let status: BookingStatus
    let createdAt: Date
}


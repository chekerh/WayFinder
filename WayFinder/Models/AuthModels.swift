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
    
    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case refreshToken = "refresh_token"
        case user
        case onboardingCompleted = "onboarding_completed"
    }
}

struct UserProfile: Decodable {
    let id: String?
    let email: String?
    let username: String?
    let firstName: String?
    let lastName: String?
    let avatarUrl: String?
    let profileImageUrl: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case email
        case username
        case firstName = "first_name"
        case lastName = "last_name"
        case avatarUrl
        case profileImageUrl = "profile_image_url"
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Handle MongoDB _id
        if let idString = try? container.decode(String.self, forKey: .id) {
            id = idString
        } else if let idDict = try? container.decode([String: String].self, forKey: .id),
                  let idValue = idDict["$oid"] {
            id = idValue
        } else {
            id = nil
        }
        
        email = try container.decodeIfPresent(String.self, forKey: .email)
        username = try container.decodeIfPresent(String.self, forKey: .username)
        firstName = try container.decodeIfPresent(String.self, forKey: .firstName)
        lastName = try container.decodeIfPresent(String.self, forKey: .lastName)
        avatarUrl = try container.decodeIfPresent(String.self, forKey: .avatarUrl)
        profileImageUrl = try container.decodeIfPresent(String.self, forKey: .profileImageUrl)
    }
}

extension UserProfile {
    var displayNameValue: String {
        if let firstName = firstName, !firstName.isEmpty,
           let lastName = lastName, !lastName.isEmpty {
            return "\(firstName) \(lastName)"
        }
        if let firstName = firstName, !firstName.isEmpty {
            return firstName
        }
        if let username = username, !username.isEmpty {
            return username
        }
        if let email = email, !email.isEmpty {
            return email
        }
        return "Utilisateur"
    }
    
    var resolvedProfileImageUrl: String? {
        profileImageUrl ?? avatarUrl
    }
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

struct SendOTPRequest: Encodable {
    let email: String
}

struct SendOTPResponse: Decodable {
    let message: String
    let email: String?
    
    enum CodingKeys: String, CodingKey {
        case message
        case email
    }
}

struct VerifyOTPRequest: Encodable {
    let email: String
    let code: String
}

struct VerifyOTPResponse: Decodable {
    let accessToken: String
    let user: UserProfile?
    let onboardingCompleted: Bool?
    
    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case user
        case onboardingCompleted = "onboarding_completed"
    }
}


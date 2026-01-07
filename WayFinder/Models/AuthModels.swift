import Foundation

struct EmailLoginRequest: Encodable {
    let email: String
    let password: String
}

struct GoogleLoginRequest: Encodable {
    let idToken: String
    let clientType: String
    
    enum CodingKeys: String, CodingKey {
        case idToken = "id_token"
        case clientType = "client_type"
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(idToken, forKey: .idToken)
        try container.encode(clientType, forKey: .clientType)
    }
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

struct UserProfile: Codable {
    let id: String?
    let email: String?
    let username: String?
    let firstName: String?
    let lastName: String?
    let avatarUrl: String?
    let profileImageUrl: String?
    let preferences: [String]?
    let onboardingCompleted: Bool?
    let onboardingSkipped: Bool?
    
    // Points and rewards
    let totalPoints: Int?
    let lifetimePoints: Int?
    let currentStreak: Int?
    let longestStreak: Int?
    
    // Lifetime metrics
    let totalBookings: Int?
    let totalDestinations: Int?
    let totalTravelDays: Int?
    let totalDistanceKm: Int?
    let totalCountries: Int?
    let totalOutfitsAnalyzed: Int?
    let totalPostsShared: Int?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case email
        case username
        case firstName = "first_name"
        case lastName = "last_name"
        case avatarUrl
        case profileImageUrl = "profile_image_url"
        case preferences
        case onboardingCompleted = "onboarding_completed"
        case onboardingSkipped = "onboarding_skipped"
        case totalPoints = "total_points"
        case lifetimePoints = "lifetime_points"
        case currentStreak = "current_streak"
        case longestStreak = "longest_streak"
        case totalBookings = "total_bookings"
        case totalDestinations = "total_destinations"
        case totalTravelDays = "total_travel_days"
        case totalDistanceKm = "total_distance_km"
        case totalCountries = "total_countries"
        case totalOutfitsAnalyzed = "total_outfits_analyzed"
        case totalPostsShared = "total_posts_shared"
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
        preferences = try container.decodeIfPresent([String].self, forKey: .preferences)
        onboardingCompleted = try container.decodeIfPresent(Bool.self, forKey: .onboardingCompleted)
        onboardingSkipped = try container.decodeIfPresent(Bool.self, forKey: .onboardingSkipped)
        
        // Points and rewards
        totalPoints = try container.decodeIfPresent(Int.self, forKey: .totalPoints)
        lifetimePoints = try container.decodeIfPresent(Int.self, forKey: .lifetimePoints)
        currentStreak = try container.decodeIfPresent(Int.self, forKey: .currentStreak)
        longestStreak = try container.decodeIfPresent(Int.self, forKey: .longestStreak)
        
        // Lifetime metrics
        totalBookings = try container.decodeIfPresent(Int.self, forKey: .totalBookings)
        totalDestinations = try container.decodeIfPresent(Int.self, forKey: .totalDestinations)
        totalTravelDays = try container.decodeIfPresent(Int.self, forKey: .totalTravelDays)
        totalDistanceKm = try container.decodeIfPresent(Int.self, forKey: .totalDistanceKm)
        totalCountries = try container.decodeIfPresent(Int.self, forKey: .totalCountries)
        totalOutfitsAnalyzed = try container.decodeIfPresent(Int.self, forKey: .totalOutfitsAnalyzed)
        totalPostsShared = try container.decodeIfPresent(Int.self, forKey: .totalPostsShared)
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encodeIfPresent(id, forKey: .id)
        try container.encodeIfPresent(email, forKey: .email)
        try container.encodeIfPresent(username, forKey: .username)
        try container.encodeIfPresent(firstName, forKey: .firstName)
        try container.encodeIfPresent(lastName, forKey: .lastName)
        try container.encodeIfPresent(avatarUrl, forKey: .avatarUrl)
        try container.encodeIfPresent(profileImageUrl, forKey: .profileImageUrl)
        try container.encodeIfPresent(preferences, forKey: .preferences)
        try container.encodeIfPresent(onboardingCompleted, forKey: .onboardingCompleted)
        try container.encodeIfPresent(onboardingSkipped, forKey: .onboardingSkipped)
        try container.encodeIfPresent(totalPoints, forKey: .totalPoints)
        try container.encodeIfPresent(lifetimePoints, forKey: .lifetimePoints)
        try container.encodeIfPresent(currentStreak, forKey: .currentStreak)
        try container.encodeIfPresent(longestStreak, forKey: .longestStreak)
        try container.encodeIfPresent(totalBookings, forKey: .totalBookings)
        try container.encodeIfPresent(totalDestinations, forKey: .totalDestinations)
        try container.encodeIfPresent(totalTravelDays, forKey: .totalTravelDays)
        try container.encodeIfPresent(totalDistanceKm, forKey: .totalDistanceKm)
        try container.encodeIfPresent(totalCountries, forKey: .totalCountries)
        try container.encodeIfPresent(totalOutfitsAnalyzed, forKey: .totalOutfitsAnalyzed)
        try container.encodeIfPresent(totalPostsShared, forKey: .totalPostsShared)
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
    
    enum CodingKeys: String, CodingKey {
        case username
        case email
        case firstName = "first_name"
        case lastName = "last_name"
        case password
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(username, forKey: .username)
        try container.encode(email, forKey: .email)
        try container.encode(firstName, forKey: .firstName)
        try container.encode(lastName, forKey: .lastName)
        try container.encode(password, forKey: .password)
    }
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

// Registration OTP Models
struct SendOTPForRegistrationRequest: Encodable {
    let email: String
}

struct SendOTPForRegistrationResponse: Decodable {
    let message: String
}

struct RegisterWithOTPRequest: Encodable {
    let email: String
    let firstName: String
    let lastName: String
    let password: String
    let otpCode: String
    
    enum CodingKeys: String, CodingKey {
        case email
        case firstName = "first_name"
        case lastName = "last_name"
        case password
        case otpCode = "otp_code"
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(email, forKey: .email)
        try container.encode(firstName, forKey: .firstName)
        try container.encode(lastName, forKey: .lastName)
        try container.encode(password, forKey: .password)
        try container.encode(otpCode, forKey: .otpCode)
    }
}

struct RegisterWithOTPResponse: Decodable {
    let message: String
    let user: UserProfile?
}

// Password Reset Models
struct RequestPasswordResetRequest: Encodable {
    let email: String
}

struct RequestPasswordResetResponse: Decodable {
    let message: String
    let email: String
}

struct ResetPasswordRequest: Encodable {
    let email: String
    let otpCode: String
    let newPassword: String

    enum CodingKeys: String, CodingKey {
        case email
        case otpCode = "code"
        case newPassword = "new_password"
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(email, forKey: .email)
        try container.encode(otpCode, forKey: .otpCode)
        try container.encode(newPassword, forKey: .newPassword)
    }
}

struct ResetPasswordResponse: Decodable {
    let message: String
}


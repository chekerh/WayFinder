import Foundation

@MainActor
final class AuthService {
    static let shared = AuthService()
    private init() {}

    func login(email: String, password: String) async throws -> UserProfile {
        let identifier = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let username: String
        if let atIndex = identifier.firstIndex(of: "@") {
            username = String(identifier[..<atIndex]).lowercased()
        } else {
            username = identifier.lowercased()
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
            // Sauvegarder le profil avec l'email pour la persistance
            UserStorage.saveProfile(user)
            
            // Restaurer l'image après reconnexion via ProfileImageService
            if let email = user.email {
                await ProfileImageService.shared.restoreAfterLogin(email: email)
                if let imageUrl = user.resolvedProfileImageUrl ?? UserStorage.fetchProfileImageUrl() {
                    await ProfileImageService.shared.updateProfileImage(imageUrl, email: email)
                }
                print("✅ [AuthService] Profile saved and image restored for email: \(email)")
            } else {
                print("✅ [AuthService] Profile saved")
            }
            
            return user
        }

        let profile = try await UserService.shared.fetchProfile()
        // Sauvegarder le profil récupéré avec l'email
        UserStorage.saveProfile(profile)
        
        // Restaurer l'image après reconnexion via ProfileImageService
        if let email = profile.email {
            await ProfileImageService.shared.restoreAfterLogin(email: email)
            if let imageUrl = profile.resolvedProfileImageUrl ?? UserStorage.fetchProfileImageUrl() {
                await ProfileImageService.shared.updateProfileImage(imageUrl, email: email)
            }
        }
        
        return profile
    }

    func loginWithGoogle(idToken: String) async throws -> UserProfile {
        let encoder = JSONEncoder()
        let data = try encoder.encode(GoogleLoginRequest(idToken: idToken))

        let builder = DefaultRequest(
            method: "POST",
            path: "auth/google",
            headers: ["Content-Type": "application/json"],
            body: data
        )

        let response = try await APIService.shared.request(builder, decodeTo: LoginResponse.self)
        TokenStorage.save(token: response.accessToken)

        if let user = response.user {
            // Sauvegarder le profil avec l'email pour la persistance
            UserStorage.saveProfile(user)
            print("✅ [AuthService] Google profile saved with email: \(user.email ?? "nil")")
            return user
        }

        let profile = try await UserService.shared.fetchProfile()
        // Sauvegarder le profil récupéré avec l'email
        UserStorage.saveProfile(profile)
        return profile
    }

    func loginWithApple(identityToken: String,
                        email: String?,
                        firstName: String?,
                        lastName: String?) async throws -> UserProfile {
        let requestBody = AppleLoginRequest(
            identityToken: identityToken,
            email: email,
            firstName: firstName,
            lastName: lastName
        )

        let encoder = JSONEncoder()
        let data = try encoder.encode(requestBody)

        let builder = DefaultRequest(
            method: "POST",
            path: "auth/apple",
            headers: ["Content-Type": "application/json"],
            body: data
        )

        let response = try await APIService.shared.request(builder, decodeTo: LoginResponse.self)
        TokenStorage.save(token: response.accessToken)

        if let user = response.user {
            // Sauvegarder le profil avec l'email pour la persistance
            UserStorage.saveProfile(user)
            print("✅ [AuthService] Apple profile saved with email: \(user.email ?? "nil")")
            return user
        }

        let profile = try await UserService.shared.fetchProfile()
        // Sauvegarder le profil récupéré avec l'email
        UserStorage.saveProfile(profile)
        return profile
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
        UserStorage.clear()
        PreferenceStorage.clearPreferenceId()
    }
}


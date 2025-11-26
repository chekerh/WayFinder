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

        // Register FCM token after login
        await registerFcmTokenIfAvailable()

        if let user = response.user {
            // Sauvegarder le profil avec l'email pour la persistance
            UserStorage.saveProfile(user)
            
            // Restaurer l'image après reconnexion via ProfileImageService
            if let email = user.email {
                ProfileImageService.shared.restoreAfterLogin(email: email)
                if let imageUrl = user.resolvedProfileImageUrl ?? UserStorage.fetchProfileImageUrl() {
                    ProfileImageService.shared.updateProfileImage(imageUrl, email: email)
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
            ProfileImageService.shared.restoreAfterLogin(email: email)
            if let imageUrl = profile.resolvedProfileImageUrl ?? UserStorage.fetchProfileImageUrl() {
                ProfileImageService.shared.updateProfileImage(imageUrl, email: email)
            }
        }
        
        return profile
    }

    /// Connexion avec Google Sign In
    /// - Parameter idToken: Token d'identification Google obtenu via GoogleSignIn SDK
    /// - Note: Le backend Render doit avoir la variable d'environnement GOOGLE_CLIENT_ID_WEB configurée
    ///   pour valider le token Google. Le GOOGLE_CLIENT_ID dans Info.plist est utilisé côté iOS.
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

        // Register FCM token after login
        await registerFcmTokenIfAvailable()

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

        // Register FCM token after login
        await registerFcmTokenIfAvailable()

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
    
    /// Envoie un code OTP à l'email de l'utilisateur
    func sendOTP(email: String) async throws -> SendOTPResponse {
        let encoder = JSONEncoder()
        let data = try encoder.encode(SendOTPRequest(email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()))

        let builder = DefaultRequest(
            method: "POST",
            path: "auth/send-otp",
            headers: ["Content-Type": "application/json"],
            body: data
        )

        return try await APIService.shared.request(builder, decodeTo: SendOTPResponse.self)
    }

    /// Vérifie le code OTP et connecte l'utilisateur
    func verifyOTP(email: String, code: String) async throws -> UserProfile {
        let encoder = JSONEncoder()
        let data = try encoder.encode(VerifyOTPRequest(
            email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            code: code.trimmingCharacters(in: .whitespacesAndNewlines)
        ))

        let builder = DefaultRequest(
            method: "POST",
            path: "auth/verify-otp",
            headers: ["Content-Type": "application/json"],
            body: data
        )

        let response = try await APIService.shared.request(builder, decodeTo: VerifyOTPResponse.self)
        TokenStorage.save(token: response.accessToken)

        // Register FCM token after login
        await registerFcmTokenIfAvailable()

        if let user = response.user {
            // Sauvegarder le profil avec l'email pour la persistance
            UserStorage.saveProfile(user)
            
            // Restaurer l'image après reconnexion via ProfileImageService
            if let email = user.email {
                ProfileImageService.shared.restoreAfterLogin(email: email)
                if let imageUrl = user.resolvedProfileImageUrl ?? UserStorage.fetchProfileImageUrl() {
                    ProfileImageService.shared.updateProfileImage(imageUrl, email: email)
                }
                print("✅ [AuthService] OTP login - Profile saved and image restored for email: \(email)")
            } else {
                print("✅ [AuthService] OTP login - Profile saved")
            }
            
            return user
        }

        let profile = try await UserService.shared.fetchProfile()
        // Sauvegarder le profil récupéré avec l'email
        UserStorage.saveProfile(profile)
        
        // Restaurer l'image après reconnexion via ProfileImageService
        if let email = profile.email {
            ProfileImageService.shared.restoreAfterLogin(email: email)
            if let imageUrl = profile.resolvedProfileImageUrl ?? UserStorage.fetchProfileImageUrl() {
                ProfileImageService.shared.updateProfileImage(imageUrl, email: email)
            }
        }
        
        return profile
    }

    /// Enregistre le token FCM si disponible après la connexion
    private func registerFcmTokenIfAvailable() async {
        if let fcmToken = await FirebaseMessagingService.shared.fcmToken {
            do {
                try await FirebaseMessagingService.shared.registerTokenWithBackend(token: fcmToken)
                print("✅ [AuthService] FCM token registered after login")
            } catch {
                print("⚠️ [AuthService] Failed to register FCM token after login: \(error.localizedDescription)")
            }
        }
    }
}


import Foundation

@MainActor
final class AuthService {
    static let shared = AuthService()
    private init() {}

    func loginWithResponse(email: String, password: String) async throws -> LoginResponse {
        // Backend expects { email, password }
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !trimmedEmail.isEmpty else {
            throw NSError(domain: "AuthService", code: -1,
                          userInfo: [NSLocalizedDescriptionKey: "Identifiant requis"])
        }

        let encoder = JSONEncoder()
        let data = try encoder.encode(EmailLoginRequest(email: trimmedEmail, password: password))

        let builder = DefaultRequest(
            method: "POST",
            path: "auth/login",
            headers: ["Content-Type": "application/json"],
            body: data
        )

        print("🔐 [AuthService] Sending login request...")
        let response = try await APIService.shared.request(builder, decodeTo: LoginResponse.self)
        print("✅ [AuthService] Login response received")
        print("✅ [AuthService] Access token: \(response.accessToken.prefix(20))...")
        print("✅ [AuthService] User: \(response.user?.username ?? "nil")")
        print("✅ [AuthService] Onboarding completed: \(response.onboardingCompleted ?? false)")
        
        TokenStorage.save(token: response.accessToken)
        print("✅ [AuthService] Token saved to storage")

        // Register FCM token after login
        await registerFcmTokenIfAvailable()

        // Backend always returns user in login response (like Android)
        guard let user = response.user else {
            print("⚠️ [AuthService] No user in response, fetching profile...")
            let profile = try await UserService.shared.fetchProfile()
            UserStorage.saveProfile(profile)
            
            if let email = profile.email {
                ProfileImageService.shared.restoreAfterLogin(email: email)
                if let imageUrl = profile.resolvedProfileImageUrl ?? UserStorage.fetchProfileImageUrl() {
                    ProfileImageService.shared.updateProfileImage(imageUrl, email: email)
                }
            }
            
            // Return response with fetched profile (struct is immutable, so we need to work with what we have)
            // The response already has the token and onboarding status, just missing user
            // Since we can't modify the struct, we'll return the original response
            // and the caller should handle the user separately
            print("⚠️ [AuthService] Returning response without user (user fetched separately)")
            return response
        }
        
        // Sauvegarder le profil avec l'email pour la persistance
        UserStorage.saveProfile(user)
        print("✅ [AuthService] User profile saved")
        
        // Restaurer l'image après reconnexion via ProfileImageService
        if let email = user.email {
            ProfileImageService.shared.restoreAfterLogin(email: email)
            if let imageUrl = user.resolvedProfileImageUrl ?? UserStorage.fetchProfileImageUrl() {
                ProfileImageService.shared.updateProfileImage(imageUrl, email: email)
            }
            print("✅ [AuthService] Profile image restored for email: \(email)")
        } else {
            print("✅ [AuthService] Profile saved (no email)")
        }
        
        return response
    }
    
    func login(email: String, password: String) async throws -> UserProfile {
        // Backend expects { email, password }
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !trimmedEmail.isEmpty else {
            throw NSError(domain: "AuthService", code: -1,
                          userInfo: [NSLocalizedDescriptionKey: "Identifiant requis"])
        }

        let encoder = JSONEncoder()
        let data = try encoder.encode(EmailLoginRequest(email: trimmedEmail, password: password))

        let builder = DefaultRequest(
            method: "POST",
            path: "auth/login",
            headers: ["Content-Type": "application/json"],
            body: data
        )

        print("🔐 [AuthService] Sending login request...")
        let response = try await APIService.shared.request(builder, decodeTo: LoginResponse.self)
        print("✅ [AuthService] Login response received")
        print("✅ [AuthService] Access token: \(response.accessToken.prefix(20))...")
        print("✅ [AuthService] User: \(response.user?.username ?? "nil")")
        print("✅ [AuthService] Onboarding completed: \(response.onboardingCompleted ?? false)")
        
        TokenStorage.save(token: response.accessToken)
        print("✅ [AuthService] Token saved to storage")

        // Register FCM token after login
        await registerFcmTokenIfAvailable()

        // Backend always returns user in login response (like Android)
        guard let user = response.user else {
            print("⚠️ [AuthService] No user in response, fetching profile...")
            let profile = try await UserService.shared.fetchProfile()
            UserStorage.saveProfile(profile)
            
            if let email = profile.email {
                ProfileImageService.shared.restoreAfterLogin(email: email)
                if let imageUrl = profile.resolvedProfileImageUrl ?? UserStorage.fetchProfileImageUrl() {
                    ProfileImageService.shared.updateProfileImage(imageUrl, email: email)
                }
            }
            
            return profile
        }
        
        // Sauvegarder le profil avec l'email pour la persistance
        UserStorage.saveProfile(user)
        print("✅ [AuthService] User profile saved")
        
        // Restaurer l'image après reconnexion via ProfileImageService
        if let email = user.email {
            ProfileImageService.shared.restoreAfterLogin(email: email)
            if let imageUrl = user.resolvedProfileImageUrl ?? UserStorage.fetchProfileImageUrl() {
                ProfileImageService.shared.updateProfileImage(imageUrl, email: email)
            }
            print("✅ [AuthService] Profile image restored for email: \(email)")
        } else {
            print("✅ [AuthService] Profile saved (no email)")
        }
        
        return user
    }

    /// Connexion avec Google Sign In
    /// - Parameter idToken: Token d'identification Google obtenu via GoogleSignIn SDK
    /// - Note: Le backend Render doit avoir la variable d'environnement GOOGLE_CLIENT_ID_WEB configurée
    ///   pour valider le token Google. Le GOOGLE_CLIENT_ID dans Info.plist est utilisé côté iOS.
    func loginWithGoogle(idToken: String) async throws -> LoginResponse {
        let encoder = JSONEncoder()
        // iOS uses web client ID, so we need to specify client_type as 'web'
        let data = try encoder.encode(GoogleLoginRequest(idToken: idToken, clientType: "web"))

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
            print("✅ [AuthService] Google login - onboarding completed: \(response.onboardingCompleted ?? false)")
            
            // Restore profile image if available
            if let email = user.email {
                ProfileImageService.shared.restoreAfterLogin(email: email)
                if let imageUrl = user.resolvedProfileImageUrl ?? UserStorage.fetchProfileImageUrl() {
                    ProfileImageService.shared.updateProfileImage(imageUrl, email: email)
                }
            }
        } else {
            // If user not in response, fetch profile
            let profile = try await UserService.shared.fetchProfile()
            UserStorage.saveProfile(profile)
            print("✅ [AuthService] Google login - profile fetched")
        }
        
        return response
    }

    func loginWithApple(identityToken: String,
                        email: String?,
                        firstName: String?,
                        lastName: String?) async throws -> LoginResponse {
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
            print("✅ [AuthService] Apple login - onboarding completed: \(response.onboardingCompleted ?? false)")
            
            // Restore profile image if available
            if let email = user.email {
                ProfileImageService.shared.restoreAfterLogin(email: email)
                if let imageUrl = user.resolvedProfileImageUrl ?? UserStorage.fetchProfileImageUrl() {
                    ProfileImageService.shared.updateProfileImage(imageUrl, email: email)
                }
            }
        } else {
            // If user not in response, fetch profile
            let profile = try await UserService.shared.fetchProfile()
            UserStorage.saveProfile(profile)
            print("✅ [AuthService] Apple login - profile fetched")
        }
        
        return response
    }

    func register(username: String,
                  email: String,
                  firstName: String,
                  lastName: String,
                  password: String) async throws -> RegisterResponse {
        // Debug: Log all values before encoding
        print("🔍 [AuthService] Register values:")
        print("   username: \(username)")
        print("   email: \(email)")
        print("   firstName: \(firstName)")
        print("   lastName: \(lastName)")
        print("   password: \(password.isEmpty ? "(empty)" : "***")")
        
        let requestBody = RegisterRequest(
            username: username,
            email: email,
            firstName: firstName,
            lastName: lastName,
            password: password
        )

        let encoder = JSONEncoder()
        // Note: CodingKeys are explicitly defined in RegisterRequest, so keyEncodingStrategy is not needed
        let data = try encoder.encode(requestBody)
        
        // Debug: Verify the encoded JSON contains all fields
        if let jsonString = String(data: data, encoding: .utf8) {
            print("🔍 [AuthService] Register request JSON: \(jsonString)")
        } else {
            print("❌ [AuthService] Failed to convert encoded data to string")
        }

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
        // Vider tous les caches de données
        AppDataCache.shared.clearAll()
        print("✅ [AuthService] All caches cleared on logout")
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

    /// Send OTP for registration - checks that email does NOT exist
    func sendOTPForRegistration(email: String) async throws -> SendOTPForRegistrationResponse {
        let encoder = JSONEncoder()
        let data = try encoder.encode(SendOTPForRegistrationRequest(email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()))

        let builder = DefaultRequest(
            method: "POST",
            path: "auth/send-otp-for-registration",
            headers: ["Content-Type": "application/json"],
            body: data
        )

        return try await APIService.shared.request(builder, decodeTo: SendOTPForRegistrationResponse.self)
    }

    /// Register user with OTP verification
    func registerWithOTP(email: String,
                        firstName: String,
                        lastName: String,
                        password: String,
                        otpCode: String) async throws -> RegisterWithOTPResponse {
        let encoder = JSONEncoder()
        let data = try encoder.encode(RegisterWithOTPRequest(
            email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            firstName: firstName.trimmingCharacters(in: .whitespacesAndNewlines),
            lastName: lastName.trimmingCharacters(in: .whitespacesAndNewlines),
            password: password,
            otpCode: otpCode.trimmingCharacters(in: .whitespacesAndNewlines)
        ))

        let builder = DefaultRequest(
            method: "POST",
            path: "auth/register-with-otp",
            headers: ["Content-Type": "application/json"],
            body: data
        )

        let response = try await APIService.shared.request(builder, decodeTo: RegisterWithOTPResponse.self)
        
        print("✅ [AuthService] Registration with OTP successful: \(response.message)")
        
        // After successful registration with OTP, auto-login the user
        do {
            let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            let loginResult = try await loginWithResponse(email: trimmedEmail, password: password)
            
            if let user = loginResult.user {
                UserStorage.saveProfile(user)
                print("✅ [AuthService] Registration with OTP successful, user auto-logged in")
                
                // Restore profile image if available
                if let email = user.email {
                    ProfileImageService.shared.restoreAfterLogin(email: email)
                    if let imageUrl = user.resolvedProfileImageUrl ?? UserStorage.fetchProfileImageUrl() {
                        ProfileImageService.shared.updateProfileImage(imageUrl, email: email)
                    }
                }
            } else {
                // If user not in response, fetch profile
                let profile = try await UserService.shared.fetchProfile()
                UserStorage.saveProfile(profile)
                print("✅ [AuthService] Registration with OTP successful, profile fetched")
            }
        } catch {
            print("⚠️ [AuthService] Registration successful but auto-login failed: \(error.localizedDescription)")
            // Still return the response even if auto-login fails
            // The user can login manually
        }
        
        return response
    }

    /// Enregistre le token FCM si disponible après la connexion
    private func registerFcmTokenIfAvailable() async {
        if let fcmToken = FirebaseMessagingService.shared.fcmToken {
            do {
                try await FirebaseMessagingService.shared.registerTokenWithBackend(token: fcmToken)
                print("✅ [AuthService] FCM token registered after login")
            } catch {
                print("⚠️ [AuthService] Failed to register FCM token after login: \(error.localizedDescription)")
            }
        }
    }
}


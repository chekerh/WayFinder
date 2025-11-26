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

        let profile = try await APIService.shared.request(builder, decodeTo: UserProfile.self)
        persistProfileUpdate(user: profile, imageUrl: profile.resolvedProfileImageUrl)
        return profile
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
        
        let profile = try await APIService.shared.request(builder, decodeTo: UserProfile.self)
        persistProfileUpdate(user: profile, imageUrl: profile.resolvedProfileImageUrl)
        return profile
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
        
        let profile = try await APIService.shared.request(builder, decodeTo: UserProfile.self)
        persistProfileUpdate(user: profile, imageUrl: profile.resolvedProfileImageUrl)
        return profile
    }
    
    /// Upload une image de profil directement via l'API backend
    func uploadProfileImage(imageData: Data) async throws -> UploadProfileImageResponse {
        guard let token = TokenStorage.fetch() else {
            throw APIError.custom("Token manquant")
        }
        
        // Préparer la requête multipart/form-data
        let boundary = UUID().uuidString
        var body = Data()
        
        // Ajouter le fichier image
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"image\"; filename=\"profile.jpg\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
        body.append(imageData)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)
        
        let url = APIConfig.baseURL.appendingPathComponent("user/profile/upload-image")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        request.httpBody = body
        
        print("🔄 [UserService] Uploading profile image to backend...")
        
        let session = URLSession.shared
        let (responseData, response) = try await session.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        
        guard (200..<300).contains(httpResponse.statusCode) else {
            let errorMessage = String(data: responseData, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
            print("❌ [UserService] Upload failed: \(errorMessage)")
            throw APIError.httpError(httpResponse.statusCode, responseData)
        }
        
        // Décoder la réponse
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        let uploadResponse = try decoder.decode(UploadProfileImageResponse.self, from: responseData)
        
        print("✅ [UserService] Profile image uploaded successfully: \(uploadResponse.profileImageUrl)")
        
        // Mettre à jour le profil localement avec l'URL de l'image
        let finalImageUrl = uploadResponse.profileImageUrl
        
        // Toujours sauvegarder l'URL de l'image avec l'email pour persistance après déconnexion
        if let user = uploadResponse.user {
            // Sauvegarder le profil complet avec l'email
            UserStorage.saveProfile(user)
            
            // Mettre à jour le service centralisé
            if let email = user.email {
                await ProfileImageService.shared.updateProfileImage(finalImageUrl, email: email)
            }
        } else {
            // Sauvegarder au moins l'URL de l'image avec l'email stocké
            let name = UserStorage.fetchDisplayName()
            let email = UserDefaults.standard.string(forKey: UserStorage.userEmailKey)
            UserStorage.saveProfile(displayName: name, profileImageUrl: finalImageUrl, email: email)
            
            if let email = email {
                await ProfileImageService.shared.updateProfileImage(finalImageUrl, email: email)
            }
        }
        
        print("✅ [UserService] Profile image URL saved to UserStorage: \(finalImageUrl)")
        
        return uploadResponse
    }
    
    /// Enregistre le token FCM auprès du backend
    func registerFcmToken(token: String) async throws {
        guard let authToken = TokenStorage.fetch() else {
            throw APIError.custom("Token manquant")
        }
        
        let request = FcmTokenRequest(token: token)
        let encoder = JSONEncoder()
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "user/fcm-token",
            headers: [
                "Authorization": "Bearer \(authToken)",
                "Content-Type": "application/json"
            ],
            body: body
        )
        
        let response: FcmTokenResponse = try await APIService.shared.request(builder, decodeTo: FcmTokenResponse.self)
        print("✅ [UserService] FCM token registered: \(response.message)")
    }
}

struct UploadProfileImageResponse: Decodable {
    let message: String
    let profileImageUrl: String
    let user: UserProfile?
    
    enum CodingKeys: String, CodingKey {
        case message
        case profileImageUrl = "profile_image_url"
        case user
    }
    
    init(message: String, profileImageUrl: String, user: UserProfile?) {
        self.message = message
        self.profileImageUrl = profileImageUrl
        self.user = user
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Décoder message
        message = try container.decode(String.self, forKey: .message)
        
        // Décoder profile_image_url - utiliser directement la clé snake_case
        // car on n'utilise pas convertFromSnakeCase pour ce décodage
        profileImageUrl = try container.decode(String.self, forKey: .profileImageUrl)
        
        // Le user est optionnel, donc on peut essayer de le décoder sans faire planter tout le décodage
        // UserProfile a son propre init(from decoder:) qui gère les CodingKeys
        // Décoder UserProfile avec un decoder séparé pour éviter les conflits
        if container.contains(.user) {
            do {
                // Créer un sous-decoder pour UserProfile
                let userDecoder = try container.superDecoder(forKey: .user)
                user = try UserProfile(from: userDecoder)
            } catch {
                print("⚠️ [API] Failed to decode user object: \(error.localizedDescription)")
                user = nil
            }
        } else {
            user = nil
        }
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
    let message: String?
}

struct UpdateProfileImageRequest: Encodable {
    let profileImageUrl: String
    
    enum CodingKeys: String, CodingKey {
        case profileImageUrl = "profile_image_url"
    }
}

struct FcmTokenRequest: Encodable {
    let token: String
}

struct FcmTokenResponse: Decodable {
    let message: String
}

private extension UserService {
    func persistProfileUpdate(user: UserProfile?, imageUrl: String?) {
        if let user = user {
            // Sauvegarder avec l'email pour la persistance après déconnexion
            UserStorage.saveProfile(user)
        } else if let imageUrl = imageUrl, !imageUrl.isEmpty {
            let name = UserStorage.fetchDisplayName()
            // Récupérer l'email stocké si disponible
            let email = UserDefaults.standard.string(forKey: UserStorage.userEmailKey)
            UserStorage.saveProfile(displayName: name, profileImageUrl: imageUrl, email: email)
        }
    }
}

